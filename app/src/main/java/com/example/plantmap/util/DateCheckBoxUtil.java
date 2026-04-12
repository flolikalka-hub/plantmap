package com.example.plantmap.util;

import android.app.DatePickerDialog;
import android.widget.CheckBox;
import android.widget.EditText;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
/**
 Чекбоксы для даты
     CheckBox checkBox                  галочка
     EditText dateEditText              поле, где показывается дата
     SimpleDateFormat sdf               преобразователь даты в текст вида "31.12.2024"
     Long currentDate                   текущая выбранная дата в миллисекундах
     OnDateChangedListener listener     слушатель, который сообщает "снаружи", что дата изменилась
 */
public class DateCheckBoxUtil {
    private final CheckBox checkBox;
    private final EditText dateEditText;
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
    private Long currentDate;
    private final OnDateChangedListener listener;

    public interface OnDateChangedListener {
        void onDateChanged(Long date);
    }

    public DateCheckBoxUtil(CheckBox checkBox, EditText dateEditText, OnDateChangedListener listener) {
        this.checkBox = checkBox;
        this.dateEditText = dateEditText;
        this.listener = listener;
        init();
    }
    /**
     dateEditText.setFocusable(false);              запрещает фокус,
                                                    чтобы при клике не выскакивала клавиатура

     Вешает слушатель на чекбокс:
         если галочку поставили — ставим сегодняшнюю дату (setDate(today.getTimeInMillis())),
         если сняли — setDate(null).

     Вешает слушатель на клик по полю:
         если дата уже выбрана (currentDate != null),
         показывает диалог выбора даты.
     */
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

     Временно убирает слушатель с чекбокса,
     чтобы изменение галочки не вызвало повторный вызов setDate (рекурсия)

     Сохраняет currentDate
     Делает поле доступным (setEnabled), если дата не null

     Устанавливает текст:
             либо форматированная дата,
             либо "Не выбрано"

     Устанавливает состояние чекбокса
     Возвращает слушатель на место

     Вызывает listener.onDateChanged(date),
     чтобы внешний код узнал об изменении
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

     Calendar cal = Calendar.getInstance(); cal.setTimeInMillis(initialDate);
     переводим миллисекунды в понятный календарю формат

     Создаем диалог. Ему нужен Context — берём его у чекбокса (checkBox.getContext()).

     Когда пользователь выбрал дату, вызывается лямбда:
     мы создаем новый Calendar, устанавливаем год, месяц, день,
     обнуляем время (часы, минуты, секунды) и вызываем setDate()

     обнуляем ибо интересует только день и чтобы не было проблем при поиске
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
