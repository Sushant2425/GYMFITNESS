package com.coderspuxelinnnovation.gymmanagementsystem.Activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.coderspuxelinnnovation.gymmanagementsystem.R;
import com.coderspuxelinnnovation.gymmanagementsystem.models.Membercollet;
import com.coderspuxelinnnovation.gymmanagementsystem.models.Payment;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CollectPaymentActivity extends AppCompatActivity {

    private EditText etSearchMember, etAmount;
    private Button btnSearchMember, btnCollectPayment;
    private TextView tvMemberName, tvMemberPhone, tvPlanType, tvTotalFee, tvTotalPaid, tvRemaining;
    private RadioGroup rgPaymentMode;
    private MaterialCardView cardMemberDetails, cardPaymentForm;

    private DatabaseReference databaseReference;
    private FirebaseAuth firebaseAuth;
    private String ownerEmail;
    private Membercollet currentMember;   // <--- changed type here
    private String currentMemberPhone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collect_payment);

        initializeViews();
        initializeFirebase();
        setupListeners();
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
        cardMemberDetails = findViewById(R.id.cardMemberDetails);
        cardPaymentForm = findViewById(R.id.cardPaymentForm);
    }

    private void initializeFirebase() {
        firebaseAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference();
        ownerEmail = firebaseAuth.getCurrentUser().getEmail().replace(".", ",");
    }

    private void setupListeners() {
        btnSearchMember.setOnClickListener(v -> searchMember());
        btnCollectPayment.setOnClickListener(v -> collectPayment());
    }

    private void searchMember() {
        String searchQuery = etSearchMember.getText().toString().trim();

        if (searchQuery.isEmpty()) {
            Toast.makeText(this, "Please enter phone or name", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference membersRef = databaseReference.child("GYM")
                .child(ownerEmail).child("members");

        membersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean found = false;

                for (DataSnapshot memberSnapshot : snapshot.getChildren()) {
                    // ***** changed Member.class to Membercollet.class *****
                    Membercollet member = memberSnapshot.getValue(Membercollet.class);

                    if (member != null && member.getInfo() != null) {
                        String phone = memberSnapshot.getKey();
                        String name = member.getInfo().getName();

                        if (phone.contains(searchQuery) ||
                                name.toLowerCase().contains(searchQuery.toLowerCase())) {

                            currentMember = member;
                            currentMemberPhone = phone;
                            displayMemberDetails(member, phone);
                            found = true;
                            break;
                        }
                    }
                }

                if (!found) {
                    Toast.makeText(CollectPaymentActivity.this,
                            "Member not found", Toast.LENGTH_SHORT).show();
                    cardMemberDetails.setVisibility(View.GONE);
                    cardPaymentForm.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(CollectPaymentActivity.this,
                        "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ***** parameter type changed to Membercollet *****
    private void displayMemberDetails(Membercollet member, String phone) {
        if (member.getCurrentPlan() == null) {
            Toast.makeText(this, "Member has no active plan", Toast.LENGTH_SHORT).show();
            cardMemberDetails.setVisibility(View.GONE);
            cardPaymentForm.setVisibility(View.GONE);
            return;
        }

        double totalPaid = 0;
        if (member.getPayments() != null) {
            for (Payment payment : member.getPayments().values()) {
                totalPaid += payment.getAmountPaid();
            }
        }

        double totalFee = member.getCurrentPlan().getTotalFee();
        double remaining = totalFee - totalPaid;

        tvMemberName.setText(member.getInfo().getName());
        tvMemberPhone.setText(phone);
        tvPlanType.setText(member.getCurrentPlan().getPlanType());
        tvTotalFee.setText("₹" + totalFee);
        tvTotalPaid.setText("₹" + totalPaid);
        tvRemaining.setText("₹" + remaining);

        cardMemberDetails.setVisibility(View.VISIBLE);
        cardPaymentForm.setVisibility(View.VISIBLE);

        etAmount.setText(String.valueOf(remaining));
    }

    private void collectPayment() {
        if (currentMember == null || currentMemberPhone == null) {
            Toast.makeText(this, "Please select a member first", Toast.LENGTH_SHORT).show();
            return;
        }

        String amountStr = etAmount.getText().toString().trim();
        if (amountStr.isEmpty()) {
            Toast.makeText(this, "Please enter amount", Toast.LENGTH_SHORT).show();
            return;
        }

        double amountPaid = Double.parseDouble(amountStr);
        if (amountPaid <= 0) {
            Toast.makeText(this, "Amount must be greater than 0", Toast.LENGTH_SHORT).show();
            return;
        }

        int selectedId = rgPaymentMode.getCheckedRadioButtonId();
        if (selectedId == -1) {
            Toast.makeText(this, "Please select payment mode", Toast.LENGTH_SHORT).show();
            return;
        }

        RadioButton selectedRadioButton = findViewById(selectedId);
        String paymentMode = selectedRadioButton.getText().toString();

        double totalPaid = 0;
        if (currentMember.getPayments() != null) {
            for (Payment payment : currentMember.getPayments().values()) {
                totalPaid += payment.getAmountPaid();
            }
        }

        double totalFee = currentMember.getCurrentPlan().getTotalFee();
        double newTotalPaid = totalPaid + amountPaid;
        double remaining = Math.max(0, totalFee - newTotalPaid);

        String paymentId = UUID.randomUUID().toString();
        Payment.PlanReference planRef = new Payment.PlanReference(
                currentMember.getCurrentPlan().getPlanType(),
                currentMember.getCurrentPlan().getStartDate(),
                currentMember.getCurrentPlan().getEndDate()
        );

        Payment payment = new Payment(
                paymentId,
                amountPaid,
                totalFee,
                remaining,
                paymentMode,
                System.currentTimeMillis(),
                remaining == 0 ? "PAID" : "PARTIAL",
                planRef
        );

        DatabaseReference paymentRef = databaseReference.child("GYM")
                .child(ownerEmail)
                .child("members")
                .child(currentMemberPhone)
                .child("payments")
                .child(paymentId);

        paymentRef.setValue(payment).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Map<String, Object> updates = new HashMap<>();
                updates.put("paymentStatus", remaining == 0 ? "FULLY_PAID" : "PENDING");
                updates.put("totalPaid", newTotalPaid);
                updates.put("remainingAmount", remaining);

                databaseReference.child("GYM")
                        .child(ownerEmail)
                        .child("members")
                        .child(currentMemberPhone)
                        .child("currentPlan")
                        .updateChildren(updates);

                Toast.makeText(this, "Payment collected successfully!", Toast.LENGTH_SHORT).show();

                searchMember();
                etAmount.setText("");
            } else {
                Toast.makeText(this, "Failed to collect payment", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
