package com.confidencecommerce.security


import javax.inject.Inject
import javax.inject.Singleton

/**
 * BackendTokenExchange — Tier 2 Secret Architecture (Backend Proxy Pattern)
 *
 * From the Hardening Report:
 * "API key never leaves your backend. App authenticates via short-lived JWT
 *  (15-min expiry). Backend enforces rate limiting per device/user."
 *
 * ARCHITECTURE:
 *
 *  ┌─────────────────┐         ┌──────────────────────┐        ┌──────────────────┐
 *  │  Android App    │──JWT──▶│  Your Backend (BFF)  │──Key──▶│  Price API       │
 *  │                 │        │  (OUR SERVER)        │        │  (3rd party)     │
 *  │  No API keys    │◀──Data─│  Holds API keys      │◀─Data──│                  │
 *  └─────────────────┘        │  Enforces rate limits│        └──────────────────┘
 *                             │  Validates requests  │
 *                             └──────────────────────┘
 *
 * SECURITY PROPERTIES:
 *  ✅ API key NEVER in APK (eliminates Attack Scenario 1 entirely)
 *  ✅ Short-lived JWTs mean leaked tokens expire in 15 minutes
 *  ✅ Backend enforces rate limits server-side (can't be bypassed by Frida)
 *  ✅ Backend validates all inputs again (defence-in-depth for OWASP M4)
 *  ✅ Request signing verified server-side (prevents replay attacks)
 *
 * MIGRATION PATH (from MVP BuildConfig keys):
 *  1. Deploy Backend For Frontend (BFF) endpoint /auth/token
 *  2. BFF authenticates via Firebase Auth or Play Integrity token
 *  3. BFF issues signed JWT (15-min TTL) with rate-limit tier claim
 *  4. App exchanges Play Integrity token → Backend JWT
 *  5. All subsequent API calls use JWT, not raw API key
 *  6. Remove BuildConfig.API_KEY entirely
 *
 * CURRENT STATE: Token contract defined, HTTP layer stubbed for MVP.
 * Replace MockTokenService with real Retrofit call to your BFF.
 */
@Singleton
class BackendTokenExchange @Inject constructor(
    private val securePrefs: SecurePreferencesManager,
    private val signer: HmacRequestSigner
) {

    companion object {
        private const val TAG              = "TokenExchange"
        private const val TOKEN_TTL_MS     = 900_000L   // 15 minutes
        private const val REFRESH_BUFFER   = 60_000L    // Refresh 1 minute before expiry
    }

    data class SessionToken(
        val accessToken: String,
        val expiresAt: Long,             // Unix epoch ms
        val rateLimitTier: RateLimitTier,
        val issuedAt: Long = System.currentTimeMillis()
    ) {
        val isExpired: Boolean
            get() = System.currentTimeMillis() > expiresAt

        val shouldRefresh: Boolean
            get() = System.currentTimeMillis() > (expiresAt - REFRESH_BUFFER)

        enum class RateLimitTier(val requestsPerHour: Int) {
            GUEST(5), AUTHENTICATED(100), ATTESTED(500)
        }
    }

    /**
     * Get a valid session token, refreshing if needed.
     * This is the single entry point for all authenticated API calls.
     *
     * Flow:
     *  1. Check in-memory cache
     *  2. Check EncryptedSharedPreferences
     *  3. Exchange refresh token with backend
     *  4. Fall back to guest tier if all else fails
     */
    suspend fun getValidToken(): SessionToken {
        // Try stored token
        val stored = loadStoredToken()
        if (stored != null && !stored.shouldRefresh) {
            SecureLogger.d(TAG, "Using cached token, expires in ${(stored.expiresAt - System.currentTimeMillis()) / 1000}s")
            return stored
        }

        // Attempt refresh
        return try {
            val fresh = exchangeForFreshToken()
            storeToken(fresh)
            SecureLogger.d(TAG, "Token refreshed. tier=${fresh.rateLimitTier}")
            fresh
        } catch (e: Exception) {
            SecureLogger.w(TAG, "Token refresh failed — downgrading to guest tier", e)
            guestToken()
        }
    }

    /**
     * Exchange a Play Integrity token for a backend-issued JWT.
     *
     * PRODUCTION WIRING:
     * ```kotlin
     * val response = authApi.exchangeIntegrityToken(
     *     IntegrityTokenRequest(
     *         integrityToken = playIntegrityToken,
     *         deviceFingerprint = Build.FINGERPRINT.sha256(),
     *         appVersion = BuildConfig.VERSION_NAME
     *     )
     * )
     * return SessionToken(
     *     accessToken    = response.jwt,
     *     expiresAt      = response.expiresAt,
     *     rateLimitTier  = SessionToken.RateLimitTier.valueOf(response.tier)
     * )
     * ```
     */
    private suspend fun exchangeForFreshToken(): SessionToken {
        // MVP: Return a mock guest token
        // PRODUCTION: Call /auth/token endpoint on your BFF
        SecureLogger.w(TAG, "Token exchange not yet wired — returning mock guest token for MVP")
        return guestToken()
    }

    private fun guestToken() = SessionToken(
        accessToken   = "guest_${System.currentTimeMillis()}",
        expiresAt     = System.currentTimeMillis() + TOKEN_TTL_MS,
        rateLimitTier = SessionToken.RateLimitTier.GUEST
    )

    private suspend fun loadStoredToken(): SessionToken? {
        return try {
            val raw = securePrefs.getSessionToken() ?: return null
            // In production: deserialize JWT claims to extract expiry
            // For MVP: check simple timestamp
            null // Force fresh exchange in MVP
        } catch (e: Exception) {
            SecureLogger.w(TAG, "Failed to load stored token", e)
            null
        }
    }

    private suspend fun storeToken(token: SessionToken) {
        securePrefs.saveSessionToken(token.accessToken)
    }
}
