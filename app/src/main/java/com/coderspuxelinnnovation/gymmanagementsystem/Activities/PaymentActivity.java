package com.coderspuxelinnnovation.gymmanagementsystem.Activities;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.telephony.SmsManager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView; // Changed from MaterialAutoCompleteTextView
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.coderspuxelinnnovation.gymmanagementsystem.R;
import com.coderspuxelinnnovation.gymmanagementsystem.Utils.PrefManager;
import com.coderspuxelinnnovation.gymmanagementsystem.base.BaseActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.UUID;

public class PaymentActivity extends BaseActivity {
    private String planId, planStartDate;
    private CheckBox checkboxSMS, checkboxWhatsApp;
    private TextInputEditText etTotalFee, etPaidAmount, etRemainingAmount;
    private AutoCompleteTextView spinnerPaymentMode; // Changed type
    private MaterialButton btnSave;
    private int totalFee;
    private ProgressBar progressBar;
    private static final int SMS_PERMISSION_CODE = 100;
    private String memberPhone;
    private MaterialToolbar toolbar; // Added toolbar

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        initViews();
        setupPaymentModeSpinner();
        loadIntentData();
        setupCheckboxes();
        setupToolbar(); // Added

        // ✅ ADD THESE 10 LINES - Permission popup on activity start
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, SMS_PERMISSION_CODE);
            Log.d("SMS", "🚨 Permission requested");
        } else {
            Log.d("SMS", "✅ SMS permission already granted");
        }

        setupListeners();
        updateRemainingAmount();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar); // Initialize toolbar
        etTotalFee = findViewById(R.id.etTotalFee);
        etPaidAmount = findViewById(R.id.etPaidAmount);
        etRemainingAmount = findViewById(R.id.etRemainingAmount);
        spinnerPaymentMode = findViewById(R.id.spinnerPaymentMode);
        btnSave = findViewById(R.id.btnSavePayment);

        this.progressBar = findViewById(R.id.progressBar);
        checkboxSMS = findViewById(R.id.checkbox_sms);
        checkboxWhatsApp = findViewById(R.id.checkbox_whatsapp);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        // Handle back arrow click
        toolbar.setNavigationOnClickListener(v -> {
            // Check if there's any unsaved data
            String paidText = etPaidAmount.getText().toString().trim();
            if (!TextUtils.isEmpty(paidText)) {
                showExitConfirmationDialog();
            }
        });
    }

    private void showExitConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Discard Changes?");
        builder.setMessage("You have unsaved changes. Are you sure you want to exit?");
        builder.setPositiveButton("Exit", (dialog, which) -> finish());
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void setupCheckboxes() {
        checkboxSMS.setChecked(true);      // SMS default ON
        checkboxWhatsApp.setChecked(false); // WhatsApp default OFF
    }

    private String appendPlanSuffix(String basePlanId) {
        return basePlanId + "-01";
    }

    private void setupPaymentModeSpinner() {
        String[] modes = {"Cash", "Online"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, modes);
        spinnerPaymentMode.setAdapter(adapter);
        spinnerPaymentMode.setText(modes[0], false); // Set default value
    }

    private void loadIntentData() {
        totalFee = getIntent().getIntExtra("totalFee", 0);
        etTotalFee.setText("₹" + totalFee);
        planId = getIntent().getStringExtra("planId");
        planStartDate = getIntent().getStringExtra("startDate");
        memberPhone = getIntent().getStringExtra("phone");
    }

    private void setupListeners() {
        etPaidAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                updateRemainingAmount();
            }
        });

        btnSave.setOnClickListener(v -> savePayment());
    }

    private void updateRemainingAmount() {
        String paidText = etPaidAmount.getText().toString().trim();
        if (!TextUtils.isEmpty(paidText)) {
            try {
                int paid = Integer.parseInt(paidText);
                int remaining = Math.max(0, totalFee - paid);
                etRemainingAmount.setText("₹" + remaining);
            } catch (NumberFormatException e) {
                etRemainingAmount.setText("₹" + totalFee);
            }
        } else {
            etRemainingAmount.setText("₹" + totalFee);
        }
    }

    private void savePayment() {
        String paidText = etPaidAmount.getText().toString().trim();
        String paymentMode = spinnerPaymentMode.getText().toString().trim();

        if (TextUtils.isEmpty(paidText)) {
            etPaidAmount.setError("Enter paid amount");
            return;
        }

        if (TextUtils.isEmpty(paymentMode)) {
            spinnerPaymentMode.setError("Select payment mode");
            return;
        }

        try {
            int paidAmount = Integer.parseInt(paidText);
            if (paidAmount <= 0) {
                etPaidAmount.setError("Amount must be greater than 0");
                return;
            }

            if (paidAmount > totalFee) {
                etPaidAmount.setError("Paid amount cannot be greater than Total Fee");
                return;
            }

            showProgress();

            saveCompleteMemberData(paidAmount, totalFee - paidAmount, paymentMode);
        } catch (NumberFormatException e) {
            etPaidAmount.setError("Invalid amount");
        }
    }

    private void showProgress() {
        progressBar.setVisibility(View.VISIBLE);
        btnSave.setEnabled(false);
    }

    private void hideProgress() {
        progressBar.setVisibility(View.GONE);
        btnSave.setEnabled(true);
    }

    private String getForMonth(String startDate) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Calendar cal = Calendar.getInstance();
            cal.setTime(sdf.parse(startDate));
            return String.format(Locale.getDefault(),
                    "%04d-%02d",
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH) + 1);
        } catch (Exception e) {
            return "";
        }
    }

    private void saveCompleteMemberData(int paidAmount, int remaining, String mode) {
        // 1️⃣ FIRST declare variables
        String ownerEmail = new PrefManager(this).getUserEmail().replace(".", ",");
        String paymentId = UUID.randomUUID().toString();

        // 2️⃣ FINAL planId तयार कर
        String finalPlanId = appendPlanSuffix(planId); // 🔥 IMPORTANT

        // 3️⃣ Payment data
        HashMap<String, Object> paymentData = new HashMap<>();
        paymentData.put("paymentId", paymentId);
        paymentData.put("amountPaid", paidAmount);
        paymentData.put("totalFee", totalFee);
        paymentData.put("remaining", remaining);
        paymentData.put("mode", mode);
        paymentData.put("date", System.currentTimeMillis());
        paymentData.put("status", "PAID");
        paymentData.put("forMonth", getForMonth(planStartDate));
        paymentData.put("planStartDate", planStartDate);
        paymentData.put("planId", finalPlanId);

        // 4️⃣ Firebase reference
        DatabaseReference memberRef = FirebaseDatabase.getInstance()
                .getReference("GYM")
                .child(ownerEmail)
                .child("members")
                .child(memberPhone);

        // 5️⃣ Member info
        HashMap<String, Object> memberInfo = new HashMap<>();
        memberInfo.put("name", getIntent().getStringExtra("name"));
        memberInfo.put("phone", memberPhone);
        memberInfo.put("email", getIntent().getStringExtra("email"));
        memberInfo.put("gender", getIntent().getStringExtra("gender"));
        memberInfo.put("joinDate", getIntent().getStringExtra("joinDate"));
        memberInfo.put("status", "ACTIVE");

        // 6️⃣ Current plan (🔥 SAME finalPlanId)
        HashMap<String, Object> currentPlan = new HashMap<>();
        currentPlan.put("planId", finalPlanId);   // 🔥 FIXED
        currentPlan.put("planType", getIntent().getStringExtra("planType"));
        currentPlan.put("startDate", planStartDate);
        currentPlan.put("endDate", getIntent().getStringExtra("endDate"));
        currentPlan.put("totalFee", totalFee);
        currentPlan.put("status", "ACTIVE");

        // 7️⃣ Save sequence
        memberRef.child("info").setValue(memberInfo)
                .addOnSuccessListener(unused -> {
                    memberRef.child("currentPlan").setValue(currentPlan)
                            .addOnSuccessListener(unused2 -> {
                                memberRef.child("payments")
//                                        .child(paymentId)
                                        .child(finalPlanId)  // NEW: Use planId as key
                                        .setValue(paymentData)
                                        .addOnSuccessListener(unused3 -> {
                                            hideProgress();

                                            new Thread(() -> {
                                                Uri pdfUri = generatePaymentPdf(
                                                        getIntent().getStringExtra("name"),
                                                        memberPhone,
                                                        paidAmount,
                                                        totalFee - paidAmount,
                                                        spinnerPaymentMode.getText().toString()
                                                );

                                                runOnUiThread(() -> {
                                                    if (pdfUri != null) {

                                                        // 1️⃣ Open WhatsApp chat FIRST
                                                        if (pdfUri != null) {
                                                            sendPdfOnWhatsApp(pdfUri); // ✅ DIRECT WhatsApp with PDF
                                                        }
                                                        // ⏱ WhatsApp open hone ka time
                                                    }


                                                    // ✅ SMS only
                                                    if (checkboxSMS.isChecked()) {
                                                        sendWelcomeSMS(paidAmount);
                                                    }
                                                });
                                            }).start();




                                        })
                                        .addOnFailureListener(e -> {
                                            hideProgress();
                                            Toast.makeText(PaymentActivity.this, "❌ Payment failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        });
                            })
                            .addOnFailureListener(e -> {
                                hideProgress();
                                Toast.makeText(
                                        PaymentActivity.this,
                                        "❌ Plan save failed: " + e.getMessage(),
                                        Toast.LENGTH_SHORT
                                ).show();
                            });
                })
                .addOnFailureListener(e -> {
                    hideProgress();
                    Toast.makeText(
                            PaymentActivity.this,
                            "❌ Member info failed: " + e.getMessage(),
                            Toast.LENGTH_SHORT
                    ).show();


                });
    }

    // 🔥 1. MAIN NOTIFICATION METHOD
    private void sendPdfOnWhatsApp(Uri pdfUri) {
        try {
            if (memberPhone == null || memberPhone.length() < 10) {
                Toast.makeText(this, "Invalid phone number", Toast.LENGTH_SHORT).show();
                return;
            }

            String phone = memberPhone.replaceAll("[^0-9]", "");
            if (!phone.startsWith("91")) {
                phone = "91" + phone;
            }

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("application/pdf");
            intent.putExtra(Intent.EXTRA_STREAM, pdfUri);
            intent.putExtra(Intent.EXTRA_TEXT, "Here is your Gym Payment Receipt 📄");
            intent.putExtra("jid", phone + "@s.whatsapp.net"); // 🔥 MOST IMPORTANT
            intent.setPackage("com.whatsapp");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(intent);

        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "WhatsApp not installed", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "WhatsApp error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    // 🔥 2. WHATSAPP DIRECT CHAT (तुझ्या exact requirement प्रमाणे)

    private void sendWelcomeSMS(int paidAmount) {
        Log.d("SMS_DEBUG", "Sending to: " + memberPhone);

        if (memberPhone == null || memberPhone.length() < 10) {
            Log.e("SMS", "Invalid phone: " + memberPhone);
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "ℹ️ Enable SMS permission in Settings", Toast.LENGTH_SHORT).show();
            return;
        }

        // ✅ FETCH GYM NAME FROM FIREBASE (Sagar Gym path)
        String ownerEmail = new PrefManager(this).getUserEmail().replace(".", ",");
        DatabaseReference gymRef = FirebaseDatabase.getInstance()
                .getReference("GYM").child(ownerEmail).child("ownerInfo").child("gymName");

        gymRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String gymName = snapshot.getValue(String.class);
                if (gymName == null) gymName = "Sagar Gym";  // Fallback

                String message = String.format("Welcome to %s! Membership active from %s (₹%d paid).",
                        gymName, planStartDate, paidAmount);

                sendSmsDirect(message);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                sendSmsDirect("Welcome to Sagar Gym! Membership active from " + planStartDate + " (₹" + totalFee + " paid).");
            }
        });
    }

    private void sendSmsDirect(String message) {
        try {
            SmsManager.getDefault().sendTextMessage(memberPhone, null, message, null, null);
            Toast.makeText(PaymentActivity.this, "📱 SMS sent to " + memberPhone, Toast.LENGTH_SHORT).show();
            Log.d("SMS", "Sent: " + message);
        } catch (Exception e) {
            Toast.makeText(PaymentActivity.this, "⚠️ SMS failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e("SMS", "Error: " + e.getMessage());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SMS_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("SMS", "✅ Permission GRANTED - SMS ready");
                Toast.makeText(this, "✅ SMS permission OK", Toast.LENGTH_SHORT).show();
            } else {
                Log.e("SMS", "🚫 Permission DENIED");
                Toast.makeText(this, "⚠️ SMS permission denied - Enable manually in Settings", Toast.LENGTH_LONG).show();
            }
        }
    }
    private Uri generatePaymentPdf(
            String memberName,
            String phone,
            int paidAmount,
            int remaining,
            String paymentMode
    ) {
        try {
            // 🔥 ADD NULL CHECKS HERE FIRST
            if (memberName == null) memberName = "Member";
            if (phone == null) phone = "N/A";
            if (paymentMode == null) paymentMode = "Cash";

            PdfDocument pdfDocument = new PdfDocument();
            Paint paint = new Paint();
            Paint titlePaint = new Paint();
            Paint headerPaint = new Paint();
            Paint footerPaint = new Paint();

            // Page setup - A4 size (595x842 points = 210x297mm)
            PdfDocument.PageInfo pageInfo =
                    new PdfDocument.PageInfo.Builder(595, 842, 1).create();
            PdfDocument.Page page = pdfDocument.startPage(pageInfo);
            Canvas canvas = page.getCanvas();

            // Background color
            Paint bgPaint = new Paint();
            bgPaint.setColor(0xFFF8F9FA);
            canvas.drawRect(0, 0, 595, 842, bgPaint);

            // Colors
            int primaryColor = 0xFF2C3E50;  // Dark Blue
            int secondaryColor = 0xFF3498DB; // Blue
            int accentColor = 0xFF2ECC71;   // Green
            int grayColor = 0xFF7F8C8D;     // Gray
            int lightGrayColor = 0xFFECF0F1; // Light Gray

            // Title section with gradient effect
            Paint titleBg = new Paint();
            titleBg.setColor(primaryColor);
            canvas.drawRect(0, 0, 595, 120, titleBg);

            titlePaint.setTextSize(28);
            titlePaint.setColor(0xFFFFFFFF);
            titlePaint.setFakeBoldText(true);
            titlePaint.setTextAlign(Paint.Align.CENTER);

            // Gym Name (fetch from preferences or use default)
            String gymName = "FITNESS CENTER"; // Default
            try {
                String prefGymName = new PrefManager(this).getGymName();
                if (prefGymName != null && !prefGymName.isEmpty()) {
                    gymName = prefGymName;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            canvas.drawText(gymName.toUpperCase(), 297, 50, titlePaint);

            titlePaint.setTextSize(20);
            titlePaint.setColor(0xFFBDC3C7);
            canvas.drawText("PAYMENT RECEIPT", 297, 85, titlePaint);

            // Receipt number and date
            paint.setTextSize(12);
            paint.setColor(grayColor);

            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
            String currentDate = sdf.format(new Date());
            String receiptNo = "REC-" + System.currentTimeMillis() % 100000;

            canvas.drawText("Receipt No: " + receiptNo, 40, 140, paint);
            canvas.drawText("Date: " + currentDate, 400, 140, paint);

            // Separator line
            Paint linePaint = new Paint();
            linePaint.setColor(0xFF3498DB);
            linePaint.setStrokeWidth(2);
            canvas.drawLine(40, 150, 555, 150, linePaint);

            int y = 180;

            // Member Info Section
            headerPaint.setTextSize(16);
            headerPaint.setColor(primaryColor);
            headerPaint.setFakeBoldText(true);
            canvas.drawText("MEMBER INFORMATION", 40, y, headerPaint);
            y += 30;

            // Info box background
            Paint infoBoxPaint = new Paint();
            infoBoxPaint.setColor(lightGrayColor);
            canvas.drawRoundRect(new RectF(40, y - 10, 555, y + 100), 8, 8, infoBoxPaint);

            paint.setTextSize(14);
            paint.setColor(0xFF2C3E50);

            canvas.drawText("Name:", 60, y + 20, paint);
            paint.setFakeBoldText(true);
            canvas.drawText(memberName != null ? memberName : "N/A", 150, y + 20, paint);
            paint.setFakeBoldText(false);

            canvas.drawText("Phone:", 60, y + 45, paint);
            canvas.drawText(phone != null ? phone : "N/A", 150, y + 45, paint);

            canvas.drawText("Plan:", 60, y + 70, paint);
            String planType = getIntent().getStringExtra("planType");
            canvas.drawText(planType != null ? planType : "Regular", 150, y + 70, paint);

            canvas.drawText("Start Date:", 300, y + 20, paint);
            canvas.drawText(planStartDate != null ? planStartDate : "N/A", 390, y + 20, paint);

            canvas.drawText("Duration:", 300, y + 45, paint);
            // 🔥 FIXED LINE - ADD NULL CHECK FOR DURATION
            String duration = getIntent().getStringExtra("duration");
            canvas.drawText(duration != null ? duration : "1 Month", 390, y + 45, paint);

            y += 120;

            // Payment Details Section
            canvas.drawText("PAYMENT DETAILS", 40, y, headerPaint);
            y += 30;

            // Table header
            Paint tableHeaderPaint = new Paint();
            tableHeaderPaint.setColor(secondaryColor);
            canvas.drawRect(40, y, 555, y + 30, tableHeaderPaint);

            Paint headerTextPaint = new Paint();
            headerTextPaint.setColor(0xFFFFFFFF);
            headerTextPaint.setTextSize(14);
            headerTextPaint.setFakeBoldText(true);

            canvas.drawText("DESCRIPTION", 60, y + 20, headerTextPaint);
            canvas.drawText("AMOUNT (₹)", 400, y + 20, headerTextPaint);

            y += 40;

            // Table rows
            paint.setTextSize(14);
            paint.setColor(0xFF2C3E50);

            // Row 1: Total Fee
            canvas.drawText("Total Membership Fee", 60, y, paint);
            canvas.drawText(String.valueOf(totalFee), 420, y, paint);
            y += 25;

            // Row 2: Paid Amount
            canvas.drawText("Amount Paid", 60, y, paint);
            paint.setColor(accentColor);
            paint.setFakeBoldText(true);
            canvas.drawText(String.valueOf(paidAmount), 420, y, paint);
            paint.setColor(0xFF2C3E50);
            paint.setFakeBoldText(false);
            y += 25;

            // Row 3: Remaining Amount
            canvas.drawText("Balance Due", 60, y, paint);
            if (remaining > 0) {
                paint.setColor(0xFFE74C3C); // Red for pending
            } else {
                paint.setColor(accentColor); // Green if paid fully
            }
            canvas.drawText(String.valueOf(remaining), 420, y, paint);
            paint.setColor(0xFF2C3E50);
            y += 25;

            // Separator line
            canvas.drawLine(40, y, 555, y, linePaint);
            y += 10;

            // Row 4: Payment Mode
            canvas.drawText("Payment Mode", 60, y, paint);
            canvas.drawText(paymentMode != null ? paymentMode : "Cash", 420, y, paint);
            y += 35;

            // Status box
            String status = remaining > 0 ? "PARTIALLY PAID" : "FULLY PAID";
            int statusColor = remaining > 0 ? 0xFFF39C12 : accentColor;

            Paint statusBg = new Paint();
            statusBg.setColor(statusColor);
            canvas.drawRoundRect(new RectF(400, y - 20, 555, y + 5), 15, 15, statusBg);

            Paint statusText = new Paint();
            statusText.setColor(0xFFFFFFFF);
            statusText.setTextSize(12);
            statusText.setFakeBoldText(true);
            statusText.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(status, 477, y - 5, statusText);

            y += 40;

            // Footer section
            Paint footerBg = new Paint();
            footerBg.setColor(0xFF2C3E50);
            canvas.drawRect(0, y, 595, 842, footerBg);

            footerPaint.setTextSize(12);
            footerPaint.setColor(0xFFBDC3C7);
            footerPaint.setTextAlign(Paint.Align.CENTER);

            canvas.drawText("Thank you for your payment!", 297, y + 30, footerPaint);
            footerPaint.setTextSize(10);
            canvas.drawText("This is a computer generated receipt", 297, y + 50, footerPaint);
            canvas.drawText("No signature required", 297, y + 65, footerPaint);

            // Contact info at bottom
            String contactPhone = "N/A";
            try {
                String prefPhone = new PrefManager(this).getPhone();
                if (prefPhone != null && !prefPhone.isEmpty()) {
                    contactPhone = prefPhone;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            String contactInfo = gymName + " | " + contactPhone;
            canvas.drawText(contactInfo, 297, y + 85, footerPaint);

            // Watermark
            Paint watermarkPaint = new Paint();
            watermarkPaint.setColor(0x0D000000); // Very light gray
            watermarkPaint.setTextSize(80);
            watermarkPaint.setTextAlign(Paint.Align.CENTER);
            watermarkPaint.setAlpha(30);
            canvas.drawText("PAID", 297, 500, watermarkPaint);

            pdfDocument.finishPage(page);

            // Create directory if not exists
            File dir = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Gym Receipts");
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // Create file with timestamp
            String fileName = "Receipt_" + memberName.replace(" ", "_") +
                    "_" + System.currentTimeMillis() + ".pdf";
            File file = new File(dir, fileName);

            // Save PDF
            pdfDocument.writeTo(new FileOutputStream(file));
            pdfDocument.close();

            // Show success message
            runOnUiThread(() ->
                    Toast.makeText(this, "✅ Receipt saved: " + fileName, Toast.LENGTH_SHORT).show()
            );

            return FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    file
            );

        } catch (Exception e) {
            e.printStackTrace();
            runOnUiThread(() ->
                    Toast.makeText(this, "❌ PDF Error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
            );
            return null;
        }
    }

    @Override
    public void onBackPressed() {
        // Check if there's any unsaved data
        String paidText = etPaidAmount.getText().toString().trim();
        if (!TextUtils.isEmpty(paidText)) {
            showExitConfirmationDialog();
        } else {
            super.onBackPressed();
        }
    }
}
