package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MealPlanEntity
import com.example.viewmodel.DietPlannerViewModel
import java.util.Locale

data class IngredientItem(
    val name: String,
    val quantity: String,
    val category: String,
    val sourceDates: List<String>
) {
    val uniqueKey: String
        get() = "${name.lowercase(Locale.ROOT).trim()}_${category}"
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ShoppingListScreen(
    viewModel: DietPlannerViewModel,
    onBack: () -> Unit
) {
    val isBengali by viewModel.isBengali.collectAsState()
    val allMealPlans by viewModel.allMealPlans.collectAsState()

    // Screen-level toggling between 'All saved plans' or 'Current week plans'
    var filterByCurrentWeekOnly by rememberSaveable { mutableStateOf(true) }

    // Parse ingredients from meal plans
    val ingredientsList = remember(allMealPlans, filterByCurrentWeekOnly) {
        parseAndConsolidateIngredients(allMealPlans, filterByCurrentWeekOnly)
    }

    // Save checked item uniqueKeys locally using rememberSaveable
    var checkedItemKeys by rememberSaveable {
        mutableStateOf(emptySet<String>())
    }

    // Group ingredients by categorized group
    val groupedIngredients = remember(ingredientsList) {
        ingredientsList.groupBy { it.category }
    }

    val categoriesOrder = listOf(
        if (isBengali) "🍎 শাকসবজি ও ফলমূল (Vegetables & Fruits)" else "🍎 Vegetables & Fruits",
        if (isBengali) "🍗 মাছ, মাংস ও প্রোটিন (Proteins & Fish)" else "🍗 Proteins, Fish & Meat",
        if (isBengali) "🥛 দুধ ও কুসুম খাবার (Dairy & Alternatives)" else "🥛 Dairy & Alternatives",
        if (isBengali) "🌾 শস্য, শর্করা ও বাদাম (Grains & Nuts)" else "🌾 Grains, Carbs & Nuts",
        if (isBengali) "🍯 মসলা, তেল ও অন্যান্য (Spices & Extras)" else "🍯 Oils, Spices & Extras"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isBengali) "সাপ্তাহিক বাজারের ফর্দ" else "Weekly Shopping List",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF1E5E2F)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF1E5E2F)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color(0xFF1E5E2F)
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF9FBF9))
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Heading card with details
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color.White, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isBengali) "সুপার স্লিম বাজারের তালিকা" else "Smart Grocery Planner",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF1B5E20)
                        )
                        Text(
                            text = if (isBengali)
                                "আপনার জেনারেট করা ডায়েট প্ল্যান থেকে রিয়েল-টাইমে সমস্ত উপাদান একত্রিত করে তৈরি করা হয়েছে। সলিড ক্যাটাগরির উপর ভিত্তি করে সুসংগঠিত।"
                            else
                                "Aggregated dynamically across all your active meal plans. Organized neatly into dietetic grocery categories.",
                            fontSize = 11.sp,
                            color = Color(0xFF2E7D32),
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            // Controls for filtering
            if (allMealPlans.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFECEFF1)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isBengali) "সাপ্তাহিক ফিল্টার সক্রিয়" else "Current Week Only",
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            color = Color(0xFF37474F)
                        )

                        Switch(
                            checked = filterByCurrentWeekOnly,
                            onCheckedChange = { filterByCurrentWeekOnly = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF2E7D32),
                                uncheckedThumbColor = Color(0xFF90A4AE),
                                uncheckedTrackColor = Color(0xFFECEFF1)
                            )
                        )
                    }
                }
            }

            // List Content
            if (ingredientsList.isEmpty()) {
                // Empty state card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFECEFF1)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("🛒", fontSize = 48.sp)
                        Text(
                            text = if (isBengali) "কোনো উপাদান পাওয়া যায়নি" else "No ingredients found",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF37474F)
                        )
                        Text(
                            text = if (isBengali)
                                "দয়া করে প্রথমে ড্যাশবোর্ড থেকে সুষম ডায়েট প্ল্যান জেনারেট করুন।"
                            else
                                "Generate a customized diet plan on the home dashboard to automatically build your smart grocery list.",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )
                    }
                }
            } else {
                // Clean and group-by list
                categoriesOrder.forEach { categoryName ->
                    // Find actual matching keys that correspond to this category header
                    val matchingGroupItems = groupedIngredients.entries.find { entry ->
                        categoryName.contains(entry.key, ignoreCase = true)
                    }?.value ?: emptyList()

                    if (matchingGroupItems.isNotEmpty()) {
                        Text(
                            text = categoryName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF2E7D32),
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFECEFF1)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                matchingGroupItems.forEachIndexed { index, item ->
                                    val isChecked = checkedItemKeys.contains(item.uniqueKey)

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                checkedItemKeys = if (isChecked) {
                                                    checkedItemKeys - item.uniqueKey
                                                } else {
                                                    checkedItemKeys + item.uniqueKey
                                                }
                                            }
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isChecked) Color(0xFFE8F5E9) else Color(
                                                        0xFFECEFF1
                                                    )
                                                )
                                                .border(
                                                    width = 1.dp,
                                                    color = if (isChecked) Color(0xFF2E7D32) else Color(
                                                        0xFFB0BEC5
                                                    ),
                                                    shape = CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isChecked) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Checked",
                                                    tint = Color(0xFF2E7D32),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.name,
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 13.sp,
                                                color = if (isChecked) Color.Gray else Color(
                                                    0xFF263238
                                                ),
                                                textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None
                                            )
                                            if (item.sourceDates.isNotEmpty()) {
                                                Text(
                                                    text = if (isBengali)
                                                        "তারিখ: ${item.sourceDates.joinToString(", ")}"
                                                    else
                                                        "Dates: ${item.sourceDates.joinToString(", ")}",
                                                    fontSize = 10.sp,
                                                    color = Color.LightGray
                                                )
                                            }
                                        }

                                        Text(
                                            text = item.quantity,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (isChecked) Color.LightGray else Color(
                                                0xFF37474F
                                            ),
                                            textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None
                                        )
                                    }

                                    if (index < matchingGroupItems.size - 1) {
                                        HorizontalDivider(
                                            color = Color(0xFFECEFF1),
                                            modifier = Modifier.padding(horizontal = 16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Global checklist actions card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDE7)),
                    border = BorderStroke(1.dp, Color(0xFFFFF59D)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFFF57F17),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (isBengali)
                                "চেক করা উপাদানগুলোর তালিকা আপনার এই স্ক্রিন খোলার সেশনে সুরক্ষিত থাকবে।"
                            else
                                "Checked products will remain tracked locally during your shopping session.",
                            fontSize = 11.sp,
                            color = Color(0xFF5D4037),
                            lineHeight = 15.sp
                        )
                    }
                }

                if (checkedItemKeys.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { checkedItemKeys = emptySet() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFE53935))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = null,
                            tint = Color(0xFFE53935),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isBengali) "সব সিলেক্ট বাতিল করুন" else "Reset Checklist",
                            color = Color(0xFFE53935),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Parser utility to loop through all plans and aggregate ingredients dynamically
 */
