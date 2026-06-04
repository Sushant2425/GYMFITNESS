package com.coderspuxelinnnovation.gymmanagementsystem.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.coderspuxelinnnovation.gymmanagementsystem.R;
import com.coderspuxelinnnovation.gymmanagementsystem.models.MemberModel;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MembersAdapter extends RecyclerView.Adapter<MembersAdapter.MemberViewHolder> {

    private List<MemberModel> members;
    private OnMemberClickListener listener;
    private Context context;

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
        context = parent.getContext();
        View view = LayoutInflater.from(context)
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

    public void updateList(List<MemberModel> newList) {
        this.members = newList;
        notifyDataSetChanged();
    }

    class MemberViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvPhone, tvPlan, tvStatus, tvExpiry;
        ImageView ivStatus;
        CardView cvStatusBadge;
        CardView cvIconBackground;

        public MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvPhone = itemView.findViewById(R.id.tvPhone);
            tvPlan = itemView.findViewById(R.id.tvPlan);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvExpiry = itemView.findViewById(R.id.tvExpiry);
            ivStatus = itemView.findViewById(R.id.ivStatus);
            cvStatusBadge = itemView.findViewById(R.id.cvStatusBadge);

            // Find the parent CardView of the icon
            View iconContainer = itemView.findViewById(R.id.ivStatus).getParent() instanceof CardView ?
                    (View) itemView.findViewById(R.id.ivStatus).getParent() : null;
            if (iconContainer instanceof CardView) {
                cvIconBackground = (CardView) iconContainer;
            }

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

            // Set basic info
            tvName.setText(info.getName());
            tvPhone.setText(info.getPhone());

            // Set plan info
            if (plan != null) {
                String planText = plan.getPlanType() + " (₹" + plan.getTotalFee() + ")";
                tvPlan.setText(planText);

                // Determine status
                boolean isActive = "ACTIVE".equals(plan.getStatus());
                boolean isExpiringSoon = isExpiringSoon(plan.getEndDate());

                // Set status text and colors - Black & Orange Theme
                if (isActive) {
                    if (isExpiringSoon) {
                        // Expiring soon - Orange/Amber theme
                        tvStatus.setText("Expiring Soon");
                        tvStatus.setTextColor(ContextCompat.getColor(context, R.color.status_pending_text));
                        cvStatusBadge.setCardBackgroundColor(ContextCompat.getColor(context, R.color.status_pending_bg));

                        ivStatus.setImageResource(R.drawable.ic_pending);
                        ivStatus.setColorFilter(ContextCompat.getColor(context, R.color.status_pending_text));
                        if (cvIconBackground != null) {
                            cvIconBackground.setCardBackgroundColor(ContextCompat.getColor(context, R.color.status_pending_bg));
                        }
                    } else {
                        // Active - Orange theme (instead of green)
                        tvStatus.setText("Active");
                        tvStatus.setTextColor(ContextCompat.getColor(context, R.color.status_active_text));
                        cvStatusBadge.setCardBackgroundColor(ContextCompat.getColor(context, R.color.status_active_bg));

                        ivStatus.setImageResource(R.drawable.ic_active);
                        ivStatus.setColorFilter(ContextCompat.getColor(context, R.color.status_active_text));
                        if (cvIconBackground != null) {
                            cvIconBackground.setCardBackgroundColor(ContextCompat.getColor(context, R.color.status_active_bg));
                        }
                    }
                } else {
                    // Expired - Red theme
                    tvStatus.setText("Expired");
                    tvStatus.setTextColor(ContextCompat.getColor(context, R.color.status_expired_text));
                    cvStatusBadge.setCardBackgroundColor(ContextCompat.getColor(context, R.color.status_expired_bg));

                    ivStatus.setImageResource(R.drawable.ic_expired);
                    ivStatus.setColorFilter(ContextCompat.getColor(context, R.color.status_expired_text));
                    if (cvIconBackground != null) {
                        cvIconBackground.setCardBackgroundColor(ContextCompat.getColor(context, R.color.status_expired_bg));
                    }
                }

                // Set expiry date
                tvExpiry.setText(plan.getEndDate());

            } else {
                tvPlan.setText("No active plan");
                tvStatus.setText("Inactive");
                tvStatus.setTextColor(ContextCompat.getColor(context, R.color.status_default_text));
                cvStatusBadge.setCardBackgroundColor(ContextCompat.getColor(context, R.color.status_default_bg));
                tvExpiry.setText("--");
                ivStatus.setImageResource(R.drawable.ic_expired);
                ivStatus.setColorFilter(ContextCompat.getColor(context, R.color.status_default_text));
                if (cvIconBackground != null) {
                    cvIconBackground.setCardBackgroundColor(ContextCompat.getColor(context, R.color.status_default_bg));
                }
            }
        }

        private boolean isExpiringSoon(String endDateStr) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                Date endDate = sdf.parse(endDateStr);

                if (endDate != null) {
                    Calendar cal = Calendar.getInstance();
                    cal.add(Calendar.DAY_OF_YEAR, 7); // Next 7 days
                    Date weekFromNow = cal.getTime();

                    Date today = new Date();
                    return endDate.after(today) && endDate.before(weekFromNow);
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
            return false;
        }
    }
}