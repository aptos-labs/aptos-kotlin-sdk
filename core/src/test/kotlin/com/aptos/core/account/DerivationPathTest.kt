package com.aptos.core.account

import com.aptos.core.error.MnemonicException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class DerivationPathTest {
    @Test
    fun `DEFAULT_APTOS path`() {
        val path = DerivationPath.DEFAULT_APTOS
        path.toString() shouldBe "m/44'/637'/0'/0'/0'"
        path.components.size shouldBe 5
    }

    @Test
    fun `all components are hardened in default path`() {
        val path = DerivationPath.DEFAULT_APTOS
        path.components.forEach { it.hardened shouldBe true }
    }

    @Test
    fun `parse hardened with apostrophe`() {
        val path = DerivationPath.parse("m/44'/637'/0'/0'/0'")
        path.components[0].index shouldBe 44u
        path.components[0].hardened shouldBe true
        path.components[1].index shouldBe 637u
        path.components[1].hardened shouldBe true
    }

    @Test
    fun `parse hardened with H`() {
        val path = DerivationPath.parse("m/44H/637H/0H")
        path.components.size shouldBe 3
        path.components[0].index shouldBe 44u
        path.components[0].hardened shouldBe true
    }

    @Test
    fun `parse non-hardened components`() {
        val path = DerivationPath.parse("m/44'/637'/0'/0/0")
        path.components[3].hardened shouldBe false
        path.components[4].hardened shouldBe false
    }

    @Test
    fun `parse different account index`() {
        val path = DerivationPath.parse("m/44'/637'/0'/0'/1'")
        path.components[4].index shouldBe 1u
    }

    @Test
    fun `reject path without m prefix`() {
        shouldThrow<MnemonicException> {
            DerivationPath.parse("44'/637'/0'/0'/0'")
        }
    }

    @Test
    fun `reject path with just m`() {
        shouldThrow<MnemonicException> {
            DerivationPath.parse("m")
        }
    }

    @Test
    fun `reject invalid component`() {
        shouldThrow<MnemonicException> {
            DerivationPath.parse("m/abc")
        }
    }

    @Test
    fun `component toInt includes hardened bit`() {
        val component = DerivationPath.Component(44u, hardened = true)
        val expected = (44 or 0x80000000.toInt())
        component.toInt() shouldBe expected
    }

    @Test
    fun `component toInt without hardened bit`() {
        val component = DerivationPath.Component(44u, hardened = false)
        component.toInt() shouldBe 44
    }

    @Test
    fun `component toString hardened`() {
        DerivationPath.Component(44u, true).toString() shouldBe "44'"
    }

    @Test
    fun `component toString non-hardened`() {
        DerivationPath.Component(0u, false).toString() shouldBe "0"
    }

    @Test
    fun `toString roundtrip`() {
        val path = DerivationPath.parse("m/44'/637'/0'/0'/0'")
        DerivationPath.parse(path.toString()) shouldBe path
    }
}
