package com.example.plantmap.model;

import java.util.List;

public class StatItem {
    public String title;               // заголовок карточки
    public String subtitle;            // пояснение / уточнение
    public boolean requiresInput;      // нужно ли открывать диалог выбора параметров
    public Runnable action;            // что делать при нажатии
    public List<StatItem> children;    // подпункты (может быть null или пусто)

    public StatItem(String title, String subtitle, boolean requiresInput, Runnable action) {
        this.title = title;
        this.subtitle = subtitle;
        this.requiresInput = requiresInput;
        this.action = action;
    }

    public StatItem(String title, String subtitle, List<StatItem> children) {
        this.title = title;
        this.subtitle = subtitle;
        this.children = children;
    }

    public boolean isSection() {
        return children != null && !children.isEmpty();
    }
}
