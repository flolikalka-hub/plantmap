package com.example.plantmap.plant;

import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.example.plantmap.model.Plant;

import java.util.List;

import android.view.inputmethod.EditorInfo;

public class PlantUniversalForm {

    public AutoCompleteTextView nameInput;
    public EditText typeInput;
    public EditText groupInput;
    public EditText potVolumeInput;
    public AutoCompleteTextView flowerColorInput;
    public EditText additionalInfoInput;

    private LinearLayout rootLayout;
    private ScrollView scrollContainer;

    //private View lastFocusedView = null;

    private Plant selectedPlantFromAutocomplete;

    public Plant getSelectedPlant() {
        return selectedPlantFromAutocomplete;
    }

    public PlantUniversalForm(Context context, PlantRepository repository) {

        rootLayout = new LinearLayout(context);
        rootLayout.setOrientation(LinearLayout.VERTICAL);

        scrollContainer = new ScrollView(context);
        scrollContainer.setFillViewport(true);
        scrollContainer.addView(rootLayout);

        // поля
        nameInput = new AutoCompleteTextView(context);
        nameInput.setHint("Название сорта");
        nameInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);

        typeInput = new EditText(context);
        typeInput.setHint("Тип растения");

        groupInput = new EditText(context);
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

        // автозаполнение остальных полей (по сорту)
        nameInput.setOnItemClickListener((parent, view, position, id) -> {
            Plant selectedPlant = (Plant) parent.getItemAtPosition(position);
            fillFromPlant(selectedPlant);
            selectedPlantFromAutocomplete = selectedPlant;
        });

        // перевод фокусов полей
        nameInput.setImeOptions(EditorInfo.IME_ACTION_NEXT | EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        typeInput.setImeOptions(EditorInfo.IME_ACTION_NEXT | EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        groupInput.setImeOptions(EditorInfo.IME_ACTION_NEXT | EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        potVolumeInput.setImeOptions(EditorInfo.IME_ACTION_NEXT | EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        flowerColorInput.setImeOptions(EditorInfo.IME_ACTION_NEXT | EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        additionalInfoInput.setImeOptions(EditorInfo.IME_ACTION_DONE | EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        // последнее поле — закрывает клавиатуру ^

        nameInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                typeInput.requestFocus();
                return true;
            }
            return false;
        });

        typeInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                groupInput.requestFocus();
                return true;
            }
            return false;
        });

        groupInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                potVolumeInput.requestFocus();
                return true;
            }
            return false;
        });

        potVolumeInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                flowerColorInput.requestFocus();
                return true;
            }
            return false;
        });

        flowerColorInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                additionalInfoInput.requestFocus();
                return true;
            }
            return false;
        });

        // дополнительнаяInfo — последняя, закрывает клавиатуру
        additionalInfoInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                v.clearFocus();
                return true;
            }
            return false;
        });

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
    /*
    // сохранение состояния
    public void saveState(Bundle outState) {
        outState.putString("name", nameInput.getText().toString());
        outState.putString("type", typeInput.getText().toString());
        outState.putString("group", groupInput.getText().toString());
        outState.putString("potVolume", potVolumeInput.getText().toString());
        outState.putString("flowerColor", flowerColorInput.getText().toString());
        outState.putString("additionalInfo", additionalInfoInput.getText().toString());

        View focused = rootLayout.findFocus();
        if (focused != null) {
            if (focused == nameInput) outState.putString("focus", "name");
            else if (focused == typeInput) outState.putString("focus", "type");
            else if (focused == groupInput) outState.putString("focus", "group");
            else if (focused == potVolumeInput) outState.putString("focus", "potVolume");
            else if (focused == flowerColorInput) outState.putString("focus", "flowerColor");
            else if (focused == additionalInfoInput) outState.putString("focus", "additionalInfo");
        }
    }

    public void restoreState(Bundle savedInstanceState) {
        if (savedInstanceState == null) return;

        nameInput.setText(savedInstanceState.getString("name", ""));
        typeInput.setText(savedInstanceState.getString("type", ""));
        groupInput.setText(savedInstanceState.getString("group", ""));
        potVolumeInput.setText(savedInstanceState.getString("potVolume", ""));
        flowerColorInput.setText(savedInstanceState.getString("flowerColor", ""));
        additionalInfoInput.setText(savedInstanceState.getString("additionalInfo", ""));

        String focus = savedInstanceState.getString("focus");
        if (focus != null) {
            switch (focus) {
                case "name": nameInput.requestFocus(); break;
                case "type": typeInput.requestFocus(); break;
                case "group": groupInput.requestFocus(); break;
                case "potVolume": potVolumeInput.requestFocus(); break;
                case "flowerColor": flowerColorInput.requestFocus(); break;
                case "additionalInfo": additionalInfoInput.requestFocus(); break;
            }
        }
    }
     */
}
