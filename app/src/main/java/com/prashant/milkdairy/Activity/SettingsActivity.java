package com.prashant.milkdairy.Activity;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.button.MaterialButton;
import com.prashant.milkdairy.R;

public class SettingsActivity extends AppCompatActivity {

    private MaterialButton btnLight, btnDark, btnSystem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        btnLight = findViewById(R.id.btnLight);
        btnDark = findViewById(R.id.btnDark);
        btnSystem = findViewById(R.id.btnSystem);

        btnLight.setOnClickListener(v -> {
            saveThemeMode("light");
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        });

        btnDark.setOnClickListener(v -> {
            saveThemeMode("dark");
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        });

        btnSystem.setOnClickListener(v -> {
            saveThemeMode("system");
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        });
    }

    private void saveThemeMode(String mode) {
        SharedPreferences preferences = getSharedPreferences("MilkDairySettings", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("theme_mode", mode);
        editor.apply();
    }
}