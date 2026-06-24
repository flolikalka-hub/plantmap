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

public class PointDataAccess {
    private final DatabaseHelper dbHelper;

    public PointDataAccess(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }
    // Добавление точки
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

        long id = db.insert("points", null, cv); // insert возвращает id новой записи
        return id; // метод возвращает id
    }

    // Обновление точки
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

        db.update("points", cv, "id=?", new String[]{String.valueOf(id)});
    }

    // Удаление точки
    public void deletePoint(int id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("points", "id=?", new String[]{String.valueOf(id)});
    }

    // убираем дублирование кода
    private PlantPoint mapFromCursor(Cursor c) {
        Plant plant = new Plant();
        plant.id = c.getInt(c.getColumnIndexOrThrow("plant_id"));
        plant.name = c.getString(c.getColumnIndexOrThrow("name"));
        plant.type = c.getString(c.getColumnIndexOrThrow("type"));
        plant.group = c.getString(c.getColumnIndexOrThrow("plant_group"));

        int pvIndex = c.getColumnIndexOrThrow("pot_volume");
        plant.potVolume = c.isNull(pvIndex) ? null : c.getInt(pvIndex);

        int colorIndex = c.getColumnIndexOrThrow("flower_color");
        plant.flowerColorId = c.isNull(colorIndex) ? 9 : c.getInt(colorIndex);

        plant.additionalInfo = c.getString(c.getColumnIndexOrThrow("additional_info"));

        int keyIndex = c.getColumnIndexOrThrow("public_key");
        plant.imagePublicKey = c.isNull(keyIndex) ? null : c.getString(keyIndex);

        PlantPoint point = new PlantPoint(
                c.getFloat(c.getColumnIndexOrThrow("x")),
                c.getFloat(c.getColumnIndexOrThrow("y"))
        );
        point.id = c.getInt(c.getColumnIndexOrThrow("id"));
        point.count = c.getInt(c.getColumnIndexOrThrow("count"));

        int pdIndex = c.getColumnIndexOrThrow("processing_date");
        point.processingDate = c.isNull(pdIndex) ? null : c.getLong(pdIndex);

        int fdIndex = c.getColumnIndexOrThrow("feeding_date");
        point.feedingDate = c.isNull(fdIndex) ? null : c.getLong(fdIndex);

        point.plant = plant;

        return point;
    }

    // Получение всех точек с ПОДТЯГИВАНИЕМ РАСТЕНИЯ
    public List<PlantPoint> getAllPoints() {
        List<PlantPoint> points = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String sql =
                "SELECT p.id, p.x, p.y, p.count, p.processing_date, p.feeding_date, " +
                        "pl.id AS plant_id, " +
                        "pl.name, " +
                        "v.type, " +
                        "v.plant_group, " +
                        "pl.pot_volume, " +
                        "pl.flower_color, " +
                        "pl.additional_info, " +
                        "pl.public_key " +
                        "FROM points p " +
                        "JOIN plants pl ON p.plant_id = pl.id " +
                        "LEFT JOIN variety v ON pl.variety_id = v.id";

        Cursor c = db.rawQuery(sql, null);

        while (c.moveToNext()) {
            points.add(mapFromCursor(c));
        }

        c.close();
        return points;
    }

    // сумма всех растений в наличии
    public int getTotalPlantCount() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT SUM(count) FROM points",
                null
        );

        int total = 0;

        if (cursor.moveToFirst()) {
            total = cursor.getInt(0);
        }

        cursor.close();

        return total;
    }

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
            sql.append(" AND pl.pot_volume = ?");
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

    // никогда не обрабатывались
    public List<PlantPoint> getNeverProcessedPoints() {
        List<PlantPoint> points = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String sql =
                "SELECT p.id, p.x, p.y, p.count, p.processing_date, p.feeding_date, " +
                        "pl.id AS plant_id, " +
                        "pl.name, " +
                        "v.type, " +
                        "v.plant_group, " +
                        "pl.pot_volume, " +
                        "pl.flower_color, " +
                        "pl.additional_info, " +
                        "pl.public_key " +
                        "FROM points p " +
                        "JOIN plants pl ON p.plant_id = pl.id " +
                        "LEFT JOIN variety v ON pl.variety_id = v.id " +
                        "WHERE p.processing_date IS NULL";

        Cursor c = db.rawQuery(sql, null);

        while (c.moveToNext()) {
            points.add(mapFromCursor(c));
        }

        c.close();
        return points;
    }

    public List<PlantPoint> getNotProcessedMoreThanDays(int days) {

        LocalDate today = LocalDate.now();
        LocalDate thresholdDate = today.minusDays(days);

        long threshold = thresholdDate
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();

        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String query =
                "SELECT p.id, p.x, p.y, p.count, p.processing_date, p.feeding_date, " +
                        "pl.id AS plant_id, " +
                        "pl.name, " +
                        "v.type, " +
                        "v.plant_group, " +
                        "pl.pot_volume, " +
                        "pl.flower_color, " +
                        "pl.additional_info, " +
                        "pl.public_key " +
                        "FROM points p " +
                        "JOIN plants pl ON p.plant_id = pl.id " +
                        "LEFT JOIN variety v ON pl.variety_id = v.id " +
                        "WHERE processing_date IS NULL OR processing_date < ?";

        Cursor cursor = db.rawQuery(query,
                new String[]{ String.valueOf(threshold) });

        List<PlantPoint> result = new ArrayList<>();

        while (cursor.moveToNext()) {
            PlantPoint point = mapFromCursor(cursor);
            result.add(point);
        }

        cursor.close();
        return result;
    }

    // никогда не подкармливались
    public List<PlantPoint> getNeverFeedingPoints() {
        List<PlantPoint> points = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String sql =
                "SELECT p.id, p.x, p.y, p.count, p.processing_date, p.feeding_date, " +
                        "pl.id AS plant_id, " +
                        "pl.name, " +
                        "v.type, " +
                        "v.plant_group, " +
                        "pl.pot_volume, " +
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

    public List<PlantPoint> getNotFeedingMoreThanDays(int days) {

        LocalDate today = LocalDate.now();
        LocalDate thresholdDate = today.minusDays(days);

        long threshold = thresholdDate
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();

        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String query =
                "SELECT p.id, p.x, p.y, p.count, p.processing_date, p.feeding_date, " +
                        "pl.id AS plant_id, " +
                        "pl.name, " +
                        "v.type, " +
                        "v.plant_group, " +
                        "pl.pot_volume, " +
                        "pl.flower_color, " +
                        "pl.additional_info, " +
                        "pl.public_key " +
                        "FROM points p " +
                        "JOIN plants pl ON p.plant_id = pl.id " +
                        "LEFT JOIN variety v ON pl.variety_id = v.id " +
                        "WHERE feeding_date IS NULL OR feeding_date < ?";

        Cursor cursor = db.rawQuery(query,
                new String[]{ String.valueOf(threshold) });

        List<PlantPoint> result = new ArrayList<>();

        while (cursor.moveToNext()) {
            PlantPoint point = mapFromCursor(cursor);
            result.add(point);
        }

        cursor.close();
        return result;
    }
}
