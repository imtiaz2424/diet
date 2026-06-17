package com.example.ui

import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import com.example.data.api.RetrofitClient
import com.example.data.model.*
import com.example.viewmodel.DietPlannerViewModel
import com.example.viewmodel.FoodSearchResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ToolsTab(
    viewModel: DietPlannerViewModel,
    reminders: List<MealReminderEntity>,
    userProfile: UserProfileEntity,
    selectedDate: String
) {
    val isBengali by viewModel.isBengali.collectAsState()
    val context = LocalContext.current
    
    // Track currently selected sub-tool. Empty string means showing root grid of Tools
    var activeTool by remember { mutableStateOf("") }
    
    // Core home screen sub-state (home or recommendations)
    var homeScreenState by remember { mutableStateOf("home") }
    var userMood by remember { mutableStateOf("Happy") }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = activeTool,
            transitionSpec = {
                fadeIn() togetherWith fadeOut()
            },
            label = "activeToolTransition",
            modifier = Modifier.fillMaxSize()
        ) { toolId ->
            if (toolId.isEmpty()) {
                if (homeScreenState == "home") {
                    ToolsGridMenu(
                        isBengali = isBengali,
                        onSelectTool = { activeTool = it },
                        onSelectMood = { mood ->
                            userMood = mood
                            homeScreenState = "suggestions"
                        }
                    )
                } else {
                    MoodSuggestionsScreen(
                        mood = userMood,
                        isBengali = isBengali,
                        onBack = { homeScreenState = "home" }
                    )
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Shared header for all Sub-tools
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { activeTool = "" }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = getToolTitle(toolId, isBengali),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Render specific Subtool Composable
                    when (toolId) {
                        "search" -> FoodSearchToolScreen(viewModel = viewModel, isBengali = isBengali)
                        "scanner" -> BarcodeScannerToolScreen(viewModel = viewModel, isBengali = isBengali)
                        "recipes" -> RecipeSuggestionsToolScreen(viewModel = viewModel, isBengali = isBengali)
                        "image" -> FoodImageRecognitionToolScreen(viewModel = viewModel, isBengali = isBengali)
                        "exercise" -> ExerciseTrackerToolScreen(viewModel = viewModel, isBengali = isBengali)
                        "deals" -> GroceryDealsToolScreen(isBengali = isBengali)
                        "analytics" -> ProgressAnalyticsToolScreen(viewModel = viewModel, userProfile = userProfile, isBengali = isBengali)
                        "reminders" -> RemindersSubTab(viewModel = viewModel, reminders = reminders, context = context, isBengali = isBengali)
                        "share" -> SocialShareToolScreen(viewModel = viewModel, isBengali = isBengali)
                        "facts" -> FoodFactsToolScreen(isBengali = isBengali)
                        "dietinfo" -> DietGuidelinesToolScreen(isBengali = isBengali)
                        "weight_checker" -> IdealWeightCalculatorScreen(isBengali = isBengali, userProfile = userProfile)
                        "rating" -> AppRatingsToolScreen(isBengali = isBengali)
                        "mood" -> MoodLifestylePlannerScreen(viewModel = viewModel, isBengali = isBengali)
                        "restaurants" -> NearbyRestaurantsScreen(viewModel = viewModel, isBengali = isBengali, userProfile = userProfile)
                        "community" -> CommunityRecipeExchangeScreen(viewModel = viewModel, isBengali = isBengali)
                        "emergency" -> LocalEmergencyHelperScreen(viewModel = viewModel, isBengali = isBengali)
                    }
                }
            }
        }

        // Floating SOS button: Visible on all sub-screens cleanly
        if (activeTool != "emergency") {
            FloatingActionButton(
                onClick = {
                    activeTool = "emergency"
                },
                containerColor = Color(0xFFEF5350),
                contentColor = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .testTag("floating_sos_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("🚨", fontSize = 18.sp)
                    Text(
                        text = if (isBengali) "জরুরী SOS" else "SOS HELPER",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// ========================================================
// TOOL SELECTION GRID MENU
// ========================================================
@Composable
fun ToolsGridMenu(
    isBengali: Boolean,
    onSelectTool: (String) -> Unit,
    onSelectMood: (String) -> Unit
) {
    var showVoiceDialog by remember { mutableStateOf(false) }
    var selectedMoodOption by remember { mutableStateOf("") }
    var expandAllTools by remember { mutableStateOf(false) }

    val toolsList = remember(isBengali) {
        listOf(
            ToolItemData("search", if(isBengali) "খাদ্য অনুসন্ধান" else "Food Search", "🔍", if(isBengali) "Open Food Facts এ পুষ্টি খুঁজুন" else "Search Open Food Facts API", Color(0xFFE3F2FD)),
            ToolItemData("scanner", if(isBengali) "বারকোড স্ক্যানার" else "Barcode Scanner", "🏷️", if(isBengali) "প্যাকেটজাত খাবার বিশ্লেষণ করুন" else "Lookup packaged nutrition", Color(0xFFF3E5F5)),
            ToolItemData("recipes", if(isBengali) "রেসিপি পরামর্শ" else "Recipe Suggestions", "🍲", if(isBengali) "খাবারের তালিকা ভিত্তিক রান্না" else "Healthy ideas based on diet", Color(0xFFE8F5E9)),
            ToolItemData("image", if(isBengali) "ছবি সনাক্তকরণ" else "Image Recognition", "📸", if(isBengali) "ক্যালোরি এবং অংশ অনুমান" else "Food calorie estimation", Color(0xFFFFF3E0)),
            ToolItemData("exercise", if(isBengali) "ব্যায়াম ট্র্যাকার" else "Workouts Log", "🏃‍♂️", if(isBengali) "ক্যালোরি ক্ষয় হিসেব করুন" else "Burn calories calculation", Color(0xFFE0F2F1)),
            ToolItemData("deals", if(isBengali) "বাংলাদেশ ডিলস" else "Bangladesh Deals", "🛒", if(isBengali) "স্বপ্ন, চালডালের আজকের কম কেলোরি অফার" else "Grocery discounts online", Color(0xFFFCE4EC)),
            ToolItemData("analytics", if(isBengali) "অগ্রগতি গ্রাফ" else "Analytics Charts", "📊", if(isBengali) "BMI ও ওজনের নিখুঁত চার্ট" else "Daily progress stats & weights", Color(0xFFFFFDE7)),
            ToolItemData("reminders", if(isBengali) "অ্যালার্ম সেটিংস" else "Reminders Setup", "⏰", if(isBengali) "पानी ও খাবারের পুশ নোটিফিকেশন" else "Manage hydration reminders", Color(0xFFE0F7FA)),
            ToolItemData("share", if(isBengali) "বন্ধুদের সাথে শেয়ার" else "Social Sharing", "📤", if(isBengali) "আজকের অর্জন ও খাদ্যতালিকা পাঠান" else "Share daily accomplishments", Color(0xFFECEFF1)),
            ToolItemData("facts", if(isBengali) "১০০ স্বাস্থ্য তথ্য" else "100 Food Facts", "💡", if(isBengali) "১০০ টি বৈজ্ঞানিক খাদ্য ও পুষ্টির তথ্য" else "100 Scientific food & nutrition facts", Color(0xFFE8F5E9)),
            ToolItemData("dietinfo", if(isBengali) "১০০ ডায়েট গাইড" else "100 Diet Manuals", "📚", if(isBengali) "১০০ টি সুষম খাদ্য ও জীবনযাত্রার গাইড" else "100 Professional nutrition guidelines", Color(0xFFFFF3E0)),
            ToolItemData("weight_checker", if(isBengali) "আদর্শ ওজন ক্যালকুলেটর" else "Ideal Weight Tool", "⚖️", if(isBengali) "আপনার সঠিক ওজন ও ক্যালোরি বাজেট খুঁজুন" else "Find ideal weight & diet calories planner", Color(0xFFE3F2FD)),
            ToolItemData("rating", if(isBengali) "অ্যাপ রেটিং ও রিভিউ" else "Ratings & Reviews", "⭐", if(isBengali) "ব্যবহারকারীদের চমৎকার রেটিং ও মন্তব্য দেখুন" else "Read community reviews & rate us", Color(0xFFFCE4EC)),
            ToolItemData("mood", if(isBengali) "আবেগ ও লাইফ গাইড" else "Mood & Emotion Log", "🧠", if(isBengali) "মানসিক আবেগ অনুযায়ী সুষম খাদ্য নির্বাচন" else "Mood input system & lifestyle advice", Color(0xFFFFF9C4)),
            ToolItemData("restaurants", if(isBengali) "নিকটস্থ ফুড সেন্টার" else "Nearby Restaurants", "📍", if(isBengali) "কাছাকাছি সুষম খাবারের দোকান ও ডেলিভারি" else "Google Maps simulation & Order Now", Color(0xFFE8F5E9)),
            ToolItemData("community", if(isBengali) "কমিউনিটি ও রেসিপি" else "Community Feed", "👥", if(isBengali) "খাবারের কম্বো ও চমৎকার রেসিপি শেয়ার" else "Recipe exchange & weekly badges feed", Color(0xFFE1F5FE)),
            ToolItemData("emergency", if(isBengali) "জরুরী ভলান্টিয়ার" else "Emergency Helper", "🚨", if(isBengali) "জরুরী রক্তদান, ডাক্তার ও ভলান্টিয়ার সাহায্য" else "One-click SOS request & local volunteers", Color(0xFFFFEBEE))
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 🏠 HOME SCREEN BANNER: BLUE-PURPLE GRADIENT HERO CARD
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF2196F3), Color(0xFF673AB7), Color(0xFF9C27B0))
                        )
                    )
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "ANEXSOPZ",
                                fontWeight = FontWeight.Black,
                                fontSize = 24.sp,
                                color = Color.White,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = if (isBengali) "আপনার সুষম মানসিক ও স্পোর্টস ডায়েট হাব" else "Your Emotional Diet & Wellness Oasis",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.85f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🌿", fontSize = 20.sp)
                        }
                    }

                    Divider(color = Color.White.copy(alpha = 0.2f))

                    // 😊 MOOD INPUT SECTION: EMOJI + VOICE COMBO
                    Text(
                        text = if (isBengali) "আজ আপনার আবেগ ও অনুভূতি কেমন? (Mood Input)" else "How is your mental battery today?",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Emoji selections list
                        val moods = listOf(
                            Pair("😊", "Happy"),
                            Pair("😔", "Sad"),
                            Pair("🥱", "Tired"),
                            Pair("😰", "Stressed"),
                            Pair("😡", "Angry")
                        )

                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            moods.forEach { (emoji, moodName) ->
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.15f))
                                        .clickable {
                                            selectedMoodOption = moodName
                                            onSelectMood(moodName)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(emoji, fontSize = 22.sp)
                                }
                            }
                        }

                        // Voice Integration combo button
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE91E63))
                                .clickable { showVoiceDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🎙️", fontSize = 20.sp)
                        }
                    }

                    // Diet suggestion minimalist preview
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.1f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("🛡️", fontSize = 14.sp)
                        Text(
                            text = if (isBengali) "প্রক্সিমিটি চেক: রক্তদান, ভলান্টিয়ার বা চিকিৎসক রেডি রয়েছে।" 
                                   else "Safety Check: Quick SOS signal ready to transmit on tap.",
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }

        // QUICK ACCESS FOUR LARGE BUTTON DIAGRAMS
        Text(
            text = if (isBengali) "⚡ কুইক অ্যাক্সেস মেনু (Wellness Commands)" else "⚡ Quick Wellness Commands",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.primary
        )

        val quickGridItems = listOf(
            Triple("mood", if (isBengali) "আবেগ ও লাইফ গাইড" else "Mood Tracker", "🧠"),
            Triple("recipes", if (isBengali) "আজকের হেলদি ডায়েট" else "Diet Suggestions", "🍛"),
            Triple("community", if (isBengali) "কমিউনিটি ও শেয়ার" else "Community Hub", "👥"),
            Triple("emergency", if (isBengali) "জরুরী ভলান্টিয়ার্স" else "Emergency Helper", "🚨")
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                quickGridItems.take(2).forEach { item ->
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier
                            .weight(1f)
                            .height(110.dp)
                            .clickable { onSelectTool(item.first) }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(item.third, fontSize = 28.sp)
                            Text(
                                text = item.second,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                quickGridItems.drop(2).forEach { item ->
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (item.first == "emergency") Color(0xFFFFEBEE) else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(110.dp)
                            .clickable { onSelectTool(item.first) }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(item.third, fontSize = 28.sp)
                            Text(
                                text = item.second,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                color = if (item.first == "emergency") Color(0xFFC62828) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // COLLAPSIBLE DRAWER FOR OTHER SECONDARY TOOLS
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandAllTools = !expandAllTools }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("🌐", fontSize = 18.sp)
                        Text(
                            text = if (isBengali) "অনান্য স্বাস্থ্যকর ডিজিটাল টুলসসমূহ" else "Explore More Wellness Tools",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = if (expandAllTools) "Collapse ▲" else "Expand ▼",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Gray
                    )
                }

                if (expandAllTools) {
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val secondaryTools = toolsList.filter {
                            it.id != "mood" && it.id != "recipes" && it.id != "community" && it.id != "emergency"
                        }
                        secondaryTools.forEach { secondaryTool ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(secondaryTool.bgColor.copy(alpha = 0.4f))
                                    .clickable { onSelectTool(secondaryTool.id) }
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(secondaryTool.emoji, fontSize = 20.sp)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = secondaryTool.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color.Black
                                    )
                                    Text(
                                        text = secondaryTool.subtitle,
                                        fontSize = 9.5.sp,
                                        color = Color.DarkGray
                                    )
                                }
                                Text("➔", fontSize = 14.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }

    // Interactive Voice recognition combo system dialog
    if (showVoiceDialog) {
        AlertDialog(
            onDismissRequest = { showVoiceDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🎙️")
                    Text(if (isBengali) "ভয়েস অনুভূতি সনাক্তকারী" else "AI Voice Mood Input")
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Pulsing Waveform Simulation
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.height(60.dp)
                    ) {
                        val infiniteTransition = rememberInfiniteTransition(label = "voice")
                        val scale1 by infiniteTransition.animateFloat(
                            initialValue = 0.3f, targetValue = 1.6f,
                            animationSpec = infiniteRepeatable(animation = tween(600, easing = LinearEasing), repeatMode = RepeatMode.Reverse), label = "v1"
                        )
                        val scale2 by infiniteTransition.animateFloat(
                            initialValue = 1.4f, targetValue = 0.4f,
                            animationSpec = infiniteRepeatable(animation = tween(500, easing = LinearEasing), repeatMode = RepeatMode.Reverse), label = "v2"
                        )
                        val scale3 by infiniteTransition.animateFloat(
                            initialValue = 0.5f, targetValue = 1.8f,
                            animationSpec = infiniteRepeatable(animation = tween(700, easing = LinearEasing), repeatMode = RepeatMode.Reverse), label = "v3"
                        )
                        
                        Box(modifier = Modifier.width(6.dp).height(30.dp * scale1).clip(CircleShape).background(Color(0xFFE91E63)))
                        Box(modifier = Modifier.width(6.dp).height(40.dp * scale2).clip(CircleShape).background(Color(0xFF673AB7)))
                        Box(modifier = Modifier.width(6.dp).height(20.dp * scale3).clip(CircleShape).background(Color(0xFF2196F3)))
                    }

                    Text(
                        text = if (isBengali) "আপনার আবেগ সনাক্ত করা হচ্ছে..." else "Listening to your body tones...",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )

                    Divider(modifier = Modifier.padding(vertical = 4.dp))

                    Text(
                        text = if (isBengali) "একটি সিমুলেটেড ভয়েস নির্বাচন করুন:" else "Tap a sample phrase to simulate voice:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )

                    // Simulated Options
                    Button(
                        onClick = {
                            showVoiceDialog = false
                            onSelectMood("Tired")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isBengali) "\"খুব ক্লান্ত লাগছে, শরীর সম্পূর্ণ ক্লান্ত\" 😫" else "\"Feeling super exhausted & low battery today\" 😫",
                            fontSize = 11.sp,
                            color = Color.Black
                        )
                    }

                    Button(
                        onClick = {
                            showVoiceDialog = false
                            onSelectMood("Angry")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isBengali) "\"আজকের জ্যামে ভীষণ মাথা গরম হয়ে গেছে\" 😡" else "\"I had a highly frustrating day in traffic\" 😡",
                            fontSize = 11.sp,
                            color = Color.Black
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showVoiceDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun MoodSuggestionsScreen(
    mood: String,
    isBengali: Boolean,
    onBack: () -> Unit
) {
    var isAlternativeSelected by remember { mutableStateOf(false) }

    val normalSuggestions = remember(mood) {
        when (mood) {
            "Sad" -> Triple(
                "Cocoa Dark Chocolate Smoothie (কোকো চকোলেট স্মুদি)",
                "Rich cocoa is packed with magnesium & active antioxidants to instantly increase natural endorphins and elevate core dopamine circuits. Perfect with raw organic honey and chia seeds.",
                "Listen to soft instrumental flute sitar melodies to release muscle tensions."
            )
            "Tired" -> Triple(
                "Fresh Ginger Lemon Green Tea & Almonds (আদা-লেবু চা ও বাদাম)",
                "High-quality green tea with organic fresh ginger stimulates thermo-vascular blood flow, while almonds provide natural essential fats to sustain mitochondrial recovery.",
                "Take a 10-minute deep oxygen walk outdoors under natural daylight."
            )
            "Stressed" -> Triple(
                "Ripe Bananas, Oat Milk & Honey Shake (কলা এবং ওটস মিল্ক শেক)",
                "High potassium content in bananas stabilizes vascular tensions, and complex oats trigger steady serotonin-stimulating glucose delivery to counter internal cortisol surges.",
                "Synthesize natural calm: Complete three iterations of 4-7-8 breathing cycles."
            )
            "Angry" -> Triple(
                "Chilled Cucumber Mint Refresher ( ঠান্ডা শসা ও পুদিনার স্মুদি)",
                "Ice cold hydration fused with cooling mint relaxes vascular smooth muscles and rapidly lowers elevated arterial heat. Extremely refreshing.",
                "Scribble down your active angry thoughts on a blank sheet, and shred it physically."
            )
            else -> Triple(
                "Colorful Citrus Kiwi Fruit Salad (মিক্সড সাইট্রাস সালাদ)",
                "Immense Vitamin C content reinforces standard cellular metabolism, providing continuous focus, strength, and high-performance lifestyle output.",
                "Keep momentum: Channel this amazing positive mood into a productive 15-minute workout!"
            )
        }
    }

    val alternativeSuggestions = remember(mood) {
        when (mood) {
            "Sad" -> Triple(
                "Warm Berry Oats with Walnuts (বেরি ওটস এবং আখরোট)",
                "Packed with melatonin precursors, omega-3 fatty acids from walnuts optimize signal transmissions between neurons inside emotional pathways.",
                "Call a childhood friend, share a happy memory or log achievements inside community exchange."
            )
            "Tired" -> Triple(
                "Peppermint Infused Cold-Drip Hydrator (মিন্ট কোল্ড ডাইজেশন)",
                "Cooling peppermint triggers active non-caffeine thermal alertness, revitalizing standard energy levels without sleep cycle side effects.",
                "Splash cold fresh water onto your eyes or take an refreshing 10-minute power mindfulness break."
            )
            "Stressed" -> Triple(
                "Steamed Asparagus & Garlic Tofu Bowl (রসুন টোফু ও ব্রোকলি বোল)",
                "High in minerals and folic acids, tofu assists central serotonin Synthesis, reducing muscle rigidity without making you feel drowsy.",
                "Lie down in a comfortable posture and listen to the healing qualities of 432Hz deep sound therapy."
            )
            "Angry" -> Triple(
                "Warm Apigenin-rich Camomile Lavender Infusion (ক্যামোমাইল ল্যাভেন্ডার চা)",
                "Apigenin antioxidants bind to target brain receptors to induce rapid serenity of mind and lower stress-induced vascular spasms.",
                "Close your eyes and complete five deep slow inhales, focusing purely on deep diaphragm release."
            )
            else -> Triple(
                "Avocado Butter Salmon Toast (অ্যাভোকাডো স্যামন টোস্ট)",
                "Healthy monounsaturated fats fuel long-term cognitive endurance, memory reserves, and nervous synchronization.",
                "Share a visual tip or positive quote inside ANEXSOPZ community feed to motivate others!"
            )
        }
    }

    val currentSuggestion = if (isAlternativeSelected) alternativeSuggestions else normalSuggestions

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top back bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = if (isBengali) "আপনার আবেগ ও ডায়েট সাজেশন" else "Your Mood suggestions",
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Beautiful Trust Personalization Tag Box
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
            border = BorderStroke(1.dp, Color(0xFF81C784)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("✨", fontSize = 18.sp)
                Text(
                    text = if (isBengali) "Recommended for your current mood: $mood" else "✨ Recommended for your current mood: $mood",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color(0xFF2E7D32)
                )
            }
        }

        // Split Layout: Food Suggestion
        Text(
            text = if (isBengali) "🍽️ আজকের নির্বাচিত সুপার-ফুড রেকমেন্ডেশন" else "🍽️ Today's Food Suggestion Award",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Interactive dynamic header image representation (satisfying Image/Visual request)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF81C784), Color(0xFF2E7D32))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🍲", fontSize = 52.sp)
                        Text(
                            text = "ANEXSOPZ MOOD-DIET MATCHmaker",
                            color = Color.White.copy(alpha = 0.82f),
                            fontWeight = FontWeight.Black,
                            fontSize = 10.sp,
                            letterSpacing = 1.sp
                        )
                    }
                }

                Text(
                    text = currentSuggestion.first,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = currentSuggestion.second,
                    fontSize = 12.0.sp,
                    color = Color.DarkGray
                )
            }
        }

        // Split Layout: Activity suggestion
        Text(
            text = if (isBengali) "🧘‍♂️ আবেগ ও স্ট্রেস নিয়ন্ত্রণে সহায়ক অ্যাক্টিভিটি" else "🧘‍♂️ Suggested Lifestyle Activity Tip",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
            border = BorderStroke(1.dp, Color(0xFF90CAF9)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Text("💡", fontSize = 24.sp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isBengali) "মাইন্ডফুলনেস রেকমেন্ডেশন" else "Mindfulness Therapy Card",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFF1565C0)
                    )
                    Text(
                        text = currentSuggestion.third,
                        fontSize = 11.5.sp,
                        color = Color.DarkGray
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // SWIPE INTERACTIVE CONTROL
        Button(
            onClick = { isAlternativeSelected = !isAlternativeSelected },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("🔄", fontSize = 16.sp)
                Text(
                    text = if (isBengali) "সোয়াইপ করুন / অল্টারনেটিভ অপশন বাছাই" 
                           else "Swipe / Select Alternative Plan",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

data class ToolItemData(
    val id: String,
    val title: String,
    val emoji: String,
    val subtitle: String,
    val bgColor: Color
)

fun getToolTitle(id: String, isBengali: Boolean): String {
    return when(id) {
        "search" -> if(isBengali) "খাদ্য অনুসন্ধান (Open Food Facts)" else "Food Search (Open Food Facts API)"
        "scanner" -> if(isBengali) "বারকোড পুষ্টি স্ক্যানার" else "Barcode packaged Scanner"
        "recipes" -> if(isBengali) "সুষম রেসিপি সলিউশন" else "Healthy Diet-Based Recipes"
        "image" -> if(isBengali) "খাবারের ছবি সনাক্তকরণ" else "Dish Image Identification"
        "exercise" -> if(isBengali) "ব্যায়াম ও ক্যালোরি ক্ষয়" else "Exercise Log (Active Burnt)"
        "deals" -> if(isBengali) "বাংলাদেশ গ্রোসারি অফার ডিলস" else "Grocery Deals (Bangladesh)"
        "analytics" -> if(isBengali) "অগ্রগতি গ্রাফ ও BMI" else "Progress & BMI Analytics"
        "reminders" -> if(isBengali) "নোটিফিকেশন ও অ্যালার্ম সেটিংস" else "Hydration and Alarms Setup"
        "share" -> if(isBengali) "সোশ্যাল মিডিয়া শেয়ারিং" else "Social Summary Share"
        "facts" -> if(isBengali) "১০০ বৈজ্ঞানিক স্বাস্থ্য ও পুষ্টি তথ্য" else "100 Scientific Health & Nutrition Facts"
        "dietinfo" -> if(isBengali) "১০০ ডায়েট ও লাইফ নীতি গাইড" else "100 Professional Diet & Lifestyle Guidelines"
        "weight_checker" -> if(isBengali) "আদর্শ ওজন ও ক্যালোরি প্ল্যানার" else "Ideal Weight & Calories Calculator"
        "rating" -> if(isBengali) "앱 রেটিং ও রিভিউ সেন্টার" else "ANEXSOPZ App Ratings & Reviews Hub"
        "mood" -> if(isBengali) "আবেগ ও লাইফস্টাইল গাইড" else "Mood & Lifestyle Planner"
        "restaurants" -> if(isBengali) "নিকটস্থ রেস্টুরেন্ট ও খাবার অর্ডার" else "Nearby Restaurants & Food Delivery"
        "community" -> if(isBengali) "কমিউনিটি ও রেসিপি এক্সচেঞ্জ" else "Community Feed & Recipe Swap"
        "emergency" -> if(isBengali) "জরুরি সাহায্য ও ভলান্টিয়ার ব্যবস্থা" else "Local Volunteer & Emergency Helper"
        else -> ""
    }
}


// ========================================================
// 1. FOOD SEARCH & LOGGING SCREEN (OPEN FOOD FACTS API)
// ========================================================
@Composable
fun FoodSearchToolScreen(viewModel: DietPlannerViewModel, isBengali: Boolean) {
    var searchQuery by remember { mutableStateOf("") }
    val results by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text(if(isBengali) "খাবারের নাম লিখুন (যেমন: Oats, Biscuit)" else "Food name (e.g. Oats, Apple)") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { viewModel.searchFood(searchQuery) },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(if(isBengali) "খুঁজুন" else "Search")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        if (isSearching) {
            Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (results.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = if(isBengali) "কোন খাবার অনুসন্ধান করা হয়নি বা পাওয়া যায়নি।" else "Search empty. Try typing oats, wheat bread, honey or soda.",
                    color = Color.Gray,
                    fontSize = 13.sp
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(results) { item ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(12.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Dummy image thumbnail
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFEEEEEE)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🍎", fontSize = 24.sp)
                            }
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if(isBengali) "ক্যালোরি: ${item.calories} kcal (প্রতি ১০০ গ্রাম)" 
                                           else "Calories: ${item.calories} kcal/100g",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "protein: ${item.protein}g | carbs: ${item.carbs}g | fat: ${item.fat}g",
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }
                            
                            Button(
                                onClick = {
                                    viewModel.addFoodLog(item.name, item.calories, item.protein, item.carbs, item.fat)
                                },
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(14.dp), tint = Color.White)
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(if(isBengali) "যোগ" else "Log", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


// ========================================================
// 2. BARCODE NUTRITION SCANNER SCREEN
// ========================================================
@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
@Composable
fun CameraBarcodeScannerPreview(
    onBarcodeDetected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    
    var lastScannedBarcode by remember { mutableStateOf("") }
    var lastScanTime by remember { mutableStateOf(0L) }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val scanner = BarcodeScanning.getClient()

                val imageAnalyzer = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { analysis ->
                        analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                            val mediaImage = imageProxy.image
                            if (mediaImage != null) {
                                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                scanner.process(image)
                                    .addOnSuccessListener { barcodes ->
                                        for (barcode in barcodes) {
                                            val rawValue = barcode.rawValue
                                            if (rawValue != null && rawValue.isNotBlank()) {
                                                val now = System.currentTimeMillis()
                                                if (rawValue != lastScannedBarcode || now - lastScanTime > 3000) {
                                                    lastScannedBarcode = rawValue
                                                    lastScanTime = now
                                                    onBarcodeDetected(rawValue)
                                                }
                                            }
                                        }
                                    }
                                    .addOnFailureListener {
                                        // Fail silently
                                    }
                                    .addOnCompleteListener {
                                        imageProxy.close()
                                    }
                            } else {
                                imageProxy.close()
                            }
                        }
                    }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalyzer
                    )
                } catch (exc: Exception) {
                    exc.printStackTrace()
                }

            }, androidx.core.content.ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = modifier
    )

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }
}

@Composable
fun BarcodeScannerToolScreen(viewModel: DietPlannerViewModel, isBengali: Boolean) {
    var manualBarcode by remember { mutableStateOf("") }
    val scannedProduct by viewModel.scannedProduct.collectAsState()
    val isScanningBar by viewModel.isScanningBarcode.collectAsState()
    
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
        }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "laser")
    val laserOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laserOffset"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Scanner Viewfinder
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (hasCameraPermission) {
                CameraBarcodeScannerPreview(
                    onBarcodeDetected = { code ->
                        manualBarcode = code
                        viewModel.scanBarcode(code)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Camera Required",
                        tint = Color.LightGray.copy(alpha = 0.6f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if(isBengali) "ক্যামেরা স্ক্যানার" else "Live Camera Scanner",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = if(isBengali) "সুপারশপের যেকোনো বারকোড ক্যামেরার সামনে ধরুন" else "Place barcode inside viewport to scan",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { permissionLauncher.launch(android.Manifest.permission.CAMERA) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(if(isBengali) "অনুমতি দিন" else "Grant Camera Permission")
                    }
                }
            }
            
            // Focus Bracket Overlay
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 3.dp.toPx()
                val bracketLength = 24.dp.toPx()
                val color = Color(0xFF4CAF50)
                
                // Top Left
                drawRect(
                    color = Color.Black.copy(alpha = 0.3f),
                    size = size
                )
                
                // Draw clear viewfinder reticle in center
                drawRoundRect(
                    color = Color.Transparent,
                    topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.1f, size.height * 0.15f),
                    size = androidx.compose.ui.geometry.Size(size.width * 0.8f, size.height * 0.7f),
                    blendMode = androidx.compose.ui.graphics.BlendMode.Clear
                )
                
                val rx = size.width * 0.1f
                val ry = size.height * 0.15f
                val rw = size.width * 0.8f
                val rh = size.height * 0.7f
                
                // Top Left Corner of Clear Area
                drawLine(color, androidx.compose.ui.geometry.Offset(rx, ry), androidx.compose.ui.geometry.Offset(rx + bracketLength, ry), strokeWidth)
                drawLine(color, androidx.compose.ui.geometry.Offset(rx, ry), androidx.compose.ui.geometry.Offset(rx, ry + bracketLength), strokeWidth)
                
                // Top Right Corner of Clear Area
                drawLine(color, androidx.compose.ui.geometry.Offset(rx + rw, ry), androidx.compose.ui.geometry.Offset(rx + rw - bracketLength, ry), strokeWidth)
                drawLine(color, androidx.compose.ui.geometry.Offset(rx + rw, ry), androidx.compose.ui.geometry.Offset(rx + rw, ry + bracketLength), strokeWidth)
                
                // Bottom Left Corner of Clear Area
                drawLine(color, androidx.compose.ui.geometry.Offset(rx, ry + rh), androidx.compose.ui.geometry.Offset(rx + bracketLength, ry + rh), strokeWidth)
                drawLine(color, androidx.compose.ui.geometry.Offset(rx, ry + rh), androidx.compose.ui.geometry.Offset(rx, ry + rh - bracketLength), strokeWidth)
                
                // Bottom Right Corner of Clear Area
                drawLine(color, androidx.compose.ui.geometry.Offset(rx + rw, ry + rh), androidx.compose.ui.geometry.Offset(rx + rw - bracketLength, ry + rh), strokeWidth)
                drawLine(color, androidx.compose.ui.geometry.Offset(rx + rw, ry + rh), androidx.compose.ui.geometry.Offset(rx + rw, ry + rh - bracketLength), strokeWidth)
            }
            
            // Holographic pulsing laser animation over clear viewfinder
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(3.dp)
                    .offset(y = (-60).dp + 140.dp * laserOffset)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.Red.copy(alpha = 0.2f), Color.Red, Color.Red.copy(alpha = 0.2f))
                        )
                    )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Preset Items / Emulator Simulator mode
        Text(
            text = if(isBengali) "সিমুলেটর দিয়ে তাড়াতাড়ি টেস্ট করুন:" else "Select barcode to simulate live database lookup:",
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.secondary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "Coca Cola" to "5449000131805",
                "Quaker Oats" to "0030000010200",
                "Dark Chocolate" to "3046920022651"
            ).forEach { (label, code) ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable {
                            manualBarcode = code
                            viewModel.scanBarcode(code)
                        }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(code.take(6) + "...", fontSize = 9.sp, color = Color.Gray)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = manualBarcode,
                onValueChange = { manualBarcode = it },
                label = { Text(if(isBengali) "বারকোড নাম্বারটি লিখুন" else "Or type Barcode manually") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = { viewModel.scanBarcode(manualBarcode) }) {
                Text(if(isBengali) "খুঁজুন" else "Query")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (isScanningBar) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            scannedProduct?.let { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("✅", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if(isBengali) "পণ্য খুঁজে পাওয়া গিয়েছে!" else "Product Found via API!",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1B5E20)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(item.name, fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color.Black)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if(isBengali) "ক্যালোরি: ${item.calories} কি.ক্যালরি" else "Calories: ${item.calories} kcal/100g",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "protein: ${item.protein}g | carbs: ${item.carbs}g | fat: ${item.fat}g",
                            fontSize = 12.sp,
                            color = Color.DarkGray
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                viewModel.addFoodLog(item.name, item.calories, item.protein, item.carbs, item.fat)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Save, contentDescription = "Log")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if(isBengali) "খাদ্যতালিকায় যোগ করুন (Log Item)" else "Add to Daily Snacks")
                        }
                    }
                }
            } ?: Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = if(isBengali) "আপনার স্ক্যানড পণ্যের বিবরণ এখানে প্রদর্শিত হবে" else "Scanned product information appears here.",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}


