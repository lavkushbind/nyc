package com.dark.nyc.ui.screens.home

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.dark.nyc.data.models.NYCUser
import com.dark.nyc.ui.screens.chat.ChatListScreen
import com.dark.nyc.ui.screens.chat.ChatScreen
import com.dark.nyc.ui.screens.discover.DiscoverScreen
import com.dark.nyc.ui.screens.home.components.SwipeCardStack
import com.dark.nyc.ui.screens.matches.MatchesScreen
import com.dark.nyc.ui.screens.profile.ProfileScreen
import com.dark.nyc.ui.theme.*

data class BottomNavItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // Active Navigation Tab (0: Explore, 1: Matches, 2: Chat, 3: Discover, 4: Profile)
    var selectedTab by remember { mutableStateOf(0) }

    // Active Chat Screen Target (if null, shows tab screens)
    var activeChatUser by remember { mutableStateOf<NYCUser?>(null) }

    val navItems = listOf(
        BottomNavItem("Explore", Icons.Filled.LocalFireDepartment, Icons.Outlined.LocalFireDepartment),
        BottomNavItem("Matches", Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder),
        BottomNavItem("Chat", Icons.Filled.ChatBubble, Icons.Outlined.ChatBubbleOutline),
        BottomNavItem("Discover", Icons.Filled.Explore, Icons.Outlined.Explore),
        BottomNavItem("Profile", Icons.Filled.Person, Icons.Outlined.Person)
    )

    // Smart Android Back Handling
    BackHandler {
        when {
            activeChatUser != null -> activeChatUser = null
            selectedTab != 0 -> selectedTab = 0
            else -> {
                // Exit app or let system handle
                (context as? android.app.Activity)?.finish()
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFFF7F8FA),
        topBar = {
            if (activeChatUser == null) {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "nyc.date",
                                color = NYC_Red,
                                fontWeight = FontWeight.Black,
                                fontSize = 24.sp,
                                letterSpacing = 1.2.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(NYC_Red, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            // VIP Badge
                            Surface(
                                shape = RoundedCornerShape(50.dp),
                                color = Color(0xFFFFECEE),
                                border = BorderStroke(1.dp, NYC_Red.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "VIP ✨",
                                    color = NYC_Red,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    },
                    actions = {
                        // Current User Profile Avatar on Top Right
                        Box(
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .size(42.dp)
                                .clip(CircleShape)
                                .border(2.dp, if (selectedTab == 4) NYC_Red else Color(0xFFE5E7EB), CircleShape)
                                .clickable { selectedTab = 4 }
                        ) {
                            SubcomposeAsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(uiState.currentUser?.photoURL)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "My Profile",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                loading = { CircularProgressIndicator(color = NYC_Red, strokeWidth = 2.dp) },
                                error = {
                                    Box(
                                        modifier = Modifier.fillMaxSize().background(Color(0xFFFFECEE)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Filled.Person, contentDescription = null, tint = NYC_Red)
                                    }
                                }
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = PureWhite)
                )
            }
        },
        bottomBar = {
            if (activeChatUser == null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .shadow(16.dp, RoundedCornerShape(32.dp), spotColor = Color.Black.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(32.dp),
                    color = PureWhite
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        navItems.forEachIndexed { index, item ->
                            val isSelected = selectedTab == index
                            val interactionSource = remember { MutableInteractionSource() }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable(interactionSource = interactionSource, indication = null) {
                                        selectedTab = index
                                    }
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label,
                                    tint = if (isSelected) NYC_Red else Color(0xFF9E9EA7),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = item.label,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) NYC_Red else Color(0xFF9E9EA7)
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (activeChatUser != null) {
                // 💬 REAL-TIME CHAT SCREEN (Full Screen Overlay)
                ChatScreen(
                    targetUser = activeChatUser!!,
                    onBack = { activeChatUser = null }
                )
            } else {
                when (selectedTab) {
                    // TAB 0: EXPLORE (FEED)
                    0 -> FeedTab(
                        viewModel = viewModel,
                        uiState = uiState,
                        onOpenChat = { user -> activeChatUser = user }
                    )

                    // TAB 1: MATCHES (Mutual Connections & Likes You)
                    1 -> MatchesScreen(
                        onChatClick = { user -> activeChatUser = user },
                        onExploreClick = { selectedTab = 0 }
                    )

                    // TAB 2: CHAT LIST (All Conversations & Unread Messages)
                    2 -> ChatListScreen(
                        onChatClick = { user -> activeChatUser = user },
                        newMatches = uiState.potentialMatches
                    )

                    // TAB 3: DISCOVER (NYC Date Venues, Speakeasies & Events)
                    3 -> DiscoverScreen(
                        onChatClick = { user -> activeChatUser = user }
                    )
                    // TAB 4: PROFILE (Full Profile View & Live Edit)
                    4 -> ProfileScreen(navController = navController)
                }
            }
        }
    }
}

// ==================================================================
// 1️⃣ FEED TAB (SOLID CARDS WITH ZERO BACKGROUND BLEED)
// ==================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedTab(
    viewModel: HomeViewModel,
    uiState: HomeViewModel.HomeUiState,
    onOpenChat: (NYCUser) -> Unit
) {
    var inspectingUser by remember { mutableStateOf<NYCUser?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F8FA))
    ) {
        // Detailed Profile BottomSheet
        if (inspectingUser != null) {
            ModalBottomSheet(
                onDismissRequest = { inspectingUser = null },
                containerColor = PureWhite,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ) {
                DetailedProfileSheetContent(
                    user = inspectingUser!!,
                    onChat = {
                        val target = inspectingUser!!
                        inspectingUser = null
                        onOpenChat(target)
                    },
                    onLike = {
                        viewModel.onSwipeRight(inspectingUser!!)
                        inspectingUser = null
                    },
                    onPass = {
                        viewModel.onSwipeLeft(inspectingUser!!)
                        inspectingUser = null
                    }
                )
            }
        }

        // Mutual Match Popup Dialog
        if (uiState.matchFound && uiState.matchedUser != null) {
            MatchPopupDialog(
                currentUser = uiState.currentUser,
                matchedUser = uiState.matchedUser!!,
                onDismiss = { viewModel.dismissMatch() },
                onChat = {
                    val target = uiState.matchedUser!!
                    viewModel.dismissMatch()
                    onOpenChat(target)
                }
            )
        }

        // Content Area
        if (uiState.isLoading && uiState.potentialMatches.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NYC_Red, strokeWidth = 3.dp)
            }
        } else if (uiState.potentialMatches.isNotEmpty()) {
            val topUser = uiState.potentialMatches.firstOrNull()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Key ensures Compose recreates the card without retaining the previous user's image cache!
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    key(topUser?.uid) {
                        SwipeCardStack(
                            users = uiState.potentialMatches,
                            onSwipeLeft = { viewModel.onSwipeLeft(it) },
                            onSwipeRight = { viewModel.onSwipeRight(it) },
                            currentUserId = uiState.currentUser?.uid ?: ""
                        )
                    }
                }

                // Bottom Floating Action Controls
                if (topUser != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Pass (X)
                        FloatingActionButton(
                            onClick = { viewModel.onSwipeLeft(topUser) },
                            containerColor = PureWhite,
                            contentColor = Color(0xFFFF5252),
                            shape = CircleShape,
                            modifier = Modifier
                                .size(54.dp)
                                .shadow(8.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.1f))
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Pass", modifier = Modifier.size(28.dp))
                        }

                        // Inspect Info (ℹ️)
                        FilledIconButton(
                            onClick = { inspectingUser = topUser },
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = PureWhite),
                            modifier = Modifier
                                .size(46.dp)
                                .shadow(6.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.08f))
                        ) {
                            Icon(Icons.Filled.Info, contentDescription = "Info", tint = Color(0xFF3867D6), modifier = Modifier.size(22.dp))
                        }

                        // Instant Chat (VIP Access)
                        FilledIconButton(
                            onClick = { onOpenChat(topUser) },
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = PureWhite),
                            modifier = Modifier
                                .size(46.dp)
                                .shadow(6.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.08f))
                        ) {
                            Icon(Icons.Filled.ChatBubble, contentDescription = "Instant Chat", tint = Color(0xFF20BF6B), modifier = Modifier.size(20.dp))
                        }

                        // Like (❤️)
                        FloatingActionButton(
                            onClick = { viewModel.onSwipeRight(topUser) },
                            containerColor = NYC_Red,
                            contentColor = PureWhite,
                            shape = CircleShape,
                            modifier = Modifier
                                .size(54.dp)
                                .shadow(12.dp, CircleShape, spotColor = NYC_Red.copy(alpha = 0.4f))
                        ) {
                            Icon(Icons.Filled.Favorite, contentDescription = "Like", modifier = Modifier.size(28.dp))
                        }
                    }
                }
            }
        } else {
            // Empty State
            FeedEmptyState(onRefresh = { viewModel.refreshMatches() })
        }
    }
}

