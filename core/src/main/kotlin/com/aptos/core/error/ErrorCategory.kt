package com.aptos.core.error

/**
 * Maps on-chain Move VM abort codes to human-readable categories.
 *
 * The Aptos Move framework uses numeric category codes (1--12) in abort errors.
 * Use [fromCode] to convert an abort code to its corresponding category.
 */
enum class ErrorCategory(val code: Int, val description: String) {
    INVALID_INPUT(1, "Invalid argument provided"),
    OUT_OF_RANGE(2, "Value out of valid range"),
    INVALID_STATE(3, "Resource in invalid state for operation"),
    REQUIRES_ADDRESS(4, "Operation requires specific address"),
    NOT_FOUND(5, "Resource not found"),
    ABORTED(6, "Transaction aborted"),
    ALREADY_EXISTS(7, "Resource already exists"),
    RESOURCE_EXHAUSTED(8, "Resource limit exceeded"),
    INTERNAL(9, "Internal error"),
    NOT_IMPLEMENTED(10, "Feature not implemented"),
    UNAVAILABLE(11, "Service temporarily unavailable"),
    PERMISSION_DENIED(12, "Insufficient permissions"),
    ;

    companion object {
        /**
         * Returns the [ErrorCategory] for the given numeric abort code, or `null` if unknown.
         *
         * @param code the numeric abort category code (1--12)
         * @return the matching category, or `null`
         */
        @JvmStatic
        fun fromCode(code: Int): ErrorCategory? = entries.find { it.code == code }
    }
}
