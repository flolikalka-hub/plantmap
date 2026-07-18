package com.example.plantmap.db.yandex_tables;

import android.util.Log;

import com.example.plantmap.BuildConfig;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Клиент для работы с закрытой папкой приложения Яндекс.Диска через OAuth-токен.
 * Использует REST API для скачивания и WebDAV-подобный подход для загрузки
 * (создание временного файла с последующим перемещением), чтобы избежать ошибки 409.
 */
public class YandexDiscClient {

    private final OkHttpClient httpClient;
    private final String token;

    public YandexDiscClient() {
        this(BuildConfig.YANDEX_DISK_TOKEN);
    }

    public YandexDiscClient(String token) {
        this.token = token;
        this.httpClient = new OkHttpClient();
    }

    /**
     * Скачивает содержимое JSON-файла таблицы из папки приложения.
     *
     * @param tableName имя файла без расширения (например, "plants")
     * @return строка JSON или null, если файл не найден (404)
     * @throws IOException при сетевых ошибках или ошибках API
     */
    public String downloadJson(String tableName) throws IOException {
        String path = "app:/" + tableName + ".json";

        // Шаг 1: получаем прямую ссылку для скачивания
        Request linkRequest = new Request.Builder()
                .url("https://cloud-api.yandex.net/v1/disk/resources/download?path=" + path)
                .header("Authorization", "OAuth " + token)
                .build();

        try (Response linkResponse = httpClient.newCall(linkRequest).execute()) {
            if (linkResponse.code() == 404) {
                return null; // файла ещё нет – это нормально
            }
            if (!linkResponse.isSuccessful()) {
                throw new IOException("Ошибка получения ссылки: " + linkResponse.code() + " " + linkResponse.message());
            }

            String body = linkResponse.body().string();
            String downloadUrl;
            try {
                JSONObject json = new JSONObject(body);
                downloadUrl = json.getString("href");
            } catch (JSONException e) {
                throw new IOException("Ошибка парсинга JSON при получении ссылки: " + e.getMessage(), e);
            }

            // Шаг 2: скачиваем сам файл по прямой ссылке (без авторизации)
            Request fileRequest = new Request.Builder().url(downloadUrl).build();
            try (Response fileResponse = httpClient.newCall(fileRequest).execute()) {
                if (!fileResponse.isSuccessful()) {
                    throw new IOException("Ошибка скачивания файла: " + fileResponse.code());
                }
                return fileResponse.body().string();
            }
        }
    }

    /**
     * Безопасная загрузка JSON: сначала удаляет возможные остатки (папку/файл),
     * затем создаёт новый файл через механизм upload-ссылки.
     * Используется для загрузки временных файлов перед перемещением.
     *
     * @param tableName имя файла без расширения
     * @param json      строка JSON для загрузки
     * @throws IOException при ошибке
     */
    public void safeUploadJson(String tableName, String json) throws IOException {
        String path = "app:/" + tableName + ".json";

        // Пытаемся удалить возможные остатки (файл или папку)
        try {
            deleteFile(tableName);
        } catch (IOException e) {
            // Логируем, но не прерываем — если нечего удалять, это ок
            Log.w("YandexDiscClient", "Не удалось удалить перед загрузкой: " + e.getMessage());
        }

        // Получаем ссылку для загрузки с overwrite (на случай, если удаление не сработало)
        Request linkRequest = new Request.Builder()
                .url("https://cloud-api.yandex.net/v1/disk/resources/upload?path=" + path + "&overwrite=true")
                .header("Authorization", "OAuth " + token)
                .get()
                .build();

        String uploadUrl;
        try (Response linkResponse = httpClient.newCall(linkRequest).execute()) {
            if (!linkResponse.isSuccessful()) {
                throw new IOException("Ошибка получения upload-ссылки: " + linkResponse.code());
            }
            String body = linkResponse.body().string();
            try {
                JSONObject jsonObj = new JSONObject(body);
                uploadUrl = jsonObj.getString("href");
            } catch (JSONException e) {
                throw new IOException("Ошибка парсинга ответа", e);
            }
        }

        // Загружаем содержимое
        RequestBody requestBody = RequestBody.create(json, MediaType.parse("application/json; charset=utf-8"));
        Request uploadRequest = new Request.Builder()
                .url(uploadUrl)
                .put(requestBody)
                .build();

        try (Response uploadResponse = httpClient.newCall(uploadRequest).execute()) {
            if (!uploadResponse.isSuccessful()) {
                throw new IOException("Ошибка загрузки файла: " + uploadResponse.code());
            }
        }
    }

    /**
     * Удаляет файл из папки приложения. Если файла нет — молча выходит.
     *
     * @param tableName имя файла без расширения
     * @throws IOException при сетевой ошибке
     */
    public void deleteFile(String tableName) throws IOException {
        String path = "app:/" + tableName + ".json";
        Request request = new Request.Builder()
                .url("https://cloud-api.yandex.net/v1/disk/resources?path=" + path + "&permanently=true")
                .header("Authorization", "OAuth " + token)
                .delete()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.code() == 404) return; // файла нет – это нормально
            if (!response.isSuccessful()) {
                throw new IOException("Ошибка удаления: " + response.code() + " " + response.message());
            }
        }
    }

    /**
     * Перемещает/переименовывает файл из fromName в toName с перезаписью.
     * Используется для замены оригинального файла таблицы временным.
     *
     * @param fromName имя файла-источника без расширения (например, "plants_temp_123")
     * @param toName   имя целевого файла без расширения (например, "plants")
     * @throws IOException при ошибке
     */
    public void moveFile(String fromName, String toName) throws IOException {
        String fromPath = "app:/" + fromName + ".json";
        String toPath = "app:/" + toName + ".json";
        Request request = new Request.Builder()
                .url("https://cloud-api.yandex.net/v1/disk/resources/move?from=" + fromPath + "&path=" + toPath + "&overwrite=true")
                .header("Authorization", "OAuth " + token)
                .post(RequestBody.create(null, ""))
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Ошибка перемещения: " + response.code() + " " + response.message());
            }
        }
    }

    /**
     * Возвращает время последнего изменения файла на сервере в миллисекундах (Unix time).
     * Если файла нет – возвращает 0.
     *
     * @param tableName имя файла
     * @return время в миллисекундах
     * @throws IOException при сетевых ошибках
     */
    public long getServerModifiedTime(String tableName) throws IOException {
        String path = "app:/" + tableName + ".json";
        Request request = new Request.Builder()
                .url("https://cloud-api.yandex.net/v1/disk/resources?path=" + path + "&fields=modified")
                .header("Authorization", "OAuth " + token)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.code() == 404) {
                return 0; // файл отсутствует
            }
            if (!response.isSuccessful()) {
                throw new IOException("Ошибка получения метаданных: " + response.code());
            }
            String body = response.body().string();
            String modifiedStr;
            try {
                JSONObject json = new JSONObject(body);
                modifiedStr = json.getString("modified");
            } catch (JSONException e) {
                throw new IOException("Ошибка парсинга JSON с временем изменения: " + e.getMessage(), e);
            }
            return parseIso8601ToMillis(modifiedStr);
        }
    }

    /**
     * Преобразует ISO-дату вида "2025-01-15T10:20:30+00:00" в миллисекунды.
     */
    private long parseIso8601ToMillis(String isoDate) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US);
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = sdf.parse(isoDate);
            return date.getTime();
        } catch (ParseException e) {
            Log.e("YandexDiscClient", "Ошибка парсинга даты: " + isoDate, e);
            return 0;
        }
    }
}