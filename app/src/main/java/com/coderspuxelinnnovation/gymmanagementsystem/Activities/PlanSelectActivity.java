package com.coderspuxelinnnovation.gymmanagementsystem.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import android.widget.ArrayAdapter;

import com.coderspuxelinnnovation.gymmanagementsystem.R;
import com.coderspuxelinnnovation.gymmanagementsystem.base.BaseActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class PlanSelectActivity extends BaseActivity {

    private MaterialAutoCompleteTextView spinnerPlan;
    private TextInputEditText etTotalFee, etStartDate, etEndDate;
    private MaterialButton btnNext;
    private String planId;

    private final String[] plans = {"1 Month", "3 Months", "6 Months", "12 Months"};
    private final int[] fees = {1000, 2500, 5000, 9000};
    private final int[] durations = {1, 3, 6, 12};

    // Member data
    private String name, phone, email, gender, joinDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plan_select);

        initViews();
        getIntentData();
        setupPlanSpinner();
        setupClickListeners();
        setInitialDates();
    }
    private String generatePlanId(String startDate, String planType) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Calendar cal = Calendar.getInstance();
            cal.setTime(sdf.parse(startDate));

            String month = new SimpleDateFormat("MMM", Locale.getDefault())
                    .format(cal.getTime()).toUpperCase();

            int year = cal.get(Calendar.YEAR);

            // "1 Month" → "1M"
            String duration = planType.split(" ")[0] + "M";

            return month + "-" + year + "-" + duration;
        } catch (Exception e) {
            return "PLAN";
        }
    }

    private void initViews() {
        spinnerPlan = findViewById(R.id.spinnerPlan);
        etTotalFee = findViewById(R.id.etTotalFee);
        etStartDate = findViewById(R.id.etStartDate);
        etEndDate = findViewById(R.id.etEndDate);
        btnNext = findViewById(R.id.btnNext);
    }

    private void getIntentData() {
        name = getIntent().getStringExtra("name");
        phone = getIntent().getStringExtra("phone");
        email = getIntent().getStringExtra("email");
        gender = getIntent().getStringExtra("gender");
        joinDate = getIntent().getStringExtra("joinDate");
    }

    private void setupPlanSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, plans);
        spinnerPlan.setAdapter(adapter);

        spinnerPlan.setOnItemClickListener((parent, view, position, id) -> {
            updatePlanDetails(position);
        });
    }

    private void setupClickListeners() {
        btnNext.setOnClickListener(v -> validateAndProceed());
    }

    private void setInitialDates() {
        if (joinDate != null) {
            etStartDate.setText(joinDate);
            etEndDate.setText("Select Plan");
        }
    }

    private void updatePlanDetails(int position) {
        if (position >= 0 && position < fees.length) {
            etTotalFee.setText("₹" + fees[position]);
            calculateEndDate(durations[position]);
        }
    }

    private void calculateEndDate(int months) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Calendar cal = Calendar.getInstance();
            cal.setTime(sdf.parse(joinDate));
            cal.add(Calendar.MONTH, months);
            etEndDate.setText(sdf.format(cal.getTime()));
        } catch (ParseException e) {
            etEndDate.setText("Invalid Date");
        }
    }

    private void validateAndProceed() {
        String selectedPlan = spinnerPlan.getText().toString().trim();

        if (TextUtils.isEmpty(selectedPlan)) {
            spinnerPlan.setError("Please select a plan");
            return;
        }

        Intent intent = new Intent(this, PaymentActivity.class);
        intent.putExtra("name", name);
        intent.putExtra("phone", phone);
        intent.putExtra("email", email);
        intent.putExtra("gender", gender);
        intent.putExtra("joinDate", joinDate);
        intent.putExtra("planType", selectedPlan);
        intent.putExtra("totalFee", extractFeeAmount());
        intent.putExtra("startDate", etStartDate.getText().toString());
        intent.putExtra("endDate", etEndDate.getText().toString());
        String generatedPlanId = generatePlanId(
                etStartDate.getText().toString(),
                selectedPlan
        );

        intent.putExtra("planId", generatedPlanId);

        startActivity(intent);
    }

    private int extractFeeAmount() {
        String feeText = etTotalFee.getText().toString().replace("₹", "").trim();
        try {
            return Integer.parseInt(feeText);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
