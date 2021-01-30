package com.wallagram;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.widget.TextView;

public class DurationActivity extends AppCompatActivity {

    private String newNumber = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_duration);

        toolbarSetup();
        metricSetup();
        numpadSetup();
    }

    private void toolbarSetup(){
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setTitle("Duration");

        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void metricSetup(){
        TextView daysBtn = findViewById(R.id.days);
        TextView hoursBtn = findViewById(R.id.hours);

        daysBtn.setOnClickListener(v -> {
            hoursBtn.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.pitch_black)));
            daysBtn.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.purple)));
        });

        hoursBtn.setOnClickListener(v -> {
            hoursBtn.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.purple)));
            daysBtn.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.pitch_black)));
        });
    }

    private void numpadSetup(){
        TextView bigNum = findViewById(R.id.bigNum);

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
            if(newNumber.length() != 3)
                newNumber = newNumber + "1";

            bigNum.setText(newNumber);
        });

        btn2.setOnClickListener(v -> {
            if(newNumber.length() != 3)
                newNumber = newNumber + "2";
            bigNum.setText(newNumber);
        });

        btn3.setOnClickListener(v -> {
            if(newNumber.length() != 3)
                newNumber = newNumber + "3";
            bigNum.setText(newNumber);
        });

        btn4.setOnClickListener(v -> {
            if(newNumber.length() != 3)
                newNumber = newNumber + "4";
            bigNum.setText(newNumber);
        });

        btn5.setOnClickListener(v -> {
            if(newNumber.length() != 3)
                newNumber = newNumber + "5";
            bigNum.setText(newNumber);
        });

        btn6.setOnClickListener(v -> {
            if(newNumber.length() != 3)
                newNumber = newNumber + "6";
            bigNum.setText(newNumber);
        });

        btn7.setOnClickListener(v -> {
            if(newNumber.length() != 3)
                newNumber = newNumber + "7";
            bigNum.setText(newNumber);
        });

        btn8.setOnClickListener(v -> {
            if(newNumber.length() != 3)
                newNumber = newNumber + "8";
            bigNum.setText(newNumber);
        });

        btn9.setOnClickListener(v -> {
            if(newNumber.length() != 3)
                newNumber = newNumber + "9";
            bigNum.setText(newNumber);
        });

        btn0.setOnClickListener(v -> {
            if(newNumber.length() != 3)
                newNumber = newNumber +"0";
            bigNum.setText(newNumber);
        });

        btnClear.setOnClickListener(v -> {
            System.out.println(newNumber);
            newNumber = "";
            bigNum.setText("1");
        });
    }
}