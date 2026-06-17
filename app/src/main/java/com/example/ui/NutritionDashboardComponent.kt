package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FoodLogEntity
import com.example.data.model.UserProfileEntity
import com.example.viewmodel.DietPlannerViewModel

@Composable
fun NutritionDashboardComponent(
    viewModel: DietPlannerViewModel,
    userProfile: UserProfileEntity,
    isBengali: Boolean
) {
    val currentFoodLogs by viewModel.currentFoodLogs.collectAsState()
    val focusManager = LocalFocusManager.current

    // Local form states for manual addition
    var isExpandedFoodForm by rememberSaveable { mutableStateOf(false) }
    var foodName by rememberSaveable { mutableStateOf("") }
    var foodCalories by rememberSaveable { mutableStateOf("") }
    var foodProtein by rememberSaveable { mutableStateOf("") }
    var foodCarbs by rememberSaveable { mutableStateOf("") }
    var foodFat by rememberSaveable { mutableStateOf("") }

    // Dynamic Macronutrient target calculation based on Goal
    val calorieTarget = userProfile.dailyCalorieTarget
    val proteinPct: Double
    val carbsPct: Double
    val fatPct: Double

    when (userProfile.goal.lowercase()) {
        "weight loss", "ওজন কমানো" -> {
            proteinPct = 0.35 // 35% Protein
            carbsPct = 0.35   // 35% Carbohydrates
            fatPct = 0.30     // 30% Fat
        }
        "weight gain", "ওজন বাড়ানো" -> {
            proteinPct = 0.25 // 25% Protein
            carbsPct = 0.50   // 50% Carbohydrates
            fatPct = 0.25     // 25% Fat
        }
        else -> { // Maintaining weight
            proteinPct = 0.25 // 25% Protein
            carbsPct = 0.45   // 45% Carbohydrates
            fatPct = 0.30     // 30% Fat
        }
    }

    // Protein & Carbs: 4 calories/g, Fat: 9 calories/g
    val targetProteinGrams = remember(calorieTarget, proteinPct) {
        ((calorieTarget * proteinPct) / 4.0).toInt().coerceAtLeast(1)
    }
    val targetCarbsGrams = remember(calorieTarget, carbsPct) {
        ((calorieTarget * carbsPct) / 4.0).toInt().coerceAtLeast(1)
    }
    val targetFatGrams = remember(calorieTarget, fatPct) {
        ((calorieTarget * fatPct) / 9.0).toInt().coerceAtLeast(1)
    }

    // Actual Consumed metrics
    val consumedCalories = remember(currentFoodLogs) {
        currentFoodLogs.sumOf { it.calories }
    }
    val consumedProtein = remember(currentFoodLogs) {
        currentFoodLogs.sumOf { it.protein }
    }
    val consumedCarbs = remember(currentFoodLogs) {
        currentFoodLogs.sumOf { it.carbs }
    }
    val consumedFat = remember(currentFoodLogs) {
        currentFoodLogs.sumOf { it.fat }
    }

    // Remaining calculations
    val remainingCalories = (calorieTarget - consumedCalories).coerceAtLeast(0)
    val caloriePercentage = if (calorieTarget > 0) consumedCalories.toFloat() / calorieTarget else 0f

    val proteinPctValue = (consumedProtein.toFloat() / targetProteinGrams).coerceIn(0f, 2f)
    val carbsPctValue = (consumedCarbs.toFloat() / targetCarbsGrams).coerceIn(0f, 2f)
    val fatPctValue = (consumedFat.toFloat() / targetFatGrams).coerceIn(0f, 2f)

    // Preset foods for quick log entries
    val presets = listOf(
        PresetFoodItem(
            nameEn = "Boiled Egg (Sheddho Dim)",
            nameBn = "ডিম সেদ্ধ (১টি)",
            calories = 78, protein = 6.3, carbs = 0.6, fat = 5.3
        ),
        PresetFoodItem(
            nameEn = "Lal Alor Bhaat (Red Rice)",
            nameBn = "লাল চালের ভাত (১ কাপ)",
            calories = 216, protein = 5.0, carbs = 45.0, fat = 1.6
        ),
        PresetFoodItem(
            nameEn = "Ata Ruti (Wheat Tortilla)",
            nameBn = "আটা রুটি (১টি)",
            calories = 104, protein = 3.5, carbs = 22.0, fat = 0.5
        ),
        PresetFoodItem(
            nameEn = "Chicken Curry (Murgir Jhol)",
            nameBn = "মুরগির ঝোল (১ বাটি)",
            calories = 195, protein = 18.0, carbs = 4.0, fat = 11.0
        ),
        PresetFoodItem(
            nameEn = "Moshur Dal (Lentils)",
            nameBn = "মসুর ডাল (১ কাপ)",
            calories = 115, protein = 9.0, carbs = 20.0, fat = 0.8
        ),
        PresetFoodItem(
            nameEn = "Banana (Shobri Kola)",
            nameBn = "সবরি কলা (১টি)",
            calories = 105, protein = 1.3, carbs = 27.0, fat = 0.3
        )
    )

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFECEFF1)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("nutrition_dashboard_card")
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFFE8F5E9), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Restaurant,
                            contentDescription = "Nutrition Tracker",
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = if (isBengali) "পুষ্টি ও ক্যালোরি ড্যাশবোর্ড" else "Daily Nutrition Tracking",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF1B5E20)
                    )
                }

                Box(
                    modifier = Modifier
                        .background(
                            brush = Brush.horizontalGradient(listOf(Color(0xFF81C784), Color(0xFF4CAF50))),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isBengali) userProfile.goal else userProfile.goal,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Progress Summary visualizer (Ring + Macros detail)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular Ring on Left side
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // Background track
                        drawCircle(
                            color = Color(0xFFF1F8E9),
                            style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                        )
                        // Progress Arc
                        drawArc(
                            color = Color(0xFF4CAF50),
                            startAngle = -90f,
                            sweepAngle = (caloriePercentage * 360f).coerceIn(0f, 360f),
                            useCenter = false,
                            style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "$consumedCalories",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF1B5E20)
                        )
                        HorizontalDivider(
                            modifier = Modifier.width(40.dp),
                            color = Color(0xFFC8E6C9),
                            thickness = 1.dp
                        )
                        Text(
                            text = "/ $calorieTarget",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "kcal",
                            fontSize = 9.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }

                // Callouts on the Right side
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Calorie remaining visual breakdown
                    Column {
                        Text(
                            text = if (isBengali) "বাকি রয়েছে" else "Remaining Cal",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = if (isBengali) "$remainingCalories কিলো ক্যালরি" else "$remainingCalories kcal",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (remainingCalories > 0) Color(0xFF1B5E20) else Color(0xFFD32F2F)
                        )
                    }

                    // Simple quick stats status label
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = if (caloriePercentage >= 0.95f) {
                                    if (isBengali) "ক্যালোরি লক্ষ্যমাত্রা পূর্ণ!" else "Target complete!"
                                } else {
                                    val percentInt = (caloriePercentage * 100).toInt()
                                    if (isBengali) "লক্ষ্যমাত্রার $percentInt% অর্জিত!" else "$percentInt% of target met!"
                                },
                                fontSize = 10.sp,
                                modifier = Modifier.weight(1f),
                                fontWeight = FontWeight.Medium,
                                color = Color.DarkGray
                            )
                        }
                    }
                }
            }

            // MACRO-NUTRIENTS BREAKDOWN BARS
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Carbs
                MacroBar(
                    label = if (isBengali) "শর্করা (Carbs)" else "Carbohydrates",
                    consumed = consumedCarbs,
                    target = targetCarbsGrams,
                    progress = carbsPctValue,
                    color = Color(0xFF388E3C), // Strong Green
                    isBengali = isBengali
                )

                // Protein
                MacroBar(
                    label = if (isBengali) "আমিষ (Protein)" else "Protein",
                    consumed = consumedProtein,
                    target = targetProteinGrams,
                    progress = proteinPctValue,
                    color = Color(0xFFF57C00), // Vibrant Orange
                    isBengali = isBengali
                )

                // Fat
                MacroBar(
                    label = if (isBengali) "স্নেহ (Fat)" else "Fat",
                    consumed = consumedFat,
                    target = targetFatGrams,
                    progress = fatPctValue,
                    color = Color(0xFFFBC02D), // Rich Yellow
                    isBengali = isBengali
                )
            }

            // Logged item list (Collapsible display inside the card)
            if (currentFoodLogs.isNotEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (isBengali) "আজকের খাওয়া খাবারের তালিকা (${currentFoodLogs.size})" else "Daily Logged Foods (${currentFoodLogs.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF9FBF9), RoundedCornerShape(12.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        currentFoodLogs.forEach { log ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = log.name,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                        color = Color(0xFF263238),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (isBengali)
                                            "আমিষ: ${log.protein}g  |  শর্করা: ${log.carbs}g  |  চর্বি: ${log.fat}g"
                                        else
                                            "P: ${log.protein}g  |  C: ${log.carbs}g  |  F: ${log.fat}g",
                                        fontSize = 9.sp,
                                        color = Color.Gray
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "${log.calories} kcal",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = Color(0xFF2E7D32)
                                    )

                                    // Touch target guaranteed to be >48dp
                                    IconButton(
                                        onClick = { viewModel.deleteFoodLog(log.id) },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .testTag("delete_food_log_${log.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete entry",
                                            tint = Color(0xFFE53935),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Quick Bangladeshi preset items buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = if (isBengali) "১-ক্লিকে বাংলা স্বাস্থ্যকর খাবার যুক্ত করুন" else "1-Click Healthy Bangla Presets",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp)
                )

                // Scrollable row of visual chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.forEach { preset ->
                        AssistChip(
                            onClick = {
                                viewModel.addFoodLog(
                                    name = if (isBengali) preset.nameBn else preset.nameEn,
                                    calories = preset.calories,
                                    protein = preset.protein,
                                    carbs = preset.carbs,
                                    fat = preset.fat
                                )
                            },
                            label = {
                                Text(
                                    text = if (isBengali) preset.nameBn else preset.nameEn,
                                    fontSize = 11.sp
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = Color(0xFFF1F8E9),
                                labelColor = Color(0xFF2E7D32)
                            ),
                            border = BorderStroke(1.dp, Color(0xFFC8E6C9)),
                            modifier = Modifier.testTag("preset_chip_${preset.calories}")
                        )
                    }
                }
            }

            // Expandable manual logging Form
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = { isExpandedFoodForm = !isExpandedFoodForm },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF1B5E20)),
                    border = BorderStroke(1.dp, Color(0xFFC8E6C9)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("toggle_food_form_btn")
                ) {
                    Icon(
                        imageVector = if (isExpandedFoodForm) Icons.Default.Clear else Icons.Default.Add,
                        contentDescription = "Expand form",
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFF2E7D32)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isExpandedFoodForm) {
                            if (isBengali) "ফর্ম বন্ধ করুন" else "Hide manual entry"
                        } else {
                            if (isBengali) "নিজে কোনো খাবার যুক্ত করুন" else "Manually log any food"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                AnimatedVisibility(
                    visible = isExpandedFoodForm,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = foodName,
                            onValueChange = { foodName = it },
                            label = { Text(if (isBengali) "খাবারের নাম" else "Food Name") },
                            placeholder = { Text(if (isBengali) "যেমন: আপেল, রুটি" else "e.g., Oatmeal, Salad") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("food_name_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = foodCalories,
                                onValueChange = { foodCalories = it },
                                label = { Text(if (isBengali) "ক্যালোরি" else "Calories (kcal)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("food_calories_input")
                            )

                            OutlinedTextField(
                                value = foodProtein,
                                onValueChange = { foodProtein = it },
                                label = { Text(if (isBengali) "আমিষ (গ্রাম)" else "Protein (g)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("food_protein_input")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = foodCarbs,
                                onValueChange = { foodCarbs = it },
                                label = { Text(if (isBengali) "শর্করা (গ্রাম)" else "Carbs (g)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("food_carbs_input")
                            )

                            OutlinedTextField(
                                value = foodFat,
                                onValueChange = { foodFat = it },
                                label = { Text(if (isBengali) "স্নেহ (গ্রাম)" else "Fat (g)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("food_fat_input")
                            )
                        }

                        Button(
                            onClick = {
                                val cal = foodCalories.toIntOrNull() ?: 0
                                val prot = foodProtein.toDoubleOrNull() ?: 0.0
                                val carb = foodCarbs.toDoubleOrNull() ?: 0.0
                                val fatVal = foodFat.toDoubleOrNull() ?: 0.0

                                if (foodName.isNotBlank() && cal >= 0) {
                                    viewModel.addFoodLog(
                                        name = foodName.trim(),
                                        calories = cal,
                                        protein = prot,
                                        carbs = carb,
                                        fat = fatVal
                                    )
                                    // Clear text inputs
                                    foodName = ""
                                    foodCalories = ""
                                    foodProtein = ""
                                    foodCarbs = ""
                                    foodFat = ""
                                    focusManager.clearFocus()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("save_manual_food_btn")
                        ) {
                            Text(
                                text = if (isBengali) "সেভ করুন" else "Add Food Log Entry",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MacroBar(
    label: String,
    consumed: Double,
    target: Int,
    progress: Float,
    color: Color,
    isBengali: Boolean
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color.DarkGray
            )

            Text(
                text = if (isBengali)
                    "${String.format("%.1f", consumed)} গ্রাম / ${target} গ্রাম"
                else
                    "${String.format("%.1f", consumed)}g / ${target}g",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFFF5F5F5))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = progress.coerceIn(0f, 1f))
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
        }
    }
}

data class PresetFoodItem(
    val nameEn: String,
    val nameBn: String,
    val calories: Int,
    val protein: Double,
    val carbs: Double,
    val fat: Double
)
