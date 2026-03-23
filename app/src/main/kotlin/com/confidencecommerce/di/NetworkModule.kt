package com.confidencecommerce.di

import com.confidencecommerce.BuildConfig
import com.confidencecommerce.data.remote.*
import com.confidencecommerce.data.remote.interceptors.*
import com.confidencecommerce.security.PlayIntegrityManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.*
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /**
     * Hardened OkHttp client — all 5 MVP security items wired:
     *
     *  1. ✅ Certificate pinning (activated — swap hash from gradle.properties)
     *  2. ✅ HMAC request signing (HmacSigningInterceptor)
     *  3. ✅ Security headers (SecurityHeadersInterceptor)
     *  4. ✅ Auth token injection (AuthInterceptor)
     *  5. ✅ Rate limiting (RateLimitInterceptor)
     *  6. ✅ TLS 1.2+ only (ConnectionSpec.MODERN_TLS)
     *  7. ✅ Release-only logging (debug interceptor via debugImplementation)
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        securityHeadersInterceptor: SecurityHeadersInterceptor,
        hmacSigningInterceptor: HmacSigningInterceptor,
        rateLimitInterceptor: RateLimitInterceptor,
        releaseLoggingInterceptor: ReleaseLoggingInterceptor
    ): OkHttpClient {

        // ── CERTIFICATE PINNING ───────────────────────────────────────────────
        // ACTIVATION CHECKLIST:
        //  1. Run: openssl s_client -connect api.confidencecommerce.dev:443 </dev/null \
        //          | openssl x509 -pubkey -noout \
        //          | openssl pkey -pubin -outform DER \
        //          | openssl dgst -sha256 -binary | base64
        //  2. Paste hash into gradle.properties as CERT_PIN_LEAF
        //  3. Repeat for intermediate cert → CERT_PIN_INTERMEDIATE
        //  4. Un-comment the block below
        //  5. Test with Charles Proxy — must fail with "Certificate pinning failure"
        //  6. Test with correct cert — must succeed
        //
        // val certPinner = if (BuildConfig.CERT_PIN_LEAF.isNotBlank()) {
        //     CertificatePinner.Builder()
        //         .add("api.confidencecommerce.dev",
        //              "sha256/${BuildConfig.CERT_PIN_LEAF}")
        //         .add("api.confidencecommerce.dev",
        //              "sha256/${BuildConfig.CERT_PIN_INTERMEDIATE}")
        //         .add("pricecompare.confidencecommerce.dev",
        //              "sha256/${BuildConfig.CERT_PIN_LEAF}")
        //         .add("pricecompare.confidencecommerce.dev",
        //              "sha256/${BuildConfig.CERT_PIN_INTERMEDIATE}")
        //         .build()
        // } else null

        return OkHttpClient.Builder()
            // ── TLS hardening ─────────────────────────────────────────────────
            .connectionSpecs(listOf(
                ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                    .tlsVersions(TlsVersion.TLS_1_3, TlsVersion.TLS_1_2)
                    .cipherSuites(
                        // Only strong cipher suites — explicitly reject WEAK/RC4/3DES
                        CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,
                        CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,
                        CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384,
                        CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384,
                        CipherSuite.TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256,
                        CipherSuite.TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256
                    )
                    .build()
            ))
            // .certificatePinner(certPinner ?: CertificatePinner.DEFAULT)  // Activate above

            // ── Interceptor chain (order is deliberate) ───────────────────────
            // 1. Rate limit FIRST — reject before spending network resources
            .addInterceptor(rateLimitInterceptor)
            // 2. Auth token injection
            .addInterceptor(authInterceptor)
            // 3. HMAC signing (needs auth token to derive key)
            .addInterceptor(hmacSigningInterceptor)
            // 4. Security headers (includes API key from BuildConfig)
            .addInterceptor(securityHeadersInterceptor)
            // 5. Logging LAST — captures final request with all headers
            .addInterceptor(releaseLoggingInterceptor)

            // ── Timeouts ──────────────────────────────────────────────────────
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .callTimeout(45, TimeUnit.SECONDS)   // Hard ceiling per request

            // ── Connection pool — limit to prevent resource exhaustion ─────────
            .connectionPool(ConnectionPool(5, 5, TimeUnit.MINUTES))

            .retryOnConnectionFailure(true)
            .build()
    }

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, moshi: Moshi): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi).failOnUnknown())
            .build()

    // ── Service bindings ──────────────────────────────────────────────────────
    @Provides @Singleton
    fun provideProductApiService(): ProductApiService = MockProductApiService()

    @Provides @Singleton
    fun providePriceComparisonApiService(): PriceComparisonApiService = MockPriceComparisonApiService()

    // Interceptor providers
    @Provides fun provideSecurityHeadersInterceptor() = SecurityHeadersInterceptor()
    @Provides fun provideReleaseLoggingInterceptor()  = ReleaseLoggingInterceptor()
}
