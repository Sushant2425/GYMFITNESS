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
        setupToolbar();
    }

    private void loadPlansFromFirebase() {
        Log.d(TAG, getString(R.string.starting_to_load_plans));

        String ownerEmail = prefManager.getUserEmail();
        if (ownerEmail == null || ownerEmail.isEmpty()) {
            Log.e(TAG, getString(R.string.owner_email_null));
            Toast.makeText(this, getString(R.string.please_login_first), Toast.LENGTH_SHORT).show();
            return;
        }

        String safeEmail = ownerEmail.replace(".", ",");
        Log.d(TAG, getString(R.string.safe_email_log, safeEmail));

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("GYM")
                .child(safeEmail)
                .child("gym_plans");

        Log.d(TAG, getString(R.string.firebase_path_log, ref.toString()));

        plansList.clear();
        planNamesList.clear();

        btnNext.setEnabled(false);
        btnNext.setText(getString(R.string.loading_plans));

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d(TAG, getString(R.string.on_data_change_log, dataSnapshot.hasChildren(), dataSnapshot.getChildrenCount()));

                plansList.clear();
                planNamesList.clear();

                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        Log.d(TAG, getString(R.string.snapshot_key_log, snapshot.getKey()));
                        Log.d(TAG, getString(R.string.snapshot_value_log, String.valueOf(snapshot.getValue())));

                        try {
                            Plan plan = snapshot.getValue(Plan.class);

                            if (plan != null) {
                                Log.d(TAG, getString(R.string.plan_object_log, plan.getPlanName()));
                                plan.setPlanId(snapshot.getKey());

                                if (plan.isActive()) {
                                    plansList.add(plan);

                                    String planName = plan.getPlanName();
                                    if (planName != null && !planName.trim().isEmpty()) {
                                        planNamesList.add(planName);
                                        Log.d(TAG, getString(R.string.added_plan_name_log, planName));
                                    } else {
                                        planNamesList.add(getString(R.string.default_plan_name, plansList.size()));
                                        Log.d(TAG, getString(R.string.added_default_plan_log));
                                    }
                                } else {
                                    Log.d(TAG, getString(R.string.plan_inactive_skipping));
                                }
                            } else {
                                Log.e(TAG, getString(R.string.failed_to_parse_plan));
                            }
                        } catch (Exception e) {
                            Log.e(TAG, getString(R.string.error_parsing_plan, e.getMessage()));
                            e.printStackTrace();
                        }
                    }

                    Log.d(TAG, getString(R.string.total_plans_loaded, plansList.size(), planNamesList.size()));

                    runOnUiThread(() -> {
                        updatePlanAdapter();
                        btnNext.setEnabled(true);
                        btnNext.setText(getString(R.string.next_payment));

                        if (plansList.isEmpty()) {
                            Toast.makeText(PlanSelectActivity.this,
                                    getString(R.string.no_active_plans_found), Toast.LENGTH_LONG).show();
                            spinnerPlan.setHint(getString(R.string.no_plans_available));
                        } else {
                            selectedPlan = plansList.get(0);
                            spinnerPlan.setText(selectedPlan.getPlanName(), false);
                            updatePlanDetails(selectedPlan);

                            Toast.makeText(PlanSelectActivity.this,
                                    getString(R.string.plans_loaded_message, plansList.size()), Toast.LENGTH_SHORT).show();
                        }
                    });

                } else {
                    Log.d(TAG, getString(R.string.no_data_found));
                    runOnUiThread(() -> {
                        btnNext.setEnabled(true);
                        btnNext.setText(getString(R.string.next_payment));
                        Toast.makeText(PlanSelectActivity.this,
                                getString(R.string.no_plans_found_message), Toast.LENGTH_LONG).show();
                        spinnerPlan.setHint(getString(R.string.no_plans_available));
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, getString(R.string.firebase_error_log, error.getMessage(), error.getCode()));
                runOnUiThread(() -> {
                    btnNext.setEnabled(true);
                    btnNext.setText(getString(R.string.next_payment));
                    Toast.makeText(PlanSelectActivity.this,
                            getString(R.string.error_loading_plans_message, error.getMessage()), Toast.LENGTH_SHORT).show();
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
        Log.d(TAG, getString(R.string.adapter_updated_log, planNamesList.size()));
    }

    private void initViews() {
        spinnerPlan = findViewById(R.id.spinnerPlan);
        etTotalFee = findViewById(R.id.etTotalFee);
        etStartDate = findViewById(R.id.etStartDate);
        etEndDate = findViewById(R.id.etEndDate);
        btnNext = findViewById(R.id.btnNext);
        toolbar = findViewById(R.id.toolbar);
    }

    private void getIntentData() {
        name = getIntent().getStringExtra("name");
        phone = getIntent().getStringExtra("phone");
        email = getIntent().getStringExtra("email");
        gender = getIntent().getStringExtra("gender");
        joinDate = getIntent().getStringExtra("joinDate");

        Log.d(TAG, getString(R.string.member_data_log, name, phone, joinDate));
    }

    private void setupPlanSpinner() {
        planNamesList = new ArrayList<>();
        planAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line,
                planNamesList);
        spinnerPlan.setAdapter(planAdapter);

        spinnerPlan.setOnItemClickListener((parent, view, position, id) -> {
            Log.d(TAG, getString(R.string.plan_selected_log, position));
            if (position >= 0 && position < plansList.size()) {
                selectedPlan = plansList.get(position);
                Log.d(TAG, getString(R.string.selected_plan_log, selectedPlan.getPlanName()));
                updatePlanDetails(selectedPlan);
            }
        });

        spinnerPlan.setOnClickListener(v -> {
            if (plansList.isEmpty()) {
                Toast.makeText(this, getString(R.string.no_plans_wait_message), Toast.LENGTH_SHORT).show();
            } else {
                spinnerPlan.showDropDown();
            }
        });
    }

    private void updatePlanDetails(Plan plan) {
        if (plan != null) {
            etTotalFee.setText(getString(R.string.rupee_prefix) + plan.getFee());
            calculateEndDate(plan.getDuration());
            Log.d(TAG, getString(R.string.updated_details_log, plan.getFee(), plan.getDuration()));
        }
    }

    private void setupClickListeners() {
        btnNext.setOnClickListener(v -> validateAndProceed());
    }

    private void setInitialDates() {
        if (joinDate != null && !joinDate.isEmpty()) {
            etStartDate.setText(joinDate);
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            String today = sdf.format(Calendar.getInstance().getTime());
            etStartDate.setText(today);
            joinDate = today;
        }
        etEndDate.setText(getString(R.string.select_plan));
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

            Log.d(TAG, getString(R.string.calculated_end_date_log, endDate, months));
        } catch (ParseException e) {
            etEndDate.setText(getString(R.string.invalid_date));
            Log.e(TAG, getString(R.string.date_calculation_error_log, e.getMessage()));
        }
    }

    private void validateAndProceed() {
        if (selectedPlan == null) {
            Toast.makeText(this, getString(R.string.please_select_plan), Toast.LENGTH_SHORT).show();
            return;
        }

        String startDate = etStartDate.getText().toString().trim();
        String endDate = etEndDate.getText().toString().trim();

        if (TextUtils.isEmpty(startDate)) {
            Toast.makeText(this, getString(R.string.invalid_start_date), Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(endDate) || endDate.equals(getString(R.string.select_plan)) || endDate.equals(getString(R.string.invalid_date))) {
            Toast.makeText(this, getString(R.string.invalid_end_date), Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, getString(R.string.proceeding_to_payment_log, selectedPlan.getPlanName()));

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

            String duration = "1M";
            if (planType != null && planType.contains(" ")) {
                String[] parts = planType.split(" ");
                if (parts.length > 0) {
                    duration = parts[0] + "M";
                }
            }

            return month + "-" + year + "-" + duration;
        } catch (Exception e) {
            Log.e(TAG, getString(R.string.plan_id_generation_error_log, e.getMessage()));
            return getString(R.string.plan_prefix) + System.currentTimeMillis();
        }
    }
}