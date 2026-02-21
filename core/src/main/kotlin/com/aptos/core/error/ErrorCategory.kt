package com.aptos.core.error

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
    PERMISSION_DENIED(12, "Insufficient permissions");

    companion object {
        @JvmStatic
        fun fromCode(code: Int): ErrorCategory? = entries.find { it.code == code }
    }
}
