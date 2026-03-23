package com.confidencecommerce.data.remote.interceptors

import com.confidencecommerce.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response
import java.util.UUID

/**
 * Adds security headers to every outbound request.
 * API key loaded from BuildConfig (gradle.properties → CI secrets).
 */
class SecurityHeadersInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header("X-Api-Key",              BuildConfig.API_KEY)
            .header("Content-Type",           "application/json; charset=utf-8")
            .header("Accept",                 "application/json")
            .header("X-Content-Type-Options", "nosniff")
            .header("X-Frame-Options",        "DENY")
            .header("Cache-Control",          "no-store, no-cache")
            .header("X-Client-Platform",      "android")
            .header("X-Client-Version",       BuildConfig.VERSION_NAME)
            .header("X-Request-Id",           UUID.randomUUID().toString())
            .build()
        return chain.proceed(request)
    }
}
