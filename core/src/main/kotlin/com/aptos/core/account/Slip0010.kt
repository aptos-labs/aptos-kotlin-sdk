package com.aptos.core.account

import com.aptos.core.error.MnemonicException
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * SLIP-0010 key derivation for Ed25519.
 * Uses HMAC-SHA512 chain derivation as specified in SLIP-0010.
 */
object Slip0010 {
    private const val ED25519_SEED = "ed25519 seed"

    @JvmStatic
    fun deriveEd25519(seed: ByteArray, path: DerivationPath): ByteArray {
        val (key, chainCode) = masterKey(seed)
        var currentKey = key
        var currentChainCode = chainCode

        for (component in path.components) {
            if (!component.hardened) {
                throw MnemonicException("SLIP-0010 Ed25519 derivation only supports hardened paths")
            }
            val derived = deriveChild(currentKey, currentChainCode, component.toInt())
            currentKey = derived.first
            currentChainCode = derived.second
        }

        return currentKey
    }

    private fun masterKey(seed: ByteArray): Pair<ByteArray, ByteArray> {
        val hmac = hmacSha512(ED25519_SEED.toByteArray(Charsets.UTF_8), seed)
        return hmac.copyOfRange(0, 32) to hmac.copyOfRange(32, 64)
    }

    private fun deriveChild(key: ByteArray, chainCode: ByteArray, index: Int): Pair<ByteArray, ByteArray> {
        val data = ByteArray(37)
        data[0] = 0x00
        System.arraycopy(key, 0, data, 1, 32)
        data[33] = ((index shr 24) and 0xFF).toByte()
        data[34] = ((index shr 16) and 0xFF).toByte()
        data[35] = ((index shr 8) and 0xFF).toByte()
        data[36] = (index and 0xFF).toByte()

        val hmac = hmacSha512(chainCode, data)
        return hmac.copyOfRange(0, 32) to hmac.copyOfRange(32, 64)
    }

    private fun hmacSha512(key: ByteArray, data: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA512")
        mac.init(SecretKeySpec(key, "HmacSHA512"))
        return mac.doFinal(data)
    }
}
