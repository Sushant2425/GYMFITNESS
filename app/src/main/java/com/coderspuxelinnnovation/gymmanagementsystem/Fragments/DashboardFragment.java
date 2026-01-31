package com.coderspuxelinnnovation.gymmanagementsystem.Fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.coderspuxelinnnovation.gymmanagementsystem.Activities.CollectPaymentActivity;
import com.coderspuxelinnnovation.gymmanagementsystem.Activities.ExpiredMembersActivity;
import com.coderspuxelinnnovation.gymmanagementsystem.Activities.MemberAddActivity;
import com.coderspuxelinnnovation.gymmanagementsystem.Activities.MembersListActivity;

import com.coderspuxelinnnovation.gymmanagementsystem.R;
import com.coderspuxelinnnovation.gymmanagementsystem.adapters.ExpiringMembersAdapter;
import com.coderspuxelinnnovation.gymmanagementsystem.models.Member;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DashboardFragment extends Fragment {

    private TextView tvWelcome, tvGymName, tvDate;
    private TextView tvTotalMembers, tvActiveMembers, tvMonthlyRevenue, tvPendingPayments;
    private CardView cardAddMember, cardViewMembers, cardCollectPayment, cardReports;
    private RecyclerView rvExpiringMembers;
    private TextView tvNoExpiring;

    private DatabaseReference databaseReference;
    private FirebaseAuth mAuth;
    private String userEmail;

    private int totalMembers = 0;
    private int activeMembers = 0;
    private double monthlyRevenue = 0;
    private double pendingDues = 0;

    private List<Member> expiringMembersList = new ArrayList<>();
    private ExpiringMembersAdapter expiringAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        initViews(view);
        setupFirebase();
        setupRecyclerView();
        setupClickListeners();
        loadDashboardData();

        return view;
    }

    private void initViews(View view) {
        tvWelcome = view.findViewById(R.id.tv_welcome);
        tvGymName = view.findViewById(R.id.tv_gym_name);
        tvDate = view.findViewById(R.id.tv_date);
        tvTotalMembers = view.findViewById(R.id.tv_total_members);
        tvActiveMembers = view.findViewById(R.id.tv_active_members);
        tvMonthlyRevenue = view.findViewById(R.id.tv_monthly_revenue);
        tvPendingPayments = view.findViewById(R.id.tv_pending_payments);

        cardAddMember = view.findViewById(R.id.card_add_member);
        cardViewMembers = view.findViewById(R.id.card_view_members);
        cardCollectPayment = view.findViewById(R.id.card_collect_payment);
        cardReports = view.findViewById(R.id.card_reports);

        rvExpiringMembers = view.findViewById(R.id.rv_expiring_members);
        tvNoExpiring = view.findViewById(R.id.tv_no_expiring);

        // Set current date
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        tvDate.setText("Today: " + sdf.format(new Date()));
    }

    private void setupFirebase() {
        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() != null) {
            userEmail = mAuth.getCurrentUser().getEmail();
            databaseReference = FirebaseDatabase.getInstance().getReference("GYM")
                    .child(userEmail.replace(".", ","));
        }
    }

    private void setupRecyclerView() {
        expiringAdapter = new ExpiringMembersAdapter(getContext(), expiringMembersList);
        rvExpiringMembers.setLayoutManager(new LinearLayoutManager(getContext()));
        rvExpiringMembers.setAdapter(expiringAdapter);
    }

    private void setupClickListeners() {
        cardAddMember.setOnClickListener(v -> {
            startActivity(new Intent(getContext(), MemberAddActivity.class));
        });

        cardViewMembers.setOnClickListener(v -> {
            startActivity(new Intent(getContext(), ExpiredMembersActivity.class));
        });

        cardCollectPayment.setOnClickListener(v -> {
            startActivity(new Intent(getContext(), CollectPaymentActivity.class));
            Toast.makeText(getContext(), "Select a member to collect payment", Toast.LENGTH_SHORT).show();
        });

        cardReports.setOnClickListener(v -> {
            // Navigate to Reports Fragment
            startActivity(new Intent(getContext(), MembersListActivity.class));
            Toast.makeText(getContext(), "Select a member to collect payment", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadDashboardData() {
        if (databaseReference == null) return;

        // Load gym name
        databaseReference.child("ownerInfo").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String gymName = snapshot.child("gymName").getValue(String.class);
                    String ownerName = snapshot.child("name").getValue(String.class);
                    if (gymName != null) {
                        tvGymName.setText(gymName);
                    }
                    if (ownerName != null) {
                        tvWelcome.setText("Welcome, " + ownerName + "!");
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Error loading gym info", Toast.LENGTH_SHORT).show();
            }
        });

        // Load members data
        databaseReference.child("members").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                totalMembers = 0;
                activeMembers = 0;
                monthlyRevenue = 0;
                pendingDues = 0;
                expiringMembersList.clear();

                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.DAY_OF_MONTH, 7); // Next 7 days
                long expiryThreshold = calendar.getTimeInMillis();

                Calendar monthStart = Calendar.getInstance();
                monthStart.set(Calendar.DAY_OF_MONTH, 1);
                monthStart.set(Calendar.HOUR_OF_DAY, 0);
                monthStart.set(Calendar.MINUTE, 0);
                monthStart.set(Calendar.SECOND, 0);
                long monthStartTime = monthStart.getTimeInMillis();

                for (DataSnapshot memberSnapshot : snapshot.getChildren()) {
                    totalMembers++;

                    // Get member info
                    DataSnapshot infoSnapshot = memberSnapshot.child("info");
                    String status = infoSnapshot.child("status").getValue(String.class);
                    String name = infoSnapshot.child("name").getValue(String.class);
                    String phone = infoSnapshot.child("phone").getValue(String.class);

                    if ("ACTIVE".equals(status)) {
                        activeMembers++;
                    }

                    // Calculate monthly revenue
                    DataSnapshot paymentsSnapshot = memberSnapshot.child("payments");
                    for (DataSnapshot paymentSnapshot : paymentsSnapshot.getChildren()) {
                        Long paymentDate = paymentSnapshot.child("date").getValue(Long.class);
                        Double amountPaid = paymentSnapshot.child("amountPaid").getValue(Double.class);

                        if (paymentDate != null && paymentDate >= monthStartTime) {
                            if (amountPaid != null) {
                                monthlyRevenue += amountPaid;
                            }
                        }
                    }

                    // Calculate pending dues
                    DataSnapshot planSnapshot = memberSnapshot.child("currentPlan");
                    Double totalFee = planSnapshot.child("totalFee").getValue(Double.class);

                    double totalPaid = 0;
                    for (DataSnapshot paymentSnapshot : paymentsSnapshot.getChildren()) {
                        Double amountPaid = paymentSnapshot.child("amountPaid").getValue(Double.class);
                        if (amountPaid != null) {
                            totalPaid += amountPaid;
                        }
                    }

                    if (totalFee != null && totalPaid < totalFee) {
                        pendingDues += (totalFee - totalPaid);
                    }

                    // Check for expiring memberships
                    String endDate = planSnapshot.child("endDate").getValue(String.class);
                    if (endDate != null && "ACTIVE".equals(status)) {
                        try {
                            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                            Date expiry = sdf.parse(endDate);
                            if (expiry != null && expiry.getTime() <= expiryThreshold && expiry.getTime() >= System.currentTimeMillis()) {
                                Member member = new Member();
                                member.setName(name);
                                member.setPhone(phone);
                                member.setEndDate(endDate);
                                expiringMembersList.add(member);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }

                updateUI();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Error loading data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUI() {
        tvTotalMembers.setText(String.valueOf(totalMembers));
        tvActiveMembers.setText(String.valueOf(activeMembers));
        tvMonthlyRevenue.setText("₹" + String.format(Locale.getDefault(), "%.0f", monthlyRevenue));
        tvPendingPayments.setText("₹" + String.format(Locale.getDefault(), "%.0f", pendingDues));

        if (expiringMembersList.isEmpty()) {
            rvExpiringMembers.setVisibility(View.GONE);
            tvNoExpiring.setVisibility(View.VISIBLE);
        } else {
            rvExpiringMembers.setVisibility(View.VISIBLE);
            tvNoExpiring.setVisibility(View.GONE);
            expiringAdapter.notifyDataSetChanged();
        }
    }
}