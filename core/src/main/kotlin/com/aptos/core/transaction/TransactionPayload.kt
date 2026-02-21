package com.aptos.core.transaction

import com.aptos.core.bcs.BcsSerializable
import com.aptos.core.bcs.BcsSerializer
import com.aptos.core.types.AccountAddress
import com.aptos.core.types.MoveModuleId
import com.aptos.core.types.TypeTag

/**
 * Sealed class representing transaction payload variants.
 * BCS variant indices: Script=0, ModuleBundle=1, EntryFunction=2, Multisig=3
 */
sealed class TransactionPayload : BcsSerializable {

    data class Script(
        val code: ByteArray,
        val typeArgs: List<TypeTag>,
        val args: List<ByteArray>,
    ) : TransactionPayload() {
        override fun serialize(serializer: BcsSerializer) {
            serializer.serializeVariantIndex(0u)
            serializer.serializeBytes(code)
            serializer.serializeSequenceLength(typeArgs.size)
            typeArgs.forEach { it.serialize(serializer) }
            serializer.serializeSequenceLength(args.size)
            args.forEach { serializer.serializeBytes(it) }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Script) return false
            return code.contentEquals(other.code) && typeArgs == other.typeArgs &&
                args.size == other.args.size &&
                args.zip(other.args).all { (a, b) -> a.contentEquals(b) }
        }

        override fun hashCode(): Int = code.contentHashCode()
    }

    data class EntryFunction(
        val moduleId: MoveModuleId,
        val functionName: String,
        val typeArgs: List<TypeTag>,
        val args: List<ByteArray>,
    ) : TransactionPayload() {
        override fun serialize(serializer: BcsSerializer) {
            serializer.serializeVariantIndex(2u)
            moduleId.serialize(serializer)
            serializer.serializeString(functionName)
            serializer.serializeSequenceLength(typeArgs.size)
            typeArgs.forEach { it.serialize(serializer) }
            serializer.serializeSequenceLength(args.size)
            args.forEach { serializer.serializeBytes(it) }
        }

        companion object {
            @JvmStatic
            fun aptTransfer(to: AccountAddress, amount: ULong): EntryFunction {
                val amountSerializer = BcsSerializer()
                amountSerializer.serializeU64(amount)

                val toSerializer = BcsSerializer()
                to.serialize(toSerializer)

                return EntryFunction(
                    moduleId = MoveModuleId(AccountAddress.ONE, "aptos_account"),
                    functionName = "transfer",
                    typeArgs = emptyList(),
                    args = listOf(toSerializer.toByteArray(), amountSerializer.toByteArray()),
                )
            }

            @JvmStatic
            fun coinTransfer(
                coinType: TypeTag,
                to: AccountAddress,
                amount: ULong,
            ): EntryFunction {
                val amountSerializer = BcsSerializer()
                amountSerializer.serializeU64(amount)

                val toSerializer = BcsSerializer()
                to.serialize(toSerializer)

                return EntryFunction(
                    moduleId = MoveModuleId(AccountAddress.ONE, "coin"),
                    functionName = "transfer",
                    typeArgs = listOf(coinType),
                    args = listOf(toSerializer.toByteArray(), amountSerializer.toByteArray()),
                )
            }
        }
    }

    data class Multisig(
        val multisigAddress: AccountAddress,
        val entryFunction: EntryFunction?,
    ) : TransactionPayload() {
        override fun serialize(serializer: BcsSerializer) {
            serializer.serializeVariantIndex(3u)
            multisigAddress.serialize(serializer)
            if (entryFunction != null) {
                serializer.serializeOptionTag(true)
                // Serialize just the inner entry function data (without the variant index)
                entryFunction.moduleId.serialize(serializer)
                serializer.serializeString(entryFunction.functionName)
                serializer.serializeSequenceLength(entryFunction.typeArgs.size)
                entryFunction.typeArgs.forEach { it.serialize(serializer) }
                serializer.serializeSequenceLength(entryFunction.args.size)
                entryFunction.args.forEach { serializer.serializeBytes(it) }
            } else {
                serializer.serializeOptionTag(false)
            }
        }
    }
}
