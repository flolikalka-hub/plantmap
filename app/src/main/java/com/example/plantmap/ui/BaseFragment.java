package com.example.plantmap.ui;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;

import com.example.plantmap.R;
/**
Общие элементы меню (справка) и само окно справки
*/
public abstract class BaseFragment extends Fragment {

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupMenu();
    }

    private void setupMenu() {
        MenuHost menuHost = requireActivity();
        menuHost.addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
                // Даем наследнику возможность добавить пункты меню
                BaseFragment.this.onCreateMenu(menu, inflater);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem item) {
                // Сначала даем шанс наследнику, потом обрабатываем общие пункты (справка)
                if (BaseFragment.this.onMenuItemSelected(item)) {
                    return true;
                }
                if (item.getItemId() == R.id.action_help) {
                    showHelp();
                    return true;
                }
                return false;
            }

            @Override
            public void onPrepareMenu(@NonNull Menu menu) {
                BaseFragment.this.onPrepareMenu(menu);
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }

    /**
     * Переопределите, чтобы добавить свои пункты меню.
     * По умолчанию добавляет menu_common.
     */
    protected void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_common, menu);
    }

    /**
     * Переопределите, чтобы настроить видимость/доступность пунктов меню.
     */
    protected void onPrepareMenu(@NonNull Menu menu) {
        // Пусто
    }

    /**
     * Переопределите для обработки специфических пунктов меню.
     * @return true если пункт обработан, иначе false.
     */
    protected boolean onMenuItemSelected(@NonNull MenuItem item) {
        return false;
    }

    @StringRes
    protected abstract int getHelpTextResId();

    private void showHelp() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Справка")
                .setMessage(getString(getHelpTextResId()))
                .setPositiveButton("ОК", null)
                .show();
    }
}