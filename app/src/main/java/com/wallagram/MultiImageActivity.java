package com.wallagram;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.CheckedTextView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import java.util.Objects;

public class MultiImageActivity extends AppCompatActivity {

    private SharedPreferences sharedpreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multi_image);

        toolbarSetup();
        buttonSetup();

        sharedpreferences = getSharedPreferences("Settings", Context.MODE_PRIVATE);
        initCheck();
    }

    private void toolbarSetup(){
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setTitle("Multi-Image Posts");

        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void buttonSetup() {
        CheckedTextView btn1 = findViewById(R.id.btn1);
        CheckedTextView btn2 = findViewById(R.id.btn2);
        CheckedTextView btn3 = findViewById(R.id.btn3);
        CheckedTextView btn4 = findViewById(R.id.btn4);
        CheckedTextView btn5 = findViewById(R.id.btn5);
        CheckedTextView btn6 = findViewById(R.id.btn6);
        CheckedTextView btn7 = findViewById(R.id.btn7);
        CheckedTextView btn8 = findViewById(R.id.btn8);
        CheckedTextView btn9 = findViewById(R.id.btn9);
        CheckedTextView btn10 = findViewById(R.id.btn10);

        RelativeLayout applyBtn = findViewById(R.id.applyBtn);

        btn1.setOnClickListener(v -> {
            uncheckAll();
            btn1.setChecked(true);
        });

        btn2.setOnClickListener(v -> {
            uncheckAll();
            btn2.setChecked(true);
        });

        btn3.setOnClickListener(v -> {
            uncheckAll();
            btn3.setChecked(true);
        });

        btn4.setOnClickListener(v -> {
            uncheckAll();
            btn4.setChecked(true);
        });

        btn5.setOnClickListener(v -> {
            uncheckAll();
            btn5.setChecked(true);
        });

        btn6.setOnClickListener(v -> {
            uncheckAll();
            btn6.setChecked(true);
        });

        btn7.setOnClickListener(v -> {
            uncheckAll();
            btn7.setChecked(true);
        });

        btn8.setOnClickListener(v -> {
            uncheckAll();
            btn8.setChecked(true);
        });

        btn9.setOnClickListener(v -> {
            uncheckAll();
            btn9.setChecked(true);
        });

        btn10.setOnClickListener(v -> {
            uncheckAll();
            btn10.setChecked(true);
        });

        applyBtn.setOnClickListener(v -> {
            SharedPreferences.Editor editor = sharedpreferences.edit();

            editor.putInt("multi-image", findChecked() -1);
            editor.apply();

            finish();
        });
    }

    private void uncheckAll() {
        LinearLayout linearLayout = findViewById(R.id.btnContainer);

        for (int i = 0; i < linearLayout.getChildCount(); i++) {
            if (linearLayout.getChildAt(i) instanceof LinearLayout) {
                LinearLayout linearLayout2 = (LinearLayout) linearLayout.getChildAt(i);

                for (int k = 0; k < linearLayout2.getChildCount(); k++) {
                    if (linearLayout2.getChildAt(k) instanceof CheckedTextView) {
                        CheckedTextView view = (CheckedTextView) linearLayout2.getChildAt(k);
                        view.setChecked(false);
                    }
                }
            }
        }
    }

    private void initCheck(){
        String checkedVal = String.valueOf(sharedpreferences.getInt("multi-image", 0) + 1);

        LinearLayout linearLayout = findViewById(R.id.btnContainer);

        for (int i = 0; i < linearLayout.getChildCount(); i++) {
            if (linearLayout.getChildAt(i) instanceof LinearLayout) {
                LinearLayout linearLayout2 = (LinearLayout) linearLayout.getChildAt(i);

                for (int k = 0; k < linearLayout2.getChildCount(); k++) {
                    if (linearLayout2.getChildAt(k) instanceof CheckedTextView) {
                        CheckedTextView view = (CheckedTextView) linearLayout2.getChildAt(k);

                        if(view.getText().toString().equalsIgnoreCase(checkedVal)){
                            view.setChecked(true);
                        }
                    }
                }
            }
        }
    }

    private int findChecked(){
        LinearLayout linearLayout = findViewById(R.id.btnContainer);

        for (int i = 0; i < linearLayout.getChildCount(); i++) {
            if (linearLayout.getChildAt(i) instanceof LinearLayout) {
                LinearLayout linearLayout2 = (LinearLayout) linearLayout.getChildAt(i);

                for (int k = 0; k < linearLayout2.getChildCount(); k++) {
                    if (linearLayout2.getChildAt(k) instanceof CheckedTextView) {
                        CheckedTextView view = (CheckedTextView) linearLayout2.getChildAt(k);

                        if(view.isChecked()){
                            return Integer.parseInt(view.getText().toString());
                        }
                    }
                }
            }
        }

        return -1;
    }
}