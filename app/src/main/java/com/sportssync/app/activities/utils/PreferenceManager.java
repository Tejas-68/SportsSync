package com.sportssync.app.activities.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferenceManager {
    private static final String PREF_NAME = "SportsSync";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USER_TYPE = "userType";
    private static final String KEY_USER_NAME = "userName";
    private static final String KEY_UUCMS_ID = "uucmsId";
    private static final String KEY_IS_REGISTERED = "isRegistered";

    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;

    public PreferenceManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    public void saveUserData(String userId, String userType, String userName, String uucmsId) {
        editor.putString(KEY_USER_ID, userId);
        editor.putString(KEY_USER_TYPE, userType);
        editor.putString(KEY_USER_NAME, userName);
        editor.putString(KEY_UUCMS_ID, uucmsId);
        editor.apply();
    }

    public void setRegistered(boolean registered) {
        editor.putBoolean(KEY_IS_REGISTERED, registered);
        editor.apply();
    }

    public String getUserId() {
        return prefs.getString(KEY_USER_ID, null);
    }

    public String getUserType() {
        return prefs.getString(KEY_USER_TYPE, null);
    }

    public String getUserName() {
        return prefs.getString(KEY_USER_NAME, null);
    }

    public String getUucmsId() {
        return prefs.getString(KEY_UUCMS_ID, null);
    }

    public boolean isRegistered() {
        return prefs.getBoolean(KEY_IS_REGISTERED, false);
    }

    public void clearData() {
        editor.clear();
        editor.apply();
    }
}