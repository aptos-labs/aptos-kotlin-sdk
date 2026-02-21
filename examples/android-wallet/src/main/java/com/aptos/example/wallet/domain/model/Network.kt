package com.aptos.example.wallet.domain.model

import com.aptos.client.config.AptosConfig

enum class Network(val displayName: String) {
    TESTNET("Testnet"),
    DEVNET("Devnet"),
    MAINNET("Mainnet"),
    ;

    fun toAptosConfig(): AptosConfig = when (this) {
        TESTNET -> AptosConfig.testnet()
        DEVNET -> AptosConfig.devnet()
        MAINNET -> AptosConfig.mainnet()
    }

    val hasFaucet: Boolean get() = this != MAINNET
}
