package com.example.data.repository

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.BuildConfig
import com.example.receiver.AlarmReceiver
import com.example.data.api.*
import com.example.data.local.DietPlannerDao
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.*

class DietPlannerRepository(private val dao: DietPlannerDao) {

    fun getUserProfile(userId: String): Flow<UserProfileEntity?> = dao.getUserProfileFlow(userId)
    suspend fun getUserProfileDirect(userId: String): UserProfileEntity? = withContext(Dispatchers.IO) {
        dao.getUserProfile(userId)
    }
    val allWeightLogs: Flow<List<WeightLogEntity>> = dao.getAllWeightLogsFlow()
    val allReminders: Flow<List<MealReminderEntity>> = dao.getAllRemindersFlow()

    fun getMoodLogs(userId: String): Flow<List<MoodLogEntity>> = dao.getMoodLogsFlow(userId)

    suspend fun saveMoodLog(moodLog: MoodLogEntity) = withContext(Dispatchers.IO) {
        dao.insertMoodLog(moodLog)
    }

    suspend fun deleteMoodLog(id: Int) = withContext(Dispatchers.IO) {
        dao.deleteMoodLog(id)
    }

    fun getMealPlanFlow(date: String): Flow<MealPlanEntity?> = dao.getMealPlanFlow(date)
    fun getAllMealPlansFlow(): Flow<List<MealPlanEntity>> = dao.getAllMealPlansFlow()
    fun getWaterLogFlow(date: String): Flow<WaterLogEntity?> = dao.getWaterLogFlow(date)
    fun getShoppingItemsFlow(date: String): Flow<List<ShoppingItemEntity>> = dao.getShoppingItemsFlow(date)

    suspend fun updateShoppingItemChecked(id: Int, isChecked: Boolean) = withContext(Dispatchers.IO) {
        dao.updateShoppingItemChecked(id, isChecked)
    }

    suspend fun addShoppingItem(item: ShoppingItemEntity) = withContext(Dispatchers.IO) {
        dao.insertShoppingItems(listOf(item))
    }

    suspend fun deleteShoppingItems(date: String) = withContext(Dispatchers.IO) {
        dao.deleteShoppingItemsForDate(date)
    }

    suspend fun saveUserProfile(profile: UserProfileEntity) = withContext(Dispatchers.IO) {
        dao.insertUserProfile(profile)
    }

    suspend fun saveWaterLog(waterLog: WaterLogEntity) = withContext(Dispatchers.IO) {
        dao.insertWaterLog(waterLog)
    }

    suspend fun saveWeightLog(weight: Double, date: String) = withContext(Dispatchers.IO) {
        val log = WeightLogEntity(date = date, weight = weight)
        dao.insertWeightLog(log)
    }

    suspend fun deleteWeightLog(date: String) = withContext(Dispatchers.IO) {
        dao.deleteWeightLog(date)
    }

    suspend fun saveReminder(reminder: MealReminderEntity) = withContext(Dispatchers.IO) {
        dao.insertReminder(reminder)
    }

    suspend fun deleteReminder(id: Int) = withContext(Dispatchers.IO) {
        dao.deleteReminder(id)
    }

    suspend fun preloadDefaultRemindersIfEmpty() = withContext(Dispatchers.IO) {
        val existing = dao.getAllReminders()
        if (existing.isEmpty()) {
            val defaults = listOf(
                MealReminderEntity(1, "Breakfast (সকালের নাস্তা)", 8, 0, true),
                MealReminderEntity(2, "Snack 1 (সকালের হালকা খাবার)", 11, 0, true),
                MealReminderEntity(3, "Lunch (দুপুরের খাবার)", 13, 30, true),
                MealReminderEntity(4, "Snack 2 (বিকালের হালকা খাবার)", 17, 0, true),
                MealReminderEntity(5, "Dinner (রাতের খাবার)", 20, 30, true),
                MealReminderEntity(6, "Water Drink Reminder (পানি পানের রিমাইন্ডার)", 10, 0, true)
            )
            dao.insertReminders(defaults)
        }
    }

    val allRecipesFlow: Flow<List<RecipeEntity>> = dao.getAllRecipesFlow()

    suspend fun toggleRecipeFavorite(id: Int, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        dao.updateRecipeFavorite(id, isFavorite)
    }

