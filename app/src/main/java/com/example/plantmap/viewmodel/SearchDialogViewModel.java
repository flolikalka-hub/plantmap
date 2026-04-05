package com.example.plantmap.viewmodel;

import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import com.example.plantmap.model.SearchFilter;

public class SearchDialogViewModel extends ViewModel {
    private static final String KEY_NAME = "name";
    private static final String KEY_TYPE = "type";
    private static final String KEY_GROUP = "group";
    private static final String KEY_POT = "pot";
    private static final String KEY_COLOR = "color";
    private static final String KEY_COUNT = "count";
    private static final String KEY_ADDITIONAL = "additional";
    private static final String KEY_PROCESSING = "processing";
    private static final String KEY_FEEDING = "feeding";

    private final SavedStateHandle state;

    public SearchDialogViewModel(SavedStateHandle state) {
        this.state = state;
    }

    public void setText(String key, String value) {
        state.set(key, value);
    }

    public String getText(String key) {
        String value = state.get(key);
        return value == null ? "" : value;
    }

    public void setProcessingDate(Long millis) {
        state.set(KEY_PROCESSING, millis);
    }

    public void setFeedingDate(Long millis) {
        state.set(KEY_FEEDING, millis);
    }

    public Long getProcessingDate() {
        return state.get(KEY_PROCESSING);
    }

    public Long getFeedingDate() {
        return state.get(KEY_FEEDING);
    }

    public SearchFilter buildFilter() {
        SearchFilter filter = new SearchFilter();
        filter.name = getText(KEY_NAME).trim();
        filter.type = getText(KEY_TYPE).trim();
        filter.group = getText(KEY_GROUP).trim();
        filter.flowerColor = getText(KEY_COLOR).trim();
        filter.additionalInfo = getText(KEY_ADDITIONAL).trim();
        filter.potVolume = parseIntOrNull(getText(KEY_POT).trim());
        filter.count = parseIntOrNull(getText(KEY_COUNT).trim());
        filter.processingDate = getProcessingDate();
        filter.feedingDate = getFeedingDate();
        return filter;
    }

    private Integer parseIntOrNull(String s) {
        try {
            return s.isEmpty() ? null : Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static String keyName() { return KEY_NAME; }
    public static String keyType() { return KEY_TYPE; }
    public static String keyGroup() { return KEY_GROUP; }
    public static String keyPot() { return KEY_POT; }
    public static String keyColor() { return KEY_COLOR; }
    public static String keyCount() { return KEY_COUNT; }
    public static String keyAdditional() { return KEY_ADDITIONAL; }
}