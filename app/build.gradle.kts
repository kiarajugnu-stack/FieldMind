import java.io.File
import java.security.KeyStore
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    id("kotlin-parcelize")
//    alias(libs.plugins.kotlin.serialization)
}


val unsignedApkOnly = providers.gradleProperty("unsignedApkOnly")
    .orElse(providers.environmentVariable("UNSIGNED_APK_ONLY"))
    .map { it.equals("true", ignoreCase = true) }
    .getOrElse(false)

android {
    namespace = "chromahub.rhythm.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "fieldmind.research.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 504031054
        versionName = "5.0.403.1054"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Product flavors for different distribution channels
    flavorDimensions += "distribution"
    
    productFlavors {
        create("fdroid") {
            dimension = "distribution"
            applicationId = "fieldmind.research.app"
            
            // F-Droid build: Enable all features (FOSS ethos)
            buildConfigField("boolean", "ENABLE_YOUTUBE_MUSIC", "true")
            buildConfigField("boolean", "ENABLE_APPLE_MUSIC", "true")
            buildConfigField("boolean", "ENABLE_DEEZER", "true")
            buildConfigField("boolean", "ENABLE_LRCLIB", "true")
            buildConfigField("boolean", "ENABLE_SPOTIFY_SEARCH", "true")
            buildConfigField("String", "FLAVOR", "\"fdroid\"")
            
            versionNameSuffix = "-fdroid"
        }
        
        create("github") {
            dimension = "distribution"
            applicationId = "fieldmind.research.app"
            
            // GitHub releases: Enable all features (same as F-Droid)
            buildConfigField("boolean", "ENABLE_YOUTUBE_MUSIC", "true")
            buildConfigField("boolean", "ENABLE_APPLE_MUSIC", "true")
            buildConfigField("boolean", "ENABLE_DEEZER", "true")
            buildConfigField("boolean", "ENABLE_LRCLIB", "true")
            buildConfigField("boolean", "ENABLE_SPOTIFY_SEARCH", "true")
            buildConfigField("String", "FLAVOR", "\"github\"")
            
            versionNameSuffix = "-gh"
        }
    }

    val rawSigningProperties = getProperties(".config/keystore.properties")
    val signingProperties = rawSigningProperties?.takeIf { it.hasValidSigningMaterial(rootProject.projectDir) }
    if (rawSigningProperties != null && signingProperties == null) {
        logger.warn("Release signing properties were present but invalid; using debug signing fallback for release builds.")
    }
    val releaseSigning = signingProperties?.let {
        signingConfigs.create("release") {
            keyAlias = it.property("key_alias")
            keyPassword = it.property("key_password")
            storePassword = it.property("store_password")
            storeFile = rootProject.file(it.property("store_file"))
        }
    }
    defaultConfig {
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Android cannot install truly unsigned APKs. When no release keystore is
            // configured (for PR previews), fall back to the standard debug signing
            // config so the release-variant APK is still installable for testing.
            signingConfig = if (unsignedApkOnly) null else releaseSigning ?: signingConfigs.getByName("debug")
//            ndk {
//                debugSymbolLevel = "SYMBOL_TABLE"
//            }
            // Reproducible builds: disable build timestamp
            if (System.getenv("CI") == "true" || System.getenv("BUILD_REPRODUCIBLE") == "true") {
                // Use a fixed timestamp for reproducible builds  
                tasks.configureEach {
                    // Disable timestamps in bundle reports for reproducible builds
                    if (name.contains("BundleReport", ignoreCase = true)) {
                        enabled = false
                    }
                }
            }
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            //isMinifyEnabled = false
            //isDebuggable = true
            signingConfig = if (unsignedApkOnly) null else signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
            freeCompilerArgs.addAll(
                "-opt-in=androidx.compose.material3.ExperimentalMaterial3ExpressiveApi",
                "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api"
            )
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    dependenciesInfo {
        // Disables dependency metadata when building APKs (for IzzyOnDroid/F-Droid)
        includeInApk = false
        // Disables dependency metadata when building Android App Bundles
        includeInBundle = false
    }

    packaging {
        resources {
            merges += "/META-INF/INDEX.LIST"
            merges += "**/io.netty.versions.properties"
        }
    }

    // Build workflows release exactly one universal APK per requested build type.
    // ABI splits stay disabled so CI does not publish per-architecture artifacts.
    splits {
        abi {
            isEnable = false
            isUniversalApk = false
        }
    }

}

androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->
            val signatureLabel = if (unsignedApkOnly) "unsigned" else "signed"
            output.outputFileName.set(
                "FieldMind-${android.defaultConfig.versionName}-${variant.name}-$signatureLabel-universal.apk"
            )
        }
    }
}

