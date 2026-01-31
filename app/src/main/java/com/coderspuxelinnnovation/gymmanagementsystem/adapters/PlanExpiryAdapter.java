package com.coderspuxelinnnovation.gymmanagementsystem.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.coderspuxelinnnovation.gymmanagementsystem.R;
import com.coderspuxelinnnovation.gymmanagementsystem.models.PlanExpiryModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PlanExpiryAdapter extends RecyclerView.Adapter<PlanExpiryAdapter.VH> {
    private final List<PlanExpiryModel> originalList;
    private final List<PlanExpiryModel> filteredList;

    public interface OnReminderClickListener {
        void onSmsClick(String phone, String name, String status);
        void onWhatsappClick(String phone, String name, String status);
    }

    private OnReminderClickListener reminderListener;

    public PlanExpiryAdapter(List<PlanExpiryModel> list) {
        this.originalList = new ArrayList<>(list);
        this.filteredList = new ArrayList<>(list);
    }

    public void setOnReminderClickListener(OnReminderClickListener listener) {
        this.reminderListener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_plan_expiry, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int i) {
        PlanExpiryModel m = filteredList.get(i);

        h.tvName.setText(m.name);
        h.tvPhone.setText("📞 " + m.phone);
        h.tvPlan.setText("Plan: " + (m.planType != null ? m.planType : "Unknown"));
        h.tvEndDate.setText("Expiry: " + m.endDate);
        h.chipStatus.setText(m.status);
        setStatusColor(h.chipStatus, m.status);

        // New: Display remaining days
        String remainingText;
        if (m.remainingDays > 0) {
            remainingText = "Expires in " + m.remainingDays + " days";
        } else if (m.remainingDays == 0) {
            remainingText = "Expires today";
        } else {
            remainingText = "Expired " + Math.abs(m.remainingDays) + " days ago";
        }
        h.tvRemaining.setText(remainingText);

        h.btnSms.setOnClickListener(v -> {
            if (reminderListener != null) {
                reminderListener.onSmsClick(m.phone, m.name, m.status);
            }
        });

        h.btnWhatsapp.setOnClickListener(v -> {
            if (reminderListener != null) {
                reminderListener.onWhatsappClick(m.phone, m.name, m.status);
            }
        });
    }

    private void setStatusColor(Chip chip, String status) {
        int colorRes;
        if ("EXPIRED".equals(status)) {
            colorRes = R.color.red;
        } else if ("EXPIRING TODAY".equals(status) || "EXPIRING SOON".equals(status)) {
            colorRes = R.color.orange;
        } else {
            colorRes = R.color.green;
        }
        chip.setChipBackgroundColor(
                ContextCompat.getColorStateList(chip.getContext(), colorRes)
        );
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    // New: Filter method for search
    public void filter(String query) {
        filteredList.clear();
        if (query.isEmpty()) {
            filteredList.addAll(originalList);
        } else {
            String lowerQuery = query.toLowerCase(Locale.getDefault());
            for (PlanExpiryModel m : originalList) {
                if (m.name.toLowerCase(Locale.getDefault()).contains(lowerQuery) ||
                        m.phone.contains(lowerQuery)) {
                    filteredList.add(m);
                }
            }
        }
        notifyDataSetChanged();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvPhone, tvPlan, tvEndDate, tvRemaining;  // New: tvRemaining
        Chip chipStatus;
        MaterialButton btnSms, btnWhatsapp;

        VH(View v) {
            super(v);
            tvName = v.findViewById(R.id.tvName);
            tvPhone = v.findViewById(R.id.tvPhone);
            tvPlan = v.findViewById(R.id.tvPlan);
            tvEndDate = v.findViewById(R.id.tvEndDate);
            chipStatus = v.findViewById(R.id.chipStatus);
            btnSms = v.findViewById(R.id.btnSms);
            btnWhatsapp = v.findViewById(R.id.btnWhatsapp);
            tvRemaining = v.findViewById(R.id.tvRemaining);  // New
        }
    }
}