package com.coderspuxelinnnovation.gymmanagementsystem.Fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.coderspuxelinnnovation.gymmanagementsystem.Activities.AddPlanActivity;
import com.coderspuxelinnnovation.gymmanagementsystem.Activities.SettingsActivity;
import com.coderspuxelinnnovation.gymmanagementsystem.R;
import com.coderspuxelinnnovation.gymmanagementsystem.Registration.LoginActivity;
import com.coderspuxelinnnovation.gymmanagementsystem.Utils.PrefManager;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class SettingsFragment extends Fragment {

    private static final String TAG = "SettingsFragment";

    private PrefManager prefManager;
    private TextView tvGymName, tvOwnerEmail;
    private Chip chipLanguage;
    private MaterialCardView cardLanguage, cardPlans, cardProfile, cardAbout, cardLogout;

    private DatabaseReference databaseReference;

    public SettingsFragment() {
        // Required empty public constructor
    }

    public static SettingsFragment newInstance() {
        return new SettingsFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefManager = new PrefManager(requireContext());
        databaseReference = FirebaseDatabase.getInstance().getReference();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        loadOwnerInfoFromFirebase();
        setupClickListeners();
    }

    private void initViews(View view) {
        tvGymName = view.findViewById(R.id.tvGymName);
        tvOwnerEmail = view.findViewById(R.id.tvOwnerEmail);
        chipLanguage = view.findViewById(R.id.chipLanguage);
        cardLanguage = view.findViewById(R.id.cardLanguage);
        cardPlans = view.findViewById(R.id.cardPlans);
        cardProfile = view.findViewById(R.id.cardProfile);
        cardAbout = view.findViewById(R.id.cardAbout);
        cardLogout = view.findViewById(R.id.cardLogout);
    }

    private void loadOwnerInfoFromFirebase() {
        String userEmail = prefManager.getUserEmail();

        if (userEmail == null || userEmail.isEmpty()) {
            Log.e(TAG, "User email not found in preferences");
            loadUserInfoFromPrefs();
            return;
        }

        // Show loading state
        tvGymName.setText(R.string.loading);
        tvOwnerEmail.setText("");

        // Query Firebase for owner info using email
        databaseReference.child("GYM")
                .orderByChild("ownerInfo/email")
                .equalTo(userEmail)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot gymSnapshot : snapshot.getChildren()) {
                                DataSnapshot ownerInfo = gymSnapshot.child("ownerInfo");

                                String gymName = ownerInfo.child("gymName").getValue(String.class);
                                String email = ownerInfo.child("email").getValue(String.class);
                                String ownerName = ownerInfo.child("name").getValue(String.class);
                                String phone = ownerInfo.child("phone").getValue(String.class);

                                // Save to preferences for offline access
                                prefManager.saveOwnerData(
                                        email != null ? email : "",
                                        gymName != null ? gymName : "",
                                        ownerName != null ? ownerName : "",
                                        phone != null ? phone : ""
                                );

                                // Update UI
                                updateUI(gymName, email);

                                Log.d(TAG, "Owner info loaded successfully from Firebase");
                                break;
                            }
                        } else {
                            Log.e(TAG, "No gym found for email: " + userEmail);
                            // Fallback to preferences
                            loadUserInfoFromPrefs();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Firebase error: " + error.getMessage());
                        showToast(getString(R.string.error_loading_data));
                        // Fallback to preferences
                        loadUserInfoFromPrefs();
                    }
                });
    }

    private void loadUserInfoFromPrefs() {
        String gymName = prefManager.getGymName();
        String email = prefManager.getUserEmail();

        updateUI(gymName, email);
        updateLanguageChip();
    }

    private void updateUI(String gymName, String email) {
        // Display gym name
        if (gymName != null && !gymName.isEmpty()) {
            tvGymName.setText(gymName);
        } else {
            tvGymName.setText(R.string.my_gym);
        }

        // Display email
        if (email != null && !email.isEmpty()) {
            tvOwnerEmail.setText(email);
        } else {
            tvOwnerEmail.setText("");
        }

        // Update language chip
        updateLanguageChip();
    }

    private void updateLanguageChip() {
        String currentLang = prefManager.getLanguage();
        if ("mr".equals(currentLang)) {
            chipLanguage.setText(R.string.marathi);
        } else if ("hi".equals(currentLang)) {
            chipLanguage.setText(R.string.hindi);
        } else {
            chipLanguage.setText(R.string.english);
        }
    }

    private void setupClickListeners() {
        // Language Settings
        cardLanguage.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), SettingsActivity.class);
            startActivity(intent);
        });

        // Manage Plans
        cardPlans.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), AddPlanActivity.class);
            startActivity(intent);
        });

        // Profile
        cardProfile.setOnClickListener(v -> {
            showToast(getString(R.string.profile_coming_soon));
        });

        // About
        cardAbout.setOnClickListener(v -> {
            showAboutDialog();
        });

        // Logout
        cardLogout.setOnClickListener(v -> {
            showLogoutDialog();
        });
    }

    private void showAboutDialog() {
        String message = getString(R.string.version) + ": 1.0.0\n\n" +
                getString(R.string.developed_by) + ": " +
                getString(R.string.company_name) + "\n\n" +
                getString(R.string.app_description);

        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.app_name))
                .setMessage(message)
                .setPositiveButton(R.string.ok, null)
                .show();
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.logout)
                .setMessage(R.string.logout_confirmation)
                .setPositiveButton(R.string.logout, (dialog, which) -> {
                    prefManager.logout();
                    Intent intent = new Intent(requireContext(), LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    requireActivity().finish();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showToast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh data when returning to fragment
        loadOwnerInfoFromFirebase();
    }
}