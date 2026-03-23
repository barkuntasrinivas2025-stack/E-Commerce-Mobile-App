package com.confidencecommerce.di

import android.content.Context
import com.confidencecommerce.security.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SecurityModule {

    @Provides @Singleton
    fun provideRuntimeIntegrityShield(
        @ApplicationContext context: Context
    ): RuntimeIntegrityShield = RuntimeIntegrityShield(context)

    @Provides @Singleton
    fun provideHmacRequestSigner(
        securePrefs: SecurePreferencesManager
    ): HmacRequestSigner = HmacRequestSigner(securePrefs)

    @Provides @Singleton
    fun provideSignedPriceVerifier(
        signer: HmacRequestSigner
    ): SignedPriceVerifier = SignedPriceVerifier(signer)

    @Provides @Singleton
    fun provideSecurityMonitor(
        @ApplicationContext context: Context
    ): SecurityMonitor = SecurityMonitor(context)

    @Provides @Singleton
    fun providePlayIntegrityManager(
        @ApplicationContext context: Context,
        securePrefs: SecurePreferencesManager
    ): PlayIntegrityManager = PlayIntegrityManager(context, securePrefs)

    @Provides @Singleton
    fun provideBehavioralAnomalyDetector(): PlayIntegrityManager.BehavioralAnomalyDetector =
        PlayIntegrityManager.BehavioralAnomalyDetector()

    @Provides @Singleton
    fun provideBackendTokenExchange(
        securePrefs: SecurePreferencesManager,
        signer: HmacRequestSigner
    ): BackendTokenExchange = BackendTokenExchange(securePrefs, signer)
}