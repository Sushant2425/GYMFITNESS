package com.coderspuxelinnnovation.gymmanagementsystem.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.coderspuxelinnnovation.gymmanagementsystem.R;
import com.coderspuxelinnnovation.gymmanagementsystem.base.BaseActivity;
import com.coderspuxelinnnovation.gymmanagementsystem.Utils.PrefManager;
import com.coderspuxelinnnovation.gymmanagementsystem.models.Plan;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class PlanSelectActivity extends BaseActivity {

    private static final String TAG = "PlanSelectActivity";

    private MaterialAutoCompleteTextView spinnerPlan;
    private TextInputEditText etTotalFee, etStartDate, etEndDate;
    private MaterialButton btnNext;

    private MaterialToolbar toolbar;
    // Member data
    private String name, phone, email, gender, joinDate;
    private PrefManager prefManager;
    private List<Plan> plansList = new ArrayList<>();
    private List<String> planNamesList = new ArrayList<>();
    private ArrayAdapter<String> planAdapter;
    private Plan selectedPlan = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plan_select);
        prefManager = new PrefManager(this);

        initViews();
        getIntentData();
        setupPlanSpinner();
        loadPlansFromFirebase();
        setupClickListeners();
        setInitialDates();
        setupToolbar();  // Add this line

    }

    private void loadPlansFromFirebase() {
        Log.d(TAG, "Starting to load plans from Firebase");

        String ownerEmail = prefManager.getUserEmail();
        if (ownerEmail == null || ownerEmail.isEmpty()) {
            Log.e(TAG, "Owner email is null or empty");
            Toast.makeText(this, "⚠️ Please login first!", Toast.LENGTH_SHORT).show();
            return;
        }

        String safeEmail = ownerEmail.replace(".", ",");
        Log.d(TAG, "Safe email: " + safeEmail);

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("GYM")
                .child(safeEmail)
                .child("gym_plans");

        Log.d(TAG, "Firebase path: " + ref.toString());

        // Clear previous data
        plansList.clear();
        planNamesList.clear();

        // Show loading state
        btnNext.setEnabled(false);
        btnNext.setText("Loading plans...");

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d(TAG, "onDataChange called, has children: " + dataSnapshot.hasChildren());
                Log.d(TAG, "Children count: " + dataSnapshot.getChildrenCount());

                // Clear lists
                plansList.clear();
                planNamesList.clear();

                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        Log.d(TAG, "Snapshot key: " + snapshot.getKey());
                        Log.d(TAG, "Snapshot value: " + snapshot.getValue());

                        try {
                            // Get the Plan object from Firebase
                            Plan plan = snapshot.getValue(Plan.class);

                            if (plan != null) {
                                Log.d(TAG, "Plan object created: " + plan.getPlanName());

                                // Set the planId from Firebase key
                                plan.setPlanId(snapshot.getKey());

                                // Only add active plans
                                if (plan.isActive()) {
                                    plansList.add(plan);

                                    String planName = plan.getPlanName();
                                    if (planName != null && !planName.trim().isEmpty()) {
                                        planNamesList.add(planName);
                                        Log.d(TAG, "Added plan name to list: " + planName);
                                    } else {
                                        planNamesList.add("Plan " + plansList.size());
                                        Log.d(TAG, "Added default plan name");
                                    }
                                } else {
                                    Log.d(TAG, "Plan is not active, skipping");
                                }
                            } else {
                                Log.e(TAG, "Failed to parse plan from snapshot");
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing plan: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }

                    Log.d(TAG, "Total plans loaded: " + plansList.size());
                    Log.d(TAG, "Total plan names: " + planNamesList.size());

                    // Update UI on main thread
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updatePlanAdapter();
                            btnNext.setEnabled(true);
                            btnNext.setText(getString(R.string.next_payment));

                            if (plansList.isEmpty()) {
                                Toast.makeText(PlanSelectActivity.this,
                                        "No active plans found", Toast.LENGTH_LONG).show();
                                spinnerPlan.setHint("No plans available");
                            } else {
                                // Auto-select first plan
                                selectedPlan = plansList.get(0);
                                spinnerPlan.setText(selectedPlan.getPlanName(), false);
                                updatePlanDetails(selectedPlan);

                                Toast.makeText(PlanSelectActivity.this,
                                        plansList.size() + " plans loaded", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

                } else {
                    Log.d(TAG, "No data found at this path");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            btnNext.setEnabled(true);
                            btnNext.setText(getString(R.string.next_payment));
                            Toast.makeText(PlanSelectActivity.this,
                                    "No plans found. Please add plans first.", Toast.LENGTH_LONG).show();
                            spinnerPlan.setHint("No plans available");
                        }
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Firebase error: " + error.getMessage() + ", Code: " + error.getCode());
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        btnNext.setEnabled(true);
                        btnNext.setText(getString(R.string.next_payment));
                        Toast.makeText(PlanSelectActivity.this,
                                "Error loading plans: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
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

    private void updatePlanAdapter() {
        if (planNamesList.isEmpty()) {
            planAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_dropdown_item_1line,
                    new ArrayList<>());
        } else {
            planAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_dropdown_item_1line,
                    planNamesList);
        }
        spinnerPlan.setAdapter(planAdapter);
        planAdapter.notifyDataSetChanged();
        Log.d(TAG, "Adapter updated with " + planNamesList.size() + " items");
    }

    private void initViews() {
        spinnerPlan = findViewById(R.id.spinnerPlan);
        etTotalFee = findViewById(R.id.etTotalFee);
        etStartDate = findViewById(R.id.etStartDate);
        etEndDate = findViewById(R.id.etEndDate);
        btnNext = findViewById(R.id.btnNext);
        toolbar = findViewById(R.id.toolbar);  // Add this line

    }

    private void getIntentData() {
        name = getIntent().getStringExtra("name");
        phone = getIntent().getStringExtra("phone");
        email = getIntent().getStringExtra("email");
        gender = getIntent().getStringExtra("gender");
        joinDate = getIntent().getStringExtra("joinDate");

        Log.d(TAG, "Member Data: Name=" + name + ", Phone=" + phone + ", JoinDate=" + joinDate);
    }


    private void setupPlanSpinner() {
        // Initialize with empty adapter
        planNamesList = new ArrayList<>();
        planAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line,
                planNamesList);
        spinnerPlan.setAdapter(planAdapter);

        spinnerPlan.setOnItemClickListener((parent, view, position, id) -> {
            Log.d(TAG, "Plan selected at position: " + position);
            if (position >= 0 && position < plansList.size()) {
                selectedPlan = plansList.get(position);
                Log.d(TAG, "Selected plan: " + selectedPlan.getPlanName());
                updatePlanDetails(selectedPlan);
            }
        });

        // Show dropdown on click
        spinnerPlan.setOnClickListener(v -> {
            if (plansList.isEmpty()) {
                Toast.makeText(this, "No plans available. Please wait for loading.", Toast.LENGTH_SHORT).show();
            } else {
                spinnerPlan.showDropDown();
            }
        });
    }

    private void updatePlanDetails(Plan plan) {
        if (plan != null) {
            etTotalFee.setText("₹" + plan.getFee());
            calculateEndDate(plan.getDuration());
            Log.d(TAG, "Updated details: Fee=" + plan.getFee() + ", Duration=" + plan.getDuration());
        }
    }

    private void setupClickListeners() {
        btnNext.setOnClickListener(v -> validateAndProceed());
    }

    private void setInitialDates() {
        if (joinDate != null && !joinDate.isEmpty()) {
            etStartDate.setText(joinDate);
        } else {
            // Set default to today
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            String today = sdf.format(Calendar.getInstance().getTime());
            etStartDate.setText(today);
            joinDate = today; // Update joinDate
        }
        etEndDate.setText("Select Plan");
    }

    private void calculateEndDate(int months) {
        try {
            String startDateStr = etStartDate.getText().toString();
            if (startDateStr.isEmpty()) {
                startDateStr = joinDate;
            }

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Calendar cal = Calendar.getInstance();
            cal.setTime(sdf.parse(startDateStr));
            cal.add(Calendar.MONTH, months);
            String endDate = sdf.format(cal.getTime());
            etEndDate.setText(endDate);

            Log.d(TAG, "Calculated end date: " + endDate + " for " + months + " months");
        } catch (ParseException e) {
            etEndDate.setText("Invalid Date");
            Log.e(TAG, "Date calculation error: " + e.getMessage());
        }
    }

    private void validateAndProceed() {
        if (selectedPlan == null) {
            Toast.makeText(this, "Please select a plan", Toast.LENGTH_SHORT).show();
            return;
        }

        String startDate = etStartDate.getText().toString().trim();
        String endDate = etEndDate.getText().toString().trim();

        if (TextUtils.isEmpty(startDate)) {
            Toast.makeText(this, "Invalid start date", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(endDate) || endDate.equals("Select Plan") || endDate.equals("Invalid Date")) {
            Toast.makeText(this, "Invalid end date", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Proceeding to payment with plan: " + selectedPlan.getPlanName());

        Intent intent = new Intent(this, PaymentActivity.class);
        intent.putExtra("name", name);
        intent.putExtra("phone", phone);
        intent.putExtra("email", email);
        intent.putExtra("gender", gender);
        intent.putExtra("joinDate", joinDate);
        intent.putExtra("planType", selectedPlan.getPlanName());
        intent.putExtra("totalFee", selectedPlan.getFee());
        intent.putExtra("planDuration", selectedPlan.getDuration());
        intent.putExtra("startDate", startDate);
        intent.putExtra("endDate", endDate);
        intent.putExtra("planId", generatePlanId(startDate, selectedPlan.getPlanName()));

        startActivity(intent);
    }

    private String generatePlanId(String startDate, String planType) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Calendar cal = Calendar.getInstance();
            cal.setTime(sdf.parse(startDate));

            String month = new SimpleDateFormat("MMM", Locale.getDefault())
                    .format(cal.getTime()).toUpperCase();

            int year = cal.get(Calendar.YEAR);

            // Extract duration (e.g., "1 Month" → "1M")
            String duration = "1M";
            if (planType != null && planType.contains(" ")) {
                String[] parts = planType.split(" ");
                if (parts.length > 0) {
                    duration = parts[0] + "M";
                }
            }

            return month + "-" + year + "-" + duration;
        } catch (Exception e) {
            Log.e(TAG, "Error generating plan ID: " + e.getMessage());
            return "PLAN-" + System.currentTimeMillis();
        }
    }
}