    suspend fun preloadDefaultRecipesIfEmpty() = withContext(Dispatchers.IO) {
        val count = dao.getRecipesCount()
        if (count == 0) {
            dao.insertRecipes(com.example.data.model.DietDataStore.getFullRecipesList())
            return@withContext
            val defaults = listOf(
                RecipeEntity(
                    title = "Low-GI Oats Vegetable Khichuri",
                    titleBn = "লো-জিআই ডায়েট ওটস খিচুড়ি",
                    duration = "20 mins",
                    durationBn = "২০ মিনিট",
                    calories = "250 kcal",
                    caloriesValue = 250,
                    category = "Diabetes & Blood Pressure Safe",
                    categoryBn = "ডায়াবেটিস ও উচ্চ রক্তচাপ বান্ধব",
                    mealType = "Breakfast",
                    dietaryRestrictions = "Vegetarian, Vegan, Diabetic-Friendly, Low-Sodium",
                    ingredients = listOf(
                        "Oats (0.5 cup, 50g)",
                        "Mung Dal (boiled, 2 tbsp)",
                        "Chopped Papaya & Carrots (1 cup)",
                        "Spinach leaves (washed, 50g)",
                        "Green Chillies & Ginger slices",
                        "Mustard Oil (Cold-pressed, 1 tsp)"
                    ),
                    ingredientsBn = listOf(
                        "ডায়েট রোলড ওটস (০.৫ কাপ, ৫০g)",
                        "মুগ ডাল (সেদ্ধ, ২ চামচ)",
                        "কুচানো পেঁপে ও গাজর (১ কাপ)",
                        "পালং শাক (ধুয়ে কাটা, ৫০g)",
                        "কাঁচা মরিচ ও আদা কুচি",
                        "খাঁটি সরিষার তেল (১ চা চামচ)"
                    ),
                    steps = listOf(
                        "Lightly dry roast the oats and mung dal in a heated pan for 2-3 minutes.",
                        "Add chopped carrots, papaya, ginger, mustard oil; sauté briefly.",
                        "Pour 2 cups of filtered water, bring to boil, then cover and cook over slow flame.",
                        "Mix in spinach leaves and green chillies in the last 2 minutes until fully simmered.",
                        "Garnish with fresh green cilantro leaves and serve warm."
                    ),
                    stepsBn = listOf(
                        "প্রথমে শুকনো প্যানে ওটস ও ডাল হালকা তাপে ২-৩ মিনিট ভেজে নিন।",
                        "এরপর গাজর, পেঁপে, আদার স্লাইস ও এক চামচ সরিষার তেল দিয়ে হালকা নাড়ুন।",
                        "দুই কাপ পানি ঢালুন এবং ঢেকে মৃদু আঁচে সেদ্ধ করুন।",
                        "নামানোর ২ মিনিট আগে পালং শাক ও কাঁচা মরিচ ছড়িয়ে দিন।",
                        "সবশেষে ধনেপাতা কুচি দিয়ে কুসুম গরম পরিবেশন করুন।"
                    ),
                    tip = "Oats are filled with beta-glucan fiber which helps regulate blood glucose spikes.",
                    tipBn = "ওটস-এ থাকা বিটা-গ্লুকান ফাইবার রক্তে গ্লুকোজের আকস্মিক বৃদ্ধি কমাতে ভীষণ সাহায্য করে।"
                ),
                RecipeEntity(
                    title = "Steamed Lean Fish in Lemon Herbs",
                    titleBn = "লেবু-ধনেপাতায় ভাপা কোরাল বা রুই",
                    duration = "25 mins",
                    durationBn = "২৫ মিনিট",
                    calories = "180 kcal",
                    caloriesValue = 180,
                    category = "Heart Healthy & Lean Protein",
                    categoryBn = "হার্ট-বান্ধব ও লীন প্রোটিন",
                    mealType = "Lunch",
                    dietaryRestrictions = "Diabetic-Friendly, Low-Sodium, Gluten-Free",
                    ingredients = listOf(
                        "Ruhi/Koral Fish chunk (150g fillet)",
                        "Lemon juice (2 tbsp)",
                        "Garlic paste (1 tsp)",
                        "Black pepper crushed (0.5 tsp)",
                        "Fresh Coriander leaves paste (2 tbsp)",
                        "Mustard Oil (0.5 tsp)"
                    ),
                    ingredientsBn = listOf(
                        "রুই বা কোরাল মাছের ফিলেট (১৫০g)",
                        "তাজা লেবুর রস (২ চামচ)",
                        "রসুন বাটা (১ চা চামচ)",
                        "গোলমরিচ গুঁড়ো (০.৫ চা চামচ)",
                        "ধনেপাতা ব্লেন্ডেড পেস্ট (২ চামচ)",
                        "সরিষার তেল (০.৫ চা চামচ)"
                    ),
                    steps = listOf(
                        "Make 2-3 small diagonal slits on both sides of the cleaned fish piece.",
                        "Mix lemon juice, garlic, black pepper, and coriander paste together and rub thoroughly on fish.",
                        "Let the marination rest in the refrigerator for 15 minutes to soak flavors.",
                        "Place the marinated fish in a heatproof steaming plate, cover with aluminum foil or lid.",
                        "Steam inside a water cooker for 12-15 minutes on medium-high heat until flaky."
                    ),
                    stepsBn = listOf(
                        "মাছের টুকরো ভালো করে ধুয়ে দুই পাশে চাকু দিয়ে হালকা ১/২ ইঞ্চি চিড়ে নিন।",
                        "লেবুর রস, রসুন, ধনেপাতা বাটা ও গোলমরিচ একসাথে মিশিয়ে মাছের গায়ে মেখে নিন।",
                        "মসলা ভেতরে ঢোকার জন্য ১৫ মিনিট ফ্রিজে ম্যারিনেট করতে রাখুন।",
                        "একটি ভাপে দেওয়ার পাত্রে মাছ রাখুন এবং ফয়েল পেপার বা ঢাকনা দিয়ে আটকে দিন।",
                        "পানির ভাপ ওঠা চুলায় ঢাকনা দিয়ে ১২-১৫ মিনিট ভাপিয়ে নিন।"
                    ),
                    tip = "Steaming preserves premium Omega-3 fatty acids without adding unhealthy oxidized cooking fats.",
                    tipBn = "ভাপ পদ্ধতি ক্ষতিকর চর্বি যুক্ত করা ছাড়াই মাছের অমূল্য ওমেগা-৩ ফ্যাটি এসিড সুরক্ষিত রাখে।"
                ),
                RecipeEntity(
                    title = "Fluffy White Veggie Frittata",
                    titleBn = "সবজি ও ডিমের প্রোটিন অমলেট",
                    duration = "15 mins",
                    durationBn = "১৫ মিনিট",
                    calories = "130 kcal",
                    caloriesValue = 130,
                    category = "Weight Management Target",
                    categoryBn = "ওজন নিয়ন্ত্রণ ও পেশী গঠন",
                    mealType = "Breakfast",
                    dietaryRestrictions = "Gluten-Free, Diabetic-Friendly",
                    ingredients = listOf(
                        "Egg whites (3 large eggs)",
                        "Whole egg (1 piece)",
                        "Diced Bell peppers & Tomato (0.5 cup)",
                        "Onion finely minced (2 tbsp)",
                        "Fresh spinach leaves chopped (0.25 cup)",
                        "Salt & pepper dash"
                    ),
                    ingredientsBn = listOf(
                        "ডিমের সাদা অংশ (৩টি বড় ডিম)",
                        "আস্ত ডিম (১টি)",
                        "কুচানো ক্যাপসিকাম ও টমেটো (০.৫ কাপ)",
                        "পেঁয়াজ কুচি (২ চামচ)",
                        "পালং শাক ছোট কাটা (০.২৫ কাপ)",
                        "সামান্য লবণ ও গোলমরিচ গুঁড়ো"
                    ),
                    steps = listOf(
                        "Whisk the egg whites and whole egg together in a bowl until light and foamy.",
                        "Stir in the chopped bell peppers, tomato, onions, spinach leaves, and black pepper.",
                        "Warm a non-stick frying pan, brush with a droplet of olive oil.",
                        "Pour the mixture, cover immediately with lid and cook on extremely low heat.",
                        "Once bottom is golden-red and top is fully firm (about 5 mins), slide onto plate."
                    ),
                    stepsBn = listOf(
                        "একটি বাটিতে ডিমের সাদা অংশ ও একটি আস্ত ডিম একসাথে নিয়ে ভালো করে ফেটিয়ে নিন।",
                        "ফেটা ডিমের সাথে পেঁয়াজ কুচি, টমেটো, ক্যাপসিকাম, পালং শাক ও গোলমরিচ মেশান।",
                        "একটি নন-স্টিক ফ্রাইপ্যানে সামান্য এক ফোঁটা তেল ব্রাশ করে নিন।",
                        "মিশ্রণটি ঢেলে দিয়ে সাথে সাথে ঢাকনা দিয়ে মৃদু আঁচে ৫ মিনিট রান্না করুন।",
                        "অমলেট ফুলে উঠলে এবং নিচের অংশ লালচে হলে নামিয়ে পরিবেশন করুন।"
                    ),
                    tip = "Frittatas keep hunger hormones in check due to highly bio-available protein density.",
                    tipBn = "উচ্চ মাত্রার প্রোটিন থাকার কারণে ফ্রিতাতা দীর্ঘক্ষণ ক্ষুধা নিয়ন্ত্রণে রাখতে অতুলনীয়।"
                ),
                RecipeEntity(
                    title = "Garlic Herbed Chicken Breast",
                    titleBn = "গার্লিক ও হার্ব চিকেন ব্রেস্ট",
                    duration = "30 mins",
                    durationBn = "৩০ মিনিট",
                    calories = "220 kcal",
                    caloriesValue = 220,
                    category = "High Protein & Lean Diet",
                    categoryBn = "উচ্চ প্রোটিন ও পেশীবহুল ডায়েট",
                    mealType = "Dinner",
                    dietaryRestrictions = "Gluten-Free, Diabetic-Friendly, Low-Sodium",
                    ingredients = listOf(
                        "Chicken Breast fillet (150g)",
                        "Minced Garlic (1.5 tsp)",
                        "Mixed dried herbs (Oregano/Thyme) (0.5 tsp)",
                        "Olive Oil (1 tsp)",
                        "Lemon wedge and zest",
                        "Crushed red pepper flakes"
                    ),
                    ingredientsBn = listOf(
                        "মুরগির বুকের ফিলেট বা হাড়হীন মাংস (১৫০g)",
                        "কুচানো রসুন (১.৫ চা চামচ)",
                        "মিক্সড হার্বস প্যাকেট (০.৫ চা চামচ)",
                        "অলিভ অয়েল (১ চা চামচ)",
                        "লেবুর রস ও খোসা কুচি",
                        "শুকনো মরিচ ভাঁজা গুঁড়ো (চিলি ফ্লেক্স)"
                    ),
                    steps = listOf(
                        "Pound the chicken breast gently to even thickness.",
                        "Marinate chicken with garlic, olive oil, herbs, lemon juice, and a pinch of salt-substitute.",
                        "Heat a grill pan on medium and spray light cooking oil.",
                        "Grill the chicken for 6-7 minutes on each side until fully cooked and golden-brown.",
                        "Allow to rest for 3 minutes before slicing to lock in juiciness."
                    ),
                    stepsBn = listOf(
                        "প্রথমে মুরগির মাংসটিকে আলতো করে পিটিয়ে সমান পুরুত্বে আনুন।",
                        "রসুন, অলিভ অয়েল, ভেষজ সুগন্ধি, লেবুর রস ও সামান্য লবণ দিয়ে ম্যারিনেট করুন।",
                        "একটি গ্রিল প্যান মাঝারি আঁচে গরম করে সামান্য রান্নার স্প্রে বা তেল দিন।",
                        "উভয় পাশ ৬-৭ মিনিট ভালো করে সোনালী-বাদামী করে গ্রিল করুন।",
                        "মাংসটির জুসিনেস বজায় রাখতে কাটার আগে ৩ মিনিট এমনি রেখে দিন।"
                    ),
                    tip = "Grilling meat at medium temperature prevents formation of harmful compounds while keeping protein intact.",
                    tipBn = "মাঝারি তাপমাত্রায় গ্রিল করার মাধ্যমে ক্ষতিকর রাসায়নিক তৈরি প্রতিরোধ হয় এবং প্রোটিন উন্নত থাকে।"
                ),
                RecipeEntity(
                    title = "Bengali Soupy Red Lentil Daal",
                    titleBn = "বাঙালি পাতলা মসুর ডাল",
                    duration = "15 mins",
                    durationBn = "১৫ মিনিট",
                    calories = "110 kcal",
                    caloriesValue = 110,
                    category = "Low Calorie Fiber & Protein",
                    categoryBn = "কম ক্যালোরিযুক্ত ফাইবার ও প্রোটিন",
                    mealType = "Lunch",
                    dietaryRestrictions = "Vegetarian, Vegan, Gluten-Free, Low-Sodium",
                    ingredients = listOf(
                        "Red Lentils (0.5 cup, 80g)",
                        "Chopped Tomato (0.5 cup)",
                        "Turmeric powder (0.25 tsp)",
                        "Green chillies (2 pieces cut)",
                        "Onion sliced (2 tbsp)",
                        "Coriander leaves for garnish"
                    ),
                    ingredientsBn = listOf(
                        "মসুর ডাল (০.৫ কাপ, ৮০g)",
                        "কুচানো টমেটো (০.৫ কাপ)",
                        "হলুদ গুঁড়ো (০.২৫ চা চামচ)",
                        "কাঁচা মরিচ ফালি (২টি)",
                        "পেঁয়াজ স্লাইস (২ চামচ)",
                        "সাজানোর জন্য ধনেপাতা"
                    ),
                    steps = listOf(
                        "Wash red lentils and boil in 3 cups of water with turmeric, green chillies, and tomatoes.",
                        "Simmer over medium-low heat for 10 minutes until the lentils are completely soft.",
                        "In another small pan, sauté onions in a drop of mustard oil until brown.",
                        "Pour the hot sautéed onion seasoning into the boiling lentil soup.",
                        "Garnish with freshly chopped coriander and serve hot with brown rice."
                    ),
                    stepsBn = listOf(
                        "মসুর ডাল ধুয়ে ৩ কাপ পানিতে হলুদ, কাঁচা মরিচ এবং টমেটো দিয়ে সেদ্ধ করুন।",
                        "মাঝারি-কম আঁচে ১০ মিনিট রান্না করুন ডাল পুরোপুরি গলে না যাওয়া পর্যন্ত।",
                        "অন্য ছোট প্যানে সামান্য সরিষার তেলে পেঁয়াজ কুচি লালচে করে ভাজুন (বাগার)।",
                        "এই গরম তেলের বাগারটি ফুটন্ত ডালের মধ্যে সাবধানে ঢেলে দিন।",
                        "কুচানো ধনেপাতা ছড়িয়ে দিয়ে গরম গরম লাল চালের ভাতের সাথে পরিবেশন করুন।"
                    ),
                    tip = "Lentils contain high amounts of plant-based protein and prebiotic fiber which promote excellent gut health.",
                    tipBn = "মসুর ডালে প্রচুর উদ্ভিজ্জ প্রোটিন এবং প্রিবায়োটিক ফাইবার রয়েছে যা পেটের স্বাস্থ্যের উন্নতি ঘটায়।"
                ),
                RecipeEntity(
                    title = "Spiced Cucumber Salad with Yogurt",
                    titleBn = "শসা ও দইয়ের পুষ্টিকর রায়তা",
                    duration = "10 mins",
                    durationBn = "১০ মিনিট",
                    calories = "75 kcal",
                    caloriesValue = 75,
                    category = "Low Calorie Refreshing Hydration",
                    categoryBn = "কম ক্যালোরি ও প্রশান্তিদায়ক হাইড্রেশন",
                    mealType = "Snack",
                    dietaryRestrictions = "Vegetarian, Gluten-Free, Diabetic-Friendly",
                    ingredients = listOf(
                        "Fresh cucumber sliced (1.5 cups)",
                        "Fat-free homemade yogurt (0.5 cup)",
                        "Chop mint leaves (1 tbsp)",
                        "Roasted cumin powder (0.25 tsp)",
                        "Pink salt or regular salt pinch",
                        "Black pepper crushed"
                    ),
                    ingredientsBn = listOf(
                        "তাজা শসা স্লাইস বা কুচি (১.৫ কাপ)",
                        "ফ্যাট-ফ্রি টকদই (০.৫ কাপ)",
                        "পুদিনা পাতা কুচি (১ চামচ)",
                        "ভাজা জিরা গুঁড়ো (০.২৫ চা চামচ)",
                        "সামান্য বিট লবণ বা সাধারণ লবণ",
                        "গোলমরিচ গুঁড়ো"
                    ),
                    steps = listOf(
                        "Peel and dice the cucumbers into thin bite-sized chunks.",
                        "Whisk the yogurt in a mixing bowl until completely smooth.",
                        "Stir in cumin powder, mint leaves, black pepper, and salt into the yogurt.",
                        "Add the cucumber pieces and fold gently until evenly coated.",
                        "Chill in the refrigerator for 5 minutes before serving."
                    ),
                    stepsBn = listOf(
                        "প্রথমে শসা ভালো করে ধুয়ে ছোট ছোট পাতলা টুকরো করে নিন।",
                        "একটি পাত্রে টকদই চামচ দিয়ে ভালো করে ফেটিয়ে নিন মসৃণ হওয়া পর্যন্ত।",
                        "দইয়ের সাথে জিরার গুঁড়ো, পুদিনা পাতা, গোলমরিচ এবং লবণ হালকা মেশান।",
                        "এবার শসার টুকরোগুলো দিয়ে ভালোভাবে আলতো করে মেখে নিন।",
                        "দুপুরের হালকা নাস্তা হিসেবে পরিবেশন করার আগে ফ্রিজে ৫ মিনিট ঠান্ডা করতে পারেন।"
                    ),
                    tip = "Yogurt provides probiotics that aid digestion, while cucumber hydrates the skin and cells.",
                    tipBn = "টকদই পরিপাকতন্ত্র সহজে সচল করতে প্রোবায়োটিক যোগায়, আর শসা ত্বকের আর্দ্রতা ধরে রাখে।"
                ),
                RecipeEntity(
                    title = "Stir Fried Tofu with Mushrooms",
                    titleBn = "মাশরুম ও টোফুর স্বাস্থ্যকর স্টির-ফ্রাই",
                    duration = "15 mins",
                    durationBn = "১৫ মিনিট",
                    calories = "160 kcal",
                    caloriesValue = 160,
                    category = "Pure Vegan Cardio Shield",
                    categoryBn = "সম্পূর্ণ নিরামিষ বা ভেগান পুষ্টি",
                    mealType = "Dinner",
                    dietaryRestrictions = "Vegetarian, Vegan, Gluten-Free, Diabetic-Friendly, Low-Sodium",
                    ingredients = listOf(
                        "Firm Tofu (cubed, 100g)",
                        "Fresh white button mushrooms (1 cup sliced)",
                        "Broccoli florets (0.5 cup)",
                        "Soy sauce (low sodium, 1 tsp)",
                        "Sesame oil (1 tsp)",
                        "Toasted sesame seeds for garnish"
                    ),
                    ingredientsBn = listOf(
                        "শক্ত টোফু বা পনির (১০০g, চারকোণা কাটা)",
                        "তাজা বোতাম মাশরুম (১ কাপ, স্লাইস করা)",
                        "ব্রকলি কুচি (০.৫ কাপ)",
                        "সয়া সস (অল্প সোডিয়াম, ১ চা চামচ)",
                        "তিলের তেল (১ চা চামচ)",
                        "সাজানোর জন্য সাদা তিল"
                    ),
                    steps = listOf(
                        "Squeeze excess moisture out of tofu and cut into 1-inch bite-sized cubes.",
                        "Heat sesame oil in a saucepan on medium-high heat.",
                        "Add tofu cubes and stir-fry for 4 minutes until golden-brown.",
                        "Add mushrooms, broccoli, and soy sauce; sauté for an additional 4 minutes.",
                        "Garnish with roasted sesame seeds and enjoy plain or with brown rice."
                    ),
                    stepsBn = listOf(
                        "টোফু থেকে অতিরিক্ত আর্দ্রতা নিংড়ে নিয়ে এক ইঞ্চি চারকোনো টুকরোয় কেটে নিন।",
                        "একটি প্যানে মাঝারি-উচ্চ তাপে তিলের তেল গরম করুন।",
                        "টোফু কিউবগুলো সোনালী-বাদামী না হওয়া পর্যন্ত ৪ মিনিট ভাজুন।",
                        "এরপর মাশরুম, ব্রকলি ও সয়া সস দিয়ে আরও ৪ মিনিট মৃদু ভাঁজুন।",
                        "ভাজা তিল ছড়িয়ে মেখে নামিয়ে গরম গরম পরিবেশন করুন।"
                    ),
                    tip = "Soy protein has been linked directly with lowered risk of arterial cardiovascular plaques.",
                    tipBn = "সয়া প্রোটিন ধমনীতে চর্বী ও প্লাক জমার ঝুঁকি সরাসরি হ্রাস করতে কার্যকর ভূমিকা রাখে।"
                )
            )
            dao.insertRecipes(defaults)
        }
    }

