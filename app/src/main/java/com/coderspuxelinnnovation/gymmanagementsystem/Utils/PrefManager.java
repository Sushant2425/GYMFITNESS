//package com.coderspuxelinnnovation.gymmanagementsystem.Utils;
//
//import android.content.Context;
//import android.content.SharedPreferences;
//
//public class PrefManager {
//
//    private static final String PREF_NAME = "ServiceControlPrefs";
//    private static final String KEY_EMAIL = "email";
//    private static final String KEY_LANGUAGE = "app_language";
//
//    SharedPreferences sharedPreferences;
//    SharedPreferences.Editor editor;
//
//    public PrefManager(Context context) {
//        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
//        editor = sharedPreferences.edit();
//    }
//
//    // ---------- LOGIN ----------
//    public void saveUserEmail(String email) {
//        editor.putString(KEY_EMAIL, email);
//        editor.commit();
//    }
//
//    public String getUserEmail() {
//        return sharedPreferences.getString(KEY_EMAIL, null);
//    }
//
//    public void logout() {
//        editor.clear();
//        editor.commit();
//    }
//
//    // ---------- LANGUAGE ----------
//    public void setLanguage(String languageCode) {
//        editor.putString(KEY_LANGUAGE, languageCode);
//        editor.commit();
//    }
//
//    public String getLanguage() {
//        return sharedPreferences.getString(KEY_LANGUAGE, "en"); // default English
//    }
//}



package com.coderspuxelinnnovation.gymmanagementsystem.Utils;

import android.content.Context;
import android.content.SharedPreferences;

public class PrefManager {

    private static final String PREF_NAME = "ServiceControlPrefs";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_LANGUAGE = "app_language";
    private static final String KEY_FIRST_TIME_LAUNCH = "first_time_launch";

    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;

    public PrefManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    // ---------- LOGIN ----------
    public void saveUserEmail(String email) {
        editor.putString(KEY_EMAIL, email);
        editor.commit();
    }

    public String getUserEmail() {
        return sharedPreferences.getString(KEY_EMAIL, null);
    }

    public void logout() {
        // Only clear email, keep language and first launch status
        editor.remove(KEY_EMAIL);
        editor.commit();
    }

    // ---------- LANGUAGE ----------
    public void setLanguage(String languageCode) {
        editor.putString(KEY_LANGUAGE, languageCode);
        editor.commit();
    }

    public String getLanguage() {
        return sharedPreferences.getString(KEY_LANGUAGE, "en"); // default English
    }

    // ---------- FIRST TIME LAUNCH ----------
    public boolean isFirstTimeLaunch() {
        return sharedPreferences.getBoolean(KEY_FIRST_TIME_LAUNCH, true); // Default true
    }

    public void setFirstTimeLaunch(boolean isFirstTime) {
        editor.putBoolean(KEY_FIRST_TIME_LAUNCH, isFirstTime);
        editor.commit();
    }

    // ---------- CLEAR ALL (for testing) ----------
    public void clearAll() {
        editor.clear();
        editor.commit();
    }
}