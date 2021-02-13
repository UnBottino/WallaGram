package com.wallagram;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.Objects;

public class StateActivity extends AppCompatActivity {

    private RelativeLayout onBtn;
    private RelativeLayout offBtn;
    private TextView onText;
    private TextView offText;

    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_state);

        toolbarSetup();

        sharedPreferences = getSharedPreferences("Settings", Context.MODE_PRIVATE);

        initState();
        buttonSetup();

    }

    private void toolbarSetup(){
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setTitle("State");

        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void initState(){
        onBtn = findViewById(R.id.onBtn);
        offBtn = findViewById(R.id.offBtn);
        onText = findViewById(R.id.onText);
        offText = findViewById(R.id.offText);

        int setState = sharedPreferences.getInt("state", 1);

        if(setState == 0){
            offBtn.setVisibility(View.VISIBLE);
            offText.setVisibility(View.VISIBLE);
            onBtn.setVisibility(View.INVISIBLE);
            onText.setVisibility(View.INVISIBLE);
        }
    }

    private void buttonSetup(){
        Animation btnBlink = AnimationUtils.loadAnimation(StateActivity.this, R.anim.blink);
        Animation textFadeIn = AnimationUtils.loadAnimation(StateActivity.this, R.anim.fade_in);
        Animation textFadeOut = AnimationUtils.loadAnimation(StateActivity.this, R.anim.fade_out);

        onBtn.setOnClickListener(v -> {
            onBtn.setEnabled(false);

            onBtn.startAnimation(btnBlink);
            onText.startAnimation(textFadeOut);
            offText.startAnimation(textFadeIn);

            Handler handler = new Handler();
            handler.postDelayed(() -> {
                onBtn.setVisibility(View.INVISIBLE);
                onText.setVisibility(View.INVISIBLE);
                offBtn.setVisibility(View.VISIBLE);
                offText.setVisibility(View.VISIBLE);

                offBtn.setEnabled(true);
            }, 1000);

            //Set displayed profile pic
            Picasso.get()
                    .load(R.drawable.frown_straight)
                    .into(MainActivity.mSetProfilePic);

            MainActivity.mSetAccountName.setText("State disabled");

            SharedPreferences.Editor editor = sharedPreferences.edit();

            editor.putInt("state", 0);
            Log.i("State", "Global state value updated to: Off");

            editor.apply();
        });

        offBtn.setOnClickListener(v -> {
            offBtn.setEnabled(false);

            offBtn.startAnimation(btnBlink);
            offText.startAnimation(textFadeOut);
            onText.startAnimation(textFadeIn);

            Handler handler = new Handler();
            handler.postDelayed(() -> {
                offBtn.setVisibility(View.INVISIBLE);
                offText.setVisibility(View.INVISIBLE);
                onBtn.setVisibility(View.VISIBLE);
                onText.setVisibility(View.VISIBLE);

                onBtn.setEnabled(true);
            }, 1000);

            SharedPreferences.Editor editor = sharedPreferences.edit();

            editor.putInt("state", 1);
            Log.i("State", "Global state value updated to: On");

            editor.apply();
        });
    }
}