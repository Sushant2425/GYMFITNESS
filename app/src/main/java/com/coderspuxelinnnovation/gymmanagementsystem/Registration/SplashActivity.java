package com.coderspuxelinnnovation.gymmanagementsystem.Registration;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.coderspuxelinnnovation.gymmanagementsystem.Activities.DashboardActivity;
import com.coderspuxelinnnovation.gymmanagementsystem.Activities.LanguageSelectionActivity;
import com.coderspuxelinnnovation.gymmanagementsystem.R;
import com.coderspuxelinnnovation.gymmanagementsystem.Utils.PrefManager;
import com.coderspuxelinnnovation.gymmanagementsystem.base.BaseActivity;
import com.coderspuxelinnnovation.gymmanagementsystem.worker.PlanStatusWorker;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.concurrent.TimeUnit;

public class SplashActivity extends BaseActivity {

    DatabaseReference rootRef;
    PrefManager prefManager;

    // Animation views
    private CardView logoCard;
    private ImageView appLogo;
    private View pulseRing;
    private TextView appName;
    private TextView tagline;
    private ProgressBar progressBar;
    private LinearLayout bottomBranding;
    private View circle1, circle2;
    private ImageView floatingIcon1, floatingIcon2, floatingIcon3;
    private View dot1, dot2, dot3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        rootRef = FirebaseDatabase.getInstance().getReference("GYM");
        prefManager = new PrefManager(this);

        // Initialize animation views
        initializeViews();

        // Start animations
        animateSplashScreen();

        PeriodicWorkRequest request =
                new PeriodicWorkRequest.Builder(
                        PlanStatusWorker.class,
                        1,
                        TimeUnit.DAYS
                ).build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "PLAN_STATUS_CHECK",
                ExistingPeriodicWorkPolicy.KEEP,
                request
        );
//        OneTimeWorkRequest testRequest =
//                new OneTimeWorkRequest.Builder(PlanStatusWorker.class)
//                        .build();
//
//        WorkManager.getInstance(this).enqueue(testRequest);

        new Handler().postDelayed(this::checkFirstTimeAndLogin, 2500);


