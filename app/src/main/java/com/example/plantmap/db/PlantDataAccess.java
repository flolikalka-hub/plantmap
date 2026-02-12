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
        ContentValues cv = new ContentValues();
        cv.put("name", plant.name);
        cv.put("type", plant.type);
        cv.put("plant_group", plant.group);
        cv.put("pot_volume", plant.potVolume);
        cv.put("flower_color", plant.flowerColor);
        cv.put("additional_info", plant.additionalInfo);
        // поле предзагруженности (is_builtin) по умолчанию 0 само проставится
        long id = db.insert("plants", null, cv);
        return id;
    }

    // Обновление растения
    public void updatePlant(Plant plant) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", plant.name);
        cv.put("type", plant.type);
        cv.put("plant_group", plant.group);
        cv.put("pot_volume", plant.potVolume);
        cv.put("flower_color", plant.flowerColor);
        cv.put("additional_info", plant.additionalInfo);
        db.update("plants", cv, "id=?", new String[]{String.valueOf(plant.id)});
    }

    // Получение всех растений (для: автокомплит)
    public List<Plant> getAllPlants() {
        List<Plant> plants = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query("plants", null, null,
                null, null, null, "name");

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
/*
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

 */
        // для исключения null добавлено COALESCE, вернет null ток если кроме null ничего нет
        StringBuilder query = new StringBuilder(
                "SELECT * FROM plants WHERE " +
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
            result.type = cursor.getString(cursor.getColumnIndexOrThrow("type"));
            result.group = cursor.getString(cursor.getColumnIndexOrThrow("plant_group"));

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

            int pvIndex = c.getColumnIndexOrThrow("pot_volume");
            p.potVolume = c.isNull(pvIndex) ? null : c.getInt(pvIndex);


            p.flowerColor = c.getString(c.getColumnIndexOrThrow("flower_color"));
            p.additionalInfo = c.getString(c.getColumnIndexOrThrow("additional_info"));
            plants.add(p);
        }

        c.close();
        return plants;
    }
}