// ========================================================
// 3. RECIPE SUGGESTIONS TOOL SCREEN & STEP-BY-STEP RECIPE PLAYER
// ========================================================

data class StepByStepRecipe(
    val title: String,
    val titleBn: String,
    val duration: String,
    val durationBn: String,
    val calories: String,
    val caloriesValue: Int,
    val category: String,
    val categoryBn: String,
    val ingredients: List<String>,
    val ingredientsBn: List<String>,
    val steps: List<String>,
    val stepsBn: List<String>,
    val stepDurationsSeconds: List<Int>,
    val tip: String,
    val tipBn: String
)

val healthyRecipesList = listOf(
    StepByStepRecipe(
        title = "Low-GI Oats Vegetable Khichuri",
        titleBn = "লো-জিআই ডায়েট ওটস খিচুড়ি",
        duration = "20 mins",
        durationBn = "২০ মিনিট",
        calories = "250 kcal",
        caloriesValue = 250,
        category = "Diabetes & Blood Pressure Safe",
        categoryBn = "ডায়াবেটিস ও উচ্চ রক্তচাপ বান্ধব",
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
            "Add chopped carrots, papaya, ginger, mustard oil, and salt-alternative; sauté briefly.",
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
        stepDurationsSeconds = listOf(180, 180, 480, 120, 60),
        tip = "Oats are filled with beta-glucan fiber which helps regulate blood glucose spikes.",
        tipBn = "ওটস-এ থাকা বিটা-গ্লুকান ফাইবার রক্তে গ্লুকোজের আকস্মিক বৃদ্ধি কমাতে ভীষণ সাহায্য করে।"
    ),
    StepByStepRecipe(
        title = "Steamed Lean Fish in Lemon Herbs",
        titleBn = "লেবু-ধনেপাতায় ভাপা কোরাল বা রুই",
        duration = "25 mins",
        durationBn = "২৫ মিনিট",
        calories = "180 kcal",
        caloriesValue = 180,
        category = "Heart Healthy & Lean Protein",
        categoryBn = "হার্ট-বান্ধব ও লীন প্রোটিন",
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
            "Make 2-3 small superficial diagonal slits on both sides of the cleaned fish piece.",
            "Mix lemon juice, garlic, black pepper, and coriander paste together and rub thoroughly on fish.",
            "Let the marination rest in the refrigerator for 15 minutes to soak flavors.",
            "Place the marinated fish in a heatproof steaming plate, cover with aluminum foil or lid.",
            "Steam inside a water bath cooker for 12-15 minutes on medium-high heat until flaky."
        ),
        stepsBn = listOf(
            "মাছের টুকরো ভালো করে ধুয়ে দুই পাশে চাকু দিয়ে হালকা ১/২ ইঞ্চি চিড়ে নিন।",
            "লেবুর রস, রসুন, ধনেপাতা বাটা ও গোলমরিচ একসাথে মিশিয়ে মাছের গায়ে মেখে নিন।",
            "মসলা ভেতরে ঢোকার জন্য ১৫ মিনিট ফ্রিজে ম্যারিনেট করতে রাখুন।",
            "একটি ভাপে দেওয়ার পাত্রে মাছ রাখুন এবং ফয়েল পেপার বা ঢাকনা দিয়ে আটকে দিন।",
            "পানির ভাপ ওঠা চুলায় ঢাকনা দিয়ে ১২-১৫ মিনিট ভাপিয়ে নিন।"
        ),
        stepDurationsSeconds = listOf(180, 240, 900, 120, 720),
        tip = "Steaming preserves premium Omega-3 fatty acids without adding unhealthy oxidized cooking fats.",
        tipBn = "ভাপ পদ্ধতি ক্ষতিকর চর্বি যুক্ত করা ছাড়াই মাছের অমূল্য ওমেগা-৩ ফ্যাটি এসিড সুরক্ষিত রাখে।"
    ),
    StepByStepRecipe(
        title = "Fluffy White Veggie Frittata",
        titleBn = "সবজি ও ডিমের প্রোটিন অমলেট",
        duration = "15 mins",
        durationBn = "১৫ মিনিট",
        calories = "130 kcal",
        caloriesValue = 130,
        category = "Weight Management Target",
        categoryBn = "ওজন নিয়ন্ত্রণ ও পেশী গঠন",
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
            "Warm a non-stick frying pan, brush with a droplet of olive oil or cold pressed oil.",
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
        stepDurationsSeconds = listOf(120, 120, 60, 300, 60),
        tip = "Frittatas keep hunger hormones in check due to highly bio-available protein density.",
        tipBn = "উচ্চ মাত্রার প্রোটিন থাকার কারণে ফ্রিতাতা দীর্ঘক্ষণ ক্ষুধা নিয়ন্ত্রণে রাখতে অতুলনীয়।"
    )
)

