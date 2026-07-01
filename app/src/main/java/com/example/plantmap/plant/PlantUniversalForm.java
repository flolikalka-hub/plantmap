package com.example.plantmap.plant;

import android.content.Context;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.app.AlertDialog;
import android.widget.ScrollView;
import android.widget.TextView;

import com.example.plantmap.model.FlowerColor;
import com.example.plantmap.model.Plant;
import com.example.plantmap.util.ImeActionUtil;
import com.example.plantmap.util.LayoutUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Универсальная форма для работы с растением.
 * Поддерживает три режима:
 * - MODE_PLANT — редактирование растения со списком объёмов горшков
 * - MODE_POINT  — создание/редактирование точки (один объём)
 * - MODE_SEARCH — поиск растений (один объём, опция "любой" для цвета)
 *
 * Все элементы интерфейса создаются программно и размещаются в вертикальном LinearLayout
 * внутри ScrollView. Форма может быть использована в диалогах.
 */
public class PlantUniversalForm {

    public static final int MODE_PLANT = 0;
    public static final int MODE_POINT = 1;
    public static final int MODE_SEARCH = 2;

    private int currentMode = MODE_PLANT;

    public AutoCompleteTextView nameInput;
    public AutoCompleteTextView typeInput;
    public AutoCompleteTextView groupInput;
    public AutoCompleteTextView potVolumeInput;   // одиночное поле (для MODE_POINT / MODE_SEARCH)
    public TextView flowerColorInput;
    public EditText additionalInfoInput;

    /** Контейнер для динамических полей объёмов (MODE_PLANT). */
    private LinearLayout potVolumesContainer;
    /** Кнопка добавления поля объёма (MODE_PLANT). */
    private Button addVolumeButton;

    private LinearLayout rootLayout;
    private ScrollView scrollContainer;

    /** Растение, выбранное из автокомплита (если есть). */
    private Plant selectedPlantFromAutocomplete;

    private int selectedFlowerColorId = 9; // по умолчанию "неизвестный"

    private Map<String, Integer> colorNameToIdMap = new HashMap<>();
    private Map<Integer, String> idToColorNameMap = new HashMap<>();
    private boolean showAllColorsOption = false;
    private List<String> originalColorNames;

    // --- Конструктор ---

    public PlantUniversalForm(Context context, PlantRepository repository) {
        LayoutUtils.ScrollableLayout scrollableLayout = LayoutUtils.createVerticalScrollView(context);
        rootLayout = scrollableLayout.layout;
        scrollContainer = scrollableLayout.scrollView;

        // Основные поля
        nameInput = new AutoCompleteTextView(context);
        nameInput.setHint("Название сорта");
        nameInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);

        typeInput = new AutoCompleteTextView(context);
        typeInput.setHint("Тип растения");

        groupInput = new AutoCompleteTextView(context);
        groupInput.setHint("Группа растения");

        // Одиночное поле объёма (скрыто по умолчанию)
        potVolumeInput = new AutoCompleteTextView(context);
        potVolumeInput.setHint("Литраж горшка");
        potVolumeInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        potVolumeInput.setVisibility(View.GONE);

        // Контейнер для нескольких объёмов (MODE_PLANT)
        potVolumesContainer = new LinearLayout(context);
        potVolumesContainer.setOrientation(LinearLayout.VERTICAL);

        addVolumeButton = new Button(context);
        addVolumeButton.setText("+ Добавить объём");
        addVolumeButton.setOnClickListener(v -> addVolumeField(null));

        LinearLayout volumesWrapper = new LinearLayout(context);
        volumesWrapper.setOrientation(LinearLayout.VERTICAL);
        volumesWrapper.addView(potVolumesContainer);
        volumesWrapper.addView(addVolumeButton);

