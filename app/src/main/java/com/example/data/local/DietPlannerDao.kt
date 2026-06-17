package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DietPlannerDao {
    // User Profile
    @Query("SELECT * FROM user_profile WHERE id = :userId LIMIT 1")
    fun getUserProfileFlow(userId: String): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE id = :userId LIMIT 1")
    suspend fun getUserProfile(userId: String): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(profile: UserProfileEntity)

    // Meal Plans
    @Query("SELECT * FROM meal_plans WHERE date = :date LIMIT 1")
    fun getMealPlanFlow(date: String): Flow<MealPlanEntity?>

    @Query("SELECT * FROM meal_plans WHERE date = :date LIMIT 1")
    suspend fun getMealPlan(date: String): MealPlanEntity?

    @Query("SELECT * FROM meal_plans ORDER BY date ASC")
    fun getAllMealPlansFlow(): Flow<List<MealPlanEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealPlan(mealPlan: MealPlanEntity)

    // Weight Logs
    @Query("SELECT * FROM weight_logs ORDER BY date ASC")
    fun getAllWeightLogsFlow(): Flow<List<WeightLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeightLog(weightLog: WeightLogEntity)

    @Query("DELETE FROM weight_logs WHERE date = :date")
    suspend fun deleteWeightLog(date: String)

    // Water Logs
    @Query("SELECT * FROM water_logs WHERE date = :date LIMIT 1")
    fun getWaterLogFlow(date: String): Flow<WaterLogEntity?>

    @Query("SELECT * FROM water_logs WHERE date = :date LIMIT 1")
    suspend fun getWaterLog(date: String): WaterLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaterLog(waterLog: WaterLogEntity)

    // Meal Reminders
    @Query("SELECT * FROM meal_reminders ORDER BY id ASC")
    fun getAllRemindersFlow(): Flow<List<MealReminderEntity>>

    @Query("SELECT * FROM meal_reminders ORDER BY id ASC")
    suspend fun getAllReminders(): List<MealReminderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: MealReminderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminders(reminders: List<MealReminderEntity>)

    @Query("DELETE FROM meal_reminders WHERE id = :id")
    suspend fun deleteReminder(id: Int)

    // Food logs
    @Query("SELECT * FROM food_logs WHERE date = :date AND userId = :userId ORDER BY timestamp DESC")
    fun getFoodLogsFlow(date: String, userId: String): Flow<List<FoodLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoodLog(foodLog: FoodLogEntity)

    @Query("DELETE FROM food_logs WHERE id = :id")
    suspend fun deleteFoodLog(id: Int)

    // Exercise logs
    @Query("SELECT * FROM exercise_logs WHERE date = :date ORDER BY timestamp DESC")
    fun getExerciseLogsFlow(date: String): Flow<List<ExerciseLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExerciseLog(exerciseLog: ExerciseLogEntity)

    @Query("DELETE FROM exercise_logs WHERE id = :id")
    suspend fun deleteExerciseLog(id: Int)

    // Shopping List Queries
    @Query("SELECT * FROM shopping_items ORDER BY id ASC")
    fun getAllShoppingItemsFlow(): Flow<List<ShoppingItemEntity>>

    @Query("SELECT * FROM shopping_items WHERE date = :date ORDER BY id ASC")
    fun getShoppingItemsFlow(date: String): Flow<List<ShoppingItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShoppingItems(items: List<ShoppingItemEntity>)

    @Query("UPDATE shopping_items SET isChecked = :isChecked WHERE id = :id")
    suspend fun updateShoppingItemChecked(id: Int, isChecked: Boolean)

    @Query("DELETE FROM shopping_items WHERE date = :date")
    suspend fun deleteShoppingItemsForDate(date: String)

    @Query("DELETE FROM shopping_items")
    suspend fun clearAllShoppingItems()

    // Healthy Recipes
    @Query("SELECT * FROM healthy_recipes ORDER BY title ASC")
    fun getAllRecipesFlow(): Flow<List<RecipeEntity>>

    @Query("SELECT COUNT(*) FROM healthy_recipes")
    suspend fun getRecipesCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipes(recipes: List<RecipeEntity>)

    @Query("UPDATE healthy_recipes SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateRecipeFavorite(id: Int, isFavorite: Boolean)

    // Mood logs
    @Query("SELECT * FROM mood_logs WHERE userId = :userId ORDER BY date DESC, timestamp DESC")
    fun getMoodLogsFlow(userId: String): Flow<List<MoodLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMoodLog(moodLog: MoodLogEntity)

    @Query("DELETE FROM mood_logs WHERE id = :id")
    suspend fun deleteMoodLog(id: Int)
}
