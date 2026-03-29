package com.example.plantmap.db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.*;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "PlantMap_DB.db";
    private static final int DB_VERSION = 21;

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
        if (dbFile.exists()) return; // БД уже скопирована

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

    @Override
    public void onCreate(SQLiteDatabase db) {
        createSchemaIfNeeded(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.beginTransaction();
        try {
            createSchemaIfNeeded(db);

            if (!columnExists(db, "plants", "variety_id")) {

                db.execSQL("ALTER TABLE plants ADD COLUMN variety_id INTEGER");

                db.execSQL(
                        "INSERT OR IGNORE INTO variety(type, plant_group) " +
                                "SELECT DISTINCT type, plant_group FROM plants"
                );

                db.execSQL(
                        "UPDATE plants SET variety_id = (" +
                                "SELECT id FROM variety v " +
                                "WHERE v.type = plants.type AND v.plant_group = plants.plant_group" +
                                ")"
                );
            }

            if (columnExists(db,"plants","type")) {

                db.execSQL(
                        "INSERT OR IGNORE INTO variety(type, plant_group)" +
                                "SELECT DISTINCT type, plant_group FROM plants"
                );

                db.execSQL(
                        "CREATE TABLE plants_new (" +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                "name TEXT NOT NULL, " +
                                "variety_id INTEGER NOT NULL, " +
                                "pot_volume INTEGER, " +
                                "flower_color TEXT, " +
                                "additional_info TEXT, " +
                                "is_builtin INTEGER NOT NULL DEFAULT 0, " +
                                "FOREIGN KEY (variety_id) REFERENCES variety(id) ON DELETE CASCADE" +
                                ")"
                );

                db.execSQL(
                        "INSERT INTO plants_new(id,name,variety_id,pot_volume,flower_color,additional_info,is_builtin) " +
                                "SELECT p.id, p.name, v.id, p.pot_volume, p.flower_color, p.additional_info, p.is_builtin " +
                                "FROM plants p " +
                                "LEFT JOIN variety v " +
                                "ON p.type = v.type AND p.plant_group = v.plant_group"
                );

                db.execSQL("DROP TABLE plants");
                db.execSQL("ALTER TABLE plants_new RENAME TO plants");
            }

            if (!columnExists(db,"plants","is_builtin")){
                db.execSQL("ALTER TABLE plants ADD COLUMN is_builtin INTEGER NOT NULL DEFAULT 0");
            }

            if (!columnExists(db,"points","processing_date")){
                db.execSQL("ALTER TABLE points ADD COLUMN processing_date INTEGER");
            }

            if (!columnExists(db,"points","feeding_date")){
                db.execSQL("ALTER TABLE points ADD COLUMN feeding_date INTEGER");
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        Cursor c = db.rawQuery("PRAGMA table_info(plants)", null);
        while (c.moveToNext()) {
            android.util.Log.d("DB", "plants column: " + c.getString(1));
        }
        c.close();
    }

    private void createSchemaIfNeeded(SQLiteDatabase db) {
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS variety (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "type TEXT, " +
                        "plant_group TEXT" +
                        ")"
        );

        db.execSQL(
                "CREATE TABLE IF NOT EXISTS plants (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "name TEXT NOT NULL, " +
                        "variety_id INTEGER NOT NULL, " +
                        "pot_volume INTEGER, " +
                        "flower_color TEXT, " +
                        "additional_info TEXT, " +
                        "is_builtin INTEGER NOT NULL DEFAULT 0, " +
                        "FOREIGN KEY (variety_id) REFERENCES variety(id) ON DELETE CASCADE" +
                        ")"
        );

        db.execSQL(
                "CREATE TABLE IF NOT EXISTS points (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "x REAL NOT NULL, " +
                        "y REAL NOT NULL, " +
                        "count INTEGER NOT NULL, " +
                        "plant_id INTEGER NOT NULL, " +
                        "processing_date INTEGER, " +
                        "feeding_date INTEGER, " +
                        "FOREIGN KEY (plant_id) REFERENCES plants(id) ON DELETE CASCADE" +
                        ")"
        );
        // индекс для plant_id таблицы points, ускоряет операции для join
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_points_plant_id ON points(plant_id)");
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