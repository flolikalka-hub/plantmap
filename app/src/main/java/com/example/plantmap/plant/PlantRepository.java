package com.example.plantmap.plant;

import com.example.plantmap.db.ColorDataAccess;
import com.example.plantmap.db.DatabaseHelper;
import com.example.plantmap.db.PlantDataAccess;
import com.example.plantmap.db.PointDataAccess;
import com.example.plantmap.model.Plant;
import com.example.plantmap.model.PlantPoint;

import java.util.List;

public class PlantRepository {
    private final PlantDataAccess plantDa;
    private final PointDataAccess pointDa;
    private final ColorDataAccess colorDa;

    public PlantRepository(DatabaseHelper dbHelper) {
        plantDa = new PlantDataAccess(dbHelper);
        pointDa = new PointDataAccess(dbHelper);
        colorDa = new ColorDataAccess(dbHelper);
    }

    public ColorDataAccess getColorDataAccess() {
        return colorDa;
    }

    public List<PlantPoint> getAllPoints() {
        return pointDa.getAllPoints();
    }

    public long addPoint(PlantPoint point) {
        return pointDa.addPoint(point);
    }

    public void updatePoint(int id, PlantPoint point) {
        pointDa.updatePoint(id, point);
    }

    public void deletePoint(int id) {
        pointDa.deletePoint(id);
    }

    public List<Plant> getAllPlants() {
        return plantDa.getAllPlants();
    }

    public Plant findPlantByAllFields(Plant plant) {
        return plantDa.findPlantByAllFields(plant);
    }

    public long addPlant(Plant plant) {
        return plantDa.addPlant(plant);
    }

    public void updatePlant(Plant plant) {
        plantDa.updatePlant(plant);
    }

    public boolean canDeletePlant(int id) {
        return plantDa.canDeletePlant(id);
    }

    public void deletePlant(int id) {
        plantDa.deletePlant(id);
    }

    public List<Plant> searchPlants(
            String plName,
            String plType,
            String plGroup,
            Integer potVolume,
            String flowerColorInput,
            String addInput) {
        return plantDa.searchPlants(
                plName,
                plType,
                plGroup,
                potVolume,
                flowerColorInput,
                addInput);
    }

    public List<String> getAllColorNames() {
        return colorDa.getAllColorNames();
    }

    // для избегания дубликатов (был ли изменен автокомплит)
    public boolean isPlantModified(Plant original, Plant modified) {
        if (original == null || modified == null) return true;

        return !original.name.equals(modified.name) ||
                !safeEquals(original.type, modified.type) ||
                !safeEquals(original.group, modified.group) ||
                !java.util.Objects.equals(original.potVolume, modified.potVolume) ||
                !safeEquals(original.flowerColor, modified.flowerColor) ||
                !safeEquals(original.additionalInfo, modified.additionalInfo);
    }

    private boolean safeEquals(String a, String b) {
        if (a == null) a = "";
        if (b == null) b = "";
        return a.equals(b);
    }

    public int getTotalPlantCount() {
        return pointDa.getTotalPlantCount();
    }

    public int getFilteredPlantCount(String name, String type, String group, String color, Integer potVolume) {
        return pointDa.getFilteredPlantCount(name, type, group, color, potVolume);
    }

    public List<PlantPoint> getNeverProcessedPoints() {
        return pointDa.getNeverProcessedPoints();
    }

    public List<PlantPoint> getNotProcessedMoreThanDays(int days) {
        return pointDa.getNotProcessedMoreThanDays(days);
    }

    public List<PlantPoint> getNeverFeedingPoints() {
        return pointDa.getNeverFeedingPoints();
    }

    public List<PlantPoint> getNotFeedingMoreThanDays(int days) {
        return pointDa.getNotFeedingMoreThanDays(days);
    }
    public List<String> getAllTypes() {
        return plantDa.getAllTypes();
    }
    public List<String> getAllGroups() {
        return plantDa.getAllGroups();
    }
    public String getTypeByGroup(String group) {
        return plantDa.getTypeByGroup(group);
    }
}
