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
import android.view.inputmethod.InputMethodManager;

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
        nameInput.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        typeInput.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        groupInput.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        potVolumeInput.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        flowerColorInput.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        additionalInfoInput.setImeOptions(EditorInfo.IME_ACTION_DONE); // последнее поле — закрывает клавиатуру

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
                hideKeyboard(v);
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

    private void hideKeyboard(View view) {
        Context context = view.getContext();
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
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
