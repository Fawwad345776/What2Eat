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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

// -------------------------------------------------------------
// Groq AI Response Models
// -------------------------------------------------------------
data class MealDeciderResponse(
    val dishName: String = "",
    val description: String = "",
    val prepTimeMinutes: Int = 30,
    val cuisine: String = "Pakistani"
)

data class PantryMatcherResponse(
    val dishName: String = "",
    val description: String = "",
    val usedIngredients: List<String> = emptyList(),
    val prepTimeMinutes: Int = 30
)

data class RecipeIngredient(
    val name: String = "",
    val quantity: String = "",
    val unit: String = ""
)

data class FullRecipeResponse(
    val dishName: String = "",
    val servings: Int = 4,
    val cuisine: String = "Pakistani",
    val ingredients: List<RecipeIngredient> = emptyList(),
    val steps: List<String> = emptyList(),
    val chefTip: String = ""
)

data class DineOutDiscoveryResponse(
    val cuisineType: String = "Desi",
    val suggestedDish: String = "Chicken Karahi",
    val searchKeywordForMaps: String = "Karahi restaurant"
)

object GroqAIService {

    private const val TAG = "GroqAIService"

    // Groq API Configuration
    private const val GROQ_ENDPOINT = "https://api.groq.com/openai/v1/chat/completions"
    const val GROQ_API_KEY = "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"
    const val GROQ_MODEL = "openai/gpt-oss-20b"
    private const val TEMPERATURE = 0.85

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    /**
     * Executes an HTTP request to the Groq Cloud Chat Completions API with JSON object response mode.
     */
    private suspend fun callGroq(prompt: String): String = withContext(Dispatchers.IO) {
        val requestJson = JsonObject().apply {
            addProperty("model", GROQ_MODEL)
            addProperty("temperature", TEMPERATURE)

            // Response Format: {"type": "json_object"}
            val formatObj = JsonObject().apply {
                addProperty("type", "json_object")
            }
            add("response_format", formatObj)

            val messagesArray = JsonArray().apply {
                val messageObj = JsonObject().apply {
                    addProperty("role", "user")
                    addProperty("content", prompt)
                }
                add(messageObj)
            }
            add("messages", messagesArray)
        }

        val requestBody = requestJson.toString().toRequestBody(JSON_MEDIA_TYPE)

        val request = Request.Builder()
            .url(GROQ_ENDPOINT)
            .addHeader("Authorization", "Bearer $GROQ_API_KEY")
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string().orEmpty()

        if (!response.isSuccessful) {
            Log.e(TAG, "Groq API error HTTP ${response.code}: $responseBody")
            throw Exception("Groq API error (${response.code}): $responseBody")
        }

        val parsed = JsonParser.parseString(responseBody).asJsonObject
        val choices = parsed.getAsJsonArray("choices")
        if (choices == null || choices.size() == 0) {
            throw Exception("Empty response choices from Groq API")
        }

        val message = choices[0].asJsonObject.getAsJsonObject("message")
        message.get("content")?.asString.orEmpty()
    }

    // -------------------------------------------------------------
    // 1. Daily Meal Decider
    // -------------------------------------------------------------
    /**
     * Daily Meal Decider:
     * - Inputs: city, mealType, isSpecial (Boolean), isSweetDish (Boolean), wantToCookList, dontWantToCookList, past30DaysHistory
     * - Rules:
     *   * If isSweetDish == true -> suggest desserts / sweet treats.
     *   * If isSpecial == true -> suggest weekend / feast dishes.
     *   * If isSpecial == false -> suggest light normal home-cooked meals.
     *   * Strictly EXCLUDE anything in dontWantToCookList (e.g. "No beef") and past30DaysHistory.
     *   * Return JSON: {"dishName": "...", "description": "...", "prepTimeMinutes": 30, "cuisine": "..."}
     */
    fun getBreakfastGuidance(country: String): String {
        val c = country.lowercase()
        return when {
            c.contains("pakistan") -> """
                - CRITICAL BREAKFAST MANDATE FOR PAKISTAN:
                  The target meal is strictly BREAKFAST (Nashta).
                  You MUST ONLY suggest authentic Pakistani breakfast specialties:
                  1. PARATHA & EGGS: Crispy Lachedar Paratha with Cheese Omelette, Pakistani Masala Omelette, Anda Ghotala with Paratha, Khagina (spiced scrambled eggs) with Paratha, Half Fry Egg with Zeera Paratha.
                  2. TRADITIONAL NASHTA: Halwa Puri with Chana & Aloo Tarkari, Nihari with hot Roghani Naan / Taftan, Maghaz Masala with Paratha, Bong Paye / Siri Paye with Naan, Chana Salan with Tandoori Kulcha.
                  3. STUFFED PARATHAS: Aloo Paratha with Dahi & Mixed Achar, Mooli Paratha with fresh Butter & Dahi, Qeema Paratha with Mint Raita.
                  4. LIGHT / MODERN: Doodh Patti Chai with Rusks / Bakarkhani, French Toast with Honey, Butter Toast with Boiled Eggs.
                  STRICT PROHIBITION: DO NOT suggest Lunch or Dinner curries (NO Daal Chawal, NO Sabzi Roti, NO Karahi, NO Korma, NO Biryani, NO Pulao).
            """.trimIndent()

            c.contains("india") -> """
                - CRITICAL BREAKFAST MANDATE FOR INDIA:
                  The target meal is strictly BREAKFAST.
                  Suggest popular Indian breakfast dishes:
                  * South Indian: Masala Dosa with Sambar & Coconut Chutney, Idli Sambar, Medu Vada, Upma, Uttapam.
                  * North Indian: Aloo Paratha with Curd & Pickle, Paneer Paratha, Poha with Peanuts & Sev, Puri Bhaji, Chole Bhature.
                  * Eggs: Masala Omelette with Pav, Egg Bhurji with Paratha.
                  STRICT PROHIBITION: DO NOT suggest Dal Makhani, Rajma Chawal, Butter Chicken, Biryani, or dinner gravies for breakfast.
            """.trimIndent()

            c.contains("bangladesh") -> """
                - CRITICAL BREAKFAST MANDATE FOR BANGLADESH:
                  Suggest authentic Bengali breakfast: Paratha with Dim Bhaji (Egg Fry), Bhuna Khichuri with Egg, Roti with Dal or Shobji, Luchi with Alur Dom, Halwa Paratha.
                  STRICT PROHIBITION: DO NOT suggest heavy dinner or lunch curries.
            """.trimIndent()

            c.contains("uae") || c.contains("saudi") || c.contains("emirates") -> """
                - CRITICAL BREAKFAST MANDATE FOR ARAB / MIDDLE EAST:
                  Suggest traditional Middle Eastern breakfast: Shakshuka with Pita, Foul Mudammas with Olive Oil & Khubz, Labneh with Za'atar & Warm Flatbread, Falafel with Tahini & Fresh Veggies, Halloumi & Olive Plate with Khubz.
                  STRICT PROHIBITION: DO NOT suggest Mandi, Kabsa, or dinner grills.
            """.trimIndent()

            else -> """
                - CRITICAL BREAKFAST MANDATE:
                  The target meal is strictly BREAKFAST.
                  Suggest popular breakfast classics:
                  * Egg Dishes: Classic Cheese Omelette with Toast & Hash Browns, Eggs Benedict with Hollandaise, Scrambled Eggs with Sourdough, Avocado Toast with Poached Eggs.
                  * Sweet Breakfast: Fluffy Pancakes with Maple Syrup & Butter, Golden French Toast with Berries, Belgian Waffles.
                  * Healthy / Cereals: Warm Oatmeal with Honey & Nuts, Breakfast Granola with Greek Yogurt & Berries, Breakfast Burrito.
                  STRICT PROHIBITION: DO NOT suggest lunch or dinner mains like Pasta, Steak, Burgers, Rice, or dinner stews.
            """.trimIndent()
        }
    }

