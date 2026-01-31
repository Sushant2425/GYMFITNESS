package com.coderspuxelinnnovation.gymmanagementsystem.Utils;

import android.content.Context;
import android.content.SharedPreferences;

public class PrefManager {

    private static final String PREF_NAME = "ServiceControlPrefs";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_LANGUAGE = "app_language";
    private static final String KEY_FIRST_TIME_LAUNCH = "first_time_launch";
    private static final String KEY_GYM_NAME = "gym_name";
    private static final String KEY_OWNER_NAME = "owner_name";
    private static final String KEY_PHONE = "phone";

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
        // Only clear user data, keep language and first launch status
        editor.remove(KEY_EMAIL);
        editor.remove(KEY_GYM_NAME);
        editor.remove(KEY_OWNER_NAME);
        editor.remove(KEY_PHONE);
        editor.commit();
    }

    // ---------- GYM INFO ----------
    public void saveGymName(String gymName) {
        editor.putString(KEY_GYM_NAME, gymName);
        editor.commit();
    }

    public String getGymName() {
        return sharedPreferences.getString(KEY_GYM_NAME, null);
    }

    public void saveOwnerName(String ownerName) {
        editor.putString(KEY_OWNER_NAME, ownerName);
        editor.commit();
    }

    public String getOwnerName() {
        return sharedPreferences.getString(KEY_OWNER_NAME, null);
    }

    public void savePhone(String phone) {
        editor.putString(KEY_PHONE, phone);
        editor.commit();
    }

    public String getPhone() {
        return sharedPreferences.getString(KEY_PHONE, null);
    }

    // ---------- SAVE ALL USER DATA ----------
    public void saveOwnerData(String email, String gymName, String ownerName, String phone) {
        editor.putString(KEY_EMAIL, email);
        editor.putString(KEY_GYM_NAME, gymName);
        editor.putString(KEY_OWNER_NAME, ownerName);
        editor.putString(KEY_PHONE, phone);
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