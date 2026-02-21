package com.aptos.example.wallet.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aptos.example.wallet.domain.model.Network
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "wallet_preferences")

@Singleton
class NetworkPreferences @Inject constructor(@ApplicationContext private val context: Context) {
    private val networkKey = stringPreferencesKey("selected_network")

    val selectedNetwork: Flow<Network> = context.dataStore.data.map { prefs ->
        val name = prefs[networkKey] ?: Network.TESTNET.name
        Network.valueOf(name)
    }

    suspend fun setNetwork(network: Network) {
        context.dataStore.edit { prefs ->
            prefs[networkKey] = network.name
        }
    }
}