    fun getNormalDayGuidance(country: String): String {
        val c = country.lowercase()
        return when {
            c.contains("pakistan") -> """
                - CRITICAL (NORMAL DAY MEAL in Pakistan - Authentic Ghar Ka Khana):
                  The user wants comforting, everyday Pakistani homestyle meals.
                  Rotate dynamically among authentic daily household staples:
                  1. DAAL ROTI: Tarka Daal with Roti, Dhaba Chana Daal Tarka with Roti, Moong Masoor Daal Fry with Roti, Bhuni Daal Mash with green chillies & Roti, Daal Palak with Roti.
                  2. SABZI ROTI: Bhindi Masala Fry with Roti, Aloo Gobi Masala with Roti, Aloo Matar Salan with Roti, Mix Sabzi (Aloo, Gajar, Matar, Methi) with Roti, Baingan ka Bharta with Roti, Palak Aloo with Roti, Karela Pyaz Fry with Roti, Shimla Mirch Aloo with Roti, Tori ki Sabzi with Roti, Lauki Chana Daal with Roti.
                  3. DAAL CHAWAL: Yellow Tarka Daal Chawal with Kachumber Salad & Achar, Zeera Chawal with Daal.
                  4. DAHI PHULKI / DAHI SPECIALTIES: Besan Dahi Phulki with warm Roti/Paratha, Dahi Baray with Chutney, Dahi Boondi with Roti.
                  5. KADHI CHAWAL: Tangy Besan Kadhi Pakora with fragrant Basmati Rice or warm Roti.
                  6. HOMESTYLE SALAN & KHICHDI: Classic Aloo Gosht Shorba with Roti, Aloo Chicken Salan with Roti, Anda Curry / Anda Ghotala with Roti, Lobia ka Salan with Roti, Moong Daal Khichdi with Dahi & Papad, Shami Kabab with Daal Chawal.
                  STRICT PROHIBITION: DO NOT suggest heavy weekend feasts, Biryani, Pulao, Paye, Nihari, Fast Food (Burgers, Pizza, Fries), or BBQ on a NORMAL day.
            """.trimIndent()

            c.contains("india") -> """
                - CRITICAL (NORMAL DAY MEAL in India - Everyday Homestyle):
                  Suggest everyday home cooking staples:
                  * Dal Roti (Dal Tadka, Dal Fry, Moong Dal with Phulka Roti)
                  * Sabzi Roti (Aloo Gobi, Bhindi Masala, Aloo Matar, Baingan Bharta with Roti)
                  * Dal Chawal (Toor Dal Rice with Ghee, Rajma Chawal, Kadhi Chawal with Pakora)
                  * Everyday Rice & South Indian: Khichdi with Dahi, Sambar Rice, Curd Rice, Plain Dosa/Idli Sambar.
                  STRICT PROHIBITION: DO NOT suggest Biryani, Pulao, Butter Chicken, Paneer Tikka, Chole Bhature, Pizza, Burgers, or Tandoori BBQ on a NORMAL day.
            """.trimIndent()

            c.contains("bangladesh") -> """
                - CRITICAL (NORMAL DAY MEAL in Bangladesh - Everyday Bengali Homestyle):
                  Suggest everyday home meals: Daal Bhaat (Lentils & Steamed Rice), Shobji Torkari (Vegetable Curry) with Roti/Rice, Dim Bhuna (Egg Curry) with Rice, Macher Patla Jhol (Light Fish Curry with Potatoes), Bhuna Khichuri, Aloo Bhorta & Daal.
                  STRICT PROHIBITION: DO NOT suggest Kacchi Biryani, Morog Polao, Beef Tehari, Shorshe Ilish, Roast Chicken, Fast Food, or BBQ on a NORMAL day.
            """.trimIndent()

            c.contains("uae") || c.contains("saudi") || c.contains("emirates") -> """
                - CRITICAL (NORMAL DAY MEAL in Middle East / Arab - Everyday Homestyle):
                  Suggest everyday home staples: Mujadara (Lentils & Rice with Fried Onions), Foul Mudammas with Pita/Khubz, Shorba Adas (Lentil Soup with Rice), Fasolia (White Bean Stew) with Rice, Bamya (Homestyle Okra Stew) with Vermicelli Rice, Kousa Mahshi, Falafel Platter with Hummus & Salad.
                  STRICT PROHIBITION: DO NOT suggest Mandi, Kabsa, Machboos, Ouzi, Mixed Grill BBQ, Shawarma platters, Burgers, or Pizza on a NORMAL day.
            """.trimIndent()

            c.contains("china") -> """
                - CRITICAL (NORMAL DAY MEAL in China - Everyday Homestyle Cooking):
                  Suggest everyday home staples: Tomato & Scrambled Egg Stir-Fry with Steamed Rice, Homestyle Stir-Fried Bok Choy / Greens with Garlic, Mapo Tofu with Rice, Egg Fried Rice, Wonton Noodle Soup, Vegetable Chow Mein, Congee with Century Egg.
                  STRICT PROHIBITION: DO NOT suggest Peking Roast Duck, Szechuan Hot Pot Feast, Dim Sum Banquet, Sweet & Sour Pork, or BBQ Char Siu on a NORMAL day.
            """.trimIndent()

            c.contains("italy") -> """
                - CRITICAL (NORMAL DAY MEAL in Italy - Everyday Homestyle Cucina):
                  Suggest simple everyday homestyle meals: Spaghetti al Pomodoro, Penne all'Arrabbiata, Spaghetti Aglio Olio e Peperoncino, Minestrone Soup with Crusty Bread, Risotto Bianco with Parmesan, Vegetable Frittata with Greens, Caprese Salad.
                  STRICT PROHIBITION: DO NOT suggest Lasagna Bolognese al Forno, Osso Buco, Bistecca alla Fiorentina, Seafood Risotto, Wood-fired Pizza, or Calzone on a NORMAL day.
            """.trimIndent()

            c.contains("mexic") -> """
                - CRITICAL (NORMAL DAY MEAL in Mexico - Comida Casera):
                  Suggest everyday homestyle meals: Frijoles de la Olla con Arroz y Tortillas, Caldo de Pollo con Verduras, Enchiladas Verdes Caseras, Quesadillas de Queso con Pico de Gallo, Chiles Rellenos Caseros, Huevos Rancheros.
                  STRICT PROHIBITION: DO NOT suggest Birria de Res, Carne Asada BBQ, Pozole Rojo, Carnitas Feast, or Tacos al Pastor on a NORMAL day.
            """.trimIndent()

            c.contains("turkey") -> """
                - CRITICAL (NORMAL DAY MEAL in Turkey - Ev Yemekleri):
                  Suggest everyday home meals: Mercimek Çorbası (Red Lentil Soup) with Bread, Kuru Fasulye with Pilav, Menemen with Crusty Bread, Zeytinyağlı Taze Fasulye with Pilav, Sebze Yemekleri.
                  STRICT PROHIBITION: DO NOT suggest Iskender Kebab, Adana Kebab BBQ, Lamb Kuzu Tandır, Lahmacun, or Doner Feast on a NORMAL day.
            """.trimIndent()

            c.contains("afghan") -> """
                - CRITICAL (NORMAL DAY MEAL in Afghanistan - Everyday Homestyle):
                  Suggest everyday home cooking: Lubya (Kidney Bean Stew) with Naan, Bonjan Borani (Eggplant in Garlicky Yogurt) with Naan, Daal Nakhod with Naan, Chalow with Sabzi, Shorma.
                  STRICT PROHIBITION: DO NOT suggest Kabuli Pulao, Shinwari Karahi, Chapli Kababs, Mantu Dumplings, or Lamb Tikka BBQ on a NORMAL day.
            """.trimIndent()

            else -> """
                - CRITICAL (NORMAL DAY MEAL in $country - Everyday Comfort Food):
                  Suggest comforting everyday homestyle staples: Simple Sandwiches (Grilled Cheese, Turkey & Avocado, Tuna Salad), Homestyle Soups (Chicken Noodle, Tomato Soup with Bread), Simple Pasta (Spaghetti Marinara, Macaroni and Cheese), Grilled Chicken Breast with Steamed Veggies, Veggie Stir-Fry with Rice.
                  STRICT PROHIBITION: DO NOT suggest heavy feasts, BBQ Ribs, Steaks, Gourmet Burgers, Artisan Pizza, or Fried Chicken on a NORMAL day.
            """.trimIndent()
        }
    }

