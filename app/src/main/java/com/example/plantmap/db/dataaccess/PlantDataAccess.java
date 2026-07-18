package com.example.plantmap.db.dataaccess;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.plantmap.db.DatabaseHelper;
import com.example.plantmap.model.FlowerColor;
import com.example.plantmap.model.Plant;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Доступ к данным таблицы plants и связанным таблицам (variety, colors, plant_pot_volumes).
 * Управляет CRUD-операциями для растений, поиском, фильтрацией и работой с объёмами горшков.
 */
public class PlantDataAccess {
    private final DatabaseHelper dbHelper;

    public PlantDataAccess(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    /**
     * Добавляет новое растение. Если связка (type, group) отсутствует в variety,
     * создаёт новую запись в variety.
     *
     * @return сгенерированный идентификатор растения
     */
    public String addPlant(Plant plant) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        String varietyId = getOrCreateVarietyId(db, plant.type, plant.group);

        String uuid = UUID.randomUUID().toString();
        ContentValues cv = new ContentValues();
        cv.put("id", uuid);
        cv.put("name", plant.name);
        cv.put("variety_id", varietyId);
        cv.put("flower_color", plant.flowerColorId);
        cv.put("additional_info", plant.additionalInfo);
        cv.put("public_key", plant.imagePublicKey);
        cv.put("last_modified", System.currentTimeMillis());

        db.insert("plants", null, cv);
        return uuid;
    }

    /**
     * Обновляет данные растения по его id.
     */
    public void updatePlant(Plant plant) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        String varietyId = getOrCreateVarietyId(db, plant.type, plant.group);

        ContentValues cv = new ContentValues();
        cv.put("name", plant.name);
        cv.put("variety_id", varietyId);
        cv.put("flower_color", plant.flowerColorId);
        cv.put("additional_info", plant.additionalInfo);
        cv.put("public_key", plant.imagePublicKey);
        cv.put("last_modified", System.currentTimeMillis());

