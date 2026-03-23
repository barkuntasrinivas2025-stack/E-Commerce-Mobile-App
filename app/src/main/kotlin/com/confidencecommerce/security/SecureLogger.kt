package com.confidencecommerce.security

import android.util.Log

/**
 * Secure logging wrapper.
 * Release builds: v/d/i calls are no-ops.
 * ProGuard strips Log.* bytecode additionally.
 */
object SecureLogger {

    private const val PREFIX = "CC"
    private var isDebug = false

    fun initialize(isDebug: Boolean) { this.isDebug = isDebug }

    fun v(tag: String, msg: String) { if (isDebug) Log.v("$PREFIX/$tag", msg) }
    fun d(tag: String, msg: String) { if (isDebug) Log.d("$PREFIX/$tag", msg) }
    fun i(tag: String, msg: String) { if (isDebug) Log.i("$PREFIX/$tag", msg) }
    fun w(tag: String, msg: String, t: Throwable? = null) { Log.w("$PREFIX/$tag", msg, t) }
    fun e(tag: String, msg: String, t: Throwable? = null) {
        if (isDebug) Log.e("$PREFIX/$tag", msg, t)
    }
    fun security(tag: String, event: String) {
        val safe = InputValidator.sanitizeForLogging(event)
        Log.w("$PREFIX/SECURITY/$tag", safe)
    }
}
