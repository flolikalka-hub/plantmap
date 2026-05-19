package com.example.plantmap.search;

//import android.util.Log;

import com.example.plantmap.model.Plant;
import com.example.plantmap.model.PlantPoint;
import com.example.plantmap.model.SearchFilter;

import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PlantSearchEngine {

    public Set<PlantPoint> applyFilter(List<PlantPoint> points, SearchFilter filter) {
        Set<PlantPoint> result = new HashSet<>();

        if (isFilterEmpty(filter)) {
            return result;
        }

        //Log.d("SEARCH","PARAMS");

        for (PlantPoint p : points) {
            if (matchesFilter(p, filter)) {
                result.add(p);
            }
        }

        return result;
    }

    private boolean isFilterEmpty(SearchFilter f) {
        return (f.name == null || f.name.isEmpty())
                && (f.type == null || f.type.isEmpty())
                && (f.group == null || f.group.isEmpty())
                && (f.flowerColor == null || f.flowerColor.isEmpty())
                && (f.additionalInfo == null || f.additionalInfo.isEmpty())
                && f.potVolume == null
                && f.count == null
                && f.processingDate == null
                && f.feedingDate == null;
    }

    // Помощник для поиска, обнуляем секунды часы минуты
    private boolean isSameDay(long ts1, long ts2) {
        Calendar c1 = Calendar.getInstance();
        Calendar c2 = Calendar.getInstance();
        c1.setTimeInMillis(ts1);
        c2.setTimeInMillis(ts2);

        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR)
                && c1.get(Calendar.MONTH) == c2.get(Calendar.MONTH)
                && c1.get(Calendar.DAY_OF_MONTH) == c2.get(Calendar.DAY_OF_MONTH);
    }


    private boolean matchesFilter(PlantPoint p, SearchFilter f) {
        if (p.plant == null) return false;
        Plant plant = p.plant;

        // фильтр по точке
        if (f.count != null && !f.count.equals(p.count)) return false;
        if (f.processingDate != null) {
            if (p.processingDate == null) {
                return false;
            }
            if (!isSameDay(f.processingDate, p.processingDate)) {
                return false;
            }
        }
        if (f.feedingDate != null) {
            if (p.feedingDate == null) {
                return false;
            }
            if (!isSameDay(f.feedingDate, p.feedingDate)) {
                return false;
            }
        }


        // фильтры по растению
        if (!matchesText(plant.name, f.name)) return false;
        if (!matchesText(plant.type, f.type)) return false;
        if (!matchesText(plant.group, f.group)) return false;
        if (!matchesText(plant.flowerColor, f.flowerColor)) return false;
        if (!matchesText(plant.additionalInfo, f.additionalInfo)) return false;

        if (f.potVolume != null && plant.potVolume != f.potVolume) return false;

        return true;
    }

    private boolean matchesText(String field, String filter) {
        if (filter == null || filter.isEmpty()) return true;
        if (field == null) return false;
        return field.toLowerCase().contains(filter.toLowerCase());
    }
}
