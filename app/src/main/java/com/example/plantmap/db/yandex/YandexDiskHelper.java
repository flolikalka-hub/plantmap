package com.example.plantmap.db.yandex;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Утилита для получения прямой ссылки на скачивание файла с Яндекс.Диска
 * по публичной ссылке (public_key).
 *
 * Использует REST API Яндекс.Диска:
 * https://cloud-api.yandex.net/v1/disk/public/resources/download?public_key=...
 */
public class YandexDiskHelper {
    private static final OkHttpClient client = new OkHttpClient();
    // Кэш: ключ - publicKey, значение - объект с прямой ссылкой и временем добавления
    private static final Map<String, CachedUrl> urlCache = new HashMap<>();
    private static final long CACHE_TTL_MILLIS = 25 * 60 * 1000; // 25 минут, чтобы не превысить время жизни
    /**
     * Возвращает прямую ссылку (href) для скачивания файла по публичной ссылке
     * С учетом кэша
     *
     * @param publicKey полный URL публичной ссылки (например, https://disk.yandex.ru/d/...)
     * @return прямая ссылка или null при ошибке
     */
    public static String getDirectUrl(String publicKey) {
        // сначала проверяем кэш
        CachedUrl cached = urlCache.get(publicKey);
        if (cached != null && System.currentTimeMillis() - cached.timestamp < CACHE_TTL_MILLIS) {
            return cached.url;
        }
        // запрос в сеть если ссылка устарела или ее нет
        String url =
                "https://cloud-api.yandex.net/v1/disk/public/resources/download?public_key="
                        + publicKey;
        Request request = new Request.Builder().url(url).build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) return null;
            String jsonData = response.body().string();
            JSONObject json = new JSONObject(jsonData);
            String href = json.optString("href", null);
            if (href != null) {
                urlCache.put(publicKey, new CachedUrl(href, System.currentTimeMillis()));
            }
            return href;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static class CachedUrl {
        final String url;
        final long timestamp;
        CachedUrl(String url, long timestamp) {
            this.url = url;
            this.timestamp = timestamp;
        }
    }
}