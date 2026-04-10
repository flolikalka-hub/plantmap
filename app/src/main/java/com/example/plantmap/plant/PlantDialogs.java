package com.example.plantmap.plant;

import android.app.AlertDialog;
import android.content.Context;
import android.text.InputType;
import android.view.LayoutInflater;
import com.example.plantmap.R;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ScrollView;

import com.example.plantmap.model.Plant;
import com.example.plantmap.model.PlantPoint;
import com.example.plantmap.util.DateCheckBoxUtil;
import com.example.plantmap.util.ImeActionUtil;
import com.example.plantmap.util.InputValidators;
import com.example.plantmap.util.LayoutUtils;

public class PlantDialogs {
    // ввод данных о растении, когда НОВАЯ ТОЧКА
    public static void showNewPlantDialog(
            Context context,
            PlantPoint point,
            PlantRepository repository,
            Runnable onSaved) {
        PlantUniversalForm form = new PlantUniversalForm(context, repository);

        // count отдельно, в универсальной болванке его нет
        EditText countInput = new EditText(context);
        countInput.setHint("Количество");
        countInput.setInputType(InputType.TYPE_CLASS_NUMBER);

        // Обработка
        CheckBox processedCheckBox = new CheckBox(context);
        processedCheckBox.setText("Уже обрабатывалось");
        EditText dateInput = new EditText(context);
        dateInput.setHint("Дата обработки");

        DateCheckBoxUtil processHelper = new DateCheckBoxUtil(
                processedCheckBox, dateInput,
                date -> point.processingDate = date
        );

        // Подкормка
        CheckBox feedingCheckBox = new CheckBox(context);
        feedingCheckBox.setText("Уже подкармливалось");
        EditText feedingDateInput = new EditText(context);
        feedingDateInput.setHint("Дата подкормки");

        DateCheckBoxUtil feedingHelper = new DateCheckBoxUtil(
                feedingCheckBox, feedingDateInput,
                date -> point.feedingDate = date
        );

        ImeActionUtil.setupImeChain(
                form.nameInput,
                form.typeInput,
                form.groupInput,
                form.potVolumeInput,
                form.flowerColorInput,
                form.additionalInfoInput,
                countInput);

        // сборка
        LayoutUtils.ScrollableLayout scrollableLayout = LayoutUtils.createVerticalScrollView(context);
        scrollableLayout.layout.addView(form.getView());
        scrollableLayout.layout.addView(countInput);

        scrollableLayout.layout.addView(processedCheckBox);
        scrollableLayout.layout.addView(dateInput);

        scrollableLayout.layout.addView(feedingCheckBox);
        scrollableLayout.layout.addView(feedingDateInput);

        // стандартное диалоговое окно, не xml
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("Новое растение")
                .setView(scrollableLayout.scrollView)
                .setPositiveButton("Сохранить", null)
                .setNegativeButton("Отмена", null)
                .create();

        dialog.setOnShowListener(d -> {
            ImeActionUtil.focusAndShowKeyboard(form.nameInput);

            Button saveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            saveButton.setOnClickListener(v -> {

                Plant modifiedPlant = form.buildPlantFromInputs();
                Plant originalPlant = form.getSelectedPlant();

                if (modifiedPlant.name.isEmpty()) {
                    form.nameInput.setError("Название обязательно");
                    form.nameInput.requestFocus();
                    return;
                }

                // валидаторные обработки
                Integer count = InputValidators.validatePositiveCount(countInput);
                if (count == null) return;

                Integer potVolume = InputValidators.validatePositiveOptionalInt(form.potVolumeInput);
                if (form.potVolumeInput.getError() != null) return;
                modifiedPlant.potVolume = potVolume;

                // поиск полного совпадения
                Plant plant;

                if (originalPlant != null &&
                        !repository.isPlantModified(originalPlant, modifiedPlant)) {

                    // выбрали существующее и ничего не изменили
                    plant = originalPlant;

                } else {

                    Plant existing = repository.findPlantByAllFields(modifiedPlant);

                    if (existing != null) {
                        plant = existing;
                    } else {
                        long plantId = repository.addPlant(modifiedPlant);
                        modifiedPlant.id = (int) plantId;
                        plant = modifiedPlant;
                    }
                }


                point.plant = plant;
                point.count = count;

                long newId = repository.addPoint(point);
                point.id = (int) newId;

                // callback для добавления точки и перерисовки
                if (onSaved != null) onSaved.run();

                dialog.dismiss();
            });
        });

        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE |
                            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            );
        }

        dialog.show();
    }

    // работа с СУЩЕСТВУЮЩЕЙ ТОЧКОЙ + изменение растения
    public static void showEditPointDialog(
            Context context,
            PlantPoint point,
            PlantRepository repository,
            Runnable onDeleted,
            Runnable onUpdated) {

        if (point.plant == null) return;

        // Inflate layout
        View view = LayoutInflater.from(context).inflate(R.layout.edit_point_dialog, null);

        EditText countInput = view.findViewById(R.id.countInput);
        CheckBox processedCheckBox = view.findViewById(R.id.processedCheck);
        EditText dateInput = view.findViewById(R.id.dateInput);
        CheckBox feedingCheckBox = view.findViewById(R.id.feedingCheck);
        EditText feedingDateInput = view.findViewById(R.id.feedingDateInput);
        Button changePlantBtn = view.findViewById(R.id.changePlantBtn);

        // Инициализация данных
        DateCheckBoxUtil processHelper = new DateCheckBoxUtil(
                processedCheckBox, dateInput,
                date -> point.processingDate = date
        );
        processHelper.setDate(point.processingDate);

        DateCheckBoxUtil feedingHelper = new DateCheckBoxUtil(
                feedingCheckBox, feedingDateInput,
                date -> point.feedingDate = date
        );
        feedingHelper.setDate(point.feedingDate);

        countInput.setText(String.valueOf(point.count));
        countInput.setSelection(countInput.getText().length());

        // Оборачиваем view в ScrollView для корректного отображения кнопок
        ScrollView scrollView = LayoutUtils.wrapInScrollView(context, view);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(point.plant.name)
                .setView(scrollView)
                .setPositiveButton("Сохранить", null)
                .setNeutralButton("Удалить", (d, which) -> {
                    repository.deletePoint(point.id);
                    if (onDeleted != null) onDeleted.run();
                })
                .setNegativeButton("Отмена", null)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
                            | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            );
        }

        dialog.setOnShowListener(d -> {
            ImeActionUtil.focusAndShowKeyboard(countInput);

            Button saveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            saveButton.setOnClickListener(v -> {
                Integer count = InputValidators.validatePositiveCount(countInput);
                if (count == null) return;

                point.count = count;
                repository.updatePoint(point.id, point);

                if (onUpdated != null) onUpdated.run();
                dialog.dismiss();
            });
        });

        // кнопка смены растения
        changePlantBtn.setOnClickListener(v -> {
            dialog.dismiss();
            showChangePlantDialog(context, point, repository, () -> {
                if (onUpdated != null) onUpdated.run();
            });
        });

        dialog.show();
    }

    public static void showChangePlantDialog(
            Context context,
            PlantPoint point,
            PlantRepository repository,
            Runnable onChanged) {
        PlantUniversalForm form = new PlantUniversalForm(context, repository);
        form.fillFromPlant(point.plant);

        // диалог
        AlertDialog changeDialog = new AlertDialog.Builder(context)
                .setTitle("Сменить растение")
                .setView(form.getView())
                .setPositiveButton("Сохранить", null)
                .setNegativeButton("Отмена", null)
                .create();

        // логика сохранения, но без создания точки
        changeDialog.setOnShowListener(d -> {
            ImeActionUtil.focusAndShowKeyboard(form.nameInput);

            Button saveButton = changeDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            saveButton.setOnClickListener(v -> {

                Plant tempPlant = form.buildPlantFromInputs();
                if (tempPlant.name.isEmpty()) {
                    form.nameInput.setError("Название обязательно");
                    form.nameInput.requestFocus();
                    return;
                }

                // обработка валидатором по условиям
                Integer potVolume = InputValidators.validatePositiveOptionalInt(form.potVolumeInput);
                if (form.potVolumeInput.getError() != null) return;

                tempPlant.potVolume = potVolume;
                Plant selectedPlant = form.getSelectedPlant();
                Plant plant;

                if (selectedPlant != null
                        && !repository.isPlantModified(selectedPlant, tempPlant)) {
                    // выбрали из автокомплита и ничего не меняли в автозаполненных полях
                    plant = selectedPlant;
                } else {
                    // поиск полного совпадения
                    Plant existingPlant = repository.findPlantByAllFields(tempPlant);
                    if (existingPlant == null) {
                        long plantId = repository.addPlant(tempPlant);
                        tempPlant.id = (int) plantId;
                        plant = tempPlant;
                    } else {
                        plant = existingPlant;
                    }
                }

                // смена растения в точке непосредственная
                point.plant = plant;
                repository.updatePoint(point.id, point);

                // callback обновления
                if (onChanged != null) onChanged.run();

                changeDialog.dismiss();
            });
        });

        if (changeDialog.getWindow() != null) {
            changeDialog.getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE |
                            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            );
        }

        changeDialog.show();
    }
}