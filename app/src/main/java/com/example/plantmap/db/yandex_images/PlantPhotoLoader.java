package com.example.plantmap.db.yandex_images;

import android.content.Context;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.example.plantmap.R;
import com.example.plantmap.model.Plant;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Загружает фотографию растения с Яндекс.Диска по публичной ссылке.
 *
 * Процесс: сначала получает прямую ссылку через YandexDiskHelper в фоновом потоке,
 * затем отдаёт её Glide для загрузки в ImageView. На время загрузки показывает
 * плейсхолдер loading, при ошибке — loading_error.
 */
public class PlantPhotoLoader {

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static CacheImages imageCache;
    /**
     * Инициализация кэша изображений
     * @param appContext контекст приложения
     */
    public static void init(Context appContext) {
        imageCache = new CacheImages(appContext);
    }
    /**
     * Загружает фото растения в ImageView
     *
     * @param context   контекст для Glide
     * @param plant     растение, у которого берётся публичная ссылка (public_key)
     * @param imageView целевой ImageView
     * @param onError   колбэк при ошибке загрузки (может быть null)
     */
    public static void loadPlantPhoto(
            Context context,
            Plant plant,
            ImageView imageView,
            Runnable onError) {

        if (imageCache == null) {
            throw new IllegalStateException("PlantPhotoLoader не инициализирован. Вызовите init() в Application.");
        }

        // Плейсхолдер на время получения файла
        Glide.with(context)
                .load(R.drawable.loading)
                .into(imageView);

        executor.execute(() -> {
            try {
                //String directUrl = YandexDiskHelper.getDirectUrl(plant.getImagePublicKey());
                File imageFile = imageCache.getCachedImage(plant.getImagePublicKey());
                imageView.post(() -> {
                    if (imageFile != null && imageFile.exists()) {
                        // Файл есть (из кэша или только что скачанный)
                        Glide.with(context)
                                .load(imageFile)
                                .placeholder(R.drawable.loading)
                                .error(R.drawable.loading_error)
                                .into(imageView);
                    } else {
                        imageView.setImageResource(R.drawable.loading_error);
                        if (onError != null) onError.run();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                imageView.post(() -> {
                    imageView.setImageResource(R.drawable.loading_error);
                    if (onError != null) onError.run();
                });
            }
        });
    }
}