package com.confidencecommerce.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OWASP M9: Insecure Data Storage — All local state is AES-256-GCM encrypted.
 *
 * Uses AndroidX Security Crypto EncryptedSharedPreferences backed by the
 * Android Keystore (hardware-backed on API 28+).
 *
 * Key hierarchy:
 *   Android Keystore (TEE/StrongBox) → MasterKey (AES-256-GCM)
 *     → EncryptedSharedPreferences (encrypts both keys AND values)
 *
 * NEVER store:
 *   - Raw payment card data (PCI-DSS violation)
 *   - Plaintext passwords
 *   - Full JWTs (store only session reference tokens)
 */
@Singleton
class SecurePreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "SecurePrefs"
        private const val PREFS_FILE = "cc_secure_prefs"
        private const val KEY_SESSION_TOKEN = "session_token"
        private const val KEY_USER_ID       = "user_id"
        private const val KEY_ONBOARDING_DONE = "onboarding_done"
        private const val KEY_RECENTLY_VIEWED = "recently_viewed_ids"
        private const val KEY_CART_ID       = "cart_id"
    }

    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            // Request hardware-backed key if available (StrongBox on API 28+)
            .setRequestStrongBoxBacked(true)
            .build()
    }

    private val encryptedPrefs: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // ── Session Management ────────────────────────────────────────────────────

    suspend fun saveSessionToken(token: String) = withContext(Dispatchers.IO) {
        SecureLogger.d(TAG, "Saving session token (length=${token.length})")
        encryptedPrefs.edit().putString(KEY_SESSION_TOKEN, token).apply()
    }

    suspend fun getSessionToken(): String? = withContext(Dispatchers.IO) {
        encryptedPrefs.getString(KEY_SESSION_TOKEN, null)
    }

    suspend fun saveUserId(userId: String) = withContext(Dispatchers.IO) {
        encryptedPrefs.edit().putString(KEY_USER_ID, userId).apply()
    }

    suspend fun getUserId(): String? = withContext(Dispatchers.IO) {
        encryptedPrefs.getString(KEY_USER_ID, null)
    }

    // ── Cart ──────────────────────────────────────────────────────────────────

    suspend fun saveCartId(cartId: String) = withContext(Dispatchers.IO) {
        encryptedPrefs.edit().putString(KEY_CART_ID, cartId).apply()
    }

    suspend fun getCartId(): String? = withContext(Dispatchers.IO) {
        encryptedPrefs.getString(KEY_CART_ID, null)
    }

    // ── User Preferences ──────────────────────────────────────────────────────

    suspend fun setOnboardingComplete() = withContext(Dispatchers.IO) {
        encryptedPrefs.edit().putBoolean(KEY_ONBOARDING_DONE, true).apply()
    }

    suspend fun isOnboardingComplete(): Boolean = withContext(Dispatchers.IO) {
        encryptedPrefs.getBoolean(KEY_ONBOARDING_DONE, false)
    }

    suspend fun saveRecentlyViewedIds(ids: Set<String>) = withContext(Dispatchers.IO) {
        encryptedPrefs.edit().putStringSet(KEY_RECENTLY_VIEWED, ids).apply()
    }

    suspend fun getRecentlyViewedIds(): Set<String> = withContext(Dispatchers.IO) {
        encryptedPrefs.getStringSet(KEY_RECENTLY_VIEWED, emptySet()) ?: emptySet()
    }

    /**
     * Securely wipe all stored data on logout.
     * Called by AuthRepository on explicit logout or token expiry.
     */
    suspend fun clearAll() = withContext(Dispatchers.IO) {
        SecureLogger.security(TAG, "Clearing all secure preferences (logout)")
        encryptedPrefs.edit().clear().apply()
    }
}
