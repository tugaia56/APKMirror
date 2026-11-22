plugins {
    id("com.android.application")
}

android {
    namespace = "cf.vojtechh.apkmirror"
    compileSdk = 35

    defaultConfig {
        // Different id from the original app (cf.vojtechh.apkmirror), so both can be installed
        applicationId = "it.tugaia56.apkmirror"
        minSdk = 26
        // 34 on purpose: the app is not edge-to-edge yet (Android 15 forces it from target 35)
        targetSdk = 34
        versionName = "4.0"
        versionCode = 40000
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        disable += "MissingTranslation"
    }

    buildTypes {
        release {
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
