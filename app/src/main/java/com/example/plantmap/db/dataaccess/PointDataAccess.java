package com.example.plantmap.db.dataaccess;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.plantmap.db.DatabaseHelper;
import com.example.plantmap.model.Plant;
import com.example.plantmap.model.PlantPoint;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Доступ к данным таблицы points (точки на плане).
 * Каждая точка связана с растением (JOIN plants) и дополнительно
 * хранит pot_volume непосредственно в своей записи.
 * Все методы получения данных подтягивают информацию о растении и его виде (variety).
 */
public class PointDataAccess {
    private final DatabaseHelper dbHelper;

    public PointDataAccess(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    /**
     * Добавляет новую точку в БД.
     *
     * @param point объект PlantPoint с заполненными координатами, растением и датами
     * @return сгенерированный идентификатор записи (id)
     */
    public long addPoint(PlantPoint point) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();

        cv.put("x", point.x);
        cv.put("y", point.y);
        cv.put("count", point.count);

        if (point.processingDate == null) {
            cv.putNull("processing_date");
        } else {
            cv.put("processing_date", point.processingDate);
        }

        if (point.feedingDate == null) {
            cv.putNull("feeding_date");
        } else {
            cv.put("feeding_date", point.feedingDate);
        }

        cv.put("plant_id", point.plant.id);

        // Сохраняем выбранный объём горшка для точки
        if (point.potVolume == null) {
            cv.putNull("pot_volume");
        } else {
            cv.put("pot_volume", point.potVolume);
        }

        long id = db.insert("points", null, cv);
        return id;
    }

    /**
     * Обновляет существующую точку по её id.
     *
     * @param id    идентификатор точки
     * @param point новые данные точки
     */
    public void updatePoint(int id, PlantPoint point) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();

        cv.put("x", point.x);
        cv.put("y", point.y);
        cv.put("count", point.count);

        if (point.processingDate == null) {
            cv.putNull("processing_date");
        } else {
            cv.put("processing_date", point.processingDate);
        }

        if (point.feedingDate == null) {
            cv.putNull("feeding_date");
        } else {
            cv.put("feeding_date", point.feedingDate);
        }

        cv.put("plant_id", point.plant.id);

        if (point.potVolume == null) {
            cv.putNull("pot_volume");
        } else {
            cv.put("pot_volume", point.potVolume);
        }

