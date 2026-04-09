package com.example.plantmap;

import android.os.Bundle;
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


public class MainActivity extends AppCompatActivity {
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;
    private FrameLayout contentContainer;
    private PlantRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        repository = App.getInstance().getRepository();

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        setContentView(R.layout.activity_main);

        toolbar = findViewById(R.id.toolbar);
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

        if (savedInstanceState == null)
        {
            showScreen(R.id.nav_plan);
        }

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
                    content.bottom
            );
            return insets;
        });

        //                          МЕНЮ
        navigationView.setNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.download_db) {
                //BackupDatabase backup = new BackupDatabase(this, App.getInstance().getDbHelper());
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