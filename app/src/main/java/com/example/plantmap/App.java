package com.example.plantmap;

import android.app.Application;
import com.example.plantmap.db.DatabaseHelper;
import com.example.plantmap.plant.PlantRepository;

/*
PlantRepository будет синглтоном на уровне приложения.
Все ViewModel смогут получить к нему доступ через App.getInstance().getRepository().
Это упростит создание ViewModel и позволит избежать передачи зависимостей через Activity.
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