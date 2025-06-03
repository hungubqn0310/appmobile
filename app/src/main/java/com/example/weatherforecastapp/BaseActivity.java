package com.example.weatherforecastapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.AnimationDrawable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
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
        // Apply colors after background theme
        applyThemeColors();
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

    protected void applyThemeColors() {
        SharedPreferences themePrefs = getSharedPreferences(THEME_PREFS_NAME, Context.MODE_PRIVATE);
        String currentTheme = themePrefs.getString(KEY_BACKGROUND_THEME, THEME_DAY);

        int textColor;
        int iconColor;

        if (THEME_NIGHT.equals(currentTheme)) {
            textColor = Color.WHITE;
            iconColor = Color.WHITE;
        } else {
            textColor = Color.BLACK;
            iconColor = Color.BLACK;
        }

        // Apply to all views
        ViewGroup rootView = (ViewGroup) getWindow().getDecorView().getRootView();
        applyColorsToViews(rootView, textColor, iconColor);
    }

    private void applyColorsToViews(ViewGroup parent, int textColor, int iconColor) {
        SharedPreferences themePrefs = getSharedPreferences(THEME_PREFS_NAME, Context.MODE_PRIVATE);
        String currentTheme = themePrefs.getString(KEY_BACKGROUND_THEME, THEME_DAY);
        boolean isDarkTheme = THEME_NIGHT.equals(currentTheme);

        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);

            // Xử lý custom views implement ThemeAware
            if (child instanceof ThemeAware) {
                ((ThemeAware) child).applyTheme(isDarkTheme);
            } else if (child instanceof Button) {
                Button button = (Button) child;
                if (isDarkTheme) {
                    button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.BLACK));
                    button.setTextColor(Color.WHITE);
                } else {
                    button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.WHITE));
                    button.setTextColor(Color.BLACK);
                }
            } else if (child instanceof TextView) {
                TextView textView = (TextView) child;
                textView.setTextColor(textColor);

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    textView.setCompoundDrawableTintList(android.content.res.ColorStateList.valueOf(iconColor));
                } else {
                    android.graphics.drawable.Drawable[] drawables = textView.getCompoundDrawables();
                    for (android.graphics.drawable.Drawable drawable : drawables) {
                        if (drawable != null) {
                            drawable.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN);
                        }
                    }
                }
            } else if (child instanceof ImageView) {
                ImageView imageView = (ImageView) child;
                if (!isWeatherIcon(imageView)) {
                    imageView.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN);
                }
            } else if (child instanceof ViewGroup) {
                applyColorsToViews((ViewGroup) child, textColor, iconColor);
            }
        }
    }

    // Method to check if ImageView is a weather icon
    private boolean isWeatherIcon(ImageView imageView) {
        // Check by tag
        if (imageView.getTag() != null && "weather_icon".equals(imageView.getTag().toString())) {
            return true;
        }

        // Check by ID (add your weather icon IDs here)
        int id = imageView.getId();
        if (
                id == R.id.ivWeatherIcon
                ) {
            return true;
        }

        // Check if content description contains "weather"
        CharSequence contentDesc = imageView.getContentDescription();
        if (contentDesc != null && contentDesc.toString().toLowerCase().contains("weather")) {
            return true;
        }

        return false;
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

            // Apply colors after updating background
            applyThemeColors();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reapply colors when activity resumes
        if (rootLayout != null) {
            applyThemeColors();
        }
    }
}