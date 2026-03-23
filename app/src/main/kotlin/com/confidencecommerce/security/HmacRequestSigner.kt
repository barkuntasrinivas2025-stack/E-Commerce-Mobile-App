package com.confidencecommerce.security

import android.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * HMAC-SHA256 Request Signer — Replay Attack Prevention
 *
 * Implements the request signing design from Phase 2 / Attack Scenario 2
 * of the Security Architecture Hardening Report.
 *
 * HOW IT WORKS:
 *  1. Client builds a canonical string:  "$productId|$timestamp|$nonce"
 *  2. Client signs it with HMAC-SHA256 using the session key
 *  3. Both signature + timestamp are sent as HTTP headers
 *  4. Backend verifies:
 *     a. Timestamp within ±5-minute window (prevents replay)
 *     b. Nonce not seen before in the last 10 minutes (prevents same-timestamp replay)
 *     c. Signature matches canonical string (prevents tampering)
 *
 * KEY SOURCE:
 *  - MVP:        Derived from BuildConfig.API_KEY (acceptable for 7-day test)
 *  - Production: Backend-issued session-bound HMAC key (rotated per-session)
 *                Retrieved via /auth/session-key after Play Integrity attestation
 *
 * WHY THIS PREVENTS PRICE MANIPULATION:
 *  Without request signing, Frida can intercept and replay any request.
 *  With signing, replayed or tampered requests are rejected server-side
 *  because the timestamp will be out of window.
 */
@Singleton
class HmacRequestSigner @Inject constructor(
    private val securePrefs: SecurePreferencesManager
) {
    companion object {
        private const val TAG           = "HmacSigner"
        private const val HMAC_ALGO     = "HmacSHA256"
        private const val REPLAY_WINDOW = 300_000L  // 5 minutes in ms
    }

    data class SignedRequest(
        val timestamp: Long,
        val nonce: String,
        val signature: String
    )

    /**
     * Sign a request for a given resource + parameters.
     *
     * @param resourceId  The primary resource identifier (e.g., productId)
     * @param extraParams Additional query parameters to include in the signature
     *                    (prevents parameter-swapping attacks)
     */
    suspend fun sign(resourceId: String, vararg extraParams: String): SignedRequest {
        val timestamp = System.currentTimeMillis()
        val nonce     = generateNonce()
        val key       = resolveSigningKey()

        // Canonical string includes all mutable parameters to prevent substitution
        val canonical = buildCanonicalString(resourceId, timestamp, nonce, *extraParams)
        val signature = hmacSha256(canonical, key)

        SecureLogger.d(TAG, "Signed request for resource=${resourceId.take(8)}… ts=$timestamp")
        return SignedRequest(timestamp, nonce, signature)
    }

    /**
     * Verify a response signature from the backend.
     * Protects against Frida hooking that injects fake high-confidence price data.
     *
     * See Attack Scenario 2 from the hardening report:
     * "Attacker hooks PriceAnchor.confidenceText → Returns 90% below market"
     */
    suspend fun verifyResponseSignature(
        payload: String,
        serverTimestamp: Long,
        signature: String
    ): Boolean {
        val now = System.currentTimeMillis()

        // 1. Timestamp freshness check — reject stale responses
        if (Math.abs(now - serverTimestamp) > REPLAY_WINDOW) {
            SecureLogger.security(TAG, "Stale response signature rejected. Age=${now - serverTimestamp}ms")
            return false
        }

        // 2. HMAC verification
        val key       = resolveSigningKey()
        val canonical = "$payload|$serverTimestamp"
        val expected  = hmacSha256(canonical, key)
        val valid     = constantTimeEquals(expected, signature)

        if (!valid) {
            SecureLogger.security(TAG, "Response signature MISMATCH — possible tampering")
        }
        return valid
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private fun buildCanonicalString(
        resourceId: String,
        timestamp: Long,
        nonce: String,
        vararg extraParams: String
    ): String = buildString {
        append(resourceId)
        append("|")
        append(timestamp)
        append("|")
        append(nonce)
        extraParams.sorted().forEach { // Sort params for deterministic canonical form
            append("|")
            append(it)
        }
    }

    private fun hmacSha256(data: String, key: ByteArray): String {
        val mac = Mac.getInstance(HMAC_ALGO)
        mac.init(SecretKeySpec(key, HMAC_ALGO))
        val digest = mac.doFinal(data.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(digest, Base64.NO_WRAP)
    }

    /**
     * Constant-time string comparison — prevents timing-based signature oracle attacks.
     * Normal string comparison short-circuits on first mismatch, leaking info.
     */
    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var result = 0
        for (i in a.indices) {
            result = result or (a[i].code xor b[i].code)
        }
        return result == 0
    }

    private fun generateNonce(): String {
        val bytes = ByteArray(16)
        java.security.SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP or Base64.URL_SAFE)
    }

    /**
     * Key resolution:
     * - Production: Retrieve per-session HMAC key from EncryptedSharedPreferences
     *               (previously fetched from backend after attestation)
     * - MVP fallback: Derive from API key using HKDF-like derivation
     */
    private suspend fun resolveSigningKey(): ByteArray {
        // Try session key first (production path)
        val sessionKey = securePrefs.getSessionToken()
        if (!sessionKey.isNullOrBlank()) {
            return sessionKey.toByteArray(Charsets.UTF_8).take(32).toByteArray()
        }

        // MVP fallback: derive from API key
        // SECURITY NOTE: This key is extractable from the APK via jadx.
        // Replace with backend-issued key (Tier 2) before production.
        val rawKey = com.confidencecommerce.BuildConfig.API_KEY.ifBlank {
            "dev-signing-key-replace-in-production"
        }
        val sha256 = java.security.MessageDigest.getInstance("SHA-256")
        return sha256.digest(rawKey.toByteArray(Charsets.UTF_8))
    }
}
