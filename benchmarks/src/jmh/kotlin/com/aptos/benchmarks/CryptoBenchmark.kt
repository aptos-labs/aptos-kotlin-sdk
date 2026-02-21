package com.aptos.benchmarks

import com.aptos.core.crypto.Ed25519
import com.aptos.core.crypto.Hashing
import com.aptos.core.crypto.Secp256k1
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
open class CryptoBenchmark {

    private lateinit var ed25519PrivateKey: Ed25519.PrivateKey
    private lateinit var ed25519PublicKey: Ed25519.PublicKey
    private lateinit var ed25519Signature: Ed25519.Signature
    private lateinit var secp256k1PrivateKey: Secp256k1.PrivateKey
    private lateinit var message: ByteArray

    @Setup
    fun setup() {
        message = "benchmark test message for signing and hashing".toByteArray()

        ed25519PrivateKey = Ed25519.PrivateKey.generate()
        ed25519PublicKey = ed25519PrivateKey.publicKey()
        ed25519Signature = ed25519PrivateKey.sign(message)

        secp256k1PrivateKey = Secp256k1.PrivateKey.generate()
    }

    @Benchmark
    fun ed25519Sign(): Ed25519.Signature = ed25519PrivateKey.sign(message)

    @Benchmark
    fun ed25519Verify(): Boolean = ed25519PublicKey.verify(message, ed25519Signature)

    @Benchmark
    fun secp256k1Sign(): Secp256k1.Signature = secp256k1PrivateKey.sign(message)

    @Benchmark
    fun sha3256Hash(): ByteArray = Hashing.sha3256(message)

    @Benchmark
    fun sha256Hash(): ByteArray = Hashing.sha256(message)
}
