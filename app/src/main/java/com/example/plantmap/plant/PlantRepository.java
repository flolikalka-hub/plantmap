package com.example.plantmap.plant;

import com.example.plantmap.db.DatabaseHelper;
import com.example.plantmap.db.dataaccess.PlantDataAccess;
import com.example.plantmap.db.dataaccess.PointDataAccess;
import com.example.plantmap.model.FlowerColor;
import com.example.plantmap.model.Plant;
import com.example.plantmap.model.PlantPoint;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlantRepository {
    private final PlantDataAccess plantDa;
    private final PointDataAccess pointDa;

    public PlantRepository(DatabaseHelper dbHelper) {
        plantDa = new PlantDataAccess(dbHelper);
        pointDa = new PointDataAccess(dbHelper);
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
            Integer flowerColorInput,
            String addInput) {
        return plantDa.searchPlants(
                plName,
                plType,
                plGroup,
                potVolume,
                flowerColorInput,
                addInput);
    }

    // для избегания дубликатов (был ли изменен автокомплит)
    public boolean isPlantModified(Plant original, Plant modified) {
        if (original == null || modified == null) return true;

        return !original.name.equals(modified.name) ||
                !safeEquals(original.type, modified.type) ||
                !safeEquals(original.group, modified.group) ||
                !java.util.Objects.equals(original.potVolume, modified.potVolume) ||
                !java.util.Objects.equals(original.flowerColorId, modified.flowerColorId) ||
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

    public int getFilteredPlantCount(String name, String type, String group, Integer color, Integer potVolume) {
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

    public List<String> getAllColorNames() {
        return plantDa.getAllColorNames();
    }

    public List<FlowerColor> getAllColors() {
        return plantDa.getAllColors();
    }

    public Map<Integer, String> getColorIdToNameMap() {
        List<FlowerColor> colors = getAllColors();
        Map<Integer, String> map = new HashMap<>();
        for (FlowerColor c : colors) {
            map.put(c.getId(), c.getName());
        }
        return map;
    }

    public Map<Integer, String> getColorIdToHexMap() {
        List<FlowerColor> colors = getAllColors();
        Map<Integer, String> map = new HashMap<>();
        for (FlowerColor c : colors) {
            map.put(c.getId(), c.getHex());
        }
        return map;
    }
}
