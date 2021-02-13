package com.wallagram;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.Log;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.Objects;

public class DurationActivity extends AppCompatActivity {

    private String mDuration = "";
    private int mMetric = -1;
    private SharedPreferences sharedPreferences;

    private TextView bigNum;
    private TextView daysBtn;
    private TextView hoursBtn;
    private RelativeLayout applyBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_duration);

        toolbarSetup();
        initValues();
        metricSetup();

        sharedPreferences = getSharedPreferences("Settings", Context.MODE_PRIVATE);

        initChoices();

        buttonSetup();

    }

    private void toolbarSetup(){
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setTitle("Duration");

        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void initValues(){
        bigNum = findViewById(R.id.bigNum);
        daysBtn = findViewById(R.id.days);
        hoursBtn = findViewById(R.id.hours);
        applyBtn = findViewById(R.id.applyBtn);
    }

    private void metricSetup(){
        daysBtn.setOnClickListener(v -> {
            hoursBtn.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.pitch_black)));
            daysBtn.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.purple)));

            mMetric = 0;

            enableApply();
        });

        hoursBtn.setOnClickListener(v -> {
            hoursBtn.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.purple)));
            daysBtn.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.pitch_black)));

            mMetric = 1;

            enableApply();
        });
    }

    private void enableApply(){
        applyBtn.setAlpha(1);
        applyBtn.setEnabled(true);
    }

    private void disableApply(){
        applyBtn.setAlpha((float)0.5);
        applyBtn.setEnabled(false);
    }

    private void initChoices(){
        int setDuration = sharedPreferences.getInt("duration", 0);
        int setMetric = sharedPreferences.getInt("metric", 0);

        bigNum.setText(String.valueOf(setDuration));

        if(setMetric == 0){
            daysBtn.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.purple)));
        }
        else{
            hoursBtn.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.purple)));
        }

    }

    private void buttonSetup(){
        TextView btn1 = findViewById(R.id.btn1);
        TextView btn2 = findViewById(R.id.btn2);
        TextView btn3 = findViewById(R.id.btn3);
        TextView btn4 = findViewById(R.id.btn4);
        TextView btn5 = findViewById(R.id.btn5);
        TextView btn6 = findViewById(R.id.btn6);
        TextView btn7 = findViewById(R.id.btn7);
        TextView btn8 = findViewById(R.id.btn8);
        TextView btn9 = findViewById(R.id.btn9);
        TextView btn0 = findViewById(R.id.btn0);
        TextView btnClear = findViewById(R.id.btnClear);

        btn1.setOnClickListener(v -> {
            if(mDuration.length() != 3)
                mDuration = mDuration + "1";
            bigNum.setText(mDuration);

            enableApply();
        });

        btn2.setOnClickListener(v -> {
            if(mDuration.length() != 3)
                mDuration = mDuration + "2";
            bigNum.setText(mDuration);

            enableApply();
        });

        btn3.setOnClickListener(v -> {
            if(mDuration.length() != 3)
                mDuration = mDuration + "3";
            bigNum.setText(mDuration);

            enableApply();
        });

        btn4.setOnClickListener(v -> {
            if(mDuration.length() != 3)
                mDuration = mDuration + "4";
            bigNum.setText(mDuration);

            enableApply();
        });

        btn5.setOnClickListener(v -> {
            if(mDuration.length() != 3)
                mDuration = mDuration + "5";
            bigNum.setText(mDuration);

            enableApply();
        });

        btn6.setOnClickListener(v -> {
            if(mDuration.length() != 3)
                mDuration = mDuration + "6";
            bigNum.setText(mDuration);

            enableApply();
        });

        btn7.setOnClickListener(v -> {
            if(mDuration.length() != 3)
                mDuration = mDuration + "7";
            bigNum.setText(mDuration);

            enableApply();
        });

        btn8.setOnClickListener(v -> {
            if(mDuration.length() != 3)
                mDuration = mDuration + "8";
            bigNum.setText(mDuration);

            enableApply();
        });

        btn9.setOnClickListener(v -> {
            if(mDuration.length() != 3)
                mDuration = mDuration + "9";
            bigNum.setText(mDuration);

            enableApply();
        });

        btn0.setOnClickListener(v -> {
            if(applyBtn.isEnabled()) {
                if (mDuration.length() != 3)
                    mDuration = mDuration + "0";
                bigNum.setText(mDuration);
            }
        });

        btnClear.setOnClickListener(v -> {
            mDuration = "";
            bigNum.setText("000");

            disableApply();
        });

        applyBtn.setOnClickListener(v -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();

            if(mMetric != sharedPreferences.getInt("metric", 0) && mMetric != -1) {
                editor.putInt("metric", mMetric);
                Log.i("Duration", "Global metric value updated to: " + mMetric);
            }

            if(!mDuration.equals("")){
                editor.putInt("duration", Integer.parseInt(mDuration));
                Log.i("Duration", "Global duration value updated to: " + mDuration);
            }

            editor.apply();

            finish();
        });
    }
}