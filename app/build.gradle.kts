import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// release signing comes from the gitignored local.properties; without it the release APK is simply unsigned
val local = Properties().apply { rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use(::load) }

android {
    namespace = "com.arahin"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.arahin"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "1.0.1"
    }

    signingConfigs {
        local.getProperty("arahin.keystore")?.let { path ->
            create("release") {
                storeFile = file(path)
                storePassword = local.getProperty("arahin.keystore.password")
                keyAlias = local.getProperty("arahin.key.alias")
                keyPassword = local.getProperty("arahin.keystore.password")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("com.huawei.hms:wearengine:5.0.2.306")
    testImplementation("junit:junit:4.13.2")
}
