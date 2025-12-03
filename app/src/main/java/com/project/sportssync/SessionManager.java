package com.project.sportssync;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF_NAME = "UserSession";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_UUCMS = "uucms";
    private static final String KEY_ROLE = "role";
    private static final String KEY_NAME = "name";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";

    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    public void createLoginSession(String userId, String uucms, String role, String name) {
        editor.putString(KEY_USER_ID, userId);
        editor.putString(KEY_UUCMS, uucms);
        editor.putString(KEY_ROLE, role);
        editor.putString(KEY_NAME, name);
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.apply();
    }

    // Overload for admin (no name needed)
    public void createLoginSession(String userId, String uucms, String role) {
        createLoginSession(userId, uucms, role, "");
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public String getUserId() {
        return prefs.getString(KEY_USER_ID, null);
    }

    public String getUucms() {
        return prefs.getString(KEY_UUCMS, null);
    }

    public String getRole() {
        return prefs.getString(KEY_ROLE, null);
    }

    public String getName() {
        return prefs.getString(KEY_NAME, "");
    }

    public void logout() {
        editor.clear();
        editor.apply();
    }
}
