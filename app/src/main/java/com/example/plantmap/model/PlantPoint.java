package com.example.plantmap.model;

public class PlantPoint {
    // POJO - простая структура данных без поведения
    public int id = 0; //id = 0 начит, что в БД еще нет
    public float x;
    public float y;
    public Plant plant;
    public int count;
    public Long processingDate;
    public Long feedingDate;
    public PlantPoint(float x, float y) {
        this.x = x;
        this.y = y;
        this.plant = new Plant();

        // по умолчанию — сегодня
        this.processingDate = System.currentTimeMillis();
    }
    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public long getProcessingDate() {
        return processingDate;
    }

    public long getFeedingDate() {
        return feedingDate;
    }

    public void setProcessingDate(long processingDate) {
        this.processingDate = processingDate;
    }

    public void setFeedingDate(long feedingDate) {
        this.feedingDate = feedingDate;
    }

    // для подсветки
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlantPoint)) return false;

        PlantPoint that = (PlantPoint) o;

        return this.id == that.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}
