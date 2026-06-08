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
import com.coderspuxelinnnovation.gymmanagementsystem.models.AllMembersReportModel;

import java.util.List;

public class AllMembersReportAdapter extends RecyclerView.Adapter<AllMembersReportAdapter.ViewHolder> {

    private Context context;
    private List<AllMembersReportModel> membersList;

    public AllMembersReportAdapter(Context context, List<AllMembersReportModel> membersList) {
        this.context = context;
        this.membersList = membersList;
    }

    public void updateList(List<AllMembersReportModel> newList) {
        this.membersList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_all_members_report, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AllMembersReportModel member = membersList.get(position);

        holder.tvSerial.setText(String.valueOf(position + 1));
        holder.tvName.setText(member.getName());
        holder.tvPhone.setText(member.getPhone());
        holder.tvPlanType.setText(member.getPlanType());
        holder.tvEndDate.setText(member.getEndDate());

        // Days remaining
        int daysRemaining = member.getDaysRemaining();
        if (daysRemaining >= 0) {
            holder.tvDaysLeft.setText(daysRemaining + "d");
            if (daysRemaining <= 7) {
                holder.tvDaysLeft.setTextColor(Color.parseColor("#FF9800"));
            } else {
                holder.tvDaysLeft.setTextColor(Color.parseColor("#4CAF50"));
            }
        } else {
            holder.tvDaysLeft.setText("-");
            holder.tvDaysLeft.setTextColor(Color.parseColor("#9E9E9E"));
        }

        holder.tvTotalFee.setText("₹" + member.getTotalFee());
        
        if (member.getRemainingAmount() > 0) {
            holder.tvRemaining.setText("₹" + member.getRemainingAmount());
            holder.tvRemaining.setTextColor(Color.parseColor("#FF6B00"));
        } else {
            holder.tvRemaining.setText("Cleared");
            holder.tvRemaining.setTextColor(Color.parseColor("#4CAF50"));
        }

        // Status
        if ("ACTIVE".equals(member.getMemberStatus())) {
            if (daysRemaining >= 0 && daysRemaining <= 7) {
                holder.tvStatus.setText("Expiring");
                holder.tvStatus.setTextColor(Color.parseColor("#FF9800"));
            } else {
                holder.tvStatus.setText("Active");
                holder.tvStatus.setTextColor(Color.parseColor("#4CAF50"));
            }
        } else {
            holder.tvStatus.setText("Expired");
            holder.tvStatus.setTextColor(Color.parseColor("#F44336"));
        }
    }

    @Override
    public int getItemCount() {
        return membersList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSerial, tvName, tvPhone, tvPlanType, tvEndDate, tvDaysLeft, tvTotalFee, tvRemaining, tvStatus;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSerial = itemView.findViewById(R.id.tvSerial);
            tvName = itemView.findViewById(R.id.tvName);
            tvPhone = itemView.findViewById(R.id.tvPhone);
            tvPlanType = itemView.findViewById(R.id.tvPlanType);
            tvEndDate = itemView.findViewById(R.id.tvEndDate);
            tvDaysLeft = itemView.findViewById(R.id.tvDaysLeft);
            tvTotalFee = itemView.findViewById(R.id.tvTotalFee);
            tvRemaining = itemView.findViewById(R.id.tvRemaining);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}