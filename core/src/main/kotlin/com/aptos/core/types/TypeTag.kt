package com.aptos.core.types

import com.aptos.core.bcs.BcsDeserializer
import com.aptos.core.bcs.BcsSerializable
import com.aptos.core.bcs.BcsSerializer
import com.aptos.core.error.TypeTagParseException

/**
 * Sealed class representing Move type tags used in the Aptos type system.
 *
 * Type tags identify the types of values on-chain and are used in entry function
 * type arguments and resource type specifiers. Parse from strings with [fromString]
 * or deserialize from BCS with [fromBcs].
 *
 * BCS variant indices: Bool=0, U8=1, U64=2, U128=3, Address=4,
 * Signer=5, Vector=6, Struct=7, U16=8, U32=9, U256=10
 */
sealed class TypeTag : BcsSerializable {
    data object Bool : TypeTag() {
        override fun serialize(serializer: BcsSerializer) = serializer.serializeVariantIndex(0u)

        override fun toString() = "bool"
    }

    data object U8 : TypeTag() {
        override fun serialize(serializer: BcsSerializer) = serializer.serializeVariantIndex(1u)

        override fun toString() = "u8"
    }

    data object U64 : TypeTag() {
        override fun serialize(serializer: BcsSerializer) = serializer.serializeVariantIndex(2u)

        override fun toString() = "u64"
    }

    data object U128 : TypeTag() {
        override fun serialize(serializer: BcsSerializer) = serializer.serializeVariantIndex(3u)

        override fun toString() = "u128"
    }

    data object Address : TypeTag() {
        override fun serialize(serializer: BcsSerializer) = serializer.serializeVariantIndex(4u)

        override fun toString() = "address"
    }

    data object Signer : TypeTag() {
        override fun serialize(serializer: BcsSerializer) = serializer.serializeVariantIndex(5u)

        override fun toString() = "signer"
    }

    data class Vector(val elementType: TypeTag) : TypeTag() {
        override fun serialize(serializer: BcsSerializer) {
            serializer.serializeVariantIndex(6u)
            elementType.serialize(serializer)
        }

        override fun toString() = "vector<$elementType>"
    }

    data class Struct(val structTag: StructTag) : TypeTag() {
        override fun serialize(serializer: BcsSerializer) {
            serializer.serializeVariantIndex(7u)
            structTag.serialize(serializer)
        }

        override fun toString() = structTag.toString()
    }

    data object U16 : TypeTag() {
        override fun serialize(serializer: BcsSerializer) = serializer.serializeVariantIndex(8u)

        override fun toString() = "u16"
    }

    data object U32 : TypeTag() {
        override fun serialize(serializer: BcsSerializer) = serializer.serializeVariantIndex(9u)

        override fun toString() = "u32"
    }

    data object U256 : TypeTag() {
        override fun serialize(serializer: BcsSerializer) = serializer.serializeVariantIndex(10u)

        override fun toString() = "u256"
    }

    companion object {
        /**
         * Parses a type tag from its string representation.
         *
         * Supported formats: `"bool"`, `"u8"`, `"u64"`, `"vector<u8>"`,
         * `"0x1::coin::CoinStore<0x1::aptos_coin::AptosCoin>"`, etc.
         *
         * @throws TypeTagParseException if the input cannot be parsed
         */
        @JvmStatic
        fun fromString(input: String): TypeTag {
            val parser = TypeTagParser(input)
            return parser.parseTypeTag()
        }

        /** Deserializes a [TypeTag] from BCS (reads variant index, then variant-specific data). */
        @JvmStatic
        fun fromBcs(deserializer: BcsDeserializer): TypeTag =
            when (val index = deserializer.deserializeVariantIndex()) {
                0u -> Bool
                1u -> U8
                2u -> U64
                3u -> U128
                4u -> Address
                5u -> Signer
                6u -> Vector(fromBcs(deserializer))
                7u -> Struct(StructTag.fromBcs(deserializer))
                8u -> U16
                9u -> U32
                10u -> U256
                else -> throw TypeTagParseException("Unknown TypeTag variant index: $index")
            }
    }
}

/**
 * Recursive descent parser for type tag strings like "vector<0x1::coin::CoinStore<0x1::aptos_coin::AptosCoin>>".
 */
internal class TypeTagParser(private val input: String) {
    private var pos = 0
    private var depth = 0
    private val maxDepth = 32

    fun parseTypeTag(): TypeTag {
        skipWhitespace()
        val tag = parseTypeTagInner()
        skipWhitespace()
        if (pos != input.length) {
            throw TypeTagParseException(
                "Unexpected characters after type tag at position $pos: '${input.substring(pos)}'",
            )
        }
        return tag
    }

