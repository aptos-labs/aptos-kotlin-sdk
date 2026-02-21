package com.aptos.example.wallet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.aptos.example.wallet.data.storage.SecureStorage
import com.aptos.example.wallet.ui.navigation.WalletNavGraph
import com.aptos.example.wallet.ui.theme.WalletTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var secureStorage: SecureStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val hasAccount = secureStorage.hasAccount()
        setContent {
            WalletTheme {
                WalletNavGraph(hasAccount = hasAccount)
            }
        }
    }
}
