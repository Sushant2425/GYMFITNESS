package com.coderspuxelinnnovation.gymmanagementsystem.Activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

import com.coderspuxelinnnovation.gymmanagementsystem.R;
import com.coderspuxelinnnovation.gymmanagementsystem.base.BaseActivity;

public class ReportsActivity extends BaseActivity {

    private CardView cardPaymentReport, cardDuesReport, cardMonthlyRevenueReport;
    private CardView cardExpiringReport, cardMemberListReport;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);

        setupToolbar();
        initViews();
        setupClickListeners();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.reports));
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void initViews() {
        cardPaymentReport       = findViewById(R.id.card_payment_report);
        cardDuesReport          = findViewById(R.id.card_dues_report);
        cardMonthlyRevenueReport= findViewById(R.id.card_monthly_revenue_report);
        cardExpiringReport      = findViewById(R.id.card_expiring_report);
        cardMemberListReport    = findViewById(R.id.card_member_list_report);
    }

    private void setupClickListeners() {
        cardPaymentReport.setOnClickListener(v ->
                startActivity(new Intent(this, PaymentCollectionReportActivity.class)));

        cardDuesReport.setOnClickListener(v ->
                startActivity(new Intent(this, PendingDuesReportActivity.class)));

        cardMonthlyRevenueReport.setOnClickListener(v ->
                startActivity(new Intent(this, MonthlyRevenueReportActivity.class)));

        cardExpiringReport.setOnClickListener(v ->
                startActivity(new Intent(this, ExpiringSoonReportActivity.class)));

        cardMemberListReport.setOnClickListener(v ->
                startActivity(new Intent(this, AllMembersReportActivity.class)));
    }
}