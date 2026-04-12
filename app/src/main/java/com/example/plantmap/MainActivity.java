package com.example.plantmap;

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
import com.example.plantmap.model.PlantPoint;
import com.example.plantmap.plant.PlantRepository;
import com.example.plantmap.ui.ColorsFragment;
import com.example.plantmap.ui.DbFragment;
import com.example.plantmap.ui.StatsFragment;
import com.example.plantmap.ui.PlanFragment;
import com.google.android.material.navigation.NavigationView;

import java.util.HashSet;
import java.util.Set;
/**
 Главный экран, запускаемый первым
 */
public class MainActivity extends AppCompatActivity {
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;
    private PlantRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        repository = App.getInstance().getRepository();

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);/* запрет
        на автоматическое расставление отступов, далее настроено вручную */

        setContentView(R.layout.activity_main);

        toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("План территории");
        setSupportActionBar(toolbar); // toolbar - главная панель действий

        drawerLayout = findViewById(R.id.drawer_layout); // боковое меню
        navigationView = findViewById(R.id.nav_view); // список пунктов меню
        FrameLayout contentContainer = findViewById(R.id.main_container); // коробка для контента (фрагментов)

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

        // если создается впервые
        if (savedInstanceState == null) {
            showScreen(R.id.nav_plan);
        }

        // отступы тулбар (гамбургер)
        applySystemBarInsets(toolbar,true,false);
        // отступы навигациннное меню
        applySystemBarInsets(navigationView,true,true);
        // отступы основной план с кнопками
        applySystemBarInsets(contentContainer,false,true);

        //                          МЕНЮ
        navigationView.setNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();

            // не открывает никаких экранов, просто скачивает, поэтому живет отдельно
            if (itemId == R.id.download_db) {
                BackupDatabase backup = new BackupDatabase(this);
                backup.exportDatabase();

                drawerLayout.closeDrawers();
                return true;
            }

            showScreen(itemId);
            drawerLayout.closeDrawers();
            return true;
        });
    }

    /**
     Системные отступы
     */
    public static void applySystemBarInsets(View view, boolean applyTop, boolean applyBottom) {
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

    public PlantRepository getRepository() {
        return repository;
    }

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

    private void showScreen(int menuItemId) {
        Fragment fragment;
        String tag;

        if (menuItemId == R.id.nav_plan) {
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
        } else if (menuItemId == R.id.nav_colors) {
            fragment = new ColorsFragment();
            tag = "colors_fragment";
            toolbar.setTitle("Цвета");
        } else if (menuItemId == R.id.nav_stats) {
            fragment = new StatsFragment();
            tag = "stats_fragment";
            toolbar.setTitle("Статистика");
        } else {
            return;
        }
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.main_container, fragment, tag)
                .commit();
    }
}