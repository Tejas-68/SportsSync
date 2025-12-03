package com.project.sportssync.utils;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.HapticFeedbackConstants;
import android.view.View;

/**
 * Utility class for haptic feedback throughout the app
 * Provides premium tactile feedback for user interactions
 */
public class HapticFeedbackUtil {
    
    /**
     * Light tap feedback for button presses
     */
    public static void lightTap(View view) {
        if (view != null) {
            view.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            );
        }
    }
    
    /**
     * Medium tap feedback for selections
     */
    public static void mediumTap(View view) {
        if (view != null) {
            view.performHapticFeedback(
                HapticFeedbackConstants.KEYBOARD_TAP,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            );
        }
    }
    
    /**
     * Strong tap feedback for confirmations
     */
    public static void strongTap(Context context) {
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(50);
                }
            } catch (Exception e) {
                // Ignore vibration errors (e.g. missing permission) to prevent crash
            }
        }
    }
    
    /**
     * Success feedback for completed actions
     */
    public static void success(Context context) {
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    long[] pattern = {0, 30, 50, 30};
                    int[] amplitudes = {0, 100, 0, 150};
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1));
                } else {
                    long[] pattern = {0, 30, 50, 30};
                    vibrator.vibrate(pattern, -1);
                }
            } catch (Exception e) {
                // Ignore vibration errors
            }
        }
    }
    
    /**
     * Error feedback for failed actions
     */
    public static void error(Context context) {
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    long[] pattern = {0, 50, 50, 50, 50, 50};
                    int[] amplitudes = {0, 200, 0, 200, 0, 200};
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1));
                } else {
                    long[] pattern = {0, 50, 50, 50, 50, 50};
                    vibrator.vibrate(pattern, -1);
                }
            } catch (Exception e) {
                // Ignore vibration errors
            }
        }
    }
    
    /**
     * Subtle feedback for UI interactions like scrolling
     */
    public static void subtle(View view) {
        if (view != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.GESTURE_START);
        }
    }
    
    /**
     * Selection feedback for checkboxes, radio buttons
     */
    public static void selection(View view) {
        if (view != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM);
            } else {
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            }
        }
    }
}
