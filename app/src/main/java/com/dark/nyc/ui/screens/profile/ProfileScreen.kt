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
        colors = listOf(NYC_Red, Color(0xFFFF5277))
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
                .addSnapshotListener { snapshot, _ ->
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
                Toast.makeText(context, "Profile updated ✨", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                isSaving = false
                Toast.makeText(context, "Failed to update: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FB))
    ) {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NYC_Red, strokeWidth = 2.5.dp, modifier = Modifier.size(36.dp))
            }
        } else {
            AnimatedContent(
                targetState = isEditing,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "profileTransition"
            ) { editing ->
                if (editing) {
                    // ==========================================
                    // ✏️ EDIT MODE (Aesthetic, Soft Input Fields)
                    // ==========================================
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        // Top Header Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Cancel",
                                color = Color(0xFF6C757D),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.clickable { isEditing = false; newImageUri = null }
                            )
                            Text("Edit Profile", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E2022))
                            Text(
                                text = if (isSaving) "Saving..." else "Done",
                                color = if (isSaving) Color(0xFFADB5BD) else NYC_Red,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable(enabled = !isSaving) { saveChanges() }
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Avatar Picker
                        Box(modifier = Modifier.align(Alignment.CenterHorizontally)) {
                            Box(
                                modifier = Modifier
                                    .size(110.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .border(2.dp, Color(0xFFEEF0F2), CircleShape)
                                    .clickable { imagePicker.launch("image/*") },
                                contentAlignment = Alignment.Center
                            ) {
                                SubcomposeAsyncImage(
                                    model = newImageUri ?: currentUser?.photoURL,
                                    contentDescription = "Edit Avatar",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                    error = { Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(44.dp)) }
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(NYC_Red)
                                    .clickable { imagePicker.launch("image/*") },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = "Change", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Fields
                        EditSectionTitle("BASICS")
                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = editName,
                            onValueChange = { editName = it },
                            label = { Text("Name") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = profileTextFieldColors(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = editAge,
                            onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) editAge = it },
                            label = { Text("Age (18+)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = profileTextFieldColors(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = editBio,
                            onValueChange = { editBio = it },
                            label = { Text("Bio") },
                            placeholder = { Text("Tell your story...") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = profileTextFieldColors(),
                            minLines = 3,
                            maxLines = 4
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        EditSectionTitle("GENDER")
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            genders.forEach { g ->
                                val isSelected = editGender == g
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) NYC_Red else Color.White)
                                        .border(1.dp, if (isSelected) NYC_Red else Color(0xFFE5E7EB), RoundedCornerShape(12.dp))
                                        .clickable { editGender = g },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = g,
                                        color = if (isSelected) Color.White else Color(0xFF4A4E69),
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        EditSectionTitle("BOROUGH")
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(boroughs) { b ->
                                val isSelected = editBorough == b
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50.dp))
                                        .background(if (isSelected) NYC_Red else Color.White)
                                        .border(1.dp, if (isSelected) NYC_Red else Color(0xFFE5E7EB), RoundedCornerShape(50.dp))
                                        .clickable {
                                            editBorough = b
                                            editNeighborhood = ""
                                        }
                                        .padding(horizontal = 14.dp, vertical = 7.dp)
                                ) {
                                    Text(b, color = if (isSelected) Color.White else Color(0xFF4A4E69), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }

                        if (editBorough.isNotEmpty() && neighborhoods[editBorough] != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Neighborhood in $editBorough", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF6C757D))
                            Spacer(modifier = Modifier.height(6.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(neighborhoods[editBorough]!!) { n ->
                                    val isSelected = editNeighborhood == n
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50.dp))
                                            .background(if (isSelected) NYC_Red.copy(alpha = 0.1f) else Color.White)
                                            .border(1.dp, if (isSelected) NYC_Red else Color(0xFFE5E7EB), RoundedCornerShape(50.dp))
                                            .clickable { editNeighborhood = n }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(n, color = if (isSelected) NYC_Red else Color(0xFF4A4E69), fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        EditSectionTitle("ENERGY & VIBE")
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(energies) { e ->
                                val isSelected = editEnergy == e
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50.dp))
                                        .background(if (isSelected) NYC_Red else Color.White)
                                        .border(1.dp, if (isSelected) NYC_Red else Color(0xFFE5E7EB), RoundedCornerShape(50.dp))
                                        .clickable { editEnergy = e }
                                        .padding(horizontal = 14.dp, vertical = 7.dp)
                                ) {
                                    Text(e, color = if (isSelected) Color.White else Color(0xFF4A4E69), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        EditSectionTitle("NYC HOT TAKE 🔥")
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = editOpinion,
                            onValueChange = { editOpinion = it },
                            placeholder = { Text("e.g. Brunch lines are a scam.") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = profileTextFieldColors()
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        EditSectionTitle("INTERESTS")
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            allPersonalityTags.forEach { tag ->
                                val isSelected = editTags.contains(tag)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50.dp))
                                        .background(if (isSelected) NYC_Red else Color.White)
                                        .border(1.dp, if (isSelected) NYC_Red else Color(0xFFE5E7EB), RoundedCornerShape(50.dp))
                                        .clickable {
                                            editTags = if (isSelected) editTags - tag else editTags + tag
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(tag, color = if (isSelected) Color.White else Color(0xFF4A4E69), fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(26.dp))

                        Button(
                            onClick = { saveChanges() },
                            enabled = !isSaving,
                            colors = ButtonDefaults.buttonColors(containerColor = NYC_Red),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text("Save Profile", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                } else {
                    // ==========================================
                    // 👤 VIEW MODE (CLEAN, MINIMAL & PROFESSIONAL)
                    // ==========================================
                    val user = currentUser ?: NYCUser(name = "Sway Member", age = 24)

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Top Action Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Profile",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E2022),
                                letterSpacing = (-0.5).sp
                            )

                            IconButton(
                                onClick = { isEditing = true },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .border(1.dp, Color(0xFFEEF0F2), CircleShape)
                            ) {
                                Icon(Icons.Outlined.Edit, contentDescription = "Edit", tint = Color(0xFF2D3142), modifier = Modifier.size(18.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Avatar Hero
                        Box(
                            modifier = Modifier
                                .size(112.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(2.5.dp, Color.White, CircleShape)
                                .shadow(8.dp, CircleShape, spotColor = Color(0x1A000000)),
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
                                error = { Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp)) }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Name, Age & Verified Status
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${user.name}, ${user.age}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E2022)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Filled.CheckCircle, contentDescription = "Verified", tint = Color(0xFF2ED573), modifier = Modifier.size(18.dp))
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF9E9EA7), modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "${user.neighborhood.ifBlank { "Manhattan" }}, ${user.borough.ifBlank { "NYC" }}",
                                fontSize = 12.5.sp,
                                color = Color(0xFF6C757D),
                                fontWeight = FontWeight.Normal
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // --- ABOUT ME CARD ---
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.White,
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(2.dp, RoundedCornerShape(20.dp), spotColor = Color(0x0A000000))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("About", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA0A3BD))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = user.bio.ifBlank { "Living the NYC dream and connecting with genuine people ✨" },
                                    fontSize = 13.5.sp,
                                    color = Color(0xFF2D3142),
                                    lineHeight = 19.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // --- VIBE & HOT TAKE CARD ---
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.White,
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(2.dp, RoundedCornerShape(20.dp), spotColor = Color(0x0A000000))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Energy", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA0A3BD))
                                    Surface(
                                        shape = RoundedCornerShape(50.dp),
                                        color = NYC_Red.copy(alpha = 0.08f)
                                    ) {
                                        Text(
                                            text = "⚡ ${user.nycEnergy.ifBlank { "Chill Local" }}",
                                            color = NYC_Red,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                if (user.unpopularOpinion.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("Hot Take 🔥", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NYC_Red)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "\"${user.unpopularOpinion}\"",
                                        fontSize = 13.sp,
                                        fontStyle = FontStyle.Italic,
                                        color = Color(0xFF2D3142)
                                    )
                                }
                            }
                        }

                        // --- INTERESTS TAGS ---
                        if (user.personalityTags.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color.White,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(2.dp, RoundedCornerShape(20.dp), spotColor = Color(0x0A000000))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Interests", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA0A3BD))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        user.personalityTags.forEach { tag ->
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(50.dp))
                                                    .background(Color(0xFFF4F5F7))
                                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Text(tag, fontSize = 11.5.sp, fontWeight = FontWeight.Medium, color = Color(0xFF4A4E69))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(22.dp))

                        // Edit Profile Action Button
                        Button(
                            onClick = { isEditing = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Text("Edit Profile", color = Color(0xFF1E2022), fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Log Out Action
                        Text(
                            text = "Log Out",
                            color = Color(0xFFFF4757),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .clickable { showLogoutDialog = true }
                                .padding(vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // LOGOUT CONFIRMATION DIALOG
        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = { Text("Log Out of Sway?", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
                text = { Text("Are you sure you want to log out from your profile?", fontSize = 13.sp, color = Color(0xFF6C757D)) },
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
                        Text("Log Out", color = NYC_Red, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) {
                        Text("Cancel", color = Color(0xFF6C757D), fontWeight = FontWeight.Medium)
                    }
                },
                shape = RoundedCornerShape(22.dp),
                containerColor = Color.White
            )
        }
    }
}

@Composable
fun EditSectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFFA0A3BD),
        letterSpacing = 0.5.sp
    )
}

@Composable
fun profileTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = NYC_Red,
    unfocusedBorderColor = Color(0xFFEEF0F2),
    focusedLabelColor = NYC_Red,
    cursorColor = NYC_Red,
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White
)