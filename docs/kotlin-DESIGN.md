# Aptos Kotlin SDK - Design Document

## Overview

A production-ready (Tier 3 / P0+P1+P2) Kotlin SDK for the Aptos blockchain, targeting JVM (Java 11+) and Android (API 26+). Built with idiomatic Kotlin patterns, coroutine-first async, and full Java interoperability.

| Property | Value |
|----------|-------|
| Tier | 3 (P0 + P1 + P2 features) |
| Platform | Kotlin/JVM 1.8+ / Android API 26+ |
| Java target | 11 |
| Build | Gradle 8.x + Kotlin DSL |
| HTTP | Ktor Client 3.0 (CIO engine) |
| JSON | kotlinx.serialization 1.7 |
| Async | Kotlin Coroutines (suspend) + blocking wrappers |
| Crypto | Bouncy Castle 1.79 (bcprov-jdk18on) |
| Tests | JUnit 5 + MockK + Ktor MockEngine + Kotest assertions |
| Benchmarks | JMH (me.champeau.jmh plugin) |
| Package | `com.aptos.core`, `com.aptos.client`, `com.aptos.sdk`, `com.aptos.indexer` |

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                        :sdk module                          │
│  ┌───────────────────────────────────────────────────────┐  │
│  │                  Aptos (Facade)                       │  │
│  │  transfer()  getBalance()  fundAccount()              │  │
│  │  getLedgerInfo()  submitTransaction()  view()          │  │
│  │  + Blocking wrappers: *Blocking() via runBlocking     │  │
│  └──────────────────────┬────────────────────────────────┘  │
│                         │ delegates to                      │
│  ┌──────────────────────┴────────────────────────────────┐  │
│  │  AptosBuilder (fluent configuration)                  │  │
│  └───────────────────────────────────────────────────────┘  │
└─────────────────────────┬───────────────────────────────────┘
                          │
┌─────────────────────────┼───────────────────────────────────┐
│                         │  :client module                   │
│  ┌──────────────────────▼──────────┐  ┌──────────────────┐  │
│  │     AptosRestClient             │  │   FaucetClient   │  │
│  │  getLedgerInfo()                │  │  fundAccount()   │  │
│  │  getAccount()                   │  │  /fund → /mint   │  │
│  │  getAccountResources()          │  │  (dual fallback) │  │
│  │  getAccountResource()           │  └──────────────────┘  │
│  │  getTransactionByHash()         │                        │
│  │  submitTransaction() [BCS body] │  ┌──────────────────┐  │
│  │  waitForTransaction() [polling] │  │   RetryPolicy    │  │
│  │  estimateGasPrice()             │  │  exp. backoff    │  │
│  │  view()                         │  │  + jitter        │  │
│  │  getBalance()                   │  │  429,500-504     │  │
│  └──────────────┬──────────────────┘  └──────────────────┘  │
│                 │                                            │
│  ┌──────────────▼──────────────────┐                        │
│  │  Ktor HttpClient (CIO engine)   │  Response Models:      │
│  │  + ContentNegotiation           │  LedgerInfo,           │
│  │  + kotlinx.serialization.json   │  AccountInfo,          │
│  │  + HttpTimeout                  │  AccountResource,      │
│  └─────────────────────────────────┘  TransactionResponse,  │
│                                       PendingTransaction,   │
│  ┌─────────────────────────────────┐  GasEstimate,          │
│  │  AptosConfig + RetryConfig      │  ApiError, ViewRequest │
│  │  mainnet() testnet() devnet()   │                        │
│  │  localnet()                     │                        │
│  └─────────────────────────────────┘                        │
└─────────────────────────┬───────────────────────────────────┘
                          │ depends on
┌─────────────────────────┼───────────────────────────────────┐
│                         │  :core module (zero network deps) │
│                         │                                   │
│  ┌──────────────────────▼──────────────────────────────┐    │
│  │  Transaction Layer                                  │    │
│  │  RawTransaction (7 fields, BCS in spec order)       │    │
│  │  TransactionPayload (sealed: Script/EntryFn/Multi)  │    │
│  │  SignedTransaction (rawTxn + authenticator)          │    │
│  │  TransactionAuthenticator (sealed: Ed25519/Single)  │    │
│  │  AccountAuthenticator (sealed: Ed25519/SingleKey)   │    │
│  │  TransactionBuilder (fluent, sensible defaults)     │    │
│  └──────────────────┬──────────────────────────────────┘    │
│                     │ uses                                   │
│  ┌──────────────────▼─────────┐  ┌────────────────────────┐ │
│  │  Account Layer             │  │  Crypto Layer          │ │
│  │  Account (interface)       │  │  Ed25519 (BC native)   │ │
│  │  Ed25519Account            │  │  Secp256k1 (RFC 6979)  │ │
│  │  Secp256k1Account          │  │  Hashing (SHA3/SHA2)   │ │
│  │  AnyAccount (sealed)       │  │  AuthenticationKey     │ │
│  │  Mnemonic (BIP-39)         │  │  SignatureScheme enum  │ │
│  │  DerivationPath            │  │  Slip0010 / Bip32      │ │
│  └────────────────────────────┘  └────────────────────────┘ │
│                     │ uses                                   │
│  ┌──────────────────▼─────────┐  ┌────────────────────────┐ │
│  │  Core Types                │  │  BCS Layer             │ │
│  │  AccountAddress (32-byte)  │  │  BcsSerializer         │ │
│  │  ChainId (inline value)    │  │  BcsDeserializer       │ │
│  │  TypeTag (sealed, 11 var)  │  │  BcsSerializable iface │ │
│  │  StructTag (data class)    │  │  ULEB128 encode/decode │ │
│  │  MoveModuleId              │  └────────────────────────┘ │
│  │  HexString (utility)       │                             │
│  └────────────────────────────┘                             │
│                                                             │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  Error Layer                                        │   │
│  │  AptosException (base RuntimeException)             │   │
│  │  ├── BcsSerializationException                      │   │
│  │  ├── BcsDeserializationException                    │   │
│  │  ├── AccountAddressParseException                   │   │
│  │  ├── TypeTagParseException                          │   │
│  │  ├── CryptoException                                │   │
│  │  ├── MnemonicException                              │   │
│  │  ├── TransactionBuildException                      │   │
│  │  └── ApiException (statusCode, errorCode)           │   │
│  │                                                     │   │
│  │  ErrorCategory enum (12 categories, code 1-12)      │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

