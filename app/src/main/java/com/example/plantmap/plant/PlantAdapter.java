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

public class PlantAdapter extends RecyclerView.Adapter<PlantAdapter.PlantViewHolder> {

    public interface OnEditClickListener {
        void onEditClick(Plant plant);
    }

    private Context context;
    private List<Plant> plants;
    private OnEditClickListener listener;

    // Карты для преобразования ID цвета в название и hex-код
    private Map<Integer, String> colorIdToName;
    private Map<Integer, String> colorIdToHex;

    public PlantAdapter(Context context,
                        List<Plant> plants,
                        OnEditClickListener listener){
        this.context = context;
        this.plants = plants;
        this.listener = listener;
    }

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

        // подпись карточки
        // название сорта + цвет цветка и литраж горшка
        holder.name.setText(formatPlantTitle(plant));
        // тип + группа
        if (plant.group != null && !plant.group.isEmpty()) {
            holder.type.setText(plant.type + " (" + plant.group + ")");
        } else {
            holder.type.setText(plant.type);
        }

        // цвет цветка
        applyColor(holder.colorView, plant.flowerColorId);

        // кнопка редактирования
        holder.editBtn.setOnClickListener(v -> {
            if (listener != null) listener.onEditClick(plant);
        });
    }

    private String formatPlantTitle(Plant plant) {
        //Подпись карточки - заголовок
        StringBuilder sb = new StringBuilder(plant.name);
        List<String> extras = new ArrayList<>();

        String colorName = colorIdToName != null ? colorIdToName.get(plant.flowerColorId) : null;
        if (colorName != null && !colorName.isEmpty()) {
            extras.add(colorName);
        }
        if (plant.potVolume != null)
            extras.add(plant.potVolume + "л");
        if (!extras.isEmpty())
            sb.append(" (").append(String.join(", ", extras)).append(")");
        return sb.toString();
    }

    @Override
    public int getItemCount() {
        return plants.size();
    }

    public static class PlantViewHolder extends RecyclerView.ViewHolder {
        TextView name, type;
        ImageButton editBtn;
        View colorView;

        public PlantViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.plantName);
            type = itemView.findViewById(R.id.plantType);
            editBtn = itemView.findViewById(R.id.editBtn);
            colorView = itemView.findViewById(R.id.flowerColorView);
        }
    }

    // красим
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
            int color = Color.parseColor(hex);
            view.setBackgroundColor(color);
        } catch (IllegalArgumentException e) {
            view.setBackgroundColor(Color.TRANSPARENT);
        }
    }

}