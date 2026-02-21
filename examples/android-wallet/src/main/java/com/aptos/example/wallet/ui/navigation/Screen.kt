package com.aptos.example.wallet.ui.navigation

import kotlinx.serialization.Serializable

sealed interface Screen {
    @Serializable
    data object Onboarding : Screen

    @Serializable
    data object Home : Screen

    @Serializable
    data object Send : Screen

    @Serializable
    data object Transactions : Screen

    @Serializable
    data object Settings : Screen
}
