package com.project.sportssync;

import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class for secure password hashing and verification.
 * Uses SHA-256 hashing algorithm for password security.
 */
public class PasswordUtils {
    
    private static final String TAG = "PasswordUtils";
    private static final String HASH_ALGORITHM = "SHA-256";
    
    /**
     * Hash a password using SHA-256 algorithm.
     * 
     * @param password Plain text password to hash
     * @return Hexadecimal string representation of the hashed password, or null if hashing fails
     */
    public static String hashPassword(String password) {
        if (password == null || password.isEmpty()) {
            Log.w(TAG, "Cannot hash null or empty password");
            return null;
        }
        
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hashBytes = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            
            // Convert byte array to hexadecimal string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "SHA-256 algorithm not available", e);
            return null;
        }
    }
    
    /**
     * Verify a password against a stored hash.
     * 
     * @param password Plain text password to verify
     * @param storedHash Stored hash to compare against
     * @return true if password matches the hash, false otherwise
     */
    public static boolean verifyPassword(String password, String storedHash) {
        if (password == null || storedHash == null) {
            Log.w(TAG, "Cannot verify null password or hash");
            return false;
        }
        
        String hashedInput = hashPassword(password);
        if (hashedInput == null) {
            return false;
        }
        
        return hashedInput.equals(storedHash);
    }
    
    /**
     * Check if a string appears to be a SHA-256 hash (64 hexadecimal characters).
     * This is useful for backward compatibility to detect if a password is already hashed.
     * 
     * @param value String to check
     * @return true if the string looks like a SHA-256 hash
     */
    public static boolean isHashed(String value) {
        if (value == null) {
            return false;
        }
        
        // SHA-256 produces 64 hexadecimal characters
        return value.length() == 64 && value.matches("[0-9a-f]{64}");
    }
}
