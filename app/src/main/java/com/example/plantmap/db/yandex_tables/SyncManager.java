package com.example.plantmap.db.yandex_tables;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.plantmap.db.DatabaseHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SyncManager {
    private static final String TAG = "SyncManager";
    private static final String PREFS_NAME = "sync_prefs";
    private static final String PREFIX_LAST_SYNC = "last_sync_";

    private final Context context;
    private final YandexDiscClient discClient;
    private final DatabaseHelper dbHelper;

    public SyncManager(Context context) {
        this.context = context.getApplicationContext();
        this.discClient = new YandexDiscClient();
        this.dbHelper = new DatabaseHelper(this.context);
    }

    /**
     * Синхронизирует все таблицы.
     * Вызывать в фоновом потоке!
     */
    public void syncAll() {
        // Сначала получаем изменения с сервера (чтобы не перетереть чужие)
        syncTable("variety");
        syncTable("plants");
        syncTable("points");
        syncTable("plant_pot_volumes");
        // Затем отправляем свои
        syncUpload("variety");
        syncUpload("plants");
        syncUpload("points");
        syncUpload("plant_pot_volumes");
    }

    /**
     * Синхронизирует одну таблицу.
     */
    private void syncTable(String tableName) {
        try {
            // 1. Получить время последней синхронизации
            long lastSync = getLastSyncTime(tableName);
            // 2. Получить время изменения серверного файла
            long serverModified = discClient.getServerModifiedTime(tableName);
            if (serverModified == 0) {
                Log.d(TAG, tableName + ".json отсутствует на сервере, пропускаем");
                return;
            }
            // Если файл не изменился - пропускаем
            if (serverModified <= lastSync) {
                Log.d(TAG, tableName + " не изменился (server=" + serverModified + ", last=" + lastSync + ")");
                return;
            }

            // 3. Скачать JSON
            String jsonStr = discClient.downloadJson(tableName);
            if (jsonStr == null) {
                Log.d(TAG, "Не удалось скачать " + tableName);
                return;
            }
            JSONArray serverArray = new JSONArray(jsonStr);

            // 4. Получить локальные данные (id, last_modified)
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Map<String, Long> localVersions = new HashMap<>(); // id -> last_modified
            Cursor cursor = db.query(tableName, new String[]{"id", "last_modified"}, null, null, null, null, null);
            while (cursor.moveToNext()) {
                String id = cursor.getString(0);
                long lastMod = cursor.getLong(1);
                localVersions.put(id, lastMod);
            }
            cursor.close();

            // 5. Слить данные
            db = dbHelper.getWritableDatabase();
            db.beginTransaction();
            try {
                for (int i = 0; i < serverArray.length(); i++) {
                    JSONObject serverObj = serverArray.getJSONObject(i);
                    String id = serverObj.getString("id");
                    long serverLastMod = serverObj.optLong("last_modified", 0);
                    int isDeleted = serverObj.optInt("is_deleted", 0);

                    if (isDeleted == 1) {
                        // Удалить локально, если есть
                        db.delete(tableName, "id=?", new String[]{id});
                        Log.d(TAG, tableName + ": удалена запись " + id);
                        continue;
                    }

                    Long localLastMod = localVersions.get(id);
                    if (localLastMod == null || serverLastMod > localLastMod) {
                        // Серверная запись новее или отсутствует локально → вставить/обновить
                        ContentValues cv = jsonToContentValues(serverObj);
                        db.insertWithOnConflict(tableName, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
                        Log.d(TAG, tableName + ": обновлена запись " + id);
                    } else {
                        Log.d(TAG, tableName + ": пропущена запись " + id + " (локальная версия новее)");
                    }
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }

            // 6. Обновить время последней синхронизации
            setLastSyncTime(tableName, serverModified);
            Log.d(TAG, "Синхронизация " + tableName + " завершена");

        } catch (IOException | JSONException e) {
            Log.e(TAG, "Ошибка синхронизации " + tableName, e);
        }
    }

    /**
     * Преобразует JSONObject в ContentValues для вставки в БД.
     * Универсальный метод: копирует все поля, которые есть в JSON.
     */
    private ContentValues jsonToContentValues(JSONObject obj) throws JSONException {
        ContentValues cv = new ContentValues();
        JSONArray names = obj.names();
        if (names == null) return cv;
        for (int i = 0; i < names.length(); i++) {
            String key = names.getString(i);
            Object value = obj.get(key);
            if (value instanceof String) {
                cv.put(key, (String) value);
            } else if (value instanceof Integer) {
                cv.put(key, (Integer) value);
            } else if (value instanceof Long) {
                cv.put(key, (Long) value);
            } else if (value instanceof Double) {
                cv.put(key, (Double) value);
            } else if (value instanceof Boolean) {
                cv.put(key, (Boolean) value);
            } else if (value == JSONObject.NULL) {
                cv.putNull(key);
            }
            // Пропускаем сложные объекты/массивы (если появятся, нужно будет добавить)
        }
        return cv;
    }

    private long getLastSyncTime(String tableName) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getLong(PREFIX_LAST_SYNC + tableName, 0);
    }

    private void setLastSyncTime(String tableName, long time) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putLong(PREFIX_LAST_SYNC + tableName, time).apply();
    }

    /**
     * Отправляет локальные изменения для указанной таблицы на сервер.
     */
    private void syncUpload(String tableName) {
        try {
            long lastSync = getLastSyncTime(tableName);
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            // Собираем локальные изменения (все, включая is_deleted=1)
            Cursor c = db.query(tableName, null, "last_modified > ?",
                    new String[]{String.valueOf(lastSync)}, null, null, null);
            List<ContentValues> localChanges = new ArrayList<>();
            while (c.moveToNext()) {
                ContentValues cv = new ContentValues();
                for (int i = 0; i < c.getColumnCount(); i++) {
                    String colName = c.getColumnName(i);
                    switch (c.getType(i)) {
                        case Cursor.FIELD_TYPE_STRING:
                            cv.put(colName, c.getString(i)); break;
                        case Cursor.FIELD_TYPE_INTEGER:
                            cv.put(colName, c.getLong(i)); break;
                        case Cursor.FIELD_TYPE_FLOAT:
                            cv.put(colName, c.getDouble(i)); break;
                        case Cursor.FIELD_TYPE_NULL:
                            cv.putNull(colName); break;
                    }
                }
                localChanges.add(cv);
            }
            c.close();

            if (localChanges.isEmpty()) {
                Log.d(TAG, tableName + ": нет локальных изменений для отправки");
                return;
            }

            // Скачиваем актуальный серверный JSON
            String serverJsonStr = discClient.downloadJson(tableName);
            JSONArray serverArray = (serverJsonStr != null) ? new JSONArray(serverJsonStr) : new JSONArray();

            // Строим Map для быстрого поиска
            Map<String, JSONObject> serverMap = new HashMap<>();
            for (int i = 0; i < serverArray.length(); i++) {
                JSONObject obj = serverArray.getJSONObject(i);
                serverMap.put(obj.getString("id"), obj);
            }

            // Слияние
            for (ContentValues cv : localChanges) {
                String id = cv.getAsString("id");
                int isDeleted = cv.getAsInteger("is_deleted");
                long localMod = cv.getAsLong("last_modified");

                JSONObject serverObj = serverMap.get(id);
                if (serverObj != null) {
                    long serverMod = serverObj.optLong("last_modified", 0);
                    if (localMod >= serverMod) {
                        if (isDeleted == 1) {
                            serverMap.remove(id);
                        } else {
                            updateJsonFromContentValues(serverObj, cv);
                        }
                    }
                } else {
                    if (isDeleted == 0) {
                        JSONObject newObj = new JSONObject();
                        for (String key : cv.keySet()) {
                            newObj.put(key, cv.get(key));
                        }
                        serverMap.put(id, newObj);
                    }
                }
            }

            // Формируем итоговый массив
            JSONArray merged = new JSONArray();
            for (JSONObject obj : serverMap.values()) merged.put(obj);

            // Загружаем как временный файл, затем перемещаем
            String tempName = tableName + "_temp_" + System.currentTimeMillis();
            discClient.safeUploadJson(tempName, merged.toString());  // используем safe, чтобы гарантированно создать
            discClient.moveFile(tempName, tableName);
            try { discClient.deleteFile(tempName); } catch (IOException ignored) {}

            // Обновляем время синхронизации
            setLastSyncTime(tableName, System.currentTimeMillis());
            Log.d(TAG, tableName + ": изменения отправлены");

        } catch (Exception e) {
            Log.e(TAG, "Ошибка отправки " + tableName, e);
        }
    }

    private void updateJsonFromContentValues(JSONObject json, ContentValues cv) throws JSONException {
        for (String key : cv.keySet()) {
            Object val = cv.get(key);
            if (val instanceof String) json.put(key, (String) val);
            else if (val instanceof Integer) json.put(key, (Integer) val);
            else if (val instanceof Long) json.put(key, (Long) val);
            else if (val instanceof Double) json.put(key, (Double) val);
            else if (val instanceof Boolean) json.put(key, (Boolean) val);
            else if (val == null) json.put(key, JSONObject.NULL);
        }
    }
}