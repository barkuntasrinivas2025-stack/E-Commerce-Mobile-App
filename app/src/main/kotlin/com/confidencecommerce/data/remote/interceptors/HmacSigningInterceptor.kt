package com.confidencecommerce.data.remote.interceptors

import com.confidencecommerce.security.HmacRequestSigner
import com.confidencecommerce.security.SecureLogger
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * HmacSigningInterceptor — Attaches HMAC signature to every price API request.
 *
 * Implements the Hardening Report's request signing protocol:
 *   X-Timestamp: 1711091234567
 *   X-Nonce:     abc123==
 *   X-Signature: Base64(HMAC_SHA256("productId|timestamp|nonce", sessionKey))
 *
 * The backend verifies:
 *   1. Timestamp within ±5-minute window (replay prevention)
 *   2. Nonce not seen before in the last 10 minutes (double-submit prevention)
 *   3. Signature matches canonical string (tamper prevention)
 *
 * Applied ONLY to price comparison endpoints — product endpoints
 * are lower-value targets and sign overhead adds latency.
 */
class HmacSigningInterceptor @Inject constructor(
    private val signer: HmacRequestSigner
) : Interceptor {

    companion object {
        private const val TAG = "HmacInterceptor"
        // Only sign these path prefixes — avoids overhead on low-value endpoints
        private val SIGNED_PATH_PREFIXES = listOf("/price-comparison", "/cart", "/checkout")
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path    = request.url.encodedPath

        // Only sign sensitive endpoints
        val shouldSign = SIGNED_PATH_PREFIXES.any { path.startsWith(it) }
        if (!shouldSign) return chain.proceed(request)

        return try {
            // Extract resource ID from path (e.g. /price-comparison/prod_001 → prod_001)
            val resourceId = path.substringAfterLast("/").takeIf { it.isNotBlank() }
                ?: "unknown"

            val signed = runBlocking {
                signer.sign(resourceId, request.method)
            }

            val signedRequest = request.newBuilder()
                .header("X-Timestamp",  signed.timestamp.toString())
                .header("X-Nonce",      signed.nonce)
                .header("X-Signature",  signed.signature)
                .build()

            SecureLogger.d(TAG, "Signed request: $path ts=${signed.timestamp}")
            chain.proceed(signedRequest)
        } catch (e: Exception) {
            SecureLogger.e(TAG, "Request signing failed — proceeding unsigned", e)
            // Fail open for MVP — fail closed (reject) in production
            chain.proceed(request)
        }
    }
}
