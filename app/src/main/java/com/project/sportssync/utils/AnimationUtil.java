package com.project.sportssync.utils;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

/**
 * Utility class for premium animations throughout the app
 * Provides smooth, purposeful animations for enhanced UX
 */
public class AnimationUtil {
    
    private static final int DURATION_SHORT = 200;
    private static final int DURATION_MEDIUM = 300;
    private static final int DURATION_LONG = 400;
    
    /**
     * Fade in animation with slide up
     */
    public static void fadeInSlideUp(View view) {
        view.setAlpha(0f);
        view.setTranslationY(30f);
        view.setVisibility(View.VISIBLE);
        
        view.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(DURATION_MEDIUM)
            .setInterpolator(new DecelerateInterpolator())
            .start();
    }
    
    /**
     * Fade out animation with slide down
     */
    public static void fadeOutSlideDown(View view, final Runnable onComplete) {
        view.animate()
            .alpha(0f)
            .translationY(30f)
            .setDuration(DURATION_MEDIUM)
            .setInterpolator(new AccelerateDecelerateInterpolator())
            .setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    view.setVisibility(View.GONE);
                    if (onComplete != null) {
                        onComplete.run();
                    }
                }
            })
            .start();
    }
    
    /**
     * Scale bounce animation for button press
     */
    public static void scalePress(View view) {
        view.animate()
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(100)
            .setInterpolator(new DecelerateInterpolator())
            .withEndAction(() -> {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .setInterpolator(new OvershootInterpolator())
                    .start();
            })
            .start();
    }
    
    /**
     * Shimmer loading animation
     */
    public static ValueAnimator createShimmerAnimation(View view) {
        ValueAnimator animator = ValueAnimator.ofFloat(0.3f, 1f);
        animator.setDuration(1000);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setRepeatMode(ValueAnimator.REVERSE);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            float alpha = (float) animation.getAnimatedValue();
            view.setAlpha(alpha);
        });
        return animator;
    }
    
    /**
     * Pulse animation for notifications
     */
    public static void pulse(View view) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.1f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.1f, 1f);
        
        scaleX.setDuration(600);
        scaleY.setDuration(600);
        scaleX.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleY.setInterpolator(new AccelerateDecelerateInterpolator());
        
        scaleX.start();
        scaleY.start();
    }
    
    /**
     * Rotate animation for refresh indicators
     */
    public static ObjectAnimator createRotateAnimation(View view) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(view, "rotation", 0f, 360f);
        animator.setDuration(1000);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        return animator;
    }
    
    /**
     * Shake animation for errors
     */
    public static void shake(View view) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(view, "translationX", 0, 25, -25, 25, -25, 15, -15, 6, -6, 0);
        animator.setDuration(500);
        animator.start();
    }
    
    /**
     * Expand animation for cards
     */
    public static void expand(View view) {
        view.setScaleX(0.9f);
        view.setScaleY(0.9f);
        view.setAlpha(0f);
        view.setVisibility(View.VISIBLE);
        
        view.animate()
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setDuration(DURATION_MEDIUM)
            .setInterpolator(new OvershootInterpolator())
            .start();
    }
    
    /**
     * Collapse animation for cards
     */
    public static void collapse(View view, final Runnable onComplete) {
        view.animate()
            .scaleX(0.9f)
            .scaleY(0.9f)
            .alpha(0f)
            .setDuration(DURATION_SHORT)
            .setInterpolator(new AccelerateDecelerateInterpolator())
            .setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    view.setVisibility(View.GONE);
                    if (onComplete != null) {
                        onComplete.run();
                    }
                }
            })
            .start();
    }
}
