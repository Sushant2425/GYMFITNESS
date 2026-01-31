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
import androidx.viewpager2.widget.ViewPager2;

import com.coderspuxelinnnovation.gymmanagementsystem.R;
import com.coderspuxelinnnovation.gymmanagementsystem.Utils.PrefManager;
import com.coderspuxelinnnovation.gymmanagementsystem.adapters.PaymentTabsAdapter;
import com.coderspuxelinnnovation.gymmanagementsystem.models.PendingDueModel;
import com.google.android.material.appbar.MaterialToolbar;
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
import java.util.UUID;

public class PendingDuesActivity extends AppCompatActivity {

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

        // Request SMS permission
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
            showAllMonths = !showAllMonths;
            updateMonthDisplay();
            applyFilters();
        });
    }

    private void updateMonthDisplay() {
        if (showAllMonths) {
            tvCurrentMonth.setText("All Months");
            tvMonthYear.setText("Complete History");
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
                tab.setText("Pending");
            } else {
                tab.setText("Paid");
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
        searchView.setQueryHint("Search member...");

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
                "All Months",
                "This Month",
                "Last Month",
                "Last 3 Months"
        };

        new MaterialAlertDialogBuilder(this)
                .setTitle("Quick Filters")
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
                    }
                    selectedMonthYear = monthYearFormat.format(currentCalendar.getTime());
                    updateMonthDisplay();
                    applyFilters();
                })
                .setNegativeButton("Cancel", null)
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
        Toast.makeText(this, "Filters cleared", Toast.LENGTH_SHORT).show();
    }

    private void setupFirebase() {
        ownerEmail = new PrefManager(this).getUserEmail().replace(".", ",");
        rootRef = FirebaseDatabase.getInstance().getReference("GYM").child(ownerEmail);
    }

    private void setupListeners() {
        fabRefresh.setOnClickListener(v -> {
            loadAllPayments();
            Toast.makeText(this, "Refreshing...", Toast.LENGTH_SHORT).show();
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

                    // Get plan type from currentPlan
                    String planType = "Regular";
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

                            // Get payment status
                            String status = paySnap.child("status").getValue(String.class);
                            due.setStatus(status != null ? status : "PENDING");

                            // Get payment mode if exists
                            String paymentMode = paySnap.child("mode").getValue(String.class);
                            if (paymentMode != null) {
                                due.setPaymentMode(paymentMode);
                            }

                            // Store plan info
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
                            "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
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
            tvTotalAmount.setText("₹" + String.format(Locale.getDefault(), "%,d", totalPending));
            tvMemberCount.setText(pending.size() + " members");
            tvTotalAmount.setTextColor(ContextCompat.getColor(this, R.color.red));
        } else {
            tvTotalAmount.setText("₹" + String.format(Locale.getDefault(), "%,d", totalPaid));
            tvMemberCount.setText(paid.size() + " members");
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

        String[] paymentModes = {"Cash", "UPI", "Card", "Bank Transfer", "Cheque"};
        android.widget.ArrayAdapter<String> modeAdapter = new android.widget.ArrayAdapter<>(this,
                R.layout.dropdown_item, paymentModes);
        etPaymentMode.setAdapter(modeAdapter);
        etPaymentMode.setText("Cash", false);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Collect Payment")
                .setMessage("Member: " + due.getName() + "\nRemaining: ₹" + due.getRemaining())
                .setView(dialogView)
                .setPositiveButton("Collect", (dialog, which) -> {
                    String amountStr = etAmount.getText().toString();
                    String mode = etPaymentMode.getText().toString();
                    String notes = etNotes.getText().toString();

                    if (amountStr.isEmpty()) {
                        Toast.makeText(this, "Enter amount", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    try {
                        int amount = Integer.parseInt(amountStr);
                        if (amount > due.getRemaining()) {
                            Toast.makeText(this, "Amount exceeds due", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (mode.isEmpty()) mode = "Cash";
                        collectPayment(due, amount, mode, notes);
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

// In the collectPayment method of PendingDuesActivity, update the payment structure:

    private void collectPayment(PendingDueModel due, int amount, String mode, String notes) {
        progressBar.setVisibility(View.VISIBLE);

        DatabaseReference memberRef = rootRef.child("members").child(due.getMemberId());

        // Get the payment details
        memberRef.child("payments").child(due.getPaymentId()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(PendingDuesActivity.this, "Payment record not found", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                // Get current values - SAME structure as PaymentActivity
                Integer currentPaid = snapshot.child("amountPaid").getValue(Integer.class);
                Integer currentRemaining = snapshot.child("remaining").getValue(Integer.class);
                Integer totalFee = snapshot.child("totalFee").getValue(Integer.class);
                String planId = snapshot.child("planId").getValue(String.class);
                String planStartDate = snapshot.child("planStartDate").getValue(String.class);
                String forMonth = snapshot.child("forMonth").getValue(String.class);

                if (currentRemaining == null || totalFee == null) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(PendingDuesActivity.this, "Invalid payment data", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                // Calculate new values
                int newPaidAmount = (currentPaid != null ? currentPaid : 0) + amount;
                int newRemaining = Math.max(0, currentRemaining - amount);
                String status = newRemaining == 0 ? "PAID" : "PARTIAL";

                // Generate transaction ID
                String transactionId = UUID.randomUUID().toString();

                // Prepare updates - SAME structure as PaymentActivity
                HashMap<String, Object> childUpdates = new HashMap<>();
                childUpdates.put("payments/" + due.getPaymentId() + "/amountPaid", newPaidAmount);
                childUpdates.put("payments/" + due.getPaymentId() + "/remaining", newRemaining);
                childUpdates.put("payments/" + due.getPaymentId() + "/status", status);
                childUpdates.put("payments/" + due.getPaymentId() + "/mode", mode);
                childUpdates.put("payments/" + due.getPaymentId() + "/lastPaymentDate", System.currentTimeMillis());
                childUpdates.put("payments/" + due.getPaymentId() + "/lastPaymentMode", mode);
                childUpdates.put("payments/" + due.getPaymentId() + "/lastPaymentNotes", notes);

                // Add transaction to paymentHistory
                HashMap<String, Object> transactionData = new HashMap<>();
                transactionData.put("transactionId", transactionId);
                transactionData.put("amount", amount);
                transactionData.put("paymentMode", mode);
                transactionData.put("notes", notes);
                transactionData.put("date", System.currentTimeMillis());
                transactionData.put("remainingAfter", newRemaining);

                childUpdates.put("payments/" + due.getPaymentId() + "/paymentHistory/" + transactionId, transactionData);

                // Update all at once
                memberRef.updateChildren(childUpdates)
                        .addOnSuccessListener(aVoid -> {
                            // Update current plan if fully paid
                            if (newRemaining == 0 && planId != null) {
                                updateCurrentPlanStatus(memberRef, planId);
                            }

                            // Generate PDF receipt - SAME as PaymentActivity
                            Uri pdfUri = generatePaymentPdf(due, amount, mode, newRemaining);

                            // Send notifications - SAME as PaymentActivity
                            sendPaymentNotifications(due, amount, mode, newRemaining, pdfUri);

                            runOnUiThread(() -> {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(PendingDuesActivity.this, "✓ Payment collected", Toast.LENGTH_SHORT).show();
                                loadAllPayments(); // Refresh data
                            });
                        })
                        .addOnFailureListener(e -> {
                            runOnUiThread(() -> {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(PendingDuesActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                        });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(PendingDuesActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
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
                            // Update current plan status
                            HashMap<String, Object> planUpdate = new HashMap<>();
                            planUpdate.put("status", "ACTIVE");
                            memberRef.child("currentPlan").updateChildren(planUpdate);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    // Ignore error for optional update
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

            // Title
            canvas.drawText("GYM PAYMENT RECEIPT", 150, y, titlePaint);
            y += 40;

            // Lines
            canvas.drawLine(20, y, 575, y, paint);
            y += 30;

            canvas.drawText("Member Name : " + due.getName(), 40, y, paint); y += 25;
            canvas.drawText("Phone       : " + due.getPhone(), 40, y, paint); y += 25;
            canvas.drawText("For Month   : " + due.getForMonth(), 40, y, paint); y += 25;
            canvas.drawText("Plan Type   : " + due.getPlanType(), 40, y, paint); y += 25;

            canvas.drawLine(20, y, 575, y, paint);
            y += 30;

            canvas.drawText("Total Fee   : ₹" + due.getTotalFee(), 40, y, paint); y += 25;
            canvas.drawText("Paid Amount : ₹" + amount, 40, y, paint); y += 25;
            canvas.drawText("Remaining   : ₹" + remaining, 40, y, paint); y += 25;
            canvas.drawText("Payment Via : " + mode, 40, y, paint); y += 25;

            canvas.drawLine(20, y, 575, y, paint);
            y += 40;

            paint.setFakeBoldText(true);
            canvas.drawText("Thank you for your payment!", 150, y, paint);

            pdfDocument.finishPage(page);

            // File
            File dir = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Bills");
            if (!dir.exists()) dir.mkdirs();

            File file = new File(dir, "Payment_" + System.currentTimeMillis() + ".pdf");
            pdfDocument.writeTo(new FileOutputStream(file));
            pdfDocument.close();

            return FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    file
            );

        } catch (Exception e) {
            e.printStackTrace();
            Log.e("PDF", "Error generating PDF: " + e.getMessage());
            return null;
        }
    }

    private void sendPaymentNotifications(PendingDueModel due, int amount, String mode, int remaining, Uri pdfUri) {
        // Send SMS
        sendPaymentSMS(due, amount, mode, remaining);

        // Send WhatsApp with PDF
        if (pdfUri != null) {
            sendPaymentWhatsApp(due, pdfUri);
        }
    }

    private void sendPaymentSMS(PendingDueModel due, int amount, String mode, int remaining) {
        try {
            String gymName = getGymName();

            String message = String.format("Payment Received: ₹%d via %s for %s. Remaining: ₹%d. Thank you from %s!",
                    amount, mode, due.getForMonth(), remaining, gymName);

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
                SmsManager smsManager = SmsManager.getDefault();
                smsManager.sendTextMessage(due.getPhone(), null, message, null, null);

                runOnUiThread(() -> {
                    Toast.makeText(PendingDuesActivity.this, "📱 SMS sent to " + due.getPhone(), Toast.LENGTH_SHORT).show();
                });
            }

            Log.d("PaymentSMS", "SMS sent: " + message + " to " + due.getPhone());
        } catch (Exception e) {
            Log.e("PaymentSMS", "Error: " + e.getMessage());
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
            intent.putExtra(Intent.EXTRA_TEXT, "Here is your Gym Payment Receipt 📄");
            intent.putExtra("jid", phone + "@s.whatsapp.net");
            intent.setPackage("com.whatsapp");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(intent);

        } catch (ActivityNotFoundException e) {
            runOnUiThread(() -> {
                Toast.makeText(this, "WhatsApp not installed", Toast.LENGTH_SHORT).show();
            });
        } catch (Exception e) {
            Log.e("WhatsApp", "Error: " + e.getMessage());
        }
    }

    private String getGymName() {
        try {
            final String[] gymName = {"Sagar Gym"};

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
                    // Keep default
                }
            });

            return gymName[0];
        } catch (Exception e) {
            return "Sagar Gym";
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SMS_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("SMS", "✅ Permission GRANTED - SMS ready");
            } else {
                Log.e("SMS", "🚫 Permission DENIED");
            }
        }
    }
}