    fun getSpecialDayGuidance(country: String): String {
        val c = country.lowercase()
        return when {
            c.contains("pakistan") -> """
                - SPECIAL DAY (FEASTS, FAST FOOD & BBQ in Pakistan):
                  The user wants an indulgent weekend, dawat, fast food, or BBQ treat!
                  Suggest rich specialties from these categories:
                  1. BIRYANI: Sindhi Dum Biryani, Karachi Student Beef Biryani, Hyderabadi Dum Biryani, Chicken Tikka Biryani, Degi Matka Biryani.
                  2. PULAO / PILAO: Mutton Yakhni Pulao, Bannu Beef Pulao, Afghani Kabuli Pulao, Degi Yakhni Pulao, Zafrani Pulao.
                  3. PAYE (PAPYE): Mutton Siri Paye with Roghani Naan, Beef Bong Paye.
                  4. NIHARI: Shahi Nalli Nihari with Roghani Naan, Beef Shank Nihari, Maghaz Nihari.
                  5. FAST FOOD: Crispy Zinger Burger with Masala Fries, Gourmet Beef Smash Burger, Chicken Tikka Pizza, Fajita Pizza, Loaded Fries, Crispy Chicken Broast with Garlic Sauce.
                  6. BBQ & GRILLS: Chicken Tikka BBQ with Paratha, Beef Seekh Kabab, Bihari Boti, Malai Boti, Balochi Sajji, Peshawari Chapli Kabab, Chargha, Mutton Chops BBQ.
                  7. DAWAT FEASTS: Chinioti Mutton Kunna, Shahi Danedaar Korma, Peshawari Shinwari Karahi, White Mutton Karahi, Koyla Karahi, Shahi Haleem.
            """.trimIndent()

            c.contains("india") -> """
                - SPECIAL DAY (FEASTS, FAST FOOD & BBQ in India):
                  Suggest rich weekend specialties: Hyderabadi Dum Biryani, Lucknowi Biryani, Kashmiri Pulao, Butter Chicken, Shahi Paneer, Paneer Tikka, Tandoori BBQ Chicken, Chole Bhature, Pav Bhaji, Chicken Tikka Pizza, Gourmet Burgers, Kathi Rolls.
            """.trimIndent()

            c.contains("bangladesh") -> """
                - SPECIAL DAY (FEASTS & DAWAT in Bangladesh):
                  Suggest rich festive specialties: Dhaka Kacchi Biryani with Borhani, Morog Polao (Wedding Chicken Pulao), Beef Tehari, Shorshe Ilish (Hilsa in Mustard Gravy), Biye Barir Roast Chicken with Polao, Tandoori BBQ, Fast Food.
            """.trimIndent()

            c.contains("uae") || c.contains("saudi") || c.contains("emirates") -> """
                - SPECIAL DAY (FEASTS, FAST FOOD & BBQ in Middle East / Arab):
                  Suggest celebratory specialties: Mutton Mandi with Fragrant Rice, Chicken Kabsa with Raisins & Nuts, Lamb Machboos, Ouzi (Whole Spiced Lamb), Mixed Grill BBQ (Shish Taouk, Kofta, Lamb Chops), Shawarma Platters, Gourmet Burgers & Pizza.
            """.trimIndent()

            c.contains("china") -> """
                - SPECIAL DAY (FEASTS & BANQUETS in China):
                  Suggest celebratory specialties: Peking Roast Duck with Pancakes, Szechuan Spicy Hot Pot Feast, Dim Sum Banquet, Crispy Sweet and Sour Pork, Kung Pao Banquet, BBQ Char Siu Pork.
            """.trimIndent()

            c.contains("italy") -> """
                - SPECIAL DAY (FEASTS & SPECIALTIES in Italy):
                  Suggest rich Italian specialties: Lasagna Bolognese al Forno, Osso Buco alla Milanese with Saffron Risotto, Bistecca alla Fiorentina (T-Bone Steak), Seafood Risotto (Frutti di Mare), Neapolitan Wood-fired Pizza Margherita, Calzone.
            """.trimIndent()

            c.contains("mexic") -> """
                - SPECIAL DAY (FEASTS & BBQ in Mexico):
                  Suggest celebratory Mexican specialties: Birria de Res with Consomé, Carne Asada BBQ Platter, Pozole Rojo, Carnitas Michoacanas Feast, Tacos al Pastor, Tamales Festivos.
            """.trimIndent()

            c.contains("turkey") -> """
                - SPECIAL DAY (FEASTS & BBQ in Turkey):
                  Suggest rich Turkish specialties: Iskender Kebab with Browned Butter & Yogurt, Adana Kebab BBQ Skewers, Lamb Kuzu Tandır, Wood-fired Lahmacun & Pide, Doner Kebab Feast Platter.
            """.trimIndent()

            c.contains("afghan") -> """
                - SPECIAL DAY (FEASTS & BBQ in Afghanistan):
                  Suggest rich Afghan specialties: Kabuli Pulao with Raisins & Sweet Carrots, Shinwari Karahi, Chapli Kabab BBQ, Mantu (Steamed Dumplings with Meat & Chana Lentils), Afghan Lamb Tikka BBQ.
            """.trimIndent()

            else -> """
                - SPECIAL DAY (FEASTS, FAST FOOD & BBQ in $country):
                  Suggest rich celebratory treats: Texas Smoked BBQ Brisket, Barbecue Baby Back Ribs, Prime Ribeye Steak with Garlic Butter, Gourmet Bacon Cheeseburgers & Onion Rings, Deep Dish / Wood-fired Pizza, Southern Fried Chicken Feast, Sunday Roast Beef with Yorkshire Pudding.
            """.trimIndent()
        }
    }

