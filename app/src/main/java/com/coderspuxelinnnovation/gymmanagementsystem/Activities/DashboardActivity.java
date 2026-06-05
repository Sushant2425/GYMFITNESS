package com.coderspuxelinnnovation.gymmanagementsystem.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.util.Log;
import android.graphics.Color;

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
import com.google.android.material.floatingactionbutton.FloatingActionButton;
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
    private FloatingActionButton fabAddMember;

    private FirebaseAuth mAuth;
    private DatabaseReference databaseReference;
    private String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        forceStatusBarColor();

        initViews();
        setupFirebase();
        setupToolbar();
        setupNavigationDrawer();
        setupBottomNavigation();
        setupFAB();
        loadNavigationHeader();
        checkPlanAndHandleExpiry();

        if (savedInstanceState == null) {
            loadFragment(new DashboardFragment());
        }
    }

    private void forceStatusBarColor() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(Color.parseColor("#FF8C42"));

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                getWindow().getDecorView().setSystemUiVisibility(
                        getWindow().getDecorView().getSystemUiVisibility()
                                & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                );
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        forceStatusBarColor();
    }

    private void initViews() {
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        bottomNav = findViewById(R.id.bottom_navigation);
        toolbar = findViewById(R.id.toolbar);
        fabAddMember = findViewById(R.id.fab_add_member);
    }

    private void setupFirebase() {
        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() != null) {
            userEmail = mAuth.getCurrentUser().getEmail();
            if (userEmail != null) {
                String emailKey = userEmail.replace(".", ",");
                databaseReference = FirebaseDatabase.getInstance().getReference("GYM").child(emailKey);
                Log.d("Dashboard", getString(R.string.firebase_path_log, emailKey));
            }
        }
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.loading));
        }
        loadGymNameToToolbar();
    }

    private void loadGymNameToToolbar() {
        if (databaseReference == null) {
            Log.e("Dashboard", getString(R.string.database_null_toolbar));
            return;
        }

        Log.d("Toolbar", getString(R.string.loading_gym_name));

        databaseReference.child("ownerInfo").child("gymName")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String gymName = snapshot.getValue(String.class);
                        Log.d("Toolbar", getString(R.string.toolbar_gym_name_raw, gymName));

                        if (gymName != null && !gymName.trim().isEmpty()) {
                            if (getSupportActionBar() != null) {
                                getSupportActionBar().setTitle(gymName);
                                Log.d("Toolbar", getString(R.string.toolbar_set_success, gymName));
                            }
                        } else {
                            if (getSupportActionBar() != null) {
                                getSupportActionBar().setTitle(getString(R.string.my_gym));
                            }
                            Log.w("Toolbar", getString(R.string.toolbar_fallback_warning));
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("Toolbar", getString(R.string.toolbar_failed, error.getMessage()));
                        if (getSupportActionBar() != null) {
                            getSupportActionBar().setTitle(getString(R.string.gym_manager));
                        }
                    }
                });
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
            } else if (item.getItemId() == R.id.nav_placeholder) {
                return false;
            }
            return false;
        });
    }

    private void setupFAB() {
        fabAddMember.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, MemberAddActivity.class);
            startActivity(intent);
        });
    }

    private void loadNavigationHeader() {
        if (databaseReference == null) {
            Log.e("Dashboard", getString(R.string.database_ref_null));
            return;
        }

        View headerView = navigationView.getHeaderView(0);
        TextView tvGymName = headerView.findViewById(R.id.nav_header_gym_name);
        TextView tvEmail = headerView.findViewById(R.id.nav_header_email);

        tvGymName.setText(getString(R.string.loading));
        tvEmail.setText(getString(R.string.loading));

        Log.d("Dashboard", getString(R.string.loading_header_log, userEmail));

        databaseReference.child("ownerInfo").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d("Dashboard", getString(R.string.owner_info_exists, snapshot.exists()));

                if (snapshot.exists()) {
                    String gymName = snapshot.child("gymName").getValue(String.class);
                    String email = snapshot.child("email").getValue(String.class);

                    Log.d("Dashboard", getString(R.string.owner_info_raw, gymName, email));

                    if (gymName != null && !gymName.isEmpty()) {
                        tvGymName.setText(gymName);
                        Log.d("Dashboard", getString(R.string.gym_name_set, gymName));
                    } else {
                        tvGymName.setText(getString(R.string.my_gym));
                        Log.w("Dashboard", getString(R.string.gym_name_fallback));
                    }

                    if (email != null && !email.isEmpty()) {
                        tvEmail.setText(email);
                    }
                } else {
                    Log.w("Dashboard", getString(R.string.owner_info_not_found));
                    tvGymName.setText(getString(R.string.my_gym));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Dashboard", getString(R.string.failed_load_owner_info, error.getMessage()));
                View headerViewRetry = navigationView.getHeaderView(0);
                TextView tvGymNameRetry = headerViewRetry.findViewById(R.id.nav_header_gym_name);
                tvGymNameRetry.setText(getString(R.string.error_loading));
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
        } else if (id == R.id.nav_add_member) {
            startActivity(new Intent(this, MemberAddActivity.class));
            return true;
        } else if (id == R.id.nav_member_list) {
            startActivity(new Intent(this, MembersListActivity.class));
            return true;
        } else if (id == R.id.PlanExpiryReminder) {
            startActivity(new Intent(this, PlanExpiryReminderActivity.class));
            return true;
        } else if (id == R.id.nav_add_plan) {
            startActivity(new Intent(this, AddPlanActivity.class));
            return true;
        } else if (id == R.id.nav_inventory) {
            startActivity(new Intent(this, PendingDuesActivity.class));
            return true;
        }

        loadFragment(fragment);
        return true;
    }

    private void checkPlanAndHandleExpiry() {
        if (databaseReference == null) return;

        databaseReference
                .child("subscription")
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) {
                        openPremiumScreen();
                        return;
                    }

                    DataSnapshot currentPlanSnap = snapshot.child("currentPlan");
                    DataSnapshot trialSnap = snapshot.child("trial");

                    if (!currentPlanSnap.exists()) {
                        openPremiumScreen();
                        return;
                    }

                    Boolean active = currentPlanSnap.child("active").getValue(Boolean.class);
                    Long endMillis = currentPlanSnap.child("endMillis").getValue(Long.class);
                    Boolean trialUsed = trialSnap.child("used").getValue(Boolean.class);
                    long now = System.currentTimeMillis();

                    if (endMillis != null && endMillis == -1) {
                        return;
                    }

                    if (active != null && active && endMillis != null && now <= endMillis) {
                        return;
                    }

                    if (trialUsed != null && trialUsed) {
                        showTrialExpiredPopup();
                    } else {
                        openPremiumScreen();
                    }
                })
                .addOnFailureListener(e -> openPremiumScreen());
    }

    private void showTrialExpiredPopup() {
        androidx.appcompat.app.AlertDialog dialog =
                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle(getString(R.string.trial_expired_title))
                        .setMessage(getString(R.string.trial_expired_message))
                        .setCancelable(false)
                        .setPositiveButton(getString(R.string.buy_premium), (d, which) -> {
                            openPremiumScreen();
                        })
                        .create();
        dialog.show();
    }

    private void openPremiumScreen() {
        Intent intent = new Intent(this, PremiumSelectionActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
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