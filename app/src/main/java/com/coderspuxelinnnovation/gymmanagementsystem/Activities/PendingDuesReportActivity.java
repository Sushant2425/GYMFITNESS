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
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.coderspuxelinnnovation.gymmanagementsystem.R;
import com.coderspuxelinnnovation.gymmanagementsystem.Utils.PrefManager;
import com.coderspuxelinnnovation.gymmanagementsystem.adapters.PendingDuesReportAdapter;
import com.coderspuxelinnnovation.gymmanagementsystem.base.BaseActivity;
import com.coderspuxelinnnovation.gymmanagementsystem.models.PendingDueReportModel;
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

public class PendingDuesReportActivity extends BaseActivity {

    private static final int STORAGE_PERMISSION_CODE = 101;

    // UI Components
    private Spinner spinnerFilterType;
    private MaterialButton btnGenerateReport, btnViewPdf, btnSharePdf;
    private MaterialCardView cardSummary;
    private TextView tvTotalDues, tvTotalMembers, tvHighDues, tvMediumDues, tvLowDues;
    private RecyclerView rvDuesList;
    private ProgressBar progressBar;
    private LinearLayout reportContainer;

    private DatabaseReference databaseReference;
    private PrefManager prefManager;
    private List<PendingDueReportModel> duesList = new ArrayList<>();
    private PendingDuesReportAdapter adapter;

    private String currentPdfPath = "";

    // Filter Types
    private String[] filterTypes = {"All Dues", "High Dues (>₹5000)", "Medium Dues (₹2000-₹5000)", "Low Dues (<₹2000)", "Critical (Overdue >30 days)"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pending_dues_report);

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
        spinnerFilterType = findViewById(R.id.spinnerFilterType);
        btnGenerateReport = findViewById(R.id.btnGenerateReport);
        btnViewPdf = findViewById(R.id.btnViewPdf);
        btnSharePdf = findViewById(R.id.btnSharePdf);
        cardSummary = findViewById(R.id.cardSummary);
        tvTotalDues = findViewById(R.id.tvTotalDues);
        tvTotalMembers = findViewById(R.id.tvTotalMembers);
        tvHighDues = findViewById(R.id.tvHighDues);
        tvMediumDues = findViewById(R.id.tvMediumDues);
        tvLowDues = findViewById(R.id.tvLowDues);
        rvDuesList = findViewById(R.id.rvDuesList);
        progressBar = findViewById(R.id.progressBar);
        reportContainer = findViewById(R.id.reportContainer);

