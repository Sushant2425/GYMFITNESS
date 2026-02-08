package com.coderspuxelinnnovation.gymmanagementsystem.Utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class PremiumStatusManager {

    public static final int TRIAL_DAYS = 7;

    // 🔹 Trial End = last day 11:59:59 PM
    public static long getTrialEndDate(long startMillis) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(startMillis);

        cal.add(Calendar.DAY_OF_YEAR, TRIAL_DAYS);

        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);

        return cal.getTimeInMillis();
    }

    // 🔹 Premium expiry (from NOW)
    public static long getPremiumEndDate(String plan) {
        Calendar cal = Calendar.getInstance();

        switch (plan) {
            case "6_MONTH":
                cal.add(Calendar.MONTH, 6);
                break;
            case "1_YEAR":
                cal.add(Calendar.YEAR, 1);
                break;
            case "LIFETIME":
                return -1;
        }

        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);

        return cal.getTimeInMillis();
    }

    public static boolean isValid(long endMillis) {
        return endMillis == -1 || System.currentTimeMillis() <= endMillis;
    }

    public static String formatDate(long millis) {
        SimpleDateFormat sdf =
                new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        return sdf.format(new Date(millis));
    }
}
