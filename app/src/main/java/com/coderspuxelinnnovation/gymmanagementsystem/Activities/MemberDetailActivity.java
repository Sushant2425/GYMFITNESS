package com.coderspuxelinnnovation.gymmanagementsystem.Activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

import com.coderspuxelinnnovation.gymmanagementsystem.R;
import com.coderspuxelinnnovation.gymmanagementsystem.Utils.PrefManager;
import com.coderspuxelinnnovation.gymmanagementsystem.base.BaseActivity;
import com.coderspuxelinnnovation.gymmanagementsystem.models.MemberModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MemberDetailActivity extends BaseActivity {

    private TextView tvName, tvPhone, tvEmail, tvGender, tvJoinDate, tvPlanType;
    private TextView tvTotalFee, tvStartDate, tvEndDate, tvPlanStatus;
    private MaterialButton btnEdit, btnDelete, btnViewPayments;
    private ProgressBar progressBar;
    private CardView cvStatusBadge;
    private ValueEventListener memberListener;
    private DatabaseReference memberRef;
    private TextView tvAmountPaid, tvRemaining, tvProgressPercentage;
    private LinearProgressIndicator progressPayment;
    private String memberPhone;
    private MemberModel member;
    private LinearLayout llOldPlansContainer;
    private TextView tvNoOldPlans, tvOldPlansCount;
    private CardView cardOldPlans;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_member_detail);

        memberPhone = getIntent().getStringExtra("phone");
        if (memberPhone == null) {
            Toast.makeText(this, "Invalid member", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupToolbar();
        loadMemberDetails();
    }

    private void initViews() {
        tvName = findViewById(R.id.tvName);
        tvPhone = findViewById(R.id.tvPhone);
        tvEmail = findViewById(R.id.tvEmail);
        tvGender = findViewById(R.id.tvGender);
        tvJoinDate = findViewById(R.id.tvJoinDate);
        tvPlanType = findViewById(R.id.tvPlanType);
        tvTotalFee = findViewById(R.id.tvTotalFee);
        tvStartDate = findViewById(R.id.tvStartDate);
        tvEndDate = findViewById(R.id.tvEndDate);
        tvPlanStatus = findViewById(R.id.tvPlanStatus);
        cvStatusBadge = findViewById(R.id.cvStatusBadge);
        btnEdit = findViewById(R.id.btnEdit);
        btnDelete = findViewById(R.id.btnDelete);
        btnViewPayments = findViewById(R.id.btnViewPayments);
        progressBar = findViewById(R.id.progressBar);


        // Add these lines
        tvAmountPaid = findViewById(R.id.tvAmountPaid);
        tvRemaining = findViewById(R.id.tvRemaining);
        tvProgressPercentage = findViewById(R.id.tvProgressPercentage);
        progressPayment = findViewById(R.id.progressPayment);


        llOldPlansContainer = findViewById(R.id.llOldPlansContainer);
        tvNoOldPlans = findViewById(R.id.tvNoOldPlans);
        tvOldPlansCount = findViewById(R.id.tvOldPlansCount);
        cardOldPlans = findViewById(R.id.cardOldPlans);
        btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(MemberDetailActivity.this, MemberEditActivity.class);
            intent.putExtra("phone", memberPhone);
            startActivity(intent);
        });

        btnDelete.setOnClickListener(v -> showDeleteConfirmDialog());
        btnViewPayments.setOnClickListener(v -> viewAllPayments()); // Change this

    }
    private void viewAllPayments() {
        Intent intent = new Intent(MemberDetailActivity.this, AllPaymentsActivity.class);
        intent.putExtra("phone", memberPhone);
        startActivity(intent);
    }
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void loadMemberDetails() {
        progressBar.setVisibility(View.VISIBLE);
        String ownerEmail = new PrefManager(this).getUserEmail().replace(".", ",");

        memberRef = FirebaseDatabase.getInstance()
                .getReference("GYM")
                .child(ownerEmail)
                .child("members")
                .child(memberPhone);

        memberRef.addValueEventListener(memberListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                progressBar.setVisibility(View.GONE);

                if (!snapshot.exists()) {
                    Toast.makeText(MemberDetailActivity.this,
                            "Member not found", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                member = snapshot.getValue(MemberModel.class);
                if (member != null) {
                    member.setPhone(memberPhone);
                    bindData(member);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(MemberDetailActivity.this,
                        "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void bindData(MemberModel member) {
        // Info
        tvName.setText(member.getInfo().getName());
        tvPhone.setText(member.getPhone());
        tvEmail.setText(member.getInfo().getEmail());
        tvGender.setText(member.getInfo().getGender());
        tvJoinDate.setText("Joined: " + member.getInfo().getJoinDate());

        // Plan
        if (member.getCurrentPlan() != null) {
            tvPlanType.setText(member.getCurrentPlan().getPlanType());
            tvTotalFee.setText("₹" + member.getCurrentPlan().getTotalFee());
            tvStartDate.setText(member.getCurrentPlan().getStartDate());
            tvEndDate.setText(member.getCurrentPlan().getEndDate());

            calculatePaymentProgress(member);

            // Check and update status
            updatePlanStatus(member);
        }
        loadOldPlans(member);

    }

    private void calculatePaymentProgress(MemberModel member) {
        if (member.getCurrentPlan() == null) return;

        int totalFee = member.getCurrentPlan().getTotalFee();
        int amountPaid = 0;
        int remaining = totalFee;

        // Check if current plan exists in payments
        if (member.getPayments() != null && member.getCurrentPlan().getPlanId() != null) {
            MemberModel.PaymentPlan currentPaymentPlan = member.getPayments().get(member.getCurrentPlan().getPlanId());

            if (currentPaymentPlan != null) {
                amountPaid = currentPaymentPlan.getAmountPaid();
                remaining = currentPaymentPlan.getRemaining();
            }
        }

        // Update UI
        tvAmountPaid.setText("₹" + amountPaid);
        tvRemaining.setText("₹" + remaining);

        // Calculate percentage
        int percentage = 0;
        if (totalFee > 0) {
            percentage = (amountPaid * 100) / totalFee;
        }

        tvProgressPercentage.setText(percentage + "%");
        progressPayment.setProgress(percentage);

        // Change progress color based on payment status
        if (percentage == 100) {
            progressPayment.setIndicatorColor(Color.parseColor("#4CAF50")); // Green
        } else if (percentage >= 50) {
            progressPayment.setIndicatorColor(Color.parseColor("#FF9800")); // Orange
        } else {
            progressPayment.setIndicatorColor(Color.parseColor("#F44336")); // Red
        }
    }
    private void loadOldPlans(MemberModel member) {
        if (member.getPayments() == null || member.getPayments().isEmpty()) {
            cardOldPlans.setVisibility(View.GONE);
            return;
        }

        cardOldPlans.setVisibility(View.VISIBLE);
        llOldPlansContainer.removeAllViews();

        // Filter out current plan from old plans
        List<MemberModel.PaymentPlan> oldPlans = new ArrayList<>();
        for (Map.Entry<String, MemberModel.PaymentPlan> entry : member.getPayments().entrySet()) {
            MemberModel.PaymentPlan plan = entry.getValue();

            // Skip current plan
            if (member.getCurrentPlan() != null &&
                    plan.getPlanId() != null &&
                    plan.getPlanId().equals(member.getCurrentPlan().getPlanId())) {
                continue;
            }

            oldPlans.add(plan);
        }

        // Update count
        tvOldPlansCount.setText("(" + oldPlans.size() + ")");

        if (oldPlans.isEmpty()) {
            tvNoOldPlans.setVisibility(View.VISIBLE);
            llOldPlansContainer.setVisibility(View.GONE);
            return;
        }

        tvNoOldPlans.setVisibility(View.GONE);
        llOldPlansContainer.setVisibility(View.VISIBLE);

        // Sort plans by date (most recent first)
        oldPlans.sort((o1, o2) -> Long.compare(o2.getDate(), o1.getDate()));

        // Add old plans to layout
        for (MemberModel.PaymentPlan plan : oldPlans) {
            addOldPlanView(plan);
        }
    }

    private void addOldPlanView(MemberModel.PaymentPlan plan) {
        if (plan == null) return;

        View planView = getLayoutInflater().inflate(R.layout.item_old_plan, null);

        try {
            // Initialize views
            TextView tvPlanId = planView.findViewById(R.id.tvPlanId);
            TextView tvPlanDates = planView.findViewById(R.id.tvPlanDates); // Correct ID
            TextView tvTotalFee = planView.findViewById(R.id.tvTotalFee);
            TextView tvAmountPaid = planView.findViewById(R.id.tvAmountPaid);
            TextView tvPaymentMode = planView.findViewById(R.id.tvPaymentMode);
            TextView tvPlanStatus = planView.findViewById(R.id.tvPlanStatus);
            CardView cvPlanStatus = planView.findViewById(R.id.cvPlanStatus);

            // Format and set data
            String monthYear = formatPlanIdToMonthYear(plan.getPlanId());
            tvPlanId.setText(monthYear);

            String dates = formatPlanDates(plan);
            tvPlanDates.setText(dates);

            tvTotalFee.setText(formatCurrency(plan.getTotalFee()));
            tvAmountPaid.setText(formatCurrency(plan.getAmountPaid()));
            tvPaymentMode.setText(getPaymentModeText(plan.getMode()));
            tvPlanStatus.setText(getStatusText(plan.getStatus()));

            // Set status styling
            setStatusStyling(tvPlanStatus, cvPlanStatus, plan.getStatus());

            // Add click listener
            planView.setOnClickListener(v -> openPaymentHistory(plan));

            // Add to container
            llOldPlansContainer.addView(planView);

        } catch (Exception e) {
            Log.e("MemberDetailActivity", "Error adding old plan view: " + e.getMessage());
            // Don't add corrupted view
        }
    }

    // Helper method to format plan ID to Month Year
    private String formatPlanIdToMonthYear(String planId) {
        if (TextUtils.isEmpty(planId)) {
            return "Unknown Month";
        }

        try {
            // Expected format: "DEC-2025-1M-01"
            String[] parts = planId.split("-");
            if (parts.length >= 2) {
                String monthCode = parts[0].toUpperCase();
                String year = parts[1];

                Map<String, String> monthMap = new HashMap<String, String>() {{
                    put("JAN", "January");
                    put("FEB", "February");
                    put("MAR", "March");
                    put("APR", "April");
                    put("MAY", "May");
                    put("JUN", "June");
                    put("JUL", "July");
                    put("AUG", "August");
                    put("SEP", "September");
                    put("OCT", "October");
                    put("NOV", "November");
                    put("DEC", "December");
                }};

                String monthName = monthMap.get(monthCode);
                if (monthName != null) {
                    return monthName + " " + year;
                } else {
                    return monthCode + " " + year;
                }
            }
        } catch (Exception e) {
            Log.w("MemberDetailActivity", "Failed to parse plan ID: " + planId);
        }

        return planId; // Fallback to original ID
    }

    // Helper method to format plan dates
    private String formatPlanDates(MemberModel.PaymentPlan plan) {
        if (plan == null) return "N/A";

        String startDate = plan.getPlanStartDate();
        String endDate = plan.getPlanEndDate();

        if (TextUtils.isEmpty(startDate) && TextUtils.isEmpty(endDate)) {
            return "N/A";
        }

        if (TextUtils.isEmpty(endDate)) {
            return startDate; // Just show start date if end date is missing
        }

        try {
            // Format: "01/12 - 31/12/2025"
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date start = sdf.parse(startDate);
            Date end = sdf.parse(endDate);

            Calendar calStart = Calendar.getInstance();
            calStart.setTime(start);
            Calendar calEnd = Calendar.getInstance();
            calEnd.setTime(end);

            // If same month and year, show: "01 - 31 December 2025"
            if (calStart.get(Calendar.MONTH) == calEnd.get(Calendar.MONTH) &&
                    calStart.get(Calendar.YEAR) == calEnd.get(Calendar.YEAR)) {

                SimpleDateFormat dayFormat = new SimpleDateFormat("dd", Locale.getDefault());
                SimpleDateFormat monthYearFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());

                return dayFormat.format(start) + " - " +
                        dayFormat.format(end) + " " +
                        monthYearFormat.format(start);
            } else {
                // Different months: "01/12/2025 - 31/01/2026"
                return startDate + " - " + endDate;
            }
        } catch (Exception e) {
            Log.w("MemberDetailActivity", "Failed to parse dates");
            return startDate + " - " + endDate;
        }
    }

    // Helper method for currency formatting
    private String formatCurrency(int amount) {
        return "₹" + amount;
    }

    // Helper method for payment mode
    private String getPaymentModeText(String mode) {
        return !TextUtils.isEmpty(mode) ? mode : "N/A";
    }

    // Helper method for status text
    private String getStatusText(String status) {
        return !TextUtils.isEmpty(status) ? status : "UNKNOWN";
    }

    // Helper method to set status styling
    private void setStatusStyling(TextView statusTextView, CardView statusCardView, String status) {
        if (status == null) return;

        int textColor;
        int bgColor;

        switch (status.toUpperCase()) {
            case "PAID":
                textColor = Color.parseColor("#4CAF50"); // Green
                bgColor = Color.parseColor("#E8F5E9"); // Light Green
                break;
            case "PENDING":
                textColor = Color.parseColor("#FF9800"); // Orange
                bgColor = Color.parseColor("#FFF3E0"); // Light Orange
                break;
            case "ACTIVE":
                textColor = Color.parseColor("#2196F3"); // Blue
                bgColor = Color.parseColor("#E3F2FD"); // Light Blue
                break;
            case "EXPIRED":
                textColor = Color.parseColor("#F44336"); // Red
                bgColor = Color.parseColor("#FFEBEE"); // Light Red
                break;
            default:
                textColor = Color.parseColor("#757575"); // Gray
                bgColor = Color.parseColor("#F5F5F5"); // Light Gray
                break;
        }

        statusTextView.setTextColor(textColor);
        statusCardView.setCardBackgroundColor(bgColor);
    }

    // Helper method to open payment history
    private void openPaymentHistory(MemberModel.PaymentPlan plan) {
        if (plan == null || TextUtils.isEmpty(plan.getPlanId())) {
            Toast.makeText(this, "Plan ID not available", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, PaymentHistoryActivity.class);
        intent.putExtra("phone", memberPhone);
        intent.putExtra("planId", plan.getPlanId());
        startActivity(intent);
    }    private void updatePlanStatus(MemberModel member) {
        if (member.getCurrentPlan() == null) return;

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        try {
            Date endDate = sdf.parse(member.getCurrentPlan().getEndDate());
            Date today = new Date();

            boolean isExpired = endDate != null && endDate.before(today);
            boolean isExpiringSoon = isExpiringSoon(endDate);

            if (isExpired) {
                // Update to EXPIRED
                setStatusUI("EXPIRED", "#F44336", "#FFEBEE");
                updateStatusInFirebase("EXPIRED");
            } else if (isExpiringSoon) {
                // Show Expiring Soon
                setStatusUI("Expiring Soon", "#FF9800", "#FFF3E0");
            } else {
                // Active
                setStatusUI("ACTIVE", "#4CAF50", "#E8F5E9");
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Default to showing current status
            String status = member.getCurrentPlan().getStatus();
            if ("ACTIVE".equals(status)) {
                setStatusUI("ACTIVE", "#4CAF50", "#E8F5E9");
            } else {
                setStatusUI("EXPIRED", "#F44336", "#FFEBEE");
            }
        }
    }

    private boolean isExpiringSoon(Date endDate) {
        if (endDate == null) return false;

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, 7); // Next 7 days
        Date weekFromNow = cal.getTime();

        Date today = new Date();
        return endDate.after(today) && endDate.before(weekFromNow);
    }

    private void setStatusUI(String statusText, String textColor, String bgColor) {
        tvPlanStatus.setText(statusText);
        tvPlanStatus.setTextColor(Color.parseColor(textColor));
        cvStatusBadge.setCardBackgroundColor(Color.parseColor(bgColor));
    }

    private void updateStatusInFirebase(String newStatus) {
        String ownerEmail = new PrefManager(this).getUserEmail().replace(".", ",");
        DatabaseReference planRef = FirebaseDatabase.getInstance()
                .getReference("GYM")
                .child(ownerEmail)
                .child("members")
                .child(memberPhone)
                .child("currentPlan")
                .child("status");

        planRef.setValue(newStatus)
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(this, "Plan status updated!", Toast.LENGTH_SHORT).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to update status", Toast.LENGTH_SHORT).show()
                );
    }


    private void showDeleteConfirmDialog() {
        if (member == null || member.getInfo() == null) {
            Toast.makeText(this, "Loading member data...", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Delete Member")
                .setMessage("Are you sure you want to delete " + member.getInfo().getName() + "?")
                .setPositiveButton("Delete", (dialog, which) -> deleteMember())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteMember() {
        progressBar.setVisibility(View.VISIBLE);
        String ownerEmail = new PrefManager(this).getUserEmail().replace(".", ",");

        FirebaseDatabase.getInstance()
                .getReference("GYM")
                .child(ownerEmail)
                .child("members")
                .child(memberPhone)
                .removeValue()
                .addOnSuccessListener(unused -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Member deleted successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Delete failed: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (memberListener != null && memberRef != null) {
            memberRef.removeEventListener(memberListener);
        }
    }
}