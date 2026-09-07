package com.dark.nyc.ui.screens.discover

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.dark.nyc.data.models.NYCUser
import com.dark.nyc.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

data class VibeCategory(
    val title: String,
    val filterKey: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    onChatClick: (NYCUser) -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val currentUserId = auth.currentUser?.uid ?: ""

    // State Variables
    var searchQuery by remember { mutableStateOf("") }
    var selectedVibe by remember { mutableStateOf("All") }
    var selectedBorough by remember { mutableStateOf("All") }

    val allUsers = remember { mutableStateListOf<NYCUser>() }
    var isLoading by remember { mutableStateOf(true) }

    // Inspect Profile State
    var inspectingUser by remember { mutableStateOf<NYCUser?>(null) }

    // Vibe Categories Matching Onboarding Personality Tags
    val vibeCategories = listOf(
        VibeCategory("✨ All Vibes", "All"),
        VibeCategory("☕ Coffee Lovers", "Coffee"),
        VibeCategory("🌃 Night Owls", "Night owl"),
        VibeCategory("🏋️ Gym & Fitness", "Gym"),
        VibeCategory("🍜 Foodies", "Food"),
        VibeCategory("🎵 Live Music", "Music"),
        VibeCategory("🎨 Creative Souls", "Creative"),
        VibeCategory("🐶 Dog Lovers", "Dog"),
        VibeCategory("☀️ Brunch Club", "Brunch")
    )

    val boroughs = listOf("All NYC", "Manhattan", "Brooklyn", "Queens", "Bronx", "Staten Island")

    // 1️⃣ FETCH ALL USERS FROM FIRESTORE (Real-time)
    DisposableEffect(currentUserId) {
        val listener = firestore.collection("nyc_users")
            .addSnapshotListener { snapshot, error ->
                if (snapshot != null) {
                    val users = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(NYCUser::class.java)?.let { u ->
                            if (u.uid.isBlank()) u.copy(uid = doc.id) else u
                        }
                    }.filter { it.uid != currentUserId } // Exclude self

                    allUsers.clear()
                    allUsers.addAll(users)
                }
                isLoading = false
            }

        onDispose { listener.remove() }
    }

    // 2️⃣ FILTER LOGIC (Dynamic & Reactive)
    val filteredProfiles = remember(allUsers.toList(), selectedVibe, selectedBorough, searchQuery) {
        allUsers.filter { user ->
            // Search Query Filter
            val matchesSearch = searchQuery.isBlank() ||
                    user.name.contains(searchQuery, ignoreCase = true) ||
                    user.neighborhood.contains(searchQuery, ignoreCase = true) ||
                    user.bio.contains(searchQuery, ignoreCase = true)

            // Borough Filter
            val matchesBorough = selectedBorough == "All NYC" ||
                    user.borough.equals(selectedBorough, ignoreCase = true)

            // Vibe / Personality Tag Filter
            val matchesVibe = if (selectedVibe == "All") {
                true
            } else {
                user.personalityTags.any { it.contains(selectedVibe, ignoreCase = true) } ||
                        user.nycEnergy.contains(selectedVibe, ignoreCase = true)
            }

            matchesSearch && matchesBorough && matchesVibe
        }
    }

    Scaffold(
        containerColor = PureWhite
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // --- HEADER TITLE (Lovora Clean UI) ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "DISCOVER BY VIBE",
                    style = MaterialTheme.typography.labelLarge,
                    color = NYC_Red,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Explore Lovora",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("🗽", fontSize = 22.sp)
                }
                Text(
                    text = "Filter NYC members by interests, energy & borough",
                    fontSize = 13.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by name, interests or area...", fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextSecondary)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NYC_Red,
                        unfocusedBorderColor = BorderLight,
                        focusedContainerColor = OffWhite,
                        unfocusedContainerColor = OffWhite
                    )
                )
            }

            // --- VIBE CATEGORY PILLS (Horizontal Scroll) ---
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                items(vibeCategories) { vibe ->
                    val isSelected = selectedVibe == vibe.filterKey
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50.dp))
                            .background(if (isSelected) NYC_Red else OffWhite)
                            .border(1.2.dp, if (isSelected) NYC_Red else BorderLight, RoundedCornerShape(50.dp))
                            .clickable { selectedVibe = vibe.filterKey }
                            .padding(horizontal = 14.dp, vertical = 9.dp)
                    ) {
                        Text(
                            text = vibe.title,
                            color = if (isSelected) Color.White else TextPrimary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.5.sp
                        )
                    }
                }
            }

            // --- BOROUGH FILTER PILLS ---
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                items(boroughs) { b ->
                    val isSelected = selectedBorough == b
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50.dp))
                            .background(if (isSelected) NYC_RedSurface else PureWhite)
                            .border(1.dp, if (isSelected) NYC_Red else BorderLight, RoundedCornerShape(50.dp))
                            .clickable { selectedBorough = b }
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = b,
                            fontSize = 11.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) NYC_RedDark else TextSecondary
                        )
                    }
                }
            }

            // Active Filter Result Count Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${filteredProfiles.size} Locals Found",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                if (selectedVibe != "All" || selectedBorough != "All NYC" || searchQuery.isNotEmpty()) {
                    Text(
                        text = "Reset Filters",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = NYC_Red,
                        modifier = Modifier.clickable {
                            selectedVibe = "All"
                            selectedBorough = "All NYC"
                            searchQuery = ""
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // --- USER PROFILES GRID ---
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NYC_Red, strokeWidth = 3.dp)
                }
            } else if (filteredProfiles.isEmpty()) {
                // Empty Search State
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(NYC_RedSurface, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PersonSearch, contentDescription = null, tint = NYC_Red, modifier = Modifier.size(40.dp))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No Profiles Found", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "No one matches this specific vibe right now. Try selecting another filter or borough!",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Button(
                            onClick = {
                                selectedVibe = "All"
                                selectedBorough = "All NYC"
                                searchQuery = ""
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NYC_Red),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text("Clear Filters", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredProfiles, key = { it.uid }) { user ->
                        DiscoverUserCard(
                            user = user,
                            onCardClick = { inspectingUser = user },
                            onChatClick = { onChatClick(user) }
                        )
                    }
                }
            }
        }

        // --- FULL DETAILED USER BOTTOMSHEET ---
        if (inspectingUser != null) {
            ModalBottomSheet(
                onDismissRequest = { inspectingUser = null },
                containerColor = PureWhite,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ) {
                DiscoverUserDetailSheet(
                    user = inspectingUser!!,
                    onStartChat = {
                        val target = inspectingUser!!
                        inspectingUser = null
                        onChatClick(target)
                    }
                )
            }
        }
    }
}

