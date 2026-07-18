package com.example.plantmap.plant;

import android.app.AlertDialog;
import android.content.Context;
import android.text.InputType;
import android.view.LayoutInflater;
import com.example.plantmap.R;
import android.view.View;
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
import com.example.plantmap.util.SoftInputUtil;

import java.util.ArrayList;

/**
 * Статические методы для построения диалогов работы с точками и растениями.
 * Все диалоги создаются программно (не используют XML-макеты, кроме edit_point_dialog).
 *
 * Основные сценарии:
 * - showNewPlantDialog — создание новой точки с новым или существующим растением
 * - showEditPointDialog — редактирование существующей точки (количество, даты, смена растения)
 * - showChangePlantDialog — замена растения в точке (без изменения координат)
 */
public class PlantDialogs {

    /**
     * Диалог добавления новой точки.
     * Позволяет выбрать/создать растение, указать количество, даты обработки/подкормки и объём горшка.
     *
     * @param context    контекст
     * @param point      объект PlantPoint, в который будут записаны координаты (уже заданы) и остальные данные
     * @param repository репозиторий для сохранения
     * @param onSaved    колбэк после успешного сохранения (обновляет список точек и карту)
     */
    public static void showNewPlantDialog(
            Context context,
            PlantPoint point,
            PlantRepository repository,
            Runnable onSaved) {

        PlantUniversalForm form = new PlantUniversalForm(context, repository);
        form.setMode(PlantUniversalForm.MODE_POINT);

        // Поле для количества (не входит в универсальную форму)
        EditText countInput = new EditText(context);
        countInput.setHint("Количество");
        countInput.setInputType(InputType.TYPE_CLASS_NUMBER);

        // Дата обработки
        CheckBox processedCheckBox = new CheckBox(context);
        processedCheckBox.setText("Уже обрабатывалось");
        EditText dateInput = new EditText(context);
        dateInput.setHint("Дата обработки");
        final Long[] processingDate = {null};
        DateCheckBoxUtil processHelper = new DateCheckBoxUtil(
                processedCheckBox,
                dateInput,
                date -> processingDate[0] = date
        );

        // Дата подкормки
        CheckBox feedingCheckBox = new CheckBox(context);
        feedingCheckBox.setText("Уже подкармливалось");
        EditText feedingDateInput = new EditText(context);
        feedingDateInput.setHint("Дата подкормки");
        final Long[] feedingDate = {null};
        DateCheckBoxUtil feedingHelper = new DateCheckBoxUtil(
                feedingCheckBox,
                feedingDateInput,
                date -> feedingDate[0] = date
        );

        // Порядок перехода между полями
        ImeActionUtil.setupImeChain(
                form.nameInput,
                form.typeInput,
                form.groupInput,
                form.potVolumeInput,
                form.additionalInfoInput,
                countInput);

        // Компоновка
        LayoutUtils.ScrollableLayout scrollableLayout = LayoutUtils.createVerticalScrollView(context);
        scrollableLayout.layout.addView(form.getView());
        scrollableLayout.layout.addView(countInput);
        scrollableLayout.layout.addView(processedCheckBox);
        scrollableLayout.layout.addView(dateInput);
        scrollableLayout.layout.addView(feedingCheckBox);
        scrollableLayout.layout.addView(feedingDateInput);

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

                // Валидация количества и объёма
                Integer count = InputValidators.validatePositiveCount(countInput);
                if (count == null) return;

                Integer potVolume = InputValidators.validatePositiveOptionalInt(form.potVolumeInput);
                if (potVolume == null && !form.potVolumeInput.getText().toString().trim().isEmpty()) {
                    return; // ошибка уже показана валидатором
                }

                // Определяем конечное растение для точки
                Plant plant;
                if (originalPlant != null) {
                    // Выбрано из автокомплита
                    boolean nameOrTypeChanged = !originalPlant.name.equals(modifiedPlant.name)
                            || !originalPlant.type.equals(modifiedPlant.type);
                    if (nameOrTypeChanged) {
                        // Имя/тип изменились — ищем или создаём новое
                        Plant existing = repository.findPlantByAllFields(modifiedPlant);
                        if (existing != null) {
                            plant = existing;
                        } else {
                            String plantId = repository.addPlant(modifiedPlant);
                            if (plantId == null) return;
                            modifiedPlant.id = plantId;
                            plant = modifiedPlant;
                        }
                    } else {
                        // Только остальные поля — обновляем оригинал
                        if (repository.isPlantModified(originalPlant, modifiedPlant)) {
                            repository.updatePlant(originalPlant, modifiedPlant);
                        }
                        plant = originalPlant;
                    }
                } else {
                    // Ввод вручную
                    Plant existing = repository.findPlantByAllFields(modifiedPlant);
                    if (existing != null) {
                        plant = existing;
                    } else {
                        String plantId = repository.addPlant(modifiedPlant);
                        if (plantId == null) return;
                        modifiedPlant.id = plantId;
                        plant = modifiedPlant;
                    }
                }

                // Добавляем новый литраж в availablePotVolumes, если его ещё нет
                if (potVolume != null) {
                    if (plant.availablePotVolumes == null || !plant.availablePotVolumes.contains(potVolume)) {
                        repository.addPlantVolume(plant.id, potVolume);
                    }
                }

                // Заполняем точку и сохраняем
                point.plant = plant;
                point.count = count;
                point.potVolume = potVolume;
                point.processingDate = processingDate[0];
                point.feedingDate = feedingDate[0];

                String newId = repository.addPoint(point);
                point.id = newId;

                if (onSaved != null) onSaved.run();
                dialog.dismiss();
            });
        });

        SoftInputUtil.setupSoftInput(dialog);
        dialog.show();
    }

    /**
     * Диалог редактирования существующей точки.
     * Позволяет изменить количество, даты обработки/подкормки, удалить точку или сменить растение.
     *
     * @param context    контекст
     * @param point      редактируемая точка (уже привязана к растению)
     * @param repository репозиторий
     * @param onDeleted  колбэк при удалении точки
     * @param onUpdated  колбэк после обновления
     */
    public static void showEditPointDialog(
            Context context,
            PlantPoint point,
            PlantRepository repository,
            Runnable onDeleted,
            Runnable onUpdated) {

        if (point.plant == null) return;

        View view = LayoutInflater.from(context).inflate(R.layout.edit_point_dialog, null);

        EditText countInput = view.findViewById(R.id.countInput);
        CheckBox processedCheckBox = view.findViewById(R.id.processedCheck);
        EditText dateInput = view.findViewById(R.id.dateInput);
        CheckBox feedingCheckBox = view.findViewById(R.id.feedingCheck);
        EditText feedingDateInput = view.findViewById(R.id.feedingDateInput);
        Button changePlantBtn = view.findViewById(R.id.changePlantBtn);

        // Связываем даты с чекбоксами
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

        SoftInputUtil.setupSoftInput(dialog);

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

        // Кнопка смены растения (открывает отдельный диалог)
        changePlantBtn.setOnClickListener(v -> {
            dialog.dismiss();
            showChangePlantDialog(context, point, repository, onUpdated);
        });

        dialog.show();
    }

    /**
     * Диалог замены растения в точке.
     * Предоставляет ту же форму, что и создание точки, но без поля количества и дат.
     * После сохранения обновляет растение и объём в точке.
     */
    public static void showChangePlantDialog(
            Context context,
            PlantPoint point,
            PlantRepository repository,
            Runnable onChanged) {

        PlantUniversalForm form = new PlantUniversalForm(context, repository);
        form.setMode(PlantUniversalForm.MODE_POINT);
        form.fillFromPlant(point.plant);

        if (point.potVolume != null) {
            form.potVolumeInput.setText(String.valueOf(point.potVolume));
        }
        if (point.plant.availablePotVolumes != null) {
            form.setVolumeSuggestions(point.plant.availablePotVolumes);
        } else {
            form.setVolumeSuggestions(new ArrayList<>());
        }

        AlertDialog changeDialog = new AlertDialog.Builder(context)
                .setTitle("Изменить растение")
                .setView(form.getView())
                .setPositiveButton("Сохранить", null)
                .setNegativeButton("Отмена", null)
                .create();

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

                Integer potVolume = InputValidators.validatePositiveOptionalInt(form.potVolumeInput);
                if (potVolume == null && !form.potVolumeInput.getText().toString().trim().isEmpty()) {
                    return;
                }

                Plant selectedFromList = form.getSelectedPlant();
                Plant originalPlant = selectedFromList != null ? selectedFromList : point.plant;
                Plant plant;

                if (originalPlant != null) {
                    boolean nameOrTypeChanged = !originalPlant.name.equals(tempPlant.name)
                            || !originalPlant.type.equals(tempPlant.type);
                    if (nameOrTypeChanged) {
                        Plant existing = repository.findPlantByAllFields(tempPlant);
                        if (existing != null) {
                            plant = existing;
                        } else {
                            String plantId = repository.addPlant(tempPlant);
                            if (plantId == null) return;
                            tempPlant.id = plantId;
                            plant = tempPlant;
                        }
                    } else {
                        if (repository.isPlantModified(originalPlant, tempPlant)) {
                            repository.updatePlant(originalPlant, tempPlant);
                        }
                        plant = originalPlant;
                    }
                } else {
                    Plant existing = repository.findPlantByAllFields(tempPlant);
                    if (existing != null) {
                        plant = existing;
                    } else {
                        String plantId = repository.addPlant(tempPlant);
                        if (plantId == null) return;
                        tempPlant.id = plantId;
                        plant = tempPlant;
                    }
                }

                if (potVolume != null) {
                    if (plant.availablePotVolumes == null || !plant.availablePotVolumes.contains(potVolume)) {
                        repository.addPlantVolume(plant.id, potVolume);
                    }
                }

                point.plant = plant;
                point.potVolume = potVolume;
                repository.updatePoint(point.id, point);

                if (onChanged != null) onChanged.run();
                changeDialog.dismiss();
            });
        });

        SoftInputUtil.setupSoftInput(changeDialog);
        changeDialog.show();
    }
}