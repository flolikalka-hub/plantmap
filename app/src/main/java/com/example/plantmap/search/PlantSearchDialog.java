package com.example.plantmap.search;

import android.app.AlertDialog;
import android.content.Context;
import android.text.InputType;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.example.plantmap.model.PlantPoint;

import java.util.List;
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
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);

        AutoCompleteTextView nameInput = new AutoCompleteTextView(context);
        nameInput.setHint("Название сорта");
        EditText typeInput = new EditText(context); typeInput.setHint("Тип растения");
        EditText groupInput = new EditText(context); groupInput.setHint("Группа растения");
        EditText potVolumeInput = new EditText(context); potVolumeInput.setHint("Литраж горшка"); potVolumeInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        AutoCompleteTextView flowerColorInput = new AutoCompleteTextView(context); flowerColorInput.setHint("Цвет цветка");
        EditText countInput = new EditText(context); countInput.setHint("Количество"); countInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        EditText addInput = new EditText(context); addInput.setHint("Дополнительная информация");

        layout.addView(nameInput);
        layout.addView(typeInput);
        layout.addView(groupInput);
        layout.addView(potVolumeInput);
        layout.addView(flowerColorInput);
        layout.addView(countInput);
        layout.addView(addInput);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("Поиск")
                .setView(layout)
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
