package com.aptos.benchmarks

import com.aptos.core.bcs.BcsSerializer
import com.aptos.core.transaction.RawTransaction
import com.aptos.core.transaction.TransactionPayload
import com.aptos.core.types.AccountAddress
import com.aptos.core.types.ChainId
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import java.util.concurrent.TimeUnit

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
open class BcsBenchmark {

    private lateinit var rawTxn: RawTransaction
    private lateinit var entryFunction: TransactionPayload.EntryFunction

    @Setup
    fun setup() {
        entryFunction = TransactionPayload.EntryFunction.aptTransfer(AccountAddress.ONE, 1_000_000uL)
        rawTxn = RawTransaction(
            sender = AccountAddress.ONE,
            sequenceNumber = 42uL,
            payload = entryFunction,
            maxGasAmount = 200_000uL,
            gasUnitPrice = 100uL,
            expirationTimestampSecs = 1700000000uL,
            chainId = ChainId.TESTNET,
        )
    }

    @Benchmark
    fun serializeRawTransaction(): ByteArray {
        val serializer = BcsSerializer()
        rawTxn.serialize(serializer)
        return serializer.toByteArray()
    }

    @Benchmark
    fun serializeEntryFunction(): ByteArray {
        val serializer = BcsSerializer()
        entryFunction.serialize(serializer)
        return serializer.toByteArray()
    }

    @Benchmark
    fun serializeU64(): ByteArray {
        val serializer = BcsSerializer()
        serializer.serializeU64(1_000_000uL)
        return serializer.toByteArray()
    }

    @Benchmark
    fun signingMessage(): ByteArray = rawTxn.signingMessage()
}
