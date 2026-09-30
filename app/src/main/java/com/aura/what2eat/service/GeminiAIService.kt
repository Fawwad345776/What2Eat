package com.aura.what2eat.service

import android.util.Log
import com.aura.what2eat.model.CuisineType
import com.aura.what2eat.model.DayType
import com.aura.what2eat.model.Dish
import com.aura.what2eat.model.MealType
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

object GeminiAIService {

    private const val TAG = "GeminiAIService"

    const val GEMINI_API_KEY = "AQ.Ab8RN6Is4LgiGuXf_G7G0zO9flMiLDwBYng7WOYu35pFR3CZjw"
    
    // Model fallback cascade for 100% high availability (avoids 429 quota and 503 errors)
    val GEMINI_MODELS = listOf(
        "gemini-flash-lite-latest",
        "gemini-flash-latest",
        "gemini-3.1-flash-lite",
        "gemini-3.5-flash-lite",
        "gemini-3.5-flash"
    )

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    // -------------------------------------------------------------
    // HTTP Call to Google Gemini API with automatic model failover
    // -------------------------------------------------------------
    private suspend fun callGemini(prompt: String): String = withContext(Dispatchers.IO) {
        var lastException: Exception? = null

        for (modelName in GEMINI_MODELS) {
            val requestUrl = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$GEMINI_API_KEY"

            try {
                // Build Gemini Request Body
                val requestJson = JsonObject().apply {
                    val contentsArray = JsonArray().apply {
                        val contentObj = JsonObject().apply {
                            val partsArray = JsonArray().apply {
                                val textObj = JsonObject().apply {
                                    addProperty("text", prompt)
                                }
                                add(textObj)
                            }
                            add("parts", partsArray)
                        }
                        add(contentObj)
                    }
                    add("contents", contentsArray)

                    val generationConfig = JsonObject().apply {
                        addProperty("responseMimeType", "application/json")
                        addProperty("temperature", 1.0)
                    }
                    add("generationConfig", generationConfig)
                }

                val requestBody = requestJson.toString().toRequestBody(JSON_MEDIA_TYPE)

                val request = Request.Builder()
                    .url(requestUrl)
                    .post(requestBody)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("X-goog-api-key", GEMINI_API_KEY)
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string().orEmpty()

                if (!response.isSuccessful) {
                    Log.w(TAG, "Model $modelName failed: HTTP ${response.code} - $responseBody. Trying next model...")
                    lastException = IllegalStateException("HTTP ${response.code} on $modelName: $responseBody")
                    continue
                }

                // Parse candidates[0].content.parts[0].text
                val rootObj = JsonParser.parseString(responseBody).asJsonObject
                val candidates = rootObj.getAsJsonArray("candidates")
                if (candidates == null || candidates.isEmpty) {
                    Log.w(TAG, "No candidates returned from $modelName. Trying next model...")
                    lastException = IllegalStateException("No candidates from $modelName")
                    continue
                }

                val firstCandidate = candidates[0].asJsonObject
                val content = firstCandidate.getAsJsonObject("content")
                val parts = content?.getAsJsonArray("parts")
                if (parts == null || parts.isEmpty) {
                    Log.w(TAG, "No text parts in $modelName response. Trying next model...")
                    lastException = IllegalStateException("No text parts from $modelName")
                    continue
                }

                val rawText = parts[0].asJsonObject.get("text")?.asString.orEmpty()
                val clean = cleanJsonResponse(rawText)
                if (clean.isNotBlank()) {
                    Log.i(TAG, "Successfully generated suggestions using model: $modelName")
                    return@withContext clean
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error executing model $modelName: ${e.message}. Trying next model...")
                lastException = e
            }
        }

        throw lastException ?: IllegalStateException("All Gemini models in fallback cascade failed")
    }

    /**
     * Strips any markdown code fences (```json ... ```) or outer text.
     * Safely determines whether the outer response is a JSON Object or a JSON Array.
     */
    private fun cleanJsonResponse(raw: String): String {
        var clean = raw.trim()
        if (clean.startsWith("```json")) {
            clean = clean.removePrefix("```json")
        } else if (clean.startsWith("```")) {
            clean = clean.removePrefix("```")
        }
        if (clean.endsWith("```")) {
            clean = clean.removeSuffix("```")
        }
        clean = clean.trim()

        val firstArray = clean.indexOf('[')
        val lastArray = clean.lastIndexOf(']')
        val firstObj = clean.indexOf('{')
        val lastObj = clean.lastIndexOf('}')

        // Determine whether outermost container is an Object {...} or Array [...]
        if (firstObj != -1 && (firstArray == -1 || firstObj < firstArray)) {
            if (lastObj > firstObj) {
                return clean.substring(firstObj, lastObj + 1).trim()
            }
        } else if (firstArray != -1 && (firstObj == -1 || firstArray < firstObj)) {
            if (lastArray > firstArray) {
                return clean.substring(firstArray, lastArray + 1).trim()
            }
        }

        return clean
    }

    // -------------------------------------------------------------
    // Public Suspend Functions (Routed via High-Speed Groq Cloud API)
    // -------------------------------------------------------------

    /**
     * Suggests a tailored list of meal dishes using Groq Cloud API.
     */
    suspend fun suggestDishes(
        country: String,
        city: String,
        mealType: MealType,
        dayType: DayType,
        dishCount: Int,
        sweetDishCount: Int,
        excludedDishes: List<String>,
        wantToCook: List<String>,
        dontWantToCook: List<String>,
        season: String,
        isRamadan: Boolean,
        occasion: String?
    ): List<Dish> {
        return try {
            GroqAIService.suggestDishes(
                country = country,
                city = city,
                mealType = mealType,
                dayType = dayType,
                dishCount = dishCount,
                sweetDishCount = sweetDishCount,
                excludedDishes = excludedDishes,
                wantToCook = wantToCook,
                dontWantToCook = dontWantToCook,
                season = season,
                isRamadan = isRamadan,
                occasion = occasion
            )
        } catch (e: Exception) {
            Log.w(TAG, "GroqAIService.suggestDishes failed, falling back to LocalFallbackService", e)
            LocalFallbackService.getFallbackSuggestions(
                country = country,
                mealType = mealType,
                dayType = dayType,
                dishCount = dishCount,
                sweetDishCount = sweetDishCount,
                excludedDishes = excludedDishes,
                wantToCook = wantToCook,
                dontWantToCook = dontWantToCook
            )
        }
    }

    /**
     * Generates a complete, detailed recipe with steps, ingredients, and chef tips using Groq Cloud API & Firestore caching.
     */
    suspend fun generateRecipe(
        dishName: String,
        cuisine: String,
        country: String
    ): Dish? {
        return try {
            GroqAIService.generateRecipe(
                dishName = dishName,
                cuisine = cuisine,
                country = country
            )
        } catch (e: Exception) {
            Log.e(TAG, "GroqAIService.generateRecipe failed for $dishName", e)
            null
        }
    }

    /**
     * Recommends standard "Want to cook" favorites and "Don't want to cook" items for a country using Groq.
     */
    suspend fun getKitchenPreferenceSuggestions(country: String): Pair<List<String>, List<String>> {
        return try {
            GroqAIService.getKitchenPreferenceSuggestions(country)
        } catch (e: Exception) {
            Log.e(TAG, "GroqAIService.getKitchenPreferenceSuggestions failed for $country", e)
            LocalFallbackService.getFallbackKitchenPreferences(country)
        }
    }

    // Direct access to Groq AI Response Models and features
    suspend fun decideDailyMeal(
        city: String,
        country: String = "Pakistan",
        mealType: MealType,
        isSpecial: Boolean,
        isSweetDish: Boolean,
        wantToCookList: List<String>,
        dontWantToCookList: List<String>,
        past30DaysHistory: List<String>
    ) = GroqAIService.decideDailyMeal(
        city = city,
        country = country,
        mealType = mealType,
        isSpecial = isSpecial,
        isSweetDish = isSweetDish,
        wantToCookList = wantToCookList,
        dontWantToCookList = dontWantToCookList,
        past30DaysHistory = past30DaysHistory
    )

    suspend fun matchPantryIngredients(
        availableIngredients: List<String>,
        dontWantToCookList: List<String> = emptyList(),
        city: String = "Karachi",
        country: String = "Pakistan"
    ) = GroqAIService.matchPantryIngredients(
        availableIngredients = availableIngredients,
        dontWantToCookList = dontWantToCookList,
        city = city,
        country = country
    )

    suspend fun generateFullRecipe(
        dishName: String,
        servings: Int = 4,
        cuisine: String = "Pakistani",
        country: String = "Pakistan"
    ) = GroqAIService.generateFullRecipe(
        dishName = dishName,
        servings = servings,
        cuisine = cuisine,
        country = country
    )

    suspend fun discoverDineOut(
        city: String,
        cuisinePreference: String,
        mealTime: String
    ) = GroqAIService.discoverDineOut(
        city = city,
        cuisinePreference = cuisinePreference,
        mealTime = mealTime
    )

    // -------------------------------------------------------------
    // Safe JSON Parsers (Handles case-insensitivity & missing fields)
    // -------------------------------------------------------------
    private fun parseDishesJson(
        json: String,
        defaultCountry: String,
        defaultMealType: MealType,
        defaultDayType: DayType
    ): List<Dish> {
        val list = mutableListOf<Dish>()
        val jsonElement = JsonParser.parseString(json)
        val jsonArray = if (jsonElement.isJsonArray) {
            jsonElement.asJsonArray
        } else if (jsonElement.isJsonObject && jsonElement.asJsonObject.has("dishes")) {
            jsonElement.asJsonObject.getAsJsonArray("dishes")
        } else {
            return emptyList()
        }

        for (elem in jsonArray) {
            if (!elem.isJsonObject) continue
            val obj = elem.asJsonObject
            val dishName = obj.get("name")?.asString.orEmpty()
            if (dishName.isBlank()) continue

            val cuisineStr = obj.get("cuisine")?.asString
            val mealTypeStr = obj.get("mealType")?.asString
            val dayTypeStr = obj.get("dayType")?.asString

            val dish = Dish(
                id = obj.get("id")?.asString.orEmpty(),
                name = dishName,
                cuisine = CuisineType.fromString(cuisineStr),
                country = obj.get("country")?.asString ?: defaultCountry,
                mealType = if (mealTypeStr != null) MealType.fromString(mealTypeStr) else defaultMealType,
                dayType = if (dayTypeStr != null) DayType.fromString(dayTypeStr) else defaultDayType,
                season = obj.get("season")?.asString.orEmpty(),
                isSpecial = obj.get("isSpecial")?.asBoolean ?: (defaultDayType == DayType.SPECIAL),
                prepTimeMinutes = obj.get("prepTimeMinutes")?.asInt ?: 30,
                difficulty = obj.get("difficulty")?.asString ?: "Medium",
                servings = obj.get("servings")?.asInt ?: 4,
                ingredients = obj.getAsJsonArray("ingredients")?.mapNotNull { it.asString } ?: emptyList(),
                recipeSteps = obj.getAsJsonArray("recipeSteps")?.mapNotNull { it.asString } ?: emptyList(),
                chefTip = obj.get("chefTip")?.asString.orEmpty(),
                tags = obj.getAsJsonArray("tags")?.mapNotNull { it.asString } ?: emptyList(),
                isAIGenerated = true
            )
            list.add(dish)
        }
        return list
    }

    private fun parseSingleDishJson(
        json: String,
        defaultName: String,
        defaultCuisine: String,
        defaultCountry: String
    ): Dish? {
        return try {
            val obj = JsonParser.parseString(json).asJsonObject
            val recognized = if (obj.has("recognized")) obj.get("recognized")?.asBoolean ?: true else true
            if (!recognized) {
                return null
            }

            val steps = (obj.getAsJsonArray("recipeSteps")
                ?: obj.getAsJsonArray("steps")
                ?: obj.getAsJsonArray("instructions")
                ?: obj.getAsJsonArray("recipe_steps"))?.mapNotNull { it.asString } ?: emptyList()
            if (steps.isEmpty()) {
                return null
            }

            val ingredients = (obj.getAsJsonArray("ingredients")
                ?: obj.getAsJsonArray("ingredientsList")
                ?: obj.getAsJsonArray("items"))?.mapNotNull { it.asString } ?: emptyList()

            val name = obj.get("name")?.asString.orEmpty().ifBlank { defaultName }
            val cuisineStr = obj.get("cuisine")?.asString ?: defaultCuisine
            val mealTypeStr = obj.get("mealType")?.asString
            val dayTypeStr = obj.get("dayType")?.asString

            Dish(
                id = obj.get("id")?.asString.orEmpty(),
                name = name,
                cuisine = CuisineType.fromString(cuisineStr),
                country = obj.get("country")?.asString ?: defaultCountry,
                mealType = MealType.fromString(mealTypeStr),
                dayType = DayType.fromString(dayTypeStr),
                season = obj.get("season")?.asString.orEmpty(),
                isSpecial = obj.get("isSpecial")?.asBoolean ?: false,
                prepTimeMinutes = obj.get("prepTimeMinutes")?.asInt ?: 35,
                difficulty = obj.get("difficulty")?.asString ?: "Medium",
                servings = obj.get("servings")?.asInt ?: 4,
                ingredients = if (ingredients.isNotEmpty()) ingredients else listOf("Main Ingredients", "Spices", "Oil / Ghee"),
                recipeSteps = steps,
                chefTip = obj.get("chefTip")?.asString.orEmpty(),
                tags = obj.getAsJsonArray("tags")?.mapNotNull { it.asString } ?: emptyList(),
                isAIGenerated = true
            )
        } catch (e: Exception) {
            Log.e(TAG, "parseSingleDishJson failed", e)
            null
        }
    }

    // -------------------------------------------------------------
    // Prompt Builders
    // -------------------------------------------------------------
    private fun buildDishSuggestionPrompt(
        country: String,
        city: String,
        mealType: MealType,
        dayType: DayType,
        dishCount: Int,
        sweetDishCount: Int,
        excludedDishes: List<String>,
        wantToCook: List<String>,
        dontWantToCook: List<String>,
        season: String,
        isRamadan: Boolean,
        occasion: String?
    ): String {
        val totalDishes = dishCount + sweetDishCount
        val occasionInfo = if (!occasion.isNullOrBlank()) "Special Occasion: $occasion" else "Regular Day"
        val ramadanInfo = if (isRamadan) "Active context: Ramadan (suggest appropriate Iftari / Sehri dishes)" else "Regular timing"

        val sessionSeed = System.currentTimeMillis() % 1000000
        val targetCountry = country.ifBlank { "Pakistan" }
        val targetCity = city.ifBlank { "Karachi" }

        val culinaryPillars = if (dayType == DayType.NORMAL) {
            listOf(
                "Wholesome Comforts & Daal Roti (e.g. Kadhi Pakora with Rice, Dhaba Chana Daal Tarka, Moong Masoor Daal with Roti, Tadka Daal Chawal with Kachumber, Daal Palak)",
                "Everyday Sabzi Roti (e.g. Bhindi Masala Fry, Aloo Gobi Masala, Mix Sabzi, Baingan Bharta, Palak Aloo with Warm Roti)",
                "Comforting Daal Chawal & Kadhi Chawal (e.g. Tarka Daal Chawal with Achar, Kadhi Chawal with Pakora, Moong Daal Khichdi with Dahi)",
                "Dahi Specialties & Homestyle Salan (e.g. Dahi Phulki with Roti, Classic Aloo Gosht Shorba, Aloo Chicken Salan, Anda Curry with Roti)"
            )
        } else {
            listOf(
                "Aromatic Rice Specialties (e.g. Sindhi Biryani, Karachi Student Biryani, Hyderabadi Dum Biryani, Mutton Yakhni Pulao, Bannu Beef Pulao, Mandi, Kabsa)",
                "Fast Food & Modern Comfort Favorites (e.g. Chicken Tikka Pizza, Crispy Zinger Burger, Cheesy Margherita Pizza, Chicken Broast with Garlic Sauce, Loaded Fries)",
                "Sizzling Karahi & Handi (e.g. Peshawari Shinwari Karahi, Chicken White Handi, Butter Chicken, Lahori Desi Ghee Karahi, Koyla Karahi)",
                "Barbecue & Grills (e.g. Chicken Tikka BBQ, Beef Seekh Kabab, Bihari Boti, Chicken Malai Boti, Balochi Sajji, Peshawari Chapli Kabab)",
                "Opulent Feasts & Celebrations (e.g. Shahi Nalli Nihari, Mutton Siri Paye, Chinioti Mutton Kunna, Beef Shahi Haleem, Shahi Danedaar Korma)"
            )
        }
        val selectedPillar = culinaryPillars.random()

        return """
            You are What2Eat AI, an elite regional and international culinary chef and intelligent kitchen decider.
            Generate a personalized list of $totalDishes delicious, appetizing, and inspiring meal suggestions.
            
            Context:
            - User Selected Country: $targetCountry
            - City: $targetCity
            - Target Meal: ${mealType.displayName}
            - Day Type: ${dayType.displayName} (NORMAL = everyday home cooking & staples; SPECIAL = celebratory feasts, fast food, BBQ & rich weekend dishes)
            - Season: ${season.ifBlank { "Current Season" }}
            - $ramadanInfo
            - $occasionInfo
            - Inspiration Focus for this session: $selectedPillar
            
            Dish Breakdown & Culinary Variety:
            - Total Savory/Main Dishes: $dishCount
            - Sweet / Dessert Dishes: $sweetDishCount (Desserts popular in $targetCountry)
            - Allowed Cuisines: PAKISTANI, INDIAN, BANGLADESHI, CHINESE, ITALIAN, CONTINENTAL, STREET_FOOD, DESSERT, OTHER
            - Allowed Meal Types: SEHRI, BREAKFAST, LUNCH, EVENING_SNACKS, DINNER, IFTARI
            - Allowed Day Types: NORMAL, SPECIAL
            
            Strict Quality & Diversity Rules:
            1. MEAL TYPE & DAY TYPE CULINARY RULES FOR $targetCountry:
               ${if (mealType == com.aura.what2eat.model.MealType.BREAKFAST) GroqAIService.getBreakfastGuidance(targetCountry) else if (dayType == DayType.NORMAL) GroqAIService.getNormalDayGuidance(targetCountry) else GroqAIService.getSpecialDayGuidance(targetCountry)}

            3. COUNTRY & LOCAL ACCURACY:
               - The user is in "$targetCountry" ($targetCity). Dishes must reflect what people living in $targetCountry love to cook or eat, including authentic local treasures and widely embraced international hits (like Pizza, Pasta, Burgers, Chinese).
               - Accurately set the "country" and "cuisine" fields for each dish.

            4. SESSION ENTROPY (Session Seed: $sessionSeed):
               - Each time the user taps or refreshes, surprise them with distinct, flavorful ideas. If dishCount is 1, pick an appetizing, stand-out meal option!

            5. EXCLUDED DISHES (Already eaten or seen - NEVER REPEAT):
               ${if (excludedDishes.isNotEmpty()) excludedDishes.joinToString(", ") else "None"}
               
            6. WANT TO COOK (Prioritize if provided):
               ${if (wantToCook.isNotEmpty()) wantToCook.joinToString(", ") else "Any appetizing dish"}
               
            7. DONT WANT TO COOK (Never suggest):
               ${if (dontWantToCook.isNotEmpty()) dontWantToCook.joinToString(", ") else "None"}
            
            OUTPUT FORMAT:
            Return ONLY a raw JSON array of dish objects (no markdown code fences, no extra commentary, no comments).
            Each object MUST strictly follow this JSON schema:
            [
              {
                "id": "ai_dish_1",
                "name": "Dish Name",
                "cuisine": "PAKISTANI",
                "country": "$targetCountry",
                "mealType": "${mealType.name}",
                "dayType": "${dayType.name}",
                "season": "$season",
                "isSpecial": ${dayType == DayType.SPECIAL},
                "prepTimeMinutes": 35,
                "difficulty": "Easy",
                "servings": 4,
                "ingredients": ["Ingredient 1", "Ingredient 2", "Spices"],
                "recipeSteps": [
                  "Step 1...",
                  "Step 2...",
                  "Step 3..."
                ],
                "chefTip": "Pro tip for cooking this dish...",
                "tags": ["Tag1", "Tag2"],
                "isAIGenerated": true
              }
            ]
        """.trimIndent()
    }

    private fun buildRecipePrompt(dishName: String, cuisine: String, country: String): String {
        val countryContext = if (country.isNotBlank()) " (regional context: $country)" else ""
        return """
            You are What2Eat AI, an elite international and regional culinary chef and intelligent kitchen assistant.
            The user wants an authentic recipe for: "$dishName" ($cuisine cuisine$countryContext).
            
            CRITICAL LANGUAGE REQUIREMENT:
            - All output including dish name, ingredients, cooking steps, and chefTip MUST strictly be written in English language.
            
            IMPORTANT FOOD VALIDATION RULES:
            1. Check if "$dishName" is a real, recognizable edible dish, food, sweet, dessert, beverage, or delicacy from ANY global or local cuisine (e.g., Pakistani, Indian, Chinese, Italian, Continental, Middle Eastern, Turkish, Mexican, Japanese, Thai, American, Street Food, Bakery, etc.).
            2. If "$dishName" is NOT a recognized food/dish, or is gibberish, nonsense words, random letters, an invalid object, or you cannot generate an authentic recipe for it, you MUST return ONLY this JSON:
               {
                 "recognized": false
               }
            3. If "$dishName" IS a real, recognizable food or dish, return ONLY this raw JSON schema:
               {
                 "recognized": true,
                 "id": "recipe_${dishName.lowercase().replace(" ", "_")}",
                 "name": "$dishName",
                 "cuisine": "${CuisineType.fromString(cuisine).name}",
                 "country": "$country",
                 "mealType": "DINNER",
                 "dayType": "NORMAL",
                 "season": "All",
                 "isSpecial": false,
                 "prepTimeMinutes": 35,
                 "difficulty": "Medium",
                 "servings": 4,
                 "ingredients": [
                   "Exact ingredient with quantity 1",
                   "Exact ingredient with quantity 2"
                 ],
                 "recipeSteps": [
                   "Detailed step 1...",
                   "Detailed step 2...",
                   "Detailed step 3..."
                 ],
                 "chefTip": "Secret chef technique for best flavor...",
                 "tags": ["Homemade", "$cuisine"],
                 "isAIGenerated": true
               }
               
            Return ONLY raw valid JSON matching this specification. Do not include markdown codeblocks or conversational text.
        """.trimIndent()
    }
}
