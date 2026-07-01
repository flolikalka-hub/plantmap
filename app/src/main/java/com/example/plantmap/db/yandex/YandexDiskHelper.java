package com.example.plantmap.db.yandex;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONObject;

/**
 * Утилита для получения прямой ссылки на скачивание файла с Яндекс.Диска
 * по публичной ссылке (public_key).
 *
 * Использует REST API Яндекс.Диска:
 * https://cloud-api.yandex.net/v1/disk/public/resources/download?public_key=...
 *
 * TODO: Добавить кэширование прямых ссылок (например, в HashMap с ограниченным временем жизни),
 *       чтобы не выполнять лишние запросы к API для одного и того же public_key.
 */
public class YandexDiskHelper {
    private static final OkHttpClient client = new OkHttpClient();

    /**
     * Возвращает прямую ссылку (href) для скачивания файла по публичной ссылке.
     *
     * @param publicKey полный URL публичной ссылки (например, https://disk.yandex.ru/d/...)
     * @return прямая ссылка или null при ошибке
     */
    public static String getDirectUrl(String publicKey) {
        String url =
                "https://cloud-api.yandex.net/v1/disk/public/resources/download?public_key="
                        + publicKey;
        Request request = new Request.Builder().url(url).build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) return null;
            String jsonData = response.body().string();
            JSONObject json = new JSONObject(jsonData);
            return json.optString("href", null);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}