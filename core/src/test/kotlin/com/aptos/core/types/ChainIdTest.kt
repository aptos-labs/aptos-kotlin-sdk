package com.aptos.core.types

import com.aptos.core.bcs.BcsDeserializer
import com.aptos.core.bcs.BcsSerializer
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ChainIdTest {

    @Test
    fun `MAINNET constant`() {
        ChainId.MAINNET.value shouldBe 1u.toUByte()
    }

    @Test
    fun `TESTNET constant`() {
        ChainId.TESTNET.value shouldBe 2u.toUByte()
    }

    @Test
    fun `LOCAL constant`() {
        ChainId.LOCAL.value shouldBe 4u.toUByte()
    }

    @Test
    fun `BCS serialization is single byte`() {
        val s = BcsSerializer()
        ChainId.TESTNET.serialize(s)
        s.toByteArray() shouldBe byteArrayOf(2)
    }

    @Test
    fun `BCS roundtrip`() {
        val s = BcsSerializer()
        ChainId.MAINNET.serialize(s)
        val d = BcsDeserializer(s.toByteArray())
        ChainId(d.deserializeU8()) shouldBe ChainId.MAINNET
    }

    @Test
    fun `toString returns numeric value`() {
        ChainId.MAINNET.toString() shouldBe "1"
        ChainId.TESTNET.toString() shouldBe "2"
    }

    @Test
    fun `custom chain id`() {
        val custom = ChainId(42u)
        custom.value shouldBe 42u.toUByte()
    }

    @Test
    fun `equality`() {
        ChainId(1u) shouldBe ChainId.MAINNET
        ChainId(2u) shouldBe ChainId.TESTNET
    }
}
