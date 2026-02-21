package com.aptos.core.types

import com.aptos.core.bcs.BcsSerializable
import com.aptos.core.bcs.BcsSerializer

/**
 * Represents an Aptos chain identifier as a single byte.
 */
@JvmInline
value class ChainId(val value: UByte) : BcsSerializable {
    override fun serialize(serializer: BcsSerializer) {
        serializer.serializeU8(value)
    }

    override fun toString(): String = value.toString()

    companion object {
        @JvmStatic val MAINNET = ChainId(1u)

        @JvmStatic val TESTNET = ChainId(2u)

        @JvmStatic val LOCAL = ChainId(4u)
    }
}
