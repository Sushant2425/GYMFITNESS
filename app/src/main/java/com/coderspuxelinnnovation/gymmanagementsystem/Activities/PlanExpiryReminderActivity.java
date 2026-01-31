package com.coderspuxelinnnovation.gymmanagementsystem.Activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.coderspuxelinnnovation.gymmanagementsystem.R;
import com.coderspuxelinnnovation.gymmanagementsystem.Utils.PrefManager;
import com.coderspuxelinnnovation.gymmanagementsystem.adapters.ExpiryPagerAdapter;
import com.coderspuxelinnnovation.gymmanagementsystem.base.BaseActivity;
import com.coderspuxelinnnovation.gymmanagementsystem.models.PlanExpiryModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class PlanExpiryReminderActivity extends BaseActivity {

    // Make these class fields so they're accessible everywhere
    private ViewPager2 viewPager;
    private TabLayout tabLayout;  // New: For updating tab titles
    private final List<PlanExpiryModel> todayList = new ArrayList<>();
    private final List<PlanExpiryModel> soonList = new ArrayList<>();
    private final List<PlanExpiryModel> expiredList = new ArrayList<>();
    private static final int SMS_PERMISSION_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plan_expiry_reminder);

        // Initialize toolbar and set title
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Make sure title is shown
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(true);
            getSupportActionBar().setTitle("Plan Expiry Reminders");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        }

        // Set navigation click listener
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.setTitleTextColor(ContextCompat.getColor(this, android.R.color.white));


        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);

        ExpiryPagerAdapter pagerAdapter = new ExpiryPagerAdapter(this, todayList, soonList, expiredList);
        viewPager.setAdapter(pagerAdapter);

        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> {
                    // Initial text (counts added later)
                    switch (position) {
                        case 0:
                            tab.setText("Expiring Today");
                            break;
                        case 1:
                            tab.setText("Expiring Soon");
                            break;
                        case 2:
                            tab.setText("Expired");
                            break;
                    }
                }).attach();

        // Find and setup FAB
        FloatingActionButton fabSendAll = findViewById(R.id.fabSendAll);
        fabSendAll.setOnClickListener(v -> {
            int total = todayList.size() + soonList.size() + expiredList.size();
            if (total == 0) {
                Toast.makeText(this, "No members found!", Toast.LENGTH_SHORT).show();
                return;
            }
            new AlertDialog.Builder(this)
                    .setTitle("Send All Reminders?")
                    .setMessage("Send SMS + WhatsApp to " + total + " members?")
                    .setPositiveButton("Send All", (dialog, which) -> sendAllReminders())
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        // Make FAB visible
        fabSendAll.setVisibility(View.INVISIBLE);

        fetchExpiringMembers();
    }
    private void sendAllReminders() {
        sendRemindersFromList(todayList);
        sendRemindersFromList(soonList);
        sendRemindersFromList(expiredList);
        Toast.makeText(this, "All reminders sent successfully!", Toast.LENGTH_LONG).show();
    }

    private void sendRemindersFromList(List<PlanExpiryModel> list) {
        for (PlanExpiryModel m : list) {
            sendReminderSMS(m.phone, m.name, m.status);
            sendReminderWhatsApp(m.phone, m.name, m.status);
        }
    }

    public void sendReminderSMS(String phone, String name, String status) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.SEND_SMS}, SMS_PERMISSION_CODE);
            return;
        }

        // New: Different message templates based on remainingDays
        PlanExpiryModel model = findModelByPhone(phone);  // Helper to get model for remainingDays
        String message;
        if (model != null) {
            int days = model.remainingDays;
            if (days < 0) {
                message = "🚨 " + name + ", your plan expired " + Math.abs(days) + " days ago! Please renew. SUSHANT GYM";
            } else if (days == 0) {
                message = "⏰ " + name + ", your plan expires today! Renew now to continue. SUSHANT GYM";
            } else {
                message = "⏰ " + name + ", your plan expires in " + days + " days. Renew soon! SUSHANT GYM";
            }
        } else {
            message = "⏰ REMINDER " + name + "! Your plan expires soon. Renew now! SUSHANT GYM";  // Fallback
        }

        try {
            SmsManager.getDefault().sendTextMessage(phone, null, message, null, null);
            Toast.makeText(this, "SMS sent to " + name, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "SMS failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public void sendReminderWhatsApp(String phone, String name, String status) {
        try {
            // New: Different message templates based on remainingDays
            PlanExpiryModel model = findModelByPhone(phone);  // Helper to get model
            String message;
            if (model != null) {
                int days = model.remainingDays;
                if (days < 0) {
                    message = "🚨 " + name + ", your plan expired " + Math.abs(days) + " days ago! Please renew. SUSHANT GYM";
                } else if (days == 0) {
                    message = "⏰ " + name + ", your plan expires today! Renew now. SUSHANT GYM";
                } else {
                    message = "⏰ " + name + ", your plan expires in " + days + " days. Renew soon! SUSHANT GYM";
                }
            } else {
                message = "⏰ REMINDER " + name + "! Plan expires soon. Renew now! SUSHANT GYM";  // Fallback
            }
            String encodedMsg = URLEncoder.encode(message, "UTF-8");
            String url = "https://wa.me/91" + phone + "?text=" + encodedMsg;
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            Toast.makeText(this, "WhatsApp failed to open", Toast.LENGTH_SHORT).show();
        }
    }

    // New: Helper to find model by phone (since we need remainingDays in send methods)
    private PlanExpiryModel findModelByPhone(String phone) {
        for (PlanExpiryModel m : todayList) if (m.phone.equals(phone)) return m;
        for (PlanExpiryModel m : soonList) if (m.phone.equals(phone)) return m;
        for (PlanExpiryModel m : expiredList) if (m.phone.equals(phone)) return m;
        return null;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SMS_PERMISSION_CODE && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "SMS permission granted", Toast.LENGTH_SHORT).show();
        } else if (requestCode == SMS_PERMISSION_CODE) {
            Toast.makeText(this, "SMS permission denied", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchExpiringMembers() {
        String ownerEmail = new PrefManager(this).getUserEmail().replace(".", ",");

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("GYM").child(ownerEmail).child("members");

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                todayList.clear();
                soonList.clear();
                expiredList.clear();

                long today = getTodayMillis();
                long next3Days = getFutureMillis(3);

                for (DataSnapshot member : snapshot.getChildren()) {
                    if (!member.hasChild("currentPlan")) continue;

                    String name = member.child("info/name").getValue(String.class);
                    String phone = member.getKey();
                    String planType = member.child("currentPlan/planType").getValue(String.class);
                    String endDateStr = member.child("currentPlan/endDate").getValue(String.class);

                    if (name == null || phone == null || endDateStr == null) continue;

                    long endMillis = parseDate(endDateStr);
                    if (endMillis == 0) continue;

                    // New: Calculate remaining days
                    long millisDiff = endMillis - today;
                    int remainingDays = (int) (millisDiff / (24 * 60 * 60 * 1000));

                    PlanExpiryModel model = new PlanExpiryModel(name, phone, planType, endDateStr, "");
                    model.remainingDays = remainingDays;

                    if (remainingDays < 0) {
                        model.status = "EXPIRED";
                        expiredList.add(model);
                    } else if (remainingDays == 0) {
                        model.status = "EXPIRING TODAY";
                        todayList.add(model);
                    } else if (endMillis <= next3Days) {
                        model.status = "EXPIRING SOON";
                        soonList.add(model);
                    }
                }

                // New: Sort lists alphabetically by name for better UX
                sortListByName(todayList);
                sortListByName(soonList);
                sortListByName(expiredList);

                // Now we can safely access viewPager
                if (viewPager != null && viewPager.getAdapter() != null) {
                    viewPager.getAdapter().notifyDataSetChanged();
                    updateTabTitles();  // New: Update tabs with counts
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(PlanExpiryReminderActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // New: Update tab titles with counts
    private void updateTabTitles() {
        if (tabLayout == null) return;
        tabLayout.getTabAt(0).setText("Expiring Today (" + todayList.size() + ")");
        tabLayout.getTabAt(1).setText("Expiring Soon (" + soonList.size() + ")");
        tabLayout.getTabAt(2).setText("Expired (" + expiredList.size() + ")");
    }

    // New: Sort helper
    private void sortListByName(List<PlanExpiryModel> list) {
        Collections.sort(list, Comparator.comparing(m -> m.name.toLowerCase(Locale.getDefault())));
    }

    private long parseDate(String date) {
        try {
            return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(date).getTime();
        } catch (Exception e) {
            return 0;
        }
    }

    private long getTodayMillis() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    private long getFutureMillis(int days) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.add(Calendar.DAY_OF_MONTH, days);
        return cal.getTimeInMillis();
    }
}