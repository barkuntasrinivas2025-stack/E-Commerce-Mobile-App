package com.confidencecommerce.security

import com.confidencecommerce.domain.model.PriceComparison
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SignedPriceResponse — Price Manipulation Prevention
 *
 * Addresses Attack Scenario 2 from the Hardening Report:
 * "Attacker uses Frida to hook PriceAnchor.confidenceText → Returns
 *  '90% below market' for any product"
 *
 * DEFENSE: Backend signs each price response with HMAC-SHA256.
 * Client verifies signature before rendering. Manipulated responses
 * will fail signature verification and show a neutral "Price unavailable"
 * state instead of the attacker's injected value.
 *
 * PROTOCOL:
 *  Server sends: { data: PriceComparisonDto, signature: "base64...", timestamp: 1234567890 }
 *  Client verifies: HMAC(data.toCanonical() + "|" + timestamp, sessionKey) == signature
 *  If mismatch → emit PriceVerificationState.TAMPERED → show warning UI
 *
 * CURRENT STATE: Verification logic implemented, backend signing is mocked.
 * Wire to real backend BEFORE handling real transactions (see Hardening Report).
 */
@Singleton
class SignedPriceVerifier @Inject constructor(
    private val signer: HmacRequestSigner
) {

    sealed class VerificationResult {
        data class Verified(val data: PriceComparison)     : VerificationResult()
        object SignatureMissing                             : VerificationResult()
        object SignatureTampered                            : VerificationResult()
        object SignatureExpired                             : VerificationResult()
    }

    /**
     * Verify a price comparison response before displaying it.
     *
     * @param data      The price comparison domain model
     * @param signature Server-provided HMAC signature (null in MVP mock)
     * @param timestamp Unix epoch milliseconds from server response
     */
    suspend fun verify(
        data: PriceComparison,
        signature: String?,
        timestamp: Long
    ): VerificationResult {
        // ── MVP Mode: no signature from mock backend ────────────────────────
        // In production this block MUST be removed. The absence of a signature
        // should be treated as UNSIGNED, not VERIFIED.
        if (signature == null) {
            SecureLogger.w("PriceVerifier", "No signature present — running in MVP unverified mode")
            return VerificationResult.Verified(data)
        }

        // ── Timestamp window check ──────────────────────────────────────────
        val ageMs = System.currentTimeMillis() - timestamp
        if (ageMs > 300_000L || ageMs < -10_000L) {  // 5-minute forward, 10s backward tolerance
            SecureLogger.security("PriceVerifier", "Expired price signature. Age=${ageMs}ms")
            return VerificationResult.SignatureExpired
        }

        // ── HMAC Verification ───────────────────────────────────────────────
        val canonicalPayload = buildCanonicalPayload(data)
        val isValid = signer.verifyResponseSignature(canonicalPayload, timestamp, signature)

        return if (isValid) {
            VerificationResult.Verified(data)
        } else {
            SecureLogger.security("PriceVerifier",
                "PRICE TAMPER DETECTED productId=${data.productId}")
            VerificationResult.SignatureTampered
        }
    }

    /**
     * Build canonical string from price data — must match backend's canonical form exactly.
     * Include all fields that matter for business logic. Order is deterministic.
     *
     * Changes to this format MUST be versioned and coordinated with the backend team.
     */
    private fun buildCanonicalPayload(data: PriceComparison): String =
        "${data.productId}|" +
        "${"%.2f".format(data.currentPrice.amount)}|" +
        "${"%.2f".format(data.marketAveragePrice.amount)}|" +
        "${data.storeCount}|" +
        "${data.confidenceScore}|" +
        "${data.pricePercentile}"
}
