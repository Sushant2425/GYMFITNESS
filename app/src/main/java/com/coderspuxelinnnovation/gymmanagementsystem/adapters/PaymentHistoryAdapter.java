package com.coderspuxelinnnovation.gymmanagementsystem.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.coderspuxelinnnovation.gymmanagementsystem.R;
import com.coderspuxelinnnovation.gymmanagementsystem.models.MemberModel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PaymentHistoryAdapter extends RecyclerView.Adapter<PaymentHistoryAdapter.ViewHolder> {

    private List<Map<String, Object>> paymentHistoryList;
    private SimpleDateFormat dateFormat;

    public PaymentHistoryAdapter(List<Map<String, Object>> paymentHistoryList) {
        this.paymentHistoryList = paymentHistoryList;
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy, hh:mm a", Locale.getDefault());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_payment_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, Object> payment = paymentHistoryList.get(position);

        // Amount - Handle different number types (int/long)
        Object amountObj = payment.get("amount");
        if (amountObj != null) {
            int amount = 0;
            if (amountObj instanceof Long) {
                amount = ((Long) amountObj).intValue();
            } else if (amountObj instanceof Integer) {
                amount = (Integer) amountObj;
            } else if (amountObj instanceof Double) {
                amount = ((Double) amountObj).intValue();
            }
            holder.tvAmount.setText("₹" + amount);
        }

        // Payment Mode
        Object modeObj = payment.get("paymentMode");
        if (modeObj != null) {
            String mode = modeObj.toString();
            holder.tvPaymentMode.setText(mode);

            // Set color based on payment mode
            if ("Cash".equalsIgnoreCase(mode)) {
                holder.cvPaymentMode.setCardBackgroundColor(holder.itemView.getContext().getColor(R.color.cash_mode_bg));
                holder.tvPaymentMode.setTextColor(holder.itemView.getContext().getColor(R.color.cash_mode_text));
            } else if ("Online".equalsIgnoreCase(mode) || "UPI".equalsIgnoreCase(mode)) {
                holder.cvPaymentMode.setCardBackgroundColor(holder.itemView.getContext().getColor(R.color.online_mode_bg));
                holder.tvPaymentMode.setTextColor(holder.itemView.getContext().getColor(R.color.online_mode_text));
            } else {
                // Default color for other modes
                holder.cvPaymentMode.setCardBackgroundColor(holder.itemView.getContext().getColor(R.color.cash_mode_bg));
                holder.tvPaymentMode.setTextColor(holder.itemView.getContext().getColor(R.color.cash_mode_text));
            }
        }

        // Date
        Object dateObj = payment.get("date");
        if (dateObj != null) {
            long timestamp = 0;
            if (dateObj instanceof Long) {
                timestamp = (Long) dateObj;
            } else if (dateObj instanceof Integer) {
                timestamp = ((Integer) dateObj).longValue();
            } else if (dateObj instanceof Double) {
                timestamp = ((Double) dateObj).longValue();
            }

            if (timestamp > 0) {
                String formattedDate = dateFormat.format(new Date(timestamp));
                holder.tvDate.setText(formattedDate);
            } else {
                holder.tvDate.setText("N/A");
            }
        }

        // Transaction ID
        Object transactionIdObj = payment.get("transactionId");
        if (transactionIdObj != null) {
            String transactionId = transactionIdObj.toString();
            // Show short ID (first 8 chars)
            String shortId = transactionId.length() > 8 ?
                    transactionId.substring(0, 8) + "..." : transactionId;
            holder.tvTransactionId.setText("ID: " + shortId);
            holder.tvTransactionId.setTag(transactionId); // Store full ID in tag
        }

        // Notes
        Object notesObj = payment.get("notes");
        if (notesObj != null && !notesObj.toString().isEmpty()) {
            holder.llNotes.setVisibility(View.VISIBLE);
            holder.tvNotes.setText(notesObj.toString());
        } else {
            holder.llNotes.setVisibility(View.GONE);
        }

        // Remaining After
        Object remainingObj = payment.get("remainingAfter");
        if (remainingObj != null) {
            int remaining = 0;
            if (remainingObj instanceof Long) {
                remaining = ((Long) remainingObj).intValue();
            } else if (remainingObj instanceof Integer) {
                remaining = (Integer) remainingObj;
            } else if (remainingObj instanceof Double) {
                remaining = ((Double) remainingObj).intValue();
            }
            holder.tvRemainingAfter.setText("₹" + remaining);
        }
    }

    @Override
    public int getItemCount() {
        return paymentHistoryList != null ? paymentHistoryList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvAmount, tvPaymentMode, tvDate, tvTransactionId, tvNotes, tvRemainingAfter;
        CardView cvPaymentMode;
        View llNotes;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvPaymentMode = itemView.findViewById(R.id.tvPaymentMode);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvTransactionId = itemView.findViewById(R.id.tvTransactionId);
            tvNotes = itemView.findViewById(R.id.tvNotes);
            tvRemainingAfter = itemView.findViewById(R.id.tvRemainingAfter);
            cvPaymentMode = itemView.findViewById(R.id.cvPaymentMode);
            llNotes = itemView.findViewById(R.id.llNotes);
        }
    }
}