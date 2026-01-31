//package com.coderspuxelinnnovation.gymmanagementsystem.adapters;
//
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.ProgressBar;
//import android.widget.TextView;
//
//import androidx.annotation.NonNull;
//import androidx.recyclerview.widget.RecyclerView;
//
//import com.coderspuxelinnnovation.gymmanagementsystem.R;
//import com.coderspuxelinnnovation.gymmanagementsystem.models.PendingDueModel;
//import com.google.android.material.button.MaterialButton;
//import com.google.android.material.card.MaterialCardView;
//
//import java.util.ArrayList;
//import java.util.List;
//
//public class PendingDuesAdapter extends RecyclerView.Adapter<PendingDuesAdapter.ViewHolder> {
//
//    private List<PendingDueModel> list;
//    private List<PendingDueModel> selectedItems = new ArrayList<>();
//    private OnPaymentClickListener paymentClickListener;
//
//    public interface OnPaymentClickListener {
//        void onPaymentClick(PendingDueModel due);
//        void onItemSelect(PendingDueModel due, boolean isSelected);
//    }
//
//    public PendingDuesAdapter(List<PendingDueModel> list, OnPaymentClickListener listener) {
//        this.list = new ArrayList<>(list);
//        this.paymentClickListener = listener;
//    }
//
//    public void updateList(List<PendingDueModel> newList) {
//        this.list = new ArrayList<>(newList);
//        selectedItems.clear();
//        notifyDataSetChanged();
//    }
//
//    public List<PendingDueModel> getSelectedItems() {
//        return new ArrayList<>(selectedItems);
//    }
//
//    @NonNull
//    @Override
//    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//        View v = LayoutInflater.from(parent.getContext())
//                .inflate(R.layout.row_pending_due, parent, false);
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
//        // Show payment progress
//        h.tvTotalFee.setText("Total: ₹" + m.getTotalFee());
//        h.tvPaidAmount.setText("Paid: ₹" + m.getAmountPaid());
//        h.tvRemaining.setText("₹" + m.getRemaining());
//
//        // Payment progress bar
//        int percentage = m.getPaymentPercentage();
//        h.progressBar.setProgress(percentage);
//        h.tvProgress.setText(percentage + "%");
//
//        // Days overdue
//        int days = m.getDaysOverdue();
//        String dayText = Math.abs(days) + " day" + (Math.abs(days) != 1 ? "s" : "");
//
//        if (days > 0) {
//            h.tvDaysAgo.setText(dayText + " overdue");
//            h.tvDaysAgo.setTextColor(h.itemView.getContext().getResources().getColor(R.color.red));
//        } else {
//            h.tvDaysAgo.setText(Math.abs(days) + " days left");
//            h.tvDaysAgo.setTextColor(h.itemView.getContext().getResources().getColor(R.color.green));
//        }
//
//        // Status badge
//        if (days > 30) {
//            h.tvStatus.setText("OVERDUE");
//            h.tvStatus.setBackgroundResource(R.drawable.bg_dark_red);
//        } else if (days > 15) {
//            h.tvStatus.setText("WARNING");
//            h.tvStatus.setBackgroundResource(R.drawable.bg_orange);
//        } else if (days > 0) {
//            h.tvStatus.setText("DUE");
//            h.tvStatus.setBackgroundResource(R.drawable.bg_yellow);
//        } else {
//            h.tvStatus.setText("UPCOMING");
//            h.tvStatus.setBackgroundResource(R.drawable.bg_light_blue);
//        }
//
//        // Selection state
//        boolean isSelected = selectedItems.contains(m);
//        h.cardView.setStrokeWidth(isSelected ? 3 : 1);
//        h.cardView.setStrokeColor(isSelected ?
//                h.itemView.getContext().getResources().getColor(R.color.colorPrimary) :
//                h.itemView.getContext().getResources().getColor(R.color.grey));
//
//        // Card click for selection
//        h.cardView.setOnClickListener(v -> {
//            if (selectedItems.contains(m)) {
//                selectedItems.remove(m);
//            } else {
//                selectedItems.add(m);
//            }
//            notifyItemChanged(position);
//            if (paymentClickListener != null) {
//                paymentClickListener.onItemSelect(m, selectedItems.contains(m));
//            }
//        });
//
//        // Collect button
//        h.btnCollect.setOnClickListener(v -> {
//            if (paymentClickListener != null) {
//                paymentClickListener.onPaymentClick(m);
//            }
//        });
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
//        TextView tvTotalFee, tvPaidAmount, tvRemaining;
//        TextView tvDaysAgo, tvStatus, tvProgress;
//        ProgressBar progressBar;
//        MaterialButton btnCollect;
//
//        ViewHolder(@NonNull View itemView) {
//            super(itemView);
//            cardView = itemView.findViewById(R.id.cardView);
//            tvName = itemView.findViewById(R.id.tvName);
//            tvPhone = itemView.findViewById(R.id.tvPhone);
//            tvPlan = itemView.findViewById(R.id.tvPlan);
//            tvMonth = itemView.findViewById(R.id.tvMonth);
//            tvTotalFee = itemView.findViewById(R.id.tvTotalFee);
//            tvPaidAmount = itemView.findViewById(R.id.tvPaidAmount);
//            tvRemaining = itemView.findViewById(R.id.tvRemaining);
//            tvDaysAgo = itemView.findViewById(R.id.tvDaysAgo);
//            tvStatus = itemView.findViewById(R.id.tvStatus);
//            tvProgress = itemView.findViewById(R.id.tvProgress);
//            progressBar = itemView.findViewById(R.id.progressBar);
//            btnCollect = itemView.findViewById(R.id.btnCollect);
//        }
//    }
//}

