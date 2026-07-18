package com.example.plantmap.plant;

import android.content.Context;

import com.example.plantmap.db.DatabaseHelper;
import com.example.plantmap.db.dataaccess.PlantDataAccess;
import com.example.plantmap.db.dataaccess.PointDataAccess;
import com.example.plantmap.db.yandex_tables.SyncManager;
import com.example.plantmap.model.FlowerColor;
import com.example.plantmap.model.Plant;
import com.example.plantmap.model.PlantPoint;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Центральный репозиторий для работы с растениями и точками.
 * Служит единой точкой доступа к данным, скрывая детали реализации (SQLite).
 * Использует PlantDataAccess и PointDataAccess для прямого обращения к БД.
 *
 * Activity и фрагменты зависят только от этого класса, а не от DatabaseHelper напрямую.
 * Это позволяет в будущем заменить слой хранения (например, на Room или сетевой API)
 * без изменения UI-слоя.
 */
public class PlantRepository {
    private final PlantDataAccess plantDa;
    private final PointDataAccess pointDa;
    private final SyncManager syncManager;

    public PlantRepository(DatabaseHelper dbHelper, Context context) {
        plantDa = new PlantDataAccess(dbHelper);
        pointDa = new PointDataAccess(dbHelper);
        this.syncManager = new SyncManager(context);
    }

    /** Запускает синхронизацию всех таблиц с Яндекс.Диском в фоновом потоке. */
    private void triggerSync() {
        new Thread(() -> syncManager.syncAll()).start();
    }

    // --- Точки ---

    /** Все точки плана с информацией о растениях. */
    public List<PlantPoint> getAllPoints() {
        return pointDa.getAllPoints();
    }

    /** Добавляет новую точку. Возвращает её id. */
    public String addPoint(PlantPoint point) {
        String id = pointDa.addPoint(point);
        triggerSync();
        return id;
    }

    /** Обновляет координаты, количество и даты точки. */
    public void updatePoint(String id, PlantPoint point) {
        pointDa.updatePoint(id, point);
        triggerSync();
    }

    /** Удаляет точку по id. */
    public void deletePoint(String id) {
        pointDa.deletePoint(id);
        triggerSync();
    }

    /** Общее количество растений на плане (сумма count). */
    public int getTotalPlantCount() {
        return pointDa.getTotalPlantCount();
    }

    /** Количество растений, соответствующих фильтрам. */
    public int getFilteredPlantCount(String name, String type, String group, Integer color, Integer potVolume) {
        return pointDa.getFilteredPlantCount(name, type, group, color, potVolume);
    }

    /** Точки, ни разу не обработанные. */
    public List<PlantPoint> getNeverProcessedPoints() {
        return pointDa.getNeverProcessedPoints();
    }

    /** Точки, не обрабатывавшиеся более указанного числа дней. */
    public List<PlantPoint> getNotProcessedMoreThanDays(int days) {
        return pointDa.getNotProcessedMoreThanDays(days);
    }

    /** Точки, ни разу не подкормленные. */
    public List<PlantPoint> getNeverFeedingPoints() {
        return pointDa.getNeverFeedingPoints();
    }

    /** Точки, не подкармливавшиеся более указанного числа дней. */
    public List<PlantPoint> getNotFeedingMoreThanDays(int days) {
        return pointDa.getNotFeedingMoreThanDays(days);
    }

    // --- Растения ---

    /** Все растения с объёмами горшков. */
    public List<Plant> getAllPlants() {
        return plantDa.getAllPlants();
    }

    /** Поиск точного совпадения растения по всем полям. */
    public Plant findPlantByAllFields(Plant plant) {
        return plantDa.findPlantByAllFields(plant);
    }

    /** Добавляет новое растение. Возвращает его id. */
    public String addPlant(Plant plant) {
        String id = plantDa.addPlant(plant);
        triggerSync();
        return id;
    }

    /** Сохраняет изменения существующего растения. */
    public void updatePlant(Plant plant) {
        plantDa.updatePlant(plant);
        triggerSync();
    }

