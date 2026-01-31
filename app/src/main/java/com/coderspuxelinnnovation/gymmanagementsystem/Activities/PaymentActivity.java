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
                                        .child(paymentId)
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

            canvas.drawText("Member Name : " + memberName, 40, y, paint); y += 25;
            canvas.drawText("Phone       : " + phone, 40, y, paint); y += 25;
            canvas.drawText("Plan Start  : " + planStartDate, 40, y, paint); y += 25;

            canvas.drawLine(20, y, 575, y, paint);
            y += 30;

            canvas.drawText("Total Fee   : ₹" + totalFee, 40, y, paint); y += 25;
            canvas.drawText("Paid Amount : ₹" + paidAmount, 40, y, paint); y += 25;
            canvas.drawText("Remaining   : ₹" + remaining, 40, y, paint); y += 25;
            canvas.drawText("Payment Via : " + paymentMode, 40, y, paint); y += 25;

            canvas.drawLine(20, y, 575, y, paint);
            y += 40;

            paint.setFakeBoldText(true);
            canvas.drawText("Thank you for your payment!", 150, y, paint);

            pdfDocument.finishPage(page);

            // File
            File dir = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Bills");
            if (!dir.exists()) dir.mkdirs();

            File file = new File(dir, "Bill_" + System.currentTimeMillis() + ".pdf");
            pdfDocument.writeTo(new FileOutputStream(file));
            pdfDocument.close();

            return FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    file
            );

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "PDF error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
