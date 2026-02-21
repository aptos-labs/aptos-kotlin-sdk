# Aptos Kotlin SDK - Design Document

## Overview

A production-ready (Tier 2 / P0+P1) Kotlin SDK for the Aptos blockchain, targeting JVM (Java 11+) and Android (API 26+). Built with idiomatic Kotlin patterns, coroutine-first async, and full Java interoperability.

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────┐
│                     :sdk module                         │
│  ┌───────────────────────────────────────────────────┐  │
│  │              Aptos (Facade)                       │  │
│  │  - transferAPT()   - getBalance()                 │  │
│  │  - fundAccount()   - getLedgerInfo()              │  │
│  │  + Java blocking wrappers via runBlocking         │  │
│  └───────────────┬───────────────────────────────────┘  │
└──────────────────┼──────────────────────────────────────┘
                   │ delegates to
┌──────────────────┼──────────────────────────────────────┐
│                  │  :client module                      │
│  ┌───────────────▼───────────┐  ┌─────────────────────┐│
│  │    AptosRestClient        │  │    FaucetClient      ││
│  │  - getLedgerInfo()        │  │  - fundAccount()     ││
│  │  - getAccount()           │  └─────────────────────┘│
│  │  - submitTransaction()    │                          │
│  │  - waitForTransaction()   │  ┌─────────────────────┐│
│  │  - view()                 │  │    RetryPolicy       ││
│  │  - estimateGasPrice()     │  │  - exp. backoff      ││
│  └───────────┬───────────────┘  │  - jitter            ││
│              │ uses              └─────────────────────┘│
│  ┌───────────▼───────────────┐                          │
│  │   Ktor HttpClient (CIO)   │                          │
│  │  + kotlinx.serialization  │                          │
│  └───────────────────────────┘                          │
└──────────────────┬──────────────────────────────────────┘
                   │ depends on
┌──────────────────┼──────────────────────────────────────┐
│                  │  :core module                        │
│                  │                                      │
│  ┌───────────────▼───────────────────────────────────┐  │
│  │  Transaction Layer                                │  │
│  │  RawTransaction ─── TransactionPayload (sealed)   │  │
│  │  SignedTransaction ─ TransactionAuthenticator      │  │
│  │  EntryFunction ──── TransactionBuilder (fluent)   │  │
│  └───────────────┬───────────────────────────────────┘  │
│                  │ uses                                  │
│  ┌───────────────▼──────────┐  ┌───────────────────┐   │
│  │  Account Layer           │  │  Crypto Layer      │   │
│  │  Account (interface)     │  │  Ed25519 keys      │   │
│  │  Ed25519Account          │  │  Secp256k1 keys    │   │
│  │  Secp256k1Account        │  │  Hashing (SHA3)    │   │
│  │  AnyAccount (sealed)     │  │  AuthenticationKey │   │
│  │  Mnemonic (BIP-39)       │  │  Mnemonic/BIP-44   │   │
│  └──────────────────────────┘  └───────────────────┘   │
│                  │ uses                                  │
│  ┌───────────────▼──────────┐  ┌───────────────────┐   │
│  │  Core Types              │  │  BCS Layer         │   │
│  │  AccountAddress          │  │  BcsSerializer     │   │
│  │  ChainId (value class)   │  │  BcsDeserializer   │   │
│  │  TypeTag (sealed)        │  │  BcsSerializable   │   │
│  │  StructTag               │  │  ULEB128           │   │
│  │  MoveModuleId            │  └───────────────────┘   │
│  └──────────────────────────┘                           │
│                                                         │
│  ┌──────────────────────────────────────────────────┐   │
│  │  Error Layer                                     │   │
│  │  AptosException (base) ── ErrorCategory (enum)   │   │
│  │  ParseException, CryptoException,                │   │
│  │  SerializationException, ApiException, ...       │   │
│  └──────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
```

## Dependency Graph

```
:sdk ──────► :client ──────► :core
  │                            ▲
  └────────────────────────────┘
```

## BCS Serialization Design

```
BcsSerializable (interface)
├── serialize(BcsSerializer)
└── toBytes(): ByteArray  (default: creates serializer, serializes, returns bytes)

BcsSerializer
├── Internal: ByteArrayOutputStream (growable, 256-byte initial)
├── Primitives: serializeBool, serializeU8..U256
├── Composites: serializeBytes, serializeStr, serializeFixedBytes
├── ULEB128: serializeU32AsUleb128
├── Collections: serializeVector<T>, serializeOption<T?>
└── toByteArray()

