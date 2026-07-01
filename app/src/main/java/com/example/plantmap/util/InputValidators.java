package com.example.plantmap.util;

import android.widget.EditText;

/**
 * Статические методы для валидации ввода в EditText.
 */
public class InputValidators {

    /**
     * Универсальный валидатор положительного целого числа.
     *
     * @param input          поле ввода
     * @param required       true — поле обязательно, false — можно оставить пустым (вернёт null без ошибки)
     * @param errorEmpty     сообщение при пустом обязательном поле
     * @param errorInvalid   сообщение при нечисловом значении
     * @param errorNotPositive сообщение при числе <= 0
     * @return значение числа или null, если валидация не пройдена (ошибка уже установлена в поле)
     */
    public static Integer validatePositiveInt(EditText input,
                                              boolean required,
                                              String errorEmpty,
                                              String errorInvalid,
                                              String errorNotPositive) {
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

    /**
     * Валидатор для опционального целого числа (обычно для литража).
     * При ошибке запрашивает фокус.
     */
    public static Integer validatePositiveOptionalInt(EditText input) {
        Integer result = validatePositiveInt(
                input,
                false,
                "", // не используется
                "Неверное число",
                "Литраж должен быть больше 0"
        );
        if (result == null) {
            input.requestFocus();
        }
        return result;
    }

    /**
     * Валидатор для обязательного положительного числа (обычно для количества).
     * При ошибке запрашивает фокус.
     */
    public static Integer validatePositiveCount(EditText input) {
        Integer result = validatePositiveInt(
                input,
                true,
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