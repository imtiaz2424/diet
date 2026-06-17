package com.example.ui

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.testTag
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.data.api.*
import com.example.viewmodel.DietPlannerViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.content.FileProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DietPlannerDashboard(viewModel: DietPlannerViewModel) {
    val context = LocalContext.current
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val currentMealPlan by viewModel.currentMealPlan.collectAsState()
    val waterLog by viewModel.waterLog.collectAsState()
    val weightLogs by viewModel.allWeightLogs.collectAsState()
    val reminders by viewModel.allReminders.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val eventMessage by viewModel.eventMessage.collectAsState()
    val isBengali by viewModel.isBengali.collectAsState()
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()

    // Screen navigation state inside our dashboard (0: Home/Tools, 1: Meals, 2: Explore, 3: Tracker, 4: Account)
    var currentTab by remember { mutableStateOf(0) }

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            currentTab = 0 // Redirect straight to Home/Tools tab upon login!
        }
    }

    // Floating success snackbar notification helper
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(eventMessage) {
        eventMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearEventMessage()
        }
    }

    val currentHeaderDay = remember(isBengali) {
        val sdf = if (isBengali) {
            SimpleDateFormat("EEEE, dd MMMM", Locale("bn", "BD"))
        } else {
            SimpleDateFormat("EEEE, MMM dd", Locale.ENGLISH)
        }
        sdf.format(Date())
    }

    // Modal Drawer dialogue tracking states
    var showTermsDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showRatingsDialog by remember { mutableStateOf(false) }
    var showAppInfoDialog by remember { mutableStateOf(false) }
    var showAICoachDialog by remember { mutableStateOf(false) }
    var showSearchDialog by remember { mutableStateOf(false) }
    var showHealthPrefsScreen by remember { mutableStateOf(false) }
    var showShoppingListScreen by remember { mutableStateOf(false) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    if (!isLoggedIn) {
        ANEXSOPZLoginScreen(
            isBengali = isBengali,
            onLogin = { email, password, onResult ->
                viewModel.login(email, password, onResult)
            },
            onSignUp = { email, password, onResult ->
                viewModel.signup(email, password, onResult)
            }
        )
    } else {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    drawerContainerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.width(300.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp)
                    ) {
                        // Header of Drawer with ANEXSOPZ Branding Logo
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(bottom = 24.dp, top = 12.dp)
                        ) {
                            ANEXSOPZModernLogo(
                                modifier = Modifier.size(54.dp),
                                showText = false,
                                isBengali = isBengali
                            )
                            Column {
                                Text(
                                    text = "ANEXSOPZ Health",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = if (isBengali) "আপনার বিশ্বস্ত জীবনযাত্রা সহযোগী" else "Your trusted Smart Health Companion",
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }

                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), modifier = Modifier.padding(bottom = 16.dp))

                        // Custom Suvecha upper drawer items (Shopping List, AI Coach, Ratings, Info Manual)
                        val upperDrawerItems = listOf(
                            Triple(if (isBengali) "সাপ্তাহিক বাজারের ফর্দ" else "Weekly Shopping List", Icons.Default.ShoppingCart) {
                                showShoppingListScreen = true
                            },
                            Triple(if (isBengali) "লাইফস্টাইল কোচ" else "Lifestyle Coach", Icons.Default.Face) {
                                showAICoachDialog = true
                            },
                            Triple(if (isBengali) "রেটিং এবং রিভিউ" else "Ratings & Reviews", Icons.Default.Star) {
                                showRatingsDialog = true
                            },
                            Triple(if (isBengali) "অ্যাপ গাইড ও সহায়িকা" else "Information Manual", Icons.Default.MenuBook) {
                                showAppInfoDialog = true
                            }
                        )

                        // Custom Suvecha lower drawer items (Terms, Privacy)
                        val lowerDrawerItems = listOf(
                            Triple(if (isBengali) "ব্যবহারের শর্তাবলী" else "Terms & Conditions", Icons.Default.Description) {
                                showTermsDialog = true
                            },
                            Triple(if (isBengali) "প্রাইভেসি পলিসি" else "Privacy Policy", Icons.Default.Security) {
                                showPrivacyDialog = true
                            }
                        )

                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            upperDrawerItems.forEach { (title, icon, action) ->
                                NavigationDrawerItem(
                                    label = { Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp) },
                                    selected = false,
                                    onClick = {
                                        scope.launch { drawerState.close() }
                                        action()
                                    },
                                    icon = { Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) },
                                    colors = NavigationDrawerItemDefaults.colors(
                                        unselectedContainerColor = Color.Transparent,
                                        unselectedIconColor = MaterialTheme.colorScheme.primary,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurface
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }

                            // Dynamic Dark Theme Switch Item in the Drawer
                            NavigationDrawerItem(
                                label = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(end = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (isBengali) "ডার্ক মোড (Dark Theme)" else "Dark Theme Mode",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp
                                        )
                                        Switch(
                                            checked = isDarkTheme,
                                            onCheckedChange = { viewModel.toggleTheme(context) },
                                            modifier = Modifier.scale(0.75f),
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                                            )
                                        )
                                    }
                                },
                                selected = false,
                                onClick = { viewModel.toggleTheme(context) },
                                icon = {
                                    Icon(
                                        imageVector = if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                                        contentDescription = "Theme Icon",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                colors = NavigationDrawerItemDefaults.colors(
                                    unselectedContainerColor = Color.Transparent,
                                    unselectedIconColor = MaterialTheme.colorScheme.primary,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurface
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.weight(1f))

                            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), modifier = Modifier.padding(vertical = 4.dp))

                            lowerDrawerItems.forEach { (title, icon, action) ->
                                NavigationDrawerItem(
                                    label = { Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp) },
                                    selected = false,
                                    onClick = {
                                        scope.launch { drawerState.close() }
                                        action()
                                    },
                                    icon = { Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) },
                                    colors = NavigationDrawerItemDefaults.colors(
                                        unselectedContainerColor = Color.Transparent,
                                        unselectedIconColor = MaterialTheme.colorScheme.primary,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurface
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }

                        // Footer section with Suvecha brand, version and sign out
                        Divider(color = Color(0xFFEEEEEE), modifier = Modifier.padding(bottom = 12.dp))
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch { drawerState.close() }
                                    viewModel.logout()
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ExitToApp,
                                contentDescription = "Sign Out",
                                tint = Color(0xFFC62828),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = if (isBengali) "লগআউট করুন" else "Sign Out",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFFC62828)
                            )
                        }

                        // Support query section beneath Sign Out
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isBengali) "যেকোনো প্রশ্নের জন্য যোগাযোগ করুন:" else "For any query, contact us at:",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.Gray
                            )
                            Text(
                                text = "support@anexsopz.com",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                        }

                        Text(
                            text = "ANEXSOPZ Health Plus v2.1",
                            fontSize = 9.sp,
                            color = Color.LightGray,
                            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp)
                        )
                    }
                }
            }
        ) {
            Scaffold(
                topBar = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                        // Left Side Hamburger Toggle Button
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier.align(Alignment.CenterStart)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "ANEXSOPZ Navigation Drawer",
                                tint = Color(0xFF1E5E2F),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Centered Logo & Brand Name
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.align(Alignment.Center)
                        ) {
                            ANEXSOPZModernLogo(
                                modifier = Modifier.size(34.dp),
                                showText = false,
                                isBengali = isBengali
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "ANEXSOPZ",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp,
                                    color = Color(0xFF1E5E2F),
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = if (isBengali) "সুস্থ জীবনের পথ" else "Path to Healthy Living",
                                    fontSize = 7.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF43A047),
                                    letterSpacing = 0.2.sp
                                )
                            }
                        }

                        // Right Side Language Switch Row with custom alerts option
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.align(Alignment.CenterEnd)
                        ) {
                            // Language Toggle
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .clickable { viewModel.toggleLanguage() }
                                    .padding(horizontal = 7.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = if (isBengali) "EN" else "বাংলা",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            // Theme Toggle Button
                            IconButton(
                                onClick = { viewModel.toggleTheme(context) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                    contentDescription = "Toggle Theme",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Notification Option with indicator badge
                            var showNotifications by remember { mutableStateOf(false) }
                            IconButton(
                                onClick = { showNotifications = true },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsActive,
                                        contentDescription = "ANEXSOPZ Alerts",
                                        tint = Color(0xFF1E5E2F),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(Color.Red)
                                            .align(Alignment.TopEnd)
                                    )
                                }
                            }

                            if (showNotifications) {
                                ANEXSOPZNotificationDialog(
                                    isBengali = isBengali,
                                    onDismiss = { showNotifications = false }
                                )
                            }

                            if (showSearchDialog) {
                                ANEXSOPZSearchDialog(
                                    isBengali = isBengali,
                                    onDismiss = { showSearchDialog = false },
                                    onNavigateToTab = { tabIndex ->
                                        currentTab = tabIndex
                                        showSearchDialog = false
                                    }
                                )
                            }

                            if (showTermsDialog) {
                                ANEXSOPZTermsDialog(
                                    isBengali = isBengali,
                                    onDismiss = { showTermsDialog = false }
                                )
                            }

                            if (showPrivacyDialog) {
                                ANEXSOPZPrivacyPolicyDialog(
                                    isBengali = isBengali,
                                    onDismiss = { showPrivacyDialog = false }
                                )
                            }

                            if (showAICoachDialog) {
                                ANEXSOPZAICoachDialog(
                                    isBengali = isBengali,
                                    onDismiss = { showAICoachDialog = false }
                                )
                            }

                            if (showRatingsDialog) {
                                ANEXSOPZRatingsDialog(
                                    isBengali = isBengali,
                                    onDismiss = { showRatingsDialog = false }
                                )
                            }

                            if (showAppInfoDialog) {
                                ANEXSOPZAppInfoDialog(
                                    isBengali = isBengali,
                                    onDismiss = { showAppInfoDialog = false }
                                )
                            }
                        }
                        }

                        // Sliding Search Box Bar below the navigation row representing standard exploration search
                        var searchPlaceholderIndex by remember { mutableStateOf(0) }
                        val searchPlaceholders = listOf(
                            "egg protein ...",
                            "brown rice carbs ...",
                            "oatmeal recipe ...",
                            "water tracker log ...",
                            "cardio exercises ...",
                            "healthy salads ..."
                        )
                        LaunchedEffect(Unit) {
                            while (true) {
                                kotlinx.coroutines.delay(2500)
                                searchPlaceholderIndex = (searchPlaceholderIndex + 1) % searchPlaceholders.size
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .clickable { showSearchDialog = true }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search icon",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Search for " + searchPlaceholders[searchPlaceholderIndex],
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    fontSize = 13.sp,
                                    maxLines = 1
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                },
                bottomBar = {
                    NavigationBar(
                        containerColor = Color(0xFFF3F4F9)
                    ) {
                        NavigationBarItem(
                            selected = currentTab == 0,
                            onClick = { currentTab = 0 },
                            icon = { Icon(Icons.Default.Home, contentDescription = "হোম (Home)") },
                            label = { Text(if (isBengali) "হোম" else "Home", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                        NavigationBarItem(
                            selected = currentTab == 1,
                            onClick = { currentTab = 1 },
                            icon = { Icon(Icons.Default.RestaurantMenu, contentDescription = "নাস্তা ও খাবার (Diet Plan)") },
                            label = { Text(if (isBengali) "খাবার" else "Meals", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                        NavigationBarItem(
                            selected = currentTab == 2,
                            onClick = { currentTab = 2 },
                            icon = { Icon(Icons.Default.Explore, contentDescription = "এক্সপলোর (Explore)") },
                            label = { Text(if (isBengali) "এক্সপ্লোর" else "Explore", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                        NavigationBarItem(
                            selected = currentTab == 3,
                            onClick = { currentTab = 3 },
                            icon = { Icon(Icons.Default.TrendingUp, contentDescription = "ওজন ও ট্র্যাক (Progress)") },
                            label = { Text(if (isBengali) "ট্র্যাকার" else "Tracker", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                        NavigationBarItem(
                            selected = currentTab == 4,
                            onClick = { currentTab = 4 },
                            icon = { Icon(Icons.Default.AccountCircle, contentDescription = "প্রোফাইল (Profile)") },
                            label = { Text(if (isBengali) "অ্যাকাউন্ট" else "Account", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                    }
                },
                floatingActionButton = {
                    if (currentTab == 1 && userProfile != null && !isGenerating) {
                        FloatingActionButton(
                            onClick = { viewModel.generateMealPlan(context) },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "Generate"
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isBengali) "সুষম খাবার" else "Custom Plan",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                },
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(MaterialTheme.colorScheme.background)
        ) {
            // Check if profile exists, if empty, redirect to profile screen first
            if (userProfile == null) {
                ProfileSetupView(
                    isBengali = isBengali,
                    onSave = { age, gender, weight, height, goal, preference, allergies, medical, cuisine ->
                        viewModel.saveProfile(age, gender, weight, height, goal, preference, allergies, medical, cuisine)
                        currentTab = 0 // Navigate to index after creation
                    }
                )
            } else if (showHealthPrefsScreen) {
                HealthPreferencesScreen(
                    viewModel = viewModel,
                    isBengali = isBengali,
                    onBack = { showHealthPrefsScreen = false }
                )
            } else if (showShoppingListScreen) {
                ShoppingListScreen(
                    viewModel = viewModel,
                    onBack = { showShoppingListScreen = false }
                )
            } else {
                when (currentTab) {
                    0 -> ToolsTab(
                        viewModel = viewModel,
                        reminders = reminders,
                        userProfile = userProfile!!,
                        selectedDate = selectedDate
                    )
                    1 -> MealPlanTab(
                        viewModel = viewModel,
                        selectedDate = selectedDate,
                        mealPlan = currentMealPlan,
                        isGenerating = isGenerating,
                        userProfile = userProfile!!,
                        context = context,
                        onNavigateToShoppingList = { showShoppingListScreen = true }
                    )
                    2 -> ExploreTab(
                        viewModel = viewModel,
                        isBengali = isBengali,
                        onNavigateToTab = { tabIndex -> currentTab = tabIndex }
                    )
                    3 -> ProgressTrackerTab(
                        viewModel = viewModel,
                        weightLogs = weightLogs,
                        waterLog = waterLog,
                        userProfile = userProfile!!,
                        selectedDate = selectedDate
                    )
                    4 -> ProfileEditTab(
                        userProfile = userProfile!!,
                        isBengali = isBengali,
                        onNavigateToHealthPrefs = { showHealthPrefsScreen = true },
                        onSave = { age, gender, weight, height, goal, preference, allergies, medical, cuisine ->
                            viewModel.saveProfile(age, gender, weight, height, goal, preference, allergies, medical, cuisine)
                        }
                    )
                }
            }
        }
    }
    }
    }
}

// ==========================================
// TAB 0: MEAL PLAN & DIET SCREEN
// ==========================================
@Composable
fun MealPlanTab(
    viewModel: DietPlannerViewModel,
    selectedDate: String,
    mealPlan: MealPlanEntity?,
    isGenerating: Boolean,
    userProfile: UserProfileEntity,
    context: Context,
    onNavigateToShoppingList: () -> Unit
) {
    // Collect extra food and exercise logs to automatically calculate net calorie balance
    val currentFoodLogs by viewModel.currentFoodLogs.collectAsState()
    val currentExerciseLogs by viewModel.currentExerciseLogs.collectAsState()
    val isBengali by viewModel.isBengali.collectAsState()
    val waterLog by viewModel.waterLog.collectAsState()

    var showDailyInsightDialog by remember { mutableStateOf(false) }

    val extraSnacksCal = currentFoodLogs.sumOf { it.calories }
    val workoutBurntCal = currentExerciseLogs.sumOf { it.caloriesBurned }

    // In-memory persistent map for checked meals per category on specific dates
    val completedMeals = remember { mutableStateMapOf<String, Boolean>() }

    val breakfastKey = "${selectedDate}_breakfast"
    val snack1Key = "${selectedDate}_snack1"
    val lunchKey = "${selectedDate}_lunch"
    val snack2Key = "${selectedDate}_snack2"
    val dinnerKey = "${selectedDate}_dinner"

    // Default mock setup for a cohesive first-run matching HTML design mockup (Oatmeal & Apple Done, Lunch Next Up)
    LaunchedEffect(selectedDate) {
        if (!completedMeals.containsKey(breakfastKey)) {
            completedMeals[breakfastKey] = true
            completedMeals[snack1Key] = true
        }
    }

    val isBreakfastDone = completedMeals[breakfastKey] == true
    val isSnack1Done = completedMeals[snack1Key] == true
    val isLunchDone = completedMeals[lunchKey] == true
    val isSnack2Done = completedMeals[snack2Key] == true
    val isDinnerDone = completedMeals[dinnerKey] == true

    val totalCalorieTarget = mealPlan?.calorieTarget ?: userProfile.dailyCalorieTarget
    var consumedCal = 0
    if (mealPlan != null) {
        if (isBreakfastDone) consumedCal += mealPlan.breakfastCal
        if (isSnack1Done) consumedCal += mealPlan.snack1Cal
        if (isLunchDone) consumedCal += mealPlan.lunchCal
        if (isSnack2Done) consumedCal += mealPlan.snack2Cal
        if (isDinnerDone) consumedCal += mealPlan.dinnerCal
    } else {
        consumedCal = (totalCalorieTarget * 0.69).toInt()
    }

    // Dynamic calorie calculations incorporating database logged actions
    val netConsumedCal = (consumedCal + extraSnacksCal - workoutBurntCal).coerceAtLeast(0)
    val progressPercent = if (totalCalorieTarget > 0) {
        (netConsumedCal.toFloat() / totalCalorieTarget.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    val nextUpCategory = when {
        !isBreakfastDone -> "Breakfast"
        !isSnack1Done -> "Snack 1"
        !isLunchDone -> "Lunch"
        !isSnack2Done -> "Snack 2"
        !isDinnerDone -> "Dinner"
        else -> null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Date Selector Bar
        DateSelectorHeader(
            selectedDate = selectedDate,
            onPreviousDate = {
                val newDate = adjustDateString(selectedDate, -1)
                viewModel.selectDate(newDate)
            },
            onNextDate = {
                val newDate = adjustDateString(selectedDate, 1)
                viewModel.selectDate(newDate)
            },
            onTodayDate = {
                val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                viewModel.selectDate(format.format(Date()))
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Beautiful application description in the main body
        ANEXSOPZAppDescriptionCard(isBengali = isBengali)

        Spacer(modifier = Modifier.height(16.dp))

        // Basic info summary card
        ProfileMiniCard(userProfile = userProfile)

        Spacer(modifier = Modifier.height(16.dp))

        // Daily Insight Dashboard Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDailyInsightDialog = true }
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary
                        )
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🌟", fontSize = 22.sp)
                    }
                    Column {
                        Text(
                            text = if (isBengali) "আজকের স্বাস্থ্য অন্তর্দৃষ্টি (Insights)" else "View Daily Health Insights",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (isBengali) "ক্যালরি, পানি ও ব্যায়ামের নিখুঁত সমন্বিত রিপোর্ট"
                                   else "Consolidated report of calories, water & active workouts",
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Show Insight Dialog",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        if (showDailyInsightDialog) {
            ANEXSOPZDailyInsightDialog(
                isBengali = isBengali,
                totalCalorieTarget = totalCalorieTarget,
                consumedMealsCal = consumedCal,
                extraSnacksCal = extraSnacksCal,
                workoutBurntCal = workoutBurntCal,
                waterLog = waterLog,
                dailyWaterTarget = userProfile.dailyWaterTargetMl,
                currentExerciseLogs = currentExerciseLogs,
                onDismiss = { showDailyInsightDialog = false }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isGenerating) {
            AIGeneratingStateCard()
        } else if (mealPlan == null) {
            NoMealPlanPlaceholderCard(
                isBengali = isBengali,
                onGenerate = {
                    viewModel.generateMealPlan(context)
                }
            )
        } else {
            // Meal Plan Display - Custom Calorie Progress card
            MealPlanHeaderSummary(
                mealPlan = mealPlan,
                consumedCal = netConsumedCal,
                progressPercent = progressPercent
            )

            Spacer(modifier = Modifier.height(16.dp))

            MealCardItem(
                mealTitle = "Breakfast (সকালের নাস্তা)",
                mealDetails = mealPlan.breakfast,
                calories = mealPlan.breakfastCal,
                categoryName = "Breakfast",
                isDone = isBreakfastDone,
                onDoneChange = { completedMeals[breakfastKey] = it },
                isNextUp = nextUpCategory == "Breakfast"
            )
            Spacer(modifier = Modifier.height(12.dp))

            MealCardItem(
                mealTitle = "Snack 1 (সকালের হালকা খাবার)",
                mealDetails = mealPlan.snack1,
                calories = mealPlan.snack1Cal,
                categoryName = "Snack 1",
                isDone = isSnack1Done,
                onDoneChange = { completedMeals[snack1Key] = it },
                isNextUp = nextUpCategory == "Snack 1"
            )
            Spacer(modifier = Modifier.height(12.dp))

            MealCardItem(
                mealTitle = "Lunch (দুপুরের খাবার)",
                mealDetails = mealPlan.lunch,
                calories = mealPlan.lunchCal,
                categoryName = "Lunch",
                isDone = isLunchDone,
                onDoneChange = { completedMeals[lunchKey] = it },
                isNextUp = nextUpCategory == "Lunch"
            )
            Spacer(modifier = Modifier.height(12.dp))

            MealCardItem(
                mealTitle = "Snack 2 (বিকালের হালকা খাবার)",
                mealDetails = mealPlan.snack2,
                calories = mealPlan.snack2Cal,
                categoryName = "Snack 2",
                isDone = isSnack2Done,
                onDoneChange = { completedMeals[snack2Key] = it },
                isNextUp = nextUpCategory == "Snack 2"
            )
            Spacer(modifier = Modifier.height(12.dp))

            MealCardItem(
                mealTitle = "Dinner (রাতের খাবার)",
                mealDetails = mealPlan.dinner,
                calories = mealPlan.dinnerCal,
                categoryName = "Dinner",
                isDone = isDinnerDone,
                onDoneChange = { completedMeals[dinnerKey] = it },
                isNextUp = nextUpCategory == "Dinner"
            )

            Spacer(modifier = Modifier.height(20.dp))

            // motivational health tip banner card
            HealthTipCard(tip = mealPlan.dailyTip)

            Spacer(modifier = Modifier.height(24.dp))

            // Auto-generated interactive ingredients checklist shopping list
            LocalShoppingListCard(viewModel = viewModel, isBengali = isBengali)

            Spacer(modifier = Modifier.height(12.dp))

            // Premium Week-long categorized checklist screen trigger
            Button(
                onClick = { onNavigateToShoppingList() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("weekly_shopping_list_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ShoppingCart,
                    contentDescription = "Weekly Shopping List",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isBengali) "সাপ্তাহিক বাজারের ফর্দ" else "View Weekly Shopping List",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Social Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        val pdfFile = viewModel.exportPdfReport(context, mealPlan, userProfile)
                        if (pdfFile != null) {
                            sharePdf(context, pdfFile)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF Report Export")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isBengali) "PDF রিপোর্ট" else "PDF Report")
                }

                OutlinedButton(
                    onClick = {
                        shareMealPlanToSocial(context, mealPlan)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Social Sharing")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isBengali) "শেয়ার করুন" else "Share Plan")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(
                onClick = { viewModel.generateMealPlan(context) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Regenerate")
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isBengali) "নতুন ডায়েট প্ল্যান জেনারেট করুন" else "Regenerate Diet Plan")
            }
        }
    }
}

@Composable
fun LocalShoppingListCard(
    viewModel: DietPlannerViewModel,
    isBengali: Boolean
) {
    val items by viewModel.shoppingItems.collectAsState()
    var manualItemName by remember { mutableStateOf("") }
    var manualItemQty by remember { mutableStateOf("") }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🛒", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isBengali) "আজকের বাজারের ফর্দ (Shopping List)" else "Daily Shopping Checklist",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFF1B5E20)
                    )
                }

                if (items.isNotEmpty()) {
                    val purchasedCount = items.filter { it.isChecked }.size
                    Text(
                        text = "$purchasedCount/${items.size}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isBengali) "কোনো বাজারের ফর্দ নেই। ডায়েট জেনারেট করলে বা নিচে কাস্টম আইটেম যোগ করলে তৈরি হবে।"
                        else "Nothing on your list yet. Complete or generate a meal plan above to populate local ingredients!",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // List of items
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (item.isChecked) Color(0xFFF1F8E9) else Color(0xFFF5F5F5))
                                .clickable { viewModel.toggleShoppingItemChecked(item.id, !item.isChecked) }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = item.isChecked,
                                onCheckedChange = { checked ->
                                    if (checked != null) {
                                        viewModel.toggleShoppingItemChecked(item.id, checked)
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.name,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (item.isChecked) Color.Gray else Color.Black,
                                    style = androidx.compose.ui.text.TextStyle(
                                        textDecoration = if (item.isChecked) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                                    )
                                )
                                Text(
                                    text = item.quantity,
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }

                    TextButton(
                        onClick = { viewModel.clearShoppingListForSelectedDate() },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.Red),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear List", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isBengali) "ফর্দ পরিষ্কার করুন" else "Clear Shopping List", fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFEEEEEE))
            Spacer(modifier = Modifier.height(12.dp))

            // Add manual item
            Text(
                text = if (isBengali) "কাস্টম আইটেম যোগ করুন:" else "Add manual item:",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = manualItemName,
                    onValueChange = { manualItemName = it },
                    label = { Text(if (isBengali) "উপকরণ" else "Name (e.g. Milk)", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                )

                OutlinedTextField(
                    value = manualItemQty,
                    onValueChange = { manualItemQty = it },
                    label = { Text(if (isBengali) "পরিমাণ" else "Qty (e.g. 1L)", fontSize = 11.sp) },
                    modifier = Modifier.weight(0.7f),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                )

                Button(
                    onClick = {
                        if (manualItemName.isNotBlank()) {
                            val qty = if (manualItemQty.isBlank()) "As needed" else manualItemQty
                            viewModel.addManualShoppingItem(manualItemName, qty)
                            manualItemName = ""
                            manualItemQty = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add custom item", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(if (isBengali) "যোগ" else "Add", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun DateSelectorHeader(
    selectedDate: String,
    onPreviousDate: () -> Unit,
    onNextDate: () -> Unit,
    onTodayDate: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onPreviousDate) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "পূর্ববর্তী দিন")
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onTodayDate() }
            ) {
                Text(
                    text = formatBanglaDate(selectedDate),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "আজকের দিনে ফিরুন (Today)",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onNextDate) {
                Icon(Icons.Default.ChevronRight, contentDescription = "পরবর্তী দিন")
            }
        }
    }
}

@Composable
fun ProfileMiniCard(userProfile: UserProfileEntity) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProfileMetricItem("ওজন", "${userProfile.weight} কেজি")
            ProfileMetricItem("উচ্চতা", "${userProfile.height} সেমি")
            ProfileMetricItem("লক্ষ্য", when (userProfile.goal) {
                "Weight Loss", "ওজন কমানো" -> "কমানো"
                "Weight Gain", "ওজন বাড়ানো" -> "বাড়ানো"
                else -> "ধরে রাখা"
            })
            ProfileMetricItem("ক্যালোরি", "${userProfile.dailyCalorieTarget} kcal")
        }
    }
}

@Composable
fun ProfileMetricItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
        Text(text = value, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

@Composable
fun MealPlanHeaderSummary(
    mealPlan: MealPlanEntity,
    consumedCal: Int,
    progressPercent: Float
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "আজকের ক্যালোরি লক্ষ্য (Daily Goal)",
                    color = Color(0xFF001D36),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = String.format("%,d", consumedCal),
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp,
                        color = Color(0xFF001D36)
                    )
                    Text(
                        text = " / ${mealPlan.calorieTarget} kcal",
                        fontWeight = FontWeight.Medium,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                val remaining = (mealPlan.calorieTarget - consumedCal).coerceAtLeast(0)
                Text(
                    text = if (remaining > 0) "$remaining kcal remaining today" else "আজকের ক্যালোরি লক্ষ্য পূরণ হয়েছে! 🎉",
                    fontSize = 12.sp,
                    color = Color(0xFF535F70),
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Circular Progress ring
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(80.dp)
            ) {
                CircularProgressIndicator(
                    progress = { progressPercent },
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 8.dp,
                    color = Color(0xFF0061A4),
                    trackColor = Color.White
                )
                Text(
                    text = "${(progressPercent * 100).toInt()}%",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF001D36)
                )
            }
        }
        
        // Nutrients Ratio divider bar
        Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 20.dp)) {
            Text(
                text = "পুষ্টি ভাগ: শর্করা ৪০% | আমিষ ৩০% | ফ্যাট ৩০%",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF001D36).copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
            ) {
                Box(modifier = Modifier.weight(0.4f).fillMaxHeight().background(Color(0xFFFFB74D)))
                Box(modifier = Modifier.weight(0.3f).fillMaxHeight().background(Color(0xFF81C784)))
                Box(modifier = Modifier.weight(0.3f).fillMaxHeight().background(Color(0xFFE57373)))
            }
        }
    }
}

@Composable
fun MealCardItem(
    mealTitle: String,
    mealDetails: String,
    calories: Int,
    categoryName: String,
    isDone: Boolean,
    onDoneChange: (Boolean) -> Unit,
    isNextUp: Boolean
) {
    val emoji = when (categoryName) {
        "Breakfast" -> "🥣"
        "Snack 1" -> "🍎"
        "Lunch" -> "🥘"
        "Snack 2" -> "🥤"
        "Dinner" -> "🍲"
        else -> "🍽️"
    }

    // Done or Next Up backgrounds
    val cardBg = if (isNextUp) {
        Color(0xFFF3F4F9)
    } else {
        MaterialTheme.colorScheme.surface
    }

    val cardBorder = if (isNextUp) {
        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    }

    val alpha = if (isDone) 0.65f else 1.0f

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = cardBorder,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onDoneChange(!isDone) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Emoji category circle
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            when(categoryName) {
                                "Breakfast" -> Color(0xFFF3F0FF)
                                "Snack 1" -> Color(0xFFFFF8F1)
                                "Lunch" -> Color(0xFFE7F3E7)
                                "Snack 2" -> Color(0xFFFFF1F1)
                                "Dinner" -> Color(0xFFF1F7FF)
                                else -> MaterialTheme.colorScheme.secondaryContainer
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = emoji, fontSize = 22.sp)
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = mealTitle,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
                        )
                        Text(
                            text = "$calories kcal",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = alpha)
                        )
                    }
                    
                    val statusText = when {
                        isDone -> "সম্পন্ন • Completed"
                        isNextUp -> "পরবর্তী খাবার • Next up"
                        else -> "অপেক্ষমান • Scheduled"
                    }
                    Text(
                        text = statusText,
                        fontSize = 12.sp,
                        fontWeight = if (isNextUp) FontWeight.Bold else FontWeight.Medium,
                        color = if (isNextUp) MaterialTheme.colorScheme.primary else Color(0xFF535F70)
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Done state indicator (Checkbox or Check Icon)
                Checkbox(
                    checked = isDone,
                    onCheckedChange = { onDoneChange(it) },
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary,
                        uncheckedColor = MaterialTheme.colorScheme.outline
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = mealDetails,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun HealthTipCard(tip: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "💡", fontSize = 20.sp)
            Text(
                text = tip,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                lineHeight = 18.sp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun AIGeneratingStateCard() {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.padding(vertical = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(modifier = Modifier.size(54.dp))
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "ডায়েট চার্ট সাজানো হচ্ছে...",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "আপনার জন্য সেরা স্থানীয় খাবার দিয়ে সাজানো হচ্ছে। অনুগ্রহ করে একটু অপেক্ষা করুন।",
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun NoMealPlanPlaceholderCard(isBengali: Boolean, onGenerate: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Restaurant,
                contentDescription = "No Diet Plan",
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (isBengali) "আজকের কোনো ডায়েট প্ল্যান নেই" else "No Diet Plan for Today",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isBengali)
                    "আপনার শরীর ও স্বাস্থ্যের লক্ষ্য অনুযায়ী এখনই ANEXSOPZ ১ দিনের কাস্টম ডায়েট প্ল্যান তৈরি করুন।"
                else
                    "Generate a 1-day custom ANEXSOPZ diet plan matching your body stats and wellness goals.",
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onGenerate,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = "Generate")
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isBengali) "ডায়েট প্ল্যান তৈরি করুন ⚡" else "Create Custom Diet Plan")
            }
        }
    }
}

// ==========================================
// TAB 1: PROGRESS TRACKER (Weight & Water)
// ==========================================
@Composable
fun ProgressTrackerTab(
    viewModel: DietPlannerViewModel,
    weightLogs: List<WeightLogEntity>,
    waterLog: WaterLogEntity?,
    userProfile: UserProfileEntity,
    selectedDate: String
) {
    val isBengali by viewModel.isBengali.collectAsState()
    val targetWeight by viewModel.targetWeight.collectAsState()
    var newWeightString by remember { mutableStateOf("") }
    var targetWeightString by remember { mutableStateOf("") }
    val context = LocalContext.current

    // Synchronize loaded target weight to input string field
    LaunchedEffect(targetWeight) {
        if (targetWeight > 0.0) {
            targetWeightString = targetWeight.toString()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // High-Fidelity Interactive Day-wise Nutrition & Macronutrients goals tracker
        NutritionDashboardComponent(
            viewModel = viewModel,
            userProfile = userProfile,
            isBengali = isBengali
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Water Tracker Card
        WaterTrackerCard(
            waterLog = waterLog,
            targetMl = userProfile.dailyWaterTargetMl,
            onAddWater = { viewModel.addWater(it) }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Daily Physical Activity & Workout Logger
        DailyPhysicalActivityTrackerCard(
            viewModel = viewModel,
            userProfile = userProfile,
            isBengali = isBengali
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Weight tracker section
        Text(
            text = if (isBengali) "ওজন পরিবর্তন ও লক্ষ্য ট্র্যাকার (Weight & Goal Progress)" else "Weight & Goal Tracker",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Weight Log & Target form
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Current Weight Log
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isBengali) "নতুন ওজন রেকর্ড:" else "Log Weight:",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        OutlinedTextField(
                            value = newWeightString,
                            onValueChange = { newWeightString = it },
                            placeholder = { Text(if (isBengali) "৭২.৫ কেজি" else "e.g., 72.5") },
                            label = { Text(if (isBengali) "ওজন" else "Current") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                val parsed = newWeightString.toDoubleOrNull()
                                if (parsed != null && parsed > 0.0) {
                                    viewModel.logWeight(parsed, selectedDate)
                                    newWeightString = ""
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Log", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isBengali) "সেভ" else "Save", fontSize = 11.sp)
                        }
                    }

                    // Target Weight Goal
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isBengali) "লক্ষ্য ওজন সেট করুন:" else "Set Target Goal:",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            color = Color(0xFFE65100),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        OutlinedTextField(
                            value = targetWeightString,
                            onValueChange = { targetWeightString = it },
                            placeholder = { Text(if (isBengali) "৬৫.০ কেজি" else "e.g., 65.0") },
                            label = { Text(if (isBengali) "লক্ষ্য" else "Goal kg") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                val parsed = targetWeightString.toDoubleOrNull()
                                if (parsed != null && parsed > 0.0) {
                                    viewModel.saveTargetWeight(context, parsed)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Flag, contentDescription = "Set Goal", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isBengali) "ট্রিগার" else "Set Set", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Dedicated Goal Info & Progress Analysis Card
        if (targetWeight > 0.0) {
            val latestLoggedWeight = weightLogs.lastOrNull()?.weight ?: userProfile.weight
            val diff = latestLoggedWeight - targetWeight
            val isLoseGoal = userProfile.goal.contains("Loss") || userProfile.goal.contains("কমানো")
            val absDiff = kotlin.math.abs(diff)
            val roundedDiff = String.format(Locale.US, "%.1f", absDiff)
            
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (diff == 0.0) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.15f)
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = if (diff == 0.0) "🎉" else "🎯",
                            fontSize = 22.sp
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isBengali) "লক্ষ্য ও অগ্রগতি বিশ্লেষণ" else "Goal & Progress Analysis",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isBengali) "আপনার সুস্থতার মাইলফলক পর্যবেক্ষণ করুন" else "Helping you achieve your weight baseline",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = if (isBengali) "সর্বশেষ ওজন" else "Latest Weight",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$latestLoggedWeight kg",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (isBengali) "লক্ষ্য ওজন" else "Target Goal",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$targetWeight kg",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color(0xFFFF9800)
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (isBengali) "অবশিষ্ট" else "Remaining",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (diff == 0.0) {
                                    if (isBengali) "অর্জিত! 🎉" else "Reached! 🎉"
                                } else {
                                    "$roundedDiff kg"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = if (diff == 0.0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val encouragementMessage = if (diff == 0.0) {
                        if (isBengali) "অসাধারণ! আপনি আপনার লক্ষ্য অর্জন করেছেন! সুস্থ থাকুন এবং এই ধারাবাহিকতা বজায় রাখুন।" 
                        else "Amazing! You have met your goal! Maintain your healthy habits and stay energized."
                    } else if (diff > 0.0) {
                        if (isLoseGoal) {
                            val targetTextEn = "You are $roundedDiff kg away from your target goal. Keep moving and stay focused!"
                            val targetTextBn = "লক্ষ্য অর্জনে আপনাকে আরও $roundedDiff কেজি কমাতে হবে। নিয়মিত ব্যায়াম এবং সঠিক খাবার গ্রহণ করতে থাকুন!"
                            if (isBengali) targetTextBn else targetTextEn
                        } else {
                            val targetTextEn = "You have exceeded your target goal weight by $roundedDiff kg. Keep training!"
                            val targetTextBn = "আপনি আপনার লক্ষ্য ওজনের চেয়ে $roundedDiff কেজি উপরে আছেন। সঠিক ভারসাম্য বজায় রাখুন!"
                            if (isBengali) targetTextBn else targetTextEn
                        }
                    } else {
                        // diff < 0.0
                        if (isLoseGoal) {
                            val targetTextEn = "You have lost more than your target goal! You are $roundedDiff kg below target. Keep steady!"
                            val targetTextBn = "আপনি লক্ষ্য ওজনের চেয়েও $roundedDiff কেজি নিচে আছেন! শক্তি বজায় রাখুন।"
                            if (isBengali) targetTextBn else targetTextEn
                        } else {
                            val targetTextEn = "You are $roundedDiff kg away from gaining to your target weight. Nourish your body!"
                            val targetTextBn = "লক্ষ্য অর্জনে আপনাকে আরও $roundedDiff কেজি বাড়াতে হবে। পর্যাপ্ত পুষ্টিকর খাবার গ্রহণ করতে থাকুন!"
                            if (isBengali) targetTextBn else targetTextEn
                        }
                    }

                    Text(
                        text = encouragementMessage,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Weight Chart Card
        WeightProgressChartCard(weightLogs = weightLogs, targetWeight = targetWeight, isBengali = isBengali)

        Spacer(modifier = Modifier.height(16.dp))

        // History Log List
        Text(
            text = "ওজনের ইতিহাস",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (weightLogs.isEmpty()) {
            Text(
                "কোনো ওজন হিস্টোরে নেই।",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.outline
            )
        } else {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    weightLogs.reversed().take(10).forEachIndexed { index, log ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "${log.weight} কেজি",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = log.date,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { viewModel.deleteWeight(log.date) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "রেকর্ড ডিলিট",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        if (index < weightLogs.size - 1) {
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WaterTrackerCard(
    waterLog: WaterLogEntity?,
    targetMl: Int,
    onAddWater: (Int) -> Unit
) {
    val amount = waterLog?.amountMl ?: 0
    val pct = (amount.toFloat() / targetMl.toFloat()).coerceIn(0f, 1f)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "পানি পানের লগ (Hydration Log)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(text = "টার্গেট: $targetMl মিলি", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(
                    imageVector = Icons.Default.LocalGasStation,
                    contentDescription = "Water tracking icon",
                    tint = Color(0xFF29B6F6)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Progress bar and details
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.outlineVariant),
                contentAlignment = Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(pct)
                        .background(Color(0xFF29B6F6))
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "$amount ml / $targetMl ml (${(pct * 100).toInt()}%)",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = Color(0xFF0288D1)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Quick add buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onAddWater(250) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF29B6F6))
                ) {
                    Text("+১ গ্লাস (250ml)", fontSize = 11.sp)
                }

                Button(
                    onClick = { onAddWater(500) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0288D1))
                ) {
                    Text("+১ বোতল (500ml)", fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = { onAddWater(-250) },
                    modifier = Modifier.weight(0.6f)
                ) {
                    Text("-১ গ্লাস", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun DailyPhysicalActivityTrackerCard(
    viewModel: DietPlannerViewModel,
    userProfile: UserProfileEntity,
    isBengali: Boolean
) {
    val loggedExercises by viewModel.currentExerciseLogs.collectAsState(initial = emptyList())
    
    // Comprehensive Activity Database
    val activityDatabase = remember {
        listOf(
            Triple("Walking", "🚶", if (isBengali) "হাঁটা" else "Walking"),
            Triple("Jogging", "🏃", if (isBengali) "দৌড়ানো / রানিং" else "Jogging / Running"),
            Triple("Cycling", "🚴", if (isBengali) "সাইকেল চালানো" else "Cycling"),
            Triple("Swimming", "🏊", if (isBengali) "সাঁতার কাটা" else "Swimming"),
            Triple("Gym", "🏋️", if (isBengali) "ভারোত্তোলন / জিম" else "Gym / Weightlifting"),
            Triple("Yoga", "🧘", if (isBengali) "যোগব্যায়াম" else "Yoga & Meditation"),
            Triple("Rope_Skipping", "🪢", if (isBengali) "দড়ি লাফ" else "Rope Skipping"),
            Triple("Soccer", "⚽", if (isBengali) "ফুটবল খেলা" else "Football / Soccer"),
            Triple("Basketball", "🏀", if (isBengali) "বাস্কেটবল" else "Basketball"),
            Triple("Badminton", "🏸", if (isBengali) "ব্যাডমিন্টন" else "Badminton"),
            Triple("Dancing", "💃", if (isBengali) "নাচ / জুম্বা" else "Dancing / Zumba"),
            Triple("Hiking", "🥾", if (isBengali) "হাইকিং" else "Hiking / Trekking"),
            Triple("Treadmill", "🏃‍♂️", if (isBengali) "ট্রেডমিল রানিং" else "Treadmill Run"),
            Triple("Aerobics", "🤸", if (isBengali) "অ্যারোবিক্স" else "Aerobics Workout"),
            Triple("Stretching", "🙆", if (isBengali) "স্ট্রেচিং" else "Stretching / Warm-up"),
            Triple("Stair_Climbing", "🪜", if (isBengali) "সিঁড়ি বেয়ে ওঠা" else "Stair Climbing"),
            Triple("Pilates", "🧘‍♀️", if (isBengali) "পাইলেটস সেশন" else "Pilates Session"),
            Triple("HIIT", "⚡", if (isBengali) "এইচআইআইটি" else "HIIT / Circuit Training")
        )
    }

    // Dynamic Form States
    var exerciseSearchQuery by remember { mutableStateOf("") }
    var selectedActivityId by remember { mutableStateOf("Walking") }
    var selectedIntensity by remember { mutableStateOf("moderate") } // "low", "moderate", "high"
    var durationMinutes by remember { mutableStateOf(30) }
    
    // Supporting custom activities
    var customActivityName by remember { mutableStateOf("") }
    var showCustomInput by remember { mutableStateOf(false) }

    // Filtered activities based on search query
    val filteredActivities = remember(exerciseSearchQuery, activityDatabase) {
        if (exerciseSearchQuery.isBlank()) {
            activityDatabase
        } else {
            activityDatabase.filter { (_, _, label) ->
                label.contains(exerciseSearchQuery, ignoreCase = true)
            }
        }
    }
    
    // Dynamic MET coefficients for activity types
    val metVal = remember(selectedActivityId, selectedIntensity, showCustomInput) {
        if (showCustomInput) {
            when (selectedIntensity) {
                "low" -> 2.5
                "high" -> 5.5
                else -> 4.0
            }
        } else {
            when (selectedActivityId) {
                "Walking" -> when (selectedIntensity) {
                    "low" -> 2.5
                    "high" -> 4.5
                    else -> 3.5
                }
                "Jogging" -> when (selectedIntensity) {
                    "low" -> 6.0
                    "high" -> 11.5
                    else -> 8.3
                }
                "Yoga" -> when (selectedIntensity) {
                    "low" -> 2.0
                    "high" -> 4.0
                    else -> 3.0
                }
                "Gym" -> when (selectedIntensity) {
                    "low" -> 3.0
                    "high" -> 8.0
                    else -> 5.5
                }
                "Cycling" -> when (selectedIntensity) {
                    "low" -> 4.0
                    "high" -> 10.0
                    else -> 7.0
                }
                "Swimming" -> when (selectedIntensity) {
                    "low" -> 5.0
                    "high" -> 9.5
                    else -> 7.0
                }
                "Rope_Skipping" -> when (selectedIntensity) {
                    "low" -> 8.0
                    "high" -> 12.5
                    else -> 11.0
                }
                "Soccer" -> when (selectedIntensity) {
                    "low" -> 6.0
                    "high" -> 9.0
                    else -> 7.0
                }
                "Basketball" -> when (selectedIntensity) {
                    "low" -> 4.5
                    "high" -> 8.0
                    else -> 6.5
                }
                "Badminton" -> when (selectedIntensity) {
                    "low" -> 4.0
                    "high" -> 7.0
                    else -> 5.5
                }
                "Dancing" -> when (selectedIntensity) {
                    "low" -> 3.0
                    "high" -> 7.5
                    else -> 5.0
                }
                "Hiking" -> when (selectedIntensity) {
                    "low" -> 5.0
                    "high" -> 8.0
                    else -> 6.5
                }
                "Treadmill" -> when (selectedIntensity) {
                    "low" -> 4.0
                    "high" -> 9.0
                    else -> 6.5
                }
                "Aerobics" -> when (selectedIntensity) {
                    "low" -> 4.0
                    "high" -> 7.5
                    else -> 6.0
                }
                "Stretching" -> when (selectedIntensity) {
                    "low" -> 1.5
                    "high" -> 3.0
                    else -> 2.3
                }
                "Stair_Climbing" -> when (selectedIntensity) {
                    "low" -> 4.0
                    "high" -> 8.5
                    else -> 6.0
                }
                "Pilates" -> when (selectedIntensity) {
                    "low" -> 2.5
                    "high" -> 4.0
                    else -> 3.0
                }
                "HIIT" -> when (selectedIntensity) {
                    "low" -> 7.0
                    "high" -> 12.0
                    else -> 9.0
                }
                else -> 4.0 // Custom default MET
            }
        }
    }
    
    val userWeight = userProfile.weight.coerceAtLeast(40.0)
    // Formula: METs * 3.5 * Weight / 200 * duration_in_minutes
    val liveCalorieBurn = remember(metVal, userWeight, durationMinutes) {
        val calculated = metVal * 3.5 * userWeight / 200.0 * durationMinutes
        calculated.toInt()
    }
    
    val totalBurnedToday = loggedExercises.sumOf { it.caloriesBurned }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isBengali) "শারীরিক কসরত ও ব্যায়াম (Physical Activities)" else "Physical Activities & Workout Log",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (isBengali) "আজকের কসরত ও ক্যালরি বার্ন ট্র্যাক করুন" else "Log activities to track dynamic burn balance",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🔥", fontSize = 18.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Stats summary chip
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isBengali) "আজকের মোট খরচকৃত শক্তি:" else "Total Burned Today:",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "$totalBurnedToday kcal",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = if (isBengali) "${loggedExercises.size} টি কসরত সম্পন্ন" else "${loggedExercises.size} exercise logs",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // Search Bar Text Field
            Text(
                text = if (isBengali) "ব্যায়ামের ডাটাবেস খুঁজুন:" else "Search Exercise Database:",
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            OutlinedTextField(
                value = exerciseSearchQuery,
                onValueChange = { exerciseSearchQuery = it },
                placeholder = { Text(if (isBengali) "যেমন: হাঁটা, দৌড়ানো, জিম, সাইকেল..." else "e.g., walk, jogging, gym, cycling...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search icon") },
                trailingIcon = {
                    if (exerciseSearchQuery.isNotEmpty()) {
                        IconButton(onClick = { exerciseSearchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )

            // Subtitle for picker
            Text(
                text = if (isBengali) "১. ব্যায়ামের ধরন নির্বাচন করুন (নির্বাচন করুন):" else "1. Choose Exercise & Activity:",
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Horizontal scrolling activity picker with filtered results
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filteredActivities.forEach { (id, emoji, label) ->
                    val isSelected = selectedActivityId == id && !showCustomInput
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .clickable {
                                selectedActivityId = id
                                showCustomInput = false
                            }
                            .border(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                RoundedCornerShape(12.dp)
                            )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(emoji, fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                
                // Custom Activity Option
                val isCustomSelected = showCustomInput
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCustomSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .clickable {
                            showCustomInput = true
                        }
                        .border(
                            1.dp,
                            if (isCustomSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            RoundedCornerShape(12.dp)
                        )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("✏️", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isBengali) "অন্যান্য / কাস্টম" else "Custom Activity",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isCustomSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            if (showCustomInput) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = customActivityName,
                    onValueChange = { customActivityName = it },
                    placeholder = { Text(if (isBengali) "ব্যায়ামের নাম লিখুন (যেমন: সাঁতার)" else "Enter activity name (e.g. Swimming)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Intensity & MET calculation guide
            Text(
                text = if (isBengali) "২. কসরতের তীব্রতা (Intensity Level):" else "2. Choose Intensity Level:",
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "low" to (if (isBengali) "হালকা (Low)" else "Low"),
                    "moderate" to (if (isBengali) "মাঝারি (Med)" else "Moderate"),
                    "high" to (if (isBengali) "তীব্র (High)" else "High")
                ).forEach { (intensity, label) ->
                    val isSelected = selectedIntensity == intensity
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedIntensity = intensity }
                            .border(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.secondary else Color.Transparent,
                                RoundedCornerShape(10.dp)
                            )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Duration slider with live controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isBengali) "৩. সময়সীমা (Duration):" else "3. Workout Duration:",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isBengali) "$durationMinutes মিনিট" else "$durationMinutes minutes",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Slider
            Slider(
                value = durationMinutes.toFloat(),
                onValueChange = { durationMinutes = if (it.toInt() < 5) 5 else it.toInt() },
                valueRange = 5f..180f,
                steps = 34, // intervals of 5 mins (5 to 180 is 175 span, divided by 5 is 35 intervals, so 34 intermediate steps)
                colors = SliderDefaults.colors(
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    thumbColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.fillMaxWidth()
            )
            
            // Quick duration incremeters
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(15, 30, 45, 60).forEach { mins ->
                    val isCurr = durationMinutes == mins
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { durationMinutes = mins },
                        colors = CardDefaults.cardColors(containerColor = if (isCurr) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                            Text(text = "$mins m", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quick Micro adjustments (-10, -5, +5, +10)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(-10, -5, 5, 10).forEach { diff ->
                    val sign = if (diff > 0) "+" else ""
                    FilledTonalButton(
                        onClick = { durationMinutes = (durationMinutes + diff).coerceIn(5, 180) },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 4.dp, horizontal = 2.dp)
                    ) {
                        Text(text = "$sign$diff m", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Calories Burned dynamically calculated in real time
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = if (isBengali) "আপেক্ষিক খরচকৃত ক্যালরি:" else "Calculated Dynamic Burn:",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "~ $liveCalorieBurn kcal",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFFE65100)
                        )
                    }
                    Text(
                        text = "METs: $metVal | Kg: ${userWeight.toInt()}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Scientific formula breakdown
            var showFormulaBreakdown by remember { mutableStateOf(false) }
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showFormulaBreakdown = !showFormulaBreakdown }
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📊", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isBengali) "ক্যালরি হিসাবের বৈজ্ঞানিক সূত্র" else "Scientific Formula Breakdown",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Icon(
                            imageVector = if (showFormulaBreakdown) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Toggle info",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    if (showFormulaBreakdown) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (isBengali) {
                                "ফর্মুলা: MET × ৩.৫ × ওজন (কেজি) × সময়কাল (মিনিট) / ২০০\n" +
                                "• কাজের তীব্রতা নির্দেশক MET: $metVal\n" +
                                "• আপনার প্রোফাইলের ওজন: $userWeight কেজি\n" +
                                "• নির্বাচিত সময়সীমা: $durationMinutes মিনিট\n\n" +
                                "হিসাব: $metVal × ৩.৫ × $userWeight × $durationMinutes ÷ ২০০ = $liveCalorieBurn ক্যালরি"
                            } else {
                                "Equation: MET × 3.5 × Weight (kg) × Duration (mins) / 200\n" +
                                "• Activity MET intensity score: $metVal\n" +
                                "• Personalized body weight: $userWeight kg\n" +
                                "• Chosen duration: $durationMinutes min\n\n" +
                                "Calculation: $metVal × 3.5 × $userWeight × $durationMinutes ÷ 200 = $liveCalorieBurn kcal"
                            },
                            fontSize = 10.sp,
                            lineHeight = 15.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Primary Log Action Button
            Button(
                onClick = {
                    val finalActivityName = if (showCustomInput) {
                        if (customActivityName.isNotBlank()) customActivityName else (if (isBengali) "অন্যান্য ব্যায়াম" else "Custom Activity")
                    } else {
                        selectedActivityId
                    }
                    viewModel.addExerciseLog(finalActivityName, durationMinutes, liveCalorieBurn)
                    
                    // Reset custom field if any
                    customActivityName = ""
                    showCustomInput = false
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = "log activity")
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isBengali) "কসরত লগ পেশ করুন" else "Log Active Workout",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (loggedExercises.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = if (isBengali) "আজকের লগের তালিকা:" else "Today's Activity Logs:",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    loggedExercises.reversed().forEach { workout ->
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val matchedActivObj = activityDatabase.find { it.first == workout.activity }
                                    val emoji = matchedActivObj?.second ?: "💪"
                                    val displayName = matchedActivObj?.third ?: workout.activity
                                    
                                    Text(emoji, fontSize = 20.sp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(displayName, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text(
                                            text = if (isBengali) "${workout.durationMin} মিনিট" else "${workout.durationMin} mins",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${workout.caloriesBurned} kcal",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFFC62828),
                                        modifier = Modifier.padding(end = 6.dp)
                                    )
                                    IconButton(
                                        onClick = { viewModel.deleteExerciseLog(workout.id) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete log",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WeightProgressChartCard(
    weightLogs: List<WeightLogEntity>,
    targetWeight: Double,
    isBengali: Boolean
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "ওজন পরিবর্তন গ্রাফ (Weight Chart)",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (weightLogs.size < 2) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "কমপক্ষে দুটি ওজনের তথ্য ইনপুট দিলে গ্রাফ দৃশ্যমান হবে।",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Draw canvas line chart including target line bounds if set
                val logWeights = weightLogs.map { it.weight }
                val allValues = if (targetWeight > 0.0) logWeights + targetWeight else logWeights
                val maxWeight = allValues.maxOrNull() ?: 80.0
                val minWeight = allValues.minOrNull() ?: 50.0
                val range = (maxWeight - minWeight).coerceAtLeast(1.0)

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                ) {
                    val width = size.width
                    val height = size.height
                    val paddingLeft = 40f
                    val paddingBottom = 40f
                    val chartWidth = width - paddingLeft
                    val chartHeight = height - paddingBottom

                    // Draw grids
                    val gridPaint = Color.LightGray.copy(alpha = 0.5f)
                    drawLine(gridPaint, Offset(paddingLeft, 0f), Offset(paddingLeft, chartHeight), strokeWidth = 1f)
                    drawLine(gridPaint, Offset(paddingLeft, chartHeight), Offset(width, chartHeight), strokeWidth = 1f)

                    val points = weightLogs.mapIndexed { idx, entity ->
                        val x = paddingLeft + (chartWidth / (weightLogs.size - 1)) * idx
                        // invert y
                        val pctY = (entity.weight - minWeight) / range
                        val y = chartHeight - (chartHeight * pctY).toFloat()
                        Offset(x, y)
                    }

                    // Draw Target Weight Goal horizontal dashed line
                    if (targetWeight > 0.0) {
                        val targetPctY = (targetWeight - minWeight) / range
                        val targetY = chartHeight - (chartHeight * targetPctY).toFloat()
                        
                        drawLine(
                            color = Color(0xFFFF9800),
                            start = Offset(paddingLeft, targetY),
                            end = Offset(width, targetY),
                            strokeWidth = 3f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                    }

                    // Stroke Path for logs
                    val path = Path().apply {
                        points.forEachIndexed { i, pt ->
                            if (i == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
                        }
                    }

                    drawPath(
                        path = path,
                        color = Color(0xFF4CAF50),
                        style = Stroke(width = 4f)
                    )

                    // Draw reference dots
                    points.forEach { pt ->
                        drawCircle(color = Color(0xFF2E7D32), radius = 6f, center = pt)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Chart Legends and metadata
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp, 4.dp)
                                .background(Color(0xFF4CAF50))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isBengali) "ওজনের অগ্রগতি" else "Weight Logs",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (targetWeight > 0.0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp, 2.dp)
                                    .background(Color(0xFFFF9800))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = (if (isBengali) "লক্ষ্য রেখা: " else "Goal Target: ") + "$targetWeight kg",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE65100)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// TAB 2: REMINDERS & NOTIFICATIONS ALARMS
// ==========================================
@Composable
fun RemindersTab(
    viewModel: DietPlannerViewModel,
    reminders: List<MealReminderEntity>,
    context: Context
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "খাবার ও পানি পানের রিমাইন্ডার (Reminders)",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Text(
            text = "নির্দিষ্ট সময়ে সিস্টেম এলার্ম রিমাইন্ডার টগল করুন। এলার্ম বাজলে নোটিফিকেশন দেয়া হবে।",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (reminders.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            reminders.forEach { reminder ->
                ReminderConfigRow(
                    reminder = reminder,
                    onToggleEnabled = { isEnabled ->
                        viewModel.updateReminderTime(
                            context = context,
                            id = reminder.id,
                            name = reminder.name,
                            hour = reminder.hour,
                            minute = reminder.minute,
                            isEnabled = isEnabled
                        )
                    },
                    onTimeChanged = { hour, min ->
                        viewModel.updateReminderTime(
                            context = context,
                            id = reminder.id,
                            name = reminder.name,
                            hour = hour,
                            minute = min,
                            isEnabled = reminder.isEnabled
                        )
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun ReminderConfigRow(
    reminder: MealReminderEntity,
    onToggleEnabled: (Boolean) -> Unit,
    onTimeChanged: (Int, Int) -> Unit
) {
    var showTimePicker by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = reminder.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = String.format("%02d:%02d", reminder.hour, reminder.minute),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    modifier = Modifier.clickable { showTimePicker = true }
                )
                Text(
                    text = "সময় পরিবর্তন করতে ক্লিক করুন",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Switch(
                checked = reminder.isEnabled,
                onCheckedChange = onToggleEnabled
            )
        }
    }

    if (showTimePicker) {
        // Simple manual input dialog for maximum compatibility on the container compiler
        var hStr by remember { mutableStateOf(reminder.hour.toString()) }
        var mStr by remember { mutableStateOf(reminder.minute.toString()) }

        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("রিমাইন্ডার সময় নির্বাচন (Time)") },
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = hStr,
                        onValueChange = { hStr = it },
                        modifier = Modifier.width(70.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Text(" : ", fontWeight = FontWeight.Bold, fontSize = 24.sp, modifier = Modifier.padding(horizontal = 8.dp))
                    OutlinedTextField(
                        value = mStr,
                        onValueChange = { mStr = it },
                        modifier = Modifier.width(70.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val h = hStr.toIntOrNull()?.coerceIn(0, 23) ?: reminder.hour
                    val m = mStr.toIntOrNull()?.coerceIn(0, 59) ?: reminder.minute
                    onTimeChanged(h, m)
                    showTimePicker = false
                }) {
                    Text("সংরক্ষণ")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("বাতিল")
                }
            }
        )
    }
}

// ==========================================
// TAB 3: PROFILE VIEWS
// ==========================================
@Composable
fun ProfileEditTab(
    userProfile: UserProfileEntity,
    isBengali: Boolean,
    onNavigateToHealthPrefs: () -> Unit,
    onSave: (Int, String, Double, Double, String, String, String, String, String) -> Unit
) {
    var age by remember { mutableStateOf(userProfile.age.toString()) }
    var weight by remember { mutableStateOf(userProfile.weight.toString()) }
    var height by remember { mutableStateOf(userProfile.height.toString()) }
    var selectedGender by remember { mutableStateOf(userProfile.gender) }
    var selectedGoal by remember { mutableStateOf(userProfile.goal) }
    var selectedPref by remember { mutableStateOf(userProfile.dietaryPreference) }
    var allergies by remember { mutableStateOf(userProfile.allergies) }
    var medicalConditions by remember { mutableStateOf(userProfile.medicalConditions) }
    var cuisinePreferences by remember { mutableStateOf(userProfile.cuisinePreferences) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "আমার শারীরিক প্রোফাইল (My Profile)",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Health & Cuisine Preferences Clickable Settings Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .clickable { onNavigateToHealthPrefs() }
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("🩺", fontSize = 24.sp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isBengali) "স্বাস্থ্য ও স্বাদের পছন্দসমূহ" else "Health & Taste Settings",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = if (isBengali) "মেডিকেল জটিলতা ও রান্নার ধরণ ম্যানেজ করুন" else "Manage medical conditions and cuisines",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = age,
                    onValueChange = { age = it },
                    label = { Text("বয়স (Age)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = { Text("ওজন কেজি (Weight in KG)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = height,
                    onValueChange = { height = it },
                    label = { Text("উচ্চতা সেমি (Height in CM)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Gender Options dropdown simulation with Row buttons
                Text("লিঙ্গ (Gender):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val genders = listOf("Male", "Female", "Other")
                    genders.forEach { g ->
                        val sel = selectedGender.lowercase() == g.lowercase()
                        Button(
                            onClick = { selectedGender = g },
                            colors = ButtonColors(
                                containerColor = if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                contentColor = if (sel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                disabledContainerColor = Color.Gray,
                                disabledContentColor = Color.DarkGray
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(g)
                        }
                    }
                }

                // Goal Selector simulation
                Text("শরীরচর্চার লক্ষ্য (Fitness Goal):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                val goals = listOf("Weight Loss", "Weight Gain", "Maintain")
                goals.forEach { g ->
                    val sel = selectedGoal == g
                    OutlinedButton(
                        onClick = { selectedGoal = g },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (sel) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(g)
                    }
                }

                // Preference dropdown simulation
                Text("খাদ্য পছন্দ (Dietary Preference):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                val prefs = listOf("Vegetarian", "Non-Vegetarian", "Vegan")
                prefs.forEach { p ->
                    val sel = selectedPref == p
                    OutlinedButton(
                        onClick = { selectedPref = p },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (sel) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(p)
                    }
                }

                OutlinedTextField(
                    value = allergies,
                    onValueChange = { allergies = it },
                    label = { Text(if (isBengali) "অ্যালার্জি থাকলে লিখুন (Allergies)" else "Any food allergies?") },
                    modifier = Modifier.fillMaxWidth()
                )

                HealthAndPreferenceComponent(
                    isBengali = isBengali,
                    medicalConditions = medicalConditions,
                    cuisinePreferences = cuisinePreferences,
                    onMedicalConditionsChanged = { medicalConditions = it },
                    onCuisinePreferencesChanged = { cuisinePreferences = it }
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        val parsedAge = age.toIntOrNull() ?: 0
                        val parsedWeight = weight.toDoubleOrNull() ?: 0.0
                        val parsedHeight = height.toDoubleOrNull() ?: 0.0

                        if (parsedAge > 0 && parsedWeight > 0 && parsedHeight > 0) {
                            onSave(parsedAge, selectedGender, parsedWeight, parsedHeight, selectedGoal, selectedPref, allergies, medicalConditions, cuisinePreferences)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("সংরক্ষণ করুন")
                }
            }
        }
    }
}

@Composable
fun ProfileSetupView(
    isBengali: Boolean,
    onSave: (Int, String, Double, Double, String, String, String, String, String) -> Unit
) {
    var age by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var selectedGender by remember { mutableStateOf("Male") }
    var selectedGoal by remember { mutableStateOf("Weight Loss") }
    var selectedPref by remember { mutableStateOf("Non-Vegetarian") }
    var allergies by remember { mutableStateOf("") }
    var medicalConditions by remember { mutableStateOf("") }
    var cuisinePreferences by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        ANEXSOPZModernLogo(
            modifier = Modifier.size(96.dp),
            showText = false,
            isBengali = isBengali
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = if (isBengali) "ANEXSOPZ ডায়েট প্ল্যানারে স্বাগতম! 🙌" else "Welcome to ANEXSOPZ Diet Planner! 🙌",
            fontWeight = FontWeight.Black,
            fontSize = 20.sp,
            textAlign = TextAlign.Center,
            color = Color(0xFF1E5E2F)
        )
        Text(
            text = if (isBengali) 
                "আপনার শরীর ও স্বাস্থ্যের লক্ষ্য অনুযায়ী কাস্টম ডায়েট প্ল্যান সাজাতে নিচের সুনির্দিষ্ট তথ্যগুলো প্রদান করুন।"
            else
                "To personalize your tailored lifestyle diet, please supply your specific metrics below.",
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = age,
                    onValueChange = { age = it },
                    label = { Text(if (isBengali) "আপনার বয়স (Age in Years)" else "Your Age (Years)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = { Text(if (isBengali) "আপনার ওজন কেজি (Weight in KG)" else "Your Weight (KG)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = height,
                    onValueChange = { height = it },
                    label = { Text(if (isBengali) "আপনার উচ্চতা সেমি (Height in CM)" else "Your Height (CM)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(6.dp))
                // Gender Buttons
                Text(
                    text = if (isBengali) "লিঙ্গ (Gender):" else "Gender:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Male", "Female", "Other").forEach { g ->
                        val sel = selectedGender == g
                        val genderLabel = when (g) {
                            "Male" -> if (isBengali) "পুরুষ (Male)" else "Male"
                            "Female" -> if (isBengali) "নারী (Female)" else "Female"
                            else -> if (isBengali) "অন্যান্য (Other)" else "Other"
                        }
                        Button(
                            onClick = { selectedGender = g },
                            colors = ButtonColors(
                                containerColor = if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                contentColor = if (sel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                disabledContainerColor = Color.Gray,
                                disabledContentColor = Color.DarkGray
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(genderLabel, fontSize = 11.sp, maxLines = 1)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                // Goal Selector Buttons
                Text(
                    text = if (isBengali) "ডায়েটের মূল লক্ষ্য (Goal):" else "Diet Goal:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                listOf("Weight Loss", "Weight Gain", "Maintain").forEach { g ->
                    val sel = selectedGoal == g
                    val goalLabel = when (g) {
                        "Weight Loss" -> if (isBengali) "ওজন কমানো (Weight Loss)" else "Weight Loss"
                        "Weight Gain" -> if (isBengali) "ওজন বাড়ানো (Weight Gain)" else "Weight Gain"
                        else -> if (isBengali) "ওজন নিয়ন্ত্রণ (Maintain)" else "Maintain"
                    }
                    androidx.compose.material3.OutlinedButton(
                        onClick = { selectedGoal = g },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (sel) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(goalLabel, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                // Preference Selection
                Text(
                    text = if (isBengali) "খাদ্য পছন্দ (Dietary Preference):" else "Dietary Preference:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                listOf("Vegetarian", "Non-Vegetarian", "Vegan").forEach { p ->
                    val sel = selectedPref == p
                    val prefLabel = when (p) {
                        "Vegetarian" -> if (isBengali) "নিরামিষাষী (Vegetarian)" else "Vegetarian"
                        "Non-Vegetarian" -> if (isBengali) "আমিষাষী (Non-Vegetarian)" else "Non-Vegetarian"
                        else -> if (isBengali) "ভেগান (Vegan)" else "Vegan"
                    }
                    androidx.compose.material3.OutlinedButton(
                        onClick = { selectedPref = p },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (sel) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(prefLabel, fontSize = 12.sp)
                    }
                }

                OutlinedTextField(
                    value = allergies,
                    onValueChange = { allergies = it },
                    label = { Text(if (isBengali) "কোনো খাবারে অ্যালার্জি থাকলে লিখুন" else "Any food allergies?") },
                    modifier = Modifier.fillMaxWidth()
                )

                HealthAndPreferenceComponent(
                    isBengali = isBengali,
                    medicalConditions = medicalConditions,
                    cuisinePreferences = cuisinePreferences,
                    onMedicalConditionsChanged = { medicalConditions = it },
                    onCuisinePreferencesChanged = { cuisinePreferences = it }
                )

                errorMsg?.let {
                    Text(text = it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        val parsedAge = age.toIntOrNull() ?: 0
                        val parsedWeight = weight.toDoubleOrNull() ?: 0.0
                        val parsedHeight = height.toDoubleOrNull() ?: 0.0

                        if (parsedAge <= 0 || parsedWeight <= 0 || parsedHeight <= 0) {
                            errorMsg = if (isBengali) "দয়া করে বয়স, ওজন ও উচ্চতার সঠিক সংখ্যা প্রদান করুন।" else "Please enter valid positive numbers for age, weight, and height."
                        } else {
                            errorMsg = null
                            val med = if (medicalConditions.isBlank()) "None" else medicalConditions
                            val cuis = if (cuisinePreferences.isBlank()) "Bengali" else cuisinePreferences
                            onSave(parsedAge, selectedGender, parsedWeight, parsedHeight, selectedGoal, selectedPref, allergies, med, cuis)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isBengali) "আমার প্রোফাইল বানান" else "Create My Profile")
                }
            }
        }
    }
}

@Composable
fun OutlinedButton(
    onClick: () -> Unit,
    bgSelector: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (bgSelector) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
        ),
        modifier = modifier,
        content = content
    )
}

// ==========================================
// UTILITIES AND HELPERS
// ==========================================
fun adjustDateString(dateStr: String, daysToAdd: Int): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val date = sdf.parse(dateStr) ?: Date()
    val cal = Calendar.getInstance()
    cal.time = date
    cal.add(Calendar.DATE, daysToAdd)
    return sdf.format(cal.time)
}

fun formatBanglaDate(dateStr: String): String {
    try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = sdf.parse(dateStr) ?: return dateStr
        val outputFormat = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("bn", "BD"))
        return outputFormat.format(date)
    } catch (e: Exception) {
        return dateStr
    }
}

fun shareMealPlanToSocial(context: Context, plan: MealPlanEntity) {
    val textMessage = """
        ANEXSOPZ Diet Planner (বাংলাদেশ) 
        তারিখ: ${plan.date} (ক্যালোরি লক্ষ্য: ${plan.calorieTarget} kcal)
        
        🍳 সকালের নাস্তা (Breakfast):
        ${plan.breakfast}
        
        🍿 সকালের হালকা খাবার (Snack 1):
        ${plan.snack1}
        
        🍛 দুপুরের খাবার (Lunch):
        ${plan.lunch}
        
        🍵 বিকালের হালকা খাবার (Snack 2):
        ${plan.snack2}
        
        🍗 রাতের খাবার (Dinner):
        ${plan.dinner}
        
        💡 স্বাস্থ্য তথ্য (Tip):
        ${plan.dailyTip}
        
        আপনার জন্য চমৎকার ডায়েট প্ল্যান তৈরি করুন সহজেই।
    """.trimIndent()

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "My Personal Diet Meal Plan")
        putExtra(Intent.EXTRA_TEXT, textMessage)
    }
    context.startActivity(Intent.createChooser(intent, "ডায়েট প্ল্যান শেয়ার করুন"))
}

fun sharePdf(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "com.aistudio.aidietplanner.pqrsxz.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "পিডিএফ রিপোর্ট শেয়ার করুন"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

// ==========================================
// CUSTOM MADD-ONS COMPOSABLES (SUVECHA)
// ==========================================

@Composable
fun ANEXSOPZAppDescriptionCard(isBengali: Boolean) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🛡️", fontSize = 20.sp)
                }
                Column {
                    Text(
                        text = if (isBengali) "ANEXSOPZ হেলথ প্লাস" else "ANEXSOPZ Health Plus",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (isBengali) "আপনার অনন্য ডায়েট ও লাইফস্টাইল সঙ্গী" else "Your Smart Healthy Lifestyle Companion",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = if (isBengali) {
                    "সুস্থ জীবনযাপনের এক নতুন দিগন্ত! ANEXSOPZ অ্যাপটি আপনাকে প্রতিদিনের পুষ্টিমান পরিমাপ, শরীরের প্রয়োজন অনুযায়ী সুষম ডায়েট চার্ট তৈরি, সঠিক সময়ে পানি পানের নোটিফিকেশন, ক্যালোরি বার্ন করার ডায়েরি এবং ওপেন ফুড ফ্যাক্টস এপিআই নির্ভর বারকোড স্ক্যানার দিয়ে সর্বদা রাখবে সুস্থ ও সতেজ।"
                } else {
                    "Experience a synchronized path to longevity! ANEXSOPZ empowers you with custom calorie & nutrient targets, automatic daily diet plans, push reminder configurations, hydration logs, exercise calorie logging, and on-the-go packaged food verification."
                },
                fontSize = 12.sp,
                lineHeight = 18.sp,
                textAlign = TextAlign.Justify,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ANEXSOPZNotificationDialog(
    isBengali: Boolean,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF1E5E2F))
            ) {
                Text(if (isBengali) "বন্ধ করুন" else "Close", fontWeight = FontWeight.Bold)
            }
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("🔔", fontSize = 22.sp)
                Text(
                    text = if (isBengali) "হেলথ নোটিফিকেশন" else "Daily Health Alerts",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF1E5E2F)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val list = remember(isBengali) {
                    listOf(
                        "⏰" to (if (isBengali) "পানি পানের সময়: আপনার হাইড্রেশন লেভেল ঠিক রাখতে এক গ্লাস বিশুদ্ধ পানি পান করুন।" else "Hydro Reminder: Have a glass of water now to stay fully active!"),
                        "🍲" to (if (isBengali) "দুপুরের চমৎকার ডায়েট: আজকের দুপুরের সুষম খাদ্যতালিকা অ্যাপ থেকে চেক করে নিন!" else "Balanced Meal: Check out your healthy lunch recommendations inside ANEXSOPZ app!"),
                        "🏃" to (if (isBengali) "ব্যায়াম এলার্ট: সুস্থ থাকতে অন্তত ২০ মিনিট হাঁটা শুরু করুন এবং ক্যালোরি কমান।" else "Workout Alert: Keep moving! Start a quick 20-min walking session to reach your target.")
                    )
                }

                list.forEach { (emoji, desc) ->
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(emoji, fontSize = 20.sp)
                            Text(
                                text = desc,
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                                color = Color.DarkGray
                            )
                        }
                    }
                }
            }
        },
        shape = RoundedCornerShape(18.dp),
        containerColor = Color.White
    )
}

@Composable
fun ANEXSOPZLoginScreen(
    isBengali: Boolean,
    onLogin: (String, String, (Boolean, String?) -> Unit) -> Unit,
    onSignUp: (String, String, (Boolean, String?) -> Unit) -> Unit
) {
    // SignUp state variables
    var fullName by remember { mutableStateOf("") }
    var nickName by remember { mutableStateOf("") }
    var emailAddress by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var signUpPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isCheckedTerms by remember { mutableStateOf(false) }

    // Login state variables
    var loginEmailOrPhone by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }

    var errorMessage by remember { mutableStateOf("") }
    var successMessage by remember { mutableStateOf("") }
    var isSignUpMode by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var showTermsAndConditionsByClick by remember { mutableStateOf(false) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFE8F5E9), // Clean organic mint
                        Color(0xFFC8E6C9),
                        Color(0xFFF1F8E9)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Modern Animated Custom Corporate Logo (leaf of unique tree)
                ANEXSOPZModernLogo(
                    modifier = Modifier.size(72.dp),
                    showText = false,
                    isBengali = false
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "ANEXSOPZ Health Plus",
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp,
                    color = Color(0xFF1E5E2F),
                    letterSpacing = 0.5.sp
                )

                Text(
                    text = if (isSignUpMode) "Register for a new health account" else "Secure access to your healthy lifestyle",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF43A047),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 2.dp)
                )

                Divider(modifier = Modifier.padding(vertical = 16.dp), color = Color(0xFFE0E0E0))

                if (isSignUpMode) {
                    // --- SIGN UP INTERFACE (English only) ---
                    
                    // Full Name
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Full Name") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF2E7D32),
                            focusedLabelColor = Color(0xFF2E7D32)
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Nick Name (optional)
                    OutlinedTextField(
                        value = nickName,
                        onValueChange = { nickName = it },
                        label = { Text("Nick Name (optional)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF2E7D32),
                            focusedLabelColor = Color(0xFF2E7D32)
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Email Address
                    OutlinedTextField(
                        value = emailAddress,
                        onValueChange = { emailAddress = it },
                        label = { Text("Email Address") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF2E7D32),
                            focusedLabelColor = Color(0xFF2E7D32)
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Phone Number
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        label = { Text("Phone Number") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF2E7D32),
                            focusedLabelColor = Color(0xFF2E7D32)
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Password
                    OutlinedTextField(
                        value = signUpPassword,
                        onValueChange = { signUpPassword = it },
                        label = { Text("Password") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF2E7D32),
                            focusedLabelColor = Color(0xFF2E7D32)
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Confirm Password
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirm Password") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF2E7D32),
                            focusedLabelColor = Color(0xFF2E7D32)
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // "I agree to Terms & Conditions" Checkbox & Details Link
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = isCheckedTerms,
                            onCheckedChange = { isCheckedTerms = it },
                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFF2E7D32))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Row {
                            Text(
                                text = "I agree to the ",
                                fontSize = 12.sp,
                                color = Color.DarkGray
                            )
                            Text(
                                text = "Terms & Conditions",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E5E2F),
                                modifier = Modifier.clickable {
                                    showTermsAndConditionsByClick = true
                                }
                            )
                        }
                    }

                } else {
                    // --- LOGIN INTERFACE (English only) ---
                    
                    // Email or Phone input option
                    OutlinedTextField(
                        value = loginEmailOrPhone,
                        onValueChange = { loginEmailOrPhone = it },
                        label = { Text("Email or Phone Number") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF2E7D32),
                            focusedLabelColor = Color(0xFF2E7D32)
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Password input
                    OutlinedTextField(
                        value = loginPassword,
                        onValueChange = { loginPassword = it },
                        label = { Text("Password") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF2E7D32),
                            focusedLabelColor = Color(0xFF2E7D32)
                        )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Forgotten Password option
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Text(
                            text = "Forgot Password?",
                            color = Color(0xFF1E5E2F),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clickable {
                                    showForgotPasswordDialog = true
                                }
                                .padding(vertical = 4.dp)
                        )
                    }
                }

                if (errorMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        color = Color(0xFFD32F2F),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }

                if (successMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = successMessage,
                        color = Color(0xFF2E7D32),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color(0xFF2E7D32),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    // Action click button
                    Button(
                        onClick = {
                            if (isSignUpMode) {
                                // SignUp validations
                                if (fullName.isBlank() || emailAddress.isBlank() || phoneNumber.isBlank() || signUpPassword.isBlank() || confirmPassword.isBlank()) {
                                    errorMessage = "Please fill in all required fields!"
                                    successMessage = ""
                                } else if (!emailAddress.contains("@") || !emailAddress.contains(".")) {
                                    errorMessage = "Please enter a valid email address!"
                                    successMessage = ""
                                } else if (signUpPassword.length < 6) {
                                    errorMessage = "Password must be at least 6 characters!"
                                    successMessage = ""
                                } else if (signUpPassword != confirmPassword) {
                                    errorMessage = "Passwords do not match!"
                                    successMessage = ""
                                } else if (!isCheckedTerms) {
                                    errorMessage = "You must agree to the Terms and Conditions!"
                                    successMessage = ""
                                } else {
                                    errorMessage = ""
                                    successMessage = ""
                                    isLoading = true
                                    
                                    // Simulated storage callback
                                    onSignUp(emailAddress, signUpPassword) { success, error ->
                                        isLoading = false
                                        if (success) {
                                            successMessage = "Account registered successfully! Please sign in."
                                            loginEmailOrPhone = emailAddress
                                            loginPassword = signUpPassword
                                            isSignUpMode = false // Switch to login screen as requested
                                        } else {
                                            errorMessage = error ?: "Registration failed."
                                        }
                                    }
                                }
                            } else {
                                // Login validations
                                if (loginEmailOrPhone.isBlank() || loginPassword.isBlank()) {
                                    errorMessage = "Please enter both Email/Phone and Password!"
                                    successMessage = ""
                                } else {
                                    errorMessage = ""
                                    successMessage = ""
                                    isLoading = true
                                    
                                    onLogin(loginEmailOrPhone, loginPassword) { success, error ->
                                        isLoading = false
                                        if (!success) {
                                            errorMessage = error ?: "Authentication failed."
                                        }
                                    }
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(
                            text = if (isSignUpMode) "Register" else "Sign In",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Text toggle mode click "Don't have an account? Sign Up" or "Already have an account? Sign In"
                    Text(
                        text = if (isSignUpMode) "Already have an account? Sign In" else "Don't have an account? Sign Up",
                        color = Color(0xFF1E5E2F),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable {
                                isSignUpMode = !isSignUpMode
                                errorMessage = ""
                                successMessage = ""
                            }
                            .padding(vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Guest Quick Entrance Card for easier user evaluation
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                errorMessage = ""
                                successMessage = ""
                                isLoading = true
                                onLogin("user@anexsopz.com", "password123") { success, error ->
                                    isLoading = false
                                    if (!success) {
                                        errorMessage = error ?: "Demo log in failed"
                                    }
                                }
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9)),
                        border = BorderStroke(1.dp, Color(0xFFDCEDC8))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text("👋", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Evaluation: Quick Demo Login",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = Color(0xFF33691E)
                            )
                        }
                    }
                }
            }
        }
    }

    // Terms and Conditions Dialog Details
    if (showTermsAndConditionsByClick) {
        ANEXSOPZTermsDialog(
            isBengali = false,
            onDismiss = { showTermsAndConditionsByClick = false }
        )
    }

    // Simulated Forgot Password Dialog
    if (showForgotPasswordDialog) {
        var recoveryContact by remember { mutableStateOf("") }
        var resetSuccess by remember { mutableStateOf(false) }

        androidx.compose.ui.window.Dialog(onDismissRequest = { showForgotPasswordDialog = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🔒 Password Recovery",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF1E5E2F)
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    if (!resetSuccess) {
                        Text(
                            text = "Enter your registered Email or Phone number to retrieve your simulated password reset instructions.",
                            fontSize = 12.sp,
                            color = Color.DarkGray,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        OutlinedTextField(
                            value = recoveryContact,
                            onValueChange = { recoveryContact = it },
                            label = { Text("Email or Phone") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = { showForgotPasswordDialog = false },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Cancel")
                            }
                            Button(
                                onClick = {
                                    if (recoveryContact.isNotBlank()) {
                                        resetSuccess = true
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                            ) {
                                Text("Reset")
                            }
                        }
                    } else {
                        Text(
                            text = "Demo Instructions Sent! ✨",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF2E7D32)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "For demo purposes, use our active account: \n\nEmail: user@anexsopz.com\nPassword: password123",
                            fontSize = 12.sp,
                            color = Color.DarkGray,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        Button(
                            onClick = { 
                                loginEmailOrPhone = "user@anexsopz.com"
                                loginPassword = "password123"
                                showForgotPasswordDialog = false 
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Auto-fill Credentials")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ANEXSOPZSearchDialog(
    isBengali: Boolean,
    onDismiss: () -> Unit,
    onNavigateToTab: (Int) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    // Search Items Data Structure
    val searchItems = remember(isBengali) {
        listOf(
            Triple(
                if (isBengali) "ভাত (Rice) - শর্করা ও ক্যালোরি" else "Rice (Vat) - Heavy Carbs",
                if (isBengali) "১ কাপ সাদা ভাতে প্রায় ২০০ ক্যালোরি থাকে। পরিমিত ডায়েটে লাল চালের ভাত খাওয়া বেশি উপকারী যা ফাইবার সমৃদ্ধ।" else "1 cup of regular boiled rice contains ~200 calories. Brown rice is a healthier alternative with lots of food fiber.",
                listOf("rice", "ভাত", "bhat", "chaol", "carb", "heavy")
            ),
            Triple(
                if (isBengali) "ডিম (Egg) - উচ্চ মানের প্রোটিন" else "Egg (Dim) - Fast Proteins",
                if (isBengali) "একটি মাঝারি সেদ্ধ ডিমে প্রায় ৭০-৮০ ক্যালোরি এবং ৬ গ্রাম খাঁটি প্রোটিন থাকে। ওজন নিয়ন্ত্রণের জন্য আদর্শ।" else "A standard hard-boiled egg contains only 70-80 calories and 6g of pure protein. Great for healthy fitness.",
                listOf("egg", "ডিম", "dim", "boiled", "protein")
            ),
            Triple(
                if (isBengali) "পানি পান (Water) - মেটাবলিজম বৃদ্ধি" else "Water Tracker - Hydration Vitals",
                if (isBengali) "দৈনিক ৮-১০ গ্লাস বা ২.৫ - ৩ লিটার বিশুদ্ধ পানি পান করা আবশ্যক। হাইড্রেশন লেভেল ট্র্যাকারটি ট্র্যাকার ট্যাবে পাবেন।" else "An adult should consume 2.5 to 3 liters of water per day. Find the Water Intake Log within the Tracker tab.",
                listOf("water", "पानी", "পানি", "pani", "glass", "log", "tracker")
            ),
            Triple(
                if (isBengali) "ওজন নিয়ন্ত্রণ ও বৃদ্ধি (Weight Loss / Gain)" else "Weight Progress - Loss & Gain Tracker",
                if (isBengali) "আপনার ওজন কমানো বা বাড়ানোর অগ্রগতি চার্ট সহ দেখতে চান? নিচে মাঝের ট্র্যাকার ট্যাবটি বেছে নিন।" else "Want to monitor your weights progression visually via graphs? Find the interactive charts in the Tracker tab.",
                listOf("weight", "loss", "gain", "ওজন", "body", "fat", "bmi")
            ),
            Triple(
                if (isBengali) "স্মার্ট ডায়েট প্ল্যান (Smart Food Planner)" else "Smart Diet Generator - Custom Meals",
                if (isBengali) "নতুন স্বাস্থ্যকর সুষম খাবারের তালিকা পেতে ড্যাশবোর্ডের খাবার ট্যাবের ভাসমান 'সুষম খাবার' বাটনটি ক্লিক করুন।" else "Generate a newly optimized daily dietary schedule with the custom planner. Just tap the green AutoAwesome floating button on Meals tab.",
                listOf("diet", "meal", "খাবার", "ডায়েট", "চার্ট", "food")
            ),
            Triple(
                if (isBengali) "সবজি ও ফাইবারের উপকারিতা" else "Vegetables & Greens - Healthy Fiber",
                if (isBengali) "সবুজ শাকসবজিতে প্রচুর ফাইবার ও ভিটামিন সি থাকে যা হজমশক্তি বাড়ায় এবং দীর্ঘসময় পেট ভরা রাখতে সাহায্য করে।" else "Fresh green vegetables contain high dietary fiber, promoting healthy gut microflora and supporting digestion efficiency.",
                listOf("vegetable", "সবজি", "sabji", "sak", "veg", "green")
            ),
            Triple(
                if (isBengali) "রুটি (Roti/Bread) - সুষম কার্ব" else "Roti / Wheat Toast - Balanced Carbs",
                if (isBengali) "মাঝারি লাল আটার রুটিতে মাত্র ৭০-৮০ ক্যালোরি থাকে। এটি দীর্ঘস্থায়ী মেটাবলিক শক্তি যোগায় এবং ভাতের দারুণ বিকল্প।" else "One whole wheat roti contains ~70-80 calories. It is rich in complex carbohydrates and makes an excellent alternative to white rice.",
                listOf("roti", "bread", "রুটি", "wheat", "low carb")
            ),
            Triple(
                if (isBengali) "শারীরিক কসরত (Regular Workout)" else "Physical Exercise - Burn Vitals",
                if (isBengali) "সহজ মেদ ঝরাতে ও মানসিক সজীবতায় দৈনিক অন্তত ২৫ মিনিট কার্ডিও বা দ্রুত হাঁটার অভ্যাস করুন। ট্র্যাকার ট্যাব দেখুন।" else "Do brisk walking or moderate cardio for 25-30 mins daily to improve cardiovascular strength. Explore more inside the Tracker Tab.",
                listOf("exercise", "workout", "ব্যায়াম", "walk", "হাঁটা", "run", "fitness")
            ),
            Triple(
                if (isBengali) "ক্যালোরি হিসাবের টুলস (Calorie Burner Tool)" else "Calorie Tools - Calculator Tools",
                if (isBengali) "কোন খাবার খেলে কত ক্যালোরি বার্ন দরকার তা হিসাব করার জন্য অ্যাপের টুলস ট্যাবের ফুড-বার্ন ক্যালকুলেটরটি দেখুন।" else "Check out the food-calories burn calculator and calculators inside the dedicated Tools tab of the ANEXSOPZ app.",
                listOf("tools", "টুলস", "calorie", "khalori", "burn", "counter")
            ),
            Triple(
                if (isBengali) "কলা (Banana) - তাৎক্ষণিক শক্তি" else "Banana (Kola) - Instant Energy boost",
                if (isBengali) "১টি কলায় প্রায় ১০৫ ক্যালোরি পাওয়া যায়। এটি পটাসিয়াম সমৃদ্ধ হওয়ায় পেশীর কর্মক্ষমতা বাড়াতে দারুন উপকারী।" else "One medium banana yields about 105 calories and is highly rich in potassium which helps avoid blood pressure fluctuations.",
                listOf("banana", "kola", "কলা", "energy", "fruit")
            ),
            Triple(
                if (isBengali) "আপেল (Apple) - ডায়েট ফ্রুট" else "Apple (Apel) - Weight-loss Fruit",
                if (isBengali) "মাঝারি আপেলে ৯০ ক্যালোরি থাকে। এর দ্রবণীয় আঁশ ‘পেকটিন’ রক্তের কোলেস্টেরল মাত্রা কমাতে সক্রিয় ভূমিকা রাখে।" else "One apple contains ~90 calories and provides amazing soluble fibers (pectin) that significantly support balanced heart health.",
                listOf("apple", "apel", "আপেল", "fruit", "vitamin")
            ),
            Triple(
                if (isBengali) "দুধ (Milk) - ক্যালসিয়াম উৎস" else "Milk (Dudh) - Strong Calcium Sources",
                if (isBengali) "বিনা চর্বির ১ গ্লাস দুধে প্রায় ৮৫ ক্যালোরি থাকে যা আমাদের সুস্থ দাঁত ও হাড়ের ক্যালসিয়ামের দৈনন্দিন চাহিদা পূরণ করে।" else "One glass of skimmed/low-fat milk provides 85 calories and high-quality bio-available calcium for strong skeletons.",
                listOf("milk", "dudh", "দুধ", "calcium", "bones")
            )
        )
    }

    val filteredItems = remember(searchQuery, isBengali) {
        if (searchQuery.trim().isEmpty()) {
            searchItems
        } else {
            val query = searchQuery.trim().lowercase(Locale.ROOT)
            searchItems.filter { item ->
                item.first.lowercase(Locale.ROOT).contains(query) ||
                item.second.lowercase(Locale.ROOT).contains(query) ||
                item.third.any { synonym -> synonym.lowercase(Locale.ROOT).contains(query) }
            }
        }
    }

    val suggestions = remember(isBengali) {
        if (isBengali) {
            listOf("ভাত", "ডিম", "পানি", "ওজন", "সবজি", "ব্যায়াম", "ক্যালোরি", "কলা")
        } else {
            listOf("Rice", "Egg", "Water", "Weight", "Vegetable", "Workout", "Tools", "Banana")
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF1E5E2F))
            ) {
                Text(if (isBengali) "বন্ধ করুন" else "Close", fontWeight = FontWeight.Bold)
            }
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("🔎", fontSize = 20.sp)
                Text(
                    text = if (isBengali) "খাদ্য ও স্বাস্থ্য অনুসন্ধান" else "ANEXSOPZ Health Search",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF1E5E2F)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Search Input Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { 
                        Text(
                            text = if (isBengali) "কি খুঁজতে চান টাইপ করুন..." else "Search foods, calories, tips...",
                            fontSize = 13.sp
                        ) 
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF2E7D32),
                        focusedLabelColor = Color(0xFF2E7D32),
                        unfocusedBorderColor = Color.LightGray
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick suggestion tags row
                Text(
                    text = if (isBengali) "জনপ্রিয় অনুসন্ধান:" else "Quick Suggestions:",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    suggestions.forEach { tag ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFE8F5E9))
                                .clickable { searchQuery = tag }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = tag,
                                fontSize = 11.sp,
                                color = Color(0xFF1E5E2F),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Divider(color = Color(0xFFEEEEEE), modifier = Modifier.padding(vertical = 4.dp))

                // Scrollable Search list suggestions
                if (filteredItems.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("🕵️", fontSize = 28.sp)
                        Text(
                            text = if (isBengali) "দুঃখিত, কোনো ফলাফল পাওয়া যায়নি!" else "No health info found!",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Gray
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(filteredItems) { item ->
                            
                            // Check if redirect helper is available
                            val targetTab = when {
                                item.first.contains("ভাত") || item.first.contains("ডিম") || item.first.contains("সবজি") || item.first.contains("রুটি") || item.first.contains("কলা") || item.first.contains("আপেল") || item.first.contains("দুধ") || item.first.contains("Rice") || item.first.contains("Egg") || item.first.contains("Vegetable") || item.first.contains("Roti") || item.first.contains("Banana") || item.first.contains("Apple") || item.first.contains("Milk") -> 1
                                item.first.contains("পানি") || item.first.contains("ওজন") || item.first.contains("কসরত") || item.first.contains("Exercise") || item.first.contains("Water") || item.first.contains("Weight") -> 3
                                item.first.contains("টুলস") || item.first.contains("ক্যালোরি") || item.first.contains("Calculator") || item.first.contains("Tools") -> 0
                                else -> null
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (targetTab != null) {
                                            onNavigateToTab(targetTab)
                                        }
                                    },
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)),
                                border = BorderStroke(1.dp, Color(0xFFF0F0F0))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = item.first,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.5.sp,
                                            color = Color(0xFF1E5E2F)
                                        )
                                        
                                        if (targetTab != null) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color(0xFFDCEDC8))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = when (targetTab) {
                                                        1 -> if (isBengali) "খাবার" else "Meals"
                                                        3 -> if (isBengali) "ট্র্যাকার" else "Tracker"
                                                        else -> if (isBengali) "হোম" else "Home"
                                                    },
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF33691E)
                                                )
                                            }
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = item.second,
                                        fontSize = 11.5.sp,
                                        color = Color.DarkGray,
                                        lineHeight = 16.sp
                                    )
                                    
                                    if (targetTab != null) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Launch,
                                                contentDescription = "Go",
                                                tint = Color(0xFF2E7D32),
                                                modifier = Modifier.size(10.0.dp)
                                            )
                                            Text(
                                                text = if (isBengali) "ট্যাব ওপেন করতে ক্লিক করুন" else "Tap to open tab section",
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF2E7D32)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun ExploreTab(
    viewModel: DietPlannerViewModel,
    isBengali: Boolean,
    onNavigateToTab: (Int) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    
    // Copy the high-quality items for Explore page
    val exploreItems = remember(isBengali) {
        listOf(
            Triple(
                if (isBengali) "ভাত (Rice) - শর্করা ও ক্যালোরি" else "Rice (Vat) - Heavy Carbs",
                if (isBengali) "১ কাপ সাদা ভাতে প্রায় ২০০ ক্যালোরি থাকে। পরিমিত ডায়েটে লাল চালের ভাত খাওয়া বেশি উপকারী যা ফাইবার সমৃদ্ধ।" else "1 cup of regular boiled rice contains ~200 calories. Brown rice is a healthier alternative with lots of food fiber.",
                listOf("rice", "ভাত", "bhat", "chaol", "carb", "heavy")
            ),
            Triple(
                if (isBengali) "ডিম (Egg) - উচ্চ মানের প্রোটিন" else "Egg (Dim) - Fast Proteins",
                if (isBengali) "একটি মাঝারি সেদ্ধ ডিমে প্রায় ৭০-৮০ ক্যালোরি এবং ৬ গ্রাম খাঁটি প্রোটিন থাকে। ওজন নিয়ন্ত্রণের জন্য আদর্শ।" else "A standard hard-boiled egg contains only 70-80 calories and 6g of pure protein. Great for healthy fitness.",
                listOf("egg", "ডিম", "dim", "boiled", "protein")
            ),
            Triple(
                if (isBengali) "পানি পান (Water) - মেটাবলিজম বৃদ্ধি" else "Water Tracker - Hydration Vitals",
                if (isBengali) "দৈনিক ৮-১০ গ্লাস বা ২.৫ - ৩ লিটার বিশুদ্ধ পানি পান করা আবশ্যক। হাইড্রেশন লেভেল ট্র্যাকারটি ট্র্যাকার ট্যাবে পাবেন।" else "An adult should consume 2.5 to 3 liters of water per day. Find the Water Intake Log within the Tracker tab.",
                listOf("water", "पानी", "পানি", "pani", "glass", "log", "tracker")
            ),
            Triple(
                if (isBengali) "ওজন নিয়ন্ত্রণ ও বৃদ্ধি (Weight Loss / Gain)" else "Weight Progress - Loss & Gain Tracker",
                if (isBengali) "আপনার ওজন কমানো বা বাড়ানোর অগ্রগতি চার্ট সহ দেখতে চান? নিচে মাঝের ট্র্যাকার ট্যাবটি বেছে নিন।" else "Want to monitor your weights progression visually via graphs? Find the interactive charts in the Tracker tab.",
                listOf("weight", "loss", "gain", "ওজন", "body", "fat", "bmi")
            ),
            Triple(
                if (isBengali) "স্মার্ট ডায়েট প্ল্যান (Smart Food Planner)" else "Smart Diet Generator - Custom Meals",
                if (isBengali) "নতুন স্বাস্থ্যকর সুষম খাবারের তালিকা পেতে ড্যাশবোর্ডের খাবার ট্যাবের ভাসমান 'সুষম খাবার' বাটনটি ক্লিক করুন।" else "Generate a newly optimized daily dietary schedule with the custom planner. Just tap the green AutoAwesome floating button on Meals tab.",
                listOf("diet", "meal", "খাবার", "ডায়েট", "চার্ট", "food")
            ),
            Triple(
                if (isBengali) "সবজি ও ফাইবারের উপকারিতা" else "Vegetables & Greens - Healthy Fiber",
                if (isBengali) "সবুজ শাকসবজিতে প্রচুর ফাইবার ও ভিটামিন সি থাকে যা হজমশক্তি বাড়ায় এবং দীর্ঘসময় পেট ভরা রাখতে সাহায্য করে।" else "Fresh green vegetables contain high dietary fiber, promoting healthy gut microflora and supporting digestion efficiency.",
                listOf("vegetable", "সবজি", "sabji", "sak", "veg", "green")
            ),
            Triple(
                if (isBengali) "রুটি (Roti/Bread) - সুষম কার্ব" else "Roti / Wheat Toast - Balanced Carbs",
                if (isBengali) "মাঝারি লাল আটার রুটিতে মাত্র ৭০-৮০ ক্যালোরি থাকে। এটি দীর্ঘস্থায়ী মেটাবলিক শক্তি যোগায় এবং ভাতের দারুণ বিকল্প।" else "One whole wheat roti contains ~70-80 calories. It is rich in complex carbohydrates and makes an excellent alternative to white rice.",
                listOf("roti", "bread", "রুটি", "wheat", "low carb")
            ),
            Triple(
                if (isBengali) "শারীরিক কসরত (Regular Workout)" else "Physical Exercise - Burn Vitals",
                if (isBengali) "সহজ মেদ ঝরাতে ও মানসিক সজীবতায় দৈনিক অন্তত ২৫ মিনিট কার্ডিও বা দ্রুত হাঁটার অভ্যাস করুন। ট্র্যাকার ট্যাব দেখুন।" else "Do brisk walking or moderate cardio for 25-30 mins daily to improve cardiovascular strength. Explore more inside the Tracker Tab.",
                listOf("exercise", "workout", "ব্যায়াম", "walk", "হাঁটা", "run", "fitness")
            ),
            Triple(
                if (isBengali) "ক্যালোরি হিসাবের টুলস (Calorie Burner Tool)" else "Calorie Tools - Calculator Tools",
                if (isBengali) "কোন খাবার খেলে কত ক্যালোরি বার্ন দরকার তা হিসাব করার জন্য অ্যাপের টুলস ট্যাবের ফুড-বার্ন ক্যালকুলেটরটি দেখুন।" else "Check out the food-calories burn calculator and calculators inside the dedicated Tools tab of the ANEXSOPZ app.",
                listOf("tools", "টুলস", "calorie", "khalori", "burn", "counter")
            ),
            Triple(
                if (isBengali) "কলা (Banana) - তাৎক্ষণিক শক্তি" else "Banana (Kola) - Instant Energy boost",
                if (isBengali) "১টি কলায় প্রায় ১০৫ ক্যালোরি পাওয়া যায়। এটি পটাসিয়াম সমৃদ্ধ হওয়ায় পেশীর কর্মক্ষমতা বাড়াতে দারুন উপকারী।" else "One medium banana yields about 105 calories and is highly rich in potassium which helps avoid blood pressure fluctuations.",
                listOf("banana", "kola", "কলা", "energy", "fruit")
            ),
            Triple(
                if (isBengali) "আপেল (Apple) - ডায়েট ফ্রুট" else "Apple (Apel) - Weight-loss Fruit",
                if (isBengali) "মাঝারি আপেলে ৯০ ক্যালোরি থাকে। এর দ্রবণীয় আঁশ ‘পেকটিন’ রক্তের কোলেস্টেরল মাত্রা কমাতে সক্রিয় ভূমিকা রাখে।" else "One apple contains ~90 calories and provides amazing soluble fibers (pectin) that significantly support balanced heart health.",
                listOf("apple", "apel", "আপেল", "fruit", "vitamin")
            ),
            Triple(
                if (isBengali) "দুধ (Milk) - ক্যালসিয়াম উৎস" else "Milk (Dudh) - Strong Calcium Sources",
                if (isBengali) "বিনা চর্বির ১ গ্লাস দুধে প্রায় ৮৫ ক্যালোরি থাকে যা আমাদের সুস্থ দাঁত ও হাড়ের ক্যালসিয়ামের দৈনন্দিন চাহিদা পূরণ করে।" else "One glass of skimmed/low-fat milk provides 85 calories and high-quality bio-available calcium for strong skeletons.",
                listOf("milk", "dudh", "দুধ", "calcium", "bones")
            )
        )
    }

    val filteredItems = remember(searchQuery, isBengali) {
        if (searchQuery.trim().isEmpty()) {
            exploreItems
        } else {
            val query = searchQuery.trim().lowercase(Locale.ROOT)
            exploreItems.filter { item ->
                item.first.lowercase(Locale.ROOT).contains(query) ||
                item.second.lowercase(Locale.ROOT).contains(query) ||
                item.third.any { synomym -> synomym.lowercase(Locale.ROOT).contains(query) }
            }
        }
    }

    val quickChips = remember(isBengali) {
        if (isBengali) {
            listOf("সব", "ভাত", "ডিম", "পানি", "ওজন", "ব্যায়াম", "সবজি", "কলা")
        } else {
            listOf("All", "Rice", "Egg", "Water", "Weight", "Workout", "Vegetables", "Banana")
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp)
                ) {
                    Text(
                        text = if (isBengali) "🌿 সুস্থ জীবনযাত্রার অভিধান ও টিপস" else "🌿 Wellness Guide & Fast Facts",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E5E2F)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isBengali) "আপনার দৈনন্দিন পুষ্টি উপাদান, ক্যালোরি মাত্রা ও ফিটনেস সম্পর্কে গুরুত্বপূর্ণ তথ্য জেনে নিন।" 
                               else "Explore critical nutritional facts, calorie indexes, food synonyms, and fast wellness tips.",
                        fontSize = 12.sp,
                        color = Color.DarkGray,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // Inline Search field
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text(if (isBengali) "অনুসন্ধান করুন যেমন: ডিম বা ব্যায়াম" else "Search foods/exercises e.g. egg") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color(0xFF2E7D32)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF2E7D32),
                    focusedLabelColor = Color(0xFF2E7D32)
                )
            )
        }

        // Quick Suggestions Horizontal Row
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(quickChips.size) { index ->
                    val chip = quickChips[index]
                    val isSelected = (chip == "All" || chip == "সব") && searchQuery.isEmpty() || searchQuery.lowercase(Locale.ROOT) == chip.lowercase(Locale.ROOT)
                    val bgColor = if (isSelected) Color(0xFF2E7D32) else Color(0xFFF1F8E9)
                    val textColor = if (isSelected) Color.White else Color(0xFF33691E)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(bgColor)
                            .clickable {
                                searchQuery = if (chip == "All" || chip == "সব") "" else chip
                            }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = chip,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textColor
                        )
                    }
                }
            }
        }

        // List inside LazyColumn
        if (filteredItems.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isBengali) "কোনো তথ্য খুঁজে পাওয়া যায়নি!" else "No matching items found in dictionary!",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Gray
                    )
                }
            }
        } else {
            items(filteredItems.size) { index ->
                val item = filteredItems[index]
                // Determine destination tab if they want redirection
                val targetTab = remember(item.third) {
                    val syns = item.third
                    when {
                        syns.contains("diet") || syns.contains("meal") -> 1 // 1 is Meals now
                        syns.contains("water") || syns.contains("weight") || syns.contains("exercise") -> 3 // 3 is Tracker now
                        syns.contains("tools") || syns.contains("calorie") -> 0 // 0 is Home/Tools now
                        else -> null
                    }
                }

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = item.first,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = Color(0xFF1E5E2F)
                            )
                            if (targetTab != null) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFFDCEDC8))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = when (targetTab) {
                                            1 -> if (isBengali) "খাবার" else "Meals"
                                            3 -> if (isBengali) "ট্র্যাকার" else "Tracker"
                                            else -> if (isBengali) "হোম" else "Home"
                                        },
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF33691E)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = item.second,
                            fontSize = 12.sp,
                            color = Color.DarkGray,
                            lineHeight = 17.sp
                        )

                        if (targetTab != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.clickable {
                                    onNavigateToTab(targetTab)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Launch,
                                    contentDescription = "Navigate to Tab",
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = if (isBengali) "সরাসরি টুলটি ব্যবহার করুন" else "Go to section...",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF2E7D32)
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
// Drawer Section Component Implementations (Terms, Privacy, AI Coach, Ratings, App Info)
// ============================================================================

@Composable
fun ANEXSOPZTermsDialog(
    isBengali: Boolean,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF1E5E2F))
            ) {
                Text(if (isBengali) "আমি সম্মত" else "I Agree", fontWeight = FontWeight.Bold)
            }
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("📜", fontSize = 22.sp)
                Text(
                    text = if (isBengali) "ব্যবহারের শর্তাবলী" else "Terms & Conditions",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF1E5E2F)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 350.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (isBengali) 
                        "ANEXSOPZ হেলথ প্লাস অ্যাপে আপনাকে স্বাগতম। এই পরিষেবাটি ব্যবহারের মাধ্যমে আপনি নিম্নলিখিত বিষয়গুলোতে সম্মত হচ্ছেন:" 
                    else 
                        "Welcome to ANEXSOPZ Health Plus. By accessing or using this health companion app, you agree to be bound by the following:",
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = Color.DarkGray
                )

                val terms = remember(isBengali) {
                    listOf(
                        (if (isBengali) "১. শারীরিক দায়িত্ব ও সতর্কতা" else "1. Health Disclaimer") to
                        (if (isBengali) "এই অ্যাপের সুষম ডায়েট চার্ট, পুষ্টির হিসাব এবং জেনারেট করা টিপস শুধুমাত্র সাধারণ সুস্থ জীবনযাপনের নির্দেশিকা। কোনো বিশেষ শারীরিক বা জটিল রোগে লাইসেন্সধারী চিকিৎসকের পরামর্শ নিন।" else "All food logs, nutrition calculators, diet planners and recommendations provided broad educational suggestions. This is not medical advice. Consult doctors for clinical decisions."),
                        (if (isBengali) "২. লোকাল ডাটা প্রসেসিং ও সেফটি" else "2. Offline Data Management") to
                        (if (isBengali) "আপনার ওজনের হিস্ট্রি, অল অ্যালার্জি ফিল্টার, এবং নোটিফিকেশন রিমাইন্ডারগুলো ডিভাইসের সুরক্ষিত ডাটাবেসে জমা থাকে। ক্যাশ মেমোরি ক্লিয়ার করলে ডাটা মুছে যেতে পারে।" else "Your calorie logs, allergen parameters, and weight metrics are kept purely on-device securely. Please secure your backups; clearing storage might delete history."),
                        (if (isBengali) "৩. ক্লাউড প্রসেসিং সংযোগ" else "3. Secure Cloud Computing") to
                        (if (isBengali) "ডায়েট চার্ট জেনারেশন ও রেসিপি আইডিয়া তৈরি করতে আমরা সিকিউর এপিআই সংযোগের মাধ্যমে গুগল সুরক্ষিত প্রসেসিং উইন্ডো ব্যবহার করি। কোনো আপত্তিজনক কুয়েরি পাঠানো নিষিদ্ধ।" else "Generating custom lifestyle designs or recipe lists utilizes standard secure channels. Avoid spamming queries.")
                    )
                }

                terms.forEach { (heading, desc) ->
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)),
                        border = BorderStroke(1.dp, Color(0xFFECEFF1)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(heading, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF2E7D32))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(desc, fontSize = 11.5.sp, lineHeight = 16.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun ANEXSOPZPrivacyPolicyDialog(
    isBengali: Boolean,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF1E5E2F))
            ) {
                Text(if (isBengali) "ঠিক আছে" else "Got It", fontWeight = FontWeight.Bold)
            }
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("🛡️", fontSize = 22.sp)
                Text(
                    text = if (isBengali) "প্রাইভেসি পলিসি" else "Privacy Policy",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF1E5E2F)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 350.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (isBengali)
                        "ANEXSOPZ হেলথ প্লাস অ্যাপ গ্রাহকদের তথ্যের গোপনীয়তা রক্ষায় দৃঢ় প্রতিশ্রুতিবদ্ধ:"
                    else 
                        "ANEXSOPZ Health Plus is committed to protecting your personal health tracking privacy:",
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = Color.DarkGray
                )

                val policies = remember(isBengali) {
                    listOf(
                        (if (isBengali) "১. শূন্য ক্লাউড ট্র্যাকিং" else "1. Full Data Control") to
                        (if (isBengali) "আপনার আপলোড করা বারকোড স্ক্যান তথ্য, ডায়েট হিস্ট্রি বা লগ করা ডাটা ANEXSOPZ ডেভেলপমেন্ট সার্ভারে জমা করা হয় না। এটি সম্পূর্ণ অফলাইন বা লোকাল স্টোরেজে থাকে।" else "We do not store your physical details, barcode logs or diet timelines on any remote cloud server. They reside locally inside your own phone."),
                        (if (isBengali) "২. নিরাপদ কুয়েরি প্রসেস" else "2. Minimal Secure Transmission") to
                        (if (isBengali) "ডায়েট চার্ট তৈরি করার সময়ে শুধুমাত্র শারীরিক পরিমাপসমূহ (উচ্চতা, ওজন, লক্ষ্য) এনক্রিপ্টেড উপায়ে সুরক্ষিত ডাটা প্রসেসরে পাঠানো হয়। কোনো ব্যক্তিগত নাম বা পরিচয় পাঠানো হয় না।" else "While deploying digital capabilities to formulate diet schedules, only anonymized bio-metrics metrics are securely passed to secure servers."),
                        (if (isBengali) "৩. ক্যামেরা এবং স্ক্যানার অনুমতি" else "3. Camera Safety Protocols") to
                        (if (isBengali) "বারকোড দিয়ে প্যাকেটজাত খাবারের উপাদান বা গুনাগুন যাচাইয়ের জন্য ক্যামেরা অনুমতি শুধুমাত্র স্ক্যান করার সময় সক্রিয় হয় এবং কোনো ছবি সংরক্ষণ করা হয় না।" else "Barcode scanning or photo-recognitions features activate the Android camera temporarily, processing inputs instantly without preserving pictures.")
                    )
                }

                policies.forEach { (heading, desc) ->
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9).copy(alpha = 0.5f)),
                        border = BorderStroke(1.dp, Color(0xFFC8E6C9)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(heading, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF1B5E20))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(desc, fontSize = 11.5.sp, lineHeight = 16.sp, color = Color.DarkGray)
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun ANEXSOPZAppInfoDialog(
    isBengali: Boolean,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF1E5E2F))
            ) {
                Text(if (isBengali) "বুঝেছি" else "I Understand", fontWeight = FontWeight.Bold)
            }
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("📚", fontSize = 22.sp)
                Text(
                    text = if (isBengali) "অ্যাপ গাইড ও সহায়িকা" else "Information Manual",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF1E5E2F)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (isBengali)
                        "ANEXSOPZ হেলথ প্লাস অ্যাপের তিনটি প্রধান ট্যাব ব্যবহারের গাইডলাইন নিচে দেওয়া হলো:"
                    else 
                        "Explore the core layouts of ANEXSOPZ Health Plus and learn how to get the most out of your lifestyle companion:",
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = Color.DarkGray
                )

                val infoSections = remember(isBengali) {
                    listOf(
                        Triple(
                            "🍛 Meals (খাবার)",
                            if (isBengali) "১. খাবার ড্যাশবোর্ড" else "1. Meals Dashboard Setup",
                            if (isBengali) "এখানে আপনি উচ্চতা, ওজন, বয়স ও লিঙ্গ দিয়ে নিজের প্রোফাইল সেটআপ করতে পারেন। আপনার লক্ষ্য (ওজন কমানো বা বৃদ্ধি বা নিয়ন্ত্রণ) এবং পছন্দ নির্বাচন করার পর ভাসমান 'সুষম খাবার' বাটনটি চেপে এক ক্লিকে সম্পূর্ণ দিনের ডায়েট চার্ট তৈরি করে নিন।" else "Configure your core vitals (weight, height, age). Set your goal to 'Weight Loss', 'Weight Gain', or 'Maintain'. Pick dietary habits, then use the bright green custom diet button to construct full nutritional daily goals."
                        ),
                        Triple(
                            "📈 Tracker (ট্র্যাকার)",
                            if (isBengali) "২. হাইড্রেশন ও প্রোগ্রেস চার্ট" else "2. Water & Weight Progression",
                            if (isBengali) "এই ট্যাবে দৈনিক ৮ গ্লাস পানি পানের হিসাব রেকর্ড করুন '+' চেপে। এছাড়া আপনার বর্তমান ওজন আপডেট করে ইন্টারেক্টিভ ওজনের প্রোগ্রেস লাইন চার্ট দিয়ে সহজে সুস্থতার অগ্রগতি পর্যবেক্ষণ করুন।" else "Manage your daily water consumption using the reactive water glass recorder. Log your regular body weight to view dynamic progression lines, graphs, and live updates."
                        ),
                        Triple(
                            "🛠️ Tools (টুলস ক্যালকুলেটর)",
                            if (isBengali) "৩. ফুড স্ক্যানার ও লাইভ রেসিপি" else "3. Custom Barcode Scanner & Recipes",
                            if (isBengali) "এখানে যেকোনো জেনারেট করা ডায়েট খাবারের কাঙ্খিত রান্নার সহজ রেসিপি বানাতে এক ক্লিকে রেসিপি উইজার্ড ব্যবহার করুন। এছাড়া বারকোড ক্যামেরা দিয়ে যেকোনো সুপারশপের প্যাকেটজাত ফল বা খাবারের পুষ্টিমান স্ক্যান করতে পারেন সহজেই।" else "Utilize our smart calories-burnt tool calculator, parse packet nutrients instantly by placing any packaged food barcodes near your camera, and leverage cooking wizard algorithms to generate tailored recipes."
                        )
                    )
                }

                infoSections.forEach { (title, sub, desc) ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA)),
                        border = BorderStroke(1.dp, Color(0xFFEEEEEE)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF1E5E2F))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(sub, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(desc, fontSize = 11.sp, lineHeight = 16.sp, color = Color.DarkGray)
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun ANEXSOPZRatingsDialog(
    isBengali: Boolean,
    onDismiss: () -> Unit
) {
    var rating by remember { mutableStateOf(5) }
    var reviewerName by remember { mutableStateOf("") }
    var commentText by remember { mutableStateOf("") }
    val context = androidx.compose.ui.platform.LocalContext.current

    val initialReviews = remember(isBengali) {
        mutableStateListOf(
            Triple("Tahsin Ahmed", 5, if (isBengali) "স্মার্ট ও সারণীবদ্ধ ডায়েট প্ল্যান জেনারেটর অসাধারণ কাজ করে! ওজন ৩ কেজি কমেছে।" else "The diet plan generator works incredibly well for custom daily diets! Underwent 3kg loss in one month!"),
            Triple("Israt Jahan", 5, if (isBengali) "বারকোড স্ক্যানার দিয়ে খাদ্যের গুণাগুণ ও উপাদান সাথে সাথে পেয়ে যাচ্ছি।" else "The barcodes scanner extracts nutrient metrics instantly! Lifesaver for grocery shopping!"),
            Triple("Rakibul Hasan", 4, if (isBengali) "পানি ট্র্যাকিং এর ইন্টারফেসটা খুব ইন্টারেক্টিভ। রেকমেন্ডেড অ্যাপ্লিকেশন।" else "The water intake graphics feels so smooth and responsive. Highly recommended app.")
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF1E5E2F))
            ) {
                Text(if (isBengali) "সমাপ্ত" else "Done", fontWeight = FontWeight.Bold)
            }
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("⭐", fontSize = 22.sp)
                Text(
                    text = if (isBengali) "রেটিং ও রিভিউ দিন" else "Ratings & Reviews",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF1E5E2F)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (isBengali) "অ্যাপটি আপনার কেমন লাগছে? রেটিং দিয়ে আপনার মতামত প্রকাশ করুন!" else "How is your health journey with ANEXSOPZ? Provide your rating & valuable feedback!",
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp,
                    color = Color.DarkGray
                )

                // Five Star Visual Selector
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (i in 1..5) {
                        IconButton(
                            onClick = { rating = i },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (i <= rating) Icons.Default.Star else Icons.Default.StarOutline,
                                contentDescription = "$i Stars",
                                tint = Color(0xFFFFB300),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = reviewerName,
                    onValueChange = { reviewerName = it },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    placeholder = { Text(if (isBengali) "আপনার নাম" else "Your Name", fontSize = 12.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF2E7D32)),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    placeholder = { Text(if (isBengali) "মতামত বা ফিডব্যাক..." else "Tell us what you like or can be improved...", fontSize = 12.sp) },
                    maxLines = 3,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF2E7D32)),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        if (reviewerName.trim().isNotBlank() && commentText.trim().isNotBlank()) {
                            initialReviews.add(0, Triple(reviewerName.trim(), rating, commentText.trim()))
                            reviewerName = ""
                            commentText = ""
                            android.widget.Toast.makeText(
                                context,
                                if (isBengali) "ফিডব্যাক দেওয়ার জন্য ধন্যবাদ!" else "Thank you for your rating!",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            android.widget.Toast.makeText(
                                context,
                                if (isBengali) "দয়া করে উপরে নাম ও বিবরণ পূরণ করুন!" else "Please fill name and review text!",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isBengali) "রিভিউ জমা দিন" else "Submit Review", color = Color.White, fontWeight = FontWeight.Bold)
                }

                Divider(color = Color(0xFFEEEEEE), modifier = Modifier.padding(vertical = 4.dp))

                Text(
                    text = if (isBengali) "অন্যান্য ব্যবহারকারীদের রিভিউ:" else "Recent User Reviews:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )

                // List of past reviews
                initialReviews.forEach { (user, stars, reviewDesc) ->
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(user, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.DarkGray)
                                Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                                    for (j in 1..5) {
                                        Icon(
                                            imageVector = if (j <= stars) Icons.Default.Star else Icons.Default.StarOutline,
                                            contentDescription = null,
                                            tint = Color(0xFFFFB300),
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(reviewDesc, fontSize = 11.sp, lineHeight = 15.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }
    )
}

data class ANEXSOPZChatMessage(
    val id: String,
    val text: String,
    val isUser: Boolean,
    val timestamp: String
)

@Composable
fun ANEXSOPZAICoachDialog(
    isBengali: Boolean,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val chatMessages = remember {
        mutableStateListOf(
            ANEXSOPZChatMessage(
                id = java.util.UUID.randomUUID().toString(),
                text = if (isBengali) 
                    "আসসালামু আলাইকুম! আমি 'ANEXSOPZ চমৎকার লাইফস্টাইল কোচ'। আপনার সুস্থ লাইফস্টাইল গড়ে তুলতে ডায়েট, হেলথ গোল বা নিউট্রিশন সম্পর্কিত যেকোনো প্রশ্ন করুন। আমি আপনাকে তাৎক্ষণিকভাবে বিজ্ঞানসম্মত পরামর্শ প্রদান করবো।"
                else 
                    "Welcome! I am your ANEXSOPZ Lifestyle & Diet Coach. Ask me any question related to dietary structures, workouts, custom weight goals, or general nutrition, and I will guide you instantly!",
                isUser = false,
                timestamp = "Now"
            )
        )
    }

    var userText by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    val lazyListState = rememberLazyListState()

    // Quick queries suggestions
    val quickPrompts = remember(isBengali) {
        if (isBengali) {
            listOf(
                "ওজন কমানোর ৩টি মূল শর্ত কি কি?",
                "কোন ধরনের নাস্তা ডায়েটের জন্য সেরা?",
                "মেটাবলিজম দ্রুত বৃদ্ধি করার উপায় কি?",
                "রাতে ভালো ঘুমের সুস্থ লাইফস্টাইল টিপস দিন"
            )
        } else {
            listOf(
                "What are 3 secrets of weight loss?",
                "Best low calorie snacks for evening?",
                "How to boost metabolism naturally?",
                "Provide healthy sleep routine tips"
            )
        }
    }

    fun handleSend(query: String) {
        if (query.trim().isEmpty()) return
        
        val userMsgText = query.trim()
        chatMessages.add(
            ANEXSOPZChatMessage(
                id = java.util.UUID.randomUUID().toString(),
                text = userMsgText,
                isUser = true,
                timestamp = "Just now"
            )
        )
        userText = ""
        isSending = true

        coroutineScope.launch {
            // Scroll to bottom
            kotlinx.coroutines.delay(100)
            lazyListState.animateScrollToItem(chatMessages.size - 1)

            // Trigger Gemini content api
            val apiKey = com.example.BuildConfig.GEMINI_API_KEY
            val systemIntroPrompt = """
                You are 'ANEXSOPZ Lifestyle Coach', a warm, encouraging, highly expert wellness companion for the "ANEXSOPZ Health Plus" app.
                Provide custom, highly tailored, scientific wellness advice. 
                Answer using the same language the user queried in (Bengali or English).
                Keep the response encouraging, beautifully structured with bullet points and emojis, and concise (within 150 words).
                The user has selected ${if (isBengali) "Bengali" else "English"} mode.
                
                User question: "$userMsgText"
            """.trimIndent()

            val request = GenerateContentRequest(
                contents = listOf(Content(parts = listOf(Part(text = systemIntroPrompt)))),
                generationConfig = GenerationConfig(temperature = 0.5f)
            )

            val replyText = try {
                if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                    throw IllegalStateException("API Key is placeholder")
                }
                val response = RetrofitClient.service.generateContent(apiKey, request)
                response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: throw Exception("Empty response candidate")
            } catch (e: Exception) {
                e.printStackTrace()
                // Standby local premium offline chatbot response generator
                generateOfflineCoachResponse(userMsgText, isBengali)
            }

            chatMessages.add(
                ANEXSOPZChatMessage(
                    id = java.util.UUID.randomUUID().toString(),
                    text = replyText,
                    isUser = false,
                    timestamp = "Just now"
                )
            )
            isSending = false
            
            kotlinx.coroutines.delay(100)
            lazyListState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF1E5E2F))) {
                Text(if (isBengali) "বন্ধ করুন" else "Close", fontWeight = FontWeight.Bold)
            }
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("🤖", fontSize = 22.sp)
                Column {
                    Text(
                        text = if (isBengali) "লাইফস্টাইল কোচ" else "Lifestyle Coach",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color(0xFF1E5E2F)
                    )
                    Text(
                        text = if (isBengali) "সুষম খাবার ও জীবন সহায়িকা" else "Powered securely by ANEXSOPZ ⚡",
                        fontWeight = FontWeight.Medium,
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Interactive conversation suggestions
                Text(
                    text = if (isBengali) "কোচকে জিজ্ঞেস করুন:" else "Ask any wellness topic:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickPrompts.forEach { hint ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFE8F5E9))
                                .clickable(enabled = !isSending) { handleSend(hint) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = hint,
                                fontSize = 10.5.sp,
                                color = Color(0xFF1E5E2F),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Divider(color = Color(0xFFEEEEEE), modifier = Modifier.padding(vertical = 4.dp))

                // Scrollable Chat Message Body
                Box(modifier = Modifier.weight(1f)) {
                    LazyColumn(
                        state = lazyListState,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(chatMessages) { msg ->
                            val alignment = if (msg.isUser) Alignment.End else Alignment.Start
                            val bubbleColor = if (msg.isUser) Color(0xFFE8F5E9) else Color(0xFFF1F1F1)
                            val textColor = if (msg.isUser) Color(0xFF1E5E2F) else Color.DarkGray

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = alignment
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth(0.85f)
                                        .padding(horizontal = 4.dp),
                                    horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
                                ) {
                                    if (!msg.isUser) {
                                        Text("🥗 ", fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
                                    }
                                    Card(
                                        shape = RoundedCornerShape(
                                            topStart = 12.dp,
                                            topEnd = 12.dp,
                                            bottomStart = if (msg.isUser) 12.dp else 0.dp,
                                            bottomEnd = if (msg.isUser) 0.dp else 12.dp
                                        ),
                                        colors = CardDefaults.cardColors(containerColor = bubbleColor),
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = msg.text,
                                            fontSize = 11.5.sp,
                                            lineHeight = 16.sp,
                                            color = textColor,
                                            modifier = Modifier.padding(10.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = msg.timestamp,
                                    fontSize = 8.5.sp,
                                    color = Color.LightGray,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 1.dp)
                                )
                            }
                        }

                        if (isSending) {
                            item {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text("🥗 ANEXSOPZ Coach is thinking...", fontSize = 11.sp, color = Color.Gray, fontStyle = FontStyle.Italic)
                                    CircularProgressIndicator(modifier = Modifier.size(10.dp), strokeWidth = 1.dp, color = Color(0xFF2E7D32))
                                }
                            }
                        }
                    }
                }

                Divider(color = Color(0xFFEEEEEE), modifier = Modifier.padding(vertical = 2.dp))

                // Input message bar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = userText,
                        onValueChange = { userText = it },
                        placeholder = { Text(if (isBengali) "প্রশ্ন করুন..." else "Query ANEXSOPZ Coach...", fontSize = 12.sp) },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        singleLine = true,
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF2E7D32),
                            focusedLabelColor = Color(0xFF2E7D32)
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(
                        onClick = { handleSend(userText) },
                        enabled = userText.trim().isNotEmpty() && !isSending,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (userText.trim().isNotEmpty() && !isSending) Color(0xFF2E7D32) else Color.LightGray)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send Message",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    )
}

fun generateOfflineCoachResponse(query: String, isBengali: Boolean): String {
    val q = query.lowercase(Locale.ROOT)
    return when {
        q.contains("weight") || q.contains("lose") || q.contains("ওজন") || q.contains("কমান") -> {
            if (isBengali) {
                "📉 **ওজন কমানোর ৩টি মূল সোনালী নিয়ম:**\n\n1. **ক্যালোরি ঘাটতি:** আপনার দৈনিক প্রয়োজনের চেয়ে অন্তত ৩০০-৫০০ ক্যালোরি কম খেলে প্রাকৃতিকভাবে সুস্থ উপায়ে ওজন কমবে।\n2. **উচ্চ প্রোটিন সমৃদ্ধ খাবার:** ডিম, শাকসবজি, এবং মুরগির অংশ বেশি রাখুন যাতে দীর্ঘসময় পেট ভরা থাকে।\n3. **পর্যাপ্ত হাইড্রেশন:** প্রতিদিন অন্তত ৩ লিটার পানি পান করুন। খাওয়ার ঠিক পূর্বে ১ গ্লাস পানি খেলে অতিরিক্ত খাওয়ার স্বভাব কমে যাবে।"
            } else {
                "📉 **3 Golden Rules for Healthy Weight Loss:**\n\n1. **Maintain Calorie Deficit:** Consume 300-500 kcal less than calculated burnt energy to enable consistent fat mobilization.\n2. **High Integrity Protein:** Double down on leafy green foods, boiled egg whites and legumes for enhanced early satisfaction of appetite.\n3. **Optimal Hydration:** Drink 3 liters of pure fluids daily. Having a glass of water 20m before dinner decreases overeating habits."
            }
        }
        q.contains("snack") || q.contains("evening") || q.contains("নাস্তা") -> {
            if (isBengali) {
                "🍿 **স্বাস্থ্যকর ডায়েট বিকেলের নাস্তা:**\n\n- সবজি ও সুপ জাতীয় খাবার যা সহজে হজম হয়।\n- ১ মুঠো কাঠবাদাম ও আখরোট (স্বাস্থ্যকর ফ্যাটের চমত্কার উৎস)।\n- ১ বাটি চিনি ছাড়া ওটমেল অথবা টক দই দিয়ে চিয়া সিড।"
            } else {
                "🍿 **Healthy Diet Afternoon Snacks:**\n\n- Fresh vegetable broth/soup for clean digestion efficiency.\n- 1 ounce of almonds or walnuts (perfect sources of heart-friendly fats).\n- 1 bowl of unsweetened Greek yogurt topped with chia seeds or sliced apple."
            }
        }
        q.contains("metabolism") || q.contains("মেটাবলিজম") -> {
            if (isBengali) {
                "🔥 **মেটাবলিজম প্রাকৃতিকভাবে বাড়াতে করণীয়:**\n\n- পর্যাপ্ত ঘুমের অভ্যাস করুন (কমপক্ষে ৭-৮ ঘন্টা প্রতিদিন)।\n- গ্রিন টি বা অল্প গ্রিন কফি ব্যবহার করতে পারেন যা অ্যান্টিঅক্সিডেন্টে ভরপুর।\n- অ্যাপের 'Tracker' ট্যাবে গিয়ে নিয়মিত ২৫-৩০ মিনিট দ্রুত হাঁটার রেকর্ড রাখুন।"
            } else {
                "🔥 **Boost Metabolism Naturally:**\n\n- Sleep consistently! Getting 7-8 hours deep sleep acts as a metabolic reset block.\n- Sip clean unsweetened Green Tea rich in antioxidant catechins.\n- Stay moving! Use our Tracker module to verify 25 mins brisk walking logs daily."
            }
        }
        else -> {
            if (isBengali) {
                "🌿 **ANEXSOPZ ডায়েট নির্দেশনা:** সুস্থ থাকতে দৈনিক পরিশোধিত সাদা চিনি বর্জন করুন, অতিরিক্ত ভাজা কড়াইয়ের খাবার পরিহার করুন এবং দৈনিক অন্তত ২.৫ লিটার পানি ও প্রচুর সবুজ সবজি খাবার অভ্যাস রাখুন। ANEXSOPZ ড্যাশবোর্ডে আপনার ওজন ও মেটাবলিজমের দিকে খেয়াল রাখুন সর্বদা!"
            } else {
                "🌿 **ANEXSOPZ Smart Wellness Tip:** Remove added sugars completely, control processed foods intake and keep hydrated with 2.5L+ clean water. Follow up your daily goals, calorie allocations and progression graphs right here on your ANEXSOPZ dashboard!"
            }
        }
    }
}

@Composable
fun HealthAndPreferenceComponent(
    isBengali: Boolean,
    medicalConditions: String,
    cuisinePreferences: String,
    onMedicalConditionsChanged: (String) -> Unit,
    onCuisinePreferencesChanged: (String) -> Unit
) {
    val predefinedConditions = listOf(
        "Diabetes" to (if (isBengali) "ডায়াবেটিস (Diabetes)" else "Diabetes"),
        "Hypertension" to (if (isBengali) "উচ্চ রক্তচাপ (Hypertension)" else "Hypertension"),
        "High Cholesterol" to (if (isBengali) "কোলেস্টেরল (Cholesterol)" else "High Cholesterol"),
        "IBS" to (if (isBengali) "আইবিএস (IBS)" else "IBS / Stomach"),
        "Thyroid" to (if (isBengali) "থাইরয়েড (Thyroid)" else "Thyroid")
    )

    val predefinedCuisines = listOf(
        "Bengali" to (if (isBengali) "বাঙালি (Bengali)" else "Bengali"),
        "Mediterranean" to (if (isBengali) "মেডিটেরিয়ান (Mediterranean)" else "Mediterranean"),
        "Indian" to (if (isBengali) "भारतीय (Indian)" else "Indian"),
        "Western" to (if (isBengali) "ওয়েস্টার্ন (Western)" else "Western"),
        "Keto" to (if (isBengali) "কেটো (Keto)" else "Keto / Low-Carb")
    )

    // Parse values safely
    val selectedConds = remember(medicalConditions) {
        medicalConditions.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }
    val selectedCuisines = remember(cuisinePreferences) {
        cuisinePreferences.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }

    // Filter out custom items
    val customCondsText = remember(selectedConds) {
        selectedConds.filterNot { cond -> 
            predefinedConditions.any { it.first.equals(cond, ignoreCase = true) }
        }.joinToString(", ")
    }
    val customCuisinesText = remember(selectedCuisines) {
        selectedCuisines.filterNot { cuis ->
            predefinedCuisines.any { it.first.equals(cuis, ignoreCase = true) }
        }.joinToString(", ")
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("🩺", fontSize = 20.sp)
                Text(
                    text = if (isBengali) "স্বাস্থ্য ও স্বাদের পছন্দসমূহ" else "Health & Taste Settings",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

            // 1. Medical Conditions Category
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (isBengali) "মেডিকেল জটিলতা (Medical Conditions):" else "Medical Conditions (Select All):",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Layout in columns / rows
                val condChunks = predefinedConditions.chunked(2)
                condChunks.forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowItems.forEach { pair ->
                            val key = pair.first
                            val label = pair.second
                            val isChecked = selectedConds.any { it.equals(key, ignoreCase = true) }
                            PreferenceChip(
                                text = label,
                                selected = isChecked,
                                onClick = {
                                    val updated = if (isChecked) {
                                        selectedConds.filterNot { it.equals(key, ignoreCase = true) }
                                    } else {
                                        selectedConds + key
                                    }
                                    onMedicalConditionsChanged(updated.joinToString(", "))
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (rowItems.size < 2) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }

                // Custom conditions text input
                OutlinedTextField(
                    value = customCondsText,
                    onValueChange = { newValue ->
                        val predefinedActive = selectedConds.filter { cond ->
                            predefinedConditions.any { it.first.equals(cond, ignoreCase = true) }
                        }
                        val customParts = newValue.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        val combined = (predefinedActive + customParts).distinct()
                        onMedicalConditionsChanged(combined.joinToString(", "))
                    },
                    label = { Text(if (isBengali) "অন্যান্য মেডিকেল সমস্যা (কমা দিয়ে লিখুন)" else "Other Medical Conditions (comma-separated)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
            }

            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

            // 2. Cuisine Preference Category
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (isBengali) "রন্ধনশৈলী পছন্দসমূহ (Cuisine Preferences):" else "Cuisine Preferences (Select All):",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Layout in columns / rows
                val cuisChunks = predefinedCuisines.chunked(2)
                cuisChunks.forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowItems.forEach { pair ->
                            val key = pair.first
                            val label = pair.second
                            val isChecked = selectedCuisines.any { it.equals(key, ignoreCase = true) }
                            PreferenceChip(
                                text = label,
                                selected = isChecked,
                                onClick = {
                                    val updated = if (isChecked) {
                                        selectedCuisines.filterNot { it.equals(key, ignoreCase = true) }
                                    } else {
                                        selectedCuisines + key
                                    }
                                    onCuisinePreferencesChanged(updated.joinToString(", "))
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (rowItems.size < 2) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }

                // Custom cuisine text input
                OutlinedTextField(
                    value = customCuisinesText,
                    onValueChange = { newValue ->
                        val predefinedActive = selectedCuisines.filter { cuis ->
                            predefinedCuisines.any { it.first.equals(cuis, ignoreCase = true) }
                        }
                        val customParts = newValue.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        val combined = (predefinedActive + customParts).distinct()
                        onCuisinePreferencesChanged(combined.joinToString(", "))
                    },
                    label = { Text(if (isBengali) "অন্যান্য রান্নার পছন্দ (কমা দিয়ে লিখুন)" else "Other Cuisines (comma-separated)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
            }
        }
    }
}

@Composable
fun PreferenceChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = modifier
            .padding(vertical = 4.dp)
            .heightIn(min = 48.dp)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (selected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun ANEXSOPZDailyInsightDialog(
    isBengali: Boolean,
    totalCalorieTarget: Int,
    consumedMealsCal: Int,
    extraSnacksCal: Int,
    workoutBurntCal: Int,
    waterLog: WaterLogEntity?,
    dailyWaterTarget: Int,
    currentExerciseLogs: List<ExerciseLogEntity>,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
                .border(2.dp, Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.secondary
                    )
                ), RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header with Spark Icon and Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("🌟", fontSize = 24.sp)
                        Column {
                            Text(
                                text = if (isBengali) "আজকের হেলথ অন্তর্দৃষ্টি" else "Daily Health Insights",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (isBengali) "লাইভ বিপাকীয় ও পুষ্টি তথ্যসংক্ষেপ" else "Real-time metabolic & status log",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Dialog",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // 1. Caloric Intake Equation Section
                Text(
                    text = if (isBengali) "🔥 ক্যালরি স্থিতি (Calorie Budget):" else "🔥 Calorie Budget Status:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                val totalConsumed = consumedMealsCal + extraSnacksCal
                val netCalories = (totalConsumed - workoutBurntCal).coerceAtLeast(0)
                val calProgress = if (totalCalorieTarget > 0) (netCalories.toFloat() / totalCalorieTarget.toFloat()).coerceIn(0f, 1f) else 0f

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (isBengali) "মেটাবলিক সমীকরণ" else "Metabolic Formula",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (isBengali) "নেট: $netCalories kcal" else "Net: $netCalories kcal",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (netCalories <= totalCalorieTarget) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(6.dp))

                        // Dynamic Equation Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                Text("$totalConsumed", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(if (isBengali) "খাদ্য ইনফ্লো" else "Food In", fontSize = 9.sp, color = Color.Gray)
                            }
                            Text("-", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                Text("$workoutBurntCal", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFFC62828))
                                Text(if (isBengali) "ব্যায়ামে বার্ন" else "Exercise Burn", fontSize = 9.sp, color = Color.Gray)
                            }
                            Text("=", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                Text("$netCalories", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                                Text(if (isBengali) "নেট ইনপুট" else "Net Input", fontSize = 9.sp, color = Color.Gray)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Progress Bar
                        LinearProgressIndicator(
                            progress = calProgress,
                            color = if (netCalories <= totalCalorieTarget) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (isBengali) "লক্ষ্য কালোরি: $totalCalorieTarget kcal" else "Target: $totalCalorieTarget kcal",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = "${(calProgress * 100).toInt()}%",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. Hydration/Water Intake Summary
                Text(
                    text = if (isBengali) "💧 পানির ভারসাম্য (Hydration Log):" else "💧 Hydration Balance:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1976D2),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                val currentWater = waterLog?.amountMl ?: 0
                val waterProgress = if (dailyWaterTarget > 0) (currentWater.toFloat() / dailyWaterTarget.toFloat()).coerceIn(0f, 1f) else 0f
                val currentGlasses = currentWater / 250

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD).copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(14.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (waterProgress >= 1f) "👑" else "🥤",
                            fontSize = 28.sp,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (isBengali) "পানি পানের মাত্রা" else "Water Statistics",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color(0xFF0D47A1)
                                )
                                Text(
                                    text = "$currentWater / $dailyWaterTarget mL",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color(0xFF0D47A1)
                                )
                            }
                            Text(
                                text = if (isBengali) "গ্লাস হিসেবে: ~$currentGlasses গ্লাস সম্পন্ন" else "Equivalent: ~$currentGlasses glasses complete",
                                fontSize = 10.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            LinearProgressIndicator(
                                progress = waterProgress,
                                color = Color(0xFF1976D2),
                                trackColor = Color(0xFFBBDEFB),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 3. Exercise/Active Burn Summary
                Text(
                    text = if (isBengali) "🏃 ব্যায়ামের অগ্রগতি (Activities Done):" else "🏃 Physical Activity Log:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE65100),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                val exerciseCount = currentExerciseLogs.size
                val totalDuration = currentExerciseLogs.sumOf { log -> log.durationMin }

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0).copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (isBengali) "আজকের কসরত সংখ্যা: $exerciseCount টি" else "Daily Workout: $exerciseCount logs",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFFE65100)
                            )
                            Text(
                                text = if (isBengali) "মোট সময়: $totalDuration মিনিট" else "Total Active: $totalDuration mins",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFFE65100)
                            )
                        }

                        if (currentExerciseLogs.isEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (isBengali) "আজকে এখনও কোনো ব্যায়াম বা কসরত লগ করা হয়নি।" else "No active workout logs for today. Move a bit!",
                                fontSize = 11.sp,
                                color = Color.Gray,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        } else {
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = Color(0xFFFFCC80).copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            // Micro list of exercises
                            currentExerciseLogs.forEach { log ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    val emoji = when (log.activity) {
                                        "Walking" -> "🚶"
                                        "Jogging" -> "🏃"
                                        "Yoga" -> "🧘"
                                        "Gym" -> "🏋️"
                                        "Cycling" -> "🚴"
                                        else -> "💪"
                                    }
                                    val activityText = when (log.activity) {
                                        "Walking" -> if (isBengali) "হাঁটা হাঁটি" else "Walking"
                                        "Jogging" -> if (isBengali) "দৌড়ানো" else "Jogging"
                                        "Yoga" -> if (isBengali) "যোগব্যায়াম" else "Yoga"
                                        "Gym" -> if (isBengali) "ভারোত্তোলন / জিম" else "Gym Workout"
                                        "Cycling" -> if (isBengali) "সাইকেল চালানো" else "Cycling"
                                        else -> log.activity
                                    }
                                    Text(
                                        text = "$emoji $activityText (${log.durationMin}m)",
                                        fontSize = 11.sp,
                                        color = Color.DarkGray
                                    )
                                    Text(
                                        text = "-${log.caloriesBurned} kcal",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFD84315)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 4. Smart Personalized Coach Tip
                Text(
                    text = if (isBengali) "💡 ANEXSOPZ স্বাস্থ্য পরামর্শ (ANEXSOPZ Insight):" else "💡 Personalized Coach Advice:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                val coachTip = remember(netCalories, currentWater, exerciseCount, isBengali) {
                    buildCustomCoachTip(netCalories, totalCalorieTarget, currentWater, dailyWaterTarget, exerciseCount, isBengali)
                }

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("💡", fontSize = 22.sp, modifier = Modifier.padding(end = 10.dp))
                        Text(
                            text = coachTip,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Dismiss Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(
                        text = if (isBengali) "বন্ধ করুন" else "Great, thanks!",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun buildCustomCoachTip(
    netCalories: Int,
    targetCalories: Int,
    currentWaterMl: Int,
    targetWaterMl: Int,
    exerciseCount: Int,
    isBengali: Boolean
): String {
    return if (isBengali) {
        val coachText = java.lang.StringBuilder()
        
        // Calorie Tip
        if (netCalories > targetCalories) {
            coachText.append("⚠️ ক্যালরি লক্ষ্যমাত্রা ছাড়িয়ে গেছে! অতিরিক্ত শর্করা খাবার পরিহার করুন। ")
        } else if (netCalories >= targetCalories - 300) {
            coachText.append("✅ ক্যালরি ইনপুট নিখুঁত ভারসাম্য বজায় রেখেছে। চমৎকার! ")
        } else {
            coachText.append("🍲 পুষ্টিকর খাবার বেশি খেলে ক্ষতি নেই, লক্ষ্যমাত্রা পূরণে পরিমিত খান। ")
        }

        // Hydration Tip
        if (currentWaterMl < targetWaterMl * 0.5) {
            coachText.append("💧 পানি পানের মাত্রা অনেক কম! কুসুম গরম পানি বা ডাবের পানি পান করতে পারেন। ")
        } else if (currentWaterMl < targetWaterMl) {
            coachText.append("💧 পানি পানের মাত্রা ভালো, আর কয়েকটি গ্লাস পান সম্পন্ন করুন। ")
        } else {
            coachText.append("🌟 দারুণ! হাইড্রেশন লক্ষ্যমাত্রা শতভাগ সফল পূরণ করেছেন। ")
        }

        // Exercise Tip
        if (exerciseCount == 0) {
            coachText.append("🚶 শরীরকে সক্রিয় রাখতে অন্তত ১০-১৫ মিনিট হাঁটাহাঁটি করুন।")
        } else {
            coachText.append("💪 কসরত সম্পন্ন করায় মেটাবলিজম ত্বরান্বিত হচ্ছে! ভালো গতি বজায় রাখুন।")
        }

        coachText.toString()
    } else {
        val coachText = java.lang.StringBuilder()
        
        // Calorie Tip
        if (netCalories > targetCalories) {
            coachText.append("⚠️ You are slightly over your calorie budget. Opt for dynamic fiber-rich alternatives. ")
        } else if (netCalories >= targetCalories - 300) {
            coachText.append("✅ Fabulous! You are keeping a perfect calorie budget balance today. ")
        } else {
            coachText.append("🍲 Caloric intake is quite low. Ensure you consume nutrient-rich items to fuel your body perfectly. ")
        }

        // Hydration Tip
        if (currentWaterMl < targetWaterMl * 0.5) {
            coachText.append("💧 Water level is critical. Grab a refreshing glass of water immediately to restore focus! ")
        } else if (currentWaterMl < targetWaterMl) {
            coachText.append("💧 Hydration is getting there! A couple more glasses to reach full goals. ")
        } else {
            coachText.append("🌟 Masterful job! You have fully satisfied your dynamic hydration goal today. ")
        }

        // Exercise Tip
        if (exerciseCount == 0) {
            coachText.append("🚶 Set a modest goal, like a 15-minute quick walk, to keep your heart healthy and active.")
        } else {
            coachText.append("💪 Splendid active workout log today! You are burning fat and strengthening your recovery.")
        }

        coachText.toString()
    }
}