        db.update("plants", cv, "id=?", new String[]{plant.id});
    }

    /**
     * Добавляет объём горшка для растения (игнорируется, если уже существует).
     */
    public void addPlantVolume(String plantId, int potVolume) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        String uuid = UUID.randomUUID().toString();
        ContentValues cv = new ContentValues();
        cv.put("id", uuid);
        cv.put("plant_id", plantId);
        cv.put("pot_volume", potVolume);
        cv.put("last_modified", System.currentTimeMillis());
        db.insertWithOnConflict("plant_pot_volumes", null, cv, SQLiteDatabase.CONFLICT_IGNORE);
    }

    /**
     * Удаляет указанный объём горшка для растения.
     */
    public void removePlantVolume(String plantId, int potVolume) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        // Находим id удаляемой записи (UUID)
        Cursor c = db.rawQuery("SELECT id FROM plant_pot_volumes WHERE plant_id=? AND pot_volume=?",
                new String[]{plantId, String.valueOf(potVolume)});
        String recordId = null;
        if (c.moveToFirst()) recordId = c.getString(0);
        c.close();

        db.beginTransaction();
        try {
            db.delete("plant_pot_volumes", "plant_id=? AND pot_volume=?",
                    new String[]{plantId, String.valueOf(potVolume)});
            if (recordId != null) {
                db.execSQL("INSERT INTO deletions (table_name, record_id, deleted_at) VALUES ('plant_pot_volumes', ?, ?)",
                        new String[]{recordId, String.valueOf(System.currentTimeMillis())});
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Возвращает список доступных объёмов горшков для растения (отсортировано по возрастанию).
     */
    public List<Integer> getPotVolumesForPlant(String plantId) {
        List<Integer> volumes = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT pot_volume FROM plant_pot_volumes WHERE plant_id=? ORDER BY pot_volume",
                new String[]{plantId});
        while (c.moveToNext()) {
            volumes.add(c.getInt(0));
        }
        c.close();
        return volumes;
    }

    /**
     * Возвращает все растения с загруженными списками доступных объёмов горшков.
     * Используется для автокомплита и выпадающих списков.
     */
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
            p.id = c.getString(c.getColumnIndexOrThrow("id"));
            p.name = c.getString(c.getColumnIndexOrThrow("name"));
            p.type = c.getString(c.getColumnIndexOrThrow("type"));
            p.group = c.getString(c.getColumnIndexOrThrow("plant_group"));

            int colorIndex = c.getColumnIndexOrThrow("flower_color");
            p.flowerColorId = c.isNull(colorIndex) ? 9 : c.getInt(colorIndex);

            p.additionalInfo = c.getString(c.getColumnIndexOrThrow("additional_info"));
            p.imagePublicKey = c.getString(c.getColumnIndexOrThrow("public_key"));

            plants.add(p);
        }
        c.close();

        // 2. Загружаем все объёмы для всех растений одним запросом
        Cursor volCursor = db.rawQuery(
                "SELECT plant_id, pot_volume FROM plant_pot_volumes ORDER BY plant_id, pot_volume",
                null);
        Map<String, Plant> plantMap = new HashMap<>();
        for (Plant p : plants) {
            plantMap.put(p.id, p);
        }
        while (volCursor.moveToNext()) {
            String plantId = volCursor.getString(0);
            int volume = volCursor.getInt(1);
            Plant p = plantMap.get(plantId);
            if (p != null) {
                p.availablePotVolumes.add(volume);
            }
        }
        volCursor.close();

        return plants;
    }

    /**
     * Удаляет растение по id (каскадное удаление связанных точек происходит через внешний ключ).
     */
    public void deletePlant(String plantId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete("plants", "id=?", new String[]{plantId});
            db.execSQL("INSERT INTO deletions (table_name, record_id, deleted_at) VALUES ('plants', ?, ?)",
                    new String[]{plantId, String.valueOf(System.currentTimeMillis())});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Проверяет, можно ли удалить растение: нет ли точек на плане, ссылающихся на него.
     */
    public boolean canDeletePlant(String plantId) {
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

    /*
    public Plant findPlantByAllFields(Plant plant) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Plant result = null;

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
            result.flowerColorId = cursor.isNull(colorIndex) ? 9 : cursor.getInt(colorIndex);

            result.additionalInfo = cursor.getString(cursor.getColumnIndexOrThrow("additional_info"));
            result.imagePublicKey = cursor.getString(cursor.getColumnIndexOrThrow("public_key"));
        }
        cursor.close();
        return result;
    }
    */

    /**
     * Ищет растение, полностью совпадающее по всем полям (name, type, group, flower_color, additional_info).
     * Используется для проверки существования такого же растения перед созданием дубликата.
     */
    public Plant findPlantByAllFields(Plant plant) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Plant result = null;

        StringBuilder query = new StringBuilder(
                "SELECT p.*, v.type, v.plant_group FROM plants p " +
                        "LEFT JOIN variety v ON p.variety_id = v.id WHERE " +
                        "COALESCE(name, '')=? AND " +
                        "COALESCE(type, '')=? AND " +
                        "COALESCE(plant_group, '')=?"
        );
        List<String> args = new ArrayList<>();
        args.add(plant.name != null ? plant.name : "");
        args.add(plant.type != null ? plant.type : "");
        args.add(plant.group != null ? plant.group : "");

        Cursor cursor = db.rawQuery(query.toString(), args.toArray(new String[0]));
        if (cursor.moveToFirst()) {
            result = new Plant();
            result.id = cursor.getString(cursor.getColumnIndexOrThrow("id"));
            result.name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
            result.type = cursor.getString(cursor.getColumnIndexOrThrow("type"));
            result.group = cursor.getString(cursor.getColumnIndexOrThrow("plant_group"));

            int colorIndex = cursor.getColumnIndexOrThrow("flower_color");
            result.flowerColorId = cursor.isNull(colorIndex) ? 9 : cursor.getInt(colorIndex);

            result.additionalInfo = cursor.getString(cursor.getColumnIndexOrThrow("additional_info"));
            result.imagePublicKey = cursor.getString(cursor.getColumnIndexOrThrow("public_key"));
        }
        cursor.close();
        return result;
    }

    /**
     * Расширенный поиск растений по заданным критериям.
     *
     * @param name           часть названия (LIKE %name%)
     * @param type           часть типа (LIKE %type%)
     * @param group          точное совпадение группы
     * @param potVolume      фильтр по объёму горшка (точное совпадение)
     * @param flowerColorId  ID цвета
     * @param additionalInfo часть дополнительной информации
     * @return список растений, удовлетворяющих всем условиям
     */
    public List<Plant> searchPlants(
            String name, String type, String group,
            Integer potVolume, Integer flowerColorId, String additionalInfo
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
            p.id = c.getString(c.getColumnIndexOrThrow("id"));
            p.name = c.getString(c.getColumnIndexOrThrow("name"));
            p.type = c.getString(c.getColumnIndexOrThrow("type"));
            p.group = c.getString(c.getColumnIndexOrThrow("plant_group"));

            int colorIndex = c.getColumnIndexOrThrow("flower_color");
            p.flowerColorId = c.isNull(colorIndex) ? 9 : c.getInt(colorIndex);

            p.additionalInfo = c.getString(c.getColumnIndexOrThrow("additional_info"));
            p.imagePublicKey = c.getString(c.getColumnIndexOrThrow("public_key"));

            plants.add(p);
        }
        c.close();

        // Загружаем объёмы для найденных растений одним запросом
        if (!plants.isEmpty()) {
            StringBuilder idList = new StringBuilder();
            for (int i = 0; i < plants.size(); i++) {
                if (i > 0) idList.append(",");
                idList.append("'").append(plants.get(i).id).append("'");
            }
            Cursor volCursor = db.rawQuery(
                    "SELECT plant_id, pot_volume FROM plant_pot_volumes " +
                            "WHERE plant_id IN (" + idList + ") ORDER BY plant_id, pot_volume",
                    null);
            Map<String, Plant> plantMap = new HashMap<>();
            for (Plant p : plants) plantMap.put(p.id, p);
            while (volCursor.moveToNext()) {
                String plantId = volCursor.getString(0);
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

    /**
     * Возвращает id записи в variety по типу и группе, создавая её при необходимости.
     */
    private String getOrCreateVarietyId(SQLiteDatabase db, String type, String group) {
        Cursor c = db.rawQuery(
                "SELECT id FROM variety WHERE COALESCE(type,'')=? AND COALESCE(plant_group,'')=?",
                new String[]{
                        type != null ? type : "",
                        group != null ? group : ""
                }
        );
        if (c.moveToFirst()) {
            String id = c.getString(0);
            c.close();
            return id;
        }
        c.close();

        String uuid = UUID.randomUUID().toString();
        ContentValues cv = new ContentValues();
        cv.put("id", uuid);
        cv.put("type", type);
        cv.put("plant_group", group);
        cv.put("last_modified", System.currentTimeMillis());
        db.insert("variety", null, cv);
        return uuid;
    }

    /**
     * Все уникальные типы растений (из таблицы variety).
     */
    public List<String> getAllTypes() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<String> list = new ArrayList<>();
        Cursor c = db.rawQuery(
                "SELECT DISTINCT type FROM variety WHERE type IS NOT NULL ORDER BY type", null);
        while (c.moveToNext()) {
            list.add(c.getString(0));
        }
        c.close();
        return list;
    }

    /**
     * Все уникальные группы растений.
     */
    public List<String> getAllGroups() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<String> list = new ArrayList<>();
        Cursor c = db.rawQuery(
                "SELECT DISTINCT plant_group FROM variety WHERE plant_group IS NOT NULL ORDER BY plant_group", null);
        while (c.moveToNext()) {
            list.add(c.getString(0));
        }
        c.close();
        return list;
    }

    /**
     * По заданной группе возвращает соответствующий тип (если есть связь).
     */
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

    /**
     * Возвращает все цвета (id, name, hex) из таблицы colors.
     */
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

    /**
     * Только названия цветов (для адаптеров автодополнения).
     */
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

    /**
     * Заменяет список объёмов горшков для растения (удаляет старые и вставляет новые).
     */
    public void replacePlantVolumes(String plantId, List<Integer> volumes) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            // Собираем id всех текущих объёмов для удаления
            Cursor c = db.rawQuery("SELECT id FROM plant_pot_volumes WHERE plant_id=?",
                    new String[]{plantId});
            List<String> idsToDelete = new ArrayList<>();
            while (c.moveToNext()) idsToDelete.add(c.getString(0));
            c.close();

            // Удаляем старые объёмы
            db.delete("plant_pot_volumes", "plant_id=?", new String[]{plantId});

            // Записываем удалённые id в deletions
            long now = System.currentTimeMillis();
            for (String id : idsToDelete) {
                db.execSQL("INSERT INTO deletions (table_name, record_id, deleted_at) VALUES ('plant_pot_volumes', ?, ?)",
                        new String[]{id, String.valueOf(now)});
            }

            // Вставляем новые объёмы
            for (Integer vol : volumes) {
                if (vol != null) {
                    addPlantVolume(plantId, vol);  // addPlantVolume теперь генерирует UUID и last_modified
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Проверяет, можно ли удалить указанный объём горшка для растения
     * (нет ли точек на плане, использующих этот объём).
     */
    public boolean canDeleteVolume(String plantId, int potVolume) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT COUNT(*) FROM points WHERE plant_id=? AND pot_volume=?",
                new String[]{plantId, String.valueOf(potVolume)});
        int count = 0;
        if (c.moveToFirst()) count = c.getInt(0);
        c.close();
        return count == 0;
    }
}