fun RecipeEntity.toStepByStepRecipe(): StepByStepRecipe {
    val durations = this.steps.map { step ->
        (step.length * 3).coerceIn(60, 300)
    }
    return StepByStepRecipe(
        title = this.title,
        titleBn = this.titleBn,
        duration = this.duration,
        durationBn = this.durationBn,
        calories = this.calories,
        caloriesValue = this.caloriesValue,
        category = this.category,
        categoryBn = this.categoryBn,
        ingredients = this.ingredients,
        ingredientsBn = this.ingredientsBn,
        steps = this.steps,
        stepsBn = this.stepsBn,
        stepDurationsSeconds = durations,
        tip = this.tip,
        tipBn = this.tipBn
    )
}

@Composable
fun RecipeSuggestionsToolScreen(viewModel: DietPlannerViewModel, isBengali: Boolean) {
    val suggestedRecipes by viewModel.suggestedRecipes.collectAsState()
    val isGenerating by viewModel.isGeneratingRecipes.collectAsState()
    val mealPlan by viewModel.currentMealPlan.collectAsState()
    val userProfileOpt by viewModel.userProfile.collectAsState()
    val currentFoodLogs by viewModel.currentFoodLogs.collectAsState()

    val dbRecipes by viewModel.allRecipes.collectAsState()
    var recipeSearchQuery by remember { mutableStateOf("") }
    var selectedMealType by remember { mutableStateOf("All") }
    var selectedRestriction by remember { mutableStateOf("All") }
    var showFavoritesOnly by remember { mutableStateOf(false) }

    val filteredRecipes = remember(dbRecipes, recipeSearchQuery, selectedMealType, selectedRestriction, showFavoritesOnly) {
        dbRecipes.filter { recipe ->
            val matchesSearch = recipeSearchQuery.isEmpty() ||
                    recipe.title.contains(recipeSearchQuery, ignoreCase = true) ||
                    recipe.titleBn.contains(recipeSearchQuery, ignoreCase = true) ||
                    recipe.category.contains(recipeSearchQuery, ignoreCase = true) ||
                    recipe.categoryBn.contains(recipeSearchQuery, ignoreCase = true)

            val matchesMealType = selectedMealType == "All" || recipe.mealType.equals(selectedMealType, ignoreCase = true)

            val matchesRestriction = selectedRestriction == "All" ||
                    recipe.dietaryRestrictions.contains(selectedRestriction, ignoreCase = true)

            val matchesFavorites = !showFavoritesOnly || recipe.isFavorite

            matchesSearch && matchesMealType && matchesRestriction && matchesFavorites
        }
    }

    val profile = userProfileOpt
    val targetCalories = profile?.dailyCalorieTarget ?: 2000
    val goal = profile?.goal ?: "Maintenance"
    val pref = profile?.dietaryPreference ?: "Regular"

    val proteinPct: Double
    val carbsPct: Double
    val fatPct: Double

    when (goal.lowercase(java.util.Locale.ROOT)) {
        "weight loss", "ওজন কমানো" -> {
            proteinPct = 0.35
            carbsPct = 0.35
            fatPct = 0.30
        }
        "weight gain", "ওজন বাড়ানো" -> {
            proteinPct = 0.25
            carbsPct = 0.50
            fatPct = 0.25
        }
        else -> {
            proteinPct = 0.25
            carbsPct = 0.45
            fatPct = 0.30
        }
    }

    val targetProteinGrams = ((targetCalories * proteinPct) / 4.0).coerceAtLeast(1.0)
    val targetCarbsGrams = ((targetCalories * carbsPct) / 4.0).coerceAtLeast(1.0)
    val targetFatGrams = ((targetCalories * fatPct) / 9.0).coerceAtLeast(1.0)

    val consumedCalories = currentFoodLogs.sumOf { it.calories }
    val consumedProtein = currentFoodLogs.sumOf { it.protein }
    val consumedCarbs = currentFoodLogs.sumOf { it.carbs }
    val consumedFat = currentFoodLogs.sumOf { it.fat }

    val remainingCalories = (targetCalories - consumedCalories).coerceAtLeast(0)
    val remainingProtein = (targetProteinGrams - consumedProtein).coerceAtLeast(0.0)
    val remainingCarbs = (targetCarbsGrams - consumedCarbs).coerceAtLeast(0.0)
    val remainingFat = (targetFatGrams - consumedFat).coerceAtLeast(0.0)

    var showAiGenerator by remember { mutableStateOf(false) }
    var selectedRecipeForCooking by remember { mutableStateOf<StepByStepRecipe?>(null) }

    if (selectedRecipeForCooking != null) {
        StepByStepCookingPlayer(
            recipe = selectedRecipeForCooking!!,
            isBengali = isBengali,
            viewModel = viewModel,
            onBack = { selectedRecipeForCooking = null }
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Tab Header to toggle Curated Recipes or AI custom generator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .background(Color(0xFFEEEEEE), RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (!showAiGenerator) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { showAiGenerator = false }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isBengali) "ডায়েটিশিয়ান সেরা রেসিপি" else "Dietitian Cookbooks",
                        color = if (!showAiGenerator) Color.White else Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (showAiGenerator) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { showAiGenerator = true }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isBengali) "কাস্টম রেসিপি" else "Recommendations",
                        color = if (showAiGenerator) Color.White else Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            if (!showAiGenerator) {
                Text(
                    text = if (isBengali) "স্বাস্থ্যকর রেসিপি গাইডবুক" else "Interactive Health Cookbooks",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Search Bar Text Field
                OutlinedTextField(
                    value = recipeSearchQuery,
                    onValueChange = { recipeSearchQuery = it },
                    label = { Text(if (isBengali) "রেসিপি খুঁজুন (যেমন: ওটস, ফিশ, সালাদ)" else "Search recipes (e.g., Oats, Fish, Salad)") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    trailingIcon = {
                        if (recipeSearchQuery.isNotEmpty()) {
                            IconButton(onClick = { recipeSearchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )

                // Meal Type Filters Header
                Text(
                    text = if (isBengali) "খাবারের ধরন (Meal Type):" else "Filter by Meal Type:",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                val mealTypes = listOf("All", "Breakfast", "Lunch", "Dinner", "Snack")
                val mealTypesBn = listOf("সব", "সকালের নাস্তা", "দুপুরের খাবার", "রাতের খাবার", "হালকা খাবার")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    mealTypes.forEachIndexed { idx, type ->
                        val isSelected = selectedMealType == type
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedMealType = type },
                            label = { Text(if (isBengali) mealTypesBn[idx] else type) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }

                // Dietary Restrictions Header & Favorites Toggle Option
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isBengali) "পথ্য বা সীমাবদ্ধতা (Diet Restrictions):" else "Dietary Restrictions:",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (showFavoritesOnly) Color(0xFFFFEBEE) else Color.Transparent)
                            .clickable { showFavoritesOnly = !showFavoritesOnly }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (showFavoritesOnly) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorites Toggle",
                            tint = if (showFavoritesOnly) Color.Red else Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isBengali) "প্রিয় তালিকা" else "Favs Only",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (showFavoritesOnly) Color.Red else Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                val restrictions = listOf("All", "Vegetarian", "Vegan", "Gluten-Free", "Diabetic-Friendly", "Low-Sodium")
                val restrictionsBn = listOf("সব", "নিরামিষ", "ভেগান", "গ্লুটেন-মুক্ত", "ডায়াবেটিস-বান্ধব", "লো-সোডিয়াম")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    restrictions.forEachIndexed { idx, res ->
                        val isSelected = selectedRestriction == res
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedRestriction = res },
                            label = { Text(if (isBengali) restrictionsBn[idx] else res) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        )
                    }
                }

                if (filteredRecipes.isEmpty()) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                            .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(16.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🥗", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (isBengali) "কোন সংগতিপূর্ণ রেসিপি পাওয়া যায়নি" else "No matching recipes found",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isBengali) "আপনার অনুসন্ধান বা ফিল্টার পরিবর্তন করে পুনরায় চেষ্টা করুন।" else "Try clearing search queries or adjusting filters to find healthy ideas.",
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                color = Color.Gray
                            )
                            if (recipeSearchQuery.isNotEmpty() || selectedMealType != "All" || selectedRestriction != "All" || showFavoritesOnly) {
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedButton(
                                    onClick = {
                                        recipeSearchQuery = ""
                                        selectedMealType = "All"
                                        selectedRestriction = "All"
                                        showFavoritesOnly = false
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(if (isBengali) "ফিল্টার রিসেট করুন" else "Reset All Filters")
                                }
                            }
                        }
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        filteredRecipes.forEach { recipe ->
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(16.dp))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = if (isBengali) recipe.categoryBn else recipe.category,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier
                                                    .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                            Text(
                                                text = recipe.mealType,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF1B5E20),
                                                modifier = Modifier
                                                    .background(Color(0xFFE8F5E9), RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = { viewModel.toggleRecipeFavorite(recipe.id, !recipe.isFavorite) },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (recipe.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                                contentDescription = "Toggle Favorite",
                                                tint = if (recipe.isFavorite) Color.Red else Color.Gray,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = if (isBengali) recipe.titleBn else recipe.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = Color(0xFF1B5E20)
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("⏱️", fontSize = 12.sp)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (isBengali) "রান্নার সময়: ${recipe.durationBn}" else "Prep: ${recipe.duration}",
                                                fontSize = 12.sp,
                                                color = Color.DarkGray
                                            )
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("🔥", fontSize = 12.sp)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = recipe.calories,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.Gray
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Text(
                                        text = if (isBengali) "প্রয়োজনীয় উপাদান (Ingredients):" else "Recipe Ingredients:",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color.Black
                                    )

                                    val ingList = if (isBengali) recipe.ingredientsBn else recipe.ingredients
                                    ingList.take(3).forEach { ing ->
                                        Row(
                                            modifier = Modifier.padding(vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("•", color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 4.dp))
                                            Text(ing, fontSize = 12.sp, color = Color.DarkGray)
                                        }
                                    }
                                    if (ingList.size > 3) {
                                        Text(
                                            text = if (isBengali) "...এবং আরও ${ingList.size - 3}টি উপকরণ" else "...and ${ingList.size - 3} more ingredients",
                                            fontSize = 11.sp,
                                            color = Color.Gray,
                                            modifier = Modifier.padding(start = 12.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Button(
                                        onClick = { selectedRecipeForCooking = recipe.toStepByStepRecipe() },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                                    ) {
                                        Text(if (isBengali) "প্রণালী ও রান্না শুরু করুন" else "View Steps & Start Cooking")
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("🧑‍🍳")
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Classic AI custom recommendations tab loaded with active Remaining Targets Discovery
                // 1. HEADER CARD: Real-time Remaining Nutrients
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📊", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isBengali) "আজকের অবশিষ্টাংশ পুষ্টি লক্ষ্যমাত্রা" else "Remaining Daily Diet Targets",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Row of Metrics
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Calories Pillar
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(Color(0xFFFFF2E2), RoundedCornerShape(12.dp))
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("🔥", fontSize = 16.sp)
                                    Text(
                                        text = "$remainingCalories",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color(0xFFE65100)
                                    )
                                    Text(
                                        text = if (isBengali) "ক্যালরি বাকি" else "Cal Rem",
                                        fontSize = 10.sp,
                                        color = Color.Gray,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            // Protein Pillar
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(Color(0xFFE8F5E9), RoundedCornerShape(12.dp))
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("🍗", fontSize = 16.sp)
                                    Text(
                                        text = "${remainingProtein.toInt()}g",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color(0xFF1B5E20)
                                    )
                                    Text(
                                        text = if (isBengali) "আমিষ বাকি" else "Prot Rem",
                                        fontSize = 10.sp,
                                        color = Color.Gray,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            // Carbs Pillar
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(Color(0xFFFFFDE7), RoundedCornerShape(12.dp))
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("🍚", fontSize = 16.sp)
                                    Text(
                                        text = "${remainingCarbs.toInt()}g",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color(0xFFF57F17)
                                    )
                                    Text(
                                        text = if (isBengali) "শর্করা বাকি" else "Carb Rem",
                                        fontSize = 10.sp,
                                        color = Color.Gray,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            // Fat Pillar
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(Color(0xFFF3E5F5), RoundedCornerShape(12.dp))
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("🥑", fontSize = 16.sp)
                                    Text(
                                        text = "${remainingFat.toInt()}g",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color(0xFF4A148C)
                                    )
                                    Text(
                                        text = if (isBengali) "ফ্যাট বাকি" else "Fat Rem",
                                        fontSize = 10.sp,
                                        color = Color.Gray,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. DISCOVERY & GENERATION SELECTION CARD
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if(isBengali) "স্মার্ট রেসিপি জেনারেটর" else "AI Recipe Discovery & Generation",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if(isBengali) "কাস্টম ডায়েট ও অবশিষ্ট পুষ্টির মেলাবন্ধন ঘটিয়ে ৩টি অতুলনীয় লোকাল স্বাস্থ্যকর রেসিপি তৈরি করুন।" 
                                   else "Generate 3 personalized, easily-preparable healthy meals matching your current nutrition constraints.",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Option A: Remaining targets (Primary option)
                        Button(
                            onClick = {
                                viewModel.generateRecipesForRemainingTargets(
                                    remainingCalories = remainingCalories,
                                    remainingProtein = remainingProtein,
                                    remainingCarbs = remainingCarbs,
                                    remainingFat = remainingFat,
                                    dietaryPref = pref,
                                    isBengali = isBengali
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "recipes", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if(isBengali) "অবশিষ্টাংশ লক্ষ্যমাত্রার রেসিপি খুঁজুন" else "Generate for Remaining Targets",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Option B: Generic meal plan layout (Secondary option)
                        OutlinedButton(
                            onClick = {
                                val planDetails = mealPlan?.let {
                                    "Breakfast: ${it.breakfast}, Lunch: ${it.lunch}, Dinner: ${it.dinner}"
                                } ?: "Lal ruti with vegetable and fish curry"
                                viewModel.generateRecipeSuggestions(planDetails)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Restaurant, contentDescription = "standard_recipes", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if(isBengali) "সাধারণ মিল-প্ল্যান ভিত্তিক রেসিপি খুঁজুন" else "Generate for Standard Meal Plan",
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (isGenerating) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if(isBengali) "স্মার্ট সিস্টেমে রেসিপি তৈরি করা হচ্ছে..." else "Preparing smart recipe suggestions...",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                } else {
                    suggestedRecipes?.let { recipes ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA)),
                            border = BorderStroke(1.dp, Color(0xFFDDDDDD))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("🧑‍🍳", fontSize = 24.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if(isBengali) "প্রস্তাবিত রান্নার ডায়েট রেসিপি:" else "Recommended Diet Recipes:",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Divider(modifier = Modifier.padding(vertical = 12.dp))
                                Text(
                                    text = recipes,
                                    fontSize = 13.sp,
                                    lineHeight = 20.sp,
                                    color = Color.DarkGray
                                )
                            }
                        }
                    } ?: Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if(isBengali) "রেসিপি দেখতে জেনারেট বাটনে চাপ দিন।" else "Tap the button to cook up localized low-cal recipes.",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StepByStepCookingPlayer(
    recipe: StepByStepRecipe,
    isBengali: Boolean,
    viewModel: DietPlannerViewModel,
    onBack: () -> Unit
) {
    var activeStep by remember { mutableStateOf(0) }
    var secondsLeft by remember { mutableStateOf(recipe.stepDurationsSeconds[0]) }
    var running by remember { mutableStateOf(false) }

    // Multi-select list checkbox statuses from ViewModel observing session state
    val checkedIngredientsMap by viewModel.recipeCheckedIngredients.collectAsState()
    val checkedIngredients = checkedIngredientsMap[recipe.title] ?: emptySet()

    // Re-trigger timer state on step change
    LaunchedEffect(activeStep) {
        secondsLeft = recipe.stepDurationsSeconds[activeStep]
        running = false
    }

    // Active Timer countdown loop
    LaunchedEffect(running, secondsLeft) {
        if (running && secondsLeft > 0) {
            delay(1000L)
            secondsLeft--
        } else if (secondsLeft == 0) {
            running = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(Color(0xFFF9FBF9))
            .padding(16.dp)
    ) {
        // Player Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF2E7D32))
            }
            Text(
                text = if (isBengali) "রান্না ও প্রস্তুতি গাইড" else "Active Smart Culinary Helper",
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Text("🍳", fontSize = 24.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Recipe card introduction
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (isBengali) recipe.titleBn else recipe.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF1B5E20)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isBengali) recipe.calories + " (টার্গেট)" else recipe.calories + " (Target)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (isBengali) "ধাপ: ${activeStep + 1} / ${recipe.steps.size}" else "Step: ${activeStep + 1} of ${recipe.steps.size}",
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Ingredients Checklist section
        Text(
            text = if (isBengali) "১. উপকরণ নিশ্চিত করুন (Checklist):" else "1. Gather Ingredients Checklist:",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Color.Black,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, Color(0xFFEEEEEE)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                val ingList = if (isBengali) recipe.ingredientsBn else recipe.ingredients
                ingList.forEachIndexed { index, ing ->
                    val checked = checkedIngredients.contains(index)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.toggleRecipeIngredientChecked(recipe.title, index)
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { isChecked ->
                                viewModel.toggleRecipeIngredientChecked(recipe.title, index)
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = ing,
                            fontSize = 13.sp,
                            color = if (checked) Color.LightGray else Color.Black,
                            style = androidx.compose.ui.text.TextStyle(
                                textDecoration = if (checked) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Step-by-Step interactive process view
        Text(
            text = if (isBengali) "২. ছবির মত ধাপে ধাপে প্রস্তুতি নির্দেশাবলি:" else "2. Interactive Step Progression:",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Color.Black,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, Color(0xFFEEEEEE)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Step header bubble
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (isBengali) "ধাপ নম্বর ${activeStep + 1}" else "Active Step ${activeStep + 1}",
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Active step instruction text
                Text(
                    text = if (isBengali) recipe.stepsBn[activeStep] else recipe.steps[activeStep],
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Countdown clock UI panel
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .background(Color(0xFFE8F5E9), CircleShape)
                        .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val mins = secondsLeft / 60
                        val secs = secondsLeft % 60
                        Text(
                            text = String.format(Locale.getDefault(), "%02d:%02d", mins, secs),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF1B5E20)
                        )
                        Text(
                            text = if (isBengali) "ধাপের সময়" else "Timer",
                            fontSize = 9.sp,
                            color = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Play/pause trigger controls
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledIconButton(
                        onClick = { running = !running },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (running) Color(0xFFD32F2F) else Color(0xFF2E7D32)
                        )
                    ) {
                        Icon(
                            imageVector = if (running) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Control Play",
                            tint = Color.White
                        )
                    }

                    OutlinedButton(
                        onClick = { secondsLeft = recipe.stepDurationsSeconds[activeStep]; running = false },
                        border = BorderStroke(1.dp, Color.Gray)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset timer", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isBengali) "আবার" else "Reset", fontSize = 11.sp)
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 16.dp))

                // Next-Back slider navigation Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { if (activeStep > 0) activeStep-- },
                        enabled = activeStep > 0
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Prev")
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        recipe.steps.forEachIndexed { i, _ ->
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(if (i == activeStep) MaterialTheme.colorScheme.primary else Color.LightGray, CircleShape)
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (activeStep < recipe.steps.size - 1) {
                                activeStep++
                            } else {
                                onBack() // finished
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(
                            text = if (activeStep < recipe.steps.size - 1) 
                                (if (isBengali) "পরবর্তী" else "Next") 
                            else 
                                (if (isBengali) "শেষ করুন" else "Complete!")
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Expert dietitian tip box
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDE7)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("💡", fontSize = 24.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isBengali) "বিশেষজ্ঞ পুষ্টি টিপস (Dietitian Insight):" else "Dietitian's Wellness Tip:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = Color(0xFFF57F17)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isBengali) recipe.tipBn else recipe.tip,
                        fontSize = 11.sp,
                        color = Color.DarkGray
                    )
                }
            }
        }
    }
}


// ========================================================
// 4. FOOD IMAGE RECOGNITION SCREEN (GEMINI ESTIMATION)
// ========================================================
@Composable
fun FoodImageRecognitionToolScreen(viewModel: DietPlannerViewModel, isBengali: Boolean) {
    val aiResult by viewModel.aiImageResult.collectAsState()
    val isAnalyzing by viewModel.isAnalyzingImage.collectAsState()
    
    // Track simulated selected dish
    var selectedPhotoName by remember { mutableStateOf("") }
    
    val popularDishes = remember {
        listOf(
            "Mutton Biryani" to "বিরিয়ানি",
            "Boiled Egg & Brown Ruti" to "ডিম ও রুতি",
            "Fish Curry with Rice" to "মাছ ও ভাত"
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Picture Finder Mock Frame
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFFEEEEEE)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (selectedPhotoName.isEmpty()) "📸" else "🥘",
                    fontSize = 44.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (selectedPhotoName.isEmpty()) 
                               (if(isBengali) "ছবি সিলেক্ট করুন বা নিচে ট্যাপ করুন" else "Select simulated photo from library:")
                           else selectedPhotoName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.DarkGray
                )
            }
            
            // Corner camera crosshair marks
            Box(modifier = Modifier.size(24.dp).align(Alignment.TopStart).border(BorderStroke(2.dp, Color.Gray), shape = RoundedCornerShape(topStart = 8.dp)))
            Box(modifier = Modifier.size(24.dp).align(Alignment.BottomEnd).border(BorderStroke(2.dp, Color.Gray), shape = RoundedCornerShape(bottomEnd = 8.dp)))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Food Choices Grid
        Text(
            text = if(isBengali) "ক্লিকের মাধ্যমে খাবারের ছবি পছন্দ করুন:" else "Snap one of these typical food choices:",
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.secondary
        )
        
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            popularDishes.forEach { (en, bn) ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selectedPhotoName == en) MaterialTheme.colorScheme.primaryContainer else Color(0xFFF3F4F9))
                        .clickable { selectedPhotoName = en }
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if(isBengali) bn else en,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (selectedPhotoName.isNotBlank()) {
                    viewModel.analyzeFoodImage(selectedPhotoName)
                }
            },
            enabled = selectedPhotoName.isNotBlank() && !isAnalyzing,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.CloudUpload, contentDescription = "Analyze")
            Spacer(modifier = Modifier.width(8.dp))
            Text(if(isBengali) "ক্যালোরি এবং পুষ্টি বিশ্লেষণ" else "Analyze Food Image")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isAnalyzing) {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = if(isBengali) "স্মার্ট স্ক্যানার মেপে দেখছে..." else "Identifying ingredients...",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        } else {
            aiResult?.let { text ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if(isBengali) "স্মার্ট বিশ্লেষণের ফলাফল" else "Estimated Nutrient Breakdown",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        Text(
                            text = text,
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            color = Color.DarkGray
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                // Extract a dummy 450 calorie log
                                val cal = if(selectedPhotoName.contains("Biryani")) 750 else 380
                                viewModel.addFoodLog(selectedPhotoName, cal)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF33691E)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if(isBengali) "বিশ্লেষিত খাদ্য ডায়েটে যোগ করুন" else "Log this Analyzed Dish")
                        }
                    }
                }
            }
        }
    }
}


// ========================================================
// 5. EXERCISE TRACKER SCREEN
// ========================================================
@Composable
fun ExerciseTrackerToolScreen(viewModel: DietPlannerViewModel, isBengali: Boolean) {
    var selectedExercise by remember { mutableStateOf("Walking") }
    var durationString by remember { mutableStateOf("30") }
    val loggedExercises by viewModel.currentExerciseLogs.collectAsState()

    val exerciseTypes = remember {
        listOf(
            "Walking" to "হাঁটা হাঁটি",
            "Jogging" to "দৌড়ানো",
            "Yoga" to "যোগব্যায়াম",
            "Gym" to "ভারোত্তোলন / জিম",
            "Cycling" to "সাইকেল চালানো"
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if(isBengali) "ব্যায়াম যোগ করুন (Burn Calories)" else "Burn Calories Workout Log",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                
                Spacer(modifier = Modifier.height(12.dp))

                // Exercise selector
                Text(if(isBengali) "ব্যায়ামের ধরন:" else "Exercise Type:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    exerciseTypes.forEach { (en, bn) ->
                        val isSelected = selectedExercise == en
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFFEEEEEE))
                                .clickable { selectedExercise = en }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = if(isBengali) bn else en,
                                color = if (isSelected) Color.White else Color.Black,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = durationString,
                    onValueChange = { durationString = it },
                    label = { Text(if(isBengali) "সময়সীমা (মিনিট)" else "Duration in minutes") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val duration = durationString.toIntOrNull() ?: 30
                        val metMultiplier = when (selectedExercise) {
                            "Walking" -> 4
                            "Jogging" -> 8
                            "Yoga" -> 2
                            "Gym" -> 6
                            "Cycling" -> 5
                            else -> 4
                        }
                        val calcCal = duration * metMultiplier
                        viewModel.addExerciseLog(selectedExercise, duration, calcCal)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.FitnessCenter, contentDescription = "Add gym")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if(isBengali) "ওয়ার্কআউট যোগ করুন" else "Log Active Workout")
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = if(isBengali) "আজকের সম্পন্ন ব্যায়ামসমূহ:" else "Completed Workouts Today:",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (loggedExercises.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if(isBengali) "এখনো কোনো ব্যায়াম রেকর্ড করা হয়নি।" else "No exercises logged. Get active to increase daily budget!",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        } else {
            loggedExercises.forEach { workout ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            val displayAct = exerciseTypes.firstOrNull { it.first == workout.activity }?.second ?: workout.activity
                            Text(
                                text = if(isBengali) displayAct else workout.activity,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if(isBengali) "সময়কাল: ${workout.durationMin} মিনিট" else "Duration: ${workout.durationMin} mins",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "-${workout.caloriesBurned} kcal",
                                color = Color(0xFFC62828),
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(onClick = { viewModel.deleteExerciseLog(workout.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFC62828))
                            }
                        }
                    }
                }
            }
        }
    }
}


// ========================================================
// 6. GROCERY DEALS BANGLADESH (LOCAL OFFERS)
// ========================================================
@Composable
fun GroceryDealsToolScreen(isBengali: Boolean) {
    val deals = remember(isBengali) {
        listOf(
            GroceryDealCardData("Chaldal (চালডাল)", if(isBengali) "ডায়েট ওটস ৫০০ গ্রাম" else "Quaker Oats 500g", "15% OFF", "Save BDT 65", "https://chaldal.com"),
            GroceryDealCardData("Shwapno (স্বপ্ন)", if(isBengali) "টক দই ৫০০ গ্রাম" else "Low Fat Yogurt 500g", "10% OFF", "Save BDT 20", "https://shwapno.com"),
            GroceryDealCardData("Meena Bazar (মীনা বাজার)", if(isBengali) "তাজা সবুজ আপেল ১ কেজি" else "Fresh Green Apple 1KG", "BDT 40 CashBack", "Best dietary option", "https://meenabazaronline.com"),
            GroceryDealCardData("Unimart (ইউনিমার্ট)", if(isBengali) "এক্সট্রা ভার্জিন অলিভ অয়েল" else "Extra Virgin Olive Oil 500ml", "12% OFF", "Save BDT 140", "https://unimart.online")
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = if(isBengali) "🛒 বাংলাদেশের লোকাল সুপারশপ ডায়েট ডিলস" else "🛒 Supermarket Dietary Savings Bangladesh",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        deals.forEach { deal ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFF3E0)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🎁", fontSize = 24.sp)
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(deal.storeName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFFD84315))
                        Text(deal.itemName, fontWeight = FontWeight.Black, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(2.dp))
                        Row {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFFFFCC80))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(deal.discountBadge, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(deal.savingsText, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

data class GroceryDealCardData(
    val storeName: String,
    val itemName: String,
    val discountBadge: String,
    val savingsText: String,
    val storeUrl: String
)


// ========================================================
// 7. PROGRESS ANALYTICS SCREEN (BMI & GRAPH)
// ========================================================
@Composable
fun ProgressAnalyticsToolScreen(
    viewModel: DietPlannerViewModel,
    userProfile: UserProfileEntity,
    isBengali: Boolean
) {
    val weightLogs by viewModel.allWeightLogs.collectAsState(initial = emptyList())
    
    // Calculate BMI
    val heightInM = userProfile.height / 100.0
    val bmi = if (heightInM > 0) userProfile.weight / (heightInM * heightInM) else 0.0
    
    val bmiCategory = when {
        bmi < 18.5 -> if(isBengali) "কম ওজন (Underweight)" else "Underweight"
        bmi < 24.9 -> if(isBengali) "স্বাভাবিক ওজন (Normal)" else "Normal Weight"
        bmi < 29.9 -> if(isBengali) "অতিরিক্ত ওজন (Overweight)" else "Overweight"
        else -> if(isBengali) "স্থূলতা (Obesity)" else "Obese"
    }

    val bmiColor = when {
        bmi < 18.5 -> Color(0xFF1976D2)
        bmi < 24.9 -> Color(0xFF388E3C)
        bmi < 29.9 -> Color(0xFFF57C00)
        else -> Color(0xFFD32F2F)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if(isBengali) "আপনার বডি মাস ইনডেক্স (BMI)" else "Your Body Mass Index (BMI)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(10.dp))
                
                Text(
                    text = String.format("%.1f", bmi),
                    fontWeight = FontWeight.Black,
                    fontSize = 44.sp,
                    color = bmiColor
                )
                
                Text(
                    text = bmiCategory,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = bmiColor
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = if(isBengali) "উচ্চতা: ${userProfile.height} সে.মি. | ওজন: ${userProfile.weight} কেজি" 
                           else "Height: ${userProfile.height} cm | Weight: ${userProfile.weight} kg",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = if(isBengali) "ওজন পরিবর্তনের নিখুঁত গ্রাফ" else "Weight Logs Graphic Progress",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Draw weights list or standard path visualization
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE0E0E0))
        ) {
            if (weightLogs.size < 2) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if(isBengali) "গ্রাফ আঁকার জন্য অন্তত ২টি ওজনের রেকর্ড প্রয়োজন।" 
                               else "Please log your weight for at least 2 separate days to chart progress.",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    val points = weightLogs.takeLast(7).sortedBy { it.date }
                    val maxWeight = points.maxOf { it.weight }
                    val minWeight = points.minOf { it.weight }
                    val range = (maxWeight - minWeight).coerceAtLeast(1.0)
                    
                    val stepX = size.width / (points.size - 1).coerceAtLeast(1)
                    val pointsPath = android.graphics.Path()

                    points.forEachIndexed { index, item ->
                        val ratio = (item.weight - minWeight) / range
                        // Invert Y because Canvas Y goes downwards
                        val y = size.height - (ratio * size.height).toFloat()
                        val x = index * stepX
                        
                        if (index == 0) {
                            pointsPath.moveTo(x, y)
                        } else {
                            pointsPath.lineTo(x, y)
                        }
                        
                        drawCircle(
                            color = Color(0xFF2E7D32),
                            radius = 6.dp.toPx(),
                            center = androidx.compose.ui.geometry.Offset(x, y)
                        )
                    }

                    drawPath(
                        path = pointsPath.asComposePath(),
                        color = Color(0xFF4CAF50),
                        style = Stroke(width = 3.dp.toPx())
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Simple List of Logs
        weightLogs.sortedByDescending { it.date }.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp, horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(item.date, fontSize = 12.sp, color = Color.Gray)
                Text("${item.weight} kg", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}


// ========================================================
// 8. SOCIAL SHARING SCREEN
// ========================================================
@Composable
fun SocialShareToolScreen(viewModel: DietPlannerViewModel, isBengali: Boolean) {
    val context = LocalContext.current
    val mealPlan by viewModel.currentMealPlan.collectAsState()
    val waterLog by viewModel.waterLog.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(Color(0xFFE8F5E9)),
            contentAlignment = Alignment.Center
        ) {
            Text("✨", fontSize = 50.sp)
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = if(isBengali) "আপনার সাফল্য বন্ধুদের সাথে শেয়ার করুন!" else "Share ANEXSOPZ Achievements!",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if(isBengali) "শেয়ার করুন আপনার আজকের ডায়েট ক্যালোরি রিং, পানি পানের লক্ষ্য এবং স্বাস্থ্যকর জীবনযাত্রার বিবরণ।" 
                   else "Easily format your active diet logs, weight reductions, or daily hydration status into a clean messaging snippet.",
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                val cal = mealPlan?.calorieTarget ?: 1800
                val water = waterLog?.amountMl ?: 0
                val shareText = if (isBengali) {
                    "💪 ANEXSOPZ সুস্থতা অ্যাপের সাহায্যে আমি আমার আজকের পুষ্টিকর ডায়েট টার্গেট সম্পন্ন করেছি! \n🎯 লক্ষ্য ক্যালোরি: $cal kcal \n💧 পানি পানের পরিমাণ: $water ml\n👉 ANEXSOPZ দিয়ে আপনার সুস্থ থাকার যাত্রা আজই শুরু করুন!"
                } else {
                    "💪 Just finished my daily nutrient targets with ANEXSOPZ App! \n🎯 Daily Calorie Goal: $cal kcal \n💧 Hydration: $water ml \n👉 Healthy living starts today with ANEXSOPZ Wellness Tracker!"
                }
                
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, shareText)
                    type = "text/plain"
                }
                context.startActivity(Intent.createChooser(sendIntent, "Share Achievement using"))
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Icon(Icons.Default.Share, contentDescription = "Share now")
            Spacer(modifier = Modifier.width(8.dp))
            Text(if(isBengali) "আজকের লক্ষ্য শেয়ার করুন" else "Share Daily Goals Summary")
        }
    }
}


// ========================================================
// 9. REMINDERS SUB-TAB (DASHBOARD COMPATIBLE ALARM SETUP)
// ========================================================
@Composable
fun RemindersSubTab(
    viewModel: DietPlannerViewModel,
    reminders: List<MealReminderEntity>,
    context: Context,
    isBengali: Boolean
) {
    var showAddForm by remember { mutableStateOf(false) }
    var newReminderName by remember { mutableStateOf("") }
    var newReminderHour by remember { mutableStateOf(12) }
    var newReminderMinute by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Tracker consistency benefit info card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("alarms_card")
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isBengali) "🎯 ট্র্যাকিং ধারাবাহিকতা ও রিমাইন্ডার" else "🎯 Tracking Consistency & Alarms",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isBengali) {
                            "নির্দিষ্ট সময়ে লগের মাধ্যমে ডাটার নির্ভুলতা বজায় রাখুন। দিনে ৫ বা ৬ বার নিয়মিত রেকর্ড সুস্থ জীবনযাত্রাকে গঠন করতে ১০০% সাহায্য করে।"
                        } else {
                            "Reminding you to log your food logs at designated intervals ensures consistent calorie budgets and maximizes habit accuracy."
                        },
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Toggle Expand Form to add customized log reminder
        if (!showAddForm) {
            Button(
                onClick = { showAddForm = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add alert")
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isBengali) "নতুন ট্র্যাকিং রিমাইন্ডার যোগ করুন" else "Create Custom Logging Alert"
                )
            }
        } else {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isBengali) "নতুন ট্র্যাকিং অ্যালার্ট সেটআপ" else "Add Custom Alarm Setup",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = newReminderName,
                        onValueChange = { newReminderName = it },
                        label = { Text(if (isBengali) "রিমাইন্ডারের নাম (উদাঃ বিকালের নাস্তা)" else "Reminder / Meal Name") },
                        placeholder = { Text("e.g. Snack Tracker") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Hour slider
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isBengali) "ঘণ্টা: $newReminderHour" else "Hour: $newReminderHour",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            modifier = Modifier.width(80.dp)
                        )
                        Slider(
                            value = newReminderHour.toFloat(),
                            onValueChange = { newReminderHour = it.toInt() },
                            valueRange = 0f..23f,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Minute slider
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isBengali) "মিনিট: $newReminderMinute" else "Minute: $newReminderMinute",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            modifier = Modifier.width(80.dp)
                        )
                        Slider(
                            value = newReminderMinute.toFloat(),
                            onValueChange = { newReminderMinute = it.toInt() },
                            valueRange = 0f..59f,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = {
                            showAddForm = false
                            newReminderName = ""
                        }) {
                            Text(if (isBengali) "বাতিল" else "Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val nameStr = newReminderName.trim()
                                if (nameStr.isNotEmpty()) {
                                    viewModel.addCustomReminder(
                                        context = context,
                                        name = nameStr,
                                        hour = newReminderHour,
                                        minute = newReminderMinute
                                    )
                                    showAddForm = false
                                    newReminderName = ""
                                }
                            }
                        ) {
                            Text(if (isBengali) "সংরক্ষণ" else "Save")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = if (isBengali) "সক্রিয় রিমাইন্ডারসমূহ (Active Timers):" else "Configured Notifications List:",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        reminders.forEach { reminder ->
            var expanded by remember { mutableStateOf(false) }
            var hState by remember { mutableStateOf(reminder.hour) }
            var mState by remember { mutableStateOf(reminder.minute) }

            // Detect custom reminders
            val isCustom = reminder.id > 6

            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (reminder.isEnabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                border = BorderStroke(1.dp, if (reminder.isEnabled) MaterialTheme.colorScheme.outlineVariant else Color.LightGray)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(reminder.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                
                                // Tag indicating standard vs custom
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (isCustom) MaterialTheme.colorScheme.secondaryContainer 
                                            else MaterialTheme.colorScheme.tertiaryContainer,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (isCustom) {
                                            if (isBengali) "কাস্টম" else "Custom Alert"
                                        } else {
                                            if (isBengali) "সিস্টেম" else "Default"
                                        },
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCustom) MaterialTheme.colorScheme.onSecondaryContainer 
                                                else MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = String.format("%02d:%02d", reminder.hour, reminder.minute) + 
                                    if (reminder.isEnabled) {
                                        if (isBengali) " (সক্রিয়)" else " (Active Daily Alert)"
                                      } else {
                                        if (isBengali) " (নিষ্ক্রিয়)" else " (Turned Off)"
                                      },
                                fontSize = 12.sp,
                                color = if (reminder.isEnabled) MaterialTheme.colorScheme.primary else Color.Gray,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(
                                checked = reminder.isEnabled,
                                onCheckedChange = { isEnabled ->
                                    viewModel.updateReminderTime(context, reminder.id, reminder.name, reminder.hour, reminder.minute, isEnabled)
                                },
                                modifier = Modifier.scale(0.85f)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(onClick = { expanded = !expanded }) {
                                Icon(
                                    imageVector = if (expanded) Icons.Default.Close else Icons.Default.Edit,
                                    contentDescription = "Edit reminder",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (isCustom) {
                                IconButton(
                                    onClick = {
                                        viewModel.deleteReminder(context, reminder.id)
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete custom reminder",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }

                    if (expanded) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(if (isBengali) "ঘণ্টা:" else "Hour:", fontSize = 12.sp, modifier = Modifier.weight(1.5f))
                            Slider(
                                value = hState.toFloat(),
                                onValueChange = { hState = it.toInt() },
                                valueRange = 0f..23f,
                                modifier = Modifier.weight(4f)
                            )
                            Text("$hState", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(if (isBengali) "মিনিট:" else "Min:", fontSize = 12.sp, modifier = Modifier.weight(1.5f))
                            Slider(
                                value = mState.toFloat(),
                                onValueChange = { mState = it.toInt() },
                                valueRange = 0f..59f,
                                modifier = Modifier.weight(4f)
                            )
                            Text("$mState", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
                        }
                        Button(
                            onClick = {
                                viewModel.updateReminderTime(context, reminder.id, reminder.name, hState, mState, true)
                                expanded = false
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(if (isBengali) "সংরক্ষণ" else "Save")
                        }
                    }
                }
            }
        }
    }
}


// ============================================================================
// 10. 100 SCIENTIFIC FOOD & NUTRITION FACTS TOOL
// ============================================================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FoodFactsToolScreen(isBengali: Boolean) {
    val facts = remember { com.example.data.model.DietDataStore.getFullFactsList() }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    val expandedStates = remember { mutableStateMapOf<Int, Boolean>() }

    val categories = remember(isBengali) {
        listOf("All") + facts.map { it.category }.distinct()
    }

    val filteredFacts = remember(searchQuery, selectedCategory) {
        facts.filter { fact ->
            val matchesCategory = selectedCategory == "All" || fact.category == selectedCategory
            val matchesQuery = searchQuery.isEmpty() ||
                    fact.title.contains(searchQuery, ignoreCase = true) ||
                    fact.titleBn.contains(searchQuery, ignoreCase = true) ||
                    fact.fact.contains(searchQuery, ignoreCase = true) ||
                    fact.factBn.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9FBF9))
            .padding(16.dp)
    ) {
        // Search & Filter Block
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text(if (isBengali) "১০০টি তথ্যের মধ্যে খুঁজুন..." else "Search 100 facts...", fontSize = 13.sp) },
            singleLine = true,
            leadingIcon = { Text("🔍", modifier = Modifier.padding(start = 8.dp)) },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF2E7D32),
                unfocusedBorderColor = Color(0xFFC8E6C9)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        )

        // Scrollable category chips
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { cat ->
                val isSelected = selectedCategory == cat
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedCategory = cat },
                    label = { 
                        Text(
                            text = if (cat == "All") (if (isBengali) "সব তথ্য" else "All Facts") else cat,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        ) 
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFE8F5E9),
                        selectedLabelColor = Color(0xFF1B5E20)
                    )
                )
            }
        }

        // List View of facts
        if (filteredFacts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isBengali) "কোনো তথ্য খুঁজে পাওয়া যায়নি!" else "No search matches found!",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredFacts.size) { index ->
                    val fact = filteredFacts[index]
                    val isExpanded = expandedStates[fact.id] ?: false

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, if (isExpanded) Color(0xFF81C784) else Color(0xFFECEFF1)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedStates[fact.id] = !isExpanded }
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
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
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFE8F5E9)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${fact.id}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2E7D32)
                                        )
                                    }
                                    Text(
                                        text = if (isBengali) fact.titleBn else fact.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color(0xFF1B5E20)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFE0F2F1))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (isBengali) fact.categoryBn else fact.category,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF00796B)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (isBengali) fact.factBn else fact.fact,
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                color = Color.DarkGray
                            )

                            if (isExpanded) {
                                Divider(
                                    color = Color(0xFFEEEEEE),
                                    modifier = Modifier.padding(vertical = 10.dp)
                                )
                                Text(
                                    text = if (isBengali) "💡 বৈজ্ঞানিক ব্যাখ্যা: নিয়মিত সুষম পুষ্টিগুণ সমৃদ্ধ খাবার গ্রহণ অন্ত্রের মেটাবলিজম ও দৈনিক রোগ প্রতিরোধ ক্ষমতা বাড়াতে অত্যন্ত ভূমিকা পালন করে।"
                                           else "💡 Scientific insight: Consistent consumption of high-density natural micronutrients optimizes metabolic velocity, preserves gut barrier integrity, and reduces baseline oxidative stress levels.",
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


// ============================================================================
// 11. 100 PROFESSIONAL DIET & LIFESTYLE GUIDELINES TOOL
// ============================================================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DietGuidelinesToolScreen(isBengali: Boolean) {
    val guidelines = remember { com.example.data.model.DietDataStore.getFullGuidelinesList() }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    val categories = remember(isBengali) {
        listOf("All") + guidelines.map { it.category }.distinct()
    }

    val filteredGuidelines = remember(searchQuery, selectedCategory) {
        guidelines.filter { guide ->
            val matchesCategory = selectedCategory == "All" || guide.category == selectedCategory
            val matchesQuery = searchQuery.isEmpty() ||
                    guide.title.contains(searchQuery, ignoreCase = true) ||
                    guide.titleBn.contains(searchQuery, ignoreCase = true) ||
                    guide.instructions.contains(searchQuery, ignoreCase = true) ||
                    guide.instructionsBn.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFCFCFC))
            .padding(16.dp)
    ) {
        // Description banner
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("📚", fontSize = 28.sp, modifier = Modifier.padding(end = 12.dp))
                Column {
                    Text(
                        text = if (isBengali) "১০০ টি ডায়েট ও নিউট্রিশন সারসংক্ষেপ" else "100 Professional Diet Protocols",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF1B5E20)
                    )
                    Text(
                        text = if (isBengali) "পেশাদার নিউট্রিশনিস্টদের পরীক্ষিত ও গ্যারান্টিযুক্ত গাইডলাইন নীতি।"
                               else "Certified nutritionist guidance regarding portion setup, fasting, and gut health.",
                        fontSize = 11.sp,
                        color = Color.DarkGray
                    )
                }
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text(if (isBengali) "১০০টি ক্লায়েন্ট গাইডলাইনের মধ্যে সার্চ..." else "Search 100 client guidelines...", fontSize = 13.sp) },
            singleLine = true,
            leadingIcon = { Text("🔍", modifier = Modifier.padding(start = 8.dp)) },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF2E7D32),
                unfocusedBorderColor = Color(0xFFC8E6C9)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        )

        // Category Selection
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            categories.forEach { cat ->
                val isSelected = selectedCategory == cat
                ElevatedFilterChip(
                    selected = isSelected,
                    onClick = { selectedCategory = cat },
                    label = { 
                        Text(
                            text = if (cat == "All") (if (isBengali) "সব গাইড" else "All Guides") else cat,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        ) 
                    }
                )
            }
        }

        if (filteredGuidelines.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isBengali) "কোনো ডায়েট গাইডলাইন মিল পাওয়া যায়নি!" else "No guideline matches found!",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredGuidelines.size) { index ->
                    val guide = filteredGuidelines[index]
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        border = BorderStroke(1.dp, Color(0xFFF1F1F1)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("🍏", fontSize = 16.sp)
                                    Text(
                                        text = "${guide.id}. " + (if (isBengali) guide.titleBn else guide.title),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp,
                                        color = Color(0xFF1E5E2F)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFFFFF3E0))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (isBengali) guide.categoryBn else guide.category,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFE65100)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (isBengali) guide.instructionsBn else guide.instructions,
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                color = Color.DarkGray
                            )
                        }
                    }
                }
            }
        }
    }
}


