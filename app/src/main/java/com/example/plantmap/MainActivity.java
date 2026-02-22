package com.example.plantmap;

import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.plantmap.db.DatabaseHelper;
import com.example.plantmap.colors.ColorView;
import com.example.plantmap.db.DbView;
import com.example.plantmap.plant.PlantRepository;
import com.example.plantmap.search.PlantSearchEngine;
import com.example.plantmap.stats.StatisticsView;
import com.example.plantmap.view.EditMode;
import com.example.plantmap.view.PlanView;
import com.google.android.material.navigation.NavigationView;

public class MainActivity extends AppCompatActivity {

    // флаги для отслеживания текущего состояния
    final boolean[] addActive = {false};
    final boolean[] editActive = {false};
    // разнообразные контейнеры
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private FrameLayout contentContainer;
    private PlanView planView;
    private DbView dbView;
    // экраны для appbar
    private enum Screen {
        PLAN,
        DB,
        COLORS,
        STATS
    }
    // текущий отображаемый
    private Screen currentScreen = Screen.PLAN;
    // для сброса фильтра
    private boolean dbSearchActive = false;
    private DatabaseHelper dbHelper;
    private PlantRepository repository;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        dbHelper = new DatabaseHelper(this);
        repository = new PlantRepository(dbHelper);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        setContentView(R.layout.activity_main);

        // визуализируем наличие меню
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("План территории");
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        contentContainer = findViewById(R.id.main_container);

