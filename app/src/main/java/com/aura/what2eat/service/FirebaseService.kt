package com.aura.what2eat.service

import android.util.Log
import com.aura.what2eat.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

object FirebaseService {

    private const val TAG = "FirebaseService"

    private val firestore: FirebaseFirestore get() = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()

    @Volatile
    private var fallbackUserId: String = ""

    fun setFallbackUserId(id: String) {
        if (id.isNotBlank() && id != "guest_user") {
            fallbackUserId = id
        }
    }

    val currentUserId: String
        get() {
            val authUid = auth.currentUser?.uid
            return if (!authUid.isNullOrBlank()) {
                authUid
            } else if (fallbackUserId.isNotBlank()) {
                fallbackUserId
            } else {
                "usr_temp"
            }
        }

    val currentUserEmail: String
        get() = auth.currentUser?.email ?: ""

    // Collections
    private const val COLLECTION_COMMUNITY = "community_dishes"
    private const val COLLECTION_RECIPES = "recipes"
    private const val COLLECTION_USERS = "users"
    private const val COLLECTION_BANNED_USERS = "banned_users"
    private const val COLLECTION_PRO_USERS = "pro_users"
    private const val SUB_COLLECTION_COOKED_HISTORY = "cooked_history"
    private const val SUB_COLLECTION_WEEKLY_PLAN = "weekly_plan"
    private const val SUB_COLLECTION_PREFERENCES = "preferences"
    private const val DOC_CURRENT_PLAN = "current_plan"
    private const val DOC_USER_PREFS = "user_prefs"

    // Community Moderation Threshold
    const val REPORT_BLOCK_THRESHOLD = 5

    // Thread-safe in-memory cache of banned user IDs / emails for fast real-time filtering
    private val bannedUsersCache = java.util.Collections.synchronizedSet(mutableSetOf<String>())

    // -------------------------------------------------------------
    // 1. Community Suggestions
    // -------------------------------------------------------------

    /**
     * Checks whether a dish is visible to the public (not blocked, under 5 reports, approved, and author not banned).
     */
    private fun isDishVisible(dish: CommunityDish): Boolean {
        if (dish.isBlocked) return false
        if (dish.reportCount >= REPORT_BLOCK_THRESHOLD) return false
        if (!dish.isApproved) return false
        if (dish.userUID.isNotBlank() && bannedUsersCache.contains(dish.userUID)) return false
        if (dish.userEmail.isNotBlank() && bannedUsersCache.contains(dish.userEmail.lowercase())) return false
        return true
    }

    /**
     * Fetches top approved community dishes ordered by likes count.
     * Excludes any dish with 5+ reports, blocked status, or from a banned author.
     */
    suspend fun fetchTopSuggestions(limit: Int = 10): List<CommunityDish> {
        return try {
            val snapshot = firestore.collection(COLLECTION_COMMUNITY)
                .get()
                .await()

            snapshot.documents
                .mapNotNull { it.data?.toCommunityDish() }
                .filter { isDishVisible(it) }
                .sortedByDescending { it.likesCount }
                .take(limit)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching top suggestions", e)
            emptyList()
        }
    }

    /**
     * Fetches all approved community dishes with most liked always visible at the top.
     * Excludes any dish with 5+ reports, blocked status, or from a banned author.
     */
    suspend fun fetchAllSuggestions(): List<CommunityDish> {
        return try {
            val snapshot = firestore.collection(COLLECTION_COMMUNITY)
                .get()
                .await()

            snapshot.documents
                .mapNotNull { it.data?.toCommunityDish() }
                .filter { isDishVisible(it) }
                .sortedByDescending { it.likesCount }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching all suggestions", e)
            emptyList()
        }
    }

