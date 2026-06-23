plugins {
    /**
    Version Catalog (libs.versions.toml)
    современный и рекомендуемый способ
    централизованного управления версиями плагинов и зависимостей.
    Упрощает обновление и обеспечивает консистентность между модулями.
     */
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.plantmap"
    compileSdk = 36 // android 16

    defaultConfig {
        applicationId = "com.example.plantmap"
        minSdk = 29 // android 10
        targetSdk = 36
        versionCode = 49
        versionName = "Release-2.4.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            /**
            обфускация, оптимизация и удаление неиспользуемого кода
            Снижает размер APK и повышает безопасность
             */
            isMinifyEnabled = true
            /**
            удаляет неиспользуемые ресурсы (картинки, строки и т.д.)
             */
            isShrinkResources = true
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

    implementation(libs.appcompat) //для старых телефонов, чтобы новые фишки выглядели на них одинаково
    implementation(libs.material) // красивые кнопки и менюшки от Google
    implementation(libs.activity) // для создания окон (экранов)
    implementation(libs.constraintlayout) // для расстановки кнопок и картинок на экране

    implementation(libs.navigation.fragment) // для переключения между экранами
    implementation(libs.lifecycle.viewmodel) // для хранения данных, которые не теряются при повороте телефона
    implementation(libs.lifecycle.livedata) // сам сообщает экрану, что данные обновились

    testImplementation(libs.junit) // тесты комп
    androidTestImplementation(libs.ext.junit) // тесты тел
    androidTestImplementation(libs.espresso.core) // имитация действий пользователя

    // Glide для загрузки и кэширования изображений
    implementation(libs.glide)
    annotationProcessor(libs.compiler)

    // OkHttp (для запроса к API Яндекса)
    implementation(libs.okhttp)
}