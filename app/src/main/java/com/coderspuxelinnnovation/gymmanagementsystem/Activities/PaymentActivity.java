package com.coderspuxelinnnovation.gymmanagementsystem.Activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.coderspuxelinnnovation.gymmanagementsystem.R;
import com.coderspuxelinnnovation.gymmanagementsystem.Utils.PrefManager;
import com.coderspuxelinnnovation.gymmanagementsystem.base.BaseActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.UUID;

public class PaymentActivity extends BaseActivity {
    private String planId, planStartDate;

    private TextInputEditText etTotalFee, etPaidAmount, etRemainingAmount;
    private MaterialAutoCompleteTextView spinnerPaymentMode;
    private MaterialButton btnSave;
    private int totalFee;
    private ProgressBar progressBar; // ADD THIS LINE

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        initViews();
        setupPaymentModeSpinner();
        loadIntentData();
        setupListeners();
        updateRemainingAmount();
    }

    private void initViews() {
        etTotalFee = findViewById(R.id.etTotalFee);
        etPaidAmount = findViewById(R.id.etPaidAmount);
        etRemainingAmount = findViewById(R.id.etRemainingAmount);
        spinnerPaymentMode = findViewById(R.id.spinnerPaymentMode);
        btnSave = findViewById(R.id.btnSavePayment);

        this.progressBar = findViewById(R.id.progressBar); // ✅ CORRECT - CLASS FIELD

    }
    private String appendPlanSuffix(String basePlanId) {
        return basePlanId + "-01";
    }


    private void setupPaymentModeSpinner() {
        String[] modes = {"Cash", "UPI", "Card", "Bank Transfer"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, modes);
        spinnerPaymentMode.setAdapter(adapter);
    }

    private void loadIntentData() {
        totalFee = getIntent().getIntExtra("totalFee", 0);
        etTotalFee.setText("₹" + totalFee);
        planId = getIntent().getStringExtra("planId");
        planStartDate = getIntent().getStringExtra("startDate");

    }

    private void setupListeners() {
        etPaidAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(android.text.Editable s) {
                updateRemainingAmount();
            }
        });

        btnSave.setOnClickListener(v -> savePayment());
    }

    private void updateRemainingAmount() {
        String paidText = etPaidAmount.getText().toString().trim();
        if (!TextUtils.isEmpty(paidText)) {
            try {
                int paid = Integer.parseInt(paidText);
                int remaining = Math.max(0, totalFee - paid);
                etRemainingAmount.setText("₹" + remaining);
            } catch (NumberFormatException e) {
                etRemainingAmount.setText("₹" + totalFee);
            }
        } else {
            etRemainingAmount.setText("₹" + totalFee);
        }
    }

    private void savePayment() {
        String paidText = etPaidAmount.getText().toString().trim();
        String paymentMode = spinnerPaymentMode.getText().toString().trim();

        if (TextUtils.isEmpty(paidText)) {
            etPaidAmount.setError("Enter paid amount");
            return;
        }

        if (TextUtils.isEmpty(paymentMode)) {
            spinnerPaymentMode.setError("Select payment mode");
            return;
        }

        try {
            int paidAmount = Integer.parseInt(paidText);
            if (paidAmount <= 0 || paidAmount > totalFee) {
                Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show();
                return;
            }
            showProgress();

            saveCompleteMemberData(paidAmount, totalFee - paidAmount, paymentMode);
        } catch (NumberFormatException e) {
            etPaidAmount.setError("Invalid amount");
        }
    }
    private void showProgress() {
        progressBar.setVisibility(View.VISIBLE);
        btnSave.setEnabled(false);
    }

    private void hideProgress() {
        progressBar.setVisibility(View.GONE);
        btnSave.setEnabled(true);
    }
    private String getForMonth(String startDate) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Calendar cal = Calendar.getInstance();
            cal.setTime(sdf.parse(startDate));
            return String.format(Locale.getDefault(),
                    "%04d-%02d",
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH) + 1);
        } catch (Exception e) {
            return "";
        }
    }

    private void saveCompleteMemberData(int paidAmount, int remaining, String mode) {

        // 1️⃣ FIRST declare variables
        String ownerEmail = new PrefManager(this).getUserEmail().replace(".", ",");
        String memberPhone = getIntent().getStringExtra("phone");
        String paymentId = UUID.randomUUID().toString();

        // 2️⃣ FINAL planId तयार कर
        String finalPlanId = appendPlanSuffix(planId); // 🔥 IMPORTANT

        // 3️⃣ Payment data
        HashMap<String, Object> paymentData = new HashMap<>();
        paymentData.put("paymentId", paymentId);
        paymentData.put("amountPaid", paidAmount);
        paymentData.put("totalFee", totalFee);
        paymentData.put("remaining", remaining);
        paymentData.put("mode", mode);
        paymentData.put("date", System.currentTimeMillis());
        paymentData.put("status", "PAID");
        paymentData.put("forMonth", getForMonth(planStartDate));
        paymentData.put("planStartDate", planStartDate);
        paymentData.put("planId", finalPlanId);

        // 4️⃣ Firebase reference
        DatabaseReference memberRef = FirebaseDatabase.getInstance()
                .getReference("GYM")
                .child(ownerEmail)
                .child("members")
                .child(memberPhone);

        // 5️⃣ Member info
        HashMap<String, Object> memberInfo = new HashMap<>();
        memberInfo.put("name", getIntent().getStringExtra("name"));
        memberInfo.put("phone", memberPhone);
        memberInfo.put("email", getIntent().getStringExtra("email"));
        memberInfo.put("gender", getIntent().getStringExtra("gender"));
        memberInfo.put("joinDate", getIntent().getStringExtra("joinDate"));
        memberInfo.put("status", "ACTIVE");

        // 6️⃣ Current plan (🔥 SAME finalPlanId)
        HashMap<String, Object> currentPlan = new HashMap<>();
        currentPlan.put("planId", finalPlanId);   // 🔥 FIXED
        currentPlan.put("planType", getIntent().getStringExtra("planType"));
        currentPlan.put("startDate", planStartDate);
        currentPlan.put("endDate", getIntent().getStringExtra("endDate"));
        currentPlan.put("totalFee", totalFee);
        currentPlan.put("status", "ACTIVE");

        // 7️⃣ Save sequence
        memberRef.child("info").setValue(memberInfo)
                .addOnSuccessListener(unused -> {
                    memberRef.child("currentPlan").setValue(currentPlan)
                            .addOnSuccessListener(unused2 -> {
                                memberRef.child("payments")
                                        .child(paymentId)
                                        .setValue(paymentData)
                                        .addOnSuccessListener(unused3 -> {
                                            hideProgress();
                                            Toast.makeText(
                                                    PaymentActivity.this,
                                                    "✅ Member & Payment saved successfully!",
                                                    Toast.LENGTH_LONG
                                            ).show();
                                            finish();
                                        })
                                        .addOnFailureListener(e -> {
                                            hideProgress();
                                            Toast.makeText(
                                                    PaymentActivity.this,
                                                    "❌ Payment failed: " + e.getMessage(),
                                                    Toast.LENGTH_SHORT
                                            ).show();
                                        });
                            })
                            .addOnFailureListener(e -> {
                                hideProgress();
                                Toast.makeText(
                                        PaymentActivity.this,
                                        "❌ Plan save failed: " + e.getMessage(),
                                        Toast.LENGTH_SHORT
                                ).show();
                            });
                })
                .addOnFailureListener(e -> {
                    hideProgress();
                    Toast.makeText(
                            PaymentActivity.this,
                            "❌ Member info failed: " + e.getMessage(),
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }
}
