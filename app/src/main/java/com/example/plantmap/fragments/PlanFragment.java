package com.example.plantmap.fragments;

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
import androidx.lifecycle.ViewModelProvider;

import com.example.plantmap.MainActivity;
import com.example.plantmap.R;
import com.example.plantmap.model.PlantPoint;
import com.example.plantmap.plan.EditMode;
import com.example.plantmap.plan.PlanView;
import com.example.plantmap.viewmodel.PlanViewModel;
import com.example.plantmap.fragments.SearchDialogFragment;
import com.example.plantmap.fragments.HelpDialogFragment;

import java.util.Set;

public class PlanFragment extends Fragment {

    private PlanView planView;
    private ImageButton btnSearch;
    private ImageButton btnAdd;
    private ImageButton btnEdit;
    private Set<PlantPoint> pendingSearchResults;
    private PlanViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        viewModel = new ViewModelProvider(this).get(PlanViewModel.class);

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

        btnSearch = new ImageButton(requireContext());
        btnSearch.setImageResource(R.drawable.btn_find);
        btnSearch.setBackground(null);

        btnAdd = new ImageButton(requireContext());
        btnAdd.setImageResource(R.drawable.btn_add_point);
        btnAdd.setBackground(null);

        btnEdit = new ImageButton(requireContext());
        btnEdit.setImageResource(R.drawable.btn_edit_point);
        btnEdit.setBackground(null);

        btnCont.addView(btnSearch);
        btnCont.addView(btnAdd);
        btnCont.addView(btnEdit);

        btnAdd.setOnClickListener(v -> {
            if (viewModel.getEditMode() == EditMode.ADD_POINT) {
                viewModel.setEditMode(EditMode.VIEW);
            } else {
                viewModel.setEditMode(EditMode.ADD_POINT);
            }
            applyEditModeUi();
        });

        btnEdit.setOnClickListener(v -> {
            if (viewModel.getEditMode() == EditMode.EDIT_POINT) {
                viewModel.setEditMode(EditMode.VIEW);
            } else {
                viewModel.setEditMode(EditMode.EDIT_POINT);
            }
            applyEditModeUi();
        });

        planView.setSearchStateListener(new PlanView.SearchStateListener() {
            @Override
            public void onSearchApplied() {
                btnSearch.setImageResource(R.drawable.btn_find_active);
                viewModel.setSearchResults(planView.getCurrentSearchResults());
            }

            @Override
            public void onSearchCleared() {
                btnSearch.setImageResource(R.drawable.btn_find);
                // сбрасываем аргументы полностью
                viewModel.clearSearchResults();
                if (getArguments() != null) {
                    getArguments().remove("search_points");
                }
            }
        });

        btnSearch.setOnClickListener(v -> {
            if (planView.isSearchActive()) {
                planView.clearSearch();
            } else {
                openSearchDialog();
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
        applyEditModeUi();
        if (viewModel.hasSearchResults()) {
            planView.setSearchResults(viewModel.getSearchResults());
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

        getParentFragmentManager().setFragmentResultListener(
                SearchDialogFragment.REQUEST_KEY,
                getViewLifecycleOwner(),
                (requestKey, result) -> {
                    Set<PlantPoint> points =
                            (Set<PlantPoint>) result.getSerializable(SearchDialogFragment.RESULT_KEY);
                    if (points != null) {
                        planView.setSearchResults(points);
                    }
                }
        );

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

    private void applyEditModeUi() {
        EditMode mode = viewModel.getEditMode();
        planView.setEditMode(mode);

        btnAdd.setImageResource(mode == EditMode.ADD_POINT
                ? R.drawable.btn_add_point_active
                : R.drawable.btn_add_point);

        btnEdit.setImageResource(mode == EditMode.EDIT_POINT
                ? R.drawable.btn_edit_point_active
                : R.drawable.btn_edit_point);
    }

    private void openSearchDialog() {
        SearchDialogFragment.newInstance(planView.getAllPointsSnapshot())
                .show(getParentFragmentManager(), SearchDialogFragment.TAG);
    }

    private void showHelp() {
        HelpDialogFragment.newInstance(R.string.help_plan)
                .show(getParentFragmentManager(), HelpDialogFragment.TAG);
    }
}