BcsDeserializer
├── Internal: ByteArray + offset pointer
├── Primitives: deserializeBool, deserializeU8..U256
├── Composites: deserializeBytes, deserializeStr, deserializeFixedBytes
├── ULEB128: deserializeUleb128
├── Collections: deserializeVector(λ), deserializeOption(λ)
└── remaining(), ensureEmpty()
```

**Unsigned type mapping:**
| Spec Type | Kotlin Type | BCS Bytes |
|-----------|------------|-----------|
| u8 | UByte | 1 LE |
| u16 | UShort | 2 LE |
| u32 | UInt | 4 LE |
| u64 | ULong | 8 LE |
| u128 | BigInteger | 16 LE |
| u256 | BigInteger | 32 LE |

## Type System Design

```
TypeTag (sealed class)
├── data object Bool                     BCS: 0x00
├── data object U8                       BCS: 0x01
├── data object U16                      BCS: 0x02
├── data object U32                      BCS: 0x03
├── data object U64                      BCS: 0x04
├── data object U128                     BCS: 0x05
├── data object U256                     BCS: 0x06
├── data object Address                  BCS: 0x07
├── data object Signer                   BCS: 0x08
├── data class Vector(elementType)       BCS: 0x09 + element
└── data class Struct(structTag)         BCS: 0x0a + struct

TransactionPayload (sealed class)
├── data class Script(...)               BCS variant: 0x00
├── data object ModuleBundle             BCS variant: 0x01
├── data class EntryFunctionPayload(...) BCS variant: 0x02
└── data class Multisig(...)             BCS variant: 0x03

TransactionAuthenticator (sealed class)
├── data class Ed25519Auth(pubkey, sig)  BCS variant: 0x00
├── data class MultiEd25519(...)         BCS variant: 0x01  (P2)
├── data class MultiAgent(...)           BCS variant: 0x02  (P2)
├── data class FeePayer(...)             BCS variant: 0x03  (P2)
└── data class SingleSender(acctAuth)    BCS variant: 0x04
```

## Cryptography Design

```
Bouncy Castle Integration
├── Ed25519
│   ├── Ed25519PrivateKeyParameters (32-byte seed)
│   ├── Ed25519PublicKeyParameters (derived)
│   ├── Ed25519Signer (RFC 8032, deterministic)
│   └── Scheme identifier: 0x00
│
├── Secp256k1
│   ├── ECPrivateKeyParameters + SECNamedCurves("secp256k1")
│   ├── ECDSASigner + HMacDSAKCalculator(SHA256Digest) (RFC 6979)
│   ├── Manual low-S normalization: if s > n/2, s = n - s
│   └── Scheme identifier: 0x01
│
├── Hashing
│   ├── SHA3-256 (SHA3Digest, primary)
│   ├── SHA2-256 (SHA256Digest, for BIP-39)
│   └── Domain separation: SHA3-256(SHA3-256(domain) || data)
│
└── Key Derivation
    ├── BIP-39: PBKDF2-HMAC-SHA512 (2048 rounds, "mnemonic"+passphrase salt)
    ├── SLIP-0010: HMAC-SHA512 chain for Ed25519 (hardened only)
    └── BIP-32: HMAC-SHA512 chain for Secp256k1
```

**Authentication Key:**
```
AuthenticationKey = SHA3-256(public_key_bytes || scheme_identifier)
  └── 32 bytes, IS the account address for new accounts
```

**Transaction Signing:**
```
signing_message = SHA3-256("APTOS::RawTransaction") || BCS(raw_txn)
                  ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
                  pre-computed: 0xb5e97db07fa0...b193

signature = Ed25519.sign(signing_message) or Secp256k1.sign(signing_message)
```

## Account Design

```
Account (interface)
├── val address: AccountAddress
├── fun publicKeyBytes(): ByteArray
├── fun signatureScheme(): SignatureScheme
├── fun sign(message: ByteArray): ByteArray
├── fun authenticationKey(): AuthenticationKey
└── fun signTransaction(rawTxn): SignedTransaction  (default impl)

Ed25519Account : Account
├── Companion: generate(), fromPrivateKey(), fromMnemonic()
├── address = authenticationKey().accountAddress()  (lazy)
└── sign() delegates to Ed25519PrivateKey.sign()

Secp256k1Account : Account
├── Same pattern as Ed25519Account
├── Uses uncompressed public key (65 bytes) for auth key
└── sign() delegates to Secp256k1PrivateKey.sign()

