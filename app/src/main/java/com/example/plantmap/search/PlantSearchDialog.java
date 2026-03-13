package com.example.plantmap.search;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.text.InputType;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.example.plantmap.model.PlantPoint;
import com.example.plantmap.model.SearchFilter;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class PlantSearchDialog {
    public interface OnSearchListener {
        void onSearchApplied(Set<PlantPoint> result);
        void onSearchCleared();
    }

    public static void showAdvancedSearchDialog(
            Context context,
            List<PlantPoint> allPoints,
            PlantSearchEngine engine,
            OnSearchListener listener) {
        ScrollView scrollView = new ScrollView(context);

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);

        AutoCompleteTextView nameInput = new AutoCompleteTextView(context);
        nameInput.setHint("Название сорта");

        EditText typeInput = new EditText(context);
        typeInput.setHint("Тип растения");

        EditText groupInput = new EditText(context);
        groupInput.setHint("Группа растения");

        EditText potVolumeInput = new EditText(context);
        potVolumeInput.setHint("Литраж горшка");
        potVolumeInput.setInputType(InputType.TYPE_CLASS_NUMBER);

        AutoCompleteTextView flowerColorInput = new AutoCompleteTextView(context);
        flowerColorInput.setHint("Цвет цветка");

        EditText countInput = new EditText(context);
        countInput.setHint("Количество");
        countInput.setInputType(InputType.TYPE_CLASS_NUMBER);

        EditText dateInput = new EditText(context);
        dateInput.setHint("Дата обработки");
        dateInput.setFocusable(false); // чтобы не открывалась клавиатура

        EditText feedingDateInput = new EditText(context);
        feedingDateInput.setHint("Дата подкормки");
        feedingDateInput.setFocusable(false);

        EditText addInput = new EditText(context); addInput.setHint("Дополнительная информация");
        // для лямбды final массив, чтобы значение можно было менять
        final long[] selectedDateMillis = {0}; // 0 = дата не выбрана
        final long[] selectedFeedingDateMillis = {0};
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());

        dateInput.setOnClickListener(v -> {

            Calendar calendar = Calendar.getInstance();

            DatePickerDialog picker = new DatePickerDialog(
                    context,
                    (view, year, month, dayOfMonth) -> {

                        Calendar selected = Calendar.getInstance();
                        selected.set(year, month, dayOfMonth, 0, 0, 0);
                        selected.set(Calendar.MILLISECOND, 0);

                        selectedDateMillis[0] = selected.getTimeInMillis();

                        dateInput.setText(sdf.format(selected.getTime()));
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );

            picker.show();
        });

        feedingDateInput.setOnClickListener(v -> {

            Calendar calendar = Calendar.getInstance();

            DatePickerDialog picker = new DatePickerDialog(
                    context,
                    (view, year, month, dayOfMonth) -> {

                        Calendar selected = Calendar.getInstance();
                        selected.set(year, month, dayOfMonth, 0, 0, 0);
                        selected.set(Calendar.MILLISECOND, 0);

                        selectedFeedingDateMillis[0] = selected.getTimeInMillis();

                        feedingDateInput.setText(sdf.format(selected.getTime()));
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );

            picker.show();
        });

        layout.addView(nameInput);
        layout.addView(typeInput);
        layout.addView(groupInput);
        layout.addView(potVolumeInput);
        layout.addView(flowerColorInput);
        layout.addView(addInput);

        layout.addView(countInput);

        layout.addView(dateInput);
        layout.addView(feedingDateInput);

        scrollView.addView(layout);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("Поиск")
                .setView(scrollView)
                .setPositiveButton("Найти", null)
                .setNegativeButton("Отменить", null)
                .create();

        dialog.setOnShowListener(d -> {
            Button findBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            findBtn.setOnClickListener(v -> {
                SearchFilter filter = new SearchFilter();
                filter.name = nameInput.getText().toString().trim();
                filter.type = typeInput.getText().toString().trim();
                filter.group = groupInput.getText().toString().trim();
                filter.flowerColor = flowerColorInput.getText().toString().trim();
                filter.additionalInfo = addInput.getText().toString().trim();
                filter.potVolume = parseIntOrNull(potVolumeInput.getText().toString().trim());
                filter.count = parseIntOrNull(countInput.getText().toString().trim());

                if (selectedDateMillis[0] != 0) {
                    filter.processingDate = selectedDateMillis[0];
                } else {
                    filter.processingDate = null;
                }

                if (selectedFeedingDateMillis[0] != 0) {
                    filter.feedingDate = selectedFeedingDateMillis[0];
                } else {
                    filter.feedingDate = null;
                }

                Set<PlantPoint> result = engine.applyFilter(allPoints, filter);
                if (listener != null) listener.onSearchApplied(result);
                dialog.dismiss();
            });
        });

        dialog.show();
    }

    private static Integer parseIntOrNull(String s) {
        try {
            return s.isEmpty() ? null : Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
