package com.dark.nyc.ui.screens.chat

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.window.Dialog
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.dark.nyc.data.models.ChatMessage
import com.dark.nyc.data.models.NYCUser
import com.dark.nyc.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    targetUser: NYCUser,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val storage = FirebaseStorage.getInstance()

    val currentUserId = auth.currentUser?.uid ?: ""
    val targetUserId = targetUser.uid

    // Unique consistent chatId
    val chatId = remember(currentUserId, targetUserId) {
        if (currentUserId < targetUserId) "${currentUserId}_${targetUserId}"
        else "${targetUserId}_${currentUserId}"
    }

    // State
    var messageText by remember { mutableStateOf("") }
    val messages = remember { mutableStateListOf<ChatMessage>() }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isUploadingImage by remember { mutableStateOf(false) }
    var replyingToMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var fullScreenImagePreview by remember { mutableStateOf<String?>(null) }

    val listState = rememberLazyListState()

    // 1️⃣ REALTIME FIRESTORE LISTENER
    DisposableEffect(chatId) {
        val query = firestore.collection("nyc_chats")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)

        val listenerRegistration = query.addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            if (snapshot != null) {
                messages.clear()
                for (doc in snapshot.documents) {
                    val msg = doc.toObject(ChatMessage::class.java)
                    if (msg != null) messages.add(msg)
                }
                scope.launch {
                    if (messages.isNotEmpty()) {
                        listState.animateScrollToItem(messages.size - 1)
                    }
                }
            }
        }

        onDispose { listenerRegistration.remove() }
    }

    // Auto-scroll on new message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    // SEND MESSAGE
    fun sendMessage() {
        if (messageText.isBlank() && selectedImageUri == null) return

        val textToSend = messageText.trim()
        val imageUriToSend = selectedImageUri
        val replyRef = replyingToMessage

        messageText = ""
        selectedImageUri = null
        replyingToMessage = null

        scope.launch {
            try {
                var uploadedImageUrl: String? = null

                if (imageUriToSend != null) {
                    isUploadingImage = true
                    val imageRef = storage.reference.child("nyc_chat_images/$chatId/${UUID.randomUUID()}.jpg")
                    imageRef.putFile(imageUriToSend).await()
                    uploadedImageUrl = imageRef.downloadUrl.await().toString()
                    isUploadingImage = false
                }

                val messageId = UUID.randomUUID().toString()
                val newMsg = ChatMessage(
                    id = messageId,
                    senderId = currentUserId,
                    receiverId = targetUserId,
                    text = textToSend,
                    imageUrl = uploadedImageUrl,
                    replyToText = replyRef?.text ?: if (replyRef?.imageUrl != null) "📷 Photo" else null,
                    replyToSenderName = if (replyRef != null) {
                        if (replyRef.senderId == currentUserId) "You" else targetUser.name
                    } else null,
                    timestamp = System.currentTimeMillis(),
                    isRead = false
                )

                firestore.collection("nyc_chats")
                    .document(chatId)
                    .collection("messages")
                    .document(messageId)
                    .set(newMsg.toMap())
                    .await()

                firestore.collection("nyc_chats").document(chatId).set(
                    mapOf(
                        "lastMessage" to if (uploadedImageUrl != null) "📷 Photo" else textToSend,
                        "lastTimestamp" to System.currentTimeMillis(),
                        "lastSenderId" to currentUserId,
                        "users" to listOf(currentUserId, targetUserId)
                    )
                )

            } catch (e: Exception) {
                isUploadingImage = false
                Toast.makeText(context, "Failed to send: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(), // Handles keyboard elevation without adding top margin
        containerColor = Color(0xFFF8F9FA),
        topBar = {
            // --- SLEEK COMPACT TOP BAR (No Extra Spacing) ---
            Surface(
                color = PureWhite,
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Avatar with online status
                    Box(modifier = Modifier.size(40.dp)) {
                        SubcomposeAsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(targetUser.photoURL)
                                .crossfade(true)
                                .build(),
                            contentDescription = targetUser.name,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop,
                            loading = { CircularProgressIndicator(strokeWidth = 2.dp, color = NYC_Red) },
                            error = { Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray) }
                        )

                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(Color(0xFF2ECC71), CircleShape)
                                .border(1.5.dp, PureWhite, CircleShape)
                                .align(Alignment.BottomEnd)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = targetUser.name,
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "📍 ${targetUser.neighborhood.ifBlank { targetUser.borough }}",
                            fontSize = 11.5.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        },
        bottomBar = {
            // --- BOTTOM COMPACT INPUT BAR ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PureWhite)
            ) {
                // Reply Quote Preview
                AnimatedVisibility(
                    visible = replyingToMessage != null,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    if (replyingToMessage != null) {
                        Surface(
                            color = Color(0xFFF1F2F6),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(30.dp)
                                        .background(NYC_Red, RoundedCornerShape(2.dp))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (replyingToMessage!!.senderId == currentUserId) "You" else targetUser.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp,
                                        color = NYC_Red
                                    )
                                    Text(
                                        text = replyingToMessage!!.text.ifBlank { "📷 Photo" },
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = TextSecondary
                                    )
                                }
                                IconButton(
                                    onClick = { replyingToMessage = null },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                // Attached Image Preview
                AnimatedVisibility(
                    visible = selectedImageUri != null,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    if (selectedImageUri != null) {
                        Surface(
                            color = Color(0xFFF3F4F6),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(70.dp)
                            ) {
                                SubcomposeAsyncImage(
                                    model = selectedImageUri,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                IconButton(
                                    onClick = { selectedImageUri = null },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(20.dp)
                                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                }
                            }
                        }
                    }
                }

                // Input Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { imagePicker.launch("image/*") },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = "Attach",
                            tint = Color(0xFF6B7280),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        placeholder = { Text("Message...", fontSize = 14.sp, color = Color(0xFF9CA3AF)) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFE5E7EB),
                            unfocusedBorderColor = Color(0xFFE5E7EB),
                            focusedContainerColor = Color(0xFFF9FAFB),
                            unfocusedContainerColor = Color(0xFFF9FAFB)
                        ),
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    val canSend = messageText.isNotBlank() || selectedImageUri != null
                    IconButton(
                        onClick = { sendMessage() },
                        enabled = canSend && !isUploadingImage,
                        modifier = Modifier
                            .size(42.dp)
                            .background(
                                if (canSend) NYC_Red else Color(0xFFE5E7EB),
                                CircleShape
                            )
                    ) {
                        if (isUploadingImage) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = if (canSend) Color.White else Color(0xFF9CA3AF),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        // --- MESSAGES LIST (Starts Immediately Under TopBar) ---
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(messages, key = { it.id }) { message ->
                val isMe = message.senderId == currentUserId
                ModernMessageBubble(
                    message = message,
                    isMe = isMe,
                    onImageClick = { url -> fullScreenImagePreview = url },
                    onReplyClick = { replyingToMessage = message }
                )
            }
        }

        // Fullscreen Image Dialog
        if (fullScreenImagePreview != null) {
            Dialog(onDismissRequest = { fullScreenImagePreview = null }) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.92f))
                        .clickable { fullScreenImagePreview = null },
                    contentAlignment = Alignment.Center
                ) {
                    SubcomposeAsyncImage(
                        model = fullScreenImagePreview,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
    }
}

// ====================================================
// MODERN CLEAN BUBBLE (iOS / Telegram Style)
// ====================================================
@Composable
fun ModernMessageBubble(
    message: ChatMessage,
    isMe: Boolean,
    onImageClick: (String) -> Unit,
    onReplyClick: () -> Unit
) {
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val formattedTime = remember(message.timestamp) { timeFormat.format(Date(message.timestamp)) }

    val bubbleShape = if (isMe) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 3.dp)
    } else {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 3.dp, bottomEnd = 16.dp)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    ) {
        Column(
            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Surface(
                shape = bubbleShape,
                color = if (isMe) NYC_Red else PureWhite,
                border = if (isMe) null else BorderStroke(1.dp, Color(0xFFE5E7EB)),
                modifier = Modifier.combinedClickable(
                    onClick = {},
                    onLongClick = onReplyClick
                )
            ) {
                Column(modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp)) {

                    // Quote Reply if present
                    if (!message.replyToText.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isMe) Color.Black.copy(alpha = 0.15f) else Color(0xFFF3F4F6),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 5.dp)
                        ) {
                            Row(modifier = Modifier.padding(5.dp)) {
                                Box(
                                    modifier = Modifier
                                        .width(2.5.dp)
                                        .height(24.dp)
                                        .background(if (isMe) Color.White else NYC_Red, RoundedCornerShape(1.dp))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = message.replyToSenderName ?: "Reply",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.5.sp,
                                        color = if (isMe) Color.White else NYC_Red
                                    )
                                    Text(
                                        text = message.replyToText,
                                        fontSize = 11.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = if (isMe) Color.White.copy(alpha = 0.85f) else TextSecondary
                                    )
                                }
                            }
                        }
                    }

                    // Image
                    if (!message.imageUrl.isNullOrBlank()) {
                        SubcomposeAsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(message.imageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onImageClick(message.imageUrl) },
                            contentScale = ContentScale.Crop,
                            loading = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp) } }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    // Text
                    if (message.text.isNotBlank()) {
                        Text(
                            text = message.text,
                            color = if (isMe) Color.White else TextPrimary,
                            fontSize = 14.sp,
                            lineHeight = 19.sp
                        )
                    }

                    // Time
                    Row(
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(top = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formattedTime,
                            fontSize = 9.5.sp,
                            color = if (isMe) Color.White.copy(alpha = 0.75f) else Color(0xFF9CA3AF)
                        )
                    }
                }
            }
        }
    }
}