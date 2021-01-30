package com.wallagram;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class LocationActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setTitle("Location");

        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        LinearLayout homeContainer = findViewById(R.id.homeContainer);
        LinearLayout lockContainer = findViewById(R.id.lockContainer);
        LinearLayout bothContainer = findViewById(R.id.bothContainer);

        ImageView homeImg = findViewById(R.id.homeImg);
        ImageView lockImg = findViewById(R.id.lockImg);
        ImageView bothImg = findViewById(R.id.bothImg);
        TextView homeText = findViewById(R.id.homeText);
        TextView lockText = findViewById(R.id.lockText);
        TextView bothText = findViewById(R.id.bothText);

        Drawable homeIcon= ContextCompat.getDrawable(this, R.drawable.home_screen);
        Drawable lockIcon= ContextCompat.getDrawable(this, R.drawable.lock_screen);
        Drawable bothIcon= ContextCompat.getDrawable(this, R.drawable.both_screen);

        homeContainer.setOnClickListener(v -> {
            homeIcon.setColorFilter(ContextCompat.getColor(this, R.color.purple), PorterDuff.Mode.MULTIPLY);
            homeImg.setImageDrawable(homeIcon);

            lockIcon.setColorFilter(ContextCompat.getColor(this, R.color.white), PorterDuff.Mode.MULTIPLY);
            lockImg.setImageDrawable(lockIcon);
            bothIcon.setColorFilter(ContextCompat.getColor(this, R.color.white), PorterDuff.Mode.MULTIPLY);
            bothImg.setImageDrawable(bothIcon);

            homeText.setTextColor(ContextCompat.getColor(this, R.color.purple));
            lockText.setTextColor(ContextCompat.getColor(this, R.color.white));
            bothText.setTextColor(ContextCompat.getColor(this, R.color.white));
        });

        lockContainer.setOnClickListener(v -> {
            lockIcon.setColorFilter(ContextCompat.getColor(this, R.color.purple), PorterDuff.Mode.MULTIPLY);
            lockImg.setImageDrawable(lockIcon);

            homeIcon.setColorFilter(ContextCompat.getColor(this, R.color.white), PorterDuff.Mode.MULTIPLY);
            homeImg.setImageDrawable(homeIcon);
            bothIcon.setColorFilter(ContextCompat.getColor(this, R.color.white), PorterDuff.Mode.MULTIPLY);
            bothImg.setImageDrawable(bothIcon);

            homeText.setTextColor(ContextCompat.getColor(this, R.color.white));
            lockText.setTextColor(ContextCompat.getColor(this, R.color.purple));
            bothText.setTextColor(ContextCompat.getColor(this, R.color.white));
        });

        bothContainer.setOnClickListener(v -> {
            bothIcon.setColorFilter(ContextCompat.getColor(this, R.color.purple), PorterDuff.Mode.MULTIPLY);
            bothImg.setImageDrawable(bothIcon);

            homeIcon.setColorFilter(ContextCompat.getColor(this, R.color.white), PorterDuff.Mode.MULTIPLY);
            homeImg.setImageDrawable(homeIcon);
            lockIcon.setColorFilter(ContextCompat.getColor(this, R.color.white), PorterDuff.Mode.MULTIPLY);
            lockImg.setImageDrawable(lockIcon);

            homeText.setTextColor(ContextCompat.getColor(this, R.color.white));
            lockText.setTextColor(ContextCompat.getColor(this, R.color.white));
            bothText.setTextColor(ContextCompat.getColor(this, R.color.purple));
        });
    }
}