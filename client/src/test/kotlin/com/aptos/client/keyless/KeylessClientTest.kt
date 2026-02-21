package com.aptos.client.keyless

import com.aptos.core.error.ApiException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class KeylessClientTest {

    @Test
    fun `PepperClient getPepper success`() = runTest {
        val engine = MockEngine {
            respond(
                content = ByteReadChannel(
                    """{"pepper":"0102030405060708091011121314151617181920212223242526272829303132"}""",
                ),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val client = PepperClient("https://pepper.test.com", engine)
        val pepper = client.getPepper("fake.jwt.token", "0xepk123", "sub")
        pepper.isNotEmpty() shouldBe true
        client.close()
    }

    @Test
    fun `PepperClient getPepper error throws ApiException`() = runTest {
        val engine = MockEngine {
            respond(
                content = ByteReadChannel("""{"error":"invalid jwt"}"""),
                status = HttpStatusCode.BadRequest,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val client = PepperClient("https://pepper.test.com", engine)
        shouldThrow<ApiException> {
            client.getPepper("bad.jwt.token", "0xepk123", "sub")
        }
        client.close()
    }

    @Test
    fun `ProverClient getProof success`() = runTest {
        val engine = MockEngine {
            respond(
                content = ByteReadChannel("""{"proof":"aabbccdd"}"""),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val client = ProverClient("https://prover.test.com", engine)
        val proof = client.getProof(
            jwt = "fake.jwt.token",
            ephemeralPublicKey = "0xepk123",
            expirationDateSecs = System.currentTimeMillis() / 1000 + 3600,
            pepper = "0xpepper",
            uidKey = "sub",
        )
        proof.isNotEmpty() shouldBe true
        client.close()
    }

    @Test
    fun `ProverClient getProof error throws ApiException`() = runTest {
        val engine = MockEngine {
            respond(
                content = ByteReadChannel("""{"error":"proving failed"}"""),
                status = HttpStatusCode.InternalServerError,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val client = ProverClient("https://prover.test.com", engine)
        shouldThrow<ApiException> {
            client.getProof(
                jwt = "fake.jwt.token",
                ephemeralPublicKey = "0xepk123",
                expirationDateSecs = System.currentTimeMillis() / 1000 + 3600,
                pepper = "0xpepper",
                uidKey = "sub",
            )
        }
        client.close()
    }
}
