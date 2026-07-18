package com.example.plantmap;

import android.app.Application;
import com.example.plantmap.db.DatabaseHelper;
import com.example.plantmap.db.yandex_images.PlantPhotoLoader;
import com.example.plantmap.plant.PlantRepository;

/**
 * Пользовательский класс приложения.
 * Создаётся раньше любого Activity/Service и живёт на протяжении всего процесса.
 * Здесь инициализируются глобальные синглтоны:
 * - DatabaseHelper — доступ к локальной БД (SQLite)
 * - PlantRepository — единая точка доступа к данным о растениях
 *
 * Внимание: статическая ссылка instance может сохранять состояние
 * между тестовыми запусками. При инструментальном тестировании рекомендуется
 * пересоздавать Application или использовать DI-фреймворк (Hilt/Koin) для
 * управления временем жизни зависимостей.
 */
public class App extends Application {

    /** Единственный экземпляр приложения (доступен через getInstance()). */
    private static App instance;

    /** Помощник для работы с базой данных. */
    private DatabaseHelper dbHelper;

    /** Репозиторий растений (абстракция над dbHelper). */
    private PlantRepository repository;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        // Инициализация слоя данных
        dbHelper = new DatabaseHelper(this);
        repository = new PlantRepository(dbHelper, this);
        // Инициализация кэша изображений
        PlantPhotoLoader.init(getApplicationContext());
    }

    /**
     * Возвращает глобальный экземпляр приложения.
     * Используется для получения репозитория из любого места,
     * где недоступен Context (например, в утилитах).
     */
    public static App getInstance() {
        return instance;
    }

    /**
     * Возвращает репозиторий растений.
     * Через него Activity и Fragment'ы получают данные, не зная о БД.
     */
    public PlantRepository getRepository() {
        return repository;
    }
}