## Dependency Graph

```
:sdk ──────► :client ──────► :core
  │                            ▲
  └────────────────────────────┘

:indexer ──────────────────► :core   (independent opt-in module)

:benchmarks ──────────────► :core   (JMH benchmarks, not in kover)

External dependencies:
:core       → kotlin-stdlib, bouncy-castle (bcprov-jdk18on 1.79)
:client     → :core, ktor-client-*, kotlinx-coroutines-core
:sdk        → :core, :client
:indexer    → :core, ktor-client-*, kotlinx-serialization (no :client dep)
:benchmarks → :core, JMH
```

## BCS Serialization Design

BCS (Binary Canonical Serialization) is the deterministic binary format used by Aptos for on-chain data, transaction encoding, and signing messages.

```
BcsSerializable (interface)
├── serialize(BcsSerializer)        ── all types implement this
└── toBytes(): ByteArray            ── default: creates serializer, serializes, returns bytes

BcsSerializer
├── Internal: ByteArrayOutputStream (growable, 256-byte initial capacity)
├── Primitives: serializeBool, serializeU8, serializeU16, serializeU32, serializeU64
├── Big integers: serializeU128(BigInteger), serializeU256(BigInteger)
├── Composites: serializeBytes (ULEB128 + data), serializeString (UTF-8 + ULEB128)
├── Fixed: serializeFixedBytes (raw, no prefix -- addresses, keys)
├── ULEB128: serializeUleb128(UInt), serializeSequenceLength, serializeVariantIndex
├── Options: serializeOptionTag(Boolean), serializeOption<T?>(T?)
├── Sequences: serializeSequence<T>(List<T>)
└── toByteArray()

BcsDeserializer
├── Internal: ByteArray + offset pointer
├── Primitives: deserializeBool, deserializeU8..U256
├── Composites: deserializeBytes, deserializeStr, deserializeFixedBytes(len)
├── ULEB128: deserializeUleb128 → UInt
├── Variant: deserializeVariantIndex → UInt
├── Collections: deserializeVector(λ), deserializeOption(λ)
└── remaining: Int, ensureEmpty()
```

**Multi-byte integer encoding:** Uses `ByteBuffer.LITTLE_ENDIAN` for u16/u32/u64 writes. For u128/u256, converts `BigInteger.toByteArray()` (big-endian) via byte reversal into fixed-size little-endian output.

**Unsigned type mapping:**
| Spec Type | Kotlin Type | BCS Bytes | Notes |
|-----------|------------|-----------|-------|
| u8 | UByte | 1 | Direct write |
| u16 | UShort | 2 LE | ByteBuffer |
| u32 | UInt | 4 LE | ByteBuffer |
| u64 | ULong | 8 LE | ByteBuffer |
| u128 | BigInteger | 16 LE | Manual reversal + padding |
| u256 | BigInteger | 32 LE | Manual reversal + padding |

## Type System Design

**TypeTag** -- sealed class representing Move type tags, used for entry function type arguments and resource type specifiers.

```
TypeTag (sealed class, 11 variants)
├── data object Bool              BCS variant index: 0
├── data object U8                BCS variant index: 1
├── data object U64               BCS variant index: 2   ← NOTE: not 4
├── data object U128              BCS variant index: 3   ← NOTE: not 5
├── data object Address           BCS variant index: 4   ← NOTE: not 7
├── data object Signer            BCS variant index: 5   ← NOTE: not 8
├── data class  Vector(element)   BCS variant index: 6   + recursive element
├── data class  Struct(structTag) BCS variant index: 7   + StructTag fields
├── data object U16               BCS variant index: 8   ← added after U64 in Move
├── data object U32               BCS variant index: 9   ← added after U64 in Move
└── data object U256              BCS variant index: 10  ← added after U128 in Move
```

> **Important:** The BCS variant indices follow the historical order types were added to Move, NOT numeric order. U16/U32/U256 were added after the original types, so they have higher indices (8, 9, 10).

**TypeTagParser** -- recursive descent parser for strings like `vector<0x1::coin::CoinStore<0x1::aptos_coin::AptosCoin>>` with max nesting depth of 32. Handles keyword disambiguation (e.g., `u8` vs `u8something`).

