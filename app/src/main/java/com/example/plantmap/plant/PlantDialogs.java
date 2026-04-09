package com.example.plantmap.plant;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.text.InputType;
import android.view.LayoutInflater;
import com.example.plantmap.R;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.example.plantmap.model.Plant;
import com.example.plantmap.model.PlantPoint;
import com.example.plantmap.util.InputValidators;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class PlantDialogs {
    public static void showNewPlantDialog(
            Context context,
            PlantPoint point,
            PlantRepository repository,
            Runnable onSaved) {
        PlantUniversalForm form = new PlantUniversalForm(context, repository);
        EditText countInput = new EditText(context);
        countInput.setHint("Количество");
        countInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        countInput.setImeOptions(EditorInfo.IME_ACTION_DONE);
        EditText dateInput = new EditText(context);
        dateInput.setHint("Дата обработки");
        dateInput.setFocusable(false);
        CheckBox processedCheckBox = new CheckBox(context);
        processedCheckBox.setText("Уже обрабатывалось");

        form.additionalInfoInput.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        form.additionalInfoInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                countInput.requestFocus();
                return true;
            }
            return false;
        });

        countInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                hideKeyboardAndClearFocus(v);
                return true;
            }
            return false;
        });

        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());

        point.processingDate = null;
        dateInput.setEnabled(false);
        dateInput.setText("Не обрабатывалось");
        processedCheckBox.setChecked(false);

        processedCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                Calendar today = Calendar.getInstance();
                today.set(Calendar.HOUR_OF_DAY, 0);
                today.set(Calendar.MINUTE, 0);
                today.set(Calendar.SECOND, 0);
                today.set(Calendar.MILLISECOND, 0);

                point.processingDate = today.getTimeInMillis();
                dateInput.setEnabled(true);
                dateInput.setText(sdf.format(today.getTime()));
            } else {
                point.processingDate = null;
                dateInput.setEnabled(false);
                dateInput.setText("Не обрабатывалось");
            }
        });

        dateInput.setOnClickListener(v -> {

            if (point.processingDate == null) return;

            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(point.processingDate);

            DatePickerDialog picker = new DatePickerDialog(
                    context,
                    (view, year, month, dayOfMonth) -> {

                        Calendar selected = Calendar.getInstance();
                        selected.set(year, month, dayOfMonth, 0, 0, 0);
                        selected.set(Calendar.MILLISECOND, 0);

                        point.processingDate = selected.getTimeInMillis();
                        dateInput.setText(sdf.format(selected.getTime()));
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );

            picker.show();
        });

        EditText feedingDateInput = new EditText(context);
        feedingDateInput.setHint("Дата подкормки");
        feedingDateInput.setFocusable(false);

        CheckBox feedingCheckBox = new CheckBox(context);
        feedingCheckBox.setText("Уже подкармливалось");

        point.feedingDate = null;
        feedingDateInput.setEnabled(false);
        feedingDateInput.setText("Не подкармливалось");
        feedingCheckBox.setChecked(false);

        feedingCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {

                Calendar today = Calendar.getInstance();
                today.set(Calendar.HOUR_OF_DAY, 0);
                today.set(Calendar.MINUTE, 0);
                today.set(Calendar.SECOND, 0);
                today.set(Calendar.MILLISECOND, 0);

                point.feedingDate = today.getTimeInMillis();
                feedingDateInput.setEnabled(true);
                feedingDateInput.setText(sdf.format(today.getTime()));

            } else {

                point.feedingDate = null;
                feedingDateInput.setEnabled(false);
                feedingDateInput.setText("Не подкармливалось");

            }
        });

        feedingDateInput.setOnClickListener(v -> {

            if (point.feedingDate == null) return;

            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(point.feedingDate);

            DatePickerDialog picker = new DatePickerDialog(
                    context,
                    (view, year, month, dayOfMonth) -> {

                        Calendar selected = Calendar.getInstance();
                        selected.set(year, month, dayOfMonth, 0, 0, 0);
                        selected.set(Calendar.MILLISECOND, 0);

                        point.feedingDate = selected.getTimeInMillis();
                        feedingDateInput.setText(sdf.format(selected.getTime()));
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );

            picker.show();
        });

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);

        layout.addView(form.getView());
        layout.addView(countInput);

        layout.addView(processedCheckBox);
        layout.addView(dateInput);

        layout.addView(feedingCheckBox);
        layout.addView(feedingDateInput);

        ScrollView scrollView = new ScrollView(context);
        scrollView.setFillViewport(true);
        scrollView.addView(layout);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("Новое растение")
                .setView(scrollView)
                .setPositiveButton("Сохранить", null)
                .setNegativeButton("Отмена", null)
                .create();

        dialog.setOnShowListener(d -> {
            focusAndShowKeyboard(form.nameInput);

            Button saveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            saveButton.setOnClickListener(v -> {

                Plant modifiedPlant = form.buildPlantFromInputs();
                Plant originalPlant = form.getSelectedPlant();

                if (modifiedPlant.name.isEmpty()) {
                    form.nameInput.setError("Название обязательно");
                    return;
                }

                Integer count = InputValidators.validatePositiveCount(countInput);
                if (count == null) return;

                Integer potVolume = InputValidators.validatePositiveOptionalInt(form.potVolumeInput);
                if (form.potVolumeInput.getError() != null) return;
                modifiedPlant.potVolume = potVolume;

                Plant plant;

                if (originalPlant != null &&
                        !repository.isPlantModified(originalPlant, modifiedPlant)) {

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

    private static void hideKeyboardAndClearFocus(android.view.View view) {
        Context context = view.getContext();
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
        view.clearFocus();
    }

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

        countInput.setText(String.valueOf(point.count));
        countInput.setSelection(countInput.getText().length());

        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());

        if (point.processingDate == null) {
            processedCheckBox.setChecked(false);
            dateInput.setEnabled(false);
            dateInput.setText("Не обрабатывалось");
        } else {
            processedCheckBox.setChecked(true);
            dateInput.setEnabled(true);
            dateInput.setText(sdf.format(new Date(point.processingDate)));
        }

        processedCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                Calendar today = Calendar.getInstance();
                today.set(Calendar.HOUR_OF_DAY, 0);
                today.set(Calendar.MINUTE, 0);
                today.set(Calendar.SECOND, 0);
                today.set(Calendar.MILLISECOND, 0);

                point.processingDate = today.getTimeInMillis();
                dateInput.setEnabled(true);
                dateInput.setText(sdf.format(today.getTime()));
            } else {
                point.processingDate = null;
                dateInput.setEnabled(false);
                dateInput.setText("Не обрабатывалось");
            }
        });

        dateInput.setOnClickListener(v -> {
            if (point.processingDate == null) return;

            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(point.processingDate);

            new DatePickerDialog(context,
                    (view1, year, month, dayOfMonth) -> {
                        Calendar selected = Calendar.getInstance();
                        selected.set(year, month, dayOfMonth, 0, 0, 0);
                        selected.set(Calendar.MILLISECOND, 0);
                        point.processingDate = selected.getTimeInMillis();
                        dateInput.setText(sdf.format(selected.getTime()));
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            ).show();
        });

        if (point.feedingDate == null) {
            feedingCheckBox.setChecked(false);
            feedingDateInput.setEnabled(false);
            feedingDateInput.setText("Не подкармливалось");
        } else {
            feedingCheckBox.setChecked(true);
            feedingDateInput.setEnabled(true);
            feedingDateInput.setText(sdf.format(new Date(point.feedingDate)));
        }

        feedingCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                Calendar today = Calendar.getInstance();
                today.set(Calendar.HOUR_OF_DAY, 0);
                today.set(Calendar.MINUTE, 0);
                today.set(Calendar.SECOND, 0);
                today.set(Calendar.MILLISECOND, 0);

                point.feedingDate = today.getTimeInMillis();
                feedingDateInput.setEnabled(true);
                feedingDateInput.setText(sdf.format(today.getTime()));
            } else {
                point.feedingDate = null;
                feedingDateInput.setEnabled(false);
                feedingDateInput.setText("Не подкармливалось");
            }
        });

        feedingDateInput.setOnClickListener(v -> {
            if (point.feedingDate == null) return;

            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(point.feedingDate);

            new DatePickerDialog(context,
                    (view12, year, month, dayOfMonth) -> {
                        Calendar selected = Calendar.getInstance();
                        selected.set(year, month, dayOfMonth, 0, 0, 0);
                        selected.set(Calendar.MILLISECOND, 0);
                        point.feedingDate = selected.getTimeInMillis();
                        feedingDateInput.setText(sdf.format(selected.getTime()));
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            ).show();
        });

        ScrollView scrollView = new ScrollView(context);
        scrollView.setFillViewport(true);
        scrollView.addView(view);

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

        dialog.setOnShowListener(d -> {
            focusAndShowKeyboard(countInput);

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

        AlertDialog changeDialog = new AlertDialog.Builder(context)
                .setTitle("Сменить растение")
                .setView(form.getView())
                .setPositiveButton("Сохранить", null)
                .setNegativeButton("Отмена", null)
                .create();
        changeDialog.setOnShowListener(d -> {
            focusAndShowKeyboard(form.nameInput);

            Button saveButton = changeDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            saveButton.setOnClickListener(v -> {

                Plant tempPlant = form.buildPlantFromInputs();
                if (tempPlant.name.isEmpty()) {
                    form.nameInput.setError("Название обязательно");
                    return;
                }

                Integer potVolume = InputValidators.validatePositiveOptionalInt(form.potVolumeInput);
                if (form.potVolumeInput.getError() != null) return;

                tempPlant.potVolume = potVolume;
                Plant selectedPlant = form.getSelectedPlant();
                Plant plant;

                if (selectedPlant != null
                        && !repository.isPlantModified(selectedPlant, tempPlant)) {
                    plant = selectedPlant;
                } else {
                    Plant existingPlant = repository.findPlantByAllFields(tempPlant);
                    if (existingPlant == null) {
                        long plantId = repository.addPlant(tempPlant);
                        tempPlant.id = (int) plantId;
                        plant = tempPlant;
                    } else {
                        plant = existingPlant;
                    }
                }

                point.plant = plant;
                repository.updatePoint(point.id, point);

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

    private static void focusAndShowKeyboard(android.view.View view) {
        view.requestFocus();
        view.requestFocusFromTouch();
        view.post(() -> {
            Context context = view.getContext();
            InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
            }
        });
    }
}