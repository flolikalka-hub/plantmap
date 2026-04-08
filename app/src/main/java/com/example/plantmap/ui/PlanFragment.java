package com.example.plantmap.ui;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;

import com.example.plantmap.MainActivity;
import com.example.plantmap.R;
import com.example.plantmap.model.PlantPoint;
import com.example.plantmap.plan.EditMode;
import com.example.plantmap.plan.PlanView;

import java.util.Set;

public class PlanFragment extends Fragment {

    private PlanView planView;
    private boolean addActive = false;
    private boolean editActive = false;
    private Set<PlantPoint> pendingSearchResults;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        FrameLayout root = new FrameLayout(requireContext());

        planView = new PlanView(
                requireContext(),
                ((MainActivity) requireActivity()).getRepository()
        );

        root.addView(
                planView,
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                )
        );

        LinearLayout btnCont = new LinearLayout(requireContext());
        FrameLayout.LayoutParams contParams =
                new FrameLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
        contParams.gravity = Gravity.BOTTOM | Gravity.END;

        ImageButton btnSearch = new ImageButton(requireContext());
        btnSearch.setImageResource(R.drawable.btn_find);
        btnSearch.setBackground(null);

        ImageButton btnAdd = new ImageButton(requireContext());
        btnAdd.setImageResource(R.drawable.btn_add_point);
        btnAdd.setBackground(null);

        ImageButton btnEdit = new ImageButton(requireContext());
        btnEdit.setImageResource(R.drawable.btn_edit_point);
        btnEdit.setBackground(null);

        btnCont.addView(btnSearch);
        btnCont.addView(btnAdd);
        btnCont.addView(btnEdit);

        btnAdd.setOnClickListener(v -> {
            if (addActive) {
                planView.setEditMode(EditMode.VIEW);
                btnAdd.setImageResource(R.drawable.btn_add_point);
                addActive = false;
            } else {
                planView.setEditMode(EditMode.ADD_POINT);
                btnAdd.setImageResource(R.drawable.btn_add_point_active);
                addActive = true;

                if (editActive) {
                    btnEdit.setImageResource(R.drawable.btn_edit_point);
                    editActive = false;
                }
            }
        });

        btnEdit.setOnClickListener(v -> {
            if (editActive) {
                planView.setEditMode(EditMode.VIEW);
                btnEdit.setImageResource(R.drawable.btn_edit_point);
                editActive = false;
            } else {
                planView.setEditMode(EditMode.EDIT_POINT);
                btnEdit.setImageResource(R.drawable.btn_edit_point_active);
                editActive = true;

                if (addActive) {
                    btnAdd.setImageResource(R.drawable.btn_add_point);
                    addActive = false;
                }
            }
        });

        planView.setSearchStateListener(new PlanView.SearchStateListener() {
            @Override
            public void onSearchApplied() {
                btnSearch.setImageResource(R.drawable.btn_find_active);
            }

            @Override
            public void onSearchCleared() {
                btnSearch.setImageResource(R.drawable.btn_find);
                // сбрасываем аргументы полностью
                if (getArguments() != null) {
                    getArguments().remove("search_points");
                }
            }
        });

        btnSearch.setOnClickListener(v -> {
            if (planView.isSearchActive()) {
                planView.clearSearch();
            } else {
                planView.showSearchDialog();
            }
        });

        root.addView(btnCont, contParams);

        if (pendingSearchResults != null) {
            planView.setSearchResults(pendingSearchResults);
            pendingSearchResults = null;
        }

        if (getArguments() != null) {
            Set<PlantPoint> points =
                    (Set<PlantPoint>) getArguments().getSerializable("search_points");

            if (points != null) {
                planView.setSearchResults(points);
                // очищаем, чтобы не восстанавливалось после поворота
                //getArguments().remove("search_points");
            }
        }

        return root;
    }

    public void setSearchResults(Set<PlantPoint> points) {
        if (planView != null) {
            planView.setSearchResults(points);
        } else {
            pendingSearchResults = points;
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MenuHost menuHost = requireActivity();
        menuHost.addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.menu_common, menu); // справка
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.action_help) {
                    showHelp();
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }
    private void showHelp() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Справка")
                .setMessage(getString(R.string.help_plan))
                .setPositiveButton("ОК", null)
                .show();
    }
}
