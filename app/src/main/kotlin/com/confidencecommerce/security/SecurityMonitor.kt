package com.confidencecommerce.security

import android.content.Context
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Security event monitor. Logs structured security events.
 * Wire to Crashlytics / Datadog in production.
 */
@Singleton
class SecurityMonitor @Inject constructor(
    private val context: Context
) {
    private val validationFailures = ArrayDeque<Long>()
    private val priceCheckTimestamps = ArrayDeque<Long>()
    private var sessionApiCallCount   = 0
    private var sessionSecurityEvents = 0

    companion object {
        private const val VALIDATION_THRESHOLD = 50
        private const val WINDOW_MS = 600_000L
    }

    fun onValidationFailure(field: String, reason: String) {
        val now = System.currentTimeMillis()
        validationFailures.addLast(now)
        pruneWindow(validationFailures)
        SecureLogger.security("Monitor", "ValidationFailure field=$field count=${validationFailures.size}")
    }

    fun onPriceCheckRequest(productId: String) {
        sessionApiCallCount++
        val now = System.currentTimeMillis()
        priceCheckTimestamps.addLast(now)
        pruneWindow(priceCheckTimestamps)
    }

    fun onIntegrityScanComplete(report: RuntimeIntegrityShield.IntegrityReport) {
        if (report.isThreatened) {
            SecureLogger.security("Monitor", "Integrity threat: ${report.threatLevel} score=${report.threatScore}")
        }
    }

    fun onPriceTamperDetected(productId: String) {
        sessionSecurityEvents++
        SecureLogger.security("Monitor", "PriceTamper productId=${productId.take(8)}")
    }

    fun flushSessionMetrics() {
        SecureLogger.d("Monitor", "Session: apiCalls=$sessionApiCallCount events=$sessionSecurityEvents")
    }

    private fun pruneWindow(queue: ArrayDeque<Long>) {
        val cutoff = System.currentTimeMillis() - WINDOW_MS
        while (queue.isNotEmpty() && queue.first() < cutoff) queue.removeFirst()
    }
}
