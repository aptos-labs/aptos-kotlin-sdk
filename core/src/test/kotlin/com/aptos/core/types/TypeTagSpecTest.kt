package com.aptos.core.types

import com.aptos.core.bcs.BcsDeserializer
import com.aptos.core.bcs.BcsSerializer
import com.aptos.core.error.TypeTagParseException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import java.util.stream.Stream

/**
 * Spec-based tests for TypeTag validated against official aptos-sdk-specs test vectors.
 */
class TypeTagSpecTest {
    // -- BCS variant indices --

    @ParameterizedTest(name = "TypeTag {0} has BCS variant index {1}")
    @MethodSource("variantIndexVectors")
    fun `primitive TypeTag BCS variant indices match spec`(tag: TypeTag, expectedIndex: Int) {
        val serializer = BcsSerializer()
        tag.serialize(serializer)
        val bytes = serializer.toByteArray()
        // First byte is the ULEB128 variant index (single byte for indices < 128)
        bytes[0].toInt() and 0xFF shouldBe expectedIndex
    }

    @Test
    fun `u64 BCS serializes as single byte 0x02`() {
        val s = BcsSerializer()
        TypeTag.U64.serialize(s)
        s.toByteArray() shouldBe byteArrayOf(0x02)
    }

    @Test
    fun `vector of u8 BCS serializes as 0x06 0x01`() {
        val s = BcsSerializer()
        TypeTag.Vector(TypeTag.U8).serialize(s)
        s.toByteArray() shouldBe byteArrayOf(0x06, 0x01)
    }

    // -- BCS deserialization of variant indices --

    @ParameterizedTest(name = "BCS variant index {1} deserializes to {0}")
    @MethodSource("variantIndexVectors")
    fun `BCS deserialization of variant indices matches spec`(expectedTag: TypeTag, variantIndex: Int) {
        // Only test data objects (primitives), not data classes
        if (expectedTag is TypeTag.Vector || expectedTag is TypeTag.Struct) return
        val d = BcsDeserializer(byteArrayOf(variantIndex.toByte()))
        TypeTag.fromBcs(d) shouldBe expectedTag
    }

    // -- String parsing --

    @Test
    fun `parse simple struct type 0x1 aptos_coin AptosCoin`() {
        val tag = TypeTag.fromString("0x1::aptos_coin::AptosCoin")
        tag.shouldBeInstanceOf<TypeTag.Struct>()
        val st = (tag as TypeTag.Struct).structTag
        st.address shouldBe AccountAddress.ONE
        st.module shouldBe "aptos_coin"
        st.name shouldBe "AptosCoin"
        st.typeArgs shouldBe emptyList()
    }

    @Test
    fun `parse CoinStore with type argument`() {
        val tag = TypeTag.fromString("0x1::coin::CoinStore<0x1::aptos_coin::AptosCoin>")
        tag.shouldBeInstanceOf<TypeTag.Struct>()
        val st = (tag as TypeTag.Struct).structTag
        st.module shouldBe "coin"
        st.name shouldBe "CoinStore"
        st.typeArgs.size shouldBe 1
        val inner = st.typeArgs[0] as TypeTag.Struct
        inner.structTag.module shouldBe "aptos_coin"
        inner.structTag.name shouldBe "AptosCoin"
    }

    @Test
    fun `parse struct with multiple type args`() {
        val tag =
            TypeTag.fromString(
                "0x1::pool::LiquidityPool<0x1::coin_a::CoinA, 0x1::coin_b::CoinB>",
            )
        tag.shouldBeInstanceOf<TypeTag.Struct>()
        val st = (tag as TypeTag.Struct).structTag
        st.module shouldBe "pool"
        st.name shouldBe "LiquidityPool"
        st.typeArgs.size shouldBe 2
    }

    @Test
    fun `parse nested generics`() {
        val tag =
            TypeTag.fromString(
                "0x1::option::Option<0x1::coin::Coin<0x1::aptos_coin::AptosCoin>>",
            )
        tag.shouldBeInstanceOf<TypeTag.Struct>()
        val st = (tag as TypeTag.Struct).structTag
        st.module shouldBe "option"
        st.name shouldBe "Option"
        st.typeArgs.size shouldBe 1
        val inner = st.typeArgs[0] as TypeTag.Struct
        inner.structTag.typeArgs.size shouldBe 1
    }

    @Test
    fun `parse vector of u8`() {
        val tag = TypeTag.fromString("vector<u8>")
        tag.shouldBeInstanceOf<TypeTag.Vector>()
        (tag as TypeTag.Vector).elementType shouldBe TypeTag.U8
    }