**TransactionPayload (sealed class):**
```
├── Script                   BCS variant: 0   (code + typeArgs + args)
├── [ModuleBundle]           BCS variant: 1   (deprecated, not implemented)
├── EntryFunction            BCS variant: 2   (moduleId + funcName + typeArgs + args)
│   ├── aptTransfer()        convenience: 0x1::aptos_account::transfer
│   └── coinTransfer()       convenience: 0x1::coin::transfer
└── Multisig                 BCS variant: 3   (multisigAddr + optional EntryFunction)
```

**TransactionAuthenticator (sealed class):**
```
TransactionAuthenticator                AccountAuthenticator
├── Ed25519Auth       variant: 0        ├── Ed25519Auth       variant: 0
│   (pubkey + sig)                      │   (pubkey + sig)
├── MultiEd25519Auth  variant: 1        ├── MultiEd25519Auth  variant: 1
│   (multiPubkey + multiSig)            │   (multiPubkey + multiSig)
├── MultiAgent        variant: 2        ├── SingleKey         variant: 2
│   (sender + secondary addrs+auths)    │   (pubKeyType+pubKeyBytes+sigType+sigBytes)
├── FeePayer          variant: 3        └── MultiKeyAuth      variant: 3
│   (sender + secondaries + feePayer)       (multiKeyPubkey + multiKeySig)
└── SingleSender      variant: 4
    (accountAuth)
```

## Cryptography Design

```
Bouncy Castle 1.79 Integration
├── Ed25519 (object)
│   ├── PrivateKey (32 bytes) → Ed25519PrivateKeyParameters
│   │   ├── generate() → SecureRandom
│   │   ├── fromHex(), fromSeed()
│   │   ├── publicKey() → derives PublicKey
│   │   ├── sign(message) → Ed25519Signer (RFC 8032, deterministic)
│   │   └── toString() → "Ed25519PrivateKey(***)"  [key erasure]
│   ├── PublicKey (32 bytes)
│   │   └── verify(message, signature) → Ed25519Signer
│   ├── Signature (64 bytes)
│   └── Scheme identifier: 0x00
│
├── Secp256k1 (object)
│   ├── PrivateKey (32 bytes)
│   │   ├── generate() → SecureRandom, reject d=0 or d≥n
│   │   ├── publicKey() → FixedPointCombMultiplier (uncompressed, 65 bytes)
│   │   ├── sign(message):
│   │   │   1. hash = SHA-256(message)
│   │   │   2. ECDSASigner + HMacDSAKCalculator(SHA256Digest) [RFC 6979]
│   │   │   3. Low-S: if s > n/2, s = n - s
│   │   │   4. Encode r||s as 64-byte concatenation
│   │   └── toString() → "Secp256k1PrivateKey(***)"  [key erasure]
│   ├── PublicKey (33 or 65 bytes)
│   │   ├── compressed() → 33-byte SEC1
│   │   ├── uncompressed() → 65-byte SEC1 (0x04 || x || y)
│   │   └── verify() → SHA-256 hash + reject high-S + ECDSASigner
│   ├── Signature (64 bytes: r || s)
│   └── Scheme identifier: 0x01
│
├── Hashing (object)
│   ├── sha3256(data) → SHA3.Digest256() → 32 bytes
│   ├── sha256(data) → SHA256.Digest() → 32 bytes
│   ├── domainSeparatedHash(data, prefix):
│   │   → SHA3-256(SHA3-256("prefix::prefix") || data)
│   └── Pre-computed lazy prefixes:
│       ├── RAW_TRANSACTION_PREFIX = SHA3-256("APTOS::RawTransaction")
│       ├── RAW_TRANSACTION_WITH_DATA_PREFIX = SHA3-256("APTOS::RawTransactionWithData")
│       └── TRANSACTION_PREFIX = SHA3-256("APTOS::Transaction")
│
└── Key Derivation
    ├── BIP-39 (Mnemonic class)
    │   └── PBKDF2-HMAC-SHA512 (2048 rounds, salt: "mnemonic" + passphrase) → 64-byte seed
    ├── SLIP-0010 (Slip0010 object)
    │   └── HMAC-SHA512 chain derivation for Ed25519 (hardened only)
    └── BIP-32 (Bip32 object)
        └── HMAC-SHA512 chain derivation for Secp256k1
```

**MultiEd25519** -- N-of-M Ed25519 multi-signature (max 32 keys):
```
MultiEd25519 (object)
├── PublicKey (keys: List<Ed25519.PublicKey>, threshold: UByte)
│   ├── toBytes() → pk1||pk2||...||pkN||threshold
│   ├── serialize() → BCS serializeBytes(toBytes())
│   └── authKey() → SHA3-256(toBytes() || 0x01)
├── Signature (signatures: List<IndexedSignature>, bitmap: Bitmap)
│   ├── toBytes() → sig1||sig2||...||sigK||4-byte-bitmap
│   └── serialize() → BCS serializeBytes(toBytes())
├── IndexedSignature (index: Int, signature: Ed25519.Signature)
├── Bitmap (bits: Int) → 4-byte MSB-first bitmap
│   └── fromIndices(sorted, unique, < numKeys)
└── verify(publicKey, message, signature) → Boolean
```