        // Цвет цветка (кликабельный TextView)
        flowerColorInput = new TextView(context);
        flowerColorInput.setHint("Цвет цветка");
        flowerColorInput.setClickable(true);
        flowerColorInput.setFocusable(true);
        flowerColorInput.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 18);
        flowerColorInput.setPadding(16, 16, 0, 16);

        additionalInfoInput = new EditText(context);
        additionalInfoInput.setHint("Дополнительная информация");

        // Инициализация адаптеров
        List<FlowerColor> allColors = repository.getAllColors();
        List<String> colorNames = new ArrayList<>();
        for (FlowerColor c : allColors) {
            colorNames.add(c.getName());
            colorNameToIdMap.put(c.getName(), c.getId());
            idToColorNameMap.put(c.getId(), c.getName());
        }
        originalColorNames = new ArrayList<>(colorNames);

        flowerColorInput.setOnClickListener(v -> showColorPickerDialog(context));

        // Адаптер для названий растений (автокомплит)
        List<Plant> plants = repository.getAllPlants();
        ArrayAdapter<Plant> plantAdapter = new ArrayAdapter<>(
                context, android.R.layout.simple_dropdown_item_1line, plants);
        nameInput.setAdapter(plantAdapter);
        nameInput.setThreshold(1);

        // Адаптер типов
        List<String> types = repository.getAllTypes();
        typeInput.setAdapter(new ArrayAdapter<>(
                context, android.R.layout.simple_dropdown_item_1line, types));
        typeInput.setThreshold(1);

        // Адаптер групп
        List<String> groups = repository.getAllGroups();
        groupInput.setAdapter(new ArrayAdapter<>(
                context, android.R.layout.simple_dropdown_item_1line, groups));
        groupInput.setThreshold(1);

        // При выборе растения — автозаполнение остальных полей
        nameInput.setOnItemClickListener((parent, view, position, id) -> {
            Plant selectedPlant = (Plant) parent.getItemAtPosition(position);
            fillFromPlant(selectedPlant);
            selectedPlantFromAutocomplete = selectedPlant;
            setVolumeSuggestions(selectedPlant.availablePotVolumes);
        });

        // При выборе группы — подстановка типа
        groupInput.setOnItemClickListener((parent, view, position, id) -> {
            String selectedGroup = (String) parent.getItemAtPosition(position);
            String type = repository.getTypeByGroup(selectedGroup);
            if (type != null) {
                typeInput.setText(type);
            }
        });

        ImeActionUtil.setupImeChain(
                nameInput, typeInput, groupInput, potVolumeInput, additionalInfoInput);

        // Сборка layout
        rootLayout.addView(nameInput);
        rootLayout.addView(typeInput);
        rootLayout.addView(groupInput);
        rootLayout.addView(potVolumeInput);
        rootLayout.addView(volumesWrapper);
        rootLayout.addView(flowerColorInput);
        rootLayout.addView(additionalInfoInput);
    }

    public View getView() {
        return scrollContainer;
    }

    /**
     * Переключает режим формы: скрывает/показывает нужные элементы управления объёмами.
     */
    public void setMode(int mode) {
        this.currentMode = mode;
        switch (mode) {
            case MODE_PLANT:
                potVolumesContainer.setVisibility(View.VISIBLE);
                addVolumeButton.setVisibility(View.VISIBLE);
                potVolumeInput.setVisibility(View.GONE);
                break;
            case MODE_POINT:
            case MODE_SEARCH:
                potVolumesContainer.setVisibility(View.GONE);
                addVolumeButton.setVisibility(View.GONE);
                potVolumeInput.setVisibility(View.VISIBLE);
                break;
        }
    }

    /**
     * Добавляет строку для ввода объёма горшка с кнопкой удаления.
     */
    private void addVolumeField(Integer value) {
        LinearLayout row = new LinearLayout(potVolumesContainer.getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        EditText volumeEdit = new EditText(potVolumesContainer.getContext());
        volumeEdit.setHint("Объём");
        volumeEdit.setInputType(InputType.TYPE_CLASS_NUMBER);
        if (value != null) volumeEdit.setText(String.valueOf(value));
        LinearLayout.LayoutParams editParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        volumeEdit.setLayoutParams(editParams);

        Button removeBtn = new Button(potVolumesContainer.getContext());
        removeBtn.setText("✕");
        removeBtn.setOnClickListener(v -> potVolumesContainer.removeView(row));

        row.addView(volumeEdit);
        row.addView(removeBtn);
        potVolumesContainer.addView(row);
    }

    /**
     * Управляет опцией "любой" в диалоге выбора цвета (для режима поиска).
     */
    public void setShowAllColorsOption(boolean show) {
        if (show == showAllColorsOption) return;
        showAllColorsOption = show;
        if (!show && "любой".equals(flowerColorInput.getText().toString())) {
            selectedFlowerColorId = 9;
        }
    }

    /**
     * Заполняет поля формы данными из объекта Plant.
     */
    public void fillFromPlant(Plant plant) {
        if (plant == null) return;
        nameInput.setText(plant.name != null ? plant.name : "");
        typeInput.setText(plant.type != null ? plant.type : "");
        groupInput.setText(plant.group != null ? plant.group : "");

        potVolumesContainer.removeAllViews();
        if (plant.availablePotVolumes != null) {
            for (Integer vol : plant.availablePotVolumes) {
                addVolumeField(vol);
            }
        }
        potVolumeInput.setText("");

        int colorId = plant.flowerColorId != null ? plant.flowerColorId : 9;
        flowerColorInput.setText(getColorNameById(colorId));
        additionalInfoInput.setText(plant.additionalInfo != null ? plant.additionalInfo : "");
    }

    /**
     * Создаёт объект Plant на основе введённых данных.
     */
    public Plant buildPlantFromInputs() {
        Plant p = new Plant();
        p.name = nameInput.getText().toString().trim();
        p.type = typeInput.getText().toString().trim();
        p.group = groupInput.getText().toString().trim();
        p.flowerColorId = getColorIdFromInput();
        p.additionalInfo = additionalInfoInput.getText().toString().trim();
        return p;
    }

    /**
     * Собирает список объёмов горшков из динамических полей (для MODE_PLANT).
     */
    public List<Integer> getPotVolumes() {
        List<Integer> volumes = new ArrayList<>();
        for (int i = 0; i < potVolumesContainer.getChildCount(); i++) {
            View row = potVolumesContainer.getChildAt(i);
            if (row instanceof LinearLayout) {
                EditText edit = (EditText) ((LinearLayout) row).getChildAt(0);
                String text = edit.getText().toString().trim();
                if (!text.isEmpty()) {
                    try {
                        volumes.add(Integer.parseInt(text));
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        return volumes;
    }

    /**
     * Устанавливает подсказки для поля объёма (автокомплит).
     */
    public void setVolumeSuggestions(List<Integer> volumes) {
        List<String> items = new ArrayList<>();
        if (volumes != null) {
            for (Integer v : volumes) {
                if (v != null) items.add(String.valueOf(v));
            }
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                potVolumeInput.getContext(),
                android.R.layout.simple_dropdown_item_1line,
                items);
        potVolumeInput.setAdapter(adapter);
        potVolumeInput.setThreshold(1);
    }

    // --- Внутренние методы для цвета ---

    private void showColorPickerDialog(Context context) {
        List<String> items = new ArrayList<>();
        if (showAllColorsOption) {
            items.add("любой");
        }
        items.addAll(originalColorNames);

        new AlertDialog.Builder(context)
                .setTitle("Выберите цвет")
                .setItems(items.toArray(new String[0]), (dialog, which) -> {
                    String selected = items.get(which);
                    flowerColorInput.setText(selected);
                    if ("любой".equals(selected)) {
                        selectedFlowerColorId = 9;
                    } else {
                        Integer colorId = colorNameToIdMap.get(selected);
                        selectedFlowerColorId = (colorId != null) ? colorId : 9;
                    }
                })
                .show();
    }

    private String getColorNameById(int colorId) {
        String name = idToColorNameMap.get(colorId);
        return name != null ? name : "";
    }

    private int getColorIdFromInput() {
        String input = flowerColorInput.getText().toString().trim();
        if (input.isEmpty()) return 9;
        Integer id = colorNameToIdMap.get(input);
        return id != null ? id : 9;
    }

    public int getSelectedFlowerColorId() {
        return getColorIdFromInput();
    }

    // --- Геттеры для доступа к полям ---

    public AutoCompleteTextView getNameInput() {
        return nameInput;
    }

    public AutoCompleteTextView getTypeInput() {
        return typeInput;
    }

    public AutoCompleteTextView getGroupInput() {
        return groupInput;
    }

    public EditText getPotVolumeInput() {
        return potVolumeInput;
    }

    public TextView getFlowerColorInput() {
        return flowerColorInput;
    }

    public EditText getAdditionalInfoInput() {
        return additionalInfoInput;
    }

    public Plant getSelectedPlant() {
        return selectedPlantFromAutocomplete;
    }
}