// =============================================================================
// Главный сборочный скрипт приложения PlantMap
// Версия 3.3 (Release)
// =============================================================================

plugins {
    // Современный способ подключения плагинов через Version Catalog
    alias(libs.plugins.android.application)
}

android {
    // Пространство имён приложения (должно совпадать с package в манифесте)
    namespace = "com.example.plantmap"

    // Уровень API для компиляции — Android 16 (Baklava) Developer Preview
    // Обеспечивает доступ к новейшим API и оптимизациям компилятора
    compileSdk = 36

    defaultConfig {
        // Уникальный идентификатор приложения
        applicationId = "com.example.plantmap"

        // Минимальная поддерживаемая версия — Android 10 (API 29)
        minSdk = 29

        // Целевая версия SDK, на которой тестировалось приложение
        targetSdk = 36

        // Версия для внутреннего учёта (код версии)
        versionCode = 56

        // Публичная версия для пользователей
        versionName = "Release-3.3.3"

        // Стандартный раннер для инструментальных тестов
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        // ---------------------------------------------------------------------
        // Релизная сборка — для публикации и финальной демонстрации
        release {
            // Включаем минификацию (обфускацию) и оптимизацию кода
            // Это уменьшает размер APK и защищает код от обратной разработки
            isMinifyEnabled = true

            // Удаление неиспользуемых ресурсов (изображений, строк, макетов)
            // Работает только при включённой минификации
            isShrinkResources = true

            // Стандартные правила ProGuard + наш файл с кастомными правилами
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        // ---------------------------------------------------------------------
        // Отладочная сборка — для разработки и тестирования
        debug {
            // Добавляем суффикс к applicationId, чтобы debug и release
            // версии можно было установить на одно устройство одновременно
            applicationIdSuffix = ".dev"

            // Суффикс в имени версии, чтобы визуально отличать сборки
            versionNameSuffix = "-dev"

            // Отключаем минификацию для ускорения сборки и удобства отладки
            isMinifyEnabled = false
        }
    }

    // Настройка совместимости с Java 11
    // Позволяет использовать современные конструкции языка (var, лямбды и т.д.)
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    // ========================
    // AndroidX UI
    // ========================
    // Обеспечивает единый внешний вид компонентов на старых версиях Android
    implementation(libs.appcompat)
    // Компоненты Material Design 3 (красивые кнопки, меню, диалоги)
    implementation(libs.material)
    // Упрощённая работа с Activity и её жизненным циклом
    implementation(libs.activity)
    // Мощный менеджер размещения элементов интерфейса
    implementation(libs.constraintlayout)

    // ========================
    // Архитектурные компоненты
    // ========================
    // Хранение данных, устойчивое к повороту экрана (ViewModel)
    implementation(libs.lifecycle.viewmodel)
    // LiveData — наблюдаемые данные, которые автоматически уведомляют UI об изменениях
    implementation(libs.lifecycle.livedata)
    // Навигация между экранами (Fragment) без ручного управления транзакциями
    implementation(libs.navigation.fragment)

    // ========================
    // Изображения и сеть
    // ========================
    // Glide — эффективная загрузка, кэширование и отображение изображений
    implementation(libs.glide)
    // Процессор аннотаций Glide (генерирует код для ускорения работы)
    annotationProcessor(libs.compiler)

    // OkHttp — надёжный HTTP-клиент для запросов к API Яндекс.Карт
    implementation(libs.okhttp)

    // ========================
    // Тестирование
    // ========================
    testImplementation(libs.junit)                  // Модульные тесты (JVM)
    androidTestImplementation(libs.ext.junit)       // Инструментальные тесты (Android)
    androidTestImplementation(libs.espresso.core)   // UI-тесты (имитация кликов, свайпов)
}