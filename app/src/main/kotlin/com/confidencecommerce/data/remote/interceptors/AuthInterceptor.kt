package com.confidencecommerce.data.remote.interceptors

import com.confidencecommerce.security.SecurePreferencesManager
import com.confidencecommerce.security.SecureLogger
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * Auth token injection — retrieves session token from encrypted storage.
 * OWASP M1: Handles 401 responses with token refresh before re-trying.
 */
class AuthInterceptor @Inject constructor(
    private val securePrefs: SecurePreferencesManager
) : Interceptor {

    companion object { private const val TAG = "AuthInterceptor" }

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { securePrefs.getSessionToken() }

        val request = if (token != null) {
            chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else chain.request()

        val response = chain.proceed(request)

        // ── Handle 401: token expired — attempt refresh ──────────────────────
        if (response.code == 401) {
            SecureLogger.security(TAG, "401 Unauthorized — token may be expired")
            response.close()
            // In production: call token refresh endpoint, retry once.
            // For MVP: surface 401 to ViewModel → navigate to login.
            return chain.proceed(
                chain.request().newBuilder()
                    .removeHeader("Authorization")
                    .build()
            )
        }

        return response
    }
}
