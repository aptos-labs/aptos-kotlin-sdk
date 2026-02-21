package com.aptos.core.error

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test

class ErrorTest {
    @Test
    fun `AptosException is RuntimeException`() {
        val ex = AptosException("test")
        ex.shouldBeInstanceOf<RuntimeException>()
        ex.message shouldBe "test"
    }

    @Test
    fun `AptosException with cause`() {
        val cause = IllegalStateException("root cause")
        val ex = AptosException("wrapped", cause)
        ex.cause shouldBe cause
    }

    @Test
    fun `BcsSerializationException extends AptosException`() {
        val ex = BcsSerializationException("bad bcs")
        ex.shouldBeInstanceOf<AptosException>()
        ex.message shouldBe "bad bcs"
    }

    @Test
    fun `BcsDeserializationException extends AptosException`() {
        val ex = BcsDeserializationException("bad deser")
        ex.shouldBeInstanceOf<AptosException>()
    }

    @Test
    fun `AccountAddressParseException extends AptosException`() {
        val ex = AccountAddressParseException("bad address")
        ex.shouldBeInstanceOf<AptosException>()
    }

    @Test
    fun `TypeTagParseException extends AptosException`() {
        val ex = TypeTagParseException("bad type tag")
        ex.shouldBeInstanceOf<AptosException>()
    }

    @Test
    fun `CryptoException extends AptosException`() {
        val ex = CryptoException("crypto error")
        ex.shouldBeInstanceOf<AptosException>()
    }

    @Test
    fun `MnemonicException extends AptosException`() {
        val ex = MnemonicException("bad mnemonic")
        ex.shouldBeInstanceOf<AptosException>()
    }

    @Test
    fun `TransactionBuildException extends AptosException`() {
        val ex = TransactionBuildException("build error")
        ex.shouldBeInstanceOf<AptosException>()
    }

    @Test
    fun `ApiException extends AptosException with status code`() {
        val ex = ApiException("api error", statusCode = 404, errorCode = "not_found")
        ex.shouldBeInstanceOf<AptosException>()
        ex.statusCode shouldBe 404
        ex.errorCode shouldBe "not_found"
    }

    @Test
    fun `ApiException with null optional fields`() {
        val ex = ApiException("simple error")
        ex.statusCode shouldBe null
        ex.errorCode shouldBe null
    }
}

class ErrorCategoryTest {
    @Test
    fun `all categories have unique codes`() {
        val codes = ErrorCategory.entries.map { it.code }
        codes.distinct().size shouldBe codes.size
    }

    @Test
    fun `fromCode finds valid category`() {
        ErrorCategory.fromCode(1) shouldBe ErrorCategory.INVALID_INPUT
        ErrorCategory.fromCode(5) shouldBe ErrorCategory.NOT_FOUND
        ErrorCategory.fromCode(9) shouldBe ErrorCategory.INTERNAL
        ErrorCategory.fromCode(12) shouldBe ErrorCategory.PERMISSION_DENIED
    }

    @Test
    fun `fromCode returns null for unknown code`() {
        ErrorCategory.fromCode(0) shouldBe null
        ErrorCategory.fromCode(99) shouldBe null
    }

    @Test
    fun `categories cover codes 1-12`() {
        for (code in 1..12) {
            ErrorCategory.fromCode(code) shouldNotBe null
        }
    }

    @Test
    fun `category descriptions are non-empty`() {
        ErrorCategory.entries.forEach {
            it.description.isNotEmpty() shouldBe true
        }
    }
}
