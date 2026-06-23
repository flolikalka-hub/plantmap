package com.example.plantmap.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.widget.Toast;

import com.example.plantmap.db.yandex.YandexDiskHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class UpdateDatabase {
    private final Context context;
    // Публичная ссылка на plants.json на Яндекс.Диске
    private static final String PLANTS_JSON_PUBLIC_KEY = "https://disk.yandex.ru/d/9jzHNPTh30boeQ";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final OkHttpClient httpClient = new OkHttpClient();

    public UpdateDatabase(Context context) {
        this.context = context.getApplicationContext();
    }

    public void importDatabase() {
        executor.execute(() -> {
            try {
                // 1. Получить прямую ссылку
                String directUrl = YandexDiskHelper.getDirectUrl(PLANTS_JSON_PUBLIC_KEY);
                if (directUrl == null) {
                    showToast("Не удалось получить ссылку на файл");
                    return;
                }

                // 2. Скачать JSON
                String jsonString = downloadJson(directUrl);
                if (jsonString == null) {
                    showToast("Ошибка загрузки файла");
                    return;
                }

                // 3. Распарсить сразу в ContentValues
                List<ContentValues> valuesList = parsePlants(jsonString);
                if (valuesList == null || valuesList.isEmpty()) {
                    showToast("Файл пуст или повреждён");
                    return;
                }

                // 4. Заменить содержимое таблицы plants
                replaceAllPlants(valuesList);

                showToast("База обновлена, загружено записей: " + valuesList.size());

            } catch (Exception e) {
                e.printStackTrace();
                showToast("Ошибка: " + e.getMessage());
            }
        });
    }

    private String downloadJson(String url) {
        Request request = new Request.Builder().url(url).build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                return response.body().string();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // Парсинг JSON-массива в список ContentValues для прямой вставки в таблицу plants
    private List<ContentValues> parsePlants(String json) {
        List<ContentValues> list = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                ContentValues cv = new ContentValues();

                // для обнаружения конфликтов
                cv.put("id", obj.getInt("id"));

                cv.put("name", obj.optString("name"));
                cv.put("variety_id", obj.optInt("variety_id"));

                Integer potVolume = null;
                if (obj.has("pot_volume") && !obj.isNull("pot_volume")) {
                    try {
                        potVolume = obj.getInt("pot_volume");
                    } catch (Exception e) {
                        potVolume = null;
                    }
                }
                cv.put("pot_volume", potVolume);

                cv.put("flower_color", obj.optInt("flower_color", 9));

                String additionalInfo = obj.isNull("additional_info")
                        ? null
                        : obj.getString("additional_info");
                cv.put("additional_info", additionalInfo);

                cv.put("is_builtin", obj.optInt("is_builtin", 0));

                String publicKey = obj.isNull("public_key")
                        ? null
                        : obj.getString("public_key");
                cv.put("public_key", publicKey);

                String nameRosebook = obj.isNull("name_rosebook")
                        ? null
                        : obj.getString("name_rosebook");
                cv.put("name_rosebook", nameRosebook);

                list.add(cv);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return list;
    }

    private void replaceAllPlants(List<ContentValues> plantsData) {
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // Собираем множество id из JSON
        Set<Integer> jsonIds = new HashSet<>();
        for (ContentValues cv : plantsData) {
            jsonIds.add(cv.getAsInteger("id"));
        }

        db.beginTransaction();
        try {
            // Для каждого растения из JSON: обновить, если существует, иначе вставить
            for (ContentValues cv : plantsData) {
                db.insertWithOnConflict("plants", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private void showToast(String message) {
        context.getMainExecutor().execute(() ->
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        );
    }
}