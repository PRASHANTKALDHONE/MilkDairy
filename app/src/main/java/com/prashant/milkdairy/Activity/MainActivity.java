package com.prashant.milkdairy.Activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.bumptech.glide.Glide;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.prashant.milkdairy.R;
import com.prashant.milkdairy.databinding.ActivityMainBinding;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;

    private TextView tvDay, tvDate, tvTime;
    private TextView tvHeaderName, tvHeaderDairy;
    private ImageView imageView;

    private final Handler handler = new Handler();

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private ListenerRegistration profileListener;

    private static final String DRAWER_PREVIEW_PREF = "MilkDairyDrawerPreview";
    private static final String KEY_DRAWER_PREVIEW_PREFIX = "drawer_preview_seen_";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applySavedTheme();

        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        setSupportActionBar(binding.appBarMain.toolbar);

        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;

        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home,
                R.id.nav_milkCollection,
                R.id.nav_advance_food,
                R.id.nav_farmer,
                R.id.nav_bills,
                R.id.nav_bonus_management,
                R.id.nav_rate_chart,
                R.id.nav_inventory,
                R.id.nav_reports
        ).setOpenableLayout(drawer).build();

        NavController navController =
                Navigation.findNavController(this, R.id.nav_host_fragment_content_main);

        NavigationUI.setupActionBarWithNavController(
                this, navController, mAppBarConfiguration);

        NavigationUI.setupWithNavController(
                navigationView, navController);

        if (savedInstanceState == null) {
            showDrawerAlreadyOpenOnceAfterLogin(drawer);
        }

        View headerView = navigationView.getHeaderView(0);
        fixSystemBars(headerView);


        imageView = headerView.findViewById(R.id.imageView);
        tvHeaderName = headerView.findViewById(R.id.tvHeaderName);
        tvHeaderDairy = headerView.findViewById(R.id.tvHeaderDairy);

        tvDay = headerView.findViewById(R.id.tvDay);
        tvDate = headerView.findViewById(R.id.tvDate);
        tvTime = headerView.findViewById(R.id.tvTime);

        startDateTimeUpdater();
        listenProfileHeader();
    }
    private void fixSystemBars(View headerView) {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        int primaryColor = getColorFromAttr(com.google.android.material.R.attr.colorPrimary);
        getWindow().setStatusBarColor(primaryColor);
        getWindow().setNavigationBarColor(getColorFromAttr(android.R.attr.colorBackground));

        View appBarLayout = findViewById(R.id.appBarLayout);
        View navHeaderRoot = headerView.findViewById(R.id.navHeaderRoot);

        if (appBarLayout != null) {
            ViewCompat.setOnApplyWindowInsetsListener(appBarLayout, (view, insets) -> {
                int statusTop = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
                int cutoutTop = insets.getInsets(WindowInsetsCompat.Type.displayCutout()).top;
                int topInset = Math.max(statusTop, cutoutTop);

                view.setPadding(
                        view.getPaddingLeft(),
                        topInset,
                        view.getPaddingRight(),
                        view.getPaddingBottom()
                );

                return insets;
            });

            ViewCompat.requestApplyInsets(appBarLayout);
        }

        if (navHeaderRoot != null) {
            ViewCompat.setOnApplyWindowInsetsListener(navHeaderRoot, (view, insets) -> {
                int statusTop = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
                int cutoutTop = insets.getInsets(WindowInsetsCompat.Type.displayCutout()).top;
                int topInset = Math.max(statusTop, cutoutTop);

                view.setPadding(
                        dpToPx(16),
                        topInset + dpToPx(16),
                        dpToPx(16),
                        dpToPx(16)
                );

                return insets;
            });

            ViewCompat.requestApplyInsets(navHeaderRoot);
        }
    }


    private int getColorFromAttr(int attr) {
        android.util.TypedValue typedValue = new android.util.TypedValue();
        getTheme().resolveAttribute(attr, typedValue, true);
        return typedValue.data;
    }


    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }



    private void listenProfileHeader() {
        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            return;
        }

        profileListener = db.collection("users")
                .document(user.getUid())
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null || !snapshot.exists()) {
                        return;
                    }

                    bindHeaderProfile(snapshot);
                });
    }

    private void showDrawerAlreadyOpenOnceAfterLogin(DrawerLayout drawer) {
        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            return;
        }

        SharedPreferences preferences =
                getSharedPreferences(DRAWER_PREVIEW_PREF, MODE_PRIVATE);

        String key = KEY_DRAWER_PREVIEW_PREFIX + user.getUid();

        boolean alreadyShown = preferences.getBoolean(key, false);

        if (alreadyShown) {
            return;
        }

        preferences.edit()
                .putBoolean(key, true)
                .apply();

        drawer.post(() -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }

            drawer.openDrawer(GravityCompat.START, false);

            handler.postDelayed(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }

                if (drawer.isDrawerOpen(GravityCompat.START)) {
                    drawer.closeDrawer(GravityCompat.START);
                }

            }, 1500);
        });
    }

    private void bindHeaderProfile(DocumentSnapshot snapshot) {
        String fullName = firstValue(snapshot, "fullName", "name");
        String dairyName = firstValue(snapshot, "dairyName", "businessName");
        String profileImage = firstValue(snapshot, "profileImage", "profileImageUrl");

        if (fullName.isEmpty()) {
            fullName = "My Dairy";
        }

        if (dairyName.isEmpty()) {
            dairyName = "Milk Dairy";
        }

        tvHeaderName.setText(fullName);
        tvHeaderDairy.setText(dairyName);

        if (!profileImage.isEmpty()) {
            Glide.with(this)
                    .load(profileImage)
                    .placeholder(R.drawable.applogo)
                    .error(R.drawable.applogo)
                    .circleCrop()
                    .into(imageView);
        } else {
            imageView.setImageResource(R.drawable.applogo);
        }
    }

    private String firstValue(DocumentSnapshot snapshot, String... keys) {
        for (String key : keys) {
            String value = snapshot.getString(key);

            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }

        return "";
    }

    private void startDateTimeUpdater() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                Date date = new Date();

                String day =
                        new SimpleDateFormat("EEEE", Locale.getDefault()).format(date);

                String currentDate =
                        new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(date);

                String currentTime =
                        new SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(date);

                if (tvDay != null && tvDate != null && tvTime != null) {
                    tvDay.setText(day);
                    tvDate.setText(currentDate);
                    tvTime.setText(currentTime);
                }

                handler.postDelayed(this, 1000);
            }
        };

        handler.post(runnable);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (profileListener == null) {
            listenProfileHeader();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_profile) {
            startActivity(new Intent(MainActivity.this, ProfileActivity.class));
            return true;
        }

        if (id == R.id.action_settings) {
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController =
                Navigation.findNavController(this, R.id.nav_host_fragment_content_main);

        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        handler.removeCallbacksAndMessages(null);

        if (profileListener != null) {
            profileListener.remove();
            profileListener = null;
        }
    }

    private void applySavedTheme() {
        SharedPreferences preferences = getSharedPreferences("MilkDairySettings", MODE_PRIVATE);
        String theme = preferences.getString("theme_mode", "system");

        if (theme.equals("light")) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else if (theme.equals("dark")) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }
}
