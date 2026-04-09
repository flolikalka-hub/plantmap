package com.example.plantmap.util;

import android.app.DatePickerDialog;
import android.widget.CheckBox;
import android.widget.EditText;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DateCheckBoxHelper {
    private final CheckBox checkBox;
    private final EditText dateEditText;
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
    private Long currentDate;
    private final OnDateChangedListener listener;

    public interface OnDateChangedListener {
        void onDateChanged(Long date);
    }

    public DateCheckBoxHelper(CheckBox checkBox, EditText dateEditText, OnDateChangedListener listener) {
        this.checkBox = checkBox;
        this.dateEditText = dateEditText;
        this.listener = listener;
        init();
    }

    private void init() {
        dateEditText.setFocusable(false);// чтобы по умолчанию не лезла клавиатура
        dateEditText.setText("Не выбрано");
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                Calendar today = Calendar.getInstance();
                today.set(Calendar.HOUR_OF_DAY, 0);
                today.set(Calendar.MINUTE, 0);
                today.set(Calendar.SECOND, 0);
                today.set(Calendar.MILLISECOND, 0);
                setDate(today.getTimeInMillis());
            } else {
                setDate(null);
            }
        });
        dateEditText.setOnClickListener(v -> {
            if (currentDate != null) showDatePicker(currentDate);
        });
    }
    /**
     Установить дату программно
     например, при редактировании существующей точки
     */
    public void setDate(Long date) {
        // Временно убираем слушатель, чтобы не вызвать рекурсию
        checkBox.setOnCheckedChangeListener(null);

        currentDate = date;
        dateEditText.setEnabled(date != null);
        dateEditText.setText(date != null ? sdf.format(new Date(date)) : "Не выбрано");
        checkBox.setChecked(date != null);

        // Восстанавливаем слушатель
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                Calendar today = Calendar.getInstance();
                today.set(Calendar.HOUR_OF_DAY, 0);
                today.set(Calendar.MINUTE, 0);
                today.set(Calendar.SECOND, 0);
                today.set(Calendar.MILLISECOND, 0);
                setDate(today.getTimeInMillis());
            } else {
                setDate(null);
            }
        });

        listener.onDateChanged(date);
    }
    /**
    Возвращает текущее значение даты
     может быть null
    */
    private void showDatePicker(long initialDate) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(initialDate);
        new DatePickerDialog(checkBox.getContext(),
                (view, year, month, day) -> {
                    Calendar selected = Calendar.getInstance();
                    selected.set(year, month, day, 0, 0, 0);
                    selected.set(Calendar.MILLISECOND, 0);
                    setDate(selected.getTimeInMillis());
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)).show();
    }
}
