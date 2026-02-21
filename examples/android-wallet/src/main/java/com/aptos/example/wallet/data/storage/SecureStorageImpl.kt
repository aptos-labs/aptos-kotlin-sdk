package com.aptos.example.wallet.data.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecureStorageImpl @Inject constructor(@ApplicationContext context: Context) : SecureStorage {

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        "aptos_wallet_secure_prefs",
        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    override fun saveMnemonic(phrase: String) {
        prefs.edit().putString(KEY_MNEMONIC, phrase).apply()
    }

    override fun getMnemonic(): String? = prefs.getString(KEY_MNEMONIC, null)

    override fun savePrivateKeyHex(hex: String) {
        prefs.edit().putString(KEY_PRIVATE_KEY, hex).apply()
    }

    override fun getPrivateKeyHex(): String? = prefs.getString(KEY_PRIVATE_KEY, null)

    override fun hasAccount(): Boolean = prefs.contains(KEY_PRIVATE_KEY)

    override fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val KEY_MNEMONIC = "mnemonic"
        const val KEY_PRIVATE_KEY = "private_key_hex"
    }
}
