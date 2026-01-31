package com.coderspuxelinnnovation.gymmanagementsystem.Activities;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
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
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.regex.Pattern;
import android.speech.RecognizerIntent;
import java.util.ArrayList;


public class MemberAddActivity extends BaseActivity {

    private TextInputEditText etName, etPhone, etEmail, etJoinDate;
    private MaterialAutoCompleteTextView spinnerGender;
    private MaterialButton btnNext;
    private ProgressBar progressBar;

    private final String EMAIL_PATTERN = "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,}|localhost)$";
    private final String PHONE_PATTERN = "^[6-9]\\d{9}$";
    private static final int REQ_CODE_SPEECH = 100;
    private TextInputLayout nameInputLayout;
    private static final int REQ_CODE_CONTACT = 200;
    private TextInputLayout phoneInputLayout;
    private MaterialToolbar toolbar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_member_add);

        initViews();
        setupGenderSpinner();
        setupClickListeners();
        setupToolbar();  // Add this line

    }

    private void initViews() {
        etName = findViewById(R.id.etMemberName);
        etPhone = findViewById(R.id.etMemberPhone);
//        etEmail = findViewById(R.id.etMemberEmail);
        etJoinDate = findViewById(R.id.etJoinDate);
        spinnerGender = findViewById(R.id.spinnerGender);
        btnNext = findViewById(R.id.btnNext);
        progressBar = findViewById(R.id.progressBar);
        nameInputLayout = findViewById(R.id.tilMemberName);
        phoneInputLayout = findViewById(R.id.tilMemberPhone);
        toolbar = findViewById(R.id.toolbar);  // Add this line


    }

    private void setupGenderSpinner() {
        String[] genders = {getString(R.string.gender_male), getString(R.string.gender_female)};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, genders);
        spinnerGender.setAdapter(adapter);
    }
    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        // Handle back arrow click - direct close
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_CODE_SPEECH && resultCode == RESULT_OK && data != null) {
            ArrayList<String> result = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);

            if (result != null && !result.isEmpty()) {
                etName.setText(result.get(0)); // Full name auto set
            }
        }
        // 📞 Contact picker
        if (requestCode == REQ_CODE_CONTACT && resultCode == RESULT_OK && data != null) {
            Uri contactUri = data.getData();
            if (contactUri == null) return;

            String[] projection = {
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
            };

            Cursor cursor = getContentResolver().query(
                    contactUri,
                    projection,
                    null,
                    null,
                    null
            );

            if (cursor != null && cursor.moveToFirst()) {
                String name = cursor.getString(0);
                String phone = cursor.getString(1);

                phone = phone.replaceAll("[^0-9]", "");
                if (phone.length() > 10) {
                    phone = phone.substring(phone.length() - 10);
                }

                etName.setText(name);
                etPhone.setText(phone);

                cursor.close();
            }
        }
    }

    private void setupClickListeners() {
        etJoinDate.setOnClickListener(v -> showDatePicker());
        btnNext.setOnClickListener(v -> validateAndProceed());
        if (nameInputLayout != null) {
            nameInputLayout.setEndIconOnClickListener(v -> startVoiceInput());
        }
        if (phoneInputLayout != null) {
            phoneInputLayout.setEndIconOnClickListener(v -> openContactPicker());
        }
    }

    private void openContactPicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType(android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE);
        startActivityForResult(intent, REQ_CODE_CONTACT);
    }

    private void startVoiceInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak full name");

        try {
            startActivityForResult(intent, REQ_CODE_SPEECH);
        } catch (Exception e) {
            Toast.makeText(this, "Voice input not supported", Toast.LENGTH_SHORT).show();
        }
    }

    private void validateAndProceed() {
        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
//        String email = etEmail.getText().toString().trim();
        String gender = spinnerGender.getText().toString().trim();
        String joinDate = etJoinDate.getText().toString().trim();

        // Validation with string resources
        if (TextUtils.isEmpty(name)) {
            etName.setError(getString(R.string.error_name_required));
            return;
        }
        if (TextUtils.isEmpty(phone) || !Pattern.matches(PHONE_PATTERN, phone)) {
            etPhone.setError(getString(R.string.error_phone_valid));
            return;
        }
//        if (TextUtils.isEmpty(email) || !Pattern.matches(EMAIL_PATTERN, email)) {
//            etEmail.setError(getString(R.string.error_email_valid));
//            return;
//        }
        if (TextUtils.isEmpty(gender)) {
            spinnerGender.setError(getString(R.string.error_gender_required));
            return;
        }
        if (TextUtils.isEmpty(joinDate)) {
            etJoinDate.setError(getString(R.string.error_date_required));
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
                            getString(R.string.error_member_exists), Toast.LENGTH_SHORT).show();
                } else {
                    proceedToPlanSelect();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                btnNext.setEnabled(true);
                Toast.makeText(MemberAddActivity.this,
                        getString(R.string.error_generic, error.getMessage()), Toast.LENGTH_SHORT).show();
            }
        });
        }

    private void proceedToPlanSelect() {
        Intent intent = new Intent(this, PlanSelectActivity.class);
        intent.putExtra("name", etName.getText().toString().trim());
        intent.putExtra("phone", etPhone.getText().toString().trim());
//        intent.putExtra("email", etEmail.getText().toString().trim());
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
