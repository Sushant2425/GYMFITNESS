package com.coderspuxelinnnovation.gymmanagementsystem.Fragments;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.coderspuxelinnnovation.gymmanagementsystem.Activities.CollectPaymentActivity;
import com.coderspuxelinnnovation.gymmanagementsystem.Activities.ExpiredMembersActivity;
import com.coderspuxelinnnovation.gymmanagementsystem.Activities.MemberAddActivity;
import com.coderspuxelinnnovation.gymmanagementsystem.Activities.MembersListActivity;
import com.coderspuxelinnnovation.gymmanagementsystem.Activities.SettingsActivity;
import com.coderspuxelinnnovation.gymmanagementsystem.R;
import com.coderspuxelinnnovation.gymmanagementsystem.adapters.ExpiringMembersAdapter;
import com.coderspuxelinnnovation.gymmanagementsystem.models.Member;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DashboardFragment extends Fragment {

    private TextView tvWelcome, tvGymName, tvDate;
    private TextView tvTotalMembers, tvActiveMembers, tvMonthlyRevenue, tvPendingPayments;
    private TextView tvRevenuePercentage, tvDuesPercentage;
    private TextView tvRevenueStartDate, tvRevenueEndDate, tvTotalExpected;
    private CardView cardAddMember, cardViewMembers, cardCollectPayment, cardReports;
    private RecyclerView rvExpiringMembers;
    private TextView tvNoExpiring;
    private LinearLayout revenueOverlay, duesOverlay;

    private ImageView ivMenu, ivSettings, ivNotification;
    private LineChart chartMonthlyRevenue;
    private LineChart chartPendingDues;

    private DatabaseReference databaseReference;
    private FirebaseAuth mAuth;
    private String userEmail;

    private int totalMembers = 0;
    private int activeMembers = 0;
    private double monthlyRevenue = 0;
    private double previousMonthRevenue = 0;
    private double pendingDues = 0;
    private double totalExpectedRevenue = 0;

    private List<Member> expiringMembersList = new ArrayList<>();
    private ExpiringMembersAdapter expiringAdapter;

    // Store actual revenue and dues for last 7 days
    private double[] last7DaysRevenueArray = new double[7];
    private double[] last7DaysDuesArray = new double[7];

    private SimpleDateFormat monthYearFormat = new SimpleDateFormat("yyyy-MM", Locale.ENGLISH);
    private String currentMonthYear = "";
    private String previousMonthYear = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        initViews(view);
        setupFirebase();
        setupRecyclerView();
        setupClickListeners();
        setupHeaderIconListeners();
        loadDashboardData();

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    private void initViews(View view) {
        tvWelcome = view.findViewById(R.id.tv_welcome);
        tvGymName = view.findViewById(R.id.tv_gym_name);
        tvDate = view.findViewById(R.id.tv_date);
        tvTotalMembers = view.findViewById(R.id.tv_total_members);
        tvActiveMembers = view.findViewById(R.id.tv_active_members);
        tvMonthlyRevenue = view.findViewById(R.id.tv_monthly_revenue);
        tvPendingPayments = view.findViewById(R.id.tv_pending_payments);
        tvRevenuePercentage = view.findViewById(R.id.tv_revenue_percentage);
        tvDuesPercentage = view.findViewById(R.id.tv_dues_percentage);
        tvRevenueStartDate = view.findViewById(R.id.tv_revenue_start_date);
        tvRevenueEndDate = view.findViewById(R.id.tv_revenue_end_date);
        tvTotalExpected = view.findViewById(R.id.tv_total_expected);

        cardAddMember = view.findViewById(R.id.card_add_member);
        cardViewMembers = view.findViewById(R.id.card_view_members);
        cardCollectPayment = view.findViewById(R.id.card_collect_payment);
        cardReports = view.findViewById(R.id.card_reports);

        rvExpiringMembers = view.findViewById(R.id.rv_expiring_members);
        tvNoExpiring = view.findViewById(R.id.tv_no_expiring);

        chartMonthlyRevenue = view.findViewById(R.id.chart_monthly_revenue);
        chartPendingDues = view.findViewById(R.id.chart_pending_dues);

        revenueOverlay = view.findViewById(R.id.revenue_overlay);
        duesOverlay = view.findViewById(R.id.dues_overlay);

        ivMenu = view.findViewById(R.id.iv_menu);
        ivSettings = view.findViewById(R.id.iv_settings);
        ivNotification = view.findViewById(R.id.iv_notification);

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH);
        tvDate.setText(getString(R.string.today_prefix) + sdf.format(new Date()));

        Calendar startCal = Calendar.getInstance();
        startCal.add(Calendar.DAY_OF_MONTH, -6);
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d", Locale.ENGLISH);
        tvRevenueStartDate.setText(dateFormat.format(startCal.getTime()));
        tvRevenueEndDate.setText(getString(R.string.today));

        Calendar now = Calendar.getInstance();
        currentMonthYear = monthYearFormat.format(now.getTime());

        now.add(Calendar.MONTH, -1);
        previousMonthYear = monthYearFormat.format(now.getTime());

        // Initialize arrays
        for (int i = 0; i < 7; i++) {
            last7DaysRevenueArray[i] = 0;
            last7DaysDuesArray[i] = 0;
        }
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

    private void setupHeaderIconListeners() {
        ivMenu.setOnClickListener(v -> {
            if (getActivity() != null) {
                View rootView = getActivity().findViewById(android.R.id.content);
                DrawerLayout drawerLayout = findDrawerLayout(rootView);
                if (drawerLayout != null) {
                    drawerLayout.openDrawer(androidx.core.view.GravityCompat.START);
                } else {
                    Toast.makeText(getContext(), getString(R.string.navigation_not_available), Toast.LENGTH_SHORT).show();
                }
            }
        });

        ivSettings.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), SettingsActivity.class);
            startActivity(intent);
        });

        ivNotification.setOnClickListener(v -> {
            Toast.makeText(getContext(), getString(R.string.notification_message, expiringMembersList.size()), Toast.LENGTH_SHORT).show();
        });
    }

    private DrawerLayout findDrawerLayout(View view) {
        if (view instanceof DrawerLayout) {
            return (DrawerLayout) view;
        }
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                DrawerLayout result = findDrawerLayout(viewGroup.getChildAt(i));
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
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
            Toast.makeText(getContext(), getString(R.string.select_member_payment), Toast.LENGTH_SHORT).show();
        });

        cardReports.setOnClickListener(v -> {
            startActivity(new Intent(getContext(), MembersListActivity.class));
            Toast.makeText(getContext(), getString(R.string.select_member_payment), Toast.LENGTH_SHORT).show();
        });
    }

    private void loadDashboardData() {
        if (databaseReference == null) return;

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
                        tvWelcome.setText(getString(R.string.welcome_owner, ownerName));
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), getString(R.string.error_loading_gym_info), Toast.LENGTH_SHORT).show();
            }
        });

        databaseReference.child("members").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                totalMembers = 0;
                activeMembers = 0;
                monthlyRevenue = 0;
                previousMonthRevenue = 0;
                pendingDues = 0;
                totalExpectedRevenue = 0;
                expiringMembersList.clear();

                // Reset arrays for last 7 days
                for (int i = 0; i < 7; i++) {
                    last7DaysRevenueArray[i] = 0;
                    last7DaysDuesArray[i] = 0;
                }

                // Get current date for last 7 days calculation
                Calendar todayCal = Calendar.getInstance();
                todayCal.set(Calendar.HOUR_OF_DAY, 0);
                todayCal.set(Calendar.MINUTE, 0);
                todayCal.set(Calendar.SECOND, 0);
                todayCal.set(Calendar.MILLISECOND, 0);
                long todayMillis = todayCal.getTimeInMillis();

                // Create array of last 7 days timestamps (start of each day)
                long[] last7DaysTimestamps = new long[7];
                for (int i = 0; i < 7; i++) {
                    Calendar dayCal = Calendar.getInstance();
                    dayCal.add(Calendar.DAY_OF_MONTH, -6 + i);
                    dayCal.set(Calendar.HOUR_OF_DAY, 0);
                    dayCal.set(Calendar.MINUTE, 0);
                    dayCal.set(Calendar.SECOND, 0);
                    dayCal.set(Calendar.MILLISECOND, 0);
                    last7DaysTimestamps[i] = dayCal.getTimeInMillis();
                }

                Calendar expiryThresholdCal = Calendar.getInstance();
                expiryThresholdCal.add(Calendar.DAY_OF_MONTH, 7);
                long expiryThreshold = expiryThresholdCal.getTimeInMillis();

                for (DataSnapshot memberSnap : snapshot.getChildren()) {
                    totalMembers++;

                    DataSnapshot infoSnap = memberSnap.child("info");
                    String name = infoSnap.child("name").getValue(String.class);
                    String phone = infoSnap.child("phone").getValue(String.class);
                    String status = infoSnap.child("status").getValue(String.class);

                    if (name == null || phone == null) continue;

                    if ("ACTIVE".equals(status)) {
                        activeMembers++;
                    }

                    DataSnapshot currentPlanSnap = memberSnap.child("currentPlan");

                    // Process payments for revenue and dues
                    DataSnapshot paymentsSnap = memberSnap.child("payments");
                    for (DataSnapshot paySnap : paymentsSnap.getChildren()) {
                        Integer remaining = paySnap.child("remaining").getValue(Integer.class);
                        Integer totalFee = paySnap.child("totalFee").getValue(Integer.class);
                        Integer amountPaid = paySnap.child("amountPaid").getValue(Integer.class);
                        String forMonth = paySnap.child("forMonth").getValue(String.class);
                        Long paymentDate = paySnap.child("date").getValue(Long.class);

                        if (forMonth != null && totalFee != null) {
                            // Calculate monthly revenue for current month
                            if (forMonth.equals(currentMonthYear)) {
                                totalExpectedRevenue += totalFee;
                                if (amountPaid != null && amountPaid > 0) {
                                    monthlyRevenue += amountPaid;
                                }
                                if (remaining != null && remaining > 0) {
                                    pendingDues += remaining;
                                }
                            }

                            // Calculate previous month revenue
                            if (forMonth.equals(previousMonthYear)) {
                                if (amountPaid != null && amountPaid > 0) {
                                    previousMonthRevenue += amountPaid;
                                }
                            }

                            // Process for last 7 days chart using payment date
                            if (paymentDate != null && amountPaid != null && amountPaid > 0) {
                                for (int i = 0; i < 7; i++) {
                                    if (paymentDate >= last7DaysTimestamps[i] &&
                                            paymentDate < last7DaysTimestamps[i] + (24 * 60 * 60 * 1000)) {
                                        last7DaysRevenueArray[i] += amountPaid;
                                        break;
                                    }
                                }
                            }

                            // Process dues for chart
                            if (forMonth.equals(currentMonthYear) && remaining != null && remaining > 0) {
                                for (int i = 0; i < 7; i++) {
                                    last7DaysDuesArray[i] += remaining / 7.0;
                                }
                            }
                        }
                    }

                    // Check for expiring memberships
                    if ("ACTIVE".equals(status) && currentPlanSnap.exists()) {
                        String endDate = currentPlanSnap.child("endDate").getValue(String.class);
                        if (endDate != null) {
                            try {
                                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH);
                                Date expiry = sdf.parse(endDate);
                                if (expiry != null && expiry.getTime() <= expiryThreshold &&
                                        expiry.getTime() >= System.currentTimeMillis()) {
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
                }

                updateUI();
                setupRevenueChart();
                setupDuesChart();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), getString(R.string.error_loading_data), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUI() {
        tvTotalMembers.setText(String.valueOf(totalMembers));
        tvActiveMembers.setText(String.valueOf(activeMembers));
        tvMonthlyRevenue.setText(getString(R.string.rupee_prefix) + String.format(Locale.getDefault(), "%.0f", monthlyRevenue));
        tvPendingPayments.setText(getString(R.string.rupee_prefix) + String.format(Locale.getDefault(), "%.0f", pendingDues));
        tvTotalExpected.setText(getString(R.string.total_expected, String.format(Locale.getDefault(), "%.0f", totalExpectedRevenue)));

        double revenueChange = 0;
        if (previousMonthRevenue > 0) {
            revenueChange = ((monthlyRevenue - previousMonthRevenue) / previousMonthRevenue) * 100;
        } else if (monthlyRevenue > 0) {
            revenueChange = 100;
        }

        String revenueChangeText = String.format(Locale.getDefault(), getString(R.string.revenue_percentage_format), revenueChange);
        tvRevenuePercentage.setText(revenueChangeText);
        tvRevenuePercentage.setTextColor(revenueChange >= 0 ?
                Color.parseColor("#4CAF50") : Color.parseColor("#EF5350"));

        double duesPercentage = 0;
        if (totalExpectedRevenue > 0) {
            duesPercentage = (pendingDues / totalExpectedRevenue) * 100;
        }

        String duesText = String.format(Locale.getDefault(), getString(R.string.dues_percentage_format), duesPercentage);
        tvDuesPercentage.setText(duesText);

        if (expiringMembersList.isEmpty()) {
            rvExpiringMembers.setVisibility(View.GONE);
            tvNoExpiring.setVisibility(View.VISIBLE);
        } else {
            rvExpiringMembers.setVisibility(View.VISIBLE);
            tvNoExpiring.setVisibility(View.GONE);
            expiringAdapter.notifyDataSetChanged();
        }
    }

    private void setupRevenueChart() {
        ArrayList<Entry> entries = new ArrayList<>();

        boolean hasData = false;
        for (int i = 0; i < 7; i++) {
            if (last7DaysRevenueArray[i] > 0) {
                hasData = true;
                break;
            }
        }

        if (hasData) {
            revenueOverlay.setVisibility(View.GONE);
            for (int i = 0; i < 7; i++) {
                entries.add(new Entry(i, (float) last7DaysRevenueArray[i]));
            }
        } else {
            revenueOverlay.setVisibility(View.VISIBLE);
            float[] sampleData = {5f, 10f, 15f, 20f, 15f, 10f, 5f};
            for (int i = 0; i < sampleData.length; i++) {
                entries.add(new Entry(i, sampleData[i]));
            }
        }

        LineDataSet dataSet = new LineDataSet(entries, getString(R.string.revenue));
        dataSet.setColor(hasData ? Color.parseColor("#9C27B0") : Color.parseColor("#E0E0E0"));
        dataSet.setLineWidth(2f);
        dataSet.setDrawCircles(false);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setCubicIntensity(0.2f);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(hasData ? Color.parseColor("#9C27B0") : Color.parseColor("#E0E0E0"));
        dataSet.setFillAlpha(hasData ? 80 : 40);

        LineData lineData = new LineData(dataSet);
        chartMonthlyRevenue.setData(lineData);
        chartMonthlyRevenue.getDescription().setEnabled(false);
        chartMonthlyRevenue.getLegend().setEnabled(false);
        chartMonthlyRevenue.setTouchEnabled(false);
        chartMonthlyRevenue.setDragEnabled(false);
        chartMonthlyRevenue.setScaleEnabled(false);
        chartMonthlyRevenue.setPinchZoom(false);
        chartMonthlyRevenue.setDrawGridBackground(false);
        chartMonthlyRevenue.setDrawBorders(false);
        chartMonthlyRevenue.getXAxis().setEnabled(false);
        chartMonthlyRevenue.getAxisLeft().setEnabled(false);
        chartMonthlyRevenue.getAxisRight().setEnabled(false);
        chartMonthlyRevenue.invalidate();
    }

    private void setupDuesChart() {
        ArrayList<Entry> entries = new ArrayList<>();

        boolean hasData = false;
        for (int i = 0; i < 7; i++) {
            if (last7DaysDuesArray[i] > 0) {
                hasData = true;
                break;
            }
        }

        if (hasData) {
            duesOverlay.setVisibility(View.GONE);
            for (int i = 0; i < 7; i++) {
                entries.add(new Entry(i, (float) last7DaysDuesArray[i]));
            }
        } else {
            duesOverlay.setVisibility(View.VISIBLE);
            float[] sampleData = {5f, 8f, 12f, 15f, 12f, 8f, 5f};
            for (int i = 0; i < sampleData.length; i++) {
                entries.add(new Entry(i, sampleData[i]));
            }
        }

        LineDataSet dataSet = new LineDataSet(entries, getString(R.string.dues));
        dataSet.setColor(hasData ? Color.parseColor("#FF9800") : Color.parseColor("#E0E0E0"));
        dataSet.setLineWidth(2f);
        dataSet.setDrawCircles(false);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setCubicIntensity(0.2f);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(hasData ? Color.parseColor("#FF9800") : Color.parseColor("#E0E0E0"));
        dataSet.setFillAlpha(hasData ? 80 : 40);

        LineData lineData = new LineData(dataSet);
        chartPendingDues.setData(lineData);
        chartPendingDues.getDescription().setEnabled(false);
        chartPendingDues.getLegend().setEnabled(false);
        chartPendingDues.setTouchEnabled(false);
        chartPendingDues.setDragEnabled(false);
        chartPendingDues.setScaleEnabled(false);
        chartPendingDues.setPinchZoom(false);
        chartPendingDues.setDrawGridBackground(false);
        chartPendingDues.setDrawBorders(false);
        chartPendingDues.getXAxis().setEnabled(false);
        chartPendingDues.getAxisLeft().setEnabled(false);
        chartPendingDues.getAxisRight().setEnabled(false);
        chartPendingDues.invalidate();
    }
}