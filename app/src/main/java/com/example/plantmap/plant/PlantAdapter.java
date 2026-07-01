package com.example.plantmap.plant;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.plantmap.R;
import com.example.plantmap.model.Plant;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Адаптер для отображения списка растений в RecyclerView.
 * Каждый элемент показывает название, тип, цветок (с цветовой индикацией)
 * и кнопку редактирования.
 *
 * Для отображения цвета используется карта цветов (id -> hex),
 * задаваемая через {@link #setColorMaps(Map, Map)}.
 * Особые случаи:
 * - flowerColorId = 9 — прозрачный (нет цвета)
 * - flowerColorId = 8 — радужный градиент
 */
public class PlantAdapter extends RecyclerView.Adapter<PlantAdapter.PlantViewHolder> {

    /** Слушатель нажатия на кнопку редактирования. */
    public interface OnEditClickListener {
        void onEditClick(Plant plant);
    }

    private Context context;
    private List<Plant> plants;
    private OnEditClickListener listener;

    /** Карта id цвета -> название (для отображения в заголовке). */
    private Map<Integer, String> colorIdToName;
    /** Карта id цвета -> hex-код (для окрашивания индикатора). */
    private Map<Integer, String> colorIdToHex;

    public PlantAdapter(Context context, List<Plant> plants, OnEditClickListener listener) {
        this.context = context;
        this.plants = plants;
        this.listener = listener;
    }

    /**
     * Устанавливает карты цветов. Должен вызываться перед отображением списка.
     */
    public void setColorMaps(Map<Integer, String> idToName, Map<Integer, String> idToHex) {
        this.colorIdToName = idToName;
        this.colorIdToHex = idToHex;
    }

    @NonNull
    @Override
    public PlantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.plant_item, parent, false);
        return new PlantViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlantViewHolder holder, int position) {
        Plant plant = plants.get(position);

        // Заголовок карточки: название + цвет + литраж
        holder.name.setText(formatPlantTitle(plant));
        // Подзаголовок: тип (группа)
        if (plant.group != null && !plant.group.isEmpty()) {
            holder.type.setText(plant.type + " (" + plant.group + ")");
        } else {
            holder.type.setText(plant.type);
        }

        // Цветовой индикатор
        applyColor(holder.colorView, plant.flowerColorId);

        holder.editBtn.setOnClickListener(v -> {
            if (listener != null) listener.onEditClick(plant);
        });
    }

    /**
     * Формирует заголовок карточки: "Название (Цвет, 5л, 10л)".
     */
    private String formatPlantTitle(Plant plant) {
        StringBuilder sb = new StringBuilder(plant.name);
        List<String> extras = new ArrayList<>();

        String colorName = colorIdToName != null ? colorIdToName.get(plant.flowerColorId) : null;
        if (colorName != null && !colorName.isEmpty()) {
            extras.add(colorName);
        }
        if (plant.availablePotVolumes != null && !plant.availablePotVolumes.isEmpty()) {
            List<String> volStrings = new ArrayList<>();
            for (Integer vol : plant.availablePotVolumes) {
                volStrings.add(vol + "л");
            }
            extras.add(String.join(", ", volStrings));
        }
        if (!extras.isEmpty()) {
            sb.append(" (").append(String.join(", ", extras)).append(")");
        }
        return sb.toString();
    }

    @Override
    public int getItemCount() {
        return plants.size();
    }

    /**
     * ViewHolder для элемента списка растений.
     */
    public static class PlantViewHolder extends RecyclerView.ViewHolder {
        TextView name, type;
        ImageButton editBtn;
        View colorView; // индикатор цвета цветка

        public PlantViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.plantName);
            type = itemView.findViewById(R.id.plantType);
            editBtn = itemView.findViewById(R.id.editBtn);
            colorView = itemView.findViewById(R.id.flowerColorView);
        }
    }

    /**
     * Устанавливает цвет фона для индикатора цветка.
     * id = 9 — прозрачный, id = 8 — радужный градиент, остальные — сплошной цвет из hex.
     */
    private void applyColor(View view, int flowerColorId) {
        if (flowerColorId == 9) {
            view.setBackgroundColor(Color.TRANSPARENT);
            return;
        }
        if (flowerColorId == 8) {
            int[] rainbowColors = {
                    Color.RED, Color.rgb(255, 165, 0), Color.YELLOW,
                    Color.GREEN, Color.CYAN, Color.BLUE, Color.MAGENTA
            };
            GradientDrawable gd = new GradientDrawable(
                    GradientDrawable.Orientation.LEFT_RIGHT, rainbowColors);
            view.setBackground(gd);
            return;
        }

        String hex = colorIdToHex != null ? colorIdToHex.get(flowerColorId) : null;
        if (hex == null || hex.isEmpty()) {
            view.setBackgroundColor(Color.TRANSPARENT);
            return;
        }
        try {
            view.setBackgroundColor(Color.parseColor(hex));
        } catch (IllegalArgumentException e) {
            view.setBackgroundColor(Color.TRANSPARENT);
        }
    }
}