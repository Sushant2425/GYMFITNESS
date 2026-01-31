package com.coderspuxelinnnovation.gymmanagementsystem.worker;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.coderspuxelinnnovation.gymmanagementsystem.Utils.FirebaseUtils;
import com.coderspuxelinnnovation.gymmanagementsystem.Utils.PlanStatusUtils;
import com.coderspuxelinnnovation.gymmanagementsystem.Utils.PrefManager;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import android.telephony.SmsManager;
import com.coderspuxelinnnovation.gymmanagementsystem.Utils.DateUtils;

public class PlanStatusWorker extends Worker {

    public PlanStatusWorker(
            @NonNull Context context,
            @NonNull WorkerParameters workerParams
    ) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {

        Log.d("PLAN_WORKER", "Worker STARTED");

        PrefManager prefManager = new PrefManager(getApplicationContext());
        String email = prefManager.getUserEmail();

        if (email == null) {
            Log.d("PLAN_WORKER", "No email found");
            return Result.success();
        }

        final String ownerEmail = email.replace(".", ",");

        CountDownLatch latch = new CountDownLatch(1);

        FirebaseUtils.membersRef(ownerEmail)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        for (DataSnapshot memberSnap : snapshot.getChildren()) {

                            String memberId = memberSnap.getKey();
                            DataSnapshot planSnap = memberSnap.child("currentPlan");

                            if (!planSnap.exists()) continue;

                            String status = planSnap.child("status").getValue(String.class);
                            String endDate = planSnap.child("endDate").getValue(String.class);

        /* ===============================
           1️⃣ AUTO SMS : 3 DAYS BEFORE
           =============================== */
                            String today = DateUtils.todayDateString();
                            String lastReminder =
                                    planSnap.child("lastReminderDate").getValue(String.class);

                            boolean alreadySentToday = today.equals(lastReminder);

                            if ("ACTIVE".equals(status)
                                    && DateUtils.isWithinNextDays(endDate, 3)
                                    && !alreadySentToday) {

                                SmsManager smsManager = SmsManager.getDefault();

                                String message =
                                        "Hello,\n"
                                                + "Tumcha gym plan "
                                                + endDate
                                                + " la sampat aahe.\n"
                                                + "Krupaya time var renewal kara.\n\n"
                                                + "- " + prefManager.getGymName();

                                smsManager.sendTextMessage(
                                        memberId,   // mobile number
                                        null,
                                        message,
                                        null,
                                        null
                                );

                                FirebaseUtils.updateLastReminderDate(
                                        ownerEmail,
                                        memberId,
                                        today
                                );

                                Log.d("PLAN_WORKER",
                                        "Auto reminder SMS sent to: " + memberId);
                            }

        /* ===============================
           2️⃣ EXPIRED LOGIC (AS IT IS)
           =============================== */
                            if ("ACTIVE".equals(status)
                                    && PlanStatusUtils.isPlanExpired(endDate)) {

                                FirebaseUtils.updateStatus(
                                        ownerEmail,
                                        memberId,
                                        "EXPIRED"
                                );

                                Log.d("PLAN_WORKER",
                                        "Expired updated: " + memberId);
                            }
                        }

                        latch.countDown(); // ✅ VERY IMPORTANT
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        latch.countDown();
                    }

                });

        try {
            latch.await(20, TimeUnit.SECONDS); // worker wait
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Log.d("PLAN_WORKER", "Worker FINISHED");
        return Result.success();
    }
}
