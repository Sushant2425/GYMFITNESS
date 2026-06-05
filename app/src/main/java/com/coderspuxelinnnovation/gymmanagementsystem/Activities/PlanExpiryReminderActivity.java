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

    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private final List<PlanExpiryModel> todayList = new ArrayList<>();
    private final List<PlanExpiryModel> soonList = new ArrayList<>();
    private final List<PlanExpiryModel> expiredList = new ArrayList<>();
    private static final int SMS_PERMISSION_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plan_expiry_reminder);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.plan_expiry_reminders));
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.setTitleTextColor(ContextCompat.getColor(this, android.R.color.white));

        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);

        ExpiryPagerAdapter pagerAdapter = new ExpiryPagerAdapter(this, todayList, soonList, expiredList);
        viewPager.setAdapter(pagerAdapter);

        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> {
                    switch (position) {
                        case 0:
                            tab.setText(getString(R.string.expiring_today));
                            break;
                        case 1:
                            tab.setText(getString(R.string.expiring_soon));
                            break;
                        case 2:
                            tab.setText(getString(R.string.expired));
                            break;
                    }
                }).attach();

        FloatingActionButton fabSendAll = findViewById(R.id.fabSendAll);
        fabSendAll.setOnClickListener(v -> {
            int total = todayList.size() + soonList.size() + expiredList.size();
            if (total == 0) {
                Toast.makeText(this, getString(R.string.no_members_found), Toast.LENGTH_SHORT).show();
                return;
            }
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.send_all_reminders))
                    .setMessage(getString(R.string.send_all_reminders_message, total))
                    .setPositiveButton(getString(R.string.send_all), (dialog, which) -> sendAllReminders())
                    .setNegativeButton(getString(R.string.cancel), null)
                    .show();
        });

        fabSendAll.setVisibility(View.INVISIBLE);

        fetchExpiringMembers();
    }

    private void sendAllReminders() {
        sendRemindersFromList(todayList);
        sendRemindersFromList(soonList);
        sendRemindersFromList(expiredList);
        Toast.makeText(this, getString(R.string.all_reminders_sent), Toast.LENGTH_LONG).show();
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

        PlanExpiryModel model = findModelByPhone(phone);
        String message;
        if (model != null) {
            int days = model.remainingDays;
            if (days < 0) {
                message = getString(R.string.sms_plan_expired, name, Math.abs(days));
            } else if (days == 0) {
                message = getString(R.string.sms_plan_expires_today, name);
            } else {
                message = getString(R.string.sms_plan_expires_in, name, days);
            }
        } else {
            message = getString(R.string.sms_plan_expires_soon_fallback, name);
        }

        try {
            SmsManager.getDefault().sendTextMessage(phone, null, message, null, null);
            Toast.makeText(this, getString(R.string.sms_sent_to, name), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.sms_failed, e.getMessage()), Toast.LENGTH_SHORT).show();
        }
    }

    public void sendReminderWhatsApp(String phone, String name, String status) {
        try {
            PlanExpiryModel model = findModelByPhone(phone);
            String message;
            if (model != null) {
                int days = model.remainingDays;
                if (days < 0) {
                    message = getString(R.string.whatsapp_plan_expired, name, Math.abs(days));
                } else if (days == 0) {
                    message = getString(R.string.whatsapp_plan_expires_today, name);
                } else {
                    message = getString(R.string.whatsapp_plan_expires_in, name, days);
                }
            } else {
                message = getString(R.string.whatsapp_plan_expires_soon_fallback, name);
            }
            String encodedMsg = URLEncoder.encode(message, "UTF-8");
            String url = "https://wa.me/91" + phone + "?text=" + encodedMsg;
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.whatsapp_failed), Toast.LENGTH_SHORT).show();
        }
    }

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
            Toast.makeText(this, getString(R.string.sms_permission_granted), Toast.LENGTH_SHORT).show();
        } else if (requestCode == SMS_PERMISSION_CODE) {
            Toast.makeText(this, getString(R.string.sms_permission_denied), Toast.LENGTH_SHORT).show();
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

                    long millisDiff = endMillis - today;
                    int remainingDays = (int) (millisDiff / (24 * 60 * 60 * 1000));

                    PlanExpiryModel model = new PlanExpiryModel(name, phone, planType, endDateStr, "");
                    model.remainingDays = remainingDays;

                    if (remainingDays < 0) {
                        model.status = getString(R.string.expired_uppercase);
                        expiredList.add(model);
                    } else if (remainingDays == 0) {
                        model.status = getString(R.string.expiring_today_uppercase);
                        todayList.add(model);
                    } else if (endMillis <= next3Days) {
                        model.status = getString(R.string.expiring_soon_uppercase);
                        soonList.add(model);
                    }
                }

                sortListByName(todayList);
                sortListByName(soonList);
                sortListByName(expiredList);

                if (viewPager != null && viewPager.getAdapter() != null) {
                    viewPager.getAdapter().notifyDataSetChanged();
                    updateTabTitles();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(PlanExpiryReminderActivity.this,
                        getString(R.string.error_prefix, error.getMessage()), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateTabTitles() {
        if (tabLayout == null) return;
        tabLayout.getTabAt(0).setText(getString(R.string.expiring_today_with_count, todayList.size()));
        tabLayout.getTabAt(1).setText(getString(R.string.expiring_soon_with_count, soonList.size()));
        tabLayout.getTabAt(2).setText(getString(R.string.expired_with_count, expiredList.size()));
    }

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