package com.example.plantmap.model;

public class PlantPoint {
    // POJO - простая структура данных без поведения
    public int id = 0; //id = 0 начит, что в БД еще нет
    public float x;
    public float y;
    public Plant plant;
    public int count;

    public PlantPoint(float x, float y) {
        this.x = x;
        this.y = y;
        this.plant = new Plant();
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
}
