package com.aptos.core.types

import com.aptos.core.bcs.BcsDeserializer
import com.aptos.core.bcs.BcsSerializer
import com.aptos.core.error.TypeTagParseException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class StructTagTest {

    @Test
    fun `parse simple struct tag`() {
        val tag = StructTag.fromString("0x1::aptos_coin::AptosCoin")
        tag.address shouldBe AccountAddress.ONE
        tag.module shouldBe "aptos_coin"
        tag.name shouldBe "AptosCoin"
        tag.typeArgs shouldBe emptyList()
    }

    @Test
    fun `parse struct tag with type arguments`() {
        val tag = StructTag.fromString("0x1::coin::CoinStore<0x1::aptos_coin::AptosCoin>")
        tag.module shouldBe "coin"
        tag.name shouldBe "CoinStore"
        tag.typeArgs.size shouldBe 1
    }

    @Test
    fun `parse struct tag with multiple type arguments`() {
        val tag = StructTag.fromString("0x1::pair::Pair<u64, bool>")
        tag.typeArgs.size shouldBe 2
        tag.typeArgs[0] shouldBe TypeTag.U64
        tag.typeArgs[1] shouldBe TypeTag.Bool
    }

    @Test
    fun `parse nested generic struct tag`() {
        val tag = StructTag.fromString("0x1::coin::CoinStore<0x1::coin::Coin<0x1::aptos_coin::AptosCoin>>")
        tag.typeArgs.size shouldBe 1
        val inner = tag.typeArgs[0] as TypeTag.Struct
        inner.structTag.name shouldBe "Coin"
        inner.structTag.typeArgs.size shouldBe 1
    }

    @Test
    fun `parse struct tag with long address`() {
        val addr = "0x" + "0".repeat(62) + "ab"
        val tag = StructTag.fromString("$addr::module::Name")
        tag.address shouldBe AccountAddress.fromHexRelaxed("0xab")
    }

    @Test
    fun `aptosCoin factory`() {
        val tag = StructTag.aptosCoin()
        tag.address shouldBe AccountAddress.ONE
        tag.module shouldBe "aptos_coin"
        tag.name shouldBe "AptosCoin"
        tag.typeArgs shouldBe emptyList()
    }

    @Test
    fun `toString simple`() {
        val tag = StructTag(AccountAddress.ONE, "module", "Name")
        tag.toString() shouldBe "0x1::module::Name"
    }

    @Test
    fun `toString with type args`() {
        val tag = StructTag(AccountAddress.ONE, "coin", "CoinStore", listOf(TypeTag.U64))
        tag.toString() shouldBe "0x1::coin::CoinStore<u64>"
    }

    @Test
    fun `BCS roundtrip`() {
        val original = StructTag.aptosCoin()
        val s = BcsSerializer()
        original.serialize(s)
        val d = BcsDeserializer(s.toByteArray())
        StructTag.fromBcs(d) shouldBe original
    }

    @Test
    fun `BCS roundtrip with type args`() {
        val original = StructTag(AccountAddress.ONE, "coin", "CoinStore", listOf(TypeTag.U64, TypeTag.Bool))
        val s = BcsSerializer()
        original.serialize(s)
        val d = BcsDeserializer(s.toByteArray())
        StructTag.fromBcs(d) shouldBe original
    }

    @Test
    fun `reject invalid format missing separator`() {
        shouldThrow<TypeTagParseException> {
            StructTag.fromString("0x1:aptos_coin::AptosCoin")
        }
    }
}

class MoveModuleIdTest {

    @Test
    fun `parse module id`() {
        val id = MoveModuleId.fromString("0x1::aptos_account")
        id.address shouldBe AccountAddress.ONE
        id.name shouldBe "aptos_account"
    }

    @Test
    fun `parse module id with long address`() {
        val id = MoveModuleId.fromString("0xdead::my_module")
        id.address shouldBe AccountAddress.fromHexRelaxed("0xdead")
        id.name shouldBe "my_module"
    }

    @Test
    fun `toString formats correctly`() {
        val id = MoveModuleId(AccountAddress.ONE, "coin")
        id.toString() shouldBe "0x1::coin"
    }

    @Test
    fun `reject invalid format`() {
        shouldThrow<TypeTagParseException> {
            MoveModuleId.fromString("not_valid")
        }
    }

    @Test
    fun `reject too many separators`() {
        shouldThrow<TypeTagParseException> {
            MoveModuleId.fromString("0x1::a::b")
        }
    }

    @Test
    fun `BCS roundtrip`() {
        val original = MoveModuleId(AccountAddress.ONE, "aptos_account")
        val s = BcsSerializer()
        original.serialize(s)
        val d = BcsDeserializer(s.toByteArray())
        MoveModuleId.fromBcs(d) shouldBe original
    }
}
