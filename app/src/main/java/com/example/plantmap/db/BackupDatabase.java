package com.example.plantmap.db;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class BackupDatabase {

    private Context context;
    private File dbFile;
    public BackupDatabase(Context context) {
        this.context = context;
        this.dbFile = context.getDatabasePath("PlantMap_DB.db");
    }

    public void exportDatabase() {

        try {
            // в общую папку загрузок
            File downloads = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS);

            if (!downloads.exists()) {
                downloads.mkdirs();
            }
            // файл с подписью день-месяц-год_час-минута
            String timestamp = new SimpleDateFormat(
                    "dd-MM-yyyy_HH-mm",
                    Locale.getDefault()
            ).format(new Date());

            File exportFile = new File(
                    downloads,
                    "PlantMap_" + timestamp + ".db"
            );

            // копирование
            try (InputStream in = new FileInputStream(dbFile);
                 OutputStream out = new FileOutputStream(exportFile)) {

                byte[] buffer = new byte[4096];
                int length;

                while ((length = in.read(buffer)) > 0) {
                    out.write(buffer, 0, length);
                }
            }
            // маленькое всплывающее окно внизу экрана
            Toast.makeText(context,
                    "БД сохранена: " + exportFile.getAbsolutePath(),
                    Toast.LENGTH_LONG).show();

            // показываем меню «Поделиться»
            shareExportedFile(exportFile);

        } catch (IOException e) {
            Toast.makeText(context, "Ошибка сохранения БД", Toast.LENGTH_LONG).show();
        }
    }
    /**
     * Отправляет Intent.ACTION_SEND для экспортированного файла.
     * Пользователь увидит стандартное окно «Поделиться».
     */
    private void shareExportedFile(File file) {
        try {
            Uri fileUri;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                fileUri = FileProvider.getUriForFile(
                        context,
                        context.getPackageName() + ".fileprovider",
                        file);
            } else {
                fileUri = Uri.fromFile(file);
            }

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/octet-stream");  // для .db файла
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            // Запускаем окно выбора приложения
            context.startActivity(Intent.createChooser(shareIntent, "Поделиться базой данных"));

        } catch (Exception e) {
            Toast.makeText(context,
                    "Не удалось поделиться: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }
}