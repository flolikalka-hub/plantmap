package com.example.plantmap;

import android.app.Application;
import com.example.plantmap.db.DatabaseHelper;
import com.example.plantmap.plant.PlantRepository;

/**
синглтоны на уровне приложения - БД (временно) и репозиторий
запускается самым первым, ещё до того, как появится первый экран
*/
public class App extends Application {
    private static App instance; /** общая для всего приложения, единственный экземпляр
    статическая переменная может запомнить старые данные от предыдущего теста, приходится чистить руками*/
    private DatabaseHelper dbHelper; //помощник, который умеет разговаривать с базой данных
    private PlantRepository repository; //склад, который знает, как правильно брать растения из помощника и отдавать их экранам

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
}