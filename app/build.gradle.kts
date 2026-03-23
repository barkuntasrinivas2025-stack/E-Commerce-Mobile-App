import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

// Load keys from gradle.properties (injected by CI in production)
val secureProps = Properties().also { props ->
    val localFile = rootProject.file("gradle.properties")
    if (localFile.exists()) localFile.inputStream().use { props.load(it) }
}
fun secureString(key: String, fallback: String = ""): String =
    System.getenv(key) ?: secureProps.getProperty(key, fallback)

android {
    namespace   = "com.confidencecommerce"
    compileSdk  = 34

    defaultConfig {
        applicationId   = "com.confidencecommerce"
        minSdk          = 26
        targetSdk       = 34
        versionCode     = 1
        versionName     = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String",  "API_BASE_URL",
            "\"${secureString("API_BASE_URL", "https://api.confidencecommerce.dev/v1/")}\"")
        buildConfigField("String",  "API_KEY",
            "\"${secureString("API_KEY_PLACEHOLDER", "")}\"")
        buildConfigField("Boolean", "IS_DEBUG_BUILD", "false")
        buildConfigField("Boolean", "CRASHLYTICS_ENABLED", "false")
        buildConfigField("String",  "VERSION_NAME", "\"1.0.0\"")
        buildConfigField("Int",     "VERSION_CODE", "1")
    }

    buildTypes {
        debug {
            applicationIdSuffix  = ".debug"
            isDebuggable         = true
            isMinifyEnabled      = false
            buildConfigField("Boolean", "IS_DEBUG_BUILD", "true")
        }
        release {
            isDebuggable         = false
            isMinifyEnabled      = true
            isShrinkResources    = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("Boolean", "IS_DEBUG_BUILD", "false")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions { jvmTarget = "17" }

    buildFeatures {
        compose     = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.3")

    // AndroidX Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)

    // Secure Storage
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.security.crypto)

    // Compose BOM
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)

    // Hilt DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Network
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.converter.moshi)
    implementation(libs.okhttp.core)
    implementation(libs.moshi.core)
    implementation(libs.moshi.kotlin)
    ksp(libs.moshi.kotlin.codegen)

    // Image Loading
    implementation(libs.coil.compose)

    // Debug
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
    debugImplementation(libs.okhttp.logging.interceptor)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(composeBom)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