    suspend fun decideDailyMeal(
        city: String,
        country: String = "Pakistan",
        mealType: MealType,
        isSpecial: Boolean,
        isSweetDish: Boolean,
        wantToCookList: List<String>,
        dontWantToCookList: List<String>,
        past30DaysHistory: List<String>
    ): MealDeciderResponse {
        return try {
            val targetCity = city.ifBlank { "Karachi" }
            val targetCountry = country.ifBlank { "Pakistan" }

            val normalMealRules = if (mealType == MealType.BREAKFAST) {
                getBreakfastGuidance(targetCountry)
            } else if (!isSpecial && !isSweetDish) {
                getNormalDayGuidance(targetCountry)
            } else if (isSpecial) {
                getSpecialDayGuidance(targetCountry)
            } else {
                "- isSweetDish: true -> MUST suggest a dessert or sweet treat (e.g. Kheer, Gajar Halwa, Gulab Jamun, Zarda, Sheer Khurma, Tiramisu, Churros, Baklava)."
            }

            val prompt = """
                You are What2Eat AI, an elite kitchen decider powered by Groq Cloud.
                Suggest an appetizing meal for a user in $targetCity, $targetCountry.
                
                Rules:
                - Meal Type: ${mealType.displayName}
                - isSweetDish: $isSweetDish
                - isSpecial: $isSpecial
                $normalMealRules
                - User Preferences (Prioritize if provided): ${if (wantToCookList.isNotEmpty()) wantToCookList.joinToString(", ") else "None"}
                - Strictly EXCLUDE anything in dontWantToCookList: ${if (dontWantToCookList.isNotEmpty()) dontWantToCookList.joinToString(", ") else "None"}
                - Strictly EXCLUDE past 30 days cooking history (do NOT repeat): ${if (past30DaysHistory.isNotEmpty()) past30DaysHistory.joinToString(", ") else "None"}
                
                Return JSON strictly matching:
                {
                  "dishName": "Dish Name",
                  "description": "Appetizing description of the dish and why it is great for this meal.",
                  "prepTimeMinutes": 30,
                  "cuisine": "Pakistani"
                }
            """.trimIndent()

            val jsonString = callGroq(prompt)
            val obj = JsonParser.parseString(jsonString).asJsonObject

            MealDeciderResponse(
                dishName = obj.get("dishName")?.asString.orEmpty().ifBlank { "Special Dish" },
                description = obj.get("description")?.asString.orEmpty(),
                prepTimeMinutes = obj.get("prepTimeMinutes")?.asInt ?: 30,
                cuisine = obj.get("cuisine")?.asString.orEmpty().ifBlank { "Pakistani" }
            )
        } catch (e: Exception) {
            Log.e(TAG, "decideDailyMeal failed, using fallback", e)
            val fallbackDishes = LocalFallbackService.getFallbackSuggestions(
                country = country,
                mealType = mealType,
                dayType = if (isSpecial) DayType.SPECIAL else DayType.NORMAL,
                dishCount = 1,
                sweetDishCount = if (isSweetDish) 1 else 0,
                excludedDishes = past30DaysHistory,
                wantToCook = wantToCookList,
                dontWantToCook = dontWantToCookList
            )
            val dish = fallbackDishes.firstOrNull()
            MealDeciderResponse(
                dishName = dish?.name ?: "Chicken Karahi",
                description = dish?.chefTip ?: "Delicious homestyle recipe prepared with aromatic spices.",
                prepTimeMinutes = dish?.prepTimeMinutes ?: 30,
                cuisine = dish?.cuisine?.displayName ?: "Pakistani"
            )
        }
    }