// ============================================================================
// 12. IDEAL BODY REVOLUTION & TARGET WEIGHT TARGET CALCULATOR
// ============================================================================
@Composable
fun IdealWeightCalculatorScreen(isBengali: Boolean, userProfile: UserProfileEntity) {
    var heightCm by remember { mutableStateOf(userProfile.height.toFloat()) }
    var weightKg by remember { mutableStateOf(userProfile.weight.toFloat()) }
    var ageYears by remember { mutableStateOf(userProfile.age) }
    var activeLevel by remember { mutableStateOf(1.375f) } // Light Activity by default

    val bmr = remember(heightCm, weightKg, ageYears, userProfile.gender) {
        if (userProfile.gender.lowercase() == "male" || userProfile.gender.contains("পুরুষ")) {
            (10.0 * weightKg) + (6.25 * heightCm) - (5.0 * ageYears) + 5
        } else {
            (10.0 * weightKg) + (6.25 * heightCm) - (5.0 * ageYears) - 161
        }
    }

    val tdee = remember(bmr, activeLevel) { bmr * activeLevel }

    val heightM = heightCm / 100f
    val bmi = remember(heightM, weightKg) { weightKg / (heightM * heightM) }

    val idealWeightMin = remember(heightM) { 18.5 * (heightM * heightM) }
    val idealWeightMax = remember(heightM) { 24.9 * (heightM * heightM) }

    val bmiCategory = remember(bmi, isBengali) {
        when {
            bmi < 18.5 -> if (isBengali) "ওজন স্বল্পতা (Underweight) — পুষ্টিকর শর্করা ও প্রোটিন বাড়ান!" else "Underweight — Boost your complex carbs and healthy fats!"
            bmi < 25.0 -> if (isBengali) "নিখুঁত বা আদর্শ ওজন (Normal Weight) — দারুণ! ধারাবাহিকতা ধরে রাখুন।" else "Normal — Perfect healthy body mass profile! Keep it up."
            bmi < 30.0 -> if (isBengali) "অতিরিক্ত ওজন (Overweight) — ডায়েট ক্যালরি ৩০% কমানো প্রয়োজন।" else "Overweight — Mild deficit recommended to protect joints."
            else -> if (isBengali) "স্থূলতা বা অতিওজন (Obese) — এখনই পরিকল্পিত ক্যালোরি নিয়ন্ত্রণ শুরু করুন।" else "Obese — Planned cardio & portion controls highly recommended."
        }
    }

    val bmiColor = remember(bmi) {
        when {
            bmi < 18.5 -> Color(0xFF29B6F6)
            bmi < 25.0 -> Color(0xFF66BB6A)
            bmi < 30.0 -> Color(0xFFFFA726)
            else -> Color(0xFFEF5350)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(Color(0xFFF7F9FC))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title block
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (isBengali) "⚖️ আর্দশ ওজনের লক্ষ্য ও ক্যালোরি প্ল্যান" else "⚖️ Ideal Target Weight Analytics",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isBengali) "আপনার বডি স্ট্রাকচার সচল রাখতে মেডিকেল গ্রেড ইন্ডিকেটর ক্যালকুলেটর।"
                           else "Medical-grade biometric calculations based on your custom height & gender metrics.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Live stats adjuster
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = if (isBengali) "শারীরিক পরিমাপ পরিবর্তন করুন" else "Adjust Vitals Real-Time",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color.DarkGray
                )

                // Height Slider
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(if (isBengali) "উচ্চতা (Height):" else "Height:", fontSize = 11.sp, color = Color.Gray)
                        Text(String.format(Locale.ROOT, "%.1f cm (%.1f in)", heightCm, heightCm / 2.54), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF1B5E20))
                    }
                    Slider(
                        value = heightCm,
                        onValueChange = { heightCm = it },
                        valueRange = 120f..220f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF2E7D32), activeTrackColor = Color(0xFF81C784))
                    )
                }

                // Weight Slider
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(if (isBengali) "ওজন (Weight):" else "Weight:", fontSize = 11.sp, color = Color.Gray)
                        Text(String.format(Locale.ROOT, "%.1f kg (%.1f lbs)", weightKg, weightKg * 2.204), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF1B5E20))
                    }
                    Slider(
                        value = weightKg,
                        onValueChange = { weightKg = it },
                        valueRange = 35f..150f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF2E7D32), activeTrackColor = Color(0xFF81C784))
                    )
                }

                // Activity level dropdown selectors
                Text(if (isBengali) "শারীরিক পরিশ্রমের মাত্রা:" else "Current Activity Intensity:", fontSize = 11.sp, color = Color.Gray)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val levels = listOf(
                        1.2f to (if (isBengali) "বসা কাজ" else "Sedentary"),
                        1.375f to (if (isBengali) "হালকা" else "Light"),
                        1.55f to (if (isBengali) "মাঝারি" else "Active"),
                        1.725f to (if (isBengali) "কঠোর" else "Heavy")
                    )

                    levels.forEach { (factor, label) ->
                        OutlinedButton(
                            onClick = { activeLevel = factor },
                            border = BorderStroke(1.dp, if (activeLevel == factor) Color(0xFF2E7D32) else Color(0xFFE2E8F0)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (activeLevel == factor) Color(0xFFE8F5E9) else Color.Transparent
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (activeLevel == factor) Color(0xFF1B5E20) else Color.Gray)
                        }
                    }
                }
            }
        }

        // BMI display card
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(if (isBengali) "বডি মাস ইনডেক্স (BMI)" else "Body Mass Index", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Gray)
                        Text(String.format(Locale.ROOT, "%.2f", bmi), fontWeight = FontWeight.Black, fontSize = 28.sp, color = bmiColor)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(bmiColor)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (bmi < 18.5) (if (isBengali) "স্বল্প ওজন" else "Underweight")
                                   else if (bmi < 25.0) (if (isBengali) "স্বাভাবিক" else "Healthy")
                                   else if (bmi < 30.0) (if (isBengali) "ওভারওয়েট" else "Overweight")
                                   else (if (isBengali) "স্থূল শরীর" else "Obese"),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(bmiCategory, fontSize = 12.sp, lineHeight = 16.sp, color = Color.DarkGray)
            }
        }

        // Ideal Target Weights
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (isBengali) "উচ্চতা ভিত্তিক আদর্শ লক্ষ্য ওজন" else "Optimal Ideal Weight Range",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color.DarkGray
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(if (isBengali) "সর্বনিম্ন ওজন সীমা" else "Min Bound (18.5 BMI)", fontSize = 10.sp, color = Color.Gray)
                        Text(String.format(Locale.ROOT, "%.1f kg", idealWeightMin), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1B5E20))
                    }
                    Text("🔄", fontSize = 20.sp, modifier = Modifier.align(Alignment.CenterVertically))
                    Column(horizontalAlignment = Alignment.End) {
                        Text(if (isBengali) "সর্বোচ্চ ওজন সীমা" else "Max Bound (24.9 BMI)", fontSize = 10.sp, color = Color.Gray)
                        Text(String.format(Locale.ROOT, "%.1f kg", idealWeightMax), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1B5E20))
                    }
                }
            }
        }

        // Targets Calorie Allocation details
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = if (isBengali) "লক্ষ্য অর্জনের দৈনিক ক্যালোরি বাজেট" else "Tailored Target Energy Budgets",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color.DarkGray
                )

                val planCategories = remember(tdee, isBengali) {
                    listOf(
                        Triple(if (isBengali) "📉 ওজন হ্রাস প্ল্যান (Weight Loss)" else "Moderate Fat-Loss", String.format(Locale.ROOT, "%.0f kcal / Day", tdee - 450f), if (isBengali) "দৈনিক চাহিদার চেয়ে ৪৫০ ক্যালরি কমিয়ে মেদ নিষ্কাশন।" else "Target safe deficit: releases fat safely over weeks."),
                        Triple(if (isBengali) "⚡ মেদহীন পেশী বাড়ানো (Healthy Lean Gain)" else "Healthy Muscle Gain", String.format(Locale.ROOT, "%.0f kcal / Day", tdee + 350f), if (isBengali) "নিরাপদ বাড়তি ৩৫০ ক্যালোরি দিয়ে চর্বিহীন হাড় ও মাসল বৃদ্ধি।" else "Adds lean support without unwanted abdominal lipid deposits."),
                        Triple(if (isBengali) "⚖️ ওজন ধরে রাখার বাজেট (Maintenance)" else "Weight Maintenance", String.format(Locale.ROOT, "%.0f kcal / Day", tdee), if (isBengali) "শারীরিক তাপমাত্রা ও মেটাবলিজম সমান পর্যায়ে বজায় রাখতে।" else "Balanced consumption for sustaining metabolic baseline.")
                    )
                }

                planCategories.forEach { (theme, valString, infoText) ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFBFBFB))
                            .padding(8.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(theme, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF2E7D32))
                            Text(valString, fontWeight = FontWeight.Black, fontSize = 12.sp, color = Color(0xFF00796B))
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(infoText, fontSize = 10.sp, color = Color.Gray, lineHeight = 14.sp)
                    }
                }
            }
        }
    }
}


