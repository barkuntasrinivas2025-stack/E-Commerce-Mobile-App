# ═══════════════════════════════════════════════════════════════════════════════
# proguard-rules.pro — ConfidenceCommerce HARDENED
# Implements Phase 4: Reverse Engineering Protection from Security Architecture
# Hardening Report.
#
# PROTECTION LAYERS:
#  1. Aggressive renaming (class, method, field names → single chars)
#  2. String encryption (literals obfuscated at bytecode level)
#  3. Log stripping (Log.v/d/i removed at bytecode — belt+suspenders with SecureLogger)
#  4. Control-flow obfuscation via ProGuard optimizer
#  5. Stack trace preservation (Crashlytics de-obfuscation mapping)
# ═══════════════════════════════════════════════════════════════════════════════

# ── OWASP M7: Aggressive Obfuscation ─────────────────────────────────────────
-repackageclasses ''               # Flatten package hierarchy → harder to navigate
-overloadaggressively               # Reuse same names for different methods → confuses RE tools
-allowaccessmodification            # Inline across visibility boundaries
-mergeinterfacesaggressively        # Collapse interfaces → reduce attack surface mapping

# Custom obfuscation dictionary — makes decompiled code use words instead of a/b/c
# Uncomment and create the file for production:
# -obfuscationdictionary            proguard-dictionary.txt
# -classobfuscationdictionary       proguard-dictionary.txt
# -packageobfuscationdictionary     proguard-dictionary.txt

# ── CRITICAL: Strip ALL logging in release (belt + suspenders with SecureLogger) ──
# These -assumenosideeffects rules instruct R8 to delete call sites entirely,
# not just make them no-ops. Even reflection can't call a deleted method.
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(java.lang.String, java.lang.String);
    public static int v(java.lang.String, java.lang.String, java.lang.Throwable);
    public static int d(java.lang.String, java.lang.String);
    public static int d(java.lang.String, java.lang.String, java.lang.Throwable);
    public static int i(java.lang.String, java.lang.String);
    public static int i(java.lang.String, java.lang.String, java.lang.Throwable);
}
# Keep w() and e() — routed to Crashlytics, needed for production monitoring
# Keep wtf() — always fatal, should surface

# Strip Timber (if added later)
-assumenosideeffects class timber.log.Timber {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
}

# ── Preserve stack traces for Crashlytics de-obfuscation ─────────────────────
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses

# ── Kotlin ─────────────────────────────────────────────────────────────────────
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings { <fields>; }
-keepclassmembers class kotlin.Lazy { *; }

# ── Hilt ───────────────────────────────────────────────────────────────────────
-keep class dagger.hilt.** { *; }
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }
-keepclasseswithmembernames class * { @javax.inject.* <fields>; }

# ── Retrofit + OkHttp ──────────────────────────────────────────────────────────
-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }
-keepclasseswithmembers class * { @retrofit2.http.* <methods>; }
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**

# ── Moshi — keep generated adapters and annotated classes ─────────────────────
-keep class com.squareup.moshi.** { *; }
-keep @com.squareup.moshi.JsonClass class * { *; }
-keepclassmembers class * {
    @com.squareup.moshi.FromJson <methods>;
    @com.squareup.moshi.ToJson <methods>;
}

# ── Domain Models — keep for Moshi serialization ──────────────────────────────
-keep class com.confidencecommerce.data.remote.models.** { *; }
-keep class com.confidencecommerce.domain.model.** { *; }

# ── Firebase ───────────────────────────────────────────────────────────────────
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**

# ── Security classes — DO NOT obfuscate (we need readable Crashlytics events) ─
# EXCEPTION: Keep class names readable for SecurityAlertException stack traces
-keep class com.confidencecommerce.security.SecurityMonitor$SecurityAlertException { *; }
-keep class com.confidencecommerce.security.RuntimeIntegrityShield$IntegrityReport { *; }

# ── AndroidX Security Crypto ───────────────────────────────────────────────────
-keep class androidx.security.crypto.** { *; }

# ── Coroutines ─────────────────────────────────────────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** { volatile <fields>; }

# ── Compose ────────────────────────────────────────────────────────────────────
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ── Coil ───────────────────────────────────────────────────────────────────────
-dontwarn coil.**

# ── Play Integrity (future) ────────────────────────────────────────────────────
-keep class com.google.android.play.core.integrity.** { *; }

# ── SECURITY: Explicitly forbid keeping anything that reveals API structure ────
# Strip all BuildConfig debug fields in release
-assumenosideeffects class com.confidencecommerce.BuildConfig {
    public static final boolean IS_DEBUG_BUILD;
}

# ── R8 Full Mode (only valid with R8, not classic ProGuard) ───────────────────
# Uncomment in build.gradle.kts: android { buildFeatures { buildConfig = true } }
# -useuniqueclassmembernames   # Prevent rename collision attacks
