package com.prashant.milkdairy.Activity;

import android.animation.Animator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.airbnb.lottie.LottieAnimationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Source;
import com.prashant.milkdairy.R;

public class SplashActivity extends AppCompatActivity {

    private static final long FALLBACK_TIMEOUT = 8000;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private LottieAnimationView lottieSplash;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private boolean animationFinished = false;
    private boolean navigationReady = false;
    private boolean navigated = false;

    private Class<?> nextActivityClass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        applySavedTheme();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        lottieSplash = findViewById(R.id.lottieSplash);

        setupAnimationListener();

        startAppInitialization();

        // Safety fallback only if animation fails
        handler.postDelayed(() -> {

            if (!navigated) {

                if (nextActivityClass == null) {
                    nextActivityClass = LoginActivity.class;
                }

                navigateToNextScreen();
            }

        }, FALLBACK_TIMEOUT);
    }

    private void setupAnimationListener() {

        lottieSplash.addAnimatorListener(new Animator.AnimatorListener() {

            @Override
            public void onAnimationStart(@NonNull Animator animation) {

            }

            @Override
            public void onAnimationEnd(@NonNull Animator animation) {

                animationFinished = true;

                tryNavigateWhenReady();
            }

            @Override
            public void onAnimationCancel(@NonNull Animator animation) {

            }

            @Override
            public void onAnimationRepeat(@NonNull Animator animation) {

            }
        });
    }

    private void startAppInitialization() {

        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {

            nextActivityClass = LoginActivity.class;

            navigationReady = true;

            tryNavigateWhenReady();

            return;
        }

        loadLoggedInUserData(user.getUid());
    }

    private void loadLoggedInUserData(String uid) {

        db.collection("users")
                .document(uid)
                .get(Source.DEFAULT)

                .addOnSuccessListener(snapshot -> {

                    loadSettings();

                    nextActivityClass = MainActivity.class;

                    navigationReady = true;

                    tryNavigateWhenReady();
                })

                .addOnFailureListener(e -> {

                    // User authenticated, allow dashboard

                    loadSettings();

                    nextActivityClass = MainActivity.class;

                    navigationReady = true;

                    tryNavigateWhenReady();
                });
    }

    private void tryNavigateWhenReady() {

        if (navigated) return;

        if (!animationFinished) return;

        if (!navigationReady) return;

        navigateToNextScreen();
    }

    private void navigateToNextScreen() {

        if (navigated) return;

        navigated = true;

        Intent intent = new Intent(
                SplashActivity.this,
                nextActivityClass
        );

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);

        finish();
    }

    private void loadSettings() {

        SharedPreferences preferences =
                getSharedPreferences("MilkDairySettings", MODE_PRIVATE);

        String theme = preferences.getString("theme_mode", "system");

        if ("light".equals(theme)) {

            AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_NO
            );

        } else if ("dark".equals(theme)) {

            AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_YES
            );

        } else {

            AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            );
        }
    }

    private void applySavedTheme() {

        SharedPreferences preferences =
                getSharedPreferences("MilkDairySettings", MODE_PRIVATE);

        String theme = preferences.getString("theme_mode", "system");

        if ("light".equals(theme)) {

            AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_NO
            );

        } else if ("dark".equals(theme)) {

            AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_YES
            );

        } else {

            AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            );
        }
    }

    @Override
    protected void onDestroy() {

        handler.removeCallbacksAndMessages(null);

        super.onDestroy();
    }
}