package com.coderspuxelinnnovation.gymmanagementsystem.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.coderspuxelinnnovation.gymmanagementsystem.R;
import com.coderspuxelinnnovation.gymmanagementsystem.Utils.PrefManager;
import com.coderspuxelinnnovation.gymmanagementsystem.Utils.PremiumStatusManager;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.razorpay.Checkout;
import com.razorpay.PaymentResultListener;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.UUID;

public class PremiumSelectionActivity extends AppCompatActivity
        implements PaymentResultListener {

    private MaterialButton btnTrial, btn6Month, btn1Year, btnLifetime;
    private DatabaseReference subscriptionRef;
    private PrefManager prefManager;
    TextView tvCurrentPlan;

    private String selectedPlan = "";

    // 🔒 TEST KEY ONLY (LIVE MODE नंतर बदलू)
    private static final String RAZORPAY_KEY_ID =
            "rzp_test_SDIasQEFDIzTH0";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_premium_selection);

        prefManager = new PrefManager(this);

        String email = prefManager.getUserEmail();
        String key = email.replace(".", ",");

        subscriptionRef = FirebaseDatabase.getInstance()
                .getReference("GYM")
                .child(key)
                .child("subscription");

        initViews();
        setupClicks();
        loadCurrentPlan();
        checkTrialStatus();
    }

    private void initViews() {
        btnTrial = findViewById(R.id.btnTrial);
        btn6Month = findViewById(R.id.btn6Month);
        btn1Year = findViewById(R.id.btn1Year);
        btnLifetime = findViewById(R.id.btnLifetime);
        tvCurrentPlan = findViewById(R.id.tvCurrentPlan);
    }

    private void loadCurrentPlan() {
        subscriptionRef.child("currentPlan")
                .get()
                .addOnSuccessListener(snapshot -> {

                    Boolean active = snapshot.child("active").getValue(Boolean.class);
                    if (active != null && active) {

                        String plan = snapshot.child("planName").getValue(String.class);
                        String endDate = snapshot.child("endDate").getValue(String.class);

                        tvCurrentPlan.setText(
                                getString(R.string.current_plan_text, plan, endDate)
                        );
                    }
                });
    }

    private void checkTrialStatus() {
        subscriptionRef.child("trial").child("used")
                .get()
                .addOnSuccessListener(snapshot -> {

                    Boolean used = snapshot.getValue(Boolean.class);
                    if (used != null && used) {
                        btnTrial.setEnabled(false);
                        btnTrial.setText(getString(R.string.free_trial_used));
                        btnTrial.setAlpha(0.6f);
                    }
                });
    }

    private void setupClicks() {
        btnTrial.setOnClickListener(v -> startTrial());
        btn6Month.setOnClickListener(v -> startPremiumPayment("6_MONTH"));
        btn1Year.setOnClickListener(v -> startPremiumPayment("1_YEAR"));
        btnLifetime.setOnClickListener(v -> startPremiumPayment("LIFETIME"));
    }

    // ========================= TRIAL =========================
    private void startTrial() {

        subscriptionRef.child("trial").get().addOnSuccessListener(snapshot -> {

            Boolean used = snapshot.child("used").getValue(Boolean.class);
            if (used != null && used) {
                Toast.makeText(this,
                        getString(R.string.trial_already_used),
                        Toast.LENGTH_LONG).show();
                return;
            }

            long startMillis = System.currentTimeMillis();
            long endMillis = PremiumStatusManager.getTrialEndDate(startMillis);

            HashMap<String, Object> updates = new HashMap<>();

            HashMap<String, Object> trialMap = new HashMap<>();
            trialMap.put("used", true);
            trialMap.put("startMillis", startMillis);
            trialMap.put("endMillis", endMillis);

            HashMap<String, Object> currentPlan = new HashMap<>();
            currentPlan.put("type", "TRIAL");
            currentPlan.put("planName", "FREE_TRIAL");
            currentPlan.put("startMillis", startMillis);
            currentPlan.put("endMillis", endMillis);
            currentPlan.put("startDate", PremiumStatusManager.formatDate(startMillis));
            currentPlan.put("endDate", PremiumStatusManager.formatDate(endMillis));
            currentPlan.put("active", true);

            updates.put("trial", trialMap);
            updates.put("currentPlan", currentPlan);

            subscriptionRef.updateChildren(updates)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this,
                                getString(R.string.trial_activated),
                                Toast.LENGTH_SHORT).show();
                        goToDashboard();
                    });
        });
    }

    // ========================= PREMIUM =========================
    private void startPremiumPayment(String plan) {
        selectedPlan = plan;

        int amount = getPlanAmount(plan);

        Checkout checkout = new Checkout();
        checkout.setKeyID(RAZORPAY_KEY_ID);

        try {
            JSONObject options = new JSONObject();
            options.put("name", getString(R.string.gym_management_app));
            options.put("description", getString(R.string.premium_plan_description, plan));
            options.put("currency", "INR");
            options.put("amount", amount);
            options.put("prefill.email", prefManager.getUserEmail());
            options.put("theme.color", "#0e6e55");

            checkout.open(this, options);

        } catch (Exception e) {
            Toast.makeText(this,
                    getString(R.string.payment_error, e.getMessage()),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private int getPlanAmount(String plan) {
        if (plan == null) return 0;

        switch (plan) {
            case "6_MONTH":
                return 499 * 100;
            case "1_YEAR":
                return 899 * 100;
            case "LIFETIME":
                return 1999 * 100;
            default:
                return 0;
        }
    }

    // ===================== PAYMENT SUCCESS =====================
    @Override
    public void onPaymentSuccess(String razorpayPaymentId) {

        long startMillis = System.currentTimeMillis();
        long endMillis = PremiumStatusManager.getPremiumEndDate(selectedPlan);

        String endDate = endMillis == -1
                ? getString(R.string.lifetime)
                : PremiumStatusManager.formatDate(endMillis);

        String txnId = UUID.randomUUID().toString();

        HashMap<String, Object> updates = new HashMap<>();

        HashMap<String, Object> currentPlan = new HashMap<>();
        currentPlan.put("type", "PREMIUM");
        currentPlan.put("planName", selectedPlan);
        currentPlan.put("startMillis", startMillis);
        currentPlan.put("endMillis", endMillis);
        currentPlan.put("startDate", PremiumStatusManager.formatDate(startMillis));
        currentPlan.put("endDate", endDate);
        currentPlan.put("active", true);

        HashMap<String, Object> history = new HashMap<>();
        history.put("planName", selectedPlan);
        history.put("startMillis", startMillis);
        history.put("endMillis", endMillis);
        history.put("activatedAt", startMillis);
        history.put("source", "RAZORPAY");
        history.put("paymentId", razorpayPaymentId);

        currentPlan.put("amountPaid", getPlanAmount(selectedPlan) / 100);
        currentPlan.put("currency", "INR");
        currentPlan.put("paymentGateway", "RAZORPAY");
        currentPlan.put("paymentId", razorpayPaymentId);
        currentPlan.put("activatedBy", "SELF");

        updates.put("currentPlan", currentPlan);
        updates.put("history/" + txnId, history);

        subscriptionRef.updateChildren(updates)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this,
                            getString(R.string.premium_activated),
                            Toast.LENGTH_SHORT).show();
                    goToDashboard();
                });
    }

    // ===================== PAYMENT FAILED =====================
    @Override
    public void onPaymentError(int code, String response) {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.payment_failed))
                .setMessage(getString(R.string.payment_failed_message))
                .setPositiveButton(getString(R.string.retry), (d, w) -> startPremiumPayment(selectedPlan))
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void goToDashboard() {
        startActivity(new Intent(this, DashboardActivity.class));
        finish();
    }
}