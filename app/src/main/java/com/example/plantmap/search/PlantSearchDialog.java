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

import com.example.plantmap.model.Plant;
import com.example.plantmap.model.PlantPoint;
import com.example.plantmap.model.SearchFilter;
import com.example.plantmap.plant.PlantRepository;
import com.example.plantmap.plant.PlantUniversalForm;

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
            PlantRepository repository,
            PlantSearchDialog.OnSearchListener listener) {

        ScrollView scrollView = new ScrollView(context);
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);

        // Универсальная форма для выбора/автокомплита растения
        PlantUniversalForm form = new PlantUniversalForm(context, repository);

        // Количество
        EditText countInput = new EditText(context);
        countInput.setHint("Количество");
        countInput.setInputType(InputType.TYPE_CLASS_NUMBER);

        // Дата обработки
        EditText dateInput = new EditText(context);
        dateInput.setHint("Дата обработки");
        dateInput.setFocusable(false); // клавиатура не нужна
        final Long[] processingDateMillis = {null}; // null = дата не выбрана

        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());

        dateInput.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();

            DatePickerDialog picker = new DatePickerDialog(
                    context,
                    (view, year, month, dayOfMonth) -> {
                        Calendar selected = Calendar.getInstance();
                        selected.set(year, month, dayOfMonth, 0, 0, 0);
                        selected.set(Calendar.MILLISECOND, 0);
                        processingDateMillis[0] = selected.getTimeInMillis();
                        dateInput.setText(sdf.format(selected.getTime()));
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            picker.show();
        });

        // Дата подкормки
        EditText feedingDateInput = new EditText(context);
        feedingDateInput.setHint("Дата подкормки");
        feedingDateInput.setFocusable(false);
        final Long[] feedingDateMillis = {null};

        feedingDateInput.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();

            DatePickerDialog picker = new DatePickerDialog(
                    context,
                    (view, year, month, dayOfMonth) -> {
                        Calendar selected = Calendar.getInstance();
                        selected.set(year, month, dayOfMonth, 0, 0, 0);
                        selected.set(Calendar.MILLISECOND, 0);
                        feedingDateMillis[0] = selected.getTimeInMillis();
                        feedingDateInput.setText(sdf.format(selected.getTime()));
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            picker.show();
        });

        // Сборка layout
        layout.addView(form.getView());
        layout.addView(countInput);
        layout.addView(dateInput);
        layout.addView(feedingDateInput);

        scrollView.addView(layout);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("Поиск")
                .setView(scrollView)
                .setPositiveButton("Найти", null)
                .setNegativeButton("Отмена", null)
                .create();

        dialog.setOnShowListener(d -> {
            Button findBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            findBtn.setOnClickListener(v -> {
                SearchFilter filter = new SearchFilter();

                // Значения из универсальной формы
                Plant selectedPlant = form.getSelectedPlant();
                if (selectedPlant != null) {
                    filter.name = selectedPlant.name;
                    filter.type = selectedPlant.type;
                    filter.group = selectedPlant.group;
                    filter.flowerColor = selectedPlant.flowerColor;
                    filter.potVolume = selectedPlant.potVolume;
                }

                // Дополнительные фильтры
                filter.count = parseIntOrNull(countInput.getText().toString().trim());
                filter.processingDate = processingDateMillis[0];
                filter.feedingDate = feedingDateMillis[0];

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