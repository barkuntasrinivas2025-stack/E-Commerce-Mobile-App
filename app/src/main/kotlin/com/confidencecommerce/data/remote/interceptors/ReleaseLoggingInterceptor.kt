package com.confidencecommerce.data.remote.interceptors

import com.confidencecommerce.BuildConfig
import com.confidencecommerce.security.InputValidator
import com.confidencecommerce.security.SecureLogger
import okhttp3.Interceptor
import okhttp3.Response

/**
 * OWASP M3 + M7: Conditional request logging.
 *
 * - DEBUG: Logs method, URL path (no query params to avoid PII leakage), status code.
 * - RELEASE: Logs ONLY errors (4xx/5xx) without any request body content.
 *
 * OkHttp's HttpLoggingInterceptor is added via debugImplementation ONLY
 * (see app/build.gradle.kts) so it never ships in release APK.
 */
class ReleaseLoggingInterceptor : Interceptor {

    companion object { private const val TAG = "Network" }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val startMs = System.currentTimeMillis()

        val response = chain.proceed(request)
        val durationMs = System.currentTimeMillis() - startMs

        val safePath = InputValidator.sanitizeForLogging(request.url.encodedPath)

        when {
            BuildConfig.IS_DEBUG_BUILD -> {
                SecureLogger.d(TAG, "${request.method} $safePath → ${response.code} (${durationMs}ms)")
            }
            response.code >= 400 -> {
                // Release: log errors for Crashlytics monitoring (no body, no PII)
                SecureLogger.w(TAG, "HTTP ${response.code} on ${request.method} $safePath (${durationMs}ms)")
            }
        }

        return response
    }
}
