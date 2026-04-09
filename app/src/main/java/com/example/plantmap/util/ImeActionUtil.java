package com.example.plantmap.util;

import android.content.Context;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

public class ImeActionUtil {
    public static void setupImeChain(EditText... fields) {
        for (int i = 0; i < fields.length; i++) {
            EditText edit = fields[i];
            boolean isLast = (i == fields.length - 1);

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

    public static void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) view.getContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}
