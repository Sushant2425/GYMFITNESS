package com.coderspuxelinnnovation.gymmanagementsystem.adapters;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.coderspuxelinnnovation.gymmanagementsystem.Activities.PlanExpiryReminderActivity;
import com.coderspuxelinnnovation.gymmanagementsystem.R;
import com.coderspuxelinnnovation.gymmanagementsystem.models.PlanExpiryModel;

import java.util.List;

public class ExpiryPagerAdapter extends RecyclerView.Adapter<ExpiryPagerAdapter.PageViewHolder> {

    private static final int PAGE_COUNT = 3;
    private final Context context;
    private final List<PlanExpiryModel> todayList;
    private final List<PlanExpiryModel> soonList;
    private final List<PlanExpiryModel> expiredList;

    public ExpiryPagerAdapter(Context context,
                              List<PlanExpiryModel> today,
                              List<PlanExpiryModel> soon,
                              List<PlanExpiryModel> expired) {
        this.context = context;
        this.todayList = today;
        this.soonList = soon;
        this.expiredList = expired;
    }

    @NonNull
    @Override
    public PageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.page_expiry_list, parent, false);
        return new PageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PageViewHolder holder, int position) {
        List<PlanExpiryModel> currentList;
        String emptyMessage;

        switch (position) {
            case 0:
                currentList = todayList;
                emptyMessage = context.getString(R.string.no_members_expiring_today);
                break;
            case 1:
                currentList = soonList;
                emptyMessage = context.getString(R.string.no_members_expiring_soon);
                break;
            default:
                currentList = expiredList;
                emptyMessage = context.getString(R.string.no_expired_plans);
                break;
        }

        PlanExpiryAdapter adapter = new PlanExpiryAdapter(currentList);
        holder.recyclerView.setAdapter(adapter);
        holder.recyclerView.setLayoutManager(new LinearLayoutManager(context));

        adapter.setOnReminderClickListener(new PlanExpiryAdapter.OnReminderClickListener() {
            @Override
            public void onSmsClick(String phone, String name, String status) {
                if (context instanceof PlanExpiryReminderActivity) {
                    PlanExpiryReminderActivity activity = (PlanExpiryReminderActivity) context;
                    activity.sendReminderSMS(phone, name, status);
                }
            }

            @Override
            public void onWhatsappClick(String phone, String name, String status) {
                if (context instanceof PlanExpiryReminderActivity) {
                    PlanExpiryReminderActivity activity = (PlanExpiryReminderActivity) context;
                    activity.sendReminderWhatsApp(phone, name, status);
                }
            }
        });

        holder.tvEmpty.setText(emptyMessage);
        holder.tvEmpty.setVisibility(currentList.isEmpty() ? View.VISIBLE : View.GONE);

        holder.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                adapter.filter(s.toString());
                holder.tvEmpty.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
            }
        });
    }

    @Override
    public int getItemCount() {
        return PAGE_COUNT;
    }

    static class PageViewHolder extends RecyclerView.ViewHolder {
        RecyclerView recyclerView;
        TextView tvEmpty;
        EditText etSearch;

        PageViewHolder(@NonNull View itemView) {
            super(itemView);
            recyclerView = itemView.findViewById(R.id.rvReminders);
            tvEmpty = itemView.findViewById(R.id.tvEmpty);
            etSearch = itemView.findViewById(R.id.etSearch);
        }
    }
}