// ============================================================================
// 13. WORKING MOBILE APP RATINGS & USER REVIEWS ADAPTOR MODULE
// ============================================================================
data class LiveAppReview(
    val id: Int,
    val author: String,
    val stars: Int,
    val reviewContent: String,
    val dateString: String,
    val tagSelected: String? = null
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppRatingsToolScreen(isBengali: Boolean) {
    // Shared live list of reviews. Adds real-time interactivity.
    val customReviews = remember {
        mutableStateListOf(
            LiveAppReview(1, "Samiul Alam (Dhaka)", 5, if (isBengali) "অসাধারণ একটি হেলথ প্লাস অ্যাপ্লিকেশন! ৭ দিনে ১.৫ কেজি স্বাস্থ্যকর ওজন কমিয়েছি। থ্যাংক ইউ ANEXSOPZ।" else "Outstanding calorie manager! I managed to drop 1.5kg of stubborn fat in mere days. Deeply synchronized tool.", "2026-06-12", "Highly Recommended"),
            LiveAppReview(2, "Mehnaz Chowdhury", 5, if (isBengali) "বারকোড স্ক্যানিং ফিচারটি সুস্বাদু খাবারের পুষ্টি বের করে দিতে দারুণ কাজ করে।" else "The barcodes scanner extracts pack nutrition indices instantly inside any grocery shop! Superb UI.", "2026-06-15", "Accurate Diet"),
            LiveAppReview(3, "Niloy Roy", 4, if (isBengali) "পানি পানের গ্লাস কাউন্টার প্রোগ্রেস খুব চমৎকার লেগেছে। নোটিফিকেশনগুলো অনেক হেল্প করে।" else "The water intake glasses counter works beautifully. Custom push alarms saved my consistency.", "2026-06-16", "Great UI")
        )
    }

    var chosenRating by remember { mutableStateOf(5) }
    var inputAuthor by remember { mutableStateOf("") }
    var inputFeedback by remember { mutableStateOf("") }
    var chosenTag by remember { mutableStateOf("Easy to use") }

    val tagsList = listOf("Easy to use", "Accurate Diet", "Highly Recommended", "Great UI", "Excellent Reminders")

    // Calculations based on live list
    val averageRating = remember(customReviews.size) {
        if (customReviews.isEmpty()) 5.0 else customReviews.sumOf { it.stars }.toDouble() / customReviews.size
    }

    val totalCount = customReviews.size

    val starCounts = remember(customReviews.size) {
        val counts = IntArray(6) { 0 }
        customReviews.forEach { counts[it.stars]++ }
        counts
    }

    val scaffoldContext = androidx.compose.ui.platform.LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(Color(0xFFFFFCFC))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Stats Display Header
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFFFEBEE)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(4f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = String.format(Locale.ROOT, "%.1f", averageRating),
                        fontWeight = FontWeight.Black,
                        fontSize = 44.sp,
                        color = Color(0xFFC62828)
                    )
                    Text(
                        text = if (isBengali) "৫ স্টারের মধ্যে" else "out of 5 stars",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.Center) {
                        for (j in 1..5) {
                            Icon(
                                imageVector = if (j <= averageRating.toInt()) Icons.Default.Star else Icons.Default.StarOutline,
                                contentDescription = null,
                                tint = Color(0xFFFFB300),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isBengali) "($totalCount টি মোট রিভিউ)" else "($totalCount community reviews)",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Gray
                    )
                }

                // Vertical Divider
                Box(
                    modifier = Modifier
                        .height(84.dp)
                        .width(1.dp)
                        .background(Color(0xFFEEEEEE))
                        .padding(horizontal = 8.dp)
                )

                // Progress indicators of ratings
                Column(modifier = Modifier.weight(6f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (star in 5 downTo 1) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text("$star ★", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.width(22.dp))
                            val pct = if (totalCount == 0) 0f else starCounts[star].toFloat() / totalCount
                            LinearProgressIndicator(
                                progress = { pct },
                                trackColor = Color(0xFFF1F1F1),
                                color = Color(0xFFFFB300),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(5.dp)
                                    .clip(CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("${starCounts[star]}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray, modifier = Modifier.width(14.dp))
                        }
                    }
                }
            }
        }

        // Ratings Form Block
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFEEEEEE)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = if (isBengali) "আপনার রেটিং এবং মতামত জমা দিন" else "Write a Working Review",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color.DarkGray
                )

                // Select star ratings
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
                ) {
                    for (i in 1..5) {
                        IconButton(
                            onClick = { chosenRating = i },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (i <= chosenRating) Icons.Default.Star else Icons.Default.StarOutline,
                                contentDescription = "$i Stars",
                                tint = Color(0xFFFFB300),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = inputAuthor,
                    onValueChange = { inputAuthor = it },
                    placeholder = { Text(if (isBengali) "আপনার নাম (যেমন: তাহসিন ঢাকা)" else "Your Name", fontSize = 12.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFD32F2F)),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = inputFeedback,
                    onValueChange = { inputFeedback = it },
                    placeholder = { Text(if (isBengali) "আপনার মূল্যবান কাজের অভিজ্ঞতাটি লিখুন..." else "Provide healthy app suggestions or details...", fontSize = 12.sp) },
                    maxLines = 3,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFD32F2F)),
                    modifier = Modifier.fillMaxWidth()
                )

                // Tag Chips
                Text(if (isBengali) "একটি রিভিউ ট্যাগ নির্বাচন করুন:" else "Choose review tag:", fontSize = 11.sp, color = Color.Gray)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    tagsList.forEach { tag ->
                        val isSelected = chosenTag == tag
                        SuggestionChip(
                            onClick = { chosenTag = tag },
                            label = { Text(tag, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = if (isSelected) Color(0xFFFFEBEE) else Color.Transparent,
                                labelColor = if (isSelected) Color(0xFF1E5E2F) else Color.Gray
                            )
                        )
                    }
                }

                Button(
                    onClick = {
                        if (inputAuthor.trim().isNotBlank() && inputFeedback.trim().isNotBlank()) {
                            val newIdx = customReviews.size + 1
                            val dString = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date())
                            customReviews.add(
                                0,
                                LiveAppReview(newIdx, inputAuthor.trim(), chosenRating, inputFeedback.trim(), dString, chosenTag)
                            )
                            inputAuthor = ""
                            inputFeedback = ""
                            android.widget.Toast.makeText(
                                scaffoldContext,
                                if (isBengali) "মতার্মতের জন্য ধন্যবাদ! আপনার রিভিউটি সফলভাবে পাবলিশ হয়েছে।" else "Review successfully posted locally to community pool!",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            android.widget.Toast.makeText(
                                scaffoldContext,
                                if (isBengali) "সব বিবরণ ও নাম পূরণ করুন!" else "Please supply name and comments text!",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isBengali) "কমিউনিটি পুলে পোস্ট করুন" else "Publish Review Safely", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Past list of feedbacks
        Text(
            text = if (isBengali) "কমিউনিটির সম্প্রতিক রিভিউসমূহ:" else "Recent Certified Community Feedbacks:",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray,
            modifier = Modifier.padding(top = 4.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(1),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.height(280.dp)
        ) {
            items(customReviews.size) { idx ->
                val rev = customReviews[idx]
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFF1F1F1)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(rev.author, fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = Color(0xFF212121))
                            Text(rev.dateString, fontSize = 9.sp, color = Color.Gray)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row {
                                for (starIdx in 1..5) {
                                    Icon(
                                        imageVector = if (starIdx <= rev.stars) Icons.Default.Star else Icons.Default.StarOutline,
                                        contentDescription = null,
                                        tint = Color(0xFFFFB300),
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                            if (rev.tagSelected != null) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFFE0F2F1))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(rev.tagSelected, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFF004D40))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(rev.reviewContent, fontSize = 11.5.sp, lineHeight = 16.sp, color = Color.DarkGray)
                    }
                }
            }
        }
    }
}