    fun parseStructTag(): StructTag {
        skipWhitespace()
        val tag = parseStructTagInner()
        skipWhitespace()
        if (pos != input.length) {
            throw TypeTagParseException("Unexpected characters after struct tag at position $pos")
        }
        return tag
    }

    private fun parseTypeTagInner(): TypeTag {
        if (depth++ > maxDepth) {
            throw TypeTagParseException("Type tag nesting exceeds maximum depth of $maxDepth")
        }
        try {
            skipWhitespace()
            return when {
                tryConsume("bool") -> TypeTag.Bool
                tryConsume("u8") && !peekIsDigit() -> TypeTag.U8
                tryConsume("u16") -> TypeTag.U16
                tryConsume("u32") -> TypeTag.U32
                tryConsume("u64") -> TypeTag.U64
                tryConsume("u128") -> TypeTag.U128
                tryConsume("u256") -> TypeTag.U256
                tryConsume("address") -> TypeTag.Address
                tryConsume("signer") -> TypeTag.Signer
                tryConsume("vector") -> {
                    skipWhitespace()
                    expect('<')
                    skipWhitespace()
                    val inner = parseTypeTagInner()
                    skipWhitespace()
                    expect('>')
                    TypeTag.Vector(inner)
                }
                else -> TypeTag.Struct(parseStructTagInner())
            }
        } finally {
            depth--
        }
    }

    private fun parseStructTagInner(): StructTag {
        val addressStr = parseAddress()
        expect(':')
        expect(':')
        val module = parseIdentifier()
        expect(':')
        expect(':')
        val name = parseIdentifier()

        val typeArgs =
            if (pos < input.length && input[pos] == '<') {
                pos++ // consume '<'
                val args = mutableListOf<TypeTag>()
                skipWhitespace()
                if (pos < input.length && input[pos] != '>') {
                    args.add(parseTypeTagInner())
                    skipWhitespace()
                    while (pos < input.length && input[pos] == ',') {
                        pos++ // consume ','
                        skipWhitespace()
                        args.add(parseTypeTagInner())
                        skipWhitespace()
                    }
                }
                skipWhitespace()
                expect('>')
                args
            } else {
                emptyList()
            }

        val address =
            try {
                AccountAddress.fromHexRelaxed(addressStr)
            } catch (e: Exception) {
                throw TypeTagParseException("Invalid address in struct tag: $addressStr", e)
            }

        return StructTag(address, module, name, typeArgs)
    }

    private fun parseAddress(): String {
        val start = pos
        if (pos + 1 < input.length && input[pos] == '0' && (input[pos + 1] == 'x' || input[pos + 1] == 'X')) {
            pos += 2
        }
        while (pos < input.length && isHexDigit(input[pos])) {
            pos++
        }
        if (pos == start) {
            throw TypeTagParseException("Expected address at position $pos")
        }
        return input.substring(start, pos)
    }

    private fun parseIdentifier(): String {
        val start = pos
        if (pos >= input.length || !(input[pos].isLetter() || input[pos] == '_')) {
            throw TypeTagParseException("Expected identifier at position $pos")
        }
        while (pos < input.length && (input[pos].isLetterOrDigit() || input[pos] == '_')) {
            pos++
        }
        return input.substring(start, pos)
    }

    private fun tryConsume(s: String): Boolean {
        if (input.startsWith(s, pos)) {
            val endPos = pos + s.length
            // Make sure the keyword isn't a prefix of an identifier
            if (endPos < input.length && (input[endPos].isLetterOrDigit() || input[endPos] == '_')) {
                if (s == "u8") {
                    // u8 might be prefix of u8 in an identifier, but not u16/u32/etc (handled separately)
                    return false
                }
                return false
            }
            pos += s.length
            return true
        }
        return false
    }

    private fun peekIsDigit(): Boolean {
        // Called after tryConsume("u8") restores position, check if next char is digit
        return pos < input.length && input[pos].isDigit()
    }

    private fun expect(c: Char) {
        if (pos >= input.length || input[pos] != c) {
            val actual = if (pos < input.length) "'${input[pos]}'" else "end of input"
            throw TypeTagParseException("Expected '$c' at position $pos, got $actual")
        }
        pos++
    }

    private fun skipWhitespace() {
        while (pos < input.length && input[pos].isWhitespace()) pos++
    }

    private fun isHexDigit(c: Char): Boolean = c in '0'..'9' || c in 'a'..'f' || c in 'A'..'F'
}
