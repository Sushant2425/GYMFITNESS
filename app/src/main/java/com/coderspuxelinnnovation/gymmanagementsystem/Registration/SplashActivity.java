package com.coderspuxelinnnovation.gymmanagementsystem.Registration;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.coderspuxelinnnovation.gymmanagementsystem.Activities.LanguageSelectionActivity;
import com.coderspuxelinnnovation.gymmanagementsystem.Utils.LocaleHelper;
import com.coderspuxelinnnovation.gymmanagementsystem.base.BaseActivity;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import com.coderspuxelinnnovation.gymmanagementsystem.Activities.DashboardActivity;
import com.coderspuxelinnnovation.gymmanagementsystem.R;
import com.coderspuxelinnnovation.gymmanagementsystem.Utils.PrefManager;

public class SplashActivity extends BaseActivity {

    DatabaseReference rootRef;
    PrefManager prefManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        rootRef = FirebaseDatabase.getInstance().getReference("ServiceCenter");
        prefManager = new PrefManager(this);

        new Handler().postDelayed(this::checkFirstTimeAndLogin, 1500);
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
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo info = cm.getActiveNetworkInfo();
            return info != null && info.isConnected();
        }
        return false;
    }
}