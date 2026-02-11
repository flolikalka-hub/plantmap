package com.example.plantmap.plant;

import android.content.Context;

import com.example.plantmap.db.DatabaseHelper;
import com.example.plantmap.model.Plant;
import com.example.plantmap.model.PlantPoint;

import java.util.List;

public class PlantRepository {
    private final DatabaseHelper dbHelper;

    public PlantRepository(Context context) {
        dbHelper = new DatabaseHelper(context);
    }

    public List<PlantPoint> getAllPoints() {
        return dbHelper.getAllPoints();
    }

    public long addPoint(PlantPoint point) {
        return dbHelper.addPoint(point);
    }

    public void updatePoint(int id, PlantPoint point) {
        dbHelper.updatePoint(id, point);
    }

    public void deletePoint(int id) {
        dbHelper.deletePoint(id);
    }

    public List<Plant> getAllPlants() {
        return dbHelper.getAllPlants();
    }

    public Plant findPlantByAllFields(Plant plant) {
        return dbHelper.findPlantByAllFields(plant);
    }

    public long addPlant(Plant plant) {
        return dbHelper.addPlant(plant);
    }

    public List<String> getAllColorNames() {
        return dbHelper.getAllColorNames();
    }

    // для избегания дубликатов (был ли изменен автокомплит)
    public boolean isPlantModified(Plant original, Plant modified) {
        if (original == null || modified == null) return true;

        return !original.name.equals(modified.name) ||
                !safeEquals(original.type, modified.type) ||
                !safeEquals(original.group, modified.group) ||
                original.potVolume != modified.potVolume ||
                !safeEquals(original.flowerColor, modified.flowerColor) ||
                !safeEquals(original.additionalInfo, modified.additionalInfo);
    }

    private boolean safeEquals(String a, String b) {
        if (a == null) a = "";
        if (b == null) b = "";
        return a.equals(b);
    }

}
