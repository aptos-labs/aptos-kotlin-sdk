package com.aptos.core.account

import com.aptos.core.error.MnemonicException

/**
 * BIP-32/BIP-44 derivation path.
 */
data class DerivationPath(val components: List<Component>) {

    data class Component(val index: UInt, val hardened: Boolean) {
        fun toInt(): Int = if (hardened) {
            (index.toInt() or HARDENED_BIT)
        } else {
            index.toInt()
        }

        override fun toString(): String = if (hardened) "${index}'" else "$index"
    }

    override fun toString(): String = "m/${components.joinToString("/")}"

    companion object {
        private const val HARDENED_BIT = 0x80000000.toInt()

        /** Default Aptos derivation path: m/44'/637'/0'/0'/0' */
        @JvmStatic
        val DEFAULT_APTOS = parse("m/44'/637'/0'/0'/0'")

        @JvmStatic
        fun parse(path: String): DerivationPath {
            val trimmed = path.trim()
            if (!trimmed.startsWith("m/")) {
                throw MnemonicException("Derivation path must start with 'm/': $path")
            }
            val parts = trimmed.substring(2).split("/")
            if (parts.isEmpty()) {
                throw MnemonicException("Derivation path has no components: $path")
            }
            val components = parts.map { part ->
                val hardened = part.endsWith("'") || part.endsWith("H")
                val indexStr = if (hardened) part.dropLast(1) else part
                val index = indexStr.toUIntOrNull()
                    ?: throw MnemonicException("Invalid derivation path component: $part")
                Component(index, hardened)
            }
            return DerivationPath(components)
        }
    }
}
