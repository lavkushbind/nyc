package com.dark.nyc.ui.screens.home.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.dark.nyc.R
import com.dark.nyc.data.models.NYCUser
import com.dark.nyc.ui.theme.*
import kotlin.math.abs

@Composable
fun SwipeCardStack(
    users: List<NYCUser>,
    onSwipeLeft: (NYCUser) -> Unit,
    onSwipeRight: (NYCUser) -> Unit,
    currentUserId: String
) {
    // Show top 3 cards max for performance
    val visibleUsers = users.take(3)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        visibleUsers.forEachIndexed { index, user ->
            // Offset for stack effect: top card is fully visible, others shifted down
            val offsetX = 0f
            val offsetY = (index * 12).dp
            val scale = 1f - (index * 0.03f)

            if (index == 0) {
                // Top card - interactive
                SwipeCard(
                    user = user,
                    onSwipeLeft = { onSwipeLeft(user) },
                    onSwipeRight = { onSwipeRight(user) },
                    modifier = Modifier
                        .fillMaxSize()
                        .shadow(16.dp, RoundedCornerShape(24.dp), clip = false)
                        .clip(RoundedCornerShape(24.dp))
                )
            } else {
                // Background cards
                Card(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset(x = offsetX.dp, y = offsetY)
                        .scale(scale),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 4.dp
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(SoftGray)
                    ) {
                        // Blurred thumbnail
                        if (user.photoURL.isNotEmpty()) {
                            Image(
                                painter = rememberAsyncImagePainter(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(user.photoURL)
                                        .crossfade(true)
                                        .build()
                                ),
                                contentDescription = user.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            // Dark Overlay
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.8f)
                                        ),
                                        startY = 300f
                                    ))
                            )
                        }
                        // User Name overlay
                        Text(
                            text = "${user.name}, ${user.age}",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SwipeCard(
    user: NYCUser,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    modifier: Modifier = Modifier
) {
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var rotation by remember { mutableStateOf(0f) }

    // Determine if card is swiped far enough
    val swipeThreshold = 150f
    val isSwipedRight = offsetX > swipeThreshold
    val isSwipedLeft = offsetX < -swipeThreshold
    val shouldFinish = abs(offsetX) > swipeThreshold

    // Like/Pass indicators
    val likeAlpha = (offsetX / swipeThreshold).coerceIn(0f, 1f)
    val passAlpha = (-offsetX / swipeThreshold).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        // Reset animation if needed
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y * 0.2f
                        rotation = offsetX * 0.05f
                    },
                    onDragEnd = {
                        if (shouldFinish) {
                            if (isSwipedRight) {
                                onSwipeRight()
                            } else {
                                onSwipeLeft()
                            }
                        } else {
                            // Animate back to center
                            offsetX = 0f
                            offsetY = 0f
                            rotation = 0f
                        }
                    }
                )
            }
            .offset(x = offsetX.dp, y = offsetY.dp)
            .rotate(rotation)
    ) {
        // Card Content
        Card(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 8.dp
            )
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Profile Image
                if (user.photoURL.isNotEmpty()) {
                    Image(
                        painter = painterResource(id = R.drawable.logo),

                        contentDescription = user.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(SoftGray)
                    ) {
                        Text(
                            text = "📸 No Photo",
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }

                // Dark Gradient Overlay (Bottom)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.85f)
                                ),
                                startY = 400f
                            )
                        )
                )

                // Like / Pass Text Overlay
                androidx.compose.animation.AnimatedVisibility(
                    visible = likeAlpha > 0.1f,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Text(
                        text = "❤️ LIKE",
                        color = Color.Green,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(24.dp)
                            .rotate(-15f)
                    )
                }
                androidx.compose.animation.AnimatedVisibility(
                    visible = passAlpha > 0.1f,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Text(
                        text = "PASS",
                        color = Color.Red,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(24.dp)
                            .rotate(15f)
                    )
                }

                // User Info (Bottom)
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(20.dp)
                ) {
                    Text(
                        text = "${user.name}, ${user.age}",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.LocationOn,
                            contentDescription = "Location",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = user.borough.ifEmpty { "NYC" },
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        // NYC Energy Badge
                        if (user.nycEnergy.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(50.dp),
                                color = NYC_Red.copy(alpha = 0.8f),
                                modifier = Modifier.padding(vertical = 2.dp)
                            ) {
                                Text(
                                    text = "🗽 ${user.nycEnergy}",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    // Personality Tags (show max 3)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(user.personalityTags.take(3)) { tag ->
                            Surface(
                                shape = RoundedCornerShape(50.dp),
                                color = Color.White.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = tag,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // Direct Message Button (Top Right)
                IconButton(
                    onClick = {
                        // Send message request
                        // We handle this in ViewModel
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .size(44.dp)
                        .clip(RoundedCornerShape(50.dp))
                        .background(Color.White)
                ) {
                    Icon(
                        Icons.Filled.Message,
                        contentDescription = "Message",
                        tint = NYC_Red
                    )
                }
            }
        }

        // Action Buttons (Bottom Center)
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 100.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Pass Button
            FloatingActionButton(
                onClick = onSwipeLeft,
                containerColor = Color.White,
                elevation = FloatingActionButtonDefaults.elevation(8.dp),
                modifier = Modifier.size(60.dp)
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Pass", tint = Color.Red, modifier = Modifier.size(32.dp))
            }

            // Super Like Button
            FloatingActionButton(
                onClick = { onSwipeRight() },
                containerColor = Color.White,
                elevation = FloatingActionButtonDefaults.elevation(8.dp),
                modifier = Modifier.size(60.dp)
            ) {
                Icon(Icons.Filled.Star, contentDescription = "Super Like", tint = Color.Blue, modifier = Modifier.size(32.dp))
            }

            // Like Button
            FloatingActionButton(
                onClick = onSwipeRight,
                containerColor = Color.White,
                elevation = FloatingActionButtonDefaults.elevation(8.dp),
                modifier = Modifier.size(60.dp)
            ) {
                Icon(Icons.Filled.Favorite, contentDescription = "Like", tint = Color.Green, modifier = Modifier.size(32.dp))
            }
        }
    }
}