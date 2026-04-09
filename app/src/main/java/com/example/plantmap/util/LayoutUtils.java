package com.example.plantmap.util;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;

public class LayoutUtils {
    public static class ScrollableLayout {
        public final ScrollView scrollView;
        public final LinearLayout layout;

        public ScrollableLayout(ScrollView scrollView, LinearLayout layout) {
            this.scrollView = scrollView;
            this.layout = layout;
        }
    }

    public static ScrollableLayout createVerticalScrollView(Context context) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        ScrollView scrollView = new ScrollView(context);
        scrollView.setFillViewport(true);
        scrollView.addView(layout);
        return new ScrollableLayout(scrollView, layout);
    }

    public static ScrollView wrapInScrollView(Context context, View content) {
        ScrollView scrollView = new ScrollView(context);
        scrollView.setFillViewport(true);
        scrollView.addView(content);
        return scrollView;
    }
}