package com.aptos.core.error

/**
 * Base exception for all Aptos SDK errors.
 */
open class AptosException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

class BcsSerializationException(
    message: String,
    cause: Throwable? = null,
) : AptosException(message, cause)

class BcsDeserializationException(
    message: String,
    cause: Throwable? = null,
) : AptosException(message, cause)

class AccountAddressParseException(
    message: String,
    cause: Throwable? = null,
) : AptosException(message, cause)

class TypeTagParseException(
    message: String,
    cause: Throwable? = null,
) : AptosException(message, cause)

class CryptoException(
    message: String,
    cause: Throwable? = null,
) : AptosException(message, cause)

class MnemonicException(
    message: String,
    cause: Throwable? = null,
) : AptosException(message, cause)

class TransactionBuildException(
    message: String,
    cause: Throwable? = null,
) : AptosException(message, cause)

class ApiException(
    message: String,
    val statusCode: Int? = null,
    val errorCode: String? = null,
    cause: Throwable? = null,
) : AptosException(message, cause)
