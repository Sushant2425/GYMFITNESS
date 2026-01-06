package com.coderspuxelinnnovation.gymmanagementsystem.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.coderspuxelinnnovation.gymmanagementsystem.R;
import com.coderspuxelinnnovation.gymmanagementsystem.models.PaymentModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PaymentsAdapter extends RecyclerView.Adapter<PaymentsAdapter.PaymentViewHolder> {
    private List<PaymentModel> paymentsList = new ArrayList<>();

    public void updatePayments(List<PaymentModel> payments) {
        this.paymentsList = payments;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PaymentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_payment, parent, false);
        return new PaymentViewHolder(view);
    }

// Replace onBindViewHolder() method only:
    @Override
    public void onBindViewHolder(@NonNull PaymentViewHolder holder, int position) {
        PaymentModel payment = paymentsList.get(position);

        holder.tvPaymentId.setText("ID: " + payment.getPaymentId().substring(0, 8) + "...");
        holder.tvAmount.setText("₹" + payment.getAmountPaid() + " Paid");
        holder.tvMode.setText(payment.getMode());
        holder.tvStatus.setText(payment.getStatus());
        holder.tvRemaining.setText("Remaining: ₹" + payment.getRemaining());

        // ✅ FIXED: Long timestamp → readable date
        if (payment.getDate() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            String dateStr = sdf.format(new Date(payment.getDate()));
            holder.tvDate.setText("Date: " + dateStr);
        } else {
            holder.tvDate.setText("Date: N/A");
        }
    }


    @Override
    public int getItemCount() {
        return paymentsList.size();
    }

    static class PaymentViewHolder extends RecyclerView.ViewHolder {
        TextView tvPaymentId, tvAmount, tvMode, tvStatus, tvDate, tvRemaining;

        public PaymentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPaymentId = itemView.findViewById(R.id.tvPaymentId);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvMode = itemView.findViewById(R.id.tvMode);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvRemaining = itemView.findViewById(R.id.tvRemaining);
        }
    }
}
