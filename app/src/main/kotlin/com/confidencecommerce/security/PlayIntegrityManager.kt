package com.confidencecommerce.security

import android.content.Context
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PlayIntegrityManager — Device Attestation (Tier 3 Secret Architecture)
 *
 * Implements the Play Integrity API (successor to SafetyNet) to verify:
 *  1. The device has not been tampered with (bootloader locked, certified OS)
 *  2. The app has not been tampered with (Google-recognized APK)
 *  3. The user's account license (for licensed features)
 *
 * ATTACK SCENARIO 3 (Hardening Report):
 * "Emulator farm, 100 instances, scripted price enumeration"
 * Play Integrity blocks all emulators and uncertified devices.
 *
 * ADAPTIVE AUTH STATES (from Hardening Report):
 *   Guest      → 5 price checks/hour (no attestation)
 *   Authed     → 100 price checks/hour (phone OTP)
 *   Attested   → 500 price checks/hour (Play Integrity passes)
 *
 * DEPENDENCY: Add to app/build.gradle.kts:
 *   implementation("com.google.android.play:integrity:1.3.0")
 *
 * CURRENT STATE: Architecture scaffolding — real API calls wired once
 * the dependency is added. The AdaptiveAuthState machine works today.
 */
@Singleton
class PlayIntegrityManager @Inject constructor(
    private val context: Context,
    private val securePrefs: SecurePreferencesManager
) {

    companion object {
        private const val TAG              = "PlayIntegrity"
        private const val ATTEST_TTL_MS    = 3_600_000L  // Re-attest every hour
        private const val PREFS_ATTEST_AT  = "last_attestation_timestamp"
        private const val PREFS_ATTEST_OK  = "attestation_passed"
    }

    // ── Adaptive Auth State ────────────────────────────────────────────────────

    /**
     * Auth states drive rate limiting tiers as specified in the hardening report.
     * The rate limits are enforced server-side; this model drives client-side
     * request throttling and UI affordances.
     */
    sealed class AdaptiveAuthState {
        /** Unauthenticated — 5 price requests/hour. No identifying information. */
        object Guest : AdaptiveAuthState() {
            val requestsPerHour   = 5
            val requiresOtp       = false
            val requiresAttestation = false
        }

        /** OTP-verified — 100 price requests/hour. Firebase Auth session. */
        data class Authenticated(val userId: String) : AdaptiveAuthState() {
            val requestsPerHour   = 100
            val requiresAttestation = false
        }

        /** Play Integrity verified — 500 price requests/hour. */
        data class Attested(val userId: String, val deviceToken: String) : AdaptiveAuthState() {
            val requestsPerHour = 500
        }
    }

    // ── Client-Side Rate Limiter ───────────────────────────────────────────────

    /**
     * Client-side rate limiting placeholder (enforced server-side in production).
     * Provides UX feedback before a server 429 response occurs.
     *
     * Implementation follows the Hardening Report recommendation:
     * "Rate limit placeholder in PriceRepository — commented, ready to enable"
     */
    class ClientRateLimiter(private val maxPerHour: Int) {

        private val requestTimestamps = ArrayDeque<Long>()
        private val windowMs = 3_600_000L  // 1 hour

        @Synchronized
        fun checkAndRecord(): RateLimitResult {
            val now = System.currentTimeMillis()

            // Evict timestamps outside the 1-hour window
            while (requestTimestamps.isNotEmpty() &&
                   now - requestTimestamps.first() > windowMs) {
                requestTimestamps.removeFirst()
            }

            return if (requestTimestamps.size >= maxPerHour) {
                val resetAt = requestTimestamps.first() + windowMs
                val waitMs  = resetAt - now
                RateLimitResult.Throttled(
                    requestsUsed  = requestTimestamps.size,
                    requestsMax   = maxPerHour,
                    resetInMillis = waitMs
                )
            } else {
                requestTimestamps.addLast(now)
                RateLimitResult.Allowed(
                    requestsUsed = requestTimestamps.size,
                    requestsMax  = maxPerHour
                )
            }
        }

        fun reset() = requestTimestamps.clear()

        sealed class RateLimitResult {
            data class Allowed(val requestsUsed: Int, val requestsMax: Int) : RateLimitResult()
            data class Throttled(
                val requestsUsed: Int,
                val requestsMax: Int,
                val resetInMillis: Long
            ) : RateLimitResult() {
                val resetInMinutes: Long get() = resetInMillis / 60_000
            }
        }
    }

    // ── Rate limiters keyed by auth state ────────────────────────────────────
    private val guestLimiter         = ClientRateLimiter(5)
    private val authenticatedLimiter = ClientRateLimiter(100)
    private val attestedLimiter      = ClientRateLimiter(500)

    fun getRateLimiterFor(state: AdaptiveAuthState): ClientRateLimiter = when (state) {
        is AdaptiveAuthState.Guest         -> guestLimiter
        is AdaptiveAuthState.Authenticated -> authenticatedLimiter
        is AdaptiveAuthState.Attested      -> attestedLimiter
    }

    // ── Play Integrity Attestation ────────────────────────────────────────────

    /**
     * Request a fresh Play Integrity token.
     *
     * PRODUCTION WIRING:
     * ```kotlin
     * val manager = IntegrityManagerFactory.create(context)
     * val nonce   = generateSecureNonce()  // from your backend — prevents replay
     * val request = StandardIntegrityTokenRequest.newBuilder()
     *     .setRequestHash(nonce)
     *     .build()
     * manager.requestCaptureAndExchangeToken(request)
     *     .addOnSuccessListener { response ->
     *         val token = response.token()
     *         // Send token to your backend for server-side verification
     *         // Backend calls: https://playintegrity.googleapis.com/v1/{package}:decodeIntegrityToken
     *         // Receive back: attestationVerdict { deviceIntegrity, appIntegrity, accountDetails }
     *     }
     * ```
     *
     * Add dependency: implementation("com.google.android.play:integrity:1.3.0")
     */
    suspend fun requestAttestation(nonce: String): AttestationResult {
        SecureLogger.i(TAG, "Requesting Play Integrity attestation")

        // ── PRODUCTION: Replace block below with real Play Integrity API call ──
        // See wiring instructions above.
        // For MVP, return UNAVAILABLE so auth downgrades to Authenticated gracefully.
        return AttestationResult.ServiceUnavailable("Play Integrity not yet wired for MVP")
    }

    sealed class AttestationResult {
        data class Passed(val token: String, val verdict: DeviceVerdict) : AttestationResult()
        data class Failed(val reason: String) : AttestationResult()
        data class ServiceUnavailable(val reason: String) : AttestationResult()
    }

    data class DeviceVerdict(
        val meetsDeviceIntegrity: Boolean,   // Certified hardware
        val meetsBasicIntegrity: Boolean,    // Not obviously tampered
        val meetsStrongIntegrity: Boolean,   // Hardware attestation passes
        val appRecognized: Boolean           // APK matches Play Store version
    )

    // ── Anomaly Detection ─────────────────────────────────────────────────────

    /**
     * Behavioral anomaly detection — flags bot-like request patterns.
     * Implements the Hardening Report recommendation:
     * "If all requests are exactly 1 second apart → Flag as bot"
     */
    class BehavioralAnomalyDetector {
        private val intervals = ArrayDeque<Long>()
        private var lastRequestTime = 0L

        fun recordRequest(): AnomalySignal {
            val now = System.currentTimeMillis()
            if (lastRequestTime > 0) {
                intervals.addLast(now - lastRequestTime)
                if (intervals.size > 20) intervals.removeFirst()
            }
            lastRequestTime = now

            return analyze()
        }

        private fun analyze(): AnomalySignal {
            if (intervals.size < 5) return AnomalySignal.Insufficient

            val avg    = intervals.average()
            val stdDev = Math.sqrt(intervals.map { (it - avg).let { d -> d * d } }.average())

            return when {
                // Perfectly regular intervals = automation script
                stdDev < 100 && avg < 2000 -> AnomalySignal.BotPattern(
                    "Suspiciously regular request intervals: avg=${avg.toLong()}ms, stdDev=${stdDev.toLong()}ms"
                )
                // Burst pattern (>20 requests/minute at any point)
                intervals.takeLast(5).all { it < 3000 } -> AnomalySignal.BurstPattern(
                    "Burst detected: 5 requests in <15 seconds"
                )
                else -> AnomalySignal.Normal
            }
        }

        sealed class AnomalySignal {
            object Normal      : AnomalySignal()
            object Insufficient: AnomalySignal()
            data class BotPattern(val detail: String)   : AnomalySignal()
            data class BurstPattern(val detail: String) : AnomalySignal()
        }
    }
}
