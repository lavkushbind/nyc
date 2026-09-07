package com.dark.nyc.ui.screens.matches

import android.widget.Toast
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// Data class for Matches Screen Item
data class MatchCardItem(
    val matchId: String,
    val user: NYCUser,
    val matchedAt: Long,
    val isMutual: Boolean = true
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchesScreen(
    onChatClick: (NYCUser) -> Unit,
    onExploreClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val currentUserId = auth.currentUser?.uid ?: ""

    // State Variables
    var selectedTopTab by remember { mutableStateOf(0) } // 0 = Mutual Matches, 1 = Likes You (VIP)
    var selectedBoroughFilter by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    val mutualMatches = remember { mutableStateListOf<MatchCardItem>() }
    val likesReceived = remember { mutableStateListOf<NYCUser>() }
    var isLoading by remember { mutableStateOf(true) }

    // Dialog & BottomSheet State
    var inspectingUser by remember { mutableStateOf<NYCUser?>(null) }
    var unmatchTarget by remember { mutableStateOf<MatchCardItem?>(null) }

    val boroughs = listOf("All", "Manhattan", "Brooklyn", "Queens", "Bronx", "Staten Island")

    // 1️⃣ REAL-TIME FIRESTORE LISTENER FOR MUTUAL MATCHES
    DisposableEffect(currentUserId) {
        if (currentUserId.isBlank()) {
            isLoading = false
            return@DisposableEffect onDispose {}
        }

        // Listen for mutual matches in nyc_matches
        val matchListener = firestore.collection("nyc_matches")
            .whereArrayContains("users", currentUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    isLoading = false
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    for (doc in snapshot.documents) {
                        val matchId = doc.id
                        val usersList = doc.get("users") as? List<*> ?: emptyList<String>()
                        val otherUserId = usersList.firstOrNull { it != currentUserId } as? String ?: continue
                        val matchedAt = doc.getLong("matchedAt") ?: System.currentTimeMillis()

                        firestore.collection("nyc_users").document(otherUserId)
                            .get()
                            .addOnSuccessListener { userDoc ->
                                val user = userDoc.toObject(NYCUser::class.java)
                                if (user != null) {
                                    val safeUser = if (user.uid.isBlank()) user.copy(uid = otherUserId) else user
                                    mutualMatches.removeAll { it.matchId == matchId }
                                    mutualMatches.add(MatchCardItem(matchId, safeUser, matchedAt, isMutual = true))
                                    mutualMatches.sortByDescending { it.matchedAt }
                                }
                            }
                    }
                    isLoading = false
                }
            }

        // Listen for incoming likes (People who swiped right on current user)
        val likesListener = firestore.collection("nyc_swipes")
            .whereEqualTo("targetId", currentUserId)
            .whereEqualTo("isLike", true)
            .addSnapshotListener { snapshot, error ->
                if (snapshot != null) {
                    for (doc in snapshot.documents) {
                        val swiperId = doc.getString("swiperId") ?: continue
                        // Check if already in mutual matches, if so, skip
                        firestore.collection("nyc_users").document(swiperId)
                            .get()
                            .addOnSuccessListener { userDoc ->
                                val user = userDoc.toObject(NYCUser::class.java)
                                if (user != null) {
                                    val safeUser = if (user.uid.isBlank()) user.copy(uid = swiperId) else user
                                    if (likesReceived.none { it.uid == safeUser.uid }) {
                                        likesReceived.add(safeUser)
                                    }
                                }
                            }
                    }
                }
            }

        onDispose {
            matchListener.remove()
            likesListener.remove()
        }
    }

    // Filter by Borough & Search
    val displayedMatches = remember(mutualMatches.toList(), selectedBoroughFilter, searchQuery) {
        mutualMatches.filter {
            (selectedBoroughFilter == "All" || it.user.borough.equals(selectedBoroughFilter, ignoreCase = true)) &&
                    (searchQuery.isBlank() || it.user.name.contains(searchQuery, ignoreCase = true) || it.user.neighborhood.contains(searchQuery, ignoreCase = true))
        }
    }

    val displayedLikes = remember(likesReceived.toList(), selectedBoroughFilter, searchQuery) {
        likesReceived.filter {
            (selectedBoroughFilter == "All" || it.borough.equals(selectedBoroughFilter, ignoreCase = true)) &&
                    (searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true) || it.neighborhood.contains(searchQuery, ignoreCase = true))
        }
    }

    Scaffold(
        containerColor = Color(0xFFF9FAFB)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // --- TOP APP BAR & TITLE ---
            Surface(
                color = PureWhite,
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Connections",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Black,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("🗽", fontSize = 24.sp)
                            }
                            Text(
                                text = "Your NYC dating circle & admirers",
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                        }

                        IconButton(
                            onClick = { isSearchActive = !isSearchActive },
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF3F4F6))
                        ) {
                            Icon(
                                imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = "Search",
                                tint = TextPrimary
                            )
                        }
                    }

                    // Collapsible Search Bar
                    AnimatedVisibility(visible = isSearchActive) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search by name or neighborhood...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NYC_Red,
                                unfocusedBorderColor = Color(0xFFE5E7EB),
                                focusedContainerColor = Color(0xFFF9FAFB),
                                unfocusedContainerColor = Color(0xFFF9FAFB)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // --- PRIMARY TABS: Mutual Matches vs Likes You (VIP) ---
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xFFF3F4F6))
                            .padding(4.dp)
                    ) {
                        // Tab 1: Mutual Matches
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (selectedTopTab == 0) PureWhite else Color.Transparent)
                                .clickable { selectedTopTab = 0 },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Matches",
                                    fontWeight = if (selectedTopTab == 0) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedTopTab == 0) NYC_Red else TextSecondary,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = if (selectedTopTab == 0) NYC_Red else Color(0xFFD1D5DB)
                                ) {
                                    Text(
                                        text = "${mutualMatches.size}",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        // Tab 2: Likes You (VIP Unlocked)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (selectedTopTab == 1) PureWhite else Color.Transparent)
                                .clickable { selectedTopTab = 1 },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Likes You ❤️",
                                    fontWeight = if (selectedTopTab == 1) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedTopTab == 1) Color(0xFFFF9F1A) else TextSecondary,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFFF9F1A)
                                ) {
                                    Text(
                                        text = "${likesReceived.size}",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- BOROUGH FILTER CHIPS ---
            LazyRow(
                modifier = Modifier.padding(vertical = 12.dp),
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(boroughs) { borough ->
                    val isSelected = selectedBoroughFilter == borough
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedBoroughFilter = borough },
                        label = { Text(borough, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NYC_Red,
                            selectedLabelColor = Color.White,
                            containerColor = PureWhite,
                            labelColor = TextPrimary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) NYC_Red else Color(0xFFE5E7EB)
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }

            // --- CONTENT GRID ---
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NYC_Red, strokeWidth = 3.dp)
                }
            } else {
                if (selectedTopTab == 0) {
                    // TAB 1: MUTUAL MATCHES
                    if (displayedMatches.isEmpty()) {
                        EmptyMatchesState(
                            title = "No Matches Yet",
                            description = "Keep swiping on Explore! As soon as someone likes you back, they will appear right here.",
                            buttonText = "Start Swiping 🔥",
                            onAction = onExploreClick
                        )
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(displayedMatches, key = { it.matchId }) { matchItem ->
                                MatchGridCard(
                                    matchItem = matchItem,
                                    onChatClick = { onChatClick(matchItem.user) },
                                    onInfoClick = { inspectingUser = matchItem.user },
                                    onOptionsClick = { unmatchTarget = matchItem }
                                )
                            }
                        }
                    }
                } else {
                    // TAB 2: LIKES YOU (VIP UNLOCKED)
                    if (displayedLikes.isEmpty()) {
                        EmptyMatchesState(
                            title = "No Admirers Right Now",
                            description = "Your profile is active in NYC. As soon as someone likes you, their profile will appear here instantly!",
                            buttonText = "Boost Your Profile ✨",
                            onAction = onExploreClick
                        )
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(displayedLikes, key = { it.uid }) { admirer ->
                                AdmirerGridCard(
                                    user = admirer,
                                    onMatchBack = {
                                        // Create instant match
                                        scope.launch {
                                            firestore.collection("nyc_matches").add(
                                                mapOf(
                                                    "users" to listOf(currentUserId, admirer.uid),
                                                    "matchedAt" to System.currentTimeMillis()
                                                )
                                            ).await()
                                            likesReceived.remove(admirer)
                                            Toast.makeText(context, "Matched with ${admirer.name}! 🎉", Toast.LENGTH_SHORT).show()
                                            onChatClick(admirer)
                                        }
                                    },
                                    onInfoClick = { inspectingUser = admirer }
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- FULL PROFILE BOTTOM SHEET ---
        if (inspectingUser != null) {
            ModalBottomSheet(
                onDismissRequest = { inspectingUser = null },
                containerColor = PureWhite,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ) {
                MatchDetailBottomSheetContent(
                    user = inspectingUser!!,
                    onStartChat = {
                        val target = inspectingUser!!
                        inspectingUser = null
                        onChatClick(target)
                    }
                )
            }
        }

        // --- UNMATCH CONFIRMATION DIALOG ---
        if (unmatchTarget != null) {
            AlertDialog(
                onDismissRequest = { unmatchTarget = null },
                title = { Text("Unmatch with ${unmatchTarget!!.user.name}?") },
                text = { Text("You won't be able to message each other anymore and this chat will disappear.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val target = unmatchTarget!!
                            unmatchTarget = null
                            scope.launch {
                                try {
                                    firestore.collection("nyc_matches").document(target.matchId).delete().await()
                                    mutualMatches.remove(target)
                                    Toast.makeText(context, "Unmatched successfully", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    ) {
                        Text("Unmatch", color = Color.Red, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { unmatchTarget = null }) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            )
        }
    }
}

// ==========================================
// 1️⃣ MUTUAL MATCH CARD (GRID ITEM)
// ==========================================
@Composable
fun MatchGridCard(
    matchItem: MatchCardItem,
    onChatClick: () -> Unit,
    onInfoClick: () -> Unit,
    onOptionsClick: () -> Unit
) {
    val user = matchItem.user

    Card(
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .shadow(8.dp, RoundedCornerShape(22.dp), spotColor = Color.Black.copy(alpha = 0.08f))
            .clickable { onInfoClick() }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Profile Photo
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
                    Box(Modifier.fillMaxSize().background(Color(0xFFFFECEE)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = NYC_Red, modifier = Modifier.size(48.dp))
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
                            startY = 240f
                        )
                    )
            )

            // Top Quick Options (Three dots)
            IconButton(
                onClick = onOptionsClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(32.dp)
                    .background(Color.Black.copy(alpha = 0.4f), CircleShape)
            ) {
                Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = Color.White, modifier = Modifier.size(18.dp))
            }

            // Bottom Content
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            ) {
                Text(
                    text = "${user.name}, ${user.age}",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "📍 ${user.neighborhood.ifBlank { user.borough }}",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Chat Action Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(NYC_Red)
                        .clickable { onChatClick() },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.ChatBubble,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Message",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ==========================================
// 2️⃣ LIKES YOU (ADMIRER) CARD
// ==========================================
@Composable
fun AdmirerGridCard(
    user: NYCUser,
    onMatchBack: () -> Unit,
    onInfoClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .shadow(8.dp, RoundedCornerShape(22.dp), spotColor = Color.Black.copy(alpha = 0.08f))
            .clickable { onInfoClick() }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(user.photoURL)
                    .crossfade(true)
                    .build(),
                contentDescription = user.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
                            startY = 240f
                        )
                    )
            )

            // VIP Liked Badge
            Surface(
                shape = RoundedCornerShape(50.dp),
                color = Color(0xFFFF9F1A),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Filled.Favorite, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Liked You", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Bottom Content
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            ) {
                Text(
                    text = "${user.name}, ${user.age}",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Text(
                    text = "📍 ${user.neighborhood}",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Instant Match Back Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF2ECC71))
                        .clickable { onMatchBack() },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Filled.Favorite, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Match Back", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ==========================================
// 3️⃣ DETAILED PROFILE BOTTOM SHEET
// ==========================================
@Composable
fun MatchDetailBottomSheetContent(
    user: NYCUser,
    onStartChat: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
    ) {
        // Full Photo
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp)
                .clip(RoundedCornerShape(24.dp))
        ) {
            SubcomposeAsyncImage(
                model = user.photoURL,
                contentDescription = user.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("${user.name}, ${user.age}", fontSize = 26.sp, fontWeight = FontWeight.Black)
                Text("📍 ${user.neighborhood}, ${user.borough}", fontSize = 14.sp, color = TextSecondary)
            }
            Surface(
                shape = RoundedCornerShape(50.dp),
                color = NYC_RedSurface
            ) {
                Text(
                    text = "⚡ ${user.nycEnergy.ifBlank { "Chill Local" }}",
                    color = NYC_Red,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Hot Take
        if (user.unpopularOpinion.isNotBlank()) {
            Text("NYC Hot Take 🔥", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = NYC_Red)
            Spacer(modifier = Modifier.height(6.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F4F6)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "\"${user.unpopularOpinion}\"",
                    fontSize = 14.sp,
                    fontStyle = FontStyle.Italic,
                    modifier = Modifier.padding(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
        }

        // Interests Tags
        if (user.personalityTags.isNotEmpty()) {
            Text("Interests & Vibe", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(user.personalityTags) { tag ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50.dp))
                            .background(Color(0xFFEEEEF2))
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(tag, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Direct Message Button
        Button(
            onClick = onStartChat,
            colors = ButtonDefaults.buttonColors(containerColor = NYC_Red),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Icon(Icons.Filled.ChatBubble, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Send Message to ${user.name}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

// ==========================================
// 4️⃣ EMPTY STATE
// ==========================================
@Composable
fun EmptyMatchesState(
    title: String,
    description: String,
    buttonText: String,
    onAction: () -> Unit
) {
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
                    .size(90.dp)
                    .background(NYC_RedSurface, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Favorite, contentDescription = null, tint = NYC_Red, modifier = Modifier.size(48.dp))
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                fontSize = 14.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onAction,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NYC_Red),
                modifier = Modifier.height(48.dp)
            ) {
                Text(buttonText, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}