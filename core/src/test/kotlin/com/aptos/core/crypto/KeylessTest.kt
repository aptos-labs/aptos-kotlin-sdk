package com.aptos.core.crypto

import com.aptos.core.error.CryptoException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test
import java.util.Base64

class KeylessTest {

    private fun createTestJwt(
        iss: String = "https://accounts.google.com",
        aud: String = "test-client-id",
        sub: String = "user123",
        nonce: String = "test-nonce",
    ): String {
        val header = Base64.getUrlEncoder().withoutPadding()
            .encodeToString("""{"alg":"RS256","typ":"JWT"}""".toByteArray())
        val payload = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(
                """{"iss":"$iss","aud":"$aud","sub":"$sub","nonce":"$nonce"}""".toByteArray(),
            )
        val signature = Base64.getUrlEncoder().withoutPadding()
            .encodeToString("fake-signature".toByteArray())
        return "$header.$payload.$signature"
    }

    @Test
    fun `parseJwt extracts claims`() {
        val jwt = createTestJwt()
        val claims = Keyless.parseJwt(jwt)
        claims.iss shouldBe "https://accounts.google.com"
        claims.aud shouldBe "test-client-id"
        claims.sub shouldBe "user123"
        claims.nonce shouldBe "test-nonce"
    }

    @Test
    fun `parseJwt throws for invalid JWT format`() {
        shouldThrow<CryptoException> {
            Keyless.parseJwt("not-a-jwt")
        }
    }

    @Test
    fun `parseJwt throws for malformed payload`() {
        shouldThrow<CryptoException> {
            Keyless.parseJwt("header.!!!invalid!!!.signature")
        }
    }

    @Test
    fun `PublicKey authKey produces 32 bytes`() {
        val pk = Keyless.PublicKey(
            iss = "https://accounts.google.com",
            aud = "test-client-id",
            uidKey = "sub",
            uidVal = "user123",
            pepper = ByteArray(31) { it.toByte() },
        )
        val authKey = pk.authKey()
        authKey.data.size shouldBe 32
    }

    @Test
    fun `PublicKey authKey matches manual computation`() {
        val pepper = ByteArray(31) { it.toByte() }
        val pk = Keyless.PublicKey(
            iss = "https://accounts.google.com",
            aud = "test-client-id",
            uidKey = "sub",
            uidVal = "user123",
            pepper = pepper,
        )
        val authKey = pk.authKey()

        // Manual computation
        val issHash = Hashing.sha3256("https://accounts.google.com".toByteArray())
        val audHash = Hashing.sha3256("test-client-id".toByteArray())
        val uidHash = Hashing.sha3256("sub:user123".toByteArray())
        val expected = Hashing.sha3256(issHash + audHash + uidHash + pepper + byteArrayOf(0x05))
        authKey.data shouldBe expected
    }

    @Test
    fun `different issuers produce different auth keys`() {
        val pepper = ByteArray(31) { 0 }
        val pk1 = Keyless.PublicKey("issuer1", "aud", "sub", "user", pepper)
        val pk2 = Keyless.PublicKey("issuer2", "aud", "sub", "user", pepper)
        pk1.authKey() shouldNotBe pk2.authKey()
    }

    @Test
    fun `different peppers produce different auth keys`() {
        val pk1 = Keyless.PublicKey("iss", "aud", "sub", "user", ByteArray(31) { 0 })
        val pk2 = Keyless.PublicKey("iss", "aud", "sub", "user", ByteArray(31) { 1 })
        pk1.authKey() shouldNotBe pk2.authKey()
    }

    @Test
    fun `PublicKey BCS serialization`() {
        val pk = Keyless.PublicKey(
            iss = "iss",
            aud = "aud",
            uidKey = "sub",
            uidVal = "user",
            pepper = ByteArray(31) { 0 },
        )
        val serializer = com.aptos.core.bcs.BcsSerializer()
        pk.serialize(serializer)
        val bytes = serializer.toByteArray()
        bytes.isNotEmpty() shouldBe true
    }
}
