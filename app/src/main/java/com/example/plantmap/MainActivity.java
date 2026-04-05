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
import androidx.lifecycle.ViewModelProvider;

import com.example.plantmap.db.BackupDatabase;
import com.example.plantmap.db.DatabaseHelper;
import com.example.plantmap.model.PlantPoint;
import com.example.plantmap.plant.PlantRepository;
import com.example.plantmap.fragments.ColorsFragment;
import com.example.plantmap.fragments.DbFragment;
import com.example.plantmap.fragments.StatsFragment;
import com.example.plantmap.fragments.PlanFragment;
import com.example.plantmap.viewmodel.MainViewModel;
import com.google.android.material.navigation.NavigationView;

import java.util.HashSet;
import java.util.Set;


public class MainActivity extends AppCompatActivity {
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;
    private DatabaseHelper dbHelper;
    private PlantRepository repository;
    private MainViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        dbHelper = new DatabaseHelper(this);
        repository = new PlantRepository(dbHelper);
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        setContentView(R.layout.activity_main);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        FrameLayout contentContainer = findViewById(R.id.main_container);

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
        viewModel.getSelectedScreen().observe(this, menuItemId -> {
            if (menuItemId == null) {
                return;
            }
            navigationView.setCheckedItem(menuItemId);
            showScreen(menuItemId);
        });

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
                BackupDatabase backup = new BackupDatabase(this, dbHelper);
                backup.exportDatabase();

                drawerLayout.closeDrawers();
                return true;
            }

            viewModel.selectScreen(itemId);
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

        viewModel.selectScreen(R.id.nav_plan);

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
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.main_container);
        if (currentFragment != null && tag.equals(currentFragment.getTag())) {
            return;
        }
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.main_container, fragment, tag)
                .commit();
    }
}