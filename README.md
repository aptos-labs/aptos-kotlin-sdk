# Aptos Kotlin SDK

![Build Status](https://img.shields.io/badge/build-passing-brightgreen)
![Version](https://img.shields.io/badge/version-0.1.0-blue)
![Kotlin](https://img.shields.io/badge/kotlin-2.1.10-7F52FF)
![License](https://img.shields.io/badge/license-Apache--2.0-green)

A Kotlin/JVM SDK for the [Aptos blockchain](https://aptos.dev), providing account management, BCS serialization, transaction building, and REST API access.

## Overview

The Aptos Kotlin SDK is a modular library for interacting with the Aptos blockchain from Kotlin and Java applications. It supports Ed25519 and Secp256k1 accounts, BIP-39 mnemonic key derivation, BCS-encoded transaction construction and signing, and a coroutine-based REST client with automatic retries. The SDK targets JVM 11+ and Android API 26+.

## Installation

**Requirements:** Java 11+, Kotlin 2.1+, Android API 26+ (for Android projects)

### Gradle (Kotlin DSL) -- Version Catalog

Add the following to your `gradle/libs.versions.toml`:

```toml
[versions]
aptos = "0.1.0"

[libraries]
aptos-core = { module = "com.aptos:aptos-core", version.ref = "aptos" }
aptos-client = { module = "com.aptos:aptos-client", version.ref = "aptos" }
aptos-sdk = { module = "com.aptos:aptos-kotlin-sdk", version.ref = "aptos" }
```

Then in your `build.gradle.kts`:

```kotlin
dependencies {
    // Full SDK (includes core + client)
    implementation(libs.aptos.sdk)

    // Or pick individual modules:
    // implementation(libs.aptos.core)     // types, crypto, BCS, accounts, transactions
    // implementation(libs.aptos.client)   // REST API client, faucet (includes core)
}
```

### Gradle (Kotlin DSL) -- Direct Dependencies

```kotlin
dependencies {
    // Full SDK -- pulls in core and client transitively
    implementation("com.aptos:aptos-kotlin-sdk:0.1.0")

    // Or individual modules:
    // implementation("com.aptos:aptos-core:0.1.0")
    // implementation("com.aptos:aptos-client:0.1.0")
}
```

## Quick Start

```kotlin
import com.aptos.sdk.Aptos
import com.aptos.core.account.Ed25519Account
import com.aptos.core.types.AccountAddress

suspend fun main() {
    // Connect to testnet
    val aptos = Aptos.testnet()

    // Generate a new account
    val account = Ed25519Account.generate()
    println("Address: ${account.address}")

    // Fund via faucet (testnet/devnet only)
    aptos.fundAccount(account.address, amount = 100_000_000uL)

    // Check balance
    val balance = aptos.getBalance(account.address)
    println("Balance: $balance")

    // Transfer APT
    val recipient = AccountAddress.fromHexRelaxed("0xBOB_ADDRESS_HERE")
    val signedTxn = aptos.transfer(account, recipient, 1_000uL)
    val pending = aptos.submitTransaction(signedTxn)
    val result = aptos.waitForTransaction(pending.hash)
    println("Transaction succeeded: ${result.success}")

    aptos.close()
}
```

## Features

### Account Management

The SDK supports Ed25519 and Secp256k1 accounts. Both implement the `Account` interface, which provides `address`, `publicKeyBytes`, `scheme`, and a `sign` method.

**Generate a random account:**

```kotlin
import com.aptos.core.account.Ed25519Account
import com.aptos.core.account.Secp256k1Account

val ed25519 = Ed25519Account.generate()
val secp256k1 = Secp256k1Account.generate()
```

**Restore from a private key:**

```kotlin
val account = Ed25519Account.fromPrivateKeyHex("0xYOUR_PRIVATE_KEY_HEX")
```

**Derive from a BIP-39 mnemonic:**

```kotlin
import com.aptos.core.account.Mnemonic
import com.aptos.core.account.DerivationPath

val mnemonic = Mnemonic.generate(wordCount = 12)
println(mnemonic.phrase())

// Default path: m/44'/637'/0'/0'/0'
val account = Ed25519Account.fromMnemonic(mnemonic)

// Custom derivation path
val customPath = DerivationPath.parse("m/44'/637'/1'/0'/0'")
val account2 = Ed25519Account.fromMnemonic(mnemonic, customPath)

// Secp256k1 from mnemonic (uses BIP-32 derivation)
val secpAccount = Secp256k1Account.fromMnemonic(mnemonic)
```

**Validate an existing mnemonic:**

```kotlin
val valid = Mnemonic.isValid("abandon abandon abandon ... about")
val restored = Mnemonic.fromPhrase("word1 word2 ... word12")
```

### Transaction Building and Signing

Use `TransactionBuilder` to construct raw transactions and sign them with any `Account`.

```kotlin
import com.aptos.core.transaction.TransactionBuilder
import com.aptos.core.transaction.TransactionPayload
import com.aptos.core.types.AccountAddress
import com.aptos.core.types.ChainId

val recipient = AccountAddress.fromHexRelaxed("0xcafe")
val payload = TransactionPayload.EntryFunction.aptTransfer(recipient, 1_000uL)

val rawTxn = TransactionBuilder.builder()
    .sender(account.address)
    .sequenceNumber(0uL)
    .payload(payload)
    .maxGasAmount(200_000uL)
    .gasUnitPrice(100uL)
    .chainId(ChainId.TESTNET)
    .build()

val signedTxn = TransactionBuilder.signTransaction(account, rawTxn)

// Or sign via the builder directly:
val signedTxn2 = TransactionBuilder.builder()
    .sender(account.address)
    .sequenceNumber(1uL)
    .payload(payload)
    .chainId(ChainId.TESTNET)
    .sign(account)
```

**Custom entry function calls:**

```kotlin
import com.aptos.core.bcs.BcsSerializer
import com.aptos.core.types.MoveModuleId
import com.aptos.core.types.TypeTag

// Coin transfer with explicit type argument
val coinPayload = TransactionPayload.EntryFunction.coinTransfer(
    coinType = TypeTag.fromString("0x1::aptos_coin::AptosCoin"),
    to = recipient,
    amount = 5_000uL,
)

// Arbitrary entry function
val customPayload = TransactionPayload.EntryFunction(
    moduleId = MoveModuleId.fromString("0x1::aptos_account"),
    functionName = "transfer",
    typeArgs = emptyList(),
    args = listOf(recipientBcs, amountBcs),
)
```

### REST API Client

The `Aptos` facade exposes all REST API operations as `suspend` functions.

```kotlin
val aptos = Aptos.testnet()

// Ledger info
val ledgerInfo = aptos.getLedgerInfo()
println("Chain ID: ${ledgerInfo.chainId}, Block height: ${ledgerInfo.blockHeight}")

// Account info
val info = aptos.getAccount(address)
println("Sequence number: ${info.sequenceNumber}")

// Account resources
val resources = aptos.getAccountResources(address)
val coinStore = aptos.getAccountResource(address, "0x1::coin::CoinStore<0x1::aptos_coin::AptosCoin>")

// Gas estimation
val gasEstimate = aptos.estimateGasPrice()
println("Gas estimate: ${gasEstimate.gasEstimate}")

// View functions
val result = aptos.view(
    function = "0x1::coin::balance",
    typeArguments = listOf("0x1::aptos_coin::AptosCoin"),
    arguments = listOf(address.toHex()),
)

// Submit and wait
val pending = aptos.submitTransaction(signedTxn)
val txn = aptos.waitForTransaction(pending.hash, timeoutMs = 30_000)
```

### BCS Serialization

The `BcsSerializer` and `BcsDeserializer` classes handle Binary Canonical Serialization used by the Aptos blockchain. All core types implement `BcsSerializable`.

```kotlin
import com.aptos.core.bcs.BcsSerializer
import com.aptos.core.bcs.BcsDeserializer

// Serialize
val serializer = BcsSerializer()
serializer.serializeU64(42uL)
serializer.serializeString("hello")
serializer.serializeBool(true)
val bytes = serializer.toByteArray()

// Deserialize
val deserializer = BcsDeserializer(bytes)
val num = deserializer.deserializeU64()     // 42
val str = deserializer.deserializeString()  // "hello"
val flag = deserializer.deserializeBool()   // true

// Serialize complex types
val address = AccountAddress.fromHexRelaxed("0x1")
val ser = BcsSerializer()
address.serialize(ser)
val addressBytes = ser.toByteArray()
```

Implement `BcsSerializable` for custom types:

```kotlin
import com.aptos.core.bcs.BcsSerializable
import com.aptos.core.bcs.BcsSerializer

data class MyStruct(val value: ULong, val name: String) : BcsSerializable {
    override fun serialize(serializer: BcsSerializer) {
        serializer.serializeU64(value)
        serializer.serializeString(name)
    }
}
```

### Type System

**`AccountAddress`** -- 32-byte address with hex parsing and short-string formatting:

```kotlin
import com.aptos.core.types.AccountAddress

val addr = AccountAddress.fromHex("0x0000000000000000000000000000000000000000000000000000000000000001")
val addrShort = AccountAddress.fromHexRelaxed("0x1")  // zero-padded automatically
println(addr.toHex())         // 0x0000...0001
println(addr.toShortString()) // 0x1
println(AccountAddress.isValid("0x1"))  // true

// Well-known addresses
AccountAddress.ZERO  // 0x0
AccountAddress.ONE   // 0x1
```

**`TypeTag`** -- Move type tags with full parser support:

```kotlin
import com.aptos.core.types.TypeTag

val simple = TypeTag.fromString("u64")
val vector = TypeTag.fromString("vector<u8>")
val struct = TypeTag.fromString("0x1::coin::CoinStore<0x1::aptos_coin::AptosCoin>")

// Primitive type singletons
TypeTag.Bool
TypeTag.U8
TypeTag.U64
TypeTag.U128
TypeTag.U256
TypeTag.Address
```

**`StructTag`** -- Typed Move struct references:

```kotlin
import com.aptos.core.types.StructTag

val tag = StructTag.fromString("0x1::aptos_coin::AptosCoin")
val aptosCoin = StructTag.aptosCoin()
println(tag)  // 0x1::aptos_coin::AptosCoin
```

**`ChainId`** -- Network chain identifiers:

```kotlin
import com.aptos.core.types.ChainId

ChainId.MAINNET  // 1
ChainId.TESTNET  // 2
ChainId.LOCAL    // 4
```

## Java Interop

All `suspend` functions have blocking counterparts suffixed with `Blocking` for use from Java code. Static factory methods are annotated with `@JvmStatic`.

```java
import com.aptos.sdk.Aptos;
import com.aptos.core.account.Ed25519Account;
import com.aptos.core.types.AccountAddress;

public class Example {
    public static void main(String[] args) {
        Aptos aptos = Aptos.testnet();
        Ed25519Account account = Ed25519Account.generate();

        // Blocking wrappers for Java
        aptos.fundAccountBlocking(account.getAddress(), 100_000_000L);
        long balance = aptos.getBalanceBlocking(account.getAddress());

        var ledgerInfo = aptos.getLedgerInfoBlocking();
        System.out.println("Chain ID: " + ledgerInfo.getChainId());

        aptos.close();
    }
}
```

## Module Architecture

| Module | Artifact | Package | Description |
|--------|----------|---------|-------------|
| `:core` | `com.aptos:aptos-core` | `com.aptos.core.*` | Types (`AccountAddress`, `TypeTag`, `StructTag`, `ChainId`), BCS serialization/deserialization, Ed25519/Secp256k1 cryptography, account management, BIP-39 mnemonics, transaction building and signing |
| `:client` | `com.aptos:aptos-client` | `com.aptos.client.*` | Coroutine-based REST API client (`AptosRestClient`), faucet client (`FaucetClient`), network configuration (`AptosConfig`), retry policy with exponential backoff |
| `:sdk` | `com.aptos:aptos-kotlin-sdk` | `com.aptos.sdk.*` | High-level `Aptos` facade composing REST client, faucet, and transaction utilities into a single entry point with convenience methods like `transfer` |

Dependency graph: `:sdk` depends on `:client`, which depends on `:core`.

## Configuration

Use `AptosConfig` to configure network endpoints, timeouts, and retry behavior.

**Pre-configured networks:**

```kotlin
import com.aptos.client.config.AptosConfig

val mainnet = AptosConfig.mainnet()   // https://fullnode.mainnet.aptoslabs.com/v1
val testnet = AptosConfig.testnet()   // https://fullnode.testnet.aptoslabs.com/v1 + faucet
val devnet  = AptosConfig.devnet()    // https://fullnode.devnet.aptoslabs.com/v1 + faucet
val local   = AptosConfig.localnet()  // http://localhost:8080/v1 + faucet
```

**Custom configuration:**

```kotlin
import com.aptos.client.config.AptosConfig
import com.aptos.client.config.RetryConfig
import com.aptos.core.types.ChainId

val config = AptosConfig(
    nodeUrl = "https://fullnode.mainnet.aptoslabs.com/v1",
    faucetUrl = null,
    chainId = ChainId.MAINNET,
    timeoutMs = 60_000,
    retryConfig = RetryConfig(
        maxRetries = 5,
        initialDelayMs = 500,
        maxDelayMs = 30_000,
        backoffMultiplier = 2.0,
    ),
)
val aptos = Aptos(config)
```

**Builder pattern:**

```kotlin
val aptos = Aptos.builder()
    .nodeUrl("http://localhost:8080/v1")
    .faucetUrl("http://localhost:8081")
    .chainId(ChainId.LOCAL)
    .timeoutMs(5_000)
    .build()
```

## Error Handling

All SDK errors extend `AptosException`, a `RuntimeException` subclass. Catch specific subtypes for fine-grained handling.

```
AptosException                       -- base class for all SDK errors
  |-- ApiException                   -- REST API / HTTP errors (includes statusCode, errorCode)
  |-- BcsSerializationException      -- BCS encoding failures
  |-- BcsDeserializationException    -- BCS decoding failures
  |-- AccountAddressParseException   -- invalid address hex strings
  |-- TypeTagParseException          -- malformed type tag strings
  |-- CryptoException                -- key generation / signing / verification errors
  |-- MnemonicException              -- invalid mnemonic phrases or derivation paths
  |-- TransactionBuildException      -- missing required fields in TransactionBuilder
```

**Example:**

```kotlin
import com.aptos.core.error.ApiException
import com.aptos.core.error.AptosException

try {
    val balance = aptos.getBalance(address)
} catch (e: ApiException) {
    println("API error (HTTP ${e.statusCode}): ${e.message}")
    println("Error code: ${e.errorCode}")
} catch (e: AptosException) {
    println("SDK error: ${e.message}")
}
```

The `ErrorCategory` enum maps on-chain VM error codes to human-readable categories:

```kotlin
import com.aptos.core.error.ErrorCategory

val category = ErrorCategory.fromCode(5) // ErrorCategory.NOT_FOUND
println(category?.description)           // "Resource not found"
```

## Contributing

Contributions are welcome. To get started:

```bash
# Clone the repository
git clone https://github.com/aptos-labs/aptos-kotlin-sdk.git
cd aptos-kotlin-sdk

# Build
./gradlew build

# Run tests
./gradlew test

# Run linter
./gradlew detekt
```

Please ensure all tests pass and the code conforms to the detekt configuration before submitting a pull request.

## License

This project is licensed under the [Apache License 2.0](LICENSE).
