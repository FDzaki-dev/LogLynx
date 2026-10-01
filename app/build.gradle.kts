plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Signing hanya aktif jika keystore + semua secret tersedia (CI via GitHub Secrets).
val releaseKeystore = rootProject.file("release.keystore")
val releaseStorePassword: String? = System.getenv("KEYSTORE_PASSWORD")?.takeIf { it.isNotBlank() }
val releaseKeyAlias: String? = System.getenv("KEY_ALIAS")?.takeIf { it.isNotBlank() }
val releaseKeyPassword: String? = System.getenv("KEY_PASSWORD")?.takeIf { it.isNotBlank() }
val canSignRelease = releaseKeystore.exists() &&
    releaseStorePassword != null && releaseKeyAlias != null && releaseKeyPassword != null

// Versi: di CI mengikuti nomor run GitHub Actions (monoton naik -> APK baru selalu bisa meng-update yang lama).
// Rilis GitHub memakai versionName ini sebagai tag (v<versionName>).
val ciRunNumber: Int? = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull()
val appVersionBase = "1.0"

android {
    namespace = "com.pro.logcatreader"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.pro.logcatreader"
        minSdk = 26
        targetSdk = 35
        versionCode = ciRunNumber ?: 1
        versionName = if (ciRunNumber != null) "$appVersionBase.$ciRunNumber" else "$appVersionBase.0-dev"
    }

    signingConfigs {
        if (canSignRelease) {
            create("release") {
                storeFile = releaseKeystore
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (canSignRelease) {
                signingConfig = signingConfigs.getByName("release")
            }
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
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Jetpack Compose UI
    implementation(platform("androidx.compose:compose-bom:2025.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")
    // Ikon dasar (Search, MoreVert, dll.) untuk UI v3; set extended sengaja tidak dipakai (APK kecil)
    implementation("androidx.compose.material:material-icons-core")

    // Activity (setContent + enableEdgeToEdge)
    implementation("androidx.activity:activity-compose:1.9.3")

    // Lifecycle & Coroutines Flow
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Shizuku API (Untuk membaca log sistem tanpa Root via ADB)
    implementation("dev.rikka.shizuku:api:13.1.5")
    implementation("dev.rikka.shizuku:provider:13.1.5")
}