    // -------------------------------------------------------------
    // 2. Pantry Ingredient Matcher
    // -------------------------------------------------------------
    /**
     * Pantry Ingredient Matcher:
     * - Inputs: availableIngredients (e.g. ["Fish", "Tomatoes", "Potatoes"]), dontWantToCookList
     * - Return JSON: {"dishName": "...", "description": "...", "usedIngredients": [...], "prepTimeMinutes": 30}
     */
    suspend fun matchPantryIngredients(
        availableIngredients: List<String>,
        dontWantToCookList: List<String> = emptyList(),
        city: String = "Karachi",
        country: String = "Pakistan"
    ): PantryMatcherResponse {
        return try {
            val ingredientsStr = availableIngredients.joinToString(", ")
            val prompt = """
                You are What2Eat AI, an intelligent kitchen pantry matcher powered by Groq Cloud.
                The user has the following available ingredients in their pantry: [$ingredientsStr]
                City: $city, Country: $country.
                Strictly EXCLUDE anything in dontWantToCookList: ${if (dontWantToCookList.isNotEmpty()) dontWantToCookList.joinToString(", ") else "None"}
                
                Suggest the most delicious dish that uses as many of the available ingredients as possible.
                
                Return JSON strictly matching:
                {
                  "dishName": "Dish Name",
                  "description": "Short appetizing description explaining how the ingredients are used.",
                  "usedIngredients": ["Ingredient1", "Ingredient2"],
                  "prepTimeMinutes": 30
                }
            """.trimIndent()

            val jsonString = callGroq(prompt)
            val obj = JsonParser.parseString(jsonString).asJsonObject

            val usedArray = obj.getAsJsonArray("usedIngredients")?.mapNotNull { it.asString } ?: availableIngredients

            PantryMatcherResponse(
                dishName = obj.get("dishName")?.asString.orEmpty().ifBlank { "Pantry Recipe" },
                description = obj.get("description")?.asString.orEmpty(),
                usedIngredients = usedArray,
                prepTimeMinutes = obj.get("prepTimeMinutes")?.asInt ?: 30
            )
        } catch (e: Exception) {
            Log.e(TAG, "matchPantryIngredients failed, using fallback", e)
            val fallback = LocalFallbackService.getFallbackSuggestions(
                country = country,
                mealType = MealType.DINNER,
                dayType = DayType.NORMAL,
                dishCount = 1,
                sweetDishCount = 0,
                excludedDishes = emptyList(),
                wantToCook = availableIngredients,
                dontWantToCook = dontWantToCookList
            ).firstOrNull()

            PantryMatcherResponse(
                dishName = fallback?.name ?: "Homestyle Stir Fry",
                description = fallback?.chefTip ?: "Crafted with available pantry staples.",
                usedIngredients = availableIngredients,
                prepTimeMinutes = fallback?.prepTimeMinutes ?: 30
            )
        }
    }

