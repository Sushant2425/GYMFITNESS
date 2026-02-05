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

    // Store payment data like PaymentActivity
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

        // Request SMS permission like PaymentActivity
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, SMS_PERMISSION_CODE);
            Log.d("SMS", "🚨 Permission requested from CollectPayment");
        } else {
            Log.d("SMS", "✅ SMS permission already granted in CollectPayment");
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
        tvPlanDates = findViewById(R.id.tvPlanDates); // Add this line

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

        // Set cash as default
        rbCash.setChecked(true);
    }

    private void searchMember() {
        String searchQuery = etSearchMember.getText().toString().trim();

        if (searchQuery.isEmpty()) {
            Toast.makeText(this, "Please enter phone or name", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSearchMember.setEnabled(false);
        btnSearchMember.setText("Searching...");

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

                            // Add to results list
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
                            "❌ No members found", Toast.LENGTH_SHORT).show();
                    cardMemberDetails.setVisibility(View.GONE);
                    cardPaymentForm.setVisibility(View.GONE);
                } else if (searchResults.size() == 1) {
                    // Only one result - show directly
                    MemberSearchResult result = searchResults.get(0);
                    currentMemberId = result.memberId;
                    currentMemberPhone = result.phone;
                    displayMemberDetails(result.snapshot, result.phone, result.name, result.memberId);
                } else {
                    // Multiple results - show dialog to choose
                    showMemberSelectionDialog(searchResults);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                btnSearchMember.setEnabled(true);
                btnSearchMember.setText(getString(R.string.search_member));
                Toast.makeText(CollectPaymentActivity.this,
                        "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void showMemberSelectionDialog(List<MemberSearchResult> results) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Member (" + results.size() + " found)");

        // Create array of display strings
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

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    private void displayMemberDetails(DataSnapshot memberSnapshot, String phone, String name, String memberId) {
        DataSnapshot currentPlanSnap = memberSnapshot.child("currentPlan");

        if (!currentPlanSnap.exists()) {
            Toast.makeText(this, "❌ Member has no active plan", Toast.LENGTH_SHORT).show();
            cardMemberDetails.setVisibility(View.GONE);
            cardPaymentForm.setVisibility(View.GONE);
            return;
        }

        // Get plan details
        planStartDate = currentPlanSnap.child("startDate").getValue(String.class);
        String planEndDate = currentPlanSnap.child("endDate").getValue(String.class);
        Integer fee = currentPlanSnap.child("totalFee").getValue(Integer.class);
        totalFee = fee != null ? fee : 0;
        planId = currentPlanSnap.child("planId").getValue(String.class);
        String planType = currentPlanSnap.child("planType").getValue(String.class);

        // Store member details
        memberName = name;
        forMonth = getForMonth(planStartDate);

        // Calculate total paid for CURRENT PLAN ONLY
        int totalPaidForCurrentPlan = 0;
        int remaining = totalFee;

        DataSnapshot paymentsSnap = memberSnapshot.child("payments");
        if (paymentsSnap.exists()) {
            for (DataSnapshot paymentSnap : paymentsSnap.getChildren()) {
                String paymentPlanId = paymentSnap.child("planId").getValue(String.class);

                // Check if this payment belongs to CURRENT PLAN
                if (paymentPlanId != null && paymentPlanId.equals(planId)) {
                    Integer amountPaid = paymentSnap.child("amountPaid").getValue(Integer.class);
                    if (amountPaid != null) {
                        totalPaidForCurrentPlan += amountPaid;
                    }

                    // Get remaining from this current plan's payment
                    Integer rem = paymentSnap.child("remaining").getValue(Integer.class);
                    if (rem != null) {
                        remaining = rem;
                    }
                }
            }
        }

        // Display member details
        tvMemberName.setText(name);
        tvMemberPhone.setText(phone);
        tvPlanType.setText(planType != null ? planType : "Regular");

        // Show both dates if available
        String startDateDisplay = planStartDate != null ? planStartDate : "N/A";
        String endDateDisplay = planEndDate != null ? planEndDate : "N/A";
        tvPlanDates.setText(startDateDisplay + " to " + endDateDisplay); // Shows "01/02/2026 to 28/02/2026"

        tvTotalFee.setText("₹" + totalFee);
        tvTotalPaid.setText("₹" + totalPaidForCurrentPlan);
        tvRemaining.setText("₹" + remaining);

        // Show cards with animation
        cardMemberDetails.setVisibility(View.VISIBLE);
        cardPaymentForm.setVisibility(View.VISIBLE);

        // Auto-fill remaining amount
        if (remaining > 0) {
            etAmount.setText(String.valueOf(remaining));
            btnCollectPayment.setEnabled(true);
        } else {
            etAmount.setText("");
            Toast.makeText(this, "✅ This member has fully paid!", Toast.LENGTH_SHORT).show();
            btnCollectPayment.setEnabled(false);
        }

        // Debug log
        Log.d("PaymentDebug", "Plan: " + planStartDate + " to " + planEndDate);
    }

    private void collectPayment() {
        if (currentMemberPhone == null || currentMemberId == null) {
            Toast.makeText(this, "Please search and select a member first", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate amount
        String amountStr = etAmount.getText().toString().trim();
        if (amountStr.isEmpty()) {
            etAmount.setError("Amount is required");
            etAmount.requestFocus();
            return;
        }

        int amountPaid;
        try {
            amountPaid = Integer.parseInt(amountStr);
        } catch (NumberFormatException e) {
            etAmount.setError("Invalid amount");
            etAmount.requestFocus();
            return;
        }

        if (amountPaid <= 0) {
            etAmount.setError("Amount must be greater than 0");
            etAmount.requestFocus();
            return;
        }

        // Get current remaining from TextView
        String remainingStr = tvRemaining.getText().toString().replace("₹", "").trim();
        int currentRemaining;
        try {
            currentRemaining = Integer.parseInt(remainingStr);
        } catch (NumberFormatException e) {
            currentRemaining = totalFee;
        }

        if (amountPaid > currentRemaining) {
            etAmount.setError("Amount exceeds remaining balance (₹" + currentRemaining + ")");
            etAmount.requestFocus();
            return;
        }

        // Get payment mode
        String paymentMode = "Cash";
        if (rbUPI.isChecked()) {
            paymentMode = "UPI";
        } else if (rbCard.isChecked()) {
            paymentMode = "Card";
        }

        // Calculate new values
        int newRemaining = Math.max(0, currentRemaining - amountPaid);
        remainingAmount = newRemaining;

        // Check if this is an existing payment or new payment
        checkAndUpdatePayment(amountPaid, paymentMode, newRemaining);
    }

    private void checkAndUpdatePayment(int amountPaid, String paymentMode, int newRemaining) {
        // Show loading
        btnCollectPayment.setEnabled(false);
        btnCollectPayment.setText("Processing...");

        DatabaseReference memberRef = databaseReference.child("GYM")
                .child(ownerEmail)
                .child("members")
                .child(currentMemberId);

        // First, check if there's an existing payment for this month
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
                    // Update existing payment (same as PendingDuesActivity)
                    updateExistingPayment(existingPaymentId, amountPaid, paymentMode, newRemaining, currentPaidAmount);
                } else {
                    // Create new payment (same as PaymentActivity)
                    createNewPayment(amountPaid, paymentMode, newRemaining);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                runOnUiThread(() -> {
                    btnCollectPayment.setEnabled(true);
                    btnCollectPayment.setText(getString(R.string.collect_payment_button));
                    Toast.makeText(CollectPaymentActivity.this,
                            "❌ Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
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

        // Calculate new total paid
        int newTotalPaid = currentPaidAmount + amountPaid;
        String status = newRemaining == 0 ? "PAID" : "PARTIAL";

        HashMap<String, Object> updates = new HashMap<>();
        updates.put("amountPaid", newTotalPaid);
        updates.put("remaining", newRemaining);
        updates.put("status", status);
        updates.put("mode", paymentMode);
        updates.put("lastPaymentDate", System.currentTimeMillis());
        updates.put("lastPaymentMode", paymentMode);

        // Create transaction for paymentHistory
        String transactionId = UUID.randomUUID().toString();
        HashMap<String, Object> transactionData = new HashMap<>();
        transactionData.put("transactionId", transactionId);
        transactionData.put("amount", amountPaid);
        transactionData.put("paymentMode", paymentMode);
        transactionData.put("date", System.currentTimeMillis());
        transactionData.put("remainingAfter", newRemaining);

        // Add to paymentHistory
        updates.put("paymentHistory/" + transactionId, transactionData);

        paymentRef.updateChildren(updates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                completePaymentProcess(amountPaid, paymentMode, newRemaining, existingPaymentId);
            } else {
                runOnUiThread(() -> {
                    btnCollectPayment.setEnabled(true);
                    btnCollectPayment.setText(getString(R.string.collect_payment_button));
                    Toast.makeText(CollectPaymentActivity.this,
                            "❌ Failed to update payment: " + task.getException().getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void createNewPayment(int amountPaid, String paymentMode, int newRemaining) {
        paymentId = UUID.randomUUID().toString();

        // Payment data (SAME structure as PaymentActivity)
        HashMap<String, Object> paymentData = new HashMap<>();
        paymentData.put("paymentId", paymentId);
        paymentData.put("amountPaid", amountPaid);
        paymentData.put("totalFee", totalFee);
        paymentData.put("remaining", newRemaining);
        paymentData.put("mode", paymentMode);
        paymentData.put("date", System.currentTimeMillis());
        paymentData.put("status", newRemaining == 0 ? "PAID" : "PARTIAL");
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
                            "❌ Failed to save payment: " + task.getException().getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void completePaymentProcess(int amountPaid, String paymentMode, int newRemaining, String pId) {
        // Update current plan
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

        // Generate PDF receipt
        Uri pdfUri = generatePaymentPdf(amountPaid, paymentMode, newRemaining);

        // Send notifications
        sendPaymentNotifications(amountPaid, paymentMode, newRemaining, pdfUri);

        runOnUiThread(() -> {
            btnCollectPayment.setEnabled(true);
            btnCollectPayment.setText(getString(R.string.collect_payment_button));

            Toast.makeText(this,
                    "✅ Payment of ₹" + amountPaid + " collected successfully!",
                    Toast.LENGTH_LONG).show();

            // Refresh member details
            searchMember();
            etAmount.setText("");

            // Reset to cash
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

            // Title - SAME as PaymentActivity
            canvas.drawText("GYM PAYMENT RECEIPT", 150, y, titlePaint);
            y += 40;

            // Lines - SAME as PaymentActivity
            canvas.drawLine(20, y, 575, y, paint);
            y += 30;

            canvas.drawText("Member Name : " + memberName, 40, y, paint); y += 25;
            canvas.drawText("Phone       : " + currentMemberPhone, 40, y, paint); y += 25;
            canvas.drawText("Plan Start  : " + planStartDate, 40, y, paint); y += 25;

            canvas.drawLine(20, y, 575, y, paint);
            y += 30;

            canvas.drawText("Total Fee   : ₹" + totalFee, 40, y, paint); y += 25;
            canvas.drawText("Paid Amount : ₹" + amount, 40, y, paint); y += 25;
            canvas.drawText("Remaining   : ₹" + remaining, 40, y, paint); y += 25;
            canvas.drawText("Payment Via : " + mode, 40, y, paint); y += 25;

            canvas.drawLine(20, y, 575, y, paint);
            y += 40;

            paint.setFakeBoldText(true);
            canvas.drawText("Thank you for your payment!", 150, y, paint);

            pdfDocument.finishPage(page);

            // File - SAME as PaymentActivity
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

    private void sendPaymentNotifications(int amount, String mode, int remaining, Uri pdfUri) {
        // Send SMS - SAME as PaymentActivity
        sendPaymentSMS(amount, mode, remaining);

        // Send WhatsApp with PDF - SAME as PaymentActivity
        if (pdfUri != null) {
            sendPaymentWhatsApp(pdfUri);
        }
    }

    private void sendPaymentSMS(int amount, String mode, int remaining) {
        try {
            // Get gym name
            String gymName = getGymName();

            // Create better message with plan details
            String message = String.format("Payment Received: ₹%d via %s for plan %s to %s. Remaining: ₹%d. Thank you from %s!",
                    amount, mode, planStartDate, getForMonth(planStartDate), remaining, gymName);

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
                SmsManager.getDefault().sendTextMessage(currentMemberPhone, null, message, null, null);
                Toast.makeText(this, "📱 SMS sent to " + currentMemberPhone, Toast.LENGTH_SHORT).show();
                Log.d("SMS", "Sent: " + message);
            }

        } catch (Exception e) {
            Toast.makeText(this, "⚠️ SMS failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e("SMS", "Error: " + e.getMessage());
        }
    }
    private void sendPaymentWhatsApp(Uri pdfUri) {
        try {
            if (currentMemberPhone == null || currentMemberPhone.length() < 10) {
                Toast.makeText(this, "Invalid phone number", Toast.LENGTH_SHORT).show();
                return;
            }

            String phone = currentMemberPhone.replaceAll("[^0-9]", "");
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
            Toast.makeText(this, "WhatsApp not installed", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "WhatsApp error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private String getGymName() {
        try {
            final String[] gymName = {"Sagar Gym"};

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
            return "Sagar Gym";
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
}