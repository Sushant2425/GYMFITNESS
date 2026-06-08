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
import com.coderspuxelinnnovation.gymmanagementsystem.adapters.ExpiringSoonReportAdapter;
import com.coderspuxelinnnovation.gymmanagementsystem.base.BaseActivity;
import com.coderspuxelinnnovation.gymmanagementsystem.models.ExpiringSoonReportModel;
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

public class ExpiringSoonReportActivity extends BaseActivity {

    private static final int STORAGE_PERMISSION_CODE = 101;

    // UI Components
    private Spinner spinnerDaysFilter;
    private MaterialButton btnGenerateReport, btnViewPdf, btnSharePdf;
    private MaterialCardView cardSummary;
    private TextView tvTotalExpiring, tvNext7Days, tvNext15Days, tvNext30Days;
    private RecyclerView rvExpiringMembers;
    private ProgressBar progressBar;
    private LinearLayout reportContainer;

    private DatabaseReference databaseReference;
    private PrefManager prefManager;
    private List<ExpiringSoonReportModel> expiringList = new ArrayList<>();
    private ExpiringSoonReportAdapter adapter;

    private String currentPdfPath = "";

    // Filter Options
    private String[] daysFilter = {"All Expiring", "Next 7 Days", "Next 15 Days", "Next 30 Days", "Expired Members"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expiring_soon_report);

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
        spinnerDaysFilter = findViewById(R.id.spinnerDaysFilter);
        btnGenerateReport = findViewById(R.id.btnGenerateReport);
        btnViewPdf = findViewById(R.id.btnViewPdf);
        btnSharePdf = findViewById(R.id.btnSharePdf);
        cardSummary = findViewById(R.id.cardSummary);
        tvTotalExpiring = findViewById(R.id.tvTotalExpiring);
        tvNext7Days = findViewById(R.id.tvNext7Days);
        tvNext15Days = findViewById(R.id.tvNext15Days);
        tvNext30Days = findViewById(R.id.tvNext30Days);
        rvExpiringMembers = findViewById(R.id.rvExpiringMembers);
        progressBar = findViewById(R.id.progressBar);
        reportContainer = findViewById(R.id.reportContainer);

