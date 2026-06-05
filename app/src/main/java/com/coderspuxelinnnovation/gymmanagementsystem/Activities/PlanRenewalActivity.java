package com.coderspuxelinnnovation.gymmanagementsystem.Activities;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.coderspuxelinnnovation.gymmanagementsystem.R;
import com.coderspuxelinnnovation.gymmanagementsystem.Utils.PrefManager;
import com.coderspuxelinnnovation.gymmanagementsystem.base.BaseActivity;
import com.coderspuxelinnnovation.gymmanagementsystem.models.Plan;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PlanRenewalActivity extends BaseActivity {

    private static final String TAG = "PlanRenewalActivity";

    // UI Components
    private TextView tvMemberName, tvMemberPhone, tvPlanStatus;
    private TextView tvPreviousPlanType, tvPreviousStartDate, tvPreviousEndDate, tvPreviousFee;
    private TextView tvOutstandingBalance, tvSummaryNewPlan, tvSummaryPreviousDue, tvTotalPayable;
    private MaterialCardView cardOutstandingBalance;
    private LinearLayout layoutPreviousDue;
    private MaterialAutoCompleteTextView spinnerNewPlanType;
    private TextInputEditText etNewStartDate, etNewEndDate, etNewPlanFee;
    private MaterialButton btnQuickRenew, btnProceedToPayment;
    private ProgressBar progressBar;

    // Data variables
    private String memberPhone, memberName, memberEmail, memberGender, memberJoinDate;
    private String previousPlanType, previousStartDate, previousEndDate, previousPlanId;
    private int previousTotalFee, outstandingBalance = 0;
    private int newPlanFee = 0;
    private String ownerEmail;
    private DatabaseReference memberRef;

    // Plan Types with Fee
    private final String[] planTypes = {"1 Month", "3 Months", "6 Months", "1 Year"};
    private final int[] planFees = {500, 1400, 2700, 5000};  // Default fees
    private final int[] planMonths = {1, 3, 6, 12};
    private List<String> planNames = new ArrayList<>();
    private Map<String, Integer> planFeeMap = new HashMap<>();
    private Map<String, Integer> planMonthMap = new HashMap<>();
    private DatabaseReference plansRef;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plan_renewal);

        // 🔥 ADD TOOLBAR SETUP (Fixes your error)
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Your existing methods
        initViews();
        loadIntentData();
        setupPlanSpinner();
        loadMemberData();
        setupListeners();
    }

    // 🔥 ADD THIS for perfect back navigation
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void initViews() {
        // Header
        tvMemberName = findViewById(R.id.tvMemberName);
        tvMemberPhone = findViewById(R.id.tvMemberPhone);
        tvPlanStatus = findViewById(R.id.tvPlanStatus);

        // Previous Plan
        tvPreviousPlanType = findViewById(R.id.tvPreviousPlanType);
        tvPreviousStartDate = findViewById(R.id.tvPreviousStartDate);
        tvPreviousEndDate = findViewById(R.id.tvPreviousEndDate);
        tvPreviousFee = findViewById(R.id.tvPreviousFee);

        // Outstanding Balance
        cardOutstandingBalance = findViewById(R.id.cardOutstandingBalance);
        tvOutstandingBalance = findViewById(R.id.tvOutstandingBalance);

        // New Plan
        spinnerNewPlanType = findViewById(R.id.spinnerNewPlanType);
        etNewStartDate = findViewById(R.id.etNewStartDate);
        etNewEndDate = findViewById(R.id.etNewEndDate);
        etNewPlanFee = findViewById(R.id.etNewPlanFee);

        // Summary
        tvSummaryNewPlan = findViewById(R.id.tvSummaryNewPlan);
        tvSummaryPreviousDue = findViewById(R.id.tvSummaryPreviousDue);
        tvTotalPayable = findViewById(R.id.tvTotalPayable);
        layoutPreviousDue = findViewById(R.id.layoutPreviousDue);

        // Buttons
        btnQuickRenew = findViewById(R.id.btnQuickRenew);
        btnProceedToPayment = findViewById(R.id.btnProceedToPayment);
        progressBar = findViewById(R.id.progressBar);
    }

    private void loadIntentData() {
        memberPhone = getIntent().getStringExtra("phone");
        memberName = getIntent().getStringExtra("name");

        if (memberPhone == null) {
            Toast.makeText(this, getString(R.string.invalid_member_data), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ownerEmail = new PrefManager(this).getUserEmail().replace(".", ",");
        memberRef = FirebaseDatabase.getInstance()
                .getReference("GYM")
                .child(ownerEmail)
                .child("members")
                .child(memberPhone);

        tvMemberName.setText(memberName != null ? memberName : getString(R.string.member));
        tvMemberPhone.setText(getString(R.string.phone_with_code, memberPhone));
    }

    private void setupPlanSpinner() {
        String ownerEmail = new PrefManager(this).getUserEmail();
        if (ownerEmail == null) return;
        ownerEmail = ownerEmail.replace(".", ",");

        plansRef = FirebaseDatabase.getInstance()
                .getReference("GYM")
                .child(ownerEmail)
                .child("gym_plans");

        plansRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                planNames.clear();
                planFeeMap.clear();
                planMonthMap.clear();

                if (snapshot.exists() && snapshot.hasChildren()) {
                    // Load from Firebase
                    for (DataSnapshot snap : snapshot.getChildren()) {
                        Plan plan = snap.getValue(Plan.class);
                        if (plan != null) {
                            planNames.add(plan.getPlanName());
                            planFeeMap.put(plan.getPlanName(), plan.getFee());
                            planMonthMap.put(plan.getPlanName(), plan.getDuration());
                        }
                    }

                    if (planNames.isEmpty()) {
                        loadDefaultPlans();
                    }
                } else {
                    // No plans in Firebase, use defaults
                    loadDefaultPlans();
                }

                // Setup spinner
                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        PlanRenewalActivity.this,
                        android.R.layout.simple_dropdown_item_1line,
                        planNames
                );
                spinnerNewPlanType.setAdapter(adapter);

                // Auto-select first plan if exists
                if (!planNames.isEmpty()) {
                    String firstPlan = planNames.get(0);
                    spinnerNewPlanType.setText(firstPlan, false);

                    // Auto-set fee and calculate dates
                    if (planFeeMap.containsKey(firstPlan)) {
                        newPlanFee = planFeeMap.get(firstPlan);
                        etNewPlanFee.setText(String.valueOf(newPlanFee));

                        if (planMonthMap.containsKey(firstPlan)) {
                            calculateEndDate(planMonthMap.get(firstPlan));
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(PlanRenewalActivity.this,
                        getString(R.string.failed_to_load_plans), Toast.LENGTH_SHORT).show();
                // Fallback to default plans
                loadDefaultPlans();
            }
        });
    }

    private void loadDefaultPlans() {
        // Fallback to hardcoded plans
        for (int i = 0; i < planTypes.length; i++) {
            planNames.add(planTypes[i]);
            planFeeMap.put(planTypes[i], planFees[i]);
            planMonthMap.put(planTypes[i], planMonths[i]);
        }
    }

    private void updateSummary() {
        // Show new plan fee
        tvSummaryNewPlan.setText(getString(R.string.rupee_prefix) + newPlanFee);

        // Show previous due if any
        if (outstandingBalance > 0) {
            layoutPreviousDue.setVisibility(View.VISIBLE);
            tvSummaryPreviousDue.setText(getString(R.string.rupee_prefix) + outstandingBalance);
        } else {
            layoutPreviousDue.setVisibility(View.GONE);
        }

        // Calculate total payable = new plan fee + old due
        int total = newPlanFee + outstandingBalance;
        tvTotalPayable.setText(getString(R.string.rupee_prefix) + total);
    }


    private void loadMemberData() {
        showProgress();
        memberRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    loadMemberInfo(snapshot.child("info"));
                    loadCurrentPlan(snapshot.child("currentPlan"));
                    calculateOutstandingBalance(snapshot.child("payments"));
                } else {
                    hideProgress();
                    Toast.makeText(PlanRenewalActivity.this,
                            getString(R.string.member_not_found), Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                hideProgress();
                Toast.makeText(PlanRenewalActivity.this,
                        getString(R.string.error_prefix, error.getMessage()), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadMemberInfo(DataSnapshot infoSnapshot) {
        if (infoSnapshot.exists()) {
            memberName = infoSnapshot.child("name").getValue(String.class);
            memberEmail = infoSnapshot.child("email").getValue(String.class);
            memberGender = infoSnapshot.child("gender").getValue(String.class);
            memberJoinDate = infoSnapshot.child("joinDate").getValue(String.class);

            if (memberName != null) {
                tvMemberName.setText(memberName);
            }
        }
    }

    private void loadCurrentPlan(DataSnapshot planSnapshot) {
        if (planSnapshot.exists()) {
            previousPlanType = planSnapshot.child("planType").getValue(String.class);
            previousStartDate = planSnapshot.child("startDate").getValue(String.class);
            previousEndDate = planSnapshot.child("endDate").getValue(String.class);
            previousPlanId = planSnapshot.child("planId").getValue(String.class);

            Long feeValue = planSnapshot.child("totalFee").getValue(Long.class);
            previousTotalFee = feeValue != null ? feeValue.intValue() : 0;

            String status = planSnapshot.child("status").getValue(String.class);

            // Update UI
            tvPreviousPlanType.setText(previousPlanType != null ? previousPlanType : getString(R.string.na));
            tvPreviousStartDate.setText(previousStartDate != null ? previousStartDate : getString(R.string.na));
            tvPreviousEndDate.setText(previousEndDate != null ? previousEndDate : getString(R.string.na));
            tvPreviousFee.setText(getString(R.string.rupee_prefix) + previousTotalFee);
            tvPlanStatus.setText(getString(R.string.plan_status_label, status != null ? status : getString(R.string.expired)));

            // Set default new start date (today or next day after expiry)
            setDefaultStartDate();
        }
    }

    private void calculateOutstandingBalance(DataSnapshot paymentsSnapshot) {
        hideProgress();

        int totalPaid = 0;
        if (paymentsSnapshot.exists()) {
            for (DataSnapshot payment : paymentsSnapshot.getChildren()) {
                String paymentPlanId = payment.child("planId").getValue(String.class);

                // Calculate for previous plan
                if (paymentPlanId != null && paymentPlanId.equals(previousPlanId)) {
                    Long paidAmount = payment.child("amountPaid").getValue(Long.class);
                    if (paidAmount != null) {
                        totalPaid += paidAmount.intValue();
                    }
                }
            }
        }

        outstandingBalance = Math.max(0, previousTotalFee - totalPaid);

        if (outstandingBalance > 0) {
            cardOutstandingBalance.setVisibility(View.VISIBLE);
            tvOutstandingBalance.setText(getString(R.string.rupee_prefix) + outstandingBalance);
            layoutPreviousDue.setVisibility(View.VISIBLE);
            tvSummaryPreviousDue.setText(getString(R.string.rupee_prefix) + outstandingBalance);
            updateSummary();
        } else {
            cardOutstandingBalance.setVisibility(View.GONE);
            layoutPreviousDue.setVisibility(View.GONE);
            updateSummary();
        }
    }

    private void setDefaultStartDate() {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String today = sdf.format(cal.getTime());

        // If previous plan exists and has end date
        if (previousEndDate != null && !previousEndDate.isEmpty()) {
            try {
                Date prevEndDate = sdf.parse(previousEndDate);
                Date currentDate = sdf.parse(today);

                if (prevEndDate.before(currentDate)) {
                    // If previous plan ended in past, start from today
                    etNewStartDate.setText(today);
                } else {
                    // If previous plan ends today or in future, start from next day
                    cal.setTime(prevEndDate);
                    cal.add(Calendar.DAY_OF_MONTH, 1);
                    etNewStartDate.setText(sdf.format(cal.getTime()));
                }
            } catch (ParseException e) {
                Log.e(TAG, getString(R.string.date_parse_error, e.getMessage()));
                etNewStartDate.setText(today);
            }
        } else {
            // No previous plan, start from today
            etNewStartDate.setText(today);
        }

        // After setting start date, calculate end date if plan is selected
        String planType = spinnerNewPlanType.getText().toString().trim();
        if (!TextUtils.isEmpty(planType) && planMonthMap.containsKey(planType)) {
            calculateEndDate(planMonthMap.get(planType));
        }
    }

    private void setupListeners() {
        // Quick Renew Button
        btnQuickRenew.setOnClickListener(v -> quickRenewSamePlan());

        // Start Date Picker
        etNewStartDate.setOnClickListener(v -> showDatePicker());

        // Plan Type Selection
        spinnerNewPlanType.setOnItemClickListener((parent, view, position, id) -> {
            String selectedPlan = planNames.get(position);

            if (planFeeMap.containsKey(selectedPlan)) {
                newPlanFee = planFeeMap.get(selectedPlan);
                etNewPlanFee.setText(String.valueOf(newPlanFee));

                if (planMonthMap.containsKey(selectedPlan)) {
                    calculateEndDate(planMonthMap.get(selectedPlan));
                }
            }
            updateSummary();
        });

        // Fee Change Listener
        etNewPlanFee.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(android.text.Editable s) {
                try {
                    if (!TextUtils.isEmpty(s.toString())) {
                        newPlanFee = Integer.parseInt(s.toString());
                        updateSummary();
                    }
                } catch (NumberFormatException e) {
                    Log.e(TAG, getString(R.string.invalid_fee, e.getMessage()));
                }
            }
        });

        // Proceed to Payment
        btnProceedToPayment.setOnClickListener(v -> proceedToPayment());
    }

    private void quickRenewSamePlan() {
        if (previousPlanType == null) {
            Toast.makeText(this, getString(R.string.no_previous_plan_found), Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if previous plan exists in available plans
        if (planNames.contains(previousPlanType)) {
            spinnerNewPlanType.setText(previousPlanType, false);

            if (planFeeMap.containsKey(previousPlanType)) {
                newPlanFee = planFeeMap.get(previousPlanType);
                etNewPlanFee.setText(String.valueOf(newPlanFee));

                if (planMonthMap.containsKey(previousPlanType)) {
                    calculateEndDate(planMonthMap.get(previousPlanType));
                }
            }
            updateSummary();

            Toast.makeText(this, getString(R.string.same_plan_selected), Toast.LENGTH_SHORT).show();
        } else {
            // If previous plan not in list, try to match by duration
            for (String plan : planNames) {
                if (plan.contains(getString(R.string.plan_1_month)) && previousPlanType.contains(getString(R.string.plan_1_month)) ||
                        plan.contains(getString(R.string.plan_3_months)) && previousPlanType.contains(getString(R.string.plan_3_months)) ||
                        plan.contains(getString(R.string.plan_6_months)) && previousPlanType.contains(getString(R.string.plan_6_months)) ||
                        plan.contains(getString(R.string.plan_1_year)) && previousPlanType.contains(getString(R.string.plan_1_year))) {

                    spinnerNewPlanType.setText(plan, false);

                    if (planFeeMap.containsKey(plan)) {
                        newPlanFee = planFeeMap.get(plan);
                        etNewPlanFee.setText(String.valueOf(newPlanFee));

                        if (planMonthMap.containsKey(plan)) {
                            calculateEndDate(planMonthMap.get(plan));
                        }
                    }
                    updateSummary();

                    Toast.makeText(this, getString(R.string.similar_plan_selected), Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            Toast.makeText(this, getString(R.string.previous_plan_not_available), Toast.LENGTH_SHORT).show();
        }
    }

    private void showDatePicker() {
        Calendar cal = Calendar.getInstance();
        DatePickerDialog picker = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    String date = String.format(Locale.getDefault(),
                            "%02d/%02d/%04d", dayOfMonth, month + 1, year);
                    etNewStartDate.setText(date);

                    // Recalculate end date
                    String planType = spinnerNewPlanType.getText().toString();
                    for (int i = 0; i < planTypes.length; i++) {
                        if (planTypes[i].equals(planType)) {
                            calculateEndDate(planMonths[i]);
                            break;
                        }
                    }
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH));

        picker.show();
    }

    private void calculateEndDate(int months) {
        String startDate = etNewStartDate.getText().toString().trim();
        if (TextUtils.isEmpty(startDate)) {
            // If start date is empty, set it first
            setDefaultStartDate();
            startDate = etNewStartDate.getText().toString().trim();
        }

        if (TextUtils.isEmpty(startDate)) return;

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Calendar cal = Calendar.getInstance();
            cal.setTime(sdf.parse(startDate));
            cal.add(Calendar.MONTH, months);
            // DON'T subtract 1 day if you want same day end
            // Example: Start 02/02, 1 month = 02/03

            etNewEndDate.setText(sdf.format(cal.getTime()));
        } catch (ParseException e) {
            Log.e(TAG, getString(R.string.date_calculation_error, e.getMessage()));
        }
    }

    private void updateTotalPayable() {
        int total = newPlanFee + outstandingBalance;
        tvSummaryNewPlan.setText(getString(R.string.rupee_prefix) + newPlanFee);
        tvTotalPayable.setText(getString(R.string.rupee_prefix) + total);
    }

    private void proceedToPayment() {
        String planType = spinnerNewPlanType.getText().toString().trim();
        String startDate = etNewStartDate.getText().toString().trim();
        String endDate = etNewEndDate.getText().toString().trim();
        String feeText = etNewPlanFee.getText().toString().trim();

        // Validation
        if (TextUtils.isEmpty(planType)) {
            spinnerNewPlanType.setError(getString(R.string.select_plan_type));
            Toast.makeText(this, getString(R.string.please_select_plan), Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(startDate)) {
            etNewStartDate.setError(getString(R.string.select_start_date));
            return;
        }

        if (TextUtils.isEmpty(feeText)) {
            etNewPlanFee.setError(getString(R.string.enter_fee_amount));
            return;
        }

        try {
            newPlanFee = Integer.parseInt(feeText);
            if (newPlanFee <= 0) {
                etNewPlanFee.setError(getString(R.string.invalid_fee));
                return;
            }
        } catch (NumberFormatException e) {
            etNewPlanFee.setError(getString(R.string.invalid_fee));
            return;
        }

        // Generate Plan ID
        String planId = generatePlanId(startDate, planType);
        int totalPayable = newPlanFee + outstandingBalance;

        // Navigate to Payment Activity
        Intent intent = new Intent(this, PaymentActivity.class);
        intent.putExtra("name", memberName);
        intent.putExtra("phone", memberPhone);
        intent.putExtra("email", memberEmail);
        intent.putExtra("gender", memberGender);
        intent.putExtra("joinDate", memberJoinDate);
        intent.putExtra("planType", planType);
        intent.putExtra("startDate", startDate);
        intent.putExtra("endDate", endDate);
        intent.putExtra("totalFee", totalPayable);
        intent.putExtra("planId", planId);
        intent.putExtra("isRenewal", true);
        intent.putExtra("outstandingBalance", outstandingBalance);

        startActivity(intent);
        finish();
    }

    private String generatePlanId(String startDate, String planType) {
        try {
            SimpleDateFormat monthYearFormat = new SimpleDateFormat("MMM-yyyy", Locale.US);
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date date = dateFormat.parse(startDate);

            String monthYear = monthYearFormat.format(date).toUpperCase();

            // Get plan code
            String planCode = "";
            if (planType.contains(getString(R.string.plan_1_month))) planCode = "1M";
            else if (planType.contains(getString(R.string.plan_3_months))) planCode = "3M";
            else if (planType.contains(getString(R.string.plan_6_months))) planCode = "6M";
            else if (planType.contains(getString(R.string.plan_1_year))) planCode = "1Y";
            else {
                // Extract number from plan name
                planCode = planType.replaceAll("[^0-9]", "") + "M";
            }

            // Add sequence number (like -01, -02)
            String planId = monthYear + "-" + planCode + "-01";

            // Check if plan ID already exists and increment if needed
            DatabaseReference paymentsRef = FirebaseDatabase.getInstance()
                    .getReference("GYM")
                    .child(ownerEmail)
                    .child("members")
                    .child(memberPhone)
                    .child("payments");

            // Note: For simplicity, using -01. You might need to query existing plans
            // and increment the sequence number if same month-year-planCode exists

            return planId;

        } catch (ParseException e) {
            Log.e(TAG, getString(R.string.plan_id_generation_error, e.getMessage()));
            return getString(R.string.plan_prefix) + System.currentTimeMillis();
        }
    }

    private void showProgress() {
        progressBar.setVisibility(View.VISIBLE);
        btnProceedToPayment.setEnabled(false);
        btnQuickRenew.setEnabled(false);
    }

    private void hideProgress() {
        progressBar.setVisibility(View.GONE);
        btnProceedToPayment.setEnabled(true);
        btnQuickRenew.setEnabled(true);
    }
}