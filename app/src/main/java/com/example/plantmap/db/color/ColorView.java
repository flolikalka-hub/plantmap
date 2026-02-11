package com.example.plantmap.db.color;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.plantmap.R;
import com.example.plantmap.db.DatabaseHelper;
import com.example.plantmap.model.FlowerColor;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.skydoves.colorpickerview.ColorEnvelope;
import com.skydoves.colorpickerview.ColorPickerDialog;
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener;

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

        // обработчик
        addButton.setOnClickListener(v -> {
            showColorDialog(null);
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
                new ColorAdapter.OnColorActionListener() {
                    @Override
                    public void onEditClick(FlowerColor color) {
                        showColorDialog(color);
                    }

                    @Override
                    public void onDeleteClick(FlowerColor color) {

                        if (dbHelper.isColorUsed(color.name, color.root)) {
                            new AlertDialog.Builder(context)
                                    .setTitle("Нельзя удалить цвет")
                                    .setMessage(
                                            "Этот цвет используется в растениях.\n" +
                                                    "Сначала измените цвет у растений."
                                    )
                                    .setPositiveButton("Ок", null)
                                    .show();
                            return;
                        }

                        new AlertDialog.Builder(context)
                                .setTitle("Удалить цвет")
                                .setMessage("Вы уверены, что хотите удалить этот цвет?")
                                .setPositiveButton("Да", (d, w) -> {
                                    dbHelper.deleteColor(color.id);
                                    refreshColorList();
                                })
                                .setNegativeButton("Отмена", null)
                                .show();
                    }

                }
        );
        recyclerView.setAdapter(adapter);
    }

    private void showColorDialog(@Nullable FlowerColor existingColor) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View view = LayoutInflater.from(context)
                .inflate(R.layout.color_dialog, null);

        EditText nameInput = view.findViewById(R.id.colorNameInput);
        EditText rootInput = view.findViewById(R.id.colorRootInput);
        View preview = view.findViewById(R.id.colorPreview);
        Button pickBtn = view.findViewById(R.id.pickColorBtn);

        final int[] selectedColor = { Color.GRAY };
        final boolean[] colorPicked = { false };

        // если редактируем существующий
        if (existingColor != null) {
            nameInput.setText(existingColor.name);
            rootInput.setText(existingColor.root);

            try {
                selectedColor[0] = Color.parseColor(existingColor.hex);
                preview.setBackgroundColor(selectedColor[0]);
                colorPicked[0] = true;
            } catch (Exception ignored) {}
        }

        pickBtn.setOnClickListener(v -> {
            new ColorPickerDialog.Builder(context)
                    .setTitle("Выбор цвета")
                    .setPositiveButton(
                            "OK",
                            new ColorEnvelopeListener() {
                                @Override
                                public void onColorSelected(ColorEnvelope envelope, boolean fromUser) {
                                    selectedColor[0] = envelope.getColor();
                                    preview.setBackgroundColor(selectedColor[0]);
                                    colorPicked[0] = true;
                                }
                            }
                    )
                    .setNegativeButton("Отмена", null)
                    .show();
        });

        builder.setView(view)
                .setTitle(existingColor == null
                        ? "Добавить цвет"
                        : "Редактировать цвет")
                .setPositiveButton("Сохранить", null)
                .setNegativeButton("Отмена", null);

        AlertDialog dialog = builder.create();
        dialog.show();

        // логика
        Button saveBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        saveBtn.setOnClickListener(v -> {
            String name = nameInput.getText().toString().trim();
            String root = rootInput.getText().toString().trim();

            if (name.isEmpty()) {
                nameInput.setError("Введите название цвета");
                return;
            }

            if (!colorPicked[0]) {
                pickBtn.setError("Выберите цвет");
                return;
            }

            if (dbHelper.colorNameExists(
                    name,
                    existingColor == null ? null : existingColor.id
            )) {
                nameInput.setError("Такой цвет уже существует");
                return;
            }

            String hex = String.format("#%06X", 0xFFFFFF & selectedColor[0]);

            if (existingColor == null) {
                dbHelper.insertColor(name, root, hex);
            } else {
                dbHelper.updateColor(existingColor.id, name, root, hex);
            }

            refreshColorList();
            dialog.dismiss();
        });
    }

}