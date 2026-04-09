package com.example.plantmap.db;

import android.app.AlertDialog;
import android.content.Context;
import android.text.InputType;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.plantmap.R;
import com.example.plantmap.colors.ColorResolver;
import com.example.plantmap.plant.PlantAdapter;
import com.example.plantmap.model.Plant;
import com.example.plantmap.plant.PlantRepository;
import com.example.plantmap.util.InputValidators;
import com.example.plantmap.plan.PlanView;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class DbView {
    private Context context;
    private PlanView planView;
    private RecyclerView recyclerView;

    public interface SearchStateListener {
        void onSearchApplied();
        void onSearchCleared();
    }
    private SearchStateListener searchListener;

    private PlantRepository repository;
    private ColorResolver colorResolver;

    public DbView(Context context,
                  PlanView planView,
                  PlantRepository repository) {
        this.context = context;
        this.planView = planView;
        this.repository = repository;

        ColorDataAccess colorDa = repository.getColorDataAccess();
        colorResolver = new ColorResolver(colorDa);
    }

    public View createDbView() {
        LinearLayout rootLayout = new LinearLayout(context);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        Button addButton = new Button(context);
        addButton.setText("Добавить новое растение");
        addButton.setBackground(ContextCompat.getDrawable(context, R.drawable.btn_add_plant));
        addButton.setTextColor(ContextCompat.getColorStateList(context, R.color.btn_add_plant_txt));
        addButton.setPadding(20, 20, 20, 20);
        recyclerView = new RecyclerView(context);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
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
    private void refreshPlantList(RecyclerView recyclerView) {
        List<Plant> plants = repository.getAllPlants();
        Collections.sort(plants, Comparator.comparing(p -> p.name));
        PlantAdapter adapter = new PlantAdapter(
                context,
                plants,
                colorResolver,
                plant -> showPlantDialog(plant, recyclerView));
        recyclerView.setAdapter(adapter);
        if (searchListener != null) {
            searchListener.onSearchCleared();
        }
    }
    private void showPlantDialog(Plant plant, RecyclerView recyclerView) {
        boolean isNew = (plant == null);
        if (isNew) plant = new Plant();
        final Plant plantFinal = plant;
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);

        EditText nameInput = new EditText(context);
        nameInput.setHint("Название сорта");
        nameInput.setText(plant.name != null ? plant.name : "");
        nameInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);

        EditText typeInput = new EditText(context);
        typeInput.setHint("Тип растения");
        typeInput.setText(plant.type != null ? plant.type : "");

        EditText groupInput = new EditText(context);
        groupInput.setHint("Группа растения");
        groupInput.setText(plant.group != null ? plant.group : "");

        EditText potVolumeInput = new EditText(context);
        potVolumeInput.setHint("Литраж горшка");
        potVolumeInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        potVolumeInput.setText(plant.potVolume != null ? String.valueOf(plant.potVolume) : "");
        AutoCompleteTextView flowerColorInput = new AutoCompleteTextView(context);
        flowerColorInput.setHint("Цвет цветка");
        flowerColorInput.setText(plant.flowerColor != null ? plant.flowerColor : "");
        EditText additionalInfoInput = new EditText(context);
        additionalInfoInput.setHint("Дополнительная информация");
        additionalInfoInput.setText(plant.additionalInfo != null ? plant.additionalInfo : "");
        List<String> colorNames = repository.getAllColorNames();

        ArrayAdapter<String> colorAdapter = new ArrayAdapter<>(
                context,
                android.R.layout.simple_dropdown_item_1line,
                colorNames
        );

        flowerColorInput.setAdapter(colorAdapter);
        flowerColorInput.setThreshold(1);
        nameInput.setImeOptions(EditorInfo.IME_ACTION_NEXT );
        typeInput.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        groupInput.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        potVolumeInput.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        flowerColorInput.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        additionalInfoInput.setImeOptions(EditorInfo.IME_ACTION_DONE);

        nameInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                typeInput.requestFocus();
                return true;
            }
            return false;
        });
        typeInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                groupInput.requestFocus();
                return true;
            }
            return false;
        });
        groupInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                potVolumeInput.requestFocus();
                return true;
            }
            return false;
        });
        potVolumeInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                flowerColorInput.requestFocus();
                return true;
            }
            return false;
        });
        flowerColorInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                additionalInfoInput.requestFocus();
                return true;
            }
            return false;
        });
        additionalInfoInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                hideKeyboardAndClearFocus(v);
                return true;
            }
            return false;
        });

        layout.addView(nameInput);
        layout.addView(typeInput);
        layout.addView(groupInput);
        layout.addView(potVolumeInput);
        layout.addView(flowerColorInput);
        layout.addView(additionalInfoInput);

        ScrollView scrollView = new ScrollView(context);
        scrollView.setFillViewport(true);
        scrollView.addView(layout);

        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(isNew ? "Новое растение" : "Редактировать растение")
                .setView(scrollView)
                .setNegativeButton("Отмена", null);

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
        builder.setPositiveButton("Сохранить", null);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
                            | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            );
        }
        focusAndShowKeyboard(nameInput);
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = nameInput.getText().toString().trim();
            if (name.isEmpty()) {
                nameInput.setError("Название обязательно");
                nameInput.requestFocus();
                return;
            }
            Integer potVolume = InputValidators.validatePositiveOptionalInt(potVolumeInput);

            if (potVolume == null && !potVolumeInput.getText().toString().trim().isEmpty()) {
                potVolumeInput.requestFocus();
                return;
            }

            plantFinal.name = name;
            plantFinal.type = typeInput.getText().toString().trim();
            plantFinal.group = groupInput.getText().toString().trim();
            plantFinal.potVolume = potVolume;
            plantFinal.flowerColor = flowerColorInput.getText().toString().trim();
            plantFinal.additionalInfo = additionalInfoInput.getText().toString().trim();

            if (isNew) {
                repository.addPlant(plantFinal);
            } else {
                repository.updatePlant(plantFinal);
            }
            if (planView != null) planView.reloadPoints();

            refreshPlantList(recyclerView);

            dialog.dismiss();
        });
    }
    private void focusAndShowKeyboard(View view) {
        view.requestFocus();
        view.requestFocusFromTouch();
        view.post(() -> {
            InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
            }
        });
    }
    private void hideKeyboardAndClearFocus(View view) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
        view.clearFocus();
    }
    public void showSearchDialog() {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);

        EditText nameInput = new EditText(context);
        nameInput.setHint("Название сорта");

        EditText typeInput = new EditText(context);
        typeInput.setHint("Тип растения");

        EditText groupInput = new EditText(context);
        groupInput.setHint("Группа растения");

        EditText potVolumeInput = new EditText(context);
        potVolumeInput.setHint("Литраж горшка");
        potVolumeInput.setInputType(InputType.TYPE_CLASS_NUMBER);

        EditText flowerColorInput = new EditText(context);
        flowerColorInput.setHint("Цвет цветка");

        EditText addInput = new EditText(context);
        addInput.setHint("Доп. информация");

        nameInput.setImeOptions(EditorInfo.IME_ACTION_NEXT );
        typeInput.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        groupInput.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        potVolumeInput.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        flowerColorInput.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        addInput.setImeOptions(EditorInfo.IME_ACTION_DONE);

        nameInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                typeInput.requestFocus();
                return true;
            }
            return false;
        });
        typeInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                groupInput.requestFocus();
                return true;
            }
            return false;
        });
        groupInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                potVolumeInput.requestFocus();
                return true;
            }
            return false;
        });
        potVolumeInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                flowerColorInput.requestFocus();
                return true;
            }
            return false;
        });
        flowerColorInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                addInput.requestFocus();
                return true;
            }
            return false;
        });
        addInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                hideKeyboardAndClearFocus(v);
                return true;
            }
            return false;
        });

        layout.addView(nameInput);
        layout.addView(typeInput);
        layout.addView(groupInput);
        layout.addView(potVolumeInput);
        layout.addView(flowerColorInput);
        layout.addView(addInput);

        AlertDialog searchDialog = new AlertDialog.Builder(context)
                .setTitle("Поиск растений")
                .setView(layout)
                .setPositiveButton("Найти", (d, w) -> {

                    Integer potVolume = potVolumeInput.getText().toString().isEmpty()
                            ? null
                            : Integer.parseInt(potVolumeInput.getText().toString());

                    List<Plant> result = repository.searchPlants(
                            nameInput.getText().toString().trim(),
                            typeInput.getText().toString().trim(),
                            groupInput.getText().toString().trim(),
                            potVolume,
                            flowerColorInput.getText().toString().trim(),
                            addInput.getText().toString().trim()
                    );
                    if (searchListener != null) {
                        searchListener.onSearchApplied();
                    }
                    PlantAdapter adapter = new PlantAdapter(
                            context,
                            result,
                            colorResolver,
                            plant -> showPlantDialog(plant, recyclerView)
                    );

                    recyclerView.setAdapter(adapter);
                })
                .setNegativeButton("Отменить", (d, w) -> refreshPlantList(recyclerView))
                .create();

        if (searchDialog.getWindow() != null) {
            searchDialog.getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
                            | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            );
        }
        focusAndShowKeyboard(nameInput);

        searchDialog.show();
    }

    public void setSearchStateListener(SearchStateListener listener) {
        this.searchListener = listener;
    }

    public void resetSearch() {
        refreshPlantList(recyclerView);
        if (searchListener != null) {
            searchListener.onSearchCleared();
        }
    }
}