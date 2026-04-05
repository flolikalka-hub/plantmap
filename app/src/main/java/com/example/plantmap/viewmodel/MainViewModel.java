package com.example.plantmap.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import com.example.plantmap.R;

public class MainViewModel extends ViewModel {
    private static final String KEY_SELECTED_SCREEN = "selected_screen";

    private final SavedStateHandle savedStateHandle;
    private final MutableLiveData<Integer> selectedScreen;

    public MainViewModel(SavedStateHandle savedStateHandle) {
        this.savedStateHandle = savedStateHandle;
        Integer initialValue = savedStateHandle.get(KEY_SELECTED_SCREEN);
        if (initialValue == null) {
            initialValue = R.id.nav_plan;
            savedStateHandle.set(KEY_SELECTED_SCREEN, initialValue);
        }
        selectedScreen = new MutableLiveData<>(initialValue);
    }

    public LiveData<Integer> getSelectedScreen() {
        return selectedScreen;
    }

    public void selectScreen(int menuId) {
        selectedScreen.setValue(menuId);
        savedStateHandle.set(KEY_SELECTED_SCREEN, menuId);
    }
}
