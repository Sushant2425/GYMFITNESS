package com.coderspuxelinnnovation.gymmanagementsystem.Activities;

import android.Manifest;
import android.app.AlertDialog;
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
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.coderspuxelinnnovation.gymmanagementsystem.R;
import com.coderspuxelinnnovation.gymmanagementsystem.Utils.PrefManager;
import com.coderspuxelinnnovation.gymmanagementsystem.base.BaseActivity;
import com.coderspuxelinnnovation.gymmanagementsystem.models.MemberSearchResult;
import com.coderspuxelinnnovation.gymmanagementsystem.models.Membercollet;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class CollectPaymentActivity extends BaseActivity {

    private EditText etSearchMember, etAmount;
    private MaterialButton btnSearchMember, btnCollectPayment;
    private TextView tvMemberName, tvMemberPhone, tvPlanType, tvTotalFee, tvTotalPaid, tvRemaining, tvPlanDates;
    private ChipGroup rgPaymentMode;
    private Chip rbCash, rbUPI, rbCard;
    private MaterialCardView cardMemberDetails, cardPaymentForm;

    private DatabaseReference databaseReference;
    private String ownerEmail;
    private String currentMemberPhone;
    private String currentMemberId;
    private String planStartDate;
    private String planId;
    private int totalFee;
    private String memberName;
    private String forMonth;
    private static final int SMS_PERMISSION_CODE = 100;

    private String paymentId;
    private int remainingAmount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collect_payment);

        initializeViews();
        setupToolbar();
        initializeFirebase();
        setupListeners();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, SMS_PERMISSION_CODE);
            Log.d("SMS", getString(R.string.sms_permission_requested));
        } else {
            Log.d("SMS", getString(R.string.sms_permission_granted_log));
        }
    }

    private void initializeViews() {
        etSearchMember = findViewById(R.id.etSearchMember);
        etAmount = findViewById(R.id.etAmount);
        btnSearchMember = findViewById(R.id.btnSearchMember);
        btnCollectPayment = findViewById(R.id.btnCollectPayment);
        tvMemberName = findViewById(R.id.tvMemberName);
        tvMemberPhone = findViewById(R.id.tvMemberPhone);
        tvPlanType = findViewById(R.id.tvPlanType);
        tvTotalFee = findViewById(R.id.tvTotalFee);
        tvTotalPaid = findViewById(R.id.tvTotalPaid);
        tvRemaining = findViewById(R.id.tvRemaining);
        rgPaymentMode = findViewById(R.id.rgPaymentMode);
        rbCash = findViewById(R.id.rbCash);
        rbUPI = findViewById(R.id.rbUPI);
        rbCard = findViewById(R.id.rbCard);
        tvPlanDates = findViewById(R.id.tvPlanDates);

        cardMemberDetails = findViewById(R.id.cardMemberDetails);
        cardPaymentForm = findViewById(R.id.cardPaymentForm);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void initializeFirebase() {
        databaseReference = FirebaseDatabase.getInstance().getReference();
        ownerEmail = new PrefManager(this).getUserEmail().replace(".", ",");
    }

    private void setupListeners() {
        btnSearchMember.setOnClickListener(v -> searchMember());
        btnCollectPayment.setOnClickListener(v -> collectPayment());

        rbCash.setChecked(true);
    }

    private void searchMember() {
        String searchQuery = etSearchMember.getText().toString().trim();

        if (searchQuery.isEmpty()) {
            Toast.makeText(this, getString(R.string.please_enter_phone_or_name), Toast.LENGTH_SHORT).show();
            return;
        }

        btnSearchMember.setEnabled(false);
        btnSearchMember.setText(getString(R.string.searching));

        DatabaseReference membersRef = databaseReference.child("GYM")
                .child(ownerEmail).child("members");

        membersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                btnSearchMember.setEnabled(true);
                btnSearchMember.setText(getString(R.string.search_member));

                List<MemberSearchResult> searchResults = new ArrayList<>();

                for (DataSnapshot memberSnapshot : snapshot.getChildren()) {
                    String memberId = memberSnapshot.getKey();
                    DataSnapshot infoSnap = memberSnapshot.child("info");

                    if (infoSnap.exists()) {
                        String phone = infoSnap.child("phone").getValue(String.class);
                        String name = infoSnap.child("name").getValue(String.class);

                        if ((phone != null && phone.contains(searchQuery)) ||
                                (name != null && name.toLowerCase().contains(searchQuery.toLowerCase()))) {

                            MemberSearchResult result = new MemberSearchResult();
                            result.memberId = memberId;
                            result.phone = phone;
                            result.name = name;
                            result.snapshot = memberSnapshot;
                            searchResults.add(result);
                        }
                    }
                }

                if (searchResults.isEmpty()) {
                    Toast.makeText(CollectPaymentActivity.this,
                            getString(R.string.no_members_found_error), Toast.LENGTH_SHORT).show();
                    cardMemberDetails.setVisibility(View.GONE);
                    cardPaymentForm.setVisibility(View.GONE);
                } else if (searchResults.size() == 1) {
                    MemberSearchResult result = searchResults.get(0);
                    currentMemberId = result.memberId;
                    currentMemberPhone = result.phone;
                    displayMemberDetails(result.snapshot, result.phone, result.name, result.memberId);
                } else {
                    showMemberSelectionDialog(searchResults);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                btnSearchMember.setEnabled(true);
                btnSearchMember.setText(getString(R.string.search_member));
                Toast.makeText(CollectPaymentActivity.this,
                        getString(R.string.error_prefix, error.getMessage()), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showMemberSelectionDialog(List<MemberSearchResult> results) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.select_member_found, results.size()));

        String[] items = new String[results.size()];
        for (int i = 0; i < results.size(); i++) {
            MemberSearchResult result = results.get(i);
            items[i] = result.name + " - " + result.phone;
        }

        builder.setItems(items, (dialog, which) -> {
            MemberSearchResult selected = results.get(which);
            currentMemberId = selected.memberId;
            currentMemberPhone = selected.phone;
            displayMemberDetails(selected.snapshot, selected.phone, selected.name, selected.memberId);
        });

        builder.setNegativeButton(getString(R.string.cancel), null);
        builder.show();
    }

    private void displayMemberDetails(DataSnapshot memberSnapshot, String phone, String name, String memberId) {
        DataSnapshot currentPlanSnap = memberSnapshot.child("currentPlan");

        if (!currentPlanSnap.exists()) {
            Toast.makeText(this, getString(R.string.no_active_plan_error), Toast.LENGTH_SHORT).show();
            cardMemberDetails.setVisibility(View.GONE);
            cardPaymentForm.setVisibility(View.GONE);
            return;
        }

        planStartDate = currentPlanSnap.child("startDate").getValue(String.class);
        String planEndDate = currentPlanSnap.child("endDate").getValue(String.class);
        Integer fee = currentPlanSnap.child("totalFee").getValue(Integer.class);
        totalFee = fee != null ? fee : 0;
        planId = currentPlanSnap.child("planId").getValue(String.class);
        String planType = currentPlanSnap.child("planType").getValue(String.class);

        memberName = name;
        forMonth = getForMonth(planStartDate);

        int totalPaidForCurrentPlan = 0;
        int remaining = totalFee;

        DataSnapshot paymentsSnap = memberSnapshot.child("payments");
        if (paymentsSnap.exists()) {
            for (DataSnapshot paymentSnap : paymentsSnap.getChildren()) {
                String paymentPlanId = paymentSnap.child("planId").getValue(String.class);

                if (paymentPlanId != null && paymentPlanId.equals(planId)) {
                    Integer amountPaid = paymentSnap.child("amountPaid").getValue(Integer.class);
                    if (amountPaid != null) {
                        totalPaidForCurrentPlan += amountPaid;
                    }

                    Integer rem = paymentSnap.child("remaining").getValue(Integer.class);
                    if (rem != null) {
                        remaining = rem;
                    }
                }
            }
        }

        tvMemberName.setText(name);
        tvMemberPhone.setText(phone);
        tvPlanType.setText(planType != null ? planType : getString(R.string.regular_plan));

        String startDateDisplay = planStartDate != null ? planStartDate : getString(R.string.na);
        String endDateDisplay = planEndDate != null ? planEndDate : getString(R.string.na);
        tvPlanDates.setText(startDateDisplay + " " + getString(R.string.to) + " " + endDateDisplay);

        tvTotalFee.setText(getString(R.string.rupee_prefix) + totalFee);
        tvTotalPaid.setText(getString(R.string.rupee_prefix) + totalPaidForCurrentPlan);
        tvRemaining.setText(getString(R.string.rupee_prefix) + remaining);

        cardMemberDetails.setVisibility(View.VISIBLE);
        cardPaymentForm.setVisibility(View.VISIBLE);

        if (remaining > 0) {
            etAmount.setText(String.valueOf(remaining));
            btnCollectPayment.setEnabled(true);
        } else {
            etAmount.setText("");
            Toast.makeText(this, getString(R.string.fully_paid_message), Toast.LENGTH_SHORT).show();
            btnCollectPayment.setEnabled(false);
        }

        Log.d("PaymentDebug", getString(R.string.plan_dates_log, planStartDate, planEndDate));
    }

    private void collectPayment() {
        if (currentMemberPhone == null || currentMemberId == null) {
            Toast.makeText(this, getString(R.string.search_member_first), Toast.LENGTH_SHORT).show();
            return;
        }

        String amountStr = etAmount.getText().toString().trim();
        if (amountStr.isEmpty()) {
            etAmount.setError(getString(R.string.amount_required));
            etAmount.requestFocus();
            return;
        }

        int amountPaid;
        try {
            amountPaid = Integer.parseInt(amountStr);
        } catch (NumberFormatException e) {
            etAmount.setError(getString(R.string.invalid_amount));
            etAmount.requestFocus();
            return;
        }

        if (amountPaid <= 0) {
            etAmount.setError(getString(R.string.amount_greater_than_zero));
            etAmount.requestFocus();
            return;
        }

        String remainingStr = tvRemaining.getText().toString().replace("₹", "").trim();
        int currentRemaining;
        try {
            currentRemaining = Integer.parseInt(remainingStr);
        } catch (NumberFormatException e) {
            currentRemaining = totalFee;
        }

        if (amountPaid > currentRemaining) {
            etAmount.setError(getString(R.string.amount_exceeds_remaining, currentRemaining));
            etAmount.requestFocus();
            return;
        }

        String paymentMode = getString(R.string.cash);
        if (rbUPI.isChecked()) {
            paymentMode = getString(R.string.upi);
        } else if (rbCard.isChecked()) {
            paymentMode = getString(R.string.card);
        }

        int newRemaining = Math.max(0, currentRemaining - amountPaid);
        remainingAmount = newRemaining;

        checkAndUpdatePayment(amountPaid, paymentMode, newRemaining);
    }

    private void checkAndUpdatePayment(int amountPaid, String paymentMode, int newRemaining) {
        btnCollectPayment.setEnabled(false);
        btnCollectPayment.setText(getString(R.string.processing));

        DatabaseReference memberRef = databaseReference.child("GYM")
                .child(ownerEmail)
                .child("members")
                .child(currentMemberId);

        memberRef.child("payments").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean existingPaymentFound = false;
                String existingPaymentId = null;
                int currentPaidAmount = 0;

                for (DataSnapshot paymentSnap : snapshot.getChildren()) {
                    String paymentForMonth = paymentSnap.child("forMonth").getValue(String.class);
                    if (forMonth.equals(paymentForMonth)) {
                        existingPaymentFound = true;
                        existingPaymentId = paymentSnap.getKey();
                        Integer paid = paymentSnap.child("amountPaid").getValue(Integer.class);
                        currentPaidAmount = paid != null ? paid : 0;
                        break;
                    }
                }

                if (existingPaymentFound) {
                    updateExistingPayment(existingPaymentId, amountPaid, paymentMode, newRemaining, currentPaidAmount);
                } else {
                    createNewPayment(amountPaid, paymentMode, newRemaining);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                runOnUiThread(() -> {
                    btnCollectPayment.setEnabled(true);
                    btnCollectPayment.setText(getString(R.string.collect_payment_button));
                    Toast.makeText(CollectPaymentActivity.this,
                            getString(R.string.error_prefix, error.getMessage()), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void updateExistingPayment(String existingPaymentId, int amountPaid, String paymentMode,
                                       int newRemaining, int currentPaidAmount) {
        DatabaseReference paymentRef = databaseReference.child("GYM")
                .child(ownerEmail)
                .child("members")
                .child(currentMemberId)
                .child("payments")
                .child(existingPaymentId);

        int newTotalPaid = currentPaidAmount + amountPaid;
        String status = newRemaining == 0 ? getString(R.string.paid_status) : getString(R.string.partial_status);

        HashMap<String, Object> updates = new HashMap<>();
        updates.put("amountPaid", newTotalPaid);
        updates.put("remaining", newRemaining);
        updates.put("status", status);
        updates.put("mode", paymentMode);
        updates.put("lastPaymentDate", System.currentTimeMillis());
        updates.put("lastPaymentMode", paymentMode);

        String transactionId = UUID.randomUUID().toString();
        HashMap<String, Object> transactionData = new HashMap<>();
        transactionData.put("transactionId", transactionId);
        transactionData.put("amount", amountPaid);
        transactionData.put("paymentMode", paymentMode);
        transactionData.put("date", System.currentTimeMillis());
        transactionData.put("remainingAfter", newRemaining);

        updates.put("paymentHistory/" + transactionId, transactionData);

        paymentRef.updateChildren(updates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                completePaymentProcess(amountPaid, paymentMode, newRemaining, existingPaymentId);
            } else {
                runOnUiThread(() -> {
                    btnCollectPayment.setEnabled(true);
                    btnCollectPayment.setText(getString(R.string.collect_payment_button));
                    Toast.makeText(CollectPaymentActivity.this,
                            getString(R.string.failed_to_update_payment, task.getException().getMessage()),
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void createNewPayment(int amountPaid, String paymentMode, int newRemaining) {
        paymentId = UUID.randomUUID().toString();

        HashMap<String, Object> paymentData = new HashMap<>();
        paymentData.put("paymentId", paymentId);
        paymentData.put("amountPaid", amountPaid);
        paymentData.put("totalFee", totalFee);
        paymentData.put("remaining", newRemaining);
        paymentData.put("mode", paymentMode);
        paymentData.put("date", System.currentTimeMillis());
        paymentData.put("status", newRemaining == 0 ? getString(R.string.paid_status) : getString(R.string.partial_status));
        paymentData.put("forMonth", forMonth);
        paymentData.put("planStartDate", planStartDate);
        paymentData.put("planId", planId);

        DatabaseReference paymentRef = databaseReference.child("GYM")
                .child(ownerEmail)
                .child("members")
                .child(currentMemberId)
                .child("payments")
                .child(paymentId);

        paymentRef.setValue(paymentData).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                completePaymentProcess(amountPaid, paymentMode, newRemaining, paymentId);
            } else {
                runOnUiThread(() -> {
                    btnCollectPayment.setEnabled(true);
                    btnCollectPayment.setText(getString(R.string.collect_payment_button));
                    Toast.makeText(CollectPaymentActivity.this,
                            getString(R.string.failed_to_save_payment, task.getException().getMessage()),
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void completePaymentProcess(int amountPaid, String paymentMode, int newRemaining, String pId) {
        if (newRemaining == 0) {
            HashMap<String, Object> planUpdate = new HashMap<>();
            planUpdate.put("status", "ACTIVE");

            databaseReference.child("GYM")
                    .child(ownerEmail)
                    .child("members")
                    .child(currentMemberId)
                    .child("currentPlan")
                    .updateChildren(planUpdate);
        }

        Uri pdfUri = generatePaymentPdf(amountPaid, paymentMode, newRemaining);
        sendPaymentNotifications(amountPaid, paymentMode, newRemaining, pdfUri);

        runOnUiThread(() -> {
            btnCollectPayment.setEnabled(true);
            btnCollectPayment.setText(getString(R.string.collect_payment_button));

            Toast.makeText(this,
                    getString(R.string.payment_collected_success, amountPaid),
                    Toast.LENGTH_LONG).show();

            searchMember();
            etAmount.setText("");
            rbCash.setChecked(true);
        });
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
            Calendar now = Calendar.getInstance();
            return String.format(Locale.getDefault(),
                    "%04d-%02d",
                    now.get(Calendar.YEAR),
                    now.get(Calendar.MONTH) + 1);
        }
    }

    private Uri generatePaymentPdf(int amount, String mode, int remaining) {
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

            canvas.drawText(getString(R.string.pdf_member_name, memberName), 40, y, paint); y += 25;
            canvas.drawText(getString(R.string.pdf_phone, currentMemberPhone), 40, y, paint); y += 25;
            canvas.drawText(getString(R.string.pdf_plan_start, planStartDate), 40, y, paint); y += 25;

            canvas.drawLine(20, y, 575, y, paint);
            y += 30;

            canvas.drawText(getString(R.string.pdf_total_fee, totalFee), 40, y, paint); y += 25;
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

            File file = new File(dir, getString(R.string.bill_filename, System.currentTimeMillis()));
            pdfDocument.writeTo(new FileOutputStream(file));
            pdfDocument.close();

            return FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    file
            );

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, getString(R.string.pdf_error, e.getMessage()), Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    private void sendPaymentNotifications(int amount, String mode, int remaining, Uri pdfUri) {
        sendPaymentSMS(amount, mode, remaining);

        if (pdfUri != null) {
            sendPaymentWhatsApp(pdfUri);
        }
    }

    private void sendPaymentSMS(int amount, String mode, int remaining) {
        try {
            String gymName = getGymName();
            String message = getString(R.string.sms_payment_message, amount, mode, planStartDate, getForMonth(planStartDate), remaining, gymName);

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
                SmsManager.getDefault().sendTextMessage(currentMemberPhone, null, message, null, null);
                Toast.makeText(this, getString(R.string.sms_sent_to_phone, currentMemberPhone), Toast.LENGTH_SHORT).show();
                Log.d("SMS", getString(R.string.sms_sent_log, message));
            }

        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.sms_failed_message, e.getMessage()), Toast.LENGTH_SHORT).show();
            Log.e("SMS", getString(R.string.sms_error_log, e.getMessage()));
        }
    }

    private void sendPaymentWhatsApp(Uri pdfUri) {
        try {
            if (currentMemberPhone == null || currentMemberPhone.length() < 10) {
                Toast.makeText(this, getString(R.string.invalid_phone_number), Toast.LENGTH_SHORT).show();
                return;
            }

            String phone = currentMemberPhone.replaceAll("[^0-9]", "");
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

    private String getGymName() {
        try {
            final String[] gymName = {getString(R.string.default_gym_name)};

            DatabaseReference gymRef = databaseReference.child("GYM")
                    .child(ownerEmail)
                    .child("ownerInfo")
                    .child("gymName");

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
            return getString(R.string.default_gym_name);
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
}