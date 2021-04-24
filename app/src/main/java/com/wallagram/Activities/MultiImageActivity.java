package com.wallagram.Activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.CheckedTextView;

import com.wallagram.R;
import com.wallagram.Utils.Functions;

import java.util.Objects;

public class MultiImageActivity extends AppCompatActivity {
    private static final String TAG = "MULTI_IMAGE_ACTIVITY";

    private SharedPreferences sharedpreferences;

    private String setChecked;
    private androidx.appcompat.widget.AppCompatButton applyBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multi_image);

        applyBtn = findViewById(R.id.applyBtn);

        toolbarSetup();
        buttonSetup();

        sharedpreferences = getSharedPreferences("Settings", Context.MODE_PRIVATE);
        initCheck();
    }

    private void toolbarSetup() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
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

        btn1.setOnClickListener(v -> {
            uncheckAll();
            btn1.setChecked(true);
            checkChange();
        });

        btn2.setOnClickListener(v -> {
            uncheckAll();
            btn2.setChecked(true);
            checkChange();
        });

        btn3.setOnClickListener(v -> {
            uncheckAll();
            btn3.setChecked(true);
            checkChange();
        });

        btn4.setOnClickListener(v -> {
            uncheckAll();
            btn4.setChecked(true);
            checkChange();
        });

        btn5.setOnClickListener(v -> {
            uncheckAll();
            btn5.setChecked(true);
            checkChange();
        });

        btn6.setOnClickListener(v -> {
            uncheckAll();
            btn6.setChecked(true);
            checkChange();
        });

        btn7.setOnClickListener(v -> {
            uncheckAll();
            btn7.setChecked(true);
            checkChange();
        });

        btn8.setOnClickListener(v -> {
            uncheckAll();
            btn8.setChecked(true);
            checkChange();
        });

        btn9.setOnClickListener(v -> {
            uncheckAll();
            btn9.setChecked(true);
            checkChange();
        });

        btn10.setOnClickListener(v -> {
            uncheckAll();
            btn10.setChecked(true);
            checkChange();
        });

        applyBtn.setOnClickListener(v -> {
            Log.d(TAG, "buttonSetup: Setting multi image pref to: " + findChecked());

            SharedPreferences.Editor editor = sharedpreferences.edit();

            editor.putInt("multiImage", findChecked());
            editor.apply();

            Intent intent = new Intent();
            setResult(222, intent);

            finish();
        });
    }

    private void uncheckAll() {
        ConstraintLayout constraintLayout = findViewById(R.id.btnContainer);

        for (int i = 0; i < constraintLayout.getChildCount(); i++) {
            if (constraintLayout.getChildAt(i) instanceof CheckedTextView) {
                CheckedTextView view = (CheckedTextView) constraintLayout.getChildAt(i);
                view.setChecked(false);
            }
        }
    }

    private void initCheck() {
        setChecked = String.valueOf(sharedpreferences.getInt("multiImage", 1));

        ConstraintLayout constraintLayout = findViewById(R.id.btnContainer);

        for (int i = 0; i < constraintLayout.getChildCount(); i++) {
            if (constraintLayout.getChildAt(i) instanceof CheckedTextView) {
                CheckedTextView view = (CheckedTextView) constraintLayout.getChildAt(i);

                if (view.getText().toString().equalsIgnoreCase(setChecked)) {
                    view.setChecked(true);
                }
            }
        }
    }

    private int findChecked() {
        ConstraintLayout constraintLayout = findViewById(R.id.btnContainer);

        for (int i = 0; i < constraintLayout.getChildCount(); i++) {
            if (constraintLayout.getChildAt(i) instanceof CheckedTextView) {
                CheckedTextView view = (CheckedTextView) constraintLayout.getChildAt(i);

                if (view.isChecked()) {
                    return Integer.parseInt(view.getText().toString());
                }
            }
        }

        return 1;
    }

    private void checkChange() {
        if (setChecked.equalsIgnoreCase(String.valueOf(findChecked()))) {
            Functions.disableApply(applyBtn);
        } else {
            Functions.enableApply(applyBtn);
        }
    }
}