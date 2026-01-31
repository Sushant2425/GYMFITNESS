package com.coderspuxelinnnovation.gymmanagementsystem.Activities;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.coderspuxelinnnovation.gymmanagementsystem.R;
import com.coderspuxelinnnovation.gymmanagementsystem.Utils.PrefManager;
import com.coderspuxelinnnovation.gymmanagementsystem.base.BaseActivity;
import com.coderspuxelinnnovation.gymmanagementsystem.models.Plan;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ViewPlansActivity extends BaseActivity {

    private Toolbar toolbar;
    private TextView tvPlanCount;
    private LinearLayout plansContainer;
    private LinearLayout emptyState;
    private ProgressBar progressBar;
    private FloatingActionButton fabAddPlan;

    private DatabaseReference plansRef;
    private PrefManager prefManager;
    private List<Plan> planList = new ArrayList<>();
    private NumberFormat currencyFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_plans);

        prefManager = new PrefManager(this);
        currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

        setupToolbar();
        initViews();
        setupListeners();
        loadPlansFromFirebase();
    }

    private void setupToolbar() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void initViews() {
        tvPlanCount = findViewById(R.id.tvPlanCount);
        plansContainer = findViewById(R.id.plansContainer);
        emptyState = findViewById(R.id.emptyState);
        progressBar = findViewById(R.id.progressBar);
        fabAddPlan = findViewById(R.id.fabAddPlan);
    }

    private void setupListeners() {
        fabAddPlan.setOnClickListener(v -> {
            startActivity(new Intent(ViewPlansActivity.this, AddPlanActivity.class));
            finish();
        });
    }

    private void loadPlansFromFirebase() {
        String ownerEmail = prefManager.getUserEmail();

        if (ownerEmail == null) {
            Toast.makeText(this, "Please login first!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        showLoading(true);

        String safeEmail = ownerEmail.replace(".", ",");
        plansRef = FirebaseDatabase.getInstance()
                .getReference("GYM")
                .child(safeEmail)
                .child("gym_plans");

        plansRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                planList.clear();

                if (dataSnapshot.exists()) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        Plan plan = snapshot.getValue(Plan.class);
                        if (plan != null) {
                            plan.setPlanId(snapshot.getKey());
                            planList.add(plan);
                        }
                    }
                }

                updateUI();
                showLoading(false);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                showLoading(false);
                Toast.makeText(ViewPlansActivity.this,
                        "Failed to load plans: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUI() {
        tvPlanCount.setText(planList.size() + " Plans Available");

        if (planList.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            plansContainer.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            plansContainer.setVisibility(View.VISIBLE);
            renderPlans();
        }
    }

    private void renderPlans() {
        plansContainer.removeAllViews();

        for (Plan plan : planList) {
            View planView = LayoutInflater.from(this)
                    .inflate(R.layout.item_plan, plansContainer, false);

            MaterialCardView cardView = planView.findViewById(R.id.cardView);
            TextView tvPlanName = planView.findViewById(R.id.tvPlanName);
            TextView tvPlanDuration = planView.findViewById(R.id.tvPlanDuration);
            TextView tvPlanFee = planView.findViewById(R.id.tvPlanFee);
            TextView tvMonths = planView.findViewById(R.id.tvMonths);
            TextView tvStatus = planView.findViewById(R.id.tvStatus);
            MaterialButton btnEdit = planView.findViewById(R.id.btnEdit);
            MaterialButton btnDelete = planView.findViewById(R.id.btnDelete);

            // Set plan data
            tvPlanName.setText(plan.getPlanName());
            tvPlanDuration.setText(plan.getDuration() + (plan.getDuration() > 1 ? "M" : "M"));
            tvPlanFee.setText(currencyFormat.format(plan.getFee()));
            tvMonths.setText(plan.getDuration() + " Month" + (plan.getDuration() > 1 ? "s" : ""));

            // Set status with proper colors
            if (plan.isActive()) {
                tvStatus.setText("Active");
                tvStatus.setTextColor(ContextCompat.getColor(this, R.color.status_active_text));
                tvStatus.setBackgroundTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(this, R.color.status_active_bg)));
                tvStatus.setCompoundDrawableTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(this, R.color.status_active_icon)));
            } else {
                tvStatus.setText("Inactive");
                tvStatus.setTextColor(ContextCompat.getColor(this, R.color.status_inactive_text));
                tvStatus.setBackgroundTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(this, R.color.status_inactive_bg)));
                tvStatus.setCompoundDrawableTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(this, R.color.status_inactive_icon)));
            }

            // Set different colors for different durations
            int durationColor;
            if (plan.getDuration() == 1) {
                durationColor = ContextCompat.getColor(this, R.color.duration_1_month);
            } else if (plan.getDuration() <= 3) {
                durationColor = ContextCompat.getColor(this, R.color.duration_3_months);
            } else if (plan.getDuration() <= 6) {
                durationColor = ContextCompat.getColor(this, R.color.duration_6_months);
            } else {
                durationColor = ContextCompat.getColor(this, R.color.duration_12_months);
            }

            tvPlanDuration.setTextColor(durationColor);
            tvPlanDuration.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.duration_bg)));
            tvMonths.setTextColor(durationColor);

            // Set fee color based on amount
            if (plan.getFee() > 2000) {
                tvPlanFee.setTextColor(ContextCompat.getColor(this, R.color.fee_high));
            } else if (plan.getFee() > 1000) {
                tvPlanFee.setTextColor(ContextCompat.getColor(this, R.color.fee_medium));
            } else {
                tvPlanFee.setTextColor(ContextCompat.getColor(this, R.color.fee_low));
            }

            // Set click listeners
            cardView.setOnClickListener(v -> showPlanDetails(plan));
            btnEdit.setOnClickListener(v -> showEditBottomSheet(plan));
            btnDelete.setOnClickListener(v -> deletePlan(plan));

            plansContainer.addView(planView);
        }
    }

    private void showPlanDetails(Plan plan) {
        androidx.appcompat.app.AlertDialog.Builder builder =
                new androidx.appcompat.app.AlertDialog.Builder(this);

        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_plan_details, null);

        TextView tvDialogPlanName = dialogView.findViewById(R.id.tvDialogPlanName);
        TextView tvDialogDuration = dialogView.findViewById(R.id.tvDialogDuration);
        TextView tvDialogFee = dialogView.findViewById(R.id.tvDialogFee);
        TextView tvDialogCreated = dialogView.findViewById(R.id.tvDialogCreated);

        tvDialogPlanName.setText(plan.getPlanName());
        tvDialogDuration.setText(plan.getDuration() + " Month(s)");
        tvDialogFee.setText(currencyFormat.format(plan.getFee()));

        // Format created date
        String createdAt = plan.getCreatedAt();
        if (createdAt != null && !createdAt.isEmpty()) {
            try {
                long timestamp = Long.parseLong(createdAt);
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM, yyyy", Locale.getDefault());
                String dateStr = sdf.format(new Date(timestamp));
                tvDialogCreated.setText("Created: " + dateStr);
            } catch (Exception e) {
                tvDialogCreated.setText("Created: Recently");
            }
        } else {
            tvDialogCreated.setText("Created: Recently");
        }

        builder.setView(dialogView)
                .setPositiveButton("OK", null)
                .show();
    }

    private void showEditBottomSheet(Plan plan) {
        try {
            // Inflate bottom sheet view
            View bottomSheetView = LayoutInflater.from(this)
                    .inflate(R.layout.layout_bottom_sheet_edit_plan, null);

            // Create bottom sheet dialog
            BottomSheetDialog editBottomSheet = new BottomSheetDialog(this);
            editBottomSheet.setContentView(bottomSheetView);
            editBottomSheet.setCancelable(true);
            editBottomSheet.setCanceledOnTouchOutside(true);

            // Initialize views
            TextInputEditText etPlanName = bottomSheetView.findViewById(R.id.etPlanName);
            TextInputEditText etPlanFee = bottomSheetView.findViewById(R.id.etPlanFee);
            TextInputEditText etDuration = bottomSheetView.findViewById(R.id.etDuration);
            MaterialButtonToggleGroup toggleStatus = bottomSheetView.findViewById(R.id.toggleStatus);
            MaterialButton btnClose = bottomSheetView.findViewById(R.id.btnClose);
            MaterialButton btnCancel = bottomSheetView.findViewById(R.id.btnCancel);
            MaterialButton btnUpdate = bottomSheetView.findViewById(R.id.btnUpdate);

            // Also get TextInputLayouts for validation
            TextInputLayout tilPlanName = bottomSheetView.findViewById(R.id.tilPlanName);
            TextInputLayout tilPlanFee = bottomSheetView.findViewById(R.id.tilPlanFee);
            TextInputLayout tilDuration = bottomSheetView.findViewById(R.id.tilDuration);

            // Fill data from plan
            etPlanName.setText(plan.getPlanName());
            etPlanFee.setText(String.valueOf((int) plan.getFee()));
            etDuration.setText(String.valueOf(plan.getDuration()));

            // Set status toggle
            if (plan.isActive()) {
                toggleStatus.check(R.id.btnActive);
            } else {
                toggleStatus.check(R.id.btnInactive);
            }

            // Setup button colors based on status
            setupToggleButtonColors(toggleStatus, bottomSheetView);

            // Setup listeners
            btnClose.setOnClickListener(v -> editBottomSheet.dismiss());
            btnCancel.setOnClickListener(v -> editBottomSheet.dismiss());

            btnUpdate.setOnClickListener(v -> {
                String planName = etPlanName.getText().toString().trim();
                String feeStr = etPlanFee.getText().toString().trim();
                String durationStr = etDuration.getText().toString().trim();

                if (validateInputs(planName, feeStr, durationStr, tilPlanName, tilPlanFee, tilDuration)) {
                    updatePlanInFirebase(
                            plan.getPlanId(),
                            planName,
                            Double.parseDouble(feeStr),
                            Integer.parseInt(durationStr),
                            toggleStatus.getCheckedButtonId() == R.id.btnActive,
                            editBottomSheet
                    );
                }
            });

            // Add text watchers for real-time validation
            addTextWatchers(etPlanName, etPlanFee, etDuration, tilPlanName, tilPlanFee, tilDuration);

            // Show bottom sheet
            editBottomSheet.show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to open editor", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupToggleButtonColors(MaterialButtonToggleGroup toggleGroup, View bottomSheetView) {
        toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                MaterialButton button = bottomSheetView.findViewById(checkedId);
                if (checkedId == R.id.btnActive) {
                    button.setBackgroundTintList(ColorStateList.valueOf(
                            ContextCompat.getColor(this, R.color.status_active_bg)));
                    button.setTextColor(ContextCompat.getColor(this, R.color.status_active_text));
                    button.setStrokeColor(ColorStateList.valueOf(
                            ContextCompat.getColor(this, R.color.status_active_icon)));
                } else {
                    button.setBackgroundTintList(ColorStateList.valueOf(
                            ContextCompat.getColor(this, R.color.status_inactive_bg)));
                    button.setTextColor(ContextCompat.getColor(this, R.color.status_inactive_text));
                    button.setStrokeColor(ColorStateList.valueOf(
                            ContextCompat.getColor(this, R.color.status_inactive_icon)));
                }
            }
        });
    }

    private void addTextWatchers(TextInputEditText etName, TextInputEditText etFee, TextInputEditText etDuration,
                                 final TextInputLayout tilName, final TextInputLayout tilFee, final TextInputLayout tilDuration) {
        etName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                validatePlanName(s.toString(), tilName);
            }
        });

        etFee.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                validatePlanFee(s.toString(), tilFee);
            }
        });

        etDuration.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                validateDuration(s.toString(), tilDuration);
            }
        });
    }

    private boolean validateInputs(String planName, String feeStr, String durationStr,
                                   TextInputLayout tilName, TextInputLayout tilFee, TextInputLayout tilDuration) {
        boolean isValid = true;

        if (!validatePlanName(planName, tilName)) {
            isValid = false;
        }

        if (!validatePlanFee(feeStr, tilFee)) {
            isValid = false;
        }

        if (!validateDuration(durationStr, tilDuration)) {
            isValid = false;
        }

        return isValid;
    }

    private boolean validatePlanName(String name, TextInputLayout inputLayout) {
        if (name.isEmpty()) {
            inputLayout.setError("Plan name is required");
            return false;
        } else if (name.length() < 3) {
            inputLayout.setError("Plan name must be at least 3 characters");
            return false;
        } else {
            inputLayout.setError(null);
            return true;
        }
    }

    private boolean validatePlanFee(String feeStr, TextInputLayout inputLayout) {
        if (feeStr.isEmpty()) {
            inputLayout.setError("Fee is required");
            return false;
        }

        try {
            double fee = Double.parseDouble(feeStr);
            if (fee <= 0) {
                inputLayout.setError("Fee must be greater than 0");
                return false;
            } else if (fee > 100000) {
                inputLayout.setError("Fee is too high");
                return false;
            } else {
                inputLayout.setError(null);
                return true;
            }
        } catch (NumberFormatException e) {
            inputLayout.setError("Invalid fee amount");
            return false;
        }
    }

    private boolean validateDuration(String durationStr, TextInputLayout inputLayout) {
        if (durationStr.isEmpty()) {
            inputLayout.setError("Duration is required");
            return false;
        }

        try {
            int duration = Integer.parseInt(durationStr);
            if (duration <= 0) {
                inputLayout.setError("Duration must be greater than 0");
                return false;
            } else if (duration > 36) {
                inputLayout.setError("Maximum duration is 36 months");
                return false;
            } else {
                inputLayout.setError(null);
                return true;
            }
        } catch (NumberFormatException e) {
            inputLayout.setError("Invalid duration");
            return false;
        }
    }

    private void updatePlanInFirebase(String planId, String planName, double fee, int duration, boolean isActive, BottomSheetDialog dialog) {
        showLoading(true);

        String ownerEmail = prefManager.getUserEmail();
        String safeEmail = ownerEmail.replace(".", ",");

        DatabaseReference planRef = FirebaseDatabase.getInstance()
                .getReference("GYM")
                .child(safeEmail)
                .child("gym_plans")
                .child(planId);

        // Update plan data
        planRef.child("planName").setValue(planName);
        planRef.child("fee").setValue(fee);
        planRef.child("duration").setValue(duration);
        planRef.child("active").setValue(isActive)
                .addOnSuccessListener(aVoid -> {
                    dialog.dismiss();
                    showLoading(false);
                    Toast.makeText(this, "Plan updated successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Failed to update plan: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void deletePlan(Plan plan) {
        androidx.appcompat.app.AlertDialog.Builder builder =
                new androidx.appcompat.app.AlertDialog.Builder(this);

        builder.setTitle("Delete Plan")
                .setMessage("Are you sure you want to delete " + plan.getPlanName() + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    String ownerEmail = prefManager.getUserEmail();
                    String safeEmail = ownerEmail.replace(".", ",");

                    DatabaseReference planRef = FirebaseDatabase.getInstance()
                            .getReference("GYM")
                            .child(safeEmail)
                            .child("gym_plans")
                            .child(plan.getPlanId());

                    planRef.removeValue()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(ViewPlansActivity.this,
                                        "Plan deleted successfully",
                                        Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(ViewPlansActivity.this,
                                        "Failed to delete plan: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        plansContainer.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPlansFromFirebase();
    }
}