        // связываем боковое меню с гамбургером
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this,
                drawerLayout,
                toolbar,
                R.string.drawer_open,
                R.string.drawer_close
        );

        // синхронизируем состояние
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // по умолчанию подсветка плана как выбранного
        navigationView.setCheckedItem(R.id.nav_plan);

        //                          ЭКРАН ПЛАН
        planView = new PlanView(this, repository);
        contentContainer.addView(
                planView,
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                )
        );

        // контейнер с кнопками
        LinearLayout btnCont = new LinearLayout(this);
        FrameLayout.LayoutParams contParams =
                new FrameLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
        contParams.gravity = Gravity.BOTTOM | Gravity.END;

        // поиск
        ImageButton btnSearch = new ImageButton(this);
        btnSearch.setImageResource(R.drawable.btn_find);
        btnSearch.setBackground(null);
        LinearLayout.LayoutParams searchParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
        searchParams.gravity = Gravity.BOTTOM | Gravity.END;
        btnCont.addView(btnSearch, searchParams);

        // добавление точки
        ImageButton btnAdd = new ImageButton(this);
        btnAdd.setImageResource(R.drawable.btn_add_point);
        btnAdd.setBackground(null);
        LinearLayout.LayoutParams addParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
        addParams.gravity = Gravity.BOTTOM | Gravity.END;
        btnCont.addView(btnAdd, addParams);

        // редактирование существующих
        ImageButton btnEdit = new ImageButton(this);
        btnEdit.setImageResource(R.drawable.btn_edit_point);
        btnEdit.setBackground(null);
        LinearLayout.LayoutParams editParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
        editParams.gravity = Gravity.BOTTOM | Gravity.END;
        btnCont.addView(btnEdit, editParams);

        // Логика работы переключателей режимов
        // кнопка добавления точки
        btnAdd.setOnClickListener(v -> {
            if (addActive[0]) {
                // если уже активна → сброс в VIEW
                planView.setEditMode(EditMode.VIEW);
                btnAdd.setImageResource(R.drawable.btn_add_point); // исходный ресурс
                addActive[0] = false;
            } else {
                // включаем ADD_POINT
                planView.setEditMode(EditMode.ADD_POINT);
                btnAdd.setImageResource(R.drawable.btn_add_point_active); // активная иконка
                addActive[0] = true;

                // сбрасываем edit
                if (editActive[0]) {
                    btnEdit.setImageResource(R.drawable.btn_edit_point);
                    editActive[0] = false;
                }
            }
        });

        // кнопка редактирования существующих
        btnEdit.setOnClickListener(v -> {
            if (editActive[0]) {
                planView.setEditMode(EditMode.VIEW);
                btnEdit.setImageResource(R.drawable.btn_edit_point);
                editActive[0] = false;
            } else {
                planView.setEditMode(EditMode.EDIT_POINT);
                btnEdit.setImageResource(R.drawable.btn_edit_point_active);
                editActive[0] = true;

                // сбрасываем add
                if (addActive[0]) {
                    btnAdd.setImageResource(R.drawable.btn_add_point);
                    addActive[0] = false;
                }
            }
        });

        // кнопка поиска
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

        contentContainer.addView(btnCont, contParams);

        // отступы тулбар (гамбургер)
        final int toolbarBaseLeft = toolbar.getPaddingLeft();
        final int toolbarBaseTop = toolbar.getPaddingTop();
        final int toolbarBaseRight = toolbar.getPaddingRight();
        final int toolbarBaseBottom = toolbar.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, insets) -> {
            Insets curr = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(
                    toolbarBaseLeft + curr.left,
                    toolbarBaseTop + curr.top,
                    toolbarBaseRight + curr.right,
                    toolbarBaseBottom
            );
            return insets;
        });

        // отступы навигациннное меню
        ViewCompat.setOnApplyWindowInsetsListener(navigationView, (v, insets) -> {
            Insets nav = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(
                    nav.left,
                    nav.top,
                    nav.right,
                    nav.bottom
            );
            return insets;
        });

        // отступы основной план с кнопками
        ViewCompat.setOnApplyWindowInsetsListener(contentContainer, (v, insets) -> {
            Insets content = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(
                    content.left,
                    v.getPaddingTop(),
                    content.right,
                    content.bottom);
            return insets;
        });

        //                          МЕНЮ
        navigationView.setNavigationItemSelectedListener(item -> {

            contentContainer.removeAllViews();

            if (item.getItemId() == R.id.nav_plan) {
                // настраиваем меню
                currentScreen = Screen.PLAN;
                invalidateOptionsMenu();
                // Загружаем основной экран с планом
                contentContainer.addView(planView);
                contentContainer.addView(btnCont);
                toolbar.setTitle("План территории");
            } else if (item.getItemId() == R.id.nav_db) {
                // настраиваем меню
                currentScreen = Screen.DB;
                invalidateOptionsMenu();
                // Сбрасываем режим
                planView.setEditMode(EditMode.VIEW);
                // Сбрасываем визуал
                addActive[0] = false;
                editActive[0] = false;
                btnAdd.setImageResource(R.drawable.btn_add_point);
                btnEdit.setImageResource(R.drawable.btn_edit_point);
                // Загружаем экран с БД
                dbView = new DbView(this, planView, repository);
                dbView.setSearchStateListener(() -> {
                    dbSearchActive = true;
                    invalidateOptionsMenu();
                });
                contentContainer.addView(dbView.createDbView());
                toolbar.setTitle("Растения");
            } else if (item.getItemId() == R.id.nav_colors) {
                // настраиваем меню
                currentScreen = Screen.COLORS;
                invalidateOptionsMenu();
                // Сбрасываем режим
                planView.setEditMode(EditMode.VIEW);
                // Сбрасываем визуал
                addActive[0] = false;
                editActive[0] = false;
                btnAdd.setImageResource(R.drawable.btn_add_point);
                btnEdit.setImageResource(R.drawable.btn_edit_point);
                // загружаем экран с цветами
                ColorView colorView = new ColorView(this, repository.getColorDataAccess());
                contentContainer.addView(colorView.createColorView());
                toolbar.setTitle("Цвета");
            } else if (item.getItemId() == R.id.nav_stats) {
                // настраиваем меню
                currentScreen = Screen.STATS;
                invalidateOptionsMenu();
                // Сбрасываем режим
                planView.setEditMode(EditMode.VIEW);
                // Сбрасываем визуал
                addActive[0] = false;
                editActive[0] = false;
                btnAdd.setImageResource(R.drawable.btn_add_point);
                btnEdit.setImageResource(R.drawable.btn_edit_point);
                // загружаем экран со статистикой
                StatisticsView statisticsView = new StatisticsView(this,
                        repository,
                        points -> {
                            planView.setSearchResults(points);

                            navigationView.setCheckedItem(R.id.nav_plan);
                            navigationView.getMenu().performIdentifierAction(R.id.nav_plan, 0);
                        });
                contentContainer.addView(statisticsView.createView());
                toolbar.setTitle("Статистика");
            }

            drawerLayout.closeDrawers();
            return true;
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (currentScreen == Screen.DB) {
            getMenuInflater().inflate(R.menu.menu_db, menu);
        }
        return true;
    }

    // управление видимостью кнопок
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        if (currentScreen == Screen.DB) {
            getMenuInflater().inflate(R.menu.menu_db, menu);
            MenuItem resetItem = menu.findItem(R.id.action_reset_db);
            if (resetItem != null) {
                resetItem.setVisible(dbSearchActive);
            }
        }
        return super.onPrepareOptionsMenu(menu);
    }

    // обработка нажатий
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_search_db) {
            if (dbView != null) {
                dbView.showSearchDialog();
            }
            return true;
        }
        if (item.getItemId() == R.id.action_reset_db) {
            if (dbView != null) {
                dbView.resetSearch();
            }
            dbSearchActive = false;
            invalidateOptionsMenu();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}