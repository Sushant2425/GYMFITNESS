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

        if (member.getName() != null && !member.getName().isEmpty()) {
            holder.tvInitial.setText(String.valueOf(member.getName().charAt(0)).toUpperCase());
        } else {
            holder.tvInitial.setText("?");
        }

        holder.tvMemberName.setText(member.getName() != null ? member.getName() : context.getString(R.string.unknown));
        holder.tvMemberPhone.setText(context.getString(R.string.phone_with_code, member.getPhone()));

        if (member.getCurrentPlan() != null) {
            GymMember.PlanDetails plan = member.getCurrentPlan();

            holder.tvPlanType.setText(plan.getPlanType() != null ? plan.getPlanType() : context.getString(R.string.no_plan));

            if (member.getDaysExpired() > 0) {
                holder.tvEndDate.setText(context.getString(R.string.days_ago_format, member.getDaysExpired()));
                holder.tvStatus.setText(context.getString(R.string.expired_uppercase));
                holder.tvStatus.setBackgroundColor(Color.parseColor("#EF5350"));
                holder.statusIndicator.setCardBackgroundColor(Color.parseColor("#EF5350"));
                holder.viewBottomIndicator.setBackgroundColor(Color.parseColor("#EF5350"));
                holder.ivExpiryIcon.setColorFilter(Color.parseColor("#EF5350"));
                holder.tvEndDate.setTextColor(Color.parseColor("#EF5350"));
            } else if (member.getDaysExpired() < 0) {
                long daysRemaining = Math.abs(member.getDaysExpired());
                holder.tvEndDate.setText(context.getString(R.string.expires_in_days, daysRemaining));
                holder.tvStatus.setText(context.getString(R.string.expiring_uppercase));
                holder.tvStatus.setBackgroundColor(Color.parseColor("#FF9800"));
                holder.statusIndicator.setCardBackgroundColor(Color.parseColor("#FF9800"));
                holder.viewBottomIndicator.setBackgroundColor(Color.parseColor("#FF9800"));
                holder.ivExpiryIcon.setColorFilter(Color.parseColor("#FF9800"));
                holder.tvEndDate.setTextColor(Color.parseColor("#FF9800"));
            } else {
                holder.tvEndDate.setText(context.getString(R.string.expires_today));
                holder.tvStatus.setText(context.getString(R.string.expired_uppercase));
                holder.tvStatus.setBackgroundColor(Color.parseColor("#EF5350"));
                holder.statusIndicator.setCardBackgroundColor(Color.parseColor("#EF5350"));
                holder.viewBottomIndicator.setBackgroundColor(Color.parseColor("#EF5350"));
            }
        } else {
            holder.tvPlanType.setText(context.getString(R.string.no_active_plan));
            holder.tvEndDate.setText(context.getString(R.string.plan_not_found));
            holder.tvStatus.setText(context.getString(R.string.no_plan_uppercase));
            holder.tvStatus.setBackgroundColor(Color.parseColor("#9E9E9E"));
        }

        if (member.hasPendingDues()) {
            holder.layoutDues.setVisibility(View.VISIBLE);
            holder.tvOutstandingBalance.setText(context.getString(R.string.rupee_prefix) + member.getOutstandingBalance());
            Log.d(TAG, member.getName() + context.getString(R.string.showing_dues_log) + member.getOutstandingBalance());
        } else {
            holder.layoutDues.setVisibility(View.GONE);
            Log.d(TAG, member.getName() + context.getString(R.string.no_dues_log));
        }

        holder.layoutExpiredActions.setVisibility(View.VISIBLE);
        holder.dividerLine.setVisibility(View.VISIBLE);

        if (member.hasPendingDues()) {
            holder.btnRenewPlan.setText(context.getString(R.string.renew_plan_text));
            holder.btnRenewPlan.setBackgroundColor(Color.parseColor("#FF9800"));
        } else {
            holder.btnRenewPlan.setText(context.getString(R.string.renew_plan_text));
            holder.btnRenewPlan.setBackgroundColor(Color.parseColor("#EF5350"));
        }

        holder.btnRenewPlan.setOnClickListener(v -> {
            Intent intent = new Intent(context, PlanRenewalActivity.class);
            intent.putExtra("phone", member.getPhone());
            intent.putExtra("name", member.getName());
            context.startActivity(intent);
        });

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
        Log.d(TAG, context.getString(R.string.list_updated_log, newList.size()));
    }

    private void showMemberDetailsToast(GymMember member) {
        StringBuilder message = new StringBuilder();
        message.append(member.getName()).append("\n");

        if (member.getCurrentPlan() != null) {
            message.append(context.getString(R.string.plan_label)).append(member.getCurrentPlan().getPlanType()).append("\n");
            message.append(context.getString(R.string.fee_label)).append(context.getString(R.string.rupee_prefix)).append(member.getCurrentPlan().getTotalFee()).append("\n");
        }

        if (member.hasPendingDues()) {
            message.append(context.getString(R.string.pending_label)).append(context.getString(R.string.rupee_prefix)).append(member.getOutstandingBalance());
        } else {
            message.append(context.getString(R.string.no_pending_dues_message));
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