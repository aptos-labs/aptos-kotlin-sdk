package com.aptos.example.wallet.domain.model

data class TransactionItem(
    val hash: String,
    val type: String?,
    val success: Boolean,
    val timestamp: String?,
    val gasUsed: String?,
    val version: String?,
)
