package com.example.plantmap.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.plantmap.MainActivity;
import com.example.plantmap.R;
import com.example.plantmap.db.DbView;

/**
 * Фрагмент базы данных растений.
 * Отображает список растений с возможностью поиска, сброса поиска
 * и просмотра детальной информации. Использует кастомный DbView.
 */
public class DbFragment extends BaseFragment {

    /** Текст справки для этого фрагмента. */
    @Override
    protected int getHelpTextResId() {
        return R.string.help_db;
    }

    private DbView dbView;
    /** Флаг активности поиска (влияет на видимость кнопки сброса). */
    private boolean dbSearchActive = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        dbView = new DbView(
                requireContext(),
                null,
                ((MainActivity) requireActivity()).getRepository()
        );

        // Слушатель событий поиска для обновления состояния меню
        dbView.setSearchStateListener(new DbView.SearchStateListener() {
            @Override
            public void onSearchApplied() {
                dbSearchActive = true;
                requireActivity().invalidateOptionsMenu(); // пересоздать меню
            }

            @Override
            public void onSearchCleared() {
                dbSearchActive = false;
                requireActivity().invalidateOptionsMenu();
            }
        });
        return dbView.createDbView();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Обновляем данные при возвращении на фрагмент
        if (dbView != null) {
            dbView.refresh();
        }
    }

    /** Добавляет к общему меню специфичные пункты (поиск, сброс). */
    @Override
    protected void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateMenu(menu, inflater); // menu_common
        inflater.inflate(R.menu.menu_db, menu);
    }

    /** Управляет видимостью кнопки сброса поиска в зависимости от dbSearchActive. */
    @Override
    public void onPrepareMenu(@NonNull Menu menu) {
        super.onPrepareMenu(menu);
        MenuItem resetItem = menu.findItem(R.id.action_reset_db);
        if (resetItem != null) {
            resetItem.setVisible(dbSearchActive);
        }
    }

    /** Обрабатывает пункты меню, специфичные для БД (поиск, сброс). */
    @Override
    protected boolean onMenuItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_search_db) {
            dbView.showSearchDialog();
            return true;
        }
        if (id == R.id.action_reset_db) {
            dbView.resetSearch();
            dbSearchActive = false;
            requireActivity().invalidateOptionsMenu();
            return true;
        }
        return false; // справка обработается в BaseFragment
    }
}