package com.wallagram.Activities;

import androidx.annotation.ColorInt;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

import com.wallagram.R;
import com.wallagram.Utils.Functions;

public class DurationActivity extends AppCompatActivity {
    private final String TAG = "DURATION_ACTIVITY";

    private int setDuration;
    private String setMetric;

    private String mDuration = "";
    private String mMetric = "";
    private SharedPreferences sharedPreferences;

    private TextView bigNum;
    private TextView daysBtn;
    private TextView hoursBtn;
    private androidx.appcompat.widget.AppCompatButton applyBtn;

    private @ColorInt
    int colorSurface;
    private @ColorInt
    int colorOnSurface;
    private @ColorInt
    int colorPrimary;
    private @ColorInt
    int colorOnPrimary;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_duration);

        toolbarSetup();
        colorSetup();

        bigNum = findViewById(R.id.bigNum);
        daysBtn = findViewById(R.id.days);
        hoursBtn = findViewById(R.id.hours);
        applyBtn = findViewById(R.id.applyBtn);

        sharedPreferences = getSharedPreferences("Settings", Context.MODE_PRIVATE);

        initChoices();

        metricSetup();
        buttonSetup();
    }

    private void colorSetup() {
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = getTheme();
        theme.resolveAttribute(R.attr.colorPrimary, typedValue, true);
        colorPrimary = typedValue.data;
        theme.resolveAttribute(R.attr.colorOnPrimary, typedValue, true);
        colorOnPrimary = typedValue.data;
        theme.resolveAttribute(R.attr.colorSurface, typedValue, true);
        colorSurface = typedValue.data;
        theme.resolveAttribute(R.attr.colorOnSurface, typedValue, true);
        colorOnSurface = typedValue.data;
    }

    private void initChoices() {
        setDuration = sharedPreferences.getInt("duration", 1);
        setMetric = sharedPreferences.getString("metric", "Hours");

        mMetric = setMetric;

        bigNum.setText(String.valueOf(setDuration));

        if (setMetric.equalsIgnoreCase("Hours")) {
            hoursBtn.setBackgroundTintList(ColorStateList.valueOf(colorPrimary));
            hoursBtn.setTextColor(colorOnPrimary);
        } else {
            daysBtn.setBackgroundTintList(ColorStateList.valueOf(colorPrimary));
            daysBtn.setTextColor(colorOnPrimary);
        }
    }

    private void metricSetup() {
        daysBtn.setOnClickListener(v -> {
            hoursBtn.setBackgroundTintList(ColorStateList.valueOf(colorSurface));
            hoursBtn.setTextColor(colorOnSurface);
            daysBtn.setBackgroundTintList(ColorStateList.valueOf(colorPrimary));
            daysBtn.setTextColor(colorOnPrimary);

            mMetric = "Days";

            checkChange();
        });

        hoursBtn.setOnClickListener(v -> {
            hoursBtn.setBackgroundTintList(ColorStateList.valueOf(colorPrimary));
            hoursBtn.setTextColor(colorOnPrimary);
            daysBtn.setBackgroundTintList(ColorStateList.valueOf(colorSurface));
            daysBtn.setTextColor(colorOnSurface);

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
                bigNum.setAlpha((float) 1);
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
            bigNum.setAlpha((float) 0.5);

            checkChange();
        });

        applyBtn.setOnClickListener(v -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();

            if (!mMetric.equalsIgnoreCase(sharedPreferences.getString("metric", "Hours"))) {
                editor.putString("metric", mMetric);
                Log.d(TAG, "buttonSetup: Metric value updated to: " + mMetric);
            }

            if (!mDuration.equals("")) {
                editor.putInt("duration", Integer.parseInt(mDuration));
                Log.d(TAG, "buttonSetup: Duration value updated to: " + mDuration);
            }

            editor.apply();

            if (sharedPreferences.getInt("state", 1) == 1 && !sharedPreferences.getString("searchName", "").equalsIgnoreCase("")) {
                Functions.findNewPostPeriodicRequest(getApplicationContext());
            }

            Intent intent = new Intent();
            setResult(111, intent);

            finish();
        });
    }

    private void checkChange() {
        int convertedDuration = 0;
        if (!mDuration.equalsIgnoreCase("")) {
            convertedDuration = Integer.parseInt(mDuration);
        }

        if (!setMetric.equalsIgnoreCase(mMetric) || (setDuration != convertedDuration && convertedDuration != 0)) {
            Functions.enableApply(applyBtn);
        } else {
            Functions.disableApply(applyBtn);
        }
    }

    private void toolbarSetup() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }
}