//        // Check login status after delay
//        new Handler().postDelayed(this::checkFirstTimeAndLogin, 2500);
//
//        OneTimeWorkRequest request =
//                new OneTimeWorkRequest.Builder(PlanStatusWorker.class)
//                        .build();
//
//        WorkManager.getInstance(this).enqueue(request);

    }

    private void initializeViews() {
        logoCard = findViewById(R.id.logoCard);
        appLogo = findViewById(R.id.appLogo);
        pulseRing = findViewById(R.id.pulseRing);
        appName = findViewById(R.id.appName);
        tagline = findViewById(R.id.tagline);
        progressBar = findViewById(R.id.progressBar);
        bottomBranding = findViewById(R.id.bottomBranding);
        circle1 = findViewById(R.id.circle1);
        circle2 = findViewById(R.id.circle2);
        floatingIcon1 = findViewById(R.id.floatingIcon1);
        floatingIcon2 = findViewById(R.id.floatingIcon2);
        floatingIcon3 = findViewById(R.id.floatingIcon3);
        dot1 = findViewById(R.id.dot1);
        dot2 = findViewById(R.id.dot2);
        dot3 = findViewById(R.id.dot3);
    }

    private void animateSplashScreen() {
        // Logo Card Animation - Scale up with bounce
        logoCard.setAlpha(0f);
        logoCard.setScaleX(0.3f);
        logoCard.setScaleY(0.3f);
        logoCard.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(800)
                .setInterpolator(new OvershootInterpolator())
                .start();

        // App Logo Rotation - 360 degree spin
        appLogo.animate()
                .rotationBy(360f)
                .setDuration(1000)
                .setStartDelay(300)
                .start();

        // Pulse Ring Animation
        animatePulse(pulseRing);

        // App Name Animation - Slide up and fade in
        appName.setTranslationY(50f);
        appName.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(600)
                .setStartDelay(400)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        // Tagline Animation - Slide up and fade in
        tagline.setTranslationY(50f);
        tagline.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(600)
                .setStartDelay(600)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        // Progress Bar Animation - Fade in
        progressBar.animate()
                .alpha(1f)
                .setDuration(400)
                .setStartDelay(800)
                .start();

        // Loading Dots Animation
        animateLoadingDots();

        // Bottom Branding Animation - Slide up and fade in
        bottomBranding.setTranslationY(30f);
        bottomBranding.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(600)
                .setStartDelay(1000)
                .start();

        // Background Circles Animation
        animateBackgroundCircles();

        // Floating Icons Animation
        animateFloatingIcons();
    }

    private void animatePulse(View view) {
        PropertyValuesHolder scaleX = PropertyValuesHolder.ofFloat("scaleX", 1f, 1.2f, 1f);
        PropertyValuesHolder scaleY = PropertyValuesHolder.ofFloat("scaleY", 1f, 1.2f, 1f);
        PropertyValuesHolder alpha = PropertyValuesHolder.ofFloat("alpha", 0f, 0.5f, 0f);

        ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(view, scaleX, scaleY, alpha);
        animator.setDuration(2000);
        animator.setRepeatCount(ObjectAnimator.INFINITE);
        animator.setStartDelay(800);
        animator.start();
    }

    private void animateLoadingDots() {
        animateDot(dot1, 0);
        animateDot(dot2, 200);
        animateDot(dot3, 400);
    }

    private void animateDot(View dot, long delay) {
        PropertyValuesHolder scaleX = PropertyValuesHolder.ofFloat("scaleX", 1f, 1.5f, 1f);
        PropertyValuesHolder scaleY = PropertyValuesHolder.ofFloat("scaleY", 1f, 1.5f, 1f);
        PropertyValuesHolder alpha = PropertyValuesHolder.ofFloat("alpha", 0.3f, 1f, 0.3f);

        ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(dot, scaleX, scaleY, alpha);
        animator.setDuration(1200);
        animator.setRepeatCount(ObjectAnimator.INFINITE);
        animator.setStartDelay(1000 + delay);
        animator.start();
    }

    private void animateBackgroundCircles() {
        // Circle 1 - Clockwise rotation
        circle1.animate()
                .rotationBy(360f)
                .setDuration(20000)
                .setInterpolator(new LinearInterpolator())
                .withEndAction(() -> animateBackgroundCircle(circle1, 360f, 20000))
                .start();

        // Circle 2 - Counter-clockwise rotation
        circle2.animate()
                .rotationBy(-360f)
                .setDuration(25000)
                .setInterpolator(new LinearInterpolator())
                .withEndAction(() -> animateBackgroundCircle(circle2, -360f, 25000))
                .start();
    }

    private void animateBackgroundCircle(View circle, float rotation, long duration) {
        circle.animate()
                .rotationBy(rotation)
                .setDuration(duration)
                .setInterpolator(new LinearInterpolator())
                .withEndAction(() -> animateBackgroundCircle(circle, rotation, duration))
                .start();
    }

    private void animateFloatingIcons() {
        animateFloatingIcon(floatingIcon1, 3000, 20f);
        animateFloatingIcon(floatingIcon2, 3500, -15f);
        animateFloatingIcon(floatingIcon3, 4000, 18f);
    }

    private void animateFloatingIcon(ImageView icon, long duration, float translationY) {
        icon.animate()
                .translationY(translationY)
                .setDuration(duration)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> {
                    icon.animate()
                            .translationY(0f)
                            .setDuration(duration)
                            .withEndAction(() -> animateFloatingIcon(icon, duration, translationY))
                            .start();
                })
                .start();
    }

    private void checkFirstTimeAndLogin() {
        // Check if this is the first time launch
        if (prefManager.isFirstTimeLaunch()) {
            // First time - go to Language Selection
            startActivity(new Intent(this, LanguageSelectionActivity.class));
            finish();
        } else {
            // Not first time - check login status
            checkLoginStatus();
        }
    }

    private void checkLoginStatus() {
        if (!isConnected()) {
            Toast.makeText(this,
                    getString(R.string.no_internet),
                    Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        String email = prefManager.getUserEmail();

        if (email == null) {
            // Not logged in - go to Login
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        } else {
            // Logged in - verify account status
            String safeEmail = email.replace(".", ",");

            rootRef.child(safeEmail)
                    .child("ownerInfo")
                    .child("status")
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        Boolean status = snapshot.getValue(Boolean.class);

                        if (status != null && status) {
                            startActivity(new Intent(this, DashboardActivity.class));
                            finish();
                        } else {
                            prefManager.logout();
                            Toast.makeText(this,
                                    getString(R.string.account_disabled_login),
                                    Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(this, LoginActivity.class));
                            finish();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this,
                                getString(R.string.login_expired),
                                Toast.LENGTH_SHORT).show();
                        prefManager.logout();
                        startActivity(new Intent(this, LoginActivity.class));
                        finish();
                    });
        }
    }

    private boolean isConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo info = cm.getActiveNetworkInfo();
            return info != null && info.isConnected();
        }
        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Cancel all animations to prevent memory leaks
        if (logoCard != null) logoCard.animate().cancel();
        if (appLogo != null) appLogo.animate().cancel();
        if (appName != null) appName.animate().cancel();
        if (tagline != null) tagline.animate().cancel();
        if (progressBar != null) progressBar.animate().cancel();
        if (bottomBranding != null) bottomBranding.animate().cancel();
        if (circle1 != null) circle1.animate().cancel();
        if (circle2 != null) circle2.animate().cancel();
        if (floatingIcon1 != null) floatingIcon1.animate().cancel();
        if (floatingIcon2 != null) floatingIcon2.animate().cancel();
        if (floatingIcon3 != null) floatingIcon3.animate().cancel();
    }
}