fun parseAndConsolidateIngredients(
    mealPlans: List<MealPlanEntity>,
    currentWeekOnly: Boolean
): List<IngredientItem> {
    val itemsMap = mutableMapOf<String, Pair<String, MutableList<String>>>() // nameKey -> Pair(Qty, DaysList)

    // Optional: filter only meal plans of the last 7 days or matching week if requested
    val filteredPlans = if (currentWeekOnly) {
        // Just take the up to 7 latest generated plans if there are more
        mealPlans.takeLast(7)
    } else {
        mealPlans
    }

    filteredPlans.forEach { mealPlan ->
        val mealsText = listOf(
            mealPlan.breakfast,
            mealPlan.snack1,
            mealPlan.lunch,
            mealPlan.snack2,
            mealPlan.dinner
        )

        mealsText.forEach { meal ->
            val parts = meal.split(Regex("[+\\n,|]"))
            parts.forEach { part ->
                val trimmed = part.trim()
                if (trimmed.length > 2 &&
                    !trimmed.contains("Water", ignoreCase = true) &&
                    !trimmed.contains("पानी", ignoreCase = true)
                ) {
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

                    if (name.isNotBlank()) {
                        val key = name.lowercase(Locale.ROOT).trim()
                        val existing = itemsMap[key]
                        // Simple nicely readable date formatting snippet "06-14"
                        val dateFormatted = try {
                            if (mealPlan.date.length >= 10) mealPlan.date.substring(5) else mealPlan.date
                        } catch (e: Exception) {
                            mealPlan.date
                        }

                        if (existing != null) {
                            val daysList = existing.second
                            if (!daysList.contains(dateFormatted)) {
                                daysList.add(dateFormatted)
                            }
                            // Keep the most prominent quantity string
                            val updatedQty = if (existing.first == "As needed" && quantity != "As needed") {
                                quantity
                            } else {
                                existing.first
                            }
                            itemsMap[key] = Pair(updatedQty, daysList)
                        } else {
                            itemsMap[key] = Pair(quantity, mutableListOf(dateFormatted))
                        }
                    }
                }
            }
        }
    }

    // Convert keys back into categorized items list
    return itemsMap.map { (nameKey, details) ->
        val originalName = nameKey.split(' ').joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
        val category = determineCategory(nameKey)
        IngredientItem(
            name = originalName,
            quantity = details.first,
            category = category,
            sourceDates = details.second
        )
    }
}

