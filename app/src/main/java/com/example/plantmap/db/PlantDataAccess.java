package com.example.plantmap.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.plantmap.model.Plant;

import java.util.ArrayList;
import java.util.List;

public class PlantDataAccess {
    private final DatabaseHelper dbHelper;

    public PlantDataAccess(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    // Добавление растения
    public long addPlant(Plant plant) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        long varietyId = getOrCreateVarietyId(db, plant.type, plant.group);

        ContentValues cv = new ContentValues();
        cv.put("name", plant.name);
        cv.put("variety_id", varietyId);
        cv.put("pot_volume", plant.potVolume);
        cv.put("flower_color", plant.flowerColor);
        cv.put("additional_info", plant.additionalInfo);

        return db.insert("plants", null, cv);
    }

    // Обновление растения
    public void updatePlant(Plant plant) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        long varietyId = getOrCreateVarietyId(db, plant.type, plant.group);

        ContentValues cv = new ContentValues();
        cv.put("name", plant.name);
        cv.put("variety_id", varietyId);
        cv.put("pot_volume", plant.potVolume);
        cv.put("flower_color", plant.flowerColor);
        cv.put("additional_info", plant.additionalInfo);

        db.update("plants", cv, "id=?", new String[]{String.valueOf(plant.id)});
    }

    // Получение всех растений (для: автокомплит)
    public List<Plant> getAllPlants() {
        List<Plant> plants = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT p.*, v.type, v.plant_group " +
                        "FROM plants p " +
                        "LEFT JOIN variety v ON p.variety_id = v.id " +
                        "ORDER BY p.name",
                null
        );

        while (c.moveToNext()) {
            Plant p = new Plant();
            p.id = c.getInt(c.getColumnIndexOrThrow("id"));
            p.name = c.getString(c.getColumnIndexOrThrow("name"));
            p.type = c.getString(c.getColumnIndexOrThrow("type"));
            p.group = c.getString(c.getColumnIndexOrThrow("plant_group"));

            int pvIndex = c.getColumnIndexOrThrow("pot_volume");
            p.potVolume = c.isNull(pvIndex) ? null : c.getInt(pvIndex);

            p.flowerColor = c.getString(c.getColumnIndexOrThrow("flower_color"));
            p.additionalInfo = c.getString(c.getColumnIndexOrThrow("additional_info"));
            plants.add(p);
        }

        c.close();
        return plants;
    }

    // Удаление растения
    public void deletePlant(int plantId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("plants", "id=?", new String[]{String.valueOf(plantId)});
    }

    // проверка на возможность удаления, что растния не числятся нигде расположенными
    public boolean canDeletePlant(int plantId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT COUNT(*) FROM points WHERE plant_id = ?",
                new String[]{String.valueOf(plantId)}
        );
        boolean canDelete = true;
        if (c.moveToFirst()) {
            canDelete = c.getInt(0) == 0;
        }
        c.close();
        return canDelete;
    }

    // Для проверки наличия растения, чтобы создавалось ноое растение при новом цвете цветка например
    public Plant findPlantByAllFields(Plant plant) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Plant result = null;
        // для исключения null добавлено COALESCE, вернет null ток если кроме null ничего нет
        StringBuilder query = new StringBuilder(
                "SELECT p.*, v.type, v.plant_group FROM plants p " +
                        "LEFT JOIN variety v ON p.variety_id = v.id WHERE " +
                        "COALESCE(name, '')=? AND " +
                        "COALESCE(type, '')=? AND " +
                        "COALESCE(plant_group, '')=? AND " +
                        "COALESCE(flower_color, '')=? AND " +
                        "COALESCE(additional_info, '')=?"
        );

        List<String> args = new ArrayList<>();
        args.add(plant.name != null ? plant.name : "");
        args.add(plant.type != null ? plant.type : "");
        args.add(plant.group != null ? plant.group : "");
        args.add(plant.flowerColor != null ? plant.flowerColor : "");
        args.add(plant.additionalInfo != null ? plant.additionalInfo : "");

        if (plant.potVolume == null) {
            query.append(" AND pot_volume IS NULL");
        } else {
            query.append(" AND pot_volume=?");
            args.add(String.valueOf(plant.potVolume));
        }

        Cursor cursor = db.rawQuery(query.toString(), args.toArray(new String[0]));

        if (cursor.moveToFirst()) {
            result = new Plant();
            result.id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
            result.name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
            result.type = cursor.getString(cursor.getColumnIndexOrThrow("v.type"));
            result.group = cursor.getString(cursor.getColumnIndexOrThrow("v.plant_group"));

            int pvIndex = cursor.getColumnIndexOrThrow("pot_volume");
            result.potVolume = cursor.isNull(pvIndex) ? null : cursor.getInt(pvIndex);


            result.flowerColor = cursor.getString(cursor.getColumnIndexOrThrow("flower_color"));
            result.additionalInfo = cursor.getString(cursor.getColumnIndexOrThrow("additional_info"));
        }

        cursor.close();
        return result;
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
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        StringBuilder where = new StringBuilder("1=1");
        List<String> args = new ArrayList<>();

        if (name != null && !name.isEmpty()) {
            where.append(" AND name LIKE ?");
            args.add("%" + name + "%");
        }

        if (type != null && !type.isEmpty()) {
            where.append(" AND v.type LIKE ?");
            args.add("%" + type + "%");
        }

        if (group != null && !group.isEmpty()) {
            where.append(" AND v.plant_group LIKE ?");
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

        String query =
                "SELECT p.*, v.type, v.plant_group " +
                        "FROM plants p " +
                        "LEFT JOIN variety v ON p.variety_id = v.id " +
                        "WHERE " + where +
                        " ORDER BY p.name";

        Cursor c = db.rawQuery(query, args.toArray(new String[0]));

        while (c.moveToNext()) {
            Plant p = new Plant();
            p.id = c.getInt(c.getColumnIndexOrThrow("id"));
            p.name = c.getString(c.getColumnIndexOrThrow("name"));
            p.type = c.getString(c.getColumnIndexOrThrow("type"));
            p.group = c.getString(c.getColumnIndexOrThrow("plant_group"));

            int pvIndex = c.getColumnIndexOrThrow("pot_volume");
            p.potVolume = c.isNull(pvIndex) ? null : c.getInt(pvIndex);


            p.flowerColor = c.getString(c.getColumnIndexOrThrow("flower_color"));
            p.additionalInfo = c.getString(c.getColumnIndexOrThrow("additional_info"));
            plants.add(p);
        }

        c.close();
        return plants;
    }

    private long getOrCreateVarietyId(SQLiteDatabase db, String type, String group) {

        Cursor c = db.rawQuery(
                "SELECT id FROM variety WHERE COALESCE(type,'')=? AND COALESCE(plant_group,'')=?",
                new String[]{
                        type != null ? type : "",
                        group != null ? group : ""
                }
        );

        if (c.moveToFirst()) {
            long id = c.getLong(0);
            c.close();
            return id;
        }
        c.close();

        ContentValues cv = new ContentValues();
        cv.put("type", type);
        cv.put("plant_group", group);

        return db.insert("variety", null, cv);
    }
    public List<String> getAllTypes() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<String> list = new ArrayList<>();

        Cursor c = db.rawQuery(
                "SELECT DISTINCT type FROM variety WHERE type IS NOT NULL ORDER BY type",
                null
        );

        while (c.moveToNext()) {
            list.add(c.getString(0));
        }
        c.close();
        return list;
    }

    public List<String> getAllGroups() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<String> list = new ArrayList<>();

        Cursor c = db.rawQuery(
                "SELECT DISTINCT plant_group FROM variety WHERE plant_group IS NOT NULL ORDER BY plant_group",
                null
        );

        while (c.moveToNext()) {
            list.add(c.getString(0));
        }
        c.close();
        return list;
    }
    public String getTypeByGroup(String group) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor c = db.rawQuery(
                "SELECT type FROM variety WHERE plant_group=? LIMIT 1",
                new String[]{group}
        );

        String result = null;
        if (c.moveToFirst()) {
            result = c.getString(0);
        }
        c.close();
        return result;
    }
}
