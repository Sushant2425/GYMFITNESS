package com.coderspuxelinnnovation.gymmanagementsystem.Utils;

public class PlanStatusUtils {

    public static boolean isPlanExpired(String endDate) {
        long endMillis = DateUtils.dateToMillis(endDate);
        long todayMillis = DateUtils.todayMillis();
        return endMillis > 0 && todayMillis > endMillis;
    }
}
