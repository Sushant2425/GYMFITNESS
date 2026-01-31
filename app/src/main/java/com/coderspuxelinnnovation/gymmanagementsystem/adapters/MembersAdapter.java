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

                // Set status text and colors
                if (isActive) {
                    if (isExpiringSoon) {
                        // Expiring soon
                        tvStatus.setText("Expiring Soon");
                        tvStatus.setTextColor(Color.parseColor("#FF9800"));
                        cvStatusBadge.setCardBackgroundColor(Color.parseColor("#FFF3E0"));

                        ivStatus.setImageResource(R.drawable.ic_pending);
                        ivStatus.setColorFilter(Color.parseColor("#FF9800"));
                        if (cvIconBackground != null) {
                            cvIconBackground.setCardBackgroundColor(Color.parseColor("#FFF3E0"));
                        }
                    } else {
                        // Active
                        tvStatus.setText("Active");
                        tvStatus.setTextColor(Color.parseColor("#4CAF50"));
                        cvStatusBadge.setCardBackgroundColor(Color.parseColor("#E8F5E9"));

                        ivStatus.setImageResource(R.drawable.ic_active);
                        ivStatus.setColorFilter(Color.parseColor("#4CAF50"));
                        if (cvIconBackground != null) {
                            cvIconBackground.setCardBackgroundColor(Color.parseColor("#E8F5E9"));
                        }
                    }
                } else {
                    // Expired
                    tvStatus.setText("Expired");
                    tvStatus.setTextColor(Color.parseColor("#F44336"));
                    cvStatusBadge.setCardBackgroundColor(Color.parseColor("#FFEBEE"));

                    ivStatus.setImageResource(R.drawable.ic_expired);
                    ivStatus.setColorFilter(Color.parseColor("#F44336"));
                    if (cvIconBackground != null) {
                        cvIconBackground.setCardBackgroundColor(Color.parseColor("#FFEBEE"));
                    }
                }

                // Set expiry date
                tvExpiry.setText(plan.getEndDate());

            } else {
                tvPlan.setText("No active plan");
                tvStatus.setText("Inactive");
                tvExpiry.setText("--");
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