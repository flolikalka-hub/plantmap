package com.example.plantmap.model;

/**
 * Фильтр для поиска точек на плане.
 * Содержит критерии для отбора точек по характеристикам растений,
 * объёму горшка, количеству, датам обработки/подкормки и цвету.
 * Используется в PlanView и StatisticsView.
 */
public class SearchFilter {
    public String name;
    public String type;
    public String group;
    public String additionalInfo;

    /** Объём горшка в литрах (null — без фильтрации). */
    public Integer potVolume;

    /** Количество растений в точке (null — любое). */
    public Integer count;

    /** Дата обработки в миллисекундах (Unix time) или null. */
    public Long processingDate;

    /** Дата подкормки в миллисекундах или null. */
    public Long feedingDate;

    /** ID цвета цветка (null — любой цвет). */
    public Integer flowerColorId;
}