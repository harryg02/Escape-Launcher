plugins {
    alias(libs.plugins.escapelauncher.android.application)
    alias(libs.plugins.escapelauncher.android.compose)
    alias(libs.plugins.escapelauncher.android.composeui)
    alias(libs.plugins.escapelauncher.android.hilt)
    alias(libs.plugins.escapelauncher.android.testing)
    alias(libs.plugins.escapelauncher.android.room)
    alias(libs.plugins.kotlin.serialization)
}

val baseVersionCode = "3.0"

// Set only by .github/workflows/release.yml. Local and other CI builds keep versionCode 3,
// versionName "3.0" and the debug signing config.
fun releaseEnv(name: String): String? =
    System.getenv(name)?.takeIf { it.isNotBlank() }

val releaseVersionCode = releaseEnv("RELEASE_VERSION_CODE")?.toInt()
val appVersionName = releaseEnv("RELEASE_VERSION_NAME") ?: baseVersionCode
val releaseKeystoreFile = releaseEnv("RELEASE_KEYSTORE_FILE")

android {
    namespace = "com.geecee.escapelauncher"

    defaultConfig {
        applicationId = "com.geecee.escapelauncher"
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 3
        releaseVersionCode?.let { versionCode = it }
        versionName = appVersionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        resValue("string", "app_version", appVersionName)
        resValue("string", "app_name", "Escape Launcher")
        resValue("string", "app_flavour", "Unknown Flavor")

        buildConfigField("String", "APP_VERSION", "\"$appVersionName\"")
        buildConfigField("String", "APP_NAME", "\"Escape Launcher\"")
        buildConfigField("String", "APP_FLAVOUR", "\"Unknown Flavor\"")
    }
    signingConfigs {
        if (releaseKeystoreFile != null) {
            create("release") {
                storeFile = file(releaseKeystoreFile)
                storeType = "PKCS12"
                storePassword = releaseEnv("RELEASE_KEYSTORE_PASSWORD")
                    ?: error("RELEASE_KEYSTORE_PASSWORD must be set when RELEASE_KEYSTORE_FILE is set")
                keyAlias = releaseEnv("RELEASE_KEY_ALIAS")
                    ?: error("RELEASE_KEY_ALIAS must be set when RELEASE_KEYSTORE_FILE is set")
                keyPassword = releaseEnv("RELEASE_KEY_PASSWORD")
                    ?: error("RELEASE_KEY_PASSWORD must be set when RELEASE_KEYSTORE_FILE is set")
            }
        }
    }
    buildTypes {
        debug {
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            resValue("string", "app_name", "Escape Launcher Dev")
            buildConfigField("String", "APP_NAME", "\"Escape Launcher Dev\"")
        }
        release {
            resValue("string", "app_name", "Escape Launcher")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "../proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName(
                if (releaseKeystoreFile != null) "release" else "debug"
            )
        }
    }

    buildFeatures {
        resValues = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.bundles.androidx.core)
    implementation(libs.bundles.compose.ui)
    implementation(libs.bundles.navigation3)
    implementation(libs.bundles.hilt)

    // WorkManager (with Hilt-injected workers)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)

    // JSON Parsing
    implementation(libs.gson)

    // Testing Libraries
    androidTestImplementation(libs.androidx.ui.test.junit4)

    // Debugging Tools
    debugImplementation(libs.androidx.ui.test.manifest)

    // Modules
    implementation(project(":core:data"))
    implementation(project(":core:ui"))
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(project(":core:domain"))
    implementation(project(":core:theme"))
    implementation(project(":core:analytics"))
    implementation(project(":core:cloudmessaging"))
    implementation(project(":feature:homescreen"))
    implementation(project(":feature:workapps"))
    implementation(project(":feature:privatespace"))
    implementation(project(":feature:securefolder"))
    implementation(project(":feature:weather"))
    implementation(project(":feature:screentime"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:appslist"))
    implementation(project(":feature:newwidgets"))
    implementation(project(":feature:onboarding"))
}