package com.dark.nyc.ui.screens.splash

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.dark.nyc.R
import com.dark.nyc.ui.navigation.Screen
import com.dark.nyc.ui.theme.NYC_Red
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SplashScreen(
    navController: NavController
) {
    var isNavigated by remember { mutableStateOf(false) }

    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()

    LaunchedEffect(key1 = Unit) {
        delay(2000)

        if (isNavigated) return@LaunchedEffect

        try {
            val currentUser = auth.currentUser

            // ❌ No User → SignUp
            if (currentUser == null) {
                isNavigated = true
                navController.navigate(Screen.SignUp.route) {
                    popUpTo(0) { inclusive = true }
                }
                return@LaunchedEffect
            }

            // ✅ User exists → Check Firestore for User Document
            val userDoc = firestore.collection("nyc_users")
                .document(currentUser.uid)
                .get()
                .await()

            if (!userDoc.exists()) {
                // 🔥 User document doesn't exist → Create it (safety fallback)
                // But better: go to Onboarding to complete profile
                isNavigated = true
                navController.navigate(Screen.Onboarding.route) {
                    popUpTo(0) { inclusive = true }
                }
                return@LaunchedEffect
            }

            // ✅ Check onboardingComplete status
            val onboardingComplete = userDoc.getBoolean("onboardingComplete") ?: false

            if (!onboardingComplete) {
                // 🔥 Onboarding NOT complete → Go to Onboarding
                isNavigated = true
                navController.navigate(Screen.Onboarding.route) {
                    popUpTo(0) { inclusive = true }
                }
                return@LaunchedEffect
            }

            // ✅ Onboarding complete → Check Subscription
            val subscriptionQuery = firestore.collection("subscriptions")
                .whereEqualTo("userId", currentUser.uid)
                .whereEqualTo("isActive", true)
                .limit(1)
                .get()
                .await()

            if (!subscriptionQuery.isEmpty) {
                // ✅ Active Subscription → Home
                isNavigated = true
                navController.navigate(Screen.Home.route) {
                    popUpTo(0) { inclusive = true }
                }
            } else {
                // ❌ No Active Subscription → Paywall
                isNavigated = true
                navController.navigate(Screen.Paywall.route) {
                    popUpTo(0) { inclusive = true }
                }
            }

        } catch (e: Exception) {
            // 🚨 Error → Safety fallback: Paywall
            if (!isNavigated) {
                isNavigated = true
                navController.navigate(Screen.Paywall.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }

    // ===== UI RENDER =====
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "NYC Dating Logo",
                modifier = Modifier.size(160.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))

            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = NYC_Red,
                strokeWidth = 4.dp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Finding your NYC vibe...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}