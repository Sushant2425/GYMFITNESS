package com.coderspuxelinnnovation.gymmanagementsystem.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.coderspuxelinnnovation.gymmanagementsystem.Fragments.PaidDuesFragment;
import com.coderspuxelinnnovation.gymmanagementsystem.Fragments.PendingDuesFragment;
import com.coderspuxelinnnovation.gymmanagementsystem.models.PendingDueModel;

import java.util.ArrayList;
import java.util.List;

public class PaymentTabsAdapter extends FragmentStateAdapter {

    private PendingDuesFragment pendingFragment;
    private PaidDuesFragment paidFragment;

    public PaymentTabsAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
        pendingFragment = new PendingDuesFragment();
        paidFragment = new PaidDuesFragment();
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 0) {
            return pendingFragment;
        } else {
            return paidFragment;
        }
    }

    @Override
    public int getItemCount() {
        return 2; // Pending and Paid tabs
    }

    public void updatePendingList(List<PendingDueModel> list) {
        if (pendingFragment != null) {
            pendingFragment.updateList(list);
        }
    }

    public void updatePaidList(List<PendingDueModel> list) {
        if (paidFragment != null) {
            paidFragment.updateList(list);
        }
    }

    public List<PendingDueModel> getPendingList() {
        return pendingFragment != null ? pendingFragment.getList() : new ArrayList<>();
    }

    public List<PendingDueModel> getPaidList() {
        return paidFragment != null ? paidFragment.getList() : new ArrayList<>();
    }
}