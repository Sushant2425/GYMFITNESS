package com.coderspuxelinnnovation.gymmanagementsystem.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.coderspuxelinnnovation.gymmanagementsystem.models.Payment;

import java.text.SimpleDateFormat;
import com.coderspuxelinnnovation.gymmanagementsystem.R;

import java.util.Date;
import java.util.List;
import java.util.Locale;

public class collectpaymentadapter extends RecyclerView.Adapter<collectpaymentadapter.PaymentViewHolder> {

    private Context context;
    private List<Payment> paymentList;

    public collectpaymentadapter(Context context, List<Payment> paymentList) {
        this.context = context;
        this.paymentList = paymentList;
    }

    @NonNull
    @Override
    public PaymentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.itemcollectpayment, parent, false);
        return new PaymentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PaymentViewHolder holder, int position) {
        Payment payment = paymentList.get(position);

        holder.tvAmount.setText("₹" + payment.getAmountPaid());
        holder.tvMode.setText(payment.getMode());
        holder.tvRemaining.setText("Remaining: ₹" + payment.getRemaining());
        holder.tvStatus.setText(payment.getStatus());

        // Format date
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault());
        String dateStr = sdf.format(new Date(payment.getDate()));
        holder.tvDate.setText(dateStr);

        // Set status color
        if (payment.getStatus().equals("PAID")) {
            holder.tvStatus.setTextColor(context.getResources().getColor(android.R.color.holo_green_dark));
        } else {
            holder.tvStatus.setTextColor(context.getResources().getColor(android.R.color.holo_orange_dark));
        }
    }

    @Override
    public int getItemCount() {
        return paymentList.size();
    }

    public static class PaymentViewHolder extends RecyclerView.ViewHolder {
        TextView tvAmount, tvMode, tvDate, tvRemaining, tvStatus;

        public PaymentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvMode = itemView.findViewById(R.id.tvMode);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvRemaining = itemView.findViewById(R.id.tvRemaining);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}