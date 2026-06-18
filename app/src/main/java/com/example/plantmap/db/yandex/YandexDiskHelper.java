package com.example.plantmap.db.yandex;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONObject;

public class YandexDiskHelper {
    private static final OkHttpClient client = new OkHttpClient();

    public static String getDirectUrl(String publicKey) {
        String fullKey = publicKey.startsWith("http")
                ? publicKey
                : "https://disk.yandex.ru/i/" + publicKey;
        String url =
                "https://cloud-api.yandex.net/v1/disk/public/resources/download?public_key="
                        + fullKey;
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