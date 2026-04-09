package com.example.plantmap.util;

import android.widget.EditText;

public class InputValidators {

    /**
     Универсальный валидатор положительного целого числа.
     input              поле ввода
     required           true — поле обязательно, false — можно оставить пустым (вернёт null без ошибки)
     errorEmpty         текст ошибки при пустом поле (если required = true)
     errorInvalid       текст ошибки при неверном формате числа
     errorNotPositive   текст ошибки, если число <= 0
     return            значение числа или null, если не прошло валидацию (ошибка уже установлена в поле)
     */
    public static Integer validatePositiveInt(EditText input, boolean required,
                                              String errorEmpty, String errorInvalid, String errorNotPositive) {
        String text = input.getText().toString().trim();
        if (text.isEmpty()) {
            if (required) {
                input.setError(errorEmpty);
            }
            return null;
        }
        try {
            int value = Integer.parseInt(text);
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

    // обертки для сохранения обратной совместимости

    public static Integer validatePositiveOptionalInt(EditText input) {
        return validatePositiveInt(
                input,
                false,                          // не обязательно
                "",                             // errorEmpty не используется
                "Неверное число",
                "Литраж должен быть больше 0"
        );
    }

    public static Integer validatePositiveCount(EditText input) {
        return validatePositiveInt(
                input,
                true,                           // обязательно
                "Количество обязательно",
                "Неверное число",
                "Количество должно быть больше 0"
        );
    }
}