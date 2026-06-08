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
import com.coderspuxelinnnovation.gymmanagementsystem.models.ExpiringSoonReportModel;

import java.util.List;

public class ExpiringSoonReportAdapter extends RecyclerView.Adapter<ExpiringSoonReportAdapter.ViewHolder> {

    private Context context;
    private List<ExpiringSoonReportModel> expiringList;

    public ExpiringSoonReportAdapter(Context context, List<ExpiringSoonReportModel> expiringList) {
        this.context = context;
        this.expiringList = expiringList;
    }

    public void updateList(List<ExpiringSoonReportModel> newList) {
        this.expiringList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_expiring_soon_report, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ExpiringSoonReportModel member = expiringList.get(position);

        holder.tvSerial.setText(String.valueOf(position + 1));
        holder.tvMemberName.setText(member.getMemberName());
        holder.tvPhone.setText(member.getMemberPhone());
        holder.tvPlanType.setText(member.getPlanType());
        holder.tvEndDate.setText(member.getEndDate());

        int daysLeft = member.getDaysRemaining();
        holder.tvDaysLeft.setText(daysLeft + " days");

        // Set color based on days remaining
        if (daysLeft <= 3) {
            holder.tvDaysLeft.setTextColor(Color.parseColor("#F44336"));
            holder.tvDaysLeft.setBackgroundColor(Color.parseColor("#FFF0F0"));
        } else if (daysLeft <= 7) {
            holder.tvDaysLeft.setTextColor(Color.parseColor("#FF9800"));
            holder.tvDaysLeft.setBackgroundColor(Color.parseColor("#FFF4EC"));
        } else {
            holder.tvDaysLeft.setTextColor(Color.parseColor("#4CAF50"));
            holder.tvDaysLeft.setBackgroundColor(Color.parseColor("#EDF7ED"));
        }

        holder.tvTotalFee.setText("₹" + member.getTotalFee());

        if (member.getRemainingAmount() > 0) {
            holder.tvRemaining.setText("₹" + member.getRemainingAmount());
            holder.tvRemaining.setTextColor(Color.parseColor("#FF6B00"));
        } else {
            holder.tvRemaining.setText("Cleared");
            holder.tvRemaining.setTextColor(Color.parseColor("#4CAF50"));
        }
    }

    @Override
    public int getItemCount() {
        return expiringList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSerial, tvMemberName, tvPhone, tvPlanType, tvEndDate, tvDaysLeft, tvTotalFee, tvRemaining;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSerial = itemView.findViewById(R.id.tvSerial);
            tvMemberName = itemView.findViewById(R.id.tvMemberName);
            tvPhone = itemView.findViewById(R.id.tvPhone);
            tvPlanType = itemView.findViewById(R.id.tvPlanType);
            tvEndDate = itemView.findViewById(R.id.tvEndDate);
            tvDaysLeft = itemView.findViewById(R.id.tvDaysLeft);
            tvTotalFee = itemView.findViewById(R.id.tvTotalFee);
            tvRemaining = itemView.findViewById(R.id.tvRemaining);
        }
    }
}