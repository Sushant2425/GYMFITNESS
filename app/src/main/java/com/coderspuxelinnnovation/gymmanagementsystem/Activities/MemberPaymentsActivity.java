package com.coderspuxelinnnovation.gymmanagementsystem.Activities;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.coderspuxelinnnovation.gymmanagementsystem.R;
import com.coderspuxelinnnovation.gymmanagementsystem.Utils.PrefManager;
import com.coderspuxelinnnovation.gymmanagementsystem.adapters.PaymentsAdapter;
import com.coderspuxelinnnovation.gymmanagementsystem.models.PaymentModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class MemberPaymentsActivity extends AppCompatActivity {

    private RecyclerView recyclerPayments;
    private PaymentsAdapter adapter;
    private ProgressBar progressBar;
    private TextView tvNoPayments;
    private MaterialToolbar toolbar;
    private String memberPhone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_member_payments);

        memberPhone = getIntent().getStringExtra("phone");
        initViews();
        loadPayments();
    }

    private void initViews() {
        recyclerPayments = findViewById(R.id.recyclerPayments);
        progressBar = findViewById(R.id.progressBar);
        tvNoPayments = findViewById(R.id.tvNoPayments);
        toolbar = findViewById(R.id.toolbar);

        toolbar.setNavigationOnClickListener(v -> finish());

        adapter = new PaymentsAdapter();
        recyclerPayments.setLayoutManager(new LinearLayoutManager(this));
        recyclerPayments.setAdapter(adapter);
    }

    private void loadPayments() {
        progressBar.setVisibility(View.VISIBLE);
        String ownerEmail = new PrefManager(this).getUserEmail().replace(".", ",");

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("GYM")
                .child(ownerEmail)
                .child("members")
                .child(memberPhone)
                .child("payments");

        ref.addValueEventListener(new ValueEventListener() {
            // Replace onDataChange() only:
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                progressBar.setVisibility(View.GONE);
                ArrayList<PaymentModel> payments = new ArrayList<>();

                for (DataSnapshot paymentSnapshot : snapshot.getChildren()) {
                    // ✅ SAFE: Manual parse instead of getValue()
                    try {
                        String paymentId = paymentSnapshot.getKey();
                        Long date = paymentSnapshot.child("date").getValue(Long.class);
                        Double amountPaid = paymentSnapshot.child("amountPaid").getValue(Double.class);
                        String mode = paymentSnapshot.child("mode").getValue(String.class);
                        String status = paymentSnapshot.child("status").getValue(String.class);
                        Double remaining = paymentSnapshot.child("remaining").getValue(Double.class);
                        Double totalFee = paymentSnapshot.child("totalFee").getValue(Double.class);

                        PaymentModel payment = new PaymentModel();
                        payment.paymentId = paymentId;
                        payment.date = date;
                        payment.amountPaid = amountPaid;
                        payment.mode = mode;
                        payment.status = status;
                        payment.remaining = remaining;
                        payment.totalFee = totalFee;

                        payments.add(payment);
                    } catch (Exception e) {
                        // Skip invalid payment entries
                    }
                }

                if (payments.isEmpty()) {
                    tvNoPayments.setVisibility(View.VISIBLE);
                    recyclerPayments.setVisibility(View.GONE);
                } else {
                    tvNoPayments.setVisibility(View.GONE);
                    recyclerPayments.setVisibility(View.VISIBLE);
                    adapter.updatePayments(payments);
                }
            }


            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
            }
        });
    }
}
