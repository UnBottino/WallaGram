package com.wallagram.Activities;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.work.WorkManager;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.SpannableString;
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
                case 333:
                    Log.d(TAG, "onActivityResult: Post pref update returned");
                    postPrefBtnSetup();
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
                    Functions.findNewPostPeriodicRequest(getApplicationContext());
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
                WorkManager.getInstance(getApplicationContext()).cancelAllWorkByTag("findNewPost: Periodic");

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

    @SuppressLint("SetTextI18n")
    private void postPrefBtnSetup() {
        RelativeLayout postPref = findViewById(R.id.postPref);

        //init value
        int setPostPref = sharedPreferences.getInt("postPref", 1);
        TextView postPrefValue = findViewById(R.id.postPrefValue);

        switch (setPostPref) {
            case 1:
                postPrefValue.setText(setPostPref + "st");
                break;
            case 2:
                postPrefValue.setText(setPostPref + "nd");
                break;
            case 3:
                postPrefValue.setText(setPostPref + "rd");
                break;
            default:
                postPrefValue.setText(setPostPref + "th");
                break;
        }

        postPref.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, PostPrefActivity.class);
            startActivityForResult(intent, 800);
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

    private void allowVideosBtnSetup() {
        SwitchCompat allowVideos = findViewById(R.id.allowVideos);

        //init value
        if (sharedPreferences.getBoolean("allowVideos", false)) {
            allowVideos.setChecked(true);
        }

        allowVideos.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            if (isChecked) {
                editor.putBoolean("allowVideos", true);
                editor.apply();
                Log.d(TAG, "allowVideosBtnSetup: Allow videos value updated to: Yes");
            } else {
                editor.putBoolean("allowVideos", false);
                editor.apply();
                Log.d(TAG, "allowVideosBtnSetup: Allow videos value updated to: No");
            }
        });
    }

    private void saveWallpaperBtnSetup() {
        SwitchCompat saveWallpaper = findViewById(R.id.saveWallpaper);

        //init value
        if (sharedPreferences.getInt("saveWallpaper", 0) == 1) {
            saveWallpaper.setChecked(true);
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

    private void clearRecentBtnSetup() {
        RelativeLayout clearRecent = findViewById(R.id.clearRecentSearches);

        clearRecent.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setCancelable(true);

            LayoutInflater inflater = getLayoutInflater();
            View dialogView = inflater.inflate(R.layout.dialog_warning, null);
            builder.setView(dialogView);
            TextView infoTitle = dialogView.findViewById(R.id.infoTitle);
            TextView infoMsg = dialogView.findViewById(R.id.infoMsg);

            infoTitle.setText(R.string.clear_previous);
            infoMsg.setText(R.string.warning_msg);

            AlertDialog dialog = builder.create();
            Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.show();

            TextView cancelBtn = dialogView.findViewById(R.id.cancelBtn);
            TextView confirmBtn = dialogView.findViewById(R.id.confirmBtn);

            confirmBtn.setOnClickListener(view -> {
                clearRecentChange = true;

                Functions.removeDBAccounts(this);

                Intent intent = new Intent();
                if (stateChange == 1) {
                    setResult(110, intent);
                } else if (stateChange == 0) {
                    setResult(111, intent);
                } else {
                    setResult(100, intent);
                }

                dialog.cancel();
            });

            cancelBtn.setOnClickListener(view -> dialog.cancel());
        });
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
        postPrefBtnSetup();
        multiPostBtnSetup();
        allowVideosBtnSetup();
        saveWallpaperBtnSetup();
        imageAlignBtnSetup();
        clearRecentBtnSetup();

        RelativeLayout stateInfo = findViewById(R.id.stateInfoBtn);
        stateInfo.setOnClickListener(v -> Functions.popupMsg(this, new SpannableString("State"), new SpannableString(getString(R.string.state_info_msg))));

        RelativeLayout durationInfo = findViewById(R.id.durationInfoBtn);
        durationInfo.setOnClickListener(v -> Functions.popupMsg(this, new SpannableString("Duration"), new SpannableString(getString(R.string.duration_info_msg))));

        RelativeLayout locationInfo = findViewById(R.id.locationInfoBtn);
        locationInfo.setOnClickListener(v -> Functions.popupMsg(this, new SpannableString("Location"), new SpannableString(getString(R.string.location_info_msg))));

        RelativeLayout postPrefInfo = findViewById(R.id.postPrefInfoBtn);
        postPrefInfo.setOnClickListener(v -> Functions.popupMsg(this, new SpannableString("Post Preference"), new SpannableString(getString(R.string.post_pref_info_msg))));

        RelativeLayout multiImagePostInfo = findViewById(R.id.multiPostInfoBtn);
        multiImagePostInfo.setOnClickListener(v -> Functions.popupMsg(this, new SpannableString("Multi-image Post"), new SpannableString(getString(R.string.multi_image_post_info_msg))));

        RelativeLayout allowVideosInfo = findViewById(R.id.allowVideosInfoBtn);
        allowVideosInfo.setOnClickListener(v -> Functions.popupMsg(this, new SpannableString("Allow Video Posts"), new SpannableString(getString(R.string.allow_videos_info_msg))));

        RelativeLayout saveWallpaperInfo = findViewById(R.id.saveWallpaperInfoBtn);
        saveWallpaperInfo.setOnClickListener(v -> Functions.popupMsg(this, new SpannableString("Save Wallpaper"), new SpannableString(getString(R.string.save_wallpaper_info_msg))));

        RelativeLayout imageAlignInfo = findViewById(R.id.imageAlignInfoBtn);
        imageAlignInfo.setOnClickListener(v -> Functions.popupMsg(this, new SpannableString("Image Align"), new SpannableString(getString(R.string.image_align_info_msg))));

        RelativeLayout clearRecentInfo = findViewById(R.id.clearRecentInfoBtn);
        clearRecentInfo.setOnClickListener(v -> Functions.popupMsg(this, new SpannableString("Clear Recent Searches"), new SpannableString(getString(R.string.clear_recent_info_msg))));
    }
}