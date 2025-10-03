import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.kapt)
    alias(libs.plugins.hiltCompiler)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlinx.serialization)
}

android {
    namespace = "com.shestikpetr.meteo"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.shestikpetr.meteo"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        // Load API keys from local.properties
        val properties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            properties.load(localPropertiesFile.inputStream())
        }

        val yandexMapkitApiKey = properties.getProperty("YANDEX_MAPKIT_API_KEY") ?: "YOUR_API_KEY_HERE"
        val defaultBaseUrl = properties.getProperty("DEFAULT_BASE_URL") ?: "https://your-api-server.com/api/v1/"
        val apiHost = properties.getProperty("API_HOST") ?: "your-api-server.com"

        debug {
            buildConfigField("String", "YANDEX_MAPKIT_API_KEY", "\"$yandexMapkitApiKey\"")
            buildConfigField("String", "DEFAULT_BASE_URL", "\"$defaultBaseUrl\"")
            buildConfigField("String", "API_HOST", "\"$apiHost\"")
            buildConfigField("Boolean", "ENABLE_NETWORK_LOGGING", "true")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "YANDEX_MAPKIT_API_KEY", "\"$yandexMapkitApiKey\"")
            buildConfigField("String", "DEFAULT_BASE_URL", "\"$defaultBaseUrl\"")
            buildConfigField("String", "API_HOST", "\"$apiHost\"")
            buildConfigField("Boolean", "ENABLE_NETWORK_LOGGING", "false")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
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
    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }
}

kapt {
    correctErrorTypes = true
    useBuildCache = true
    mapDiagnosticLocations = true
}

dependencies {
    // Security
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Localization dependencies
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    // Splash Screen API
    implementation(libs.androidx.core.splashscreen)

    // Chart
    implementation(libs.yml.ycharts)

    // Retrofit
    implementation(libs.retrofit.android)
    implementation(libs.retrofit.android.converter)
    implementation("com.google.code.gson:gson:2.10.1")
    implementation(libs.androidx.animation.core.lint)
    implementation(libs.androidx.espresso.core)
    implementation(libs.androidx.espresso.core)

    // Hilt
    kapt(libs.hilt.android.compiler)
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation)
    implementation(libs.logging.interceptor)
    kapt(libs.dagger.hilt.android.compiler)
    implementation(libs.androidx.hilt.common)
    kapt(libs.androidx.hilt.compiler)

    // Yandex Maps
    implementation(libs.yandex.mapkit.kmp)
    implementation(libs.yandex.mapkit.kmp.compose)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation("androidx.compose.material:material-icons-extended:1.7.6")
    testImplementation(libs.junit)
    testImplementation("org.mockito:mockito-core:5.7.0")
    testImplementation("org.mockito:mockito-inline:5.2.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("org.robolectric:robolectric:4.11")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}