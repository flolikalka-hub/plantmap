package com.example.plantmap.util;

import android.content.Context;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

public class ImeActionUtil {
    /**
     Настраивает цепочку перехода по кнопке "Далее" (IME_ACTION_NEXT)
     и автоматически делает все поля однострочными.

     fields поля ввода в порядке их следования
     */
    public static void setupImeChain(EditText... fields) {
        for (int i = 0; i < fields.length; i++) {
            EditText edit = fields[i];
            boolean isLast = (i == fields.length - 1);

            // однострочность чтобы энтер превратился в далее
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
     Скрывает клавиатуру
     */
    public static void hideKeyboard(View view) {
        /*У View есть Context
        используем, чтобы получить
        специальную системную службу InputMethodManager,
        которая управляет клавиатурой.*/
        InputMethodManager imm = (InputMethodManager) view.getContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    /**
     Запрашивает фокус и показывает клавиатуру для указанного view.
     */
    public static void focusAndShowKeyboard(View view) {
        view.requestFocus();
        view.requestFocusFromTouch();
        // выполняем код после того, как все события обработаются
        view.post(() -> {
            // получаем службу ввода (клаиатуру)
            InputMethodManager imm = (InputMethodManager) view.getContext()
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            // программный вызов, а не по требованию пользователя
            if (imm != null) {
                imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
            }
        });
    }
}
