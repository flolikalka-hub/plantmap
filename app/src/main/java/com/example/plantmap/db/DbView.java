package com.example.plantmap.db;

import android.app.AlertDialog;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.plantmap.R;
import com.example.plantmap.model.FlowerColor;
import com.example.plantmap.model.Plant;
import com.example.plantmap.plant.PlantAdapter;
import com.example.plantmap.plant.PlantRepository;
import com.example.plantmap.plant.PlantUniversalForm;
import com.example.plantmap.util.ImeActionUtil;
import com.example.plantmap.util.SoftInputUtil;
import com.example.plantmap.plan.PlanView;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Экран со списком растений в виде карточек.
 * Используется во фрагменте DbFragment.
 *
 * Основные элементы:
 * - recyclerView — прокручиваемый список растений
 * - planView — ссылка на карту (для обновления после изменений)
 * - searchListener — слушатель событий поиска (начало/конец)
 * - repository — склад данных о растениях
 */
public class DbView {
    private Context context;
    private PlanView planView;
    private RecyclerView recyclerView;

    /**
     * Слушатель для уведомления о начале и окончании поиска.
     */
    public interface SearchStateListener {
        void onSearchApplied();
        void onSearchCleared();
    }
    private SearchStateListener searchListener;

    private PlantRepository repository;

    public DbView(Context context,
                  PlanView planView,
                  PlantRepository repository) {
        this.context = context;
        this.planView = planView;
        this.repository = repository;
    }