// ==========================================
// 1️⃣ DISCOVER USER CARD (SOLID NO BLEED)
// ==========================================
@Composable
fun DiscoverUserCard(
    user: NYCUser,
    onCardClick: () -> Unit,
    onChatClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(270.dp)
            .shadow(6.dp, RoundedCornerShape(20.dp), spotColor = Color.Black.copy(alpha = 0.08f))
            .clickable { onCardClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF16161A)) // Solid background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // User Photo
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(user.photoURL)
                    .crossfade(true)
                    .build(),
                contentDescription = user.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                loading = { CircularProgressIndicator(color = NYC_Red, strokeWidth = 2.dp) },
                error = {
                    Box(Modifier.fillMaxSize().background(Color(0xFFEEEEF2)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                    }
                }
            )

            // Gradient Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
                            startY = 220f
                        )
                    )
            )

            // Top Energy Tag
            Surface(
                shape = RoundedCornerShape(50.dp),
                color = Color.Black.copy(alpha = 0.6f),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
            ) {
                Text(
                    text = user.nycEnergy.ifBlank { "Chill Local" },
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }

            // Bottom Profile Info & Direct Chat CTA
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            ) {
                Text(
                    text = "${user.name}, ${user.age}",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "📍 ${user.neighborhood.ifBlank { user.borough }}",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Direct Chat Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(NYC_Red)
                        .clickable { onChatClick() },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.ChatBubble,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Say Hi 👋",
                        color = Color.White,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ==========================================
// 2️⃣ DETAILED PROFILE BOTTOM SHEET
// ==========================================
@Composable
fun DiscoverUserDetailSheet(
    user: NYCUser,
    onStartChat: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 36.dp)
    ) {
        // Image Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF1E1E24))
        ) {
            SubcomposeAsyncImage(
                model = user.photoURL,
                contentDescription = user.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("${user.name}, ${user.age}", fontSize = 24.sp, fontWeight = FontWeight.Black, color = TextPrimary)
                Text("📍 ${user.neighborhood}, ${user.borough}", fontSize = 14.sp, color = TextSecondary)
            }

            Surface(
                shape = RoundedCornerShape(50.dp),
                color = NYC_RedSurface
            ) {
                Text(
                    text = "⚡ ${user.nycEnergy.ifBlank { "Chill Local" }}",
                    fontWeight = FontWeight.Bold,
                    color = NYC_RedDark,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (user.bio.isNotBlank()) {
            Text("About Me", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(user.bio, fontSize = 14.sp, color = TextSecondary, lineHeight = 20.sp)
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (user.unpopularOpinion.isNotBlank()) {
            Text("NYC Hot Take 🔥", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NYC_Red)
            Spacer(modifier = Modifier.height(4.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = OffWhite),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "\"${user.unpopularOpinion}\"",
                    fontSize = 14.sp,
                    fontStyle = FontStyle.Italic,
                    color = TextPrimary,
                    modifier = Modifier.padding(14.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (user.personalityTags.isNotEmpty()) {
            Text("Interests & Vibe", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(user.personalityTags) { tag ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50.dp))
                            .background(OffWhite)
                            .border(1.dp, BorderLight, RoundedCornerShape(50.dp))
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Text(tag, fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(26.dp))

        // Start Chat CTA
        Button(
            onClick = onStartChat,
            colors = ButtonDefaults.buttonColors(containerColor = NYC_Red),
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Icon(Icons.Filled.ChatBubble, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Send Message to ${user.name} 💬", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}