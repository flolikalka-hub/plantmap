package com.example.plantmap.util;

import android.widget.EditText;

public class InputValidators {
    // проверка литража горшков
    public static Integer validatePositiveOptionalInt(EditText input) {
        String text = input.getText().toString().trim();

        if (text.isEmpty()) {
            return null; // пусто можно
        }

        int value;
        try {
            value = Integer.parseInt(text);
        } catch (NumberFormatException e) {
            input.setError("Неверное число");
            return null;
        }

        if (value <= 0) {
            input.setError("Литраж должен быть больше 0");
            return null;
        }

        return value;
    }

    // проверка количества горшков в точке
    public static Integer validatePositiveCount(EditText input) {
        String text = input.getText().toString().trim();

        if (text.isEmpty()) {
            input.setError("Количество обязательно");
            return null;
        }

        int value;
        try {
            value = Integer.parseInt(text);
        } catch (NumberFormatException e) {
            input.setError("Неверное число");
            return null;
        }

        if (value <= 0) {
            input.setError("Количество должно быть больше 0");
            return null;
        }

        return value;
    }
}
