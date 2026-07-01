package com.example.plantmap.util;

import android.content.Context;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

/**
 * Утилиты для работы с клавиатурой и переходами между полями ввода (IME Actions).
 */
public class ImeActionUtil {

    /**
     * Настраивает цепочку полей: каждое поле получает action "Далее",
     * последнее — "Готово". Все поля становятся однострочными.
     * При нажатии "Далее" фокус переходит к следующему полю.
     *
     * @param fields поля ввода в порядке их следования
     */
    public static void setupImeChain(EditText... fields) {
        for (int i = 0; i < fields.length; i++) {
            EditText edit = fields[i];
            boolean isLast = (i == fields.length - 1);

            // Однострочность, чтобы Enter превращался в "Далее"
            edit.setSingleLine(true);
            edit.setImeOptions(isLast ? EditorInfo.IME_ACTION_DONE : EditorInfo.IME_ACTION_NEXT);

            int nextIndex = i + 1;
            edit.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_NEXT && !isLast) {
                    fields[nextIndex].requestFocus();
                    return true;
                } else if (actionId == EditorInfo.IME_ACTION_DONE) {
                    v.clearFocus();
                    hideKeyboard(v);
                    return true;
                }
                return false;
            });
        }
    }

    /**
     * Скрывает экранную клавиатуру.
     */
    public static void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) view.getContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    /**
     * Запрашивает фокус и показывает клавиатуру для указанного view.
     */
    public static void focusAndShowKeyboard(View view) {
        view.requestFocus();
        view.requestFocusFromTouch();
        // Выполняем после завершения всех pending-событий
        view.post(() -> {
            InputMethodManager imm = (InputMethodManager) view.getContext()
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
            }
        });
    }
}