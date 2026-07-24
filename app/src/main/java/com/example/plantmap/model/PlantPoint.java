package com.example.plantmap.model;

/**
 * Точка на плане, представляющая одно или несколько растений.
 * Содержит координаты, количество, даты обработки/подкормки,
 * выбранный объём горшка и ссылку на объект растения.
 *
 * equals() и hashCode() переопределены по id для корректной работы подсветки
 * (например, в результатах поиска).
 */
public class PlantPoint {
    /** Идентификатор в БД (0 — точка ещё не сохранена). */
    public String id = null;

    /** Координата X на плане (в эталонных пикселях, не зависит от плотности экрана). */
    public float x;

    /** Координата Y на плане. */
    public float y;

    /** Растение, которое находится в этой точке. */
    public Plant plant;

    /** Количество растений в данной точке. */
    public int count;

    /** Дата последней обработки в миллисекундах (Unix time) или null. */
    public Long processingDate;

    /** Дата последней подкормки в миллисекундах (Unix time) или null. */
    public Long feedingDate;

    /** Объём горшка для этой точки (в литрах), может быть null. */
    public Integer potVolume;

    public PlantPoint(float x, float y) {
        this.x = x;
        this.y = y;
        this.plant = new Plant();
        // processingDate по умолчанию не устанавливается, чтобы можно было
        // различать "не обработано" и "обработано только что".
    }

    public float getX() { return x; }
    public void setX(float x) { this.x = x; }

    public float getY() { return y; }
    public void setY(float y) { this.y = y; }

    public long getProcessingDate() { return processingDate; }
    public void setProcessingDate(long processingDate) { this.processingDate = processingDate; }

    public long getFeedingDate() { return feedingDate; }
    public void setFeedingDate(long feedingDate) { this.feedingDate = feedingDate; }

    public Integer getPotVolume() { return potVolume; }
    public void setPotVolume(Integer potVolume) { this.potVolume = potVolume; }

    /**
     * Сравнение по id (для подсветки найденных точек).
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlantPoint)) return false;
        PlantPoint that = (PlantPoint) o;
        return this.id != null && this.id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}