package com.coderspuxelinnnovation.gymmanagementsystem.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.coderspuxelinnnovation.gymmanagementsystem.R;
import com.coderspuxelinnnovation.gymmanagementsystem.models.PendingDueModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

public class PendingDuesAdapter extends RecyclerView.Adapter<PendingDuesAdapter.ViewHolder> {

    private List<PendingDueModel> list;
    private List<PendingDueModel> selectedItems = new ArrayList<>();
    private OnPaymentClickListener paymentClickListener;

    public interface OnPaymentClickListener {
        void onPaymentClick(PendingDueModel due);
        void onItemSelect(PendingDueModel due, boolean isSelected);
    }

    public PendingDuesAdapter(List<PendingDueModel> list, OnPaymentClickListener listener) {
        this.list = new ArrayList<>(list);
        this.paymentClickListener = listener;
    }

    public void updateList(List<PendingDueModel> newList) {
        this.list = new ArrayList<>(newList);
        selectedItems.clear();
        notifyDataSetChanged();
    }

    public List<PendingDueModel> getSelectedItems() {
        return new ArrayList<>(selectedItems);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_pending_due, parent, false);
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

        // Payment amounts
        h.tvTotalFee.setText("Total: ₹" + m.getTotalFee());
        h.tvPaidAmount.setText("Paid: ₹" + m.getAmountPaid());
        h.tvRemaining.setText("₹" + m.getRemaining());

        // Get days overdue
        int days = m.getDaysOverdue();

        // Set status badge based on overdue days
        if (days > 15) {
            // OVERDUE - Red
            h.tvStatus.setText("OVERDUE");
            h.tvStatus.setBackgroundResource(R.drawable.bg_status_overdue);
            h.tvStatus.setTextColor(ContextCompat.getColor(context, android.R.color.white));
        } else if (days > 0) {
            // PENDING - Orange
            h.tvStatus.setText("PENDING");
            h.tvStatus.setBackgroundResource(R.drawable.bg_status_pending);
            h.tvStatus.setTextColor(ContextCompat.getColor(context, android.R.color.white));
        } else {
            // UPCOMING - Blue/Green
            h.tvStatus.setText("DUE SOON");
            h.tvStatus.setBackgroundResource(R.drawable.bg_status_due);
            h.tvStatus.setTextColor(ContextCompat.getColor(context, android.R.color.white));
        }

        // Days overdue text
        String dayText;
        int absdays = Math.abs(days);

        if (days > 0) {
            // Overdue
            dayText = absdays + (absdays == 1 ? " day" : " days") + " overdue";
            h.tvDaysAgo.setText(dayText);
            h.tvDaysAgo.setTextColor(ContextCompat.getColor(context, R.color.red));
        } else if (days == 0) {
            // Due today
            h.tvDaysAgo.setText("Due today");
            h.tvDaysAgo.setTextColor(ContextCompat.getColor(context, R.color.orange));
        } else {
            // Upcoming
            dayText = absdays + (absdays == 1 ? " day" : " days") + " left";
            h.tvDaysAgo.setText(dayText);
            h.tvDaysAgo.setTextColor(ContextCompat.getColor(context, R.color.green));
        }

        // Progress bar - Show only if partial payment made
        int amountPaid = m.getAmountPaid();
        int totalFee = m.getTotalFee();

        if (amountPaid > 0 && amountPaid < totalFee) {
            // Partial payment - show progress
            h.progressContainer.setVisibility(View.VISIBLE);
            int percentage = m.getPaymentPercentage();
            h.progressBar.setProgress(percentage);
            h.tvProgress.setText(percentage + "%");
        } else {
            // No payment or full payment - hide progress
            h.progressContainer.setVisibility(View.GONE);
        }

        // Selection state for multi-select
        boolean isSelected = selectedItems.contains(m);
        h.cardView.setStrokeWidth(isSelected ? 4 : 0);
        h.cardView.setStrokeColor(isSelected ?
                ContextCompat.getColor(context, R.color.colorPrimary) :
                ContextCompat.getColor(context, android.R.color.transparent));

        // Card click for selection
        h.cardView.setOnClickListener(v -> {
            if (selectedItems.contains(m)) {
                selectedItems.remove(m);
            } else {
                selectedItems.add(m);
            }
            notifyItemChanged(position);
            if (paymentClickListener != null) {
                paymentClickListener.onItemSelect(m, selectedItems.contains(m));
            }
        });

        // Collect button
        h.btnCollect.setOnClickListener(v -> {
            if (paymentClickListener != null) {
                paymentClickListener.onPaymentClick(m);
            }
        });
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
        TextView tvTotalFee, tvPaidAmount, tvRemaining;
        TextView tvDaysAgo, tvStatus, tvProgress;
        ProgressBar progressBar;
        LinearLayout progressContainer;
        MaterialButton btnCollect;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            tvName = itemView.findViewById(R.id.tvName);
            tvPhone = itemView.findViewById(R.id.tvPhone);
            tvPlan = itemView.findViewById(R.id.tvPlan);
            tvMonth = itemView.findViewById(R.id.tvMonth);
            tvTotalFee = itemView.findViewById(R.id.tvTotalFee);
            tvPaidAmount = itemView.findViewById(R.id.tvPaidAmount);
            tvRemaining = itemView.findViewById(R.id.tvRemaining);
            tvDaysAgo = itemView.findViewById(R.id.tvDaysAgo);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvProgress = itemView.findViewById(R.id.tvProgress);
            progressBar = itemView.findViewById(R.id.progressBar);
            progressContainer = itemView.findViewById(R.id.progressContainer);
            btnCollect = itemView.findViewById(R.id.btnCollect);
        }
    }
}