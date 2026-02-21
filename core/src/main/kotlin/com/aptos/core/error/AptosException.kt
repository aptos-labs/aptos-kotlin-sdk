package com.aptos.core.error

/**
 * Base exception for all Aptos SDK errors.
 *
 * All domain-specific exceptions extend this class, allowing callers to
 * catch `AptosException` for broad error handling or specific subclasses
 * for fine-grained handling.
 */
open class AptosException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

/** Thrown when BCS serialization fails (e.g., value out of range). */
class BcsSerializationException(
    message: String,
    cause: Throwable? = null,
) : AptosException(message, cause)

/** Thrown when BCS deserialization fails (e.g., unexpected end of input, invalid boolean). */
class BcsDeserializationException(
    message: String,
    cause: Throwable? = null,
) : AptosException(message, cause)

/** Thrown when an account address hex string cannot be parsed. */
class AccountAddressParseException(
    message: String,
    cause: Throwable? = null,
) : AptosException(message, cause)

/** Thrown when a type tag or struct tag string cannot be parsed. */
class TypeTagParseException(
    message: String,
    cause: Throwable? = null,
) : AptosException(message, cause)

/** Thrown when a cryptographic operation fails (key generation, signing, verification). */
class CryptoException(
    message: String,
    cause: Throwable? = null,
) : AptosException(message, cause)

/** Thrown for invalid mnemonic phrases or key derivation errors. */
class MnemonicException(
    message: String,
    cause: Throwable? = null,
) : AptosException(message, cause)

/** Thrown when a [com.aptos.core.transaction.TransactionBuilder] is missing required fields. */
class TransactionBuildException(
    message: String,
    cause: Throwable? = null,
) : AptosException(message, cause)

/**
 * Thrown for REST API errors including HTTP failures and on-chain errors.
 *
 * @property statusCode the HTTP status code, or `null` if not an HTTP error
 * @property errorCode the Aptos error code string (e.g., `"account_not_found"`), or `null`
 */
class ApiException(
    message: String,
    val statusCode: Int? = null,
    val errorCode: String? = null,
    cause: Throwable? = null,
) : AptosException(message, cause)
