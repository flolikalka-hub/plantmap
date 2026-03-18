package com.example.plantmap.db;

import android.content.Context;
import android.os.Environment;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

public class BackupDatabase {

    private Context context;
    private DatabaseHelper dbHelper;

    public BackupDatabase(Context context, DatabaseHelper dbHelper) {
        this.context = context;
        this.dbHelper = dbHelper;
    }

    public void exportDatabase() {

        try {

            // закрываем БД чтобы SQLite записал изменения
            dbHelper.close();

            File dbFile = context.getDatabasePath("PlantMap_DB.db");

            File downloads = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS);

            if (!downloads.exists()) {
                downloads.mkdirs();
            }

            File exportFile = new File(downloads, "PlantMap_DB_backup.db");

            if (exportFile.exists()) {
                exportFile.delete();
            };

            try (InputStream in = new FileInputStream(dbFile);
                 OutputStream out = new FileOutputStream(exportFile)) {

                byte[] buffer = new byte[4096];
                int length;

                while ((length = in.read(buffer)) > 0) {
                    out.write(buffer, 0, length);
                }
            }

            Toast.makeText(context,
                    "БД сохранена: " + exportFile.getAbsolutePath(),
                    Toast.LENGTH_LONG).show();

        } catch (IOException e) {
            Toast.makeText(context, "Ошибка сохранения БД", Toast.LENGTH_LONG).show();
            // e.printStackTrace();
            //Toast.makeText(context, "Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}