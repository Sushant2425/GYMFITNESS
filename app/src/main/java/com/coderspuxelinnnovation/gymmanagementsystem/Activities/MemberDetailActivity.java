package com.coderspuxelinnnovation.gymmanagementsystem.Activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MemberDetailActivity extends BaseActivity {

    private TextView tvName, tvPhone, tvEmail, tvGender, tvJoinDate, tvPlanType;
    private TextView tvTotalFee, tvStartDate, tvEndDate, tvPlanStatus;
    private MaterialButton btnEdit, btnDelete, btnViewPayments;
    private ProgressBar progressBar;
    private CardView cvStatusBadge;
    private ValueEventListener memberListener;
    private DatabaseReference memberRef;

    private String memberPhone;
    private MemberModel member;

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

        btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(MemberDetailActivity.this, MemberEditActivity.class);
            intent.putExtra("phone", memberPhone);
            startActivity(intent);
        });

        btnDelete.setOnClickListener(v -> showDeleteConfirmDialog());
        btnViewPayments.setOnClickListener(v -> viewPayments());
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

            // Check and update status
            updatePlanStatus(member);
        }
    }

    private void updatePlanStatus(MemberModel member) {
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

    private void viewPayments() {
        Intent intent = new Intent(MemberDetailActivity.this, MemberPaymentsActivity.class);
        intent.putExtra("phone", memberPhone);
        startActivity(intent);
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