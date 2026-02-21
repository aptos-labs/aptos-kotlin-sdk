package com.aptos.example.wallet.domain.model

sealed interface SendResult {
    data class Success(val hash: String) : SendResult
    data class Error(val message: String) : SendResult
}
