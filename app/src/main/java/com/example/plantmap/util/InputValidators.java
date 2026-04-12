package com.example.plantmap.util;

import android.widget.EditText;

public class InputValidators {

    /**
     Универсальный валидатор положительного целого числа.
     input              поле ввода
     required           true — поле обязательно, false — можно оставить пустым (вернет null без ошибки)
     errorEmpty         текст ошибки при пустом поле (если required = true)
     errorInvalid       текст ошибки при неверном формате числа
     errorNotPositive   текст ошибки, если число <= 0
     return             значение числа или null, если не прошло валидацию (ошибка уже установлена в поле)
     */
    public static Integer validatePositiveInt(EditText input,
                                              boolean required,
                                              String errorEmpty,
                                              String errorInvalid,
                                              String errorNotPositive) {
        String text = input.getText().toString().trim();
        // проверка на пустое поле
        if (text.isEmpty()) {
            if (required) {
                input.setError(errorEmpty);
            }
            return null;
        }
        // отлов "не числа"
        try {
            int value = Integer.parseInt(text);
            // проверка положительности
            if (value <= 0) {
                input.setError(errorNotPositive);
                return null;
            }
            return value;
        } catch (NumberFormatException e) {
            input.setError(errorInvalid);
            return null;
        }
    }

    /**
     обертки для сохранения обратной совместимости.
     переводят фокус на ошибку
     */
    public static Integer validatePositiveOptionalInt(EditText input) {
        Integer result = validatePositiveInt(
                input,
                false,      // не обязательно
                "",                 // errorEmpty не используется
                "Неверное число",
                "Литраж должен быть больше 0"
        );
        if (result == null) {
            input.requestFocus();
        }
        return result;
    }

    public static Integer validatePositiveCount(EditText input) {
        Integer result = validatePositiveInt(
                input,
                true,       // обязательно
                "Количество обязательно",
                "Неверное число",
                "Количество должно быть больше 0"
        );
        if (result == null) {
            input.requestFocus();
        }
        return result;
    }
}