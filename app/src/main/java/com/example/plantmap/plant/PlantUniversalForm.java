package com.example.plantmap.plant;

import android.content.Context;
import android.text.InputType;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.view.inputmethod.InputMethodManager;

import com.example.plantmap.model.FlowerColor;
import com.example.plantmap.model.Plant;
import com.example.plantmap.util.ImeActionUtil;
import com.example.plantmap.util.LayoutUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlantUniversalForm {

    public AutoCompleteTextView nameInput;
    public AutoCompleteTextView typeInput;
    public AutoCompleteTextView groupInput;
    public EditText potVolumeInput;
    public AutoCompleteTextView flowerColorInput;
    public EditText additionalInfoInput;

    private LinearLayout rootLayout;
    private ScrollView scrollContainer;
    private Plant selectedPlantFromAutocomplete;
    private int selectedFlowerColorId = 9; // по умолчанию «неизвестный»
    // Карты для конвертации название ↔ ID
    private Map<String, Integer> colorNameToIdMap = new HashMap<>();
    private Map<Integer, String> idToColorNameMap = new HashMap<>();
    public Plant getSelectedPlant() {

        return selectedPlantFromAutocomplete;
    }

    public PlantUniversalForm(Context context, PlantRepository repository) {

        LayoutUtils.ScrollableLayout scrollableLayout = LayoutUtils.createVerticalScrollView(context);
        rootLayout = scrollableLayout.layout;
        scrollContainer = scrollableLayout.scrollView;

        // поля
        nameInput = new AutoCompleteTextView(context);
        nameInput.setHint("Название сорта");
        nameInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);

        typeInput = new AutoCompleteTextView(context);
        typeInput.setHint("Тип растения");

        groupInput = new AutoCompleteTextView(context);
        groupInput.setHint("Группа растения");

        potVolumeInput = new EditText(context);
        potVolumeInput.setHint("Литраж горшка");
        potVolumeInput.setInputType(InputType.TYPE_CLASS_NUMBER);

        flowerColorInput = new AutoCompleteTextView(context);
        flowerColorInput.setHint("Цвет цветка");

        additionalInfoInput = new EditText(context);
        additionalInfoInput.setHint("Дополнительная информация");

        // адаптеры
        // Получаем все цвета из репозитория
        List<FlowerColor> allColors = repository.getAllColors();
        List<String> colorNames = new ArrayList<>();
        for (FlowerColor c : allColors) {
            colorNames.add(c.getName());
            colorNameToIdMap.put(c.getName(), c.getId());
            idToColorNameMap.put(c.getId(), c.getName());
        }

        ArrayAdapter<String> colorAdapter =
                new ArrayAdapter<>(
                        context,
                        android.R.layout.simple_dropdown_item_1line,
                        colorNames
                );
        flowerColorInput.setAdapter(colorAdapter);
        flowerColorInput.setThreshold(1);

        // При выборе цвета из выпадающего списка запоминаем его ID
        flowerColorInput.setOnItemClickListener((parent, view, position, id) -> {
            String selectedName = (String) parent.getItemAtPosition(position);
            Integer colorId = colorNameToIdMap.get(selectedName);
            selectedFlowerColorId = (colorId != null) ? colorId : 9;
        });

        List<Plant> plants = repository.getAllPlants();
        ArrayAdapter<Plant> plantAdapter =
                new ArrayAdapter<>(
                        context,
                        android.R.layout.simple_dropdown_item_1line,
                        plants
                );

        nameInput.setAdapter(plantAdapter);
        nameInput.setThreshold(1);

        List<String> types = repository.getAllTypes();
        ArrayAdapter<String> typeAdapter =
                new ArrayAdapter<>(
                        context,
                        android.R.layout.simple_dropdown_item_1line,
                        types
                );

        typeInput.setAdapter(typeAdapter);
        typeInput.setThreshold(1);


        List<String> groups = repository.getAllGroups();
        ArrayAdapter<String> groupAdapter =
                new ArrayAdapter<>(
                        context,
                        android.R.layout.simple_dropdown_item_1line,
                        groups
                );

        groupInput.setAdapter(groupAdapter);
        groupInput.setThreshold(1);

        // автозаполнение остальных полей (по сорту)
        nameInput.setOnItemClickListener((parent, view, position, id) -> {
            Plant selectedPlant = (Plant) parent.getItemAtPosition(position);
            fillFromPlant(selectedPlant);
            selectedPlantFromAutocomplete = selectedPlant;
        });
        groupInput.setOnItemClickListener((parent, view, position, id) -> {
            String selectedGroup = (String) parent.getItemAtPosition(position);

            String type = repository.getTypeByGroup(selectedGroup);
            if (type != null) {
                typeInput.setText(type);
            }
        });

        // перевод фокусов полей
        ImeActionUtil.setupImeChain(
                nameInput,
                typeInput,
                groupInput,
                potVolumeInput,
                flowerColorInput,
                additionalInfoInput);

        // сборка
        rootLayout.addView(nameInput);
        rootLayout.addView(typeInput);
        rootLayout.addView(groupInput);
        rootLayout.addView(potVolumeInput);
        rootLayout.addView(flowerColorInput);
        rootLayout.addView(additionalInfoInput);
    }

    public View getView() {
        return scrollContainer;
    }

    public void fillFromPlant(Plant plant) {
        if (plant == null) return;

        nameInput.setText(plant.name != null ? plant.name : "");
        typeInput.setText(plant.type != null ? plant.type : "");
        groupInput.setText(plant.group != null ? plant.group : "");
        potVolumeInput.setText(plant.potVolume != null && plant.potVolume > 0
                ? String.valueOf(plant.potVolume)
                : "");
        flowerColorInput.setText(getColorNameById(plant.flowerColorId));
        additionalInfoInput.setText(plant.additionalInfo != null ? plant.additionalInfo : "");
    }

    public Plant buildPlantFromInputs() {
        Plant p = new Plant();

        p.name = nameInput.getText().toString().trim();
        p.type = typeInput.getText().toString().trim();
        p.group = groupInput.getText().toString().trim();

        String potVolumeStr = potVolumeInput.getText().toString().trim();
        if (potVolumeStr.isEmpty()) {
            p.potVolume = null;
        } else {
            try {
                p.potVolume = Integer.parseInt(potVolumeStr);
            } catch (NumberFormatException e) {
                p.potVolume = null;
            }
        }

        p.flowerColorId = getColorIdFromInput();
        p.additionalInfo = additionalInfoInput.getText().toString().trim();

        return p;
    }

    // Вспомогательные методы для работы с цветом
    private String getColorNameById(int colorId) {
        String name = idToColorNameMap.get(colorId);
        return name != null ? name : "";
    }

    private int getColorIdFromInput() {
        String input = flowerColorInput.getText().toString().trim();
        if (input.isEmpty()) {
            return 9;
        }
        Integer id = colorNameToIdMap.get(input);
        return id != null ? id : 9;
    }

    // Метод для получения текущего выбранного ID цвета (можно использовать извне)
    public int getSelectedFlowerColorId() {
        return getColorIdFromInput();
    }

    // чтобы получать все эти поля
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

    public AutoCompleteTextView getFlowerColorInput() {
        return flowerColorInput;
    }

    public EditText getAdditionalInfoInput() {
        return additionalInfoInput;
    }
}
