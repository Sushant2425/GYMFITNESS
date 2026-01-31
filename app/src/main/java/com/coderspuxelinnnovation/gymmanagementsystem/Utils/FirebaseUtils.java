package com.coderspuxelinnnovation.gymmanagementsystem.Utils;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class FirebaseUtils {

    public static DatabaseReference membersRef(String ownerEmail) {
        return FirebaseDatabase.getInstance()
                .getReference("GYM")
                .child(ownerEmail)
                .child("members");
    }
    public static void updateLastReminderDate(String ownerEmail,
                                              String memberId,
                                              String date) {

        membersRef(ownerEmail)
                .child(memberId)
                .child("currentPlan")
                .child("lastReminderDate")
                .setValue(date);
    }

    public static void updateStatus(String ownerEmail,
                                    String memberId,
                                    String status) {

        membersRef(ownerEmail)
                .child(memberId)
                .child("currentPlan")
                .child("status")
                .setValue(status);
    }
}
