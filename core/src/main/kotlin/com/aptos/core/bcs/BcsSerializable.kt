package com.aptos.core.bcs

/**
 * Interface for types that can be serialized into BCS (Binary Canonical Serialization) format.
 */
interface BcsSerializable {
    fun serialize(serializer: BcsSerializer)
}
