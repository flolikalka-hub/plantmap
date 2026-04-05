package com.example.plantmap.viewmodel;

import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import com.example.plantmap.model.PlantPoint;
import com.example.plantmap.plan.EditMode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class PlanViewModel extends ViewModel {
    private static final String KEY_EDIT_MODE = "edit_mode";
    private static final String KEY_SEARCH_RESULTS = "search_results";

    private final SavedStateHandle savedStateHandle;

    public PlanViewModel(SavedStateHandle savedStateHandle) {
        this.savedStateHandle = savedStateHandle;
    }

    public EditMode getEditMode() {
        String mode = savedStateHandle.get(KEY_EDIT_MODE);
        if (mode == null) {
            return EditMode.VIEW;
        }
        return EditMode.valueOf(mode);
    }

    public void setEditMode(EditMode mode) {
        savedStateHandle.set(KEY_EDIT_MODE, mode.name());
    }

    public void setSearchResults(Set<PlantPoint> points) {
        savedStateHandle.set(KEY_SEARCH_RESULTS, new ArrayList<>(points));
    }

    @SuppressWarnings("unchecked")
    public Set<PlantPoint> getSearchResults() {
        ArrayList<PlantPoint> saved = savedStateHandle.get(KEY_SEARCH_RESULTS);
        if (saved == null) {
            return new HashSet<>();
        }
        return new HashSet<>(saved);
    }

    public boolean hasSearchResults() {
        ArrayList<PlantPoint> saved = savedStateHandle.get(KEY_SEARCH_RESULTS);
        return saved != null && !saved.isEmpty();
    }

    public void clearSearchResults() {
        savedStateHandle.set(KEY_SEARCH_RESULTS, new ArrayList<PlantPoint>());
    }
}