**MultiKey** -- Heterogeneous multi-sig (mixed Ed25519 + Secp256k1), scheme byte 0x03:
```
MultiKey (object)
├── AnyPublicKey (type: UByte, keyBytes: ByteArray)
│   ├── ed25519(pk) / secp256k1(pk) factories
│   └── BCS: variant_index(type) || bytes
├── PublicKey (keys: List<AnyPublicKey>, sigsRequired: UByte)
│   ├── BCS: ULEB128(N) || pk1.serialize() || ... || u8(sigsRequired)
│   └── authKey() → SHA3-256(BCS(PublicKey) || 0x03)
├── AnySignature (type: UByte, sigBytes: ByteArray)
│   └── ed25519(sig) / secp256k1(sig) factories
└── Signature (signatures: List<AnySignature>, bitmap: Bitmap)
```

**Keyless** -- OIDC-based keyless authentication:
```
Keyless (object)
├── JwtClaims (iss, aud, sub, nonce, uidKey, uidVal)
├── parseJwt(token) → JwtClaims (base64 decode payload)
├── PublicKey (iss, aud, uidKey, uidVal, pepper)
│   ├── BCS: string fields + bytes(pepper)
│   └── authKey() → SHA3-256(H(iss)||H(aud)||H(uid_key:uid_val)||pepper||0x05)
│
EphemeralKeyPair
├── Ed25519 key pair + expiration + nonce + blinder(31 bytes)
├── isExpired() → Boolean
├── sign(message) → checks expiration, signs with ephemeral key
└── generate(expirationDateSecs) → factory
```

**Authentication Key derivation:**
```
AuthenticationKey = SHA3-256(public_key_bytes || scheme_identifier_byte)
├── fromEd25519(pubkey)       → SHA3-256(32_bytes || 0x00)
├── fromSecp256k1(pubkey)     → SHA3-256(65_bytes || 0x01)  [uncompressed]
├── fromMultiEd25519(pubkey)  → SHA3-256(pk1||...||pkN||threshold || 0x01)
├── fromMultiKey(pubkey)      → SHA3-256(BCS(pubkey) || 0x03)
├── fromKeyless(issH,audH,uidH,pepper) → SHA3-256(issH||audH||uidH||pepper || 0x05)
└── derivedAddress()          → AccountAddress(authKey.data)
```

**Transaction signing flow:**
```
signing_message = RAW_TRANSACTION_PREFIX || BCS(raw_txn)
                  ^^^^^^^^^^^^^^^^^^^^^^^^
                  = SHA3-256("APTOS::RawTransaction")  [pre-computed, lazy]

signature = Ed25519.sign(signing_message) | Secp256k1.sign(signing_message)
```

## Account Design

```
Account (interface)
├── val address: AccountAddress         derived from AuthenticationKey
├── val publicKeyBytes: ByteArray       raw public key for verification
├── val scheme: SignatureScheme          ED25519 or SECP256K1
├── val authenticationKey: AuthKey       SHA3-256(pubkey || scheme) [lazy]
├── fun sign(message): ByteArray        raw signature bytes
└── fun signTransaction(rawTxnBytes)    defaults to sign()

Ed25519Account : Account
├── Companion factories:
│   ├── generate()           → SecureRandom
│   ├── fromPrivateKey()     → derives pubkey + address
│   ├── fromPrivateKeyHex()  → hex parse + fromPrivateKey
│   └── fromMnemonic()       → SLIP-0010 derivation (m/44'/637'/0'/0'/0')
├── signEd25519()            → typed Ed25519.Signature return
└── address = authKey.derivedAddress() (computed at construction)

Secp256k1Account : Account
├── Companion factories:
│   ├── generate()           → SecureRandom (reject invalid scalars)
│   ├── fromPrivateKey()     → derives pubkey (uncompressed) + address
│   ├── fromPrivateKeyHex()  → hex parse + fromPrivateKey
│   └── fromMnemonic()       → BIP-32 derivation (m/44'/637'/0'/0'/0')
├── signSecp256k1()          → typed Secp256k1.Signature return
└── Uses uncompressed (65-byte) public key for auth key derivation

MultiEd25519Account : Account
├── signers: List<Ed25519Account>    participating signers
├── signerIndices: List<Int>          position in full key set
├── multiPublicKey: MultiEd25519.PublicKey  all M keys + threshold
├── sign() → concatenated sigs + 4-byte bitmap
├── signMultiEd25519() → typed MultiEd25519.Signature
└── Companion: create(signers, indices, allPubKeys, threshold)

KeylessAccount : Account
├── ephemeralKeyPair: EphemeralKeyPair    for signing
├── keylessPublicKey: Keyless.PublicKey    OIDC-derived
├── proof: ByteArray                       ZK proof
├── jwt: String                            original token
├── sign() → signs with ephemeral key (checks expiration)
└── Companion: create(ekp, pk, proof, jwt)

AnyAccount (sealed class) : Account
├── data class Ed25519(account: Ed25519Account)
├── data class Secp256k1(account: Secp256k1Account)
├── data class MultiEd25519(account: MultiEd25519Account)
├── data class Keyless(account: KeylessAccount)
└── Companion: from(account: Account) → wraps into correct variant
```

