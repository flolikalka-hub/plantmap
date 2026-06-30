package com.example.plantmap.db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.io.*;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "PlantMap_DB.db";
    private static final int DB_VERSION = 32;
    private final Context context;
    private String dbPath;

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        this.context = context;
        dbPath = context.getDatabasePath(DB_NAME).getPath();
        try {
            copyDatabaseIfNeeded();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void copyDatabaseIfNeeded() throws IOException {
        File dbFile = new File(dbPath);

        if (dbFile.exists()) {
            int existingVersion = getDatabaseVersionFromFile(dbFile);
            if (existingVersion >= 32) {
                return; // файл актуален
            }
            // Удаляем устаревший файл БД и связанные журналы
            context.deleteDatabase(DB_NAME);
        }

        dbFile.getParentFile().mkdirs();

        try (InputStream is = context.getAssets().open(DB_NAME);
             OutputStream os = new FileOutputStream(dbFile)) {

            byte[] buffer = new byte[4096];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
            os.flush();
        }
    }

    /**
     * Считывает user_version из существующего файла БД без привлечения SQLiteOpenHelper.
     * Возвращает -1, если файл повреждён или версию не удалось прочитать.
     */
    private int getDatabaseVersionFromFile(File dbFile) {
        SQLiteDatabase tempDb = null;
        try {
            tempDb = SQLiteDatabase.openDatabase(
                    dbFile.getAbsolutePath(),
                    null,
                    SQLiteDatabase.OPEN_READONLY
            );
            try (Cursor cursor = tempDb.rawQuery("PRAGMA user_version", null)) {
                if (cursor.moveToFirst()) {
                    return cursor.getInt(0);
                }
            }
        } catch (Exception e) {
            Log.e("DBHelper", "Failed to read DB version, will replace", e);
        } finally {
            if (tempDb != null && tempDb.isOpen()) {
                tempDb.close();
            }
        }
        return -1;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createSchemaIfNeeded(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Миграции начинаются с версии, следующей за текущей актуальной.
        for (int v = oldVersion + 1; v <= newVersion; v++) {
            switch (v) {
                case 25: migrateTo25(db); break;
                case 31: migrateTo31(db); break;
                default:
                    throw new IllegalStateException("Unknown migration from " + oldVersion + " to " + newVersion);
            }
        }
    }

    private void migrateTo25(SQLiteDatabase db) {
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_plants_variety_id ON plants(variety_id)");
    }

    //Нормализация по pot_volume
    private void migrateTo31(SQLiteDatabase db) {
        db.beginTransaction();
        try {
            db.execSQL("PRAGMA foreign_keys = OFF");

            // 1. Создаём plant_pot_volumes БЕЗ внешнего ключа
            db.execSQL("CREATE TABLE IF NOT EXISTS plant_pot_volumes (" +
                    "plant_id INTEGER NOT NULL, " +
                    "pot_volume INTEGER NOT NULL, " +
                    "PRIMARY KEY(plant_id, pot_volume)" +
                    ")");

            // 2. Добавляем поле pot_volume в points
            if (!columnExists(db, "points", "pot_volume")) {
                db.execSQL("ALTER TABLE points ADD COLUMN pot_volume INTEGER");
            }

            // 3. Сохраняем в points.pot_volume значение из plants
            db.execSQL("UPDATE points SET pot_volume = (" +
                    "SELECT p.pot_volume FROM plants p WHERE p.id = points.plant_id" +
                    ")");

            // 4. Временная таблица дубликатов
            db.execSQL("CREATE TEMP TABLE IF NOT EXISTS temp_plant_dedup AS " +
                    "SELECT p.id AS old_id, g.min_id AS new_id " +
                    "FROM plants p " +
                    "JOIN (SELECT variety_id, name, flower_color, MIN(id) AS min_id " +
                    "      FROM plants GROUP BY variety_id, name, flower_color) g " +
                    "  ON p.variety_id = g.variety_id " +
                    " AND p.name = g.name " +
                    " AND p.flower_color = g.flower_color " +
                    "WHERE p.id != g.min_id");

            // 5. Переносим объёмы в plant_pot_volumes
            db.execSQL("INSERT OR IGNORE INTO plant_pot_volumes (plant_id, pot_volume) " +
                    "SELECT g.survivor_id, p.pot_volume " +
                    "FROM plants p " +
                    "JOIN (SELECT variety_id, name, flower_color, MIN(id) AS survivor_id " +
                    "      FROM plants GROUP BY variety_id, name, flower_color) g " +
                    "  ON p.variety_id = g.variety_id " +
                    " AND p.name = g.name " +
                    " AND p.flower_color = g.flower_color " +
                    "WHERE p.pot_volume IS NOT NULL");

            // ПРОВЕРКА 1
//            Cursor c = db.rawQuery("SELECT COUNT(*) FROM plant_pot_volumes", null);
//            if (c.moveToFirst()) Log.d("MIGRATE1-pot", "plant_pot_volumes count: " + c.getInt(0));
//            c.close();
//            c = db.rawQuery("SELECT COUNT(*) FROM points", null);
//            if (c.moveToFirst()) Log.d("MIGRATE1-point", "points count: " + c.getInt(0));
//            c.close();

            // 6. Обновляем points на выжившие растения
            db.execSQL("UPDATE points SET plant_id = (" +
                    "SELECT new_id FROM temp_plant_dedup WHERE old_id = points.plant_id" +
                    ") WHERE plant_id IN (SELECT old_id FROM temp_plant_dedup)");

            // 7. Удаляем дубликаты растений
            db.execSQL("DELETE FROM plants WHERE id IN (SELECT old_id FROM temp_plant_dedup)");

            // 8. Удаляем временную таблицу
            db.execSQL("DROP TABLE IF EXISTS temp_plant_dedup");

            // 9. Пересоздаём points БЕЗ внешнего ключа (чтобы отвязаться от старой plants)
            db.execSQL("ALTER TABLE points RENAME TO points_old");
            db.execSQL("CREATE TABLE points (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "x REAL NOT NULL, " +
                    "y REAL NOT NULL, " +
                    "count INTEGER NOT NULL, " +
                    "plant_id INTEGER NOT NULL, " +
                    "processing_date INTEGER, " +
                    "feeding_date INTEGER, " +
                    "pot_volume INTEGER" +
                    ")");
            // ЯВНО перечисляем столбцы при копировании
            db.execSQL("INSERT INTO points (id, x, y, count, plant_id, processing_date, feeding_date, pot_volume) " +
                    "SELECT id, x, y, count, plant_id, processing_date, feeding_date, pot_volume " +
                    "FROM points_old");
            db.execSQL("DROP TABLE points_old");

            // 10. Пересоздаём plants без pot_volume
            db.execSQL("ALTER TABLE plants RENAME TO plants_old");
            db.execSQL("CREATE TABLE plants (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "variety_id INTEGER NOT NULL, " +
                    "name TEXT NOT NULL, " +
                    "flower_color INTEGER DEFAULT 9, " +
                    "additional_info TEXT, " +
                    "is_builtin INTEGER NOT NULL DEFAULT 0, " +
                    "public_key TEXT, " +
                    "name_rosebook TEXT, " +
                    "FOREIGN KEY(flower_color) REFERENCES colors(id), " +
                    "FOREIGN KEY(variety_id) REFERENCES variety(id) ON DELETE CASCADE" +
                    ")");
            db.execSQL("INSERT INTO plants (id, variety_id, name, flower_color, additional_info, is_builtin, public_key, name_rosebook) " +
                    "SELECT id, variety_id, name, flower_color, additional_info, is_builtin, public_key, name_rosebook " +
                    "FROM plants_old");
            db.execSQL("DROP TABLE plants_old");

            // 11. Добавляем внешний ключ в points (на новую plants)
            db.execSQL("ALTER TABLE points RENAME TO points_old");
            db.execSQL("CREATE TABLE points (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "x REAL NOT NULL, " +
                    "y REAL NOT NULL, " +
                    "count INTEGER NOT NULL, " +
                    "plant_id INTEGER NOT NULL, " +
                    "processing_date INTEGER, " +
                    "feeding_date INTEGER, " +
                    "pot_volume INTEGER, " +
                    "FOREIGN KEY(plant_id) REFERENCES plants(id) ON DELETE CASCADE" +
                    ")");
            db.execSQL("INSERT INTO points (id, x, y, count, plant_id, processing_date, feeding_date, pot_volume) " +
                    "SELECT id, x, y, count, plant_id, processing_date, feeding_date, pot_volume " +
                    "FROM points_old");
            db.execSQL("DROP TABLE points_old");

            // 12. Добавляем внешний ключ в plant_pot_volumes
            db.execSQL("ALTER TABLE plant_pot_volumes RENAME TO ppv_old");
            db.execSQL("CREATE TABLE plant_pot_volumes (" +
                    "plant_id INTEGER NOT NULL, " +
                    "pot_volume INTEGER NOT NULL, " +
                    "PRIMARY KEY(plant_id, pot_volume), " +
                    "FOREIGN KEY(plant_id) REFERENCES plants(id) ON DELETE CASCADE" +
                    ")");
            db.execSQL("INSERT INTO plant_pot_volumes (plant_id, pot_volume) " +
                    "SELECT plant_id, pot_volume FROM ppv_old");
            db.execSQL("DROP TABLE ppv_old");

            // 13. Индексы
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_points_plant_id ON points(plant_id)");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_plants_variety_id ON plants(variety_id)");

            // ПРОВЕРКА 2
//            c = db.rawQuery("SELECT COUNT(*) FROM plant_pot_volumes", null);
//            if (c.moveToFirst()) Log.d("MIGRATE2-pot", "plant_pot_volumes count: " + c.getInt(0));
//            c.close();
//            c = db.rawQuery("SELECT COUNT(*) FROM points", null);
//            if (c.moveToFirst()) Log.d("MIGRATE2-point", "points count: " + c.getInt(0));
//            c.close();

            db.execSQL("PRAGMA foreign_keys = ON");
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private void createSchemaIfNeeded(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS variety (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "type TEXT, " +
                "plant_group TEXT" +
                ")");
        db.execSQL("CREATE TABLE IF NOT EXISTS colors (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT, " +
                "hex TEXT" +
                ")");
        db.execSQL("CREATE TABLE IF NOT EXISTS plants (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "variety_id INTEGER NOT NULL, " +
                "name TEXT NOT NULL, " +
                "flower_color INTEGER DEFAULT 9, " +
                "additional_info TEXT, " +
                "is_builtin INTEGER NOT NULL DEFAULT 0, " +
                "public_key TEXT, " +
                "name_rosebook TEXT, " +
                "FOREIGN KEY(flower_color) REFERENCES colors(id), " +
                "FOREIGN KEY(variety_id) REFERENCES variety(id) ON DELETE CASCADE" +
                ")");
        db.execSQL("CREATE TABLE IF NOT EXISTS plant_pot_volumes (" +
                "plant_id INTEGER NOT NULL, " +
                "pot_volume INTEGER NOT NULL, " +
                "PRIMARY KEY(plant_id, pot_volume), " +
                "FOREIGN KEY(plant_id) REFERENCES plants(id) ON DELETE CASCADE" +
                ")");
        db.execSQL("CREATE TABLE IF NOT EXISTS points (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "x REAL NOT NULL, " +
                "y REAL NOT NULL, " +
                "count INTEGER NOT NULL, " +
                "plant_id INTEGER NOT NULL, " +
                "processing_date INTEGER, " +
                "feeding_date INTEGER, " +
                "pot_volume INTEGER, " +
                "FOREIGN KEY(plant_id) REFERENCES plants(id) ON DELETE CASCADE" +
                ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_points_plant_id ON points(plant_id)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_plants_variety_id ON plants(variety_id)");
    }

    private boolean columnExists(SQLiteDatabase db, String tableName, String columnName) {
        try (Cursor cursor = db.rawQuery("PRAGMA table_info(" + tableName + ")", null)) {
            int nameIndex = cursor.getColumnIndex("name");
            while (cursor.moveToNext()) {
                if (columnName.equals(cursor.getString(nameIndex))) {
                    return true;
                }
            }
            return false;
        }
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }
}