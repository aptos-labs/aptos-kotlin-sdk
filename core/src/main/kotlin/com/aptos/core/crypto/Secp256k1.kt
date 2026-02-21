package com.aptos.core.crypto

import com.aptos.core.bcs.BcsSerializable
import com.aptos.core.bcs.BcsSerializer
import com.aptos.core.error.CryptoException
import com.aptos.core.types.HexString
import org.bouncycastle.asn1.ASN1Integer
import org.bouncycastle.asn1.DERSequence
import org.bouncycastle.asn1.ASN1Sequence
import org.bouncycastle.crypto.ec.CustomNamedCurves
import org.bouncycastle.crypto.params.ECDomainParameters
import org.bouncycastle.crypto.params.ECPrivateKeyParameters
import org.bouncycastle.crypto.params.ECPublicKeyParameters
import org.bouncycastle.crypto.signers.ECDSASigner
import org.bouncycastle.crypto.signers.HMacDSAKCalculator
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.math.ec.ECPoint
import org.bouncycastle.math.ec.FixedPointCombMultiplier
import java.math.BigInteger
import java.security.SecureRandom

object Secp256k1 {

    const val PRIVATE_KEY_LENGTH = 32
    const val PUBLIC_KEY_COMPRESSED_LENGTH = 33
    const val PUBLIC_KEY_UNCOMPRESSED_LENGTH = 65
    const val SIGNATURE_LENGTH = 64

    private val curveParams = CustomNamedCurves.getByName("secp256k1")
    private val domainParams = ECDomainParameters(
        curveParams.curve, curveParams.g, curveParams.n, curveParams.h
    )
    private val halfN = curveParams.n.shiftRight(1)

    data class PrivateKey(val data: ByteArray) {
        init {
            require(data.size == PRIVATE_KEY_LENGTH) {
                "Secp256k1 private key must be $PRIVATE_KEY_LENGTH bytes, got ${data.size}"
            }
        }

        fun publicKey(): PublicKey {
            val d = BigInteger(1, data)
            val q = FixedPointCombMultiplier().multiply(curveParams.g, d)
            return PublicKey(q.getEncoded(false))
        }

        fun sign(message: ByteArray): Signature {
            val hash = Hashing.sha256(message)
            val d = BigInteger(1, data)
            val signer = ECDSASigner(HMacDSAKCalculator(SHA256Digest()))
            signer.init(true, ECPrivateKeyParameters(d, domainParams))
            val components = signer.generateSignature(hash)
            var r = components[0]
            var s = components[1]
            // Low-S normalization
            if (s > halfN) {
                s = curveParams.n.subtract(s)
            }
            return Signature(encodeRS(r, s))
        }

        fun toHex(): String = HexString.encodeWithPrefix(data)

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is PrivateKey) return false
            return data.contentEquals(other.data)
        }

        override fun hashCode(): Int = data.contentHashCode()
        override fun toString(): String = "Secp256k1PrivateKey(***)"

        companion object {
            @JvmStatic
            fun generate(): PrivateKey {
                val random = SecureRandom()
                val bytes = ByteArray(PRIVATE_KEY_LENGTH)
                var d: BigInteger
                do {
                    random.nextBytes(bytes)
                    d = BigInteger(1, bytes)
                } while (d == BigInteger.ZERO || d >= curveParams.n)
                return PrivateKey(bytes)
            }

            @JvmStatic
            fun fromHex(hex: String): PrivateKey = PrivateKey(HexString.decode(hex))
        }
    }

    data class PublicKey(val data: ByteArray) : BcsSerializable {
        init {
            require(
                data.size == PUBLIC_KEY_UNCOMPRESSED_LENGTH || data.size == PUBLIC_KEY_COMPRESSED_LENGTH
            ) {
                "Secp256k1 public key must be $PUBLIC_KEY_COMPRESSED_LENGTH or " +
                    "$PUBLIC_KEY_UNCOMPRESSED_LENGTH bytes, got ${data.size}"
            }
        }

        override fun serialize(serializer: BcsSerializer) {
            serializer.serializeBytes(data)
        }

        fun compressed(): ByteArray {
            if (data.size == PUBLIC_KEY_COMPRESSED_LENGTH) return data.copyOf()
            val point = curveParams.curve.decodePoint(data)
            return point.getEncoded(true)
        }

        fun uncompressed(): ByteArray {
            if (data.size == PUBLIC_KEY_UNCOMPRESSED_LENGTH) return data.copyOf()
            val point = curveParams.curve.decodePoint(data)
            return point.getEncoded(false)
        }

        fun verify(message: ByteArray, signature: Signature): Boolean {
            return try {
                val hash = Hashing.sha256(message)
                val (r, s) = decodeRS(signature.data)
                // Reject high-S
                if (s > halfN) return false
                val point = curveParams.curve.decodePoint(data)
                val signer = ECDSASigner()
                signer.init(false, ECPublicKeyParameters(point, domainParams))
                signer.verifySignature(hash, r, s)
            } catch (e: Exception) {
                throw CryptoException("Secp256k1 verification failed", e)
            }
        }

        fun toHex(): String = HexString.encodeWithPrefix(data)

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is PublicKey) return false
            return data.contentEquals(other.data)
        }

        override fun hashCode(): Int = data.contentHashCode()
        override fun toString(): String = "Secp256k1PublicKey(${toHex()})"

        companion object {
            @JvmStatic
            fun fromHex(hex: String): PublicKey = PublicKey(HexString.decode(hex))
        }
    }

    data class Signature(val data: ByteArray) : BcsSerializable {
        init {
            require(data.size == SIGNATURE_LENGTH) {
                "Secp256k1 signature must be $SIGNATURE_LENGTH bytes, got ${data.size}"
            }
        }

        override fun serialize(serializer: BcsSerializer) {
            serializer.serializeBytes(data)
        }

        fun toHex(): String = HexString.encodeWithPrefix(data)

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Signature) return false
            return data.contentEquals(other.data)
        }

        override fun hashCode(): Int = data.contentHashCode()
        override fun toString(): String = "Secp256k1Signature(${toHex()})"

        companion object {
            @JvmStatic
            fun fromHex(hex: String): Signature = Signature(HexString.decode(hex))
        }
    }

    private fun encodeRS(r: BigInteger, s: BigInteger): ByteArray {
        val result = ByteArray(SIGNATURE_LENGTH)
        val rBytes = r.toByteArray()
        val sBytes = s.toByteArray()
        // r: right-aligned in first 32 bytes
        val rStart = if (rBytes[0] == 0.toByte()) 1 else 0
        val rLen = rBytes.size - rStart
        System.arraycopy(rBytes, rStart, result, 32 - rLen, rLen)
        // s: right-aligned in last 32 bytes
        val sStart = if (sBytes[0] == 0.toByte()) 1 else 0
        val sLen = sBytes.size - sStart
        System.arraycopy(sBytes, sStart, result, 64 - sLen, sLen)
        return result
    }

    private fun decodeRS(data: ByteArray): Pair<BigInteger, BigInteger> {
        val r = BigInteger(1, data.copyOfRange(0, 32))
        val s = BigInteger(1, data.copyOfRange(32, 64))
        return r to s
    }
}
