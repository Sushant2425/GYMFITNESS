package com.coderspuxelinnnovation.gymmanagementsystem.Registration;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.coderspuxelinnnovation.gymmanagementsystem.Activities.DashboardActivity;
import com.coderspuxelinnnovation.gymmanagementsystem.R;
import com.coderspuxelinnnovation.gymmanagementsystem.Utils.PrefManager;
import com.coderspuxelinnnovation.gymmanagementsystem.base.BaseActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends BaseActivity {

    // UI Elements
    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin;
    private TextView tvRegister, tvForgotPassword;
    private ProgressBar progressBar;

    // Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference rootRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        rootRef = FirebaseDatabase.getInstance().getReference("ServiceCenter");

        // Initialize Views
        initViews();
        setupListeners();
    }

    private void initViews() {
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupListeners() {
        btnLogin.setOnClickListener(v -> loginUser());

        tvRegister.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));

        tvForgotPassword.setOnClickListener(v -> resetPassword());
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Validation
        if (TextUtils.isEmpty(email)) {
            etEmail.setError(getString(R.string.error_enter_email));
            etEmail.requestFocus();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError(getString(R.string.error_enter_valid_email));
            etEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError(getString(R.string.error_enter_password));
            etPassword.requestFocus();
            return;
        }

        // Show progress
        progressBar.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(false);

        // Firebase Authentication
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Check user data in database
                        checkUserInDatabase(email, password);
                    } else {
                        progressBar.setVisibility(View.GONE);
                        btnLogin.setEnabled(true);

                        String errorMessage = task.getException() != null ?
                                task.getException().getMessage() : getString(R.string.login_failed);

                        Toast.makeText(LoginActivity.this,
                                errorMessage,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void checkUserInDatabase(String email, String password) {
        String safeEmailKey = email.replace(".", ",");

        rootRef.child(safeEmailKey)
                .child("ownerInfo")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        progressBar.setVisibility(View.GONE);
                        btnLogin.setEnabled(true);

                        if (snapshot.exists()) {
                            // Check if account is active
                            boolean status = false;
                            if (snapshot.child("status").exists()) {
                                Boolean statusValue = snapshot.child("status").getValue(Boolean.class);
                                status = statusValue != null && statusValue;
                            }

                            if (status) {
                                // Login successful
                                Toast.makeText(LoginActivity.this,
                                        getString(R.string.login_success),
                                        Toast.LENGTH_SHORT).show();

                                // Save user email in SharedPreferences
                                PrefManager prefManager = new PrefManager(LoginActivity.this);
                                prefManager.saveUserEmail(email);

                                // Update password in database
                                updatePasswordInDatabase(email, password);

                                // Navigate to Dashboard
                                Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            } else {
                                // Account is disabled
                                mAuth.signOut();
                                Toast.makeText(LoginActivity.this,
                                        getString(R.string.account_disabled),
                                        Toast.LENGTH_LONG).show();
                            }
                        } else {
                            // User not found in database
                            mAuth.signOut();
                            Toast.makeText(LoginActivity.this,
                                    getString(R.string.owner_not_found),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        progressBar.setVisibility(View.GONE);
                        btnLogin.setEnabled(true);
                        Toast.makeText(LoginActivity.this,
                                getString(R.string.error_prefix) + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updatePasswordInDatabase(String email, String password) {
        String safeEmailKey = email.replace(".", ",");
        FirebaseDatabase.getInstance()
                .getReference("ServiceCenter")
                .child(safeEmailKey)
                .child("ownerInfo")
                .child("password")
                .setValue(password);
    }

    private void resetPassword() {
        String email = etEmail.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            etEmail.setError(getString(R.string.error_enter_email_reset));
            etEmail.requestFocus();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError(getString(R.string.error_enter_valid_email));
            etEmail.requestFocus();
            return;
        }

        // Show progress
        progressBar.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(false);

        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    btnLogin.setEnabled(true);

                    if (task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this,
                                getString(R.string.reset_link_sent),
                                Toast.LENGTH_LONG).show();
                    } else {
                        String errorMessage = task.getException() != null ?
                                task.getException().getMessage() : getString(R.string.failed_reset);

                        Toast.makeText(LoginActivity.this,
                                getString(R.string.failed_prefix) + errorMessage,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is already logged in
        if (mAuth.getCurrentUser() != null) {
            PrefManager prefManager = new PrefManager(this);
            String savedEmail = prefManager.getUserEmail();

            if (savedEmail != null && !savedEmail.isEmpty()) {
                // User is logged in, redirect to Dashboard
                Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        }
    }
}