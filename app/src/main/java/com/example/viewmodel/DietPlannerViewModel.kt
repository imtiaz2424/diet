package com.example.viewmodel

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.api.*
import com.example.data.model.*
import com.example.data.repository.DietPlannerRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*

data class FoodSearchResult(
    val name: String,
    val calories: Int,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val imageUrl: String
)

class DietPlannerViewModel(
    private val repository: DietPlannerRepository,
    context: Context
) : ViewModel() {

    val authManager = com.example.data.auth.FirebaseAuthManager(context.applicationContext)

    val currentUserId: StateFlow<String> = authManager.currentUser
        .map { it?.uid ?: "guest" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "guest")

    val isLoggedIn: StateFlow<Boolean> = authManager.currentUser
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isFirebaseReal: StateFlow<Boolean> = authManager.isFirebaseReal

    val userProfile: StateFlow<UserProfileEntity?> = currentUserId
        .flatMapLatest { uid -> repository.getUserProfile(uid) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentMoodLogs: StateFlow<List<MoodLogEntity>> = currentUserId
        .flatMapLatest { uid -> repository.getMoodLogs(uid) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allWeightLogs: StateFlow<List<WeightLogEntity>> = repository.allWeightLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allReminders: StateFlow<List<MealReminderEntity>> = repository.allReminders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedDate = MutableStateFlow(getTodayDateString())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    // Dynamic state flow observing changes based on selectedDate
    val currentMealPlan: StateFlow<MealPlanEntity?> = _selectedDate
        .flatMapLatest { date -> repository.getMealPlanFlow(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allMealPlans: StateFlow<List<MealPlanEntity>> = repository.getAllMealPlansFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val waterLog: StateFlow<WaterLogEntity?> = _selectedDate
        .flatMapLatest { date -> repository.getWaterLogFlow(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val shoppingItems: StateFlow<List<ShoppingItemEntity>> = _selectedDate
        .flatMapLatest { date -> repository.getShoppingItemsFlow(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _eventMessage = MutableStateFlow<String?>(null)
    val eventMessage: StateFlow<String?> = _eventMessage.asStateFlow()

    private val _recipeCheckedIngredients = MutableStateFlow<Map<String, Set<Int>>>(emptyMap())
    val recipeCheckedIngredients: StateFlow<Map<String, Set<Int>>> = _recipeCheckedIngredients.asStateFlow()

    val allRecipes: StateFlow<List<RecipeEntity>> = repository.allRecipesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleRecipeFavorite(id: Int, isFavorite: Boolean) {
        viewModelScope.launch {
            repository.toggleRecipeFavorite(id, isFavorite)
        }
    }

    fun toggleRecipeIngredientChecked(recipeTitle: String, index: Int) {
        val currentMap = _recipeCheckedIngredients.value
        val currentSet = currentMap[recipeTitle] ?: emptySet()
        val newSet = if (currentSet.contains(index)) {
            currentSet - index
        } else {
            currentSet + index
        }
        _recipeCheckedIngredients.value = currentMap + (recipeTitle to newSet)
    }

    init {
        viewModelScope.launch {
            repository.preloadDefaultRemindersIfEmpty()
            repository.preloadDefaultRecipesIfEmpty()
        }
        viewModelScope.launch {
            currentUserId.collect { uid ->
                if (uid.isNotEmpty()) {
                    val existing = repository.getUserProfileDirect(uid)
                    if (existing == null) {
                        val profile = UserProfileEntity(
                            id = uid,
                            age = 25,
                            gender = "Male",
                            weight = 70.0,
                            height = 175.0,
                            goal = "Maintain",
                            dietaryPreference = "Non-Vegetarian",
                            allergies = "",
                            dailyCalorieTarget = 2000,
                            dailyWaterTargetMl = 2500,
                            medicalConditions = "None",
                            cuisinePreferences = "Bengali"
                        )
                        repository.saveUserProfile(profile)
                    }
                }
            }
        }
    }

    fun selectDate(dateString: String) {
        _selectedDate.value = dateString
    }

    fun clearEventMessage() {
        _eventMessage.value = null
    }

    fun toggleShoppingItemChecked(id: Int, isChecked: Boolean) {
        viewModelScope.launch {
            repository.updateShoppingItemChecked(id, isChecked)
        }
    }

    fun addManualShoppingItem(name: String, quantity: String) {
        viewModelScope.launch {
            val date = _selectedDate.value
            repository.addShoppingItem(ShoppingItemEntity(date = date, name = name, quantity = quantity))
        }
    }

    fun clearShoppingListForSelectedDate() {
        viewModelScope.launch {
            repository.deleteShoppingItems(_selectedDate.value)
        }
    }

    fun saveProfile(
        age: Int,
        gender: String,
        weight: Double,
        height: Double,
        goal: String,
        dietaryPreference: String,
        allergies: String,
        medicalConditions: String = "None",
        cuisinePreferences: String = "Bengali"
    ) {
        viewModelScope.launch {
            // Harris-Benedict formula offline baseline target calories
            val bmr = if (gender.lowercase(Locale.ROOT) == "male") {
                88.362 + (13.397 * weight) + (4.799 * height) - (5.677 * age)
            } else {
                447.593 + (9.247 * weight) + (3.098 * height) - (4.330 * age)
            }
            val maintenance = (bmr * 1.25).toInt()
            val targetCalories = when (goal) {
                "Weight Loss", "ওজন কমানো" -> maintenance - 450
                "Weight Gain", "ওজন বাড়ানো" -> maintenance + 450
                else -> maintenance
            }.coerceAtLeast(1200)

            val profile = UserProfileEntity(
                id = currentUserId.value,
                age = age,
                gender = gender,
                weight = weight,
                height = height,
                goal = goal,
                dietaryPreference = dietaryPreference,
                allergies = allergies,
                dailyCalorieTarget = targetCalories,
                dailyWaterTargetMl = 2500,
                medicalConditions = medicalConditions,
                cuisinePreferences = cuisinePreferences
            )

            repository.saveUserProfile(profile)

            // Log current weight for history as well
            repository.saveWeightLog(weight, getTodayDateString())

            _eventMessage.value = "পেশাদার প্রোফাইল সফলভাবে আপডেট করা হয়েছে! (User Profile updated successfully!)"
        }
    }

    fun updateHealthPreferences(medicalConditions: List<String>, cuisinePreferences: List<String>) {
        viewModelScope.launch {
            userProfile.value?.let { profile ->
                val updatedProfile = profile.copy(
                    medical_conditions = medicalConditions,
                    cuisine_preferences = cuisinePreferences
                )
                repository.saveUserProfile(updatedProfile)
                _eventMessage.value = "স্বাস্থ্য ও স্বাদের পছন্দসমূহ সংরক্ষণ করা হয়েছে! (Health & Taste preferences persisted!)"
            }
        }
    }

    fun generateMealPlan(context: Context) {
        val profile = userProfile.value ?: return
        val date = _selectedDate.value
        viewModelScope.launch {
            _isGenerating.value = true
            val result = repository.generateMealPlanFromAI(profile, date, context)
            _isGenerating.value = false
            if (result.isSuccess) {
                _eventMessage.value = "নতুন ডায়েট প্ল্যান সফলভাবে জেনারেট করা হয়েছে! (New diet plan updated!)"
            } else {
                _eventMessage.value = "ত্রুটি: ডায়েট প্ল্যান জেনারেট করা যায়নি। অফলাইন প্ল্যান লোড করা হয়েছে। (Failed to generate. Meal plan loaded offline.)"
            }
        }
    }

    fun addWater(amountMl: Int) {
        val date = _selectedDate.value
        viewModelScope.launch {
            val currentLog = repository.getWaterLogFlow(date).first()
            val currentAmount = currentLog?.amountMl ?: 0
            val target = userProfile.value?.dailyWaterTargetMl ?: 2500
            val newAmount = (currentAmount + amountMl).coerceAtLeast(0)
            repository.saveWaterLog(WaterLogEntity(date = date, amountMl = newAmount))
        }
    }

    fun logWeight(weight: Double, date: String) {
        viewModelScope.launch {
            repository.saveWeightLog(weight, date)
            // Update profile weight as well if logs correspond to today
            userProfile.value?.let { profile ->
                if (date == getTodayDateString()) {
                    repository.saveUserProfile(profile.copy(weight = weight))
                }
            }
            _eventMessage.value = "ওজন ট্র্যাকিং রেকর্ড আপডেট করা হয়েছে!"
        }
    }

    fun deleteWeight(date: String) {
        viewModelScope.launch {
            repository.deleteWeightLog(date)
            _eventMessage.value = "রেকর্ড সফলভাবে মুছে ফেলা হয়েছে!"
        }
    }

    fun updateReminderTime(context: Context, id: Int, name: String, hour: Int, minute: Int, isEnabled: Boolean) {
        viewModelScope.launch {
            val reminder = MealReminderEntity(id, name, hour, minute, isEnabled)
            repository.saveReminder(reminder)

            // Re-schedule alarms
            val all = repository.allReminders.first()
            val updatedList = all.map { if (it.id == id) reminder else it }
            repository.scheduleReminders(context, updatedList)
            _eventMessage.value = "রিমাইন্ডার পছন্দসমূহ আপডেট করা হয়েছে!"
        }
    }

    fun addCustomReminder(context: Context, name: String, hour: Int, minute: Int) {
        viewModelScope.launch {
            val randomId = (System.currentTimeMillis() % 1000000).toInt() + 10
            val reminder = MealReminderEntity(randomId, name, hour, minute, true)
            repository.saveReminder(reminder)
            
            val updatedList = repository.allReminders.first()
            repository.scheduleReminders(context, updatedList)
            _eventMessage.value = if (_isBengali.value) {
                "নতুন রিমাইন্ডার যোগ করা হয়েছে: $name"
            } else {
                "New reminder added: $name"
            }
        }
    }

    fun deleteReminder(context: Context, id: Int) {
        viewModelScope.launch {
            repository.deleteReminder(id)
            val updatedList = repository.allReminders.first()
            repository.scheduleReminders(context, updatedList)
            _eventMessage.value = if (_isBengali.value) {
                "রিমাইন্ডারটি সফলভাবে ডিলিট করা হয়েছে!"
            } else {
                "Reminder deleted successfully!"
            }
        }
    }

    fun exportPdfReport(context: Context, mealPlan: MealPlanEntity, profile: UserProfileEntity): File? {
        try {
            val pdfDocument = PdfDocument()
            val paint = Paint()
            val titlePaint = Paint()

            // Page info
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
            val page = pdfDocument.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            // Background & Title Styling
            canvas.drawColor(Color.WHITE)

            // Primary Brand Title banner
            titlePaint.color = Color.rgb(46, 125, 50) // Green theme
            titlePaint.textSize = 24f
            titlePaint.isFakeBoldText = true
            canvas.drawText("AI Diet Planner Report (বাংলাদেশ)", 40f, 60f, titlePaint)

            // Sub-title
            paint.color = Color.DKGRAY
            paint.textSize = 12f
            canvas.drawText("Generated on: ${mealPlan.date}", 40f, 95f, paint)

            // Line separation
            paint.strokeWidth = 2f
            canvas.drawLine(40f, 110f, 555f, 110f, paint)

            // User Info Section
            paint.textSize = 14f
            paint.isFakeBoldText = true
            paint.color = Color.BLACK
            canvas.drawText("ইউজার প্রোফাইল ও টার্গেট (User Profile & Target):", 40f, 140f, paint)

            paint.isFakeBoldText = false
            paint.textSize = 11f
            paint.color = Color.rgb(60, 60, 60)
            canvas.drawText("বয়স (Age): ${profile.age} বছর   |   লিঙ্গ (Gender): ${profile.gender}", 40f, 170f, paint)
            canvas.drawText("উচ্চতা (Height): ${profile.height} সেমি   |   ওজন (Weight): ${profile.weight} কেজি", 40f, 190f, paint)
            canvas.drawText("খাদ্য পছন্দ (Preference): ${profile.dietaryPreference}", 40f, 210f, paint)
            canvas.drawText("অ্যালার্জি (Allergies): ${profile.allergies.ifBlank { "None" }}", 40f, 230f, paint)
            canvas.drawText("লক্ষ্য (Goal): ${profile.goal}   |   ক্যালোরি লক্ষ্য: ${mealPlan.calorieTarget} kcal", 40f, 250f, paint)

            canvas.drawLine(40f, 270f, 555f, 270f, paint)

            // Meal details
            paint.textSize = 14f
            paint.isFakeBoldText = true
            paint.color = Color.rgb(46, 125, 50)
            canvas.drawText("প্রতিদিনের খাবার রূপরেখা (Daily Meal Plan):", 40f, 300f, paint)

            paint.textSize = 11f
            paint.isFakeBoldText = false
            paint.color = Color.BLACK

            var currentY = 330f
            val meals = listOf(
                "Breakfast (সকালের নাস্তা) [${mealPlan.breakfastCal} kcal]" to mealPlan.breakfast,
                "Snack 1 (সকালের হালকা খাবার) [${mealPlan.snack1Cal} kcal]" to mealPlan.snack1,
                "Lunch (দুপুরের খাবার) [${mealPlan.lunchCal} kcal]" to mealPlan.lunch,
                "Snack 2 (বিকালের হালকা খাবার) [${mealPlan.snack2Cal} kcal]" to mealPlan.snack2,
                "Dinner (রাতের খাবার) [${mealPlan.dinnerCal} kcal]" to mealPlan.dinner
            )

            for ((label, detail) in meals) {
                paint.isFakeBoldText = true
                paint.color = Color.rgb(46, 125, 50)
                canvas.drawText(label, 40f, currentY, paint)
                currentY += 20f

                paint.isFakeBoldText = false
                paint.color = Color.DKGRAY

                // Handle text wrap for details
                val words = detail.split(" ")
                var line = ""
                for (word in words) {
                    val testLine = if (line.isEmpty()) word else "$line $word"
                    val measure = paint.measureText(testLine)
                    if (measure > 515f) {
                        canvas.drawText(line, 45f, currentY, paint)
                        currentY += 15f
                        line = word
                    } else {
                        line = testLine
                    }
                }
                if (line.isNotEmpty()) {
                    canvas.drawText(line, 45f, currentY, paint)
                    currentY += 20f
                }
                currentY += 5f
            }

            canvas.drawLine(40f, currentY, 555f, currentY, paint)
            currentY += 20f

            // Day tip
            paint.isFakeBoldText = true
            paint.color = Color.BLACK
            canvas.drawText("Daily Tip (টিপস):", 40f, currentY, paint)
            currentY += 18f
            paint.isFakeBoldText = false
            paint.color = Color.rgb(100, 30, 22)
            canvas.drawText(mealPlan.dailyTip, 45f, currentY, paint)

            pdfDocument.finishPage(page)

            val dir = context.getExternalFilesDir(null)
            val file = File(dir, "AI_Diet_Planner_Report_${mealPlan.date}.pdf")
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    val currentFoodLogs: StateFlow<List<FoodLogEntity>> = _selectedDate
        .combine(currentUserId) { date, uid -> Pair(date, uid) }
        .flatMapLatest { (date, uid) -> repository.getLocalFoodLogsFlow(date, uid) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentExerciseLogs: StateFlow<List<ExerciseLogEntity>> = _selectedDate
        .flatMapLatest { date -> repository.getLocalExerciseLogsFlow(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Language state: true for Bengali (default), false for English
    private val _isBengali = MutableStateFlow(true)
    val isBengali: StateFlow<Boolean> = _isBengali.asStateFlow()

    // Theme state: true for Dark Mode, false for Light Mode
    private val _isDarkTheme = MutableStateFlow(false)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    fun setInitialDarkTheme(isDark: Boolean) {
        _isDarkTheme.value = isDark
    }

    // Target Weight Goal State
    private val _targetWeight = MutableStateFlow(0.0)
    val targetWeight: StateFlow<Double> = _targetWeight.asStateFlow()

    fun setInitialTargetWeight(weight: Double) {
        _targetWeight.value = weight
    }

    fun saveTargetWeight(context: Context, weight: Double) {
        _targetWeight.value = weight
        val sharedPrefs = context.getSharedPreferences("suvecha_settings", Context.MODE_PRIVATE)
        sharedPrefs.edit().putFloat("target_weight", weight.toFloat()).apply()
        _eventMessage.value = if (_isBengali.value) {
            "আপনার লক্ষ্য ওজন সেট করা হয়েছে: $weight কেজি!"
        } else {
            "Your target weight goal has been set to: $weight kg!"
        }
    }

    fun toggleTheme(context: Context) {
        val newValue = !_isDarkTheme.value
        _isDarkTheme.value = newValue
        val sharedPrefs = context.getSharedPreferences("suvecha_settings", Context.MODE_PRIVATE)
        sharedPrefs.edit().putBoolean("dark_mode", newValue).apply()
        _eventMessage.value = if (_isBengali.value) {
            if (newValue) "ডার্ক মোড সক্রিয় করা হয়েছে!" else "লাইট মোড সক্রিয় করা হয়েছে!"
        } else {
            if (newValue) "Dark mode enabled!" else "Light mode enabled!"
        }
    }

    // Login/account state (delegated to FirebaseAuthManager)
    fun toggleLanguage() {
        _isBengali.value = !_isBengali.value
    }

    fun login() {
        // Fallback for simple calls if any exist
        login("user@subecha.com", "password123") { _, _ -> }
    }

    fun login(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        authManager.signIn(email, password) { success, error ->
            if (success) {
                _eventMessage.value = if (_isBengali.value) "সফলভাবে লগইন সম্পূর্ণ হয়েছে!" else "Logged in successfully!"
            }
            onResult(success, error)
        }
    }

    fun signup(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        authManager.signUp(email, password) { success, error ->
            if (success) {
                _eventMessage.value = if (_isBengali.value) "সফলভাবে রেজিস্ট্রেশন সম্পূর্ণ হয়েছে!" else "Signed up successfully!"
            }
            onResult(success, error)
        }
    }

    fun logout() {
        authManager.signOut()
        _eventMessage.value = if (_isBengali.value) "সফলভাবে লগআউট সম্পূর্ণ হয়েছে!" else "Logged out successfully!"
    }

    fun addFoodLog(name: String, calories: Int, protein: Double = 0.0, carbs: Double = 0.0, fat: Double = 0.0) {
        val date = _selectedDate.value
        val uid = currentUserId.value
        viewModelScope.launch {
            repository.saveFoodLog(FoodLogEntity(userId = uid, date = date, name = name, calories = calories, protein = protein, carbs = carbs, fat = fat))
            _eventMessage.value = if (_isBengali.value) "খাবার সফলভাবে যোগ করা হয়েছে!" else "Food logged successfully!"
        }
    }

    fun saveMoodLog(mood: String, note: String, food: String, activity: String) {
        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val uid = currentUserId.value
        viewModelScope.launch {
            repository.saveMoodLog(MoodLogEntity(userId = uid, date = todayDate, mood = mood, note = note, food = food, activity = activity))
            _eventMessage.value = if (_isBengali.value) "আবেগ ডায়েরি সফলভাবে সংরক্ষণ করা হয়েছে!" else "Mood logged successfully!"
        }
    }

    fun deleteMoodLog(id: Int) {
        viewModelScope.launch {
            repository.deleteMoodLog(id)
            _eventMessage.value = if (_isBengali.value) "আবেগ ডায়েরি মুছে ফেলা হয়েছে!" else "Mood log deleted!"
        }
    }

    fun deleteFoodLog(id: Int) {
        viewModelScope.launch {
            repository.deleteFoodLog(id)
            _eventMessage.value = if (_isBengali.value) "খাবার মুছে ফেলা হয়েছে!" else "Food log deleted!"
        }
    }

    fun addExerciseLog(activity: String, durationMin: Int, caloriesBurned: Int) {
        val date = _selectedDate.value
        viewModelScope.launch {
            repository.saveExerciseLog(ExerciseLogEntity(date = date, activity = activity, durationMin = durationMin, caloriesBurned = caloriesBurned))
            _eventMessage.value = if (_isBengali.value) "ব্যায়াম সফলভাবে যোগ করা হয়েছে!" else "Exercise logged successfully!"
        }
    }

    fun deleteExerciseLog(id: Int) {
        viewModelScope.launch {
            repository.deleteExerciseLog(id)
            _eventMessage.value = if (_isBengali.value) "ব্যায়াম মুছে ফেলা হয়েছে!" else "Exercise log deleted!"
        }
    }

    // Open Food Facts Search results
    private val _searchResults = MutableStateFlow<List<FoodSearchResult>>(emptyList())
    val searchResults: StateFlow<List<FoodSearchResult>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    fun searchFood(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _isSearching.value = true
            try {
                val client = okhttp3.OkHttpClient()
                val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
                val url = "https://world.openfoodfacts.org/cgi/search.pl?search_terms=$encodedQuery&search_simple=1&action=process&json=1"
                val request = okhttp3.Request.Builder()
                    .url(url)
                    .header("User-Agent", "SuvechaApp - Android - Version 1.0")
                    .build()
                
                withContext(Dispatchers.IO) {
                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val body = response.body?.string()
                            if (body != null) {
                                val json = org.json.JSONObject(body)
                                val products = json.optJSONArray("products")
                                val results = mutableListOf<FoodSearchResult>()
                                if (products != null) {
                                    val count = minOf(products.length(), 25)
                                    for (i in 0 until count) {
                                        val prod = products.optJSONObject(i) ?: continue
                                        val productName = prod.optString("product_name") ?: ""
                                        val brands = prod.optString("brands") ?: ""
                                        val imageUrl = prod.optString("image_front_thumb_url") ?: ""
                                        val nutriments = prod.optJSONObject("nutriments")
                                        val calories = nutriments?.optDouble("energy-kcal_100g") ?: nutriments?.optDouble("energy-kcal") ?: 0.0
                                        val protein = nutriments?.optDouble("proteins_100g") ?: 0.0
                                        val carbs = nutriments?.optDouble("carbohydrates_100g") ?: 0.0
                                        val fat = nutriments?.optDouble("fat_100g") ?: 0.0
                                        
                                        val dispName = if (brands.isNotBlank()) "$productName ($brands)" else productName
                                        if (productName.isNotBlank()) {
                                            results.add(
                                                FoodSearchResult(
                                                    name = dispName,
                                                    calories = calories.toInt(),
                                                    protein = protein,
                                                    carbs = carbs,
                                                    fat = fat,
                                                    imageUrl = imageUrl
                                                )
                                            )
                                        }
                                    }
                                }
                                _searchResults.value = results
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isSearching.value = false
            }
        }
    }

    private val _scannedProduct = MutableStateFlow<FoodSearchResult?>(null)
    val scannedProduct: StateFlow<FoodSearchResult?> = _scannedProduct.asStateFlow()

    private val _isScanningBarcode = MutableStateFlow(false)
    val isScanningBarcode: StateFlow<Boolean> = _isScanningBarcode.asStateFlow()

    fun scanBarcode(barcode: String) {
        if (barcode.isBlank()) return
        viewModelScope.launch {
            _isScanningBarcode.value = true
            _scannedProduct.value = null
            try {
                val client = okhttp3.OkHttpClient()
                val url = "https://world.openfoodfacts.org/api/v0/product/$barcode.json"
                val request = okhttp3.Request.Builder()
                    .url(url)
                    .header("User-Agent", "SuvechaApp - Android - Version 1.0")
                    .build()
                
                withContext(Dispatchers.IO) {
                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val body = response.body?.string()
                            if (body != null) {
                                val json = org.json.JSONObject(body)
                                val status = json.optInt("status")
                                if (status == 1) {
                                    val prod = json.optJSONObject("product")
                                    if (prod != null) {
                                        val productName = prod.optString("product_name") ?: "Unknown Packed Item"
                                        val brands = prod.optString("brands") ?: ""
                                        val imageUrl = prod.optString("image_front_thumb_url") ?: ""
                                        val nutriments = prod.optJSONObject("nutriments")
                                        val calories = nutriments?.optDouble("energy-kcal_100g") ?: nutriments?.optDouble("energy-kcal") ?: 0.0
                                        val protein = nutriments?.optDouble("proteins_100g") ?: 0.0
                                        val carbs = nutriments?.optDouble("carbohydrates_100g") ?: 0.0
                                        val fat = nutriments?.optDouble("fat_100g") ?: 0.0
                                        
                                        val dispName = if (brands.isNotBlank()) "$productName ($brands)" else productName
                                        _scannedProduct.value = FoodSearchResult(
                                            name = dispName,
                                            calories = calories.toInt(),
                                            protein = protein,
                                            carbs = carbs,
                                            fat = fat,
                                            imageUrl = imageUrl
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isScanningBarcode.value = false
            }
        }
    }

    private val _aiImageResult = MutableStateFlow<String?>(null)
    val aiImageResult: StateFlow<String?> = _aiImageResult.asStateFlow()

    private val _isAnalyzingImage = MutableStateFlow(false)
    val isAnalyzingImage: StateFlow<Boolean> = _isAnalyzingImage.asStateFlow()

    fun analyzeFoodImage(dishName: String) {
        val apiKey = com.example.BuildConfig.GEMINI_API_KEY
        viewModelScope.launch {
            _isAnalyzingImage.value = true
            _aiImageResult.value = null
            if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                delay(1200)
                _aiImageResult.value = getOfflineImageEstimation(dishName)
                _isAnalyzingImage.value = false
                return@launch
            }

            val promptText = """
                You are a nutrition AI expert. Analyze the following local Bengali or international dish name: "$dishName".
                Calculate:
                1. Estimated serving size (e.g. 1 plate or 200g)
                2. Average total calories (kcal)
                3. Protein weight (g)
                4. Carbohydrates weight (g)
                5. Fat weight (g)
                6. Brief tips on nutrition and health value of this dish.
                
                Please format your response clearly in a highly readable summary, using English and Bengali translations where appropriate. Use bullet points and clean structure.
            """.trimIndent()

            val request = GenerateContentRequest(
                contents = listOf(Content(parts = listOf(Part(text = promptText)))),
                generationConfig = GenerationConfig(temperature = 0.3f)
            )

            try {
                val response = RetrofitClient.service.generateContent(apiKey, request)
                val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                _aiImageResult.value = text ?: "Could not analyze the food image."
            } catch (e: Exception) {
                e.printStackTrace()
                _aiImageResult.value = getOfflineImageEstimation(dishName)
            } finally {
                _isAnalyzingImage.value = false
            }
        }
    }

    private fun getOfflineImageEstimation(dish: String): String {
        return when {
            dish.lowercase(Locale.ROOT).contains("biryani") || dish.contains("বিরিয়ানি") -> """
                Estimated Serving Size: 1 plate (approx 350g)
                Calories: ~750 kcal
                Protein: 28g | Carbs: 85g | Fat: 32g
                
                💡 Health Note: High calorie and fat content. Try to control portions, pair with plenty of fresh salads, and balance your next meals.
            """.trimIndent()
            dish.lowercase(Locale.ROOT).contains("egg") || dish.contains("ডিম") -> """
                Estimated Serving Size: 2 Lal Attar Ruti + 1 Boiled Egg + Vegetables (300g)
                Calories: ~380 kcal
                Protein: 18g | Carbs: 48g | Fat: 11g
                
                💡 Health Note: Excellent balanced breakfast rich in fiber, complex carbohydrates, and high-quality protein. Highly recommended!
            """.trimIndent()
            dish.lowercase(Locale.ROOT).contains("fish") || dish.contains("মাছ") -> """
                Estimated Serving Size: 1 cup Steamed Rice + 1 medium piece Fish Curry (e.g., Ruhi) with gravy (300g)
                Calories: ~480 kcal
                Protein: 24g | Carbs: 65g | Fat: 13g
                
                💡 Health Note: Traditional balanced Bengali dish. Ruhi provides heart-healthy Omega-3 fatty acids. Try to keep rice portions moderate.
            """.trimIndent()
            else -> """
                Estimated Serving Size: 1 Standard Portion (approx 250g)
                Calories: ~320 kcal
                Protein: 14g | Carbs: 45g | Fat: 9g
                
                💡 Health Note: Good choice! Make sure to log details to track your daily progress and hit goals easily.
            """.trimIndent()
        }
    }

    private val _suggestedRecipes = MutableStateFlow<String?>(null)
    val suggestedRecipes: StateFlow<String?> = _suggestedRecipes.asStateFlow()

    private val _isGeneratingRecipes = MutableStateFlow(false)
    val isGeneratingRecipes: StateFlow<Boolean> = _isGeneratingRecipes.asStateFlow()

    fun generateRecipeSuggestions(mealPlanDetails: String) {
        val apiKey = com.example.BuildConfig.GEMINI_API_KEY
        viewModelScope.launch {
            _isGeneratingRecipes.value = true
            _suggestedRecipes.value = null
            if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                delay(1200)
                _suggestedRecipes.value = getOfflineRecipes(mealPlanDetails)
                _isGeneratingRecipes.value = false
                return@launch
            }

            val promptText = """
                Based on this meal/diet plan segment: "$mealPlanDetails", suggest 3 incredibly delicious, simple, and healthy cooking recipes designed for Bengali households.
                For each recipe, specify:
                1. Recipe Name
                2. Ingredients list (locally available in Bangladesh)
                3. Step-by-step instructions (short and simple)
                4. Cooking time & Calorie target
                
                Output in a warm, helpful format, supporting both English and Bengali translations where appropriate.
            """.trimIndent()

            val request = GenerateContentRequest(
                contents = listOf(Content(parts = listOf(Part(text = promptText)))),
                generationConfig = GenerationConfig(temperature = 0.4f)
            )

            try {
                val response = RetrofitClient.service.generateContent(apiKey, request)
                val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                _suggestedRecipes.value = text ?: "No recipes generated. Please try again."
            } catch (e: Exception) {
                e.printStackTrace()
                _suggestedRecipes.value = getOfflineRecipes(mealPlanDetails)
            } finally {
                _isGeneratingRecipes.value = false
            }
        }
    }

    private fun getOfflineRecipes(details: String): String {
        return """
            🥘 **১. সবজি ডাল খিচুড়ি (Healthy Vegetable Khichuri)**
            * **время:** ২৫ মিনিট | **ক্যালোরি:** ৩১০ kcal (প্রতি বাটি)
            * **উপকরণ:** লাল চাল, মুগ ডাল, গাজর, পেঁপে, পালং শাক, হালকা হলুদ ও সরিষার তেল।
            * **প্রণালী:** ডাল ও চাল হালকা ভেজে নিন। সবজি কুচি ও মশলা মিশিয়ে প্রেসার কুকারে অথবা হাঁড়িতে সেদ্ধ করুন। কম তেলে পরিবেশন করুন।
            
            🥣 **২. ওটস ফ্রুট বোল (Healthy Diet Oats Bowl)**
            * **সময়:** ১০ মিনিট | **ক্যালোরি:** ২৪০ kcal
            * **উপকরণ:** ওটস ০.৫ কাপ, টক দই বা ফ্যাট-ফ্রি দুধ, চিয়া সিড, পাকা কলা বা আম।
            * **প্রণালী:** ওটস হালকা গরম দুধে ভিজিয়ে রাখুন। উপরে স্লাইস করা ফল ও চিয়া সিড সাজিয়ে ঠান্ডা ঠান্ডা উপভোগ করুন।
            
            🐟 **৩. রুই মাছ বা ভাপ্পা (Steamed Baked Fish)**
            * **সময়:** ২০ মিনিট | **ক্যালোরি:** ২৭০ kcal
            * **উপকরণ:** রুপচাঁদা বা রুই মাছ ১ টুকরো, সরিষা বাটা ১ চামচ, কাঁচামরিচ, লেবুর রস ও ১/২ চামচ সরিষার তেল।
            * **প্রণালী:** মাছে মশলা মাখিয়ে কলাপাতা বা ফয়েল পেপারে মুড়ে ভাপে সিদ্ধ বা বেক করুন। গরম ভাতের সাথে দারুণ পুষ্টিকর!
        """.trimIndent()
    }

    fun generateRecipesForRemainingTargets(
        remainingCalories: Int,
        remainingProtein: Double,
        remainingCarbs: Double,
        remainingFat: Double,
        dietaryPref: String,
        isBengali: Boolean
    ) {
        val apiKey = com.example.BuildConfig.GEMINI_API_KEY
        viewModelScope.launch {
            _isGeneratingRecipes.value = true
            _suggestedRecipes.value = null
            if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                delay(1200)
                _suggestedRecipes.value = getOfflineRecipesForRemaining(remainingCalories, remainingProtein, remainingCarbs, remainingFat, isBengali)
                _isGeneratingRecipes.value = false
                return@launch
            }

            val promptText = """
                Based on the user's current day metrics, they have the following remaining dietary targets to complete their daily goals:
                - Remaining Calories: $remainingCalories kcal
                - Remaining Protein: ${String.format("%.1f", remainingProtein)}g
                - Remaining Carbohydrates: ${String.format("%.1f", remainingCarbs)}g
                - Remaining Fat: ${String.format("%.1f", remainingFat)}g
                - Dietary Preference: $dietaryPref
                
                Your task:
                Provide exactly 3 custom-crafted healthy healthy meals/recipes that perfectly fit within these remaining targets (both collectively or as alternative single-dish solutions).
                These recipes must be customized for Bengali/South Asian households, using ingredients commonly available in local grocery stores or markets.
                
                For each recipe, specify:
                1. Recipe Name
                2. Why it fits (explain the macronutrient profile matching the remaining targets)
                3. Ingredients list (with locally available materials in Bangladesh)
                4. Clear step-by-step cooking preparation instructions (short and simple)
                5. Total Prep/Cooking Time, and Nutritional Values (Calories, Carbs, Protein, Fat)
                
                Provide the response in a very neat, structured, and easy-to-read markdown format. Use ${if (isBengali) "Bengali language with English terms in brackets" else "English language"}.
            """.trimIndent()

            val request = GenerateContentRequest(
                contents = listOf(Content(parts = listOf(Part(text = promptText)))),
                generationConfig = GenerationConfig(temperature = 0.4f)
            )

            try {
                val response = RetrofitClient.service.generateContent(apiKey, request)
                val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                _suggestedRecipes.value = text ?: "No customized recipes generated. Please try again."
            } catch (e: Exception) {
                e.printStackTrace()
                _suggestedRecipes.value = getOfflineRecipesForRemaining(remainingCalories, remainingProtein, remainingCarbs, remainingFat, isBengali)
            } finally {
                _isGeneratingRecipes.value = false
            }
        }
    }

    private fun getOfflineRecipesForRemaining(
        remainingCalories: Int,
        remainingProtein: Double,
        remainingCarbs: Double,
        remainingFat: Double,
        isBengali: Boolean
    ): String {
        return if (isBengali) {
            """
                🥦 **১. ডিম-সবজির সুস্বাদু ভুর্জি (Scrambled Egg with Local Mixed Greens)**
                * **কেন এটি উপযোগী:** আপনার অবশিষ্টাংশ পুষ্টিকর লক্ষ্যমাত্রার সাথে খাপ খায়। কম শর্করা এবং উচ্চ প্রোটিন যুক্ত।
                * **সময়:** ১২ মিনিট | **পুষ্টিমান:** ক্যালোরি: ~১৮০ kcal | শর্করা: ২g | আমিষ: ১৪g | ফ্যাট: ১২g
                * **উপকরণ:** ২টি দেশী ডিম, কুচানো পালং শাক বা বাঁধাকপি, পেঁয়াজ, কাঁচামরিচ, সামান্য হলুদ ও ১ চামচ সরিষার তেল।
                * **প্রণালী:** প্যানে সামান্য সরিষার তেল দিয়ে পেঁয়াজ ও মরিচ হালকা ভাজুন। সবজি দিয়ে সেদ্ধ হওয়া পর্যন্ত নাড়ুন। এবার ডিম ভেঙে দিয়ে একসাথে ফেটিয়ে ঝুরি বা ভুর্জি করুন। 
                
                🥣 **২. ওটস-সবজি স্যুপ (High-Fiber Oats & Vegetable Soup)**
                * **কেন এটি উপযোগী:** এটি কম ক্যালোরি ও চর্বিমুক্ত, যা আপনার লক্ষ্য পূরণে আদর্শ।
                * **সময়:** ১৫ মিনিট | **পুষ্টিমান:** ক্যালোরি: ~১৫০ kcal | শর্করা: ২২g | আমিষ: ৫g | ফ্যাট: ৩g
                * **উপকরণ:** ওটস ৪ চামচ, গাজর কুচি, পেঁপে বা টমেটো কুচি, গোলমরিচের গুঁড়ো ও লবণ।
                * **প্রণালী:** সবজি প্যানে সামান্য পানি দিয়ে সেদ্ধ করুন। এরপর ওটস যোগ করুন এবং আরও ৫ মিনিট জ্বাল দিন। লবণ ও গোলমরিচ ছড়িয়ে কুসুম গরম পরিবেশন করুন। 
                
                🍗 **৩. প্যান-গ্রিলড চিকেন ব্রেস্ট (Lean Mint & Mustard Chicken)**
                * **কেন এটি উপযোগী:** উচ্চমানের প্রোটিনের উৎস যা পেশী গঠনে এবং ক্ষুধা কমাতে অত্যন্ত কার্যকর।
                * **সময়:** ১৫ মিনিট | **পুষ্টিমান:** ক্যালোরি: ~২১০ kcal | শর্করা: ১g | আমিষ: ২৮g | ফ্যাট: ৫g
                * **উপকরণ:** চামড়াবিহীন মুরগির বুকের মাংস ১০০ গ্রাম, লেবুর রস, পুদিনা পাতা কুচি, সরিষার পেস্ট ও আদা বাটা।
                * **প্রণালী:** মাংসে সব মশলা ভালোভাবে মেখে রাখুন। হালকা তেল ব্রাশ করা প্যানে এপিঠ-ওপিঠ ১০-১২ মিনিট মাঝারি আঁচে গ্রিল বা ভাজুন। সালাদের সাথে দারুণ পুষ্টিকর!
            """.trimIndent()
        } else {
            """
                🥦 **1. Low-Carb Veggie & Egg Scramble**
                * **Why it fits:** Crafted to meet low carb constraints with high bio-available protein.
                * **Time:** 12 mins | **Nutrients:** Calories: ~180 kcal | Carbs: 2g | Protein: 14g | Fat: 12g
                * **Ingredients:** 2 local eggs, baby spinach or cabbage, onions, green chilies, mustard oil.
                * **Steps:** Saute onions & chilies in 1 tsp mustard oil. Add greens until cooked. Whisk in eggs and scramble gently.
                
                🥣 **2. Wholesome Oats & Garden Vegetable Soup**
                * **Why it fits:** Low-fat, mineral-rich option to easily fit your remaining target.
                * **Time:** 15 mins | **Nutrients:** Calories: ~150 kcal | Carbs: 22g | Protein: 5g | Fat: 3g
                * **Ingredients:** 4 tbsp whole oats, diced carrots, papaya, chopped tomatoes, black pepper.
                * **Steps:** Boil vegetables in salted water. Stir in oats, simmer for 5 mins, sprinkle freshly cracked pepper, serve warm.
                
                🍗 **3. Mustard Herb Pan-Grilled Chicken**
                * **Why it fits:** Lean high-protein and near-zero carbs option for muscle preservation.
                * **Time:** 15 mins | **Nutrients:** Calories: ~210 kcal | Carbs: 1g | Protein: 28g | Fat: 5g
                * **Ingredients:** 100g skinless chicken breast, lemon juice, mint leaves, half tsp mustard oil.
                * **Steps:** Marinate chicken with mustard paste and lemon. Pan-fry in a lightly oiled non-stick skillet for 6 minutes on each side.
            """.trimIndent()
        }
    }

    private fun getTodayDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    class Factory(private val repository: DietPlannerRepository, private val context: Context) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(DietPlannerViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return DietPlannerViewModel(repository, context) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
