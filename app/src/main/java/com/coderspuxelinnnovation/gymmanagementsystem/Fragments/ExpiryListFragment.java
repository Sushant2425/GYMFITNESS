package com.coderspuxelinnnovation.gymmanagementsystem.Fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.coderspuxelinnnovation.gymmanagementsystem.Activities.PlanExpiryReminderActivity;
import com.coderspuxelinnnovation.gymmanagementsystem.R;
import com.coderspuxelinnnovation.gymmanagementsystem.adapters.PlanExpiryAdapter;
import com.coderspuxelinnnovation.gymmanagementsystem.models.PlanExpiryModel;

import java.util.List;

public class ExpiryListFragment extends Fragment {

    private final List<PlanExpiryModel> list;
    private PlanExpiryAdapter adapter;
    private TextView tvEmpty;

    public ExpiryListFragment(List<PlanExpiryModel> list) {
        this.list = list;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_expiry_list, container, false);

        RecyclerView rv = v.findViewById(R.id.rvReminders);
        tvEmpty = v.findViewById(R.id.tvEmpty);

        adapter = new PlanExpiryAdapter(list);
        rv.setAdapter(adapter);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter.setOnReminderClickListener(new PlanExpiryAdapter.OnReminderClickListener() {
            @Override
            public void onSmsClick(String phone, String name, String status) {
                ((PlanExpiryReminderActivity) getActivity()).sendReminderSMS(phone, name, status);
            }

            @Override
            public void onWhatsappClick(String phone, String name, String status) {
                ((PlanExpiryReminderActivity) getActivity()).sendReminderWhatsApp(phone, name, status);
            }
        });

        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
    }
}