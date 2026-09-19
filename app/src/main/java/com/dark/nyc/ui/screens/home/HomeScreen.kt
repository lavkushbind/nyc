package com.dark.nyc.ui.screens.home

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import com.dark.nyc.ui.screens.matches.MatchesScreen
import com.dark.nyc.ui.screens.profile.ProfileScreen
import com.dark.nyc.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.abs

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

    // 0: Explore, 1: Matches, 2: Chat, 3: Discover, 4: Profile
    var selectedTab by remember { mutableStateOf(0) }
    var activeChatUser by remember { mutableStateOf<NYCUser?>(null) }

    val navItems = listOf(
        BottomNavItem("Explore", Icons.Filled.LocalFireDepartment, Icons.Outlined.LocalFireDepartment),
        BottomNavItem("Matches", Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder),
        BottomNavItem("Chat", Icons.Filled.ChatBubble, Icons.Outlined.ChatBubbleOutline),
        BottomNavItem("Discover", Icons.Filled.Explore, Icons.Outlined.Explore),
        BottomNavItem("Profile", Icons.Filled.Person, Icons.Outlined.Person)
    )

    // Android Hardware Back Handling
    BackHandler {
        when {
            activeChatUser != null -> activeChatUser = null
            selectedTab != 0 -> selectedTab = 0
            else -> (context as? Activity)?.finish()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFFF8F9FB),
        topBar = {
            if (activeChatUser == null && selectedTab == 0) {
                Surface(
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Brand Title
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(50.dp),
                                color = NYC_Red.copy(alpha = 0.1f)
                            ) {
                                Text(
                                    text = "Sway",
                                    color = NYC_Red,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // Top Right User Avatar
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, Color(0xFFEEEEF2), CircleShape)
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
                                error = {
                                    Box(
                                        modifier = Modifier.fillMaxSize().background(Color(0xFFFFECEE)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Filled.Person, contentDescription = null, tint = NYC_Red, modifier = Modifier.size(20.dp))
                                    }
                                }
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            if (activeChatUser == null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .shadow(16.dp, RoundedCornerShape(32.dp), spotColor = Color.Black.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(32.dp),
                    color = Color.White
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
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label,
                                    tint = if (isSelected) NYC_Red else Color(0xFFA0A3BD),
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = item.label,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) NYC_Red else Color(0xFFA0A3BD)
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
                ChatScreen(
                    targetUser = activeChatUser!!,
                    onBack = { activeChatUser = null }
                )
            } else {
                when (selectedTab) {
                    0 -> FeedTab(
                        viewModel = viewModel,
                        uiState = uiState,
                        onOpenChat = { user -> activeChatUser = user }
                    )
                    1 -> MatchesScreen(
                        onChatClick = { user -> activeChatUser = user },
                        onExploreClick = { selectedTab = 0 }
                    )
                    2 -> ChatListScreen(
                        onChatClick = { user -> activeChatUser = user },
                        newMatches = uiState.potentialMatches
                    )
                    3 -> DiscoverScreen(
                        onChatClick = { user -> activeChatUser = user }
                    )
                    4 -> ProfileScreen(navController = navController)
                }
            }
        }
    }
}

// ==================================================================
// 1️⃣ FEED TAB (WITH INFINITE RECYCLE LOOP AT THE END)
// ==================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedTab(
    viewModel: HomeViewModel,
    uiState: HomeViewModel.HomeUiState,
    onOpenChat: (NYCUser) -> Unit
) {
    var inspectingUser by remember { mutableStateOf<NYCUser?>(null) }

    // 🔥 MEMORY STORE FOR INFINITE LOOPING
    val allRecycledProfiles = remember { mutableStateListOf<NYCUser>() }
    val localDisplayProfiles = remember { mutableStateListOf<NYCUser>() }

    // Sync from ViewModel and preserve in loop buffer
    LaunchedEffect(uiState.potentialMatches) {
        if (uiState.potentialMatches.isNotEmpty()) {
            uiState.potentialMatches.forEach { user ->
                if (allRecycledProfiles.none { it.uid == user.uid }) {
                    allRecycledProfiles.add(user)
                }
                if (localDisplayProfiles.none { it.uid == user.uid }) {
                    localDisplayProfiles.add(user)
                }
            }
        }
    }

    // Auto-restart loop when all profiles are swiped
    fun handleSwipeComplete(swipedUser: NYCUser, isLike: Boolean) {
        if (isLike) {
            viewModel.onSwipeRight(swipedUser)
        } else {
            viewModel.onSwipeLeft(swipedUser)
        }

        localDisplayProfiles.remove(swipedUser)

        // 🔄 JAB SARE FINISH HO JAYE TO DUBARA RE-POPULATE KR DO
        if (localDisplayProfiles.isEmpty() && allRecycledProfiles.isNotEmpty()) {
            localDisplayProfiles.addAll(allRecycledProfiles)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF8F9FB))) {
        if (inspectingUser != null) {
            ModalBottomSheet(
                onDismissRequest = { inspectingUser = null },
                containerColor = Color.White,
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
                        val user = inspectingUser!!
                        inspectingUser = null
                        handleSwipeComplete(user, isLike = true)
                    },
                    onPass = {
                        val user = inspectingUser!!
                        inspectingUser = null
                        handleSwipeComplete(user, isLike = false)
                    }
                )
            }
        }

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

        if (uiState.isLoading && localDisplayProfiles.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NYC_Red, strokeWidth = 2.5.dp, modifier = Modifier.size(36.dp))
            }
        } else if (localDisplayProfiles.isNotEmpty()) {
            val topUser = localDisplayProfiles.first()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Next Card In Background
                    if (localDisplayProfiles.size > 1) {
                        val nextUser = localDisplayProfiles[1]
                        Card(
                            shape = RoundedCornerShape(26.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 10.dp, start = 8.dp, end = 8.dp)
                                .graphicsLayer {
                                    scaleX = 0.94f
                                    scaleY = 0.94f
                                }
                        ) {
                            SubcomposeAsyncImage(
                                model = nextUser.photoURL,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    // Top Active Swipable Card
                    key(topUser.uid) {
                        SmoothSwipeCard(
                            user = topUser,
                            onSwipeLeft = { handleSwipeComplete(topUser, isLike = false) },
                            onSwipeRight = { handleSwipeComplete(topUser, isLike = true) },
                            onCardClick = { inspectingUser = topUser }
                        )
                    }
                }

                // Controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 40.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Pass (X)
                    FloatingActionButton(
                        onClick = { handleSwipeComplete(topUser, isLike = false) },
                        containerColor = Color.White,
                        contentColor = Color(0xFFFF4757),
                        shape = CircleShape,
                        elevation = FloatingActionButtonDefaults.elevation(4.dp),
                        modifier = Modifier.size(52.dp)
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Pass", modifier = Modifier.size(26.dp))
                    }

                    // Info
                    FilledIconButton(
                        onClick = { inspectingUser = topUser },
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.White),
                        modifier = Modifier
                            .size(44.dp)
                            .shadow(4.dp, CircleShape, spotColor = Color(0x15000000))
                    ) {
                        Icon(Icons.Filled.Info, contentDescription = "Info", tint = Color(0xFF3867D6), modifier = Modifier.size(20.dp))
                    }

                    // Direct Chat
                    FilledIconButton(
                        onClick = { onOpenChat(topUser) },
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.White),
                        modifier = Modifier
                            .size(44.dp)
                            .shadow(4.dp, CircleShape, spotColor = Color(0x15000000))
                    ) {
                        Icon(Icons.Filled.ChatBubble, contentDescription = "Chat", tint = Color(0xFF2ED573), modifier = Modifier.size(18.dp))
                    }

                    // Like (❤️)
                    FloatingActionButton(
                        onClick = { handleSwipeComplete(topUser, isLike = true) },
                        containerColor = NYC_Red,
                        contentColor = Color.White,
                        shape = CircleShape,
                        elevation = FloatingActionButtonDefaults.elevation(6.dp),
                        modifier = Modifier.size(52.dp)
                    ) {
                        Icon(Icons.Filled.Favorite, contentDescription = "Like", modifier = Modifier.size(26.dp))
                    }
                }
            }
        } else {
            FeedEmptyState(onRefresh = {
                viewModel.refreshMatches()
                if (allRecycledProfiles.isNotEmpty()) {
                    localDisplayProfiles.addAll(allRecycledProfiles)
                }
            })
        }
    }
}

