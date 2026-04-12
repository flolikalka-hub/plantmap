package com.example.plantmap.util;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
/**
 ScrollView с LinearLayout
 */
public class LayoutUtils {
    public static class ScrollableLayout {
        public final ScrollView scrollView;
        public final LinearLayout layout;

        public ScrollableLayout(ScrollView scrollView, LinearLayout layout) {
            this.scrollView = scrollView;
            this.layout = layout;
        }
    }
    /**
     создание прокручиваемого контейнера,
     когда нужен полный контроль
     над добавляемыми View и логикой сохранения
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
     берет уже готовую деталь (content)
     и заворачивает её в прокрутку
     */
    public static ScrollView wrapInScrollView(Context context, View content) {
        ScrollView scrollView = new ScrollView(context);
        scrollView.setFillViewport(true);
        scrollView.addView(content);
        return scrollView;
    }
}