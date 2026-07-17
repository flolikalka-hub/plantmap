package com.example.plantmap.db.yandex;

import android.content.Context;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class CacheImages {
    private final File cacheDir;
    private final OkHttpClient httpClient = new OkHttpClient();

    public CacheImages(Context context) {
        cacheDir = new File(context.getCacheDir(), "images");
        if (!cacheDir.exists()) cacheDir.mkdirs();
    }

    /**
     * Возвращает файл изображения из кэша
     * если его нет скачивает и сохраняет
     *
     * @param publicKey публичный ключ Яндекс.Диска
     * @return файл с изображением или null при ошибке
     */
    public File getCachedImage(String publicKey) throws Exception {
        // MD5-хэш от publicKey
        String fileName = md5(publicKey) + ".jpg";
        File imageFile = new File(cacheDir, fileName);

        // если уже есть в кэше возвращаем
        if (imageFile.exists()) {
            return imageFile;
        }

        // получаем прямую ссылку и скачиваем
        String directUrl = YandexDiskHelper.getDirectUrl(publicKey);
        if (directUrl == null) return null;
        Request request = new Request.Builder().url(directUrl).build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) return null;
            InputStream is = response.body().byteStream();
            FileOutputStream fos = new FileOutputStream(imageFile);
            byte[] buffer = new byte[4096];
            int len;
            while ((len = is.read(buffer)) != -1) {
                fos.write(buffer, 0, len);
            }
            fos.close();
            is.close();
            return imageFile;
        }
    }

    /** Очистка кэша */
    public void clearCache() {
        if (cacheDir.isDirectory()) {
            for (File f : cacheDir.listFiles()) f.delete();
        }
    }

    private static String md5(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] digest = md.digest(input.getBytes("UTF-8"));
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
