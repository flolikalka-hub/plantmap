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
 * Базовый фрагмент с общей логикой меню и справки.
 * Каждый наследник обязан реализовать getHelpTextResId() для отображения
 * своего текста справки. По умолчанию добавляет общее меню menu_common,
 * которое можно расширить или переопределить.
 */
public abstract class BaseFragment extends Fragment {

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupMenu();
    }

    /**
     * Регистрирует MenuProvider, привязанный к жизненному циклу view.
     * Обеспечивает вызов методов onCreateMenu/onPrepareMenu/onMenuItemSelected
     * у наследников, а также обрабатывает общий пункт "Справка".
     */
    private void setupMenu() {
        MenuHost menuHost = requireActivity();
        menuHost.addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
                // Даём наследнику возможность добавить свои пункты меню
                BaseFragment.this.onCreateMenu(menu, inflater);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem item) {
                // Сначала даём шанс наследнику обработать пункт
                if (BaseFragment.this.onMenuItemSelected(item)) {
                    return true;
                }
                // Общий пункт — справка
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
     * Вызывается при создании меню. По умолчанию надувает menu_common.
     * Переопределите в наследнике, чтобы добавить специфичные пункты.
     */
    protected void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_common, menu);
    }

    /**
     * Вызывается перед показом меню. Можно использовать для изменения
     * видимости или доступности пунктов меню.
     */
    protected void onPrepareMenu(@NonNull Menu menu) {
        // По умолчанию пусто
    }

    /**
     * Обработчик пунктов меню, специфичных для конкретного фрагмента.
     *
     * @param item выбранный пункт меню
     * @return true, если пункт обработан, иначе false (будет передано дальше)
     */
    protected boolean onMenuItemSelected(@NonNull MenuItem item) {
        return false;
    }

    /**
     * Ресурс строки с текстом справки для данного фрагмента.
     * Обязателен к реализации в наследниках.
     */
    @StringRes
    protected abstract int getHelpTextResId();

    /**
     * Показывает диалоговое окно со справкой.
     */
    private void showHelp() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Справка")
                .setMessage(getString(getHelpTextResId()))
                .setPositiveButton("ОК", null)
                .show();
    }
}