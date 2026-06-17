package com.example

import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.data.local.AppDatabase
import com.example.data.repository.DietPlannerRepository
import com.example.ui.DietPlannerDashboard
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.DietPlannerViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Request post notifications permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
        }

        val database = AppDatabase.getDatabase(applicationContext)
        val repository = DietPlannerRepository(database.dietPlannerDao())
        
        val viewModel: DietPlannerViewModel by viewModels {
            DietPlannerViewModel.Factory(repository, applicationContext)
        }

        // Load the saved dark mode preference
        val sharedPrefs = getSharedPreferences("suvecha_settings", Context.MODE_PRIVATE)
        val isDark = sharedPrefs.getBoolean("dark_mode", false)
        viewModel.setInitialDarkTheme(isDark)

        val targetWeight = sharedPrefs.getFloat("target_weight", 0.0f).toDouble()
        viewModel.setInitialTargetWeight(targetWeight)

        enableEdgeToEdge()
        setContent {
            val isDarkTheme by viewModel.isDarkTheme.collectAsState()
            MyApplicationTheme(darkTheme = isDarkTheme) {
                DietPlannerDashboard(viewModel = viewModel)
            }
        }
    }
}
