//package com.coderspuxelinnnovation.gymmanagementsystem.adapters;
//
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.TextView;
//
//import androidx.annotation.NonNull;
//import androidx.recyclerview.widget.RecyclerView;
//
//import com.coderspuxelinnnovation.gymmanagementsystem.R;
//import com.coderspuxelinnnovation.gymmanagementsystem.models.PendingDueModel;
//import com.google.android.material.card.MaterialCardView;
//
//import java.util.ArrayList;
//import java.util.List;
//
//public class PaidDuesAdapter extends RecyclerView.Adapter<PaidDuesAdapter.ViewHolder> {
//
//    private List<PendingDueModel> list;
//
//    public PaidDuesAdapter(List<PendingDueModel> list) {
//        this.list = new ArrayList<>(list);
//    }
//
//    public void updateList(List<PendingDueModel> newList) {
//        this.list = new ArrayList<>(newList);
//        notifyDataSetChanged();
//    }
//
//    @NonNull
//    @Override
//    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//        View v = LayoutInflater.from(parent.getContext())
//                .inflate(R.layout.row_paid_due, parent, false);
//        return new ViewHolder(v);
//    }
//
//    @Override
//    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
//        PendingDueModel m = list.get(position);
//
//        h.tvName.setText(m.getName());
//        h.tvPhone.setText(m.getPhone());
//        h.tvPlan.setText("Plan: " + m.getPlanType());
//        h.tvMonth.setText("Month: " + formatMonth(m.getForMonth()));
//
//        h.tvTotalAmount.setText("₹" + m.getTotalFee());
//        h.tvPaidAmount.setText("Paid: ₹" + m.getAmountPaid());
//
//        // Status badge
//        h.tvStatus.setText("PAID");
//        h.tvStatus.setBackgroundResource(R.drawable.bg_green);
//
//        // Payment date info
//        int daysAgo = Math.abs(m.getDaysOverdue());
//        if (daysAgo == 0) {
//            h.tvPaymentDate.setText("Paid today");
//        } else if (daysAgo == 1) {
//            h.tvPaymentDate.setText("Paid 1 day ago");
//        } else {
//            h.tvPaymentDate.setText("Paid " + daysAgo + " days ago");
//        }
//    }
//
//    private String formatMonth(String forMonth) {
//        try {
//            String[] parts = forMonth.split("-");
//            int year = Integer.parseInt(parts[0]);
//            int month = Integer.parseInt(parts[1]);
//
//            String[] monthNames = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
//                    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
//
//            return monthNames[month - 1] + " " + year;
//        } catch (Exception e) {
//            return forMonth;
//        }
//    }
//
//    @Override
//    public int getItemCount() {
//        return list.size();
//    }
//
//    static class ViewHolder extends RecyclerView.ViewHolder {
//        MaterialCardView cardView;
//        TextView tvName, tvPhone, tvPlan, tvMonth;
//        TextView tvTotalAmount, tvPaidAmount, tvStatus, tvPaymentDate;
//
//        ViewHolder(@NonNull View itemView) {
//            super(itemView);
//            cardView = itemView.findViewById(R.id.cardView);
//            tvName = itemView.findViewById(R.id.tvName);
//            tvPhone = itemView.findViewById(R.id.tvPhone);
//            tvPlan = itemView.findViewById(R.id.tvPlan);
//            tvMonth = itemView.findViewById(R.id.tvMonth);
//            tvTotalAmount = itemView.findViewById(R.id.tvTotalAmount);
//            tvPaidAmount = itemView.findViewById(R.id.tvPaidAmount);
//            tvStatus = itemView.findViewById(R.id.tvStatus);
//            tvPaymentDate = itemView.findViewById(R.id.tvPaymentDate);
//        }
//    }
//}

package com.coderspuxelinnnovation.gymmanagementsystem.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.coderspuxelinnnovation.gymmanagementsystem.R;
import com.coderspuxelinnnovation.gymmanagementsystem.models.PendingDueModel;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

public class PaidDuesAdapter extends RecyclerView.Adapter<PaidDuesAdapter.ViewHolder> {

    private List<PendingDueModel> list;

    public PaidDuesAdapter(List<PendingDueModel> list) {
        this.list = new ArrayList<>(list);
    }

    public void updateList(List<PendingDueModel> newList) {
        this.list = new ArrayList<>(newList);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_paid_due, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        PendingDueModel m = list.get(position);
        Context context = h.itemView.getContext();

        // Member info
        h.tvName.setText(m.getName());
        h.tvPhone.setText(m.getPhone());

        // Plan and month
        h.tvPlan.setText(m.getPlanType() + " Plan");
        h.tvMonth.setText(formatMonth(m.getForMonth()));

        // Amount
        h.tvTotalAmount.setText("₹" + m.getTotalFee());

        // Status - Always PAID for this adapter
        h.tvStatus.setText("PAID");
        h.tvStatus.setBackgroundResource(R.drawable.bg_status_paid);
        h.tvStatus.setTextColor(ContextCompat.getColor(context, android.R.color.white));

        // Payment date info
        int daysAgo = Math.abs(m.getDaysOverdue());
        String paymentText;

        if (daysAgo == 0) {
            paymentText = "Paid today";
        } else if (daysAgo == 1) {
            paymentText = "1 day ago";
        } else {
            paymentText = daysAgo + " days ago";
        }

        h.tvPaymentDate.setText(paymentText);
    }

    private String formatMonth(String forMonth) {
        try {
            String[] parts = forMonth.split("-");
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);

            String[] monthNames = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

            return monthNames[month - 1] + " " + year;
        } catch (Exception e) {
            return forMonth;
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardView;
        TextView tvName, tvPhone, tvPlan, tvMonth;
        TextView tvTotalAmount, tvPaidAmount, tvStatus, tvPaymentDate;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            tvName = itemView.findViewById(R.id.tvName);
            tvPhone = itemView.findViewById(R.id.tvPhone);
            tvPlan = itemView.findViewById(R.id.tvPlan);
            tvMonth = itemView.findViewById(R.id.tvMonth);
            tvTotalAmount = itemView.findViewById(R.id.tvTotalAmount);
            tvPaidAmount = itemView.findViewById(R.id.tvPaidAmount);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvPaymentDate = itemView.findViewById(R.id.tvPaymentDate);
        }
    }
}