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
    var selectedBorough by remember { mutableStateOf("All NYC") }

    val allUsers = remember { mutableStateListOf<NYCUser>() }
    var isLoading by remember { mutableStateOf(true) }

    // Inspect Profile State
    var inspectingUser by remember { mutableStateOf<NYCUser?>(null) }

    val vibeCategories = listOf(
        VibeCategory("✨ All Vibes", "All"),
        VibeCategory("☕ Coffee Lovers", "Coffee"),
        VibeCategory("🌃 Night Owls", "Night owl"),
        VibeCategory("🏋️ Fitness", "Gym"),
        VibeCategory("🍜 Foodies", "Food"),
        VibeCategory("🎵 Music", "Music"),
        VibeCategory("🎨 Creatives", "Creative"),
        VibeCategory("🐶 Pet Lovers", "Dog"),
        VibeCategory("☀️ Brunch", "Brunch")
    )

    val boroughs = listOf("All NYC", "Manhattan", "Brooklyn", "Queens", "Bronx", "Staten Island")

    // Real-time Firestore Listener
    DisposableEffect(currentUserId) {
        val listener = firestore.collection("nyc_users")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val users = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(NYCUser::class.java)?.let { u ->
                            if (u.uid.isBlank()) u.copy(uid = doc.id) else u
                        }
                    }.filter { it.uid != currentUserId }

                    allUsers.clear()
                    allUsers.addAll(users)
                }
                isLoading = false
            }

        onDispose { listener.remove() }
    }

    // Dynamic Filter Logic
    val filteredProfiles = remember(allUsers.toList(), selectedVibe, selectedBorough, searchQuery) {
        allUsers.filter { user ->
            val matchesSearch = searchQuery.isBlank() ||
                    user.name.contains(searchQuery, ignoreCase = true) ||
                    user.neighborhood.contains(searchQuery, ignoreCase = true) ||
                    user.bio.contains(searchQuery, ignoreCase = true)

            val matchesBorough = selectedBorough == "All NYC" ||
                    user.borough.equals(selectedBorough, ignoreCase = true)

            val matchesVibe = if (selectedVibe == "All") {
                true
            } else {
                user.personalityTags.any { it.contains(selectedVibe, ignoreCase = true) } ||
                        user.nycEnergy.contains(selectedVibe, ignoreCase = true)
            }

            matchesSearch && matchesBorough && matchesVibe
        }
    }

    // 🚀 FIXED: Nested Scaffold removed to eliminate excessive top margin/gap
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFB))
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // --- TOP COMPACT APP BAR (Like reference screenshot) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Discover",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E2022),
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "Find your vibe in NYC",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF9E9EA7)
                    )
                }

                // Compact Location Filter Chip
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFF4F5F7),
                    modifier = Modifier.clickable {
                        val nextIdx = (boroughs.indexOf(selectedBorough) + 1) % boroughs.size
                        selectedBorough = boroughs[nextIdx]
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = NYC_Red,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = selectedBorough,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF2D3142)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = Color(0xFF9E9EA7),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // --- COMPACT SEARCH BAR ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 20.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .clip(RoundedCornerShape(21.dp))
                        .background(Color(0xFFF4F5F7))
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color(0xFFA0A3BD),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    androidx.compose.foundation.text.BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    "Search by name, area or vibe...",
                                    fontSize = 13.sp,
                                    color = Color(0xFFA0A3BD)
                                )
                            }
                            innerTextField()
                        }
                    )
                    if (searchQuery.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = Color(0xFFA0A3BD),
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { searchQuery = "" }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // --- VIBE CHIPS ROW ---
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(vibeCategories) { vibe ->
                    val isSelected = selectedVibe == vibe.filterKey
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50.dp))
                            .background(if (isSelected) NYC_Red else Color.White)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) NYC_Red else Color(0xFFEBECEF),
                                shape = RoundedCornerShape(50.dp)
                            )
                            .clickable { selectedVibe = vibe.filterKey }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = vibe.title,
                            color = if (isSelected) Color.White else Color(0xFF4A4E69),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // --- RESULT COUNT & RESET BAR ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${filteredProfiles.size} Profiles Nearby",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4A4E69)
                )

                if (selectedVibe != "All" || selectedBorough != "All NYC" || searchQuery.isNotEmpty()) {
                    Text(
                        text = "Reset",
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

            // --- USER PROFILES GRID ---
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NYC_Red, strokeWidth = 2.5.dp, modifier = Modifier.size(32.dp))
                }
            } else if (filteredProfiles.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(NYC_Red.copy(alpha = 0.08f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.PersonSearch, contentDescription = null, tint = NYC_Red, modifier = Modifier.size(32.dp))
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No Profiles Found", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E2022))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Try switching your vibe filter or borough",
                            fontSize = 12.sp,
                            color = Color(0xFF9E9EA7),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 20.dp),
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

        // --- USER PROFILE BOTTOM SHEET ---
        if (inspectingUser != null) {
            ModalBottomSheet(
                onDismissRequest = { inspectingUser = null },
                containerColor = Color.White,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                dragHandle = {
                    Box(
                        modifier = Modifier
                            .padding(vertical = 10.dp)
                            .size(width = 38.dp, height = 4.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE0E0E6))
                    )
                }
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
// 1️⃣ DISCOVER USER CARD (CLEAN MODERN UI)
// ==========================================
@Composable
fun DiscoverUserCard(
    user: NYCUser,
    onCardClick: () -> Unit,
    onChatClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(255.dp)
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(22.dp),
                spotColor = Color(0x1A000000)
            )
            .clickable { onCardClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Profile Image
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(user.photoURL)
                    .crossfade(true)
                    .build(),
                contentDescription = user.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                loading = {
                    Box(Modifier.fillMaxSize().background(Color(0xFFE5E7EB)), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = NYC_Red, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                    }
                },
                error = {
                    Box(Modifier.fillMaxSize().background(Color(0xFFEEEEF2)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(40.dp))
                    }
                }
            )

            // Modern Smooth Gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.15f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.85f)
                            ),
                            startY = 0f,
                            endY = Float.POSITIVE_INFINITY
                        )
                    )
            )

            // Top Status Badge
            Surface(
                shape = RoundedCornerShape(50.dp),
                color = Color.Black.copy(alpha = 0.45f),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
            ) {
                Text(
                    text = user.nycEnergy.ifBlank { "NYC Local" },
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }

            // Bottom Profile Info & Chat Action
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
                    Text(
                        text = "${user.name}, ${user.age}",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = user.neighborhood.ifBlank { user.borough },
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Reference Mockup Style Rounded Chat Button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(NYC_Red, Color(0xFFFF5277))
                            )
                        )
                        .clickable { onChatClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.ChatBubble,
                        contentDescription = "Chat",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// ==========================================
// 2️⃣ MODERN PROFILE BOTTOM SHEET
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
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp)
    ) {
        // Clean Profile Image
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFFF3F4F6))
        ) {
            SubcomposeAsyncImage(
                model = user.photoURL,
                contentDescription = user.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Title and Borough
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "${user.name}, ${user.age}",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E2022)
                )
                Text(
                    text = "📍 ${user.neighborhood}, ${user.borough}",
                    fontSize = 13.sp,
                    color = Color(0xFF6C757D)
                )
            }

            Surface(
                shape = RoundedCornerShape(50.dp),
                color = NYC_Red.copy(alpha = 0.1f)
            ) {
                Text(
                    text = user.nycEnergy.ifBlank { "Chill Local" },
                    fontWeight = FontWeight.SemiBold,
                    color = NYC_Red,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (user.bio.isNotBlank()) {
            Text("About", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E2022))
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = user.bio,
                fontSize = 13.5.sp,
                color = Color(0xFF4A4E69),
                lineHeight = 19.sp
            )
            Spacer(modifier = Modifier.height(14.dp))
        }

        if (user.unpopularOpinion.isNotBlank()) {
            Text("NYC Hot Take 🔥", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NYC_Red)
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                color = Color(0xFFF8F9FA),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "\"${user.unpopularOpinion}\"",
                    fontSize = 13.sp,
                    fontStyle = FontStyle.Italic,
                    color = Color(0xFF2D3142),
                    modifier = Modifier.padding(12.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        if (user.personalityTags.isNotEmpty()) {
            Text("Interests", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E2022))
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(user.personalityTags) { tag ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50.dp))
                            .background(Color(0xFFF1F3F5))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = tag,
                            fontSize = 11.5.sp,
                            color = Color(0xFF495057),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        Button(
            onClick = onStartChat,
            colors = ButtonDefaults.buttonColors(containerColor = NYC_Red),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Icon(Icons.Filled.ChatBubble, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Send Message", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}