        db.update("points", cv, "id=?", new String[]{String.valueOf(id)});
    }

    /**
     * Удаляет точку по идентификатору.
     */
    public void deletePoint(int id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("points", "id=?", new String[]{String.valueOf(id)});
    }

    /**
     * Преобразует текущую строку курсора в объект PlantPoint.
     * Ожидает, что курсор содержит столбцы из запросов с JOIN (растение, variety).
     */
    private PlantPoint mapFromCursor(Cursor c) {
        float x = c.getFloat(c.getColumnIndexOrThrow("x"));
        float y = c.getFloat(c.getColumnIndexOrThrow("y"));
        PlantPoint point = new PlantPoint(x, y);

        point.id = c.getInt(c.getColumnIndexOrThrow("id"));
        point.count = c.getInt(c.getColumnIndexOrThrow("count"));

        // processing_date
        int pdIndex = c.getColumnIndexOrThrow("processing_date");
        point.processingDate = c.isNull(pdIndex) ? null : c.getLong(pdIndex);

        // feeding_date
        int fdIndex = c.getColumnIndexOrThrow("feeding_date");
        point.feedingDate = c.isNull(fdIndex) ? null : c.getLong(fdIndex);

        // pot_volume — теперь из таблицы points
        int pvIndex = c.getColumnIndexOrThrow("pot_volume");
        point.potVolume = c.isNull(pvIndex) ? null : c.getInt(pvIndex);

        // Данные растения
        Plant plant = new Plant();
        plant.id = c.getInt(c.getColumnIndexOrThrow("plant_id"));
        plant.name = c.getString(c.getColumnIndexOrThrow("name"));
        plant.type = c.getString(c.getColumnIndexOrThrow("type"));
        plant.group = c.getString(c.getColumnIndexOrThrow("plant_group"));

        int colorIndex = c.getColumnIndexOrThrow("flower_color");
        plant.flowerColorId = c.isNull(colorIndex) ? 9 : c.getInt(colorIndex);

        plant.additionalInfo = c.getString(c.getColumnIndexOrThrow("additional_info"));

        int keyIndex = c.getColumnIndexOrThrow("public_key");
        plant.imagePublicKey = c.isNull(keyIndex) ? null : c.getString(keyIndex);

        // availablePotVolumes пока оставляем пустым (заполняется отдельно при необходимости)
        plant.availablePotVolumes = null;

        point.plant = plant;
        return point;
    }

    /**
     * Возвращает все точки на плане с информацией о растениях и их виде.
     */
    public List<PlantPoint> getAllPoints() {
        List<PlantPoint> points = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String sql =
                "SELECT p.*, pl.id AS plant_id, " +
                        "pl.name, " +
                        "v.type, " +
                        "v.plant_group, " +
                        "pl.flower_color, " +
                        "pl.additional_info, " +
                        "pl.public_key " +
                        "FROM points p JOIN plants pl ON p.plant_id = pl.id " +
                        "LEFT JOIN variety v ON pl.variety_id = v.id";

        Cursor c = db.rawQuery(sql, null);
        while (c.moveToNext()) {
            points.add(mapFromCursor(c));
        }
        c.close();
        return points;
    }

    /**
     * Возвращает общее количество растений (сумма count всех точек).
     */
    public int getTotalPlantCount() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT SUM(count) FROM points", null);
        int total = 0;
        if (cursor.moveToFirst()) {
            total = cursor.getInt(0);
        }
        cursor.close();
        return total;
    }

    /**
     * Возвращает суммарное количество растений, удовлетворяющих фильтрам.
     *
     * @param name      фильтр по названию растения (LIKE %name%)
     * @param type      фильтр по типу растения (LIKE %type%)
     * @param group     фильтр по группе (точное совпадение)
     * @param color     фильтр по ID цвета
     * @param potVolume фильтр по объёму горшка
     */
    public int getFilteredPlantCount(String name, String type, String group, Integer color, Integer potVolume) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        StringBuilder sql = new StringBuilder(
                "SELECT SUM(count) FROM points p " +
                        "JOIN plants pl ON p.plant_id = pl.id " +
                        "LEFT JOIN variety v ON pl.variety_id = v.id WHERE 1=1"
        );
        List<String> args = new ArrayList<>();

        if (name != null && !name.isEmpty()) {
            sql.append(" AND pl.name LIKE ?");
            args.add("%" + name + "%");
        }
        if (type != null && !type.isEmpty()) {
            sql.append(" AND v.type LIKE ?");
            args.add("%" + type + "%");
        }
        if (group != null && !group.isEmpty()) {
            sql.append(" AND v.plant_group = ?");
            args.add(group);
        }
        if (color != null) {
            sql.append(" AND pl.flower_color = ?");
            args.add(String.valueOf(color));
        }
        if (potVolume != null) {
            sql.append(" AND p.pot_volume = ?");
            args.add(String.valueOf(potVolume));
        }

        Cursor cursor = db.rawQuery(sql.toString(), args.toArray(new String[0]));
        int total = 0;
        if (cursor.moveToFirst()) {
            total = cursor.getInt(0);
        }
        cursor.close();
        return total;
    }

    /**
     * Точки, для которых ни разу не задана дата обработки (processing_date IS NULL).
     */
    public List<PlantPoint> getNeverProcessedPoints() {
        List<PlantPoint> points = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String sql =
                "SELECT p.*, pl.id AS plant_id, " +
                        "pl.name, " +
                        "v.type, " +
                        "v.plant_group, " +
                        "pl.flower_color, " +
                        "pl.additional_info, " +
                        "pl.public_key " +
                        "FROM points p JOIN plants pl ON p.plant_id = pl.id " +
                        "LEFT JOIN variety v ON pl.variety_id = v.id " +
                        "WHERE p.processing_date IS NULL";

        Cursor c = db.rawQuery(sql, null);
        while (c.moveToNext()) {
            points.add(mapFromCursor(c));
        }
        c.close();
        return points;
    }

    /**
     * Точки, которые не обрабатывались дольше указанного количества дней.
     * Включает те, у которых processing_date IS NULL.
     *
     * @param days количество дней
     */
    public List<PlantPoint> getNotProcessedMoreThanDays(int days) {
        LocalDate today = LocalDate.now();
        LocalDate thresholdDate = today.minusDays(days);
        long threshold = thresholdDate
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();

        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String query =
                "SELECT p.*, pl.id AS plant_id, " +
                        "pl.name, " +
                        "v.type, " +
                        "v.plant_group, " +
                        "pl.flower_color, " +
                        "pl.additional_info, " +
                        "pl.public_key " +
                        "FROM points p " +
                        "JOIN plants pl ON p.plant_id = pl.id " +
                        "LEFT JOIN variety v ON pl.variety_id = v.id " +
                        "WHERE processing_date IS NULL OR processing_date < ?";

        Cursor cursor = db.rawQuery(query, new String[]{ String.valueOf(threshold) });
        List<PlantPoint> result = new ArrayList<>();
        while (cursor.moveToNext()) {
            result.add(mapFromCursor(cursor));
        }
        cursor.close();
        return result;
    }

    /**
     * Точки, для которых ни разу не задана дата подкормки (feeding_date IS NULL).
     */
    public List<PlantPoint> getNeverFeedingPoints() {
        List<PlantPoint> points = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String sql =
                "SELECT p.*, pl.id AS plant_id, " +
                        "pl.name, " +
                        "v.type, " +
                        "v.plant_group, " +
                        "pl.flower_color, " +
                        "pl.additional_info, " +
                        "pl.public_key " +
                        "FROM points p " +
                        "JOIN plants pl ON p.plant_id = pl.id " +
                        "LEFT JOIN variety v ON pl.variety_id = v.id " +
                        "WHERE p.feeding_date IS NULL";

        Cursor c = db.rawQuery(sql, null);
        while (c.moveToNext()) {
            points.add(mapFromCursor(c));
        }
        c.close();
        return points;
    }

    /**
     * Точки, которые не подкармливались дольше указанного количества дней.
     * Включает те, у которых feeding_date IS NULL.
     *
     * @param days количество дней
     */
    public List<PlantPoint> getNotFeedingMoreThanDays(int days) {
        LocalDate today = LocalDate.now();
        LocalDate thresholdDate = today.minusDays(days);
        long threshold = thresholdDate
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();

        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String query =
                "SELECT p.*, pl.id AS plant_id, " +
                        "pl.name, " +
                        "v.type, " +
                        "v.plant_group, " +
                        "pl.flower_color, " +
                        "pl.additional_info, " +
                        "pl.public_key " +
                        "FROM points p " +
                        "JOIN plants pl ON p.plant_id = pl.id " +
                        "LEFT JOIN variety v ON pl.variety_id = v.id " +
                        "WHERE feeding_date IS NULL OR feeding_date < ?";

        Cursor cursor = db.rawQuery(query, new String[]{ String.valueOf(threshold) });
        List<PlantPoint> result = new ArrayList<>();
        while (cursor.moveToNext()) {
            result.add(mapFromCursor(cursor));
        }
        cursor.close();
        return result;
    }
}