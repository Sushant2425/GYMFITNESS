package com.coderspuxelinnnovation.gymmanagementsystem.Activities;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.coderspuxelinnnovation.gymmanagementsystem.R;
import com.coderspuxelinnnovation.gymmanagementsystem.adapters.ExpiredMemberAdapter;
import com.coderspuxelinnnovation.gymmanagementsystem.base.BaseActivity;
import com.coderspuxelinnnovation.gymmanagementsystem.models.GymMember;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ExpiredMembersActivity extends BaseActivity {

    private static final String TAG = "ExpiredMembersActivity";
    private static final int EXPIRING_SOON_DAYS = 3;

    // UI Components
    private MaterialToolbar toolbar;
    private EditText etSearchExpired;
    private ImageView btnClearSearch, btnFilter;
    private Chip chipAll, chipWithDues, chipClearDues;
    private TextView tvTotalExpired, tvWithDues, tvTotalDuesAmount;
    private Spinner spinnerSort;
    private RecyclerView rvExpiredMembers;
    private LinearLayout layoutEmptyState;
    private ProgressBar progressBar;

    // Data
    private ExpiredMemberAdapter adapter;
    private List<GymMember> allExpiredMembers = new ArrayList<>();
    private List<GymMember> filteredMembers = new ArrayList<>();
    private DatabaseReference databaseReference;
    private FirebaseAuth mAuth;
    private String userEmail;
    private ValueEventListener membersListener;

    // Filter & Sort state
    private String currentFilter = "ALL";
    private String currentSort = "Name (A-Z)";

    private final String[] sortOptions = {
            "Name (A-Z)",
            "Name (Z-A)",
            "Most Expired First",
            "Recently Expired",
            "Expiring Soon First",
            "Highest Dues",
            "Lowest Dues"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expired_members);

        initViews();
        setupFirebase();
        setupRecyclerView();
        setupSortSpinner();
        setupListeners();
        loadExpiredMembers();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        etSearchExpired = findViewById(R.id.etSearchExpired);
        btnClearSearch = findViewById(R.id.btnClearSearch);
        btnFilter = findViewById(R.id.btnFilter);
        chipAll = findViewById(R.id.chipAll);
        chipWithDues = findViewById(R.id.chipWithDues);
        chipClearDues = findViewById(R.id.chipClearDues);
        tvTotalExpired = findViewById(R.id.tvTotalExpired);
        tvWithDues = findViewById(R.id.tvWithDues);
        tvTotalDuesAmount = findViewById(R.id.tvTotalDuesAmount);
        spinnerSort = findViewById(R.id.spinnerSort);
        rvExpiredMembers = findViewById(R.id.rvExpiredMembers);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        progressBar = findViewById(R.id.progressBar);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupFirebase() {
        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() != null) {
            userEmail = mAuth.getCurrentUser().getEmail();
            if (userEmail != null) {
                databaseReference = FirebaseDatabase.getInstance()
                        .getReference("GYM")
                        .child(userEmail.replace(".", ","));
            }
        }
    }

    private void setupRecyclerView() {
        adapter = new ExpiredMemberAdapter(this, filteredMembers);
        rvExpiredMembers.setLayoutManager(new LinearLayoutManager(this));
        rvExpiredMembers.setAdapter(adapter);
    }

    private void setupSortSpinner() {
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, sortOptions);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSort.setAdapter(spinnerAdapter);
    }

    private void setupListeners() {
        // Toolbar back button
        toolbar.setNavigationOnClickListener(v -> finish());

        // Filter button
        btnFilter.setOnClickListener(v -> {
            // Show sort options or additional filters
            Toast.makeText(this, "Use chips to filter members", Toast.LENGTH_SHORT).show();
        });

        // Search functionality
        etSearchExpired.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                btnClearSearch.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                filterMembers(s.toString().trim());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Clear search button
        btnClearSearch.setOnClickListener(v -> {
            etSearchExpired.setText("");
            btnClearSearch.setVisibility(View.GONE);
        });

        // Filter chips - Fixed implementation
        chipAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                currentFilter = "ALL";
                Log.d(TAG, "Filter changed to: ALL");
                filterMembers(etSearchExpired.getText().toString().trim());
            }
        });

        chipWithDues.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                currentFilter = "WITH_DUES";
                Log.d(TAG, "Filter changed to: WITH_DUES");
                filterMembers(etSearchExpired.getText().toString().trim());
            }
        });

        chipClearDues.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                currentFilter = "CLEAR_DUES";
                Log.d(TAG, "Filter changed to: CLEAR_DUES");
                filterMembers(etSearchExpired.getText().toString().trim());
            }
        });

        // Sort spinner
        spinnerSort.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                currentSort = sortOptions[position];
                Log.d(TAG, "Sort changed to: " + currentSort);
                sortMembers();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private void loadExpiredMembers() {
        if (databaseReference == null) {
            Toast.makeText(this, "Unable to connect to database", Toast.LENGTH_SHORT).show();
            return;
        }

        showProgress();

        if (membersListener != null) {
            databaseReference.child("members").removeEventListener(membersListener);
        }

        membersListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allExpiredMembers.clear();

                Calendar today = Calendar.getInstance();
                today.set(Calendar.HOUR_OF_DAY, 0);
                today.set(Calendar.MINUTE, 0);
                today.set(Calendar.SECOND, 0);
                today.set(Calendar.MILLISECOND, 0);

                Calendar expiringSoonDate = Calendar.getInstance();
                expiringSoonDate.add(Calendar.DAY_OF_MONTH, EXPIRING_SOON_DAYS);

                Log.d(TAG, "Loading members - Today: " + today.getTime());

                if (snapshot.exists()) {
                    for (DataSnapshot memberSnapshot : snapshot.getChildren()) {
                        try {
                            GymMember member = parseMember(memberSnapshot);

                            if (member != null && member.getCurrentPlan() != null) {
                                String endDate = member.getCurrentPlan().getEndDate();

                                if (isPlanExpiredOrExpiringSoon(endDate, today, expiringSoonDate)) {
                                    // Calculate balance BEFORE adding to list
                                    int balance = calculateOutstandingBalance(memberSnapshot, member);
                                    member.setOutstandingBalance(balance);

                                    // Calculate days expired
                                    long daysExpired = calculateDaysExpired(member, today);
                                    member.setDaysExpired(daysExpired);

                                    // Set status
                                    if (daysExpired >= 0) {
                                        member.getCurrentPlan().setStatus("EXPIRED");
                                    } else {
                                        member.getCurrentPlan().setStatus("EXPIRING_SOON");
                                    }

                                    allExpiredMembers.add(member);

                                    Log.d(TAG, "Member: " + member.getName() +
                                            ", Days: " + daysExpired +
                                            ", Balance: " + balance +
                                            ", Has Dues: " + member.hasPendingDues());
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing member: " + e.getMessage(), e);
                        }
                    }
                }

                Log.d(TAG, "Total expired/expiring members: " + allExpiredMembers.size());

                updateStats();
                filterMembers(etSearchExpired.getText().toString().trim());
                hideProgress();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                hideProgress();
                Toast.makeText(ExpiredMembersActivity.this,
                        "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Firebase error: " + error.getMessage());
            }
        };

        databaseReference.child("members").addValueEventListener(membersListener);
    }

    private boolean isPlanExpiredOrExpiringSoon(String endDate, Calendar today, Calendar expiringSoonDate) {
        if (endDate == null) return false;

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Calendar endCal = Calendar.getInstance();
            endCal.setTime(sdf.parse(endDate));
            endCal.set(Calendar.HOUR_OF_DAY, 23);
            endCal.set(Calendar.MINUTE, 59);
            endCal.set(Calendar.SECOND, 59);

            return !endCal.after(today) ||
                    (endCal.after(today) && !endCal.after(expiringSoonDate));
        } catch (ParseException e) {
            Log.e(TAG, "Date parse error: " + e.getMessage());
            return false;
        }
    }

    private GymMember parseMember(DataSnapshot memberSnapshot) {
        try {
            DataSnapshot infoSnapshot = memberSnapshot.child("info");
            String name = infoSnapshot.child("name").getValue(String.class);
            String phone = infoSnapshot.child("phone").getValue(String.class);
            String email = infoSnapshot.child("email").getValue(String.class);
            String gender = infoSnapshot.child("gender").getValue(String.class);
            String joinDate = infoSnapshot.child("joinDate").getValue(String.class);
            String status = infoSnapshot.child("status").getValue(String.class);

            DataSnapshot planSnapshot = memberSnapshot.child("currentPlan");
            GymMember.PlanDetails currentPlan = null;

            if (planSnapshot.exists()) {
                String planId = planSnapshot.child("planId").getValue(String.class);
                String planType = planSnapshot.child("planType").getValue(String.class);
                String startDate = planSnapshot.child("startDate").getValue(String.class);
                String endDate = planSnapshot.child("endDate").getValue(String.class);

                Object feeObject = planSnapshot.child("totalFee").getValue();
                int totalFee = 0;
                if (feeObject instanceof Long) {
                    totalFee = ((Long) feeObject).intValue();
                } else if (feeObject instanceof Integer) {
                    totalFee = (Integer) feeObject;
                } else if (feeObject instanceof Double) {
                    totalFee = ((Double) feeObject).intValue();
                }

                String planStatus = planSnapshot.child("status").getValue(String.class);
                currentPlan = new GymMember.PlanDetails(planId, planType, startDate,
                        endDate, totalFee, planStatus);
            }

            return new GymMember(name, phone, email, gender, joinDate, status, currentPlan);

        } catch (Exception e) {
            Log.e(TAG, "Error parsing member data: " + e.getMessage());
            return null;
        }
    }

    private int calculateOutstandingBalance(DataSnapshot memberSnapshot, GymMember member) {

        // 1) First priority: remaining in currentPlan (latest & correct)
        DataSnapshot planSnap = memberSnapshot.child("currentPlan").child("remaining");
        if (planSnap.exists()) {
            Object obj = planSnap.getValue();
            if (obj instanceof Long) return ((Long) obj).intValue();
            if (obj instanceof Integer) return (Integer) obj;
            if (obj instanceof Double) return ((Double) obj).intValue();
            if (obj instanceof String) return Integer.parseInt((String) obj);
        }

        // 2) Fallback: last payment remaining (old records)
        DataSnapshot paymentsSnapshot = memberSnapshot.child("payments");

        long latestTimestamp = 0;
        int latestRemaining = 0;

        for (DataSnapshot payment : paymentsSnapshot.getChildren()) {
            Long time = payment.child("date").getValue(Long.class);
            Integer remaining = payment.child("remaining").getValue(Integer.class);

            if (time != null && remaining != null && time > latestTimestamp) {
                latestTimestamp = time;
                latestRemaining = remaining;
            }
        }

        return latestRemaining;
    }



    // Helper method to format timestamp for logging
    private String formatDate(long timestamp) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        } catch (Exception e) {
            return String.valueOf(timestamp);
        }
    }
    private long calculateDaysExpired(GymMember member, Calendar today) {
        if (member.getCurrentPlan() == null || member.getCurrentPlan().getEndDate() == null) {
            return 0;
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Calendar endDate = Calendar.getInstance();
            endDate.setTime(sdf.parse(member.getCurrentPlan().getEndDate()));
            endDate.set(Calendar.HOUR_OF_DAY, 0);
            endDate.set(Calendar.MINUTE, 0);
            endDate.set(Calendar.SECOND, 0);
            endDate.set(Calendar.MILLISECOND, 0);

            long diffMillis = today.getTimeInMillis() - endDate.getTimeInMillis();
            return diffMillis / (1000 * 60 * 60 * 24);
        } catch (ParseException e) {
            Log.e(TAG, "Date calculation error: " + e.getMessage());
            return 0;
        }
    }

    private void filterMembers(String query) {
        filteredMembers.clear();

        Log.d(TAG, "Filtering - Query: '" + query + "', Filter: " + currentFilter +
                ", Total members: " + allExpiredMembers.size());

        for (GymMember member : allExpiredMembers) {
            boolean matchesSearch = true;
            boolean matchesFilter = true;

            // Search filter
            if (!query.isEmpty()) {
                String lowerQuery = query.toLowerCase();
                matchesSearch = (member.getName() != null &&
                        member.getName().toLowerCase().contains(lowerQuery)) ||
                        (member.getPhone() != null &&
                                member.getPhone().contains(lowerQuery));
            }

            // Dues filter - FIXED
            if (currentFilter.equals("WITH_DUES")) {
                matchesFilter = member.hasPendingDues();
                Log.d(TAG, member.getName() + " - Has dues: " + member.hasPendingDues() +
                        ", Balance: " + member.getOutstandingBalance());
            } else if (currentFilter.equals("CLEAR_DUES")) {
                matchesFilter = !member.hasPendingDues();
                Log.d(TAG, member.getName() + " - Clear dues: " + !member.hasPendingDues() +
                        ", Balance: " + member.getOutstandingBalance());
            }

            if (matchesSearch && matchesFilter) {
                filteredMembers.add(member);
            }
        }

        Log.d(TAG, "Filtered members count: " + filteredMembers.size());
        sortMembers();
    }

    private void sortMembers() {
        switch (currentSort) {
            case "Name (A-Z)":
                Collections.sort(filteredMembers, (m1, m2) -> {
                    String name1 = m1.getName() != null ? m1.getName() : "";
                    String name2 = m2.getName() != null ? m2.getName() : "";
                    return name1.compareToIgnoreCase(name2);
                });
                break;

            case "Name (Z-A)":
                Collections.sort(filteredMembers, (m1, m2) -> {
                    String name1 = m1.getName() != null ? m1.getName() : "";
                    String name2 = m2.getName() != null ? m2.getName() : "";
                    return name2.compareToIgnoreCase(name1);
                });
                break;

            case "Most Expired First":
                Collections.sort(filteredMembers, (m1, m2) ->
                        Long.compare(m2.getDaysExpired(), m1.getDaysExpired()));
                break;

            case "Recently Expired":
                Collections.sort(filteredMembers, (m1, m2) ->
                        Long.compare(m1.getDaysExpired(), m2.getDaysExpired()));
                break;

            case "Expiring Soon First":
                Collections.sort(filteredMembers, (m1, m2) ->
                        Long.compare(m1.getDaysExpired(), m2.getDaysExpired()));
                break;

            case "Highest Dues":
                Collections.sort(filteredMembers, (m1, m2) ->
                        Integer.compare(m2.getOutstandingBalance(), m1.getOutstandingBalance()));
                break;

            case "Lowest Dues":
                Collections.sort(filteredMembers, (m1, m2) ->
                        Integer.compare(m1.getOutstandingBalance(), m2.getOutstandingBalance()));
                break;
        }

        adapter.updateList(filteredMembers);
        updateEmptyState();
    }

    private void updateStats() {
        int totalExpired = 0;
        int withDues = 0;
        int totalDuesAmount = 0;

        for (GymMember member : allExpiredMembers) {
            if (member.getDaysExpired() >= 0) {
                totalExpired++;
            }

            if (member.hasPendingDues()) {
                withDues++;
                totalDuesAmount += member.getOutstandingBalance();
            }
        }

        tvTotalExpired.setText(String.valueOf(totalExpired));
        tvWithDues.setText(String.valueOf(withDues));
        tvTotalDuesAmount.setText("₹" + totalDuesAmount);

        Log.d(TAG, "Stats - Expired: " + totalExpired + ", With Dues: " + withDues +
                ", Total Dues: ₹" + totalDuesAmount);
    }

    private void updateEmptyState() {
        if (filteredMembers.isEmpty()) {
            layoutEmptyState.setVisibility(View.VISIBLE);
            rvExpiredMembers.setVisibility(View.GONE);
        } else {
            layoutEmptyState.setVisibility(View.GONE);
            rvExpiredMembers.setVisibility(View.VISIBLE);
        }
    }

    private void showProgress() {
        progressBar.setVisibility(View.VISIBLE);
        rvExpiredMembers.setVisibility(View.GONE);
        layoutEmptyState.setVisibility(View.GONE);
    }

    private void hideProgress() {
        progressBar.setVisibility(View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (membersListener != null && databaseReference != null) {
            databaseReference.child("members").removeEventListener(membersListener);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Auto-refresh via ValueEventListener
    }
}