    // -------------------------------------------------------------
    // 3. Full Recipe Generator (with Firestore Caching)
    // -------------------------------------------------------------
    /**
     * Full Recipe Generator (with Firestore Caching):
     * - Inputs: dishName, servings, cuisine
     * - First check Firestore /recipes/{dishName}. If cached, return it.
     * - If not cached: Call Groq to generate full recipe with ingredients (name, quantity, unit), steps (array of step strings), and chefTip.
     *   Save result to Firestore for future users.
     */
    suspend fun generateFullRecipe(
        dishName: String,
        servings: Int = 4,
        cuisine: String = "Pakistani",
        country: String = "Pakistan"
    ): FullRecipeResponse {
        // 1. Check Firestore /recipes/{dishName} cache
        try {
            val cachedData = FirebaseService.getCachedRecipe(dishName)
            if (cachedData != null) {
                Log.d(TAG, "Found cached recipe in Firestore for: $dishName")
                val ingredientsRaw = cachedData["ingredients"] as? List<*>
                val parsedIngredients = ingredientsRaw?.mapNotNull { item ->
                    if (item is Map<*, *>) {
                        RecipeIngredient(
                            name = item["name"]?.toString().orEmpty(),
                            quantity = item["quantity"]?.toString().orEmpty(),
                            unit = item["unit"]?.toString().orEmpty()
                        )
                    } else if (item is String) {
                        RecipeIngredient(name = item, quantity = "As needed", unit = "")
                    } else null
                } ?: emptyList()

                @Suppress("UNCHECKED_CAST")
                val stepsRaw = (cachedData["steps"] as? List<String>)
                    ?: (cachedData["recipeSteps"] as? List<String>)
                    ?: emptyList()

                return FullRecipeResponse(
                    dishName = cachedData["dishName"]?.toString() ?: dishName,
                    servings = (cachedData["servings"] as? Number)?.toInt() ?: servings,
                    cuisine = cachedData["cuisine"]?.toString() ?: cuisine,
                    ingredients = parsedIngredients,
                    steps = stepsRaw,
                    chefTip = cachedData["chefTip"]?.toString().orEmpty()
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed reading Firestore recipe cache for $dishName", e)
        }

        // 2. Call Groq Cloud API
        return try {
            val prompt = """
                You are What2Eat AI, an elite international chef powered by Groq Cloud.
                Generate a full, authentic, and detailed recipe for: "$dishName" ($cuisine cuisine, country context: $country, servings: $servings).
                
                CRITICAL LANGUAGE REQUIREMENT:
                - All dish name, ingredients, cooking steps, and chefTip MUST strictly be generated in English language.
                
                Return JSON strictly matching:
                {
                  "dishName": "$dishName",
                  "servings": $servings,
                  "cuisine": "$cuisine",
                  "ingredients": [
                    {
                      "name": "Ingredient Name",
                      "quantity": "Quantity number or amount",
                      "unit": "unit (kg, g, tbsp, tsp, cups, piece, to taste)"
                    }
                  ],
                  "steps": [
                    "Step 1: ...",
                    "Step 2: ...",
                    "Step 3: ..."
                  ],
                  "chefTip": "Secret chef technique for best taste and presentation."
                }
            """.trimIndent()

            val jsonString = callGroq(prompt)
            val obj = JsonParser.parseString(jsonString).asJsonObject

            val ingredientsList = mutableListOf<RecipeIngredient>()
            obj.getAsJsonArray("ingredients")?.forEach { elem ->
                if (elem.isJsonObject) {
                    val ingObj = elem.asJsonObject
                    ingredientsList.add(
                        RecipeIngredient(
                            name = ingObj.get("name")?.asString.orEmpty(),
                            quantity = ingObj.get("quantity")?.asString.orEmpty(),
                            unit = ingObj.get("unit")?.asString.orEmpty()
                        )
                    )
                }
            }

            val stepsList = obj.getAsJsonArray("steps")?.mapNotNull { it.asString }
                ?: obj.getAsJsonArray("recipeSteps")?.mapNotNull { it.asString }
                ?: emptyList()

            val recipeResponse = FullRecipeResponse(
                dishName = obj.get("dishName")?.asString.orEmpty().ifBlank { dishName },
                servings = obj.get("servings")?.asInt ?: servings,
                cuisine = obj.get("cuisine")?.asString.orEmpty().ifBlank { cuisine },
                ingredients = ingredientsList,
                steps = stepsList,
                chefTip = obj.get("chefTip")?.asString.orEmpty()
            )

            // 3. Cache to Firestore /recipes/{dishName} for future users
            try {
                val cacheMap = mapOf(
                    "dishName" to recipeResponse.dishName,
                    "servings" to recipeResponse.servings,
                    "cuisine" to recipeResponse.cuisine,
                    "ingredients" to recipeResponse.ingredients.map { mapOf("name" to it.name, "quantity" to it.quantity, "unit" to it.unit) },
                    "steps" to recipeResponse.steps,
                    "chefTip" to recipeResponse.chefTip,
                    "createdAt" to System.currentTimeMillis()
                )
                FirebaseService.cacheRecipe(dishName, cacheMap)
            } catch (e: Exception) {
                Log.w(TAG, "Failed caching recipe to Firestore", e)
            }

            if (recipeResponse.steps.isNotEmpty()) {
                AiStatusService.reportSuccess("Groq AI (LLaMA 3.3 70B)", "Recipe: $dishName")
            }

            recipeResponse
        } catch (e: Exception) {
            Log.e(TAG, "generateFullRecipe failed, using fallback recipe", e)
            AiStatusService.reportOffline("Recipe AI offline, using local fallback")
            val fallback = LocalFallbackService.getFallbackRecipe(dishName, cuisine, country)
            FullRecipeResponse(
                dishName = fallback.name,
                servings = fallback.servings,
                cuisine = fallback.cuisine.displayName,
                ingredients = fallback.ingredients.map { RecipeIngredient(name = it, quantity = "As needed", unit = "") },
                steps = fallback.recipeSteps,
                chefTip = fallback.chefTip
            )
        }
    }

    // -------------------------------------------------------------
    // 4. Dine-Out & Delivery Discovery
    // -------------------------------------------------------------
    /**
     * Dine-Out & Delivery Discovery:
     * - Inputs: city, cuisinePreference, mealTime
     * - Return JSON: {"cuisineType": "...", "suggestedDish": "...", "searchKeywordForMaps": "..."}
     */
    suspend fun discoverDineOut(
        city: String,
        cuisinePreference: String,
        mealTime: String
    ): DineOutDiscoveryResponse {
        return try {
            val targetCity = city.ifBlank { "Karachi" }
            val prompt = """
                You are What2Eat AI, an intelligent dining discovery assistant powered by Groq Cloud.
                City: $targetCity
                Cuisine Preference: $cuisinePreference
                Meal Time: $mealTime
                
                Suggest a standout dish to order or dine out for, along with an effective search query for Google Maps to find the best local restaurant in $targetCity.
                
                Return JSON strictly matching:
                {
                  "cuisineType": "$cuisinePreference",
                  "suggestedDish": "Signature Dish Name",
                  "searchKeywordForMaps": "Best search keyword for Google Maps in $targetCity"
                }
            """.trimIndent()

            val jsonString = callGroq(prompt)
            val obj = JsonParser.parseString(jsonString).asJsonObject

            DineOutDiscoveryResponse(
                cuisineType = obj.get("cuisineType")?.asString.orEmpty().ifBlank { cuisinePreference },
                suggestedDish = obj.get("suggestedDish")?.asString.orEmpty().ifBlank { "Specialty Dish" },
                searchKeywordForMaps = obj.get("searchKeywordForMaps")?.asString.orEmpty().ifBlank { "$cuisinePreference restaurant in $targetCity" }
            )
        } catch (e: Exception) {
            Log.e(TAG, "discoverDineOut failed, using fallback", e)
            DineOutDiscoveryResponse(
                cuisineType = cuisinePreference,
                suggestedDish = if (cuisinePreference.contains("Desi", true)) "Chicken Karahi" else "Pizza",
                searchKeywordForMaps = "$cuisinePreference restaurant in $city"
            )
        }
    }

    // -------------------------------------------------------------
    // 5. Backwards Compatibility: suggestDishes, generateRecipe, preferences
    // -------------------------------------------------------------
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
            val targetCountry = country.ifBlank { "Pakistan" }
            val targetCity = city.ifBlank { "Karachi" }
            val totalDishes = dishCount + sweetDishCount

            // Critical fix: mealType must be checked FIRST before dayType
            // otherwise breakfast always gets lunch/dinner instructions
            val dayTypeInstructions = when (mealType) {
                MealType.BREAKFAST, MealType.SEHRI -> getBreakfastGuidance(targetCountry)
                MealType.IFTARI -> """
                    - CRITICAL IFTARI MANDATE:
                      The target meal is IFTARI (breaking fast). Suggest authentic Iftari / Iftar items:
                      Crispy Pakoras, Dahi Bhallay, Fruit Chaat, Samosa, Dates, Jaljeera, Rooh Afza, Shami Kabab, Spring Rolls, Fruit Salad.
                      STRICT PROHIBITION: DO NOT suggest full heavy dinner or lunch gravies.
                """.trimIndent()
                else -> if (dayType == DayType.NORMAL) {
                    getNormalDayGuidance(targetCountry)
                } else {
                    getSpecialDayGuidance(targetCountry)
                }
            }

            val prompt = """
                You are What2Eat AI powered by high-speed Groq Cloud.
                Generate a list of $totalDishes personalized meal suggestions for a user in $targetCity, $targetCountry.
                
                Context:
                - Target Meal: ${mealType.displayName}
                - Day Type: ${dayType.displayName}
                - Total Savory Dishes: $dishCount
                - Total Sweet / Desserts: $sweetDishCount
                $dayTypeInstructions
                ${if (!occasion.isNullOrBlank()) "- Specific Occasion / Preference: $occasion\n- CRITICAL RULE: If a specific preference is given above (e.g. Rice, Pasta, etc.), you MUST strictly follow it and override general restrictions if needed." else ""}
                - Prioritize: ${if (wantToCook.isNotEmpty()) wantToCook.joinToString(", ") else "Appetizing favorites"}
                - Strictly Exclude (Disliked): ${if (dontWantToCook.isNotEmpty()) dontWantToCook.joinToString(", ") else "None"}
                - Strictly Exclude (Recent history): ${if (excludedDishes.isNotEmpty()) excludedDishes.joinToString(", ") else "None"}
                
                Return JSON strictly with a "dishes" array of objects:
                {
                  "dishes": [
                    {
                      "id": "dish_1",
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
                      "ingredients": ["Ingredient 1", "Ingredient 2"],
                      "recipeSteps": ["Step 1...", "Step 2..."],
                      "chefTip": "Pro tip...",
                      "tags": ["Tag1", "Tag2"],
                      "isAIGenerated": true
                    }
                  ]
                }
            """.trimIndent()

            val jsonString = callGroq(prompt)
            val parsedObj = JsonParser.parseString(jsonString).asJsonObject
            val dishesArray = if (parsedObj.has("dishes")) {
                parsedObj.getAsJsonArray("dishes")
            } else if (parsedObj.isJsonArray) {
                parsedObj.asJsonArray
            } else null

            val result = mutableListOf<Dish>()
            dishesArray?.forEachIndexed { index, elem ->
                if (elem.isJsonObject) {
                    val obj = elem.asJsonObject
                    val name = obj.get("name")?.asString.orEmpty()
                    if (name.isNotBlank()) {
                        result.add(
                            Dish(
                                id = obj.get("id")?.asString.orEmpty().ifBlank { "groq_${System.currentTimeMillis()}_$index" },
                                name = name,
                                cuisine = CuisineType.fromString(obj.get("cuisine")?.asString),
                                country = obj.get("country")?.asString ?: targetCountry,
                                mealType = MealType.fromString(obj.get("mealType")?.asString),
                                dayType = DayType.fromString(obj.get("dayType")?.asString),
                                season = obj.get("season")?.asString ?: season,
                                isSpecial = obj.get("isSpecial")?.asBoolean ?: (dayType == DayType.SPECIAL),
                                prepTimeMinutes = obj.get("prepTimeMinutes")?.asInt ?: 35,
                                difficulty = obj.get("difficulty")?.asString ?: "Medium",
                                servings = obj.get("servings")?.asInt ?: 4,
                                ingredients = obj.getAsJsonArray("ingredients")?.mapNotNull { it.asString } ?: emptyList(),
                                recipeSteps = obj.getAsJsonArray("recipeSteps")?.mapNotNull { it.asString } ?: emptyList(),
                                chefTip = obj.get("chefTip")?.asString.orEmpty(),
                                tags = obj.getAsJsonArray("tags")?.mapNotNull { it.asString } ?: emptyList(),
                                isAIGenerated = true
                            )
                        )
                    }
                }
            }

            if (result.isNotEmpty()) {
                AiStatusService.reportSuccess("Groq AI (LLaMA 3.3 70B)", "${result.size} dishes")
                result
            } else {
                AiStatusService.reportOffline("No results from AI")
                LocalFallbackService.getFallbackSuggestions(country, mealType, dayType, dishCount, sweetDishCount, excludedDishes, wantToCook, dontWantToCook)
            }
        } catch (e: Exception) {
            Log.e(TAG, "suggestDishes via Groq failed, using LocalFallbackService", e)
            AiStatusService.reportOffline(e.localizedMessage ?: "AI timeout / network error")
            LocalFallbackService.getFallbackSuggestions(country, mealType, dayType, dishCount, sweetDishCount, excludedDishes, wantToCook, dontWantToCook)
        }
    }

