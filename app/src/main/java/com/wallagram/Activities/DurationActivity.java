package com.wallagram.Activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.wallagram.R;
import com.wallagram.Utils.Functions;

import java.util.Objects;

public class DurationActivity extends AppCompatActivity {
    private int setDuration;
    private String setMetric;

    private String mDuration = "";
    private String mMetric = "";
    private SharedPreferences sharedPreferences;

    private TextView bigNum;
    private TextView daysBtn;
    private TextView hoursBtn;
    private androidx.appcompat.widget.AppCompatButton applyBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_duration);

        toolbarSetup();

        bigNum = findViewById(R.id.bigNum);
        daysBtn = findViewById(R.id.days);
        hoursBtn = findViewById(R.id.hours);
        applyBtn = findViewById(R.id.applyBtn);

        sharedPreferences = getSharedPreferences("Settings", Context.MODE_PRIVATE);

        initChoices();

        metricSetup();
        buttonSetup();
    }

    private void initChoices() {
        setDuration = sharedPreferences.getInt("duration", 1);
        setMetric = sharedPreferences.getString("metric", "Hours");

        mMetric = setMetric;

        bigNum.setText(String.valueOf(setDuration));

        if (setMetric.equalsIgnoreCase("Hours")) {
            hoursBtn.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.purple)));
        } else {
            daysBtn.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.purple)));
        }
    }

    private void metricSetup() {
        daysBtn.setOnClickListener(v -> {
            hoursBtn.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.pitch_black)));
            daysBtn.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.purple)));

            mMetric = "Days";

            checkChange();
        });

        hoursBtn.setOnClickListener(v -> {
            hoursBtn.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.purple)));
            daysBtn.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.pitch_black)));

            mMetric = "Hours";

            checkChange();
        });
    }

    private void buttonSetup() {
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

        View.OnClickListener buttonClickListener = v -> {
            boolean update = true;

            if (mDuration.length() != 3) {
                if (btn0.equals(v)) {
                    if (applyBtn.isEnabled() || mDuration.equalsIgnoreCase(String.valueOf(setDuration))) {
                        mDuration = mDuration + "0";
                    } else {
                        update = false;
                    }
                } else if (btn1.equals(v)) {
                    mDuration = mDuration + "1";
                } else if (btn2.equals(v)) {
                    mDuration = mDuration + "2";
                } else if (btn3.equals(v)) {
                    mDuration = mDuration + "3";
                } else if (btn4.equals(v)) {
                    mDuration = mDuration + "4";
                } else if (btn5.equals(v)) {
                    mDuration = mDuration + "5";
                } else if (btn6.equals(v)) {
                    mDuration = mDuration + "6";
                } else if (btn7.equals(v)) {
                    mDuration = mDuration + "7";
                } else if (btn8.equals(v)) {
                    mDuration = mDuration + "8";
                } else if (btn9.equals(v)) {
                    mDuration = mDuration + "9";
                }
            }

            if (update) {
                bigNum.setText(mDuration);
                bigNum.setTextColor(ContextCompat.getColor(this, R.color.white));
            }

            checkChange();
        };

        btn0.setOnClickListener(buttonClickListener);
        btn1.setOnClickListener(buttonClickListener);
        btn2.setOnClickListener(buttonClickListener);
        btn3.setOnClickListener(buttonClickListener);
        btn4.setOnClickListener(buttonClickListener);
        btn5.setOnClickListener(buttonClickListener);
        btn6.setOnClickListener(buttonClickListener);
        btn7.setOnClickListener(buttonClickListener);
        btn8.setOnClickListener(buttonClickListener);
        btn9.setOnClickListener(buttonClickListener);

        btnClear.setOnClickListener(v -> {
            mDuration = "";
            bigNum.setText(String.valueOf(setDuration));
            bigNum.setTextColor(ContextCompat.getColor(this, R.color.light_grey));

            Functions.disableApply(applyBtn);
        });

        applyBtn.setOnClickListener(v -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();

            if (!mMetric.equalsIgnoreCase(sharedPreferences.getString("metric", "Hours"))) {
                editor.putString("metric", mMetric);
                Log.d("Duration", "Metric value updated to: " + mMetric);
            }

            if (!mDuration.equals("")) {
                editor.putInt("duration", Integer.parseInt(mDuration));
                Log.d("Duration", "Duration value updated to: " + mDuration);
            }

            editor.apply();

            Functions.callAlarm(getApplicationContext());

            Intent intent = new Intent();
            setResult(111, intent);

            finish();
        });
    }

    private void checkChange() {
        int tempDuration = 0;
        if (!mDuration.equalsIgnoreCase("")) {
            tempDuration = Integer.parseInt(mDuration);
        }

        if ((setDuration == tempDuration && setMetric.equalsIgnoreCase(mMetric)) || tempDuration == 0) {
            Functions.disableApply(applyBtn);
        } else {
            Functions.enableApply(applyBtn);
        }
    }

    private void toolbarSetup() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }
}