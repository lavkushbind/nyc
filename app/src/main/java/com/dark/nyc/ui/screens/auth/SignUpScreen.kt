package com.dark.nyc.ui.screens.auth

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.dark.nyc.R
import com.dark.nyc.ui.navigation.Screen
import com.dark.nyc.ui.theme.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun SignUpScreen(
    navController: NavController,
    onSignUpSuccess: () -> Unit,

    onGoogleSignInClick: () -> Unit,// Success ke baad Onboarding pe jayega
//    googleSignInLauncher: (Int, androidx.activity.result.ActivityResultLauncher<android.content.Intent>) -> Unit // MainActivity se pass hoga
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()

    // ===== STATE VARIABLES =====
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // ===== GOOGLE SIGN-IN CLIENT =====


    val webClientId = stringResource(
        id = R.string.default_web_client_id
    )

    val gso = GoogleSignInOptions.Builder(
        GoogleSignInOptions.DEFAULT_SIGN_IN
    )
        .requestIdToken(webClientId)
        .requestEmail()
        .build()

    val googleSignInClient = GoogleSignIn.getClient(
        context,
        gso
    )

    // ===== UI RENDER =====
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp)
            .padding(top = 48.dp, bottom = 32.dp)
            .systemBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- 1. BRAND HEADER ---
        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "NYC Dating Logo",
            modifier = Modifier.size(80.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Create Account",
            style = MaterialTheme.typography.headlineLarge,
            color = TextPrimary,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Join the NYC dating vibe",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(32.dp))


        OutlinedButton(
            onClick = {
                if (!isLoading) {
                    onGoogleSignInClick()
                }
            },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(50.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.White,
                contentColor = TextPrimary
            ),
            border = BorderStroke(1.dp, BorderLight)  // ✅ BorderStroke is imported
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.google),
                    contentDescription = "Google",
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Continue with Google",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        // --- 2. GOOGLE SIGN-UP BUTTON ---


        // --- 3. DIVIDER ---
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Divider(
                color = BorderLight,
                modifier = Modifier.weight(1f),
                thickness = 1.dp
            )
            Text(
                text = " OR ",
                style = MaterialTheme.typography.labelSmall,
                color = TextHint,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Divider(
                color = BorderLight,
                modifier = Modifier.weight(1f),
                thickness = 1.dp
            )
        }
        Spacer(modifier = Modifier.height(24.dp))

        // --- 4. EMAIL / PASSWORD INPUTS ---
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Name Field
            // Full Name
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    errorMessage = null
                },
                label = { Text("Full Name", color = TextSecondary) },
                placeholder = { Text("Alex", color = TextHint) },
                modifier = Modifier
                    .fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = {
                        focusManager.moveFocus(FocusDirection.Down)
                    }
                ),
                colors = nycTextFieldColors(),
                singleLine = true,
                isError = errorMessage != null && name.isEmpty()
            )

// Email Field
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    errorMessage = null
                },
                label = { Text("Email Address", color = TextSecondary) },
                placeholder = { Text("alex@nyc.com", color = TextHint) },
                modifier = Modifier
                    .fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = {
                        focusManager.moveFocus(FocusDirection.Down)
                    }
                ),
                colors = nycTextFieldColors(),
                singleLine = true,
                isError = errorMessage != null &&
                        email.isNotEmpty() &&
                        !android.util.Patterns.EMAIL_ADDRESS
                            .matcher(email)
                            .matches()
            )

// Password Field
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    errorMessage = null
                },
                label = { Text("Password", color = TextSecondary) },
                placeholder = { Text("••••••••", color = TextHint) },
                modifier = Modifier
                    .fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                    }
                ),
                visualTransformation = if (isPasswordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    IconButton(
                        onClick = {
                            isPasswordVisible = !isPasswordVisible
                        }
                    ) {
                        Icon(
                            imageVector = if (isPasswordVisible) {
                                Icons.Default.Visibility
                            } else {
                                Icons.Default.VisibilityOff
                            },
                            contentDescription = if (isPasswordVisible) {
                                "Hide password"
                            } else {
                                "Show password"
                            },
                            tint = TextHint
                        )
                    }
                },
                colors = nycTextFieldColors(),
                singleLine = true,
                isError = errorMessage != null &&
                        password.isNotEmpty() &&
                        password.length < 6
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- 5. ERROR MESSAGE (Animated) ---
        AnimatedVisibility(
            visible = errorMessage != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Text(
                text = errorMessage ?: "",
                color = StatusError,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- 6. SIGN UP BUTTON (SOLID RED) ---
        Button(
            onClick = {
                scope.launch {
                    // ===== VALIDATION =====
                    if (name.isBlank()) {
                        errorMessage = "Please enter your full name"
                        focusManager.clearFocus()
                        return@launch
                    }
                    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        errorMessage = "Please enter a valid email"
                        focusManager.clearFocus()
                        return@launch
                    }
                    if (password.length < 6) {
                        errorMessage = "Password must be at least 6 characters"
                        focusManager.clearFocus()
                        return@launch
                    }

                    isLoading = true
                    errorMessage = null

                    try {
                        // 1. Firebase Auth Sign-Up
                        val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                        val user = authResult.user

                        if (user != null) {
                            // 2. Save User Data to Firestore (nyc_users)
                            val userMap = mapOf(
                                "uid" to user.uid,
                                "name" to name.trim(),
                                "email" to email.trim(),
                                "createdAt" to System.currentTimeMillis(),
                                "onboardingComplete" to false, // Onboarding pending
                                "photoURL" to "",
                                "borough" to "",
                                "personalityTags" to emptyList<String>()
                            )

                            firestore.collection("nyc_users")
                                .document(user.uid)
                                .set(userMap)
                                .await()

                            isLoading = false

                            // 3. Success -> Onboarding Screen
                            Toast.makeText(context, "Account created successfully!", Toast.LENGTH_SHORT).show()
                            onSignUpSuccess()

                        } else {
                            errorMessage = "Sign-up failed. Please try again."
                            isLoading = false
                        }

                    } catch (e: Exception) {
                        errorMessage = e.localizedMessage ?: "Sign-up failed. Please try again."
                        isLoading = false
                    }
                }
            },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(50.dp)),
            colors = ButtonDefaults.buttonColors(
                containerColor = NYC_Red,
                contentColor = TextInverse,
                disabledContainerColor = SoftGray,
                disabledContentColor = TextHint
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 0.dp,
                pressedElevation = 2.dp
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = TextInverse,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text(
                    text = "CREATE ACCOUNT",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextInverse,
                    letterSpacing = 0.5.sp
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // --- 7. ALREADY HAVE ACCOUNT? ---
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Text(
                text = "Already have an account? ",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            TextButton(
                onClick = {
                    navController.navigate("login") {
                        popUpTo("signup") { inclusive = true }
                    }
                }
            ) {
                Text(
                    text = "Log In",
                    style = MaterialTheme.typography.labelMedium,
                    color = NYC_Red,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ===== Reusable TextField Colors =====
@Composable
fun nycTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = NYC_Red,
    unfocusedBorderColor = BorderLight,
    focusedLabelColor = NYC_Red,
    cursorColor = NYC_Red,
    focusedContainerColor = OffWhite,
    unfocusedContainerColor = OffWhite
)