package com.aptos.core.account

import com.aptos.core.error.MnemonicException
import org.bouncycastle.crypto.ec.CustomNamedCurves
import org.bouncycastle.math.ec.FixedPointCombMultiplier
import java.math.BigInteger
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * BIP-32 key derivation for Secp256k1.
 */
object Bip32 {
    private const val BITCOIN_SEED = "Bitcoin seed"
    private val curveParams = CustomNamedCurves.getByName("secp256k1")

    @JvmStatic
    fun deriveSecp256k1(seed: ByteArray, path: DerivationPath): ByteArray {
        val (key, chainCode) = masterKey(seed)
        var currentKey = key
        var currentChainCode = chainCode

        for (component in path.components) {
            val derived =
                if (component.hardened) {
                    deriveHardenedChild(currentKey, currentChainCode, component.toInt())
                } else {
                    deriveNormalChild(currentKey, currentChainCode, component.index.toInt())
                }
            currentKey = derived.first
            currentChainCode = derived.second
        }

        return currentKey
    }

    private fun masterKey(seed: ByteArray): Pair<ByteArray, ByteArray> {
        val hmac = hmacSha512(BITCOIN_SEED.toByteArray(Charsets.UTF_8), seed)
        val key = hmac.copyOfRange(0, 32)
        val chainCode = hmac.copyOfRange(32, 64)
        val keyInt = BigInteger(1, key)
        if (keyInt == BigInteger.ZERO || keyInt >= curveParams.n) {
            throw MnemonicException("Invalid master key derived")
        }
        return key to chainCode
    }

    private fun deriveHardenedChild(key: ByteArray, chainCode: ByteArray, index: Int): Pair<ByteArray, ByteArray> {
        val data = ByteArray(37)
        data[0] = 0x00
        System.arraycopy(key, 0, data, 1, 32)
        data[33] = ((index shr 24) and 0xFF).toByte()
        data[34] = ((index shr 16) and 0xFF).toByte()
        data[35] = ((index shr 8) and 0xFF).toByte()
        data[36] = (index and 0xFF).toByte()

        return deriveFromHmac(key, chainCode, data)
    }

    private fun deriveNormalChild(key: ByteArray, chainCode: ByteArray, index: Int): Pair<ByteArray, ByteArray> {
        val pubKey = publicKeyFromPrivate(key)
        val data = ByteArray(37)
        System.arraycopy(pubKey, 0, data, 0, 33)
        data[33] = ((index shr 24) and 0xFF).toByte()
        data[34] = ((index shr 16) and 0xFF).toByte()
        data[35] = ((index shr 8) and 0xFF).toByte()
        data[36] = (index and 0xFF).toByte()

        return deriveFromHmac(key, chainCode, data)
    }

    private fun deriveFromHmac(
        parentKey: ByteArray,
        chainCode: ByteArray,
        data: ByteArray,
    ): Pair<ByteArray, ByteArray> {
        val hmac = hmacSha512(chainCode, data)
        val il = BigInteger(1, hmac.copyOfRange(0, 32))
        val parentKeyInt = BigInteger(1, parentKey)
        val childKey = il.add(parentKeyInt).mod(curveParams.n)
        if (il >= curveParams.n || childKey == BigInteger.ZERO) {
            throw MnemonicException("Invalid child key derived")
        }
        val childKeyBytes =
            childKey.toByteArray().let { bytes ->
                when {
                    bytes.size > 32 -> bytes.copyOfRange(bytes.size - 32, bytes.size)
                    bytes.size < 32 -> ByteArray(32 - bytes.size) + bytes
                    else -> bytes
                }
            }
        return childKeyBytes to hmac.copyOfRange(32, 64)
    }

    private fun publicKeyFromPrivate(privateKey: ByteArray): ByteArray {
        val d = BigInteger(1, privateKey)
        val q = FixedPointCombMultiplier().multiply(curveParams.g, d)
        return q.getEncoded(true)
    }

    private fun hmacSha512(key: ByteArray, data: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA512")
        mac.init(SecretKeySpec(key, "HmacSHA512"))
        return mac.doFinal(data)
    }
}
