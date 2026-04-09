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

import com.example.plantmap.MainActivity;
import com.example.plantmap.R;
import com.example.plantmap.model.PlantPoint;
import com.example.plantmap.plan.EditMode;
import com.example.plantmap.plan.PlanView;

import java.util.Set;

public class PlanFragment extends BaseFragment {
    @Override
    protected int getHelpTextResId() {
        return R.string.help_plan;
    }

    private PlanView planView;
    private boolean addActive = false;
    private boolean editActive = false;
    private Set<PlantPoint> pendingSearchResults;
    private ImageButton btnAdd, btnEdit;

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
            addActive = toggleEditMode(
                    EditMode.ADD_POINT,
                    addActive,
                    R.drawable.btn_add_point_active,
                    R.drawable.btn_add_point,
                    btnAdd
            );
        });

        btnEdit.setOnClickListener(v -> {
            editActive = toggleEditMode(
                    EditMode.EDIT_POINT,
                    editActive,
                    R.drawable.btn_edit_point_active,
                    R.drawable.btn_edit_point,
                    btnEdit
            );
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

    /**
     Переключает указанный режим редактирования
     mode               режим (ADD_POINT или EDIT_POINT)
     isActive           текущее состояние активности этого режима
     activeIcon         ресурс иконки для активного состояния
     inactiveIcon       ресурс иконки для неактивного состояния
     button             кнопка, на которой меняется иконка
     return             новое состояние активности режима (true — включён, false — выключен)
     */
    private boolean toggleEditMode(EditMode mode, boolean isActive,
                                   int activeIcon, int inactiveIcon,
                                   ImageButton button) {
        if (isActive) {
            planView.setEditMode(EditMode.VIEW);
            button.setImageResource(inactiveIcon);
            return false;
        } else {
            planView.setEditMode(mode);
            button.setImageResource(activeIcon);
            // Деактивируем противоположный режим, если он был активен
            if (mode == EditMode.ADD_POINT && editActive) {
                editActive = false;
                btnEdit.setImageResource(R.drawable.btn_edit_point);
            } else if (mode == EditMode.EDIT_POINT && addActive) {
                addActive = false;
                btnAdd.setImageResource(R.drawable.btn_add_point);
            }
            return true;
        }
    }

    public void setSearchResults(Set<PlantPoint> points) {
        if (planView != null) {
            planView.setSearchResults(points);
        } else {
            pendingSearchResults = points;
        }
    }

}
