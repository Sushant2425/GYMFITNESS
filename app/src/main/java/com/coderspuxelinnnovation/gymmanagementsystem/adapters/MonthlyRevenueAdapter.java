package com.coderspuxelinnnovation.gymmanagementsystem.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.coderspuxelinnnovation.gymmanagementsystem.R;
import com.coderspuxelinnnovation.gymmanagementsystem.models.MonthlyRevenueModel;

import java.util.List;

public class MonthlyRevenueAdapter extends RecyclerView.Adapter<MonthlyRevenueAdapter.ViewHolder> {

    private Context context;
    private List<MonthlyRevenueModel> revenueList;

    public MonthlyRevenueAdapter(Context context, List<MonthlyRevenueModel> revenueList) {
        this.context = context;
        this.revenueList = revenueList;
    }

    public void updateList(List<MonthlyRevenueModel> newList) {
        this.revenueList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_monthly_revenue, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MonthlyRevenueModel revenue = revenueList.get(position);

        holder.tvSerial.setText(String.valueOf(position + 1));
        holder.tvMonth.setText(revenue.getMonthName());
        holder.tvTotalRevenue.setText("₹" + String.format("%,d", revenue.getTotalRevenue()));
        holder.tvPaymentCount.setText(String.valueOf(revenue.getPaymentCount()));
        holder.tvCashAmount.setText("₹" + String.format("%,d", revenue.getCashAmount()));
        holder.tvUpiAmount.setText("₹" + String.format("%,d", revenue.getUpiAmount()));
        holder.tvCardAmount.setText("₹" + String.format("%,d", revenue.getCardAmount()));

        int avgPerPayment = revenue.getPaymentCount() > 0 ? 
                revenue.getTotalRevenue() / revenue.getPaymentCount() : 0;
        holder.tvAvgPerPayment.setText("₹" + String.format("%,d", avgPerPayment));

        if (revenue.getTotalRevenue() == 0) {
            holder.tvStatus.setText("No Revenue");
            holder.tvStatus.setTextColor(Color.parseColor("#9E9E9E"));
        } else {
            holder.tvStatus.setText("Active");
            holder.tvStatus.setTextColor(Color.parseColor("#4CAF50"));
        }

        // Highlight highest revenue month
        if (position == getHighestRevenuePosition()) {
            holder.itemView.setBackgroundColor(Color.parseColor("#332A1A"));
        }
    }

    private int getHighestRevenuePosition() {
        int maxPos = 0;
        int maxRevenue = 0;
        for (int i = 0; i < revenueList.size(); i++) {
            if (revenueList.get(i).getTotalRevenue() > maxRevenue) {
                maxRevenue = revenueList.get(i).getTotalRevenue();
                maxPos = i;
            }
        }
        return maxPos;
    }

    @Override
    public int getItemCount() {
        return revenueList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSerial, tvMonth, tvTotalRevenue, tvPaymentCount, tvCashAmount, tvUpiAmount, tvCardAmount, tvAvgPerPayment, tvStatus;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSerial = itemView.findViewById(R.id.tvSerial);
            tvMonth = itemView.findViewById(R.id.tvMonth);
            tvTotalRevenue = itemView.findViewById(R.id.tvTotalRevenue);
            tvPaymentCount = itemView.findViewById(R.id.tvPaymentCount);
            tvCashAmount = itemView.findViewById(R.id.tvCashAmount);
            tvUpiAmount = itemView.findViewById(R.id.tvUpiAmount);
            tvCardAmount = itemView.findViewById(R.id.tvCardAmount);
            tvAvgPerPayment = itemView.findViewById(R.id.tvAvgPerPayment);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}