dependencies {
    implementation(libs.androidx.compose.ui.unit)
    // Desugaring library
    coreLibraryDesugaring(libs.androidx.desugar.jdk.libs)

    // Core Android dependencies
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.core)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    
    // Compose dependencies
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    
    // Material 3 dependencies
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.material3.android)
    implementation(libs.androidx.material3.window.size)
    implementation(libs.com.google.android.material)

    // Media3 dependencies
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayer.dash)
    implementation(libs.androidx.media3.exoplayer.hls)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.exoplayer.midi)
    implementation(libs.org.jellyfin.media3.ffmpeg.decoder)
    
    // Icons - Material Symbols variable font (res/font/material_symbols_outlined.ttf)
    // Replaces the deprecated material-icons-extended library for faster build times
    implementation(libs.androidx.palette.ktx)
    
    // Glance for modern widgets
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
    
    // Physics-based animations
    implementation(libs.androidx.compose.animation)
    //noinspection GradleDependency
    implementation(libs.androidx.compose.animation.graphics)
    implementation(libs.androidx.compose.animation.core)
    
    // Navigation
    implementation(libs.androidx.navigation.compose)
    
    // Permissions
    implementation(libs.com.google.accompanist.accompanist.permissions)
    
    // Fragment
    implementation(libs.androidx.fragment.ktx)
    
    // MediaRouter for Android media output switching
    implementation(libs.androidx.mediarouter)
    
    // Coil for image loading
    implementation(libs.io.coil.kt.coil.compose)
    
    // Audio metadata editing
    implementation(libs.net.jthink.jaudiotagger)
    
    // Network
    implementation(libs.com.squareup.retrofit2.retrofit)
    implementation(libs.com.squareup.retrofit2.converter.gson)
    implementation(libs.com.squareup.okhttp3.okhttp)
    implementation(libs.com.squareup.okhttp3.logging.interceptor)
    implementation(libs.com.google.code.gson.gson)
//    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
//    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")

    // OpenStreetMap for offline GPS maps
    implementation(libs.osmdroid.android)

    // Biometric authentication for privacy lock
    implementation("androidx.biometric:biometric:1.2.0-alpha05")

    // Coroutines for async operations
    implementation(libs.org.jetbrains.kotlinx.coroutines.core)
    implementation(libs.org.jetbrains.kotlinx.coroutines.android)
    implementation(libs.androidx.foundation.layout)
    
    // WorkManager for background tasks
    implementation(libs.androidx.work.runtime.ktx)
    
    // Room database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    
    // LeakCanary for memory leak detection (debug builds only)
    debugImplementation(libs.com.squareup.leakcanary.leakcanary.android)
}

fun getProperties(fileName: String): Properties? {
    val file = rootProject.file(fileName)
    return if (file.exists()) {
        Properties().also { properties ->
            file.inputStream().use { properties.load(it) }
        }
    } else null
}

fun Properties.property(key: String) =
    this.getProperty(key) ?: "$key missing"

fun Properties.hasValidSigningMaterial(rootDir: File): Boolean {
    val alias = getProperty("key_alias")?.takeIf { it.isNotBlank() } ?: return false
    val keyPassword = getProperty("key_password")?.takeIf { it.isNotBlank() } ?: return false
    val storePassword = getProperty("store_password")?.takeIf { it.isNotBlank() } ?: return false
    val storePath = getProperty("store_file")?.takeIf { it.isNotBlank() } ?: return false
    val storeFile = File(storePath).let { if (it.isAbsolute) it else File(rootDir, storePath) }
    if (!storeFile.isFile || storeFile.length() == 0L) return false

    return listOf(KeyStore.getDefaultType(), "JKS", "PKCS12").distinct().any { type ->
        runCatching {
            val keyStore = KeyStore.getInstance(type)
            storeFile.inputStream().use { keyStore.load(it, storePassword.toCharArray()) }
            keyStore.containsAlias(alias) && keyStore.getKey(alias, keyPassword.toCharArray()) != null
        }.getOrDefault(false)
    }
}
