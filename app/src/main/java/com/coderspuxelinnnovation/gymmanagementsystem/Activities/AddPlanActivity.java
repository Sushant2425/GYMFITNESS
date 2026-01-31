package com.coderspuxelinnnovation.gymmanagementsystem.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;

import com.coderspuxelinnnovation.gymmanagementsystem.models.Plan;
import com.coderspuxelinnnovation.gymmanagementsystem.R;
import com.coderspuxelinnnovation.gymmanagementsystem.Utils.PrefManager;
import com.coderspuxelinnnovation.gymmanagementsystem.base.BaseActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class AddPlanActivity extends BaseActivity {

    private MaterialAutoCompleteTextView spinnerPlanDuration;
    private TextInputEditText etFee;
    private MaterialButton btnSavePlan;
    private ImageView btnViewPlans; // Changed to ImageView
    private DatabaseReference plansRef;
    private PrefManager prefManager;

    // Dynamic 1-12 months
    private final String[] planDurations = new String[12];
    private final int[] planMonths = new int[12];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_plan);

        prefManager = new PrefManager(this);
        initPlansArray();
        setupToolbar();
        initViews();
        setupPlanSpinner();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void initPlansArray() {
        for (int i = 1; i <= 12; i++) {
            planMonths[i - 1] = i;
            planDurations[i - 1] = i + " Month" + (i > 1 ? "s" : "");
        }
    }

    private void initViews() {
        spinnerPlanDuration = findViewById(R.id.spinnerPlanDuration);
        etFee = findViewById(R.id.etFee);
        btnSavePlan = findViewById(R.id.btnSavePlan);
        btnViewPlans = findViewById(R.id.btnViewPlans); // Initialize ImageView

        btnSavePlan.setOnClickListener(v -> savePlanToFirebase());
        btnViewPlans.setOnClickListener(v -> navigateToViewPlans()); // Set click listener

        // Optional: Add ripple effect programmatically
        btnViewPlans.setOnLongClickListener(v -> {
            Toast.makeText(this, "View All Plans", Toast.LENGTH_SHORT).show();
            return true;
        });
    }

    private void setupPlanSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                planDurations
        );
        spinnerPlanDuration.setAdapter(adapter);
        spinnerPlanDuration.setThreshold(0);

        spinnerPlanDuration.setOnClickListener(v -> spinnerPlanDuration.showDropDown());
    }

    private void navigateToViewPlans() {
        Intent intent = new Intent(AddPlanActivity.this, ViewPlansActivity.class);
        startActivity(intent);
        // Optional: add animation
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    private void savePlanToFirebase() {
        String selectedPlan = spinnerPlanDuration.getText().toString().trim();
        String feeStr = etFee.getText().toString().trim();

        // Validation
        if (TextUtils.isEmpty(selectedPlan)) {
            Toast.makeText(this, getString(R.string.select_plan_duration), Toast.LENGTH_SHORT).show();
            spinnerPlanDuration.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(feeStr)) {
            etFee.setError(getString(R.string.fee_required));
            etFee.requestFocus();
            return;
        }

        try {
            int fee = Integer.parseInt(feeStr);

            if (fee <= 0) {
                etFee.setError(getString(R.string.fee_positive));
                etFee.requestFocus();
                return;
            }

            String ownerEmail = prefManager.getUserEmail();

            if (ownerEmail == null) {
                Toast.makeText(this, "❌ Please login first!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Show loading
            btnSavePlan.setEnabled(false);
            btnSavePlan.setText(getString(R.string.saving));

            int duration = getDurationFromPlan(selectedPlan);
            Plan plan = new Plan(selectedPlan, fee, duration);

            // Firebase path: GYM/{ownerEmail}/gym_plans/
            String safeEmail = ownerEmail.replace(".", ",");
            plansRef = FirebaseDatabase.getInstance()
                    .getReference("GYM")
                    .child(safeEmail)
                    .child("gym_plans");

            String planId = plansRef.push().getKey();
            if (planId != null) {
                plansRef.child(planId).setValue(plan)
                        .addOnSuccessListener(aVoid -> {
                            btnSavePlan.setEnabled(true);
                            btnSavePlan.setText(getString(R.string.save_plan));
                            Toast.makeText(this,
                                    "✅ " + getString(R.string.plan_saved),
                                    Toast.LENGTH_LONG).show();

                            // Show success dialog
                            showSuccessDialog();
                        })
                        .addOnFailureListener(e -> {
                            btnSavePlan.setEnabled(true);
                            btnSavePlan.setText(getString(R.string.save_plan));
                            Toast.makeText(this,
                                    "❌ " + getString(R.string.plan_save_failed) + ": " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        });
            }

        } catch (NumberFormatException e) {
            etFee.setError(getString(R.string.fee_invalid));
            etFee.requestFocus();
        }
    }

    private void showSuccessDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(getString(R.string.plan_saved_success))
                .setMessage(getString(R.string.view_all_plans_prompt))
                .setPositiveButton(getString(R.string.view_plans), (dialog, which) -> {
                    navigateToViewPlans();
                    clearFields();
                })
                .setNegativeButton(getString(R.string.add_more), (dialog, which) -> {
                    clearFields();
                })
                .setCancelable(false)
                .show();
    }

    private int getDurationFromPlan(String planName) {
        for (int i = 0; i < planDurations.length; i++) {
            if (planDurations[i].equals(planName)) {
                return planMonths[i];
            }
        }
        return 1; // Default to 1 month
    }

    private void clearFields() {
        spinnerPlanDuration.setText("");
        etFee.setText("");
        spinnerPlanDuration.requestFocus();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        // Optional: add back animation
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}