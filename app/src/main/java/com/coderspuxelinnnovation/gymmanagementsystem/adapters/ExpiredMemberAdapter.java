package com.coderspuxelinnnovation.gymmanagementsystem.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.coderspuxelinnnovation.gymmanagementsystem.Activities.PlanRenewalActivity;
import com.coderspuxelinnnovation.gymmanagementsystem.R;
import com.coderspuxelinnnovation.gymmanagementsystem.models.GymMember;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class ExpiredMemberAdapter extends RecyclerView.Adapter<ExpiredMemberAdapter.ExpiredMemberViewHolder> {

    private static final String TAG = "ExpiredMemberAdapter";
    private Context context;
    private List<GymMember> expiredMembersList;

    public ExpiredMemberAdapter(Context context, List<GymMember> expiredMembersList) {
        this.context = context;
        this.expiredMembersList = expiredMembersList;
    }

    @NonNull
    @Override
    public ExpiredMemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(
                R.layout.item_member_with_renew, parent, false);
        return new ExpiredMemberViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExpiredMemberViewHolder holder, int position) {
        GymMember member = expiredMembersList.get(position);

        // Set initial (first letter of name)
        if (member.getName() != null && !member.getName().isEmpty()) {
            holder.tvInitial.setText(String.valueOf(member.getName().charAt(0)).toUpperCase());
        } else {
            holder.tvInitial.setText("?");
        }

        // Set member basic info
        holder.tvMemberName.setText(member.getName() != null ? member.getName() : "Unknown");
        holder.tvMemberPhone.setText("+91 " + member.getPhone());

        // Set plan details
        if (member.getCurrentPlan() != null) {
            GymMember.PlanDetails plan = member.getCurrentPlan();

            holder.tvPlanType.setText(plan.getPlanType() != null ? plan.getPlanType() : "No Plan");

            // Show expiry information
            if (member.getDaysExpired() > 0) {
                holder.tvEndDate.setText(member.getDaysExpired() + " days ago");
                holder.tvStatus.setText("EXPIRED");
                holder.tvStatus.setBackgroundColor(Color.parseColor("#EF5350")); // Red
                holder.statusIndicator.setCardBackgroundColor(Color.parseColor("#EF5350"));
                holder.viewBottomIndicator.setBackgroundColor(Color.parseColor("#EF5350"));
                holder.ivExpiryIcon.setColorFilter(Color.parseColor("#EF5350"));
                holder.tvEndDate.setTextColor(Color.parseColor("#EF5350"));
            } else if (member.getDaysExpired() < 0) {
                long daysRemaining = Math.abs(member.getDaysExpired());
                holder.tvEndDate.setText("Expires in " + daysRemaining + " days");
                holder.tvStatus.setText("EXPIRING");
                holder.tvStatus.setBackgroundColor(Color.parseColor("#FF9800")); // Orange
                holder.statusIndicator.setCardBackgroundColor(Color.parseColor("#FF9800"));
                holder.viewBottomIndicator.setBackgroundColor(Color.parseColor("#FF9800"));
                holder.ivExpiryIcon.setColorFilter(Color.parseColor("#FF9800"));
                holder.tvEndDate.setTextColor(Color.parseColor("#FF9800"));
            } else {
                holder.tvEndDate.setText("Expires today");
                holder.tvStatus.setText("EXPIRED");
                holder.tvStatus.setBackgroundColor(Color.parseColor("#EF5350"));
                holder.statusIndicator.setCardBackgroundColor(Color.parseColor("#EF5350"));
                holder.viewBottomIndicator.setBackgroundColor(Color.parseColor("#EF5350"));
            }
        } else {
            holder.tvPlanType.setText("No Active Plan");
            holder.tvEndDate.setText("Plan not found");
            holder.tvStatus.setText("NO PLAN");
            holder.tvStatus.setBackgroundColor(Color.parseColor("#9E9E9E"));
        }

        // Show/hide dues layout
        if (member.hasPendingDues()) {
            holder.layoutDues.setVisibility(View.VISIBLE);
            holder.tvOutstandingBalance.setText("₹" + member.getOutstandingBalance());
            Log.d(TAG, member.getName() + " - Showing dues: ₹" + member.getOutstandingBalance());
        } else {
            holder.layoutDues.setVisibility(View.GONE);
            Log.d(TAG, member.getName() + " - No dues");
        }

        // Always show renew button and divider
        holder.layoutExpiredActions.setVisibility(View.VISIBLE);
        holder.dividerLine.setVisibility(View.VISIBLE);

        // Update button text based on dues
        if (member.hasPendingDues()) {
            holder.btnRenewPlan.setText(" Renew Plan");
            holder.btnRenewPlan.setBackgroundColor(Color.parseColor("#FF9800")); // Orange for dues
        } else {
            holder.btnRenewPlan.setText(" Renew Plan");
            holder.btnRenewPlan.setBackgroundColor(Color.parseColor("#EF5350")); // Red for normal
        }

        // Renew Plan Button Click
        holder.btnRenewPlan.setOnClickListener(v -> {
            Intent intent = new Intent(context, PlanRenewalActivity.class);
            intent.putExtra("phone", member.getPhone());
            intent.putExtra("name", member.getName());
            context.startActivity(intent);
        });

        // Card Click - Show details
        holder.cardMember.setOnClickListener(v -> {
            showMemberDetailsToast(member);
        });
    }

    @Override
    public int getItemCount() {
        return expiredMembersList != null ? expiredMembersList.size() : 0;
    }

    public void updateList(List<GymMember> newList) {
        this.expiredMembersList = newList;
        notifyDataSetChanged();
        Log.d(TAG, "List updated with " + newList.size() + " members");
    }

    private void showMemberDetailsToast(GymMember member) {
        StringBuilder message = new StringBuilder();
        message.append(member.getName()).append("\n");

        if (member.getCurrentPlan() != null) {
            message.append("Plan: ").append(member.getCurrentPlan().getPlanType()).append("\n");
            message.append("Fee: ₹").append(member.getCurrentPlan().getTotalFee()).append("\n");
        }

        if (member.hasPendingDues()) {
            message.append("Pending: ₹").append(member.getOutstandingBalance());
        } else {
            message.append("No pending dues");
        }

        android.widget.Toast.makeText(context, message.toString(), android.widget.Toast.LENGTH_SHORT).show();
    }

    static class ExpiredMemberViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardMember;
        TextView tvInitial, tvMemberName, tvMemberPhone, tvPlanType, tvEndDate, tvStatus;
        TextView tvOutstandingBalance;
        LinearLayout layoutExpiredActions, layoutDues;
        MaterialButton btnRenewPlan;
        CardView statusIndicator;
        View viewBottomIndicator, dividerLine;
        ImageView ivExpiryIcon;

        public ExpiredMemberViewHolder(@NonNull View itemView) {
            super(itemView);
            cardMember = itemView.findViewById(R.id.cardMember);
            tvInitial = itemView.findViewById(R.id.tvInitial);
            tvMemberName = itemView.findViewById(R.id.tvMemberName);
            tvMemberPhone = itemView.findViewById(R.id.tvMemberPhone);
            tvPlanType = itemView.findViewById(R.id.tvPlanType);
            tvEndDate = itemView.findViewById(R.id.tvEndDate);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvOutstandingBalance = itemView.findViewById(R.id.tvOutstandingBalance);
            layoutExpiredActions = itemView.findViewById(R.id.layoutExpiredActions);
            layoutDues = itemView.findViewById(R.id.layoutDues);
            btnRenewPlan = itemView.findViewById(R.id.btnRenewPlan);
            statusIndicator = itemView.findViewById(R.id.statusIndicator);
            viewBottomIndicator = itemView.findViewById(R.id.viewBottomIndicator);
            dividerLine = itemView.findViewById(R.id.dividerLine);
            ivExpiryIcon = itemView.findViewById(R.id.ivExpiryIcon);
        }
    }
}