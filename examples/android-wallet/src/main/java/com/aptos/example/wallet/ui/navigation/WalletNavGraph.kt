package com.aptos.example.wallet.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aptos.example.wallet.ui.screen.home.HomeScreen
import com.aptos.example.wallet.ui.screen.onboarding.OnboardingScreen
import com.aptos.example.wallet.ui.screen.send.SendScreen
import com.aptos.example.wallet.ui.screen.settings.SettingsScreen
import com.aptos.example.wallet.ui.screen.transactions.TransactionsScreen

@Composable
fun WalletNavGraph(hasAccount: Boolean) {
    val navController = rememberNavController()
    val startDestination = if (hasAccount) Screen.Home else Screen.Onboarding

    NavHost(navController = navController, startDestination = startDestination) {
        composable<Screen.Onboarding> {
            OnboardingScreen(
                onAccountCreated = {
                    navController.navigate(Screen.Home) {
                        popUpTo(Screen.Onboarding) { inclusive = true }
                    }
                },
            )
        }
        composable<Screen.Home> {
            HomeScreen(
                onNavigateToSend = { navController.navigate(Screen.Send) },
                onNavigateToTransactions = { navController.navigate(Screen.Transactions) },
                onNavigateToSettings = { navController.navigate(Screen.Settings) },
            )
        }
        composable<Screen.Send> {
            SendScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable<Screen.Transactions> {
            TransactionsScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable<Screen.Settings> {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onWalletDeleted = {
                    navController.navigate(Screen.Onboarding) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }
    }
}
