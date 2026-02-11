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
import com.example.plantmap.db.color.ColorResolver;
import com.example.plantmap.model.Plant;

import java.util.List;

public class PlantAdapter extends RecyclerView.Adapter<PlantAdapter.PlantViewHolder> {

    public interface OnEditClickListener {
        void onEditClick(Plant plant);
    }

    private Context context;
    private List<Plant> plants;
    private OnEditClickListener listener;
    private ColorResolver colorResolver;

    public PlantAdapter(Context context,
                        List<Plant> plants,
                        ColorResolver colorResolver,
                        OnEditClickListener listener) {
        this.context = context;
        this.plants = plants;
        this.listener = listener;
        this.colorResolver = colorResolver;
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
        boolean hasPotVolume = plant.potVolume != null;
        boolean hasFlowerColor = plant.flowerColor != null && !plant.flowerColor.isEmpty();

        if (hasPotVolume && hasFlowerColor) {
            holder.name.setText(plant.name + " (" + plant.flowerColor + ", " + plant.potVolume + "л)");
        } else if (hasFlowerColor) {
            holder.name.setText(plant.name + " (" + plant.flowerColor + ")");
        } else if (hasPotVolume) {
            holder.name.setText(plant.name + " (" + plant.potVolume + "л)");
        } else {
            holder.name.setText(plant.name);
        }
        // тип + группа
        if (plant.group != null && !plant.group.isEmpty()) {
            holder.type.setText(plant.type + " (" + plant.group + ")");
        } else {
            holder.type.setText(plant.type);
        }

        // цвет цветка
        try {
            applyColor(holder.colorView, plant.flowerColor);
        } catch (IllegalArgumentException e) {
            holder.colorView.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        }

        // кнопка редактирования
        holder.editBtn.setOnClickListener(v -> {
            if (listener != null) listener.onEditClick(plant);
        });
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

    // делаем градиентики
    private void applyColor(View view, String flowerColor) {
        if (flowerColor == null || flowerColor.isEmpty()) {
            view.setBackgroundColor(Color.TRANSPARENT);
            return;
        }

        List<Integer> colors = colorResolver.resolveColors(flowerColor);

        if (colors.isEmpty()) {
            view.setBackgroundColor(Color.TRANSPARENT);
            return;
        }

        if (colors.size() == 1) {
            view.setBackgroundColor(colors.get(0));
        } else {
            int[] gradientColors = new int[colors.size()];
            for (int i = 0; i < colors.size(); i++) {
                gradientColors[i] = colors.get(i);
            }

            GradientDrawable drawable = new GradientDrawable(
                    GradientDrawable.Orientation.LEFT_RIGHT,
                    gradientColors
            );
            view.setBackground(drawable);
        }
    }

}