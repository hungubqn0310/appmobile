package com.example.weatherforecastapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.AnimationDrawable;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

public class BaseActivity extends AppCompatActivity {
    // Theme constants
    protected static final String THEME_PREFS_NAME = "ThemePrefs";
    protected static final String KEY_BACKGROUND_THEME = "backgroundTheme";
    protected static final String THEME_DAY = "day";
    protected static final String THEME_NIGHT = "night";

    protected ConstraintLayout rootLayout;

    protected void initializeBackground() {
        rootLayout = findViewById(R.id.rootLayout);
        applyBackgroundTheme();
    }

    protected void applyBackgroundTheme() {
        if (rootLayout == null) {
            Log.e(getClass().getSimpleName(), "Root layout not found!");
            return;
        }

        SharedPreferences themePrefs = getSharedPreferences(THEME_PREFS_NAME, Context.MODE_PRIVATE);
        String currentTheme = themePrefs.getString(KEY_BACKGROUND_THEME, THEME_DAY);

        if (THEME_NIGHT.equals(currentTheme)) {
            rootLayout.setBackgroundResource(R.drawable.animated_background_night);
        } else {
            rootLayout.setBackgroundResource(R.drawable.animated_background_day);
        }

        // Start animation
        AnimationDrawable animationDrawable = (AnimationDrawable) rootLayout.getBackground();
        if (animationDrawable != null) {
            animationDrawable.setEnterFadeDuration(6000);
            animationDrawable.setExitFadeDuration(6000);
            animationDrawable.start();
        }

        Log.d(getClass().getSimpleName(), "Applied theme: " + currentTheme);
    }

    protected void saveBackgroundTheme(String theme) {
        SharedPreferences themePrefs = getSharedPreferences(THEME_PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = themePrefs.edit();
        editor.putString(KEY_BACKGROUND_THEME, theme);
        editor.apply();
        Log.d(getClass().getSimpleName(), "Saved theme: " + theme);
    }

    protected void updateBackground(String localtime) {
        String[] parts = localtime.split(" ");
        if (parts.length == 2) {
            String timePart = parts[1];
            String[] timeSplit = timePart.split(":");
            int hour = Integer.parseInt(timeSplit[0]);

            if (hour >= 6 && hour < 18) {
                // Day theme
                rootLayout.setBackgroundResource(R.drawable.animated_background_day);
                saveBackgroundTheme(THEME_DAY);
            } else {
                // Night theme
                rootLayout.setBackgroundResource(R.drawable.animated_background_night);
                saveBackgroundTheme(THEME_NIGHT);
            }

            // Start animation
            AnimationDrawable animationDrawable = (AnimationDrawable) rootLayout.getBackground();
            if (animationDrawable != null) {
                animationDrawable.setEnterFadeDuration(6000);
                animationDrawable.setExitFadeDuration(6000);
                animationDrawable.start();
            }
        }
    }
}