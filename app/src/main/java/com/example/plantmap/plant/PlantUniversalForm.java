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

import com.example.plantmap.model.Plant;
import com.example.plantmap.util.ImeActionUtil;
import com.example.plantmap.util.LayoutUtils;

import java.util.List;

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
        List<Plant> plants = repository.getAllPlants();
        ArrayAdapter<Plant> plantAdapter =
                new ArrayAdapter<>(
                        context,
                        android.R.layout.simple_dropdown_item_1line,
                        plants
                );

        nameInput.setAdapter(plantAdapter);
        nameInput.setThreshold(1);

        List<String> colorNames = repository.getAllColorNames();
        ArrayAdapter<String> colorAdapter =
                new ArrayAdapter<>(
                        context,
                        android.R.layout.simple_dropdown_item_1line,
                        colorNames
                );

        flowerColorInput.setAdapter(colorAdapter);
        flowerColorInput.setThreshold(1);

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
        flowerColorInput.setText(plant.flowerColor != null ? plant.flowerColor : "");
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

        p.flowerColor = flowerColorInput.getText().toString().trim();
        p.additionalInfo = additionalInfoInput.getText().toString().trim();

        return p;
    }

    // чтобы получать все эти поля
    public AutoCompleteTextView getNameInput() {
        return nameInput;
    }

    public EditText getTypeInput() {
        return typeInput;
    }

    public EditText getGroupInput() {
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
