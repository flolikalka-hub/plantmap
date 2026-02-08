package com.example.plantmap.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import com.example.plantmap.model.ColorModifier;
import com.example.plantmap.model.FlowerColor;
import com.example.plantmap.model.Plant;
import com.example.plantmap.model.PlantPoint;
import java.util.ArrayList;
import java.util.List;

import java.io.*;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "PlantMap_DB.db";
    private static final int DB_VERSION = 9;

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
            if (!columnExists(db,"plants","is_builtin")){
                db.execSQL("ALTER TABLE plants ADD COLUMN is_builtin INTEGER NOT NULL DEFAULT 0");
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private void createSchemaIfNeeded(SQLiteDatabase db) {
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS plants (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "name TEXT NOT NULL, " +
                        "type TEXT, " +
                        "plant_group TEXT, " +
                        "pot_volume INTEGER, " +
                        "flower_color TEXT, " +
                        "additional_info TEXT, " +
                        "is_builtin INTEGER NOT NULL DEFAULT 0" +
                        ")"
        );

        db.execSQL(
                "CREATE TABLE IF NOT EXISTS points (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "x REAL NOT NULL, " +
                        "y REAL NOT NULL, " +
                        "count INTEGER NOT NULL DEFAULT 1, " +
                        "plant_id INTEGER NOT NULL, " +
                        "FOREIGN KEY (plant_id) REFERENCES plants(id) ON DELETE CASCADE" +
                        ")"
        );

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

    // РАСТЕНИЯ
    // Добавление растения
    public long addPlant(Plant plant) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", plant.name);
        cv.put("type", plant.type);
        cv.put("plant_group", plant.group);
        cv.put("pot_volume", plant.potVolume);
        cv.put("flower_color", plant.flowerColor);
        cv.put("additional_info", plant.additionalInfo);
        // поле предзагруженности (is_builtin) по умолчанию 0 само проставится
        long id = db.insert("plants", null, cv);
        db.close();
        return id;
    }

    // Обновление растения
    public void updatePlant(Plant plant) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", plant.name);
        cv.put("type", plant.type);
        cv.put("plant_group", plant.group);
        cv.put("pot_volume", plant.potVolume);
        cv.put("flower_color", plant.flowerColor);
        cv.put("additional_info", plant.additionalInfo);
        db.update("plants", cv, "id=?", new String[]{String.valueOf(plant.id)});
        db.close();
    }

    // Поиск растения для автозаполнения
    public Plant findPlantByName(String name) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(
                "plants",
                null,
                "name = ?",
                new String[]{name},
                null, null, null
        );

        Plant plant = null;
        if (c.moveToFirst()) {
            plant = new Plant();
            plant.id = c.getInt(c.getColumnIndexOrThrow("id"));
            plant.name = c.getString(c.getColumnIndexOrThrow("name"));
            plant.type = c.getString(c.getColumnIndexOrThrow("type"));
            plant.group = c.getString(c.getColumnIndexOrThrow("plant_group"));
            plant.potVolume = c.getInt(c.getColumnIndexOrThrow("pot_volume"));
            plant.flowerColor = c.getString(c.getColumnIndexOrThrow("flower_color"));
            plant.additionalInfo = c.getString(c.getColumnIndexOrThrow("additional_info"));
        }

        c.close();
        db.close();
        return plant;
    }

    // Получение всех растений (для: автокомплит)
    public List<Plant> getAllPlants() {
        List<Plant> plants = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query("plants", null, null,
                null, null, null, "name");

        while (c.moveToNext()) {
            Plant p = new Plant();
            p.id = c.getInt(c.getColumnIndexOrThrow("id"));
            p.name = c.getString(c.getColumnIndexOrThrow("name"));
            p.type = c.getString(c.getColumnIndexOrThrow("type"));
            p.group = c.getString(c.getColumnIndexOrThrow("plant_group"));
            p.potVolume = c.getInt(c.getColumnIndexOrThrow("pot_volume"));
            p.flowerColor = c.getString(c.getColumnIndexOrThrow("flower_color"));
            p.additionalInfo = c.getString(c.getColumnIndexOrThrow("additional_info"));
            plants.add(p);
        }

        c.close();
        db.close();
        return plants;
    }

    // Удаление растения
    public void deletePlant(int plantId) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete("plants", "id=?", new String[]{String.valueOf(plantId)});
        db.close();
    }

    // проверка на возможность удаления, что растния не числятся нигде расположенными
    public boolean canDeletePlant(int plantId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT COUNT(*) FROM points WHERE plant_id = ?",
                new String[]{String.valueOf(plantId)}
        );
        boolean canDelete = true;
        if (c.moveToFirst()) {
            canDelete = c.getInt(0) == 0;
        }
        c.close();
        db.close();
        return canDelete;
    }

    // ТОЧКИ
    // Добавление точки
    public long addPoint(PlantPoint point) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();

        cv.put("x", point.x);
        cv.put("y", point.y);
        cv.put("count", point.count);
        cv.put("plant_id", point.plant.id);

        long id = db.insert("points", null, cv); // insert возвращает id новой записи
        db.close();
        return id; // метод возвращает id
    }

    // Обновление точки
    public void updatePoint(int id, PlantPoint point) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();

        cv.put("x", point.x);
        cv.put("y", point.y);
        cv.put("count", point.count);
        cv.put("plant_id", point.plant.id);

        db.update("points", cv, "id=?", new String[]{String.valueOf(id)});
        db.close();
    }

    // Удаление точки
    public void deletePoint(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("points", "id=?", new String[]{String.valueOf(id)});
        db.close();
    }

    // Получение всех точек с ПОДТЯГИВАНИЕМ РАСТЕНИЯ
    public List<PlantPoint> getAllPoints() {
        List<PlantPoint> points = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        String sql =
                "SELECT p.id, p.x, p.y, p.count, " +
                "pl.id AS plant_id, " +
                        "pl.name, " +
                        "pl.type, " +
                        "pl.plant_group, " +
                        "pl.pot_volume, " +
                        "pl.flower_color, " +
                        "pl.additional_info " +
                "FROM points p " +
                "JOIN plants pl ON p.plant_id = pl.id";

        Cursor c = db.rawQuery(sql, null);

        while (c.moveToNext()) {
            Plant plant = new Plant();
            plant.id = c.getInt(c.getColumnIndexOrThrow("plant_id"));
            plant.name = c.getString(c.getColumnIndexOrThrow("name"));
            plant.type = c.getString(c.getColumnIndexOrThrow("type"));
            plant.group = c.getString(c.getColumnIndexOrThrow("plant_group"));
            plant.potVolume = c.getInt(c.getColumnIndexOrThrow("pot_volume"));
            plant.flowerColor = c.getString(c.getColumnIndexOrThrow("flower_color"));
            plant.additionalInfo = c.getString(c.getColumnIndexOrThrow("additional_info"));

            PlantPoint point = new PlantPoint(
                    c.getFloat(c.getColumnIndexOrThrow("x")),
                    c.getFloat(c.getColumnIndexOrThrow("y"))
            );
            point.id = c.getInt(c.getColumnIndexOrThrow("id"));
            point.count = c.getInt(c.getColumnIndexOrThrow("count"));
            point.plant = plant;

            points.add(point);
        }

        c.close();
        db.close();
        return points;
    }

    // Для проверки наличия растения, чтобы создавалось ноое растение при новом цвете цветка например
    public Plant findPlantByAllFields(Plant plant) {
        SQLiteDatabase db = getReadableDatabase();
        Plant result = null;

        String query = "SELECT * FROM plants WHERE " +
                "name=? AND " +
                "type=? AND " +
                "plant_group=? AND " +
                "pot_volume=? AND " +
                "flower_color=? AND " +
                "additional_info=?";
        Cursor cursor = db.rawQuery(query, new String[]{
                plant.name,
                plant.type,
                plant.group,
                String.valueOf(plant.potVolume),
                plant.flowerColor,
                plant.additionalInfo
        });

        if (cursor.moveToFirst()) {
            result = new Plant();
            result.id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
            result.name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
            result.type = cursor.getString(cursor.getColumnIndexOrThrow("type"));
            result.group = cursor.getString(cursor.getColumnIndexOrThrow("plant_group"));
            result.potVolume = cursor.getInt(cursor.getColumnIndexOrThrow("pot_volume"));
            result.flowerColor = cursor.getString(cursor.getColumnIndexOrThrow("flower_color"));
            result.additionalInfo = cursor.getString(cursor.getColumnIndexOrThrow("additional_info"));
        }

        cursor.close();
        db.close();
        return result;
    }

    // ЦВЕТА
    // поиск цвета по корню
    public FlowerColor findColorByRoot(String part) {
        SQLiteDatabase db = getReadableDatabase();
        FlowerColor color = null;

        Cursor c = db.rawQuery(
                "SELECT * FROM colors WHERE ? LIKE root || '%' LIMIT 1",
                new String[]{part}
        );

        if (c.moveToFirst()) {
            color = new FlowerColor();
            color.id = c.getInt(c.getColumnIndexOrThrow("id"));
            color.name = c.getString(c.getColumnIndexOrThrow("name"));
            color.root = c.getString(c.getColumnIndexOrThrow("root"));
            color.hex = c.getString(c.getColumnIndexOrThrow("hex"));
        }

        c.close();
        db.close();
        return color;
    }

    // поиск модификатора по корню
    public ColorModifier findModifierByRoot(String part) {
        SQLiteDatabase db = getReadableDatabase();
        ColorModifier modifier = null;

        Cursor c = db.rawQuery(
                "SELECT * FROM modifiers WHERE ? LIKE root || '%' LIMIT 1",
                new String[]{part}
        );

        if (c.moveToFirst()) {
            modifier = new ColorModifier();
            modifier.id = c.getInt(c.getColumnIndexOrThrow("id"));
            modifier.name = c.getString(c.getColumnIndexOrThrow("name"));
            modifier.root = c.getString(c.getColumnIndexOrThrow("root"));
            modifier.lightness = c.getDouble(c.getColumnIndexOrThrow("lightness"));
            modifier.saturation = c.getDouble(c.getColumnIndexOrThrow("saturation"));
        }

        c.close();
        db.close();
        return modifier;
    }

    // получаем все цвета
    public List<String> getAllColorNames() {
        List<String> colors = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query("colors", new String[]{"name"}, null, null, null, null, "name");

        while (c.moveToNext()) {
            colors.add(c.getString(c.getColumnIndexOrThrow("name")));
        }

        c.close();
        db.close();
        return colors;
    }

    // метод поиска
    public List<Plant> searchPlants(
            String name,
            String type,
            String group,
            Integer potVolume,
            String flowerColor,
            String additionalInfo
    ) {
        List<Plant> plants = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        StringBuilder where = new StringBuilder("1=1");
        List<String> args = new ArrayList<>();

        if (name != null && !name.isEmpty()) {
            where.append(" AND name LIKE ?");
            args.add("%" + name + "%");
        }

        if (type != null && !type.isEmpty()) {
            where.append(" AND type LIKE ?");
            args.add("%" + type + "%");
        }

        if (group != null && !group.isEmpty()) {
            where.append(" AND plant_group LIKE ?");
            args.add("%" + group + "%");
        }

        if (potVolume != null) {
            where.append(" AND pot_volume = ?");
            args.add(String.valueOf(potVolume));
        }

        if (flowerColor != null && !flowerColor.isEmpty()) {
            where.append(" AND flower_color LIKE ?");
            args.add("%" + flowerColor + "%");
        }

        if (additionalInfo != null && !additionalInfo.isEmpty()) {
            where.append(" AND additional_info LIKE ?");
            args.add("%" + additionalInfo + "%");
        }

        Cursor c = db.query(
                "plants",
                null,
                where.toString(),
                args.toArray(new String[0]),
                null, null,
                "name"
        );

        while (c.moveToNext()) {
            Plant p = new Plant();
            p.id = c.getInt(c.getColumnIndexOrThrow("id"));
            p.name = c.getString(c.getColumnIndexOrThrow("name"));
            p.type = c.getString(c.getColumnIndexOrThrow("type"));
            p.group = c.getString(c.getColumnIndexOrThrow("plant_group"));
            p.potVolume = c.getInt(c.getColumnIndexOrThrow("pot_volume"));
            p.flowerColor = c.getString(c.getColumnIndexOrThrow("flower_color"));
            p.additionalInfo = c.getString(c.getColumnIndexOrThrow("additional_info"));
            plants.add(p);
        }

        c.close();
        db.close();
        return plants;
    }

    // ЦВЕТА как отдельные объекты
    public List<FlowerColor> getAllColors() {
        List<FlowerColor> colors = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        Cursor c = db.query(
                "colors",
                null,
                null,
                null,
                null,
                null,
                "name"
        );

        while (c.moveToNext()) {
            FlowerColor color = new FlowerColor();
            color.id = c.getInt(c.getColumnIndexOrThrow("id"));
            color.name = c.getString(c.getColumnIndexOrThrow("name"));
            color.root = c.getString(c.getColumnIndexOrThrow("root"));
            color.hex = c.getString(c.getColumnIndexOrThrow("hex"));
            colors.add(color);
        }

        c.close();
        db.close();
        return colors;
    }
    // Добавление нового цвета
    public long insertColor(String name, String root, String hex) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", name.toLowerCase().trim());
        cv.put("root", root != null ? root.toLowerCase().trim() : null);
        cv.put("hex", hex != null ? hex.toLowerCase().trim() : null);
        long id = db.insert("colors", null, cv); // вернет -1 если не удалось (дубликат прим)
        db.close();
        return id;
    }

    // Обновление существующего цвета
    public void updateColor(int id, String name, String root, String hex) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", name.toLowerCase().trim());
        cv.put("root", root != null ? root.toLowerCase().trim() : null);
        cv.put("hex", hex != null ? hex.toLowerCase().trim() : null);
        db.update("colors", cv, "id=?", new String[]{String.valueOf(id)});
        db.close();
    }

    public void deleteColor(int id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete("colors", "id=?", new String[]{String.valueOf(id)});
        db.close();
    }

    public boolean colorNameExists(String name, @Nullable Integer excludeId) {
        SQLiteDatabase db = getReadableDatabase();

        String normalized = name.toLowerCase().trim();

        String sql;
        String[] args;

        if (excludeId == null) {
            sql = "SELECT 1 FROM colors WHERE LOWER(name) = ? LIMIT 1";
            args = new String[]{ normalized };
        } else {
            sql = "SELECT 1 FROM colors WHERE LOWER(name) = ? AND id != ? LIMIT 1";
            args = new String[]{ normalized, String.valueOf(excludeId) };
        }

        Cursor cursor = db.rawQuery(sql, args);
        boolean exists = cursor.moveToFirst();
        cursor.close();

        return exists;
    }

    public boolean isColorUsed(String colorName) {
        SQLiteDatabase db = getReadableDatabase();

        String sql = "SELECT 1 FROM plants WHERE LOWER(flower_color) = ? LIMIT 1";
        String[] args = new String[]{ colorName.toLowerCase().trim() };

        Cursor cursor = db.rawQuery(sql, args);
        boolean used = cursor.moveToFirst();
        cursor.close();

        return used;
    }

}