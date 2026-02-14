package com.example.plantmap.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.plantmap.model.Plant;
import com.example.plantmap.model.PlantPoint;

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

        //cv.put("processing_date", point.processingDate);
        if (point.processingDate == null) {
            cv.putNull("processing_date");
        } else {
            cv.put("processing_date", point.processingDate);
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

        //cv.put("processing_date", point.processingDate);
        if (point.processingDate == null) {
            cv.putNull("processing_date");
        } else {
            cv.put("processing_date", point.processingDate);
        }

        cv.put("plant_id", point.plant.id);

        db.update("points", cv, "id=?", new String[]{String.valueOf(id)});
    }

    // Удаление точки
    public void deletePoint(int id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("points", "id=?", new String[]{String.valueOf(id)});
    }

    // Получение всех точек с ПОДТЯГИВАНИЕМ РАСТЕНИЯ
    public List<PlantPoint> getAllPoints() {
        List<PlantPoint> points = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String sql =
                "SELECT p.id, p.x, p.y, p.count, p.processing_date, " +
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

            int pvIndex = c.getColumnIndexOrThrow("pot_volume");
            plant.potVolume = c.isNull(pvIndex) ? null : c.getInt(pvIndex);

            plant.flowerColor = c.getString(c.getColumnIndexOrThrow("flower_color"));
            plant.additionalInfo = c.getString(c.getColumnIndexOrThrow("additional_info"));

            PlantPoint point = new PlantPoint(
                    c.getFloat(c.getColumnIndexOrThrow("x")),
                    c.getFloat(c.getColumnIndexOrThrow("y"))
            );
            point.id = c.getInt(c.getColumnIndexOrThrow("id"));
            point.count = c.getInt(c.getColumnIndexOrThrow("count"));

            //point.processingDate = c.getLong(c.getColumnIndexOrThrow("processing_date"));
            int pdIndex = c.getColumnIndexOrThrow("processing_date");
            point.processingDate = c.isNull(pdIndex) ? null : c.getLong(pdIndex);

            point.plant = plant;

            points.add(point);
        }

        c.close();
        return points;
    }
}