    suspend fun generateRecipe(dishName: String, cuisine: String, country: String): Dish? {
        return try {
            val fullRecipe = generateFullRecipe(dishName = dishName, servings = 4, cuisine = cuisine, country = country)
            val dish = Dish(
                id = "recipe_${dishName.lowercase().replace(" ", "_")}",
                name = fullRecipe.dishName,
                cuisine = CuisineType.fromString(fullRecipe.cuisine),
                country = country.ifBlank { "Pakistan" },
                mealType = MealType.DINNER,
                dayType = DayType.NORMAL,
                season = "All",
                isSpecial = false,
                prepTimeMinutes = 35,
                difficulty = "Medium",
                servings = fullRecipe.servings,
                ingredients = fullRecipe.ingredients.map { "${it.quantity} ${it.unit} ${it.name}".trim() },
                recipeSteps = fullRecipe.steps,
                chefTip = fullRecipe.chefTip,
                tags = listOf("Homemade", fullRecipe.cuisine),
                isAIGenerated = true
            )
            AiStatusService.reportSuccess("Groq AI (LLaMA 3.3 70B)", "Recipe: ${dish.name}")
            dish
        } catch (e: Exception) {
            Log.e(TAG, "generateRecipe failed", e)
            AiStatusService.reportOffline("Recipe generation failed: ${e.localizedMessage}")
            null
        }
    }

