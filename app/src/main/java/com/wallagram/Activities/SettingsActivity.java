package com.wallagram.Activities;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Layout;
import android.text.SpannableString;
import android.text.style.AlignmentSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.wallagram.R;
import com.wallagram.Utils.Functions;

import java.util.Objects;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class SettingsActivity extends AppCompatActivity {
    private static final String TAG = "SETTINGS_ACTIVITY";

    private int stateChange = -1;
    private boolean clearRecentChange = false;

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        sharedPreferences = getSharedPreferences("Settings", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
        editor.apply();

        toolbarSetup();

        buttonSetup();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 800) {
            switch (resultCode) {
                case 111:
                    Log.d(TAG, "onActivityResult: Duration update returned");
                    durationBtnSetup();
                    break;
                case 222:
                    Log.d(TAG, "onActivityResult: Multi-Image update returned");
                    multiPostBtnSetup();
                    break;
            }
        }
    }

    private void stateBtnSetup() {
        SwitchCompat state = findViewById(R.id.state);

        //init value
        if (sharedPreferences.getInt("state", 1) == 0) {
            state.setChecked(false);
        }

        state.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            if (isChecked) {
                stateChange = 1;

                editor.putInt("state", 1);
                editor.apply();

                Log.d(TAG, "stateBtnSetup: State value updated to: On");
                if (!sharedPreferences.getString("searchName", "").equalsIgnoreCase("")) {
                    Functions.callAlarm(getApplicationContext());
                }

                Intent intent = new Intent();
                if (clearRecentChange) {
                    setResult(110, intent);
                } else {
                    setResult(10, intent);
                }
            } else {
                stateChange = 0;

                editor.putInt("state", 0);
                editor.apply();

                Log.d(TAG, "stateBtnSetup: State value updated to: Off");
                Functions.cancelAlarm(getApplicationContext());

                Intent intent = new Intent();
                if (clearRecentChange) {
                    setResult(111, intent);
                } else {
                    setResult(11, intent);
                }
            }
        });
    }

    private void durationBtnSetup() {
        RelativeLayout duration = findViewById(R.id.duration);

        //init value
        int setDuration = sharedPreferences.getInt("duration", 1);
        String setMetric = sharedPreferences.getString("metric", "Hours");

        TextView durationValue = findViewById(R.id.durationValue);

        if (setDuration == 1) {
            setMetric = setMetric.substring(0, setMetric.length() - 1);
        }

        String displayText = setDuration + " " + setMetric;

        durationValue.setText(displayText);

        duration.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, DurationActivity.class);
            startActivityForResult(intent, 800);
        });
    }

    private void locationBtnSetup() {
        RelativeLayout location = findViewById(R.id.location);
        TextView locationValue = findViewById(R.id.locationValue);

        //init value
        switch (sharedPreferences.getInt("location", 0)) {
            case 0:
                locationValue.setText(R.string.home_screen);
                break;
            case 1:
                locationValue.setText(R.string.lock_screen);
                break;
            case 2:
                locationValue.setText(R.string.both_screens);
                break;
        }

        location.setOnClickListener(v -> {
            editor.putBoolean("settingsUpdated", true);

            switch (locationValue.getText().toString()) {
                case "Home Screen":
                    Log.d(TAG, "locationBtnSetup: Location set to 'Lock'");
                    locationValue.setText(R.string.lock_screen);
                    editor.putInt("location", 1);
                    editor.apply();
                    break;
                case "Lock Screen":
                    Log.d(TAG, "locationBtnSetup: Location set to 'Both'");
                    locationValue.setText(R.string.both_screens);
                    editor.putInt("location", 2);
                    editor.apply();
                    break;
                case "Both":
                    Log.d(TAG, "locationBtnSetup: Location set to 'Home'");
                    locationValue.setText(R.string.home_screen);
                    editor.putInt("location", 0);
                    editor.apply();
                    break;
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void multiPostBtnSetup() {
        RelativeLayout multiPost = findViewById(R.id.multiImage);

        //init value
        int setMultiImage = sharedPreferences.getInt("multiImage", 1);
        TextView multiImageValue = findViewById(R.id.multiImageValue);

        switch (setMultiImage) {
            case 1:
                multiImageValue.setText(setMultiImage + "st");
                break;
            case 2:
                multiImageValue.setText(setMultiImage + "nd");
                break;
            case 3:
                multiImageValue.setText(setMultiImage + "rd");
                break;
            default:
                multiImageValue.setText(setMultiImage + "th");
                break;
        }

        multiPost.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, MultiImageActivity.class);
            startActivityForResult(intent, 800);
        });
    }

    private void saveWallpaperBtnSetup() {
        SwitchCompat saveWallpaper = findViewById(R.id.saveWallpaper);

        //init value
        if (sharedPreferences.getInt("saveWallpaper", 0) == 0) {
            saveWallpaper.setChecked(false);
        }

        saveWallpaper.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            //Request Storage Access
            Functions.requestPermission(this);

            if (ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                saveWallpaper.setChecked(false);
            } else {
                if (isChecked) {
                    editor.putInt("saveWallpaper", 1);
                    editor.apply();
                    Log.d(TAG, "saveWallpaperBtnSetup: Save Wallpaper value updated to: Yes");
                } else {
                    editor.putInt("saveWallpaper", 0);
                    editor.apply();
                    Log.d(TAG, "saveWallpaperBtnSetup: Save Wallpaper value updated to: No");
                }
            }
        });
    }

    private void imageAlignBtnSetup() {
        RelativeLayout imageAlign = findViewById(R.id.imageAlign);
        TextView alignValue = findViewById(R.id.alignValue);

        //init value
        switch (sharedPreferences.getInt("align", 1)) {
            case 0:
                alignValue.setText(R.string.left_align);
                break;
            case 1:
                alignValue.setText(R.string.centre_align);
                break;
            case 2:
                alignValue.setText(R.string.right_align);
                break;
        }

        imageAlign.setOnClickListener(v -> {
            editor.putBoolean("settingsUpdated", true);

            switch (alignValue.getText().toString()) {
                case "Left":
                    Log.d(TAG, "imageAlignBtnSetup: ImageAlign set to 'Centre'");
                    alignValue.setText(R.string.centre_align);
                    editor.putInt("align", 1);
                    editor.apply();
                    break;
                case "Centre":
                    Log.d(TAG, "imageAlignBtnSetup: ImageAlign set to 'Right'");
                    alignValue.setText(R.string.right_align);
                    editor.putInt("align", 2);
                    editor.apply();
                    break;
                case "Right":
                    Log.d(TAG, "imageAlignBtnSetup: ImageAlign set to 'Left'");
                    alignValue.setText(R.string.left_align);
                    editor.putInt("align", 0);
                    editor.apply();
                    break;
                default:
                    Log.e(TAG, "imageAlignBtnSetup: Image align error");
                    break;
            }
        });
    }

    private void clearRecentBtnSetup() {
        RelativeLayout clearRecent = findViewById(R.id.clearRecentSearches);

        clearRecent.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogCustom);
            builder.setCancelable(true);
            builder.setTitle("Clear Recent Searches");
            builder.setMessage("Are you sure?");
            builder.setPositiveButton("Confirm", (dialog, which) -> {
                clearRecentChange = true;

                Functions.removeDBAccounts(this);

                /*mAdapter.notifyItemRangeRemoved(0, mDBAccountList.size());
                mAdapter.notifyDataSetChanged();

                mDBAccountList.clear();*/

                Intent intent = new Intent();
                if (stateChange == 1) {
                    setResult(110, intent);
                } else if (stateChange == 0) {
                    setResult(111, intent);
                } else {
                    setResult(100, intent);
                }
            });
            builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> {
                //Do nothing
            });

            AlertDialog dialog = builder.create();
            Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.show();
        });
    }

    private void popupMsg(SpannableString title, SpannableString msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogCustom);
        builder.setCancelable(true);

        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.info_dialog, null);
        builder.setView(dialogView);

        TextView alertInfoBtn = dialogView.findViewById(R.id.alertInfoBtn);

        // alert dialog title align center
        title.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), 0, title.length(), 0);
        // alert dialog msg align center
        msg.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), 0, msg.length(), 0);

        builder.setTitle(title);
        builder.setMessage(msg);

        AlertDialog dialog = builder.create();
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.show();

        alertInfoBtn.setOnClickListener(v -> dialog.cancel());
    }

    private void toolbarSetup() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void buttonSetup() {
        stateBtnSetup();
        durationBtnSetup();
        locationBtnSetup();
        multiPostBtnSetup();
        saveWallpaperBtnSetup();
        imageAlignBtnSetup();
        clearRecentBtnSetup();

        RelativeLayout stateInfo = findViewById(R.id.stateInfoBtn);
        stateInfo.setOnClickListener(v -> popupMsg(new SpannableString("State"), new SpannableString(getString(R.string.stateInfoMsg))));

        RelativeLayout durationInfo = findViewById(R.id.durationInfoBtn);
        durationInfo.setOnClickListener(v -> popupMsg(new SpannableString("Duration"), new SpannableString(getString(R.string.durationInfoMsg))));

        RelativeLayout locationInfo = findViewById(R.id.locationInfoBtn);
        locationInfo.setOnClickListener(v -> popupMsg(new SpannableString("Location"), new SpannableString(getString(R.string.locationInfoMsg))));

        RelativeLayout multiImagePostInfo = findViewById(R.id.multiPostInfoBtn);
        multiImagePostInfo.setOnClickListener(v -> popupMsg(new SpannableString("Multi-image Post"), new SpannableString(getString(R.string.multiImagePostInfoMsg))));

        RelativeLayout saveWallpaperInfo = findViewById(R.id.saveWallpaperInfoBtn);
        saveWallpaperInfo.setOnClickListener(v -> popupMsg(new SpannableString("Save Wallpaper"), new SpannableString(getString(R.string.saveWallpaperInfoMsg))));

        RelativeLayout imageAlignInfo = findViewById(R.id.imageAlignInfoBtn);
        imageAlignInfo.setOnClickListener(v -> popupMsg(new SpannableString("Image Align"), new SpannableString(getString(R.string.imageAlignInfoMsg))));

        RelativeLayout clearRecentInfo = findViewById(R.id.clearRecentInfoBtn);
        clearRecentInfo.setOnClickListener(v -> popupMsg(new SpannableString("Clear Recent Searches"), new SpannableString(getString(R.string.clearRecentInfoMsg))));
    }
}