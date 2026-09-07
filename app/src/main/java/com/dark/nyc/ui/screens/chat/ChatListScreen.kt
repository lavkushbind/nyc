package com.dark.nyc.ui.screens.chat

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.dark.nyc.data.models.NYCUser
import com.dark.nyc.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

data class ChatConversationItem(
    val chatId: String,
    val otherUser: NYCUser,
    val lastMessage: String,
    val lastTimestamp: Long,
    val lastSenderId: String,
    val unreadCount: Int = 0,
    val isOnline: Boolean = true
)

@Composable
fun ChatListScreen(
    onChatClick: (NYCUser) -> Unit,
    newMatches: List<NYCUser> = emptyList()
) {
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val currentUserId = auth.currentUser?.uid ?: ""

    var searchQuery by remember { mutableStateOf("") }
    val conversations = remember { mutableStateListOf<ChatConversationItem>() }
    var isLoading by remember { mutableStateOf(true) }

    // 1️⃣ REALTIME FIRESTORE LISTENER
    DisposableEffect(currentUserId) {
        if (currentUserId.isBlank()) {
            isLoading = false
            return@DisposableEffect onDispose {}
        }

        val listener = firestore.collection("nyc_chats")
            .whereArrayContains("users", currentUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    isLoading = false
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    for (doc in snapshot.documents) {
                        val chatId = doc.id
                        val usersList = doc.get("users") as? List<*> ?: emptyList<String>()
                        val otherUserId = usersList.firstOrNull { it != currentUserId } as? String ?: continue
                        val lastMsg = doc.getString("lastMessage") ?: ""
                        val lastTimestamp = doc.getLong("lastTimestamp") ?: 0L
                        val lastSender = doc.getString("lastSenderId") ?: ""
                        val unread = (doc.getLong("unreadCount_$currentUserId") ?: 0L).toInt()

                        firestore.collection("nyc_users").document(otherUserId)
                            .get()
                            .addOnSuccessListener { userDoc ->
                                val user = userDoc.toObject(NYCUser::class.java)
                                if (user != null) {
                                    val safeUser = if (user.uid.isBlank()) user.copy(uid = otherUserId) else user
                                    conversations.removeAll { it.chatId == chatId }
                                    conversations.add(
                                        ChatConversationItem(
                                            chatId = chatId,
                                            otherUser = safeUser,
                                            lastMessage = lastMsg,
                                            lastTimestamp = lastTimestamp,
                                            lastSenderId = lastSender,
                                            unreadCount = unread
                                        )
                                    )
                                    conversations.sortByDescending { it.lastTimestamp }
                                }
                            }
                    }
                    isLoading = false
                }
            }

        onDispose { listener.remove() }
    }

    // Filter Logic
    val filteredConversations = remember(searchQuery, conversations.toList()) {
        if (searchQuery.isBlank()) {
            conversations
        } else {
            conversations.filter {
                it.otherUser.name.contains(searchQuery, ignoreCase = true) ||
                        it.otherUser.neighborhood.contains(searchQuery, ignoreCase = true) ||
                        it.lastMessage.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val totalUnread = remember(conversations.toList()) {
        conversations.sumOf { it.unreadCount }
    }

    // NO INNER SCAFFOLD - PURE COMPACT COLUMN
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureWhite)
    ) {
        // --- COMPACT HEADER (Zero Extra Margins) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Messages",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary
                )
                if (totalUnread > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(NYC_Red)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "$totalUnread",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(50.dp),
                color = NYC_RedSurface,
                border = BorderStroke(1.dp, NYC_Red.copy(alpha = 0.3f))
            ) {
                Text(
                    text = "Lovora Direct",
                    color = NYC_RedDark,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        // --- SLEEK COMPACT SEARCH BAR ---
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search matches or messages...", fontSize = 13.5.sp, color = TextSecondary) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp)) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(25.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NYC_Red,
                unfocusedBorderColor = BorderLight,
                focusedContainerColor = OffWhite,
                unfocusedContainerColor = OffWhite
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // --- NEW CONNECTIONS (HORIZONTAL STORIES) ---
        if (newMatches.isNotEmpty() && searchQuery.isEmpty()) {
            Column {
                Text(
                    text = "New Matches 🔥",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(newMatches) { user ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { onChatClick(user) }
                                .padding(vertical = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(62.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, NYC_Red, CircleShape)
                                    .padding(2.5.dp)
                            ) {
                                SubcomposeAsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(user.photoURL)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = user.name,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop,
                                    loading = { CircularProgressIndicator(color = NYC_Red, strokeWidth = 2.dp) }
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = user.name,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = Color(0xFFF3F4F6), thickness = 1.dp)
            }
        }

        // --- CONVERSATIONS LIST ---
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NYC_Red, strokeWidth = 3.dp)
            }
        } else if (filteredConversations.isEmpty()) {
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
                            .background(NYC_RedSurface, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (searchQuery.isNotEmpty()) Icons.Default.SearchOff else Icons.Outlined.ChatBubbleOutline,
                            contentDescription = null,
                            tint = NYC_Red,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty()) "No chats matching '$searchQuery'" else "No messages yet",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty()) "Try searching another name" else "Connect with locals on Explore to start vibrant chats!",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp)
            ) {
                items(filteredConversations, key = { it.chatId }) { item ->
                    val hasUnread = item.unreadCount > 0
                    val isMe = item.lastSenderId == currentUserId

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onChatClick(item.otherUser) }
                            .background(if (hasUnread) Color(0xFFFFF7F8) else Color.Transparent)
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // User Avatar with Online Dot
                        Box(modifier = Modifier.size(54.dp)) {
                            SubcomposeAsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(item.otherUser.photoURL)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = item.otherUser.name,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .border(
                                        width = if (hasUnread) 2.dp else 1.dp,
                                        color = if (hasUnread) NYC_Red else BorderLight,
                                        shape = CircleShape
                                    ),
                                contentScale = ContentScale.Crop,
                                loading = { CircularProgressIndicator(color = NYC_Red, strokeWidth = 2.dp) }
                            )

                            if (item.isOnline) {
                                Box(
                                    modifier = Modifier
                                        .size(13.dp)
                                        .background(Color(0xFF2ECC71), CircleShape)
                                        .border(2.dp, PureWhite, CircleShape)
                                        .align(Alignment.BottomEnd)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        // User Details & Last Message
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = item.otherUser.name,
                                    fontSize = 15.5.sp,
                                    fontWeight = if (hasUnread) FontWeight.ExtraBold else FontWeight.Bold,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(50.dp),
                                    color = OffWhite
                                ) {
                                    Text(
                                        text = item.otherUser.neighborhood.ifBlank { item.otherUser.borough },
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextSecondary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(3.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isMe) {
                                    Icon(
                                        imageVector = Icons.Default.DoneAll,
                                        contentDescription = null,
                                        tint = if (hasUnread) NYC_Red else Color.Gray,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }

                                Text(
                                    text = if (isMe) "You: ${item.lastMessage}" else item.lastMessage,
                                    fontSize = 13.sp,
                                    fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.Normal,
                                    color = if (hasUnread) TextPrimary else TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Time & Badge
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = formatChatListTime(item.lastTimestamp),
                                fontSize = 11.sp,
                                fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.Normal,
                                color = if (hasUnread) NYC_Red else Color.Gray
                            )

                            Spacer(modifier = Modifier.height(5.dp))

                            if (hasUnread) {
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(NYC_Red)
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (item.unreadCount > 99) "99+" else item.unreadCount.toString(),
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// SMART RELATIVE TIME FORMATTER
fun formatChatListTime(timestamp: Long): String {
    if (timestamp <= 0) return ""
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
    val hours = TimeUnit.MILLISECONDS.toHours(diff)
    val days = TimeUnit.MILLISECONDS.toDays(diff)

    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "${minutes}m"
        hours < 24 -> {
            val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
        days == 1L -> "Yesterday"
        days < 7 -> "${days}d"
        else -> {
            val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}