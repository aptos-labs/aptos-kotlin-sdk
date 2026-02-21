package com.aptos.example.wallet.domain.model

data class WalletAccount(val address: String, val balanceOctas: ULong) {
    val shortAddress: String
        get() = if (address.length > 10) {
            "${address.take(6)}...${address.takeLast(4)}"
        } else {
            address
        }

    val balanceApt: String
        get() {
            val whole = balanceOctas / 100_000_000uL
            val fraction = balanceOctas % 100_000_000uL
            return if (fraction == 0uL) {
                "$whole.0 APT"
            } else {
                val fractionStr = fraction.toString().padStart(8, '0').trimEnd('0')
                "$whole.$fractionStr APT"
            }
        }
}
