package com.coderspuxelinnnovation.gymmanagementsystem.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.coderspuxelinnnovation.gymmanagementsystem.R;
import com.coderspuxelinnnovation.gymmanagementsystem.models.PendingDueReportModel;

import java.util.List;

public class PendingDuesReportAdapter extends RecyclerView.Adapter<PendingDuesReportAdapter.ViewHolder> {

    private Context context;
    private List<PendingDueReportModel> duesList;

    public PendingDuesReportAdapter(Context context, List<PendingDueReportModel> duesList) {
        this.context = context;
        this.duesList = duesList;
    }

    public void updateList(List<PendingDueReportModel> newList) {
        this.duesList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_pending_dues_report, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PendingDueReportModel due = duesList.get(position);

        holder.tvSerial.setText(String.valueOf(position + 1));
        holder.tvMemberName.setText(due.getMemberName());
        holder.tvRemainingAmount.setText("₹" + due.getRemainingAmount());
        holder.tvPlanType.setText(due.getPlanType());
        holder.tvForMonth.setText(due.getForMonth());

        // Set overdue days with color
        int overdueDays = due.getOverdueDays();
        if (overdueDays > 30) {
            holder.tvOverdueDays.setTextColor(android.graphics.Color.parseColor("#F44336"));
            holder.tvOverdueDays.setText(overdueDays + " days");
        } else if (overdueDays > 0) {
            holder.tvOverdueDays.setTextColor(android.graphics.Color.parseColor("#FF9800"));
            holder.tvOverdueDays.setText(overdueDays + " days");
        } else {
            holder.tvOverdueDays.setTextColor(android.graphics.Color.parseColor("#4CAF50"));
            holder.tvOverdueDays.setText("Current");
        }
    }

    @Override
    public int getItemCount() {
        return duesList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSerial, tvMemberName, tvRemainingAmount, tvPlanType, tvOverdueDays, tvForMonth;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSerial = itemView.findViewById(R.id.tvSerial);
            tvMemberName = itemView.findViewById(R.id.tvMemberName);
            tvRemainingAmount = itemView.findViewById(R.id.tvRemainingAmount);
            tvPlanType = itemView.findViewById(R.id.tvPlanType);
            tvOverdueDays = itemView.findViewById(R.id.tvOverdueDays);
            tvForMonth = itemView.findViewById(R.id.tvForMonth);
        }
    }
}