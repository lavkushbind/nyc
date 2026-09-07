package com.dark.nyc.ui.screens.profile

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.dark.nyc.data.models.NYCUser
import com.dark.nyc.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val storage = FirebaseStorage.getInstance()

    val currentUid = auth.currentUser?.uid ?: ""

    // User State
    var currentUser by remember { mutableStateOf<NYCUser?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Toggle Modes
    var isEditing by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Edit Fields State
    var editName by remember { mutableStateOf("") }
    var editAge by remember { mutableStateOf("") }
    var editGender by remember { mutableStateOf("") }
    var editBio by remember { mutableStateOf("") }
    var editBorough by remember { mutableStateOf("") }
    var editNeighborhood by remember { mutableStateOf("") }
    var editEnergy by remember { mutableStateOf("") }
    var editOpinion by remember { mutableStateOf("") }
    var editTags by remember { mutableStateOf<List<String>>(emptyList()) }
    var newImageUri by remember { mutableStateOf<Uri?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    // Static Options (Matching Onboarding)
    val genders = listOf("Man", "Woman", "Non-binary")
    val boroughs = listOf("Manhattan", "Brooklyn", "Queens", "Bronx", "Staten Island")
    val neighborhoods = mapOf(
        "Manhattan" to listOf("SoHo", "Upper East Side", "Greenwich Village", "Chelsea", "Harlem"),
        "Brooklyn" to listOf("Williamsburg", "DUMBO", "Greenpoint", "Park Slope", "Bushwick"),
        "Queens" to listOf("Astoria", "Long Island City", "Jackson Heights"),
        "Bronx" to listOf("Fordham", "Pelham Bay"),
        "Staten Island" to listOf("St. George", "Tottenville")
    )
    val energies = listOf("Chill Local", "Night Explorer", "Foodie", "Social Butterfly", "Culture Seeker")
    val allPersonalityTags = listOf(
        "☕ Coffee person", "🎵 Live music lover", "🏋️ Gym rat",
        "🍜 Food explorer", "🐶 Dog person", "🎭 Theatre enthusiast",
        "🌳 Park wanderer", "🎨 Creative soul", "🌃 Night owl",
        "📚 Reader", "🚴 Cyclist", "☀️ Brunch enthusiast"
    )

    val primaryGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFFFF334B), Color(0xFFFF5E62))
    )

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) newImageUri = uri
    }

    // 1️⃣ LISTEN TO LIVE USER DATA
    LaunchedEffect(currentUid) {
        if (currentUid.isNotEmpty()) {
            firestore.collection("nyc_users").document(currentUid)
                .addSnapshotListener { snapshot, error ->
                    if (snapshot != null && snapshot.exists()) {
                        val user = snapshot.toObject(NYCUser::class.java)
                        if (user != null) {
                            currentUser = if (user.uid.isBlank()) user.copy(uid = currentUid) else user
                            editName = user.name
                            editAge = if (user.age > 0) user.age.toString() else ""
                            editGender = user.gender
                            editBio = user.bio
                            editBorough = user.borough
                            editNeighborhood = user.neighborhood
                            editEnergy = user.nycEnergy
                            editOpinion = user.unpopularOpinion
                            editTags = user.personalityTags
                        }
                    }
                    isLoading = false
                }
        } else {
            isLoading = false
        }
    }

    // SAVE EDITED DETAILS
    fun saveChanges() {
        if (editName.isBlank() || editAge.toIntOrNull() == null || (editAge.toIntOrNull() ?: 0) < 18) {
            Toast.makeText(context, "Please enter a valid name and age (18+)", Toast.LENGTH_SHORT).show()
            return
        }

        isSaving = true
        scope.launch {
            try {
                var photoUrl = currentUser?.photoURL ?: ""

                // Upload new image if chosen
                if (newImageUri != null) {
                    val ref = storage.reference.child("nyc_users/$currentUid/profile_${UUID.randomUUID()}.jpg")
                    ref.putFile(newImageUri!!).await()
                    photoUrl = ref.downloadUrl.await().toString()
                }

                val updateData = mapOf(
                    "name" to editName.trim(),
                    "age" to editAge.toInt(),
                    "gender" to editGender,
                    "bio" to editBio.trim(),
                    "borough" to editBorough,
                    "neighborhood" to editNeighborhood,
                    "nycEnergy" to editEnergy,
                    "unpopularOpinion" to editOpinion.trim(),
                    "personalityTags" to editTags,
                    "photoURL" to photoUrl
                )

                firestore.collection("nyc_users").document(currentUid)
                    .update(updateData)
                    .await()

                isSaving = false
                isEditing = false
                newImageUri = null
                Toast.makeText(context, "Lovora Profile Updated ✨", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                isSaving = false
                Toast.makeText(context, "Failed to update: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureWhite)
    ) {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NYC_Red, strokeWidth = 3.dp)
            }
        } else {
            AnimatedContent(
                targetState = isEditing,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "profileTransition"
            ) { editing ->
                if (editing) {
                    // ==========================================
                    // ✏️ EDIT MODE (Matches Onboarding Form)
                    // ==========================================
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp)
                            .padding(top = 16.dp, bottom = 32.dp)
                    ) {
                        // Top Bar Actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { isEditing = false; newImageUri = null }) {
                                Text("Cancel", color = TextSecondary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Text("Edit Lovora Profile", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            TextButton(onClick = { saveChanges() }, enabled = !isSaving) {
                                if (isSaving) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = NYC_Red, strokeWidth = 2.dp)
                                } else {
                                    Text("Save", color = NYC_Red, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Avatar Changer
                        Box(
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(130.dp)
                                    .shadow(12.dp, CircleShape, spotColor = NYC_Red.copy(alpha = 0.25f))
                                    .clip(CircleShape)
                                    .background(OffWhite)
                                    .border(3.dp, NYC_Red, CircleShape)
                                    .clickable { imagePicker.launch("image/*") },
                                contentAlignment = Alignment.Center
                            ) {
                                SubcomposeAsyncImage(
                                    model = newImageUri ?: currentUser?.photoURL,
                                    contentDescription = "Edit Avatar",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                    loading = { CircularProgressIndicator(color = NYC_Red, strokeWidth = 2.dp) },
                                    error = { Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(54.dp)) }
                                )
                            }

                            FilledIconButton(
                                onClick = { imagePicker.launch("image/*") },
                                colors = IconButtonDefaults.filledIconButtonColors(containerColor = NYC_Red),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(38.dp)
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = "Change", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        // Section 1: Basic Info
                        Text("BASIC INFO", style = MaterialTheme.typography.labelLarge, color = NYC_Red, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = editName,
                            onValueChange = { editName = it },
                            label = { Text("First Name") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = profileTextFieldColors(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = editAge,
                            onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) editAge = it },
                            label = { Text("Age (18+)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = profileTextFieldColors(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = editBio,
                            onValueChange = { editBio = it },
                            label = { Text("About Me / Bio") },
                            placeholder = { Text("Share what you love about NYC life...") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = profileTextFieldColors(),
                            minLines = 3,
                            maxLines = 4
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Gender Identity Selector
                        Text("GENDER IDENTITY", style = MaterialTheme.typography.labelLarge, color = NYC_Red, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            genders.forEach { g ->
                                val isSelected = editGender == g
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(46.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (isSelected) NYC_Red else OffWhite)
                                        .border(1.5.dp, if (isSelected) NYC_Red else BorderLight, RoundedCornerShape(14.dp))
                                        .clickable { editGender = g },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = g,
                                        color = if (isSelected) Color.White else TextPrimary,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Section 2: Borough & Neighborhood
                        Text("LOCATION & BOROUGH", style = MaterialTheme.typography.labelLarge, color = NYC_Red, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(10.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(boroughs) { b ->
                                val isSelected = editBorough == b
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50.dp))
                                        .background(if (isSelected) NYC_Red else OffWhite)
                                        .border(1.2.dp, if (isSelected) NYC_Red else BorderLight, RoundedCornerShape(50.dp))
                                        .clickable {
                                            editBorough = b
                                            editNeighborhood = ""
                                        }
                                        .padding(horizontal = 16.dp, vertical = 10.dp)
                                ) {
                                    Text(b, color = if (isSelected) Color.White else TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

                        if (editBorough.isNotEmpty() && neighborhoods[editBorough] != null) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Text("Neighborhood in $editBorough", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(neighborhoods[editBorough]!!) { n ->
                                    val isSelected = editNeighborhood == n
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50.dp))
                                            .background(if (isSelected) NYC_RedSurface else OffWhite)
                                            .border(1.2.dp, if (isSelected) NYC_Red else BorderLight, RoundedCornerShape(50.dp))
                                            .clickable { editNeighborhood = n }
                                            .padding(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Text(n, color = if (isSelected) NYC_RedDark else TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Section 3: Vibe & Energy
                        Text("LOVORA ENERGY", style = MaterialTheme.typography.labelLarge, color = NYC_Red, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(10.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(energies) { e ->
                                val isSelected = editEnergy == e
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50.dp))
                                        .background(if (isSelected) NYC_Red else OffWhite)
                                        .border(1.2.dp, if (isSelected) NYC_Red else BorderLight, RoundedCornerShape(50.dp))
                                        .clickable { editEnergy = e }
                                        .padding(horizontal = 16.dp, vertical = 9.dp)
                                ) {
                                    Text(e, color = if (isSelected) Color.White else TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Hot Take
                        Text("UNPOPULAR NYC OPINION", style = MaterialTheme.typography.labelLarge, color = NYC_Red, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = editOpinion,
                            onValueChange = { editOpinion = it },
                            placeholder = { Text("e.g. Brunch is overrated.") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = profileTextFieldColors()
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Tags
                        Text("INTERESTS & PASSIONS", style = MaterialTheme.typography.labelLarge, color = NYC_Red, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(10.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            allPersonalityTags.forEach { tag ->
                                val isSelected = editTags.contains(tag)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50.dp))
                                        .background(if (isSelected) NYC_Red else OffWhite)
                                        .border(1.2.dp, if (isSelected) NYC_Red else BorderLight, RoundedCornerShape(50.dp))
                                        .clickable {
                                            editTags = if (isSelected) editTags - tag else editTags + tag
                                        }
                                        .padding(horizontal = 14.dp, vertical = 9.dp)
                                ) {
                                    Text(tag, color = if (isSelected) Color.White else TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // Save Button (Gradient)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .shadow(10.dp, RoundedCornerShape(28.dp), spotColor = Color(0xFFFF334B).copy(alpha = 0.4f))
                                .clip(RoundedCornerShape(28.dp))
                                .background(primaryGradient)
                                .clickable(enabled = !isSaving) { saveChanges() },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                            } else {
                                Text("SAVE LOVORA PROFILE ✨", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                            }
                        }
                    }
                } else {
                    // ==========================================
                    // 👤 VIEW MODE (Clean & Ultra-Polished)
                    // ==========================================
                    val user = currentUser ?: NYCUser(name = "Lovora Member", age = 24)

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp)
                            .padding(top = 20.dp, bottom = 36.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Screen Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("MY PROFILE", style = MaterialTheme.typography.labelLarge, color = NYC_Red, fontWeight = FontWeight.Bold)
                                Text("Lovora NYC", style = MaterialTheme.typography.headlineMedium, color = TextPrimary, fontWeight = FontWeight.ExtraBold)
                            }

                            FilledIconButton(
                                onClick = { isEditing = true },
                                colors = IconButtonDefaults.filledIconButtonColors(containerColor = OffWhite),
                                modifier = Modifier
                                    .size(44.dp)
                                    .border(1.dp, BorderLight, CircleShape)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = NYC_Red, modifier = Modifier.size(20.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        // Profile Avatar
                        Box(
                            modifier = Modifier
                                .size(140.dp)
                                .shadow(16.dp, CircleShape, spotColor = NYC_Red.copy(alpha = 0.3f))
                                .clip(CircleShape)
                                .background(OffWhite)
                                .border(3.5.dp, NYC_Red, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            SubcomposeAsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(user.photoURL)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = user.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                loading = { CircularProgressIndicator(color = NYC_Red, strokeWidth = 2.dp) },
                                error = { Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(60.dp)) }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // User Name & Age
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${user.name}, ${user.age}",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.Filled.Verified, contentDescription = "Verified", tint = Color(0xFF3498DB), modifier = Modifier.size(22.dp))
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "📍 ${user.neighborhood.ifBlank { "Manhattan" }}, ${user.borough.ifBlank { "NYC" }}",
                            fontSize = 14.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // VIP Badge Pill
                        Surface(
                            shape = RoundedCornerShape(50.dp),
                            color = NYC_RedSurface,
                            border = BorderStroke(1.dp, NYC_Red.copy(alpha = 0.3f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text("✨ Lovora VIP Member", color = NYC_RedDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        // --- ABOUT ME CARD ---
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = OffWhite,
                            border = BorderStroke(1.dp, BorderLight),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Text("ABOUT ME", style = MaterialTheme.typography.labelLarge, color = NYC_Red, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = user.bio.ifBlank { "Living the NYC dream and connecting with authentic people ✨" },
                                    fontSize = 14.5.sp,
                                    color = TextPrimary,
                                    lineHeight = 22.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // --- NYC ENERGY & HOT TAKE CARD ---
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = OffWhite,
                            border = BorderStroke(1.dp, BorderLight),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Text("LOVORA VIBE", style = MaterialTheme.typography.labelLarge, color = NYC_Red, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(10.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("⚡ Energy:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(50.dp),
                                        color = NYC_RedSurface
                                    ) {
                                        Text(
                                            text = user.nycEnergy.ifBlank { "Chill Local" },
                                            color = NYC_RedDark,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                if (user.unpopularOpinion.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text("Hot Take 🔥", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NYC_Red)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "\"${user.unpopularOpinion}\"",
                                        fontSize = 14.sp,
                                        fontStyle = FontStyle.Italic,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }

                        // --- PERSONALITY TAGS CARD ---
                        if (user.personalityTags.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = OffWhite,
                                border = BorderStroke(1.dp, BorderLight),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    Text("INTERESTS", style = MaterialTheme.typography.labelLarge, color = NYC_Red, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        user.personalityTags.forEach { tag ->
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(50.dp))
                                                    .background(PureWhite)
                                                    .border(1.dp, BorderLight, RoundedCornerShape(50.dp))
                                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Text(tag, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        // Edit Button (Gradient CTA)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .shadow(8.dp, RoundedCornerShape(28.dp), spotColor = Color(0xFFFF334B).copy(alpha = 0.3f))
                                .clip(RoundedCornerShape(28.dp))
                                .background(primaryGradient)
                                .clickable { isEditing = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("EDIT PROFILE", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, letterSpacing = 1.sp)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Log Out Button
                        OutlinedButton(
                            onClick = { showLogoutDialog = true },
                            shape = RoundedCornerShape(28.dp),
                            border = BorderStroke(1.dp, Color(0xFFFF5252)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Icon(Icons.Default.ExitToApp, contentDescription = null, tint = Color(0xFFFF5252))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Log Out of Lovora", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        // LOGOUT CONFIRMATION DIALOG
        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = { Text("Log Out of Lovora?", fontWeight = FontWeight.Bold) },
                text = { Text("Are you sure you want to log out from your profile?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showLogoutDialog = false
                            auth.signOut()
                            navController.navigate("login") {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    ) {
                        Text("Log Out", color = Color.Red, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) {
                        Text("Cancel", color = TextSecondary, fontWeight = FontWeight.SemiBold)
                    }
                },
                shape = RoundedCornerShape(24.dp),
                containerColor = PureWhite
            )
        }
    }
}

@Composable
fun profileTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = NYC_Red,
    unfocusedBorderColor = BorderLight,
    focusedLabelColor = NYC_Red,
    cursorColor = NYC_Red,
    focusedContainerColor = PureWhite,
    unfocusedContainerColor = OffWhite
)