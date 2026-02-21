package com.aptos.core.types

import com.aptos.core.bcs.BcsDeserializer
import com.aptos.core.bcs.BcsSerializer
import com.aptos.core.error.TypeTagParseException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class TypeTagTest {

    @ParameterizedTest
    @CsvSource(
        "bool, bool",
        "u8, u8",
        "u16, u16",
        "u32, u32",
        "u64, u64",
        "u128, u128",
        "u256, u256",
        "address, address",
        "signer, signer",
    )
    fun `parse primitive type tags`(input: String, expected: String) {
        val tag = TypeTag.fromString(input)
        tag.toString() shouldBe expected
    }

    @Test
    fun `parse vector type`() {
        val tag = TypeTag.fromString("vector<u8>")
        tag.shouldBeInstanceOf<TypeTag.Vector>()
        tag.elementType shouldBe TypeTag.U8
    }

    @Test
    fun `parse nested vector type`() {
        val tag = TypeTag.fromString("vector<vector<u64>>")
        tag.shouldBeInstanceOf<TypeTag.Vector>()
        val inner = (tag as TypeTag.Vector).elementType
        inner.shouldBeInstanceOf<TypeTag.Vector>()
        (inner as TypeTag.Vector).elementType shouldBe TypeTag.U64
    }

    @Test
    fun `parse struct type tag`() {
        val tag = TypeTag.fromString("0x1::aptos_coin::AptosCoin")
        tag.shouldBeInstanceOf<TypeTag.Struct>()
        val st = (tag as TypeTag.Struct).structTag
        st.address shouldBe AccountAddress.ONE
        st.module shouldBe "aptos_coin"
        st.name shouldBe "AptosCoin"
        st.typeArgs shouldBe emptyList()
    }

    @Test
    fun `parse struct with type args`() {
        val tag = TypeTag.fromString("0x1::coin::CoinStore<0x1::aptos_coin::AptosCoin>")
        tag.shouldBeInstanceOf<TypeTag.Struct>()
        val st = (tag as TypeTag.Struct).structTag
        st.module shouldBe "coin"
        st.name shouldBe "CoinStore"
        st.typeArgs.size shouldBe 1
        st.typeArgs[0].shouldBeInstanceOf<TypeTag.Struct>()
    }

    @Test
    fun `parse struct with multiple type args`() {
        val tag = TypeTag.fromString("0x1::pair::Pair<u64, bool>")
        tag.shouldBeInstanceOf<TypeTag.Struct>()
        val st = (tag as TypeTag.Struct).structTag
        st.typeArgs.size shouldBe 2
        st.typeArgs[0] shouldBe TypeTag.U64
        st.typeArgs[1] shouldBe TypeTag.Bool
    }

    @Test
    fun `parse vector of struct`() {
        val tag = TypeTag.fromString("vector<0x1::aptos_coin::AptosCoin>")
        tag.shouldBeInstanceOf<TypeTag.Vector>()
        (tag as TypeTag.Vector).elementType.shouldBeInstanceOf<TypeTag.Struct>()
    }

    @Test
    fun `reject invalid type tag`() {
        shouldThrow<TypeTagParseException> {
            TypeTag.fromString("invalid_type")
        }
    }

    @Test
    fun `reject empty input`() {
        shouldThrow<TypeTagParseException> {
            TypeTag.fromString("")
        }
    }

    @Test
    fun `toString roundtrip for primitives`() {
        val primitives = listOf(
            TypeTag.Bool, TypeTag.U8, TypeTag.U16, TypeTag.U32, TypeTag.U64,
            TypeTag.U128, TypeTag.U256, TypeTag.Address, TypeTag.Signer,
        )
        for (p in primitives) {
            TypeTag.fromString(p.toString()).toString() shouldBe p.toString()
        }
    }

    @Test
    fun `BCS roundtrip for primitive types`() {
        val types = listOf(
            TypeTag.Bool, TypeTag.U8, TypeTag.U16, TypeTag.U32, TypeTag.U64,
            TypeTag.U128, TypeTag.U256, TypeTag.Address, TypeTag.Signer,
        )
        for (tag in types) {
            val s = BcsSerializer()
            tag.serialize(s)
            val d = BcsDeserializer(s.toByteArray())
            TypeTag.fromBcs(d) shouldBe tag
        }
    }

    @Test
    fun `BCS roundtrip for vector type`() {
        val tag = TypeTag.Vector(TypeTag.U8)
        val s = BcsSerializer()
        tag.serialize(s)
        val d = BcsDeserializer(s.toByteArray())
        TypeTag.fromBcs(d) shouldBe tag
    }

    @Test
    fun `BCS roundtrip for struct type`() {
        val tag = TypeTag.Struct(StructTag.aptosCoin())
        val s = BcsSerializer()
        tag.serialize(s)
        val d = BcsDeserializer(s.toByteArray())
        TypeTag.fromBcs(d) shouldBe tag
    }
}
