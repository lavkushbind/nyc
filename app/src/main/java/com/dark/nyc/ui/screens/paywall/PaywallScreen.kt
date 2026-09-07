package com.dark.nyc.ui.screens.paywall

import android.widget.Toast

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

import com.dark.nyc.R
import com.dark.nyc.ui.navigation.Screen
import com.dark.nyc.ui.theme.*

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

import kotlin.random.Random


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaywallScreen(
    navController: NavController,
    onSubscriptionSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()

    // ===== STATES =====

    var selectedPlan by remember {
        mutableStateOf("yearly")
    }

    var codeInput by remember {
        mutableStateOf("")
    }

    var isLoading by remember {
        mutableStateOf(false)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    // ===== PRICING =====

    val monthlyPrice = "$29.99"
    val yearlyPrice = "$89.99"

    // ===== MAIN UI =====

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(
                    top = 48.dp,
                    bottom = 24.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ){

            // ============================================================
            // HEADER
            // ============================================================

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                NYC_Red,
                                NYC_RedDark
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Icon(
                        imageVector = Icons.Filled.EmojiEvents,
                        contentDescription = "Premium",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )

                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )

                    Text(
                        text = "PREMIUM ACCESS",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp
                    )

                    Text(
                        text = "Get the full NYC dating experience",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(24.dp)
            )

            // ============================================================
            // PRICING CARDS
            // ============================================================

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                PlanCard(
                    title = "Monthly",
                    price = monthlyPrice,
                    isSelected = selectedPlan == "monthly",
                    onClick = {
                        selectedPlan = "monthly"
                    }
                )

                PlanCard(
                    title = "Yearly",
                    price = yearlyPrice,
                    isSelected = selectedPlan == "yearly",
                    isBestValue = true,
                    onClick = {
                        selectedPlan = "yearly"
                    }
                )
            }

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            // ============================================================
            // SAVINGS
            // ============================================================

            if (selectedPlan == "yearly") {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(50.dp))
                        .background(NYC_RedSurface)
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Save",
                            tint = NYC_Red,
                            modifier = Modifier.size(16.dp)
                        )

                        Spacer(
                            modifier = Modifier.width(6.dp)
                        )

                        Text(
                            text = "You save 40% compared to monthly!",
                            color = NYC_RedDark,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(
                    modifier = Modifier.height(8.dp)
                )
            }

            // ============================================================
            // FEATURES
            // ============================================================

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = OffWhite
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 0.dp
                )
            ) {

                Column(
                    modifier = Modifier.padding(
                        horizontal = 16.dp,
                        vertical = 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {

                    PremiumFeatureRow(
                        icon = Icons.Filled.Swipe,
                        title = "Unlimited Swipes",
                        subtitle = "Swipe without any restrictions"
                    )

                    PremiumFeatureRow(
                        icon = Icons.Filled.Message,
                        title = "Direct Messages",
                        subtitle = "Message anyone instantly"
                    )

                    PremiumFeatureRow(
                        icon = Icons.Filled.People,
                        title = "3 Seats (1 Code)",
                        subtitle = "Share with 2 friends absolutely free"
                    )

                    PremiumFeatureRow(
                        icon = Icons.Filled.Verified,
                        title = "Verified Profiles",
                        subtitle = "Trustworthy dating community"
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            // ============================================================
            // SOCIAL PROOF
            // ============================================================

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Row {

                    repeat(3) {

                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(50.dp))
                                .background(SoftGray)
                                .border(
                                    2.dp,
                                    Color.White,
                                    RoundedCornerShape(50.dp)
                                )
                        )

                        Spacer(
                            modifier = Modifier.width((-6).dp)
                        )
                    }
                }

                Spacer(
                    modifier = Modifier.width(12.dp)
                )

                Text(
                    text = "9,200+ New Yorkers joined this week",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            // ============================================================
            // SUBSCRIBE BUTTON
            // ============================================================

            Button(
                onClick = {

                    scope.launch {

                        isLoading = true
                        errorMessage = null

                        try {

                            val user = auth.currentUser ?: run {

                                errorMessage = "Please login again"
                                isLoading = false

                                return@launch
                            }

                            // Check existing subscription

                            val existing = firestore
                                .collection("nyc_subscriptions")
                                .whereEqualTo(
                                    "userId",
                                    user.uid
                                )
                                .whereEqualTo(
                                    "isActive",
                                    true
                                )
                                .get()
                                .await()

                            if (!existing.isEmpty) {

                                errorMessage = "Already subscribed!"
                                isLoading = false

                                return@launch
                            }

                            // Generate Code

                            val code =
                                "NYC-${user.uid.takeLast(4).uppercase()}-${
                                    Random.nextInt(
                                        1000,
                                        9999
                                    )
                                }"

                            val expiry =
                                System.currentTimeMillis() +
                                        (
                                                if (selectedPlan == "yearly") {
                                                    365L
                                                } else {
                                                    30L
                                                }
                                                ) *
                                        24 *
                                        60 *
                                        60 *
                                        1000

                            val subMap = mapOf(

                                "userId" to user.uid,

                                "groupCode" to code,

                                "isActive" to true,

                                "seatsTotal" to 3,

                                "seatsUsed" to 1,

                                "members" to listOf(user.uid),

                                "plan" to selectedPlan,

                                "createdAt" to System.currentTimeMillis(),

                                "expiresAt" to expiry
                            )

                            firestore
                                .collection("nyc_subscriptions")
                                .document(user.uid)
                                .set(subMap)
                                .await()

                            firestore
                                .collection("nyc_users")
                                .document(user.uid)
                                .update(
                                    "hasActiveSubscription",
                                    true,
                                    "groupCode",
                                    code
                                )
                                .await()

                            isLoading = false

                            Toast.makeText(
                                context,
                                "🎉 Welcome to Premium!",
                                Toast.LENGTH_LONG
                            ).show()

                            onSubscriptionSuccess()

                        } catch (e: Exception) {

                            errorMessage =
                                e.localizedMessage
                                    ?: "Subscription failed"

                            isLoading = false
                        }
                    }
                },

                enabled = !isLoading,

                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .shadow(
                        12.dp,
                        RoundedCornerShape(50.dp),
                        clip = false
                    )
                    .clip(
                        RoundedCornerShape(50.dp)
                    )
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                NYC_Red,
                                NYC_RedDark
                            )
                        )
                    ),

                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.White
                ),

                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp
                )
            ) {

                if (isLoading) {

                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(24.dp)
                    )

                } else {

                    Text(
                        text =
                            "SUBSCRIBE NOW - ${
                                if (selectedPlan == "yearly") {
                                    yearlyPrice
                                } else {
                                    monthlyPrice
                                }
                            }",

                        style = MaterialTheme.typography.labelLarge,

                        color = Color.White,

                        fontWeight = FontWeight.Black,

                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            // ============================================================
            // HAVE A CODE
            // ============================================================

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                HorizontalDivider(
                    color = BorderLight,
                    modifier = Modifier.weight(1f),
                    thickness = 1.dp
                )

                Text(
                    text = " Have a code? ",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextHint,
                    fontSize = 12.sp
                )

                HorizontalDivider(
                    color = BorderLight,
                    modifier = Modifier.weight(1f),
                    thickness = 1.dp
                )
            }

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            // ============================================================
            // CODE INPUT
            // ============================================================

            OutlinedTextField(
                value = codeInput,

                onValueChange = {

                    codeInput = it
                        .uppercase()
                        .trim()

                    errorMessage = null
                },

                placeholder = {

                    Text(
                        text = "Enter friend's code (e.g. NYC-ABCD-1234)",
                        color = TextHint,
                        fontSize = 13.sp
                    )
                },

                leadingIcon = {

                    Icon(
                        imageVector = Icons.Filled.Group,
                        contentDescription = "Code",
                        tint = if (codeInput.isNotEmpty()) {
                            NYC_Red
                        } else {
                            TextHint
                        },
                        modifier = Modifier.size(20.dp)
                    )
                },

                trailingIcon = {

                    if (codeInput.isNotEmpty()) {

                        IconButton(
                            onClick = {
                                codeInput = ""
                            }
                        ) {

                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Clear",
                                tint = TextHint
                            )
                        }
                    }
                },

                // IMPORTANT:
                // .imeAction() yahan nahi lagana hai.
                modifier = Modifier
                    .fillMaxWidth(),

                shape = RoundedCornerShape(16.dp),

                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done
                ),

                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                    }
                ),

                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NYC_Red,
                    unfocusedBorderColor = BorderLight,
                    focusedLabelColor = NYC_Red,
                    unfocusedLabelColor = TextSecondary,
                    cursorColor = NYC_Red,

                    // IMPORTANT:
                    // containerColor nahi,
                    // focusedContainerColor/unfocusedContainerColor
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),

                singleLine = true,

                isError = errorMessage != null
            )

            // ============================================================
            // ERROR MESSAGE
            // ============================================================

            if (errorMessage != null) {

                Text(
                    text = errorMessage ?: "",
                    color = StatusError,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            // ============================================================
            // REDEEM CODE
            // ============================================================

            OutlinedButton(

                onClick = {

                    scope.launch {

                        if (codeInput.isBlank()) {

                            errorMessage = "Please enter a code"

                            return@launch
                        }

                        isLoading = true

                        try {

                            val user = auth.currentUser ?: run {

                                errorMessage = "Please login"

                                isLoading = false

                                return@launch
                            }

                            val query = firestore
                                .collection("nyc_subscriptions")
                                .whereEqualTo(
                                    "groupCode",
                                    codeInput
                                )
                                .whereEqualTo(
                                    "isActive",
                                    true
                                )
                                .limit(1)
                                .get()
                                .await()

                            if (query.isEmpty) {

                                errorMessage =
                                    "Invalid code. Please check."

                                isLoading = false

                                return@launch
                            }

                            val doc = query.documents[0]

                            val seatsUsed =
                                doc.getLong("seatsUsed")
                                    ?.toInt()
                                    ?: 0

                            val seatsTotal =
                                doc.getLong("seatsTotal")
                                    ?.toInt()
                                    ?: 3

                            val members =
                                doc.get("members")
                                        as? List<String>
                                    ?: emptyList()

                            if (seatsUsed >= seatsTotal) {

                                errorMessage =
                                    "This code is fully redeemed (3/3 seats taken)."

                                isLoading = false

                                return@launch
                            }

                            if (members.contains(user.uid)) {

                                errorMessage =
                                    "You are already in this group!"

                                isLoading = false

                                return@launch
                            }

                            val updatedMembers =
                                members + user.uid

                            firestore
                                .collection("nyc_subscriptions")
                                .document(doc.id)
                                .update(
                                    mapOf(
                                        "members" to updatedMembers,
                                        "seatsUsed" to seatsUsed + 1
                                    )
                                )
                                .await()

                            firestore
                                .collection("nyc_users")
                                .document(user.uid)
                                .update(
                                    "hasActiveSubscription",
                                    true,
                                    "groupCode",
                                    codeInput
                                )
                                .await()

                            isLoading = false

                            Toast.makeText(
                                context,
                                "✅ Code redeemed successfully!",
                                Toast.LENGTH_LONG
                            ).show()

                            onSubscriptionSuccess()

                        } catch (e: Exception) {

                            errorMessage =
                                e.localizedMessage
                                    ?: "Failed to redeem code"

                            isLoading = false
                        }
                    }
                },

                enabled = !isLoading &&
                        codeInput.isNotBlank(),

                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(
                        RoundedCornerShape(16.dp)
                    ),

                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.White,
                    contentColor = NYC_Red
                ),

                border = BorderStroke(
                    2.dp,
                    NYC_Red
                )
            ) {

                if (isLoading) {

                    CircularProgressIndicator(
                        color = NYC_Red,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(20.dp)
                    )

                } else {

                    Text(
                        text = "REDEEM CODE",
                        style = MaterialTheme.typography.labelLarge,
                        color = NYC_Red,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            IconButton(
                onClick = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Paywall.route) {
                            inclusive = true
                        }
                    }
                },
                modifier = Modifier
//                    .align(Alignment.TopEnd)
                    .padding(
                        top = 8.dp,
                        end = 8.dp
                    )
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Skip",
                    tint = TextSecondary
                )
            }
            // ============================================================
            // FOOTER
            // ============================================================

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = "Secure",
                        tint = TextHint,
                        modifier = Modifier.size(14.dp)
                    )

                    Spacer(
                        modifier = Modifier.width(4.dp)
                    )

                    Text(
                        text = "Secure Payment",
                        color = TextHint,
                        fontSize = 11.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Icon(
                        imageVector = Icons.Outlined.Sync,
                        contentDescription = "Cancel",
                        tint = TextHint,
                        modifier = Modifier.size(14.dp)
                    )

                    Spacer(
                        modifier = Modifier.width(4.dp)
                    )

                    Text(
                        text = "Cancel Anytime",
                        color = TextHint,
                        fontSize = 11.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Icon(
                        imageVector = Icons.Outlined.Verified,
                        contentDescription = "Trust",
                        tint = TextHint,
                        modifier = Modifier.size(14.dp)
                    )

                    Spacer(
                        modifier = Modifier.width(4.dp)
                    )

                    Text(
                        text = "30-Day Guarantee",
                        color = TextHint,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            // ============================================================
            // TERMS
            // ============================================================

            Text(
                text = "By subscribing, you agree to our Terms & Privacy Policy.",
                color = TextHint,
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}


// ========================================================================
// PLAN CARD
// ========================================================================

@Composable
fun RowScope.PlanCard(
    title: String,
    price: String,
    isSelected: Boolean,
    isBestValue: Boolean = false,
    onClick: () -> Unit
) {

    Card(
        modifier = Modifier
            .weight(1f)
            .height(120.dp)
            .clickable {
                onClick()
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                Color.White
            } else {
                OffWhite
            }
        ),
        border = if (isSelected) {
            BorderStroke(
                2.dp,
                NYC_Red
            )
        } else {
            null
        }
    ) {

        Box(
            modifier = Modifier.fillMaxSize()
        ) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {

                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) {
                        NYC_Red
                    } else {
                        TextSecondary
                    },
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                Text(
                    text = price,
                    style = MaterialTheme.typography.headlineLarge,
                    color = if (isSelected) {
                        TextPrimary
                    } else {
                        TextSecondary
                    },
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp
                )

                if (title.lowercase() == "monthly") {
                    Text(
                        text = "/mo",
                        color = TextHint,
                        fontSize = 12.sp
                    )
                }
            }

            // BEST VALUE BADGE
            if (isBestValue) {

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(50.dp))
                        .background(NYC_Red)
                        .padding(
                            horizontal = 10.dp,
                            vertical = 4.dp
                        )
                ) {

                    Text(
                        text = "SAVE 40%",
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // CHECK MARK
            if (isSelected) {

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .size(20.dp)
                        .clip(RoundedCornerShape(50.dp))
                        .background(NYC_Red),
                    contentAlignment = Alignment.Center
                ) {

                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

// ========================================================================
// PREMIUM FEATURE ROW
// ========================================================================

@Composable
fun PremiumFeatureRow(
    icon: ImageVector,
    title: String,
    subtitle: String
) {

    Row(

        modifier = Modifier.fillMaxWidth(),

        verticalAlignment =
            Alignment.CenterVertically,

        horizontalArrangement =
            Arrangement.spacedBy(12.dp)
    ) {

        Box(

            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(NYC_RedSurface),

            contentAlignment = Alignment.Center
        ) {

            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = NYC_Red,
                modifier = Modifier.size(20.dp)
            )
        }

        Column {

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                fontSize = 12.sp
            )
        }
    }
}