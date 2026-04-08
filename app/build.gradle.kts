plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.plantmap"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.plantmap"
        minSdk = 29
        targetSdk = 36
        versionCode = 42
        versionName = "Release-2.3.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            //isMinifyEnabled = false
            isMinifyEnabled = true // Включает сжатие и обфускацию
            isShrinkResources = true // Включает удаление неиспользуемых ресурсов
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".dev" // отдельный ID
            versionNameSuffix = "-dev"   // чтобы отличать версии
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    implementation(libs.navigation.fragment)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(libs.colorpickerview)
}