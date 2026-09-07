package com.dark.nyc.ui.screens.onboarding

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.dark.nyc.data.models.NYCUser
import com.dark.nyc.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

@OptIn(ExperimentalAnimationApi::class, ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    navController: NavController,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val storage = FirebaseStorage.getInstance()

    // SharedPreferences for Auto-Save & Resume Onboarding State
    val prefs = remember { context.getSharedPreferences("nyc_onboarding_draft", Context.MODE_PRIVATE) }

    // ===== STEP STATE =====
    val totalSteps = 4
    var currentStep by remember { mutableStateOf(prefs.getInt("draft_step", 1)) }

    // ===== STEP 1: PHOTO =====
    val savedUriStr = prefs.getString("draft_photo_uri", null)
    var selectedImageUri by remember { mutableStateOf<Uri?>(savedUriStr?.let { Uri.parse(it) }) }
    var isUploading by remember { mutableStateOf(false) }

    // ===== STEP 2: DETAILS =====
    var name by remember { mutableStateOf(prefs.getString("draft_name", "") ?: "") }
    var age by remember { mutableStateOf(prefs.getString("draft_age", "") ?: "") }
    var gender by remember { mutableStateOf(prefs.getString("draft_gender", "") ?: "") }
    val genders = listOf("Man", "Woman", "Non-binary")

    // ===== STEP 3: LOCATION =====
    var borough by remember { mutableStateOf(prefs.getString("draft_borough", "") ?: "") }
    var neighborhood by remember { mutableStateOf(prefs.getString("draft_neighborhood", "") ?: "") }
    val boroughs = listOf("Manhattan", "Brooklyn", "Queens", "Bronx", "Staten Island")
    val neighborhoods = mapOf(
        "Manhattan" to listOf("Upper East Side", "Greenwich Village", "SoHo", "Chelsea", "Harlem"),
        "Brooklyn" to listOf("Williamsburg", "Greenpoint", "DUMBO", "Park Slope", "Bushwick"),
        "Queens" to listOf("Astoria", "Long Island City", "Jackson Heights"),
        "Bronx" to listOf("Fordham", "Pelham Bay"),
        "Staten Island" to listOf("St. George", "Tottenville")
    )

    // ===== STEP 4: PERSONALITY =====
    val allPersonalityTags = listOf(
        "☕ Coffee person", "🎵 Live music lover", "🏋️ Gym rat",
        "🍜 Food explorer", "🐶 Dog person", "🎭 Theatre enthusiast",
        "🌳 Park wanderer", "🎨 Creative soul", "🌃 Night owl",
        "📚 Reader", "🚴 Cyclist", "☀️ Brunch enthusiast"
    )
    val savedTagsSet = prefs.getStringSet("draft_tags", emptySet()) ?: emptySet()
    var selectedTags by remember { mutableStateOf(savedTagsSet.toList()) }
    var unpopularOpinion by remember { mutableStateOf(prefs.getString("draft_opinion", "") ?: "") }
    val opinions = listOf(
        "Times Square is actually fun.",
        "Brunch is overrated.",
        "The subway isn't that bad.",
        "Brooklyn is better than Manhattan.",
        "NYC pizza is overhyped.",
        "I actually like the tourists."
    )
    var nycEnergy by remember { mutableStateOf(prefs.getString("draft_energy", "") ?: "") }
    val energies = listOf("Night Explorer", "Chill Local", "Foodie", "Social Butterfly", "Culture Seeker")

    // Save helper to persist changes immediately
    fun saveDraft() {
        prefs.edit {
            putInt("draft_step", currentStep)
            putString("draft_photo_uri", selectedImageUri?.toString())
            putString("draft_name", name)
            putString("draft_age", age)
            putString("draft_gender", gender)
            putString("draft_borough", borough)
            putString("draft_neighborhood", neighborhood)
            putStringSet("draft_tags", selectedTags.toSet())
            putString("draft_opinion", unpopularOpinion)
            putString("draft_energy", nycEnergy)
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            prefs.edit { putString("draft_photo_uri", uri.toString()) }
        }
    }

    // Modern Gradient for Button
    val primaryGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFFFF334B), Color(0xFFFF5E62))
    )

    val animatedProgress by animateFloatAsState(
        targetValue = currentStep.toFloat() / totalSteps.toFloat(),
        animationSpec = tween(400),
        label = "progressAnim"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureWhite)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 16.dp, bottom = 20.dp)
        ) {
            // --- TOP BAR (Back Indicator & Progress) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentStep > 1) {
                    IconButton(
                        onClick = {
                            currentStep--
                            saveDraft()
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(SoftGray)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }

                // Smooth Progress Bar
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(50.dp)),
                    color = NYC_Red,
                    trackColor = SoftGray
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Step count indicator
                Text(
                    text = "$currentStep/$totalSteps",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // --- STEP CONTENT (Animated Transition) ---
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        slideInHorizontally(
                            initialOffsetX = { if (targetState > initialState) it else -it },
                            animationSpec = tween(350)
                        ) + fadeIn(animationSpec = tween(350)) togetherWith
                                slideOutHorizontally(
                                    targetOffsetX = { if (targetState > initialState) -it else it },
                                    animationSpec = tween(350)
                                ) + fadeOut(animationSpec = tween(350))
                    },
                    label = "stepAnimation"
                ) { step ->
                    when (step) {
                        1 -> Step1Photo(selectedImageUri, { imagePicker.launch("image/*") }, isUploading)
                        2 -> Step2Details(
                            name = name,
                            onNameChange = { name = it; saveDraft() },
                            age = age,
                            onAgeChange = { age = it; saveDraft() },
                            gender = gender,
                            onGenderChange = { gender = it; saveDraft() },
                            genders = genders
                        )
                        3 -> Step3Location(
                            borough = borough,
                            onBoroughChange = { borough = it; saveDraft() },
                            neighborhood = neighborhood,
                            onNeighborhoodChange = { neighborhood = it; saveDraft() },
                            boroughs = boroughs,
                            neighborhoods = neighborhoods
                        )
                        4 -> Step4Personality(
                            allTags = allPersonalityTags,
                            selectedTags = selectedTags,
                            onTagToggle = { tag ->
                                selectedTags = if (selectedTags.contains(tag)) selectedTags - tag else selectedTags + tag
                                saveDraft()
                            },
                            unpopularOpinion = unpopularOpinion,
                            onOpinionChange = { unpopularOpinion = it; saveDraft() },
                            opinions = opinions,
                            nycEnergy = nycEnergy,
                            onEnergyChange = { nycEnergy = it; saveDraft() },
                            energies = energies
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- PRIMARY ACTION BUTTON ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(12.dp, RoundedCornerShape(28.dp), spotColor = Color(0xFFFF334B).copy(alpha = 0.4f))
                    .clip(RoundedCornerShape(28.dp))
                    .background(primaryGradient) // Visible gradient background
                    .clickable(enabled = !isUploading) {
                        when (currentStep) {
                            1 -> {
                                if (selectedImageUri == null) {
                                    Toast.makeText(context, "Please upload a photo to proceed", Toast.LENGTH_SHORT).show()
                                    return@clickable
                                }
                                currentStep++
                                saveDraft()
                            }
                            2 -> {
                                val parsedAge = age.toIntOrNull()
                                if (name.isBlank() || parsedAge == null || parsedAge < 18 || gender.isBlank()) {
                                    Toast.makeText(context, "Please enter valid info (Must be 18+)", Toast.LENGTH_SHORT).show()
                                    return@clickable
                                }
                                currentStep++
                                saveDraft()
                            }
                            3 -> {
                                if (borough.isBlank() || neighborhood.isBlank()) {
                                    Toast.makeText(context, "Please select your borough & neighborhood", Toast.LENGTH_SHORT).show()
                                    return@clickable
                                }
                                currentStep++
                                saveDraft()
                            }
                            4 -> {
                                if (selectedTags.size < 3) {
                                    Toast.makeText(context, "Please select at least 3 interests", Toast.LENGTH_SHORT).show()
                                    return@clickable
                                }
                                if (unpopularOpinion.isBlank() || nycEnergy.isBlank()) {
                                    Toast.makeText(context, "Please select an opinion and energy vibe", Toast.LENGTH_SHORT).show()
                                    return@clickable
                                }

                                // Final Upload & Profile Creation
                                isUploading = true
                                scope.launch {
                                    try {
                                        val uid = auth.currentUser?.uid ?: return@launch
                                        val email = auth.currentUser?.email ?: ""

                                        var photoUrl = ""
                                        if (selectedImageUri != null) {
                                            val ref = storage.reference.child("nyc_users/$uid/profile_${UUID.randomUUID()}.jpg")
                                            ref.putFile(selectedImageUri!!).await()
                                            photoUrl = ref.downloadUrl.await().toString()
                                        }

                                        val user = NYCUser(
                                            uid = uid,
                                            name = name.trim(),
                                            email = email,
                                            age = age.toInt(),
                                            gender = gender,
                                            borough = borough,
                                            neighborhood = neighborhood,
                                            photoURL = photoUrl,
                                            personalityTags = selectedTags,
                                            nycEnergy = nycEnergy,
                                            unpopularOpinion = unpopularOpinion,
                                            lookingFor = "Open to anything",
                                            onboardingComplete = true
                                        )

                                        firestore.collection("nyc_users")
                                            .document(uid)
                                            .set(user.toMap())
                                            .await()

                                        // Clear saved draft on completion
                                        prefs.edit().clear().apply()

                                        isUploading = false
                                        onComplete()
                                    } catch (e: Exception) {
                                        isUploading = false
                                        Toast.makeText(context, "Upload failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isUploading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(26.dp)
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (currentStep == totalSteps) "LET'S FIND NYC ✨" else "CONTINUE",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.2.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

// ===== STEP 1: PHOTO =====
@Composable
fun Step1Photo(
    selectedImageUri: Uri?,
    onPickImage: () -> Unit,
    isUploading: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("First Impressions", style = MaterialTheme.typography.labelLarge, color = NYC_Red, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text("Show NYC Who You Are", style = MaterialTheme.typography.headlineMedium, color = TextPrimary, fontWeight = FontWeight.ExtraBold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Add a clear photo of yourself. Clear, authentic shots match 4x faster!",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.padding(horizontal = 16.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(40.dp))

        Box(
            modifier = Modifier
                .size(210.dp)
                .shadow(16.dp, CircleShape, spotColor = NYC_Red.copy(alpha = 0.25f))
                .clip(CircleShape)
                .background(OffWhite)
                .border(3.5.dp, if (selectedImageUri != null) NYC_Red else BorderLight, CircleShape)
                .clickable(enabled = !isUploading) { onPickImage() },
            contentAlignment = Alignment.Center
        ) {
            if (selectedImageUri != null) {
                Image(
                    painter = rememberAsyncImagePainter(model = selectedImageUri),
                    contentDescription = "Profile Photo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .background(NYC_RedSurface, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddAPhoto,
                            contentDescription = "Add Photo",
                            tint = NYC_Red,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Upload Photo", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
            }
        }

        if (selectedImageUri != null) {
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = onPickImage,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, NYC_Red)
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = NYC_Red, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Change Photo", color = NYC_Red, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ===== STEP 2: DETAILS =====
@Composable
fun Step2Details(
    name: String, onNameChange: (String) -> Unit,
    age: String, onAgeChange: (String) -> Unit,
    gender: String, onGenderChange: (String) -> Unit,
    genders: List<String>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text("Basic Profile", style = MaterialTheme.typography.labelLarge, color = NYC_Red, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text("Tell Us About Yourself", style = MaterialTheme.typography.headlineMedium, color = TextPrimary, fontWeight = FontWeight.ExtraBold)

        Spacer(modifier = Modifier.height(28.dp))

        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("First Name") },
            placeholder = { Text("e.g. Alex") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = nycTextFieldColors(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(18.dp))

        OutlinedTextField(
            value = age,
            onValueChange = { if (it.length <= 2 && it.all { char -> char.isDigit() }) onAgeChange(it) },
            label = { Text("Age (Must be 18+)") },
            placeholder = { Text("e.g. 24") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = nycTextFieldColors(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(26.dp))
        Text("Gender Identity", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            genders.forEach { g ->
                val isSelected = gender == g
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) NYC_Red else OffWhite)
                        .border(1.5.dp, if (isSelected) NYC_Red else BorderLight, RoundedCornerShape(16.dp))
                        .clickable { onGenderChange(g) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = g,
                        color = if (isSelected) Color.White else TextPrimary,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

// ===== STEP 3: LOCATION =====
@Composable
fun Step3Location(
    borough: String, onBoroughChange: (String) -> Unit,
    neighborhood: String, onNeighborhoodChange: (String) -> Unit,
    boroughs: List<String>, neighborhoods: Map<String, List<String>>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text("Location", style = MaterialTheme.typography.labelLarge, color = NYC_Red, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text("Your NYC Turf", style = MaterialTheme.typography.headlineMedium, color = TextPrimary, fontWeight = FontWeight.ExtraBold)

        Spacer(modifier = Modifier.height(24.dp))
        Text("Select Borough", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(10.dp))

        boroughs.forEach { b ->
            val isSelected = borough == b
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .height(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isSelected) NYC_RedSurface else OffWhite)
                    .border(1.5.dp, if (isSelected) NYC_Red else BorderLight, RoundedCornerShape(16.dp))
                    .clickable {
                        onBoroughChange(b)
                        onNeighborhoodChange("")
                    }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = b,
                    color = if (isSelected) NYC_RedDark else TextPrimary,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 15.sp
                )
                RadioButton(
                    selected = isSelected,
                    onClick = {
                        onBoroughChange(b)
                        onNeighborhoodChange("")
                    },
                    colors = RadioButtonDefaults.colors(selectedColor = NYC_Red)
                )
            }
        }

        if (borough.isNotEmpty() && neighborhoods[borough] != null) {
            Spacer(modifier = Modifier.height(24.dp))
            Text("Select Neighborhood in $borough", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            neighborhoods[borough]?.forEach { n ->
                val isSelected = neighborhood == n
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .height(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSelected) NYC_Red else SoftGray)
                        .clickable { onNeighborhoodChange(n) }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = n,
                        color = if (isSelected) Color.White else TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                    if (isSelected) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}

// ===== STEP 4: PERSONALITY =====
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Step4Personality(
    allTags: List<String>, selectedTags: List<String>, onTagToggle: (String) -> Unit,
    unpopularOpinion: String, onOpinionChange: (String) -> Unit, opinions: List<String>,
    nycEnergy: String, onEnergyChange: (String) -> Unit, energies: List<String>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text("The NYC Vibe", style = MaterialTheme.typography.labelLarge, color = NYC_Red, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text("Your Personality & Takes", style = MaterialTheme.typography.headlineMedium, color = TextPrimary, fontWeight = FontWeight.ExtraBold)

        Spacer(modifier = Modifier.height(24.dp))
        Text("Pick Interests (${selectedTags.size}/3 min)", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        // Dynamic FlowRow for tags
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            allTags.forEach { tag ->
                val isSelected = selectedTags.contains(tag)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .background(if (isSelected) NYC_Red else OffWhite)
                        .border(1.2.dp, if (isSelected) NYC_Red else BorderLight, RoundedCornerShape(50.dp))
                        .clickable { onTagToggle(tag) }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tag,
                        color = if (isSelected) Color.White else TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Hot Take / Unpopular NYC Opinion", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(10.dp))

        opinions.forEach { op ->
            val isSelected = unpopularOpinion == op
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isSelected) NYC_RedSurface else OffWhite)
                    .border(1.2.dp, if (isSelected) NYC_Red else BorderLight, RoundedCornerShape(14.dp))
                    .clickable { onOpinionChange(op) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = op,
                    color = if (isSelected) NYC_RedDark else TextPrimary,
                    fontSize = 13.5.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.weight(1f)
                )
                if (isSelected) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = NYC_Red, modifier = Modifier.size(18.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Your NYC Energy", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(energies) { e ->
                val isSelected = nycEnergy == e
                Box(
                    modifier = Modifier
                        .height(44.dp)
                        .clip(RoundedCornerShape(50.dp))
                        .background(if (isSelected) NYC_Red else SoftGray)
                        .clickable { onEnergyChange(e) }
                        .padding(horizontal = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = e,
                        color = if (isSelected) Color.White else TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun nycTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = NYC_Red,
    unfocusedBorderColor = BorderLight,
    focusedLabelColor = NYC_Red,
    cursorColor = NYC_Red,
    focusedContainerColor = PureWhite,
    unfocusedContainerColor = OffWhite
)