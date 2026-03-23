package com.confidencecommerce.security

import java.util.regex.Pattern

/**
 * OWASP M4: Insufficient Input/Output Validation.
 * Central validator — every user input routes through here before
 * touching API calls or local storage.
 */
object InputValidator {

    private val EMAIL_RE    = Pattern.compile("^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,63}$")
    private val PHONE_RE    = Pattern.compile("^[+]?[0-9]{10,15}$")
    private val PRODUCT_ID  = Pattern.compile("^[a-zA-Z0-9_\\-]{1,64}$")
    private val PINCODE_RE  = Pattern.compile("^[0-9]{6}$")
    private val SQL_INJECT  = Regex("(?i)(union\\s+select|drop\\s+table|insert\\s+into|delete\\s+from|--|;\\s*$|exec\\s*\\()")
    private val LOG_CTRL    = Regex("[\\r\\n\\t]")

    sealed class Result {
        object Valid : Result()
        data class Invalid(val reason: String) : Result()
    }

    fun validateEmail(email: String): Result {
        val t = email.trim()
        if (t.isBlank()) return Result.Invalid("Email cannot be empty")
        if (t.length > 254) return Result.Invalid("Email too long")
        return if (EMAIL_RE.matcher(t).matches()) Result.Valid else Result.Invalid("Invalid email format")
    }

    fun validateProductId(id: String): Result {
        if (id.isBlank()) return Result.Invalid("Product ID cannot be empty")
        if (id.hasSqlInjection()) return Result.Invalid("Invalid product ID")
        return if (PRODUCT_ID.matcher(id).matches()) Result.Valid else Result.Invalid("Invalid product ID format")
    }

    fun validateQuantity(qty: Int): Result =
        if (qty in 1..99) Result.Valid else Result.Invalid("Quantity must be 1-99")

    fun validatePincode(pin: String): Result =
        if (PINCODE_RE.matcher(pin.trim()).matches()) Result.Valid else Result.Invalid("Enter a valid 6-digit pincode")

    fun validateSearchQuery(q: String): Result {
        val t = q.trim()
        if (t.isBlank()) return Result.Invalid("Search cannot be empty")
        if (t.length > 200) return Result.Invalid("Query too long")
        if (t.hasSqlInjection()) return Result.Invalid("Invalid search query")
        return Result.Valid
    }

    /** Strip control chars + PII before writing to any log. */
    fun sanitizeForLogging(input: String): String =
        input
            .replace(LOG_CTRL, " ")
            .take(256)
            .replace(EMAIL_RE.toRegex(), "[EMAIL]")
            .replace(Regex("\\b\\d{4}[- ]?\\d{4}[- ]?\\d{4}[- ]?\\d{4}\\b"), "[CARD]")

    private fun String.hasSqlInjection(): Boolean = SQL_INJECT.containsMatchIn(this)
    private fun Pattern.toRegex(): Regex = Regex(this.pattern())
}
