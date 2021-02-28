package com.wallagram.Activities;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.wallagram.Adapters.AccountListAdapter;
import com.wallagram.Adapters.SuggestionListAdapter;
import com.wallagram.Connectors.ForegroundService;
import com.wallagram.Model.Account;
import com.wallagram.Model.SuggestionAccount;
import com.wallagram.R;
import com.wallagram.Utils.Functions;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements LifecycleObserver {
    private static final String TAG = "MAIN_ACTIVITY";

    public static boolean IS_APP_IN_FOREGROUND = false;

    private SharedPreferences sharedPreferences;

    public static ConstraintLayout mLoadingView;

    // TODO: 13/02/2021 Create a broadcast receiver for static
    public static ImageView mSetProfilePic;
    public static TextView mSetAccountName;

    public androidx.appcompat.widget.SearchView mSearchBar;

    public static RecyclerView mRecyclerView;
    public static AccountListAdapter mAdapter;
    public static List<Account> mDBAccountList = new ArrayList<>();

    private static boolean suggestionsOpened = false;
    private LinearLayout suggestionLayout;
    private ImageView suggestionIcon;

    private static final List<SuggestionAccount> mSuggestionAccountList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Check is app in foreground
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);

        sharedPreferences = getApplicationContext().getSharedPreferences("Settings", Context.MODE_PRIVATE);

        //Request Storage Access
        Functions.requestPermission(this);
        getScreenSize();

        //General page setup
        pageSetup();

        // TODO: 21/02/2021 Test on a fresh install
        //Run Continuously
        if (sharedPreferences.getInt("state", 0) == 1 && !sharedPreferences.getString("searchName", "").equalsIgnoreCase("")) {
            Functions.callAlarm(getApplicationContext());
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        PowerManager pm = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
        if (pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName())) {
            Log.e(TAG, "Using Optimization");

            AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogCustom);
            builder.setCancelable(false);
            builder.setTitle("Important");
            builder.setMessage(R.string.optimise_msg);
            builder.setPositiveButton("Continue", (dialog, which) -> {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                startActivity(intent);
            });
            builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> finish());

            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }

    private void pageSetup() {
        //Loading View
        mLoadingView = findViewById(R.id.loadingView);
        mLoadingView.setOnClickListener(v -> {
            //Do nothing
        });

        //Settings Button
        RelativeLayout settingsBtn = findViewById(R.id.settingsBtn);
        settingsBtn.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivityForResult(intent, 59);
        });

        //Set Profile Information
        mSetProfilePic = findViewById(R.id.setProfilePic);

        String setProfilePic = sharedPreferences.getString("setProfilePic", "");

        if (!setProfilePic.equalsIgnoreCase("")) {
            Picasso.get()
                    .load(Uri.parse(setProfilePic))
                    .into(mSetProfilePic);
        }

        mSetAccountName = findViewById(R.id.SetAccountName);
        mSetAccountName.setText(sharedPreferences.getString("setAccountName", "No Account Set"));

        //Search Box
        setupSearchBar();

        //Previous Accounts
        setupPreviousAccounts();

        //Suggestions
        setupSuggestions();

        //Clear Listeners
        setupClearListeners();
    }

    private void setupSearchBar() {
        mSearchBar = findViewById(R.id.searchBar);

        mSearchBar.setOnClickListener(v -> mSearchBar.setIconified(false));

        mSearchBar.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Log.d(TAG, "Searching for: " + query);

                mLoadingView.setVisibility(View.VISIBLE);

                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("searchName", query);
                editor.apply();

                mSearchBar.setQuery("", false);
                mSearchBar.setIconified(true);
                mSearchBar.clearFocus();

                Intent i = new Intent(getApplicationContext(), ForegroundService.class);
                i.setAction(ForegroundService.ACTION_START_FOREGROUND_SERVICE);
                startForegroundService(i);

                return false;
            }

            @Override
            public boolean onQueryTextChange(String query) {
                return false;
            }
        });
    }

    private void setupPreviousAccounts() {
        mRecyclerView = findViewById(R.id.accountNameList);
        mDBAccountList = Functions.getDBAccounts(this);
        Collections.reverse(mDBAccountList);

        mAdapter = new AccountListAdapter(getApplicationContext(), mDBAccountList);

        mAdapter.setOnDataChangeListener(accountName -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this, R.style.AlertDialogCustom);
            builder.setCancelable(true);
            builder.setTitle("Remove '" + accountName + "' from list");
            builder.setMessage("Are you sure?");
            builder.setPositiveButton("Confirm", (dialog, which) -> {
                Functions.removeDBAccountByName(MainActivity.this, accountName);

                int pos = 0;
                for (Account a : mDBAccountList) {
                    if (a.getAccountName().equalsIgnoreCase(accountName)) {
                        mDBAccountList.remove(a);
                        mAdapter.notifyItemRemoved(pos);
                        mAdapter.notifyDataSetChanged();
                        break;
                    }
                    pos++;
                }
            });
            builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> {
                //Do nothing
            });

            AlertDialog dialog = builder.create();
            Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.show();
        });

        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupSuggestions() {
        suggestionLayout = findViewById(R.id.suggestionsLayout);
        suggestionIcon = findViewById(R.id.suggestionIcon);
        RecyclerView mSuggestionRecyclerView = findViewById(R.id.suggestionAccountList);

        String[] nameList = getResources().getStringArray(R.array.suggestion_names);

        for (String s : nameList) {
            int id = getResources().getIdentifier("suggestion_" + s, "drawable", getPackageName());
            SuggestionAccount a = new SuggestionAccount(s, id);
            mSuggestionAccountList.add(a);
        }

        SuggestionListAdapter mSuggestionAdapter = new SuggestionListAdapter(getApplicationContext(), mSuggestionAccountList);
        mSuggestionRecyclerView.setAdapter(mSuggestionAdapter);
        mSuggestionRecyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));

        findViewById(R.id.suggestionBtn).setOnClickListener(v -> {
            if (!suggestionsOpened) {
                showSuggestions();
            } else {
                hideSuggestions();
            }
            suggestionsOpened = !suggestionsOpened;
        });
    }

    private void showSuggestions() {
        Log.d(TAG, "Opening suggestions menu");

        mAdapter.isClickable = false;

        Drawable unwrappedDrawable = suggestionIcon.getBackground();
        Drawable wrappedDrawable = DrawableCompat.wrap(unwrappedDrawable);
        DrawableCompat.setTint(wrappedDrawable, ContextCompat.getColor(this, R.color.purple));

        suggestionLayout.setVisibility(View.VISIBLE);
        TranslateAnimation animate = new TranslateAnimation(
                0,
                0,
                suggestionLayout.getHeight(),
                0);
        animate.setDuration(300);
        animate.setFillAfter(true);
        suggestionLayout.startAnimation(animate);
    }

    private void hideSuggestions() {
        Log.d(TAG, "Closing suggestions menu");

        mAdapter.isClickable = true;

        Drawable unwrappedDrawable = suggestionIcon.getBackground();
        Drawable wrappedDrawable = DrawableCompat.wrap(unwrappedDrawable);
        DrawableCompat.setTint(wrappedDrawable, ContextCompat.getColor(this, R.color.white));

        suggestionLayout.setVisibility(View.INVISIBLE);
        TranslateAnimation animate = new TranslateAnimation(
                0,
                0,
                0,
                suggestionLayout.getHeight() + 20);
        animate.setDuration(300);
        animate.setFillAfter(false);
        suggestionLayout.startAnimation(animate);
    }

    @SuppressLint("ClickableViewAccessibility")
    public void setupClearListeners() {
        RelativeLayout settingsBtn = findViewById(R.id.settingsBtn);
        ConstraintLayout mainContainer = findViewById(R.id.mainContainer);

        mainContainer.setOnTouchListener(new clearViewListener());
        settingsBtn.setOnTouchListener(new clearViewListener());
        mSearchBar.setOnTouchListener(new clearSuggestionsListener());
    }

    class clearViewListener implements View.OnTouchListener {
        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent event) {

            if (suggestionsOpened) {
                hideSuggestions();
            }

            mSearchBar.setIconified(true);

            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            }

            getWindow().getDecorView().clearFocus();

            return false;
        }
    }

    class clearSuggestionsListener implements View.OnTouchListener {
        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent event) {

            if (suggestionsOpened) {
                hideSuggestions();
            }

            return false;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 59) {
            if (resultCode == 111) {
                Log.d(TAG, "Update RecyclerView Received (Clear Recent Searches)");
                mAdapter.notifyItemRangeRemoved(0, mDBAccountList.size());
                mAdapter.notifyDataSetChanged();
                mDBAccountList.clear();
            }
        }
    }

    private void getScreenSize() {
        DisplayMetrics metrics = new DisplayMetrics();
        Display display = getWindowManager().getDefaultDisplay();
        display.getMetrics(metrics);
        final int screenWidth = metrics.widthPixels;
        final int screenHeight = metrics.heightPixels;

        Log.d(TAG, "Screen Width: " + screenWidth);
        Log.d(TAG, "Screen Height: " + screenHeight);

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("screenWidth", screenWidth);
        editor.putInt("screenHeight", screenHeight);
        Log.i("ScreenSize", "Global screen dimensions set");
        editor.apply();
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onAppBackgrounded() {
        //App in background
        Log.d(TAG, "App moved to background");
        IS_APP_IN_FOREGROUND = false;
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onAppForegrounded() {
        // App in foreground
        Log.d(TAG, "App moved to foreground");
        IS_APP_IN_FOREGROUND = true;
    }
}