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

public class PlantPhotoLoader {
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    public static void loadPlantPhoto(Context context, Plant plant, ImageView imageView, Runnable onError) {
        // Ставим плейсхолдер на время загрузки
        Glide.with(context)
                .load(R.drawable.loading)
                .into(imageView);

        // Получаем прямую ссылку в фоновом потоке
        executor.execute(() -> {
            String directUrl = YandexDiskHelper.getDirectUrl(plant.getImagePublicKey());
            // Возвращаемся в UI-поток
            imageView.post(() -> {
                if (directUrl != null) {
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

    private static boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        Network network = cm.getActiveNetwork();
        if (network == null) return false;
        NetworkCapabilities caps = cm.getNetworkCapabilities(network);
        return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }
}