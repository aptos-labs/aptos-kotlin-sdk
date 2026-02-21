package com.aptos.example.wallet.ui.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight

@Composable
fun BalanceText(
    balanceApt: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = balanceApt,
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.Bold,
        modifier = modifier,
    )
}
