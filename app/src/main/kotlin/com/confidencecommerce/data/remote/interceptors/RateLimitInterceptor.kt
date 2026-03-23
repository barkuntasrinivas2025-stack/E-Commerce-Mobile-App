package com.confidencecommerce.data.remote.interceptors

import com.confidencecommerce.security.PlayIntegrityManager
import com.confidencecommerce.security.SecureLogger
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.Protocol
import okio.Buffer
import javax.inject.Inject

/**
 * RateLimitInterceptor — Client-side rate limiting enforcement
 *
 * Implements the "Rate limiting placeholder" from the Hardening Report.
 * Provides user-visible feedback BEFORE a server-side 429 occurs.
 *
 * Auth tier → requests/hour mapping (from PlayIntegrityManager):
 *   GUEST        →  5/hour
 *   AUTHENTICATED→ 100/hour
 *   ATTESTED     → 500/hour
 *
 * NOTE: Client-side limiting is UX-only. Server-side enforcement is authoritative.
 * This interceptor prevents the user from burning their quota by accident.
 */
class RateLimitInterceptor @Inject constructor(
    private val playIntegrityManager: PlayIntegrityManager,
    private val anomalyDetector: PlayIntegrityManager.BehavioralAnomalyDetector
) : Interceptor {

    companion object {
        private const val TAG = "RateLimitInterceptor"
        private val RATE_LIMITED_PATHS = listOf("/price-comparison")
    }

    // ── Current auth state — updated by AuthInterceptor on successful auth ────
    // Default to Guest tier (most restrictive)
    @Volatile var currentAuthState: PlayIntegrityManager.AdaptiveAuthState =
        PlayIntegrityManager.AdaptiveAuthState.Guest

    override fun intercept(chain: Interceptor.Chain): Response {
        val path = chain.request().url.encodedPath

        // Only rate-limit high-value endpoints
        if (RATE_LIMITED_PATHS.none { path.startsWith(it) }) {
            return chain.proceed(chain.request())
        }

        // Behavioral anomaly check
        val anomaly = anomalyDetector.recordRequest()
        if (anomaly is PlayIntegrityManager.BehavioralAnomalyDetector.AnomalySignal.BotPattern ||
            anomaly is PlayIntegrityManager.BehavioralAnomalyDetector.AnomalySignal.BurstPattern) {
            SecureLogger.security(TAG, "Anomaly detected: ${(anomaly as? PlayIntegrityManager.BehavioralAnomalyDetector.AnomalySignal.BotPattern)?.detail}")
        }

        // Rate limit check
        val limiter = playIntegrityManager.getRateLimiterFor(currentAuthState)
        return when (val result = limiter.checkAndRecord()) {
            is PlayIntegrityManager.ClientRateLimiter.RateLimitResult.Throttled -> {
                SecureLogger.w(TAG,
                    "Rate limit reached: ${result.requestsUsed}/${result.requestsMax} " +
                    "resets in ${result.resetInMinutes}m")
                syntheticRateLimitResponse(chain, result)
            }
            is PlayIntegrityManager.ClientRateLimiter.RateLimitResult.Allowed -> {
                chain.proceed(chain.request())
            }
        }
    }

    /** Return a synthetic 429 response to the caller without making a network call. */
    private fun syntheticRateLimitResponse(
        chain: Interceptor.Chain,
        result: PlayIntegrityManager.ClientRateLimiter.RateLimitResult.Throttled
    ): Response = Response.Builder()
        .request(chain.request())
        .protocol(Protocol.HTTP_1_1)
        .code(429)
        .message("Too Many Requests (client-side rate limit)")
        .header("X-RateLimit-Limit",     result.requestsMax.toString())
        .header("X-RateLimit-Remaining", "0")
        .header("X-RateLimit-Reset",     ((System.currentTimeMillis() + result.resetInMillis) / 1000).toString())
        .header("Retry-After",           (result.resetInMillis / 1000).toString())
        .body(okhttp3.ResponseBody.create(
            okhttp3.MediaType.parse("application/json"),
            """{"error":"RATE_LIMIT_EXCEEDED","reset_in_minutes":${result.resetInMinutes}}"""
        ))
        .build()
}
