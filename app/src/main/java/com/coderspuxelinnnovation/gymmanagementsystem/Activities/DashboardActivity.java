package com.coderspuxelinnnovation.gymmanagementsystem.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.coderspuxelinnnovation.gymmanagementsystem.Fragments.DashboardFragment;
import com.coderspuxelinnnovation.gymmanagementsystem.Fragments.ProfileFragment;
import com.coderspuxelinnnovation.gymmanagementsystem.Fragments.ReportsFragment;
import com.coderspuxelinnnovation.gymmanagementsystem.Fragments.SettingsFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import com.coderspuxelinnnovation.gymmanagementsystem.R;
import com.coderspuxelinnnovation.gymmanagementsystem.base.BaseActivity;

public class DashboardActivity extends BaseActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private BottomNavigationView bottomNav;
    private Toolbar toolbar;

    private FirebaseAuth mAuth;
    private DatabaseReference databaseReference;
    private String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        initViews();
        setupFirebase();
        setupToolbar();
        setupNavigationDrawer();
        setupBottomNavigation();
        loadNavigationHeader();

        if (savedInstanceState == null) {
            loadFragment(new DashboardFragment());
        }
    }

    private void initViews() {
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        bottomNav = findViewById(R.id.bottom_navigation);
        toolbar = findViewById(R.id.toolbar);
    }

    private void setupFirebase() {
        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() != null) {
            userEmail = mAuth.getCurrentUser().getEmail();
            if (userEmail != null) {
                databaseReference = FirebaseDatabase.getInstance().getReference("GYM")
                        .child(userEmail.replace(".", ","));
            }
        }
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getString(R.string.app_name));
        }
    }

    private void setupNavigationDrawer() {
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);
    }

    private void setupBottomNavigation() {
        bottomNav.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_dashboard) {
                loadFragment(new DashboardFragment());
                return true;
            } else if (item.getItemId() == R.id.nav_settings) {
                loadFragment(new SettingsFragment());
                return true;
            }
            return false;
        });
    }

    private void loadNavigationHeader() {
        if (databaseReference == null) return;

        View headerView = navigationView.getHeaderView(0);
        TextView tvGymName = headerView.findViewById(R.id.nav_header_gym_name);
        TextView tvEmail = headerView.findViewById(R.id.nav_header_email);

        databaseReference.child("ownerInfo").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String gymName = snapshot.child("gymName").getValue(String.class);
                    String email = snapshot.child("email").getValue(String.class);

                    if (gymName != null) {
                        tvGymName.setText(gymName);
                    }
                    if (email != null) {
                        tvEmail.setText(email);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error
            }
        });
    }

    private void loadFragment(Fragment fragment) {
        if (fragment != null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.frame_container, fragment)
                    .commit();
            drawerLayout.closeDrawer(GravityCompat.START);
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        Fragment fragment = null;
        int id = item.getItemId();

        if (id == R.id.nav_dashboard) {
            fragment = new DashboardFragment();
            bottomNav.setSelectedItemId(R.id.nav_dashboard);
        } else if (id == R.id.nav_reports) {
            fragment = new ReportsFragment();
        } else if (id == R.id.nav_profile) {
            fragment = new ProfileFragment();
        } else if (id == R.id.nav_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        else if (id == R.id.nav_add_member) {
            startActivity(new Intent(this, MemberAddActivity.class));
            return true;
        }
        else if (id == R.id.nav_member_list) {
            startActivity(new Intent(this, MembersListActivity.class));
            return true;
        }

        loadFragment(fragment);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}