        rvDuesList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PendingDuesReportAdapter(this, duesList);
        rvDuesList.setAdapter(adapter);
    }

    private void setupToolbar() {
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.pending_dues_report));
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupSpinners() {
        ArrayAdapter<String> filterAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, filterTypes);
        filterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilterType.setAdapter(filterAdapter);
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
        String filterType = filterTypes[spinnerFilterType.getSelectedItemPosition()];
        progressBar.setVisibility(View.VISIBLE);
        reportContainer.setVisibility(View.GONE);
        duesList.clear();

        loadDuesFromFirebase(filterType);
    }

    private void loadDuesFromFirebase(String filterType) {
        databaseReference.child("members").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                duesList.clear();

                for (DataSnapshot memberSnap : snapshot.getChildren()) {
                    String memberName = memberSnap.child("info").child("name").getValue(String.class);
                    String memberPhone = memberSnap.child("info").child("phone").getValue(String.class);
                    String memberEmail = memberSnap.child("info").child("email").getValue(String.class);

                    if (memberName == null) continue;

                    // Get plan type from currentPlan (this is the active plan)
                    String planType = "Regular";
                    DataSnapshot currentPlanSnap = memberSnap.child("currentPlan");
                    if (currentPlanSnap.exists()) {
                        String type = currentPlanSnap.child("planType").getValue(String.class);
                        if (type != null && !type.isEmpty()) {
                            planType = type;
                        }
                    }

                    DataSnapshot paymentsSnap = memberSnap.child("payments");
                    for (DataSnapshot paySnap : paymentsSnap.getChildren()) {
                        Integer remaining = paySnap.child("remaining").getValue(Integer.class);
                        if (remaining != null && remaining > 0) {
                            Integer totalFee = paySnap.child("totalFee").getValue(Integer.class);
                            Integer amountPaid = paySnap.child("amountPaid").getValue(Integer.class);
                            String forMonth = paySnap.child("forMonth").getValue(String.class);
                            String dueDate = paySnap.child("planStartDate").getValue(String.class);

                            // Also try to get plan type from payment node if available
                            String paymentPlanType = paySnap.child("planType").getValue(String.class);
                            if (paymentPlanType != null && !paymentPlanType.isEmpty()) {
                                planType = paymentPlanType;
                            }

                            PendingDueReportModel due = new PendingDueReportModel();
                            due.setMemberName(memberName);
                            due.setMemberPhone(memberPhone);
                            due.setMemberEmail(memberEmail);
                            due.setRemainingAmount(remaining);
                            due.setTotalFee(totalFee != null ? totalFee : 0);
                            due.setAmountPaid(amountPaid != null ? amountPaid : 0);
                            due.setForMonth(forMonth != null ? forMonth : "");
                            due.setPlanType(planType);  // This will now have the correct plan name
                            due.setDueDate(dueDate);
                            due.setOverdueDays(calculateOverdueDays(dueDate));

                            // Apply filter
                            if (applyFilter(due, filterType)) {
                                duesList.add(due);
                            }
                        }
                    }
                }

                // Sort by remaining amount (highest first)
                Collections.sort(duesList, (d1, d2) -> Integer.compare(d2.getRemainingAmount(), d1.getRemainingAmount()));

                runOnUiThread(() -> {
                    updateSummary(filterType);
                    adapter.updateList(duesList);
                    progressBar.setVisibility(View.GONE);
                    reportContainer.setVisibility(View.VISIBLE);

                    if (duesList.isEmpty()) {
                        Toast.makeText(PendingDuesReportActivity.this,
                                getString(R.string.no_dues_found), Toast.LENGTH_SHORT).show();
                    } else {
                        generatePdfReport(filterType);
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(PendingDuesReportActivity.this,
                            getString(R.string.error_with_message, error.getMessage()), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    private boolean applyFilter(PendingDueReportModel due, String filterType) {
        int amount = due.getRemainingAmount();
        int days = due.getOverdueDays();

        switch (filterType) {
            case "High Dues (>₹5000)":
                return amount > 5000;
            case "Medium Dues (₹2000-₹5000)":
                return amount >= 2000 && amount <= 5000;
            case "Low Dues (<₹2000)":
                return amount < 2000;
            case "Critical (Overdue >30 days)":
                return days > 30;
            default:
                return true;
        }
    }

    private int calculateOverdueDays(String dueDate) {
        if (dueDate == null || dueDate.isEmpty()) return 0;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date due = sdf.parse(dueDate);
            if (due != null && due.before(new Date())) {
                long diff = new Date().getTime() - due.getTime();
                return (int) (diff / (1000 * 60 * 60 * 24));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    private void updateSummary(String filterType) {
        int totalDues = 0;
        int highCount = 0, mediumCount = 0, lowCount = 0;

        for (PendingDueReportModel due : duesList) {
            totalDues += due.getRemainingAmount();
            int amount = due.getRemainingAmount();
            if (amount > 5000) highCount++;
            else if (amount >= 2000) mediumCount++;
            else lowCount++;
        }

        tvTotalDues.setText(getString(R.string.rupee_prefix) + String.format("%,d", totalDues));
        tvTotalMembers.setText(String.valueOf(duesList.size()));
        tvHighDues.setText(String.valueOf(highCount));
        tvMediumDues.setText(String.valueOf(mediumCount));
        tvLowDues.setText(String.valueOf(lowCount));
    }

    private void generatePdfReport(String filterType) {
        try {
            PdfDocument pdfDocument = new PdfDocument();

            // ── Colors ────────────────────────────────────────────────────────
            int COLOR_ACCENT       = Color.parseColor("#FF6B00");
            int COLOR_DARK_BG      = Color.parseColor("#1A1A1A");
            int COLOR_BODY_TEXT    = Color.parseColor("#1A1A1A");
            int COLOR_SUBTEXT      = Color.parseColor("#555555");
            int COLOR_ROW_ODD      = Color.parseColor("#FFFFFF");
            int COLOR_ROW_EVEN     = Color.parseColor("#FFF4EC");
            int COLOR_BORDER       = Color.parseColor("#E0E0E0");
            int COLOR_FOOTER_BG    = Color.parseColor("#F5F5F5");
            int COLOR_HIGH         = Color.parseColor("#D32F2F");
            int COLOR_MEDIUM       = Color.parseColor("#F57C00");
            int COLOR_LOW          = Color.parseColor("#388E3C");

            // ── Page / layout constants ───────────────────────────────────────
            final int PW = 595, PH = 842;
            final int ML = 36, MR = 36;
            final int CONTENT_W = PW - ML - MR; // 523

            // ── Column definitions (auto proportional) ────────────────────────
            // #  | Member Name | Phone | Total Fee | Paid | Due Amt | Overdue | For Month | Plan
            float[] colRatios = {0.04f, 0.18f, 0.12f, 0.10f, 0.10f, 0.11f, 0.11f, 0.14f, 0.10f};
            float ratioSum = 0;
            for (float r : colRatios) ratioSum += r;
            float[] colW = new float[colRatios.length];
            for (int i = 0; i < colRatios.length; i++) colW[i] = (colRatios[i] / ratioSum) * CONTENT_W;

            String[] colHeaders = {"#", "Member Name", "Phone", "Total Fee", "Paid", "Due Amt", "Overdue", "For Month", "Plan"};

            // ── Meta ──────────────────────────────────────────────────────────
            String gymName = prefManager.getGymName();
            if (gymName == null || gymName.isEmpty()) gymName = "GYM MANAGEMENT";
            gymName = gymName.toUpperCase();

            SimpleDateFormat sdfFull = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
            String generatedOn = sdfFull.format(new Date());

            // ── Summary numbers ───────────────────────────────────────────────
            int totalDues = 0, highCount = 0, mediumCount = 0, lowCount = 0;
            for (PendingDueReportModel d : duesList) {
                totalDues += d.getRemainingAmount();
                int amt = d.getRemainingAmount();
                if (amt > 5000)              highCount++;
                else if (amt >= 2000)        mediumCount++;
                else                          lowCount++;
            }

            // ── Page state ────────────────────────────────────────────────────
            int pageNumber = 1;
            int totalPages = Math.max(1, (int) Math.ceil(duesList.size() / 24.0));

            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(PW, PH, pageNumber).create();
            PdfDocument.Page page = pdfDocument.startPage(pageInfo);
            Canvas canvas = page.getCanvas();
            canvas.drawColor(Color.WHITE);

            Paint bgPaint     = new Paint();
            Paint accentPaint = new Paint();
            Paint whitePaint  = new Paint();

            // ══════════════════════════════════════════════════════════════════
            // HEADER BLOCK
            // ══════════════════════════════════════════════════════════════════
            bgPaint.setColor(COLOR_DARK_BG);
            canvas.drawRect(0, 0, PW, 100, bgPaint);

            accentPaint.setColor(COLOR_ACCENT);
            canvas.drawRect(0, 0, 8, 100, accentPaint);

            whitePaint.setColor(Color.WHITE);
            whitePaint.setTextSize(20f);
            whitePaint.setFakeBoldText(true);
            whitePaint.setTextAlign(Paint.Align.LEFT);
            canvas.drawText(gymName, ML + 10, 36, whitePaint);

            whitePaint.setTextSize(11f);
            whitePaint.setFakeBoldText(false);
            whitePaint.setColor(Color.parseColor("#AAAAAA"));
            canvas.drawText("PENDING DUES REPORT", ML + 10, 56, whitePaint);

            whitePaint.setTextAlign(Paint.Align.RIGHT);
            whitePaint.setTextSize(9f);
            whitePaint.setColor(Color.parseColor("#CCCCCC"));
            canvas.drawText("Generated: " + generatedOn, PW - MR, 36, whitePaint);
            canvas.drawText("Filter: " + filterType, PW - MR, 52, whitePaint);
            canvas.drawText("Page " + pageNumber + " of " + totalPages, PW - MR, 68, whitePaint);

            accentPaint.setStrokeWidth(3f);
            canvas.drawLine(0, 100, PW, 100, accentPaint);

            int y = 118;

            // ══════════════════════════════════════════════════════════════════
            // SUMMARY CARDS (page 1 only)
            // 4 cards: Total Dues | High | Medium | Low
            // ══════════════════════════════════════════════════════════════════
            String[] cardLabels  = {"TOTAL PENDING DUES", "HIGH (>₹5000)", "MEDIUM (₹2K-₹5K)", "LOW (<₹2000)"};
            String[] cardValTxt  = {
                    "\u20B9" + String.format("%,d", totalDues),
                    String.valueOf(highCount),
                    String.valueOf(mediumCount),
                    String.valueOf(lowCount)
            };
            String[] cardSubTxt  = {duesList.size() + " members", "members", "members", "members"};
            int[]    cardBgColor = {COLOR_ACCENT, COLOR_HIGH, COLOR_MEDIUM, COLOR_LOW};

            float cardW = CONTENT_W / 4.0f;
            float cardH = 58f;

            for (int c = 0; c < 4; c++) {
                float cx = ML + c * cardW;
                bgPaint.setColor(cardBgColor[c]);
                canvas.drawRect(cx, y, cx + cardW - 2, y + cardH, bgPaint);

                Paint lbl = new Paint();
                lbl.setTextSize(7.5f);
                lbl.setFakeBoldText(true);
                lbl.setColor(Color.parseColor("#FFD0A0"));
                lbl.setTextAlign(Paint.Align.LEFT);
                canvas.drawText(cardLabels[c], cx + 7, y + 15, lbl);

                Paint val = new Paint();
                val.setTextSize(15f);
                val.setFakeBoldText(true);
                val.setColor(Color.WHITE);
                val.setTextAlign(Paint.Align.LEFT);
                canvas.drawText(cardValTxt[c], cx + 7, y + 35, val);

                val.setTextSize(8.5f);
                val.setFakeBoldText(false);
                val.setColor(Color.parseColor("#FFD0A0"));
                canvas.drawText(cardSubTxt[c], cx + 7, y + 50, val);
            }

            y += (int)(cardH + 14);

            // ══════════════════════════════════════════════════════════════════
            // TABLE HEADER ROW
            // ══════════════════════════════════════════════════════════════════
            final float ROW_H       = 22f;
            final float TEXT_OFFSET = 14f;

            bgPaint.setColor(COLOR_ACCENT);
            canvas.drawRect(ML, y, ML + CONTENT_W, y + ROW_H, bgPaint);

            Paint hdrTxt = new Paint();
            hdrTxt.setTextSize(8f);
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
            Paint rowTxt    = new Paint();
            rowTxt.setTextSize(7.8f);
            rowTxt.setFakeBoldText(false);
            rowTxt.setColor(COLOR_BODY_TEXT);
            rowTxt.setTextAlign(Paint.Align.LEFT);

            Paint rowBgPaint = new Paint();
            Paint borderPt   = new Paint();
            borderPt.setColor(COLOR_BORDER);
            borderPt.setStrokeWidth(0.5f);

            final int FOOTER_H = 36;
            final int MAX_Y    = PH - FOOTER_H;

            int rowNum = 1;
            for (PendingDueReportModel due : duesList) {

                // ── New page ──────────────────────────────────────────────────
                if (y + ROW_H > MAX_Y) {
                    drawDuesPdfFooter(canvas, PW, PH, ML, MR, gymName, pageNumber, totalPages,
                            COLOR_FOOTER_BG, COLOR_ACCENT);
                    pdfDocument.finishPage(page);

                    pageNumber++;
                    pageInfo = new PdfDocument.PageInfo.Builder(PW, PH, pageNumber).create();
                    page = pdfDocument.startPage(pageInfo);
                    canvas = page.getCanvas();
                    canvas.drawColor(Color.WHITE);

                    // Compact continuation header
                    bgPaint.setColor(COLOR_DARK_BG);
                    canvas.drawRect(0, 0, PW, 42, bgPaint);
                    accentPaint.setColor(COLOR_ACCENT);
                    canvas.drawRect(0, 0, 6, 42, accentPaint);

                    whitePaint.setTextAlign(Paint.Align.LEFT);
                    whitePaint.setTextSize(13f);
                    whitePaint.setFakeBoldText(true);
                    whitePaint.setColor(Color.WHITE);
                    canvas.drawText(gymName + " — Pending Dues Report (cont.)", ML + 8, 27, whitePaint);
                    whitePaint.setTextAlign(Paint.Align.RIGHT);
                    whitePaint.setTextSize(9f);
                    whitePaint.setFakeBoldText(false);
                    whitePaint.setColor(Color.parseColor("#CCCCCC"));
                    canvas.drawText("Page " + pageNumber + " of " + totalPages, PW - MR, 27, whitePaint);

                    y = 55;

                    // Re-draw table header
                    bgPaint.setColor(COLOR_ACCENT);
                    canvas.drawRect(ML, y, ML + CONTENT_W, y + ROW_H, bgPaint);
                    colX = ML;
                    for (int col = 0; col < colHeaders.length; col++) {
                        canvas.drawText(colHeaders[col], colX + 4, y + TEXT_OFFSET, hdrTxt);
                        colX += colW[col];
                    }
                    y += (int)ROW_H;
                }

                // ── Row background ────────────────────────────────────────────
                rowBgPaint.setColor(rowNum % 2 == 0 ? COLOR_ROW_EVEN : COLOR_ROW_ODD);
                canvas.drawRect(ML, y, ML + CONTENT_W, y + ROW_H, rowBgPaint);
                canvas.drawLine(ML, y + ROW_H, ML + CONTENT_W, y + ROW_H, borderPt);

                // ── Due amount color by severity ──────────────────────────────
                int dueColor;
                int amt = due.getRemainingAmount();
                if (amt > 5000)       dueColor = COLOR_HIGH;
                else if (amt >= 2000) dueColor = COLOR_MEDIUM;
                else                   dueColor = COLOR_LOW;

                // ── Overdue badge text ────────────────────────────────────────
                String overdueText = due.getOverdueDays() > 0 ? due.getOverdueDays() + "d" : "Current";

                String[] cells = {
                        String.valueOf(rowNum),
                        truncateDues(due.getMemberName(), 13),
                        due.getMemberPhone() != null ? due.getMemberPhone() : "-",
                        "\u20B9" + String.format("%,d", due.getTotalFee()),
                        "\u20B9" + String.format("%,d", due.getAmountPaid()),
                        "\u20B9" + String.format("%,d", due.getRemainingAmount()),
                        overdueText,
                        due.getForMonth() != null ? due.getForMonth() : "-",
                        truncateDues(due.getPlanType() != null ? due.getPlanType() : "Regular", 8)  // This will now show actual plan name
                };

                colX = ML;
                for (int col = 0; col < cells.length; col++) {
                    // Due amount column (index 5) gets severity color + bold
                    if (col == 5) {
                        rowTxt.setFakeBoldText(true);
                        rowTxt.setColor(dueColor);
                    } else if (col == 6 && due.getOverdueDays() > 30) {
                        // Critical overdue in red
                        rowTxt.setFakeBoldText(true);
                        rowTxt.setColor(COLOR_HIGH);
                    } else {
                        rowTxt.setFakeBoldText(false);
                        rowTxt.setColor(COLOR_BODY_TEXT);
                    }
                    canvas.drawText(cells[col], colX + 4, y + TEXT_OFFSET, rowTxt);
                    colX += colW[col];
                }

                y += (int)ROW_H;
                rowNum++;
            }

            // ── Empty state ───────────────────────────────────────────────────
            if (duesList.isEmpty()) {
                rowTxt.setTextAlign(Paint.Align.CENTER);
                rowTxt.setColor(COLOR_SUBTEXT);
                rowTxt.setTextSize(11f);
                canvas.drawText("No pending dues found for selected filter.", PW / 2f, y + 30, rowTxt);
                y += 50;
            }

            // ── Total row ─────────────────────────────────────────────────────
            if (!duesList.isEmpty() && y + ROW_H <= MAX_Y) {
                bgPaint.setColor(COLOR_DARK_BG);
                canvas.drawRect(ML, y, ML + CONTENT_W, y + ROW_H, bgPaint);

                Paint totalTxt = new Paint();
                totalTxt.setTextSize(9f);
                totalTxt.setFakeBoldText(true);
                totalTxt.setColor(Color.WHITE);
                totalTxt.setTextAlign(Paint.Align.LEFT);
                canvas.drawText("TOTAL", ML + 4, y + TEXT_OFFSET, totalTxt);

                // Due amount at column 5 position
                float dueColX = ML;
                for (int c = 0; c < 5; c++) dueColX += colW[c];
                totalTxt.setColor(Color.parseColor("#FF9944"));
                canvas.drawText("\u20B9" + String.format("%,d", totalDues), dueColX + 4, y + TEXT_OFFSET, totalTxt);

                totalTxt.setColor(Color.parseColor("#AAAAAA"));
                totalTxt.setTextSize(8f);
                totalTxt.setFakeBoldText(false);
                float countX = dueColX + colW[5];
                canvas.drawText(duesList.size() + " members", countX + 4, y + TEXT_OFFSET, totalTxt);

                y += (int)ROW_H;
            }

            // ── Footer ────────────────────────────────────────────────────────
            drawDuesPdfFooter(canvas, PW, PH, ML, MR, gymName, pageNumber, totalPages,
                    COLOR_FOOTER_BG, COLOR_ACCENT);
            pdfDocument.finishPage(page);

            // ── Save ──────────────────────────────────────────────────────────
            File dir = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Reports");
            if (!dir.exists()) dir.mkdirs();

            String fileName = "Pending_Dues_Report_" + System.currentTimeMillis() + ".pdf";
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

    // ── Helper: footer ───────────────────────────────────────────────────────────
    private void drawDuesPdfFooter(Canvas canvas, int PW, int PH, int ML, int MR,
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

    // ── Helper: truncate ─────────────────────────────────────────────────────────
    private String truncateDues(String text, int maxLen) {
        if (text == null) return "-";
        return text.length() > maxLen ? text.substring(0, maxLen - 1) + "\u2026" : text;
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
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.pending_dues_report_subject));
        shareIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.pending_dues_report_message));
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