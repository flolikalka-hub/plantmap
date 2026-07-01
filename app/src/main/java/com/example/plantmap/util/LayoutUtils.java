package com.example.plantmap.util;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;

/**
 * Фабрики для создания часто используемых комбинаций View.
 */
public class LayoutUtils {

    /**
     * Контейнер, объединяющий ScrollView и внутренний LinearLayout.
     */
    public static class ScrollableLayout {
        public final ScrollView scrollView;
        public final LinearLayout layout;

        public ScrollableLayout(ScrollView scrollView, LinearLayout layout) {
            this.scrollView = scrollView;
            this.layout = layout;
        }
    }

    /**
     * Создаёт вертикальный прокручиваемый контейнер с LinearLayout внутри.
     * Используется, когда нужен полный контроль над добавляемыми View,
     * а не готовый XML-макет.
     */
    public static ScrollableLayout createVerticalScrollView(Context context) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        ScrollView scrollView = new ScrollView(context);
        scrollView.setFillViewport(true);
        scrollView.addView(layout);
        return new ScrollableLayout(scrollView, layout);
    }

    /**
     * Оборачивает готовый View в ScrollView.
     * Удобно, когда содержимое уже создано (например, из LayoutInflater),
     * но нужна прокрутка.
     */
    public static ScrollView wrapInScrollView(Context context, View content) {
        ScrollView scrollView = new ScrollView(context);
        scrollView.setFillViewport(true);
        scrollView.addView(content);
        return scrollView;
    }
}