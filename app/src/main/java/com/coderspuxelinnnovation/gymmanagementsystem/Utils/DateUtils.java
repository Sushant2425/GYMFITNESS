package com.coderspuxelinnnovation.gymmanagementsystem.Utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DateUtils {

    private static final char[] MARATHI_DIGITS = {'०','१','२','३','४','५','६','७','८','९'};
    private static final char[] ENGLISH_DIGITS = {'0','1','2','3','4','5','6','7','8','9'};

    public static String marathiToEnglishDigits(String input) {
        if (input == null) return "";
        for (int i = 0; i < MARATHI_DIGITS.length; i++) {
            input = input.replace(MARATHI_DIGITS[i], ENGLISH_DIGITS[i]);
        }
        return input;
    }
    public static boolean isWithinNextDays(String endDate, int days) {
        long endMillis = dateToMillis(endDate);
        long todayMillis = todayMillis();

        long diff = endMillis - todayMillis;
        long limit = days * 24L * 60 * 60 * 1000;

        return diff > 0 && diff <= limit;
    }

    public static String todayDateString() {
        SimpleDateFormat sdf =
                new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        return sdf.format(new Date());
    }

    public static long dateToMillis(String dateStr) {
        try {
            dateStr = marathiToEnglishDigits(dateStr);
            SimpleDateFormat sdf =
                    new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

            Date date = sdf.parse(dateStr);
            if (date == null) return 0;

            Calendar cal = Calendar.getInstance();
            cal.setTime(date);

            // 🔥 END OF DAY FIX
            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            cal.set(Calendar.SECOND, 59);
            cal.set(Calendar.MILLISECOND, 999);

            return cal.getTimeInMillis();

        } catch (Exception e) {
            return 0;
        }
    }

    public static long todayMillis() {
        return System.currentTimeMillis();
    }
}