// ==================================================================
// 2️⃣ HIGH PERFORMANCE GESTURE SWIPE CARD
// ==================================================================
@Composable
fun SmoothSwipeCard(
    user: NYCUser,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    onCardClick: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val density = LocalDensity.current
    val screenWidthPx = with(density) { screenWidth.toPx() }

    val offsetX = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    val rotation = (offsetX.value / 60f).coerceIn(-18f, 18f)

    Card(
        shape = RoundedCornerShape(26.dp),
        modifier = Modifier
            .fillMaxSize()
            .offset(x = with(density) { offsetX.value.toDp() })
            .rotate(rotation)
            .shadow(12.dp, RoundedCornerShape(26.dp), spotColor = Color(0x15000000))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        coroutineScope.launch {
                            offsetX.snapTo(offsetX.value + dragAmount.x)
                        }
                    },
                    onDragEnd = {
                        val threshold = screenWidthPx * 0.35f
                        coroutineScope.launch {
                            when {
                                offsetX.value > threshold -> {
                                    offsetX.animateTo(screenWidthPx * 1.4f, tween(240))
                                    onSwipeRight()
                                }
                                offsetX.value < -threshold -> {
                                    offsetX.animateTo(-screenWidthPx * 1.4f, tween(240))
                                    onSwipeLeft()
                                }
                                else -> {
                                    offsetX.animateTo(
                                        0f,
                                        spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMediumLow
                                        )
                                    )
                                }
                            }
                        }
                    }
                )
            }
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
                    Box(Modifier.fillMaxSize().background(Color(0xFFE2E8F0)), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = NYC_Red, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
                    }
                },
                error = {
                    Box(Modifier.fillMaxSize().background(Color(0xFFF1F2F6)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Person, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(54.dp))
                    }
                }
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.2f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.85f)
                            ),
                            startY = 0f,
                            endY = Float.POSITIVE_INFINITY
                        )
                    )
            )

            val dragAlpha = (abs(offsetX.value) / 180f).coerceIn(0f, 1f)
            if (offsetX.value > 20f) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Transparent,
                    border = BorderStroke(3.dp, Color(0xFF2ED573).copy(alpha = dragAlpha)),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(24.dp)
                        .rotate(-15f)
                ) {
                    Text(
                        text = "LIKE",
                        color = Color(0xFF2ED573).copy(alpha = dragAlpha),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                    )
                }
            } else if (offsetX.value < -20f) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Transparent,
                    border = BorderStroke(3.dp, Color(0xFFFF4757).copy(alpha = dragAlpha)),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(24.dp)
                        .rotate(15f)
                ) {
                    Text(
                        text = "NOPE",
                        color = Color(0xFFFF4757).copy(alpha = dragAlpha),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(50.dp),
                color = Color.Black.copy(alpha = 0.45f),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(14.dp)
            ) {
                Text(
                    text = user.nycEnergy.ifBlank { "NYC Local" },
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "${user.name}, ${user.age}",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${user.neighborhood.ifBlank { user.borough }}, New York",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        fontWeight = FontWeight.Medium
                    )
                }

                if (user.bio.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = user.bio,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ==================================================================
// 3️⃣ DETAILED PROFILE BOTTOM SHEET
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
            .padding(horizontal = 22.dp)
            .padding(bottom = 32.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
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
                contentScale = ContentScale.Crop
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
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "📍 ${user.neighborhood}, ${user.borough}",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (user.nycEnergy.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(50.dp),
                color = NYC_Red.copy(alpha = 0.1f)
            ) {
                Text(
                    text = "⚡ Energy: ${user.nycEnergy}",
                    fontWeight = FontWeight.Bold,
                    color = NYC_Red,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        if (user.bio.isNotBlank()) {
            Text("About Me", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E2022))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = user.bio, fontSize = 13.5.sp, color = Color(0xFF4A4E69), lineHeight = 19.sp)
            Spacer(modifier = Modifier.height(14.dp))
        }

        if (user.unpopularOpinion.isNotBlank()) {
            Text("NYC Hot Take 🔥", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NYC_Red)
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
                    modifier = Modifier.padding(14.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        if (user.personalityTags.isNotEmpty()) {
            Text("Interests", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E2022))
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(user.personalityTags) { tag ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50.dp))
                            .background(Color(0xFFF1F3F5))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(text = tag, fontSize = 12.sp, color = Color(0xFF495057), fontWeight = FontWeight.Medium)
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onPass,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F2F6)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.weight(1f).height(48.dp)
            ) {
                Text("Pass", color = Color(0xFF2D3142), fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onChat,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2ED573)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.weight(1f).height(48.dp)
            ) {
                Text("💬 Chat", color = Color.White, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onLike,
                colors = ButtonDefaults.buttonColors(containerColor = NYC_Red),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.weight(1f).height(48.dp)
            ) {
                Text("❤️ Like", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ==================================================================
// 4️⃣ MATCH POPUP DIALOG
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
                .fillMaxWidth(0.86f)
                .clickable(enabled = false) {},
            shape = RoundedCornerShape(30.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("It's a Match! 🎉", fontSize = 24.sp, fontWeight = FontWeight.Black, color = NYC_Red)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "You and ${matchedUser.name} liked each other in NYC!",
                    fontSize = 13.sp,
                    color = Color(0xFF6C757D),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .border(3.dp, Color.White, CircleShape)
                    ) {
                        SubcomposeAsyncImage(
                            model = currentUser?.photoURL,
                            contentDescription = "You",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Spacer(modifier = Modifier.width(-16.dp))
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .border(3.dp, NYC_Red, CircleShape)
                    ) {
                        SubcomposeAsyncImage(
                            model = matchedUser.photoURL,
                            contentDescription = matchedUser.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onChat,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NYC_Red)
                ) {
                    Text("Say Hello 👋", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(onClick = onDismiss) {
                    Text("Keep Swiping", color = Color(0xFF6C757D), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ==================================================================
// 5️⃣ EMPTY RADAR STATE
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
                    .size(80.dp)
                    .background(NYC_Red.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Explore, contentDescription = null, tint = NYC_Red, modifier = Modifier.size(40.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("You're All Caught Up!", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E2022))
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "No more new profiles around your area right now. Check back soon for new NYC locals!",
                fontSize = 13.sp,
                color = Color(0xFF6C757D),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onRefresh,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NYC_Red),
                modifier = Modifier.height(46.dp)
            ) {
                Text("Refresh Radar 🔄", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}