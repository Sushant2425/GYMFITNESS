package com.coderspuxelinnnovation.gymmanagementsystem.Activities;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.util.Pair;
import androidx.viewpager2.widget.ViewPager2;

import com.coderspuxelinnnovation.gymmanagementsystem.R;
import com.coderspuxelinnnovation.gymmanagementsystem.Utils.PrefManager;
import com.coderspuxelinnnovation.gymmanagementsystem.adapters.PaymentTabsAdapter;
import com.coderspuxelinnnovation.gymmanagementsystem.base.BaseActivity;
import com.coderspuxelinnnovation.gymmanagementsystem.models.PendingDueModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

public class PendingDuesActivity extends BaseActivity {
    private MaterialToolbar toolbar;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private ImageButton btnPrevMonth, btnNextMonth;
    private TextView tvCurrentMonth, tvMonthYear;
    private LinearProgressIndicator progressBar;
    private FloatingActionButton fabRefresh;
    private TextView tvTotalAmount, tvMemberCount, tvPendingCount, tvPaidCount;
    private SearchView searchView;

    private List<PendingDueModel> allPendingDues = new ArrayList<>();
    private List<PendingDueModel> allPaidDues = new ArrayList<>();
    private PaymentTabsAdapter tabsAdapter;

    private DatabaseReference rootRef;
    private String ownerEmail;
    private Calendar currentCalendar;
    private SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM", Locale.getDefault());
    private SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy", Locale.getDefault());
    private SimpleDateFormat monthYearFormat = new SimpleDateFormat("yyyy-MM", Locale.getDefault());

