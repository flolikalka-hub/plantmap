package com.example.plantmap.plant;

import android.app.AlertDialog;
import android.content.Context;
import android.text.InputType;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.example.plantmap.model.Plant;
import com.example.plantmap.model.PlantPoint;
import com.example.plantmap.util.InputValidators;

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
        countInput.setImeOptions(EditorInfo.IME_ACTION_DONE);

        // сборка
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);

        layout.addView(form.getView());
        layout.addView(countInput);

        // стандартное диалоговое окно, не xml
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("Новое растение")
                .setView(layout)
                .setPositiveButton("Сохранить", null)
                .setNegativeButton("Отмена", null)
                .create();

        dialog.setOnShowListener(d -> {
            Button saveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            saveButton.setOnClickListener(v -> {

                Plant modifiedPlant = form.buildPlantFromInputs();
                Plant originalPlant = form.getSelectedPlant();


                if (modifiedPlant.name.isEmpty()) {
                    form.nameInput.setError("Название обязательно");
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

        dialog.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
        );

        dialog.show();
    }

    // работа с СУЩЕСТВУЮЩЕЙ ТОЧКОЙ + изменение растения
    public static void showEditPointDialog(
            Context context,
            PlantPoint point,
            PlantRepository repository,
            Runnable onUpdated,
            Runnable onDeleted) {

        if (point.plant == null) {
            return;
        }

        // поле количества
        EditText countInput = new EditText(context);
        countInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        countInput.setText(String.valueOf(point.count));
        countInput.setSelection(countInput.getText().length());
        countInput.setHint("Количество");

        // контейнер
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(countInput);

        // кнопка смены растения
        Button changePlantBtn = new Button(context);
        changePlantBtn.setText("Сменить растение");

        layout.addView(changePlantBtn);

        // диалог
        AlertDialog editDialog = new AlertDialog.Builder(context)
                .setTitle(point.plant.name)
                .setMessage("Изменить количество или удалить точку")
                .setView(layout)
                .setPositiveButton("Сохранить", null)
                .setNegativeButton("Удалить", (dialog, which) -> {
                    repository.deletePoint(point.id);
                    // callback удаления
                    if (onDeleted != null) onDeleted.run();
                })
                .setNeutralButton("Отмена", null)
                // только создаем, но не показываем ибо мб переход в редактирование растения
                .create();
        editDialog.setOnShowListener(d -> {
            Button saveButton = editDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            saveButton.setOnClickListener(v -> {

                Integer count = InputValidators.validatePositiveCount(countInput);
                if (count == null) return;

                point.count = count;
                repository.updatePoint(point.id, point);

                // callback обновления
                if (onUpdated != null) onUpdated.run();

                editDialog.dismiss();
            });
        });

        changePlantBtn.setOnClickListener(v -> {
            // закрываем текущий диалог редактирования
            editDialog.dismiss();
            // открываем диалог смены растения
            showChangePlantDialog(
                    context,
                    point,
                    repository,
                    () -> {
                        if (onUpdated != null) onUpdated.run();
                    }
                    );
        });

        // показываем диалог
        editDialog.show();
    }

    public static void showChangePlantDialog(
            Context context,
            PlantPoint point,
            PlantRepository repository,
            Runnable onChanged) {
        PlantUniversalForm form = new PlantUniversalForm(context, repository);
        form.fillFromPlant(point.plant);

        // диалог
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("Сменить растение")
                .setView(form.getView())
                .setPositiveButton("Сохранить", null)
                .setNegativeButton("Отмена", null)
                .create();

        // логика сохранения, но без создания точки
        dialog.setOnShowListener(d -> {
            Button saveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            saveButton.setOnClickListener(v -> {

                Plant tempPlant = form.buildPlantFromInputs();
                if (tempPlant.name.isEmpty()) {
                    form.nameInput.setError("Название обязательно");
                    return;
                }

                // обработка валидатором по условиям
                Integer potVolume = InputValidators.validatePositiveOptionalInt(form.potVolumeInput);
                if (form.potVolumeInput.getError() != null) return;

                tempPlant.potVolume = potVolume;

                // поиск полного совпадения
                Plant plant = repository.findPlantByAllFields(tempPlant);
                if (plant == null) {
                    long plantId = repository.addPlant(tempPlant);
                    tempPlant.id = (int) plantId;
                    plant = tempPlant;
                }

                // смена растения в точке непосредственная
                point.plant = plant;
                repository.updatePoint(point.id, point);

                // callback обновления
                if (onChanged != null) onChanged.run();

                dialog.dismiss();
            });
        });

        dialog.show();
    }
}
