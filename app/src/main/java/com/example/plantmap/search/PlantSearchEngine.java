package com.example.plantmap.search;

import com.example.plantmap.model.Plant;
import com.example.plantmap.model.PlantPoint;
import com.example.plantmap.model.SearchFilter;

import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Движок поиска точек по набору фильтров.
 * Не зависит от UI, только сравнивает поля объектов PlantPoint и Plant
 * с критериями из {@link SearchFilter}.
 */
public class PlantSearchEngine {

    /**
     * Применяет фильтр к списку точек.
     *
     * @param points все доступные точки
     * @param filter критерии поиска
     * @return множество точек, удовлетворяющих фильтру (или пустое, если фильтр не задан)
     */
    public Set<PlantPoint> applyFilter(List<PlantPoint> points, SearchFilter filter) {
        Set<PlantPoint> result = new HashSet<>();

        if (isFilterEmpty(filter)) {
            return result;
        }

        for (PlantPoint p : points) {
            if (matchesFilter(p, filter)) {
                result.add(p);
            }
        }
        return result;
    }

    /**
     * Проверяет, пустой ли фильтр (ни одно поле не заполнено).
     */
    private boolean isFilterEmpty(SearchFilter f) {
        return (f.name == null || f.name.isEmpty())
                && (f.type == null || f.type.isEmpty())
                && (f.group == null || f.group.isEmpty())
                && f.flowerColorId == null
                && (f.additionalInfo == null || f.additionalInfo.isEmpty())
                && f.potVolume == null
                && f.count == null
                && f.processingDate == null
                && f.feedingDate == null;
    }

    /**
     * Сравнивает две метки времени с точностью до дня.
     */
    private boolean isSameDay(long ts1, long ts2) {
        Calendar c1 = Calendar.getInstance();
        Calendar c2 = Calendar.getInstance();
        c1.setTimeInMillis(ts1);
        c2.setTimeInMillis(ts2);

        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR)
                && c1.get(Calendar.MONTH) == c2.get(Calendar.MONTH)
                && c1.get(Calendar.DAY_OF_MONTH) == c2.get(Calendar.DAY_OF_MONTH);
    }

    /**
     * Проверяет, соответствует ли точка всем заданным критериям.
     */
    private boolean matchesFilter(PlantPoint p, SearchFilter f) {
        if (p.plant == null) return false;
        Plant plant = p.plant;

        // Фильтры по точке
        if (f.count != null && !f.count.equals(p.count)) return false;
        if (f.processingDate != null) {
            if (p.processingDate == null) return false;
            if (!isSameDay(f.processingDate, p.processingDate)) return false;
        }
        if (f.feedingDate != null) {
            if (p.feedingDate == null) return false;
            if (!isSameDay(f.feedingDate, p.feedingDate)) return false;
        }

        // Фильтры по растению
        if (!matchesText(plant.name, f.name)) return false;
        if (!matchesText(plant.type, f.type)) return false;
        if (!matchesText(plant.group, f.group)) return false;
        if (f.flowerColorId != null && plant.flowerColorId != f.flowerColorId) return false;
        if (!matchesText(plant.additionalInfo, f.additionalInfo)) return false;
        if (f.potVolume != null && !f.potVolume.equals(p.potVolume)) return false;

        return true;
    }

    /**
     * Сравнение строк с частичным совпадением (без учёта регистра).
     * Если фильтр пустой, возвращает true (пропускает).
     */
    private boolean matchesText(String field, String filter) {
        if (filter == null || filter.isEmpty()) return true;
        if (field == null) return false;
        return field.toLowerCase().contains(filter.toLowerCase());
    }
}