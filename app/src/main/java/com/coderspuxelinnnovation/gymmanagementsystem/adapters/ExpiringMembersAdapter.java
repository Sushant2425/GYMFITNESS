package com.coderspuxelinnnovation.gymmanagementsystem.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.coderspuxelinnnovation.gymmanagementsystem.R;
import com.coderspuxelinnnovation.gymmanagementsystem.models.Member;

import java.util.List;

public class ExpiringMembersAdapter extends RecyclerView.Adapter<ExpiringMembersAdapter.ViewHolder> {

    private Context context;
    private List<Member> memberList;

    public ExpiringMembersAdapter(Context context, List<Member> memberList) {
        this.context = context;
        this.memberList = memberList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_expiring_member, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Member member = memberList.get(position);
        holder.tvMemberName.setText(member.getName());
        holder.tvMemberPhone.setText(member.getPhone());
        holder.tvExpiryDate.setText(member.getEndDate());
    }

    @Override
    public int getItemCount() {
        return memberList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvMemberName, tvMemberPhone, tvExpiryDate;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMemberName = itemView.findViewById(R.id.tv_member_name);
            tvMemberPhone = itemView.findViewById(R.id.tv_member_phone);
            tvExpiryDate = itemView.findViewById(R.id.tv_expiry_date);
        }
    }
}