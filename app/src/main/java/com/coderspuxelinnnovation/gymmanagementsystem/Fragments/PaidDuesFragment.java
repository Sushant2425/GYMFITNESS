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

import com.coderspuxelinnnovation.gymmanagementsystem.R;
import com.coderspuxelinnnovation.gymmanagementsystem.adapters.PaidDuesAdapter;
import com.coderspuxelinnnovation.gymmanagementsystem.models.PendingDueModel;

import java.util.ArrayList;
import java.util.List;

public class PaidDuesFragment extends Fragment {

    private RecyclerView recyclerView;
    private TextView tvEmpty;
    private PaidDuesAdapter adapter;
    private List<PendingDueModel> list = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_paid_dues, container, false);

        recyclerView = view.findViewById(R.id.recyclerView);
        tvEmpty = view.findViewById(R.id.tvEmpty);

        setupRecyclerView();
        return view;
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new PaidDuesAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);
    }

    public void updateList(List<PendingDueModel> newList) {
        this.list = new ArrayList<>(newList);
        if (adapter != null) {
            adapter.updateList(newList);
        }

        if (tvEmpty != null) {
            if (newList.isEmpty()) {
                tvEmpty.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                tvEmpty.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            }
        }
    }

    public List<PendingDueModel> getList() {
        return new ArrayList<>(list);
    }
}