package com.coderspuxelinnnovation.gymmanagementsystem.Activities;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.coderspuxelinnnovation.gymmanagementsystem.R;
import com.coderspuxelinnnovation.gymmanagementsystem.Utils.PrefManager;
import com.coderspuxelinnnovation.gymmanagementsystem.base.BaseActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.regex.Pattern;

public class MemberAddActivity extends BaseActivity {

    private TextInputEditText etName, etPhone, etEmail, etJoinDate;
    private MaterialAutoCompleteTextView spinnerGender;
    private MaterialButton btnNext;
    private ProgressBar progressBar;

    private final String EMAIL_PATTERN = "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,}|localhost)$";
    private final String PHONE_PATTERN = "^[6-9]\\d{9}$";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_member_add);

        initViews();
        setupGenderSpinner();
        setupClickListeners();
    }

    private void initViews() {
        etName = findViewById(R.id.etMemberName);
        etPhone = findViewById(R.id.etMemberPhone);
        etEmail = findViewById(R.id.etMemberEmail);
        etJoinDate = findViewById(R.id.etJoinDate);
        spinnerGender = findViewById(R.id.spinnerGender);
        btnNext = findViewById(R.id.btnNext);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupGenderSpinner() {
        String[] genders = {"Male", "Female", "Other"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, genders);
        spinnerGender.setAdapter(adapter);
    }

    private void setupClickListeners() {
        etJoinDate.setOnClickListener(v -> showDatePicker());
        btnNext.setOnClickListener(v -> validateAndProceed());
    }

    private void validateAndProceed() {
        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String gender = spinnerGender.getText().toString().trim();
        String joinDate = etJoinDate.getText().toString().trim();

        // Validation
        if (TextUtils.isEmpty(name)) {
            etName.setError("Name is required");
            return;
        }
        if (TextUtils.isEmpty(phone) || !Pattern.matches(PHONE_PATTERN, phone)) {
            etPhone.setError("Valid 10-digit phone required");
            return;
        }
        if (TextUtils.isEmpty(email) || !Pattern.matches(EMAIL_PATTERN, email)) {
            etEmail.setError("Valid email required");
            return;
        }
        if (TextUtils.isEmpty(gender)) {
            spinnerGender.setError("Gender required");
            return;
        }
        if (TextUtils.isEmpty(joinDate)) {
            etJoinDate.setError("Join date required");
            return;
        }

        // Check if member already exists
        checkMemberExists(phone);
    }

    private void checkMemberExists(String phone) {
        progressBar.setVisibility(View.VISIBLE);
        btnNext.setEnabled(false);

        String ownerEmail = new PrefManager(this).getUserEmail().replace(".", ",");

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("GYM")
                .child(ownerEmail)
                .child("members")
                .child(phone);

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                progressBar.setVisibility(View.GONE);
                btnNext.setEnabled(true);

                if (snapshot.exists()) {
                    Toast.makeText(MemberAddActivity.this,
                            "Member already exists!", Toast.LENGTH_SHORT).show();
                } else {
                    proceedToPlanSelect();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                btnNext.setEnabled(true);
                Toast.makeText(MemberAddActivity.this,
                        "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void proceedToPlanSelect() {
        Intent intent = new Intent(this, PlanSelectActivity.class);
        intent.putExtra("name", etName.getText().toString().trim());
        intent.putExtra("phone", etPhone.getText().toString().trim());
        intent.putExtra("email", etEmail.getText().toString().trim());
        intent.putExtra("gender", spinnerGender.getText().toString().trim());
        intent.putExtra("joinDate", etJoinDate.getText().toString().trim());
        startActivity(intent);
    }

    private void showDatePicker() {
        Calendar currentDate = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            String formattedDate = String.format("%02d/%02d/%d", dayOfMonth, month + 1, year);
            etJoinDate.setText(formattedDate);
        }, currentDate.get(Calendar.YEAR), currentDate.get(Calendar.MONTH),
                currentDate.get(Calendar.DAY_OF_MONTH)).show();
    }
}
