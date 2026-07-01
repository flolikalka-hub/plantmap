package com.example.plantmap.model;

import java.util.List;

/**
 * Элемент статистики для отображения в виде карточки или раскрывающегося списка.
 * Может быть как конечным действием, так и секцией с дочерними элементами.
 * Используется в StatisticsView для построения дерева статистик.
 */
public class StatItem {
    /** Заголовок карточки. */
    public String title;

    /** Пояснение или уточнение (может быть null). */
    public String subtitle;

    /** Требуется ли диалог для ввода параметров (например, количества дней). */
    public boolean requiresInput;

    /** Действие при нажатии (если не секция). */
    public Runnable action;

    /** Подпункты (если это секция). Может быть null или пустым. */
    public List<StatItem> children;

    /**
     * Конструктор для конечного действия.
     */
    public StatItem(String title, String subtitle, boolean requiresInput, Runnable action) {
        this.title = title;
        this.subtitle = subtitle;
        this.requiresInput = requiresInput;
        this.action = action;
    }

    /**
     * Конструктор для секции с дочерними элементами.
     */
    public StatItem(String title, String subtitle, List<StatItem> children) {
        this.title = title;
        this.subtitle = subtitle;
        this.children = children;
    }

    /**
     * Является ли этот элемент секцией (содержит дочерние элементы).
     */
    public boolean isSection() {
        return children != null && !children.isEmpty();
    }
}