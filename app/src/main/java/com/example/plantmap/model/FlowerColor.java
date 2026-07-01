package com.example.plantmap.model;

/**
 * Модель цвета цветка.
 * Хранит идентификатор, название и шестнадцатеричный код цвета (hex).
 * Используется для отображения цветовой маркировки растений в интерфейсе.
 */
public class FlowerColor {
    private final int id;
    private final String name;
    private final String hex;

    public FlowerColor(int id, String name, String hex) {
        this.id = id;
        this.name = name;
        this.hex = hex;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getHex() { return hex; }
}