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
import com.coderspuxelinnnovation.gymmanagementsystem.adapters.MonthlyRevenueAdapter;
import com.coderspuxelinnnovation.gymmanagementsystem.base.BaseActivity;
import com.coderspuxelinnnovation.gymmanagementsystem.models.MonthlyRevenueModel;
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MonthlyRevenueReportActivity extends BaseActivity {

    private static final int STORAGE_PERMISSION_CODE = 101;

    // UI Components
    private Spinner spinnerYear;
    private MaterialButton btnGenerateReport, btnViewPdf, btnSharePdf;
    private MaterialCardView cardSummary;
    private TextView tvTotalRevenue, tvAverageRevenue, tvHighestMonth, tvLowestMonth;
    private RecyclerView rvRevenueList;
    private ProgressBar progressBar;
    private LinearLayout reportContainer;

    private DatabaseReference databaseReference;
    private PrefManager prefManager;
    private List<MonthlyRevenueModel> revenueList = new ArrayList<>();
    private MonthlyRevenueAdapter adapter;

    private String currentPdfPath = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monthly_revenue_report);

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
        spinnerYear = findViewById(R.id.spinnerYear);
        btnGenerateReport = findViewById(R.id.btnGenerateReport);
        btnViewPdf = findViewById(R.id.btnViewPdf);
        btnSharePdf = findViewById(R.id.btnSharePdf);
        cardSummary = findViewById(R.id.cardSummary);
        tvTotalRevenue = findViewById(R.id.tvTotalRevenue);
        tvAverageRevenue = findViewById(R.id.tvAverageRevenue);
        tvHighestMonth = findViewById(R.id.tvHighestMonth);
        tvLowestMonth = findViewById(R.id.tvLowestMonth);
        rvRevenueList = findViewById(R.id.rvRevenueList);
        progressBar = findViewById(R.id.progressBar);
        reportContainer = findViewById(R.id.reportContainer);

        rvRevenueList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MonthlyRevenueAdapter(this, revenueList);
        rvRevenueList.setAdapter(adapter);
    }

    private void setupToolbar() {
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.monthly_revenue_report));
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupSpinners() {
        // Year Spinner
        ArrayList<String> years = new ArrayList<>();
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        for (int i = currentYear - 5; i <= currentYear; i++) {
            years.add(String.valueOf(i));
        }
        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, years);
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerYear.setAdapter(yearAdapter);
        spinnerYear.setSelection(years.indexOf(String.valueOf(currentYear)));
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
        int selectedYear = Integer.parseInt(spinnerYear.getSelectedItem().toString());
        progressBar.setVisibility(View.VISIBLE);
        reportContainer.setVisibility(View.GONE);
        revenueList.clear();

        loadRevenueFromFirebase(selectedYear);
    }

    private void loadRevenueFromFirebase(int year) {
        databaseReference.child("members").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                revenueList.clear();

                // Map to store monthly revenue
                Map<Integer, MonthlyRevenueModel> monthlyMap = new HashMap<>();

                // Initialize months Jan-Dec
                String[] monthNames = {"January", "February", "March", "April", "May", "June",
                        "July", "August", "September", "October", "November", "December"};

                for (int i = 1; i <= 12; i++) {
                    MonthlyRevenueModel model = new MonthlyRevenueModel();
                    model.setMonthNumber(i);
                    model.setMonthName(monthNames[i - 1]);
                    model.setYear(year);
                    model.setTotalRevenue(0);
                    model.setPaymentCount(0);
                    model.setCashAmount(0);
                    model.setUpiAmount(0);
                    model.setCardAmount(0);
                    monthlyMap.put(i, model);
                }

                for (DataSnapshot memberSnap : snapshot.getChildren()) {
                    DataSnapshot paymentsSnap = memberSnap.child("payments");
                    
                    for (DataSnapshot paySnap : paymentsSnap.getChildren()) {
                        Long paymentDate = paySnap.child("date").getValue(Long.class);
                        if (paymentDate == null) continue;

                        Calendar cal = Calendar.getInstance();
                        cal.setTimeInMillis(paymentDate);
                        int paymentYear = cal.get(Calendar.YEAR);
                        int paymentMonth = cal.get(Calendar.MONTH) + 1;

                        if (paymentYear == year) {
                            Integer amount = paySnap.child("amountPaid").getValue(Integer.class);
                            String mode = paySnap.child("mode").getValue(String.class);

                            if (amount != null && amount > 0) {
                                MonthlyRevenueModel monthModel = monthlyMap.get(paymentMonth);
                                if (monthModel != null) {
                                    monthModel.setTotalRevenue(monthModel.getTotalRevenue() + amount);
                                    monthModel.setPaymentCount(monthModel.getPaymentCount() + 1);

                                    // Categorize by payment mode
                                    if (mode != null) {
                                        if (mode.equalsIgnoreCase("Cash")) {
                                            monthModel.setCashAmount(monthModel.getCashAmount() + amount);
                                        } else if (mode.equalsIgnoreCase("UPI")) {
                                            monthModel.setUpiAmount(monthModel.getUpiAmount() + amount);
                                        } else if (mode.equalsIgnoreCase("Card")) {
                                            monthModel.setCardAmount(monthModel.getCardAmount() + amount);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Convert map to list and sort by month
                revenueList.addAll(monthlyMap.values());
                Collections.sort(revenueList, Comparator.comparingInt(MonthlyRevenueModel::getMonthNumber));

                runOnUiThread(() -> {
                    updateSummary();
                    adapter.updateList(revenueList);
                    progressBar.setVisibility(View.GONE);
                    reportContainer.setVisibility(View.VISIBLE);

                    if (revenueList.isEmpty() || getTotalRevenue() == 0) {
                        Toast.makeText(MonthlyRevenueReportActivity.this,
                                getString(R.string.no_revenue_found), Toast.LENGTH_SHORT).show();
                    } else {
                        generatePdfReport(year);
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(MonthlyRevenueReportActivity.this,
                            getString(R.string.error_with_message, error.getMessage()), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private int getTotalRevenue() {
        int total = 0;
        for (MonthlyRevenueModel model : revenueList) {
            total += model.getTotalRevenue();
        }
        return total;
    }

    private void updateSummary() {
        int totalRevenue = 0;
        int totalPayments = 0;
        int highestRevenue = 0;
        int lowestRevenue = Integer.MAX_VALUE;
        String highestMonth = "";
        String lowestMonth = "";

        for (MonthlyRevenueModel model : revenueList) {
            totalRevenue += model.getTotalRevenue();
            totalPayments += model.getPaymentCount();

            if (model.getTotalRevenue() > highestRevenue) {
                highestRevenue = model.getTotalRevenue();
                highestMonth = model.getMonthName();
            }
            if (model.getTotalRevenue() < lowestRevenue && model.getTotalRevenue() > 0) {
                lowestRevenue = model.getTotalRevenue();
                lowestMonth = model.getMonthName();
            }
        }

        int averageRevenue = totalPayments > 0 ? totalRevenue / 12 : 0;

        tvTotalRevenue.setText(getString(R.string.rupee_prefix) + String.format("%,d", totalRevenue));
        tvAverageRevenue.setText(getString(R.string.rupee_prefix) + String.format("%,d", averageRevenue));
        tvHighestMonth.setText(highestMonth + " (" + getString(R.string.rupee_prefix) + String.format("%,d", highestRevenue) + ")");
        
        if (lowestRevenue == Integer.MAX_VALUE) {
            tvLowestMonth.setText("N/A");
        } else {
            tvLowestMonth.setText(lowestMonth + " (" + getString(R.string.rupee_prefix) + String.format("%,d", lowestRevenue) + ")");
        }
    }

    private void generatePdfReport(int year) {
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
            int COLOR_POSITIVE = Color.parseColor("#4CAF50");

            // Page A4 595 × 842
            final int PW = 595, PH = 842;
            final int ML = 36, MR = 36;
            final int CONTENT_W = PW - ML - MR;

            // Column definitions
            float[] colRatios = {0.05f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f};
            float ratioSum = 0;
            for (float r : colRatios) ratioSum += r;
            float[] colW = new float[colRatios.length];
            for (int i = 0; i < colRatios.length; i++) colW[i] = (colRatios[i] / ratioSum) * CONTENT_W;

            String[] colHeaders = {"#", "Month", "Total Revenue", "Payments", "Cash", "UPI", "Card", "Avg/ Payment", "Status"};

            // Meta info
            String gymName = prefManager.getGymName();
            if (gymName == null || gymName.isEmpty()) gymName = "GYM MANAGEMENT";
            gymName = gymName.toUpperCase();

            SimpleDateFormat sdfFull = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
            String generatedOn = sdfFull.format(new Date());

            // Summary numbers
            int totalRevenue = getTotalRevenue();
            int totalPayments = 0;
            int highestRevenue = 0;
            for (MonthlyRevenueModel model : revenueList) {
                totalPayments += model.getPaymentCount();
                if (model.getTotalRevenue() > highestRevenue) highestRevenue = model.getTotalRevenue();
            }
            int avgRevenue = totalPayments > 0 ? totalRevenue / 12 : 0;

            // Page state
            int pageNumber = 1;
            int totalPages = Math.max(1, (int) Math.ceil(revenueList.size() / 25.0) + 1);

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
            canvas.drawText("MONTHLY REVENUE REPORT - " + year, ML + 10, 56, whitePaint);

            whitePaint.setTextAlign(Paint.Align.RIGHT);
            whitePaint.setTextSize(9f);
            whitePaint.setColor(Color.parseColor("#CCCCCC"));
            canvas.drawText("Generated: " + generatedOn, PW - MR, 36, whitePaint);
            canvas.drawText("Year: " + year, PW - MR, 52, whitePaint);
            canvas.drawText("Page " + pageNumber + " of " + totalPages, PW - MR, 68, whitePaint);

            accentPaint.setStrokeWidth(3f);
            canvas.drawLine(0, 100, PW, 100, accentPaint);

            int y = 118;

            // SUMMARY CARDS
            String[] cardLabels = {"TOTAL REVENUE", "AVERAGE MONTHLY", "HIGHEST MONTH", "TOTAL PAYMENTS"};
            String[] cardValues = {
                    "\u20B9" + String.format("%,d", totalRevenue),
                    "\u20B9" + String.format("%,d", avgRevenue),
                    "\u20B9" + String.format("%,d", highestRevenue),
                    String.valueOf(totalPayments)
            };
            int[] cardColors = {COLOR_ACCENT, COLOR_POSITIVE, COLOR_ACCENT, COLOR_POSITIVE};

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
                val.setTextSize(13f);
                val.setFakeBoldText(true);
                val.setColor(Color.WHITE);
                val.setTextAlign(Paint.Align.LEFT);
                canvas.drawText(cardValues[c], cx + 7, y + 40, val);
            }

            y += (int) (cardH + 14);

            // TABLE HEADER
            final float ROW_H = 22f;
            final float TEXT_OFFSET = 14f;

            bgPaint.setColor(COLOR_ACCENT);
            canvas.drawRect(ML, y, ML + CONTENT_W, y + ROW_H, bgPaint);

            Paint hdrTxt = new Paint();
            hdrTxt.setTextSize(7.5f);
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
            rowTxt.setTextSize(7.5f);
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
            for (MonthlyRevenueModel revenue : revenueList) {
                if (y + ROW_H > MAX_Y) {
                    drawRevenueFooter(canvas, PW, PH, ML, MR, gymName, pageNumber, totalPages,
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
                    canvas.drawText(gymName + " — Monthly Revenue Report (cont.)", ML + 8, 27, whitePaint);
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

                int avgPerPayment = revenue.getPaymentCount() > 0 ? 
                        revenue.getTotalRevenue() / revenue.getPaymentCount() : 0;
                
                String status = revenue.getTotalRevenue() > 0 ? "Active" : "No Revenue";

                String[] cells = {
                        String.valueOf(rowNum),
                        revenue.getMonthName(),
                        "\u20B9" + String.format("%,d", revenue.getTotalRevenue()),
                        String.valueOf(revenue.getPaymentCount()),
                        "\u20B9" + String.format("%,d", revenue.getCashAmount()),
                        "\u20B9" + String.format("%,d", revenue.getUpiAmount()),
                        "\u20B9" + String.format("%,d", revenue.getCardAmount()),
                        "\u20B9" + String.format("%,d", avgPerPayment),
                        status
                };

                colX = ML;
                for (int col = 0; col < cells.length; col++) {
                    if (col == 2) { // Revenue column - bold and orange
                        rowTxt.setFakeBoldText(true);
                        rowTxt.setColor(COLOR_ACCENT);
                    } else if (col == 8 && revenue.getTotalRevenue() == 0) { // Status column for no revenue
                        rowTxt.setFakeBoldText(false);
                        rowTxt.setColor(Color.parseColor("#9E9E9E"));
                    } else {
                        rowTxt.setFakeBoldText(false);
                        rowTxt.setColor(COLOR_BODY_TEXT);
                    }
                    canvas.drawText(cells[col], colX + 4, y + TEXT_OFFSET, rowTxt);
                    colX += colW[col];
                }

                y += (int) ROW_H;
                rowNum++;
            }

            // TOTAL ROW
            if (!revenueList.isEmpty() && y + ROW_H <= MAX_Y) {
                bgPaint.setColor(COLOR_DARK_BG);
                canvas.drawRect(ML, y, ML + CONTENT_W, y + ROW_H, bgPaint);

                Paint totalTxt = new Paint();
                totalTxt.setTextSize(9f);
                totalTxt.setFakeBoldText(true);
                totalTxt.setColor(Color.WHITE);
                totalTxt.setTextAlign(Paint.Align.LEFT);
                canvas.drawText("TOTAL", ML + 4, y + TEXT_OFFSET, totalTxt);

                float revenueX = ML;
                for (int c = 0; c < 2; c++) revenueX += colW[c];
                totalTxt.setColor(Color.parseColor("#FF9944"));
                canvas.drawText("\u20B9" + String.format("%,d", totalRevenue), revenueX + 4, y + TEXT_OFFSET, totalTxt);

                totalTxt.setColor(Color.parseColor("#AAAAAA"));
                totalTxt.setTextSize(8f);
                totalTxt.setFakeBoldText(false);
                float countX = revenueX + colW[2];
                canvas.drawText(totalPayments + " payments", countX + 4, y + TEXT_OFFSET, totalTxt);

                y += (int) ROW_H;
            }

            drawRevenueFooter(canvas, PW, PH, ML, MR, gymName, pageNumber, totalPages,
                    COLOR_FOOTER_BG, COLOR_ACCENT);
            pdfDocument.finishPage(page);

            // Save PDF
            File dir = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Reports");
            if (!dir.exists()) dir.mkdirs();

            String fileName = "Monthly_Revenue_Report_" + year + "_" + System.currentTimeMillis() + ".pdf";
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

    private void drawRevenueFooter(Canvas canvas, int PW, int PH, int ML, int MR,
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
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.monthly_revenue_report_subject));
        shareIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.monthly_revenue_report_message));
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