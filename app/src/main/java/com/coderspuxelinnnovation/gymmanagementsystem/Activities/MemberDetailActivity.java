package com.coderspuxelinnnovation.gymmanagementsystem.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

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
import java.util.Date;
import java.util.Locale;

public class MemberDetailActivity extends BaseActivity {

    private TextView tvName, tvPhone, tvEmail, tvGender, tvJoinDate, tvPlanType;
    private TextView tvTotalFee, tvStartDate, tvEndDate, tvPlanStatus;
    private MaterialButton btnEdit, btnDelete, btnViewPayments;
    private ProgressBar progressBar;
    private ValueEventListener memberListener;  // ✅ Added for cleanup
    private DatabaseReference memberRef;  // ✅ Added reference

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
                    Toast.makeText(MemberDetailActivity.this, "Member not found", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(MemberDetailActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void bindData(MemberModel member) {
        // Info
        tvName.setText(member.getInfo().getName());
        tvPhone.setText(member.getPhone());
        tvEmail.setText(member.getInfo().getEmail());
        tvGender.setText(member.getInfo().getGender());
        tvJoinDate.setText(member.getInfo().getJoinDate());

        // Plan
        tvPlanType.setText(member.getCurrentPlan().getPlanType());
        tvTotalFee.setText("₹" + member.getCurrentPlan().getTotalFee());
        tvStartDate.setText(member.getCurrentPlan().getStartDate());
        tvEndDate.setText(member.getCurrentPlan().getEndDate());
        tvPlanStatus.setText(member.getCurrentPlan().getStatus());

        // Status color  ✅ FIXED: checkAndUpdatePlanStatus बाहेर काढले
        tvPlanStatus.setTextColor(
                "ACTIVE".equals(member.getCurrentPlan().getStatus()) ?
                        getResources().getColor(R.color.green, null) :
                        getResources().getColor(R.color.red, null)
        );

        // ✅ Plan expiry check - FIXED position
        checkAndUpdatePlanStatus(member);
    }

    private void checkAndUpdatePlanStatus(MemberModel member) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        try {
            Date endDate = sdf.parse(member.getCurrentPlan().getEndDate());
            Date today = new Date();

            if (endDate != null && endDate.before(today)) {
                // Plan expired - update status
                String ownerEmail = new PrefManager(this).getUserEmail().replace(".", ",");
                DatabaseReference planRef = FirebaseDatabase.getInstance()
                        .getReference("GYM")
                        .child(ownerEmail)
                        .child("members")
                        .child(memberPhone)
                        .child("currentPlan")
                        .child("status");

                planRef.setValue("EXPIRED")
                        .addOnSuccessListener(aVoid -> {
                            tvPlanStatus.setText("EXPIRED");
                            tvPlanStatus.setTextColor(getResources().getColor(R.color.red, null));
                            Toast.makeText(this, "Plan expired automatically!", Toast.LENGTH_SHORT).show();
                        });
            }
        } catch (Exception e) {
            // Date parse error
        }
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
        String ownerEmail = new PrefManager(this).getUserEmail().replace(".", ",");

        FirebaseDatabase.getInstance()
                .getReference("GYM")
                .child(ownerEmail)
                .child("members")
                .child(memberPhone)
                .removeValue()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Member deleted successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
