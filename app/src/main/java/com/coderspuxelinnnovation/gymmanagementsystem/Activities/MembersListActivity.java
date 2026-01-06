package com.coderspuxelinnnovation.gymmanagementsystem.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.coderspuxelinnnovation.gymmanagementsystem.R;
import com.coderspuxelinnnovation.gymmanagementsystem.Utils.PrefManager;
import com.coderspuxelinnnovation.gymmanagementsystem.base.BaseActivity;
import com.coderspuxelinnnovation.gymmanagementsystem.adapters.MembersAdapter;
import com.coderspuxelinnnovation.gymmanagementsystem.models.MemberModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MembersListActivity extends BaseActivity {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private SearchView searchView;
    private FloatingActionButton fabAddMember;
    private MembersAdapter adapter;
    private ArrayList<MemberModel> memberList;
    private ArrayList<MemberModel> filteredList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_members_list);

        initViews();
        setupRecyclerView();
        setupSearch();
        loadMembers();
        setupFab();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerMembers);
        progressBar = findViewById(R.id.progressBar);
        searchView = findViewById(R.id.searchView);
        fabAddMember = findViewById(R.id.fabAddMember);
    }

    private void setupRecyclerView() {
        memberList = new ArrayList<>();
        filteredList = new ArrayList<>();
        adapter = new MembersAdapter(filteredList, this::onMemberClick);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        recyclerView.setHasFixedSize(true);
    }
    // Add this method to MembersListActivity
    @Override
    protected void onResume() {
        super.onResume();
        loadMembers(); // Refresh list when returning
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

    private void setupFab() {
        fabAddMember.setOnClickListener(v -> {
            startActivity(new Intent(this, MemberAddActivity.class));
        });
    }

    private void loadMembers() {
        progressBar.setVisibility(View.VISIBLE);
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

                filteredList.clear();
                filteredList.addAll(memberList);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(MembersListActivity.this,
                        "Error loading members: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterMembers(String query) {
        filteredList.clear();
        if (query.isEmpty()) {
            filteredList.addAll(memberList);
        } else {
            for (MemberModel member : memberList) {
                if (member.getInfo().getName().toLowerCase().contains(query.toLowerCase()) ||
                        member.getPhone().contains(query)) {
                    filteredList.add(member);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void onMemberClick(MemberModel member) {
        Intent intent = new Intent(this, MemberDetailActivity.class);
        intent.putExtra("phone", member.getPhone());
        startActivity(intent);
    }
}
