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
import java.util.Calendar;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Locale;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.coderspuxelinnnovation.gymmanagementsystem.R;
import com.coderspuxelinnnovation.gymmanagementsystem.Utils.PrefManager;
import com.coderspuxelinnnovation.gymmanagementsystem.adapters.AllMembersReportAdapter;
import com.coderspuxelinnnovation.gymmanagementsystem.base.BaseActivity;
import com.coderspuxelinnnovation.gymmanagementsystem.models.AllMembersReportModel;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AllMembersReportActivity extends BaseActivity {

    private static final int STORAGE_PERMISSION_CODE = 101;

    // UI Components
    private Spinner spinnerStatusFilter, spinnerSortBy;
    private MaterialButton btnGenerateReport, btnViewPdf, btnSharePdf;
    private MaterialCardView cardSummary;
    private TextView tvTotalMembers, tvActiveMembers, tvExpiredMembers, tvExpiringMembers;
    private RecyclerView rvMembersList;
    private ProgressBar progressBar;
    private LinearLayout reportContainer;

    private DatabaseReference databaseReference;
    private PrefManager prefManager;
    private List<AllMembersReportModel> membersList = new ArrayList<>();
    private List<AllMembersReportModel> filteredList = new ArrayList<>();
    private AllMembersReportAdapter adapter;

    private String currentPdfPath = "";

    // Filter options
    private String[] statusFilters = {"All Members", "Active Members", "Expired Members", "Expiring Soon (7 days)"};
    private String[] sortOptions = {"Name (A-Z)", "Name (Z-A)", "Plan Type", "Join Date (Newest)", "Join Date (Oldest)", "Days Remaining"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_members_report);

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
        spinnerStatusFilter = findViewById(R.id.spinnerStatusFilter);
        spinnerSortBy = findViewById(R.id.spinnerSortBy);
        btnGenerateReport = findViewById(R.id.btnGenerateReport);
        btnViewPdf = findViewById(R.id.btnViewPdf);
        btnSharePdf = findViewById(R.id.btnSharePdf);
        cardSummary = findViewById(R.id.cardSummary);
        tvTotalMembers = findViewById(R.id.tvTotalMembers);
        tvActiveMembers = findViewById(R.id.tvActiveMembers);
        tvExpiredMembers = findViewById(R.id.tvExpiredMembers);
        tvExpiringMembers = findViewById(R.id.tvExpiringMembers);
        rvMembersList = findViewById(R.id.rvMembersList);
        progressBar = findViewById(R.id.progressBar);
        reportContainer = findViewById(R.id.reportContainer);

        rvMembersList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AllMembersReportAdapter(this, filteredList);
        rvMembersList.setAdapter(adapter);
    }

    private void setupToolbar() {
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.all_members_report));
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupSpinners() {
        // Status Filter Spinner
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, statusFilters);
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStatusFilter.setAdapter(statusAdapter);

        // Sort By Spinner
        ArrayAdapter<String> sortAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, sortOptions);
        sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSortBy.setAdapter(sortAdapter);
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
        String statusFilter = statusFilters[spinnerStatusFilter.getSelectedItemPosition()];
        String sortBy = sortOptions[spinnerSortBy.getSelectedItemPosition()];
        
        progressBar.setVisibility(View.VISIBLE);
        reportContainer.setVisibility(View.GONE);
        membersList.clear();
        filteredList.clear();

        loadMembersFromFirebase(statusFilter, sortBy);
    }

    private void loadMembersFromFirebase(String statusFilter, String sortBy) {
        databaseReference.child("members").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                membersList.clear();

                Calendar today = Calendar.getInstance();
                today.set(Calendar.HOUR_OF_DAY, 0);
                today.set(Calendar.MINUTE, 0);
                today.set(Calendar.SECOND, 0);
                today.set(Calendar.MILLISECOND, 0);
                long todayMillis = today.getTimeInMillis();

                Calendar expiringCal = Calendar.getInstance();
                expiringCal.add(Calendar.DAY_OF_MONTH, 7);
                long expiringThreshold = expiringCal.getTimeInMillis();

                for (DataSnapshot memberSnap : snapshot.getChildren()) {
                    DataSnapshot infoSnap = memberSnap.child("info");
                    
                    String name = infoSnap.child("name").getValue(String.class);
                    String phone = infoSnap.child("phone").getValue(String.class);
                    String email = infoSnap.child("email").getValue(String.class);
                    String gender = infoSnap.child("gender").getValue(String.class);
                    String joinDate = infoSnap.child("joinDate").getValue(String.class);
                    String memberStatus = infoSnap.child("status").getValue(String.class);

                    if (name == null) continue;

                    // Get current plan details
                    DataSnapshot currentPlanSnap = memberSnap.child("currentPlan");
                    String planType = "No Plan";
                    String startDate = "";
                    String endDate = "";
                    int totalFee = 0;
                    int remainingAmount = 0;
                    int daysRemaining = -1;
                    String planStatus = "INACTIVE";

                    if (currentPlanSnap.exists()) {
                        planType = currentPlanSnap.child("planType").getValue(String.class);
                        if (planType == null) planType = "Regular";
                        startDate = currentPlanSnap.child("startDate").getValue(String.class);
                        endDate = currentPlanSnap.child("endDate").getValue(String.class);
                        
                        Object feeObj = currentPlanSnap.child("totalFee").getValue();
                        if (feeObj != null) {
                            if (feeObj instanceof Long) totalFee = ((Long) feeObj).intValue();
                            else if (feeObj instanceof Integer) totalFee = (Integer) feeObj;
                        }
                        
                        Object remainingObj = currentPlanSnap.child("remaining").getValue();
                        if (remainingObj != null) {
                            if (remainingObj instanceof Long) remainingAmount = ((Long) remainingObj).intValue();
                            else if (remainingObj instanceof Integer) remainingAmount = (Integer) remainingObj;
                        }
                        
                        planStatus = currentPlanSnap.child("status").getValue(String.class);
                        if (planStatus == null) planStatus = "INACTIVE";

                        // Calculate days remaining
                        if (endDate != null && !endDate.isEmpty()) {
                            try {
                                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                                Date end = sdf.parse(endDate);
                                if (end != null) {
                                    long diff = end.getTime() - todayMillis;
                                    daysRemaining = (int) (diff / (1000 * 60 * 60 * 24));
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    AllMembersReportModel member = new AllMembersReportModel();
                    member.setName(name);
                    member.setPhone(phone);
                    member.setEmail(email);
                    member.setGender(gender);
                    member.setJoinDate(joinDate);
                    member.setMemberStatus(memberStatus != null ? memberStatus : "INACTIVE");
                    member.setPlanType(planType);
                    member.setStartDate(startDate);
                    member.setEndDate(endDate);
                    member.setTotalFee(totalFee);
                    member.setRemainingAmount(remainingAmount);
                    member.setPlanStatus(planStatus);
                    member.setDaysRemaining(daysRemaining);

                    membersList.add(member);
                }

                // Apply filter
                applyFilter(statusFilter);
                
                // Apply sort
                applySort(sortBy);
                
                // Update summary
                updateSummary();

                runOnUiThread(() -> {
                    adapter.updateList(filteredList);
                    progressBar.setVisibility(View.GONE);
                    reportContainer.setVisibility(View.VISIBLE);

                    if (filteredList.isEmpty()) {
                        Toast.makeText(AllMembersReportActivity.this,
                                getString(R.string.no_members_found_filter), Toast.LENGTH_SHORT).show();
                    } else {
                        generatePdfReport(statusFilter, sortBy);
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(AllMembersReportActivity.this,
                            getString(R.string.error_prefix, error.getMessage()), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void applyFilter(String statusFilter) {
        filteredList.clear();
        
        for (AllMembersReportModel member : membersList) {
            boolean shouldInclude = true;
            
            switch (statusFilter) {
                case "Active Members":
                    shouldInclude = "ACTIVE".equals(member.getMemberStatus());
                    break;
                case "Expired Members":
                    shouldInclude = "INACTIVE".equals(member.getMemberStatus()) || 
                                   (member.getDaysRemaining() < 0 && member.getDaysRemaining() != -1);
                    break;
                case "Expiring Soon (7 days)":
                    shouldInclude = member.getDaysRemaining() >= 0 && member.getDaysRemaining() <= 7;
                    break;
                default:
                    shouldInclude = true;
                    break;
            }
            
            if (shouldInclude) {
                filteredList.add(member);
            }
        }
    }

    private void applySort(String sortBy) {
        switch (sortBy) {
            case "Name (A-Z)":
                Collections.sort(filteredList, Comparator.comparing(AllMembersReportModel::getName));
                break;
            case "Name (Z-A)":
                Collections.sort(filteredList, (a, b) -> b.getName().compareTo(a.getName()));
                break;
            case "Plan Type":
                Collections.sort(filteredList, Comparator.comparing(AllMembersReportModel::getPlanType));
                break;
            case "Join Date (Newest)":
                Collections.sort(filteredList, (a, b) -> {
                    if (a.getJoinDate() == null) return 1;
                    if (b.getJoinDate() == null) return -1;
                    return b.getJoinDate().compareTo(a.getJoinDate());
                });
                break;
            case "Join Date (Oldest)":
                Collections.sort(filteredList, (a, b) -> {
                    if (a.getJoinDate() == null) return 1;
                    if (b.getJoinDate() == null) return -1;
                    return a.getJoinDate().compareTo(b.getJoinDate());
                });
                break;
            case "Days Remaining":
                Collections.sort(filteredList, (a, b) -> Integer.compare(b.getDaysRemaining(), a.getDaysRemaining()));
                break;
        }
    }

    private void updateSummary() {
        int total = membersList.size();
        int active = 0;
        int expired = 0;
        int expiring = 0;

        for (AllMembersReportModel member : membersList) {
            if ("ACTIVE".equals(member.getMemberStatus())) {
                active++;
                if (member.getDaysRemaining() >= 0 && member.getDaysRemaining() <= 7) {
                    expiring++;
                }
            } else {
                expired++;
            }
        }

        tvTotalMembers.setText(String.valueOf(total));
        tvActiveMembers.setText(String.valueOf(active));
        tvExpiredMembers.setText(String.valueOf(expired));
        tvExpiringMembers.setText(String.valueOf(expiring));
    }

    private void generatePdfReport(String statusFilter, String sortBy) {
        try {
            PdfDocument pdfDocument = new PdfDocument();

            // Colors
            int COLOR_ACCENT = Color.parseColor("#FF6B00");
            int COLOR_DARK_BG = Color.parseColor("#1A1A1A");
            int COLOR_BODY_TEXT = Color.parseColor("#1A1A1A");
            int COLOR_SUBTEXT = Color.parseColor("#555555");
            int COLOR_ROW_ODD = Color.parseColor("#FFFFFF");
            int COLOR_ROW_EVEN = Color.parseColor("#FFF4EC");
            int COLOR_BORDER = Color.parseColor("#E0E0E0");
            int COLOR_FOOTER_BG = Color.parseColor("#F5F5F5");
            int COLOR_ACTIVE = Color.parseColor("#4CAF50");
            int COLOR_EXPIRED = Color.parseColor("#F44336");
            int COLOR_EXPIRING = Color.parseColor("#FF9800");

            // Page A4 595 × 842
            final int PW = 595, PH = 842;
            final int ML = 36, MR = 36;
            final int CONTENT_W = PW - ML - MR;

            // Column definitions
            float[] colRatios = {0.04f, 0.14f, 0.12f, 0.10f, 0.10f, 0.09f, 0.10f, 0.10f, 0.11f, 0.10f};
            float ratioSum = 0;
            for (float r : colRatios) ratioSum += r;
            float[] colW = new float[colRatios.length];
            for (int i = 0; i < colRatios.length; i++) colW[i] = (colRatios[i] / ratioSum) * CONTENT_W;

            String[] colHeaders = {"#", "Name", "Phone", "Plan", "Join Date", "End Date", "Days Left", "Total Fee", "Remaining", "Status"};

            // Meta info
            String gymName = prefManager.getGymName();
            if (gymName == null || gymName.isEmpty()) gymName = "GYM MANAGEMENT";
            gymName = gymName.toUpperCase();

            SimpleDateFormat sdfFull = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
            String generatedOn = sdfFull.format(new Date());

            // Page state
            int pageNumber = 1;
            int totalPages = Math.max(1, (int) Math.ceil(filteredList.size() / 22.0) + 1);

            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(PW, PH, pageNumber).create();
            PdfDocument.Page page = pdfDocument.startPage(pageInfo);
            Canvas canvas = page.getCanvas();
            canvas.drawColor(Color.WHITE);

            Paint bgPaint = new Paint();
            Paint accentPaint = new Paint();
            Paint whitePaint = new Paint();

            // HEADER BLOCK
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
            canvas.drawText("ALL MEMBERS REPORT", ML + 10, 56, whitePaint);

            whitePaint.setTextAlign(Paint.Align.RIGHT);
            whitePaint.setTextSize(9f);
            whitePaint.setColor(Color.parseColor("#CCCCCC"));
            canvas.drawText("Generated: " + generatedOn, PW - MR, 36, whitePaint);
            canvas.drawText("Filter: " + statusFilter, PW - MR, 52, whitePaint);
            canvas.drawText("Sort: " + sortBy, PW - MR, 68, whitePaint);
            canvas.drawText("Page " + pageNumber + " of " + totalPages, PW - MR, 84, whitePaint);

            accentPaint.setStrokeWidth(3f);
            canvas.drawLine(0, 100, PW, 100, accentPaint);

            int y = 118;

            // SUMMARY CARDS
            String[] cardLabels = {"TOTAL MEMBERS", "ACTIVE", "EXPIRED", "EXPIRING (7d)"};
            String[] cardValues = {
                    tvTotalMembers.getText().toString(),
                    tvActiveMembers.getText().toString(),
                    tvExpiredMembers.getText().toString(),
                    tvExpiringMembers.getText().toString()
            };
            int[] cardColors = {COLOR_ACCENT, COLOR_ACTIVE, COLOR_EXPIRED, COLOR_EXPIRING};

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
                val.setTextSize(16f);
                val.setFakeBoldText(true);
                val.setColor(Color.WHITE);
                val.setTextAlign(Paint.Align.LEFT);
                canvas.drawText(cardValues[c], cx + 7, y + 42, val);
            }

            y += (int) (cardH + 14);

            // TABLE HEADER
            final float ROW_H = 22f;
            final float TEXT_OFFSET = 14f;

            bgPaint.setColor(COLOR_ACCENT);
            canvas.drawRect(ML, y, ML + CONTENT_W, y + ROW_H, bgPaint);

            Paint hdrTxt = new Paint();
            hdrTxt.setTextSize(7.2f);
            hdrTxt.setFakeBoldText(true);
            hdrTxt.setColor(Color.WHITE);
            hdrTxt.setTextAlign(Paint.Align.LEFT);

            float colX = ML;
            for (int col = 0; col < colHeaders.length; col++) {
                canvas.drawText(colHeaders[col], colX + 4, y + TEXT_OFFSET, hdrTxt);
                colX += colW[col];
            }
            y += (int) ROW_H;

            // TABLE ROWS
            Paint rowTxt = new Paint();
            rowTxt.setTextSize(7.2f);
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
            for (AllMembersReportModel member : filteredList) {
                if (y + ROW_H > MAX_Y) {
                    drawMembersFooter(canvas, PW, PH, ML, MR, gymName, pageNumber, totalPages,
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
                    canvas.drawText(gymName + " — All Members Report (cont.)", ML + 8, 27, whitePaint);
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

                // Determine status color
                int statusColor;
                String statusDisplay;
                if ("ACTIVE".equals(member.getMemberStatus())) {
                    if (member.getDaysRemaining() >= 0 && member.getDaysRemaining() <= 7) {
                        statusColor = COLOR_EXPIRING;
                        statusDisplay = "Expiring";
                    } else {
                        statusColor = COLOR_ACTIVE;
                        statusDisplay = "Active";
                    }
                } else {
                    statusColor = COLOR_EXPIRED;
                    statusDisplay = "Expired";
                }

                String daysLeft = member.getDaysRemaining() >= 0 ? member.getDaysRemaining() + "d" : "-";

                String[] cells = {
                        String.valueOf(rowNum),
                        truncate(member.getName(), 14),
                        member.getPhone() != null ? member.getPhone() : "-",
                        truncate(member.getPlanType(), 8),
                        member.getJoinDate() != null ? member.getJoinDate() : "-",
                        member.getEndDate() != null ? member.getEndDate() : "-",
                        daysLeft,
                        "\u20B9" + String.format("%,d", member.getTotalFee()),
                        member.getRemainingAmount() > 0 ? "\u20B9" + String.format("%,d", member.getRemainingAmount()) : "-",
                        statusDisplay
                };

                colX = ML;
                for (int col = 0; col < cells.length; col++) {
                    if (col == 9) { // Status column
                        rowTxt.setColor(statusColor);
                        rowTxt.setFakeBoldText(true);
                    } else if (col == 7 || col == 8) { // Fee columns
                        rowTxt.setColor(COLOR_ACCENT);
                        rowTxt.setFakeBoldText(false);
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

            if (filteredList.isEmpty()) {
                rowTxt.setTextAlign(Paint.Align.CENTER);
                rowTxt.setColor(COLOR_SUBTEXT);
                rowTxt.setTextSize(11f);
                canvas.drawText("No members found for selected filter.", PW / 2f, y + 30, rowTxt);
                y += 50;
            }

            drawMembersFooter(canvas, PW, PH, ML, MR, gymName, pageNumber, totalPages,
                    COLOR_FOOTER_BG, COLOR_ACCENT);
            pdfDocument.finishPage(page);

            // Save PDF
            File dir = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Reports");
            if (!dir.exists()) dir.mkdirs();

            String fileName = "All_Members_Report_" + System.currentTimeMillis() + ".pdf";
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

    private void drawMembersFooter(Canvas canvas, int PW, int PH, int ML, int MR,
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
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.all_members_report_subject));
        shareIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.all_members_report_message));
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