**Mnemonic (BIP-39):**
```
Mnemonic
├── words: List<String>                 12/15/18/21/24 words
├── phrase() → "word1 word2 ..."
├── toSeed(passphrase) → 64 bytes       PBKDF2-HMAC-SHA512, 2048 rounds
├── wordCount() → Int
├── Companion:
│   ├── generate(wordCount=12)           entropy → SHA-256 checksum → words
│   ├── fromPhrase(phrase)               validate + normalize
│   ├── fromEntropy(entropy)             raw entropy → mnemonic
│   ├── validate(words)                  checks count + wordlist membership
│   └── isValid(phrase) → Boolean
└── BIP39_ENGLISH_WORDLIST (2048 words, embedded)

DerivationPath
├── components: List<Component>
│   └── Component(index: UInt, hardened: Boolean)
├── DEFAULT_APTOS = m/44'/637'/0'/0'/0'
├── Companion: parse("m/44'/637'/0'/0'/0'")
└── toString() → "m/44'/637'/0'/0'/0'"
```

## Transaction Building

```
TransactionBuilder (fluent API)
├── .sender(address)                    required
├── .sequenceNumber(seqNum)             required
├── .payload(payload)                   required
├── .chainId(chainId)                   required
├── .maxGasAmount(200_000uL)            default
├── .gasUnitPrice(100uL)                default
├── .expirationTimestampSecs(...)       default: now + 600s
├── .secondarySigners(signers)          multi-agent mode
├── .feePayer(payer)                    fee-payer mode
├── .build() → RawTransaction           throws TransactionBuildException
├── .sign(account) → SignedTransaction  builds + signs (detects mode)
│   ├── feePayer set → FeePayer mode (RawTransactionWithData.FeePayer)
│   ├── secondarySigners set → MultiAgent mode (RawTransactionWithData.MultiAgent)
│   └── else → single signer (existing)
└── Companion: signTransaction(), createAccountAuthenticator()

RawTransactionWithData (sealed class)
├── MultiAgent(rawTxn, secondarySignerAddresses)          variant: 0
├── FeePayer(rawTxn, secondarySignerAddresses, feePayer)  variant: 1
└── signingMessage() → RAW_TRANSACTION_WITH_DATA_PREFIX || BCS(self)

RawTransaction (data class, 7 fields)
├── sender: AccountAddress
├── sequenceNumber: ULong
├── payload: TransactionPayload
├── maxGasAmount: ULong
├── gasUnitPrice: ULong
├── expirationTimestampSecs: ULong
├── chainId: ChainId
├── serialize() → BCS in spec field order (above)
├── signingMessage() → RAW_TRANSACTION_PREFIX || BCS(self)
└── toBcs() → BCS(self) without prefix

SignedTransaction (data class)
├── rawTransaction: RawTransaction
├── authenticator: TransactionAuthenticator
├── toSubmitBytes() → BCS(rawTxn) + BCS(authenticator)
└── hash() → SHA3-256(TRANSACTION_PREFIX || submitBytes)
```

## API Client Design

```
AptosConfig (data class)
├── nodeUrl: String                     full REST API URL
├── faucetUrl: String?                  null for mainnet
├── chainId: ChainId?                   optional, avoids fetching
├── timeoutMs: Long = 30_000
├── retryConfig: RetryConfig
├── pepperServiceUrl: String?           keyless pepper service
├── proverServiceUrl: String?           keyless prover service
│   ├── maxRetries: Int = 3
│   ├── initialDelayMs: Long = 200
│   ├── maxDelayMs: Long = 10_000
│   └── backoffMultiplier: Double = 2.0
└── Companion factories:
    ├── mainnet()   → fullnode.mainnet.aptoslabs.com/v1,   chain_id=1
    ├── testnet()   → fullnode.testnet.aptoslabs.com/v1,   chain_id=2, faucet
    ├── devnet()    → fullnode.devnet.aptoslabs.com/v1,    faucet
    └── localnet()  → localhost:8080/v1, localhost:8081,    chain_id=4

AptosRestClient (suspend functions + blocking wrappers)
├── GET  /                       → getLedgerInfo(): LedgerInfo
├── GET  /accounts/{addr}        → getAccount(addr): AccountInfo
├── GET  /accounts/{a}/resources → getAccountResources(addr): List<AccountResource>
├── GET  /accounts/{a}/resource/{t} → getAccountResource(addr, type): AccountResource
├── GET  /accounts/{a}/transactions → getAccountTransactions(addr, start?, limit?)
├── GET  /accounts/{a}/events/{h}/{f} → getEvents(addr, handle, field, start?, limit?)
├── GET  /transactions/by_hash/{h}  → getTransactionByHash(hash): TransactionResponse
├── POST /transactions           → submitTransaction(signedTxn): PendingTransaction
├── POST /transactions/simulate  → simulateTransaction(signedTxn): List<SimulationResult>
├── GET  /transactions/by_hash/{h} (polling) → waitForTransaction(hash, timeout, interval)
├── GET  /estimate_gas_price     → estimateGasPrice(): GasEstimate
├── POST /view                   → view(function, typeArgs, args): JsonArray
└── Combined: getBalance(addr) → reads CoinStore<AptosCoin> resource → ULong octas

Keyless Clients (in :client module):
├── PepperClient → POST /v0/pepper → getPepper(jwt, epk, uidKey): ByteArray
└── ProverClient → POST /v0/prove  → getProof(jwt, epk, exp, pepper, uidKey): ByteArray

AptosIndexerClient (in :indexer module, opt-in):
├── GraphQL POST to indexer URL
├── getAccountTokens(owner, offset?, limit?): List<Token>
├── getCollections(creator?, offset?, limit?): List<Collection>
├── getAccountTransactionsWithPayload(sender, offset?, limit?): List<IndexerTransaction>
├── getEvents(address?, type?, offset?, limit?): List<IndexerEvent>
└── query(graphqlQuery, variables?): String  (raw GraphQL)

FaucetClient
├── fundAccount(address, amount=100_000_000)
│   POST /fund  → fallback to  POST /mint  (legacy endpoint)
├── fundAccountBlocking() via runBlocking
└── close()

RetryPolicy (object)
├── Retryable HTTP codes: 429, 500, 502, 503, 504
├── Strategy: exponential backoff with random jitter
│   delay = currentDelay + Random(0, currentDelay/2 + 1)
│   currentDelay = min(currentDelay * multiplier, maxDelay)
├── Default: 200ms initial → 2x multiplier → 10s max → 3 retries
└── Only retries ApiException with matching status codes
```