        rvExpiringMembers.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ExpiringSoonReportAdapter(this, expiringList);
        rvExpiringMembers.setAdapter(adapter);
    }

    private void setupToolbar() {
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.expiring_soon_report));
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupSpinners() {
        ArrayAdapter<String> filterAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, daysFilter);
        filterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDaysFilter.setAdapter(filterAdapter);
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
        String filterType = daysFilter[spinnerDaysFilter.getSelectedItemPosition()];
        progressBar.setVisibility(View.VISIBLE);
        reportContainer.setVisibility(View.GONE);
        expiringList.clear();

        loadExpiringFromFirebase(filterType);
    }

    private void loadExpiringFromFirebase(String filterType) {
        databaseReference.child("members").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                expiringList.clear();

                Calendar today = Calendar.getInstance();
                today.set(Calendar.HOUR_OF_DAY, 0);
                today.set(Calendar.MINUTE, 0);
                today.set(Calendar.SECOND, 0);
                today.set(Calendar.MILLISECOND, 0);
                long todayMillis = today.getTimeInMillis();

                Calendar next7Cal = Calendar.getInstance();
                next7Cal.add(Calendar.DAY_OF_MONTH, 7);
                long next7Days = next7Cal.getTimeInMillis();

                Calendar next15Cal = Calendar.getInstance();
                next15Cal.add(Calendar.DAY_OF_MONTH, 15);
                long next15Days = next15Cal.getTimeInMillis();

                Calendar next30Cal = Calendar.getInstance();
                next30Cal.add(Calendar.DAY_OF_MONTH, 30);
                long next30Days = next30Cal.getTimeInMillis();

                int count7Days = 0, count15Days = 0, count30Days = 0;

                for (DataSnapshot memberSnap : snapshot.getChildren()) {
                    String memberName = memberSnap.child("info").child("name").getValue(String.class);
                    String memberPhone = memberSnap.child("info").child("phone").getValue(String.class);
                    String memberEmail = memberSnap.child("info").child("email").getValue(String.class);
                    String status = memberSnap.child("info").child("status").getValue(String.class);

                    if (memberName == null) continue;

                    // Only process active members
                    if (!"ACTIVE".equals(status)) continue;

                    DataSnapshot currentPlanSnap = memberSnap.child("currentPlan");
                    if (!currentPlanSnap.exists()) continue;

                    String planType = currentPlanSnap.child("planType").getValue(String.class);
                    String planId = currentPlanSnap.child("planId").getValue(String.class);
                    String startDate = currentPlanSnap.child("startDate").getValue(String.class);
                    String endDateStr = currentPlanSnap.child("endDate").getValue(String.class);
                    Integer totalFee = currentPlanSnap.child("totalFee").getValue(Integer.class);
                    Integer remaining = currentPlanSnap.child("remaining").getValue(Integer.class);

                    if (endDateStr == null) continue;

                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                        Date endDate = sdf.parse(endDateStr);
                        if (endDate == null) continue;

                        long endMillis = endDate.getTime();

                        // Calculate days remaining
                        long diffMillis = endMillis - todayMillis;
                        int daysRemaining = (int) (diffMillis / (1000 * 60 * 60 * 24));

                        // Apply filter
                        boolean shouldInclude = false;
                        switch (filterType) {
                            case "Next 7 Days":
                                shouldInclude = daysRemaining >= 0 && daysRemaining <= 7;
                                break;
                            case "Next 15 Days":
                                shouldInclude = daysRemaining >= 0 && daysRemaining <= 15;
                                break;
                            case "Next 30 Days":
                                shouldInclude = daysRemaining >= 0 && daysRemaining <= 30;
                                break;
                            case "Expired Members":
                                shouldInclude = daysRemaining < 0;
                                break;
                            default: // "All Expiring"
                                shouldInclude = daysRemaining >= 0 && daysRemaining <= 30;
                                break;
                        }

                        if (shouldInclude) {
                            ExpiringSoonReportModel member = new ExpiringSoonReportModel();
                            member.setMemberName(memberName);
                            member.setMemberPhone(memberPhone);
                            member.setMemberEmail(memberEmail);
                            member.setPlanType(planType != null ? planType : "Regular");
                            member.setPlanId(planId);
                            member.setStartDate(startDate);
                            member.setEndDate(endDateStr);
                            member.setDaysRemaining(daysRemaining);
                            member.setTotalFee(totalFee != null ? totalFee : 0);
                            member.setRemainingAmount(remaining != null ? remaining : 0);
                            expiringList.add(member);
                        }

                        // Count for summary
                        if (daysRemaining >= 0 && daysRemaining <= 7) count7Days++;
                        if (daysRemaining >= 0 && daysRemaining <= 15) count15Days++;
                        if (daysRemaining >= 0 && daysRemaining <= 30) count30Days++;

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                // Sort by days remaining (soonest first)
                Collections.sort(expiringList, Comparator.comparingInt(ExpiringSoonReportModel::getDaysRemaining));

                final int finalCount7Days = count7Days;
                final int finalCount15Days = count15Days;
                final int finalCount30Days = count30Days;

                runOnUiThread(() -> {
                    updateSummary(finalCount7Days, finalCount15Days, finalCount30Days);
                    adapter.updateList(expiringList);
                    progressBar.setVisibility(View.GONE);
                    reportContainer.setVisibility(View.VISIBLE);

                    if (expiringList.isEmpty()) {
                        Toast.makeText(ExpiringSoonReportActivity.this,
                                getString(R.string.no_expiring_members_found), Toast.LENGTH_SHORT).show();
                    } else {
                        generatePdfReport(filterType);
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(ExpiringSoonReportActivity.this,
                            getString(R.string.error_with_message, error.getMessage()), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void updateSummary(int count7Days, int count15Days, int count30Days) {
        tvTotalExpiring.setText(String.valueOf(expiringList.size()));
        tvNext7Days.setText(String.valueOf(count7Days));
        tvNext15Days.setText(String.valueOf(count15Days));
        tvNext30Days.setText(String.valueOf(count30Days));
    }

    private void generatePdfReport(String filterType) {
        try {
            PdfDocument pdfDocument = new PdfDocument();

            // Colors
            int COLOR_ACCENT       = Color.parseColor("#FF6B00");
            int COLOR_DARK_BG      = Color.parseColor("#1A1A1A");
            int COLOR_BODY_TEXT    = Color.parseColor("#1A1A1A");
            int COLOR_SUBTEXT      = Color.parseColor("#555555");
            int COLOR_ROW_ODD      = Color.parseColor("#FFFFFF");
            int COLOR_ROW_EVEN     = Color.parseColor("#FFF4EC");
            int COLOR_BORDER       = Color.parseColor("#E0E0E0");
            int COLOR_FOOTER_BG    = Color.parseColor("#F5F5F5");
            int COLOR_EXPIRING_SOON = Color.parseColor("#FF9800");
            int COLOR_CRITICAL     = Color.parseColor("#F44336");
            int COLOR_SAFE         = Color.parseColor("#4CAF50");

            // Page A4 595 × 842
            final int PW = 595, PH = 842;
            final int ML = 36, MR = 36; // margins left/right
            final int CONTENT_W = PW - ML - MR; // 523

            // Column definitions
            float[] colRatios = {0.05f, 0.20f, 0.12f, 0.12f, 0.10f, 0.12f, 0.14f, 0.15f};
            float ratioSum = 0;
            for (float r : colRatios) ratioSum += r;
            float[] colW = new float[colRatios.length];
            for (int i = 0; i < colRatios.length; i++) colW[i] = (colRatios[i] / ratioSum) * CONTENT_W;

            String[] colHeaders = {"#", "Member Name", "Phone", "Plan", "Expiry Date", "Days Left", "Total Fee", "Remaining"};

            // Meta info
            String gymName = prefManager.getGymName();
            if (gymName == null || gymName.isEmpty()) gymName = "GYM MANAGEMENT";
            gymName = gymName.toUpperCase();

            SimpleDateFormat sdfFull = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
            SimpleDateFormat sdfDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            String generatedOn = sdfFull.format(new Date());

            // Page state
            int pageNumber = 1;
            int totalPages = Math.max(1, (int) Math.ceil(expiringList.size() / 24.0) + 1);

            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(PW, PH, pageNumber).create();
            PdfDocument.Page page = pdfDocument.startPage(pageInfo);
            Canvas canvas = page.getCanvas();
            canvas.drawColor(Color.WHITE);

            Paint bgPaint = new Paint();
            Paint accentPaint = new Paint();
            Paint whitePaint = new Paint();

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
            canvas.drawText("EXPIRING SOON REPORT", ML + 10, 56, whitePaint);

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
            // SUMMARY CARDS
            // ══════════════════════════════════════════════════════════════════
            String[] cardLabels = {"TOTAL EXPIRING", "NEXT 7 DAYS", "NEXT 15 DAYS", "NEXT 30 DAYS"};
            String[] cardValues = {
                    String.valueOf(expiringList.size()),
                    tvNext7Days.getText().toString(),
                    tvNext15Days.getText().toString(),
                    tvNext30Days.getText().toString()
            };
            int[] cardColors = {COLOR_ACCENT, COLOR_EXPIRING_SOON, COLOR_EXPIRING_SOON, COLOR_SAFE};

            float cardW = CONTENT_W / 4.0f;
            float cardH = 58f;

            for (int c = 0; c < 4; c++) {
                float cx = ML + c * cardW;
                bgPaint.setColor(cardColors[c]);
                canvas.drawRect(cx, y, cx + cardW - 2, y + cardH, bgPaint);

                Paint lbl = new Paint();
                lbl.setTextSize(7.5f);
                lbl.setFakeBoldText(true);
                lbl.setColor(Color.parseColor("#FFD0A0"));
                lbl.setTextAlign(Paint.Align.LEFT);
                canvas.drawText(cardLabels[c], cx + 7, y + 15, lbl);

                Paint val = new Paint();
                val.setTextSize(18f);
                val.setFakeBoldText(true);
                val.setColor(Color.WHITE);
                val.setTextAlign(Paint.Align.LEFT);
                canvas.drawText(cardValues[c], cx + 7, y + 40, val);
            }

            y += (int) (cardH + 14);

            // ══════════════════════════════════════════════════════════════════
            // TABLE HEADER
            // ══════════════════════════════════════════════════════════════════
            final float ROW_H = 22f;
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
            y += (int) ROW_H;

            // ══════════════════════════════════════════════════════════════════
            // TABLE ROWS
            // ══════════════════════════════════════════════════════════════════
            Paint rowTxt = new Paint();
            rowTxt.setTextSize(7.8f);
            rowTxt.setFakeBoldText(false);
            rowTxt.setColor(COLOR_BODY_TEXT);
            rowTxt.setTextAlign(Paint.Align.LEFT);

            Paint rowBgPaint = new Paint();
            Paint borderPt = new Paint();
            borderPt.setColor(COLOR_BORDER);
            borderPt.setStrokeWidth(0.5f);

            final int FOOTER_H = 36;
            final int MAX_Y = PH - FOOTER_H;

            int rowNum = 1;
            for (ExpiringSoonReportModel member : expiringList) {

                if (y + ROW_H > MAX_Y) {
                    drawExpiringFooter(canvas, PW, PH, ML, MR, gymName, pageNumber, totalPages,
                            COLOR_FOOTER_BG, COLOR_ACCENT);
                    pdfDocument.finishPage(page);

                    pageNumber++;
                    pageInfo = new PdfDocument.PageInfo.Builder(PW, PH, pageNumber).create();
                    page = pdfDocument.startPage(pageInfo);
                    canvas = page.getCanvas();
                    canvas.drawColor(Color.WHITE);

                    // Compact header for continuation
                    bgPaint.setColor(COLOR_DARK_BG);
                    canvas.drawRect(0, 0, PW, 42, bgPaint);
                    accentPaint.setColor(COLOR_ACCENT);
                    canvas.drawRect(0, 0, 6, 42, accentPaint);

                    whitePaint.setTextAlign(Paint.Align.LEFT);
                    whitePaint.setTextSize(13f);
                    whitePaint.setFakeBoldText(true);
                    whitePaint.setColor(Color.WHITE);
                    canvas.drawText(gymName + " — Expiring Soon Report (cont.)", ML + 8, 27, whitePaint);
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
                    y += (int) ROW_H;
                }

                // Row background
                rowBgPaint.setColor(rowNum % 2 == 0 ? COLOR_ROW_EVEN : COLOR_ROW_ODD);
                canvas.drawRect(ML, y, ML + CONTENT_W, y + ROW_H, rowBgPaint);
                canvas.drawLine(ML, y + ROW_H, ML + CONTENT_W, y + ROW_H, borderPt);

                // Set color based on days remaining
                int daysLeft = member.getDaysRemaining();
                int daysColor;
                if (daysLeft <= 3) daysColor = COLOR_CRITICAL;
                else if (daysLeft <= 7) daysColor = COLOR_EXPIRING_SOON;
                else daysColor = COLOR_SAFE;

                String[] cells = {
                        String.valueOf(rowNum),
                        truncate(member.getMemberName(), 14),
                        member.getMemberPhone() != null ? member.getMemberPhone() : "-",
                        truncate(member.getPlanType(), 10),
                        member.getEndDate() != null ? member.getEndDate() : "-",
                        daysLeft + " days",
                        "\u20B9" + String.format("%,d", member.getTotalFee()),
                        member.getRemainingAmount() > 0 ? "\u20B9" + String.format("%,d", member.getRemainingAmount()) : "Cleared"
                };

                colX = ML;
                for (int col = 0; col < cells.length; col++) {
                    if (col == 5) { // Days left column
                        rowTxt.setColor(daysColor);
                        rowTxt.setFakeBoldText(true);
                    } else {
                        rowTxt.setColor(COLOR_BODY_TEXT);
                        rowTxt.setFakeBoldText(false);
                    }
                    canvas.drawText(cells[col], colX + 4, y + TEXT_OFFSET, rowTxt);
                    colX += colW[col];
                }

                y += (int) ROW_H;
                rowNum++;
            }

            if (expiringList.isEmpty()) {
                rowTxt.setTextAlign(Paint.Align.CENTER);
                rowTxt.setColor(COLOR_SUBTEXT);
                rowTxt.setTextSize(11f);
                canvas.drawText("No expiring members found for selected filter.", PW / 2f, y + 30, rowTxt);
                y += 50;
            }

            drawExpiringFooter(canvas, PW, PH, ML, MR, gymName, pageNumber, totalPages,
                    COLOR_FOOTER_BG, COLOR_ACCENT);
            pdfDocument.finishPage(page);

            // Save PDF
            File dir = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Reports");
            if (!dir.exists()) dir.mkdirs();

            String fileName = "Expiring_Soon_Report_" + System.currentTimeMillis() + ".pdf";
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

    private void drawExpiringFooter(Canvas canvas, int PW, int PH, int ML, int MR,
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
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.expiring_soon_report_subject));
        shareIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.expiring_soon_report_message));
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