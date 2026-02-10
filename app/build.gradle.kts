import java.util.Properties
import org.gradle.api.GradleException

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.englishword"
    compileSdk = 34

    val isReleaseTaskRequested = gradle.startParameter.taskNames.any { taskName ->
        taskName.contains("release", ignoreCase = true) || taskName.contains("bundle", ignoreCase = true)
    }
    val releaseApplicationId = ((project.findProperty("RELEASE_APPLICATION_ID") as String?)?.trim())
        ?.takeIf { it.isNotEmpty() }
        ?: "com.longs.englishword"

    val releaseAdmobAppId = ((project.findProperty("ADMOB_APP_ID_RELEASE") as String?)?.trim()).orEmpty()
    val releaseBannerAdUnitId = ((project.findProperty("ADMOB_BANNER_AD_UNIT_ID_RELEASE") as String?)?.trim()).orEmpty()
    val releaseInterstitialAdUnitId = ((project.findProperty("ADMOB_INTERSTITIAL_AD_UNIT_ID_RELEASE") as String?)?.trim()).orEmpty()
    val releaseRewardedAdUnitId = ((project.findProperty("ADMOB_REWARDED_AD_UNIT_ID_RELEASE") as String?)?.trim()).orEmpty()
    val admobTestPrefix = "3940256099942544"

    if (isReleaseTaskRequested) {
        if (releaseAdmobAppId.isBlank() || releaseAdmobAppId.contains(admobTestPrefix)) {
            throw GradleException("Release build requires a valid ADMOB_APP_ID_RELEASE (test ID is not allowed).")
        }
        if (releaseBannerAdUnitId.isBlank() || releaseBannerAdUnitId.contains(admobTestPrefix)) {
            throw GradleException("Release build requires a valid ADMOB_BANNER_AD_UNIT_ID_RELEASE (test ID is not allowed).")
        }
        if (releaseInterstitialAdUnitId.isBlank() || releaseInterstitialAdUnitId.contains(admobTestPrefix)) {
            throw GradleException("Release build requires a valid ADMOB_INTERSTITIAL_AD_UNIT_ID_RELEASE (test ID is not allowed).")
        }
        if (releaseRewardedAdUnitId.isBlank() || releaseRewardedAdUnitId.contains(admobTestPrefix)) {
            throw GradleException("Release build requires a valid ADMOB_REWARDED_AD_UNIT_ID_RELEASE (test ID is not allowed).")
        }
    }

    signingConfigs {
        create("release") {
            val keystorePropertiesFile = rootProject.file("keystore.properties")
            if (isReleaseTaskRequested && !keystorePropertiesFile.exists()) {
                throw GradleException("Release build requires keystore.properties in project root.")
            }
            if (keystorePropertiesFile.exists()) {
                val keystoreProperties = Properties()
                keystoreProperties.load(keystorePropertiesFile.inputStream())
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }

    defaultConfig {
        applicationId = releaseApplicationId
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            manifestPlaceholders["admobAppId"] = releaseAdmobAppId
            buildConfigField("String", "ADMOB_BANNER_AD_UNIT_ID", "\"$releaseBannerAdUnitId\"")
            buildConfigField("String", "ADMOB_INTERSTITIAL_AD_UNIT_ID", "\"$releaseInterstitialAdUnitId\"")
            buildConfigField("String", "ADMOB_REWARDED_AD_UNIT_ID", "\"$releaseRewardedAdUnitId\"")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            manifestPlaceholders["admobAppId"] = "ca-app-pub-3940256099942544~3347511713"
            buildConfigField("String", "ADMOB_BANNER_AD_UNIT_ID", "\"ca-app-pub-3940256099942544/6300978111\"")
            buildConfigField("String", "ADMOB_INTERSTITIAL_AD_UNIT_ID", "\"ca-app-pub-3940256099942544/1033173712\"")
            buildConfigField("String", "ADMOB_REWARDED_AD_UNIT_ID", "\"ca-app-pub-3940256099942544/5224354917\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Compose BOM
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose UI
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)

    // Navigation Compose
    implementation(libs.navigation.compose)

    // Lifecycle ViewModel Compose
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Coroutines
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)

    // Google Play Billing
    implementation(libs.billing.ktx)

    // AdMob (Google Mobile Ads)
    implementation(libs.play.services.ads)

    // WorkManager for scheduled notifications
    implementation(libs.work.runtime.ktx)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.room.testing)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.mockk.android)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
}