    private String selectedMonthYear = "";
    private String searchQuery = "";
    private boolean showAllMonths = true;
    private static final int SMS_PERMISSION_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pending_dues);

        initializeViews();
        setupToolbar();
        setupMonthNavigation();
        setupTabs();
        setupFirebase();
        setupListeners();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, SMS_PERMISSION_CODE);
        }

        loadAllPayments();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);
        btnPrevMonth = findViewById(R.id.btnPrevMonth);
        btnNextMonth = findViewById(R.id.btnNextMonth);
        tvCurrentMonth = findViewById(R.id.tvCurrentMonth);
        tvMonthYear = findViewById(R.id.tvMonthYear);
        progressBar = findViewById(R.id.progressBar);
        fabRefresh = findViewById(R.id.fabRefresh);
        tvTotalAmount = findViewById(R.id.tvTotalAmount);
        tvMemberCount = findViewById(R.id.tvMemberCount);
        tvPendingCount = findViewById(R.id.tvPendingCount);
        tvPaidCount = findViewById(R.id.tvPaidCount);

        currentCalendar = Calendar.getInstance();
        selectedMonthYear = monthYearFormat.format(currentCalendar.getTime());
        showAllMonths = true;
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.setOverflowIcon(ContextCompat.getDrawable(this, R.drawable.ic_more_vert_white));
    }

    private void setupMonthNavigation() {
        updateMonthDisplay();

        btnPrevMonth.setOnClickListener(v -> {
            if (showAllMonths) {
                showAllMonths = false;
            }
            currentCalendar.add(Calendar.MONTH, -1);
            selectedMonthYear = monthYearFormat.format(currentCalendar.getTime());
            updateMonthDisplay();
            applyFilters();
        });

        btnNextMonth.setOnClickListener(v -> {
            if (showAllMonths) {
                showAllMonths = false;
            }
            currentCalendar.add(Calendar.MONTH, 1);
            selectedMonthYear = monthYearFormat.format(currentCalendar.getTime());
            updateMonthDisplay();
            applyFilters();
        });

        View monthContainer = findViewById(R.id.monthContainer);
        monthContainer.setOnClickListener(v -> {
            openMonthPicker();
        });
    }

    private void openMonthPicker() {
        Calendar calendar = Calendar.getInstance();
        if (!showAllMonths) {
            calendar.setTime(currentCalendar.getTime());
        }

        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long selectedDate = calendar.getTimeInMillis();

        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(getString(R.string.select_month))
                .setSelection(selectedDate)
                .setInputMode(MaterialDatePicker.INPUT_MODE_CALENDAR)
                .build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            Calendar selectedCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            selectedCal.setTimeInMillis(selection);

            currentCalendar.set(Calendar.YEAR, selectedCal.get(Calendar.YEAR));
            currentCalendar.set(Calendar.MONTH, selectedCal.get(Calendar.MONTH));
            currentCalendar.set(Calendar.DAY_OF_MONTH, 1);

            showAllMonths = false;
            selectedMonthYear = monthYearFormat.format(currentCalendar.getTime());
            updateMonthDisplay();
            applyFilters();

            Toast.makeText(this,
                    getString(R.string.selected_month,
                            monthFormat.format(currentCalendar.getTime()),
                            yearFormat.format(currentCalendar.getTime())),
                    Toast.LENGTH_SHORT).show();
        });

        datePicker.addOnNegativeButtonClickListener(dialog -> {
            // User cancelled
        });

        datePicker.show(getSupportFragmentManager(), "MONTH_PICKER");
    }

    private void updateMonthDisplay() {
        if (showAllMonths) {
            tvCurrentMonth.setText(getString(R.string.all_months));
            tvMonthYear.setText(getString(R.string.complete_history));
            btnPrevMonth.setAlpha(0.5f);
            btnNextMonth.setAlpha(0.5f);
        } else {
            tvCurrentMonth.setText(monthFormat.format(currentCalendar.getTime()));
            tvMonthYear.setText(yearFormat.format(currentCalendar.getTime()));
            btnPrevMonth.setAlpha(1.0f);
            btnNextMonth.setAlpha(1.0f);

            Calendar now = Calendar.getInstance();
            if (currentCalendar.get(Calendar.YEAR) > now.get(Calendar.YEAR) ||
                    (currentCalendar.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                            currentCalendar.get(Calendar.MONTH) > now.get(Calendar.MONTH))) {
                btnNextMonth.setEnabled(false);
                btnNextMonth.setAlpha(0.3f);
            } else {
                btnNextMonth.setEnabled(true);
                btnNextMonth.setAlpha(1.0f);
            }
        }
    }

    private void setupTabs() {
        tabsAdapter = new PaymentTabsAdapter(this);
        viewPager.setAdapter(tabsAdapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            if (position == 0) {
                tab.setText(getString(R.string.pending_tab));
            } else {
                tab.setText(getString(R.string.paid_tab));
            }
        }).attach();

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateSummaryCards();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_pending_dues, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        searchView = (SearchView) searchItem.getActionView();
        searchView.setQueryHint(getString(R.string.search_member_hint));

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                searchQuery = newText.trim();
                applyFilters();
                return true;
            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_filter) {
            showQuickFilters();
            return true;
        } else if (id == R.id.action_clear) {
            clearFilters();
            return true;
        } else if (id == R.id.action_refresh) {
            loadAllPayments();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showQuickFilters() {
        String[] filterOptions = {
                getString(R.string.all_months_filter),
                getString(R.string.this_month),
                getString(R.string.last_month),
                getString(R.string.last_3_months),
                getString(R.string.custom_month)
        };

        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.quick_filters))
                .setItems(filterOptions, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            showAllMonths = true;
                            currentCalendar = Calendar.getInstance();
                            break;
                        case 1:
                            showAllMonths = false;
                            currentCalendar = Calendar.getInstance();
                            break;
                        case 2:
                            showAllMonths = false;
                            currentCalendar = Calendar.getInstance();
                            currentCalendar.add(Calendar.MONTH, -1);
                            break;
                        case 3:
                            showAllMonths = false;
                            break;
                        case 4:
                            openMonthPicker();
                            return;
                    }
                    selectedMonthYear = monthYearFormat.format(currentCalendar.getTime());
                    updateMonthDisplay();
                    applyFilters();
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void clearFilters() {
        showAllMonths = true;
        currentCalendar = Calendar.getInstance();
        searchQuery = "";
        if (searchView != null) {
            searchView.setQuery("", false);
            searchView.clearFocus();
        }
        updateMonthDisplay();
        applyFilters();
        Toast.makeText(this, getString(R.string.filters_cleared), Toast.LENGTH_SHORT).show();
    }

    private void setupFirebase() {
        ownerEmail = new PrefManager(this).getUserEmail().replace(".", ",");
        rootRef = FirebaseDatabase.getInstance().getReference("GYM").child(ownerEmail);
    }

    private void setupListeners() {
        fabRefresh.setOnClickListener(v -> {
            loadAllPayments();
            Toast.makeText(this, getString(R.string.refreshing), Toast.LENGTH_SHORT).show();
        });
    }

    private void loadAllPayments() {
        progressBar.setVisibility(View.VISIBLE);
        allPendingDues.clear();
        allPaidDues.clear();

        rootRef.child("members").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot memberSnap : snapshot.getChildren()) {
                    String memberId = memberSnap.getKey();
                    DataSnapshot infoSnap = memberSnap.child("info");
                    String name = infoSnap.child("name").getValue(String.class);
                    String phone = infoSnap.child("phone").getValue(String.class);

                    if (name == null || phone == null) continue;

                    String planType = getString(R.string.regular_plan);
                    DataSnapshot currentPlanSnap = memberSnap.child("currentPlan");
                    if (currentPlanSnap.exists()) {
                        String type = currentPlanSnap.child("planType").getValue(String.class);
                        if (type != null) {
                            planType = type;
                        }
                    }

                    DataSnapshot paymentsSnap = memberSnap.child("payments");
                    for (DataSnapshot paySnap : paymentsSnap.getChildren()) {
                        String paymentId = paySnap.getKey();
                        Integer remaining = paySnap.child("remaining").getValue(Integer.class);
                        Integer totalFee = paySnap.child("totalFee").getValue(Integer.class);
                        Integer amountPaid = paySnap.child("amountPaid").getValue(Integer.class);
                        String forMonth = paySnap.child("forMonth").getValue(String.class);
                        String planStartDate = paySnap.child("planStartDate").getValue(String.class);
                        String planId = paySnap.child("planId").getValue(String.class);

                        if (forMonth != null && totalFee != null) {
                            Long dueDate = paySnap.child("dueDate").getValue(Long.class);
                            if (dueDate == null && planStartDate != null) {
                                dueDate = calculateDueDateFromStartDate(planStartDate);
                            }
                            if (dueDate == null) {
                                dueDate = System.currentTimeMillis();
                            }

                            PendingDueModel due = new PendingDueModel(
                                    name, phone, planType,
                                    forMonth, remaining != null ? remaining : 0,
                                    memberId, paymentId, dueDate
                            );

                            due.setTotalFee(totalFee);
                            due.setAmountPaid(amountPaid != null ? amountPaid : 0);

                            String status = paySnap.child("status").getValue(String.class);
                            due.setStatus(status != null ? status : getString(R.string.pending_status));

                            String paymentMode = paySnap.child("mode").getValue(String.class);
                            if (paymentMode != null) {
                                due.setPaymentMode(paymentMode);
                            }

                            due.setPlanId(planId);
                            due.setPlanStartDate(planStartDate);

                            if (remaining != null && remaining > 0) {
                                allPendingDues.add(due);
                            } else if (remaining != null && remaining == 0) {
                                allPaidDues.add(due);
                            }
                        }
                    }
                }

                runOnUiThread(() -> {
                    applyFilters();
                    progressBar.setVisibility(View.GONE);
                    updateSummaryCards();
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(PendingDuesActivity.this,
                            getString(R.string.error_prefix, error.getMessage()), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

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

    private void applyFilters() {
        List<PendingDueModel> filteredPending = filterByMonth(allPendingDues);
        List<PendingDueModel> filteredPaid = filterByMonth(allPaidDues);

        if (!searchQuery.isEmpty()) {
            filteredPending = searchFilter(filteredPending);
            filteredPaid = searchFilter(filteredPaid);
        }

        Collections.sort(filteredPending, (d1, d2) -> Long.compare(d2.getDueDate(), d1.getDueDate()));
        Collections.sort(filteredPaid, (d1, d2) -> Long.compare(d2.getDueDate(), d1.getDueDate()));

        tabsAdapter.updatePendingList(filteredPending);
        tabsAdapter.updatePaidList(filteredPaid);

        updateSummaryCards();
    }

    private List<PendingDueModel> filterByMonth(List<PendingDueModel> list) {
        if (showAllMonths) {
            return new ArrayList<>(list);
        }

        List<PendingDueModel> filtered = new ArrayList<>();
        for (PendingDueModel due : list) {
            if (due.getForMonth().equals(selectedMonthYear)) {
                filtered.add(due);
            }
        }
        return filtered;
    }

    private List<PendingDueModel> searchFilter(List<PendingDueModel> list) {
        List<PendingDueModel> filtered = new ArrayList<>();
        String query = searchQuery.toLowerCase();

        for (PendingDueModel due : list) {
            if (due.getName().toLowerCase().contains(query) ||
                    due.getPhone().contains(query) ||
                    (due.getPlanType() != null && due.getPlanType().toLowerCase().contains(query))) {
                filtered.add(due);
            }
        }
        return filtered;
    }

    private void updateSummaryCards() {
        int currentTab = viewPager.getCurrentItem();

        List<PendingDueModel> pending = tabsAdapter.getPendingList();
        List<PendingDueModel> paid = tabsAdapter.getPaidList();

        int totalPending = 0;
        int totalPaid = 0;

        for (PendingDueModel due : pending) {
            totalPending += due.getRemaining();
        }

        for (PendingDueModel due : paid) {
            totalPaid += due.getAmountPaid();
        }

        tvPendingCount.setText(String.valueOf(pending.size()));
        tvPaidCount.setText(String.valueOf(paid.size()));

        if (currentTab == 0) {
            tvTotalAmount.setText(getString(R.string.rupee_prefix) + String.format(Locale.getDefault(), "%,d", totalPending));
            tvMemberCount.setText(getString(R.string.members_count_format, pending.size()));
            tvTotalAmount.setTextColor(ContextCompat.getColor(this, R.color.red));
        } else {
            tvTotalAmount.setText(getString(R.string.rupee_prefix) + String.format(Locale.getDefault(), "%,d", totalPaid));
            tvMemberCount.setText(getString(R.string.members_count_format, paid.size()));
            tvTotalAmount.setTextColor(ContextCompat.getColor(this, R.color.green));
        }
    }

    public void onPaymentCollected(PendingDueModel due, int amount) {
        showPaymentDialog(due, amount);
    }

    private void showPaymentDialog(PendingDueModel due, int suggestedAmount) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_collect_payment, null);

        TextInputEditText etAmount = dialogView.findViewById(R.id.etAmount);
        MaterialAutoCompleteTextView etPaymentMode = dialogView.findViewById(R.id.etPaymentMode);
        TextInputEditText etNotes = dialogView.findViewById(R.id.etNotes);

        etAmount.setText(String.valueOf(suggestedAmount));

        String[] paymentModes = {getString(R.string.cash), getString(R.string.upi),
                getString(R.string.card), getString(R.string.bank_transfer),
                getString(R.string.cheque)};
        android.widget.ArrayAdapter<String> modeAdapter = new android.widget.ArrayAdapter<>(this,
                R.layout.dropdown_item, paymentModes);
        etPaymentMode.setAdapter(modeAdapter);
        etPaymentMode.setText(getString(R.string.cash), false);

        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.collect_payment))
                .setMessage(getString(R.string.collect_payment_message, due.getName(), due.getRemaining()))
                .setView(dialogView)
                .setPositiveButton(getString(R.string.collect), (dialog, which) -> {
                    String amountStr = etAmount.getText().toString();
                    String mode = etPaymentMode.getText().toString();
                    String notes = etNotes.getText().toString();

                    if (amountStr.isEmpty()) {
                        Toast.makeText(this, getString(R.string.enter_amount), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    try {
                        int amount = Integer.parseInt(amountStr);
                        if (amount > due.getRemaining()) {
                            Toast.makeText(this, getString(R.string.amount_exceeds_due), Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (mode.isEmpty()) mode = getString(R.string.cash);
                        collectPayment(due, amount, mode, notes);
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, getString(R.string.invalid_amount), Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void collectPayment(PendingDueModel due, int amount, String mode, String notes) {
        progressBar.setVisibility(View.VISIBLE);

        DatabaseReference memberRef = rootRef.child("members").child(due.getMemberId());

        memberRef.child("payments").child(due.getPaymentId()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(PendingDuesActivity.this, getString(R.string.payment_record_not_found), Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                Integer currentPaid = snapshot.child("amountPaid").getValue(Integer.class);
                Integer currentRemaining = snapshot.child("remaining").getValue(Integer.class);
                Integer totalFee = snapshot.child("totalFee").getValue(Integer.class);
                String planId = snapshot.child("planId").getValue(String.class);
                String planStartDate = snapshot.child("planStartDate").getValue(String.class);
                String forMonth = snapshot.child("forMonth").getValue(String.class);

                if (currentRemaining == null || totalFee == null) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(PendingDuesActivity.this, getString(R.string.invalid_payment_data), Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                int newPaidAmount = (currentPaid != null ? currentPaid : 0) + amount;
                int newRemaining = Math.max(0, currentRemaining - amount);
                String status = newRemaining == 0 ? getString(R.string.paid_status) : getString(R.string.partial_status);

                String transactionId = UUID.randomUUID().toString();

                HashMap<String, Object> childUpdates = new HashMap<>();
                childUpdates.put("payments/" + due.getPaymentId() + "/amountPaid", newPaidAmount);
                childUpdates.put("payments/" + due.getPaymentId() + "/remaining", newRemaining);
                childUpdates.put("payments/" + due.getPaymentId() + "/status", status);
                childUpdates.put("payments/" + due.getPaymentId() + "/mode", mode);
                childUpdates.put("payments/" + due.getPaymentId() + "/lastPaymentDate", System.currentTimeMillis());
                childUpdates.put("payments/" + due.getPaymentId() + "/lastPaymentMode", mode);
                childUpdates.put("payments/" + due.getPaymentId() + "/lastPaymentNotes", notes);

                HashMap<String, Object> transactionData = new HashMap<>();
                transactionData.put("transactionId", transactionId);
                transactionData.put("amount", amount);
                transactionData.put("paymentMode", mode);
                transactionData.put("notes", notes);
                transactionData.put("date", System.currentTimeMillis());
                transactionData.put("remainingAfter", newRemaining);

                childUpdates.put("payments/" + due.getPaymentId() + "/paymentHistory/" + transactionId, transactionData);

                memberRef.updateChildren(childUpdates)
                        .addOnSuccessListener(aVoid -> {
                            if (newRemaining == 0 && planId != null) {
                                updateCurrentPlanStatus(memberRef, planId);
                            }

                            Uri pdfUri = generatePaymentPdf(due, amount, mode, newRemaining);
                            sendPaymentNotifications(due, amount, mode, newRemaining, pdfUri);

                            runOnUiThread(() -> {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(PendingDuesActivity.this, getString(R.string.payment_collected_message), Toast.LENGTH_SHORT).show();
                                loadAllPayments();
                            });
                        })
                        .addOnFailureListener(e -> {
                            runOnUiThread(() -> {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(PendingDuesActivity.this, getString(R.string.error_prefix, e.getMessage()), Toast.LENGTH_SHORT).show();
                            });
                        });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(PendingDuesActivity.this, getString(R.string.error_prefix, error.getMessage()), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void updateCurrentPlanStatus(DatabaseReference memberRef, String planId) {
        if (planId != null) {
            memberRef.child("currentPlan").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String currentPlanIdInDb = snapshot.child("planId").getValue(String.class);
                        if (currentPlanIdInDb != null && currentPlanIdInDb.equals(planId)) {
                            HashMap<String, Object> planUpdate = new HashMap<>();
                            planUpdate.put("status", getString(R.string.active_status));
                            memberRef.child("currentPlan").updateChildren(planUpdate);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                }
            });
        }
    }

    private Uri generatePaymentPdf(PendingDueModel due, int amount, String mode, int remaining) {
        try {
            PdfDocument pdfDocument = new PdfDocument();
            Paint paint = new Paint();
            Paint titlePaint = new Paint();

            PdfDocument.PageInfo pageInfo =
                    new PdfDocument.PageInfo.Builder(595, 842, 1).create();
            PdfDocument.Page page = pdfDocument.startPage(pageInfo);
            Canvas canvas = page.getCanvas();

            titlePaint.setTextSize(20);
            titlePaint.setFakeBoldText(true);

            paint.setTextSize(14);

            int y = 50;

            canvas.drawText(getString(R.string.pdf_title), 150, y, titlePaint);
            y += 40;

            canvas.drawLine(20, y, 575, y, paint);
            y += 30;

            canvas.drawText(getString(R.string.pdf_member_name, due.getName()), 40, y, paint); y += 25;
            canvas.drawText(getString(R.string.pdf_phone, due.getPhone()), 40, y, paint); y += 25;
            canvas.drawText(getString(R.string.pdf_for_month, due.getForMonth()), 40, y, paint); y += 25;
            canvas.drawText(getString(R.string.pdf_plan_type, due.getPlanType()), 40, y, paint); y += 25;

            canvas.drawLine(20, y, 575, y, paint);
            y += 30;

            canvas.drawText(getString(R.string.pdf_total_fee, due.getTotalFee()), 40, y, paint); y += 25;
            canvas.drawText(getString(R.string.pdf_paid_amount, amount), 40, y, paint); y += 25;
            canvas.drawText(getString(R.string.pdf_remaining, remaining), 40, y, paint); y += 25;
            canvas.drawText(getString(R.string.pdf_payment_via, mode), 40, y, paint); y += 25;

            canvas.drawLine(20, y, 575, y, paint);
            y += 40;

            paint.setFakeBoldText(true);
            canvas.drawText(getString(R.string.pdf_thank_you), 150, y, paint);

            pdfDocument.finishPage(page);

            File dir = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), getString(R.string.bills_folder));
            if (!dir.exists()) dir.mkdirs();

            File file = new File(dir, getString(R.string.payment_filename, System.currentTimeMillis()));
            pdfDocument.writeTo(new FileOutputStream(file));
            pdfDocument.close();

            return FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    file
            );

        } catch (Exception e) {
            e.printStackTrace();
            Log.e("PDF", getString(R.string.pdf_error_log, e.getMessage()));
            return null;
        }
    }

    private void sendPaymentNotifications(PendingDueModel due, int amount, String mode, int remaining, Uri pdfUri) {
        sendPaymentSMS(due, amount, mode, remaining);

        if (pdfUri != null) {
            sendPaymentWhatsApp(due, pdfUri);
        }
    }

    private void sendPaymentSMS(PendingDueModel due, int amount, String mode, int remaining) {
        try {
            String gymName = getGymName();

            String message = getString(R.string.sms_payment_message, amount, mode, due.getForMonth(), remaining, gymName);

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
                SmsManager smsManager = SmsManager.getDefault();
                smsManager.sendTextMessage(due.getPhone(), null, message, null, null);

                runOnUiThread(() -> {
                    Toast.makeText(PendingDuesActivity.this, getString(R.string.sms_sent_to_phone, due.getPhone()), Toast.LENGTH_SHORT).show();
                });
            }

            Log.d("PaymentSMS", getString(R.string.sms_sent_log, message, due.getPhone()));
        } catch (Exception e) {
            Log.e("PaymentSMS", getString(R.string.error_log, e.getMessage()));
        }
    }

    private void sendPaymentWhatsApp(PendingDueModel due, Uri pdfUri) {
        try {
            if (due.getPhone() == null || due.getPhone().length() < 10) {
                return;
            }

            String phone = due.getPhone().replaceAll("[^0-9]", "");
            if (!phone.startsWith("91")) {
                phone = "91" + phone;
            }

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("application/pdf");
            intent.putExtra(Intent.EXTRA_STREAM, pdfUri);
            intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.whatsapp_receipt_message));
            intent.putExtra("jid", phone + "@s.whatsapp.net");
            intent.setPackage("com.whatsapp");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(intent);

        } catch (ActivityNotFoundException e) {
            runOnUiThread(() -> {
                Toast.makeText(this, getString(R.string.whatsapp_not_installed), Toast.LENGTH_SHORT).show();
            });
        } catch (Exception e) {
            Log.e("WhatsApp", getString(R.string.error_log, e.getMessage()));
        }
    }

    private String getGymName() {
        try {
            final String[] gymName = {getString(R.string.default_gym_name)};

            DatabaseReference gymRef = rootRef.child("ownerInfo").child("gymName");
            gymRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        gymName[0] = snapshot.getValue(String.class);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                }
            });

            return gymName[0];
        } catch (Exception e) {
            return getString(R.string.default_gym_name);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SMS_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("SMS", getString(R.string.sms_permission_granted_log));
            } else {
                Log.e("SMS", getString(R.string.sms_permission_denied_log));
            }
        }
    }
}