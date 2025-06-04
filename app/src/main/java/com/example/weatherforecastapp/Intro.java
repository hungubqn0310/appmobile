package com.example.weatherforecastapp;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class Intro extends AppCompatActivity {

    ImageView sky;
    ImageView introImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);

        sky = findViewById(R.id.sky);
        introImage = findViewById(R.id.intro_image);

        float screenWidth = getResources().getDisplayMetrics().widthPixels;

        // Animation cho background sky
        ObjectAnimator skyAnimator = ObjectAnimator.ofFloat(sky, "translationX", 0f, -screenWidth);
        skyAnimator.setInterpolator(new LinearInterpolator());
        skyAnimator.setDuration(30000);
        skyAnimator.setRepeatMode(ObjectAnimator.REVERSE);
        skyAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        skyAnimator.start();
        // Animation cho logo intro (di chuyển nhanh hơn background)
        ObjectAnimator introAnimator = ObjectAnimator.ofFloat(introImage, "translationX", 0f, -screenWidth);
        introAnimator.setInterpolator(new LinearInterpolator());
        introAnimator.setDuration(4500); // Nhanh hơn 2 lần so với background
        introAnimator.setRepeatMode(ObjectAnimator.REVERSE);
        introAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        introAnimator.start();

        new Handler().postDelayed(() -> {
            startActivity(new Intent(Intro.this, MainActivity.class));
            finish();
        }, 3000);
    }
}