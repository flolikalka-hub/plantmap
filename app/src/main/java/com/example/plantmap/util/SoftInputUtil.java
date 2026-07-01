package com.example.plantmap.util;

import android.app.Dialog;
import android.view.Window;
import android.view.WindowManager;

/**
 * Утилита для настройки поведения экранной клавиатуры в диалогах.
 */
public class SoftInputUtil {

    /**
     * Настраивает окно диалога так, чтобы клавиатура показывалась автоматически,
     * а окно изменяло размер при её появлении, не перекрывая поля ввода.
     *
     * @param dialog диалог, для окна которого применяются настройки
     */
    public static void setupSoftInput(Dialog dialog) {
        Window window = dialog.getWindow();
        if (window != null) {
            window.setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
                            | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            );
        }
    }
}