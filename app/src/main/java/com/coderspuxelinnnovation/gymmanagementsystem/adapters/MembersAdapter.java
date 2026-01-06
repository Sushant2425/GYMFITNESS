package com.coderspuxelinnnovation.gymmanagementsystem.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.coderspuxelinnnovation.gymmanagementsystem.R;
import com.coderspuxelinnnovation.gymmanagementsystem.models.MemberModel;

import java.util.List;

public class MembersAdapter extends RecyclerView.Adapter<MembersAdapter.MemberViewHolder> {

    private List<MemberModel> members;
    private OnMemberClickListener listener;

    public interface OnMemberClickListener {
        void onMemberClick(MemberModel member);
    }

    public MembersAdapter(List<MemberModel> members, OnMemberClickListener listener) {
        this.members = members;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_member, parent, false);
        return new MemberViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        MemberModel member = members.get(position);
        holder.bind(member);
    }

    @Override
    public int getItemCount() {
        return members.size();
    }

    class MemberViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvPhone, tvPlan, tvStatus, tvExpiry;
        ImageView ivStatus;

        public MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvPhone = itemView.findViewById(R.id.tvPhone);
            tvPlan = itemView.findViewById(R.id.tvPlan);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvExpiry = itemView.findViewById(R.id.tvExpiry);
            ivStatus = itemView.findViewById(R.id.ivStatus);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onMemberClick(members.get(position));
                }
            });
        }

        void bind(MemberModel member) {
            MemberModel.Info info = member.getInfo();
            MemberModel.CurrentPlan plan = member.getCurrentPlan();

            tvName.setText(info.getName());
            tvPhone.setText(info.getPhone());
            tvPlan.setText(plan.getPlanType() + " (₹" + plan.getTotalFee() + ")");
            
            String statusText = plan.getStatus().equals("ACTIVE") ? "Active" : "Expired";
            tvStatus.setText(statusText);
            tvExpiry.setText("Expires: " + plan.getEndDate());

            // Status icon
            ivStatus.setImageResource(
                plan.getStatus().equals("ACTIVE") ? 
                R.drawable.ic_active : R.drawable.ic_expired);
        }
    }
}
