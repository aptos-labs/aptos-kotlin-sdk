package com.aptos.core.types

import com.aptos.core.bcs.BcsDeserializer
import com.aptos.core.bcs.BcsSerializable
import com.aptos.core.bcs.BcsSerializer
import com.aptos.core.error.TypeTagParseException

/**
 * Represents a fully qualified Move struct type: `address::module::Name<TypeArgs>`.
 *
 * Examples: `0x1::aptos_coin::AptosCoin`, `0x1::coin::CoinStore<0x1::aptos_coin::AptosCoin>`
 *
 * @property address the account address where the module is published
 * @property module the Move module name
 * @property name the struct name within the module
 * @property typeArgs generic type arguments (empty if the struct is not generic)
 */
data class StructTag(
    val address: AccountAddress,
    val module: String,
    val name: String,
    val typeArgs: List<TypeTag> = emptyList(),
) : BcsSerializable {
    override fun serialize(serializer: BcsSerializer) {
        address.serialize(serializer)
        serializer.serializeString(module)
        serializer.serializeString(name)
        serializer.serializeSequenceLength(typeArgs.size)
        typeArgs.forEach { it.serialize(serializer) }
    }

    override fun toString(): String {
        val base = "${address.toShortString()}::$module::$name"
        return if (typeArgs.isEmpty()) {
            base
        } else {
            "$base<${typeArgs.joinToString(", ")}>"
        }
    }

    companion object {
        /**
         * Parses a struct tag from a string like `"0x1::module::Name<T1, T2>"`.
         *
         * @throws TypeTagParseException if the format is invalid
         */
        @JvmStatic
        fun fromString(input: String): StructTag {
            val parser = TypeTagParser(input)
            return parser.parseStructTag()
        }

        @JvmStatic
        fun fromBcs(deserializer: BcsDeserializer): StructTag {
            val address = AccountAddress.fromBcs(deserializer)
            val module = deserializer.deserializeString()
            val name = deserializer.deserializeString()
            val typeArgCount = deserializer.deserializeSequenceLength()
            val typeArgs = (0 until typeArgCount).map { TypeTag.fromBcs(deserializer) }
            return StructTag(address, module, name, typeArgs)
        }

        /** Returns the struct tag for `0x1::aptos_coin::AptosCoin`. */
        @JvmStatic
        fun aptosCoin(): StructTag = StructTag(
            address = AccountAddress.ONE,
            module = "aptos_coin",
            name = "AptosCoin",
        )
    }
}

/**
 * Represents a Move module identifier: `address::module_name`.
 *
 * @property address the account address where the module is published
 * @property name the module name
 */
data class MoveModuleId(val address: AccountAddress, val name: String) : BcsSerializable {
    override fun serialize(serializer: BcsSerializer) {
        address.serialize(serializer)
        serializer.serializeString(name)
    }

    override fun toString(): String = "${address.toShortString()}::$name"

    companion object {
        /**
         * Parses a module ID from a string like `"0x1::aptos_account"`.
         *
         * @throws TypeTagParseException if the format is invalid
         */
        @JvmStatic
        fun fromString(input: String): MoveModuleId {
            val parts = input.split("::")
            if (parts.size != 2) {
                throw TypeTagParseException("Invalid module ID format: $input (expected 'address::module')")
            }
            val address =
                try {
                    AccountAddress.fromHexRelaxed(parts[0])
                } catch (e: Exception) {
                    throw TypeTagParseException("Invalid address in module ID: ${parts[0]}", e)
                }
            return MoveModuleId(address, parts[1])
        }

        @JvmStatic
        fun fromBcs(deserializer: BcsDeserializer): MoveModuleId {
            val address = AccountAddress.fromBcs(deserializer)
            val name = deserializer.deserializeString()
            return MoveModuleId(address, name)
        }
    }
}
