package com.coderspuxelinnnovation.gymmanagementsystem.Registration;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.coderspuxelinnnovation.gymmanagementsystem.R;
import com.coderspuxelinnnovation.gymmanagementsystem.base.BaseActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class RegisterActivity extends BaseActivity {

    // UI Elements
    private TextInputEditText etGymName, etOwnerName, etEmail, etPhone;
    private TextInputEditText etPassword, etConfirmPassword;
    private CheckBox cbTerms;
    private MaterialButton btnRegister;
    private ProgressBar progressBar;
    private TextView tvBackToLogin;

    // Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("ServiceCenter");

        // Initialize Views
        initViews();
        setupListeners();
    }

    private void initViews() {
        etGymName = findViewById(R.id.etGymName);
        etOwnerName = findViewById(R.id.etOwnerName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        cbTerms = findViewById(R.id.cbTerms);
        btnRegister = findViewById(R.id.btnRegister);
        progressBar = findViewById(R.id.progressBar);
        tvBackToLogin = findViewById(R.id.tvBackToLogin);
    }

    private void setupListeners() {
        // Register Button
        btnRegister.setOnClickListener(v -> validateAndRegister());

        // Back to Login
        tvBackToLogin.setOnClickListener(v -> finish());
    }

    private void validateAndRegister() {
        // Get input values
        String gymName = etGymName.getText().toString().trim();
        String ownerName = etOwnerName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // Validation
        if (TextUtils.isEmpty(gymName)) {
            etGymName.setError(getString(R.string.error_enter_gym_name));
            etGymName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(ownerName)) {
            etOwnerName.setError(getString(R.string.error_enter_name));
            etOwnerName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(email) || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError(getString(R.string.error_enter_valid_email));
            etEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(phone) || phone.length() != 10) {
            etPhone.setError(getString(R.string.error_enter_valid_phone));
            etPhone.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password) || password.length() < 6) {
            etPassword.setError(getString(R.string.error_password_min));
            etPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError(getString(R.string.error_password_mismatch));
            etConfirmPassword.requestFocus();
            return;
        }

        if (!cbTerms.isChecked()) {
            Toast.makeText(this, getString(R.string.error_accept_terms), Toast.LENGTH_SHORT).show();
            return;
        }

        // All validations passed, proceed with registration
        registerGym(gymName, ownerName, email, phone, password);
    }

    private void registerGym(String gymName, String ownerName, String email, String phone, String password) {

        progressBar.setVisibility(View.VISIBLE);
        btnRegister.setEnabled(false);

        // Create user with Firebase Authentication
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Save gym data to Realtime Database
                        saveGymDataToDatabase(gymName, ownerName, email, phone, password);
                    } else {
                        progressBar.setVisibility(View.GONE);
                        btnRegister.setEnabled(true);

                        String errorMessage = task.getException() != null ?
                                task.getException().getMessage() : getString(R.string.register_failed);

                        Toast.makeText(RegisterActivity.this,
                                errorMessage,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveGymDataToDatabase(String gymName, String ownerName, String email, String phone, String password) {

        // Create safe key from email (replace . with ,)
        String safeKey = email.replace(".", ",");

        // Create gym data HashMap
        HashMap<String, Object> gymData = new HashMap<>();
        gymData.put("gymName", gymName);
        gymData.put("name", ownerName);
        gymData.put("email", email);
        gymData.put("phone", phone);
        gymData.put("password", password);
        gymData.put("status", true);
        gymData.put("registrationDate", System.currentTimeMillis());

        // Save to Firebase Database
        databaseReference.child(safeKey)
                .child("ownerInfo")
                .setValue(gymData)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    btnRegister.setEnabled(true);

                    if (task.isSuccessful()) {
                        Toast.makeText(RegisterActivity.this,
                                getString(R.string.register_success),
                                Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(RegisterActivity.this,
                                getString(R.string.register_data_failed),
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    btnRegister.setEnabled(true);
                    Toast.makeText(RegisterActivity.this,
                            getString(R.string.error_prefix) + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }
}