**Response Models** (all `@Serializable`):
| Model | Source Endpoint |
|-------|----------------|
| `LedgerInfo` | `GET /` (chain_id, epoch, ledger_version, etc.) |
| `AccountInfo` | `GET /accounts/{a}` (sequence_number, authentication_key) |
| `AccountResource` | `GET /accounts/{a}/resources` (type, data as JsonObject) |
| `TransactionResponse` | `GET /transactions/*` (hash, sender, success, vm_status, etc.) |
| `PendingTransaction` | `POST /transactions` (hash, sender, seq, max_gas, etc.) |
| `GasEstimate` | `GET /estimate_gas_price` (gas_estimate, prioritized/deprioritized) |
| `ApiError` | Error responses (message, error_code, vm_error_code) |
| `ViewRequest` | `POST /view` body (function, type_arguments, arguments) |

## Java Interop Strategy

Every public API is designed for seamless Java consumption:

```kotlin
// 1. Companion factories → @JvmStatic
companion object {
    @JvmStatic fun fromHex(hex: String): AccountAddress
    @JvmStatic fun generate(): Ed25519Account
    @JvmStatic fun mainnet(): AptosConfig
}

// 2. Default parameters → @JvmOverloads
@JvmOverloads
fun toSeed(passphrase: String = ""): ByteArray

@JvmStatic @JvmOverloads
fun localnet(
    nodeUrl: String = "http://localhost:8080/v1",
    faucetUrl: String = "http://localhost:8081",
): AptosConfig

// 3. Suspend functions → blocking wrappers with @JvmName
suspend fun getBalance(address: AccountAddress): ULong

@JvmName("getBalanceSync")
fun getBalanceBlocking(address: AccountAddress): ULong = runBlocking { getBalance(address) }

// 4. Inline value classes → @JvmInline
@JvmInline value class ChainId(val value: UByte)
```

**Pattern:** Every `suspend fun` in the client/SDK facade has a corresponding `*Blocking` method that wraps `runBlocking { ... }` with `@JvmName` to avoid signature clashes.

## Error Handling Design

All SDK exceptions extend `AptosException` (a `RuntimeException`), enabling both broad and specific catch patterns:

```
AptosException (open class : RuntimeException)
├── message: String
├── cause: Throwable?
│
├── BcsSerializationException     value out of range, negative unsigned
├── BcsDeserializationException   unexpected EOF, invalid boolean byte
├── AccountAddressParseException  empty, too long, invalid hex chars
├── TypeTagParseException         invalid syntax, unknown variant index
├── CryptoException               key gen failure, sign/verify error
├── MnemonicException             invalid word count, unknown words
├── TransactionBuildException     missing required builder fields
└── ApiException                  HTTP/API errors
    ├── statusCode: Int?          HTTP status code (e.g. 404, 429, 500)
    └── errorCode: String?        Aptos error code (e.g. "account_not_found")

ErrorCategory (enum, 12 values)
├── INVALID_INPUT(1)    REQUIRES_ADDRESS(4)   ALREADY_EXISTS(7)    NOT_IMPLEMENTED(10)
├── OUT_OF_RANGE(2)     NOT_FOUND(5)          RESOURCE_EXHAUSTED(8) UNAVAILABLE(11)
├── INVALID_STATE(3)    ABORTED(6)            INTERNAL(9)          PERMISSION_DENIED(12)
└── fromCode(code: Int): ErrorCategory?
```

**Design choice:** Domain-specific exception classes rather than a single exception with category enum. This enables idiomatic Kotlin `when` patterns and Java `catch` blocks:

```kotlin
try {
    val addr = AccountAddress.fromHex(input)
} catch (e: AccountAddressParseException) {
    // specific handling
} catch (e: AptosException) {
    // broad fallback
}
```

## Differences from Reference Implementations

