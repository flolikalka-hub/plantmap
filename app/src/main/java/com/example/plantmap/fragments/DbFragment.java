package com.example.plantmap.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;

import com.example.plantmap.MainActivity;
import com.example.plantmap.R;
import com.example.plantmap.db.DbView;
import com.example.plantmap.fragments.HelpDialogFragment;

public class DbFragment extends Fragment {

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
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MenuHost menuHost = requireActivity();
        menuHost.addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.menu_db, menu);
                menuInflater.inflate(R.menu.menu_common, menu); // справка
            }

            @Override
            public void onPrepareMenu(@NonNull Menu menu) {
                MenuItem resetItem = menu.findItem(R.id.action_reset_db);
                if (resetItem != null) {
                    resetItem.setVisible(dbSearchActive);
                }
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.action_search_db) {
                    dbView.showSearchDialog();
                    return true;
                }
                if (menuItem.getItemId() == R.id.action_reset_db) {
                    dbView.resetSearch();
                    dbSearchActive = false;
                    requireActivity().invalidateOptionsMenu();
                    return true;
                }
                if (menuItem.getItemId() == R.id.action_help) {
                    showHelp();
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }
    private void showHelp() {
        HelpDialogFragment.newInstance(R.string.help_db)
                .show(getParentFragmentManager(), HelpDialogFragment.TAG);
    }
}
