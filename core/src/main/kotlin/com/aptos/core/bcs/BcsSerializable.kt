package com.aptos.core.bcs

/**
 * Interface for types that can be serialized into BCS (Binary Canonical Serialization) format.
 *
 * All core Aptos types (addresses, transactions, type tags, etc.) implement this interface.
 * Custom types can implement it to participate in BCS encoding:
 *
 * ```kotlin
 * data class MyStruct(val value: ULong) : BcsSerializable {
 *     override fun serialize(serializer: BcsSerializer) {
 *         serializer.serializeU64(value)
 *     }
 * }
 * ```
 */
interface BcsSerializable {
    /**
     * Writes this value into the given [serializer] in BCS format.
     *
     * @param serializer the target serializer to write bytes into
     */
    fun serialize(serializer: BcsSerializer)
}
