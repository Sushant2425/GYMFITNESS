package com.coderspuxelinnnovation.gymmanagementsystem.Activities;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;

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

import java.util.Calendar;
import java.util.HashMap;
import java.util.regex.Pattern;

public class MemberEditActivity extends BaseActivity {

    private TextInputEditText etName, etPhone, etEmail, etJoinDate;
    private MaterialAutoCompleteTextView spinnerGender;
    private MaterialButton btnUpdate, btnDelete;

    private String originalName, originalEmail, originalGender, originalJoinDate;
    private String memberPhone;

    private final String EMAIL_PATTERN = "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,}|localhost)$";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_member_edit);

        memberPhone = getIntent().getStringExtra("phone");
        if (memberPhone == null) {
            Toast.makeText(this, getString(R.string.invalid_member), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupToolbar();
        setupGenderSpinner();
        setupClickListeners();
        loadMemberData();
    }

    private void initViews() {
        etName = findViewById(R.id.etMemberName);
        etPhone = findViewById(R.id.etMemberPhone);
        etEmail = findViewById(R.id.etMemberEmail);
        etJoinDate = findViewById(R.id.etJoinDate);
        spinnerGender = findViewById(R.id.spinnerGender);
        btnUpdate = findViewById(R.id.btnUpdate);
        btnDelete = findViewById(R.id.btnDelete);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupGenderSpinner() {
        String[] genders = {getString(R.string.gender_male),
                getString(R.string.gender_female),
                getString(R.string.gender_other)};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, genders);
        spinnerGender.setAdapter(adapter);
    }

    private void setupClickListeners() {
        etJoinDate.setOnClickListener(v -> showDatePicker());
        btnUpdate.setOnClickListener(v -> validateAndUpdate());
        btnDelete.setOnClickListener(v -> showDeleteConfirmDialog());
    }

    private void loadMemberData() {
        String ownerEmail = new PrefManager(this).getUserEmail().replace(".", ",");

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("GYM")
                .child(ownerEmail)
                .child("members")
                .child(memberPhone);

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists() || !snapshot.child("info").exists()) {
                    Toast.makeText(MemberEditActivity.this,
                            getString(R.string.member_not_found), Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                originalName = snapshot.child("info").child("name").getValue(String.class);
                originalEmail = snapshot.child("info").child("email").getValue(String.class);
                originalGender = snapshot.child("info").child("gender").getValue(String.class);
                originalJoinDate = snapshot.child("info").child("joinDate").getValue(String.class);

                etName.setText(originalName);
                etPhone.setText(memberPhone);
                etEmail.setText(originalEmail);
                if (originalGender != null) {
                    spinnerGender.setText(originalGender, false);
                }
                etJoinDate.setText(originalJoinDate);

                etPhone.setEnabled(false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MemberEditActivity.this,
                        getString(R.string.error_prefix, error.getMessage()), Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void validateAndUpdate() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String gender = spinnerGender.getText().toString().trim();
        String joinDate = etJoinDate.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            etName.setError(getString(R.string.error_name_required));
            etName.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(email) || !Pattern.matches(EMAIL_PATTERN, email)) {
            etEmail.setError(getString(R.string.error_email_valid));
            etEmail.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(gender)) {
            spinnerGender.setError(getString(R.string.error_gender_required));
            spinnerGender.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(joinDate)) {
            etJoinDate.setError(getString(R.string.error_date_required));
            etJoinDate.requestFocus();
            return;
        }

        if (name.equals(originalName) &&
                email.equals(originalEmail) &&
                gender.equals(originalGender) &&
                joinDate.equals(originalJoinDate)) {
            Toast.makeText(this, getString(R.string.no_changes_detected), Toast.LENGTH_SHORT).show();
            return;
        }

        updateMemberInfo(name, email, gender, joinDate);
    }

    private void updateMemberInfo(String name, String email, String gender, String joinDate) {
        btnUpdate.setEnabled(false);
        btnUpdate.setText(getString(R.string.updating));

        String ownerEmail = new PrefManager(this).getUserEmail().replace(".", ",");

        HashMap<String, Object> infoUpdates = new HashMap<>();
        infoUpdates.put("name", name);
        infoUpdates.put("phone", memberPhone);
        infoUpdates.put("email", email);
        infoUpdates.put("gender", gender);
        infoUpdates.put("joinDate", joinDate);
        infoUpdates.put("status", "ACTIVE");

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("GYM")
                .child(ownerEmail)
                .child("members")
                .child(memberPhone)
                .child("info");

        ref.updateChildren(infoUpdates)
                .addOnSuccessListener(unused -> {
                    btnUpdate.setEnabled(true);
                    btnUpdate.setText(getString(R.string.update_member));
                    Toast.makeText(this,
                            getString(R.string.member_updated_success), Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnUpdate.setEnabled(true);
                    btnUpdate.setText(getString(R.string.update_member));
                    Toast.makeText(this,
                            getString(R.string.update_failed, e.getMessage()), Toast.LENGTH_SHORT).show();
                });
    }

    private void showDeleteConfirmDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(getString(R.string.delete_member))
                .setMessage(getString(R.string.delete_confirmation_message))
                .setPositiveButton(getString(R.string.delete), (dialog, which) -> deleteMember())
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void deleteMember() {
        btnDelete.setEnabled(false);

        String ownerEmail = new PrefManager(this).getUserEmail().replace(".", ",");

        FirebaseDatabase.getInstance()
                .getReference("GYM")
                .child(ownerEmail)
                .child("members")
                .child(memberPhone)
                .removeValue()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this,
                            getString(R.string.member_deleted_success), Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnDelete.setEnabled(true);
                    Toast.makeText(this,
                            getString(R.string.delete_failed, e.getMessage()), Toast.LENGTH_SHORT).show();
                });
    }

    private void showDatePicker() {
        Calendar currentDate = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            String formattedDate = String.format("%02d/%02d/%d", dayOfMonth, month + 1, year);
            etJoinDate.setText(formattedDate);
        }, currentDate.get(Calendar.YEAR),
                currentDate.get(Calendar.MONTH),
                currentDate.get(Calendar.DAY_OF_MONTH)).show();
    }
}