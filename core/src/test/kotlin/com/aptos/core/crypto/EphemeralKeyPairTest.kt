package com.aptos.core.crypto

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldNotBeEmpty
import org.junit.jupiter.api.Test

class EphemeralKeyPairTest {

    @Test
    fun `generate creates valid key pair`() {
        val futureExpiration = System.currentTimeMillis() / 1000 + 3600
        val ekp = EphemeralKeyPair.generate(futureExpiration)
        ekp.publicKey.data.size shouldBe Ed25519.PUBLIC_KEY_LENGTH
        ekp.expirationDateSecs shouldBe futureExpiration
        ekp.nonce.shouldNotBeEmpty()
    }

    @Test
    fun `isExpired returns false for future expiration`() {
        val futureExpiration = System.currentTimeMillis() / 1000 + 3600
        val ekp = EphemeralKeyPair.generate(futureExpiration)
        ekp.isExpired() shouldBe false
    }

    @Test
    fun `isExpired returns true for past expiration`() {
        val pastExpiration = System.currentTimeMillis() / 1000 - 3600
        val ekp = EphemeralKeyPair.generate(pastExpiration)
        ekp.isExpired() shouldBe true
    }

    @Test
    fun `sign succeeds when not expired`() {
        val futureExpiration = System.currentTimeMillis() / 1000 + 3600
        val ekp = EphemeralKeyPair.generate(futureExpiration)
        val sig = ekp.sign("test message".toByteArray())
        sig.data.size shouldBe Ed25519.SIGNATURE_LENGTH
        ekp.publicKey.verify("test message".toByteArray(), sig) shouldBe true
    }

    @Test
    fun `sign throws when expired`() {
        val pastExpiration = System.currentTimeMillis() / 1000 - 3600
        val ekp = EphemeralKeyPair.generate(pastExpiration)
        shouldThrow<IllegalStateException> {
            ekp.sign("test".toByteArray())
        }
    }

    @Test
    fun `fromPrivateKey restores key pair`() {
        val futureExpiration = System.currentTimeMillis() / 1000 + 3600
        val original = EphemeralKeyPair.generate(futureExpiration)
        val restored = EphemeralKeyPair.fromPrivateKey(
            original.privateKey,
            original.expirationDateSecs,
            original.blinder,
        )
        restored.nonce shouldBe original.nonce
        restored.publicKey shouldBe original.publicKey
    }

    @Test
    fun `different key pairs have different nonces`() {
        val futureExpiration = System.currentTimeMillis() / 1000 + 3600
        val ekp1 = EphemeralKeyPair.generate(futureExpiration)
        val ekp2 = EphemeralKeyPair.generate(futureExpiration)
        ekp1.nonce shouldNotBe ekp2.nonce
    }

    @Test
    fun `publicKeyBytes returns correct data`() {
        val ekp = EphemeralKeyPair.generate(System.currentTimeMillis() / 1000 + 3600)
        ekp.publicKeyBytes().size shouldBe Ed25519.PUBLIC_KEY_LENGTH
        ekp.publicKeyBytes() shouldBe ekp.publicKey.data
    }
}
