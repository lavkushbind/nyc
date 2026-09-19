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

    // 0 = Matches, 1 = Likes You
    var selectedTopTab by remember { mutableStateOf(0) }
    var selectedBoroughFilter by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    val mutualMatches = remember { mutableStateListOf<MatchCardItem>() }
    val likesReceived = remember { mutableStateListOf<NYCUser>() }
    var isLoading by remember { mutableStateOf(true) }

    var inspectingUser by remember { mutableStateOf<NYCUser?>(null) }
    var unmatchTarget by remember { mutableStateOf<MatchCardItem?>(null) }

    val boroughs = listOf("All", "Manhattan", "Brooklyn", "Queens", "Bronx", "Staten Island")

    // Real-time Firestore Listener
    DisposableEffect(currentUserId) {
        if (currentUserId.isBlank()) {
            isLoading = false
            return@DisposableEffect onDispose {}
        }

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

        val likesListener = firestore.collection("nyc_swipes")
            .whereEqualTo("targetId", currentUserId)
            .whereEqualTo("isLike", true)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    for (doc in snapshot.documents) {
                        val swiperId = doc.getString("swiperId") ?: continue
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

    // 🚀 FIXED: Nested Scaffold removed to eliminate massive 300dp top gap
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FB))
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // --- TOP CLEAN HEADER & SEGMENT CONTROL ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Connections",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E2022),
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            text = "People you connected with",
                            fontSize = 12.sp,
                            color = Color(0xFF9E9EA7)
                        )
                    }

                    IconButton(
                        onClick = { isSearchActive = !isSearchActive },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF3F4F6))
                    ) {
                        Icon(
                            imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color(0xFF4A4E69),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Smooth Collapsible Search Bar
                AnimatedVisibility(visible = isSearchActive) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .height(42.dp)
                            .clip(RoundedCornerShape(21.dp))
                            .background(Color(0xFFF4F5F7))
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFFA0A3BD), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        androidx.compose.foundation.text.BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) {
                                    Text("Search matches or area...", fontSize = 13.sp, color = Color(0xFFA0A3BD))
                                }
                                innerTextField()
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // --- SLEEK TAB SWITCHER ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .clip(RoundedCornerShape(21.dp))
                        .background(Color(0xFFF1F3F5))
                        .padding(3.dp)
                ) {
                    // Matches Tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (selectedTopTab == 0) Color.White else Color.Transparent)
                            .clickable { selectedTopTab = 0 },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Matches",
                                fontWeight = if (selectedTopTab == 0) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTopTab == 0) NYC_Red else Color(0xFF6C757D),
                                fontSize = 13.sp
                            )
                            if (mutualMatches.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(if (selectedTopTab == 0) NYC_Red else Color(0xFFADB5BD))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${mutualMatches.size}",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Likes You Tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (selectedTopTab == 1) Color.White else Color.Transparent)
                            .clickable { selectedTopTab = 1 },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Likes You",
                                fontWeight = if (selectedTopTab == 1) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTopTab == 1) Color(0xFFFF4757) else Color(0xFF6C757D),
                                fontSize = 13.sp
                            )
                            if (likesReceived.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(if (selectedTopTab == 1) Color(0xFFFF4757) else Color(0xFFADB5BD))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${likesReceived.size}",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- BOROUGH FILTER CHIPS ---
            LazyRow(
                modifier = Modifier.padding(vertical = 8.dp),
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(boroughs) { borough ->
                    val isSelected = selectedBoroughFilter == borough
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50.dp))
                            .background(if (isSelected) NYC_Red else Color.White)
                            .border(1.dp, if (isSelected) NYC_Red else Color(0xFFE5E7EB), RoundedCornerShape(50.dp))
                            .clickable { selectedBoroughFilter = borough }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = borough,
                            fontSize = 11.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else Color(0xFF4A4E69)
                        )
                    }
                }
            }

            // --- CONTENT GRID ---
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NYC_Red, strokeWidth = 2.5.dp, modifier = Modifier.size(32.dp))
                }
            } else {
                if (selectedTopTab == 0) {
                    // MUTUAL MATCHES
                    if (displayedMatches.isEmpty()) {
                        EmptyMatchesState(
                            title = "No Matches Yet",
                            description = "Keep swiping on Explore! When someone likes you back, they will appear here.",
                            buttonText = "Start Swiping 🔥",
                            onAction = onExploreClick
                        )
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(displayedMatches, key = { it.matchId }) { matchItem ->
                                MatchGridCard(
                                    matchItem = matchItem,
                                    onChatClick = { onChatClick(matchItem.user) },
                                    onCardClick = { inspectingUser = matchItem.user }
                                )
                            }
                        }
                    }
                } else {
                    // LIKES YOU
                    if (displayedLikes.isEmpty()) {
                        EmptyMatchesState(
                            title = "No Admirers Right Now",
                            description = "Your profile is active. As soon as someone likes you, their profile will appear here!",
                            buttonText = "Explore Profiles ✨",
                            onAction = onExploreClick
                        )
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(displayedLikes, key = { it.uid }) { admirer ->
                                AdmirerGridCard(
                                    user = admirer,
                                    onMatchBack = {
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
                                    onCardClick = { inspectingUser = admirer }
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
                title = { Text("Unmatch with ${unmatchTarget!!.user.name}?", fontWeight = FontWeight.Bold) },
                text = { Text("You won't be able to message each other anymore.", color = Color(0xFF6C757D)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val target = unmatchTarget!!
                            unmatchTarget = null
                            scope.launch {
                                try {
                                    firestore.collection("nyc_matches").document(target.matchId).delete().await()
                                    mutualMatches.remove(target)
                                    Toast.makeText(context, "Unmatched", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    ) {
                        Text("Unmatch", color = NYC_Red, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { unmatchTarget = null }) {
                        Text("Cancel", color = Color(0xFF6C757D))
                    }
                }
            )
        }
    }
}

// ==========================================
// 1️⃣ MUTUAL MATCH CARD
// ==========================================
@Composable
fun MatchGridCard(
    matchItem: MatchCardItem,
    onChatClick: () -> Unit,
    onCardClick: () -> Unit
) {
    val user = matchItem.user

    Card(
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(255.dp)
            .shadow(6.dp, RoundedCornerShape(22.dp), spotColor = Color(0x15000000))
            .clickable { onCardClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
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
                    Box(Modifier.fillMaxSize().background(Color(0xFFF1F2F6)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(40.dp))
                    }
                }
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.85f)
                            ),
                            startY = 200f
                        )
                    )
            )

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
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(NYC_Red, Color(0xFFFF5277))))
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
// 2️⃣ LIKES YOU CARD
// ==========================================
@Composable
fun AdmirerGridCard(
    user: NYCUser,
    onMatchBack: () -> Unit,
    onCardClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(255.dp)
            .shadow(6.dp, RoundedCornerShape(22.dp), spotColor = Color(0x15000000))
            .clickable { onCardClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24))
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
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.85f)
                            ),
                            startY = 200f
                        )
                    )
            )

            Surface(
                shape = RoundedCornerShape(50.dp),
                color = Color(0xFFFF4757).copy(alpha = 0.85f),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
            ) {
                Text(
                    text = "Liked You ❤️",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }

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
                    Text(
                        text = "📍 ${user.neighborhood.ifBlank { user.borough }}",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2ED573))
                        .clickable { onMatchBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = "Match",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
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
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp)
    ) {
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("${user.name}, ${user.age}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E2022))
                Text("📍 ${user.neighborhood}, ${user.borough}", fontSize = 13.sp, color = Color(0xFF6C757D))
            }
            if (user.nycEnergy.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(50.dp),
                    color = NYC_Red.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = "⚡ ${user.nycEnergy}",
                        color = NYC_Red,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (user.bio.isNotBlank()) {
            Text("About", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E2022))
            Spacer(modifier = Modifier.height(4.dp))
            Text(user.bio, fontSize = 13.5.sp, color = Color(0xFF4A4E69), lineHeight = 19.sp)
            Spacer(modifier = Modifier.height(14.dp))
        }

        if (user.unpopularOpinion.isNotBlank()) {
            Text("NYC Hot Take 🔥", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NYC_Red)
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFF8F9FA),
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
                        Text(tag, fontSize = 11.5.sp, color = Color(0xFF495057), fontWeight = FontWeight.Medium)
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

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
            Text("Send Message", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
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
                    .size(70.dp)
                    .background(NYC_Red.copy(alpha = 0.08f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.FavoriteBorder, contentDescription = null, tint = NYC_Red, modifier = Modifier.size(34.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E2022))
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = description,
                fontSize = 13.sp,
                color = Color(0xFF9E9EA7),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onAction,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NYC_Red),
                modifier = Modifier.height(44.dp)
            ) {
                Text(buttonText, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}