| Aspect | TypeScript SDK | Python SDK | Go SDK | .NET SDK | Kotlin (this) |
|--------|---------------|-----------|--------|----------|---------------|
| Architecture | Facade + 13 mixins | Monolithic RestClient | Separate packages | Namespace-based | Facade + 3 Gradle modules |
| Module system | npm packages | Single package | Go modules | NuGet packages | Maven modules (core/client/sdk) |
| Async model | Promises | async/await (asyncio) | Goroutines/channels | async/await (Task) | Coroutines (suspend) + blocking |
| BCS buffer | Uint8Array + pool | io.BytesIO | bytes.Buffer | MemoryStream | ByteArrayOutputStream |
| Unsigned ints | BigInt (u128/256) | Python int (arbitrary) | math/big.Int | BigInteger | ULong (≤u64), BigInteger (u128/256) |
| Type variants | Union types | Enum + dataclass | Interface + struct | Abstract class | Sealed classes (exhaustive when) |
| Crypto lib | @noble/curves | PyNaCl + ecdsa | crypto/ed25519 | BouncyCastle | Bouncy Castle (JVM) |
| JSON parsing | native JSON | built-in json | encoding/json | System.Text.Json | kotlinx.serialization |
| Null safety | optional chaining | Optional/None | Pointers + error | Nullable refs | Kotlin `?` + smart casts |
| Key erasure | Not implemented | Not implemented | Not implemented | Not implemented | Private key `toString()` = "***" |
| HTTP client | Axios | httpx | net/http | HttpClient | Ktor Client (CIO engine) |
| Test mocking | Jest mocks | pytest + httpx | httptest | Moq | Ktor MockEngine + MockK |

**Notable Kotlin-specific design choices:**

1. **Sealed classes** for TypeTag, TransactionPayload, TransactionAuthenticator, AccountAuthenticator, AnyAccount -- enables exhaustive `when` expressions (compiler-enforced branch coverage)
2. **Inline value class** for ChainId -- zero-overhead type safety at runtime
3. **Data classes** for all value types -- automatic `equals()`, `hashCode()`, `copy()`, destructuring
4. **`by lazy`** for pre-computed hash prefixes and derived addresses -- deferred computation
5. **`@JvmStatic`/`@JvmName`/`@JvmOverloads`** annotations on every public API entry point

## Performance Optimizations

1. **BCS pre-sizing**: 256-byte initial `ByteArrayOutputStream` capacity (typical txn = 200-400 bytes, avoids early resizing)
2. **ByteBuffer.LITTLE_ENDIAN**: Direct little-endian writes for u16/u32/u64 (no manual bit shifting)
3. **Pre-computed hash prefixes**: `RAW_TRANSACTION_PREFIX`, `RAW_TRANSACTION_WITH_DATA_PREFIX`, `TRANSACTION_PREFIX` -- computed once via `by lazy`, reused for every signing operation
4. **Lazy account address**: `authenticationKey` derived via `by lazy` delegate, only computed on first access
5. **Reusable `Charsets.UTF_8`**: Reference for all string encoding, avoids charset lookup
6. **Clear deserialization errors**: Bounds-check before every read with remaining byte count in error message
7. **FixedPointCombMultiplier** for Secp256k1: Constant-time EC point multiplication for key derivation
8. **Ktor connection pooling**: CIO engine reuses HTTP connections across requests

## Spec Coverage