    /**
     * Real-time listener for community dish suggestions.
     * Newly added suggestions directly appear in the list in real-time,
     * with the most liked dishes always visible at the top.
     * Excludes dishes with 5+ reports, blocked status, or from banned users.
     */
    fun observeSuggestions(): Flow<List<CommunityDish>> = callbackFlow {
        val query = firestore.collection(COLLECTION_COMMUNITY)

        val listenerRegistration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error listening to community suggestions", error)
                trySend(emptyList())
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val dishes = snapshot.documents
                    .mapNotNull { it.data?.toCommunityDish() }
                    .filter { isDishVisible(it) }
                    .sortedByDescending { it.likesCount }
                trySend(dishes)
            }
        }

        awaitClose {
            listenerRegistration.remove()
        }
    }

    /**
     * Checks if a user is banned from submitting suggestions due to community reports.
     */
    suspend fun isUserBanned(userId: String, userEmail: String = ""): Boolean {
        if (userId.isNotBlank() && bannedUsersCache.contains(userId)) return true
        val normalizedEmail = userEmail.trim().lowercase()
        if (normalizedEmail.isNotBlank() && bannedUsersCache.contains(normalizedEmail)) return true

        return try {
            if (userId.isNotBlank()) {
                val doc = firestore.collection(COLLECTION_BANNED_USERS).document(userId).get().await()
                if (doc.exists() && doc.getBoolean("banned") == true) {
                    bannedUsersCache.add(userId)
                    return true
                }
            }
            if (normalizedEmail.isNotBlank()) {
                val emailDocId = "email_${normalizedEmail.replace(".", "_").replace("@", "_")}"
                val doc = firestore.collection(COLLECTION_BANNED_USERS).document(emailDocId).get().await()
                if (doc.exists() && doc.getBoolean("banned") == true) {
                    bannedUsersCache.add(normalizedEmail)
                    return true
                }
            }
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error checking user ban status", e)
            false
        }
    }

    /**
     * Submits a user's community recipe suggestion to Firestore.
     * If the user is banned, the submission is rejected.
     */
    suspend fun submitSuggestion(dish: CommunityDish): Boolean {
        return try {
            if (isUserBanned(dish.userUID, dish.userEmail)) {
                Log.w(TAG, "Blocked banned user ${dish.userUID} from submitting dish suggestion")
                return false
            }

            val docRef = if (dish.id.isNotBlank()) {
                firestore.collection(COLLECTION_COMMUNITY).document(dish.id)
            } else {
                firestore.collection(COLLECTION_COMMUNITY).document()
            }

            val dishToSave = dish.copy(
                id = docRef.id,
                createdAt = if (dish.createdAt == 0L) System.currentTimeMillis() else dish.createdAt,
                isApproved = true,
                isBlocked = false,
                reportCount = 0
            )

            docRef.set(dishToSave.toFirebaseMap()).await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error submitting dish suggestion", e)
            false
        }
    }

    /**
     * Atomically increments the like count for a community dish.
     */
    suspend fun likeSuggestion(id: String, userId: String) {
        try {
            val docRef = firestore.collection(COLLECTION_COMMUNITY).document(id)
            docRef.update(
                mapOf(
                    "likesCount" to FieldValue.increment(1),
                    "likedBy" to FieldValue.arrayUnion(userId)
                )
            ).await()
        } catch (e: Exception) {
            Log.e(TAG, "Error liking suggestion $id", e)
        }
    }

    /**
     * Atomically decrements the like count (unlike) for a community dish.
     */
    suspend fun unlikeSuggestion(id: String, userId: String) {
        try {
            val docRef = firestore.collection(COLLECTION_COMMUNITY).document(id)
            docRef.update(
                mapOf(
                    "likesCount" to FieldValue.increment(-1),
                    "likedBy" to FieldValue.arrayRemove(userId)
                )
            ).await()
        } catch (e: Exception) {
            Log.e(TAG, "Error unliking suggestion $id", e)
        }
    }

    /**
     * Submits a report against an inappropriate dish suggestion.
     * If a recipe receives 3 reports from users:
     * 1. The recipe is marked as blocked and not approved.
     * 2. The author is added to the banned_users collection so they cannot add suggestions anymore.
     * 3. ALL existing suggestions by this user are blocked and hidden so they cannot be seen by anyone.
     */
    suspend fun reportSuggestion(id: String, userId: String, reason: String) {
        try {
            val dishRef = firestore.collection(COLLECTION_COMMUNITY).document(id)
            // Use userId as document key in the subcollection to ensure unique reports per user
            val reportDocId = if (userId.isNotBlank()) userId else "anon_${System.currentTimeMillis()}"
            val reportRef = dishRef.collection("reports").document(reportDocId)

            val reportData = mapOf(
                "reportId" to reportRef.id,
                "userId" to userId,
                "reason" to reason,
                "reportedAt" to System.currentTimeMillis()
            )

            reportRef.set(reportData).await()

            // Count unique user reports for this dish
            val reportsSnapshot = dishRef.collection("reports").get().await()
            val totalReports = reportsSnapshot.size()

            dishRef.update("reportCount", totalReports).await()

            // If 5 or more users report this recipe:
            if (totalReports >= REPORT_BLOCK_THRESHOLD) {
                val dishSnapshot = dishRef.get().await()
                val authorUid = dishSnapshot.getString("userUID").orEmpty()
                val authorEmail = dishSnapshot.getString("userEmail").orEmpty().trim().lowercase()
                val authorName = dishSnapshot.getString("submittedBy").orEmpty()

                Log.w(TAG, "Dish $id reached $totalReports reports! Banning user $authorUid ($authorEmail) and blocking all their recipes.")

                // 1. Block the reported dish immediately
                dishRef.update(
                    mapOf(
                        "isBlocked" to true,
                        "isApproved" to false,
                        "reportCount" to totalReports
                    )
                ).await()

                // 2. Add author to banned users collection so they cannot add suggestions anymore
                if (authorUid.isNotBlank()) {
                    firestore.collection(COLLECTION_BANNED_USERS).document(authorUid).set(
                        mapOf(
                            "banned" to true,
                            "userUID" to authorUid,
                            "userEmail" to authorEmail,
                            "userName" to authorName,
                            "bannedAt" to System.currentTimeMillis(),
                            "reason" to "Recipe reached $totalReports community reports"
                        )
                    ).await()
                    bannedUsersCache.add(authorUid)
                }

                if (authorEmail.isNotBlank()) {
                    val emailDocId = "email_${authorEmail.replace(".", "_").replace("@", "_")}"
                    firestore.collection(COLLECTION_BANNED_USERS).document(emailDocId).set(
                        mapOf(
                            "banned" to true,
                            "userUID" to authorUid,
                            "userEmail" to authorEmail,
                            "bannedAt" to System.currentTimeMillis(),
                            "reason" to "Recipe reached $totalReports community reports"
                        )
                    ).await()
                    bannedUsersCache.add(authorEmail)
                }

                // 3. Block all old suggestions by this banned author from the app so they cannot be seen by anyone
                blockAllDishesFromUser(authorUid, authorEmail)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reporting suggestion $id", e)
        }
    }

    /**
     * Queries and blocks all dishes submitted by a banned user.
     */
    private suspend fun blockAllDishesFromUser(authorUid: String, authorEmail: String) {
        try {
            val dishIdsToBlock = mutableSetOf<String>()

            if (authorUid.isNotBlank()) {
                val uidSnapshot = firestore.collection(COLLECTION_COMMUNITY)
                    .whereEqualTo("userUID", authorUid)
                    .get()
                    .await()
                dishIdsToBlock.addAll(uidSnapshot.documents.map { it.id })
            }

            if (authorEmail.isNotBlank()) {
                val emailSnapshot = firestore.collection(COLLECTION_COMMUNITY)
                    .whereEqualTo("userEmail", authorEmail)
                    .get()
                    .await()
                dishIdsToBlock.addAll(emailSnapshot.documents.map { it.id })
            }

            if (dishIdsToBlock.isNotEmpty()) {
                firestore.runBatch { batch ->
                    for (dishDocId in dishIdsToBlock) {
                        val ref = firestore.collection(COLLECTION_COMMUNITY).document(dishDocId)
                        batch.update(ref, mapOf("isApproved" to false, "isBlocked" to true))
                    }
                }.await()
                Log.i(TAG, "Successfully blocked ${dishIdsToBlock.size} old suggestions from user $authorUid ($authorEmail)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error blocking dishes for user $authorUid", e)
        }
    }

    // -------------------------------------------------------------
    // 2. Cooked History (30-Day Anti-Repetition)
    // -------------------------------------------------------------

    /**
     * Records a dish as cooked by the user.
     */
    suspend fun saveCookedDish(dish: Dish, mealType: MealType) {
        try {
            val uid = currentUserId
            val entry = CookedHistoryEntry(
                dishName = dish.name,
                dishId = dish.id,
                mealType = mealType.name,
                cookedDate = System.currentTimeMillis()
            )

            val docRef = firestore.collection(COLLECTION_USERS)
                .document(uid)
                .collection(SUB_COLLECTION_COOKED_HISTORY)
                .document()

            docRef.set(entry).await()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving cooked dish", e)
        }
    }

    /**
     * Retrieves the entire cooked history for the current user.
     */
    suspend fun getCookedHistory(): List<CookedHistoryEntry> {
        return try {
            val uid = currentUserId
            val snapshot = firestore.collection(COLLECTION_USERS)
                .document(uid)
                .collection(SUB_COLLECTION_COOKED_HISTORY)
                .orderBy("cookedDate", Query.Direction.DESCENDING)
                .get()
                .await()

            snapshot.toObjects(CookedHistoryEntry::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching cooked history", e)
            emptyList()
        }
    }

    /**
     * Returns a list of distinct dish names cooked within the last 30 days to prevent repetition.
     */
    suspend fun getDishesCooked30Days(): List<String> {
        return try {
            val uid = currentUserId
            val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000L)

            val snapshot = firestore.collection(COLLECTION_USERS)
                .document(uid)
                .collection(SUB_COLLECTION_COOKED_HISTORY)
                .whereGreaterThanOrEqualTo("cookedDate", thirtyDaysAgo)
                .get()
                .await()

            snapshot.toObjects(CookedHistoryEntry::class.java)
                .map { it.dishName.trim() }
                .filter { it.isNotBlank() }
                .distinctBy { it.lowercase() }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching 30-day cooked dishes", e)
            emptyList()
        }
    }

    // -------------------------------------------------------------
    // 3. Weekly Meal Plan
    // -------------------------------------------------------------

    /**
     * Saves the user's weekly meal plan to Firestore.
     */
    suspend fun saveWeeklyPlan(plan: WeeklyPlan) {
        try {
            val uid = currentUserId
            val docRef = firestore.collection(COLLECTION_USERS)
                .document(uid)
                .collection(SUB_COLLECTION_WEEKLY_PLAN)
                .document(DOC_CURRENT_PLAN)

            docRef.set(plan, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving weekly plan", e)
        }
    }

    /**
     * Fetches the active weekly meal plan for the user.
     */
    suspend fun fetchWeeklyPlan(): WeeklyPlan? {
        return try {
            val uid = currentUserId
            val docRef = firestore.collection(COLLECTION_USERS)
                .document(uid)
                .collection(SUB_COLLECTION_WEEKLY_PLAN)
                .document(DOC_CURRENT_PLAN)

            val snapshot = docRef.get().await()
            if (snapshot.exists()) {
                snapshot.toObject(WeeklyPlan::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching weekly plan", e)
            null
        }
    }

    // -------------------------------------------------------------
    // 4. User Kitchen Preferences
    // -------------------------------------------------------------

    /**
     * Saves user kitchen preferences (want to cook / don't want to cook / daily habits).
     */
    suspend fun savePreferences(prefs: UserPreferences) {
        try {
            val uid = currentUserId
            val docRef = firestore.collection(COLLECTION_USERS)
                .document(uid)
                .collection(SUB_COLLECTION_PREFERENCES)
                .document(DOC_USER_PREFS)

            docRef.set(prefs, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving user preferences", e)
        }
    }

    /**
     * Fetches user kitchen preferences from Firestore.
     */
    suspend fun fetchPreferences(): UserPreferences? {
        return try {
            val uid = currentUserId
            val docRef = firestore.collection(COLLECTION_USERS)
                .document(uid)
                .collection(SUB_COLLECTION_PREFERENCES)
                .document(DOC_USER_PREFS)

            val snapshot = docRef.get().await()
            if (snapshot.exists()) {
                snapshot.toObject(UserPreferences::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user preferences", e)
            null
        }
    }

    /**
     * Saves user subscription state to Firestore.
     */
    suspend fun saveUserSubscription(subscription: UserSubscription) {
        try {
            val uid = currentUserId
            val docRef = firestore.collection(COLLECTION_USERS)
                .document(uid)
                .collection("subscription")
                .document("current_plan")

            docRef.set(subscription, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving user subscription", e)
        }
    }

    /**
     * Fetches user subscription state from Firestore.
     */
    suspend fun fetchUserSubscription(): UserSubscription? {
        return try {
            val uid = currentUserId
            val docRef = firestore.collection(COLLECTION_USERS)
                .document(uid)
                .collection("subscription")
                .document("current_plan")

            val snapshot = docRef.get().await()
            if (snapshot.exists()) {
                snapshot.toObject(UserSubscription::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user subscription", e)
            null
        }
    }

    /**
     * Ensures a user document exists in Firestore under `users/{uid}`.
     * By default sets `isPro = false` for newly created user records.
     * If the document already exists, it preserves the existing `isPro` status set by the admin!
     */
    suspend fun ensureUserDocumentExists(
        userId: String = currentUserId,
        userEmail: String = currentUserEmail
    ) {
        if (userId.isBlank() || userId == "guest_user" || userId == "usr_temp") return
        try {
            val userRef = firestore.collection(COLLECTION_USERS).document(userId)
            val snapshot = userRef.get().await()
            if (!snapshot.exists()) {
                val defaultData = mapOf(
                    "userId" to userId,
                    "email" to userEmail,
                    "isPro" to false,
                    "createdAt" to System.currentTimeMillis(),
                    "lastActiveAt" to System.currentTimeMillis(),
                    "platform" to "android"
                )
                userRef.set(defaultData).await()
                Log.d(TAG, "Created user document in Firestore: $userId with default isPro=false")
            } else {
                // Update last active time, never override isPro
                val updateMap = mutableMapOf<String, Any>(
                    "lastActiveAt" to System.currentTimeMillis()
                )
                if (userEmail.isNotBlank() && snapshot.getString("email").isNullOrBlank()) {
                    updateMap["email"] = userEmail
                }
                userRef.update(updateMap).await()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error ensuring user document exists in Firestore", e)
        }
    }

    /**
     * Checks if a user is granted PRO status remotely in Firestore.
     * Checks:
     * 1. users/{uid}/isPro
     * 2. pro_users/{uid}
     * 3. pro_users/{email}
     * 4. users/{uid}/subscription/current_plan/isPro
     */
    suspend fun checkRemoteProStatus(
        userId: String = currentUserId,
        userEmail: String = auth.currentUser?.email ?: ""
    ): Boolean {
        if (userId.isBlank() && userEmail.isBlank()) return false
        return try {
            // 1. Check users/{uid} root document (primary location for isPro)
            if (userId.isNotBlank() && userId != "guest_user" && userId != "usr_temp") {
                val userDoc = firestore.collection(COLLECTION_USERS).document(userId).get().await()
                if (userDoc.exists() && userDoc.getBoolean("isPro") == true) {
                    return true
                }

                // 2. Check pro_users collection by UID
                val docUid = firestore.collection(COLLECTION_PRO_USERS).document(userId).get().await()
                if (docUid.exists()) {
                    val isPro = docUid.getBoolean("isPro") ?: true
                    if (isPro) return true
                }

                // 3. Check users/{uid}/subscription/current_plan
                val subDoc = firestore.collection(COLLECTION_USERS)
                    .document(userId)
                    .collection("subscription")
                    .document("current_plan")
                    .get()
                    .await()
                if (subDoc.exists() && subDoc.getBoolean("isPro") == true) {
                    return true
                }
            }

            // 4. Check pro_users collection by email
            val cleanEmail = userEmail.trim().lowercase()
            if (cleanEmail.isNotBlank()) {
                val docEmail = firestore.collection(COLLECTION_PRO_USERS).document(cleanEmail).get().await()
                if (docEmail.exists()) {
                    val isPro = docEmail.getBoolean("isPro") ?: true
                    if (isPro) return true
                }
            }

            false
        } catch (e: Exception) {
            Log.e(TAG, "Error checking remote PRO status", e)
            false
        }
    }

    /**
     * Listens in real-time to remote PRO status changes in Firestore.
     * When the developer changes isPro to true in Firebase Console under users/{uid}, this callback fires immediately!
     */
    fun listenToRemoteProStatus(
        userId: String = currentUserId,
        userEmail: String = auth.currentUser?.email ?: "",
        onStatusChanged: (Boolean) -> Unit
    ) {
        val cleanEmail = userEmail.trim().lowercase()

        // Listener for users/{userId}
        if (userId.isNotBlank() && userId != "guest_user" && userId != "usr_temp") {
            firestore.collection(COLLECTION_USERS).document(userId)
                .addSnapshotListener { snapshot, error ->
                    if (error == null && snapshot != null && snapshot.exists()) {
                        val isPro = snapshot.getBoolean("isPro") == true
                        onStatusChanged(isPro)
                    }
                }

            firestore.collection(COLLECTION_PRO_USERS).document(userId)
                .addSnapshotListener { snapshot, error ->
                    if (error == null && snapshot != null && snapshot.exists()) {
                        val isPro = snapshot.getBoolean("isPro") ?: true
                        if (isPro) onStatusChanged(true)
                    }
                }
        }

        // Listener for pro_users/{email}
        if (cleanEmail.isNotBlank()) {
            firestore.collection(COLLECTION_PRO_USERS).document(cleanEmail)
                .addSnapshotListener { snapshot, error ->
                    if (error == null && snapshot != null && snapshot.exists()) {
                        val isPro = snapshot.getBoolean("isPro") ?: true
                        if (isPro) onStatusChanged(true)
                    }
                }
        }
    }

    /**
     * Grants PRO status to a user in Firestore (usable by admin).
     */
    suspend fun grantUserPro(targetUidOrEmail: String, planType: String = "admin_grant"): Boolean {
        return try {
            val docId = targetUidOrEmail.trim()
            val data = mapOf(
                "isPro" to true,
                "planType" to planType,
                "grantedAt" to System.currentTimeMillis()
            )
            // Update both users and pro_users collections
            firestore.collection(COLLECTION_USERS).document(docId).set(data, SetOptions.merge()).await()
            firestore.collection(COLLECTION_PRO_USERS).document(docId.lowercase()).set(data, SetOptions.merge()).await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error granting PRO to $targetUidOrEmail", e)
            false
        }
    }

    /**
     * Standardized fallback community dishes by country with consistent likes count across all screens.
     */
    fun getDefaultCommunityDishes(country: String): List<CommunityDish> {
        return when {
            country.contains("India", ignoreCase = true) -> listOf(
                CommunityDish(id = "c_in1", dishName = "Paneer Butter Masala", submittedBy = "Chef Priya", likesCount = 189, cuisine = "Indian", country = "India",
                    ingredients = listOf("Paneer", "Butter", "Tomatoes", "Cashew Paste", "Cream", "Garam Masala", "Kasuri Methi"),
                    recipeSteps = listOf("Blend cooked tomatoes, cashews, and spices into smooth gravy.", "Simmer with butter and heavy cream.", "Toss soft paneer cubes and finish with crushed kasuri methi."),
                    prepTime = 30, difficulty = "Medium", chefTip = "Add honey or sugar to balance tomato acidity."),
                CommunityDish(id = "c_in2", dishName = "Hyderabadi Dum Biryani", submittedBy = "Amina K.", likesCount = 165, cuisine = "Indian", country = "India",
                    ingredients = listOf("Basmati Rice", "Mutton/Chicken", "Yogurt", "Fried Onions", "Saffron Milk", "Mint"),
                    recipeSteps = listOf("Marinate meat with spiced yogurt and aromatics.", "Parboil aged basmati rice.", "Layer meat and rice, seal with dough, and slow dum cook."),
                    prepTime = 60, difficulty = "Hard", chefTip = "Use heavy bottom pot for even heat."),
                CommunityDish(id = "c_in3", dishName = "Dal Makhani & Naan", submittedBy = "Rohan S.", likesCount = 142, cuisine = "Indian", country = "India",
                    ingredients = listOf("Black Urad Dal", "Kidney Beans", "Butter", "Fresh Cream", "Garlic", "Kashmiri Chili"),
                    recipeSteps = listOf("Slow cook dal overnight or 60 mins.", "Add tomato puree and butter tarka.", "Finish with cream and smoke with hot coal (dhungar)."),
                    prepTime = 50, difficulty = "Medium", chefTip = "Slow simmering develops the silky texture."),
                CommunityDish(id = "c_in4", dishName = "Chole Bhature", submittedBy = "Chef Vikram", likesCount = 120, cuisine = "Indian", country = "India",
                    ingredients = listOf("Chickpeas", "Tea Bag", "Onions", "Pomegranate Seeds (Anardana)", "Flour for Bhature"),
                    recipeSteps = listOf("Boil chickpeas with black tea bag for deep dark color.", "Bhunify with anardana and chole masala.", "Serve with hot puffed bhature and spiced onions."),
                    prepTime = 40, difficulty = "Medium", chefTip = "Anardana gives the signature tart Punjabi punch.")
            )
            country.contains("Bangladesh", ignoreCase = true) -> listOf(
                CommunityDish(id = "c_bd1", dishName = "Dhaka Morog Polao", submittedBy = "Tariq H.", likesCount = 154, cuisine = "Bangladeshi", country = "Bangladesh",
                    ingredients = listOf("Chinigura Rice", "Desi Chicken", "Ghee", "Fried Onions (Beresta)", "Mawa", "Kewra Water"),
                    recipeSteps = listOf("Sear chicken in ghee with aromatic whole spices.", "Cook small grain chinigura rice in rich chicken stock.", "Assemble and steam on low heat with beresta and mawa."),
                    prepTime = 45, difficulty = "Medium", chefTip = "A touch of mawa brings authentic Dhakai richness."),
                CommunityDish(id = "c_bd2", dishName = "Kacchi Biryani", submittedBy = "Chef Nabila", likesCount = 143, cuisine = "Bangladeshi", country = "Bangladesh",
                    ingredients = listOf("Mutton", "Potatoes", "Basmati Rice", "Mustard Oil", "Mawa", "Alu Bukhara"),
                    recipeSteps = listOf("Marinate raw mutton with papaya paste and spices.", "Parboil rice with whole spices.", "Layer raw meat, fried potatoes, and rice; seal pot and dum cook."),
                    prepTime = 70, difficulty = "Hard", chefTip = "Do not open seal until completely cooled down."),
                CommunityDish(id = "c_bd3", dishName = "Shorshe Ilish", submittedBy = "Tanvir M.", likesCount = 118, cuisine = "Bangladeshi", country = "Bangladesh",
                    ingredients = listOf("Hilsa Fish (Ilish)", "Yellow & Black Mustard Paste", "Green Chilies", "Mustard Oil", "Turmeric"),
                    recipeSteps = listOf("Blend mustard seeds with green chilies and pinch of salt to prevent bitterness.", "Gently coat fish with mustard gravy and turmeric in mustard oil.", "Simmer with whole green chilies until oil floats."),
                    prepTime = 25, difficulty = "Easy", chefTip = "Always cook in pure pungent mustard oil.")
            )
            country.contains("Emirates", ignoreCase = true) || country.contains("UAE", ignoreCase = true) || country.contains("Saudi", ignoreCase = true) -> listOf(
                CommunityDish(id = "c_me1", dishName = "Mutton Mandi", submittedBy = "Chef Rashid", likesCount = 210, cuisine = "Middle Eastern", country = country,
                    ingredients = listOf("Tender Lamb/Mutton", "Basmati Rice", "Hawaij Spice Blend", "Dried Lemon (Loomi)", "Fried Nuts"),
                    recipeSteps = listOf("Rub lamb with Hawaij spices and slow roast until fall-apart.", "Cook rice in fragrant broth with loomi.", "Top rice with roasted lamb, toasted almonds, and raisins."),
                    prepTime = 60, difficulty = "Medium", chefTip = "Smoke the assembled rice and meat with ghee on charcoal."),
                CommunityDish(id = "c_me2", dishName = "Chicken Machboos", submittedBy = "Chef Fatima", likesCount = 178, cuisine = "Middle Eastern", country = country,
                    ingredients = listOf("Chicken", "Basmati Rice", "Bahararat Spices", "Tomatoes", "Loomi", "Cardamom"),
                    recipeSteps = listOf("Boil chicken with aromatics and spices until tender.", "Broil chicken until crispy skin.", "Cook rice in rich strained broth and serve hot with Dakkoos salsa."),
                    prepTime = 45, difficulty = "Medium", chefTip = "Dakkoos tomato garlic salsa is essential."),
                CommunityDish(id = "c_me3", dishName = "Shish Tawook with Hummus", submittedBy = "Chef Zaid", likesCount = 135, cuisine = "Middle Eastern", country = country,
                    ingredients = listOf("Chicken Cubes", "Garlic Toum", "Lemon Juice", "Paprika", "Olive Oil", "Creamy Hummus"),
                    recipeSteps = listOf("Marinate chicken in garlic, yogurt, and lemon for 4 hours.", "Skewer and grill over hot charcoals.", "Serve over warm flatbread with creamy hummus and pickles."),
                    prepTime = 30, difficulty = "Easy", chefTip = "Do not overcook the skewers to keep them juicy.")
            )
            country.contains("China", ignoreCase = true) -> listOf(
                CommunityDish(id = "c_cn1", dishName = "Szechuan Kung Pao Chicken", submittedBy = "Chef Wei", likesCount = 195, cuisine = "Chinese", country = "China",
                    ingredients = listOf("Diced Chicken", "Roasted Peanuts", "Sichuan Peppercorns", "Dried Red Chilies", "Scallions", "Dark Soy Sauce"),
                    recipeSteps = listOf("Marinate chicken in soy and cornstarch.", "Stir fry dried chilies and Sichuan peppercorns in hot wok.", "Add chicken, peanuts, and Kung Pao sauce; toss on high heat."),
                    prepTime = 20, difficulty = "Medium", chefTip = "Wok hei high flame gives the authentic smoky flavor."),
                CommunityDish(id = "c_cn2", dishName = "Yangzhou Fried Rice", submittedBy = "Chef Mei", likesCount = 160, cuisine = "Chinese", country = "China",
                    ingredients = listOf("Day-old Jasmine Rice", "Eggs", "Shrimp / Chicken", "Green Peas", "Carrots", "Sesame Oil"),
                    recipeSteps = listOf("Scramble eggs lightly and set aside.", "Stir fry meats and vegetables.", "Add cold rice and toss vigorously until every grain is separated."),
                    prepTime = 15, difficulty = "Easy", chefTip = "Day-old chilled rice prevents mushiness."),
                CommunityDish(id = "c_cn3", dishName = "Cantonese Dim Sum Platter", submittedBy = "Chef Li", likesCount = 140, cuisine = "Chinese", country = "China",
                    ingredients = listOf("Dumpling Wrappers", "Minced Chicken & Prawns", "Ginger", "Water Chestnuts", "Soy Dipping Sauce"),
                    recipeSteps = listOf("Fill wrappers with seasoned filling and pleat edges.", "Steam in bamboo steamers for 8-10 minutes.", "Serve steaming hot with chili crisp and soy sauce."),
                    prepTime = 35, difficulty = "Medium", chefTip = "Water chestnuts add delicious crunch.")
            )
            country.contains("Italy", ignoreCase = true) -> listOf(
                CommunityDish(id = "c_it1", dishName = "Fettuccine Alfredo with Truffle", submittedBy = "Chef Marco", likesCount = 220, cuisine = "Italian", country = "Italy",
                    ingredients = listOf("Fresh Fettuccine", "Parmigiano Reggiano", "Butter", "Heavy Cream", "Black Pepper", "Truffle Oil"),
                    recipeSteps = listOf("Boil pasta al dente in salted water.", "Melt butter and cream, whisk in finely grated parmesan.", "Toss pasta with sauce and starchy pasta water; finish with truffle oil."),
                    prepTime = 20, difficulty = "Easy", chefTip = "Reserve pasta water to emulsify the sauce."),
                CommunityDish(id = "c_it2", dishName = "Neapolitan Margherita Pizza", submittedBy = "Chef Sofia", likesCount = 195, cuisine = "Italian", country = "Italy",
                    ingredients = listOf("Pizza Dough", "San Marzano Tomatoes", "Fresh Mozzarella", "Fresh Basil", "Extra Virgin Olive Oil"),
                    recipeSteps = listOf("Stretch dough by hand from center outward.", "Spread crushed San Marzano tomatoes and torn mozzarella.", "Bake on baking stone at maximum temperature; garnish with fresh basil."),
                    prepTime = 15, difficulty = "Medium", chefTip = "High oven heat is the key to airy crust blisters."),
                CommunityDish(id = "c_it3", dishName = "Creamy Risotto ai Funghi", submittedBy = "Chef Luca", likesCount = 150, cuisine = "Italian", country = "Italy",
                    ingredients = listOf("Arborio Rice", "Porcini Mushrooms", "Vegetable Stock", "Shallots", "Butter", "Parmesan"),
                    recipeSteps = listOf("Sauté shallots and toast rice.", "Add warm stock one ladle at a time while stirring constantly.", "Mantecatura: vigorously beat in cold butter and parmesan off heat."),
                    prepTime = 30, difficulty = "Medium", chefTip = "Vigorous beating at the end releases starch.")
            )
            country.contains("United States", ignoreCase = true) || country.contains("USA", ignoreCase = true) || country.contains("United Kingdom", ignoreCase = true) || country.contains("UK", ignoreCase = true) -> listOf(
                CommunityDish(id = "c_w1", dishName = "Smoked BBQ Beef Burger", submittedBy = "Chef David", likesCount = 205, cuisine = "Continental", country = country,
                    ingredients = listOf("Ground Beef Patty", "Brioche Buns", "Cheddar Cheese", "Caramelized Onions", "Smoky BBQ Sauce", "Crispy Bacon/Beef Strips"),
                    recipeSteps = listOf("Form coarse beef patties and season generously.", "Sear on blazing hot cast iron for crispy crust.", "Melt cheddar cheese, toast brioche buns, and assemble with BBQ sauce."),
                    prepTime = 20, difficulty = "Easy", chefTip = "Do not press the patties while cooking to keep juices inside."),
                CommunityDish(id = "c_w2", dishName = "Classic Chicken Tikka Masala", submittedBy = "Chef Sarah", likesCount = 188, cuisine = "Continental", country = country,
                    ingredients = listOf("Grilled Chicken Tikka", "Tomato Puree", "Heavy Cream", "Fenugreek", "Garam Masala", "Cumin"),
                    recipeSteps = listOf("Grill marinated chicken skewers until charred.", "Simmer creamy tomato gravy with spices.", "Stir chicken into sauce and serve with basmati rice or garlic naan."),
                    prepTime = 35, difficulty = "Medium", chefTip = "Slightly charred chicken adds genuine smoky character."),
                CommunityDish(id = "c_w3", dishName = "Creamy Garlic Herb Steak", submittedBy = "Chef Emma", likesCount = 142, cuisine = "Continental", country = country,
                    ingredients = listOf("Ribeye or Sirloin Steak", "Butter", "Garlic Cloves", "Fresh Rosemary", "Thyme", "Sea Salt"),
                    recipeSteps = listOf("Bring steak to room temp and season generously.", "Sear in screaming hot pan for 3 mins per side.", "Baste continuously with foaming butter, smashed garlic, and rosemary."),
                    prepTime = 15, difficulty = "Medium", chefTip = "Rest steak for 5 minutes before slicing.")
            )
            else -> listOf(
                CommunityDish(id = "c1", dishName = "Peshawari Charsi Karahi", submittedBy = "Chef Bilal", likesCount = 192, cuisine = "Pakistani", country = "Pakistan",
                    ingredients = listOf("Chicken or Mutton", "Fresh Tomatoes", "Green Chilies", "Animal Fat or Ghee", "Coarse Black Pepper", "Salt"),
                    recipeSteps = listOf("Fry meat in fat/ghee until seared golden.", "Cover with halved tomatoes until soft and peel skins.", "Bhunai on high flame with crushed black pepper and green chilies."),
                    prepTime = 30, difficulty = "Medium", chefTip = "Cook on high flame with no onions or water."),
                CommunityDish(id = "c2", dishName = "Special Beef Nalli Nihari", submittedBy = "Chef Usman", likesCount = 180, cuisine = "Pakistani", country = "Pakistan",
                    ingredients = listOf("Beef Shank (Bong)", "Bone Marrow (Nalli)", "Nihari Masala", "Flour Slurry", "Ginger Juliennes", "Green Chilies"),
                    recipeSteps = listOf("Sear beef shank with aromatic Nihari spices and ghee.", "Slow cook until fork-tender, thicken with flour slurry.", "Top with melted bone marrow and fried chili oil (tari)."),
                    prepTime = 90, difficulty = "Hard", chefTip = "Let sit 20 minutes for the oil to separate."),
                CommunityDish(id = "c3", dishName = "Karachi Chicken Biryani", submittedBy = "Chef Amina", likesCount = 165, cuisine = "Pakistani", country = "Pakistan",
                    ingredients = listOf("Aged Sella / Basmati Rice", "Chicken", "Potatoes (Aloo)", "Plums (Alu Bukhara)", "Yogurt", "Fried Onions"),
                    recipeSteps = listOf("Cook chicken with potatoes and spicy biryani masala gravy.", "Parboil rice with whole spices.", "Layer with fresh mint, yellow food color, and steam on dum for 15 mins."),
                    prepTime = 55, difficulty = "Hard", chefTip = "Aloo Bukhara gives signature Karachi tanginess."),
                CommunityDish(id = "c4", dishName = "Balochi Mutton Sajji", submittedBy = "Chef Tariq", likesCount = 135, cuisine = "Pakistani", country = "Pakistan",
                    ingredients = listOf("Whole Leg of Lamb/Mutton", "Rock Salt", "Garlic Water", "Chaat Masala", "Lemon Juice", "Kaftan Rice"),
                    recipeSteps = listOf("Brine meat in garlic water and rock salt for 4 hours.", "Slow roast vertically next to open wood fire embers until skin is crispy.", "Sprinkle heavily with Balochi chaat masala and lemon juice."),
                    prepTime = 80, difficulty = "Medium", chefTip = "Carve slits to let the garlic brine penetrate."),
                CommunityDish(id = "c5", dishName = "Lahori Dahi Bhallay", submittedBy = "Chef Hira", likesCount = 110, cuisine = "Street Food", country = "Pakistan",
                    ingredients = listOf("Lentil Dumplings (Bhallay)", "Thick Sweet Yogurt", "Boiled Potatoes & Chickpeas", "Imli Chutney", "Green Chutney", "Chaat Masala", "Papdi"),
                    recipeSteps = listOf("Soak fried lentil bhallay in warm water and gently squeeze.", "Layer with chickpeas, potatoes, and whisked sweet creamy yogurt.", "Drizzle sweet imli chutney, spicy mint chutney, and crunchy papdi."),
                    prepTime = 20, difficulty = "Easy", chefTip = "Whisk yogurt with a spoon of sugar and pinch of black salt."),
                CommunityDish(id = "c6", dishName = "Mughlai Shahi Tukray", submittedBy = "Chef Farooq", likesCount = 95, cuisine = "Dessert", country = "Pakistan",
                    ingredients = listOf("White Bread", "Full Fat Milk", "Cardamom", "Condensed Milk / Rabri", "Saffron", "Silver Leaf (Warq)", "Pistachios"),
                    recipeSteps = listOf("Deep fry bread triangles in ghee until golden and crisp.", "Dip briefly in fragrant cardamom sugar syrup.", "Pour thickened rabri over toast and garnish with saffron and slivered nuts."),
                    prepTime = 25, difficulty = "Easy", chefTip = "Fry bread in pure ghee, never cooking oil.")
            )
        }
    }

    // -------------------------------------------------------------
    // 9. Recipe Caching in Firestore (/recipes/{dishName})
    // -------------------------------------------------------------

    /**
     * Checks Firestore collection /recipes/{dishName} for cached recipe.
     */
    suspend fun getCachedRecipe(dishName: String): Map<String, Any>? {
        return try {
            val docId = dishName.trim().lowercase().replace(Regex("[^a-z0-9_]"), "_")
            val doc = firestore.collection(COLLECTION_RECIPES).document(docId).get().await()
            if (doc.exists()) {
                doc.data
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching cached recipe for $dishName", e)
            null
        }
    }

    /**
     * Saves generated recipe to Firestore collection /recipes/{dishName} for future users.
     */
    suspend fun cacheRecipe(dishName: String, recipeData: Map<String, Any>) {
        try {
            val docId = dishName.trim().lowercase().replace(Regex("[^a-z0-9_]"), "_")
            firestore.collection(COLLECTION_RECIPES)
                .document(docId)
                .set(recipeData, SetOptions.merge())
                .await()
            Log.d(TAG, "Cached recipe successfully in Firestore for $dishName ($docId)")
        } catch (e: Exception) {
            Log.e(TAG, "Error caching recipe for $dishName", e)
        }
    }

    /**
     * Saves a liked restaurant to the user's document in Firestore.
     */
    suspend fun saveLikedRestaurant(restaurantId: String) {
        try {
            val uid = currentUserId
            firestore.collection(COLLECTION_USERS)
                .document(uid)
                .collection("dining")
                .document("liked_restaurants")
                .set(mapOf("restaurantIds" to FieldValue.arrayUnion(restaurantId)), SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.w(TAG, "Error saving liked restaurant $restaurantId", e)
        }
    }

    /**
     * Removes a liked restaurant from Firestore.
     */
    suspend fun removeLikedRestaurant(restaurantId: String) {
        try {
            val uid = currentUserId
            firestore.collection(COLLECTION_USERS)
                .document(uid)
                .collection("dining")
                .document("liked_restaurants")
                .set(mapOf("restaurantIds" to FieldValue.arrayRemove(restaurantId)), SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.w(TAG, "Error removing liked restaurant $restaurantId", e)
        }
    }

    /**
     * Saves a hidden / not-interested restaurant to Firestore.
     */
    suspend fun saveHiddenRestaurant(restaurantId: String) {
        try {
            val uid = currentUserId
            firestore.collection(COLLECTION_USERS)
                .document(uid)
                .collection("dining")
                .document("hidden_restaurants")
                .set(mapOf("restaurantIds" to FieldValue.arrayUnion(restaurantId)), SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.w(TAG, "Error saving hidden restaurant $restaurantId", e)
        }
    }

    /**
     * Removes a hidden restaurant from Firestore when unhidden by user.
     */
    suspend fun removeHiddenRestaurant(restaurantId: String) {
        try {
            val uid = currentUserId
            firestore.collection(COLLECTION_USERS)
                .document(uid)
                .collection("dining")
                .document("hidden_restaurants")
                .set(mapOf("restaurantIds" to FieldValue.arrayRemove(restaurantId)), SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.w(TAG, "Error removing hidden restaurant $restaurantId", e)
        }
    }

    /**
     * Clears all hidden restaurants for the user in Firestore.
     */
    suspend fun clearHiddenRestaurants() {
        try {
            val uid = currentUserId
            firestore.collection(COLLECTION_USERS)
                .document(uid)
                .collection("dining")
                .document("hidden_restaurants")
                .set(mapOf("restaurantIds" to emptyList<String>()))
                .await()
        } catch (e: Exception) {
            Log.w(TAG, "Error clearing hidden restaurants", e)
        }
    }

    /**
     * Fetches liked restaurants from Firestore.
     */
    suspend fun fetchLikedRestaurants(): Set<String> {
        return try {
            val uid = currentUserId
            val doc = firestore.collection(COLLECTION_USERS)
                .document(uid)
                .collection("dining")
                .document("liked_restaurants")
                .get()
                .await()
            @Suppress("UNCHECKED_CAST")
            val list = doc.get("restaurantIds") as? List<String>
            list?.toSet() ?: emptySet()
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching liked restaurants", e)
            emptySet()
        }
    }

    /**
     * Fetches hidden restaurants from Firestore.
     */
    suspend fun fetchHiddenRestaurants(): Set<String> {
        return try {
            val uid = currentUserId
            val doc = firestore.collection(COLLECTION_USERS)
                .document(uid)
                .collection("dining")
                .document("hidden_restaurants")
                .get()
                .await()
            @Suppress("UNCHECKED_CAST")
            val list = doc.get("restaurantIds") as? List<String>
            list?.toSet() ?: emptySet()
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching hidden restaurants", e)
            emptySet()
        }
    }
}
