package com.example.plantmap.db;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.plantmap.R;
import com.example.plantmap.model.FlowerColor;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ColorView {

    private Context context;
    private DatabaseHelper dbHelper;
    private RecyclerView recyclerView;

    public ColorView(Context context) {
        this.context = context;
        dbHelper = new DatabaseHelper(context);
    }

    public View createColorView() {
        // корневой контейнер
        LinearLayout rootLayout = new LinearLayout(context);
        rootLayout.setOrientation(LinearLayout.VERTICAL);

        // список цветов
        recyclerView = new RecyclerView(context);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));

        LinearLayout.LayoutParams recyclerParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1f
                );
        recyclerView.setLayoutParams(recyclerParams);

        // кнопка добавления
        Button addButton = new Button(context);
        addButton.setText("Добавить новый цвет");
        addButton.setBackground(ContextCompat.getDrawable(context, R.drawable.btn_add_plant));
        addButton.setTextColor(ContextCompat.getColorStateList(context, R.color.btn_add_plant_txt));
        addButton.setPadding(20, 20, 20, 20);

        // обработчик пока заглушка
        addButton.setOnClickListener(v -> {
            // showColorDialog(null);
        });

        rootLayout.addView(recyclerView);
        rootLayout.addView(addButton);

        refreshColorList();

        return rootLayout;
    }

    private void refreshColorList() {
        List<FlowerColor> colors = dbHelper.getAllColors();
        Collections.sort(colors, Comparator.comparing(c -> c.name));

        ColorAdapter adapter = new ColorAdapter(
                context,
                colors,
                color -> {
                    // showColorDialog(color);
                }
        );
        recyclerView.setAdapter(adapter);
    }
}