    /**
     * Обновляет растение по образцу: копирует изменяемые поля из modified в original,
     * кроме name и type (которые не редактируются). Используется при редактировании
     * точки, чтобы не создавать дубликат растения.
     */
    public void updatePlant(Plant original, Plant modified) {
        original.group = modified.group;
        original.flowerColorId = modified.flowerColorId;
        original.additionalInfo = modified.additionalInfo;
        // variety_id будет пересчитан внутри updatePlant
        updatePlant(original);
    }

    /** Можно ли удалить растение (не используется ли оно в точках). */
    public boolean canDeletePlant(String id) {
        return plantDa.canDeletePlant(id);
    }

    /** Удаляет растение (каскадно удалятся связанные точки через внешний ключ). */
    public void deletePlant(String id) {
        plantDa.deletePlant(id);
        triggerSync();
    }

    /** Поиск растений по критериям. */
    public List<Plant> searchPlants(
            String plName, String plType, String plGroup,
            Integer potVolume, Integer flowerColorInput, String addInput) {
        return plantDa.searchPlants(plName, plType, plGroup, potVolume, flowerColorInput, addInput);
    }

    /**
     * Проверяет, были ли изменены пользователем характеристики растения
     * (по сравнению с оригиналом). Используется для предотвращения создания дубликатов.
     */
    public boolean isPlantModified(Plant original, Plant modified) {
        if (original == null || modified == null) return true;
        return !original.name.equals(modified.name) ||
                !safeEquals(original.type, modified.type) ||
                !safeEquals(original.group, modified.group) ||
                !java.util.Objects.equals(original.flowerColorId, modified.flowerColorId) ||
                !safeEquals(original.additionalInfo, modified.additionalInfo);
    }

    private boolean safeEquals(String a, String b) {
        if (a == null) a = "";
        if (b == null) b = "";
        return a.equals(b);
    }

    // --- Вспомогательные данные ---

    /** Все уникальные типы. */
    public List<String> getAllTypes() {
        return plantDa.getAllTypes();
    }

    /** Все уникальные группы. */
    public List<String> getAllGroups() {
        return plantDa.getAllGroups();
    }

    /** Тип, соответствующий указанной группе. */
    public String getTypeByGroup(String group) {
        return plantDa.getTypeByGroup(group);
    }

    /** Названия всех цветов. */
    public List<String> getAllColorNames() {
        return plantDa.getAllColorNames();
    }

    /** Полная информация о цветах (id, name, hex). */
    public List<FlowerColor> getAllColors() {
        return plantDa.getAllColors();
    }

    /** Карта id -> название цвета. */
    public Map<Integer, String> getColorIdToNameMap() {
        List<FlowerColor> colors = getAllColors();
        Map<Integer, String> map = new HashMap<>();
        for (FlowerColor c : colors) {
            map.put(c.getId(), c.getName());
        }
        return map;
    }

    /** Карта id -> hex-код цвета. */
    public Map<Integer, String> getColorIdToHexMap() {
        List<FlowerColor> colors = getAllColors();
        Map<Integer, String> map = new HashMap<>();
        for (FlowerColor c : colors) {
            map.put(c.getId(), c.getHex());
        }
        return map;
    }

    // --- Объёмы горшков ---

    /** Заменяет список объёмов горшков для растения. */
    public void replacePlantVolumes(String plantId, List<Integer> volumes) {
        plantDa.replacePlantVolumes(plantId, volumes);
        triggerSync();
    }

    /** Добавляет объём горшка (игнорирует дубликат). */
    public void addPlantVolume(String plantId, int volume) {
        plantDa.addPlantVolume(plantId, volume);
        triggerSync();
    }

    /** Возвращает отсортированный список объёмов для растения. */
    public List<Integer> getPotVolumesForPlant(String plantId) {
        return plantDa.getPotVolumesForPlant(plantId);
    }

    /** Можно ли удалить объём (не используется ли он в точках). */
    public boolean canDeleteVolume(String plantId, int potVolume) {
        return plantDa.canDeleteVolume(plantId, potVolume);
    }
}