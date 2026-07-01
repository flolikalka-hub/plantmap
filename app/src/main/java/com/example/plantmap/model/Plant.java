package com.example.plantmap.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Модель растения.
 * Содержит все характеристики растения и список доступных объёмов горшков.
 * Используется как в базе данных, так и в интерфейсе (адаптеры, формы).
 */
public class Plant {
    /** Идентификатор в БД (0 — ещё не сохранено). */
    public int id = 0;

    /** Название растения (например, "Роза чайно-гибридная"). */
    public String name;

    /** Тип растения (из таблицы variety). */
    public String type;

    /** Группа растения (из таблицы variety). */
    public String group;

    /** Дополнительная информация (описание, заметки). */
    public String additionalInfo;

    /** ID цвета цветка (ссылка на таблицу colors). */
    public Integer flowerColorId;

    /** Публичная ссылка на изображение (Яндекс.Диск). */
    public String imagePublicKey;

    /** Список доступных объёмов горшков в литрах. Инициализируется пустым списком. */
    public List<Integer> availablePotVolumes = new ArrayList<>();

    public Plant() {}

    /**
     * Конструктор для удобного создания растения с основными полями.
     */
    public Plant(String name, String type, Integer flowerColorId, String group,
                 String additionalInfo, String imagePublicKey) {
        this.name = name;
        this.type = type;
        this.flowerColorId = flowerColorId;
        this.group = group;
        this.additionalInfo = additionalInfo;
        this.imagePublicKey = imagePublicKey;
    }

    @Override
    public String toString() {
        return name;
    }

    public String getImagePublicKey() {
        return imagePublicKey;
    }

    public void setImagePublicKey(String imagePublicKey) {
        this.imagePublicKey = imagePublicKey;
    }
}