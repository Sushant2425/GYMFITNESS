package com.coderspuxelinnnovation.gymmanagementsystem.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.coderspuxelinnnovation.gymmanagementsystem.R;
import com.coderspuxelinnnovation.gymmanagementsystem.Utils.PrefManager;
import com.coderspuxelinnnovation.gymmanagementsystem.base.BaseActivity;
import com.coderspuxelinnnovation.gymmanagementsystem.adapters.MembersAdapter;
import com.coderspuxelinnnovation.gymmanagementsystem.models.MemberModel;
import com.google.android.material.chip.Chip;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MembersListActivity extends BaseActivity {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private SearchView searchView;
    private ExtendedFloatingActionButton fabAddMember;
    private LinearLayout emptyState;
    private TextView tvTitle;
    private MembersAdapter adapter;
    private ArrayList<MemberModel> memberList;
    private ArrayList<MemberModel> filteredList;

    // Filter chips
    private Chip chipAll, chipActive, chipExpired, chipExpiring;
    private String currentFilter = "ALL";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_members_list);

        initViews();
        setupToolbar();
        setupRecyclerView();
        setupSearch();
        setupFilterChips();
        setupFab();
        loadMembers();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerMembers);
        progressBar = findViewById(R.id.progressBar);
        searchView = findViewById(R.id.searchView);
        fabAddMember = findViewById(R.id.fabAddMember);
        emptyState = findViewById(R.id.emptyState);
        tvTitle = findViewById(R.id.tvTitle);

        // Filter chips
        chipAll = findViewById(R.id.chipAll);
        chipActive = findViewById(R.id.chipActive);
        chipExpired = findViewById(R.id.chipExpired);
        chipExpiring = findViewById(R.id.chipExpiring);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        memberList = new ArrayList<>();
        filteredList = new ArrayList<>();
        adapter = new MembersAdapter(filteredList, this::onMemberClick);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        recyclerView.setHasFixedSize(true);

        // Add scroll listener for FAB
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0) {
                    fabAddMember.shrink();
                } else if (dy < 0) {
                    fabAddMember.extend();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadMembers();
    }

    private void setupSearch() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterMembers(newText);
                return true;
            }
        });
    }

    private void setupFilterChips() {
        chipAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                currentFilter = "ALL";
                applyFilter();
            }
        });

        chipActive.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                currentFilter = "ACTIVE";
                applyFilter();
            }
        });

        chipExpired.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                currentFilter = "EXPIRED";
                applyFilter();
            }
        });

        chipExpiring.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                currentFilter = "EXPIRING";
                applyFilter();
            }
        });
    }

    private void setupFab() {
        fabAddMember.setOnClickListener(v -> {
            startActivity(new Intent(this, MemberAddActivity.class));
        });
    }

    private void loadMembers() {
        progressBar.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.GONE);

        String ownerEmail = new PrefManager(this).getUserEmail().replace(".", ",");

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("GYM")
                .child(ownerEmail)
                .child("members");

        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                progressBar.setVisibility(View.GONE);
                memberList.clear();

                for (DataSnapshot memberSnapshot : snapshot.getChildren()) {
                    MemberModel member = memberSnapshot.getValue(MemberModel.class);
                    if (member != null && member.getInfo() != null) {
                        member.setPhone(memberSnapshot.getKey());
                        memberList.add(member);
                    }
                }

                // Update title
                updateTitle();

                // Apply current filter
                applyFilter();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(MembersListActivity.this,
                        getString(R.string.error_loading_members) + ": " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateTitle() {
        tvTitle.setText("Members (" + memberList.size() + ")");
    }

    private void applyFilter() {
        filteredList.clear();

        for (MemberModel member : memberList) {
            boolean shouldAdd = false;

            switch (currentFilter) {
                case "ALL":
                    shouldAdd = true;
                    break;
                case "ACTIVE":
                    shouldAdd = member.getCurrentPlan() != null &&
                            "ACTIVE".equals(member.getCurrentPlan().getStatus());
                    break;
                case "EXPIRED":
                    shouldAdd = member.getCurrentPlan() != null &&
                            "EXPIRED".equals(member.getCurrentPlan().getStatus());
                    break;
                case "EXPIRING":
                    shouldAdd = isExpiringSoon(member);
                    break;
            }

            if (shouldAdd) {
                filteredList.add(member);
            }
        }

        // Apply search query if exists
        String query = searchView.getQuery().toString();
        if (!query.isEmpty()) {
            filterMembers(query);
        } else {
            updateEmptyState();
            adapter.notifyDataSetChanged();
        }
    }

    private boolean isExpiringSoon(MemberModel member) {
        if (member.getCurrentPlan() == null ||
                !"ACTIVE".equals(member.getCurrentPlan().getStatus())) {
            return false;
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date endDate = sdf.parse(member.getCurrentPlan().getEndDate());

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

    private void filterMembers(String query) {
        if (query.isEmpty()) {
            applyFilter();
            return;
        }

        ArrayList<MemberModel> tempList = new ArrayList<>(filteredList);
        filteredList.clear();

        for (MemberModel member : tempList) {
            if (member.getInfo().getName().toLowerCase().contains(query.toLowerCase()) ||
                    member.getPhone().contains(query)) {
                filteredList.add(member);
            }
        }

        updateEmptyState();
        adapter.notifyDataSetChanged();
    }

    private void updateEmptyState() {
        if (filteredList.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void onMemberClick(MemberModel member) {
        Intent intent = new Intent(this, MemberDetailActivity.class);
        intent.putExtra("phone", member.getPhone());
        startActivity(intent);
    }
}