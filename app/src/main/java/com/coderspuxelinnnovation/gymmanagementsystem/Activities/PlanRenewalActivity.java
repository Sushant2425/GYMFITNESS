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

        initViews();
        loadIntentData();
        setupPlanSpinner();
        loadMemberData();
        setupListeners();
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
            Toast.makeText(this, "Invalid member data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ownerEmail = new PrefManager(this).getUserEmail().replace(".", ",");
        memberRef = FirebaseDatabase.getInstance()
                .getReference("GYM")
                .child(ownerEmail)
                .child("members")
                .child(memberPhone);

        tvMemberName.setText(memberName != null ? memberName : "Member");
        tvMemberPhone.setText("Phone: +91 " + memberPhone);
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

                for (DataSnapshot snap : snapshot.getChildren()) {
                    Plan plan = snap.getValue(Plan.class);
                    if (plan != null) {
                        planNames.add(plan.getPlanName());
                        planFeeMap.put(plan.getPlanName(), plan.getFee());
                        planMonthMap.put(plan.getPlanName(), plan.getDuration());
                    }
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        PlanRenewalActivity.this,
                        android.R.layout.simple_dropdown_item_1line,
                        planNames
                );
                spinnerNewPlanType.setAdapter(adapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(PlanRenewalActivity.this,
                        "Failed to load plans", Toast.LENGTH_SHORT).show();
            }
        });

        spinnerNewPlanType.setOnItemClickListener((parent, view, position, id) -> {
            String selectedPlan = planNames.get(position);

            int fee = planFeeMap.get(selectedPlan);
            int months = planMonthMap.get(selectedPlan);

            newPlanFee = fee;
            etNewPlanFee.setText(String.valueOf(fee));

            calculateEndDate(months);
            updateSummary();
        });
    }
    private void updateSummary() {
        // Show new plan fee
        tvSummaryNewPlan.setText("₹" + newPlanFee);

        // Show previous due if any
        if (outstandingBalance > 0) {
            layoutPreviousDue.setVisibility(View.VISIBLE);
            tvSummaryPreviousDue.setText("₹" + outstandingBalance);
        } else {
            layoutPreviousDue.setVisibility(View.GONE);
        }

        // Calculate total payable = new plan fee + old due
        int total = newPlanFee + outstandingBalance;
        tvTotalPayable.setText("₹" + total);
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
                            "Member not found", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                hideProgress();
                Toast.makeText(PlanRenewalActivity.this,
                        "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
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
            tvPreviousPlanType.setText(previousPlanType != null ? previousPlanType : "N/A");
            tvPreviousStartDate.setText(previousStartDate != null ? previousStartDate : "N/A");
            tvPreviousEndDate.setText(previousEndDate != null ? previousEndDate : "N/A");
            tvPreviousFee.setText("₹" + previousTotalFee);
            tvPlanStatus.setText("Plan Status: " + (status != null ? status : "EXPIRED"));

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
            tvOutstandingBalance.setText("₹" + outstandingBalance);
            layoutPreviousDue.setVisibility(View.VISIBLE);
            tvSummaryPreviousDue.setText("₹" + outstandingBalance);
            updateTotalPayable();
        } else {
            cardOutstandingBalance.setVisibility(View.GONE);
            layoutPreviousDue.setVisibility(View.GONE);
        }
    }

    private void setDefaultStartDate() {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        // If previous end date exists, start from next day
        if (previousEndDate != null) {
            try {
                cal.setTime(sdf.parse(previousEndDate));
                cal.add(Calendar.DAY_OF_MONTH, 1);
            } catch (ParseException e) {
                Log.e(TAG, "Date parse error: " + e.getMessage());
            }
        }

        etNewStartDate.setText(sdf.format(cal.getTime()));
    }

    private void setupListeners() {
        // Quick Renew Button
        btnQuickRenew.setOnClickListener(v -> quickRenewSamePlan());

        // Start Date Picker
        etNewStartDate.setOnClickListener(v -> showDatePicker());

        // Plan Type Selection
        spinnerNewPlanType.setOnItemClickListener((parent, view, position, id) -> {
            newPlanFee = planFees[position];
            etNewPlanFee.setText(String.valueOf(newPlanFee));
            calculateEndDate(planMonths[position]);
            updateTotalPayable();
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
                        updateTotalPayable();
                    }
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Invalid fee: " + e.getMessage());
                }
            }
        });

        // Proceed to Payment
        btnProceedToPayment.setOnClickListener(v -> proceedToPayment());
    }

    private void quickRenewSamePlan() {
        if (previousPlanType == null) {
            Toast.makeText(this, "No previous plan found", Toast.LENGTH_SHORT).show();
            return;
        }

        // Find plan index
        int index = -1;
        for (int i = 0; i < planTypes.length; i++) {
            if (planTypes[i].equals(previousPlanType)) {
                index = i;
                break;
            }
        }

        if (index != -1) {
            spinnerNewPlanType.setText(planTypes[index], false);
            newPlanFee = previousTotalFee; // Use previous fee
            etNewPlanFee.setText(String.valueOf(newPlanFee));
            calculateEndDate(planMonths[index]);
            updateTotalPayable();

            Toast.makeText(this, "✅ Same plan selected", Toast.LENGTH_SHORT).show();
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
        if (TextUtils.isEmpty(startDate)) return;

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Calendar cal = Calendar.getInstance();
            cal.setTime(sdf.parse(startDate));
            cal.add(Calendar.MONTH, months);
            cal.add(Calendar.DAY_OF_MONTH, -1); // Subtract 1 day for proper end date

            etNewEndDate.setText(sdf.format(cal.getTime()));
        } catch (ParseException e) {
            Log.e(TAG, "Date calculation error: " + e.getMessage());
        }
    }

    private void updateTotalPayable() {
        int total = newPlanFee + outstandingBalance;
        tvSummaryNewPlan.setText("₹" + newPlanFee);
        tvTotalPayable.setText("₹" + total);
    }

    private void proceedToPayment() {
        String planType = spinnerNewPlanType.getText().toString().trim();
        String startDate = etNewStartDate.getText().toString().trim();
        String endDate = etNewEndDate.getText().toString().trim();
        String feeText = etNewPlanFee.getText().toString().trim();

        // Validation
        if (TextUtils.isEmpty(planType)) {
            spinnerNewPlanType.setError("Select plan type");
            Toast.makeText(this, "Please select a plan", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(startDate)) {
            etNewStartDate.setError("Select start date");
            return;
        }

        if (TextUtils.isEmpty(feeText)) {
            etNewPlanFee.setError("Enter fee amount");
            return;
        }

        try {
            newPlanFee = Integer.parseInt(feeText);
            if (newPlanFee <= 0) {
                etNewPlanFee.setError("Invalid fee");
                return;
            }
        } catch (NumberFormatException e) {
            etNewPlanFee.setError("Invalid fee");
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
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Calendar cal = Calendar.getInstance();
            cal.setTime(sdf.parse(startDate));

            String month = new SimpleDateFormat("MMM", Locale.getDefault())
                    .format(cal.getTime()).toUpperCase();
            int year = cal.get(Calendar.YEAR);

            String planCode = "";
            if (planType.contains("1 Month")) planCode = "1M";
            else if (planType.contains("3 Months")) planCode = "3M";
            else if (planType.contains("6 Months")) planCode = "6M";
            else if (planType.contains("1 Year")) planCode = "1Y";

            return month + "-" + year + "-" + planCode;

        } catch (ParseException e) {
            Log.e(TAG, "Plan ID generation error: " + e.getMessage());
            return "PLAN-" + System.currentTimeMillis();
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