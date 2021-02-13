package com.wallagram;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.Objects;

public class LocationActivity extends AppCompatActivity {

    private ImageView homeImg;
    private ImageView lockImg;
    private ImageView bothImg;

    private TextView homeText;
    private TextView lockText;
    private TextView bothText;

    private LinearLayout homeContainer;
    private LinearLayout lockContainer;
    private LinearLayout bothContainer;

    private Drawable homeIcon;
    private Drawable lockIcon;
    private Drawable bothIcon;

    private int mOption = 0;

    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location);

        toolbarSetup();
        initValues();

        sharedPreferences = getSharedPreferences("Settings", Context.MODE_PRIVATE);

        buttonSetup();
        initChecked();
    }

    private void toolbarSetup(){
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setTitle("Location");

        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void initValues(){
        homeImg = findViewById(R.id.homeImg);
        lockImg = findViewById(R.id.lockImg);
        bothImg = findViewById(R.id.bothImg);

        homeText = findViewById(R.id.homeText);
        lockText = findViewById(R.id.lockText);
        bothText = findViewById(R.id.bothText);

        homeContainer = findViewById(R.id.homeContainer);
        lockContainer = findViewById(R.id.lockContainer);
        bothContainer = findViewById(R.id.bothContainer);

        homeIcon = ContextCompat.getDrawable(this, R.drawable.home_screen);
        lockIcon = ContextCompat.getDrawable(this, R.drawable.lock_screen);
        bothIcon = ContextCompat.getDrawable(this, R.drawable.both_screen);
    }

    private void buttonSetup(){
        RelativeLayout applyBtn = findViewById(R.id.applyBtn);

        homeContainer.setOnClickListener(v -> {
            resetIcons();

            homeIcon.setColorFilter(ContextCompat.getColor(this, R.color.purple), PorterDuff.Mode.MULTIPLY);
            homeImg.setImageDrawable(homeIcon);
            homeText.setTextColor(ContextCompat.getColor(this, R.color.purple));

            mOption = 0;
        });

        lockContainer.setOnClickListener(v -> {
            resetIcons();

            lockIcon.setColorFilter(ContextCompat.getColor(this, R.color.purple), PorterDuff.Mode.MULTIPLY);
            lockImg.setImageDrawable(lockIcon);
            lockText.setTextColor(ContextCompat.getColor(this, R.color.purple));

            mOption = 1;
        });

        bothContainer.setOnClickListener(v -> {
            resetIcons();

            bothIcon.setColorFilter(ContextCompat.getColor(this, R.color.purple), PorterDuff.Mode.MULTIPLY);
            bothImg.setImageDrawable(bothIcon);
            bothText.setTextColor(ContextCompat.getColor(this, R.color.purple));

            mOption = 2;
        });

        applyBtn.setOnClickListener(v -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt("location", mOption);
            editor.apply();

            finish();
        });
    }

    private void initChecked(){
        int checkedVal = sharedPreferences.getInt("location", 0);

        resetIcons();

        if(checkedVal == 0){
            homeIcon.setColorFilter(ContextCompat.getColor(this, R.color.purple), PorterDuff.Mode.MULTIPLY);
            homeImg.setImageDrawable(homeIcon);
            homeText.setTextColor(ContextCompat.getColor(this, R.color.purple));
        }
        else if(checkedVal == 1){
            lockIcon.setColorFilter(ContextCompat.getColor(this, R.color.purple), PorterDuff.Mode.MULTIPLY);
            lockImg.setImageDrawable(lockIcon);
            lockText.setTextColor(ContextCompat.getColor(this, R.color.purple));
        }
        else if(checkedVal == 2){
            bothIcon.setColorFilter(ContextCompat.getColor(this, R.color.purple), PorterDuff.Mode.MULTIPLY);
            bothImg.setImageDrawable(bothIcon);
            bothText.setTextColor(ContextCompat.getColor(this, R.color.purple));
        }
    }

    private void resetIcons(){
        homeIcon.setColorFilter(ContextCompat.getColor(this, R.color.white), PorterDuff.Mode.MULTIPLY);
        homeImg.setImageDrawable(homeIcon);
        lockIcon.setColorFilter(ContextCompat.getColor(this, R.color.white), PorterDuff.Mode.MULTIPLY);
        lockImg.setImageDrawable(lockIcon);
        bothIcon.setColorFilter(ContextCompat.getColor(this, R.color.white), PorterDuff.Mode.MULTIPLY);
        bothImg.setImageDrawable(bothIcon);

        homeText.setTextColor(ContextCompat.getColor(this, R.color.white));
        lockText.setTextColor(ContextCompat.getColor(this, R.color.white));
        bothText.setTextColor(ContextCompat.getColor(this, R.color.white));
    }
}