package com.coderspuxelinnnovation.gymmanagementsystem.Activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.coderspuxelinnnovation.gymmanagementsystem.R;
import com.coderspuxelinnnovation.gymmanagementsystem.Utils.PrefManager;
import com.coderspuxelinnnovation.gymmanagementsystem.adapters.PaymentReportAdapter;
import com.coderspuxelinnnovation.gymmanagementsystem.base.BaseActivity;
import com.coderspuxelinnnovation.gymmanagementsystem.models.PaymentReportModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
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
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PaymentCollectionReportActivity extends BaseActivity {

    private static final int STORAGE_PERMISSION_CODE = 101;

    // UI Components
    private Spinner spinnerReportType, spinnerMonth, spinnerYear;
    private MaterialButton btnGenerateReport, btnViewPdf, btnSharePdf;
    private MaterialCardView cardSummary;
    private TextView tvTotalAmount, tvTotalPayments, tvCashAmount, tvUpiAmount, tvCardAmount;
    private RecyclerView rvPayments;
    private ProgressBar progressBar;
    private LinearLayout reportContainer;

    private DatabaseReference databaseReference;
    private PrefManager prefManager;
    private List<PaymentReportModel> paymentList = new ArrayList<>();
    private PaymentReportAdapter adapter;

    private String currentPdfPath = "";

    // Report Types
    private String[] reportTypes = {"Daily", "Weekly", "Monthly", "Yearly", "Custom Range"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_collection_report);

        requestStoragePermission();
        initViews();
        setupToolbar();
        setupSpinners();
        setupFirebase();
        setupListeners();
    }

    private void requestStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    STORAGE_PERMISSION_CODE);
        }
    }

    private void initViews() {
        spinnerReportType = findViewById(R.id.spinnerReportType);
        spinnerMonth = findViewById(R.id.spinnerMonth);
        spinnerYear = findViewById(R.id.spinnerYear);
        btnGenerateReport = findViewById(R.id.btnGenerateReport);
        btnViewPdf = findViewById(R.id.btnViewPdf);
        btnSharePdf = findViewById(R.id.btnSharePdf);
        cardSummary = findViewById(R.id.cardSummary);
        tvTotalAmount = findViewById(R.id.tvTotalAmount);
        tvTotalPayments = findViewById(R.id.tvTotalPayments);
        tvCashAmount = findViewById(R.id.tvCashAmount);
        tvUpiAmount = findViewById(R.id.tvUpiAmount);
        tvCardAmount = findViewById(R.id.tvCardAmount);
        rvPayments = findViewById(R.id.rvPayments);
        progressBar = findViewById(R.id.progressBar);
        reportContainer = findViewById(R.id.reportContainer);

        rvPayments.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PaymentReportAdapter(this, paymentList);
        rvPayments.setAdapter(adapter);
    }

    private void setupToolbar() {
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.payment_collection_report));
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupSpinners() {
        // Report Type Spinner
        ArrayAdapter<String> reportAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, reportTypes);
        reportAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerReportType.setAdapter(reportAdapter);

        // Month Spinner
        String[] months = {"January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"};
        ArrayAdapter<String> monthAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, months);
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMonth.setAdapter(monthAdapter);
        spinnerMonth.setSelection(Calendar.getInstance().get(Calendar.MONTH));

        // Year Spinner
        ArrayList<String> years = new ArrayList<>();
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        for (int i = currentYear - 5; i <= currentYear + 1; i++) {
            years.add(String.valueOf(i));
        }
        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, years);
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerYear.setAdapter(yearAdapter);
        spinnerYear.setSelection(years.indexOf(String.valueOf(currentYear)));

        // Show/hide month/year based on report type
        spinnerReportType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = reportTypes[position];
                if (selected.equals("Monthly") || selected.equals("Custom Range")) {
                    findViewById(R.id.layoutMonthYear).setVisibility(View.VISIBLE);
                } else {
                    findViewById(R.id.layoutMonthYear).setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupFirebase() {
        prefManager = new PrefManager(this);
        String ownerEmail = prefManager.getUserEmail();
        if (ownerEmail != null) {
            databaseReference = FirebaseDatabase.getInstance()
                    .getReference("GYM")
                    .child(ownerEmail.replace(".", ","));
        }
    }

    private void setupListeners() {
        btnGenerateReport.setOnClickListener(v -> generateReport());
        btnViewPdf.setOnClickListener(v -> viewPdf());
        btnSharePdf.setOnClickListener(v -> sharePdf());
    }

    private void generateReport() {
        String reportType = reportTypes[spinnerReportType.getSelectedItemPosition()];
        progressBar.setVisibility(View.VISIBLE);
        reportContainer.setVisibility(View.GONE);
        paymentList.clear();

        long startDate = getStartDate(reportType);
        long endDate = System.currentTimeMillis();

        loadPaymentsFromFirebase(startDate, endDate);
    }

    private long getStartDate(String reportType) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        switch (reportType) {
            case "Daily":
                return cal.getTimeInMillis();
            case "Weekly":
                cal.add(Calendar.DAY_OF_MONTH, -7);
                return cal.getTimeInMillis();
            case "Monthly":
                int selectedMonth = spinnerMonth.getSelectedItemPosition();
                int selectedYear = Integer.parseInt(spinnerYear.getSelectedItem().toString());
                cal.set(selectedYear, selectedMonth, 1);
                return cal.getTimeInMillis();
            case "Yearly":
                cal.add(Calendar.YEAR, -1);
                return cal.getTimeInMillis();
            default:
                cal.add(Calendar.DAY_OF_MONTH, -30);
                return cal.getTimeInMillis();
        }
    }

    private void loadPaymentsFromFirebase(long startDate, long endDate) {
        databaseReference.child("members").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                paymentList.clear();

                for (DataSnapshot memberSnap : snapshot.getChildren()) {
                    String memberName = memberSnap.child("info").child("name").getValue(String.class);
                    String memberPhone = memberSnap.child("info").child("phone").getValue(String.class);

                    if (memberName == null) continue;

                    // Get plan type from currentPlan (member's active plan)
                    String memberPlanType = "Regular";
                    DataSnapshot currentPlanSnap = memberSnap.child("currentPlan");
                    if (currentPlanSnap.exists()) {
                        String planType = currentPlanSnap.child("planType").getValue(String.class);
                        if (planType != null && !planType.isEmpty()) {
                            memberPlanType = planType;
                        }
                    }

                    DataSnapshot paymentsSnap = memberSnap.child("payments");
                    for (DataSnapshot paySnap : paymentsSnap.getChildren()) {
                        Long paymentDate = paySnap.child("date").getValue(Long.class);
                        if (paymentDate == null) continue;

                        if (paymentDate >= startDate && paymentDate <= endDate) {
                            Integer amount = paySnap.child("amountPaid").getValue(Integer.class);
                            String mode = paySnap.child("mode").getValue(String.class);
                            String forMonth = paySnap.child("forMonth").getValue(String.class);
                            String planType = paySnap.child("planType").getValue(String.class); // Try from payment
                            String status = paySnap.child("status").getValue(String.class);

                            // If planType is not found in payment node, use member's current plan
                            if (planType == null || planType.isEmpty()) {
                                planType = memberPlanType;
                            }

                            if (amount != null && amount > 0) {
                                PaymentReportModel payment = new PaymentReportModel();
                                payment.setMemberName(memberName);
                                payment.setMemberPhone(memberPhone);
                                payment.setAmount(amount);
                                payment.setPaymentMode(mode != null ? mode : "Cash");
                                payment.setPaymentDate(paymentDate);
                                payment.setForMonth(forMonth != null ? forMonth : "");
                                payment.setPlanType(planType != null ? planType : "Regular");
                                payment.setStatus(status != null ? status : "PAID");
                                paymentList.add(payment);
                            }
                        }
                    }
                }

                // Sort by date (newest first)
                Collections.sort(paymentList, (p1, p2) -> Long.compare(p2.getPaymentDate(), p1.getPaymentDate()));

                runOnUiThread(() -> {
                    updateSummary();
                    adapter.updateList(paymentList);
                    progressBar.setVisibility(View.GONE);
                    reportContainer.setVisibility(View.VISIBLE);

                    if (paymentList.isEmpty()) {
                        Toast.makeText(PaymentCollectionReportActivity.this,
                                getString(R.string.no_payments_found_period), Toast.LENGTH_SHORT).show();
                    } else {
                        generatePdfReport();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(PaymentCollectionReportActivity.this,
                            getString(R.string.error_prefix, error.getMessage()), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    private void updateSummary() {
        int totalAmount = 0;
        int cashTotal = 0, upiTotal = 0, cardTotal = 0;

        for (PaymentReportModel payment : paymentList) {
            totalAmount += payment.getAmount();
            String mode = payment.getPaymentMode();
            if (mode.equalsIgnoreCase("Cash")) {
                cashTotal += payment.getAmount();
            } else if (mode.equalsIgnoreCase("UPI")) {
                upiTotal += payment.getAmount();
            } else if (mode.equalsIgnoreCase("Card")) {
                cardTotal += payment.getAmount();
            }
        }

        tvTotalAmount.setText(getString(R.string.rupee_prefix) + String.format("%,d", totalAmount));
        tvTotalPayments.setText(String.valueOf(paymentList.size()));
        tvCashAmount.setText(getString(R.string.rupee_prefix) + String.format("%,d", cashTotal));
        tvUpiAmount.setText(getString(R.string.rupee_prefix) + String.format("%,d", upiTotal));
        tvCardAmount.setText(getString(R.string.rupee_prefix) + String.format("%,d", cardTotal));
    }

    private void generatePdfReport() {
        try {
            PdfDocument pdfDocument = new PdfDocument();

            // ── Paint objects ─────────────────────────────────────────────────
            Paint bgPaint       = new Paint();
            Paint accentPaint   = new Paint();
            Paint whitePaint    = new Paint();
            Paint darkPaint     = new Paint();
            Paint grayPaint     = new Paint();
            Paint lightGrayPaint= new Paint();
            Paint linePaint     = new Paint();

            // Colors
            int COLOR_ACCENT      = Color.parseColor("#FF6B00");
            int COLOR_DARK_BG     = Color.parseColor("#1A1A1A");
            int COLOR_HEADER_TEXT = Color.WHITE;
            int COLOR_BODY_TEXT   = Color.parseColor("#1A1A1A");
            int COLOR_SUBTEXT     = Color.parseColor("#555555");
            int COLOR_ROW_ODD     = Color.parseColor("#FFFFFF");
            int COLOR_ROW_EVEN    = Color.parseColor("#FFF4EC");
            int COLOR_TABLE_HEADER= COLOR_ACCENT;
            int COLOR_BORDER      = Color.parseColor("#E0E0E0");
            int COLOR_FOOTER_BG   = Color.parseColor("#F5F5F5");

            // Page A4 595 × 842
            final int PW = 595, PH = 842;
            final int ML = 36, MR = 36; // margins left/right
            final int CONTENT_W = PW - ML - MR; // 523

            // ── Column definitions (auto proportional) ─────────────────────────
            // #  | Member Name | Phone | Amount | Mode | Date | For Month | Plan
            float[] colRatios = {0.05f, 0.20f, 0.13f, 0.10f, 0.10f, 0.13f, 0.14f, 0.15f};
            // Normalise just in case
            float ratioSum = 0;
            for (float r : colRatios) ratioSum += r;
            float[] colW = new float[colRatios.length];
            for (int i = 0; i < colRatios.length; i++) colW[i] = (colRatios[i] / ratioSum) * CONTENT_W;

            String[] colHeaders = {"#", "Member Name", "Phone", "Amount", "Mode", "Date", "For Month", "Plan"};

            // ── Helper lambdas replaced by helper vars ─────────────────────────
            String gymName = prefManager.getGymName();
            if (gymName == null || gymName.isEmpty()) gymName = "GYM MANAGEMENT";
            gymName = gymName.toUpperCase();

            String reportType = reportTypes[spinnerReportType.getSelectedItemPosition()];
            SimpleDateFormat sdfFull  = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
            SimpleDateFormat sdfDate  = new SimpleDateFormat("dd/MM/yy", Locale.getDefault());
            String generatedOn = sdfFull.format(new Date());

            String periodLabel;
            if (reportType.equals("Monthly")) {
                periodLabel = spinnerMonth.getSelectedItem().toString() + " " + spinnerYear.getSelectedItem().toString();
            } else {
                periodLabel = reportType;
            }

            // ── Summary numbers ────────────────────────────────────────────────
            int totalAmount = 0, cashTotal = 0, upiTotal = 0, cardTotal = 0;
            for (PaymentReportModel p : paymentList) {
                totalAmount += p.getAmount();
                String mode = p.getPaymentMode();
                if ("Cash".equalsIgnoreCase(mode))      cashTotal += p.getAmount();
                else if ("UPI".equalsIgnoreCase(mode))  upiTotal  += p.getAmount();
                else if ("Card".equalsIgnoreCase(mode)) cardTotal += p.getAmount();
            }

            // ── Page state ─────────────────────────────────────────────────────
            int pageNumber = 1;
            int totalPages = (int) Math.ceil(paymentList.size() / 25.0) + 1;
            if (totalPages < 1) totalPages = 1;

            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(PW, PH, pageNumber).create();
            PdfDocument.Page page = pdfDocument.startPage(pageInfo);
            Canvas canvas = page.getCanvas();
            canvas.drawColor(Color.WHITE);

            // ══════════════════════════════════════════════════════════════════
            // HEADER BLOCK (dark bar)
            // ══════════════════════════════════════════════════════════════════
            bgPaint.setColor(COLOR_DARK_BG);
            canvas.drawRect(0, 0, PW, 100, bgPaint);

            // Orange left accent strip
            accentPaint.setColor(COLOR_ACCENT);
            canvas.drawRect(0, 0, 8, 100, accentPaint);

            // Gym name
            whitePaint.setColor(Color.WHITE);
            whitePaint.setTextSize(20f);
            whitePaint.setFakeBoldText(true);
            whitePaint.setTextAlign(Paint.Align.LEFT);
            canvas.drawText(gymName, ML + 10, 36, whitePaint);

            // Sub-title
            whitePaint.setTextSize(11f);
            whitePaint.setFakeBoldText(false);
            whitePaint.setColor(Color.parseColor("#AAAAAA"));
            canvas.drawText("PAYMENT COLLECTION REPORT", ML + 10, 56, whitePaint);

            // Right side meta
            whitePaint.setTextAlign(Paint.Align.RIGHT);
            whitePaint.setTextSize(9f);
            whitePaint.setColor(Color.parseColor("#CCCCCC"));
            canvas.drawText("Generated: " + generatedOn, PW - MR, 36, whitePaint);
            canvas.drawText("Period: " + periodLabel, PW - MR, 52, whitePaint);
            canvas.drawText("Page " + pageNumber + " of " + totalPages, PW - MR, 68, whitePaint);

            // Orange bottom border of header
            accentPaint.setStrokeWidth(3f);
            canvas.drawLine(0, 100, PW, 100, accentPaint);

            int y = 118;

            // ══════════════════════════════════════════════════════════════════
            // SUMMARY CARDS ROW (only on page 1)
            // ══════════════════════════════════════════════════════════════════
            // 4 cards: Total | Cash | UPI | Card
            String[] cardLabels  = {"TOTAL COLLECTED", "CASH", "UPI", "CARD"};
            int[]    cardValues  = {totalAmount, cashTotal, upiTotal, cardTotal};
            int[]    cardCounts  = {paymentList.size(), -1, -1, -1};
            float cardW = CONTENT_W / 4.0f;
            float cardH = 58;

            for (int c = 0; c < 4; c++) {
                float cx = ML + c * cardW;

                // Card bg
                bgPaint.setColor(c == 0 ? COLOR_ACCENT : Color.parseColor("#F9F9F9"));
                Paint cardBorder = new Paint();
                cardBorder.setColor(COLOR_BORDER);
                cardBorder.setStyle(Paint.Style.STROKE);
                cardBorder.setStrokeWidth(1f);
                canvas.drawRect(cx, y, cx + cardW - 2, y + cardH, bgPaint);
                canvas.drawRect(cx, y, cx + cardW - 2, y + cardH, cardBorder);

                // Label
                Paint lbl = new Paint();
                lbl.setTextSize(8f);
                lbl.setFakeBoldText(true);
                lbl.setColor(c == 0 ? Color.parseColor("#FFD0A0") : COLOR_SUBTEXT);
                lbl.setTextAlign(Paint.Align.LEFT);
                canvas.drawText(cardLabels[c], cx + 8, y + 16, lbl);

                // Amount value
                Paint val = new Paint();
                val.setTextSize(15f);
                val.setFakeBoldText(true);
                val.setColor(c == 0 ? Color.WHITE : COLOR_BODY_TEXT);
                val.setTextAlign(Paint.Align.LEFT);
                canvas.drawText("\u20B9" + String.format("%,d", cardValues[c]), cx + 8, y + 36, val);

                // Count (only for total)
                if (c == 0) {
                    val.setTextSize(9f);
                    val.setFakeBoldText(false);
                    val.setColor(Color.parseColor("#FFD0A0"));
                    canvas.drawText(paymentList.size() + " payments", cx + 8, y + 52, val);
                }
            }

            y += (int)(cardH + 14);

            // ══════════════════════════════════════════════════════════════════
            // TABLE HEADER ROW
            // ══════════════════════════════════════════════════════════════════
            final float ROW_H = 22f;
            final float TEXT_OFFSET = 14f; // baseline inside row

            // Header background
            bgPaint.setColor(COLOR_TABLE_HEADER);
            canvas.drawRect(ML, y, ML + CONTENT_W, y + ROW_H, bgPaint);

            // Header text
            Paint hdrTxt = new Paint();
            hdrTxt.setTextSize(8.5f);
            hdrTxt.setFakeBoldText(true);
            hdrTxt.setColor(Color.WHITE);
            hdrTxt.setTextAlign(Paint.Align.LEFT);

            float colX = ML;
            for (int col = 0; col < colHeaders.length; col++) {
                canvas.drawText(colHeaders[col], colX + 4, y + TEXT_OFFSET, hdrTxt);
                colX += colW[col];
            }
            y += (int)ROW_H;

            // ══════════════════════════════════════════════════════════════════
            // TABLE ROWS
            // ══════════════════════════════════════════════════════════════════
            Paint rowTxt = new Paint();
            rowTxt.setTextSize(8f);
            rowTxt.setFakeBoldText(false);
            rowTxt.setColor(COLOR_BODY_TEXT);
            rowTxt.setTextAlign(Paint.Align.LEFT);

            Paint rowBgPaint = new Paint();
            Paint borderPt   = new Paint();
            borderPt.setColor(COLOR_BORDER);
            borderPt.setStrokeWidth(0.5f);

            final int FOOTER_H = 36; // reserve space for footer
            final int MAX_Y    = PH - FOOTER_H;

            int rowNum = 1;
            for (PaymentReportModel payment : paymentList) {

                // New page if needed
                if (y + ROW_H > MAX_Y) {
                    // Draw footer on current page
                    drawPdfFooter(canvas, PW, PH, ML, MR, gymName, pageNumber, totalPages, COLOR_FOOTER_BG, COLOR_ACCENT);
                    pdfDocument.finishPage(page);

                    pageNumber++;
                    pageInfo = new PdfDocument.PageInfo.Builder(PW, PH, pageNumber).create();
                    page = pdfDocument.startPage(pageInfo);
                    canvas = page.getCanvas();
                    canvas.drawColor(Color.WHITE);

                    // Compact header for continuation pages
                    bgPaint.setColor(COLOR_DARK_BG);
                    canvas.drawRect(0, 0, PW, 42, bgPaint);
                    accentPaint.setColor(COLOR_ACCENT);
                    canvas.drawRect(0, 0, 6, 42, accentPaint);

                    whitePaint.setTextAlign(Paint.Align.LEFT);
                    whitePaint.setTextSize(13f);
                    whitePaint.setFakeBoldText(true);
                    whitePaint.setColor(Color.WHITE);
                    canvas.drawText(gymName + " — Payment Report (cont.)", ML + 8, 27, whitePaint);
                    whitePaint.setTextAlign(Paint.Align.RIGHT);
                    whitePaint.setTextSize(9f);
                    whitePaint.setFakeBoldText(false);
                    whitePaint.setColor(Color.parseColor("#CCCCCC"));
                    canvas.drawText("Page " + pageNumber + " of " + totalPages, PW - MR, 27, whitePaint);

                    y = 55;

                    // Re-draw table header
                    bgPaint.setColor(COLOR_TABLE_HEADER);
                    canvas.drawRect(ML, y, ML + CONTENT_W, y + ROW_H, bgPaint);
                    colX = ML;
                    for (int col = 0; col < colHeaders.length; col++) {
                        canvas.drawText(colHeaders[col], colX + 4, y + TEXT_OFFSET, hdrTxt);
                        colX += colW[col];
                    }
                    y += (int)ROW_H;
                }

                // Row background
                rowBgPaint.setColor(rowNum % 2 == 0 ? COLOR_ROW_EVEN : COLOR_ROW_ODD);
                canvas.drawRect(ML, y, ML + CONTENT_W, y + ROW_H, rowBgPaint);

                // Bottom border
                canvas.drawLine(ML, y + ROW_H, ML + CONTENT_W, y + ROW_H, borderPt);

                // Row data
                // Row data
                String planDisplay = payment.getPlanType();
// Clean up plan display if needed
                if (planDisplay == null || planDisplay.isEmpty() || planDisplay.equals("null")) {
                    planDisplay = "Regular";
                }

                String[] cells = {
                        String.valueOf(rowNum),
                        truncate(payment.getMemberName(), 14),
                        payment.getMemberPhone() != null ? payment.getMemberPhone() : "-",
                        "\u20B9" + String.format("%,d", payment.getAmount()),
                        payment.getPaymentMode(),
                        sdfDate.format(new Date(payment.getPaymentDate())),
                        payment.getForMonth() != null ? payment.getForMonth() : "-",
                        truncate(planDisplay, 10)  // This will now show the correct plan name
                };

                // Bold amount
                colX = ML;
                for (int col = 0; col < cells.length; col++) {
                    rowTxt.setFakeBoldText(col == 3); // bold amount column
                    rowTxt.setColor(col == 3 ? COLOR_ACCENT : COLOR_BODY_TEXT);
                    canvas.drawText(cells[col], colX + 4, y + TEXT_OFFSET, rowTxt);
                    colX += colW[col];
                }

                y += (int)ROW_H;
                rowNum++;
            }

            // Empty state
            if (paymentList.isEmpty()) {
                rowTxt.setTextAlign(Paint.Align.CENTER);
                rowTxt.setColor(COLOR_SUBTEXT);
                rowTxt.setTextSize(11f);
                canvas.drawText("No payment records found for selected period.", PW / 2f, y + 30, rowTxt);
                y += 50;
            }

            // ══════════════════════════════════════════════════════════════════
            // TOTAL ROW at bottom of table
            // ══════════════════════════════════════════════════════════════════
            if (!paymentList.isEmpty() && y + ROW_H <= MAX_Y) {
                bgPaint.setColor(Color.parseColor("#1A1A1A"));
                canvas.drawRect(ML, y, ML + CONTENT_W, y + ROW_H, bgPaint);

                Paint totalTxt = new Paint();
                totalTxt.setTextSize(9f);
                totalTxt.setFakeBoldText(true);
                totalTxt.setColor(Color.WHITE);
                totalTxt.setTextAlign(Paint.Align.LEFT);

                canvas.drawText("TOTAL", ML + 4, y + TEXT_OFFSET, totalTxt);

                // Amount in total row at column 3 position
                float amtX = ML;
                for (int c = 0; c < 3; c++) amtX += colW[c];
                totalTxt.setColor(Color.parseColor("#FF9944"));
                canvas.drawText("\u20B9" + String.format("%,d", totalAmount), amtX + 4, y + TEXT_OFFSET, totalTxt);

                totalTxt.setColor(Color.parseColor("#AAAAAA"));
                totalTxt.setTextSize(8f);
                totalTxt.setFakeBoldText(false);
                float countX = amtX + colW[3];
                canvas.drawText(paymentList.size() + " entries", countX + 4, y + TEXT_OFFSET, totalTxt);

                y += (int)ROW_H;
            }

            // ── Footer ─────────────────────────────────────────────────────────
            drawPdfFooter(canvas, PW, PH, ML, MR, gymName, pageNumber, totalPages, COLOR_FOOTER_BG, COLOR_ACCENT);
            pdfDocument.finishPage(page);

            // ── Save ────────────────────────────────────────────────────────────
            File dir = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Reports");
            if (!dir.exists()) dir.mkdirs();

            String fileName = "Payment_Report_" + System.currentTimeMillis() + ".pdf";
            File file = new File(dir, fileName);
            pdfDocument.writeTo(new FileOutputStream(file));
            pdfDocument.close();

            currentPdfPath = file.getAbsolutePath();

            runOnUiThread(() -> {
                btnViewPdf.setEnabled(true);
                btnSharePdf.setEnabled(true);
                Toast.makeText(this, getString(R.string.pdf_generated), Toast.LENGTH_SHORT).show();
            });

        } catch (Exception e) {
            e.printStackTrace();
            runOnUiThread(() -> Toast.makeText(this,
                    getString(R.string.pdf_error, e.getMessage()), Toast.LENGTH_SHORT).show());
        }
    }

    // ── Helper: draw footer ──────────────────────────────────────────────────────
    private void drawPdfFooter(Canvas canvas, int PW, int PH, int ML, int MR,
                               String gymName, int pageNum, int totalPages,
                               int footerBg, int accentColor) {
        Paint fp = new Paint();
        fp.setColor(footerBg);
        canvas.drawRect(0, PH - 32, PW, PH, fp);

        fp.setColor(accentColor);
        fp.setStrokeWidth(1.5f);
        canvas.drawLine(0, PH - 32, PW, PH - 32, fp);

        fp.setColor(Color.parseColor("#777777"));
        fp.setTextSize(8f);
        fp.setFakeBoldText(false);
        fp.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("This is a computer generated report  •  " + gymName, ML, PH - 12, fp);

        fp.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText("Page " + pageNum + " / " + totalPages, PW - MR, PH - 12, fp);
    }

    // ── Helper: truncate text ────────────────────────────────────────────────────
    private String truncate(String text, int maxLen) {
        if (text == null) return "-";
        return text.length() > maxLen ? text.substring(0, maxLen - 1) + "…" : text;
    }
    private void viewPdf() {
        if (currentPdfPath.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_pdf_generated), Toast.LENGTH_SHORT).show();
            return;
        }

        File file = new File(currentPdfPath);
        if (!file.exists()) {
            Toast.makeText(this, getString(R.string.pdf_file_not_found), Toast.LENGTH_SHORT).show();
            return;
        }

        Uri pdfUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(pdfUri, "application/pdf");
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.no_pdf_viewer), Toast.LENGTH_SHORT).show();
        }
    }

    private void sharePdf() {
        if (currentPdfPath.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_pdf_generated), Toast.LENGTH_SHORT).show();
            return;
        }

        File file = new File(currentPdfPath);
        if (!file.exists()) {
            Toast.makeText(this, getString(R.string.pdf_file_not_found), Toast.LENGTH_SHORT).show();
            return;
        }

        Uri pdfUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("application/pdf");
        shareIntent.putExtra(Intent.EXTRA_STREAM, pdfUri);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.payment_report_subject));
        shareIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.payment_report_message));
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_pdf_via)));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, getString(R.string.storage_permission_granted), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, getString(R.string.storage_permission_denied), Toast.LENGTH_LONG).show();
            }
        }
    }
}