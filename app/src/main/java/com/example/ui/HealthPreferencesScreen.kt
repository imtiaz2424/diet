package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.DietPlannerViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HealthPreferencesScreen(
    viewModel: DietPlannerViewModel,
    isBengali: Boolean,
    onBack: () -> Unit
) {
    val userProfile by viewModel.userProfile.collectAsState()
    
    // Default fallback values if user profile is null
    val currentMedicalConditionsList = userProfile?.medical_conditions ?: listOf("None")
    val currentCuisinePreferencesList = userProfile?.cuisine_preferences ?: listOf("Bengali")

    val predefinedConditions = listOf(
        "Diabetes" to (if (isBengali) "ডায়াবেটিস (Diabetes)" else "Diabetes"),
        "Hypertension" to (if (isBengali) "উচ্চ রক্তচাপ (Hypertension)" else "Hypertension"),
        "High Cholesterol" to (if (isBengali) "কোলেস্টেরল (Cholesterol)" else "High Cholesterol"),
        "IBS" to (if (isBengali) "আইবিএস বা পেটের সমস্যা (IBS)" else "IBS / Stomach Sensitivity"),
        "Thyroid" to (if (isBengali) "থাইরয়েড (Thyroid)" else "Thyroid Issues"),
        "Kidney Disease" to (if (isBengali) "কিডনি রোগ (Kidney Disease)" else "Kidney Health"),
        "Celiac Disease" to (if (isBengali) "গ্লুটেন অ্যালার্জি (Celiac / Gluten-Free)" else "Celiac (Gluten-Free)"),
        "Lactose Intolerance" to (if (isBengali) "ল্যাকটোজ ইনটলারেন্স (Lactose Intolerance)" else "Lactose Intolerance")
    )

    val predefinedCuisines = listOf(
        "Bengali" to (if (isBengali) "বাঙালি খাবার (Bengali)" else "Bengali"),
        "Mediterranean" to (if (isBengali) "মেডিটেরিয়ান (Mediterranean)" else "Mediterranean"),
        "Indian" to (if (isBengali) "ভারতীয় খাবার (Indian)" else "Indian"),
        "Western" to (if (isBengali) "ওয়েস্টার্ন খাবার (Western)" else "Western"),
        "East Asian" to (if (isBengali) "ইস্ট এশিয়ান (East Asian)" else "East Asian"),
        "Middle Eastern" to (if (isBengali) "মধ্যপ্রাচ্য (Middle Eastern)" else "Middle Eastern"),
        "Keto" to (if (isBengali) "কেটো ডায়েট (Keto / Low-Carb)" else "Keto / Low-Carb")
    )

    // Current states parsed from DB
    var selectedConds by remember(currentMedicalConditionsList) {
        mutableStateOf(
            currentMedicalConditionsList
                .filter { it.isNotEmpty() && !it.equals("None", ignoreCase = true) }
                .toSet()
        )
    }

    var selectedCuisines by remember(currentCuisinePreferencesList) {
        mutableStateOf(
            currentCuisinePreferencesList
                .filter { it.isNotEmpty() }
                .toSet()
        )
    }

    // Custom entries tracking
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

    var customCondsInput by remember { mutableStateOf(customCondsText) }
    var customCuisinesInput by remember { mutableStateOf(customCuisinesText) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isBengali) "স্বাস্থ্য ও খাদ্য পছন্দসমূহ" else "Health & Cuisine Settings",
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
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                color = Color.White,
                modifier = Modifier.navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, Color(0xFF2E7D32))
                    ) {
                        Text(
                            text = if (isBengali) "বাতিল করুন" else "Cancel",
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = {
                            // Compile final lists
                            val finalPredefinedConds = selectedConds.filter { cond ->
                                predefinedConditions.any { it.first.equals(cond, ignoreCase = true) }
                            }.toSet()
                            val parsedCustomConds = customCondsInput.split(",")
                                .map { it.trim() }
                                .filter { it.isNotEmpty() }
                            val finalCondsList = (finalPredefinedConds + parsedCustomConds).distinct()

                            val finalPredefinedCuisines = selectedCuisines.filter { cuis ->
                                predefinedCuisines.any { it.first.equals(cuis, ignoreCase = true) }
                            }.toSet()
                            val parsedCustomCuisines = customCuisinesInput.split(",")
                                .map { it.trim() }
                                .filter { it.isNotEmpty() }
                            val finalCuisinesList = (finalPredefinedCuisines + parsedCustomCuisines).distinct()

                            viewModel.updateHealthPreferences(finalCondsList, finalCuisinesList)
                            onBack()
                        },
                        modifier = Modifier.weight(1.2f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isBengali) "সংরক্ষণ করুন" else "Save Settings",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF9FBF9)) // Gentle off-white health app canvas
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Introductory Card
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
                        Text("🩺", fontSize = 24.sp)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isBengali) "স্মার্ট ডায়েট ও স্বাস্থ্য সমন্বয়" else "Clinical Nutrition Adjuster",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF1B5E20)
                        )
                        Text(
                            text = if (isBengali) 
                                "এখানে আপনার মেডিকেল কন্ডিশন এবং পছন্দের রন্ধনশৈলী নির্বাচন করুন। আমাদের স্মার্ট জেনারেটর আপনার খাদ্যতালিকায় এগুলোকে সর্বোচ্চ গুরুত্ব দিয়ে সংগতিপূর্ণ ডায়েট প্ল্যান সাজাবে।" 
                            else 
                                "Choose health needs and preferred flavors. ANEXSOPZ's recipe engine dynamically formats wellness plans conforming to chosen criteria.",
                            fontSize = 11.sp,
                            color = Color(0xFF2E7D32),
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            // SECTION 1: Medical Conditions
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFECEFF1)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Healing,
                            contentDescription = null,
                            tint = Color(0xFFC62828),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (isBengali) "মেডিকেল প্রোফাইল (Medical Conditions)" else "Medical Health Profiler",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF263238)
                        )
                    }

                    Text(
                        text = if (isBengali) "আপনার কোনো স্বাস্থ্য সমস্যা থাকলে নির্বাচন করুন:" else "Select health conditions that apply to you:",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )

                    // Grid / Flow-style list of Chips
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        predefinedConditions.forEach { pair ->
                            val key = pair.first
                            val label = pair.second
                            val isSelected = selectedConds.any { it.equals(key, ignoreCase = true) }

                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedConds = if (isSelected) {
                                        selectedConds.filterNot { it.equals(key, ignoreCase = true) }.toSet()
                                    } else {
                                        selectedConds + key
                                    }
                                },
                                label = {
                                    Text(
                                        text = label,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 12.sp
                                    )
                                },
                                leadingIcon = if (isSelected) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFFFEBEE),
                                    selectedLabelColor = Color(0xFFB71C1C),
                                    selectedLeadingIconColor = Color(0xFFB71C1C),
                                    containerColor = Color(0xFFF5F7F8),
                                    labelColor = Color(0xFF546E7A)
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = Color(0xFFCFD8DC),
                                    selectedBorderColor = Color(0xFFEF9A9A),
                                    borderWidth = 1.dp,
                                    selectedBorderWidth = 1.5.dp,
                                    enabled = true,
                                    selected = isSelected
                                )
                            )
                        }
                    }

                    // Custom Condition Text Input
                    OutlinedTextField(
                        value = customCondsInput,
                        onValueChange = { customCondsInput = it },
                        label = {
                            Text(
                                text = if (isBengali) "অন্যান্য মেডিকেল সমস্যা (কমা দিয়ে লিখুন)" else "Other custom health issues (comma-separated)",
                                fontSize = 12.sp
                            )
                        },
                        placeholder = {
                            Text(
                                text = "e.g., Asthma, Migraine",
                                fontSize = 12.sp,
                                color = Color.LightGray
                            )
                        },
                        leadingIcon = { Text("📝", fontSize = 16.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFC62828),
                            unfocusedBorderColor = Color(0xFFCFD8DC)
                        ),
                        singleLine = true
                    )
                }
            }

            // SECTION 2: Dynamic Health Advice Banner (if items are active!)
            AnimatedVisibility(
                visible = selectedConds.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFAED)),
                    border = BorderStroke(1.dp, Color(0xFFFFECB3)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFFF57F17),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (isBengali) "ক্লিনিক্যাল ডায়েট নোটস (Dietary Directives)" else "Dynamic Medical Advisories",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF5D4037)
                            )
                        }

                        // Render custom helpful tips depending on selected items
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (selectedConds.any { it.equals("Diabetes", ignoreCase = true) }) {
                                AdvisoryItem(
                                    title = if (isBengali) "ডায়াবেটিস পুষ্টি নির্দেশিকা:" else "Diabetes Diet Strategy:",
                                    text = if (isBengali) "সহজ সুগার বর্জন করুন। জটিল কার্বোহাইড্রেট যেমন লাল আটা, ওটস এবং প্রচুর সবুজ শাকসবজি ডায়েটে প্রাধান্য পাবে।" else "Restrict simple sugars completely. Prioritize complex carbs with low glycemic index, healthy fiber, lean proteins, and structural meal timing.",
                                    bulletColor = Color(0xFFC62828)
                                )
                            }
                            if (selectedConds.any { it.equals("Hypertension", ignoreCase = true) }) {
                                AdvisoryItem(
                                    title = if (isBengali) "উচ্চ রক্তচাপ নির্দেশিকা:" else "Hypertension Low-Sodium Plan:",
                                    text = if (isBengali) "দৈনিক লবণের পরিমাণ সীমিত রাখুন (১ চা চামচের কম)। পটাসিয়াম সমৃদ্ধ যেমন কলা, ডাব এবং তাজা ফল বেশি খাবেন।" else "Enforce low sodium limits (under 1500mg daily). Emphasize potassium-rich foods, fresh greens, and avoid salted snacks or pickles.",
                                    bulletColor = Color(0xFF1E88E5)
                                )
                            }
                            if (selectedConds.any { it.equals("High Cholesterol", ignoreCase = true) }) {
                                AdvisoryItem(
                                    title = if (isBengali) "উচ্চ কোলেস্টেরল নিয়ন্ত্রণ:" else "Hyperlipidemia Cardiovascular Plan:",
                                    text = if (isBengali) "ডালডা, ভাজা পোড়া ও অতিরিক্ত চর্বি পরিহার করুন। ওমেগা-৩ ও দ্রবণীয় ফাইবার যেমন ইসুবগুল বা ওটস নিয়মিত খাবেন।" else "Avoid hydrogenated trans-fats, processed margarine, and deep frying. Boost omega-3 intake, heart-healthy seed oils, and soluble oat beta-glucan.",
                                    bulletColor = Color(0xFF8E24AA)
                                )
                            }
                            if (selectedConds.any { it.equals("IBS", ignoreCase = true) }) {
                                AdvisoryItem(
                                    title = if (isBengali) "আইবিএস উপশম ডায়েট:" else "Irritable Bowel (IBS) Plan:",
                                    text = if (isBengali) "গ্যাস ও বদহজম সৃষ্টিকারী গুরুপাক খাবার বাদ দিন। লো-FODMAP খাবার ও সতেজ বাটি খাবারে গুরুত্ব দিন।" else "Focus on gastrointestinal-safe, soothing low-FODMAP options. Track personalized flare triggers and favor prebiotic digestion support.",
                                    bulletColor = Color(0xFFF57C00)
                                )
                            }
                            if (selectedConds.any { it.equals("Celiac Disease", ignoreCase = true) }) {
                                AdvisoryItem(
                                    title = if (isBengali) "সেলিয়াক (গ্লুটেন-মুক্ত):" else "Gluten-Free Protocols:",
                                    text = if (isBengali) "ময়দা, সুজি, রুটি, বার্লি সম্পূর্ণ বর্জনীয়। লাল চালের ভাত, ভুট্টা ও চিঁড়ে সুরক্ষিত শর্করার উৎস হিসেবে ব্যবহৃত হবে।" else "Strict rejection of wheat, barley, rye, semolina, and processed bakery. Substituted by whole rice, quinoa, and certified gluten-free foods.",
                                    bulletColor = Color(0xFF2E7D32)
                                )
                            }
                            if (selectedConds.any { it.equals("Lactose Intolerance", ignoreCase = true) }) {
                                AdvisoryItem(
                                    title = if (isBengali) "ল্যাকটোজ ইনটলারেন্স ব্যবস্থাপনা:" else "Lactose-Free Plan:",
                                    text = if (isBengali) "গরুর দুধ ও ক্রিম জাতীয় জিনিস এড়িয়ে চলুন। প্রোটিন ও ক্যালসিয়ামের জন্য ডিম, কাঠবাদাম বা উদ্ভিজ্জ দুধ বেছে নিন।" else "Avoid standard bovine milk and heavy dairy creams. calcium and protein provided via fish, eggs, almonds, and dairy alternatives.",
                                    bulletColor = Color(0xFF455A64)
                                )
                            }
                        }
                    }
                }
            }

            // SECTION 3: Regional Cuisines
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFECEFF1)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Restaurant,
                            contentDescription = null,
                            tint = Color(0xFF388E3C),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (isBengali) "রন্ধনশৈলী প্রাধিকার (Cuisine Styles)" else "Regional Cuisine Preferences",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF263238)
                        )
                    }

                    Text(
                        text = if (isBengali) "আপনার পছন্দের রান্নার ধরণ নির্বাচন করুন (একাধিক সম্ভব):" else "Select your favored cuisine styles (choose one or more):",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )

                    // Grid / Flow-style list of Chips for Cuisines
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        predefinedCuisines.forEach { pair ->
                            val key = pair.first
                            val label = pair.second
                            val isSelected = selectedCuisines.any { it.equals(key, ignoreCase = true) }

                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedCuisines = if (isSelected) {
                                        selectedCuisines.filterNot { it.equals(key, ignoreCase = true) }.toSet()
                                    } else {
                                        selectedCuisines + key
                                    }
                                },
                                label = {
                                    Text(
                                        text = label,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 12.sp
                                    )
                                },
                                leadingIcon = if (isSelected) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFE8F5E9),
                                    selectedLabelColor = Color(0xFF2E7D32),
                                    selectedLeadingIconColor = Color(0xFF2E7D32),
                                    containerColor = Color(0xFFF5F7F8),
                                    labelColor = Color(0xFF546E7A)
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = Color(0xFFCFD8DC),
                                    selectedBorderColor = Color(0xFFA5D6A7),
                                    borderWidth = 1.dp,
                                    selectedBorderWidth = 1.5.dp,
                                    enabled = true,
                                    selected = isSelected
                                )
                            )
                        }
                    }

                    // Custom Cuisine Text Input
                    OutlinedTextField(
                        value = customCuisinesInput,
                        onValueChange = { customCuisinesInput = it },
                        label = {
                            Text(
                                text = if (isBengali) "অন্যান্য রান্নার ধরণ (কমা দিয়ে লিখুন)" else "Other custom cuisines (comma-separated)",
                                fontSize = 12.sp
                            )
                        },
                        placeholder = {
                            Text(
                                text = "e.g., Turkish, Mexican",
                                fontSize = 12.sp,
                                color = Color.LightGray
                            )
                        },
                        leadingIcon = { Text("🍜", fontSize = 16.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF2E7D32),
                            unfocusedBorderColor = Color(0xFFCFD8DC)
                        ),
                        singleLine = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun AdvisoryItem(
    title: String,
    text: String,
    bulletColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .size(6.dp)
                .clip(RoundedCornerShape(50))
                .background(bulletColor)
        )
        Column {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = Color(0xFF5D4037)
            )
            Text(
                text = text,
                fontSize = 11.sp,
                color = Color(0xFF795548),
                lineHeight = 14.sp
            )
        }
    }
}

// FlowRow replacement inline support since FlowRow might be from foundation-layout which is available on newer Compose but wrapping safely:
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable FlowRowScope.() -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement,
        content = content
    )
}
