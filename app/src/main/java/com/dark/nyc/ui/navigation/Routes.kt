package com.dark.nyc.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object SignUp : Screen("signup")
    object Onboarding : Screen("onboarding")
    object Paywall : Screen("paywall")
    object Home : Screen("home")
}