    @Test
    fun `parse vector of address`() {
        val tag = TypeTag.fromString("vector<address>")
        tag.shouldBeInstanceOf<TypeTag.Vector>()
        (tag as TypeTag.Vector).elementType shouldBe TypeTag.Address
    }

    @Test
    fun `parse nested vector`() {
        val tag = TypeTag.fromString("vector<vector<u8>>")
        tag.shouldBeInstanceOf<TypeTag.Vector>()
        val inner = (tag as TypeTag.Vector).elementType
        inner.shouldBeInstanceOf<TypeTag.Vector>()
        (inner as TypeTag.Vector).elementType shouldBe TypeTag.U8
    }

    @Test
    fun `parse vector of struct`() {
        val tag = TypeTag.fromString("vector<0x1::aptos_coin::AptosCoin>")
        tag.shouldBeInstanceOf<TypeTag.Vector>()
        (tag as TypeTag.Vector).elementType.shouldBeInstanceOf<TypeTag.Struct>()
    }

    // -- toString formatting --

    @Test
    fun `struct toString uses short address form`() {
        val tag = TypeTag.fromString("0x1::coin::Coin")
        tag.toString() shouldBe "0x1::coin::Coin"
    }

    @Test
    fun `struct with type args toString includes generics`() {
        val tag = TypeTag.fromString("0x1::coin::CoinStore<0x1::aptos_coin::AptosCoin>")
        tag.toString() shouldBe "0x1::coin::CoinStore<0x1::aptos_coin::AptosCoin>"
    }

    // -- Full address struct parsing normalizes to short form --

    @Test
    fun `full address in struct is normalized to short form in toString`() {
        val tag =
            TypeTag.fromString(
                "0x0000000000000000000000000000000000000000000000000000000000000001::coin::Coin",
            )
        tag.toString() shouldBe "0x1::coin::Coin"
    }

    // -- Invalid type tag strings from spec --

    @ParameterizedTest(name = "reject invalid type tag: \"{0}\"")
    @ValueSource(
        strings = [
            "",
            "int",
            "vector<u8",
            "vector<>",
            "0x1::module",
            "::",
        ],
    )
    fun `invalid type tag strings are rejected`(input: String) {
        shouldThrow<TypeTagParseException> {
            TypeTag.fromString(input)
        }
    }

    @Test
    fun `unclosed generic in struct is rejected`() {
        shouldThrow<TypeTagParseException> {
            TypeTag.fromString("0x1::coin::Coin<0x1::aptos_coin::AptosCoin")
        }
    }

    // -- BCS roundtrip for all primitive types --

    @Test
    fun `BCS roundtrip for all 11 type tags`() {
        val primitives =
            listOf(
                TypeTag.Bool,
                TypeTag.U8,
                TypeTag.U64,
                TypeTag.U128,
                TypeTag.Address,
                TypeTag.Signer,
                TypeTag.U16,
                TypeTag.U32,
                TypeTag.U256,
            )
        for (tag in primitives) {
            val s = BcsSerializer()
            tag.serialize(s)
            val d = BcsDeserializer(s.toByteArray())
            TypeTag.fromBcs(d) shouldBe tag
        }

        // Vector
        val vectorTag = TypeTag.Vector(TypeTag.U8)
        val sv = BcsSerializer()
        vectorTag.serialize(sv)
        val dv = BcsDeserializer(sv.toByteArray())
        TypeTag.fromBcs(dv) shouldBe vectorTag

        // Struct
        val structTag = TypeTag.Struct(StructTag.aptosCoin())
        val ss = BcsSerializer()
        structTag.serialize(ss)
        val ds = BcsDeserializer(ss.toByteArray())
        TypeTag.fromBcs(ds) shouldBe structTag
    }

    companion object {
        @JvmStatic
        fun variantIndexVectors(): Stream<Arguments> = Stream.of(
            Arguments.of(TypeTag.Bool, 0),
            Arguments.of(TypeTag.U8, 1),
            Arguments.of(TypeTag.U64, 2),
            Arguments.of(TypeTag.U128, 3),
            Arguments.of(TypeTag.Address, 4),
            Arguments.of(TypeTag.Signer, 5),
            Arguments.of(TypeTag.Vector(TypeTag.U8), 6),
            Arguments.of(TypeTag.Struct(StructTag.aptosCoin()), 7),
            Arguments.of(TypeTag.U16, 8),
            Arguments.of(TypeTag.U32, 9),
            Arguments.of(TypeTag.U256, 10),
        )
    }
}
