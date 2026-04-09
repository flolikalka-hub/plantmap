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

public class DbFragment extends BaseFragment {
    @Override
    protected int getHelpTextResId() {
        return R.string.help_db;
    }

    private DbView dbView;
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

        dbView.setSearchStateListener(new DbView.SearchStateListener() {
            @Override
            public void onSearchApplied() {
                dbSearchActive = true;
                requireActivity().invalidateOptionsMenu();
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
    protected void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateMenu(menu, inflater); // добавляет menu_common
        inflater.inflate(R.menu.menu_db, menu); // добавляем специфичное для БД меню
    }

    @Override
    public void onPrepareMenu(@NonNull Menu menu) {
        super.onPrepareMenu(menu);
        MenuItem resetItem = menu.findItem(R.id.action_reset_db);
        if (resetItem != null) {
            resetItem.setVisible(dbSearchActive);
        }
    }

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
        return false; // справка уже обработана в BaseFragment
    }
}