// ==================================================================
// 2️⃣ DETAILED PROFILE BOTTOM SHEET
// ==================================================================
@Composable
fun DetailedProfileSheetContent(
    user: NYCUser,
    onChat: () -> Unit,
    onLike: () -> Unit,
    onPass: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
    ) {
        // Solid Header Image Container (No transparency bleed)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF1E1E24))
        ) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(user.photoURL)
                    .crossfade(true)
                    .build(),
                contentDescription = user.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                loading = { CircularProgressIndicator(color = NYC_Red, modifier = Modifier.align(Alignment.Center)) }
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                            startY = 200f
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(18.dp)
            ) {
                Text(
                    text = "${user.name}, ${user.age}",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text(
                    text = "📍 ${user.neighborhood}, ${user.borough}",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.85f),
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // NYC Energy Badge
        if (user.nycEnergy.isNotBlank()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(50.dp))
                    .background(NYC_RedSurface)
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(text = "⚡ Energy: ", fontWeight = FontWeight.Bold, color = NYC_Red, fontSize = 13.sp)
                Text(text = user.nycEnergy, fontWeight = FontWeight.Medium, color = NYC_RedDark, fontSize = 13.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Unpopular Opinion / Hot Take
        if (user.unpopularOpinion.isNotBlank()) {
            Text("NYC Hot Take 🔥", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(6.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F7)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "\"${user.unpopularOpinion}\"",
                    fontSize = 14.sp,
                    fontStyle = FontStyle.Italic,
                    color = TextPrimary,
                    modifier = Modifier.padding(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Personality Tags
        if (user.personalityTags.isNotEmpty()) {
            Text("Interests & Vibe", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(user.personalityTags) { tag ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50.dp))
                            .background(Color(0xFFEEEEF2))
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(text = tag, fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Action Buttons inside Sheet
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onPass,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F2F6)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f).height(50.dp)
            ) {
                Text("Pass", color = TextPrimary, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onChat,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF20BF6B)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f).height(50.dp)
            ) {
                Text("💬", color = Color.White, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onLike,
                colors = ButtonDefaults.buttonColors(containerColor = NYC_Red),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f).height(50.dp)
            ) {
                Text("❤️", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ==================================================================
// 3️⃣ MATCH POPUP DIALOG
// ==================================================================
@Composable
fun MatchPopupDialog(
    currentUser: NYCUser?,
    matchedUser: NYCUser,
    onDismiss: () -> Unit,
    onChat: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .clickable(enabled = false) {}
                .shadow(32.dp, RoundedCornerShape(32.dp)),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = PureWhite)
        ) {
            Column(
                modifier = Modifier.padding(26.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("It's a Match! 🎉", fontSize = 28.sp, fontWeight = FontWeight.Black, color = NYC_Red)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "You and ${matchedUser.name} liked each other in NYC!",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Dual Overlapping Avatars
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(84.dp)
                            .clip(CircleShape)
                            .border(3.dp, PureWhite, CircleShape)
                            .background(Color(0xFFEEEEF2))
                    ) {
                        SubcomposeAsyncImage(
                            model = currentUser?.photoURL,
                            contentDescription = "You",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Spacer(modifier = Modifier.width(-18.dp))
                    Box(
                        modifier = Modifier
                            .size(84.dp)
                            .clip(CircleShape)
                            .border(3.dp, NYC_Red, CircleShape)
                            .background(Color(0xFFEEEEF2))
                    ) {
                        SubcomposeAsyncImage(
                            model = matchedUser.photoURL,
                            contentDescription = matchedUser.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                Button(
                    onClick = onChat,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NYC_Red)
                ) {
                    Text("Say Hello 👋", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                Spacer(modifier = Modifier.height(10.dp))

                TextButton(onClick = onDismiss) {
                    Text("Keep Swiping", color = TextSecondary, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ==================================================================
// 4️⃣ EMPTY RADAR STATE
// ==================================================================
@Composable
fun FeedEmptyState(onRefresh: () -> Unit) {
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
                Icon(Icons.Filled.Explore, contentDescription = null, tint = NYC_Red, modifier = Modifier.size(46.dp))
            }
            Spacer(modifier = Modifier.height(18.dp))
            Text("You're All Caught Up!", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "No more new profiles around your area right now. Check back soon for new NYC locals!",
                fontSize = 14.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onRefresh,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NYC_Red),
                modifier = Modifier.height(48.dp)
            ) {
                Text("Refresh Radar 🔄", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}