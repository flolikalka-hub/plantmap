package com.example.plantmap.ui;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

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

        return root;
    }

    public void setSearchResults(Set<PlantPoint> points) {
        if (planView != null) {
            planView.setSearchResults(points);
        } else {
            pendingSearchResults = points;
        }
    }
}
