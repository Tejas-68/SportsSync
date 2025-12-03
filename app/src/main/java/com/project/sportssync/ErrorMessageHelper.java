package com.project.sportssync;

import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.firestore.FirebaseFirestoreException;

/**
 * Utility class to convert technical Firebase exceptions into user-friendly messages
 */
public class ErrorMessageHelper {

    /**
     * Get user-friendly error message from any exception
     */
    public static String getUserFriendlyMessage(Exception e) {
        if (e == null) {
            return "An unknown error occurred";
        }

        // Network errors
        if (e instanceof FirebaseNetworkException) {
            return "Network error. Please check your internet connection.";
        }

        // Firestore errors
        if (e instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException firestoreException = (FirebaseFirestoreException) e;
            switch (firestoreException.getCode()) {
                case PERMISSION_DENIED:
                    return "Permission denied. Please contact admin.";
                case UNAVAILABLE:
                    return "Service temporarily unavailable. Please try again.";
                case UNAUTHENTICATED:
                    return "Session expired. Please login again.";
                case NOT_FOUND:
                    return "Requested data not found.";
                case ALREADY_EXISTS:
                    return "This record already exists.";
                case RESOURCE_EXHAUSTED:
                    return "Too many requests. Please wait a moment.";
                case CANCELLED:
                    return "Operation cancelled.";
                case DATA_LOSS:
                    return "Data error. Please contact admin.";
                case DEADLINE_EXCEEDED:
                    return "Request timed out. Please try again.";
                case ABORTED:
                    return "Operation failed. Please try again.";
                default:
                    return "Database error: " + firestoreException.getMessage();
            }
        }

        // Auth errors
        if (e instanceof FirebaseAuthException) {
            FirebaseAuthException authException = (FirebaseAuthException) e;
            String errorCode = authException.getErrorCode();
            
            switch (errorCode) {
                case "ERROR_INVALID_EMAIL":
                    return "Invalid email address format.";
                case "ERROR_WRONG_PASSWORD":
                    return "Incorrect password.";
                case "ERROR_USER_NOT_FOUND":
                    return "No account found with this email.";
                case "ERROR_USER_DISABLED":
                    return "This account has been disabled.";
                case "ERROR_TOO_MANY_REQUESTS":
                    return "Too many failed attempts. Please try again later.";
                case "ERROR_EMAIL_ALREADY_IN_USE":
                    return "An account with this email already exists.";
                case "ERROR_WEAK_PASSWORD":
                    return "Password is too weak. Use at least 6 characters.";
                case "ERROR_NETWORK_REQUEST_FAILED":
                    return "Network error. Please check your connection.";
                default:
                    return "Authentication error: " + authException.getMessage();
            }
        }

        // Generic Firebase errors
        if (e instanceof FirebaseException) {
            return "Service error. Please try again.";
        }

        // Network/IO errors
        String message = e.getMessage();
        if (message != null) {
            String lowerMessage = message.toLowerCase();
            if (lowerMessage.contains("network") || lowerMessage.contains("connection")) {
                return "Network error. Please check your internet connection.";
            }
            if (lowerMessage.contains("timeout")) {
                return "Request timed out. Please try again.";
            }
        }

        // Default fallback
        return "An error occurred. Please try again.";
    }

    /**
     * Get short error message for Snackbar (max 2 lines)
     */
    public static String getShortMessage(Exception e) {
        String fullMessage = getUserFriendlyMessage(e);
        // Truncate if too long
        if (fullMessage.length() > 60) {
            return fullMessage.substring(0, 57) + "...";
        }
        return fullMessage;
    }
}
