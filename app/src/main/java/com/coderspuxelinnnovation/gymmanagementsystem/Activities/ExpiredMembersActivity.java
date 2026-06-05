package com.coderspuxelinnnovation.gymmanagementsystem.Activities;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
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

    private String[] sortOptions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expired_members);

        sortOptions = new String[]{
                getString(R.string.sort_name_az),
                getString(R.string.sort_name_za),
                getString(R.string.sort_most_expired),
                getString(R.string.sort_recently_expired),
                getString(R.string.sort_expiring_soon),
                getString(R.string.sort_highest_dues),
                getString(R.string.sort_lowest_dues)
        };

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
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, sortOptions) {

            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                if (view instanceof TextView) {
                    TextView textView = (TextView) view;
                    textView.setTextColor(Color.WHITE);
                    textView.setBackgroundColor(Color.parseColor("#1A1A1A"));
                    textView.setPadding(16, 10, 16, 10);
                    textView.setTextSize(14f);
                }
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                if (view instanceof TextView) {
                    TextView textView = (TextView) view;
                    textView.setTextColor(Color.WHITE);
                    textView.setBackgroundColor(Color.parseColor("#2C2C2C"));
                    textView.setPadding(16, 12, 16, 12);
                    textView.setTextSize(14f);

                    if (position < getCount() - 1) {
                        textView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                    }
                }
                return view;
            }
        };

        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSort.setAdapter(spinnerAdapter);
        spinnerSort.setBackground(ContextCompat.getDrawable(this, R.drawable.spinner_background_black));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            spinnerSort.setPopupBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.spinner_popup_background));
        }
    }

    private void setupListeners() {
        toolbar.setNavigationOnClickListener(v -> finish());

        btnFilter.setOnClickListener(v -> {
            Toast.makeText(this, getString(R.string.use_chips_to_filter), Toast.LENGTH_SHORT).show();
        });

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

        btnClearSearch.setOnClickListener(v -> {
            etSearchExpired.setText("");
            btnClearSearch.setVisibility(View.GONE);
        });

        chipAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                currentFilter = "ALL";
                Log.d(TAG, getString(R.string.filter_changed_all));
                filterMembers(etSearchExpired.getText().toString().trim());
            }
        });

        chipWithDues.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                currentFilter = "WITH_DUES";
                Log.d(TAG, getString(R.string.filter_changed_dues));
                filterMembers(etSearchExpired.getText().toString().trim());
            }
        });

        chipClearDues.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                currentFilter = "CLEAR_DUES";
                Log.d(TAG, getString(R.string.filter_changed_clear));
                filterMembers(etSearchExpired.getText().toString().trim());
            }
        });

        spinnerSort.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                currentSort = sortOptions[position];
                Log.d(TAG, getString(R.string.sort_changed, currentSort));
                sortMembers();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private void loadExpiredMembers() {
        if (databaseReference == null) {
            Toast.makeText(this, getString(R.string.unable_to_connect), Toast.LENGTH_SHORT).show();
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

                Log.d(TAG, getString(R.string.loading_members_log, today.getTime()));

                if (snapshot.exists()) {
                    for (DataSnapshot memberSnapshot : snapshot.getChildren()) {
                        try {
                            GymMember member = parseMember(memberSnapshot);

                            if (member != null && member.getCurrentPlan() != null) {
                                String endDate = member.getCurrentPlan().getEndDate();

                                if (isPlanExpiredOrExpiringSoon(endDate, today, expiringSoonDate)) {
                                    int balance = calculateOutstandingBalance(memberSnapshot, member);
                                    member.setOutstandingBalance(balance);

                                    long daysExpired = calculateDaysExpired(member, today);
                                    member.setDaysExpired(daysExpired);

                                    if (daysExpired >= 0) {
                                        member.getCurrentPlan().setStatus(getString(R.string.expired_status));
                                    } else {
                                        member.getCurrentPlan().setStatus(getString(R.string.expiring_soon_status));
                                    }

                                    allExpiredMembers.add(member);

                                    Log.d(TAG, getString(R.string.member_log,
                                            member.getName(), daysExpired, balance, member.hasPendingDues()));
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, getString(R.string.error_parsing_member, e.getMessage()), e);
                        }
                    }
                }

                Log.d(TAG, getString(R.string.total_expired_members, allExpiredMembers.size()));

                updateStats();
                filterMembers(etSearchExpired.getText().toString().trim());
                hideProgress();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                hideProgress();
                Toast.makeText(ExpiredMembersActivity.this,
                        getString(R.string.error_prefix, error.getMessage()), Toast.LENGTH_SHORT).show();
                Log.e(TAG, getString(R.string.firebase_error, error.getMessage()));
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
            Log.e(TAG, getString(R.string.date_parse_error, e.getMessage()));
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
            Log.e(TAG, getString(R.string.error_parsing_member_data, e.getMessage()));
            return null;
        }
    }

    private int calculateOutstandingBalance(DataSnapshot memberSnapshot, GymMember member) {
        DataSnapshot planSnap = memberSnapshot.child("currentPlan").child("remaining");
        if (planSnap.exists()) {
            Object obj = planSnap.getValue();
            if (obj instanceof Long) return ((Long) obj).intValue();
            if (obj instanceof Integer) return (Integer) obj;
            if (obj instanceof Double) return ((Double) obj).intValue();
            if (obj instanceof String) return Integer.parseInt((String) obj);
        }

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
            Log.e(TAG, getString(R.string.date_calc_error, e.getMessage()));
            return 0;
        }
    }

    private void filterMembers(String query) {
        filteredMembers.clear();

        Log.d(TAG, getString(R.string.filtering_log, query, currentFilter, allExpiredMembers.size()));

        for (GymMember member : allExpiredMembers) {
            boolean matchesSearch = true;
            boolean matchesFilter = true;

            if (!query.isEmpty()) {
                String lowerQuery = query.toLowerCase();
                matchesSearch = (member.getName() != null &&
                        member.getName().toLowerCase().contains(lowerQuery)) ||
                        (member.getPhone() != null &&
                                member.getPhone().contains(lowerQuery));
            }

            if (currentFilter.equals("WITH_DUES")) {
                matchesFilter = member.hasPendingDues();
                Log.d(TAG, member.getName() + getString(R.string.has_dues_log) + member.hasPendingDues() +
                        getString(R.string.balance_log) + member.getOutstandingBalance());
            } else if (currentFilter.equals("CLEAR_DUES")) {
                matchesFilter = !member.hasPendingDues();
                Log.d(TAG, member.getName() + getString(R.string.clear_dues_log) + !member.hasPendingDues() +
                        getString(R.string.balance_log) + member.getOutstandingBalance());
            }

            if (matchesSearch && matchesFilter) {
                filteredMembers.add(member);
            }
        }

        Log.d(TAG, getString(R.string.filtered_count_log, filteredMembers.size()));
        sortMembers();
    }

    private void sortMembers() {
        if (currentSort.equals(getString(R.string.sort_name_az))) {
            Collections.sort(filteredMembers, (m1, m2) -> {
                String name1 = m1.getName() != null ? m1.getName() : "";
                String name2 = m2.getName() != null ? m2.getName() : "";
                return name1.compareToIgnoreCase(name2);
            });
        } else if (currentSort.equals(getString(R.string.sort_name_za))) {
            Collections.sort(filteredMembers, (m1, m2) -> {
                String name1 = m1.getName() != null ? m1.getName() : "";
                String name2 = m2.getName() != null ? m2.getName() : "";
                return name2.compareToIgnoreCase(name1);
            });
        } else if (currentSort.equals(getString(R.string.sort_most_expired))) {
            Collections.sort(filteredMembers, (m1, m2) ->
                    Long.compare(m2.getDaysExpired(), m1.getDaysExpired()));
        } else if (currentSort.equals(getString(R.string.sort_recently_expired))) {
            Collections.sort(filteredMembers, (m1, m2) ->
                    Long.compare(m1.getDaysExpired(), m2.getDaysExpired()));
        } else if (currentSort.equals(getString(R.string.sort_expiring_soon))) {
            Collections.sort(filteredMembers, (m1, m2) ->
                    Long.compare(m1.getDaysExpired(), m2.getDaysExpired()));
        } else if (currentSort.equals(getString(R.string.sort_highest_dues))) {
            Collections.sort(filteredMembers, (m1, m2) ->
                    Integer.compare(m2.getOutstandingBalance(), m1.getOutstandingBalance()));
        } else if (currentSort.equals(getString(R.string.sort_lowest_dues))) {
            Collections.sort(filteredMembers, (m1, m2) ->
                    Integer.compare(m1.getOutstandingBalance(), m2.getOutstandingBalance()));
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
        tvTotalDuesAmount.setText(getString(R.string.rupee_prefix) + totalDuesAmount);

        Log.d(TAG, getString(R.string.stats_log, totalExpired, withDues, totalDuesAmount));
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
    }
}