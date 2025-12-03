package com.project.sportssync;

import android.text.TextUtils;
import java.util.regex.Pattern;

/**
 * Centralized validation utility class for input validation across the app
 */
public class ValidationUtils {

    // UUCMS format: U + 2 digits + 2 letters + 2 digits + 1 letter + 4 digits (e.g., U11SZ23S0189)
    private static final Pattern UUCMS_PATTERN = Pattern.compile("^U\\d{2}[A-Z]{2}\\d{2}[A-Z]\\d{4}$");
    
    // Name: Only letters, spaces, and hyphens
    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z\\s-]+$");
    
    // Password: Minimum 6 characters, at least 1 letter and 1 number
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).{6,}$");

    /**
     * Validates UUCMS ID format
     * @param uucms The UUCMS ID to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidUUCMS(String uucms) {
        if (TextUtils.isEmpty(uucms)) {
            return false;
        }
        return UUCMS_PATTERN.matcher(uucms.trim()).matches();
    }

    /**
     * Validates password strength
     * Requires: minimum 6 characters, at least 1 letter and 1 number
     * @param password The password to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidPassword(String password) {
        if (TextUtils.isEmpty(password)) {
            return false;
        }
        return PASSWORD_PATTERN.matcher(password).matches();
    }

    /**
     * Validates name format (only letters, spaces, and hyphens)
     * @param name The name to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidName(String name) {
        if (TextUtils.isEmpty(name)) {
            return false;
        }
        String trimmed = name.trim();
        return trimmed.length() >= 2 && NAME_PATTERN.matcher(trimmed).matches();
    }

    /**
     * Validates quantity is within acceptable range
     * @param quantity The quantity to validate
     * @param max Maximum allowed quantity
     * @return true if valid, false otherwise
     */
    public static boolean isValidQuantity(int quantity, int max) {
        return quantity > 0 && quantity <= max;
    }

    /**
     * Sanitizes input by trimming and removing extra spaces
     * @param input The input to sanitize
     * @return Sanitized string
     */
    public static String sanitizeInput(String input) {
        if (TextUtils.isEmpty(input)) {
            return "";
        }
        // Trim and replace multiple spaces with single space
        return input.trim().replaceAll("\\s+", " ");
    }

    /**
     * Gets user-friendly error message for UUCMS validation
     * @return Error message
     */
    public static String getUUCMSErrorMessage() {
        return "UUCMS ID must be in uppercase format: U11SZ23S0189";
    }

    /**
     * Gets user-friendly error message for password validation
     * @return Error message
     */
    public static String getPasswordErrorMessage() {
        return "Password must be at least 6 characters with 1 letter and 1 number";
    }

    /**
     * Gets user-friendly error message for name validation
     * @return Error message
     */
    public static String getNameErrorMessage() {
        return "Name must be at least 2 characters (letters only)";
    }

    /**
     * Gets user-friendly error message for quantity validation
     * @param max Maximum allowed quantity
     * @return Error message
     */
    public static String getQuantityErrorMessage(int max) {
        return "Quantity must be between 1 and " + max;
    }

    /**
     * Validates email format
     * @param email The email to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidEmail(String email) {
        if (TextUtils.isEmpty(email)) {
            return false;
        }
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    /**
     * Validates admin ID format (alphanumeric, 4-20 characters)
     * @param adminId The admin ID to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidAdminId(String adminId) {
        if (TextUtils.isEmpty(adminId)) {
            return false;
        }
        String trimmed = adminId.trim();
        return trimmed.length() >= 4 && trimmed.length() <= 20 && trimmed.matches("^[a-zA-Z0-9]+$");
    }
}
