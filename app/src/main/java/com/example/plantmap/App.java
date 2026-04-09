package com.example.plantmap;

import android.app.Application;
import com.example.plantmap.db.DatabaseHelper;
import com.example.plantmap.plant.PlantRepository;

/*
синглтоны на уровне приложения - БД (временно) и репозиторий
*/
public class App extends Application {
    private static App instance;
    private DatabaseHelper dbHelper;
    private PlantRepository repository;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        dbHelper = new DatabaseHelper(this);
        repository = new PlantRepository(dbHelper);
    }

    public static App getInstance() {
        return instance;
    }

    public PlantRepository getRepository() {
        return repository;
    }
    public DatabaseHelper getDbHelper() {
        return dbHelper;
    }
}