/**
 * Determine the dietetic category group for food keyword items
 */
fun determineCategory(name: String): String {
    val lower = name.lowercase(Locale.ROOT)

    val vegetablesKeywords = listOf(
        "spinach", "apple", "banana", "vegetable", "vaji", "carrots", "broccoli", "garlic", "onion",
        "ginger", "tomato", "lemon", "cucumber", "lettuce", "potato", "cauliflower", "cabbage",
        "gourd", "pumpkin", "papaya", "বেগুন", "আলু", "টমেটো", "পিয়াজ", "রসুন", "আদা", "গাজর",
        "লেবু", "শসা", "শাক", "লাউ", "মিষ্টি কুমড়া", "কপি", "পেঁপে", "কলা", "আপেল", "ফল", "ক্যাপসিকাম"
    )

    val proteinKeywords = listOf(
        "chicken", "fish", "egg", "beef", "mutton", "lean", "lentil", "dal", "pulses", "chickpea",
        "tofu", "paneer", "ডাল", "ডিম", "মুরগি", "লাল মাংস", "মাছ", "পনির", "বুট", "ছোলা", "সয়াবিন"
    )

    val dairyKeywords = listOf(
        "milk", "yogurt", "curd", "cheese", "cream", "ghee", "মাখন", "দুধ", "দই", "ছানা", "ঘি"
    )

    val grainsKeywords = listOf(
        "rice", "ruti", "bread", "oats", "chia", "quinoa", "almond", "walnut", "peanut", "flax",
        "atta", "flour", "semolina", "সুজি", "রুটি", "ভাত", "ওটস", "ওটমিল", "চিয়া সিড", "আটা",
        "ময়দা", "বাদাম", "চিঁড়ে"
    )

    return when {
        vegetablesKeywords.any { lower.contains(it) } -> "Vegetables & Fruits"
        proteinKeywords.any { lower.contains(it) } -> "Proteins & Fish"
        dairyKeywords.any { lower.contains(it) } -> "Dairy & Alternatives"
        grainsKeywords.any { lower.contains(it) } -> "Grains & Nuts"
        else -> "Spices & Extras"
    }
}
