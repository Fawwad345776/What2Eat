package com.aura.what2eat.service

import android.content.Context
import android.util.Log
import com.aura.what2eat.model.CuisineType
import com.aura.what2eat.model.DayType
import com.aura.what2eat.model.Dish
import com.aura.what2eat.model.MealType
import com.google.gson.JsonParser
import java.io.InputStreamReader

object LocalFallbackService {
    private const val TAG = "LocalFallbackService"
    private var loadedAssetDishes: List<Dish>? = null

    /**
     * Loads the 1000+ global dishes database from assets/dishes.json.
     */
    fun init(context: Context) {
        if (loadedAssetDishes != null) return
        try {
            context.assets.open("dishes.json").use { stream ->
                val reader = InputStreamReader(stream, Charsets.UTF_8)
                val jsonElement = JsonParser.parseReader(reader)
                if (jsonElement.isJsonArray) {
                    val array = jsonElement.asJsonArray
                    val list = ArrayList<Dish>(array.size())
                    for (item in array) {
                        if (!item.isJsonObject) continue
                        val obj = item.asJsonObject
                        val dish = Dish(
                            id = obj.get("id")?.asString.orEmpty(),
                            name = obj.get("name")?.asString.orEmpty(),
                            cuisine = CuisineType.fromString(obj.get("cuisine")?.asString),
                            country = obj.get("country")?.asString.orEmpty(),
                            mealType = MealType.fromString(obj.get("mealType")?.asString),
                            dayType = DayType.fromString(obj.get("dayType")?.asString),
                            season = obj.get("season")?.asString.orEmpty(),
                            isSpecial = obj.get("isSpecial")?.asBoolean ?: false,
                            prepTimeMinutes = obj.get("prepTimeMinutes")?.asInt ?: 30,
                            difficulty = obj.get("difficulty")?.asString ?: "Easy",
                            servings = obj.get("servings")?.asInt ?: 4,
                            ingredients = obj.getAsJsonArray("ingredients")?.mapNotNull { it.asString } ?: emptyList(),
                            recipeSteps = obj.getAsJsonArray("recipeSteps")?.mapNotNull { it.asString } ?: emptyList(),
                            chefTip = obj.get("chefTip")?.asString.orEmpty(),
                            tags = obj.getAsJsonArray("tags")?.mapNotNull { it.asString } ?: emptyList(),
                            isAIGenerated = false
                        )
                        list.add(dish)
                    }
                    loadedAssetDishes = list
                    Log.d(TAG, "Successfully loaded ${list.size} dishes from assets/dishes.json")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading dishes from assets/dishes.json", e)
        }
    }

    val allDishes: List<Dish>
        get() = loadedAssetDishes ?: (pakistaniDishes + indianDishes + chineseDishes + italianDishes + bangladeshiDishes)

    // -------------------------------------------------------------
    // 1. 25 PAKISTANI DISHES
    // -------------------------------------------------------------
    val pakistaniDishes = listOf(
        // Breakfast / Sehri
        Dish(
            id = "pk_1",
            name = "Halwa Puri Chana",
            cuisine = CuisineType.PAKISTANI,
            country = "Pakistan",
            mealType = MealType.BREAKFAST,
            dayType = DayType.SPECIAL,
            season = "All",
            isSpecial = true,
            prepTimeMinutes = 45,
            difficulty = "Medium",
            servings = 4,
            ingredients = listOf("Semolina (Sooji)", "Flour (Maida)", "Chickpeas (Chana)", "Potatoes", "Ghee", "Spices", "Sugar"),
            recipeSteps = listOf(
                "Boil soaked chickpeas with spices and simmer into a thick savory gravy.",
                "Roast sooji in ghee with cardamom, then add sugar syrup until aromatic and soft.",
                "Knead a pliable dough, roll into thin circles and deep fry until puffed and golden.",
                "Serve hot with spiced aloo bhujia and mixed pickles."
            ),
            chefTip = "Fry puris in very hot oil for only 5-10 seconds per side so they stay soft and light.",
            tags = listOf("Breakfast", "Weekend Special", "Lahori", "Sweet & Savory")
        ),
        Dish(
            id = "pk_2",
            name = "Anda Paratha with Karak Chai",
            cuisine = CuisineType.PAKISTANI,
            country = "Pakistan",
            mealType = MealType.BREAKFAST,
            dayType = DayType.NORMAL,
            season = "All",
            isSpecial = false,
            prepTimeMinutes = 20,
            difficulty = "Easy",
            servings = 2,
            ingredients = listOf("Whole Wheat Flour", "Eggs", "Onions", "Green Chillies", "Fresh Coriander", "Ghee", "Milk", "Black Tea"),
            recipeSteps = listOf(
                "Knead wheat flour with a touch of oil, roll and layer with ghee to create a flaky paratha.",
                "Whisk eggs with finely chopped onions, green chillies, salt, and crushed black pepper.",
                "Fry paratha on a tawa until crispy; fry seasoned omelette on medium heat.",
                "Brew black tea leaves in milk and crushed cardamom until rich golden brown."
            ),
            chefTip = "Press the edges of the paratha firmly while shallow frying to ensure crispy layers throughout.",
            tags = listOf("Breakfast", "Everyday", "Quick", "Comfort Food")
        ),
        Dish(
            id = "pk_3",
            name = "Nihari with Roghani Naan",
            cuisine = CuisineType.PAKISTANI,
            country = "Pakistan",
            mealType = MealType.BREAKFAST,
            dayType = DayType.SPECIAL,
            season = "Winter",
            isSpecial = true,
            prepTimeMinutes = 90,
            difficulty = "Hard",
            servings = 6,
            ingredients = listOf("Beef Shank (Bong)", "Nihari Masala", "Wheat Flour Slurry", "Ginger", "Green Chillies", "Lemon", "Ghee"),
            recipeSteps = listOf(
                "Sear beef shank in ghee with ginger-garlic and authentic Nihari spice blend.",
                "Slow cook meat with water on low flame until fall-apart tender.",
                "Thicken gravy with roasted wheat flour slurry and simmer until oil (tari) floats on top.",
                "Garnish generously with julienned ginger, chopped chillies, fresh cilantro, and lemon."
            ),
            chefTip = "Let the Nihari sit for 30 minutes after cooking so the tari separates beautifully.",
            tags = listOf("Breakfast", "Dinner", "Karachi", "Slow Cook", "Rich")
        ),
        Dish(
            id = "pk_4",
            name = "Khagina with Whole Wheat Paratha",
            cuisine = CuisineType.PAKISTANI,
            country = "Pakistan",
            mealType = MealType.SEHRI,
            dayType = DayType.NORMAL,
            season = "All",
            isSpecial = false,
            prepTimeMinutes = 15,
            difficulty = "Easy",
            servings = 2,
            ingredients = listOf("Eggs", "Tomatoes", "Onions", "Green Chillies", "Turmeric", "Coriander", "Cumin Seeds"),
            recipeSteps = listOf(
                "Sauté onions with cumin seeds until translucent, then add diced tomatoes and turmeric.",
                "Beat eggs and pour into the aromatic masala on low heat.",
                "Gently scramble until soft curds form without drying out.",
                "Garnish with fresh coriander and serve with crisp paratha."
            ),
            chefTip = "Turn off heat just before the eggs are fully set; residual heat will keep them moist.",
            tags = listOf("Sehri", "Breakfast", "Quick", "Protein")
        ),
        Dish(
            id = "pk_5",
            name = "Doodh Pheni & Yogurt",
            cuisine = CuisineType.PAKISTANI,
            country = "Pakistan",
            mealType = MealType.SEHRI,
            dayType = DayType.NORMAL,
            season = "Summer",
            isSpecial = false,
            prepTimeMinutes = 10,
            difficulty = "Easy",
            servings = 2,
            ingredients = listOf("Pheni (Vermicelli)", "Full Cream Milk", "Sugar", "Cardamom", "Almonds", "Fresh Yogurt"),
            recipeSteps = listOf(
                "Warm milk with sugar and crushed cardamom pods.",
                "Place delicate fried pheni in serving bowls and pour hot milk over it.",
                "Let it soak for 2 minutes and top with sliced almonds.",
                "Serve alongside chilled plain yogurt to stay hydrated during fasting."
            ),
            chefTip = "Use warm milk rather than boiling milk so the pheni retains its delicate texture.",
            tags = listOf("Sehri", "Ramadan", "Hydrating", "Light")
        ),
        Dish(
            id = "pk_6",
            name = "Mutton Siri Paye",
            cuisine = CuisineType.PAKISTANI,
            country = "Pakistan",
            mealType = MealType.BREAKFAST,
            dayType = DayType.SPECIAL,
            season = "Winter",
            isSpecial = true,
            prepTimeMinutes = 120,
            difficulty = "Hard",
            servings = 5,
            ingredients = listOf("Mutton Trotters (Paye)", "Onion Paste", "Ginger-Garlic", "Whole Spices", "Ghee", "Yogurt"),
            recipeSteps = listOf(
                "Clean trotters thoroughly and boil with whole spices to create a gelatinous stock.",
                "Bhunify onion paste and spices in ghee until deep brown and fragrant.",
                "Combine stock and trotters, simmer gently for 2 hours until thick and sticky.",
                "Serve piping hot with khameeri naan and lemon wedges."
            ),
            chefTip = "Slow-simmering on the lowest heat extracts the deepest collagen flavors.",
            tags = listOf("Winter Special", "Breakfast", "Lahori Traditional")
        ),

        // Lunch / Everyday Mains
        Dish(
            id = "pk_7",
            name = "Daal Chawal with Kachumber Salad",
            cuisine = CuisineType.PAKISTANI,
            country = "Pakistan",
            mealType = MealType.LUNCH,
            dayType = DayType.NORMAL,
            season = "All",
            isSpecial = false,
            prepTimeMinutes = 35,
            difficulty = "Easy",
            servings = 4,
            ingredients = listOf("Yellow Moong Daal", "Masoor Daal", "Basmati Rice", "Garlic", "Cumin Seeds", "Dried Red Chillies", "Ghee", "Cucumber", "Onion"),
            recipeSteps = listOf(
                "Cook moong and masoor lentils with turmeric, salt, and water until creamy.",
                "Prepare aromatic tarka with sliced garlic, whole cumin, and dried red chillies sizzled in ghee.",
                "Pour hot tarka over the lentils and cover immediately to trap the aroma.",
                "Serve over fluffy steamed basmati rice with cucumber-tomato kachumber and mango pickle."
            ),
            chefTip = "Add a pinch of hing (asafoetida) to the tarka for restaurant-level aroma.",
            tags = listOf("Everyday Lunch", "Comfort Food", "Vegetarian", "Healthy")
        ),
        Dish(
            id = "pk_8",
            name = "Bhindi Masala (Okra)",
            cuisine = CuisineType.PAKISTANI,
            country = "Pakistan",
            mealType = MealType.LUNCH,
            dayType = DayType.NORMAL,
            season = "Summer",
            isSpecial = false,
            prepTimeMinutes = 30,
            difficulty = "Easy",
            servings = 3,
            ingredients = listOf("Fresh Bhindi (Okra)", "Sliced Onions", "Tomatoes", "Cumin", "Coriander Powder", "Red Chilli Flakes", "Amchur Powder"),
            recipeSteps = listOf(
                "Wash and thoroughly dry okra before chopping to avoid sliminess.",
                "Shallow fry okra pieces in oil on medium-high until tender, then remove.",
                "In the same pan, sauté lots of sliced onions, diced tomatoes, and ground spices.",
                "Toss fried okra with the masala and steam on low heat (dum) for 5 minutes."
            ),
            chefTip = "Never cover the pan while initial frying of okra to keep it completely slime-free.",
            tags = listOf("Lunch", "Vegetarian", "Summer Sabzi", "Quick")
        ),
        Dish(
            id = "pk_9",
            name = "Aloo Gosht Shorba",
            cuisine = CuisineType.PAKISTANI,
            country = "Pakistan",
            mealType = MealType.LUNCH,
            dayType = DayType.NORMAL,
            season = "All",
            isSpecial = false,
            prepTimeMinutes = 50,
            difficulty = "Medium",
            servings = 4,
            ingredients = listOf("Mutton or Beef", "Potatoes (Aloo)", "Onions", "Yogurt", "Ginger-Garlic", "Garam Masala", "Coriander"),
            recipeSteps = listOf(
                "Brown meat pieces with ginger-garlic paste and whole spices in oil.",
                "Add fried onion paste and whisked yogurt, bhunify until oil separates.",
                "Add water and simmer until meat is 80% tender, then drop in large potato halves.",
                "Cook until potatoes are melt-in-mouth and gravy is medium-thin shorba."
            ),
            chefTip = "Keep the potatoes in large chunks so they absorb the meat broth without breaking.",
            tags = listOf("Lunch", "Dinner", "Classic Ghar Ka Khana", "Comfort")
        ),
        Dish(
            id = "pk_10",
            name = "Chicken Karahi (Street Style)",
            cuisine = CuisineType.PAKISTANI,
            country = "Pakistan",
            mealType = MealType.DINNER,
            dayType = DayType.NORMAL,
            season = "All",
            isSpecial = false,
            prepTimeMinutes = 35,
            difficulty = "Easy",
            servings = 4,
            ingredients = listOf("Chicken with Bone", "Fresh Tomatoes", "Ginger Juliennes", "Green Chillies", "Black Pepper", "Coriander Seeds", "Ghee"),
            recipeSteps = listOf(
                "Sear chicken in hot ghee with ginger-garlic paste until lightly golden.",
                "Place halved tomatoes cut-side down, cover to steam, and peel off skins.",
                "Bhunify on high flame with crushed coriander seeds, cumin, and coarse black pepper.",
                "Top with sliced green chillies and fresh ginger juliennes."
            ),
            chefTip = "Never add onions or water to authentic Shinwari / Peshawari style chicken karahi.",
            tags = listOf("Dinner", "Popular", "Spicy", "Quick")
        ),
        Dish(
            id = "pk_11",
            name = "Tadka Moong Daal with Jeera Rice",
            cuisine = CuisineType.PAKISTANI,
            country = "Pakistan",
            mealType = MealType.LUNCH,
            dayType = DayType.NORMAL,
            season = "All",
            isSpecial = false,
            prepTimeMinutes = 25,
            difficulty = "Easy",
            servings = 3,
            ingredients = listOf("Yellow Moong Daal", "Cumin", "Garlic", "Curry Leaves", "Ghee", "Rice"),
            recipeSteps = listOf(
                "Boil yellow moong daal with turmeric and salt until velvety smooth.",
                "Sizzle sliced garlic, curry leaves, and cumin in golden desi ghee.",
                "Temper the daal with the sizzling garnish and serve alongside jeera rice."
            ),
            chefTip = "Lightly dry roast the moong daal for 2 minutes before boiling for a nutty flavor.",
            tags = listOf("Lunch", "Light Meal", "Diet Friendly")
        ),
        Dish(
            id = "pk_12",
            name = "Baingan Bharta (Smoked Eggplant)",
            cuisine = CuisineType.PAKISTANI,
            country = "Pakistan",
            mealType = MealType.LUNCH,
            dayType = DayType.NORMAL,
            season = "Winter",
            isSpecial = false,
            prepTimeMinutes = 30,
            difficulty = "Easy",
            servings = 3,
            ingredients = listOf("Large Eggplant (Baingan)", "Onions", "Tomatoes", "Garlic", "Green Chillies", "Mustard Oil", "Coriander"),
            recipeSteps = listOf(
                "Roast whole eggplant directly over an open flame until skin is charred and flesh is tender.",
                "Peel off charred skin and mash the smoky flesh thoroughly.",
                "Sauté onions, garlic, and tomatoes in mustard oil, then mix in mashed eggplant.",
                "Smoke with a glowing charcoal piece and a drop of oil for 3 minutes."
            ),
            chefTip = "Slit the eggplant and insert whole garlic cloves inside before roasting for infused flavor.",
            tags = listOf("Lunch", "Smoky", "Vegetarian", "Winter")
        ),

        // Dinner / Festive / Biryani
        Dish(
            id = "pk_13",
            name = "Karachi Special Chicken Biryani",
            cuisine = CuisineType.PAKISTANI,
            country = "Pakistan",
            mealType = MealType.DINNER,
            dayType = DayType.SPECIAL,
            season = "All",
            isSpecial = true,
            prepTimeMinutes = 60,
            difficulty = "Hard",
            servings = 6,
            ingredients = listOf("Basmati Sella/Basmati Rice", "Chicken", "Potatoes (Aloo)", "Yogurt", "Plums (Aloo Bukhara)", "Biryani Spices", "Kewra Water", "Saffron/Food Color"),
            recipeSteps = listOf(
                "Marinate chicken in spiced yogurt, fried onions, dried plums, and crushed mint.",
                "Cook potatoes in spiced water and prepare a rich chicken qorma gravy.",
                "Parboil aged basmati rice to 75% doneness with whole aromatics.",
                "Layer rice over chicken qorma, drizzle saffron kewra milk, and steam (dum) for 20 minutes."
            ),
            chefTip = "Yellow food-colored soft potatoes (Aloo) are the heart and soul of authentic Karachi Biryani.",
            tags = listOf("Dinner", "Biryani", "Festive", "Weekend", "Crowd Pleaser")
        ),
        Dish(
            id = "pk_14",
            name = "Mutton Yakhni Pulao with Shami Kabab",
            cuisine = CuisineType.PAKISTANI,
            country = "Pakistan",
            mealType = MealType.DINNER,
            dayType = DayType.SPECIAL,
            season = "Winter",
            isSpecial = true,
            prepTimeMinutes = 75,
            difficulty = "Medium",
            servings = 6,
            ingredients = listOf("Mutton Pieces", "Basmati Rice", "Fennel Seeds (Saunf)", "Coriander Seeds", "Whole Garam Masala", "Onions", "Shami Kababs"),
            recipeSteps = listOf(
                "Make aromatic yakhni (bone broth) by simmering mutton with a spice bouquet (potli).",
                "Lightly brown sliced onions in ghee, add ginger-garlic and strained mutton pieces.",
                "Pour in yakhni broth, bring to boil, and add soaked basmati rice.",
                "Simmer until water evaporates, then dum on low flame for 15 minutes. Serve with fried shami kababs."
            ),
            chefTip = "Do not over-brown the onions if you want an authentic light-golden translucent pulao.",
            tags = listOf("Dinner", "Pulao", "Traditional", "Royal")
        ),
        Dish(
            id = "pk_15",
            name = "Beef Shahi Haleem (Daleem)",
            cuisine = CuisineType.PAKISTANI,
            country = "Pakistan",
            mealType = MealType.DINNER,
            dayType = DayType.SPECIAL,
            season = "All",
            isSpecial = true,
            prepTimeMinutes = 120,
            difficulty = "Hard",
            servings = 8,
            ingredients = listOf("Shredded Beef", "Wheat Grains (Gandum)", "Barley (Jow)", "Mixed Lentils (Daals)", "Ghee Tarka", "Fried Onions", "Chaat Masala", "Mint"),
            recipeSteps = listOf(
                "Slow cook cracked wheat, barley, and mixed lentils overnight until completely broken down.",
                "Cook beef korma with aromatic spices until shreds effortlessly.",
                "Combine grains and meat, blend with wooden masher (ghota) to achieve a silky stringy texture.",
                "Finish with hot desi ghee tarka, crispy fried onions, chaat masala, and mint."
            ),
            chefTip = "Traditional hand-mashing with a wooden ghota creates the signature fibrous texture that blenders destroy.",
            tags = listOf("Festive", "Special Occasion", "Muharram", "Slow Cook")
        ),
        Dish(
            id = "pk_16",
            name = "Peshawari Chapli Kabab",
            cuisine = CuisineType.PAKISTANI,
            country = "Pakistan",
            mealType = MealType.DINNER,
            dayType = DayType.SPECIAL,
            season = "Winter",
            isSpecial = true,
            prepTimeMinutes = 40,
            difficulty = "Medium",
            servings = 4,
            ingredients = listOf("Coarse Beef Mince (with Fat)", "Maize Flour (Makai Ka Atta)", "Pomegranate Seeds (Anardana)", "Crushed Coriander", "Tomatoes", "Green Chillies"),
            recipeSteps = listOf(
                "Mix mince with coarsely ground anardana, coriander seeds, chopped onions, and corn flour.",
                "Flatten into wide thin patties and press a fresh tomato slice onto the top.",
                "Shallow fry in hot animal fat or ghee in a flat iron tawa until crisp and juicy."
            ),
            chefTip = "Use 20% fat in the mince to keep the kababs melt-in-mouth juicy inside with crunchy edges.",
            tags = listOf("Dinner", "BBQ", "Kabab", "Crispy", "Peshawar")
        ),
        Dish(
            id = "pk_17",
            name = "Shahi Chicken Korma with Roghani Naan",
            cuisine = CuisineType.PAKISTANI,
            country = "Pakistan",
            mealType = MealType.DINNER,
            dayType = DayType.SPECIAL,
            season = "All",
            isSpecial = true,
            prepTimeMinutes = 45,
            difficulty = "Medium",
            servings = 5,
            ingredients = listOf("Chicken", "Yogurt", "Crispy Fried Onions (Barista)", "Nutmeg & Mace", "Cardamom", "Kewra Essence", "Ghee"),
            recipeSteps = listOf(
                "Crush crispy golden fried onions into a coarse paste.",
                "Cook chicken in ghee with whole spices, ginger-garlic, and whisked yogurt.",
                "Add crushed brown onions, nutmeg-mace powder, and simmer until gravy turns danedaar (grainy).",
                "Finish with a few drops of kewra essence and serve with hot roghani naan."
            ),
            chefTip = "Never blend fried onions in a machine with water; crush them by hand for grainy korma texture.",
            tags = listOf("Dinner", "Dawat", "Wedding Special", "Royal")
        ),

        // Iftari / Snacks
        Dish(
            id = "pk_18",
            name = "Crispy Mixed Pakoras & Mint Chutney",
            cuisine = CuisineType.PAKISTANI,
            country = "Pakistan",
            mealType = MealType.IFTARI,
            dayType = DayType.NORMAL,
            season = "All",
            isSpecial = false,
            prepTimeMinutes = 20,
            difficulty = "Easy",
            servings = 4,
            ingredients = listOf("Besan (Gram Flour)", "Potatoes", "Onions", "Spinach (Palak)", "Ajwain (Carom Seeds)", "Coriander Seeds", "Pomegranate Seeds"),
            recipeSteps = listOf(
                "Slice potatoes, onions, and spinach thinly.",
                "Mix with besan, carom seeds, crushed coriander, chilli flakes, and minimal water.",
                "Drop spoonfuls into medium-hot oil and deep fry until ultra-crisp and golden."
            ),
            chefTip = "Add a spoonful of hot oil into the dry besan batter before frying for extra-crisp pakoras.",
            tags = listOf("Iftari", "Ramadan", "Snack", "Rainy Day")
        ),
        Dish(
            id = "pk_19",
            name = "Lahori Dahi Bhallay with Imli Chutney",
            cuisine = CuisineType.PAKISTANI,
            country = "Pakistan",
            mealType = MealType.IFTARI,
            dayType = DayType.NORMAL,
            season = "Summer",
            isSpecial = false,
            prepTimeMinutes = 30,
            difficulty = "Easy",
            servings = 4,
            ingredients = listOf("Mash Daal Vadas", "Thick Sweetened Yogurt", "Tamarind (Imli) Chutney", "Mint Chutney", "Chana & Boiled Potatoes", "Papri", "Chaat Masala"),
            recipeSteps = listOf(
                "Soak fried mash daal vadas in warm water, then gently squeeze out excess water.",
                "Arrange in a platter with boiled chickpeas and diced potatoes.",
                "Cover generously with creamy whisked yogurt and drizzle with tangy tamarind and mint chutneys.",
                "Sprinkle chaat masala and crush crispy papri on top."
            ),
            chefTip = "Whisk the yogurt with 1 tablespoon of sugar and a pinch of black salt for balanced flavor.",
            tags = listOf("Iftari", "Street Food", "Refreshing", "Cold")
        ),
        Dish(
            id = "pk_20",
            name = "Chicken Tikka Paratha Roll",
            cuisine = CuisineType.PAKISTANI,
            country = "Pakistan",
            mealType = MealType.EVENING_SNACKS,
            dayType = DayType.NORMAL,
            season = "All",
            isSpecial = false,
            prepTimeMinutes = 30,
            difficulty = "Easy",
            servings = 3,
            ingredients = listOf("Boneless Chicken", "Tikka Masala", "Crispy Puri Paratha", "Onion Rings", "Garlic Mayo / Mint Chutney", "Coal for Smoke"),
            recipeSteps = listOf(
                "Marinate chicken cubes in tikka spices, pan-fry, and smoke with charcoal.",
                "Fry flaky parathas on a tawa until golden brown.",
                "Place chicken tikka, crunchy onions, and mint mayo on the paratha, roll tight in butter paper."
            ),
            chefTip = "Smoke the cooked chicken with a live charcoal cube for that authentic street grill aroma.",
            tags = listOf("Evening Snacks", "Street Food", "Kids Favorite")
        ),
        Dish(
            id = "pk_21",
            name = "Crispy Aloo Samosa with Mint Sauce",
            cuisine = CuisineType.PAKISTANI,
            country = "Pakistan",
            mealType = MealType.EVENING_SNACKS,
            dayType = DayType.NORMAL,
            season = "All",
            isSpecial = false,
            prepTimeMinutes = 40,
            difficulty = "Medium",
            servings = 4,
            ingredients = listOf("All-Purpose Flour Dough", "Coarse Mashed Potatoes", "Green Peas", "Whole Cumin", "Coriander Seeds", "Green Chillies"),
            recipeSteps = listOf(
                "Knead stiff flour dough with carom seeds and ghee (moyen).",
                "Prepare spiced potato and pea stuffing with roasted cumin and crushed coriander.",
                "Shape into cones, fill with stuffing, seal with water, and slow-fry on low flame until crunchy."
            ),
            chefTip = "Fry samosas on gentle low flame; fast frying causes bubbles and soggy pastry.",
            tags = listOf("Snacks", "Tea Time", "Crispy", "Street Style")
        ),

        // Desserts
        Dish(
            id = "pk_22",
            name = "Gulab Jamun with Cardamom Syrup",
            cuisine = CuisineType.DESSERT,
            country = "Pakistan",
            mealType = MealType.DINNER,
            dayType = DayType.SPECIAL,
            season = "All",
            isSpecial = true,
            prepTimeMinutes = 40,
            difficulty = "Medium",
            servings = 6,
            ingredients = listOf("Milk Powder / Khoya", "Flour", "Semolina", "Cardamom Sugar Syrup", "Rose Water", "Pistachios", "Ghee"),
            recipeSteps = listOf(
                "Form smooth dough from milk powder, ghee, and milk without over-kneading.",
                "Roll into crack-free small spheres and fry on very low heat in ghee until deep golden.",
                "Drop immediately into warm cardamom-infused sugar syrup and soak for 2 hours."
            ),
            chefTip = "Keep the sugar syrup warm (not boiling) when dropping in fried jamuns so they remain soft.",
            tags = listOf("Dessert", "Mithai", "Festive", "Sweet")
        ),
        Dish(
            id = "pk_23",
            name = "Zafrani Kheer (Rice Pudding)",
            cuisine = CuisineType.DESSERT,
            country = "Pakistan",
            mealType = MealType.DINNER,
            dayType = DayType.SPECIAL,
            season = "All",
            isSpecial = true,
            prepTimeMinutes = 50,
            difficulty = "Medium",
            servings = 6,
            ingredients = listOf("Broken Basmati Rice", "Full Fat Milk", "Sugar", "Saffron Strands", "Green Cardamom", "Khoya", "Silver Leaf (Warq)"),
            recipeSteps = listOf(
                "Soak and coarsely grind basmati rice.",
                "Simmer in full cream milk on low heat while stirring continuously until thick and creamy.",
                "Add sugar, saffron infused in milk, and khoya; simmer for another 10 minutes.",
                "Chill in earthenware bowls (kullhad) and garnish with almonds and silver leaf."
            ),
            chefTip = "Slow reduction of milk on low heat imparts the authentic caramelized rabri flavor.",
            tags = listOf("Dessert", "Royal", "Dawat", "Chilled")
        ),
        Dish(
            id = "pk_24",
            name = "Gajar Ka Halwa (Carrot Halwa)",
            cuisine = CuisineType.DESSERT,
            country = "Pakistan",
            mealType = MealType.DINNER,
            dayType = DayType.SPECIAL,
            season = "Winter",
            isSpecial = true,
            prepTimeMinutes = 60,
            difficulty = "Medium",
            servings = 6,
            ingredients = listOf("Red Winter Carrots", "Fresh Milk", "Desi Ghee", "Sugar", "Khoya (Mawa)", "Cardamom", "Cashews & Pistachios"),
            recipeSteps = listOf(
                "Grate fresh red carrots and simmer in milk until milk evaporates completely.",
                "Add generous desi ghee and bhunify on medium flame until glossy and aromatic.",
                "Add sugar and cook until moisture dries up; fold in crumbled khoya and roasted nuts."
            ),
            chefTip = "Use sweet seasonal red winter carrots for natural vibrancy without artificial color.",
            tags = listOf("Winter Special", "Dessert", "Warm", "Rich")
        ),
        Dish(
            id = "pk_25",
            name = "Shahi Tukray with Saffron Rabri",
            cuisine = CuisineType.DESSERT,
            country = "Pakistan",
            mealType = MealType.DINNER,
            dayType = DayType.SPECIAL,
            season = "All",
            isSpecial = true,
            prepTimeMinutes = 35,
            difficulty = "Easy",
            servings = 4,
            ingredients = listOf("White Bread Slices", "Desi Ghee for Frying", "Cardamom Sugar Syrup", "Condensed Milk / Thick Rabri", "Saffron", "Pistachios"),
            recipeSteps = listOf(
                "Cut bread triangles and fry in pure ghee until crispy golden.",
                "Dip fried bread quickly in warm cardamom syrup, then arrange in serving tray.",
                "Pour thick saffron rabri over the bread and top with sliced almonds and pistachios."
            ),
            chefTip = "Quick dip in sugar syrup ensures the bread stays crunchy rather than soggy under the rabri.",
            tags = listOf("Dessert", "Eid Special", "Quick Sweet", "Mughlai")
        ),

        // --- EXPANDED NORMAL PAKISTANI DISHES (Daal, Sabzi, Everyday Salan) ---
        Dish(
            id = "pk_26",
            name = "Dhaba Style Daal Maash",
            cuisine = CuisineType.PAKISTANI,
            country = "Pakistan",
            mealType = MealType.LUNCH,
            dayType = DayType.NORMAL,
            season = "All",
            isSpecial = false,
            prepTimeMinutes = 35,
            difficulty = "Easy",
            servings = 4,
            ingredients = listOf("White Urad Daal (Maash)", "Onions", "Tomatoes", "Ginger Juliennes", "Green Chillies", "Ghee", "Cumin"),
            recipeSteps = listOf(
                "Boil soaked daal maash until tender but holding its shape (khilwan).",
                "Prepare onion tomato masala with sliced ginger and green chillies in ghee.",
                "Toss boiled daal in masala, simmer on dum for 5 minutes, and garnish with fresh ginger."
            ),
            chefTip = "Do not overboil daal maash; grains must remain separate for dhaba style texture.",
            tags = listOf("Normal", "Everyday Lunch", "Dhaba", "Daal")
        ),
        Dish(
            id = "pk_27",
            name = "Dhaba Chana Daal Tarka",
            cuisine = CuisineType.PAKISTANI,
            country = "Pakistan",
            mealType = MealType.LUNCH,
            dayType = DayType.NORMAL,
            season = "All",
            isSpecial = false,
            prepTimeMinutes = 40,
            difficulty = "Easy",
            servings = 4,
            ingredients = listOf("Split Bengal Gram (Chana Daal)", "Garlic", "Whole Cumin", "Button Red Chillies", "Desi Ghee", "Turmeric"),
            recipeSteps = listOf(
                "Pressure cook soaked chana daal with turmeric and salt until soft.",
                "Sizzle golden sliced garlic, cumin, and whole button red chillies in desi ghee.",
                "Pour bubbling tarka over hot daal and cover immediately to trap the aroma."
            ),
            chefTip = "Serve with warm tandoori roti and thinly sliced raw onions soaked in lemon juice.",
            tags = listOf("Normal", "Daal", "Comfort Food", "Vegetarian")
        ),
        Dish(
            id = "pk_28",
            name = "Daal Palak (Lentils with Spinach)",
            cuisine = CuisineType.PAKISTANI,
            country = "Pakistan",
            mealType = MealType.DINNER,
            dayType = DayType.NORMAL,
            season = "Winter",
            isSpecial = false,
            prepTimeMinutes = 35,
            difficulty = "Easy",
            servings = 4,
            ingredients = listOf("Moong & Masoor Daal", "Fresh Spinach (Palak)", "Garlic", "Tomatoes", "Green Chillies", "Cumin"),
            recipeSteps = listOf(
                "Boil mixed lentils with salt and turmeric until smooth.",
                "Finely chop washed spinach and cook with onions, tomatoes, and garlic.",
                "Combine lentils and spinach, temper with sizzling garlic and cumin ghee tarka."
            ),
            chefTip = "Plunge spinach into cold water after washing to maintain bright green color.",
            tags = listOf("Normal", "Healthy", "Daal", "Sabzi")
        ),
        Dish(
            id = "pk_29",
            name = "Aloo Palak Homestyle Curry",
            cuisine = CuisineType.PAKISTANI,
            country = "Pakistan",
            mealType = MealType.DINNER,
            dayType = DayType.NORMAL,
            season = "All",
            isSpecial = false,
            prepTimeMinutes = 30,
            difficulty = "Easy",
            servings = 4,
            ingredients = listOf("Potatoes (Aloo)", "Fresh Spinach", "Fenugreek Leaves (Methi)", "Tomatoes", "Cumin Seeds", "Red Chilli Flakes"),
            recipeSteps = listOf(
                "Sauté cubed potatoes in oil with cumin seeds until light golden edges form.",
                "Add chopped spinach and fresh fenugreek with spices without adding extra water.",
                "Cover and cook on low heat until potatoes are tender and liquid dries up into a rich bhuna sabzi."
            ),
            chefTip = "Fresh methi leaves add an unbeatable earthy restaurant aroma to aloo palak.",
            tags = listOf("Normal", "Sabzi", "Vegetarian", "Everyday Dinner")
        ),
        Dish(
            id = "pk_30",
            name = "Aloo Gobhi Masala (Cauliflower & Potato)",
            cuisine = CuisineType.PAKISTANI,
            country = "Pakistan",
            mealType = MealType.LUNCH,
            dayType = DayType.NORMAL,
            season = "Winter",
            isSpecial = false,
            prepTimeMinutes = 30,
            difficulty = "Easy",
            servings = 4,
            ingredients = listOf("Cauliflower Florets", "Potatoes", "Onions", "Ginger Juliennes", "Tomatoes", "Garam Masala"),
            recipeSteps = listOf(
                "Cut cauliflower florets and soak in warm salted water for 5 minutes.",
                "Bhunify onions, ginger-garlic, and spices, then add potatoes and cauliflower.",
                "Steam covered on low flame until tender, then sprinkle garam masala and fresh coriander."
            ),
            chefTip = "Lots of fresh julienned ginger helps digest cauliflower and enhances its natural flavor.",
            tags = listOf("Normal", "Sabzi", "Vegetarian", "Winter Special")
        ),
        Dish(
            id = "pk_31",
            name = "Tori Ki Sabzi (Ridge Gourd Bhujia)",
            cuisine = CuisineType.PAKISTANI,
            country = "Pakistan",
            mealType = MealType.LUNCH,
            dayType = DayType.NORMAL,
            season = "Summer",
            isSpecial = false,
            prepTimeMinutes = 25,
            difficulty = "Easy",
            servings = 3,
            ingredients = listOf("Tender Ridge Gourd (Tori)", "Sliced Onions", "Tomatoes", "Cumin Seeds", "Green Chillies", "Turmeric"),
            recipeSteps = listOf(
                "Peel ridges and slice tori into round discs.",
                "Sauté onions with cumin seeds, add sliced tori, salt, and turmeric.",
                "Cook covered; tori releases its own juices. Simmer until tender and oil separates."
            ),
            chefTip = "Do not add any water; ridge gourd cooks completely in its own natural moisture.",
            tags = listOf("Normal", "Sabzi", "Light Meal", "Summer")
        ),
        Dish(
            id = "pk_32",
            name = "Karela Pyaz (Crispy Bitter Gourd with Onions)",
            cuisine = CuisineType.PAKISTANI,
            country = "Pakistan",
            mealType = MealType.LUNCH,
            dayType = DayType.NORMAL,
            season = "Summer",
            isSpecial = false,
            prepTimeMinutes = 35,
            difficulty = "Medium",
            servings = 3,
            ingredients = listOf("Bitter Gourd (Karela)", "Lots of Sliced Onions", "Tomatoes", "Pomegranate Seeds (Anardana)", "Fennel Seeds (Saunf)"),
            recipeSteps = listOf(
                "Slice bitter gourd thinly, rub with salt, let rest 20 minutes, then squeeze out bitter water.",
                "Shallow fry karela slices in mustard oil until crisp and brown.",
                "In separate oil, fry double quantity of sliced onions with anardana and spices, then toss with fried karela."
            ),
            chefTip = "Generous caramelized sweet onions and tangy anardana perfectly balance the bitterness.",
            tags = listOf("Normal", "Sabzi", "Traditional", "Health")
        ),
        Dish(
            id = "pk_33",
            name = "Aloo Shimla Mirch (Potato & Capsicum)",
            cuisine = CuisineType.PAKISTANI,
            country = "Pakistan",
            mealType = MealType.DINNER,
            dayType = DayType.NORMAL,
            season = "All",
            isSpecial = false,
            prepTimeMinutes = 25,
            difficulty = "Easy",
            servings = 3,
            ingredients = listOf("Green Bell Peppers (Capsicum)", "Potatoes", "Onions", "Cumin", "Coriander Powder", "Chilli Flakes"),
            recipeSteps = listOf(
                "Cube potatoes and capsicum into even bite-sized pieces.",
                "Fry potatoes with cumin seeds until half tender, then add chopped onions and tomatoes.",
                "Toss in capsicum during the last 6 minutes so it stays crunchy and colorful."
            ),
            chefTip = "Never overcook capsicum; it should retain its crisp bite and sweet aroma.",
            tags = listOf("Normal", "Sabzi", "Quick", "Comfort")
        ),
        Dish(
            id = "pk_34",
            name = "Kadhi Pakora with Steamed Basmati Rice",
            cuisine = CuisineType.PAKISTANI,
            country = "Pakistan",
            mealType = MealType.LUNCH,
            dayType = DayType.NORMAL,
            season = "All",
            isSpecial = false,
            prepTimeMinutes = 50,
            difficulty = "Medium",
            servings = 4,
            ingredients = listOf("Sour Yogurt (Khatta Dahi)", "Besan (Gram Flour)", "Fenugreek Seeds", "Curry Leaves", "Fried Besan Pakoras", "Red Button Chillies"),
            recipeSteps = listOf(
                "Whisk sour curd and gram flour with water, turmeric, and salt into a smooth slurry.",
                "Simmer slowly on low heat for 45 minutes until rich, thick, and aromatic.",
                "Drop in freshly fried crispy onion pakoras and pour sizzling tarka with curry leaves and button chillies."
            ),
            chefTip = "Use 2-day-old naturally sour yogurt for authentic tangy kadhi flavor.",
            tags = listOf("Normal", "Comfort Food", "Lahori", "Everyday Lunch")
        ),
        Dish(
            id = "pk_35",
            name = "Aloo Matar Shorba",
            cuisine = CuisineType.PAKISTANI,
            country = "Pakistan",
            mealType = MealType.DINNER,
            dayType = DayType.NORMAL,
            season = "Winter",
            isSpecial = false,
            prepTimeMinutes = 30,
            difficulty = "Easy",
            servings = 4,
            ingredients = listOf("Potatoes", "Fresh Green Peas (Matar)", "Onion-Tomato Puree", "Ginger-Garlic", "Garam Masala", "Coriander"),
            recipeSteps = listOf(
                "Sauté onion-tomato masala with ginger-garlic paste until oil surfaces.",
                "Add potato chunks and fresh sweet green peas, cook with spices for 3 minutes.",
                "Add warm water to form a light shorba gravy, simmer until potatoes are melt-in-mouth."
            ),
            chefTip = "Crush roasted cumin seeds between your palms on top before serving.",
            tags = listOf("Normal", "Sabzi", "Curry", "Ghar Ka Khana")
        ),
        Dish(
            id = "pk_36",
            name = "Arbi Masala (Crispy Fried Taro Root)",
            cuisine = CuisineType.PAKISTANI,
            country = "Pakistan",
            mealType = MealType.LUNCH,
            dayType = DayType.NORMAL,
            season = "Summer",
            isSpecial = false,
            prepTimeMinutes = 30,
            difficulty = "Easy",
            servings = 3,
            ingredients = listOf("Taro Root (Arbi)", "Ajwain (Carom Seeds)", "Onions", "Tomatoes", "Amchur Powder", "Lemon Juice"),
            recipeSteps = listOf(
                "Peel, wash, and salt arbi to remove sliminess, then shallow fry until golden and crisp.",
                "Temper ajwain seeds in oil, make a spicy onion tomato masala.",
                "Toss fried arbi in masala with amchur and lemon juice on low dum for 5 minutes."
            ),
            chefTip = "Ajwain is essential in arbi cooking both for flavor and easing digestion.",
            tags = listOf("Normal", "Sabzi", "Crispy", "Lunch")
        ),
        Dish(
            id = "pk_37",
            name = "Tinda Masala (Apple Gourd Bhujia)",
            cuisine = CuisineType.PAKISTANI,
            country = "Pakistan",
            mealType = MealType.LUNCH,
            dayType = DayType.NORMAL,
            season = "Summer",
            isSpecial = false,
            prepTimeMinutes = 25,
            difficulty = "Easy",
            servings = 3,
            ingredients = listOf("Tender Tinday", "Onions", "Tomatoes", "Cumin", "Turmeric", "Green Chillies", "Fresh Coriander"),
            recipeSteps = listOf(
                "Peel and slice round tinday into thin wedges.",
                "Sauté with chopped onions, cumin, and sliced green chillies.",
                "Cover and cook on gentle steam until soft and lightly caramelized."
            ),
            chefTip = "Select small, tender tinday without hard yellow seeds for sweetest taste.",
            tags = listOf("Normal", "Sabzi", "Light", "Everyday")
        ),
        Dish(
            id = "pk_38",
            name = "Aloo Methi (Potatoes with Fresh Fenugreek)",
            cuisine = CuisineType.PAKISTANI,
            country = "Pakistan",
            mealType = MealType.DINNER,
            dayType = DayType.NORMAL,
            season = "Winter",
            isSpecial = false,
            prepTimeMinutes = 30,
            difficulty = "Easy",
            servings = 3,
            ingredients = listOf("Fresh Methi Leaves", "Diced Potatoes", "Garlic", "Red Chilli Flakes", "Mustard Oil"),
            recipeSteps = listOf(
                "Pluck fresh methi leaves, wash thoroughly, and chop finely.",
                "Fry cubed potatoes in mustard oil with sliced garlic and chilli flakes until half done.",
                "Add chopped methi leaves and cook uncovered until potatoes are tender and fragrant."
            ),
            chefTip = "Never cover aloo methi while cooking; cooking uncovered prevents any bitterness.",
            tags = listOf("Normal", "Sabzi", "Winter Classic", "Healthy")
        ),
        Dish(
            id = "pk_39",
            name = "Everyday Chicken Ka Salan",
            cuisine = CuisineType.PAKISTANI,
            country = "Pakistan",
            mealType = MealType.DINNER,
            dayType = DayType.NORMAL,
            season = "All",
            isSpecial = false,
            prepTimeMinutes = 35,
            difficulty = "Easy",
            servings = 4,
            ingredients = listOf("Bone-in Chicken", "Onion Paste", "Tomatoes", "Ginger-Garlic", "Coriander Powder", "Yogurt", "Garam Masala"),
            recipeSteps = listOf(
                "Sauté onion paste in oil until golden, then fry chicken with ginger-garlic.",
                "Stir in whisked yogurt, tomatoes, and ground spices, bhunify until oil surfaces.",
                "Add warm water to create a homestyle medium-thin salan, simmer for 15 minutes."
            ),
            chefTip = "Bhunify the masala thoroughly until oil separates before adding water for deep flavor.",
            tags = listOf("Normal", "Everyday Dinner", "Chicken Curry", "Ghar Ka Khana")
        ),
        Dish(
            id = "pk_40",
            name = "Keema Matar (Spiced Mince with Peas)",
            cuisine = CuisineType.PAKISTANI,
            country = "Pakistan",
            mealType = MealType.DINNER,
            dayType = DayType.NORMAL,
            season = "All",
            isSpecial = false,
            prepTimeMinutes = 35,
            difficulty = "Easy",
            servings = 4,
            ingredients = listOf("Beef or Mutton Mince (Keema)", "Green Peas", "Onions", "Tomatoes", "Ginger Juliennes", "Whole Spices"),
            recipeSteps = listOf(
                "Brown whole spices and chopped onions in oil, then add ginger-garlic and minced meat.",
                "Bhunify mince on high flame until moisture evaporates and meat turns fragrant.",
                "Add tomatoes and green peas, simmer covered on low heat until tender."
            ),
            chefTip = "Add a dollop of butter or ghee right at the end for rich home-cooked taste.",
            tags = listOf("Normal", "Meat", "Dinner", "Quick")
        ),
        Dish(
            id = "pk_41",
            name = "Keema Aloo (Homestyle Minced Meat & Potatoes)",
            cuisine = CuisineType.PAKISTANI,
            country = "Pakistan",
            mealType = MealType.LUNCH,
            dayType = DayType.NORMAL,
            season = "All",
            isSpecial = false,
            prepTimeMinutes = 35,
            difficulty = "Easy",
            servings = 4,
            ingredients = listOf("Mince (Keema)", "Potatoes (Cubed)", "Onions", "Tomatoes", "Yogurt", "Green Chillies"),
            recipeSteps = listOf(
                "Sauté onions and ginger-garlic, add mince and bhunify thoroughly.",
                "Drop in cubed potatoes and spices with a cup of water.",
                "Cover and simmer until potatoes are fork-tender and gravy is semi-dry."
            ),
            chefTip = "Cut potatoes into small equal cubes so they cook at the exact same rate as the mince.",
            tags = listOf("Normal", "Meat", "Lunch", "Comfort")
        ),
        Dish(
            id = "pk_42",
            name = "Daal Gosht (Mutton with Chana Daal)",
            cuisine = CuisineType.PAKISTANI,
            country = "Pakistan",
            mealType = MealType.DINNER,
            dayType = DayType.NORMAL,
            season = "All",
            isSpecial = false,
            prepTimeMinutes = 55,
            difficulty = "Medium",
            servings = 4,
            ingredients = listOf("Mutton Pieces", "Chana Daal", "Onions", "Yogurt", "Ginger-Garlic", "Ghee Tarka", "Green Chillies"),
            recipeSteps = listOf(
                "Cook mutton with onions, ginger-garlic, and spices until 80% tender.",
                "Boil soaked chana daal separately until soft but not mashed.",
                "Combine meat and daal, simmer on low flame so flavors meld, finish with a garlic-cumin tarka."
            ),
            chefTip = "Boiling daal separately keeps the gravy clean and prevents the daal from getting mushy.",
            tags = listOf("Normal", "Meat & Daal", "Comfort Food", "Dinner")
        ),
        Dish(
            id = "pk_43",
            name = "Matar Pulao with Zeera Raita",
            cuisine = CuisineType.PAKISTANI,
            country = "Pakistan",
            mealType = MealType.LUNCH,
            dayType = DayType.NORMAL,
            season = "Winter",
            isSpecial = false,
            prepTimeMinutes = 30,
            difficulty = "Easy",
            servings = 4,
            ingredients = listOf("Basmati Rice", "Fresh Green Peas (Matar)", "Whole Cumin", "Whole Garam Masala", "Fried Onions", "Yogurt Raita"),
            recipeSteps = listOf(
                "Lightly sauté whole cumin, cloves, and cardamom in oil with sliced onions.",
                "Add sweet green peas and soaked basmati rice with seasoned water.",
                "Bring to boil, cover and steam (dum) on low flame for 12 minutes. Serve with zeera dahi."
            ),
            chefTip = "Do not brown the onions too much; keep them translucent for sweet, light-colored pulao.",
            tags = listOf("Normal", "Everyday Lunch", "Rice", "Vegetarian")
        ),
        Dish(
            id = "pk_44",
            name = "Anda Ghotala with Tawa Paratha",
            cuisine = CuisineType.PAKISTANI,
            country = "Pakistan",
            mealType = MealType.DINNER,
            dayType = DayType.NORMAL,
            season = "All",
            isSpecial = false,
            prepTimeMinutes = 15,
            difficulty = "Easy",
            servings = 2,
            ingredients = listOf("Eggs", "Butter / Ghee", "Finely Chopped Onions", "Tomatoes", "Green Chillies", "Pav Bhaji or Tikka Masala"),
            recipeSteps = listOf(
                "Melt butter on a flat tawa, sauté onions, tomatoes, and chopped green chillies with spices.",
                "Scramble 2 eggs into the bhuna masala while half-frying 1 sunny-side-up egg.",
                "Mash together gently on the tawa and serve sizzling hot with crispy flaky parathas."
            ),
            chefTip = "Karachi Burns Road secret: keep the eggs slightly runny before pulling off heat.",
            tags = listOf("Normal", "Eggs", "Street Style", "Quick Dinner")
        ),
        Dish(
            id = "pk_45",
            name = "Anda Aloo Curry",
            cuisine = CuisineType.PAKISTANI,
            country = "Pakistan",
            mealType = MealType.DINNER,
            dayType = DayType.NORMAL,
            season = "All",
            isSpecial = false,
            prepTimeMinutes = 25,
            difficulty = "Easy",
            servings = 3,
            ingredients = listOf("Hard Boiled Eggs", "Potatoes", "Onion Paste", "Tomato Puree", "Turmeric", "Cumin", "Fresh Coriander"),
            recipeSteps = listOf(
                "Prick boiled eggs with a fork, coat in turmeric and salt, shallow fry until blistered golden.",
                "Cook onion-tomato masala with whole cumin, then add potato pieces.",
                "Add water for gravy, slide in golden eggs, and simmer for 10 minutes."
            ),
            chefTip = "Frying the boiled eggs with turmeric gives a delicious chewy crispy skin that absorbs gravy.",
            tags = listOf("Normal", "Eggs", "Budget Friendly", "Everyday Dinner")
        ),
        Dish(
            id = "pk_46",
            name = "Lobia Ka Salan (Red Kidney Bean Curry)",
            cuisine = CuisineType.PAKISTANI,
            country = "Pakistan",
            mealType = MealType.LUNCH,
            dayType = DayType.NORMAL,
            season = "All",
            isSpecial = false,
            prepTimeMinutes = 35,
            difficulty = "Easy",
            servings = 4,
            ingredients = listOf("Red Lobia / Kidney Beans", "Onions", "Tomatoes", "Ginger-Garlic", "Coriander", "Cumin"),
            recipeSteps = listOf(
                "Boil soaked beans until buttery soft.",
                "Prepare aromatic bhuna masala of onions and tomatoes.",
                "Add beans with cooking water, mash a small spoonful to naturally thicken gravy, simmer."
            ),
            chefTip = "A squeeze of lemon juice at the end elevates the earthy beans flavor immensely.",
            tags = listOf("Normal", "Healthy", "Vegetarian", "Lunch")
        ),
        Dish(
            id = "pk_47",
            name = "Moong Daal Khichdi with Dahi & Achar",
            cuisine = CuisineType.PAKISTANI,
            country = "Pakistan",
            mealType = MealType.DINNER,
            dayType = DayType.NORMAL,
            season = "All",
            isSpecial = false,
            prepTimeMinutes = 25,
            difficulty = "Easy",
            servings = 3,
            ingredients = listOf("Yellow Moong Daal", "Rice", "Cumin Seeds", "Black Peppercorns", "Desi Ghee", "Fresh Dahi"),
            recipeSteps = listOf(
                "Wash rice and yellow moong daal together, soak for 20 minutes.",
                "Sizzle cumin seeds and black peppercorns in pure desi ghee.",
                "Add rice, daal, and seasoned water; cook until soft, fluffy, and comforting. Serve with fresh curd."
            ),
            chefTip = "Serve with a generous spoonful of desi ghee melting right on top of piping hot khichdi.",
            tags = listOf("Normal", "Comfort Food", "Light Dinner", "Healthy")
        ),
        Dish(
            id = "pk_48",
            name = "Shami Kabab with Phulka Roti & Mint Raita",
            cuisine = CuisineType.PAKISTANI,
            country = "Pakistan",
            mealType = MealType.DINNER,
            dayType = DayType.NORMAL,
            season = "All",
            isSpecial = false,
            prepTimeMinutes = 30,
            difficulty = "Easy",
            servings = 3,
            ingredients = listOf("Beef / Chicken Shami Kababs", "Whole Wheat Flour Roti", "Fresh Mint Chutney", "Yogurt (Dahi)", "Onion Salad"),
            recipeSteps = listOf(
                "Pan-fry shami kababs dipped in whisked egg on medium heat until crispy golden brown.",
                "Make thin, puffed whole wheat phulkas directly on an open flame.",
                "Whisk yogurt with crushed mint-coriander green chutney and sliced onion rings."
            ),
            chefTip = "Dip kababs in lightly whisked egg white for extra crispy exterior without sogginess.",
            tags = listOf("Normal", "Quick Dinner", "Comfort Food", "High Protein")
        ),
        Dish(
            id = "pk_49",
            name = "Gobi Gosht (Cauliflower Simmered with Mutton)",
            cuisine = CuisineType.PAKISTANI,
            country = "Pakistan",
            mealType = MealType.DINNER,
            dayType = DayType.NORMAL,
            season = "Winter",
            isSpecial = false,
            prepTimeMinutes = 45,
            difficulty = "Medium",
            servings = 4,
            ingredients = listOf("Mutton Pieces", "Cauliflower Florets", "Onions", "Tomatoes", "Ginger Juliennes", "Whole Garam Masala"),
            recipeSteps = listOf(
                "Cook mutton with onions, tomatoes, and ginger-garlic until 80% tender.",
                "Add cauliflower florets, toss in meat juices, and cover on low dum heat for 12 minutes.",
                "Garnish with julienned ginger and fresh coriander leaves."
            ),
            chefTip = "Do not stir too frequently after adding cauliflower to keep florets intact.",
            tags = listOf("Normal", "Ghar Ka Khana", "Meat & Sabzi", "Winter")
        ),

        Dish(
            id = "pk_kadhi_chawal",
            name = "Kadhi Chawal with Pakora",
            cuisine = CuisineType.PAKISTANI,
            country = "Pakistan",
            mealType = MealType.LUNCH,
            dayType = DayType.NORMAL,
            season = "All",
            isSpecial = false,
            prepTimeMinutes = 45,
            difficulty = "Medium",
            servings = 4,
            ingredients = listOf("Gram Flour (Besan)", "Sour Yogurt (Dahi)", "Onions", "Potatoes", "Fenugreek Seeds (Methi Dana)", "Cumin", "Turmeric", "Green Chillies", "Basmati Rice"),
            recipeSteps = listOf(
                "Whisk besan and sour yogurt with water, turmeric, and salt; simmer on medium flame for 40 minutes stirring continuously until thick and tangy.",
                "Make a batter of besan, sliced onions, potatoes, crushed coriander, and spices, then deep-fry into crispy pakoras.",
                "Drop the hot pakoras directly into the simmering kadhi.",
                "Temper with cumin seeds, curry leaves, and round red chillies in hot oil, and serve over steamed Basmati rice."
            ),
            chefTip = "Use slightly sour (khatta) yogurt to give the kadhi its signature authentic tangy punch.",
            tags = listOf("Normal", "Kadhi Chawal", "Comfort Food", "Vegetarian", "Lunch")
        ),
        Dish(
            id = "pk_dahi_phulki",
            name = "Dahi Phulki with Warm Roti",
            cuisine = CuisineType.PAKISTANI,
            country = "Pakistan",
            mealType = MealType.LUNCH,
            dayType = DayType.NORMAL,
            season = "Summer",
            isSpecial = false,
            prepTimeMinutes = 25,
            difficulty = "Easy",
            servings = 3,
            ingredients = listOf("Gram Flour (Besan)", "Fresh Yogurt", "Roasted Cumin Powder", "Chaat Masala", "Mint & Coriander Chutney", "Whole Wheat Flour for Roti"),
            recipeSteps = listOf(
                "Whisk besan with a pinch of baking soda, salt, and water, then drop small spoonfuls into hot oil to make light phulkis.",
                "Soak the fried phulkis in warm water for 5 minutes, gently squeeze out excess water.",
                "Whisk fresh creamy yogurt with roasted ground cumin, black salt, and green chutney.",
                "Immerse soft phulkis in the spiced yogurt, sprinkle chaat masala and red chilli flakes, and enjoy with soft phulkay (roti)."
            ),
            chefTip = "Squeezing the phulkis in warm water makes them incredibly soft and allows them to absorb the yogurt completely.",
            tags = listOf("Normal", "Dahi Phulki", "Cooling", "Summer Meal", "Light & Healthy", "Lunch")
        ),
        Dish(
            id = "pk_chana_pulao",
            name = "Chana Pulao with Mint Raita",
            cuisine = CuisineType.PAKISTANI,
            country = "Pakistan",
            mealType = MealType.LUNCH,
            dayType = DayType.NORMAL,
            season = "All",
            isSpecial = false,
            prepTimeMinutes = 40,
            difficulty = "Easy",
            servings = 4,
            ingredients = listOf("Boiled Chickpeas (Safaid Chana)", "Aged Basmati Rice", "Onions", "Ginger-Garlic", "Whole Garam Masala", "Green Chillies", "Yogurt Raita"),
            recipeSteps = listOf(
                "Brown sliced onions in oil with bay leaf, cinnamon, cloves, and cumin seeds.",
                "Add ginger-garlic paste, slit green chillies, and boiled chickpeas; sauté for 3 minutes.",
                "Add water, bring to a rolling boil, and gently slide in soaked Basmati rice.",
                "Cook until water level recedes, then cover tightly on dum for 15 minutes. Serve with cool mint yogurt raita."
            ),
            chefTip = "Using chickpea cooking water (aquafaba) to cook the rice imparts rich savory depth.",
            tags = listOf("Normal", "Chawal", "Pulao", "Protein Rich", "Lunch")
        ),
        Dish(
            id = "pk_tahiri",
            name = "Tahiri (Aloo Pulao / Yellow Spiced Rice)",
            cuisine = CuisineType.PAKISTANI,
            country = "Pakistan",
            mealType = MealType.LUNCH,
            dayType = DayType.NORMAL,
            season = "All",
            isSpecial = false,
            prepTimeMinutes = 30,
            difficulty = "Easy",
            servings = 4,
            ingredients = listOf("Basmati Rice", "Potatoes (Cubed)", "Onions", "Turmeric Powder", "Cumin Seeds", "Tomatoes", "Green Chillies", "Fresh Mint"),
            recipeSteps = listOf(
                "Sauté sliced onions in oil with cumin seeds and whole cloves until translucent.",
                "Add diced tomatoes, turmeric, red chilli, and ginger paste; cook until oil releases.",
                "Add cubed potatoes and sauté for 3 minutes, then add water and soaked rice.",
                "Cook until moisture is absorbed, scatter fresh mint and whole green chillies, and dum for 15 minutes."
            ),
            chefTip = "A generous pinch of turmeric gives Tahiri its iconic vibrant golden hue and earthy aroma.",
            tags = listOf("Normal", "Chawal", "Homestyle Classic", "Kids Favorite", "Lunch")
        ),
        Dish(
            id = "pk_mix_sabzi",
            name = "Mix Sabzi (Aloo, Gajar, Matar, Methi)",
            cuisine = CuisineType.PAKISTANI,
            country = "Pakistan",
            mealType = MealType.LUNCH,
            dayType = DayType.NORMAL,
            season = "Winter",
            isSpecial = false,
            prepTimeMinutes = 30,
            difficulty = "Easy",
            servings = 4,
            ingredients = listOf("Potatoes", "Carrots (Gajar)", "Green Peas", "Fresh Fenugreek (Kasuri Methi)", "Onions", "Tomatoes", "Green Chillies"),
            recipeSteps = listOf(
                "Dice potatoes and carrots evenly, and shell fresh peas.",
                "Sauté onions with cumin, ginger, and green chillies, then add chopped tomatoes and dry spices.",
                "Add all vegetables and sauté on medium heat for 5 minutes.",
                "Cover tightly on low flame (dum) with fresh methi leaves until vegetables are tender, and serve with paratha or roti."
            ),
            chefTip = "Fresh methi added at the end imparts the signature winter aroma of dhaba-style mix sabzi.",
            tags = listOf("Normal", "Sabzi", "Vegetarian", "Nutritious", "Lunch", "Dinner")
        ),
        Dish(
            id = "pk_lauki_chana_daal",
            name = "Lauki Chana Daal (Bottle Gourd with Lentils)",
            cuisine = CuisineType.PAKISTANI,
            country = "Pakistan",
            mealType = MealType.LUNCH,
            dayType = DayType.NORMAL,
            season = "Summer",
            isSpecial = false,
            prepTimeMinutes = 30,
            difficulty = "Easy",
            servings = 4,
            ingredients = listOf("Bottle Gourd (Lauki / Kaddu)", "Chana Daal (Soaked)", "Onions", "Tomatoes", "Ginger-Garlic", "Ghee Tarka", "Spices"),
            recipeSteps = listOf(
                "Soak chana daal and peel and cube fresh bottle gourd.",
                "Sauté onions with ginger-garlic and tomatoes until aromatic.",
                "Add chana daal, lauki cubes, and 2 cups water; cook until daal and lauki are both tender.",
                "Temper with sizzling cumin seeds and garlic in ghee; serve warm with chapati."
            ),
            chefTip = "Lauki cooked with chana daal is a sunnah food that is cooling, light, and extremely nourishing.",
            tags = listOf("Normal", "Daal", "Sabzi", "Cooling", "Sunnah Food", "Healthy", "Lunch")
        ),

        // --- EXPANDED SPECIAL PAKISTANI DISHES (Gosht, Nihari, Paye, Biryani, Pulao, BBQ) ---
        Dish(
            id = "pk_50",
            name = "Karachi Special Beef Biryani with Aloo",
            cuisine = CuisineType.PAKISTANI,
            country = "Pakistan",
            mealType = MealType.DINNER,
            dayType = DayType.SPECIAL,
            season = "All",
            isSpecial = true,
            prepTimeMinutes = 75,
            difficulty = "Hard",
            servings = 6,
            ingredients = listOf("Aged Basmati Rice", "Tender Beef Chunks", "Large Potatoes (Aloo)", "Dried Plums (Aloo Bukhara)", "Yogurt", "Kewra Water", "Saffron Milk"),
            recipeSteps = listOf(
                "Marinate beef in yogurt, fried onions, dried plums, and ground biryani spice blend.",
                "Cook beef until succulent and prepare saffron-tinted soft potatoes.",
                "Parboil aged basmati rice to 75% doneness with whole spices and salt.",
                "Layer rice over rich beef masala, splash with kewra saffron milk, and seal for 20 minutes dum."
            ),
            chefTip = "Tender, melt-in-mouth beef paired with large golden potatoes is the unmistakable hallmark of Karachi.",
            tags = listOf("Special", "Biryani", "Karachi", "Dawat", "Feast")
        ),
        Dish(
            id = "pk_51",
            name = "Sindhi Spicy Chicken Biryani",
            cuisine = CuisineType.PAKISTANI,
            country = "Pakistan",
            mealType = MealType.DINNER,
            dayType = DayType.SPECIAL,
            season = "All",
            isSpecial = true,
            prepTimeMinutes = 60,
            difficulty = "Medium",
            servings = 5,
            ingredients = listOf("Chicken", "Basmati Rice", "Dried Plums", "Mint Leaves", "Green Chillies", "Yogurt", "Tomatoes", "Biryani Spices"),
            recipeSteps = listOf(
                "Cook spicy chicken gravy with lots of sliced green chillies, dried plums, and tangy yogurt.",
                "Layer parboiled rice with fresh mint, coriander, and tomato slices.",
                "Dum tightly on high flame for 5 minutes, then low heat for 15 minutes."
            ),
            chefTip = "Lots of fresh mint leaves between layers gives Sindhi Biryani its signature refreshing aroma.",
            tags = listOf("Special", "Biryani", "Sindhi", "Spicy", "Weekend")
        ),
        Dish(
            id = "pk_52",
            name = "Bannu Beef Pulao",
            cuisine = CuisineType.PAKISTANI,
            country = "Pakistan",
            mealType = MealType.LUNCH,
            dayType = DayType.SPECIAL,
            season = "All",
            isSpecial = true,
            prepTimeMinutes = 80,
            difficulty = "Hard",
            servings = 6,
            ingredients = listOf("Beef Shank & Ribs", "Aged Sella / Basmati Rice", "Bone Marrow Yakhni", "Whole Garam Masala", "Desi Ghee"),
            recipeSteps = listOf(
                "Boil beef shank with whole bone marrow and spices for 2 hours into an ultra-rich bone broth.",
                "Cook soaked rice directly in the reduced beef marrow yakhni with caramelized onions.",
                "Dum until rice absorbs all the rich gelatinous beef juices."
            ),
            chefTip = "Using beef with bone marrow gives Bannu Pulao its distinct glossy, sticky richness.",
            tags = listOf("Special", "Pulao", "KPK", "Gosht", "Royal Feast")
        ),
        Dish(
            id = "pk_53",
            name = "Kabuli Pulao with Carrots & Raisins",
            cuisine = CuisineType.PAKISTANI,
            country = "Pakistan",
            mealType = MealType.LUNCH,
            dayType = DayType.SPECIAL,
            season = "All",
            isSpecial = true,
            prepTimeMinutes = 70,
            difficulty = "Medium",
            servings = 5,
            ingredients = listOf("Mutton Shank", "Basmati Sella Rice", "Carrot Juliennes", "Black Raisins (Kishmish)", "Cardamom Powder", "Sugar Glaze"),
            recipeSteps = listOf(
                "Cook mutton until fall-apart tender in a light aromatic broth.",
                "Sauté carrot juliennes and raisins in ghee with a touch of sugar until caramelized and plump.",
                "Cook rice in mutton broth, crown with glazed carrots and raisins, and steam on low dum."
            ),
            chefTip = "Caramelized carrots and plump raisins provide the heavenly sweet contrast to savory mutton.",
            tags = listOf("Special", "Pulao", "Peshawari", "Festive", "Royal")
        ),
        Dish(
            id = "pk_54",
            name = "Nalli Nihari with Bone Marrow & Roghani Naan",
            cuisine = CuisineType.PAKISTANI,
            country = "Pakistan",
            mealType = MealType.BREAKFAST,
            dayType = DayType.SPECIAL,
            season = "Winter",
            isSpecial = true,
            prepTimeMinutes = 95,
            difficulty = "Hard",
            servings = 6,
            ingredients = listOf("Beef Shank (Bong)", "Nalli (Bone Marrow)", "Nihari Masala", "Wheat Flour Slurry", "Ghee", "Julienned Ginger", "Lemons"),
            recipeSteps = listOf(
                "Sear shank meat and marrow bones in ghee with ginger-garlic and authentic Nihari spice blend.",
                "Slow cook meat on low heat until fork-tender and bone marrow is luscious.",
                "Thicken gravy with roasted wheat flour slurry and simmer until tari separates.",
                "Tap bone marrow over the bowl and serve with steaming hot roghani naan."
            ),
            chefTip = "Slow-simmering on the lowest possible flame is the only way to melt collagen properly.",
            tags = listOf("Special", "Nihari", "Gosht", "Breakfast / Dinner", "Karachi Special")
        ),
        Dish(
            id = "pk_55",
            name = "Chinioti Mutton Kunna",
            cuisine = CuisineType.PAKISTANI,
            country = "Pakistan",
            mealType = MealType.DINNER,
            dayType = DayType.SPECIAL,
            season = "All",
            isSpecial = true,
            prepTimeMinutes = 90,
            difficulty = "Hard",
            servings = 5,
            ingredients = listOf("Mutton Leg / Shoulder", "Desi Ghee", "Whole Spices", "Roasted Flour", "Kala Zeera", "Clay Pot (Handi)"),
            recipeSteps = listOf(
                "Sear mutton in generous desi ghee inside a traditional clay handi.",
                "Add whole spices, water, and seal the clay pot lid with dough.",
                "Slow cook on embers for 2 hours, thicken with roasted flour, and finish with royal black cumin."
            ),
            chefTip = "Cooking inside an unglazed earthenware clay pot imparts a unique earthy aroma.",
            tags = listOf("Special", "Gosht", "Traditional", "Royal Feast")
        ),
        Dish(
            id = "pk_56",
            name = "Shahi Mutton Danedaar Korma",
            cuisine = CuisineType.PAKISTANI,
            country = "Pakistan",
            mealType = MealType.DINNER,
            dayType = DayType.SPECIAL,
            season = "All",
            isSpecial = true,
            prepTimeMinutes = 60,
            difficulty = "Medium",
            servings = 5,
            ingredients = listOf("Mutton", "Whisked Yogurt", "Crispy Fried Golden Onions", "Nutmeg & Mace", "Green Cardamom", "Kewra Water", "Ghee"),
            recipeSteps = listOf(
                "Crush crispy golden fried onions into a coarse texture by hand.",
                "Cook mutton in hot ghee with cardamom, ginger-garlic paste, and whisked yogurt.",
                "Add crushed onions and mace-nutmeg powder; cook on low heat until gravy turns danedaar (grainy).",
                "Sprinkle fragrant kewra water and serve with sesame naan."
            ),
            chefTip = "Hand-crush fried onions; never blend with water or the korma loses its signature graininess.",
            tags = listOf("Special", "Gosht", "Dawat Special", "Wedding Classic")
        ),
        Dish(
            id = "pk_57",
            name = "Shinwari Mutton Karahi",
            cuisine = CuisineType.PAKISTANI,
            country = "Pakistan",
            mealType = MealType.DINNER,
            dayType = DayType.SPECIAL,
            season = "All",
            isSpecial = true,
            prepTimeMinutes = 40,
            difficulty = "Easy",
            servings = 4,
            ingredients = listOf("Mutton on Bone", "Animal Fat / Ghee", "Ripe Tomatoes", "Slit Green Chillies", "Coarse Black Pepper", "Salt"),
            recipeSteps = listOf(
                "Fry mutton in hot animal fat or ghee with ginger-garlic and salt until browned.",
                "Cover with tomato halves; steam until skins peel off easily.",
                "Bhunify on roaring high flame with green chillies and fresh crushed black pepper."
            ),
            chefTip = "Authentic Shinwari contains zero onions, turmeric, or red chilli; black pepper and tomatoes only.",
            tags = listOf("Special", "Karahi", "Peshawari", "Gosht", "Meat Lover")
        ),
        Dish(
            id = "pk_58",
            name = "Chicken White Handi",
            cuisine = CuisineType.PAKISTANI,
            country = "Pakistan",
            mealType = MealType.DINNER,
            dayType = DayType.SPECIAL,
            season = "All",
            isSpecial = true,
            prepTimeMinutes = 35,
            difficulty = "Medium",
            servings = 4,
            ingredients = listOf("Boneless Chicken Cubes", "Cashew & Almond Paste", "Heavy Cream", "Yogurt", "White Pepper", "Kasuri Methi", "Butter"),
            recipeSteps = listOf(
                "Sauté boneless chicken in butter with ginger-garlic and white pepper.",
                "Pour in cashew paste and whisked yogurt, simmer until chicken is tender.",
                "Stir in fresh heavy cream and crushed kasuri methi for a silky velvet sauce."
            ),
            chefTip = "Keep heat very low when adding cream to prevent sauce from curdling.",
            tags = listOf("Special", "Handi", "Creamy", "Restaurant Favorite")
        ),
        Dish(
            id = "pk_59",
            name = "Balochi Sajji with Spiced Yakhni Rice",
            cuisine = CuisineType.PAKISTANI,
            country = "Pakistan",
            mealType = MealType.DINNER,
            dayType = DayType.SPECIAL,
            season = "All",
            isSpecial = true,
            prepTimeMinutes = 70,
            difficulty = "Hard",
            servings = 5,
            ingredients = listOf("Whole Chicken or Lamb Leg", "Rock Salt", "Ajwain & Cumin", "Chaat Masala", "Lemons", "Spiced Basmati Rice"),
            recipeSteps = listOf(
                "Marinate meat simply in rock salt, garlic paste, and lemon juice.",
                "Roast on slow embers or skewers until skin is crispy crackling and interior is ultra-juicy.",
                "Dust generously with special Balochi chat masala and serve over aromatic rice."
            ),
            chefTip = "Continuous basting with oil and lemon keeps the exterior crackling while inside remains succulent.",
            tags = listOf("Special", "BBQ", "Balochi", "Weekend Feast")
        ),
        Dish(
            id = "pk_60",
            name = "Lahori Chargha",
            cuisine = CuisineType.PAKISTANI,
            country = "Pakistan",
            mealType = MealType.DINNER,
            dayType = DayType.SPECIAL,
            season = "All",
            isSpecial = true,
            prepTimeMinutes = 55,
            difficulty = "Medium",
            servings = 4,
            ingredients = listOf("Whole Skinless Chicken with Deep Cuts", "Yogurt & Lemon Marinade", "Chargha Spice Blend", "Chaat Masala", "Ghee for Frying"),
            recipeSteps = listOf(
                "Make deep diagonal slits across the whole chicken and coat in fiery spiced yogurt marinade.",
                "Steam the chicken until 90% cooked through.",
                "Deep fry in smoking hot ghee for 4 minutes until exterior turns crispy deep golden.",
                "Sprinkle with Lahori chaat masala and lemon juice."
            ),
            chefTip = "Steaming first ensures the chicken stays juicy throughout without any raw spots near the bone.",
            tags = listOf("Special", "Fried Chicken", "Lahori Special", "Crowd Pleaser")
        ),
        Dish(
            id = "pk_61",
            name = "Mutton Dum Pukht",
            cuisine = CuisineType.PAKISTANI,
            country = "Pakistan",
            mealType = MealType.DINNER,
            dayType = DayType.SPECIAL,
            season = "All",
            isSpecial = true,
            prepTimeMinutes = 110,
            difficulty = "Hard",
            servings = 6,
            ingredients = listOf("Mutton Chunks with Bone", "Animal Fat / Ghee", "Whole Potatoes", "Whole Garlic Bulbs", "Tomatoes", "Black Peppercorns", "Dough Seal"),
            recipeSteps = listOf(
                "Layer animal fat, mutton pieces, peeled whole potatoes, garlic bulbs, and tomatoes in a heavy pot.",
                "Season with only salt and whole black peppercorns.",
                "Seal lid with flour dough and slow cook on gentle flame for 2.5 hours."
            ),
            chefTip = "Zero water added: meat and potatoes cook entirely in their own natural juices and rendered fat.",
            tags = listOf("Special", "Gosht", "KPK", "Traditional", "Slow Cook")
        ),
        Dish(
            id = "pk_62",
            name = "Mutton Champ Masala (Spiced Rib Chops)",
            cuisine = CuisineType.PAKISTANI,
            country = "Pakistan",
            mealType = MealType.DINNER,
            dayType = DayType.SPECIAL,
            season = "All",
            isSpecial = true,
            prepTimeMinutes = 50,
            difficulty = "Medium",
            servings = 4,
            ingredients = listOf("Mutton Rib Chops (Champs)", "Yogurt", "Raw Papaya Paste", "Ginger-Garlic", "Garam Masala", "Butter"),
            recipeSteps = listOf(
                "Tenderize mutton chops with raw papaya, yogurt, and spices for 1 hour.",
                "Pan-fry chops with butter until caramelized crust develops.",
                "Bhunify with masala gravy until chops are glazed and fork-tender."
            ),
            chefTip = "Raw papaya paste tenderizes the chops so they dissolve effortlessly in your mouth.",
            tags = listOf("Special", "Gosht", "Chops", "BBQ", "Dawat")
        ),
        Dish(
            id = "pk_63",
            name = "Maghaz Masala Fry (Spiced Brain Masala)",
            cuisine = CuisineType.PAKISTANI,
            country = "Pakistan",
            mealType = MealType.DINNER,
            dayType = DayType.SPECIAL,
            season = "All",
            isSpecial = true,
            prepTimeMinutes = 25,
            difficulty = "Medium",
            servings = 3,
            ingredients = listOf("Cleaned Lamb Brain (Maghaz)", "Tomatoes", "Onions", "Ginger Juliennes", "Green Chillies", "Butter / Ghee", "Fenugreek"),
            recipeSteps = listOf(
                "Blanch brain in water with turmeric, clean delicate veins thoroughly.",
                "Sauté fine onions, tomatoes, and ginger in butter on a flat tawa.",
                "Add brain, gently chop with spatulas into bite-sized pieces, fold in masala on medium heat for 8 minutes."
            ),
            chefTip = "Handle brain gently while cooking on the tawa so pieces stay soft and distinct.",
            tags = listOf("Special", "Gosht", "Delicacy", "Karachi Dhaba")
        ),
        Dish(
            id = "pk_64",
            name = "Seekh Kabab Handi Gravy",
            cuisine = CuisineType.PAKISTANI,
            country = "Pakistan",
            mealType = MealType.DINNER,
            dayType = DayType.SPECIAL,
            season = "All",
            isSpecial = true,
            prepTimeMinutes = 40,
            difficulty = "Medium",
            servings = 4,
            ingredients = listOf("Grilled Beef / Chicken Seekh Kababs", "Tomato-Yogurt Gravy", "Kasuri Methi", "Ginger Juliennes", "Fresh Cream", "Coal Smoke"),
            recipeSteps = listOf(
                "Cut grilled seekh kababs into 2-inch pieces.",
                "Cook a rich makhani-style tomato yogurt gravy with aromatic spices.",
                "Fold in grilled kabab pieces, drizzle fresh cream, and smoke with glowing charcoal for 2 minutes."
            ),
            chefTip = "Charcoal smoke elevates the entire handi with genuine tandoor flavor.",
            tags = listOf("Special", "BBQ & Handi", "Dawat", "Smoky")
        ),
        Dish(
            id = "pk_65",
            name = "Chicken Malai Boti Platter",
            cuisine = CuisineType.PAKISTANI,
            country = "Pakistan",
            mealType = MealType.DINNER,
            dayType = DayType.SPECIAL,
            season = "All",
            isSpecial = true,
            prepTimeMinutes = 40,
            difficulty = "Easy",
            servings = 4,
            ingredients = listOf("Boneless Chicken Thighs", "Heavy Cream", "Greek Yogurt", "Green Chilli Paste", "Cardamom Powder", "Garlic Mayo", "Puri Paratha"),
            recipeSteps = listOf(
                "Marinate chicken cubes in cream, yogurt, white pepper, and green chilli paste for 2 hours.",
                "Grill on skewers until tender with charred edges.",
                "Serve with garlic mayo, crisp onion rings, and flaky puri parathas."
            ),
            chefTip = "Use chicken thigh meat instead of breast meat; it stays infinitely juicier on the grill.",
            tags = listOf("Special", "BBQ", "Kids Favorite", "Melt In Mouth")
        ),
        Dish(
            id = "pk_66",
            name = "Bihari Kabab with Puri Paratha",
            cuisine = CuisineType.PAKISTANI,
            country = "Pakistan",
            mealType = MealType.DINNER,
            dayType = DayType.SPECIAL,
            season = "All",
            isSpecial = true,
            prepTimeMinutes = 45,
            difficulty = "Medium",
            servings = 4,
            ingredients = listOf("Paper Thin Beef Strips (Pasanday)", "Raw Papaya", "Mustard Oil", "Fried Onions", "Bihari Spice Blend", "Charcoal"),
            recipeSteps = listOf(
                "Marinate thin beef strips in raw papaya, pungent mustard oil, crushed fried onions, and roasted spices for 4 hours.",
                "Thread onto flat skewers like ribbons and grill on charcoal until charred and ultra-tender."
            ),
            chefTip = "Pure mustard oil gives Bihari kabab its authentic smoky sharpness.",
            tags = listOf("Special", "BBQ", "Beef Lover", "Karachi Famous")
        ),
        Dish(
            id = "pk_67",
            name = "Lahori Tawa Fish",
            cuisine = CuisineType.PAKISTANI,
            country = "Pakistan",
            mealType = MealType.DINNER,
            dayType = DayType.SPECIAL,
            season = "Winter",
            isSpecial = true,
            prepTimeMinutes = 30,
            difficulty = "Easy",
            servings = 4,
            ingredients = listOf("Fresh Fish Fillets (Surmai or Rahu)", "Carom Seeds (Ajwain)", "Gram Flour (Besan)", "Crushed Coriander", "Pomegranate Seeds", "Lemon Juice"),
            recipeSteps = listOf(
                "Rub fish fillets with ajwain, crushed coriander, chilli flakes, and lemon juice.",
                "Dust lightly in seasoned gram flour batter.",
                "Shallow fry on a cast-iron tawa in hot oil until golden crisp outside and flaky inside."
            ),
            chefTip = "Ajwain (carom seeds) is the secret soul of Lahori fried fish.",
            tags = listOf("Special", "Seafood", "Winter Classic", "Crispy")
        ),
        Dish(
            id = "pk_68",
            name = "Royal Kofta Curry with Boiled Eggs",
            cuisine = CuisineType.PAKISTANI,
            country = "Pakistan",
            mealType = MealType.DINNER,
            dayType = DayType.SPECIAL,
            season = "All",
            isSpecial = true,
            prepTimeMinutes = 55,
            difficulty = "Hard",
            servings = 5,
            ingredients = listOf("Fine Beef Mince", "Roasted Gram (Bhuna Chana)", "Poppy Seeds (Khaskhas)", "Yogurt Gravy", "Hard Boiled Eggs", "Kewra"),
            recipeSteps = listOf(
                "Grind mince with roasted chana powder, poppy seeds, onions, and spices into a silky paste.",
                "Shape into smooth crack-free meatballs (koftay).",
                "Simmer gently in rich yogurt onion gravy without stirring vigorously with spoons; swirl the pot.",
                "Garnish with halved boiled eggs and drops of kewra."
            ),
            chefTip = "Swirl the pot by its handles rather than stirring with a spoon to prevent tender meatballs from breaking.",
            tags = listOf("Special", "Gosht", "Royal Mughlai", "Dawat Classic")
        ),
        Dish(
            id = "pk_69",
            name = "Sindhi Biryani (with Spicy Aloo & Dried Plums)",
            cuisine = CuisineType.PAKISTANI,
            country = "Pakistan",
            mealType = MealType.DINNER,
            dayType = DayType.NORMAL,
            season = "All",
            isSpecial = false,
            prepTimeMinutes = 60,
            difficulty = "Medium",
            servings = 6,
            ingredients = listOf("Basmati Sella / Basmati Rice", "Chicken or Mutton", "Potatoes (Aloo)", "Sour Plums (Aloo Bukhara)", "Yogurt", "Green Chillies", "Fresh Mint", "Saffron Kewra"),
            recipeSteps = listOf(
                "Marinate meat in tangy yogurt, crushed mint, green chillies, dried plums, and authentic Sindhi biryani spices.",
                "Simmer gravy until meat is tender and potatoes absorb the deep tangy-spicy masala.",
                "Parboil long-grain basmati rice with whole spices, star anise, and mint.",
                "Layer rice over the rich Sindhi masala, sprinkle fried onions and kewra, and steam on dum for 20 minutes."
            ),
            chefTip = "Dried sour plums (Aloo Bukhara) and green chillies give Sindhi Biryani its distinctive spicy-tangy kick.",
            tags = listOf("Biryani", "Sindhi", "Dinner", "Spicy", "Popular")
        ),
        Dish(
            id = "pk_70",
            name = "Afghani Pulao (Kabuli Pulao with Meat & Sweet Carrots)",
            cuisine = CuisineType.PAKISTANI,
            country = "Pakistan",
            mealType = MealType.DINNER,
            dayType = DayType.NORMAL,
            season = "All",
            isSpecial = false,
            prepTimeMinutes = 55,
            difficulty = "Medium",
            servings = 5,
            ingredients = listOf("Mutton or Beef Shanks", "Long-Grain Sella Rice", "Carrot Juliennes", "Black Raisins (Kishmish)", "Cardamom", "Desi Ghee", "Sugar"),
            recipeSteps = listOf(
                "Simmer meat with whole onion and garlic until tender to create a clear fragrant yakhni broth.",
                "Caramelize carrot juliennes and black raisins in desi ghee with a pinch of sugar.",
                "Cook soaked rice in the seasoned meat broth until liquid is absorbed.",
                "Crown the rice with tender meat and the glazed carrots and raisins, then seal for 15 minutes on low dum."
            ),
            chefTip = "Sauté the raisins until they puff like pearls; their sweetness perfectly balances the savory meat broth.",
            tags = listOf("Pulao", "Afghani", "Peshawari", "Royal", "Dinner")
        ),
        Dish(
            id = "pk_71",
            name = "Chicken Tikka Stuffed Crust Pizza",
            cuisine = CuisineType.ITALIAN,
            country = "Pakistan",
            mealType = MealType.DINNER,
            dayType = DayType.NORMAL,
            season = "All",
            isSpecial = false,
            prepTimeMinutes = 35,
            difficulty = "Easy",
            servings = 4,
            ingredients = listOf("Pizza Dough", "Smoky Chicken Tikka Chunks", "Mozzarella & Cheddar Cheese", "Spicy Pizza Sauce", "Bell Peppers", "Onions", "Oregano"),
            recipeSteps = listOf(
                "Roll out fresh pizza dough and line the crust edges with mozzarella cheese sticks, folding over to seal stuffed crust.",
                "Spread spicy herbed tomato pizza sauce over the base.",
                "Top generously with shredded cheese, charcoal-smoked chicken tikka cubes, sliced onions, and green bell peppers.",
                "Bake at 220°C (430°F) for 12-15 minutes until the cheese is bubbling golden and crust is crispy."
            ),
            chefTip = "Preheat your baking tray or stone so the bottom crust turns perfectly crisp and never soggy.",
            tags = listOf("Pizza", "Fast Food", "Dinner", "Kids Favorite", "Tikka")
        ),
        Dish(
            id = "pk_72",
            name = "Crispy Zinger Burger with Seasoned Fries",
            cuisine = CuisineType.CONTINENTAL,
            country = "Pakistan",
            mealType = MealType.DINNER,
            dayType = DayType.NORMAL,
            season = "All",
            isSpecial = false,
            prepTimeMinutes = 30,
            difficulty = "Easy",
            servings = 3,
            ingredients = listOf("Chicken Thigh Fillets", "Buttermilk Marinade", "Spiced Flour Dredge", "Toasted Sesame Buns", "Crisp Iceberg Lettuce", "Garlic Pepper Mayo", "French Fries"),
            recipeSteps = listOf(
                "Marinate chicken fillets in seasoned buttermilk with paprika, garlic, and hot sauce for 1 hour.",
                "Dredge in seasoned flour, dip in ice-cold water, and dredge again to create flaky zinger ridges.",
                "Deep fry on medium heat for 7-8 minutes until crunchy golden brown.",
                "Assemble on warm buttered buns with garlic mayo and shredded lettuce; serve with salted fries."
            ),
            chefTip = "The ice-water dip technique creates the ultra-crispy, signature rippled zinger crust.",
            tags = listOf("Burger", "Fast Food", "Street Food", "Dinner", "Crispy")
        ),
        Dish(
            id = "pk_73",
            name = "Chicken Chow Mein (Hakka Street Style)",
            cuisine = CuisineType.CHINESE,
            country = "Pakistan",
            mealType = MealType.DINNER,
            dayType = DayType.NORMAL,
            season = "All",
            isSpecial = false,
            prepTimeMinutes = 25,
            difficulty = "Easy",
            servings = 3,
            ingredients = listOf("Egg Noodles", "Boneless Chicken Strips", "Shredded Cabbage", "Carrot Juliennes", "Capsicum", "Soy Sauce", "Chilli Garlic Sauce", "Sesame Oil"),
            recipeSteps = listOf(
                "Boil egg noodles al dente, rinse under cold water, and toss in a drop of oil.",
                "Stir-fry chicken strips in a smoking hot wok with minced garlic until golden.",
                "Toss in crunchy shredded vegetables on roaring high heat for 1 minute.",
                "Add noodles and wok sauce (soy, oyster, vinegar, chilli garlic); toss vigorously until smoky and glossy."
            ),
            chefTip = "Keep the wok intensely hot and stir-fry in small batches to achieve restaurant wok hei (breath of wok).",
            tags = listOf("Chinese", "Noodles", "Quick Dinner", "Street Food")
        ),
        Dish(
            id = "pk_74",
            name = "Peshawari Shinwari Chicken Karahi",
            cuisine = CuisineType.PAKISTANI,
            country = "Pakistan",
            mealType = MealType.DINNER,
            dayType = DayType.NORMAL,
            season = "All",
            isSpecial = false,
            prepTimeMinutes = 30,
            difficulty = "Easy",
            servings = 4,
            ingredients = listOf("Chicken with Bone", "Fresh Red Tomatoes", "Ginger Juliennes", "Green Chillies", "Crushed Black Pepper", "Rock Salt", "Desi Ghee"),
            recipeSteps = listOf(
                "Sear chicken pieces in hot desi ghee with garlic paste and salt until golden.",
                "Halve fresh tomatoes and place cut-side down over the chicken; cover to steam for 5 minutes.",
                "Peel off softened tomato skins and crush into a thick, glossy gravy on high flame.",
                "Finish with coarse freshly ground black pepper, slit green chillies, and ginger juliennes."
            ),
            chefTip = "Traditional Shinwari contains no onions or packaged spices; sweet tomatoes and fresh black pepper create all the magic.",
            tags = listOf("Karahi", "Shinwari", "Dinner", "Peshawari", "Desi")
        ),
        Dish(
            id = "pk_75",
            name = "Creamy Fettuccine Alfredo with Grilled Chicken",
            cuisine = CuisineType.ITALIAN,
            country = "Pakistan",
            mealType = MealType.DINNER,
            dayType = DayType.NORMAL,
            season = "All",
            isSpecial = false,
            prepTimeMinutes = 25,
            difficulty = "Easy",
            servings = 3,
            ingredients = listOf("Fettuccine Pasta", "Heavy Cream", "Butter", "Parmesan Cheese", "Garlic", "Grilled Herb Chicken Breast", "Cracked Black Pepper"),
            recipeSteps = listOf(
                "Boil fettuccine pasta in salted water until al dente; reserve 1/2 cup pasta water.",
                "Melt butter in a pan with minced garlic and gently pour in fresh heavy cream.",
                "Stir in grated Parmesan until smooth and velvety, adding splashes of pasta water to emulsify.",
                "Toss with warm fettuccine and crown with sliced tender grilled chicken breast."
            ),
            chefTip = "Emulsify with starchy pasta water to keep the Alfredo sauce silky without feeling overly heavy.",
            tags = listOf("Pasta", "Italian", "Dinner", "Creamy", "Popular")
        )
    )

    // -------------------------------------------------------------
    // 2. 15 INDIAN DISHES
    // -------------------------------------------------------------
    val indianDishes = listOf(
        Dish(
            id = "in_1",
            name = "Paneer Butter Masala with Butter Naan",
            cuisine = CuisineType.INDIAN,
            country = "India",
            mealType = MealType.DINNER,
            dayType = DayType.SPECIAL,
            season = "All",
            isSpecial = true,
            prepTimeMinutes = 35,
            difficulty = "Medium",
            servings = 4,
            ingredients = listOf("Paneer Cubes", "Tomatoes", "Cashew Paste", "Butter", "Fresh Cream", "Kasuri Methi", "Garam Masala"),
            recipeSteps = listOf(
                "Boil tomatoes and cashews, then blend into a velvety smooth makhani puree.",
                "Cook puree in generous butter with aromatic spices until fragrant.",
                "Add soft paneer cubes, fresh cream, and crushed kasuri methi; simmer for 5 minutes."
            ),
            chefTip = "Soak paneer cubes in warm water for 10 minutes before adding to keep them pillow-soft.",
            tags = listOf("Dinner", "Vegetarian", "Rich", "North Indian")
        ),
        Dish(
            id = "in_2",
            name = "Masala Dosa with Sambar & Coconut Chutney",
            cuisine = CuisineType.INDIAN,
            country = "India",
            mealType = MealType.BREAKFAST,
            dayType = DayType.NORMAL,
            season = "All",
            isSpecial = false,
            prepTimeMinutes = 30,
            difficulty = "Medium",
            servings = 3,
            ingredients = listOf("Fermented Rice-Lentil Batter", "Spiced Mashed Potatoes", "Mustard Seeds", "Curry Leaves", "Ghee", "Toor Dal Sambar"),
            recipeSteps = listOf(
                "Spread fermented batter thinly on a hot tawa with a swirl of ghee until paper crisp.",
                "Place savory spiced mustard potato filling in the center and fold.",
                "Serve hot with piping hot vegetable sambar and fresh coconut chutney."
            ),
            chefTip = "Wipe tawa with a wet cloth before pouring batter to regulate surface temperature.",
            tags = listOf("Breakfast", "South Indian", "Crispy", "Healthy")
        ),
        Dish(
            id = "in_3",
            name = "Delhi Chole Bhature with Pickled Onions",
            cuisine = CuisineType.INDIAN,
            country = "India",
            mealType = MealType.LUNCH,
            dayType = DayType.SPECIAL,
            season = "All",
            isSpecial = true,
            prepTimeMinutes = 50,
            difficulty = "Medium",
            servings = 4,
            ingredients = listOf("Kabuli Chickpeas", "Tea Bag for Dark Color", "Anardana (Pomegranate)", "Fermented Bhatura Dough", "Ghee", "Pickles"),
            recipeSteps = listOf(
                "Boil chickpeas with tea bag and whole spices to get signature dark color.",
                "Bhunify with onion-tomato masala, anardana, and chole masala.",
                "Roll rested dough and deep fry in smoking hot oil until puffed into balloon bhature."
            ),
            chefTip = "Add a dollop of yogurt to bhatura dough and let it ferment for 2 hours for maximum puff.",
            tags = listOf("Lunch", "Street Food", "Punjab Special", "Indulgent")
        ),
        Dish(
            id = "in_4",
            name = "Palak Paneer with Makki / Wheat Roti",
            cuisine = CuisineType.INDIAN,
            country = "India",
            mealType = MealType.LUNCH,
            dayType = DayType.NORMAL,
            season = "Winter",
            isSpecial = false,
            prepTimeMinutes = 30,
            difficulty = "Easy",
            servings = 4,
            ingredients = listOf("Fresh Spinach (Palak)", "Paneer Cubes", "Garlic", "Green Chillies", "Ginger", "Fresh Cream", "Cumin"),
            recipeSteps = listOf(
                "Blanch spinach in boiling water for 2 minutes, then plunge into ice water to preserve vibrant green.",
                "Puree with green chillies and garlic.",
                "Simmer with cumin, light spices, paneer cubes, and finish with a swirl of cream."
            ),
            chefTip = "Never cover the pan after adding spinach puree to maintain the lush green color.",
            tags = listOf("Lunch", "Healthy", "Vegetarian", "Iron Rich")
        ),
        Dish(
            id = "in_5",
            name = "Dal Makhani with Jeera Rice",
            cuisine = CuisineType.INDIAN,
            country = "India",
            mealType = MealType.DINNER,
            dayType = DayType.SPECIAL,
            season = "All",
            isSpecial = true,
            prepTimeMinutes = 60,
            difficulty = "Medium",
            servings = 5,
            ingredients = listOf("Whole Black Urad Dal", "Kidney Beans (Rajma)", "Butter", "Fresh Cream", "Tomato Puree", "Kashmiri Mirch", "Kasuri Methi"),
            recipeSteps = listOf(
                "Slow cook soaked black lentils and rajma for hours until melt-in-mouth soft.",
                "Simmer with tomato puree, Kashmiri chilli, and generous knobs of white butter.",
                "Finish with heavy cream and crushed kasuri methi."
            ),
            chefTip = "The longer Dal Makhani slow-simmers on low flame, the creamier and richer it tastes.",
            tags = listOf("Dinner", "Restaurant Style", "Vegetarian", "North Indian")
        ),
        Dish(
            id = "in_6",
            name = "Butter Chicken (Murgh Makhani)",
            cuisine = CuisineType.INDIAN,
            country = "India",
            mealType = MealType.DINNER,
            dayType = DayType.SPECIAL,
            season = "All",
            isSpecial = true,
            prepTimeMinutes = 45,
            difficulty = "Medium",
            servings = 4,
            ingredients = listOf("Tandoori Chicken Tikka", "Tomato-Cashew Puree", "Butter", "Cream", "Honey / Sugar", "Kasuri Methi"),
            recipeSteps = listOf(
                "Grill marinated chicken pieces with smoky char marks.",
                "Simmer silky tomato-cashew makhani gravy with butter and a dash of honey.",
                "Combine chicken tikka into the gravy and finish with cream."
            ),
            chefTip = "A touch of honey balances the acidity of tomatoes perfectly in makhani sauce.",
            tags = listOf("Dinner", "Classic", "Non-Veg", "Global Favorite")
        ),
        Dish(
            id = "in_7",
            name = "Punjabi Rajma Chawal",
            cuisine = CuisineType.INDIAN,
            country = "India",
            mealType = MealType.LUNCH,
            dayType = DayType.NORMAL,
            season = "All",
            isSpecial = false,
            prepTimeMinutes = 40,
            difficulty = "Easy",
            servings = 4,
            ingredients = listOf("Red Kidney Beans (Rajma)", "Onions", "Tomatoes", "Ginger-Garlic", "Coriander", "Basmati Rice"),
            recipeSteps = listOf(
                "Pressure cook soaked red kidney beans until soft.",
                "Prepare rich onion-tomato masala and mash a few beans to thicken gravy.",
                "Simmer together and serve steaming hot over fragrant basmati rice."
            ),
            chefTip = "Mash 2 tablespoons of cooked rajma into the gravy for natural thickness without cornstarch.",
            tags = listOf("Lunch", "Everyday", "Vegetarian", "Comfort Food")
        ),
        Dish(
            id = "in_8",
            name = "Aloo Paratha with Curd & Mango Pickle",
            cuisine = CuisineType.INDIAN,
            country = "India",
            mealType = MealType.BREAKFAST,
            dayType = DayType.NORMAL,
            season = "All",
            isSpecial = false,
            prepTimeMinutes = 25,
            difficulty = "Easy",
            servings = 2,
            ingredients = listOf("Wheat Dough", "Boiled Mashed Potatoes", "Green Chillies", "Amchur", "Coriander", "White Butter", "Curd"),
            recipeSteps = listOf(
                "Mix mashed potatoes with green chillies, dry mango powder, and coriander.",
                "Stuff inside wheat dough ball, roll gently without tearing.",
                "Cook on tawa with ghee until crisp and golden brown; serve with dollop of white butter."
            ),
            chefTip = "Cool the mashed potato stuffing completely before rolling to prevent dough from tearing.",
            tags = listOf("Breakfast", "Everyday", "North Indian")
        ),
        Dish(
            id = "in_9",
            name = "Mumbai Pav Bhaji",
            cuisine = CuisineType.INDIAN,
            country = "India",
            mealType = MealType.EVENING_SNACKS,
            dayType = DayType.NORMAL,
            season = "All",
            isSpecial = false,
            prepTimeMinutes = 30,
            difficulty = "Easy",
            servings = 4,
            ingredients = listOf("Mixed Veggies (Potatoes, Cauliflower, Peas)", "Tomatoes", "Pav Bhaji Masala", "Lots of Butter", "Ladi Pav Buns", "Lemon"),
            recipeSteps = listOf(
                "Boil and mash vegetables smoothly.",
                "Cook with finely chopped onions, capsicum, tomatoes, pav bhaji masala, and butter on a wide tawa.",
                "Toast pav buns with butter and coriander; serve with lemon wedges."
            ),
            chefTip = "Cook on a wide flat tawa with continuous mashing for the authentic Mumbai street texture.",
            tags = listOf("Street Food", "Snacks", "Mumbai", "Family Favorite")
        ),
        Dish(
            id = "in_10",
            name = "Steamed Idli & Medu Vada",
            cuisine = CuisineType.INDIAN,
            country = "India",
            mealType = MealType.BREAKFAST,
            dayType = DayType.NORMAL,
            season = "All",
            isSpecial = false,
            prepTimeMinutes = 25,
            difficulty = "Medium",
            servings = 3,
            ingredients = listOf("Idli Batter", "Urad Dal Vada Batter", "Curry Leaves", "Ginger", "Coconut Chutney", "Sambar"),
            recipeSteps = listOf(
                "Steam fermented idli batter in moulds for 10 minutes until fluffy.",
                "Shape urad dal batter into doughnuts and deep fry until golden crisp.",
                "Serve warm with freshly ground coconut chutney and hot sambar."
            ),
            chefTip = "Whisk the urad dal batter vigorously to incorporate air before frying crisp vadas.",
            tags = listOf("Breakfast", "South Indian", "Healthy", "Steamed")
        ),
        Dish(
            id = "in_11",
            name = "Hyderabadi Vegetable Dum Biryani",
            cuisine = CuisineType.INDIAN,
            country = "India",
            mealType = MealType.LUNCH,
            dayType = DayType.NORMAL,
            season = "All",
            isSpecial = false,
            prepTimeMinutes = 45,
            difficulty = "Medium",
            servings = 4,
            ingredients = listOf("Basmati Rice", "Carrots", "Beans", "Paneer", "Yogurt", "Biryani Spices", "Fried Onions", "Mint"),
            recipeSteps = listOf(
                "Marinate vegetables in spiced yogurt, mint, and fried onions.",
                "Layer parboiled basmati rice over vegetables in a sealed heavy pot.",
                "Dum cook on low heat for 20 minutes until rice grains are separate and aromatic."
            ),
            chefTip = "Seal the pot with dough around the rim for authentic airtight dum cooking.",
            tags = listOf("Lunch", "Biryani", "Vegetarian")
        ),
        Dish(
            id = "in_12",
            name = "Kadai Paneer with Missi Roti",
            cuisine = CuisineType.INDIAN,
            country = "India",
            mealType = MealType.DINNER,
            dayType = DayType.NORMAL,
            season = "All",
            isSpecial = false,
            prepTimeMinutes = 30,
            difficulty = "Easy",
            servings = 3,
            ingredients = listOf("Paneer Cubes", "Bell Peppers (Capsicum)", "Onion Chunks", "Kadai Masala (Coriander & Red Chillies)", "Tomato Gravy"),
            recipeSteps = listOf(
                "Freshly roast and crush whole coriander seeds and dry red chillies.",
                "Toss crunchy capsicum and onion petals with the kadai spice blend.",
                "Add tomato masala and paneer cubes; cook on high flame for 5 minutes."
            ),
            chefTip = "Do not overcook bell peppers; keep them crunchy for signature kadai texture.",
            tags = listOf("Dinner", "Vegetarian", "Spicy")
        ),
        Dish(
            id = "in_13",
            name = "Kolkata Rasgulla",
            cuisine = CuisineType.DESSERT,
            country = "India",
            mealType = MealType.DINNER,
            dayType = DayType.SPECIAL,
            season = "All",
            isSpecial = true,
            prepTimeMinutes = 40,
            difficulty = "Medium",
            servings = 5,
            ingredients = listOf("Fresh Chhena (Cow Milk Paneer)", "Semolina", "Light Sugar Syrup", "Rose Water", "Cardamom"),
            recipeSteps = listOf(
                "Knead fresh homemade chhena smoothly until completely non-grainy.",
                "Roll into small balls and boil in boiling light sugar syrup for 15 minutes.",
                "Watch them double in size; cool and serve chilled."
            ),
            chefTip = "Knead chhena with the heel of your palm for 10 minutes until oil starts releasing.",
            tags = listOf("Dessert", "Bengali Sweet", "Spongy", "Light")
        ),
        Dish(
            id = "in_14",
            name = "Kesar Rasmalai",
            cuisine = CuisineType.DESSERT,
            country = "India",
            mealType = MealType.DINNER,
            dayType = DayType.SPECIAL,
            season = "All",
            isSpecial = true,
            prepTimeMinutes = 45,
            difficulty = "Medium",
            servings = 6,
            ingredients = listOf("Chhena Discs", "Reduced Saffron Milk (Rabri)", "Cardamom", "Pistachios", "Almonds"),
            recipeSteps = listOf(
                "Boil flattened chhena discs in sugar syrup, then squeeze gently.",
                "Simmer milk with saffron and cardamom until reduced to a fragrant creamy rabri.",
                "Soak chhena discs in warm rabri and chill thoroughly before serving."
            ),
            chefTip = "Soak the discs while the rabri is still warm so they absorb the saffron milk fully.",
            tags = listOf("Dessert", "Royal", "Festive", "Chilled")
        ),
        Dish(
            id = "in_15",
            name = "Kaju Katli (Cashew Fudge)",
            cuisine = CuisineType.DESSERT,
            country = "India",
            mealType = MealType.EVENING_SNACKS,
            dayType = DayType.SPECIAL,
            season = "All",
            isSpecial = true,
            prepTimeMinutes = 30,
            difficulty = "Medium",
            servings = 6,
            ingredients = listOf("Cashew Nut Powder", "Sugar Syrup", "Ghee", "Silver Leaf"),
            recipeSteps = listOf(
                "Grind cashews into a fine dry powder without making it oily.",
                "Cook cashew powder in 1-string sugar syrup on low heat until dough forms.",
                "Roll between parchment paper, cut into diamond shapes, and garnish with silver leaf."
            ),
            chefTip = "Pulse cashews in short bursts in the grinder to avoid releasing cashew oil.",
            tags = listOf("Dessert", "Diwali Special", "Gift", "Mithai")
        )
    )

    // -------------------------------------------------------------
    // 3. 10 CHINESE DISHES
    // -------------------------------------------------------------
    val chineseDishes = listOf(
        Dish(
            id = "cn_1",
            name = "Kung Pao Chicken with Steamed Rice",
            cuisine = CuisineType.CHINESE,
            country = "China",
            mealType = MealType.DINNER,
            dayType = DayType.NORMAL,
            season = "All",
            isSpecial = false,
            prepTimeMinutes = 25,
            difficulty = "Medium",
            servings = 3,
            ingredients = listOf("Chicken Cubes", "Roasted Peanuts", "Dried Red Chillies", "Sichuan Pepper", "Soy Sauce", "Rice Vinegar", "Scallions"),
            recipeSteps = listOf(
                "Marinate chicken in soy sauce and cornstarch, then stir-fry in a hot wok.",
                "Fragrance dried chillies and Sichuan peppercorns in oil.",
                "Toss chicken with sweet-tangy sauce and crunchy peanuts."
            ),
            chefTip = "Use a screaming hot wok to get authentic wok hei (breath of the wok).",
            tags = listOf("Chinese", "Dinner", "Spicy", "Nutty")
        ),
        Dish(
            id = "cn_2",
            name = "Chicken Manchurian with Egg Fried Rice",
            cuisine = CuisineType.CHINESE,
            country = "China",
            mealType = MealType.DINNER,
            dayType = DayType.NORMAL,
            season = "All",
            isSpecial = false,
            prepTimeMinutes = 30,
            difficulty = "Easy",
            servings = 4,
            ingredients = listOf("Crispy Fried Chicken Bites", "Garlic", "Ginger", "Soy Sauce", "Chilli Sauce", "Tomato Ketchup", "Spring Onions", "Fried Rice"),
            recipeSteps = listOf(
                "Coat chicken cubes in batter and fry until crunchy.",
                "Sauté minced garlic and ginger, then add sauces and cornstarch slurry to create glossy red sauce.",
                "Toss fried chicken and scallions in the sauce; serve with hot egg fried rice."
            ),
            chefTip = "Double fry the chicken for extra crunch that stays crisp inside the sauce.",
            tags = listOf("Chinese", "Desi Chinese", "Dinner", "Popular")
        ),
        Dish(
            id = "cn_3",
            name = "Vegetable Hakka Chow Mein",
            cuisine = CuisineType.CHINESE,
            country = "China",
            mealType = MealType.LUNCH,
            dayType = DayType.NORMAL,
            season = "All",
            isSpecial = false,
            prepTimeMinutes = 20,
            difficulty = "Easy",
            servings = 3,
            ingredients = listOf("Egg / Wheat Noodles", "Shredded Cabbage", "Carrots", "Capsicum", "Dark Soy Sauce", "Vinegar", "Sesame Oil"),
            recipeSteps = listOf(
                "Boil noodles al dente, rinse with cold water, and toss in a drop of oil.",
                "Stir-fry shredded crunchy veggies on high heat in a wok.",
                "Add noodles, soy sauce, and chilli vinegar; toss vigorously for 2 minutes."
            ),
            chefTip = "Rinse boiled noodles in cold water immediately to stop the cooking process.",
            tags = listOf("Lunch", "Noodles", "Vegetarian", "Quick")
        ),
        Dish(
            id = "cn_4",
            name = "Hot and Sour Soup with Crispy Wontons",
            cuisine = CuisineType.CHINESE,
            country = "China",
            mealType = MealType.EVENING_SNACKS,
            dayType = DayType.NORMAL,
            season = "Winter",
            isSpecial = false,
            prepTimeMinutes = 25,
            difficulty = "Easy",
            servings = 4,
            ingredients = listOf("Chicken Broth", "Shredded Chicken", "Mushrooms", "Tofu / Paneer", "White Pepper", "Vinegar", "Soy Sauce", "Egg Ribbons"),
            recipeSteps = listOf(
                "Simmer chicken stock with mushrooms and shredded chicken.",
                "Add soy sauce, vinegar, white pepper, and thicken with cornstarch slurry.",
                "Slowly drizzle whisked egg while gently stirring to create silky egg ribbons."
            ),
            chefTip = "Use white pepper powder instead of black pepper for genuine Chinese heat.",
            tags = listOf("Soup", "Winter", "Appetizer", "Warm")
        ),
        Dish(
            id = "cn_5",
            name = "Beef Chilli Dry with Garlic Fried Rice",
            cuisine = CuisineType.CHINESE,
            country = "China",
            mealType = MealType.DINNER,
            dayType = DayType.SPECIAL,
            season = "All",
            isSpecial = true,
            prepTimeMinutes = 30,
            difficulty = "Medium",
            servings = 3,
            ingredients = listOf("Thin Sliced Beef Undercut", "Green Chillies", "Ginger Juliennes", "Soy Sauce", "Oyster Sauce", "Sesame Seeds"),
            recipeSteps = listOf(
                "Velvet beef slices with soy sauce, egg white, and cornstarch, then flash fry.",
                "Stir-fry sliced green chillies and ginger juliennes in hot oil.",
                "Toss beef with savory glaze and finish with toasted sesame seeds."
            ),
            chefTip = "Slice beef thinly against the grain to ensure super tender bites.",
            tags = listOf("Dinner", "Desi Chinese", "Spicy", "Meat")
        ),
        Dish(
            id = "cn_6",
            name = "Szechuan Chicken with Peppers",
            cuisine = CuisineType.CHINESE,
            country = "China",
            mealType = MealType.DINNER,
            dayType = DayType.NORMAL,
            season = "All",
            isSpecial = false,
            prepTimeMinutes = 25,
            difficulty = "Easy",
            servings = 3,
            ingredients = listOf("Chicken Strips", "Szechuan Pepper Sauce", "Bell Peppers", "Onions", "Garlic", "Chilli Paste"),
            recipeSteps = listOf(
                "Stir-fry chicken strips with minced garlic until cooked.",
                "Add bell pepper chunks and spicy Szechuan sauce.",
                "Toss on high flame until sauce coats the chicken evenly."
            ),
            chefTip = "Crushed Sichuan peppercorns provide the authentic tingling citrusy numbing heat.",
            tags = listOf("Chinese", "Dinner", "Spicy")
        ),
        Dish(
            id = "cn_7",
            name = "Sweet and Sour Fish",
            cuisine = CuisineType.CHINESE,
            country = "China",
            mealType = MealType.DINNER,
            dayType = DayType.SPECIAL,
            season = "All",
            isSpecial = true,
            prepTimeMinutes = 30,
            difficulty = "Medium",
            servings = 4,
            ingredients = listOf("White Fish Fillets", "Pineapple Chunks", "Bell Peppers", "Sweet & Sour Glaze", "Rice Vinegar", "Ketchup"),
            recipeSteps = listOf(
                "Dip fish fillets in crispy batter and deep fry until golden.",
                "Prepare glossy sweet and sour glaze with pineapple juice and vinegar.",
                "Toss crispy fish and pineapple chunks right before serving."
            ),
            chefTip = "Toss the fish in the sauce just before serving so the batter remains crunchy.",
            tags = listOf("Dinner", "Seafood", "Sweet & Sour")
        ),
        Dish(
            id = "cn_8",
            name = "Crispy Spring Rolls with Plum Sauce",
            cuisine = CuisineType.CHINESE,
            country = "China",
            mealType = MealType.EVENING_SNACKS,
            dayType = DayType.NORMAL,
            season = "All",
            isSpecial = false,
            prepTimeMinutes = 30,
            difficulty = "Medium",
            servings = 4,
            ingredients = listOf("Spring Roll Wrappers", "Shredded Chicken", "Cabbage", "Carrots", "Soy Sauce", "Black Pepper"),
            recipeSteps = listOf(
                "Sauté shredded chicken and vegetables with soy sauce and black pepper; cool completely.",
                "Wrap filling tightly inside spring roll pastry sheets and seal with flour paste.",
                "Deep fry until golden brown and super crispy."
            ),
            chefTip = "Ensure vegetable filling has no residual water so rolls stay crisp for hours.",
            tags = listOf("Snacks", "Appetizer", "Crispy", "Party Food")
        ),
        Dish(
            id = "cn_9",
            name = "Steamed Chicken Dumplings (Dim Sum)",
            cuisine = CuisineType.CHINESE,
            country = "China",
            mealType = MealType.EVENING_SNACKS,
            dayType = DayType.NORMAL,
            season = "All",
            isSpecial = false,
            prepTimeMinutes = 35,
            difficulty = "Medium",
            servings = 4,
            ingredients = listOf("Dumpling Wrappers", "Minced Chicken", "Scallions", "Ginger", "Sesame Oil", "Chilli Garlic Dip"),
            recipeSteps = listOf(
                "Season minced chicken with sesame oil, grated ginger, and scallions.",
                "Pleat wrappers around the filling into crescent shapes.",
                "Steam in bamboo steamer for 10 minutes until translucent and juicy."
            ),
            chefTip = "Serve immediately with chili oil and dark vinegar dipping sauce.",
            tags = listOf("Dim Sum", "Healthy", "Steamed", "Snacks")
        ),
        Dish(
            id = "cn_10",
            name = "Crispy Honey Sesame Prawns",
            cuisine = CuisineType.CHINESE,
            country = "China",
            mealType = MealType.DINNER,
            dayType = DayType.SPECIAL,
            season = "All",
            isSpecial = true,
            prepTimeMinutes = 25,
            difficulty = "Easy",
            servings = 3,
            ingredients = listOf("Jumbo Prawns", "Honey", "Soy Sauce", "Toasted Sesame Seeds", "Garlic", "Spring Onion"),
            recipeSteps = listOf(
                "Batter and deep fry prawns until crispy.",
                "Glaze quickly in hot honey-garlic soy reduction.",
                "Garnish with roasted white sesame seeds."
            ),
            chefTip = "Do not overcook prawns in oil; 2 minutes is all they need to stay tender.",
            tags = listOf("Dinner", "Seafood", "Special", "Appetizer")
        )
    )

    // -------------------------------------------------------------
    // 4. 5 ITALIAN DISHES
    // -------------------------------------------------------------
    val italianDishes = listOf(
        Dish(
            id = "it_1",
            name = "Classic Margherita Pizza",
            cuisine = CuisineType.ITALIAN,
            country = "Italy",
            mealType = MealType.DINNER,
            dayType = DayType.SPECIAL,
            season = "All",
            isSpecial = true,
            prepTimeMinutes = 35,
            difficulty = "Medium",
            servings = 4,
            ingredients = listOf("Pizza Dough", "San Marzano Tomato Sauce", "Fresh Mozzarella", "Fresh Basil Leaves", "Extra Virgin Olive Oil"),
            recipeSteps = listOf(
                "Stretch fermented pizza dough into a thin crust.",
                "Spread crushed tomato sauce, tear fresh mozzarella, and drizzle olive oil.",
                "Bake at highest oven temperature until crust is charred and cheese bubbles; top with fresh basil."
            ),
            chefTip = "Bake on a preheated pizza stone at the highest oven temperature for authentic blistered crust.",
            tags = listOf("Pizza", "Italian", "Dinner", "Vegetarian")
        ),
        Dish(
            id = "it_2",
            name = "Fettuccine Alfredo with Grilled Chicken",
            cuisine = CuisineType.ITALIAN,
            country = "Italy",
            mealType = MealType.DINNER,
            dayType = DayType.NORMAL,
            season = "All",
            isSpecial = false,
            prepTimeMinutes = 25,
            difficulty = "Easy",
            servings = 3,
            ingredients = listOf("Fettuccine Pasta", "Heavy Cream", "Butter", "Parmesan Cheese", "Garlic", "Grilled Chicken Breast", "Black Pepper"),
            recipeSteps = listOf(
                "Boil fettuccine pasta in salted water until al dente.",
                "Melt butter with minced garlic, add cream, and simmer gently.",
                "Stir in freshly grated Parmesan cheese until sauce is creamy; toss with pasta and sliced grilled chicken."
            ),
            chefTip = "Reserve 1/2 cup pasta cooking water to emulsify the Parmesan cream sauce smoothly.",
            tags = listOf("Pasta", "Creamy", "Dinner", "Comfort Food")
        ),
        Dish(
            id = "it_3",
            name = "Penne all'Arrabbiata with Garlic Bread",
            cuisine = CuisineType.ITALIAN,
            country = "Italy",
            mealType = MealType.LUNCH,
            dayType = DayType.NORMAL,
            season = "All",
            isSpecial = false,
            prepTimeMinutes = 20,
            difficulty = "Easy",
            servings = 3,
            ingredients = listOf("Penne Pasta", "Crushed Tomatoes", "Garlic Cloves", "Red Pepper Flakes (Chilli)", "Olive Oil", "Fresh Parsley"),
            recipeSteps = listOf(
                "Gently sizzle sliced garlic and red chilli flakes in olive oil until aromatic.",
                "Add crushed tomatoes and simmer until rich and spicy sauce forms.",
                "Toss boiled penne pasta in the sauce and garnish with chopped parsley."
            ),
            chefTip = "Toast garlic slowly on low heat so it sweetens without burning and turning bitter.",
            tags = listOf("Lunch", "Pasta", "Spicy", "Vegetarian")
        ),
        Dish(
            id = "it_4",
            name = "Lasagna Bolognese",
            cuisine = CuisineType.ITALIAN,
            country = "Italy",
            mealType = MealType.DINNER,
            dayType = DayType.SPECIAL,
            season = "Winter",
            isSpecial = true,
            prepTimeMinutes = 60,
            difficulty = "Hard",
            servings = 6,
            ingredients = listOf("Lasagna Sheets", "Minced Beef Bolognese Sauce", "Creamy Béchamel Sauce", "Mozzarella & Parmesan", "Oregano"),
            recipeSteps = listOf(
                "Slow cook minced beef with tomatoes, onions, garlic, and herbs into a rich Bolognese.",
                "Prepare silky white béchamel sauce with butter, flour, and milk.",
                "Layer lasagna sheets, Bolognese, béchamel, and mozzarella in baking dish; bake until golden and bubbly."
            ),
            chefTip = "Let lasagna rest for 15 minutes after baking so layers settle cleanly before slicing.",
            tags = listOf("Dinner", "Baked", "Comfort Food", "Special")
        ),
        Dish(
            id = "it_5",
            name = "Classic Italian Tiramisu",
            cuisine = CuisineType.DESSERT,
            country = "Italy",
            mealType = MealType.DINNER,
            dayType = DayType.SPECIAL,
            season = "All",
            isSpecial = true,
            prepTimeMinutes = 30,
            difficulty = "Medium",
            servings = 6,
            ingredients = listOf("Savoiardi (Ladyfinger Biscuits)", "Mascarpone Cheese", "Fresh Espresso Coffee", "Egg Yolks / Cream", "Sugar", "Cocoa Powder"),
            recipeSteps = listOf(
                "Whisk mascarpone with sweetened cream until velvety and light.",
                "Dip ladyfinger biscuits quickly into cold espresso coffee.",
                "Layer dipped biscuits with mascarpone cream; dust generously with unsweetened cocoa powder and chill for 4 hours."
            ),
            chefTip = "Dip ladyfingers for just 1 second per side so they don't become soggy.",
            tags = listOf("Dessert", "Coffee", "No Bake", "Chilled")
        )
    )

    // -------------------------------------------------------------
    // 5. 5 BANGLADESHI DISHES
    // -------------------------------------------------------------
    val bangladeshiDishes = listOf(
        Dish(
            id = "bd_1",
            name = "Dhaka Kacchi Biryani with Borhani",
            cuisine = CuisineType.BANGLADESHI,
            country = "Bangladesh",
            mealType = MealType.DINNER,
            dayType = DayType.SPECIAL,
            season = "All",
            isSpecial = true,
            prepTimeMinutes = 90,
            difficulty = "Hard",
            servings = 6,
            ingredients = listOf("Raw Mutton Marinated in Spices & Yogurt", "Chinigura / Basmati Rice", "Fried Potatoes", "Alu Bukhara", "Mustard Oil", "Spiced Borhani Drink"),
            recipeSteps = listOf(
                "Marinate raw mutton pieces in spiced yogurt, mustard oil, and ground mace-nutmeg.",
                "Layer raw marinated meat with semi-cooked chinigura rice and fried potatoes in sealed handi.",
                "Dum cook on low flame for 1.5 hours until meat is tender and rice is fragrant; serve with spicy yogurt Borhani."
            ),
            chefTip = "Using chinigura aromatic rice provides the authentic Old Dhaka kacchi fragrance.",
            tags = listOf("Biryani", "Special", "Dhaka", "Festive")
        ),
        Dish(
            id = "bd_2",
            name = "Ilish Macher Jhol (Hilsa Curry)",
            cuisine = CuisineType.BANGLADESHI,
            country = "Bangladesh",
            mealType = MealType.LUNCH,
            dayType = DayType.SPECIAL,
            season = "Monsoon",
            isSpecial = true,
            prepTimeMinutes = 30,
            difficulty = "Easy",
            servings = 4,
            ingredients = listOf("Fresh Hilsa (Ilish) Fish Steaks", "Kalonji (Nigella Seeds)", "Green Chillies", "Turmeric Powder", "Mustard Oil", "Steamed Rice"),
            recipeSteps = listOf(
                "Rub fish steaks with turmeric and salt.",
                "Sizzle nigella seeds and slit green chillies in smoking hot pure mustard oil.",
                "Add light turmeric water broth, gently slide in fish, and simmer for 10 minutes."
            ),
            chefTip = "Cook in pure mustard oil and do not over-fry the hilsa to preserve its prized natural oil.",
            tags = listOf("Lunch", "Fish", "Monsoon", "Traditional")
        ),
        Dish(
            id = "bd_3",
            name = "Shorshe Ilish (Hilsa in Mustard Gravy)",
            cuisine = CuisineType.BANGLADESHI,
            country = "Bangladesh",
            mealType = MealType.LUNCH,
            dayType = DayType.NORMAL,
            season = "All",
            isSpecial = false,
            prepTimeMinutes = 30,
            difficulty = "Easy",
            servings = 4,
            ingredients = listOf("Hilsa Fish Steaks", "Yellow & Black Mustard Seed Paste", "Green Chillies", "Mustard Oil", "Turmeric"),
            recipeSteps = listOf(
                "Blend mustard seeds with green chillies and a pinch of salt to prevent bitterness.",
                "Cook mustard paste with turmeric and mustard oil into a vibrant gravy.",
                "Place hilsa steaks in the gravy and simmer on gentle heat."
            ),
            chefTip = "Always grind mustard seeds with green chillies and salt to prevent the paste from turning bitter.",
            tags = listOf("Lunch", "Fish", "Mustard", "Classic")
        ),
        Dish(
            id = "bd_4",
            name = "Morog Polao (Bengali Chicken Pulao)",
            cuisine = CuisineType.BANGLADESHI,
            country = "Bangladesh",
            mealType = MealType.DINNER,
            dayType = DayType.SPECIAL,
            season = "All",
            isSpecial = true,
            prepTimeMinutes = 60,
            difficulty = "Medium",
            servings = 5,
            ingredients = listOf("Country Chicken Pieces", "Chinigura Rice", "Ghee", "Yogurt", "Mawa (Khoya)", "Green Chillies", "Fried Onions"),
            recipeSteps = listOf(
                "Cook chicken pieces in spiced yogurt, ginger-garlic, and ghee until tender.",
                "Sauté fragrant chinigura rice in ghee, then cook in aromatic chicken broth.",
                "Combine chicken, drizzle saffron milk and crumbled mawa, and dum cook for 15 minutes."
            ),
            chefTip = "A touch of mawa (khoya) in Morog Polao adds rich sweetness characteristic of Bangladeshi celebrations.",
            tags = listOf("Dinner", "Festive", "Chicken Pulao", "Celebration")
        ),
        Dish(
            id = "bd_5",
            name = "Traditional Mishti Doi (Sweet Yogurt)",
            cuisine = CuisineType.DESSERT,
            country = "Bangladesh",
            mealType = MealType.DINNER,
            dayType = DayType.NORMAL,
            season = "Summer",
            isSpecial = false,
            prepTimeMinutes = 40,
            difficulty = "Medium",
            servings = 6,
            ingredients = listOf("Full Cream Milk", "Caramelized Sugar", "Yogurt Starter Culture", "Cardamom"),
            recipeSteps = listOf(
                "Reduce milk to half its volume on low flame.",
                "Caramelize sugar until amber brown and whisk into warm milk.",
                "Stir in yogurt starter culture, pour into earthenware clay pots, and ferment in a warm place for 8 hours until set."
            ),
            chefTip = "Earthen clay pots absorb excess whey water, creating ultra-thick, creamy Mishti Doi.",
            tags = listOf("Dessert", "Sweet Yogurt", "Classic", "Chilled")
        )
    )

    // -------------------------------------------------------------
    // Helper selection methods for Fallback
    // -------------------------------------------------------------
    fun isSpecialDish(name: String): Boolean {
        val n = name.lowercase()
        if (n.contains("shami kabab with daal") || n.contains("moong daal khichdi")) {
            return false
        }
        return n.contains("biryani") || n.contains("pulao") || n.contains("pilao") ||
               n.contains("polao") || n.contains("tehari") || n.contains("tahiri") ||
               n.contains("nihari") || n.contains("paye") || n.contains("paya") ||
               n.contains("siri paye") || n.contains("kunna") || n.contains("haleem") ||
               n.contains("daleem") || n.contains("shinwari") || n.contains("butt style") ||
               n.contains("koyla") || n.contains("white karahi") || n.contains("handi karahi") ||
               n.contains("karahi") || n.contains("burger") || n.contains("pizza") ||
               n.contains("broast") || n.contains("fries") || n.contains("wings") ||
               n.contains("bbq") || n.contains("barbecue") || n.contains("tikka") ||
               n.contains("seekh") || n.contains("bihari") || n.contains("boti") ||
               n.contains("sajji") || n.contains("chapli") || n.contains("chargha") ||
               n.contains("kebab") || n.contains("shish tawook") || n.contains("mandi") ||
               n.contains("kabsa") || n.contains("machboos") || n.contains("ouzi") ||
               n.contains("shawarma") || n.contains("steak") || n.contains("ribeye") ||
               n.contains("ribs") || n.contains("bistecca") || n.contains("carne asada") ||
               n.contains("carnitas") || n.contains("birria") || n.contains("lasagna") ||
               n.contains("osso buco") || n.contains("hot pot") || n.contains("peking duck") ||
               n.contains("char siu") || n.contains("mantu") || n.contains("adana kebab") ||
               n.contains("iskender") || n.contains("roast chicken") || n.contains("sunday roast")
    }

    fun isNormalStapleForCountry(country: String, name: String): Boolean {
        val n = name.lowercase()
        val c = country.lowercase()
        if (isSpecialDish(name)) return false
        return when {
            c.contains("pakistan") -> {
                n.contains("daal") || n.contains("roti") || n.contains("phulki") ||
                n.contains("kadhi") || n.contains("sabzi") || n.contains("bhindi") ||
                n.contains("gobi") || n.contains("matar") || n.contains("chawal") ||
                n.contains("baingan") || n.contains("palak") || n.contains("karela") ||
                n.contains("tori") || n.contains("lauki") || n.contains("khichdi") ||
                n.contains("shorba") || n.contains("salan") || n.contains("anda") ||
                n.contains("lobia") || n.contains("shami kabab with daal")
            }
            c.contains("india") -> {
                n.contains("dal") || n.contains("roti") || n.contains("sabzi") ||
                n.contains("chawal") || n.contains("rajma") || n.contains("kadhi") ||
                n.contains("khichdi") || n.contains("sambar") || n.contains("curd rice") ||
                n.contains("dosa") || n.contains("idli") || n.contains("paneer") ||
                n.contains("gobi") || n.contains("bhindi")
            }
            c.contains("bangladesh") -> {
                n.contains("daal") || n.contains("bhaat") || n.contains("torkari") ||
                n.contains("dim") || n.contains("macher") || n.contains("jhol") ||
                n.contains("bhorta") || n.contains("khichuri")
            }
            c.contains("uae") || c.contains("saudi") || c.contains("emirates") -> {
                n.contains("mujadara") || n.contains("foul") || n.contains("shorba adas") ||
                n.contains("fasolia") || n.contains("bamya") || n.contains("kousa") ||
                n.contains("falafel") || n.contains("hummus") || n.contains("shakshuka")
            }
            c.contains("china") -> {
                n.contains("egg") || n.contains("tofu") || n.contains("greens") ||
                n.contains("bok choy") || n.contains("fried rice") || n.contains("congee") ||
                n.contains("wonton") || n.contains("chow mein")
            }
            c.contains("italy") -> {
                n.contains("pomodoro") || n.contains("aglio") || n.contains("minestrone") ||
                n.contains("risotto bianco") || n.contains("frittata") || n.contains("penne")
            }
            c.contains("mexic") -> {
                n.contains("frijoles") || n.contains("arroz") || n.contains("caldo") ||
                n.contains("enchiladas") || n.contains("quesadilla") || n.contains("huevos")
            }
            c.contains("turkey") -> {
                n.contains("mercimek") || n.contains("kuru fasulye") || n.contains("menemen") ||
                n.contains("sebze") || n.contains("corba")
            }
            c.contains("afghan") -> {
                n.contains("lubya") || n.contains("bonjan") || n.contains("nakhod") ||
                n.contains("chalow") || n.contains("shorma")
            }
            else -> {
                n.contains("sandwich") || n.contains("soup") || n.contains("salad") ||
                n.contains("pasta") || n.contains("cheese") || n.contains("stew") ||
                n.contains("pie") || n.contains("stir fry")
            }
        }
    }

    fun isNormalPakistaniStaple(name: String): Boolean {
        return isNormalStapleForCountry("Pakistan", name)
    }

    fun getFallbackSuggestions(
        country: String,
        mealType: MealType,
        dayType: DayType,
        dishCount: Int,
        sweetDishCount: Int,
        excludedDishes: List<String>,
        wantToCook: List<String>,
        dontWantToCook: List<String>
    ): List<Dish> {
        val normalizedCountry = country.trim().lowercase()
        val allAvailable = allDishes
        val countryMatches = allAvailable.filter {
            val c = it.country.trim().lowercase()
            c == normalizedCountry || c.contains(normalizedCountry) || normalizedCountry.contains(c)
        }
        val savedAiDishes = LocalAiStorageService.getSavedAiSuggestions(
            country = country,
            mealType = mealType,
            dayType = dayType,
            excludedDishes = excludedDishes,
            wantToCook = wantToCook,
            dontWantToCook = dontWantToCook
        )

        val basePrimaryPool = if (countryMatches.isNotEmpty()) {
            countryMatches
        } else {
            when {
                normalizedCountry.contains("pakistan") -> allAvailable.filter { it.country.equals("Pakistan", ignoreCase = true) }.ifEmpty { pakistaniDishes }
                normalizedCountry.contains("india") -> allAvailable.filter { it.country.equals("India", ignoreCase = true) }.ifEmpty { indianDishes }
                normalizedCountry.contains("bangladesh") -> allAvailable.filter { it.country.equals("Bangladesh", ignoreCase = true) }.ifEmpty { bangladeshiDishes }
                normalizedCountry.contains("china") -> allAvailable.filter { it.country.equals("China", ignoreCase = true) }.ifEmpty { chineseDishes }
                normalizedCountry.contains("italy") -> allAvailable.filter { it.country.equals("Italy", ignoreCase = true) }.ifEmpty { italianDishes }
                normalizedCountry.contains("uae") || normalizedCountry.contains("emirates") || normalizedCountry.contains("saudi") ->
                    allAvailable.filter { it.country.contains("UAE", ignoreCase = true) || it.country.contains("Saudi", ignoreCase = true) }
                normalizedCountry.contains("usa") || normalizedCountry.contains("united states") ->
                    allAvailable.filter { it.country.equals("USA", ignoreCase = true) }
                normalizedCountry.contains("uk") || normalizedCountry.contains("united kingdom") ->
                    allAvailable.filter { it.country.equals("UK", ignoreCase = true) }
                normalizedCountry.contains("afghan") ->
                    allAvailable.filter { it.country.equals("Afghanistan", ignoreCase = true) }
                normalizedCountry.contains("turkey") || normalizedCountry.contains("turkish") ->
                    allAvailable.filter { it.country.equals("Turkey", ignoreCase = true) }
                normalizedCountry.contains("mexic") ->
                    allAvailable.filter { it.country.equals("Mexico", ignoreCase = true) }
                else -> allAvailable
            }
        }.ifEmpty { allAvailable }

        val primaryPool = (savedAiDishes + basePrimaryPool).distinctBy { it.name.trim().lowercase() }

        val excludedSet = (excludedDishes + dontWantToCook).map { it.trim().lowercase() }.toSet()
        val wantSet = wantToCook.map { it.trim().lowercase() }.toSet()

        // Filter local pool and international pool separately
        val localSavory = primaryPool.filter { dish ->
            dish.cuisine != CuisineType.DESSERT &&
            !excludedSet.any { excluded -> dish.name.lowercase().contains(excluded) || excluded.contains(dish.name.lowercase()) }
        }

        val internationalPool = allDishes.filter { dish ->
            dish !in primaryPool &&
            dish.cuisine != CuisineType.DESSERT &&
            !excludedSet.any { excluded -> dish.name.lowercase().contains(excluded) || excluded.contains(dish.name.lowercase()) }
        }

        val sweetDishes = (allDishes.filter { it.cuisine == CuisineType.DESSERT }).filter { dish ->
            !excludedSet.any { excluded -> dish.name.lowercase().contains(excluded) || excluded.contains(dish.name.lowercase()) }
        }

        val isNormalPakistaniDay = dayType == DayType.NORMAL && country.contains("pakistan", ignoreCase = true)

        // Determine local vs international count (e.g. 5 dishes -> 4 local, 1 international)
        val internationalCount = when {
            isNormalPakistaniDay -> 0
            dishCount <= 1 -> if (Math.random() < 0.35 && internationalPool.isNotEmpty()) 1 else 0
            dishCount in 2..4 -> 1
            else -> 1 + (dishCount - 5) / 4
        }
        val localCount = (dishCount - internationalCount).coerceAtLeast(0)

        // Helper to check meal compatibility:
        // In Pakistani/South Asian cooking, Lunch & Dinner main courses (daal, sabzi, curries, rice) are interchangeable.
        fun isMealCompatible(dishMeal: MealType, targetMeal: MealType): Boolean {
            if (dishMeal == targetMeal) return true
            if (targetMeal == MealType.BREAKFAST) {
                return dishMeal == MealType.BREAKFAST || dishMeal == MealType.SEHRI
            }
            val isMainCourse = (dishMeal == MealType.LUNCH || dishMeal == MealType.DINNER)
            val isTargetMainCourse = (targetMeal == MealType.LUNCH || targetMeal == MealType.DINNER)
            return isMainCourse && isTargetMainCourse
        }

        val isTargetBreakfast = (mealType == MealType.BREAKFAST)

        // Filter local pool:
        // On NORMAL days, allow only everyday staples (Daal Roti, Sabzi Roti, Daal Chawal, Dahi Phulki, Kadhi Chawal).
        // On SPECIAL days, prioritize celebratory feasts (Biryani, Pulao, Paye, Nihari, Fast Food, BBQ).
        // On BREAKFAST, strictly prioritize breakfast meals.
        val baseLocal = if (isTargetBreakfast) {
            val bList = localSavory.filter { isMealCompatible(it.mealType, MealType.BREAKFAST) }
            if (bList.isNotEmpty()) bList else localSavory
        } else localSavory

        val dayFilteredLocal = baseLocal.filter { dish ->
            if (isTargetBreakfast) true
            else if (dayType == DayType.NORMAL) {
                !dish.isSpecial && dish.dayType != DayType.SPECIAL && !isSpecialDish(dish.name)
            } else {
                dish.isSpecial || dish.dayType == DayType.SPECIAL || isSpecialDish(dish.name)
            }
        }.ifEmpty {
            if (!isTargetBreakfast && dayType == DayType.NORMAL) {
                baseLocal.filter { !isSpecialDish(it.name) }
            } else baseLocal
        }

        val baseInternational = if (isTargetBreakfast) {
            val bList = internationalPool.filter { isMealCompatible(it.mealType, MealType.BREAKFAST) }
            if (bList.isNotEmpty()) bList else internationalPool
        } else internationalPool

        val dayFilteredInternational = baseInternational.filter { dish ->
            if (isTargetBreakfast) true
            else if (dayType == DayType.NORMAL) {
                !dish.isSpecial && dish.dayType != DayType.SPECIAL && !isSpecialDish(dish.name)
            } else {
                dish.isSpecial || dish.dayType == DayType.SPECIAL || isSpecialDish(dish.name)
            }
        }.ifEmpty { baseInternational }

        val sortedLocal = dayFilteredLocal.shuffled().sortedWith(
            compareByDescending<Dish> { it.mealType == mealType }
                .thenByDescending { isMealCompatible(it.mealType, mealType) }
                .thenByDescending { dish ->
                    if (wantSet.isNotEmpty()) {
                        val dName = dish.name.lowercase()
                        wantSet.any { dName.contains(it) || dish.ingredients.any { ing -> ing.lowercase().contains(it) } }
                    } else false
                }
                .thenByDescending {
                    if (isTargetBreakfast) it.mealType == MealType.BREAKFAST
                    else if (dayType == DayType.NORMAL) isNormalStapleForCountry(country, it.name)
                    else isSpecialDish(it.name)
                }
        )

        val sortedInternational = dayFilteredInternational.shuffled().sortedWith(
            compareByDescending<Dish> { it.mealType == mealType }
                .thenByDescending { isMealCompatible(it.mealType, mealType) }
                .thenByDescending { dish ->
                    if (wantSet.isNotEmpty()) {
                        val dName = dish.name.lowercase()
                        wantSet.any { dName.contains(it) || dish.ingredients.any { ing -> ing.lowercase().contains(it) } }
                    } else false
                }
                .thenByDescending {
                    if (isTargetBreakfast) it.mealType == MealType.BREAKFAST
                    else if (dayType == DayType.NORMAL) isNormalStapleForCountry("International", it.name)
                    else isSpecialDish(it.name)
                }
        )

    val selectedLocal = sortedLocal.take(localCount)
    val selectedInternational = sortedInternational.take(internationalCount)
    val selectedMains = (selectedLocal + selectedInternational).shuffled().ifEmpty {
        sortedLocal.take(dishCount.coerceAtLeast(1))
    }

    val selectedDesserts = if (sweetDishCount > 0) sweetDishes.shuffled().take(sweetDishCount) else emptyList()

    return (selectedMains + selectedDesserts).ifEmpty {
        dayFilteredLocal.shuffled().take(dishCount)
    }
}

    fun getFallbackRecipe(dishName: String, cuisine: String, country: String): Dish {
        val savedLocal = LocalAiStorageService.getSavedRecipeLocally(dishName)
        if (savedLocal != null && savedLocal.recipeSteps.isNotEmpty()) {
            return savedLocal
        }

        val matched = allDishes.firstOrNull { it.name.contains(dishName, ignoreCase = true) || dishName.contains(it.name, ignoreCase = true) }
        if (matched != null) return matched

        return Dish(
            id = "fallback_${System.currentTimeMillis()}",
            name = dishName.ifBlank { "Classic Special Dish" },
            cuisine = CuisineType.fromString(cuisine),
            country = country.ifBlank { "Pakistan" },
            mealType = MealType.DINNER,
            dayType = DayType.NORMAL,
            season = "All",
            isSpecial = false,
            prepTimeMinutes = 35,
            difficulty = "Easy",
            servings = 4,
            ingredients = listOf("Main ingredients", "Onions", "Tomatoes", "Ginger-Garlic", "Spices", "Cooking Oil / Ghee", "Fresh Herbs"),
            recipeSteps = listOf(
                "Heat oil or ghee in a pan and sauté aromatics until fragrant.",
                "Add main ingredients and sauté with spices until lightly browned.",
                "Simmer on low flame with water or broth until tender and flavorful.",
                "Garnish with fresh herbs and serve hot."
            ),
            chefTip = "Adjust spices to your preferred heat level and serve fresh.",
            tags = listOf("Homemade", "Quick Recipe")
        )
    }

    fun getFallbackKitchenPreferences(country: String): Pair<List<String>, List<String>> {
        return when {
            country.contains("pakistan", ignoreCase = true) -> Pair(
                listOf("Chicken Karahi", "Sindhi Biryani", "Afghani Pulao", "Chicken Tikka Pizza", "Aloo Gosht", "Shinwari Karahi", "Seekh Kabab", "Daal Chawal"),
                listOf("Bitter Gourd (Karela)", "Tinda", "Tori", "Arbi", "Kidney Beans")
            )
            country.contains("india", ignoreCase = true) -> Pair(
                listOf("Paneer Butter Masala", "Dal Makhani", "Chole Bhature", "Rajma Chawal", "Butter Chicken", "Dosa", "Palak Paneer", "Pav Bhaji"),
                listOf("Karela", "Lauki", "Tinda", "Bitter Melon", "Raw Papaya")
            )
            country.contains("bangladesh", ignoreCase = true) -> Pair(
                listOf("Ilish Macher Jhol", "Kacchi Biryani", "Morog Polao", "Shorshe Ilish", "Bhuna Khichuri", "Chingri Malai Curry"),
                listOf("Karela", "Overly Sweet Gravies", "Raw Turnip")
            )
            country.contains("emirates", ignoreCase = true) || country.contains("uae", ignoreCase = true) || country.contains("saudi", ignoreCase = true) -> Pair(
                listOf("Mutton Mandi", "Chicken Machboos", "Shish Tawook", "Shawarma Platter", "Kabsa", "Hummus with Lamb", "Lentil Soup"),
                listOf("Bitter Melon", "Heavy Pork", "Over-spicy Chilies")
            )
            country.contains("china", ignoreCase = true) -> Pair(
                listOf("Kung Pao Chicken", "Sichuan Beef", "Yangzhou Fried Rice", "Dim Sum Dumplings", "Chow Mein", "Hot and Sour Soup"),
                listOf("Heavy Dairy", "Overly Sweet Gravies", "Raw Onions")
            )
            country.contains("italy", ignoreCase = true) -> Pair(
                listOf("Fettuccine Alfredo", "Margherita Pizza", "Lasagna Bolognese", "Risotto ai Funghi", "Pasta Carbonara", "Penne Arrabbiata"),
                listOf("Overcooked Pasta", "Heavy Cumin", "Artificial Flavors")
            )
            country.contains("united states", ignoreCase = true) || country.contains("usa", ignoreCase = true) || country.contains("united kingdom", ignoreCase = true) || country.contains("uk", ignoreCase = true) -> Pair(
                listOf("Grilled BBQ Chicken", "Steak with Roasted Veggies", "Chicken Tikka Masala", "Classic Beef Burger", "Creamy Pasta", "Fish and Chips"),
                listOf("Bitter Gourd", "Organ Meats", "Extremely Oily Curries")
            )
            else -> Pair(
                listOf("Grilled Chicken", "Biryani", "Pasta Alfredo", "Chicken Stir Fry", "Homemade Curry", "Steak", "Garlic Bread"),
                listOf("Bitter Gourd", "Raw Okra", "Excessively Oily Dishes")
            )
        }
    }
}
