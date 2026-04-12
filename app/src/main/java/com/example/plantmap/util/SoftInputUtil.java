package com.example.plantmap.util;

import android.app.Dialog;
import android.view.Window;
import android.view.WindowManager;

public class SoftInputUtil {
    /**
     Настраивает режим ввода для диалога:
     клавиатура автоматически показывается при открытии
     диалог изменяет размеры при появлении клавиатуры

     dialog                 диалог, для окна которого применяются настройки
     */
    public static void setupSoftInput(Dialog dialog) {
        Window window = dialog.getWindow();
        if (window != null) {
            dialog.getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
                            | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            );
        }
    }

}