    /**
     * Создаёт и возвращает корневое View для списка растений.
     * Содержит кнопку "Добавить новое растение" и RecyclerView.
     */
    public View createDbView() {
        LinearLayout rootLayout = new LinearLayout(context);
        rootLayout.setOrientation(LinearLayout.VERTICAL);

        // Кнопка добавления нового растения
        Button addButton = new Button(context);
        addButton.setText("Добавить новое растение");
        addButton.setBackground(ContextCompat.getDrawable(context, R.drawable.btn_add_plant));
        addButton.setTextColor(ContextCompat.getColorStateList(context, R.color.btn_add_plant_txt));
        addButton.setPadding(20, 20, 20, 20);

        // Список растений
        recyclerView = new RecyclerView(context);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));

        // Растягиваем RecyclerView на всё свободное место
        LinearLayout.LayoutParams recyclerParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1f
                );
        recyclerView.setLayoutParams(recyclerParams);

        addButton.setOnClickListener(v -> showPlantDialog(null, recyclerView));

        rootLayout.addView(recyclerView);
        rootLayout.addView(addButton);

        refreshPlantList(recyclerView);

        return rootLayout;
    }

    /**
     * Обновляет список растений в RecyclerView.
     * Сортирует по имени, создаёт адаптер с привязкой цветов.
     * Сбрасывает состояние поиска, уведомляя слушатель.
     */
    private void refreshPlantList(RecyclerView recyclerView) {
        List<Plant> plants = repository.getAllPlants();
        Collections.sort(plants, Comparator.comparing(p -> p.name));
        PlantAdapter adapter = new PlantAdapter(
                context,
                plants,
                plant -> showPlantDialog(plant, recyclerView));
        adapter.setColorMaps(repository.getColorIdToNameMap(), repository.getColorIdToHexMap());
        recyclerView.setAdapter(adapter);
        if (searchListener != null) {
            searchListener.onSearchCleared();
        }
    }

    /**
     * Обновляет список растений извне (например, при возвращении на фрагмент).
     */
    public void refresh() {
        if (recyclerView != null) {
            refreshPlantList(recyclerView);
        }
    }

    /**
     * Показывает диалог добавления или редактирования растения.
     *
     * @param plant         существующее растение для редактирования или null для создания нового
     * @param recyclerView  RecyclerView, который нужно обновить после сохранения
     */
    private void showPlantDialog(Plant plant, RecyclerView recyclerView) {
        boolean isNew = (plant == null);
        if (isNew) plant = new Plant();
        final Plant originalPlant = plant;

        PlantUniversalForm form = new PlantUniversalForm(context, repository);
        form.fillFromPlant(plant);

        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(isNew ? "Новое растение" : "Редактировать растение")
                .setView(form.getView())
                .setNegativeButton("Отмена", null)
                .setPositiveButton("Сохранить", null);

        // Кнопка "Удалить" только для существующих растений
        if (!isNew) {
            final int idPlant = plant.id;
            builder.setNeutralButton("Удалить", (d, which) -> {
                if (!repository.canDeletePlant(idPlant)) {
                    new AlertDialog.Builder(context)
                            .setTitle("Растение используется в точках")
                            .setMessage("Сначала удалите точки с этим растением")
                            .setNegativeButton("Понятно", null)
                            .show();
                } else {
                    new AlertDialog.Builder(context)
                            .setTitle("Удалить растение")
                            .setMessage("Растение будет удалено. Вы уверены?")
                            .setPositiveButton("Удалить", (dd, w) -> {
                                repository.deletePlant(idPlant);
                                if (planView != null) planView.reloadPoints();
                                refreshPlantList(recyclerView);
                            })
                            .setNegativeButton("Отменить", null)
                            .show();
                }
            });
        }

        AlertDialog dialog = builder.create();
        SoftInputUtil.setupSoftInput(dialog);

        dialog.setOnShowListener(d -> {
            ImeActionUtil.focusAndShowKeyboard(form.getNameInput());
            Button saveBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            saveBtn.setOnClickListener(v -> {
                // Валидация названия
                String name = form.getNameInput().getText().toString().trim();
                if (name.isEmpty()) {
                    form.getNameInput().setError("Название обязательно");
                    form.getNameInput().requestFocus();
                    return;
                }

                // Получаем список введённых объёмов из формы
                List<Integer> potVolumes = form.getPotVolumes();
                // Строим объект Plant на основе введённых данных
                Plant updatedPlant = form.buildPlantFromInputs();

                // Проверка на существование такого же растения
                Plant existingPlant = repository.findPlantByAllFields(updatedPlant);
                Plant plantToSave;
                if (existingPlant != null) {
                    // Если редактируем существующее и найденное — это оно само, разрешаем
                    if (!isNew && existingPlant.id == originalPlant.id) {
                        plantToSave = updatedPlant;
                        plantToSave.id = originalPlant.id;
                    } else {
                        // Дубликат: для нового или чужого при редактировании
                        new AlertDialog.Builder(context)
                                .setTitle("Растение уже существует")
                                .setMessage("Такое растение уже есть в базе. Измените данные или используйте существующее.")
                                .setPositiveButton("ОК", null)
                                .show();
                        return; // прерываем сохранение, диалог остаётся открытым
                    }
                } else {
                    plantToSave = updatedPlant;
                    if (!isNew) {
                        plantToSave.id = originalPlant.id;
                    }
                }

                long savedPlantId;
                if (isNew) {
                    // Если растение уже существует, не добавляем, а используем его id
                    if (existingPlant != null) {
                        savedPlantId = existingPlant.id;
                    } else {
                        savedPlantId = repository.addPlant(plantToSave);
                    }
                } else {
                    repository.updatePlant(plantToSave);
                    savedPlantId = originalPlant.id;
                }

                // Проверяем, какие объёмы удаляются (нельзя удалить используемые в точках)
                List<Integer> oldVolumes = repository.getPotVolumesForPlant((int) savedPlantId);
                for (Integer oldVol : oldVolumes) {
                    if (!potVolumes.contains(oldVol)) {
                        if (!repository.canDeleteVolume((int) savedPlantId, oldVol)) {
                            new AlertDialog.Builder(context)
                                    .setTitle("Нельзя удалить объём")
                                    .setMessage("Объём " + oldVol + "л используется в точках на плане. Сначала удалите эти точки.")
                                    .setPositiveButton("ОК", null)
                                    .show();
                            return; // прерываем сохранение
                        }
                    }
                }

                // Если проверки пройдены — заменяем объёмы и обновляем UI
                repository.replacePlantVolumes((int) savedPlantId, potVolumes);

                if (planView != null) planView.reloadPoints();
                refreshPlantList(recyclerView);
                dialog.dismiss();
            });
        });
        dialog.show();
    }

    /**
     * Показывает диалог поиска растений по различным полям.
     * Использует форму PlantUniversalForm в режиме MODE_SEARCH.
     */
    public void showSearchDialog() {
        PlantUniversalForm form = new PlantUniversalForm(context, repository);
        form.setMode(PlantUniversalForm.MODE_SEARCH);
        form.fillFromPlant(new Plant());

        // Сбрасываем предустановленный цвет "неизвестный", чтобы поиск был по всем цветам
        form.getFlowerColorInput().setText("");
        form.setShowAllColorsOption(true);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("Поиск растений")
                .setView(form.getView())
                .setPositiveButton("Найти", null)
                .setNegativeButton("Отменить", (d, w) -> refreshPlantList(recyclerView))
                .create();

        dialog.setOnShowListener(d -> {
            ImeActionUtil.focusAndShowKeyboard(form.getNameInput());

            Button findBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            findBtn.setOnClickListener(v -> {
                // Получаем данные из формы
                String name = form.getNameInput().getText().toString().trim();
                String type = form.getTypeInput().getText().toString().trim();
                String group = form.getGroupInput().getText().toString().trim();

                Integer flowerColorId = null;
                String flowerColorStr = form.getFlowerColorInput().getText().toString().trim();
                if (!flowerColorStr.isEmpty() && !"любой".equals(flowerColorStr)) {
                    Map<String, Integer> nameToId = new HashMap<>();
                    for (FlowerColor c : repository.getAllColors()) {
                        nameToId.put(c.getName(), c.getId());
                    }
                    flowerColorId = nameToId.get(flowerColorStr);
                }

                String addInfo = form.getAdditionalInfoInput().getText().toString().trim();

                Integer potVolume = null;
                String potVolumeStr = form.getPotVolumeInput().getText().toString().trim();
                if (!potVolumeStr.isEmpty()) {
                    try {
                        potVolume = Integer.parseInt(potVolumeStr);
                    } catch (NumberFormatException ignored) {}
                }

                // Выполняем поиск
                List<Plant> result = repository.searchPlants(
                        name,
                        type,
                        group,
                        potVolume,
                        flowerColorId,
                        addInfo
                );

                if (searchListener != null) {
                    searchListener.onSearchApplied();
                }

                PlantAdapter adapter = new PlantAdapter(
                        context,
                        result,
                        plant -> showPlantDialog(plant, recyclerView)
                );
                adapter.setColorMaps(repository.getColorIdToNameMap(), repository.getColorIdToHexMap());
                recyclerView.setAdapter(adapter);

                dialog.dismiss();
            });
        });

        SoftInputUtil.setupSoftInput(dialog);
        dialog.show();
    }

    /**
     * Устанавливает слушатель событий поиска.
     */
    public void setSearchStateListener(SearchStateListener listener) {
        this.searchListener = listener;
    }

    /**
     * Сбрасывает поиск и показывает полный список растений.
     */
    public void resetSearch() {
        refreshPlantList(recyclerView);
        if (searchListener != null) {
            searchListener.onSearchCleared();
        }
    }
}