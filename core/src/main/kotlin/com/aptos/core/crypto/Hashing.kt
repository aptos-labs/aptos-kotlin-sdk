package com.aptos.core.crypto

import org.bouncycastle.jcajce.provider.digest.SHA256
import org.bouncycastle.jcajce.provider.digest.SHA3

/**
 * Cryptographic hashing utilities for the Aptos SDK.
 *
 * Provides SHA3-256 and SHA-256 hash functions, plus Aptos-specific domain-separated
 * hashing used for transaction signing messages.
 */
object Hashing {
    /** Computes the SHA3-256 hash of [data] (32-byte output). */
    @JvmStatic
    fun sha3256(data: ByteArray): ByteArray {
        val digest = SHA3.Digest256()
        return digest.digest(data)
    }

    /** Computes the SHA-256 hash of [data] (32-byte output). */
    @JvmStatic
    fun sha256(data: ByteArray): ByteArray {
        val digest = SHA256.Digest()
        return digest.digest(data)
    }

    /**
     * Computes `SHA3-256(SHA3-256("prefix::prefix") || data)`.
     *
     * This is the Aptos domain-separated hash construction used for transaction
     * signing messages and other typed hashes.
     */
    @JvmStatic
    fun domainSeparatedHash(data: ByteArray, domainPrefix: String): ByteArray {
        val prefixHash = sha3256("$domainPrefix::$domainPrefix".toByteArray(Charsets.UTF_8))
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