    suspend fun getKitchenPreferenceSuggestions(country: String): Pair<List<String>, List<String>> {
        return try {
            val prompt = """
                You are What2Eat AI powered by Groq Cloud.
                Generate kitchen preferences for users living in $country.
                Return JSON strictly matching:
                {
                  "wantToCook": ["Popular dish 1", "Popular dish 2", "Popular dish 3", "Popular dish 4", "Popular dish 5", "Popular dish 6", "Popular dish 7", "Popular dish 8"],
                  "dontWantToCook": ["Disliked veggie 1", "Disliked ingredient 2", "Disliked veggie 3", "Disliked ingredient 4"]
                }
            """.trimIndent()

            val jsonString = callGroq(prompt)
            val jsonObj = JsonParser.parseString(jsonString).asJsonObject
            val wantList = jsonObj.getAsJsonArray("wantToCook")?.mapNotNull { it.asString } ?: emptyList()
            val dontWantList = jsonObj.getAsJsonArray("dontWantToCook")?.mapNotNull { it.asString } ?: emptyList()

            if (wantList.isNotEmpty()) {
                Pair(wantList, dontWantList)
            } else {
                LocalFallbackService.getFallbackKitchenPreferences(country)
            }
        } catch (e: Exception) {
            Log.e(TAG, "getKitchenPreferenceSuggestions failed", e)
            LocalFallbackService.getFallbackKitchenPreferences(country)
        }
    }
}
