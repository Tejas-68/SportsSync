package com.sportssync.app.activities.utils;

import java.util.regex.Pattern;

public class ValidationHelper {

    public static boolean isValidUUCMS(String uucmsId) {
        if (uucmsId == null || uucmsId.isEmpty()) {
            return false;
        }
        Pattern pattern = Pattern.compile("^U11[A-Z]{2}\\d{2}[A-Z]\\d{4}$", Pattern.CASE_INSENSITIVE);
        return pattern.matcher(uucmsId).matches();
    }

    public static boolean isValidName(String name) {
        return name != null && !name.trim().isEmpty() && name.length() >= 3;
    }

    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= 6;
    }
}