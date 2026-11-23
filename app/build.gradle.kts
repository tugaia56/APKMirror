import java.util.Properties

plugins {
    id("com.android.application")
}

// Signing: keystore.properties (not in git) in the project root: storeFile, storePassword, keyAlias, keyPassword
val keystoreProps = Properties()
val keystoreFile = rootProject.file("keystore.properties")
if (keystoreFile.exists()) keystoreProps.load(keystoreFile.inputStream())

android {
    namespace = "cf.vojtechh.apkmirror"
    compileSdk = 35

    defaultConfig {
        // Different id from the original app (cf.vojtechh.apkmirror), so both can be installed
        applicationId = "it.tugaia56.apkmirror"
        minSdk = 26
        // 34 on purpose: the app is not edge-to-edge yet (Android 15 forces it from target 35)
        targetSdk = 34
        versionName = "4.1"
        versionCode = 40100
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        disable += "MissingTranslation"
    }

    signingConfigs {
        if (keystoreFile.exists()) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            if (keystoreFile.exists()) signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("androidx.preference:preference:1.2.1")
    implementation("androidx.webkit:webkit:1.12.1")
    implementation("androidx.fragment:fragment:1.8.5")
    // Licenses dialog
    implementation("de.psdev.licensesdialog:licensesdialog:2.2.0")
}
