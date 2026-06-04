package com.coderspuxelinnnovation.gymmanagementsystem.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.coderspuxelinnnovation.gymmanagementsystem.Activities.AllPaymentsActivity;
import com.coderspuxelinnnovation.gymmanagementsystem.R;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class AllPaymentsAdapter extends RecyclerView.Adapter<AllPaymentsAdapter.ViewHolder> {

    private List<AllPaymentsActivity.PaymentItem> paymentList;
    private OnPaymentClickListener listener;

    public interface OnPaymentClickListener {
        void onPaymentClick(AllPaymentsActivity.PaymentItem paymentItem);
    }

    public AllPaymentsAdapter(List<AllPaymentsActivity.PaymentItem> paymentList,
                              OnPaymentClickListener listener) {
        this.paymentList = paymentList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_all_payment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AllPaymentsActivity.PaymentItem item = paymentList.get(position);

        holder.tvTitle.setText(item.getTitle());
        holder.tvDate.setText(item.getDate());
        holder.tvStatus.setText(item.getStatus());

        // Show Paid/Total amount
        holder.tvAmount.setText("₹" + item.getAmountPaid() + " / ₹" + item.getTotalFee());

        // Set status color based on status type
        int textColor, bgColor;
        switch (item.getStatus().toUpperCase()) {
            case "PAID":
                textColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.status_paid_text);
                bgColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.status_paid_bg);
                break;
            case "ACTIVE":
                textColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.status_active_text);
                bgColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.status_active_bg);
                break;
            case "PENDING":
                textColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.status_pending_text);
                bgColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.status_pending_bg);
                break;
            case "EXPIRED":
                textColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.status_expired_text);
                bgColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.status_expired_bg);
                break;
            default:
                textColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.status_default_text);
                bgColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.status_default_bg);
                break;
        }

        holder.tvStatus.setTextColor(textColor);
        holder.cvStatus.setCardBackgroundColor(bgColor);

        // Show/hide view button based on plan type
        if (item.isCurrentPlan()) {
            holder.btnViewDetails.setText("Current");
            holder.btnViewDetails.setEnabled(false);
            holder.btnViewDetails.setAlpha(0.5f);
        } else {
            holder.btnViewDetails.setText("View");
            holder.btnViewDetails.setEnabled(true);
            holder.btnViewDetails.setAlpha(1f);
        }

        holder.btnViewDetails.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPaymentClick(item);
            }
        });

        // Whole item click
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPaymentClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return paymentList != null ? paymentList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDate, tvAmount, tvStatus;
        CardView cvStatus;
        MaterialButton btnViewDetails;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            cvStatus = itemView.findViewById(R.id.cvStatus);
            btnViewDetails = itemView.findViewById(R.id.btnViewDetails);
        }
    }
}