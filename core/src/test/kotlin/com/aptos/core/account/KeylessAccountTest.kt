package com.aptos.core.account

import com.aptos.core.crypto.EphemeralKeyPair
import com.aptos.core.crypto.Keyless
import com.aptos.core.crypto.SignatureScheme
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class KeylessAccountTest {

    private fun createKeylessAccount(expired: Boolean = false): KeylessAccount {
        val expiration = if (expired) {
            System.currentTimeMillis() / 1000 - 3600
        } else {
            System.currentTimeMillis() / 1000 + 3600
        }
        val ekp = EphemeralKeyPair.generate(expiration)
        val pk = Keyless.PublicKey(
            iss = "https://accounts.google.com",
            aud = "test-client-id",
            uidKey = "sub",
            uidVal = "user123",
            pepper = ByteArray(31) { it.toByte() },
        )
        return KeylessAccount.create(ekp, pk, ByteArray(64) { 0 }, "fake.jwt.token")
    }

    @Test
    fun `create keyless account`() {
        val account = createKeylessAccount()
        account.scheme shouldBe SignatureScheme.KEYLESS
        account.address.toHex().length shouldBe 66 // 0x + 64 hex chars
    }

    @Test
    fun `address derived from keyless auth key`() {
        val ekp = EphemeralKeyPair.generate(System.currentTimeMillis() / 1000 + 3600)
        val pk = Keyless.PublicKey(
            iss = "https://accounts.google.com",
            aud = "test-client-id",
            uidKey = "sub",
            uidVal = "user123",
            pepper = ByteArray(31) { 0 },
        )
        val account = KeylessAccount.create(ekp, pk, ByteArray(32), "jwt")
        account.address shouldBe pk.authKey().derivedAddress()
    }

    @Test
    fun `sign succeeds when not expired`() {
        val account = createKeylessAccount(expired = false)
        val sig = account.sign("test message".toByteArray())
        sig.isNotEmpty() shouldBe true
    }

    @Test
    fun `sign throws when expired`() {
        val account = createKeylessAccount(expired = true)
        shouldThrow<IllegalStateException> {
            account.sign("test message".toByteArray())
        }
    }

    @Test
    fun `publicKeyBytes returns BCS-serialized keyless public key`() {
        val account = createKeylessAccount()
        account.publicKeyBytes.isNotEmpty() shouldBe true
    }

    @Test
    fun `authenticationKey matches public key authKey`() {
        val ekp = EphemeralKeyPair.generate(System.currentTimeMillis() / 1000 + 3600)
        val pk = Keyless.PublicKey(
            iss = "iss",
            aud = "aud",
            uidKey = "sub",
            uidVal = "user",
            pepper = ByteArray(31) { 0 },
        )
        val account = KeylessAccount.create(ekp, pk, ByteArray(32), "jwt")
        account.authenticationKey shouldBe pk.authKey()
    }
}
