package com.example.plantmap.db.dataaccess;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.plantmap.db.DatabaseHelper;
import com.example.plantmap.model.FlowerColor;
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
        cv.put("flower_color", plant.flowerColorId);
        cv.put("additional_info", plant.additionalInfo);
        cv.put("public_key", plant.imagePublicKey);

        long result = db.insert("plants", null, cv);
        //Log.d("ADD_PLANT", "Insert result: " + result + ", name: " + plant.name);

        return result;
    }

    // Обновление растения
    public void updatePlant(Plant plant) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        long varietyId = getOrCreateVarietyId(db, plant.type, plant.group);

        ContentValues cv = new ContentValues();
        cv.put("name", plant.name);
        cv.put("variety_id", varietyId);
        cv.put("flower_color", plant.flowerColorId);
        cv.put("additional_info", plant.additionalInfo);
        cv.put("public_key", plant.imagePublicKey);

        db.update("plants", cv, "id=?", new String[]{String.valueOf(plant.id)});
    }

    public void addPlantVolume(int plantId, int potVolume) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("plant_id", plantId);
        cv.put("pot_volume", potVolume);
        db.insertWithOnConflict("plant_pot_volumes", null, cv, SQLiteDatabase.CONFLICT_IGNORE);
    }

    public void removePlantVolume(int plantId, int potVolume) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("plant_pot_volumes", "plant_id=? AND pot_volume=?",
                new String[]{String.valueOf(plantId), String.valueOf(potVolume)});
    }

    public List<Integer> getPotVolumesForPlant(int plantId) {
        List<Integer> volumes = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT pot_volume FROM plant_pot_volumes WHERE plant_id=? ORDER BY pot_volume",
                new String[]{String.valueOf(plantId)});
        while (c.moveToNext()) {
            volumes.add(c.getInt(0));
        }
        c.close();
        return volumes;
    }

    // Получение всех растений (для: автокомплит)
    public List<Plant> getAllPlants() {
        List<Plant> plants = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        // 1. Получаем все растения с их variety-данными
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

            int colorIndex = c.getColumnIndexOrThrow("flower_color");
            p.flowerColorId = c.isNull(colorIndex)
                    ? 9
                    : c.getInt(colorIndex);

            p.additionalInfo = c.getString(c.getColumnIndexOrThrow("additional_info"));

            p.imagePublicKey = c.getString(c.getColumnIndexOrThrow("public_key"));

            plants.add(p);
        }
        c.close();

        // 2. Загружаем все объёмы для всех растений одним запросом
        Cursor volCursor = db.rawQuery(
                "SELECT plant_id, pot_volume " +
                        "FROM plant_pot_volumes " +
                        "ORDER BY plant_id, pot_volume",
                null);
        // Сопоставляем объёмы с растениями
        java.util.Map<Integer, Plant> plantMap = new java.util.HashMap<>();
        for (Plant p : plants) {
            plantMap.put(p.id, p);
        }
        while (volCursor.moveToNext()) {
            int plantId = volCursor.getInt(0);
            int volume = volCursor.getInt(1);
            Plant p = plantMap.get(plantId);
            if (p != null) {
                p.availablePotVolumes.add(volume);
            }
        }
        volCursor.close();

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

    // Для проверки наличия растения, чтобы создавалось новое растение при новом цвете цветка например
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
                        "flower_color=? AND " +
                        "COALESCE(additional_info, '')=?"
        );

        List<String> args = new ArrayList<>();
        args.add(plant.name != null ? plant.name : "");
        args.add(plant.type != null ? plant.type : "");
        args.add(plant.group != null ? plant.group : "");
        args.add(String.valueOf(plant.flowerColorId));
        args.add(plant.additionalInfo != null ? plant.additionalInfo : "");

        Cursor cursor = db.rawQuery(query.toString(), args.toArray(new String[0]));

        if (cursor.moveToFirst()) {
            result = new Plant();
            result.id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
            result.name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
            result.type = cursor.getString(cursor.getColumnIndexOrThrow("type"));
            result.group = cursor.getString(cursor.getColumnIndexOrThrow("plant_group"));

            int colorIndex = cursor.getColumnIndexOrThrow("flower_color");
            result.flowerColorId = cursor.isNull(colorIndex)
                    ? 9
                    : cursor.getInt(colorIndex);

            result.additionalInfo = cursor.getString(cursor.getColumnIndexOrThrow("additional_info"));
            result.imagePublicKey = cursor.getString(cursor.getColumnIndexOrThrow("public_key"));
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
            Integer flowerColorId,
            String additionalInfo
    ) {
        List<Plant> plants = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        StringBuilder where = new StringBuilder("1=1");
        List<String> args = new ArrayList<>();

        if (name != null && !name.isEmpty()) {
            where.append(" AND p.name LIKE ?");
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
            // Фильтр по объёму: ищем растения, у которых есть такой объём
            where.append(" AND p.id IN (SELECT plant_id FROM plant_pot_volumes WHERE pot_volume=?)");
            args.add(String.valueOf(potVolume));
        }

        if (flowerColorId != null) {
            where.append(" AND p.flower_color = ?");
            args.add(String.valueOf(flowerColorId));
        }

        if (additionalInfo != null && !additionalInfo.isEmpty()) {
            where.append(" AND p.additional_info LIKE ?");
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

            int colorIndex = c.getColumnIndexOrThrow("flower_color");
            p.flowerColorId = c.isNull(colorIndex)
                    ? 9
                    : c.getInt(colorIndex);

            p.additionalInfo = c.getString(c.getColumnIndexOrThrow("additional_info"));
            p.imagePublicKey = c.getString(c.getColumnIndexOrThrow("public_key"));

            plants.add(p);
        }

        c.close();

        // Загружаем объёмы для найденных растений
        if (!plants.isEmpty()) {
            // Собираем список id
            StringBuilder idList = new StringBuilder();
            for (int i = 0; i < plants.size(); i++) {
                if (i > 0) idList.append(",");
                idList.append(plants.get(i).id);
            }
            Cursor volCursor = db.rawQuery(
                    "SELECT plant_id, pot_volume " +
                            "FROM plant_pot_volumes " +
                            "WHERE plant_id IN (" + idList + ") " +
                            "ORDER BY plant_id, pot_volume",
                    null);
            java.util.Map<Integer, Plant> plantMap = new java.util.HashMap<>();
            for (Plant p : plants) plantMap.put(p.id, p);
            while (volCursor.moveToNext()) {
                int plantId = volCursor.getInt(0);
                int volume = volCursor.getInt(1);
                Plant p = plantMap.get(plantId);
                if (p != null && p.availablePotVolumes != null) {
                    p.availablePotVolumes.add(volume);
                }
            }
            volCursor.close();
        }

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

    // Простой контейнер для строки таблицы colors
    public static class ColorInfo {
        public int id;
        public String name;
        public String hex;
    }

    // Получить все цвета (id, name, hex)
    public List<FlowerColor> getAllColors() {
        List<FlowerColor> colors = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT id, name, hex FROM colors ORDER BY id", null);
        while (c.moveToNext()) {
            colors.add(new FlowerColor(c.getInt(0), c.getString(1), c.getString(2)));
        }
        c.close();
        return colors;
    }

    // Только названия (для адаптера AutoCompleteTextView)
    public List<String> getAllColorNames() {
        List<String> names = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT name FROM colors ORDER BY id", null);
        while (c.moveToNext()) {
            names.add(c.getString(0));
        }
        c.close();
        return names;
    }

    public void replacePlantVolumes(int plantId, List<Integer> volumes) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete("plant_pot_volumes", "plant_id=?", new String[]{String.valueOf(plantId)});
            for (Integer vol : volumes) {
                if (vol != null) {
                    addPlantVolume(plantId, vol);
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public boolean canDeleteVolume(int plantId, int potVolume) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT COUNT(*) FROM points WHERE plant_id=? AND pot_volume=?",
                new String[]{String.valueOf(plantId), String.valueOf(potVolume)});
        int count = 0;
        if (c.moveToFirst()) count = c.getInt(0);
        c.close();
        return count == 0;
    }
}