    suspend fun generateMealPlanFromAI(
        profile: UserProfileEntity,
        date: String,
        context: Context
    ): Result<MealPlanEntity> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            // API key is missing or is the placeholder value.
            // Fallback to offline meal generation automatically.
            val offlinePlan = generateOfflineMealPlan(profile, date)
            dao.insertMealPlan(offlinePlan)
            dao.deleteShoppingItemsForDate(date)
            dao.insertShoppingItems(generateLocalShoppingList(offlinePlan))
            return@withContext Result.success(offlinePlan)
        }

        val promptText = """
            You are an AI Diet Planner. 
            Your task is to create a personalized daily meal plan based on the following user inputs:
            
            - Age: ${profile.age} years
            - Gender: ${profile.gender}
            - Weight: ${profile.weight} kg
            - Height: ${profile.height} cm
            - Goal: ${profile.goal}
            - Dietary Preference: ${profile.dietaryPreference}
            - Allergies: ${profile.allergies.ifBlank { "None" }}
            - Medical Conditions: ${profile.medicalConditions}
            - Cuisine Preference: ${profile.cuisinePreferences}
            
            Instructions:
            1. Calculate daily calorie needs using standard nutrition formulas.
            2. Distribute calories into 3 main meals and 2 snacks.
            3. Ensure balanced macros: 40% carbs, 30% protein, 30% fat.
            4. Suggest local food options aligned with the ${profile.cuisinePreferences} cuisine.
            5. Since they have ${profile.medicalConditions}, ensure the recipe choices and food items are tailored for this specific condition (e.g. low glycemic for Diabetes, low sodium for Hypertension).
            6. Provide portion sizes in grams or cups.
            7. Add one motivational tip for healthy living at the end.
            8. Calculate total daily calories based on the provided menu.
            9. Ensure all measurements are in metric units.
            
            Output format MUST strictly match the system's requested JSON schema.
        """.trimIndent()

        // Configure Gemini JSON response schema to get a fully parsed response
        val systemInstructionText = """
            You are an AI Diet Planner. You must respond ONLY with a JSON object matching this schema:
            {
              "totalCalories": Int,
              "breakfast": String,
              "breakfastCal": Int,
              "snack1": String,
              "snack1Cal": Int,
              "lunch": String,
              "lunchCal": Int,
              "snack2": String,
              "snack2Cal": Int,
              "dinner": String,
              "dinnerCal": Int,
              "dailyTip": String
            }
            All fields are required. Keep descriptions brief and focus on foods from the ${profile.cuisinePreferences} cuisine style that fit their ${profile.medicalConditions} conditions. Detail the exact portions (e.g., "Lal Attar Ruti (2 pieces, 60g) with 1 boiled egg").
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = promptText)))),
            generationConfig = GenerationConfig(
                responseFormat = ResponseFormat(
                    text = ResponseFormatText(mimeType = "application/json")
                ),
                temperature = 0.2f
            ),
            systemInstruction = Content(parts = listOf(Part(text = systemInstructionText)))
        )

        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: throw Exception("No response text found in Gemini payload")

            val adapter = RetrofitClient.moshiInstance.adapter(DietPlanJsonResponse::class.java)
            val parsedResult = adapter.fromJson(jsonText)
                ?: throw Exception("Failed to deserialize output into DietPlanJsonResponse")

            val mealPlan = MealPlanEntity(
                date = date,
                calorieTarget = parsedResult.totalCalories,
                breakfast = parsedResult.breakfast,
                breakfastCal = parsedResult.breakfastCal,
                snack1 = parsedResult.snack1,
                snack1Cal = parsedResult.snack1Cal,
                lunch = parsedResult.lunch,
                lunchCal = parsedResult.lunchCal,
                snack2 = parsedResult.snack2,
                snack2Cal = parsedResult.snack2Cal,
                dinner = parsedResult.dinner,
                dinnerCal = parsedResult.dinnerCal,
                dailyTip = parsedResult.dailyTip,
                rawResponse = jsonText
            )

            dao.insertMealPlan(mealPlan)
            dao.deleteShoppingItemsForDate(date)
            dao.insertShoppingItems(generateLocalShoppingList(mealPlan))
            Result.success(mealPlan)
        } catch (e: Exception) {
            e.printStackTrace()
            // In case of error (timeout, network down, rate limit, quota issue), gracefully fall back offline
            val offlinePlan = generateOfflineMealPlan(profile, date)
            dao.insertMealPlan(offlinePlan)
            dao.deleteShoppingItemsForDate(date)
            dao.insertShoppingItems(generateLocalShoppingList(offlinePlan))
            Result.success(offlinePlan)
        }
    }

    fun generateOfflineMealPlan(profile: UserProfileEntity, date: String): MealPlanEntity {
        // Calculate daily target calories locally (BMR based on Harris-Benedict)
        val bmr = if (profile.gender.lowercase(Locale.ROOT) == "male") {
            88.362 + (13.397 * profile.weight) + (4.799 * profile.height) - (5.677 * profile.age)
        } else {
            447.593 + (9.247 * profile.weight) + (3.098 * profile.height) - (4.330 * profile.age)
        }

        val multiplier = 1.25 // Lightly active multiplier
        val maintenanceCalories = (bmr * multiplier).toInt()
        val dailyTarget = when (profile.goal) {
            "Weight Loss", "ওজন কমানো" -> maintenanceCalories - 450
            "Weight Gain", "ওজন বাড়ানো" -> maintenanceCalories + 450
            else -> maintenanceCalories
        }.coerceAtLeast(1200)

        // Suggest clean Bangladeshi foods
        val pref = profile.dietaryPreference.lowercase(Locale.ROOT)
        val isVegOrVegan = pref.contains("veg")

        // Formulate portions based on calorie tiers
        val bCal = (dailyTarget * 0.25).toInt()
        val s1Cal = (dailyTarget * 0.10).toInt()
        val lCal = (dailyTarget * 0.35).toInt()
        val s2Cal = (dailyTarget * 0.10).toInt()
        val dCal = (dailyTarget * 0.20).toInt()

        val breakfast = if (isVegOrVegan) {
            "Lal Attar Ruti (2 pcs, 60g) + Mixed Vegetable Vaji with Soybean oil (1 cup, 150g) + Lentil Daal (0.5 cup)"
        } else {
            "Lal Attar Ruti (2 pcs, 60g) + Boiled Egg (1 pc) + Mixed Vegetable Vaji (0.5 cup, 75g)"
        }

        val snack1 = "Banana (সাগর কলা - 1 medium, 100g) or Native Apple + Hydration Water (1 glass)"

        val lunch = if (isVegOrVegan) {
            "Brown/Basmati Rice (1.5 cups, 200g) + Chola/Boot Daal (1 cup) + Bhorta/Vaji (0.5 cup) + Local Salad"
        } else {
            "Brown Rice (1.5 cups, 200g) + Baked Ruhi Fish / Chicken Curry (100g cooked) + Mixed Shobji Vaji (1 cup) + Thick Daal (0.5 cup)"
        }

        val snack2 = "Roasted Bengal Gram / Muri (মুড়ি - 1 cup, 30g) + Black Tea or Infused Lemon Water"

        val dinner = if (isVegOrVegan) {
            "Lal Attar Ruti (2 pcs, 60g) + Tofu or Paneer Curry (100g) + Broccoli or Cabbage Vaji"
        } else {
            "Lal Attar Ruti (2 pcs, 60g) or Steamed Rice (1 cup) + Chicken Breast Soup (100g cooked) + Sauteed Spinach (পালং শাক - 1 cup)"
        }

        val tips = listOf(
            "পর্যাপ্ত পানি পান করুন, এটি মেটাবলিজম বাড়াতে সাহায্য করে। (Stay hydrated, it helps boost metabolism.)",
            "খাবার ভালোভাবে চিবিয়ে ধীরে সুস্থে খান। (Chew food slowly to digest properly.)",
            "প্রতিদিন অন্তত ৭-৮ ঘণ্টা গভীর ঘুম নিশ্চিত করুন। (Get 7-8 hours of sound sleep daily.)",
            "চিনিযুক্ত পানীয় বা অতিরিক্ত চা-কফি পরিহার করার চেষ্টা করুন। (Avoid sugary drinks or extra caffeine.)",
            "প্রতিদিন অন্তত ৩০ মিনিট দ্রুত হাঁটার অভ্যাস গড়ে তুলুন। (Walk briskly for at least 30 minutes a day.)"
        )
        val randomTip = tips[date.hashCode().coerceAtLeast(0) % tips.size]

        return MealPlanEntity(
            date = date,
            calorieTarget = dailyTarget,
            breakfast = breakfast,
            breakfastCal = bCal,
            snack1 = snack1,
            snack1Cal = s1Cal,
            lunch = lunch,
            lunchCal = lCal,
            snack2 = snack2,
            snack2Cal = s2Cal,
            dinner = dinner,
            dinnerCal = dCal,
            dailyTip = randomTip,
            rawResponse = "Offline calculated meal plan"
        )
    }

    fun getLocalFoodLogsFlow(date: String, userId: String): Flow<List<FoodLogEntity>> = dao.getFoodLogsFlow(date, userId)
    suspend fun saveFoodLog(foodLog: FoodLogEntity) = withContext(Dispatchers.IO) {
        dao.insertFoodLog(foodLog)
    }
    suspend fun deleteFoodLog(id: Int) = withContext(Dispatchers.IO) {
        dao.deleteFoodLog(id)
    }

    fun getLocalExerciseLogsFlow(date: String): Flow<List<ExerciseLogEntity>> = dao.getExerciseLogsFlow(date)
    suspend fun saveExerciseLog(exerciseLog: ExerciseLogEntity) = withContext(Dispatchers.IO) {
        dao.insertExerciseLog(exerciseLog)
    }
    suspend fun deleteExerciseLog(id: Int) = withContext(Dispatchers.IO) {
        dao.deleteExerciseLog(id)
    }

    fun scheduleReminders(context: Context, reminders: List<MealReminderEntity>) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        for (reminder in reminders) {
            val isWater = reminder.name.lowercase().contains("water") || reminder.name.contains("পানি")
            
            // Craft dynamic consistency titles & messages
            val titleText = if (isWater) {
                "💧 Water Hydration Alert!"
            } else {
                "⏰ Log Meal Tracker: ${reminder.name}"
            }

            val msgText = if (isWater) {
                "Time to drink water! Record it in the app now to keep your hydration streak consistent / পানি পানের ফ্রেশ রেকর্ড রাখুন!"
            } else {
                "It's time to eat! Please log your meal now to maintain your tracking consistency and hit your goals / এখনই খাবারটি রেকর্ড করুন!"
            }

            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra("title", titleText)
                putExtra("message", msgText)
                putExtra("notification_id", reminder.id)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                reminder.id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (!reminder.isEnabled) {
                alarmManager.cancel(pendingIntent)
                continue
            }

            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, reminder.hour)
                set(Calendar.MINUTE, reminder.minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (before(Calendar.getInstance())) {
                    add(Calendar.DATE, 1)
                }
            }

            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
        }
    }

    private fun generateLocalShoppingList(mealPlan: MealPlanEntity): List<ShoppingItemEntity> {
        val items = mutableListOf<ShoppingItemEntity>()
        val mealsText = listOf(
            mealPlan.breakfast,
            mealPlan.snack1,
            mealPlan.lunch,
            mealPlan.snack2,
            mealPlan.dinner
        )
        val seen = mutableSetOf<String>()
        for (meal in mealsText) {
            // Split by "+", "or", "and", ",", unique values
            val parts = meal.split(Regex("[+\\n,|]"))
            for (part in parts) {
                val trimmed = part.trim()
                if (trimmed.length > 2 && !trimmed.contains("Water", ignoreCase = true) && !trimmed.contains("পানি", ignoreCase = true)) {
                    val openIndex = trimmed.indexOf('(')
                    val closeIndex = trimmed.indexOf(')')
                    val name: String
                    val quantity: String
                    if (openIndex != -1 && closeIndex != -1 && closeIndex > openIndex) {
                        name = trimmed.substring(0, openIndex).trim()
                        quantity = trimmed.substring(openIndex + 1, closeIndex).trim()
                    } else {
                        name = trimmed
                        quantity = "As needed"
                    }
                    val lower = name.lowercase(Locale.ROOT)
                    if (lower.isNotBlank() && !seen.contains(lower)) {
                        seen.add(lower)
                        items.add(
                            ShoppingItemEntity(
                                date = mealPlan.date,
                                name = name,
                                quantity = quantity,
                                isChecked = false
                            )
                        )
                    }
                }
            }
        }
        return items
    }
}
