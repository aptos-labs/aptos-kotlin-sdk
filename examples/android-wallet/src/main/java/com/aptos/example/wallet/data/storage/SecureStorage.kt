package com.aptos.example.wallet.data.storage

interface SecureStorage {
    fun saveMnemonic(phrase: String)
    fun getMnemonic(): String?
    fun savePrivateKeyHex(hex: String)
    fun getPrivateKeyHex(): String?
    fun hasAccount(): Boolean
    fun clear()
}
