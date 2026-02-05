package com.coderspuxelinnnovation.gymmanagementsystem.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.coderspuxelinnnovation.gymmanagementsystem.R;
import com.coderspuxelinnnovation.gymmanagementsystem.Utils.PrefManager;
import com.coderspuxelinnnovation.gymmanagementsystem.adapters.AllPaymentsAdapter;
import com.coderspuxelinnnovation.gymmanagementsystem.models.MemberModel;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AllPaymentsActivity extends AppCompatActivity {

    private RecyclerView rvAllPayments;
    private ProgressBar progressBar;
    private TextView tvNoPayments, tvTotalAmount;
    private String memberPhone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_payments);

        memberPhone = getIntent().getStringExtra("phone");
        if (memberPhone == null) {
            Toast.makeText(this, "Invalid member", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        loadAllPayments();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("All Payments");
        }

        rvAllPayments = findViewById(R.id.rvAllPayments);
        progressBar = findViewById(R.id.progressBar);
        tvNoPayments = findViewById(R.id.tvNoPayments);
        tvTotalAmount = findViewById(R.id.tvTotalAmount);

        rvAllPayments.setLayoutManager(new LinearLayoutManager(this));
    }

    private void loadAllPayments() {
        progressBar.setVisibility(View.VISIBLE);
        String ownerEmail = new PrefManager(this).getUserEmail().replace(".", ",");

        DatabaseReference memberRef = FirebaseDatabase.getInstance()
                .getReference("GYM")
                .child(ownerEmail)
                .child("members")
                .child(memberPhone);

        memberRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                progressBar.setVisibility(View.GONE);

                if (!snapshot.exists()) {
                    Toast.makeText(AllPaymentsActivity.this,
                            "Member not found", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                try {
                    List<PaymentItem> allPaymentItems = new ArrayList<>();
                    int totalPaid = 0;

                    // SKIP CURRENT PLAN - DON'T SHOW IT
                    // We only want to show plans from payments node

                    // Read payments MANUALLY
                    DataSnapshot paymentsSnap = snapshot.child("payments");
                    if (paymentsSnap.exists()) {
                        for (DataSnapshot planSnap : paymentsSnap.getChildren()) {
                            try {
                                String planId = planSnap.getKey();

                                // Get plan details
                                String planStartDate = planSnap.child("planStartDate").getValue(String.class);
                                String forMonth = planSnap.child("forMonth").getValue(String.class);
                                String planStatus = planSnap.child("status").getValue(String.class);

                                // Get amounts
                                Integer totalFee = planSnap.child("totalFee").getValue(Integer.class);
                                Integer amountPaid = planSnap.child("amountPaid").getValue(Integer.class);

                                // Log for debugging
                                Log.d("AllPayments", "Payment Plan: " + planId +
                                        " AmountPaid: " + amountPaid + " TotalFee: " + totalFee);

                                if (planStartDate == null) planStartDate = "";
                                if (forMonth == null) forMonth = "";
                                if (planStatus == null) planStatus = "";
                                if (totalFee == null) totalFee = 0;
                                if (amountPaid == null) amountPaid = 0;

                                String monthYear = formatMonthYear(planId);
                                if (monthYear.equals("Unknown")) {
                                    // Try to get from forMonth field
                                    monthYear = formatForMonth(forMonth);
                                }

                                // Check if this is current active plan
                                DataSnapshot currentPlanSnap = snapshot.child("currentPlan");
                                boolean isCurrent = false;
                                if (currentPlanSnap.exists()) {
                                    String currentPlanId = currentPlanSnap.child("planId").getValue(String.class);
                                    if (planId != null && planId.equals(currentPlanId)) {
                                        isCurrent = true;
                                    }
                                }

                                String title;
                                if (isCurrent) {
                                    title = "Current: " + monthYear;
                                } else {
                                    title = monthYear;
                                }

                                PaymentItem paymentItem = new PaymentItem(
                                        planId,
                                        title,
                                        planStartDate,
                                        amountPaid,
                                        totalFee,
                                        planStatus,
                                        isCurrent  // Pass whether it's current or not
                                );
                                allPaymentItems.add(paymentItem);
                                totalPaid += amountPaid;

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    // Update UI - Show TOTAL PAID
                    tvTotalAmount.setText("Total Paid: ₹" + totalPaid);
                    Log.d("AllPayments", "Total Paid: " + totalPaid);

                    if (allPaymentItems.isEmpty()) {
                        tvNoPayments.setVisibility(View.VISIBLE);
                        rvAllPayments.setVisibility(View.GONE);
                    } else {
                        tvNoPayments.setVisibility(View.GONE);
                        rvAllPayments.setVisibility(View.VISIBLE);

                        // Log each item
                        for (PaymentItem item : allPaymentItems) {
                            Log.d("AllPayments", "Item: " + item.getTitle() +
                                    " Paid: " + item.getAmountPaid() +
                                    " Total: " + item.getTotalFee() +
                                    " IsCurrent: " + item.isCurrentPlan());
                        }

                        AllPaymentsAdapter adapter = new AllPaymentsAdapter(allPaymentItems,
                                new AllPaymentsAdapter.OnPaymentClickListener() {
                                    @Override
                                    public void onPaymentClick(PaymentItem paymentItem) {
                                        if (!paymentItem.isCurrentPlan()) {
                                            Intent intent = new Intent(AllPaymentsActivity.this,
                                                    PaymentHistoryActivity.class);
                                            intent.putExtra("phone", memberPhone);
                                            intent.putExtra("planId", paymentItem.getPlanId());
                                            startActivity(intent);
                                        } else {
                                            Toast.makeText(AllPaymentsActivity.this,
                                                    "View current plan in member details",
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                        rvAllPayments.setAdapter(adapter);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(AllPaymentsActivity.this,
                            "Error loading payments: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    tvNoPayments.setVisibility(View.VISIBLE);
                    Log.e("AllPayments", "Error: " + e.getMessage());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(AllPaymentsActivity.this,
                        "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("AllPayments", "Database error: " + error.getMessage());
            }
        });
    }
    // Add this helper method
    private String formatForMonth(String forMonth) {
        if (forMonth == null || forMonth.isEmpty()) return "Unknown";

        try {
            // forMonth format: "2026-01"
            String[] parts = forMonth.split("-");
            if (parts.length >= 2) {
                int year = Integer.parseInt(parts[0]);
                int month = Integer.parseInt(parts[1]);

                String[] monthNames = {"January", "February", "March", "April", "May", "June",
                        "July", "August", "September", "October", "November", "December"};

                if (month >= 1 && month <= 12) {
                    return monthNames[month-1] + " " + year;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return forMonth;
    }
    private String formatMonthYear(String planId) {
        if (planId == null || planId.isEmpty()) return "Unknown";

        try {
            String[] parts = planId.split("-");
            if (parts.length >= 2) {
                String monthCode = parts[0].toUpperCase();
                String year = parts[1];

                String monthName = getMonthName(monthCode);
                return monthName + " " + year;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return planId;
    }

    private String getMonthName(String code) {
        switch (code) {
            case "JAN":
                return "January";
            case "FEB":
                return "February";
            case "MAR":
                return "March";
            case "APR":
                return "April";
            case "MAY":
                return "May";
            case "JUN":
                return "June";
            case "JUL":
                return "July";
            case "AUG":
                return "August";
            case "SEP":
                return "September";
            case "OCT":
                return "October";
            case "NOV":
                return "November";
            case "DEC":
                return "December";
            default:
                return code;
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    // Model class for payment items
    // Model class for payment items
    public static class PaymentItem {
        private String planId;
        private String title;
        private String date;
        private int amountPaid;  // Actual amount paid
        private int totalFee;    // Total fee for the plan
        private String status;
        private boolean isCurrentPlan;

        // Constructor with 7 parameters (including totalFee)
        public PaymentItem(String planId, String title, String date,
                           int amountPaid, int totalFee, String status, boolean isCurrentPlan) {
            this.planId = planId;
            this.title = title;
            this.date = date;
            this.amountPaid = amountPaid;
            this.totalFee = totalFee;
            this.status = status;
            this.isCurrentPlan = isCurrentPlan;
        }

        // Keep the old constructor for backward compatibility (or remove if not needed)
        public PaymentItem(String planId, String title, String date,
                           int amount, String status, boolean isCurrentPlan) {
            this.planId = planId;
            this.title = title;
            this.date = date;
            this.amountPaid = amount; // Map amount to amountPaid
            this.totalFee = amount;   // For old calls, totalFee = amount
            this.status = status;
            this.isCurrentPlan = isCurrentPlan;
        }

        // Getters
        public String getPlanId() {
            return planId;
        }

        public String getTitle() {
            return title;
        }

        public String getDate() {
            return date;
        }

        public int getAmountPaid() {
            return amountPaid;
        }

        public int getTotalFee() {
            return totalFee;
        }

        public String getStatus() {
            return status;
        }

        public boolean isCurrentPlan() {
            return isCurrentPlan;
        }

        // For backward compatibility - getAmount() returns amountPaid
        public int getAmount() {
            return amountPaid;
        }
    }
}