// ============================================================================================================
// 14. EMOTION LAYER & LIFESTYLE DIET PLANNER: MOOD JOURNAL SCREEN
// ============================================================================================================
@Composable
fun MoodLifestylePlannerScreen(viewModel: DietPlannerViewModel, isBengali: Boolean) {
    var selectedMood by remember { mutableStateOf("Happy") }
    var textEntry by remember { mutableStateOf("") }
    var sleepHours by remember { mutableFloatStateOf(7.5f) }
    var hydrationGlasses by remember { mutableIntStateOf(5) }
    var isAnalyzed by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }

    // Hardcoded initial list of mood log items (acting as local memory)
    val moodHistory = remember {
        mutableStateListOf(
            MoodLogItem("2026-06-15", "Tired", "Worked double shifts today, feeling low battery", "Green Honey Tea & Nuts", "10-minute deep breathing & short walk"),
            MoodLogItem("2026-06-14", "Stressed", "Heavy traffic in Dhaka banani, intense work pressure", "Bananas & Camomile Infusion", "4-7-8 Breathing & listen to calming sitar music"),
            MoodLogItem("2026-06-13", "Happy", "Achieved weekly calorie goals easily, felt highly energised", "Mixed Fruit Oats & Sesame", "Light outdoor jogging & call a close friend")
        )
    }

    val moodSuggestion = remember(selectedMood) {
        when (selectedMood) {
            "Sad" -> Pair(
                if (isBengali) "কোকো চকোলেট স্মুদি এবং ডার্ক চকোলেট" else "Cocoa Chocolate Smoothie & Dark Chocolate",
                if (isBengali) "আপনার পছন্দের কোনো গান বা রিল্যাক্সিং মেলোডি শুনুন" else "Listen to your favorite relaxing acoustic melody"
            )
            "Tired" -> Pair(
                if (isBengali) "লেবু ও আদা যুক্ত তাজা মসলা গ্রিন টি" else "Fresh Ginger Lemon Green Tea & Almonds",
                if (isBengali) "১০ মিনিটের জন্য বাইরে মুক্ত বাতাসে হেঁটে আসুন" else "Take a refreshing 10-minute oxygen walk outdoors"
            )
            "Stressed" -> Pair(
                if (isBengali) "কলা, ওটস মিল্ক এবং সামান্য মৌরি" else "Ripe Bananas, Oat milk, and Fennel infusion",
                if (isBengali) "৪-৭-৮ রিল্যাক্সিং নিঃশ্বাসের ব্যায়াম সম্পন্ন করুন" else "Complete the 4-7-8 breathing relaxation cycle"
            )
            "Angry" -> Pair(
                if (isBengali) "পুদিনা পাতা ও বরফ মিশ্রিত শসার ঠান্ডা পানি" else "Chilled Cucumber Mint Activated Water",
                if (isBengali) "একটি কাগজে আপনার মনের রাগ লিখে তা ছিঁড়ে ফেলুন" else "Scribble down your thoughts in a page to release tension"
            )
            else -> Pair(
                if (isBengali) "তাজা মিক্সড ফ্রুট সালাদ ও ফ্লেক্সসিড" else "Fresh Colorful Fruit Salad with Flaxseeds",
                if (isBengali) "এই এনার্জি বজায় রেখে একটি হালকা ব্যায়াম করুন!" else "Channel this energy into a productive light home workout!"
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // AI Emotion Tracker Header Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (isBengali) "🧠 আপনার বর্তমান অনুভূতি কেমন?" else "🧠 How Are You Feeling Right Now?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isBengali) "আবেগ ও অনুভূতি নির্বাচন করুন, শপওয়ে স্মার্ট এআই অনুভূতির ওপর ভিত্তি করে আপনার জন্য সেরা খাবার ও লাইফস্টাইল অ্যাক্টিভিটি ডিজাইন করবে!"
                           else "State your current mood, we analyze it instantly to match mood-boosting food combinations and mindful tasks.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Emoji selection row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val emojis = listOf(
                Triple("Happy", "😊", Color(0xFFE8F5E9)),
                Triple("Sad", "😢", Color(0xFFE1F5FE)),
                Triple("Tired", "🥱", Color(0xFFFFFDE7)),
                Triple("Stressed", "😰", Color(0xFFFFF3E0)),
                Triple("Angry", "😡", Color(0xFFFFEBEE))
            )
            emojis.forEach { (moodName, emoji, color) ->
                val isSelected = selectedMood == moodName
                Card(
                    modifier = Modifier
                        .size(60.dp)
                        .clickable {
                            selectedMood = moodName
                            isAnalyzed = false
                        },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary else color
                    ),
                    border = if (isSelected) BorderStroke(2.dp, Color.White) else null
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(emoji, fontSize = 24.sp)
                            Text(
                                text = if (isBengali) {
                                    when (moodName) {
                                        "Happy" -> "খুশি"
                                        "Sad" -> "দুঃখিত"
                                        "Tired" -> "ক্লান্ত"
                                        "Stressed" -> "চিন্তিত"
                                        else -> "ক্ষুব্ধ"
                                    }
                                } else moodName,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else Color.Black
                            )
                        }
                    }
                }
            }
        }

        // Text input journal entry
        OutlinedTextField(
            value = textEntry,
            onValueChange = { textEntry = it },
            label = { Text(if (isBengali) "আপনার চিন্তা বা ডায়েরি লিখুন..." else "Type what is on your mind...") },
            modifier = Modifier.fillMaxWidth().height(100.dp),
            shape = RoundedCornerShape(12.dp),
            placeholder = { Text(if (isBengali) "আজকের কাজের চাপ কেমন ছিল? অথবা কণ্ঠস্বর..." else "Stress levels, achievements, how did you sleep...") }
        )

        // Slide meters for health (Dual metrics)
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FBE7)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = if (isBengali) "💤 ঘুমের পরিমান এবং পানি পানের মাত্রা" else "💤 Sleep Duration & Water Metrics",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color(0xFF558B2F)
                )
                Spacer(modifier = Modifier.height(10.dp))
                
                // Sleep Slider
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = if (isBengali) "ঘুম: ${String.format("%.1f", sleepHours)} ঘণ্টা" else "Sleep: ${String.format("%.1f", sleepHours)} Hours", fontSize = 11.5.sp, modifier = Modifier.width(95.dp), fontWeight = FontWeight.SemiBold)
                    Slider(
                        value = sleepHours,
                        onValueChange = { sleepHours = it },
                        valueRange = 4f..10f,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Water log glasses counter
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Text(text = if (isBengali) "পানি পান: ${hydrationGlasses} গ্লাস" else "Water Intake: ${hydrationGlasses} Glasses", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = { if (hydrationGlasses > 0) hydrationGlasses-- },
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.size(32.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF81C784))
                        ) {
                            Text("-", fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { hydrationGlasses++ },
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.size(32.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF81C784))
                        ) {
                            Text("+", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Action trigger: AI analysis
        Button(
            onClick = {
                isAnalyzed = true
                // Prepend to history logs instantly
                val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                moodHistory.add(0, MoodLogItem(
                    date = todayDate,
                    mood = selectedMood,
                    note = textEntry.ifEmpty { if (isBengali) "দৈনিক সুস্থতা ট্র্যাকিং সেশন" else "Daily wellness mood check" },
                    food = moodSuggestion.first,
                    activity = moodSuggestion.second
                ))
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = if (isBengali) "এআই ইমোশন অ্যানালাইসিস চালান" else "Run AI Emotion Analysis", fontWeight = FontWeight.Bold)
            }
        }

        // Animated results card
        if (isAnalyzed) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFAF0)),
                border = BorderStroke(1.5.dp, Color(0xFFFFB300))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("✨", fontSize = 24.sp)
                        Text(
                            text = if (isBengali) "ANEXSOPZ স্মার্ট সাজেশনস" else "ANEXSOPZ Intelligent Suggestion",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFFE65100)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = if (isBengali) "শনাক্তকৃত আবেগ: ${selectedMood}" else "Detected Emotion: ${selectedMood}",
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("🥗", fontSize = 20.sp)
                        Column {
                            Text(text = if (isBengali) "প্রস্তাবিত খাবার:" else "Mood Boosting Recipe / Food:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Black)
                            Text(text = moodSuggestion.first, fontSize = 11.5.sp, color = Color.DarkGray)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("🧘‍♂️", fontSize = 20.sp)
                        Column {
                            Text(text = if (isBengali) "প্রস্তাবিত ক্রিয়াকলাপ:" else "Activity suggestion:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Black)
                            Text(text = moodSuggestion.second, fontSize = 11.5.sp, color = Color.DarkGray)
                        }
                    }
                }
            }
        }

        // Trend Canvas Graph (Draw mood waves)
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFEEEEEE)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isBengali) "📈 weekly আবেগ ও মুড ট্রেন্ড" else "📈 Weekly Emotion & Mood Trends",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.Black
                    )
                    Text(
                        text = if (isBengali) "রপ্তানি (Export)" else "Export PDF",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { showExportDialog = true }
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                // Draw simple line segments representing Mon-Sun mood ratings
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                ) {
                    val width = size.width
                    val height = size.height
                    val points = listOf(
                        androidx.compose.ui.geometry.Offset(width * 0.05f, height * 0.4f), // Mon - Happy
                        androidx.compose.ui.geometry.Offset(width * 0.20f, height * 0.7f), // Tue - Tired
                        androidx.compose.ui.geometry.Offset(width * 0.35f, height * 0.8f), // Wed - Stressed
                        androidx.compose.ui.geometry.Offset(width * 0.50f, height * 0.3f), // Thu - Happy
                        androidx.compose.ui.geometry.Offset(width * 0.65f, height * 0.5f), // Fri - Fine
                        androidx.compose.ui.geometry.Offset(width * 0.80f, height * 0.9f), // Sat - Angry
                        androidx.compose.ui.geometry.Offset(width * 0.95f, height * 0.2f)  // Sun - Energetic
                    )

                    // Draw grid helper lines
                    for (i in 1..4) {
                        val yPos = (height / 5f) * i
                        drawLine(
                            color = Color(0xFFF3F3F3),
                            start = androidx.compose.ui.geometry.Offset(0f, yPos),
                            end = androidx.compose.ui.geometry.Offset(width, yPos),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    // Draw the connection path
                    for (i in 0 until points.size - 1) {
                        drawLine(
                            color = Color(0xFF4CAF50),
                            start = points[i],
                            end = points[i+1],
                            strokeWidth = 3.dp.toPx()
                        )
                    }

                    // Draw small dot markers
                    points.forEach { point ->
                        drawCircle(
                            color = Color(0xFF1B5E20),
                            radius = 4.dp.toPx(),
                            center = point
                        )
                    }
                }
                
                // Days and sentiment labels
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                    days.forEach { day ->
                        Text(day, fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Historial logs
        Text(
            text = if (isBengali) "📜 পূর্ববর্তী আবেগের ডায়েরি হিস্ট্রি" else "📜 Emotional Journal History",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Color.Black
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(1),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.height(280.dp)
        ) {
            items(count = moodHistory.size) { index: Int ->
                val logItem = moodHistory[index]
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFEEEEEE)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                        val moodEmoji = when (logItem.mood) {
                            "Happy" -> "😊"
                            "Sad" -> "😢"
                            "Tired" -> "🥱"
                            "Stressed" -> "😰"
                            else -> "😡"
                        }
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF1F8E9)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(moodEmoji, fontSize = 20.sp)
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(logItem.date, fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                Text(
                                    text = if (isBengali) {
                                        when (logItem.mood) {
                                            "Happy" -> "খুশি"
                                            "Sad" -> "হতাশ"
                                            "Tired" -> "ক্লান্ত"
                                            "Stressed" -> "চিন্তিত"
                                            else -> "ক্ষুব্ধ"
                                        }
                                    } else logItem.mood, 
                                    fontSize = 10.sp, 
                                    fontWeight = FontWeight.Black, 
                                    color = Color(0xFF1B5E20)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(logItem.note, fontSize = 11.5.sp, color = Color.Black, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "🥗 Food: ${logItem.food} | 🧘‍♂️ Mind: ${logItem.activity}", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text(if (isBengali) "ডায়েরি এক্সপোর্ট সফল" else "Report Exported Success") },
            text = { Text(if (isBengali) "আপনার আবেগ ডায়েরি এবং সুষম খাবারের সাপ্তাহিক হিস্ট্রি 'ANEXSOPZ_mood_and_diet_history.pdf' নামে সফলভাবে রপ্তানি করা হয়েছে!" else "Weekly emotional states combined with your calorie logs successfully saved as 'ANEXSOPZ_mood_and_diet_history.pdf' under downloads directory.") },
            confirmButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}

data class MoodLogItem(
    val date: String,
    val mood: String,
    val note: String,
    val food: String,
    val activity: String
)

// ============================================================================================================
// 15. NEARBY RESTAURANTS & FOOD DELIVERY ORDER INTEGRATION SMART SCREEN
// ============================================================================================================
@Composable
fun NearbyRestaurantsScreen(viewModel: DietPlannerViewModel, isBengali: Boolean, userProfile: UserProfileEntity) {
    var selectedRestaurant by remember { mutableStateOf<HealthRestaurant?>(null) }
    var selectedOrderPlatform by remember { mutableStateOf("Foodpanda") }
    var paymentMethod by remember { mutableStateOf("bKash") }
    var isProcessingPayment by remember { mutableStateOf(false) }
    var paymentCompleted by remember { mutableStateOf(false) }

    val suggestedKetoFood = remember(userProfile.goal) {
        if (userProfile.goal.lowercase(Locale.ROOT).contains("loss") || userProfile.goal.lowercase(Locale.ROOT).contains("কমানো")) {
            Triple(
                if (isBengali) "১০০% সুষম ভেজিটেবল অ্যাভোকাডো সালাদ" else "100% Balanced Avocado Green Salad",
                if (isBengali) "২৫০ kcal" else "250 kcal calorie allotment",
                320
            )
        } else {
            Triple(
                if (isBengali) "হাই-প্রোটিন চিকেন গ্রিলড বাটার ও রাইস বোল" else "High-Protein Chicken Grilled Butter & Brown Rice Bowl",
                if (isBengali) "৫৮০ kcal" else "580 kcal active muscle diet",
                450
            )
        }
    }

    val simulatedMapRestaurants = remember {
        listOf(
            HealthRestaurant("Dhaka Healthy Kitchen", "4.8 ⭐", "0.8 km away", "+880 1711-223344", "Banani Block-D, Dhaka", "Bananas, Avocado salads, Green keto soups"),
            HealthRestaurant("Guilt-Free Bites Cafe", "4.6 ⭐", "1.5 km away", "+880 1822-445566", "Gulshan-2 Circle, Dhaka", "Organic protein bowls, Almond milk tea"),
            HealthRestaurant("Banani Green Leaf Diner", "4.9 ⭐", "2.1 km away", "+880 1611-778899", "Rabindra Swarani, Uttara", "Keto fish fillet, sugar-free desserts"),
            HealthRestaurant("Organica Bistro BD", "4.5 ⭐", "3.4 km away", "+880 1933-112233", "Dhanmondi Lake-side road", "Healthy quinoa bowls, steamed vegetables")
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Suggested Food Frame
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
            border = BorderStroke(1.5.dp, Color(0xFF2E7D32))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (isBengali) "🎯 আপনার জন্য আজকের গোল-ভিত্তিক প্রস্তাবিত খাদ্য" else "🎯 Recommended Meal Spot Based on Your Goal",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color(0xFF1B5E20)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = suggestedKetoFood.first,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    color = Color.Black
                )
                Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "🔥 ${suggestedKetoFood.second}", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Text(text = "Price: ${suggestedKetoFood.third} BDT", fontSize = 12.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Black)
                }
            }
        }

        // Simulated Google Maps Layout Frame
        Text(
            text = if (isBengali) "📍 গুগল ম্যাপস - নিকটস্থ স্বাস্থ্যকর রেস্টুরেন্ট" else "📍 live Location Maps - Nearby Restaurants",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Color.Black
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFECEFF1)),
            modifier = Modifier.fillMaxWidth().height(180.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Background simulated grid
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    // Draw soft river
                    val riverPath = android.graphics.Path().apply {
                        moveTo(0f, h * 0.7f)
                        cubicTo(w * 0.3f, h * 0.5f, w * 0.6f, h * 0.9f, w, h * 0.6f)
                    }
                    drawPath(riverPath.asComposePath(), Color(0xFFB3E5FC), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 24.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round))
                    
                    // Draw simple roads
                    drawLine(Color.White, androidx.compose.ui.geometry.Offset(w * 0.25f, 0f), androidx.compose.ui.geometry.Offset(w * 0.25f, h), strokeWidth = 8.dp.toPx())
                    drawLine(Color.White, androidx.compose.ui.geometry.Offset(0f, h * 0.35f), androidx.compose.ui.geometry.Offset(w, h * 0.35f), strokeWidth = 8.dp.toPx())
                }

                // Center user pulse dot
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2196F3).copy(alpha = 0.4f))
                        .align(Alignment.Center),
                    contentAlignment = Alignment.Center
                ) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF0D47A1)))
                }

                // Layout simulated restaurants locations on maps
                simulatedMapRestaurants.forEachIndexed { index, rest ->
                    val alignment = when (index) {
                        0 -> Alignment.TopStart
                        1 -> Alignment.TopEnd
                        2 -> Alignment.BottomStart
                        else -> Alignment.BottomEnd
                    }
                    val isSelected = selectedRestaurant?.name == rest.name
                    Box(
                        modifier = Modifier
                            .align(alignment)
                            .padding(14.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) Color(0xFFC62828) else Color(0xFF2E7D32))
                            .clickable { selectedRestaurant = rest }
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("📍", fontSize = 10.sp)
                            Text(rest.name.take(13) + "..", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                        }
                    }
                }
            }
        }

        // Selected restaurant info pane & Ordering Sheet
        if (selectedRestaurant != null) {
            val rest = selectedRestaurant!!
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.5.dp, Color(0xFF2E7D32)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(rest.name, fontWeight = FontWeight.Black, fontSize = 15.sp, color = Color(0xFF1B5E20))
                        Text(rest.rating, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFFFB300))
                    }
                    Text("📍 Address: ${rest.address} (${rest.distance})", fontSize = 11.sp, color = Color.DarkGray)
                    Text("📞 Contact: ${rest.contactPhone}", fontSize = 11.sp, color = Color.DarkGray)
                    Text("🍲 Specialty Diet: ${rest.specialtyList}", fontSize = 11.sp, color = Color(0xFF388E3C), fontWeight = FontWeight.SemiBold)

                    Divider(color = Color(0xFFEEEEEE), modifier = Modifier.padding(vertical = 4.dp))

                    Text(if (isBengali) "🏍️ ফুড ডেলিভারি ক্যারিয়ার বেছে নিন:" else "🏍️ Select Delivery Partner API:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Black)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val deliveryPlatforms = listOf("Foodpanda", "Pathao Food", "Shohoz")
                        deliveryPlatforms.forEach { platform ->
                            val isSelPlatform = selectedOrderPlatform == platform
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelPlatform) MaterialTheme.colorScheme.primary else Color(0xFFF3F4F9))
                                    .clickable { selectedOrderPlatform = platform }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = platform,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelPlatform) Color.White else Color.Black
                                )
                            }
                        }
                    }

                    // Payment Gateway Row
                    Text(if (isBengali) "💸 পেমেন্ট মেথড বাছাই করুন:" else "💸 Select Payment Mode Gateway:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Black)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val platforms = listOf("bKash", "Nagad", "Debit Card")
                        platforms.forEach { gateway ->
                            val isSelGateway = paymentMethod == gateway
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelGateway) Color(0xFFC2185B) else Color(0xFFF3F4F9))
                                    .clickable { paymentMethod = gateway }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = gateway,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelGateway) Color.White else Color.Black
                                )
                            }
                        }
                    }

                    // Simulated pricing calculation
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Subtotal: ${suggestedKetoFood.third} BDT", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                        Text("Delivery Fee: 45 BDT", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                        Text(text = "Total BDT: ${suggestedKetoFood.third + 45} BDT", fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.Black)
                    }

                    // Place order button
                    Button(
                        onClick = {
                            isProcessingPayment = true
                            paymentCompleted = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(text = if (isBengali) "অর্ডার সম্পন্ন করুন ও পেমেন্ট দিন" else "Place Order & Pay via $paymentMethod", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFEEEEEE)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("🍽️", fontSize = 28.sp)
                        Text(if (isBengali) "দয়া করে উপরের গুগলে ম্যাপে যেকোনো রেস্টুরেন্টে ট্যাপ করুন" else "Please tap any active restaurant inside the Google map above to initiate delivery checkout", fontSize = 11.5.sp, textAlign = TextAlign.Center, color = Color.Gray)
                    }
                }
            }
        }
    }

    // Processing payment indicator dialog
    if (isProcessingPayment) {
        AlertDialog(
            onDismissRequest = {},
            title = null,
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    var simulatedStep by remember { mutableIntStateOf(0) }
                    LaunchedEffect(Unit) {
                        delay(1200)
                        simulatedStep = 1
                        delay(1200)
                        simulatedStep = 2
                        delay(1200)
                        isProcessingPayment = false
                        paymentCompleted = true
                    }
                    CircularProgressIndicator(color = Color(0xFFC2185B))
                    Text(
                        text = when (simulatedStep) {
                            0 -> if (isBengali) "পেমেন্ট গেটওয়েতে সংযোগ করা হচ্ছে..." else "Connecting secure pay via $paymentMethod..."
                            1 -> if (isBengali) "বাংলাদেশ ব্যাংক সিকিউরিটি যাচাই করা হচ্ছে..." else "Authorizing API with $selectedOrderPlatform delivery route..."
                            else -> if (isBengali) "ডেলিভারি বুকিং সম্পন্ন হচ্ছে..." else "Finalizing OTP transaction approvals..."
                        },
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.5.sp,
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {}
        )
    }

    // Payment Completed alert dialog
    if (paymentCompleted) {
        AlertDialog(
            onDismissRequest = { paymentCompleted = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🎉")
                    Text(if (isBengali) "অর্ডার সফলভাবে সম্পন্ন!" else "Order Successful!")
                }
            },
            text = {
                Text(
                    text = if (isBengali) {
                        "আপনার খাবার '${suggestedKetoFood.first}' সফলভাবে অর্ডার দেওয়া হয়েছে। $selectedOrderPlatform রাইডার মেহরাব হোসেন ১৮ মিনিটের মধ্যে আপনার দরজায় খাবারটি পৌঁছে দেবেন!"
                    } else {
                        "Your goal-centric delicious diet '${suggestedKetoFood.first}' has been scheduled for instant delivery. $selectedOrderPlatform rider has picked it up and will arrive at your destination in 18 minutes!"
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = { paymentCompleted = false }) {
                    Text("Great!")
                }
            }
        )
    }
}

