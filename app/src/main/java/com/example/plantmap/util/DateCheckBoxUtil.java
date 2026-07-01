package com.example.plantmap.util;

import android.app.DatePickerDialog;
import android.widget.CheckBox;
import android.widget.EditText;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Утилита, связывающая CheckBox и EditText для ввода/отображения даты.
 * При установке галочки автоматически подставляется сегодняшняя дата,
 * при снятии — дата сбрасывается. По клику на поле открывается DatePickerDialog.
 *
 * Используется в диалогах создания/редактирования точки для указания
 * дат обработки и подкормки.
 */
public class DateCheckBoxUtil {

    private final CheckBox checkBox;
    private final EditText dateEditText;
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
    private Long currentDate; // null, если дата не выбрана
    private final OnDateChangedListener listener;

    /** Слушатель изменения даты. */
    public interface OnDateChangedListener {
        void onDateChanged(Long date);
    }

    /**
     * Создаёт экземпляр утилиты и сразу настраивает поведение чекбокса и поля.
     *
     * @param checkBox    чекбокс "Уже обрабатывалось" / "Уже подкармливалось"
     * @param dateEditText поле для отображения даты
     * @param listener    слушатель, вызываемый при изменении даты
     */
    public DateCheckBoxUtil(CheckBox checkBox, EditText dateEditText, OnDateChangedListener listener) {
        this.checkBox = checkBox;
        this.dateEditText = dateEditText;
        this.listener = listener;
        init();
    }

    /**
     * Настраивает начальное состояние и обработчики событий.
     * Запрещает фокус поля (чтобы не вылезала клавиатура),
     * вешает слушатель на чекбокс и клик по полю.
     */
    private void init() {
        dateEditText.setFocusable(false); // чтобы не показывать клавиатуру
        dateEditText.setText("Не выбрано");

        // Галочка: включена -> сегодня, выключена -> null
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

        // Клик по полю: открыть DatePicker, если дата уже выбрана
        dateEditText.setOnClickListener(v -> {
            if (currentDate != null) showDatePicker(currentDate);
        });
    }

    /**
     * Устанавливает дату программно (например, при редактировании существующей точки).
     * Временно отключает слушатель чекбокса, чтобы избежать рекурсии.
     *
     * @param date миллисекунды (Unix time) или null для сброса
     */
    public void setDate(Long date) {
        // Временно убираем слушатель, чтобы не уйти в рекурсию
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
     * Показывает диалог выбора даты, начиная с переданного значения.
     * После выбора обнуляет время (часы/минуты/секунды), так как
     * для поиска и сравнения важна только дата.
     *
     * @param initialDate начальная дата в миллисекундах
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
                cal.get(Calendar.DAY_OF_MONTH))
                .show();
    }
}