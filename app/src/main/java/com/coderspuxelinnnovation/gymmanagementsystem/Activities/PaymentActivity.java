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
import android.widget.AutoCompleteTextView;
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
    private AutoCompleteTextView spinnerPaymentMode;
    private MaterialButton btnSave;
    private int totalFee;
    private ProgressBar progressBar;
    private static final int SMS_PERMISSION_CODE = 100;
    private String memberPhone;
    private MaterialToolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        initViews();
        setupPaymentModeSpinner();
        loadIntentData();
        setupCheckboxes();
        setupToolbar();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, SMS_PERMISSION_CODE);
            Log.d("SMS", getString(R.string.sms_permission_requested));
        } else {
            Log.d("SMS", getString(R.string.sms_permission_granted_log));
        }

        setupListeners();
        updateRemainingAmount();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        etTotalFee = findViewById(R.id.etTotalFee);
        etPaidAmount = findViewById(R.id.etPaidAmount);
        etRemainingAmount = findViewById(R.id.etRemainingAmount);
        spinnerPaymentMode = findViewById(R.id.spinnerPaymentMode);
        btnSave = findViewById(R.id.btnSavePayment);
        progressBar = findViewById(R.id.progressBar);
        checkboxSMS = findViewById(R.id.checkbox_sms);
        checkboxWhatsApp = findViewById(R.id.checkbox_whatsapp);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        toolbar.setNavigationOnClickListener(v -> {
            String paidText = etPaidAmount.getText().toString().trim();
            if (!TextUtils.isEmpty(paidText)) {
                showExitConfirmationDialog();
            } else {
                finish();
            }
        });
    }

    private void showExitConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.discard_changes_title));
        builder.setMessage(getString(R.string.discard_changes_message));
        builder.setPositiveButton(getString(R.string.exit), (dialog, which) -> finish());
        builder.setNegativeButton(getString(R.string.cancel), null);
        builder.show();
    }

    private void setupCheckboxes() {
        checkboxSMS.setChecked(true);
        checkboxWhatsApp.setChecked(false);
    }

    private String appendPlanSuffix(String basePlanId) {
        return basePlanId + "-01";
    }

    private void setupPaymentModeSpinner() {
        String[] modes = {getString(R.string.cash), getString(R.string.online)};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, modes);
        spinnerPaymentMode.setAdapter(adapter);
        spinnerPaymentMode.setText(modes[0], false);
    }

    private void loadIntentData() {
        totalFee = getIntent().getIntExtra("totalFee", 0);
        etTotalFee.setText(getString(R.string.rupee_prefix) + totalFee);
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
                etRemainingAmount.setText(getString(R.string.rupee_prefix) + remaining);
            } catch (NumberFormatException e) {
                etRemainingAmount.setText(getString(R.string.rupee_prefix) + totalFee);
            }
        } else {
            etRemainingAmount.setText(getString(R.string.rupee_prefix) + totalFee);
        }
    }

    private void savePayment() {
        String paidText = etPaidAmount.getText().toString().trim();
        String paymentMode = spinnerPaymentMode.getText().toString().trim();

        if (TextUtils.isEmpty(paidText)) {
            etPaidAmount.setError(getString(R.string.error_enter_amount));
            return;
        }

        if (TextUtils.isEmpty(paymentMode)) {
            spinnerPaymentMode.setError(getString(R.string.select_payment_mode));
            return;
        }

        try {
            int paidAmount = Integer.parseInt(paidText);
            if (paidAmount <= 0) {
                etPaidAmount.setError(getString(R.string.amount_greater_than_zero));
                return;
            }

            if (paidAmount > totalFee) {
                etPaidAmount.setError(getString(R.string.paid_amount_exceeds));
                return;
            }

            showProgress();

            saveCompleteMemberData(paidAmount, totalFee - paidAmount, paymentMode);
        } catch (NumberFormatException e) {
            etPaidAmount.setError(getString(R.string.invalid_amount));
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
        String ownerEmail = new PrefManager(this).getUserEmail().replace(".", ",");
        String paymentId = UUID.randomUUID().toString();
        String finalPlanId = appendPlanSuffix(planId);

        HashMap<String, Object> paymentData = new HashMap<>();
        paymentData.put("paymentId", paymentId);
        paymentData.put("amountPaid", paidAmount);
        paymentData.put("totalFee", totalFee);
        paymentData.put("remaining", remaining);
        paymentData.put("mode", mode);
        paymentData.put("date", System.currentTimeMillis());
        paymentData.put("status", getString(R.string.paid_status));
        paymentData.put("forMonth", getForMonth(planStartDate));
        paymentData.put("planStartDate", planStartDate);
        paymentData.put("planId", finalPlanId);

        DatabaseReference memberRef = FirebaseDatabase.getInstance()
                .getReference("GYM")
                .child(ownerEmail)
                .child("members")
                .child(memberPhone);

        HashMap<String, Object> memberInfo = new HashMap<>();
        memberInfo.put("name", getIntent().getStringExtra("name"));
        memberInfo.put("phone", memberPhone);
        memberInfo.put("email", getIntent().getStringExtra("email"));
        memberInfo.put("gender", getIntent().getStringExtra("gender"));
        memberInfo.put("joinDate", getIntent().getStringExtra("joinDate"));
        memberInfo.put("status", "ACTIVE");

        HashMap<String, Object> currentPlan = new HashMap<>();
        currentPlan.put("planId", finalPlanId);
        currentPlan.put("planType", getIntent().getStringExtra("planType"));
        currentPlan.put("startDate", planStartDate);
        currentPlan.put("endDate", getIntent().getStringExtra("endDate"));
        currentPlan.put("totalFee", totalFee);
        currentPlan.put("status", "ACTIVE");

        memberRef.child("info").setValue(memberInfo)
                .addOnSuccessListener(unused -> {
                    memberRef.child("currentPlan").setValue(currentPlan)
                            .addOnSuccessListener(unused2 -> {
                                memberRef.child("payments")
                                        .child(finalPlanId)
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
                                                        sendPdfOnWhatsApp(pdfUri);
                                                    }

                                                    if (checkboxSMS.isChecked()) {
                                                        sendWelcomeSMS(paidAmount);
                                                    }
                                                });
                                            }).start();
                                        })
                                        .addOnFailureListener(e -> {
                                            hideProgress();
                                            Toast.makeText(PaymentActivity.this,
                                                    getString(R.string.payment_failed, e.getMessage()),
                                                    Toast.LENGTH_SHORT).show();
                                        });
                            })
                            .addOnFailureListener(e -> {
                                hideProgress();
                                Toast.makeText(PaymentActivity.this,
                                        getString(R.string.plan_save_failed, e.getMessage()),
                                        Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    hideProgress();
                    Toast.makeText(PaymentActivity.this,
                            getString(R.string.member_info_failed, e.getMessage()),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void sendPdfOnWhatsApp(Uri pdfUri) {
        try {
            if (memberPhone == null || memberPhone.length() < 10) {
                Toast.makeText(this, getString(R.string.invalid_phone_number), Toast.LENGTH_SHORT).show();
                return;
            }

            String phone = memberPhone.replaceAll("[^0-9]", "");
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
            Toast.makeText(this, getString(R.string.whatsapp_not_installed), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.whatsapp_error, e.getMessage()), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void sendWelcomeSMS(int paidAmount) {
        Log.d("SMS_DEBUG", getString(R.string.sending_to, memberPhone));

        if (memberPhone == null || memberPhone.length() < 10) {
            Log.e("SMS", getString(R.string.invalid_phone_error, memberPhone));
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, getString(R.string.enable_sms_permission), Toast.LENGTH_SHORT).show();
            return;
        }

        String ownerEmail = new PrefManager(this).getUserEmail().replace(".", ",");
        DatabaseReference gymRef = FirebaseDatabase.getInstance()
                .getReference("GYM").child(ownerEmail).child("ownerInfo").child("gymName");

        gymRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String gymName = snapshot.getValue(String.class);
                if (gymName == null) gymName = getString(R.string.default_gym_name);

                String message = getString(R.string.welcome_sms_message, gymName, planStartDate, paidAmount);
                sendSmsDirect(message);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                String message = getString(R.string.welcome_sms_message_fallback, planStartDate, totalFee);
                sendSmsDirect(message);
            }
        });
    }

    private void sendSmsDirect(String message) {
        try {
            SmsManager.getDefault().sendTextMessage(memberPhone, null, message, null, null);
            Toast.makeText(PaymentActivity.this, getString(R.string.sms_sent_to_phone, memberPhone), Toast.LENGTH_SHORT).show();
            Log.d("SMS", getString(R.string.sms_sent_log, message));
        } catch (Exception e) {
            Toast.makeText(PaymentActivity.this, getString(R.string.sms_failed_message, e.getMessage()), Toast.LENGTH_SHORT).show();
            Log.e("SMS", getString(R.string.sms_error_log, e.getMessage()));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SMS_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("SMS", getString(R.string.sms_permission_granted_log));
                Toast.makeText(this, getString(R.string.sms_permission_ok), Toast.LENGTH_SHORT).show();
            } else {
                Log.e("SMS", getString(R.string.sms_permission_denied_log));
                Toast.makeText(this, getString(R.string.sms_permission_denied_message), Toast.LENGTH_LONG).show();
            }
        }
    }

    private Uri generatePaymentPdf(String memberName, String phone, int paidAmount, int remaining, String paymentMode) {
        try {
            if (memberName == null) memberName = getString(R.string.member_default);
            if (phone == null) phone = getString(R.string.na);
            if (paymentMode == null) paymentMode = getString(R.string.cash);

            PdfDocument pdfDocument = new PdfDocument();
            Paint paint = new Paint();
            Paint titlePaint = new Paint();
            Paint headerPaint = new Paint();
            Paint footerPaint = new Paint();

            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
            PdfDocument.Page page = pdfDocument.startPage(pageInfo);
            Canvas canvas = page.getCanvas();

            Paint bgPaint = new Paint();
            bgPaint.setColor(0xFFF8F9FA);
            canvas.drawRect(0, 0, 595, 842, bgPaint);

            int primaryColor = 0xFF2C3E50;
            int secondaryColor = 0xFF3498DB;
            int accentColor = 0xFF2ECC71;
            int grayColor = 0xFF7F8C8D;
            int lightGrayColor = 0xFFECF0F1;

            Paint titleBg = new Paint();
            titleBg.setColor(primaryColor);
            canvas.drawRect(0, 0, 595, 120, titleBg);

            titlePaint.setTextSize(28);
            titlePaint.setColor(0xFFFFFFFF);
            titlePaint.setFakeBoldText(true);
            titlePaint.setTextAlign(Paint.Align.CENTER);

            String gymName = getString(R.string.fitness_center);
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
            canvas.drawText(getString(R.string.pdf_title), 297, 85, titlePaint);

            paint.setTextSize(12);
            paint.setColor(grayColor);

            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
            String currentDate = sdf.format(new Date());
            String receiptNo = getString(R.string.receipt_prefix) + (System.currentTimeMillis() % 100000);

            canvas.drawText(getString(R.string.receipt_no, receiptNo), 40, 140, paint);
            canvas.drawText(getString(R.string.date_label, currentDate), 400, 140, paint);

            Paint linePaint = new Paint();
            linePaint.setColor(0xFF3498DB);
            linePaint.setStrokeWidth(2);
            canvas.drawLine(40, 150, 555, 150, linePaint);

            int y = 180;

            headerPaint.setTextSize(16);
            headerPaint.setColor(primaryColor);
            headerPaint.setFakeBoldText(true);
            canvas.drawText(getString(R.string.member_info_section), 40, y, headerPaint);
            y += 30;

            Paint infoBoxPaint = new Paint();
            infoBoxPaint.setColor(lightGrayColor);
            canvas.drawRoundRect(new RectF(40, y - 10, 555, y + 100), 8, 8, infoBoxPaint);

            paint.setTextSize(14);
            paint.setColor(0xFF2C3E50);

            canvas.drawText(getString(R.string.name_label), 60, y + 20, paint);
            paint.setFakeBoldText(true);
            canvas.drawText(memberName, 150, y + 20, paint);
            paint.setFakeBoldText(false);

            canvas.drawText(getString(R.string.phone_label), 60, y + 45, paint);
            canvas.drawText(phone, 150, y + 45, paint);

            canvas.drawText(getString(R.string.plan_label), 60, y + 70, paint);
            String planType = getIntent().getStringExtra("planType");
            canvas.drawText(planType != null ? planType : getString(R.string.regular_plan), 150, y + 70, paint);

            canvas.drawText(getString(R.string.start_date_label), 300, y + 20, paint);
            canvas.drawText(planStartDate != null ? planStartDate : getString(R.string.na), 390, y + 20, paint);

            canvas.drawText(getString(R.string.duration_label), 300, y + 45, paint);
            String duration = getIntent().getStringExtra("duration");
            canvas.drawText(duration != null ? duration : getString(R.string.one_month), 390, y + 45, paint);

            y += 120;

            canvas.drawText(getString(R.string.payment_details_section), 40, y, headerPaint);
            y += 30;

            Paint tableHeaderPaint = new Paint();
            tableHeaderPaint.setColor(secondaryColor);
            canvas.drawRect(40, y, 555, y + 30, tableHeaderPaint);

            Paint headerTextPaint = new Paint();
            headerTextPaint.setColor(0xFFFFFFFF);
            headerTextPaint.setTextSize(14);
            headerTextPaint.setFakeBoldText(true);

            canvas.drawText(getString(R.string.description_label), 60, y + 20, headerTextPaint);
            canvas.drawText(getString(R.string.amount_label), 400, y + 20, headerTextPaint);

            y += 40;

            paint.setTextSize(14);
            paint.setColor(0xFF2C3E50);

            canvas.drawText(getString(R.string.total_fee_label), 60, y, paint);
            canvas.drawText(String.valueOf(totalFee), 420, y, paint);
            y += 25;

            canvas.drawText(getString(R.string.amount_paid_label), 60, y, paint);
            paint.setColor(accentColor);
            paint.setFakeBoldText(true);
            canvas.drawText(String.valueOf(paidAmount), 420, y, paint);
            paint.setColor(0xFF2C3E50);
            paint.setFakeBoldText(false);
            y += 25;

            canvas.drawText(getString(R.string.balance_due_label), 60, y, paint);
            if (remaining > 0) {
                paint.setColor(0xFFE74C3C);
            } else {
                paint.setColor(accentColor);
            }
            canvas.drawText(String.valueOf(remaining), 420, y, paint);
            paint.setColor(0xFF2C3E50);
            y += 25;

            canvas.drawLine(40, y, 555, y, linePaint);
            y += 10;

            canvas.drawText(getString(R.string.payment_mode_label), 60, y, paint);
            canvas.drawText(paymentMode, 420, y, paint);
            y += 35;

            String status = remaining > 0 ? getString(R.string.partially_paid) : getString(R.string.fully_paid);
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

            Paint footerBg = new Paint();
            footerBg.setColor(primaryColor);
            canvas.drawRect(0, y, 595, 842, footerBg);

            footerPaint.setTextSize(12);
            footerPaint.setColor(0xFFBDC3C7);
            footerPaint.setTextAlign(Paint.Align.CENTER);

            canvas.drawText(getString(R.string.thank_you_message), 297, y + 30, footerPaint);
            footerPaint.setTextSize(10);
            canvas.drawText(getString(R.string.computer_generated), 297, y + 50, footerPaint);
            canvas.drawText(getString(R.string.no_signature_required), 297, y + 65, footerPaint);

            String contactPhone = getString(R.string.na);
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

            Paint watermarkPaint = new Paint();
            watermarkPaint.setColor(0x0D000000);
            watermarkPaint.setTextSize(80);
            watermarkPaint.setTextAlign(Paint.Align.CENTER);
            watermarkPaint.setAlpha(30);
            canvas.drawText(getString(R.string.paid_watermark), 297, 500, watermarkPaint);

            pdfDocument.finishPage(page);

            File dir = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), getString(R.string.gym_receipts_folder));
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String fileName = getString(R.string.receipt_filename, memberName.replace(" ", "_"), System.currentTimeMillis());
            File file = new File(dir, fileName);

            pdfDocument.writeTo(new FileOutputStream(file));
            pdfDocument.close();

            runOnUiThread(() ->
                    Toast.makeText(this, getString(R.string.receipt_saved, fileName), Toast.LENGTH_SHORT).show()
            );

            return FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);

        } catch (Exception e) {
            e.printStackTrace();
            runOnUiThread(() ->
                    Toast.makeText(this, getString(R.string.pdf_error, e.getMessage()), Toast.LENGTH_SHORT).show()
            );
            return null;
        }
    }

    @Override
    public void onBackPressed() {
        String paidText = etPaidAmount.getText().toString().trim();
        if (!TextUtils.isEmpty(paidText)) {
            showExitConfirmationDialog();
        } else {
            super.onBackPressed();
        }
    }
}