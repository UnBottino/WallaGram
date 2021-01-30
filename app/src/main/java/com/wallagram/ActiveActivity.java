package com.wallagram;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class ActiveActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_state);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setTitle("Active");

        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        RelativeLayout onBtn = findViewById(R.id.onBtn);
        RelativeLayout offBtn = findViewById(R.id.offBtn);
        TextView onText = findViewById(R.id.onText);
        TextView offText = findViewById(R.id.offText);

        onBtn.setOnClickListener(v -> {
            onBtn.setEnabled(false);

            Animation animation = AnimationUtils.loadAnimation(ActiveActivity.this, R.anim.blink);
            onBtn.startAnimation(animation);

            Handler handler = new Handler();
            handler.postDelayed(() -> {
                onBtn.setVisibility(View.INVISIBLE);
                onText.setVisibility(View.INVISIBLE);
                offBtn.setVisibility(View.VISIBLE);
                offText.setVisibility(View.VISIBLE);

                offBtn.setEnabled(true);
            }, 1200);
        });

        offBtn.setOnClickListener(v -> {
            offBtn.setEnabled(false);

            Animation animation = AnimationUtils.loadAnimation(ActiveActivity.this, R.anim.blink);
            offBtn.startAnimation(animation);

            Handler handler = new Handler();
            handler.postDelayed(() -> {
                offBtn.setVisibility(View.INVISIBLE);
                offText.setVisibility(View.INVISIBLE);
                onBtn.setVisibility(View.VISIBLE);
                onText.setVisibility(View.VISIBLE);

                onBtn.setEnabled(true);
            }, 1200);
        });
    }
}