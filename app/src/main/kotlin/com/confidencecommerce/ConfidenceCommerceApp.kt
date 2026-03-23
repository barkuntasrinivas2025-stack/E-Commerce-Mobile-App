package com.confidencecommerce

import android.app.Application
import com.confidencecommerce.security.SecureLogger
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ConfidenceCommerceApp : Application() {
    override fun onCreate() {
        super.onCreate()
        SecureLogger.initialize(isDebug = BuildConfig.IS_DEBUG_BUILD)
    }
}
