package com.example.plantmap;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.example.plantmap.db.BackupDatabase;
import com.example.plantmap.db.yandex.UpdateDatabase;
import com.example.plantmap.model.PlantPoint;
import com.example.plantmap.plant.PlantRepository;
import com.example.plantmap.ui.DbFragment;
import com.example.plantmap.ui.StatsFragment;
import com.example.plantmap.ui.PlanFragment;
import com.google.android.material.navigation.NavigationView;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Главная (и единственная) Activity приложения.
 * Управляет навигацией через боковое меню (DrawerLayout) и переключением фрагментов:
 * - План территории (PlanFragment)
 * - База растений (DbFragment)
 * - Статистика (StatsFragment)
 *
 * Также содержит функции экспорта/импорта базы данных через пункты меню
 * "Скачать" и "Обновить растения".
 * Выполняет однократную миграцию координат растений при первом запуске после обновления.
 */
public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;
    private PlantRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // --- Инициализация данных ---
        repository = App.getInstance().getRepository();
        // Однократная миграция координат из-за смены плотности экрана целевого устройства
        migratePointsIfNeeded();

        // --- Настройка отображения ---
        // Отключаем автоматические отступы под системные бары, будем управлять вручную
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_main);

        // Привязываем UI-компоненты
        toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("План территории");
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        FrameLayout contentContainer = findViewById(R.id.main_container);

        // --- Боковое меню (Drawer + Toolbar) ---
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this,
                drawerLayout,
                toolbar,
                R.string.drawer_open,
                R.string.drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // По умолчанию выделен пункт "План территории"
        navigationView.setCheckedItem(R.id.nav_plan);

        // Если активность создаётся впервые (не пересоздаётся после поворота)
        if (savedInstanceState == null) {
            showScreen(R.id.nav_plan);
        }

        // --- Системные отступы (под status bar / navigation bar) ---
        applySystemBarInsets(toolbar, true, false);          // отступ сверху для toolbar
        applySystemBarInsets(navigationView, true, true);    // сверху и снизу для меню
        applySystemBarInsets(contentContainer, false, true); // снизу для основного контейнера

        // --- Обработчик пунктов меню ---
        navigationView.setNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();

            // Экспорт базы данных (не открывает фрагмент, только выполняет действие)
            if (itemId == R.id.download_db) {
                BackupDatabase backup = new BackupDatabase(this);
                backup.exportDatabase();
                drawerLayout.closeDrawers();
                return true;
            }

            // Импорт базы данных из файла на устройстве
            if (itemId == R.id.update_db) {
                UpdateDatabase update = new UpdateDatabase(this);
                update.importDatabase();
                drawerLayout.closeDrawers();
                return true;
            }

            // Обычные экраны приложения
            showScreen(itemId);
            drawerLayout.closeDrawers();
            return true;
        });
    }

    /**
     * Применяет отступы под системные бары (status bar, navigation bar).
     * Используется для корректного позиционирования UI под вырезы и прозрачные панели.
     *
     * @param view       View, к которому применяются отступы
     * @param applyTop   добавлять ли отступ сверху
     * @param applyBottom добавлять ли отступ снизу
     */
    public static void applySystemBarInsets(View view, boolean applyTop, boolean applyBottom) {
        // Сохраняем базовые отступы (могли быть заданы в XML)
        final int baseLeft = view.getPaddingLeft();
        final int baseTop = view.getPaddingTop();
        final int baseRight = view.getPaddingRight();
        final int baseBottom = view.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(
                    baseLeft + bars.left,
                    applyTop ? baseTop + bars.top : baseTop,
                    baseRight + bars.right,
                    applyBottom ? baseBottom + bars.bottom : baseBottom
            );
            return insets;
        });
    }

    /**
     * Возвращает репозиторий растений для передачи во фрагменты.
     */
    public PlantRepository getRepository() {
        return repository;
    }

    /**
     * Открывает экран плана, передавая в него набор найденных точек (результаты поиска).
     * Используется из поискового фрагмента для отображения результатов на карте.
     *
     * @param points множество точек растений для отображения на плане
     */
    public void openPlanWithResults(Set<PlantPoint> points) {
        PlanFragment fragment = new PlanFragment();
        Bundle args = new Bundle();
        args.putSerializable("search_points", new HashSet<>(points));
        fragment.setArguments(args);

        navigationView.setCheckedItem(R.id.nav_plan);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.main_container, fragment, "plan_fragment")
                .commit();
    }

    /**
     * Переключает отображаемый фрагмент в соответствии с выбранным пунктом меню.
     *
     * @param menuItemId идентификатор пункта меню (R.id.nav_plan, R.id.nav_db, R.id.nav_stats)
     */
    private void showScreen(int menuItemId) {
        Fragment fragment;
        String tag;

        if (menuItemId == R.id.nav_plan) {
            // Пытаемся найти существующий фрагмент с тегом (чтобы не терять состояние)
            fragment = getSupportFragmentManager().findFragmentByTag("plan_fragment");
            if (fragment == null) {
                fragment = new PlanFragment();
            }
            tag = "plan_fragment";
            toolbar.setTitle("План территории");
        } else if (menuItemId == R.id.nav_db) {
            fragment = new DbFragment();
            tag = "db_fragment";
            toolbar.setTitle("Растения");
        } else if (menuItemId == R.id.nav_stats) {
            fragment = new StatsFragment();
            tag = "stats_fragment";
            toolbar.setTitle("Статистика");
        } else {
            return; // неизвестный пункт меню
        }

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.main_container, fragment, tag)
                .commit();
    }

    /**
     * Однократно запускает миграцию координат всех растений.
     *
     * Необходимость возникла из-за смены целевого устройства: старые координаты
     * были рассчитаны для экрана с плотностью 2.8125 (A16),
     * новые должны быть в эталонных пикселях (density 1.0).
     * Применяется масштабирующий коэффициент 1 / 2.8125.
     *
     * После выполнения миграции в SharedPreferences записывается флаг,
     * чтобы операция не повторялась.
     */
    private void migratePointsIfNeeded() {
        SharedPreferences prefs = getSharedPreferences("migration", MODE_PRIVATE);
        if (prefs.getBoolean("density_migrated", false)) return;

        float oldDensity = 2.8125f;   // плотность экрана старого устройства
        float scale = 1f / oldDensity; // коэффициент для приведения к эталону

        List<PlantPoint> allPoints = repository.getAllPoints();
        for (PlantPoint p : allPoints) {
            p.setX(p.x * scale);
            p.setY(p.y * scale);
            repository.updatePoint(p.id, p);
        }

        // Отмечаем, что миграция выполнена
        prefs.edit().putBoolean("density_migrated", true).apply();
    }
}