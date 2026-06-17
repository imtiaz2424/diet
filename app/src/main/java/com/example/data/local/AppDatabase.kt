package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.*

import androidx.room.TypeConverters

@Database(
    entities = [
        UserProfileEntity::class,
        MealPlanEntity::class,
        WeightLogEntity::class,
        WaterLogEntity::class,
        MealReminderEntity::class,
        FoodLogEntity::class,
        ExerciseLogEntity::class,
        ShoppingItemEntity::class,
        RecipeEntity::class,
        MoodLogEntity::class
    ],
    version = 6,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dietPlannerDao(): DietPlannerDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "diet_planner_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
