package com.wallagram.Activities;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
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
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.wallagram.R;
import com.wallagram.Utils.Functions;

import java.util.ArrayList;
import java.util.Objects;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class SettingsActivity extends AppCompatActivity {
    private static final String TAG = "SETTINGS_ACTIVITY";

    private String mMode;

    private int mStateChange = -1;
    private String mSetLocation;
    private String mSetSearchSort;
    private String mSetImageAlign;
    private String mSetTheme;
    private boolean mClearRecentChange = false;

    private SharedPreferences mSharedPreferences;
    private SharedPreferences.Editor mEditor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mSharedPreferences = getSharedPreferences("Settings", Context.MODE_PRIVATE);
        mEditor = mSharedPreferences.edit();
        mEditor.apply();

        toolbarSetup();

        settingTypeSetup();

        buttonSetup();
    }

    private void settingTypeSetup(){
        mMode = mSharedPreferences.getString("setMode", "Insta");

        View redditSearchSort = findViewById(R.id.redditSearchSort);

        View instaPostPref = findViewById(R.id.instaPostPref);
        View instaMultiImage = findViewById(R.id.instaMultiImage);
        View instaAllVideos = findViewById(R.id.instaAllowVideos);

        View redditSearchSortBtn = findViewById(R.id.redditSearchSortInfoBtn);

        View instaPostPrefBtn = findViewById(R.id.instaPostPrefInfoBtn);
        View instaMultiImageBtn  = findViewById(R.id.instaMultiImageInfoBtn);
        View instaAllowVideosBtn = findViewById(R.id.instaAllowVideosInfoBtn);

        ArrayList<View> redditSettingList = new ArrayList<>();
        redditSettingList.add(redditSearchSort);
        redditSettingList.add(redditSearchSortBtn);

        ArrayList<View> instaSettingList = new ArrayList<>();
        instaSettingList.add(instaPostPref);
        instaSettingList.add(instaMultiImage);
        instaSettingList.add(instaAllVideos);
        instaSettingList.add(instaPostPrefBtn);
        instaSettingList.add(instaMultiImageBtn);
        instaSettingList.add(instaAllowVideosBtn);

        if(!mMode.equalsIgnoreCase("insta")) {
            for (View v : instaSettingList) {
                v.setVisibility(View.GONE);
            }
        }

        if(!mMode.equalsIgnoreCase("reddit")) {
            for (View v : redditSettingList) {
                v.setVisibility(View.GONE);
            }
        }
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
        if (mSharedPreferences.getInt("state", 1) == 0) {
            state.setChecked(false);
        }

        state.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            if (isChecked) {
                mStateChange = 1;

                mEditor.putInt("state", 1);
                mEditor.apply();

                Log.d(TAG, "stateBtnSetup: State value updated to: On");
                if (!mSharedPreferences.getString("searchName", "").equalsIgnoreCase("")) {
                    Functions.findNewPostPeriodicRequest(getApplicationContext());
                }

                Intent intent = new Intent();
                if (mClearRecentChange) {
                    setResult(110, intent);
                } else {
                    setResult(10, intent);
                }
            } else {
                mStateChange = 0;

                mEditor.putInt("state", 0);
                mEditor.apply();

                Log.d(TAG, "stateBtnSetup: State value updated to: Off");
                WorkManager.getInstance(getApplicationContext()).cancelAllWorkByTag("findNewPost: Periodic");

                Intent intent = new Intent();
                if (mClearRecentChange) {
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
        int setDuration = mSharedPreferences.getInt("duration", 1);
        String setMetric = mSharedPreferences.getString("metric", "Hours");

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

    private void searchSortBtnSetup() {
        RelativeLayout searchSortBtn = findViewById(R.id.redditSearchSort);
        String[] redditOptions = {"Random", "New", "Hot", "Top"};
        String[] instaOptions = {"Random", "New"};
        String[] options;

        if(mMode.equalsIgnoreCase("insta")){
            options = instaOptions;
        }
        else {
            options = redditOptions;
        }

        //init value
        mSetSearchSort = mSharedPreferences.getString("setSearchSort", options[0]);

        TextView searchSortValue = findViewById(R.id.searchSortValue);
        searchSortValue.setText(mSetSearchSort);

        searchSortBtn.setOnClickListener(v -> {
            mSetSearchSort = mSharedPreferences.getString("setSearchSort", options[0]);

            View.OnClickListener searchSortRadioClick = view -> {
                RadioButton rb = (RadioButton) view;
                String chosenOption = null;

                switch (rb.getText().toString()) {
                    case "Random":
                        Log.d(TAG, "SearchSortBtnSetup: Search sort set to '" + options[0] + "'");
                        chosenOption = options[0];
                        break;
                    case "New":
                        Log.d(TAG, "SearchSortBtnSetup: Search sort set to '" + options[1] + "'");
                        chosenOption = options[1];
                        break;
                    case "Hot":
                        Log.d(TAG, "SearchSortBtnSetup: Search sort set to '" + redditOptions[2] + "'");
                        chosenOption = options[2];
                        break;
                    case "Top":
                        Log.d(TAG, "SearchSortBtnSetup: Search sort set to '" + redditOptions[3] + "'");
                        chosenOption = options[3];
                        break;
                    default:
                        Log.e(TAG, "SearchSortBtnSetup: Search sort error");
                        break;
                }

                if (chosenOption != null) {
                    mEditor.putString("setSearchSort", chosenOption);
                    mEditor.commit();
                    searchSortValue.setText(chosenOption);
                    rb.toggle();
                }
            };

            radioDialog("Search sort", options, searchSortRadioClick, mSetSearchSort);
        });
    }

    private void locationBtnSetup() {
        RelativeLayout location = findViewById(R.id.location);
        TextView locationValue = findViewById(R.id.locationValue);
        String[] options = {"Home Screen", "Lock Screen", "Both"};

        mSetLocation = mSharedPreferences.getString("setLocation", options[0]);
        locationValue.setText(mSetLocation);

        location.setOnClickListener(v -> {
            mSetLocation = mSharedPreferences.getString("setLocation", options[0]);

            View.OnClickListener locationRadioClick = view -> {
                RadioButton rb = (RadioButton) view;

                String chosenOption = null;

                switch (rb.getText().toString()) {
                    case "Home Screen":
                        Log.d(TAG, "locationBtnSetup: Location set to 'Home Screen'");
                        chosenOption = options[0];
                        break;
                    case "Lock Screen":
                        Log.d(TAG, "locationBtnSetup: Location set to 'Lock Screen'");
                        chosenOption = options[1];
                        break;
                    case "Both":
                        Log.d(TAG, "locationBtnSetup: Location set to 'Both'");
                        chosenOption = options[2];
                        break;
                    default:
                        Log.e(TAG, "imageAlignBtnSetup: Image align error");
                        break;
                }

                if (chosenOption != null) {
                    mEditor.putString("setLocation", chosenOption);
                    mEditor.commit();
                    locationValue.setText(chosenOption);
                    rb.toggle();
                }
            };

            radioDialog("Location", options, locationRadioClick, mSetLocation);
        });
    }

    private void imageAlignBtnSetup() {
        RelativeLayout imageAlign = findViewById(R.id.imageAlign);
        TextView alignValue = findViewById(R.id.alignValue);
        String[] options = {"Left", "Centre", "Right"};

        mSetImageAlign = mSharedPreferences.getString("setImageAlign", options[1]);
        alignValue.setText(mSetImageAlign);

        imageAlign.setOnClickListener(v -> {
            mSetImageAlign = mSharedPreferences.getString("setImageAlign", options[1]);

            View.OnClickListener alignRadioClick = view -> {
                RadioButton rb = (RadioButton) view;

                String chosenOption = null;

                switch (rb.getText().toString()) {
                    case "Left":
                        Log.d(TAG, "imageAlignBtnSetup: ImageAlign set to 'Left'");
                        chosenOption = options[0];
                        break;
                    case "Centre":
                        Log.d(TAG, "imageAlignBtnSetup: ImageAlign set to 'Centre'");
                        chosenOption = options[1];
                        break;
                    case "Right":
                        Log.d(TAG, "imageAlignBtnSetup: ImageAlign set to 'Right'");
                        chosenOption = options[2];
                        break;
                    default:
                        Log.e(TAG, "imageAlignBtnSetup: Image align error");
                        break;
                }

                if (chosenOption != null) {
                    mEditor.putString("setImageAlign", chosenOption);
                    mEditor.commit();
                    alignValue.setText(chosenOption);
                    rb.toggle();
                }
            };

            radioDialog("Image Align", options, alignRadioClick, mSetImageAlign);
        });
    }

    @SuppressLint("SetTextI18n")
    private void postPrefBtnSetup() {
        RelativeLayout postPref = findViewById(R.id.instaPostPref);

        //init value
        int setPostPref = mSharedPreferences.getInt("postPref", 1);
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
        RelativeLayout multiPost = findViewById(R.id.instaMultiImage);

        //init value
        int setMultiImage = mSharedPreferences.getInt("multiImage", 1);
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
        SwitchCompat allowVideos = findViewById(R.id.instaAllowVideos);

        //init value
        if (mSharedPreferences.getBoolean("allowVideos", false)) {
            allowVideos.setChecked(true);
        }

        allowVideos.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            if (isChecked) {
                mEditor.putBoolean("allowVideos", true);
                mEditor.apply();
                Log.d(TAG, "allowVideosBtnSetup: Allow videos value updated to: Yes");
            } else {
                mEditor.putBoolean("allowVideos", false);
                mEditor.apply();
                Log.d(TAG, "allowVideosBtnSetup: Allow videos value updated to: No");
            }
        });
    }

    private void saveWallpaperBtnSetup() {
        SwitchCompat saveWallpaper = findViewById(R.id.saveWallpaper);

        //init value
        if (mSharedPreferences.getInt("saveWallpaper", 0) == 1) {
            saveWallpaper.setChecked(true);
        }

        saveWallpaper.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            //Request Storage Access
            Functions.requestPermission(this);

            if (ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                saveWallpaper.setChecked(false);
            } else {
                if (isChecked) {
                    mEditor.putInt("saveWallpaper", 1);
                    mEditor.apply();
                    Log.d(TAG, "saveWallpaperBtnSetup: Save Wallpaper value updated to: Yes");
                } else {
                    mEditor.putInt("saveWallpaper", 0);
                    mEditor.apply();
                    Log.d(TAG, "saveWallpaperBtnSetup: Save Wallpaper value updated to: No");
                }
            }
        });
    }

    private void themeBtnSetup() {
        RelativeLayout theme = findViewById(R.id.theme);
        TextView themeValue = findViewById(R.id.themeValue);
        String[] options = {"System Default", "Light", "Dark"};
        mSetTheme = mSharedPreferences.getString("setTheme", options[0]);

        themeValue.setText(mSetTheme);

        theme.setOnClickListener(v -> {
            View.OnClickListener radioClick = view -> {
                RadioButton rb = (RadioButton) view;

                String chosenOption = null;

                switch (rb.getText().toString()) {
                    case "System Default":
                        Log.d(TAG, "themeBtnSetup: Theme set to 'System Default'");
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                        chosenOption = options[0];
                        break;
                    case "Light":
                        Log.d(TAG, "themeBtnSetup: Theme set to 'Light'");
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                        chosenOption = options[1];
                        break;
                    case "Dark":
                        Log.d(TAG, "themeBtnSetup: Theme set to 'Dark'");
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                        chosenOption = options[2];
                        break;
                    default:
                        Log.e(TAG, "themeBtnSetup: Theme setting error");
                        break;
                }

                if (chosenOption != null) {
                    mEditor.putString("setTheme", chosenOption);
                    mEditor.commit();
                    themeValue.setText(chosenOption);
                    rb.toggle();
                }
            };

            radioDialog("Theme", options, radioClick, mSetTheme);
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
                mClearRecentChange = true;

                Functions.removeDBAccounts(this);

                Intent intent = new Intent();
                if (mStateChange == 1) {
                    setResult(110, intent);
                } else if (mStateChange == 0) {
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

        if(mMode.equalsIgnoreCase("reddit")) {
            searchSortBtnSetup();
        }

        locationBtnSetup();

        if(mMode.equalsIgnoreCase("insta")) {
            postPrefBtnSetup();
            multiPostBtnSetup();
            allowVideosBtnSetup();
        }

        saveWallpaperBtnSetup();
        imageAlignBtnSetup();
        themeBtnSetup();
        clearRecentBtnSetup();

        RelativeLayout stateInfo = findViewById(R.id.stateInfoBtn);
        stateInfo.setOnClickListener(v -> Functions.popupMsg(this, new SpannableString("State"), new SpannableString(getString(R.string.state_info_msg))));

        RelativeLayout durationInfo = findViewById(R.id.durationInfoBtn);
        durationInfo.setOnClickListener(v -> Functions.popupMsg(this, new SpannableString("Duration"), new SpannableString(getString(R.string.duration_info_msg))));

        if(mMode.equalsIgnoreCase("reddit")) {
            RelativeLayout searchSortInfo = findViewById(R.id.redditSearchSortInfoBtn);
            searchSortInfo.setOnClickListener(v -> Functions.popupMsg(this, new SpannableString("Search sort"), new SpannableString(getString(R.string.search_sort_info_msg))));
        }

        RelativeLayout locationInfo = findViewById(R.id.locationInfoBtn);
        locationInfo.setOnClickListener(v -> Functions.popupMsg(this, new SpannableString("Location"), new SpannableString(getString(R.string.location_info_msg))));

        RelativeLayout imageAlignInfo = findViewById(R.id.imageAlignInfoBtn);
        imageAlignInfo.setOnClickListener(v -> Functions.popupMsg(this, new SpannableString("Image Align"), new SpannableString(getString(R.string.image_align_info_msg))));

        if(mMode.equalsIgnoreCase("insta")) {
            RelativeLayout postPrefInfo = findViewById(R.id.instaPostPrefInfoBtn);
            postPrefInfo.setOnClickListener(v -> Functions.popupMsg(this, new SpannableString("Post Preference"), new SpannableString(getString(R.string.post_pref_info_msg))));

            RelativeLayout multiImagePostInfo = findViewById(R.id.instaMultiImageInfoBtn);
            multiImagePostInfo.setOnClickListener(v -> Functions.popupMsg(this, new SpannableString("Multi-image Post"), new SpannableString(getString(R.string.multi_image_post_info_msg))));

            RelativeLayout allowVideosInfo = findViewById(R.id.instaAllowVideosInfoBtn);
            allowVideosInfo.setOnClickListener(v -> Functions.popupMsg(this, new SpannableString("Allow Video Posts"), new SpannableString(getString(R.string.allow_videos_info_msg))));
        }

        RelativeLayout saveWallpaperInfo = findViewById(R.id.saveWallpaperInfoBtn);
        saveWallpaperInfo.setOnClickListener(v -> Functions.popupMsg(this, new SpannableString("Save Wallpapers"), new SpannableString(getString(R.string.save_wallpaper_info_msg))));

        RelativeLayout themeInfo = findViewById(R.id.themeInfoBtn);
        themeInfo.setOnClickListener(v -> Functions.popupMsg(this, new SpannableString("Theme"), new SpannableString(getString(R.string.theme_info_msg))));

        RelativeLayout clearRecentInfo = findViewById(R.id.clearRecentInfoBtn);
        clearRecentInfo.setOnClickListener(v -> Functions.popupMsg(this, new SpannableString("Clear Recent Searches"), new SpannableString(getString(R.string.clear_recent_info_msg))));
    }

    private void radioDialog(String title, String[] options, View.OnClickListener radioClick, String setValue) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        LayoutInflater inflater = getLayoutInflater();
        View dialogView;

        if(options.length == 2) {
            dialogView = inflater.inflate(R.layout.dialog_2_options, null);
        }
        else if(options.length == 3) {
            dialogView = inflater.inflate(R.layout.dialog_3_options, null);
        }
        else {
            dialogView = inflater.inflate(R.layout.dialog_4_options, null);
        }

        builder.setView(dialogView);

        TextView infoTitle = dialogView.findViewById(R.id.title);
        infoTitle.setText(title);

        RadioGroup radioGroup = dialogView.findViewById(R.id.radioGroup);
        for (int i = 0; i < radioGroup.getChildCount(); i++) {
            View radioButton = radioGroup.getChildAt(i);
            if (radioButton instanceof RadioButton) {
                ((RadioButton) radioButton).setText(options[i]);
                radioButton.setOnClickListener(radioClick);

                if (setValue.equalsIgnoreCase(((RadioButton) radioButton).getText().toString())) {
                    ((RadioButton) radioButton).toggle();
                }
            }
        }

        AlertDialog dialog = builder.create();
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.show();
    }
}