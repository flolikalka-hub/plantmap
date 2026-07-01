package com.example.plantmap.db.yandex;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.example.plantmap.R;
import com.example.plantmap.model.Plant;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Загружает фотографию растения с Яндекс.Диска по публичной ссылке.
 *
 * Процесс: сначала получает прямую ссылку через YandexDiskHelper в фоновом потоке,
 * затем отдаёт её Glide для загрузки в ImageView. На время загрузки показывает
 * плейсхолдер loading, при ошибке — loading_error.
 *
 * TODO: Настроить кэширование Glide, чтобы избежать повторных сетевых запросов
 *       при каждом отображении.
 *       Также стоит добавить кэш на уровне прямых ссылок (мемоизация) в YandexDiskHelper,
 *       чтобы не дергать API Яндекса для одного и того же public_key.
 */
public class PlantPhotoLoader {

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    /**
     * Загружает фото растения в ImageView.
     *
     * @param context   контекст для Glide
     * @param plant     растение, у которого берётся публичная ссылка (public_key)
     * @param imageView целевой ImageView
     * @param onError   колбэк при ошибке загрузки (может быть null)
     */
    public static void loadPlantPhoto(Context context, Plant plant, ImageView imageView, Runnable onError) {
        // Плейсхолдер на время получения прямой ссылки
        Glide.with(context)
                .load(R.drawable.loading)
                .into(imageView);

        executor.execute(() -> {
            String directUrl = YandexDiskHelper.getDirectUrl(plant.getImagePublicKey());
            imageView.post(() -> {
                if (directUrl != null) {
                    // TODO: добавить .diskCacheStrategy(DiskCacheStrategy.ALL)
                    Glide.with(context)
                            .load(directUrl)
                            .placeholder(R.drawable.loading)
                            .error(R.drawable.loading_error)
                            .into(imageView);
                } else {
                    imageView.setImageResource(R.drawable.loading_error);
                    if (onError != null) onError.run();
                }
            });
        });
    }
}