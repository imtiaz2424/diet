package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Ignore
import java.io.Serializable

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: String = "guest", // Matches firebase uid or guest account
    val age: Int,
    val gender: String, // Male, Female, Other
    val weight: Double, // in kg
    val height: Double, // in cm
    val goal: String, // Weight Loss, Weight Gain, Maintain
    val dietaryPreference: String, // Vegetarian, Non-Vegetarian, Vegan
    val allergies: String, // comma-separated or text
    val dailyCalorieTarget: Int,
    val dailyWaterTargetMl: Int = 2500,
    val medical_conditions: List<String> = listOf("None"),
    val cuisine_preferences: List<String> = listOf("Bengali"),
    val lastUpdated: Long = System.currentTimeMillis()
) {
    @Ignore
    constructor(
        id: String = "guest",
        age: Int,
        gender: String,
        weight: Double,
        height: Double,
        goal: String,
        dietaryPreference: String,
        allergies: String,
        dailyCalorieTarget: Int,
        dailyWaterTargetMl: Int = 2500,
        medicalConditions: String = "None",
        cuisinePreferences: String = "Bengali",
        lastUpdated: Long = System.currentTimeMillis()
    ) : this(
        id = id,
        age = age,
        gender = gender,
        weight = weight,
        height = height,
        goal = goal,
        dietaryPreference = dietaryPreference,
        allergies = allergies,
        dailyCalorieTarget = dailyCalorieTarget,
        dailyWaterTargetMl = dailyWaterTargetMl,
        medical_conditions = medicalConditions.split(",").map { it.trim() }.filter { it.isNotEmpty() },
        cuisine_preferences = cuisinePreferences.split(",").map { it.trim() }.filter { it.isNotEmpty() },
        lastUpdated = lastUpdated
    )

    val medicalConditions: String
        get() = medical_conditions.joinToString(", ")

    val cuisinePreferences: String
        get() = cuisine_preferences.joinToString(", ")
}

@Entity(tableName = "shopping_items")
data class ShoppingItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String, // associated date of the meal plan
    val name: String,
    val quantity: String,
    val isChecked: Boolean = false
)

@Entity(tableName = "meal_plans")
data class MealPlanEntity(
    @PrimaryKey val date: String, // YYYY-MM-DD
    val calorieTarget: Int,
    val breakfast: String,
    val breakfastCal: Int,
    val snack1: String,
    val snack1Cal: Int,
    val lunch: String,
    val lunchCal: Int,
    val snack2: String,
    val snack2Cal: Int,
    val dinner: String,
    val dinnerCal: Int,
    val dailyTip: String,
    val rawResponse: String // Full raw response text from Gemini
)

@Entity(tableName = "weight_logs")
data class WeightLogEntity(
    @PrimaryKey val date: String, // YYYY-MM-DD
    val weight: Double,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "water_logs")
data class WaterLogEntity(
    @PrimaryKey val date: String, // YYYY-MM-DD
    val amountMl: Int,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "meal_reminders")
data class MealReminderEntity(
    @PrimaryKey val id: Int, // 1 for Breakfast, 2 for Snack 1, 3 for Lunch, 4 for Snack 2, 5 for Dinner, 6 for Water
    val name: String,
    val hour: Int,
    val minute: Int,
    val isEnabled: Boolean
)

@Entity(tableName = "food_logs")
data class FoodLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String = "guest",
    val date: String, // YYYY-MM-DD
    val name: String,
    val calories: Int,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fat: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "exercise_logs")
data class ExerciseLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String, // YYYY-MM-DD
    val activity: String,
    val durationMin: Int,
    val caloriesBurned: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "healthy_recipes")
data class RecipeEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val titleBn: String,
    val duration: String,
    val durationBn: String,
    val calories: String,
    val caloriesValue: Int,
    val category: String,
    val categoryBn: String,
    val mealType: String, // "Breakfast", "Lunch", "Dinner", "Snack"
    val dietaryRestrictions: String, // Comma-separated (e.g., "Vegetarian, Vegan, Gluten-Free, Diabetic-Friendly")
    val ingredients: List<String>,
    val ingredientsBn: List<String>,
    val steps: List<String>,
    val stepsBn: List<String>,
    val tip: String,
    val tipBn: String,
    val isFavorite: Boolean = false
)

@Entity(tableName = "mood_logs")
data class MoodLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String,
    val date: String, // YYYY-MM-DD
    val mood: String,
    val note: String,
    val food: String,
    val activity: String,
    val timestamp: Long = System.currentTimeMillis()
)


