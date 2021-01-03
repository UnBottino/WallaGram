package com.wallagram;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.Bundle;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        setupToolbar();
    }

    private void setupToolbar() {
        Toolbar mToolbar = findViewById(R.id.toolbar);
        setTitle("Settings");
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mToolbar.setNavigationIcon(R.drawable.ic_baseline_keyboard_backspace_32);
        mToolbar.setNavigationOnClickListener(v -> finish());
    }
}