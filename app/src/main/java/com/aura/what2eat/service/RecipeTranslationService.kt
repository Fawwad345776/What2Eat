package com.aura.what2eat.service

import android.util.Log
import com.aura.what2eat.model.Dish
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

data class SupportedLanguage(
    val code: String,
    val displayName: String,
    val flag: String
)

val RECIPE_SUPPORTED_LANGUAGES = listOf(
    SupportedLanguage("en", "English", "🇬🇧"),
    SupportedLanguage("roman", "Roman Urdu / Hindi", "🇵🇰"),
    SupportedLanguage("ur", "اردو (Urdu)", "🇵🇰"),
    SupportedLanguage("hi", "हिंदी (Hindi)", "🇮🇳"),
    SupportedLanguage("ar", "العربية (Arabic)", "🇸🇦"),
    SupportedLanguage("es", "Español (Spanish)", "🇪🇸"),
    SupportedLanguage("bn", "বাংলা (Bengali)", "🇧🇩")
)

data class TranslatedRecipe(
    val languageCode: String,
    val dishName: String,
    val ingredients: List<String>,
    val steps: List<String>,
    val chefTip: String
)

object RecipeTranslationService {

    private const val TAG = "RecipeTranslationService"
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    private val gson = Gson()

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    // Fast in-memory cache: (DishKey, LanguageCode) -> TranslatedRecipe
    private val translationCache = ConcurrentHashMap<String, TranslatedRecipe>()

    private fun getCacheKey(dishName: String, langCode: String): String {
        return "${dishName.trim().lowercase()}_$langCode"
    }

    /**
     * Translates a recipe into the target language.
     * If English is requested, returns the original dish directly.
     * Uses in-memory cache for instant switching.
     */
    suspend fun translateRecipe(dish: Dish, targetLang: SupportedLanguage): TranslatedRecipe = withContext(Dispatchers.IO) {
        if (targetLang.code == "en") {
            return@withContext TranslatedRecipe(
                languageCode = "en",
                dishName = dish.name,
                ingredients = dish.ingredients,
                steps = dish.recipeSteps,
                chefTip = dish.chefTip
            )
        }

        val cacheKey = getCacheKey(dish.name, targetLang.code)
        val cached = translationCache[cacheKey]
        if (cached != null) {
            return@withContext cached
        }

        // Build prompt for AI translation
        val prompt = buildTranslationPrompt(dish, targetLang)

        // Try Gemini AI first, fallback to Groq AI if needed
        val translated = tryGeminiTranslation(prompt, targetLang.code)
            ?: tryGroqTranslation(prompt, targetLang.code)
            ?: fallbackTranslation(dish, targetLang.code)

        translationCache[cacheKey] = translated
        translated
    }

    private fun buildTranslationPrompt(dish: Dish, targetLang: SupportedLanguage): String {
        val langInstruction = when (targetLang.code) {
            "roman" -> "Translate into conversational, clear Roman Urdu / Roman Hindi using English alphabet (e.g., 'Pehle pyaz ko golden brown fry karein', 'Namak aur mirch shamil karein', etc.)."
            "ur" -> "Translate into authentic, fluent Urdu in Urdu Nastaliq script."
            "hi" -> "Translate into standard conversational Hindi in Devanagari script."
            "ar" -> "Translate into clear Modern Standard Arabic."
            "es" -> "Translate into clear Spanish (Español)."
            "bn" -> "Translate into standard Bengali."
            else -> "Translate into ${targetLang.displayName}."
        }

        return """
        You are an expert culinary translator. $langInstruction
        Translate the following recipe details accurately.
        
        Original Recipe:
        Dish Name: ${dish.name}
        Ingredients:
        ${dish.ingredients.joinToString("\n") { "- $it" }}
        
        Steps:
        ${dish.recipeSteps.mapIndexed { i, s -> "${i + 1}. $s" }.joinToString("\n")}
        
        Chef Tip: ${dish.chefTip}

        You MUST respond ONLY with a valid JSON object matching this schema:
        {
            "dishName": "Translated dish name",
            "ingredients": ["Translated ingredient 1", "Translated ingredient 2"],
            "steps": ["Translated step 1", "Translated step 2"],
            "chefTip": "Translated chef tip"
        }
        """.trimIndent()
    }

