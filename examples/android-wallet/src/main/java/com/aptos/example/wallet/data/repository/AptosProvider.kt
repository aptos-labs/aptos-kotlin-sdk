package com.aptos.example.wallet.data.repository

import com.aptos.example.wallet.domain.model.Network
import com.aptos.sdk.Aptos
import io.ktor.client.engine.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AptosProvider @Inject constructor(private val engine: HttpClientEngine) {
    private var cachedNetwork: Network? = null
    private var cachedAptos: Aptos? = null

    @Synchronized
    fun get(network: Network): Aptos {
        if (cachedNetwork == network && cachedAptos != null) {
            return cachedAptos!!
        }
        cachedAptos?.close()
        val config = network.toAptosConfig()
        val aptos = Aptos(config, engine)
        cachedAptos = aptos
        cachedNetwork = network
        return aptos
    }

    @Synchronized
    fun invalidate() {
        cachedAptos?.close()
        cachedAptos = null
        cachedNetwork = null
    }
}
