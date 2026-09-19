package com.dark.nyc

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dark.nyc.ui.navigation.Screen
import com.dark.nyc.ui.screens.auth.LoginScreen
import com.dark.nyc.ui.screens.auth.SignUpScreen
import com.dark.nyc.ui.screens.home.HomeScreen
import com.dark.nyc.ui.screens.onboarding.OnboardingScreen
import com.dark.nyc.ui.screens.paywall.PaywallScreen
import com.dark.nyc.ui.screens.splash.SplashScreen
import com.dark.nyc.ui.theme.NYCDatingTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()



    // ✅ Google Sign-In Launcher
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                auth.signInWithCredential(credential)
                    .addOnSuccessListener { authResult ->
                        val user = authResult.user
                        if (user != null) {
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    val userMap = mapOf(
                                        "uid" to user.uid,
                                        "name" to (account.displayName ?: ""),
                                        "email" to (account.email ?: ""),
                                        "photoURL" to (account.photoUrl?.toString() ?: ""),
                                        "createdAt" to System.currentTimeMillis(),
                                        "onboardingComplete" to false
                                    )
                                    firestore.collection("Users")
                                        .document(user.uid)
                                        .set(userMap)
                                        .await()

                                    runOnUiThread {
                                        Toast.makeText(
                                            this@MainActivity,
                                            "Google Sign-In Success!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        // ✅ Navigate to Onboarding
                                        navigateToOnboarding?.invoke()
                                    }
                                } catch (e: Exception) {
                                    runOnUiThread {
                                        Toast.makeText(
                                            this@MainActivity,
                                            "Save failed: ${e.message}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Google Auth failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } catch (e: ApiException) {
                Toast.makeText(this, "Google Sign-In failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ✅ Navigation callback for Google Sign-In
    private var navigateToOnboarding: (() -> Unit)? = null

    fun launchGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        val client = GoogleSignIn.getClient(this, gso)
        googleSignInLauncher.launch(client.signInIntent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NYCDatingTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }

    @Composable
    fun AppNavigation() {
        val navController = rememberNavController()

        // ✅ Set the navigation callback for Google Sign-In
        navigateToOnboarding = {
            navController.navigate(Screen.Onboarding.route) {
                popUpTo(0) { inclusive = true }
            }
        }

        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route
        ) {
            composable(route = Screen.Splash.route) {
                SplashScreen(navController = navController)
            }

            composable(route = Screen.SignUp.route) {
                SignUpScreen(
                    navController = navController,
                    onSignUpSuccess = {
                        // ✅ Email/Password sign-up -> Onboarding
                        navController.navigate(Screen.Onboarding.route) {
                            popUpTo(0) { inclusive = true }  // popUpTo (small p, small U)
                        }
                    },
                    onGoogleSignInClick = {
                        launchGoogleSignIn()
                    }
                )
            }

            composable("login") {
                LoginScreen(
                    navController = navController,
                    onLoginSuccess = { isOnboardingComplete ->
                        if (isOnboardingComplete) {
                            navController.navigate("home") {
                                popUpTo("login") { inclusive = true }
                            }
                        } else {
                            navController.navigate("onboarding") {
                                popUpTo("login") { inclusive = true }
                            }
                        }
                    },
                    onGoogleSignInClick = {
                        launchGoogleSignIn()
                    },
                    onNavigateToSignUp = {
                        navController.navigate("signup")
                    }
                )
            }

            composable(route = Screen.Onboarding.route) {
                OnboardingScreen(
                    navController = navController,
                    onComplete = {
                        // ✅ Onboarding complete hone ke baad Paywall pe jao
                        navController.navigate(Screen.Paywall.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
            composable(route = Screen.Paywall.route) {
                PaywallScreen(
                    navController = navController,
                    onSubscriptionSuccess = {
                        // ✅ Subscription/Code success -> Home
                        navController.navigate(Screen.Home.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

// Home screen placeholder (abhi ke liye)
            composable(route = Screen.Home.route) {
                HomeScreen(navController = navController)
            }        }
    }
}