    private suspend fun tryGeminiTranslation(prompt: String, langCode: String): TranslatedRecipe? {
        for (model in GeminiAIService.GEMINI_MODELS) {
            try {
                val requestUrl = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=${GeminiAIService.GEMINI_API_KEY}"
                val requestJson = JsonObject().apply {
                    val contentsArray = com.google.gson.JsonArray().apply {
                        val contentObj = JsonObject().apply {
                            val partsArray = com.google.gson.JsonArray().apply {
                                val textObj = JsonObject().apply { addProperty("text", prompt) }
                                add(textObj)
                            }
                            add("parts", partsArray)
                        }
                        add(contentObj)
                    }
                    add("contents", contentsArray)
                    val genConfig = JsonObject().apply {
                        addProperty("responseMimeType", "application/json")
                        addProperty("temperature", 0.3)
                    }
                    add("generationConfig", genConfig)
                }

                val body = requestJson.toString().toRequestBody(JSON_MEDIA_TYPE)
                val request = Request.Builder()
                    .url(requestUrl)
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string().orEmpty()
                if (response.isSuccessful && responseBody.isNotBlank()) {
                    val root = JsonParser.parseString(responseBody).asJsonObject
                    val candidates = root.getAsJsonArray("candidates")
                    if (candidates != null && !candidates.isEmpty) {
                        val firstCandidate = candidates[0].asJsonObject
                        val text = firstCandidate.getAsJsonObject("content")
                            .getAsJsonArray("parts")[0].asJsonObject
                            .get("text").asString
                        val parsed = parseTranslationJson(text, langCode)
                        if (parsed != null) return parsed
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Gemini model $model translation failed: ${e.message}")
            }
        }
        return null
    }

    private suspend fun tryGroqTranslation(prompt: String, langCode: String): TranslatedRecipe? {
        try {
            val requestJson = JsonObject().apply {
                addProperty("model", GroqAIService.GROQ_MODEL)
                addProperty("temperature", 0.3)
                val formatObj = JsonObject().apply { addProperty("type", "json_object") }
                add("response_format", formatObj)
                val messagesArray = com.google.gson.JsonArray().apply {
                    val msg = JsonObject().apply {
                        addProperty("role", "user")
                        addProperty("content", prompt)
                    }
                    add(msg)
                }
                add("messages", messagesArray)
            }

            val body = requestJson.toString().toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder()
                .url("https://api.groq.com/openai/v1/chat/completions")
                .post(body)
                .addHeader("Authorization", "Bearer ${GroqAIService.GROQ_API_KEY}")
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()
            if (response.isSuccessful && responseBody.isNotBlank()) {
                val root = JsonParser.parseString(responseBody).asJsonObject
                val choices = root.getAsJsonArray("choices")
                if (choices != null && !choices.isEmpty) {
                    val content = choices[0].asJsonObject.getAsJsonObject("message").get("content").asString
                    val parsed = parseTranslationJson(content, langCode)
                    if (parsed != null) return parsed
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Groq translation failed: ${e.message}")
        }
        return null
    }

    private fun parseTranslationJson(jsonString: String, langCode: String): TranslatedRecipe? {
        return try {
            val cleanJson = jsonString.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val obj = JsonParser.parseString(cleanJson).asJsonObject

            val dishName = obj.get("dishName")?.asString.orEmpty()
            val chefTip = obj.get("chefTip")?.asString.orEmpty()

            val ingredients = mutableListOf<String>()
            obj.getAsJsonArray("ingredients")?.forEach { el ->
                if (!el.isJsonNull) ingredients.add(el.asString)
            }

            val steps = mutableListOf<String>()
            obj.getAsJsonArray("steps")?.forEach { el ->
                if (!el.isJsonNull) steps.add(el.asString)
            }

            if (steps.isNotEmpty() || ingredients.isNotEmpty()) {
                TranslatedRecipe(
                    languageCode = langCode,
                    dishName = dishName,
                    ingredients = ingredients,
                    steps = steps,
                    chefTip = chefTip
                )
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse translated recipe json: ${e.message}", e)
            null
        }
    }

    private fun fallbackTranslation(dish: Dish, langCode: String): TranslatedRecipe {
        return TranslatedRecipe(
            languageCode = langCode,
            dishName = dish.name,
            ingredients = dish.ingredients,
            steps = dish.recipeSteps,
            chefTip = dish.chefTip
        )
    }
}
