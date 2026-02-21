package com.aptos.core.crypto

import org.bouncycastle.jcajce.provider.digest.SHA256
import org.bouncycastle.jcajce.provider.digest.SHA3

/**
 * Cryptographic hashing utilities for the Aptos SDK.
 */
object Hashing {

    @JvmStatic
    fun sha3256(data: ByteArray): ByteArray {
        val digest = SHA3.Digest256()
        return digest.digest(data)
    }

    @JvmStatic
    fun sha256(data: ByteArray): ByteArray {
        val digest = SHA256.Digest()
        return digest.digest(data)
    }

    @JvmStatic
    fun domainSeparatedHash(data: ByteArray, domainPrefix: String): ByteArray {
        val prefixHash = sha3256("${domainPrefix}::$domainPrefix".toByteArray(Charsets.UTF_8))
        val digest = SHA3.Digest256()
        digest.update(prefixHash)
        digest.update(data)
        return digest.digest()
    }

    /** Pre-computed SHA3-256("APTOS::RawTransaction") for signing messages. */
    @JvmStatic
    val RAW_TRANSACTION_PREFIX: ByteArray by lazy {
        sha3256("APTOS::RawTransaction".toByteArray(Charsets.UTF_8))
    }

    /** Pre-computed SHA3-256("APTOS::RawTransactionWithData") for multi-agent/fee-payer. */
    @JvmStatic
    val RAW_TRANSACTION_WITH_DATA_PREFIX: ByteArray by lazy {
        sha3256("APTOS::RawTransactionWithData".toByteArray(Charsets.UTF_8))
    }

    @JvmStatic
    val TRANSACTION_PREFIX: ByteArray by lazy {
        sha3256("APTOS::Transaction".toByteArray(Charsets.UTF_8))
    }
}
