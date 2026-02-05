package com.coderspuxelinnnovation.gymmanagementsystem.Activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.coderspuxelinnnovation.gymmanagementsystem.R;
import com.coderspuxelinnnovation.gymmanagementsystem.Utils.PrefManager;
import com.coderspuxelinnnovation.gymmanagementsystem.adapters.PaymentHistoryAdapter;
import com.coderspuxelinnnovation.gymmanagementsystem.models.MemberModel;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PaymentHistoryActivity extends AppCompatActivity {

    private RecyclerView rvPaymentHistory;
    private ProgressBar progressBar;
    private TextView tvNoPayments, tvPaymentCount, tvPlanId, tvTotalFee, tvAmountPaid;
    private View cardPlanInfo;
    private String memberPhone, planId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_history);

        memberPhone = getIntent().getStringExtra("phone");
        planId = getIntent().getStringExtra("planId");

        if (memberPhone == null || planId == null) {
            Toast.makeText(this, "Invalid data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        loadPaymentHistory();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Payment History");
        }

        rvPaymentHistory = findViewById(R.id.rvPaymentHistory);
        progressBar = findViewById(R.id.progressBar);
        tvNoPayments = findViewById(R.id.tvNoPayments);
        tvPaymentCount = findViewById(R.id.tvPaymentCount);
        cardPlanInfo = findViewById(R.id.cardPlanInfo);
        tvPlanId = findViewById(R.id.tvPlanId);
        tvTotalFee = findViewById(R.id.tvTotalFee);
        tvAmountPaid = findViewById(R.id.tvAmountPaid);

        rvPaymentHistory.setLayoutManager(new LinearLayoutManager(this));
    }

    private void loadPaymentHistory() {
        progressBar.setVisibility(View.VISIBLE);
        String ownerEmail = new PrefManager(this).getUserEmail().replace(".", ",");

        DatabaseReference paymentRef = FirebaseDatabase.getInstance()
                .getReference("GYM")
                .child(ownerEmail)
                .child("members")
                .child(memberPhone)
                .child("payments")
                .child(planId);

        paymentRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                progressBar.setVisibility(View.GONE);

                if (!snapshot.exists()) {
                    Toast.makeText(PaymentHistoryActivity.this,
                            "Payment plan not found", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                // Get payment plan details
                MemberModel.PaymentPlan paymentPlan = snapshot.getValue(MemberModel.PaymentPlan.class);

                if (paymentPlan != null) {
                    // Show plan info
                    cardPlanInfo.setVisibility(View.VISIBLE);
                    tvPlanId.setText(paymentPlan.getPlanId() != null ? paymentPlan.getPlanId() : planId);
                    tvTotalFee.setText("₹" + paymentPlan.getTotalFee());
                    tvAmountPaid.setText("₹" + paymentPlan.getAmountPaid());

                    // Create list for ALL payments
                    List<Map<String, Object>> historyList = new ArrayList<>();

                    // ADD PAYMENT HISTORY ENTRIES (FIRST - these are actual transactions)
                    DataSnapshot paymentHistorySnapshot = snapshot.child("paymentHistory");

                    if (paymentHistorySnapshot.exists() && paymentHistorySnapshot.hasChildren()) {
                        for (DataSnapshot historySnap : paymentHistorySnapshot.getChildren()) {
                            MemberModel.PaymentHistory payment = historySnap.getValue(MemberModel.PaymentHistory.class);
                            if (payment != null) {
                                Map<String, Object> paymentMap = convertPaymentToMap(payment);
                                historyList.add(paymentMap);
                            }
                        }
                    }

                    // NOW ADD MAIN PAYMENT ONLY IF payment history is empty
                    // OR if amountPaid is different from sum of payment history
                    if (historyList.isEmpty()) {
                        // No payment history found, add main payment
                        if (paymentPlan.getDate() > 0 && paymentPlan.getAmountPaid() > 0) {
                            Map<String, Object> mainPayment = new java.util.HashMap<>();
                            mainPayment.put("transactionId", paymentPlan.getPaymentId() != null ?
                                    paymentPlan.getPaymentId() : "plan_payment_" + planId);
                            mainPayment.put("amount", paymentPlan.getAmountPaid());
                            mainPayment.put("date", paymentPlan.getDate());
                            mainPayment.put("notes", "Initial payment for " + paymentPlan.getForMonth());
                            mainPayment.put("paymentMode", paymentPlan.getMode() != null ?
                                    paymentPlan.getMode() : paymentPlan.getLastPaymentMode());
                            mainPayment.put("remainingAfter", paymentPlan.getRemaining());
                            historyList.add(mainPayment);
                        }
                    } else {
                        // Check if we need to show main payment separately
                        // Calculate total from payment history
                        double historyTotal = 0;
                        for (Map<String, Object> payment : historyList) {
                            Object amountObj = payment.get("amount");
                            if (amountObj instanceof Long) {
                                historyTotal += ((Long) amountObj).doubleValue();
                            } else if (amountObj instanceof Integer) {
                                historyTotal += ((Integer) amountObj).doubleValue();
                            } else if (amountObj instanceof Double) {
                                historyTotal += (Double) amountObj;
                            }
                        }

                        // If payment history total is LESS than amountPaid, add difference as main payment
                        if (historyTotal < paymentPlan.getAmountPaid()) {
                            double difference = paymentPlan.getAmountPaid() - historyTotal;

                            Map<String, Object> mainPayment = new java.util.HashMap<>();
                            mainPayment.put("transactionId", paymentPlan.getPaymentId() != null ?
                                    paymentPlan.getPaymentId() : "plan_payment_" + planId);
                            mainPayment.put("amount", (int) difference);
                            mainPayment.put("date", paymentPlan.getDate());
                            mainPayment.put("notes", "Additional payment for " + paymentPlan.getForMonth());
                            mainPayment.put("paymentMode", paymentPlan.getMode() != null ?
                                    paymentPlan.getMode() : paymentPlan.getLastPaymentMode());
                            mainPayment.put("remainingAfter", paymentPlan.getRemaining());
                            historyList.add(mainPayment);
                        }
                    }

                    if (!historyList.isEmpty()) {
                        // Sort by date (most recent first)
                        historyList.sort((o1, o2) -> {
                            long date1 = (long) o1.get("date");
                            long date2 = (long) o2.get("date");
                            return Long.compare(date2, date1); // Descending
                        });

                        PaymentHistoryAdapter adapter = new PaymentHistoryAdapter(historyList);
                        rvPaymentHistory.setAdapter(adapter);
                        rvPaymentHistory.setVisibility(View.VISIBLE);
                        tvNoPayments.setVisibility(View.GONE);

                        // Update count
                        tvPaymentCount.setText("(" + historyList.size() + " payments)");

                        // Also show total payments made
                        double totalPayments = 0;
                        for (Map<String, Object> payment : historyList) {
                            Object amountObj = payment.get("amount");
                            if (amountObj instanceof Long) {
                                totalPayments += ((Long) amountObj).doubleValue();
                            } else if (amountObj instanceof Integer) {
                                totalPayments += ((Integer) amountObj).doubleValue();
                            }
                        }

                        // You can add this info somewhere in UI if needed
                        // tvTotalPayments.setText("Total Paid: ₹" + totalPayments);
                    } else {
                        showNoPayments();
                    }
                } else {
                    showNoPayments();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(PaymentHistoryActivity.this,
                        "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private Map<String, Object> convertPaymentToMap(MemberModel.PaymentHistory payment) {
        Map<String, Object> map = new java.util.HashMap<>();
        map.put("transactionId", payment.getTransactionId());
        map.put("amount", payment.getAmount());
        map.put("date", payment.getDate());
        map.put("notes", payment.getNotes());
        map.put("paymentMode", payment.getPaymentMode());
        map.put("remainingAfter", payment.getRemainingAfter());
        return map;
    }

    private void showNoPayments() {
        tvNoPayments.setVisibility(View.VISIBLE);
        rvPaymentHistory.setVisibility(View.GONE);
        tvPaymentCount.setText("(0 payments)");
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}






