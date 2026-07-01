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

/**
 * Фрагмент с планом территории (картой с растениями).
 * Поддерживает режимы добавления/редактирования точек, поиск растений
 * и передачу результатов поиска с других экранов.
 */
public class PlanFragment extends BaseFragment {

    @Override
    protected int getHelpTextResId() {
        return R.string.help_plan;
    }

    private PlanView planView;
    private boolean addActive = false;
    private boolean editActive = false;
    /** Ожидающие результаты поиска (если PlanView ещё не создан). */
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

        // Контейнер для кнопок управления (поиск, добавить, редактировать)
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

        // Переключение режима добавления
        btnAdd.setOnClickListener(v -> {
            addActive = toggleEditMode(
                    EditMode.ADD_POINT,
                    addActive,
                    R.drawable.btn_add_point_active,
                    R.drawable.btn_add_point,
                    btnAdd
            );
        });

        // Переключение режима редактирования
        btnEdit.setOnClickListener(v -> {
            editActive = toggleEditMode(
                    EditMode.EDIT_POINT,
                    editActive,
                    R.drawable.btn_edit_point_active,
                    R.drawable.btn_edit_point,
                    btnEdit
            );
        });

        // Слушатель состояния поиска для смены иконки
        planView.setSearchStateListener(new PlanView.SearchStateListener() {
            @Override
            public void onSearchApplied() {
                btnSearch.setImageResource(R.drawable.btn_find_active);
            }

            @Override
            public void onSearchCleared() {
                btnSearch.setImageResource(R.drawable.btn_find);
                // Полностью сбрасываем аргументы, чтобы поиск не восстанавливался
                if (getArguments() != null) {
                    getArguments().remove("search_points");
                }
            }
        });

        // Кнопка поиска: включает/выключает режим поиска на карте
        btnSearch.setOnClickListener(v -> {
            if (planView.isSearchActive()) {
                planView.clearSearch();
            } else {
                planView.showSearchDialog();
            }
        });

        root.addView(btnCont, contParams);

        // Применяем ожидающие результаты поиска, если они были заданы до создания view
        if (pendingSearchResults != null) {
            planView.setSearchResults(pendingSearchResults);
            pendingSearchResults = null;
        }

        // Если фрагмент запущен с аргументами поиска, сразу показываем результаты
        if (getArguments() != null) {
            Set<PlantPoint> points =
                    (Set<PlantPoint>) getArguments().getSerializable("search_points");
            if (points != null) {
                planView.setSearchResults(points);
            }
        }

        return root;
    }

    /**
     * Переключает указанный режим редактирования (добавление или редактирование точек).
     * При активации одного режима автоматически деактивирует противоположный.
     *
     * @param mode         режим (ADD_POINT или EDIT_POINT)
     * @param isActive     текущее состояние активности этого режима
     * @param activeIcon   ресурс иконки для активного состояния
     * @param inactiveIcon ресурс иконки для неактивного состояния
     * @param button       кнопка, на которой меняется иконка
     * @return новое состояние активности режима (true — включен, false — выключен)
     */
    private boolean toggleEditMode(EditMode mode, boolean isActive,
                                   int activeIcon, int inactiveIcon,
                                   ImageButton button) {
        if (isActive) {
            // Выключаем режим — возвращаемся в режим просмотра
            planView.setEditMode(EditMode.VIEW);
            button.setImageResource(inactiveIcon);
            return false;
        } else {
            // Включаем режим
            planView.setEditMode(mode);
            button.setImageResource(activeIcon);
            // Автоматически выключаем противоположный режим, если он был активен
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

    /**
     * Устанавливает результаты поиска для отображения на плане.
     * Может быть вызван до того, как PlanView создан — тогда результаты
     * будут применены позже при создании view.
     */
    public void setSearchResults(Set<PlantPoint> points) {
        if (planView != null) {
            planView.setSearchResults(points);
        } else {
            pendingSearchResults = points;
        }
    }
}