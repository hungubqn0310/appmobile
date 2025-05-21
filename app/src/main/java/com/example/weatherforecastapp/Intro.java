package com.example.weatherforecastapp;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class Intro extends AppCompatActivity {

    ImageView sky, sun;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);

        sky = findViewById(R.id.sky);
        sun = findViewById(R.id.sun);

        ObjectAnimator sunRotate = ObjectAnimator.ofFloat(sun, "rotation", 0f, 360f);
        sunRotate.setDuration(6000);
        sunRotate.setInterpolator(new LinearInterpolator());
        sunRotate.setRepeatCount(ObjectAnimator.INFINITE);
        sunRotate.start();

        float screenWidth = getResources().getDisplayMetrics().widthPixels;
        ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(sky, "translationX", 0f, -screenWidth,1);
        objectAnimator.setInterpolator(new LinearInterpolator());
        objectAnimator.setDuration(30000);
        objectAnimator.setRepeatMode(ObjectAnimator.REVERSE);
        objectAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        objectAnimator.start();

        new Handler().postDelayed(() -> {
            startActivity(new Intent(Intro.this, MainActivity.class));
            finish();
        }, 3000);
    }
}