AnyAccount (sealed class) : Account
├── data class Ed25519(account) : AnyAccount()
└── data class Secp256k1(account) : AnyAccount()
```

## API Client Design

```
AptosConfig
├── fullnodeUrl: String
├── faucetUrl: String?
├── requestTimeoutMs: Long = 30_000
├── retryConfig: RetryConfig
└── Companion: mainnet(), testnet(), devnet(), localnet()
    ├── Mainnet: https://fullnode.mainnet.aptoslabs.com/v1, chain_id=1
    ├── Testnet: https://fullnode.testnet.aptoslabs.com/v1, chain_id=2
    └── Localnet: http://localhost:8080/v1, chain_id=4

AptosRestClient (suspend functions)
├── GET  /v1              → getLedgerInfo(): LedgerInfo
├── GET  /v1/accounts/{a} → getAccount(addr): AccountInfo
├── GET  /v1/accounts/{a}/resources → getAccountResources(addr): List<Resource>
├── GET  /v1/accounts/{a}/resource/{t} → getAccountResource(addr, type): Resource
├── GET  /v1/transactions/by_hash/{h} → getTransactionByHash(hash): Transaction
├── POST /v1/transactions → submitTransaction(signedTxn): PendingTransaction
│   Content-Type: application/x.aptos.signed_transaction+bcs
│   Body: BCS-encoded SignedTransaction bytes
├── GET  /v1/transactions/by_hash/{h} (poll) → waitForTransaction(hash, timeout)
├── GET  /v1/estimate_gas_price → estimateGasPrice(): GasEstimate
├── POST /v1/view → view(function, typeArgs, args): List<Any>
└── Combined: submitAndWait(signedTxn): Transaction

RetryPolicy
├── Retryable: network errors, timeouts, HTTP 429, 5xx
├── Not retryable: 400, 404, parse errors
├── Strategy: exponential backoff (100ms → 200ms → 400ms...) + jitter
└── Max retries: 3, max delay: 5s
```

## Java Interop Strategy

```kotlin
// Every companion factory gets @JvmStatic
companion object {
    @JvmStatic fun fromHex(hex: String): AccountAddress
}

// Functions with defaults get @JvmOverloads
@JvmOverloads
fun toSeed(passphrase: String = ""): ByteArray

// Suspend functions get blocking wrappers
suspend fun getBalance(address: AccountAddress): ULong

@JvmName("getBalanceBlocking")
fun getBalanceSync(address: AccountAddress): ULong = runBlocking { getBalance(address) }
```

## Differences from Reference Implementations

| Aspect | TypeScript | Python | Kotlin (this SDK) |
|--------|-----------|--------|--------------------|
| Architecture | Facade + 13 mixins | Monolithic RestClient | Facade + 3 modules |
| Async | Promises | async/await (asyncio) | Coroutines (suspend) + blocking |
| BCS buffer | Uint8Array + pool | io.BytesIO | ByteArrayOutputStream |
| Unsigned ints | BigInt (u128/256) | Python int (arbitrary) | ULong (u64), BigInteger (u128/256) |
| Type variants | Union types | Enum classes | Sealed classes (exhaustive when) |
| Crypto lib | @noble/curves | PyNaCl + ecdsa | Bouncy Castle |
| JSON | native JSON | built-in json | kotlinx.serialization |
| Null safety | optional chaining | Optional/None | Kotlin null safety (?) |
| Key erasure | N/A | N/A | Private toString = "***" |

## Error Handling Design

```
AptosException (RuntimeException)
├── category: ErrorCategory (enum of 12 categories)
├── message: String
├── errorCode: String?
├── cause: Throwable?
│
├── ParseException          (PARSE)
├── CryptoException         (CRYPTO)
├── SerializationException  (SERIALIZATION)
├── NetworkException        (NETWORK)
├── ApiException            (API) + httpStatus, apiErrorCode, vmErrorCode
├── TimeoutException        (TIMEOUT)
├── InvalidInputException   (INVALID_INPUT)
└── InvalidStateException   (INVALID_STATE)

HTTP Status → Exception Mapping:
  400 → ParseException or InvalidInputException
  401 → AptosException(UNAUTHORIZED)
  404 → AptosException(NOT_FOUND)
  429 → AptosException(RATE_LIMITED)
  5xx → ApiException
  Timeout → TimeoutException
  Connection error → NetworkException
```

## Performance Optimizations

1. **BCS pre-sizing**: 256-byte initial buffer (typical txn = 200-400 bytes)
2. **ByteBuffer.LITTLE_ENDIAN** for multi-byte int writes
3. **Pre-computed hash prefixes**: RAW_TRANSACTION_PREFIX, TRANSACTION_PREFIX
4. **Lazy account address**: derived from auth key only when first accessed
5. **Reusable Charsets.UTF_8** reference for string encoding
6. **Clear deserialization errors**: bounds-check before every read with remaining byte count