Based on [aptos-sdk-specs](https://github.com/aptos-labs/aptos-sdk-specs), the SDK implements:

| Spec Area | P0 | P1 | P2 | Status |
|-----------|-----|-----|-----|--------|
| BCS Serialization | All primitives, ULEB128, composites | u128/u256, sequences, options | — | Implemented |
| Account Address | fromHex (strict), toHex, toShortString | fromHexRelaxed, constants, BCS | — | Implemented |
| Type Tags | All 11 variants, BCS, toString | fromString parser (recursive) | — | Implemented |
| Ed25519 | Key gen, sign, verify | fromSeed, fromHex | — | Implemented |
| Secp256k1 | Key gen (RFC 6979), sign, verify | Low-S normalization | — | Implemented |
| MultiEd25519 | — | — | N-of-M multi-sig, bitmap, auth key | Implemented |
| MultiKey | — | — | Mixed-type multi-sig (Ed25519+Secp256k1) | Implemented |
| Hashing | SHA3-256, domain separation | SHA-256, pre-computed prefixes | — | Implemented |
| Authentication Key | fromEd25519, derivedAddress | fromSecp256k1 | fromMultiEd25519, fromMultiKey, fromKeyless | Implemented |
| Mnemonic (BIP-39) | generate, fromPhrase, toSeed | Passphrase, validate | — | Implemented |
| Key Derivation | SLIP-0010 (Ed25519) | BIP-32 (Secp256k1) | — | Implemented |
| Transactions | RawTxn, EntryFunction, signing | Builder, SignedTxn, hash | Multi-agent, fee-payer | Implemented |
| REST Client | Ledger, account, submit, wait | Resources, gas, view, balance | Simulation, account txns, events | Implemented |
| Faucet | Fund account | Dual endpoint fallback | — | Implemented |
| Retry | Exponential backoff | Jitter, configurable | — | Implemented |
| Keyless (OIDC) | — | — | JWT parsing, ephemeral keys, pepper/prover | Implemented |
| Indexer | — | — | GraphQL client (tokens, collections, events) | Implemented |
| Benchmarks | — | — | JMH (BCS, Ed25519, Secp256k1, SHA3) | Implemented |

**Not yet implemented (future):**
- Event stream / WebSocket subscriptions
- Sponsored transaction helpers (beyond fee-payer builder)

## Test Strategy

| Test Type | Count | Framework | Approach |
|-----------|-------|-----------|----------|
| BCS unit tests | ~60 | JUnit 5 + Kotest | Serialize/deserialize all types, boundary values |
| BCS spec tests | ~30 | JUnit 5 `@MethodSource` | Test vectors from aptos-sdk-specs |
| Address tests | ~25 | JUnit 5 `@CsvSource`/`@ValueSource` | Parsing, formatting, equality, BCS roundtrip |
| Crypto tests | ~50 | JUnit 5 + Kotest | Key gen, sign/verify, determinism, low-S |
| Account tests | ~30 | JUnit 5 | Generate, from mnemonic, sign transactions |
| Transaction tests | ~40 | JUnit 5 | Builder, BCS encoding, signing message |
| Type tag tests | ~25 | JUnit 5 | Parsing, BCS, recursive struct tags |
| REST client tests | ~40 | Ktor MockEngine | All endpoints, error handling, retry |
| SDK facade tests | ~20 | Ktor MockEngine | High-level flows, Java interop |
| **Total** | **~340** | | |

All tests run via `./gradlew test`. Detekt static analysis via `./gradlew detekt`.

## Project File Structure

```
aptos-kotlin-sdk/
├── settings.gradle.kts                        root project + 5 submodules
├── build.gradle.kts                           plugins, allprojects config
├── gradle.properties                          kotlin.code.style=official
├── gradle/libs.versions.toml                  version catalog (all versions centralized)
├── .gitignore, .editorconfig, detekt.yml
├── .github/workflows/ci.yml                   GitHub Actions (test + detekt)
├── README.md                                  user-facing documentation
│
├── core/
│   ├── build.gradle.kts
│   └── src/
│       ├── main/kotlin/com/aptos/core/
│       │   ├── bcs/           BcsSerializable, BcsSerializer, BcsDeserializer
│       │   ├── types/         AccountAddress, ChainId, TypeTag, StructTag, MoveModuleId, HexString
│       │   ├── crypto/        Ed25519, Secp256k1, MultiEd25519, MultiKey, Keyless,
│       │   │                  EphemeralKeyPair, Hashing, AuthenticationKey, SignatureScheme
│       │   ├── account/       Account, Ed25519Account, Secp256k1Account, MultiEd25519Account,
│       │   │                  KeylessAccount, AnyAccount, Mnemonic, DerivationPath, Slip0010, Bip32
│       │   ├── transaction/   RawTransaction, RawTransactionWithData, TransactionPayload,
│       │   │                  SignedTransaction, TransactionAuthenticator, AccountAuthenticator,
│       │   │                  TransactionBuilder (single/multi-agent/fee-payer)
│       │   └── error/         AptosException hierarchy (8 subclasses), ErrorCategory enum
│       └── test/kotlin/com/aptos/core/
│           ├── bcs/           BcsSerializerTest, BcsDeserializerTest, BcsSpecTest
│           ├── types/         AccountAddressTest, TypeTagTest, AddressSpecTest
│           ├── crypto/        Ed25519Test, Secp256k1Test, MultiEd25519Test, MultiKeyTest,
│           │                  KeylessTest, EphemeralKeyPairTest, HashingTest, AuthenticationKeyTest
│           ├── account/       AccountTest, MultiEd25519AccountTest, KeylessAccountTest, MnemonicTest
│           └── transaction/   TransactionTest, MultiAgentTransactionTest, FeePayerTransactionTest
│
├── client/
│   ├── build.gradle.kts
│   └── src/
│       ├── main/kotlin/com/aptos/client/
│       │   ├── config/   AptosConfig (+ pepper/prover URLs), RetryConfig
│       │   ├── rest/     AptosRestClient (+ simulate, accountTxns, events), Models
│       │   ├── faucet/   FaucetClient
│       │   ├── keyless/  PepperClient, ProverClient
│       │   └── retry/    RetryPolicy
│       └── test/kotlin/com/aptos/client/
│           ├── rest/     AptosRestClientTest, SimulationTest, AccountTransactionsTest, EventsTest
│           ├── faucet/   FaucetClientTest
│           ├── keyless/  KeylessClientTest
│           └── retry/    RetryPolicyTest
│
├── sdk/
│   ├── build.gradle.kts                       + integrationTest source set
│   └── src/
│       ├── main/kotlin/com/aptos/sdk/         Aptos (facade + keyless), AptosBuilder
│       ├── test/kotlin/com/aptos/sdk/         AptosTest
│       └── integrationTest/kotlin/            AccountIT, TransferIT, SimulationIT, MultiAgentIT
│
├── indexer/                                   (opt-in module)
│   ├── build.gradle.kts
│   └── src/
│       ├── main/kotlin/com/aptos/indexer/    IndexerConfig, Models, Queries, AptosIndexerClient
│       └── test/kotlin/com/aptos/indexer/    AptosIndexerClientTest, QueriesTest
│
├── benchmarks/                                (JMH benchmarks, not in kover)
│   ├── build.gradle.kts
│   └── src/jmh/kotlin/com/aptos/benchmarks/  BcsBenchmark, CryptoBenchmark
│
└── docs/
    └── kotlin-DESIGN.md                       ← this file
```
