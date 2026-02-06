package com.coderspuxelinnnovation.gymmanagementsystem.Fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
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

    // Header icons
    private ImageView ivMenu, ivSettings, ivNotification;

    // Charts
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

    // Chart data storage - using Map for better day tracking
    private Map<Integer, Double> last7DaysRevenue = new HashMap<>();
    private Map<Integer, Double> last7DaysDues = new HashMap<>();

    private SimpleDateFormat monthYearFormat = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
    private String currentMonthYear = "";
    private String previousMonthYear = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        // Set status bar color - Works on all Android versions
        setStatusBarColor();

        initViews(view);
        setupFirebase();
        setupRecyclerView();
        setupClickListeners();
        setupHeaderIconListeners();
        loadDashboardData();

        return view;
    }

    // Method to set status bar color for all Android versions
    private void setStatusBarColor() {
        if (getActivity() != null) {
            Activity activity = getActivity();

            // For Android 5.0 (Lollipop) and above
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Window window = activity.getWindow();
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                window.setStatusBarColor(ContextCompat.getColor(activity, R.color.blue_800));
            }
            // For Android 4.4 (KitKat)
            else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

                // Create a status bar view
                View statusBarView = new View(activity);
                ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        getStatusBarHeight(activity)
                );

                statusBarView.setBackgroundColor(ContextCompat.getColor(activity, R.color.blue_800));

                // Add the status bar view to the decor view
                ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
                decorView.addView(statusBarView);
            }
        }
    }

    // Helper method to get status bar height
    private int getStatusBarHeight(Context context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    // Optional: Reset status bar when fragment is destroyed
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Reset status bar if needed (optional)
        // resetStatusBarColor();
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

        // Header icons
        ivMenu = view.findViewById(R.id.iv_menu);
        ivSettings = view.findViewById(R.id.iv_settings);
        ivNotification = view.findViewById(R.id.iv_notification);

        // Set current date
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        tvDate.setText("Today: " + sdf.format(new Date()));

        // Set date range labels for last 7 days
        Calendar startCal = Calendar.getInstance();
        startCal.add(Calendar.DAY_OF_MONTH, -6);
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d", Locale.getDefault());
        tvRevenueStartDate.setText(dateFormat.format(startCal.getTime()));
        tvRevenueEndDate.setText("Today");

        // Get current and previous month-year strings
        Calendar now = Calendar.getInstance();
        currentMonthYear = monthYearFormat.format(now.getTime());

        now.add(Calendar.MONTH, -1);
        previousMonthYear = monthYearFormat.format(now.getTime());

        // Initialize chart data for last 7 days
        for (int i = 0; i < 7; i++) {
            last7DaysRevenue.put(i, 0.0);
            last7DaysDues.put(i, 0.0);
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

    /**
     * Setup click listeners for header icons
     */
    private void setupHeaderIconListeners() {
        // Menu icon - open navigation drawer
        ivMenu.setOnClickListener(v -> {
            if (getActivity() != null) {
                // Try to find DrawerLayout in parent activity
                View rootView = getActivity().findViewById(android.R.id.content);
                DrawerLayout drawerLayout = findDrawerLayout(rootView);

                if (drawerLayout != null) {
                    // Open the drawer
                    drawerLayout.openDrawer(androidx.core.view.GravityCompat.START);
                } else {
                    Toast.makeText(getContext(), "Navigation drawer not available", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Settings icon - open settings activity
        ivSettings.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), SettingsActivity.class);
            startActivity(intent);
        });

        // Notification icon - you can implement notification functionality
        ivNotification.setOnClickListener(v -> {
            // TODO: Implement notification activity or dialog
            Toast.makeText(getContext(), "Notifications: " + expiringMembersList.size() + " members expiring soon", Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * Helper method to find DrawerLayout in the view hierarchy
     */
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
            Toast.makeText(getContext(), "Select a member to collect payment", Toast.LENGTH_SHORT).show();
        });

        cardReports.setOnClickListener(v -> {
            startActivity(new Intent(getContext(), MembersListActivity.class));
            Toast.makeText(getContext(), "Select a member to collect payment", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadDashboardData() {
        if (databaseReference == null) return;

        // Load gym name and owner info
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

        // Load all members data - using same structure as PendingDuesActivity
        databaseReference.child("members").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Reset all counters
                totalMembers = 0;
                activeMembers = 0;
                monthlyRevenue = 0;
                previousMonthRevenue = 0;
                pendingDues = 0;
                totalExpectedRevenue = 0;
                expiringMembersList.clear();

                // Reset chart data
                for (int i = 0; i < 7; i++) {
                    last7DaysRevenue.put(i, 0.0);
                    last7DaysDues.put(i, 0.0);
                }

                // Calculate date thresholds
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.DAY_OF_MONTH, 7);
                long expiryThreshold = calendar.getTimeInMillis();

                // Iterate through all members
                for (DataSnapshot memberSnap : snapshot.getChildren()) {
                    totalMembers++;

                    String memberId = memberSnap.getKey();
                    DataSnapshot infoSnap = memberSnap.child("info");

                    String name = infoSnap.child("name").getValue(String.class);
                    String phone = infoSnap.child("phone").getValue(String.class);
                    String status = infoSnap.child("status").getValue(String.class);

                    if (name == null || phone == null) continue;

                    // Count active members
                    if ("ACTIVE".equals(status)) {
                        activeMembers++;
                    }

                    // Get plan type from currentPlan
                    String planType = "Regular";
                    DataSnapshot currentPlanSnap = memberSnap.child("currentPlan");
                    if (currentPlanSnap.exists()) {
                        String type = currentPlanSnap.child("planType").getValue(String.class);
                        if (type != null) {
                            planType = type;
                        }
                    }

                    // Process payments - SAME LOGIC AS PendingDuesActivity
                    DataSnapshot paymentsSnap = memberSnap.child("payments");
                    for (DataSnapshot paySnap : paymentsSnap.getChildren()) {
                        String paymentId = paySnap.getKey();
                        Integer remaining = paySnap.child("remaining").getValue(Integer.class);
                        Integer totalFee = paySnap.child("totalFee").getValue(Integer.class);
                        Integer amountPaid = paySnap.child("amountPaid").getValue(Integer.class);
                        String forMonth = paySnap.child("forMonth").getValue(String.class);
                        String planStartDate = paySnap.child("planStartDate").getValue(String.class);

                        if (forMonth != null && totalFee != null) {
                            // Get due date
                            Long dueDate = paySnap.child("dueDate").getValue(Long.class);
                            if (dueDate == null && planStartDate != null) {
                                dueDate = calculateDueDateFromStartDate(planStartDate);
                            }
                            if (dueDate == null) {
                                dueDate = System.currentTimeMillis();
                            }

                            // Get payment status
                            String paymentStatus = paySnap.child("status").getValue(String.class);
                            if (paymentStatus == null) {
                                paymentStatus = "PENDING";
                            }

                            // Calculate monthly revenue - current month payments
                            if (forMonth.equals(currentMonthYear)) {
                                totalExpectedRevenue += totalFee;

                                if (amountPaid != null && amountPaid > 0) {
                                    monthlyRevenue += amountPaid;
                                }

                                // Calculate pending dues for current month
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

                            // Process last 7 days data for charts
                            processLast7DaysData(dueDate, amountPaid, remaining, forMonth);
                        }
                    }

                    // Check for expiring memberships
                    if ("ACTIVE".equals(status) && currentPlanSnap.exists()) {
                        String endDate = currentPlanSnap.child("endDate").getValue(String.class);
                        if (endDate != null) {
                            try {
                                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
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
                setupCharts();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Error loading data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Calculate due date from plan start date (same as PendingDuesActivity)
     */
    private long calculateDueDateFromStartDate(String startDate) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Calendar cal = Calendar.getInstance();
            cal.setTime(sdf.parse(startDate));
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
            return cal.getTimeInMillis();
        } catch (Exception e) {
            return System.currentTimeMillis();
        }
    }

    /**
     * Process payment data for last 7 days chart
     */
    private void processLast7DaysData(Long paymentDate, Integer amountPaid, Integer remaining, String forMonth) {
        if (paymentDate == null) return;

        Calendar tempCal = Calendar.getInstance();
        tempCal.add(Calendar.DAY_OF_MONTH, -6);
        tempCal.set(Calendar.HOUR_OF_DAY, 0);
        tempCal.set(Calendar.MINUTE, 0);
        tempCal.set(Calendar.SECOND, 0);
        tempCal.set(Calendar.MILLISECOND, 0);

        // Check each of the last 7 days
        for (int i = 0; i < 7; i++) {
            Calendar nextDay = (Calendar) tempCal.clone();
            nextDay.add(Calendar.DAY_OF_MONTH, 1);

            // Check if payment falls in this day
            if (paymentDate >= tempCal.getTimeInMillis() && paymentDate < nextDay.getTimeInMillis()) {
                // Add revenue for this day
                if (amountPaid != null && amountPaid > 0) {
                    last7DaysRevenue.put(i, last7DaysRevenue.get(i) + amountPaid);
                }
            }

            // For dues chart - distribute current month dues across last 7 days
            if (forMonth.equals(currentMonthYear) && remaining != null && remaining > 0) {
                // Create variation for each day
                double dayDue = remaining / 7.0;
                double variation = 0.8 + (0.4 * Math.sin((i + paymentDate.hashCode() % 7) * Math.PI / 3));
                last7DaysDues.put(i, last7DaysDues.get(i) + (dayDue * variation));
            }

            tempCal.add(Calendar.DAY_OF_MONTH, 1);
        }
    }

    private void updateUI() {
        tvTotalMembers.setText(String.valueOf(totalMembers));
        tvActiveMembers.setText(String.valueOf(activeMembers));
        tvMonthlyRevenue.setText("₹" + String.format(Locale.getDefault(), "%.0f", monthlyRevenue));
        tvPendingPayments.setText("₹" + String.format(Locale.getDefault(), "%.0f", pendingDues));

        // Update total expected revenue label
        tvTotalExpected.setText("Total Expected: ₹" + String.format(Locale.getDefault(), "%.0f", totalExpectedRevenue));

        // Calculate revenue percentage change vs previous month
        double revenueChange = 0;
        if (previousMonthRevenue > 0) {
            revenueChange = ((monthlyRevenue - previousMonthRevenue) / previousMonthRevenue) * 100;
        } else if (monthlyRevenue > 0) {
            revenueChange = 100;
        }

        String revenueChangeText = String.format(Locale.getDefault(), "%+.1f%% vs last month", revenueChange);
        tvRevenuePercentage.setText(revenueChangeText);
        tvRevenuePercentage.setTextColor(revenueChange >= 0 ?
                Color.parseColor("#4CAF50") : Color.parseColor("#EF5350"));

        // Calculate dues percentage of total expected
        double duesPercentage = 0;
        if (totalExpectedRevenue > 0) {
            duesPercentage = (pendingDues / totalExpectedRevenue) * 100;
        }

        String duesText = String.format(Locale.getDefault(), "%.1f%% of total fees", duesPercentage);
        tvDuesPercentage.setText(duesText);

        // Update expiring members list
        if (expiringMembersList.isEmpty()) {
            rvExpiringMembers.setVisibility(View.GONE);
            tvNoExpiring.setVisibility(View.VISIBLE);
        } else {
            rvExpiringMembers.setVisibility(View.VISIBLE);
            tvNoExpiring.setVisibility(View.GONE);
            expiringAdapter.notifyDataSetChanged();
        }
    }

    private void setupCharts() {
        setupRevenueChart();
        setupDuesChart();
    }

    private void setupRevenueChart() {
        ArrayList<Entry> entries = new ArrayList<>();

        // Check if we have any revenue data
        boolean hasData = false;
        for (int i = 0; i < 7; i++) {
            Double revenue = last7DaysRevenue.get(i);
            if (revenue != null && revenue > 0) {
                hasData = true;
                break;
            }
        }

        // Show/hide overlay based on data
        if (hasData) {
            revenueOverlay.setVisibility(View.GONE);
            // Add real data points
            for (int i = 0; i < 7; i++) {
                Double revenue = last7DaysRevenue.get(i);
                entries.add(new Entry(i, revenue != null ? revenue.floatValue() : 0f));
            }
        } else {
            revenueOverlay.setVisibility(View.VISIBLE);
            // Add minimal sample data for visual representation
            float[] sampleData = {5f, 10f, 15f, 20f, 15f, 10f, 5f};
            for (int i = 0; i < sampleData.length; i++) {
                entries.add(new Entry(i, sampleData[i]));
            }
        }

        LineDataSet dataSet = new LineDataSet(entries, "Revenue");

        // Styling - Mountain chart effect
        dataSet.setColor(hasData ? Color.parseColor("#9C27B0") : Color.parseColor("#E0E0E0"));
        dataSet.setLineWidth(2f);
        dataSet.setDrawCircles(false);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setCubicIntensity(0.2f);

        // Fill area under line (mountain effect)
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(hasData ? Color.parseColor("#9C27B0") : Color.parseColor("#E0E0E0"));
        dataSet.setFillAlpha(hasData ? 80 : 40);

        LineData lineData = new LineData(dataSet);
        chartMonthlyRevenue.setData(lineData);

        // Chart configuration
        chartMonthlyRevenue.getDescription().setEnabled(false);
        chartMonthlyRevenue.getLegend().setEnabled(false);
        chartMonthlyRevenue.setTouchEnabled(false);
        chartMonthlyRevenue.setDragEnabled(false);
        chartMonthlyRevenue.setScaleEnabled(false);
        chartMonthlyRevenue.setPinchZoom(false);
        chartMonthlyRevenue.setDrawGridBackground(false);
        chartMonthlyRevenue.setDrawBorders(false);

        // X-axis configuration
        XAxis xAxis = chartMonthlyRevenue.getXAxis();
        xAxis.setEnabled(false);

        // Y-axis configuration
        YAxis leftAxis = chartMonthlyRevenue.getAxisLeft();
        leftAxis.setEnabled(false);

        YAxis rightAxis = chartMonthlyRevenue.getAxisRight();
        rightAxis.setEnabled(false);

        chartMonthlyRevenue.invalidate();
    }

    private void setupDuesChart() {
        ArrayList<Entry> entries = new ArrayList<>();

        // Check if we have any dues data
        boolean hasData = false;
        for (int i = 0; i < 7; i++) {
            Double due = last7DaysDues.get(i);
            if (due != null && due > 0) {
                hasData = true;
                break;
            }
        }

        // Show/hide overlay based on data
        if (hasData) {
            duesOverlay.setVisibility(View.GONE);
            // Add real data points
            for (int i = 0; i < 7; i++) {
                Double due = last7DaysDues.get(i);
                entries.add(new Entry(i, due != null ? due.floatValue() : 0f));
            }
        } else {
            duesOverlay.setVisibility(View.VISIBLE);
            // Add minimal sample data for visual representation
            float[] sampleData = {5f, 8f, 12f, 15f, 12f, 8f, 5f};
            for (int i = 0; i < sampleData.length; i++) {
                entries.add(new Entry(i, sampleData[i]));
            }
        }

        LineDataSet dataSet = new LineDataSet(entries, "Dues");

        // Styling - Mountain chart effect
        dataSet.setColor(hasData ? Color.parseColor("#FF9800") : Color.parseColor("#E0E0E0"));
        dataSet.setLineWidth(2f);
        dataSet.setDrawCircles(false);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setCubicIntensity(0.2f);

        // Fill area under line (mountain effect)
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(hasData ? Color.parseColor("#FF9800") : Color.parseColor("#E0E0E0"));
        dataSet.setFillAlpha(hasData ? 80 : 40);

        LineData lineData = new LineData(dataSet);
        chartPendingDues.setData(lineData);

        // Chart configuration
        chartPendingDues.getDescription().setEnabled(false);
        chartPendingDues.getLegend().setEnabled(false);
        chartPendingDues.setTouchEnabled(false);
        chartPendingDues.setDragEnabled(false);
        chartPendingDues.setScaleEnabled(false);
        chartPendingDues.setPinchZoom(false);
        chartPendingDues.setDrawGridBackground(false);
        chartPendingDues.setDrawBorders(false);

        // X-axis configuration
        XAxis xAxis = chartPendingDues.getXAxis();
        xAxis.setEnabled(false);

        // Y-axis configuration
        YAxis leftAxis = chartPendingDues.getAxisLeft();
        leftAxis.setEnabled(false);

        YAxis rightAxis = chartPendingDues.getAxisRight();
        rightAxis.setEnabled(false);

        chartPendingDues.invalidate();
    }
}