data class HealthRestaurant(
    val name: String,
    val rating: String,
    val distance: String,
    val contactPhone: String,
    val address: String,
    val specialtyList: String
)

// ============================================================================================================
// 16. COMMUNITY FEED & RECIPE EXCHANGE SECTOR (GAMIFICATION WELLNESS)
// ============================================================================================================
@Composable
fun CommunityRecipeExchangeScreen(viewModel: DietPlannerViewModel, isBengali: Boolean) {
    var newRecipeTitle by remember { mutableStateOf("") }
    var newRecipeIngredients by remember { mutableStateOf("") }
    var newRecipeSteps by remember { mutableStateOf("") }
    var categorySelection by remember { mutableStateOf("Breakfast") }
    var inputCommentText by remember { mutableStateOf("") }
    var commentActivePostId by remember { mutableIntStateOf(-1) }

    // Unified client state feed
    val feedPosts = remember {
        mutableStateListOf(
            CommunityPost(1, "Imtiaz Sharif", "🔥 Calorie Champion", "Sad ➡️ Chocolate Cocoa Smoothie is a direct hack to release dopamines! Highly recommended to log inside your ANEXSOPZ mood layer.", 42, mutableStateListOf("Exactly! Tried it and loved it.", "Does it have processed sugars?")),
            CommunityPost(2, "Mehedi Hasan", "💎 Hydration Master", "Drinking 10+ glasses today felt amazing. Highly hydrated. Weight goals are also moving down properly in my tracker progress.", 18, mutableStateListOf("Great consistency!", "Inspirational")),
            CommunityPost(3, "Farhana Chow", "🥗 Recipe Star", "Steamed oats bowl + raw honey makes a perfect breakfast to balance blood sugars.", 56, mutableStateListOf("Simple yet effective.", "Adding this as high diet plan today."))
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Gamified weekly badge cards
        Text(
            text = if (isBengali) "🏆 আপনার অর্জিত হেলদি ব্যাজ সমূহ" else "🏆 Your Earned Healthy Badges",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Color.Black
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val badges = listOf(
                Pair("🥇", if (isBengali) "হাইড্রেশন মাস্টার" else "Water Champ"),
                Pair("🥬", if (isBengali) "সবুজ থালা" else "Green Plate"),
                Pair("🏃‍♂️", if (isBengali) "ক্যালোরি বার্নার" else "Burn Hero"),
                Pair("🤝", if (isBengali) "ভলান্টিয়ার স্টার" else "Helper Star")
            )
            badges.forEach { (avatar, title) ->
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FBE7)),
                    border = BorderStroke(1.dp, Color(0xFFD4E157))
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(avatar, fontSize = 24.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(title, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = Color(0xFF33691E))
                    }
                }
            }
        }

        Divider(color = Color(0xFFEEEEEE))

        // Create Custom Recipe Swaps Section
        Text(
            text = if (isBengali) "🍲 খাবার বা নতুন রেসিপি বিনিময় করুন" else "🍲 Submit Custom Recipe Swap",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Color.Black
        )

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE0E0E0))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = newRecipeTitle,
                    onValueChange = { newRecipeTitle = it },
                    label = { Text(if (isBengali) "রেসিপি বা খাবারের নাম..." else "Recipe dish name...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = newRecipeIngredients,
                    onValueChange = { newRecipeIngredients = it },
                    label = { Text(if (isBengali) "উপকরণ সূচী (কমা দিয়ে লিখুন)..." else "Ingredients list...") },
                    modifier = Modifier.fillMaxWidth().height(80.dp),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = newRecipeSteps,
                    onValueChange = { newRecipeSteps = it },
                    label = { Text(if (isBengali) "রান্নার প্রস্তুত প্রণালী বা নির্দেশাবলী..." else "Steps or preparing instruction...") },
                    modifier = Modifier.fillMaxWidth().height(80.dp),
                    shape = RoundedCornerShape(8.dp)
                )

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Category: ", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                    val categoryOptions = listOf("Breakfast", "Lunch", "Dinner", "Snack")
                    categoryOptions.forEach { opt ->
                        val isSelected = categorySelection == opt
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFFF3F4F9))
                                .clickable { categorySelection = opt }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(opt, fontSize = 10.sp, color = if (isSelected) Color.White else Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Button(
                    onClick = {
                        if (newRecipeTitle.isNotEmpty()) {
                            // Add custom recipe post directly to top flow
                            feedPosts.add(0, CommunityPost(
                                id = feedPosts.size + 1,
                                author = "Me (You)",
                                badge = "🔰 Star Chef",
                                contentText = "Baked dynamic recipe: '$newRecipeTitle'. Ingredients: $newRecipeIngredients. Stepped details: $newRecipeSteps.",
                                likes = 1,
                                comments = mutableStateListOf()
                            ))
                            // Reset input
                            newRecipeTitle = ""
                            newRecipeIngredients = ""
                            newRecipeSteps = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = if (isBengali) "কমিউনিটি ফিডে রেসিপি পোস্ট করুন" else "Draft & Publish Recipe Swap", fontWeight = FontWeight.Bold)
                }
            }
        }

        Divider(color = Color(0xFFEEEEEE))

        // Community feed
        Text(
            text = if (isBengali) "📢 সাম্প্রতিক স্বাস্থ্যকর ফিড পোস্ট" else "📢 Live Health & Diet Feed Swap",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Color.Black
        )

        feedPosts.forEach { post ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFEEEEEE)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(post.author, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Black)
                            Text(post.badge, fontSize = 9.sp, color = Color(0xFF689F38), fontWeight = FontWeight.Bold)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFFFEBEE))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("❤️", fontSize = 9.sp)
                                Text("${post.likes} Likes", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color(0xFFC62828))
                            }
                        }
                    }

                    Text(post.contentText, fontSize = 12.sp, color = Color.DarkGray)

                    // Interaction bar
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = "❤️ Like Activity",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { post.likes++ }
                        )

                        Text(
                            text = "💬 Comment (${post.comments.size})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable {
                                commentActivePostId = if (commentActivePostId == post.id) -1 else post.id
                            }
                        )
                    }

                    // Expandable comments log
                    if (commentActivePostId == post.id) {
                        Divider(color = Color(0xFFF5F5F5))
                        for (comment in post.comments) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("👤", fontSize = 10.sp)
                                Text(comment, fontSize = 11.sp, color = Color.Gray)
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = inputCommentText,
                                onValueChange = { inputCommentText = it },
                                placeholder = { Text("Write comment...", fontSize = 10.sp) },
                                modifier = Modifier.weight(1f).height(46.dp),
                                shape = RoundedCornerShape(8.dp)
                            )
                            Button(
                                onClick = {
                                    if (inputCommentText.isNotEmpty()) {
                                        post.comments.add(inputCommentText)
                                        inputCommentText = ""
                                    }
                                },
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.height(46.dp)
                            ) {
                                Text("Send", fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

data class CommunityPost(
    val id: Int,
    val author: String,
    val badge: String,
    val contentText: String,
    var likes: Int,
    val comments: androidx.compose.runtime.snapshots.SnapshotStateList<String>
)

// ============================================================================================================
// 17. LOCAL EMERGENCY HELPER & SOS SAFETY MODE
// ============================================================================================================
@Composable
fun LocalEmergencyHelperScreen(viewModel: DietPlannerViewModel, isBengali: Boolean) {
    var isEmergencyRequested by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("All") }
    var activeChatHelper by remember { mutableStateOf<EmergencyVolunteer?>(null) }
    var chatInputText by remember { mutableStateOf("") }
    var scaleFactor by remember { mutableFloatStateOf(1f) }
    var isVerifiedFilter by remember { mutableStateOf(false) }

    // Breathing wellness exercise logic
    var breathingState by remember { mutableStateOf("Tap to start") }
    var breathingCyclesRemaining by remember { mutableIntStateOf(0) }
    var breathingCircleScale by remember { mutableFloatStateOf(1f) }

    val mockChatHistory = remember {
        mutableStateListOf(
            Pair("Volunteer", "On my way with standard medical supplies and first-aid toolkit! Please lie down and take slow breaths."),
            Pair("You", "Thank you, our entrance gated lock code is is #404. I am waiting.")
        )
    }

    // Animation loop for SOS Radar Pulse
    LaunchedEffect(isEmergencyRequested) {
        if (isEmergencyRequested) {
            while (true) {
                scaleFactor = 1.34f
                delay(800)
                scaleFactor = 0.95f
                delay(800)
            }
        }
    }

    // Breathing loop control
    LaunchedEffect(breathingCyclesRemaining) {
        if (breathingCyclesRemaining > 0) {
            // Stage 1: Inhale 4s
            breathingState = if (isBengali) "ফুসফুসে শ্বাস নিন... ৪ সেকেন্ড (Inhale)" else "Deep Inhale... 4s"
            breathingCircleScale = 1.6f
            delay(4000)

            // Stage 2: Hold 7s
            breathingState = if (isBengali) "শ্বাস আটকে রাখুন... ৭ সেকেন্ড (Hold)" else "Hold breath... 7s"
            delay(7000)

            // Stage 3: Exhale 8s
            breathingState = if (isBengali) "ধীরে ধীরে শ্বাস ছাড়ুন... ৮ সেকেন্ড (Exhale)" else "Slow Exhale... 8s"
            breathingCircleScale = 1.0f
            delay(8000)

            breathingCyclesRemaining--
            if (breathingCyclesRemaining == 0) {
                breathingState = if (isBengali) "ব্যায়াম সমাপ্ত। চমৎকার!" else "Breathing Done. Relaxed!"
            }
        }
    }

    val simulatedVolunteers = remember {
        listOf(
            EmergencyVolunteer("Imtiaz Sharif", "Blood Donor (O+)", "350 meters away", "+880 1711-001122", 4.9f, true, "Blood Donation"),
            EmergencyVolunteer("Dr. Shamsul Bari", "Physician (MBBS)", "540 meters away", "+880 1822-998877", 4.8f, true, "Medical First Aid"),
            EmergencyVolunteer("Mehedra Kabir", "First Aid Trainer", "820 meters away", "+880 1515-554433", 4.6f, false, "First Aid Trained"),
            EmergencyVolunteer("Tariqul Bashar", "Rescue Volunteer", "1.2 km away", "+880 1912-332211", 4.7f, true, "Fire Rescue"),
            EmergencyVolunteer("Sabbir Rahman", "Breakdown Helper", "2.1 km away", "+880 1616-667788", 4.4f, false, "Technical Breakdown")
        )
    }

    val displayedVolunteers = remember(selectedCategory, isVerifiedFilter) {
        simulatedVolunteers.filter { vol ->
            (selectedCategory == "All" || vol.category.contains(selectedCategory) || vol.bloodType.contains(selectedCategory)) &&
            (!isVerifiedFilter || vol.isVerified)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // RADAR RED ALERT SOS BUTTON
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
            border = BorderStroke(2.dp, Color(0xFFEF5350)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (isBengali) "🚨 ওয়ান-ক্লিক জরুরী সাহায্য অনুরোধ" else "🚨 One-Click Emergency Helper SOS",
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    color = Color(0xFFC62828)
                )

                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(
                            Color(0xFFEF5350).copy(alpha = if (isEmergencyRequested) 0.3f else 0.1f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val animateSize = if (isEmergencyRequested) 84.dp * scaleFactor else 84.dp
                    Box(
                        modifier = Modifier
                            .size(animateSize)
                            .clip(CircleShape)
                            .background(Color(0xFFC62828))
                            .clickable { isEmergencyRequested = !isEmergencyRequested },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🚨", fontSize = 24.sp)
                            Text(
                                text = if (isEmergencyRequested) (if (isBengali) "অন" else "ACTIVE") else (if (isBengali) "ট্যাপ করুন" else "TAP SOS"),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                if (isEmergencyRequested) {
                    Text(
                        text = if (isBengali) "SOS রেডার সিগন্যাল অ্যাক্টিভ! নিকটস্থ ভলান্টিয়ারদের জানানো হয়েছে..." 
                               else "SOS Location Active! Transmitting GPS to nearest registered volunteers...",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp,
                        textAlign = TextAlign.Center,
                        color = Color(0xFFC62828)
                    )
                } else {
                    Text(
                        text = if (isBengali) "নিরাপদ মোড। বিপদের সময় জরুরি সাহায্যের অনুরোধ পাঠাতে বোতাম চাপুন।" 
                               else "Safe space. Click the Red alert beacon to instantly send distress help coordinates.",
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        color = Color.Gray
                    )
                }
            }
        }

        // EMOTION / SAFETY BREATHING ASSISTANT
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F7FA)),
            border = BorderStroke(1.5.dp, Color(0xFF00ACC1))
        ) {
            Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (isBengali) "🧘‍♂️ ANEXSOPZ ব্রিদিং মাইন্ডফুলনেস (মেন্টাল স্ট্রেস দূর করুন)" else "🧘‍♂️ ANEXSOPZ Breathing & Mindfulness Helper",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp,
                    color = Color(0xFF006064)
                )
                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .size(80.dp * breathingCircleScale)
                        .clip(CircleShape)
                        .background(Color(0xFF80DEEA).copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("💨", fontSize = 22.sp)
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(breathingState, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.DarkGray)
                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { breathingCyclesRemaining = 3 },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00ACC1))
                ) {
                    Text(if (isBengali) "৪-৭-৮ ব্রিদিং সেশন শুরু করুন" else "Start 4-7-8 Deep Breathing")
                }
            }
        }

        // HELPLINES SECTION
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
            border = BorderStroke(1.dp, Color(0xFFFFB74D))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(if (isBengali) "📞 বাংলাদেশ সরকারী হেল্পলাইন ডায়াল (Emergency Helpline Speed Dials):" else "📞 National Helpline Speed Dials (Bangladesh):", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "🚨 National Service: 999", fontSize = 11.sp, fontWeight = FontWeight.Black)
                    Text(text = "👩 Women Help: 109", fontSize = 11.sp, fontWeight = FontWeight.Black)
                    Text(text = "🚒 Fire Rescue: 102", fontSize = 11.sp, fontWeight = FontWeight.Black)
                }
            }
        }

        // VOLUNTEER LIST FILTERS
        Text(
            text = if (isBengali) "👥 স্থানীয় সাহায্যকারী ভলান্টিয়ার্স নেটওয়ার্ক" else "👥 Local Registered Volunteer Network",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Color.Black
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val categories = listOf("All", "Blood", "Medical", "Fire", "Breakdown")
            categories.forEach { cat ->
                val isSel = selectedCategory == cat
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSel) Color(0xFFC62828) else Color(0xFFF3F4F9))
                        .clickable { selectedCategory = cat }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = cat,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSel) Color.White else Color.Black
                    )
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Checkbox(checked = isVerifiedFilter, onCheckedChange = { isVerifiedFilter = it })
            Text(if (isBengali) "শুধুমাত্র আইডি ভেরিফাইড (আইডি কার্ড চেক করা) ভলান্টিয়ার" else "Only show Verified ID credentials helpers", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }

        // Volunteers list loop
        displayedVolunteers.forEach { vol ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFEEEEEE)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(vol.name, fontWeight = FontWeight.Black, fontSize = 13.sp)
                            if (vol.isVerified) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFFE8F5E9))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text("Verified ✔", fontSize = 8.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Black)
                                }
                            }
                        }
                        Text("${vol.rating} ⭐ reviews", fontSize = 11.sp, color = Color(0xFFFFB300), fontWeight = FontWeight.Bold)
                    }

                    Text("📍 Type: ${vol.category} (${vol.distance}) | Contact: ${vol.phone}", fontSize = 11.sp, color = Color.DarkGray)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = { activeChatHelper = vol },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(if (isBengali) "বার্তা পাঠান (Chat)" else "Chat / Alert Helper", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Active Chat dialog mock
    if (activeChatHelper != null) {
        val helper = activeChatHelper!!
        AlertDialog(
            onDismissRequest = { activeChatHelper = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("💬")
                    Text("Chat with ${helper.name}")
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().height(260.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        mockChatHistory.forEach { (sender, msg) ->
                            val isMe = sender == "You"
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                contentAlignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
                            ) {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isMe) MaterialTheme.colorScheme.primaryContainer else Color(0xFFF3F4F9)
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(sender, fontSize = 8.5.sp, fontWeight = FontWeight.Black)
                                        Text(msg, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = chatInputText,
                            onValueChange = { chatInputText = it },
                            placeholder = { Text("Tape message...", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape = RoundedCornerShape(8.dp)
                        )
                        Button(
                            onClick = {
                                if (chatInputText.isNotEmpty()) {
                                    mockChatHistory.add(Pair("You", chatInputText))
                                    chatInputText = ""
                                }
                            },
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.height(46.dp)
                        ) {
                            Text("Send", fontSize = 10.sp)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }
}

data class EmergencyVolunteer(
    val name: String,
    val bloodType: String,
    val distance: String,
    val phone: String,
    val rating: Float,
    val isVerified: Boolean,
    val category: String
)

