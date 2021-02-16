package com.wallagram;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
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
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.material.navigation.NavigationView;
import com.wallagram.Adapters.AccountListAdapter;
import com.wallagram.Connectors.ForegroundService;
import com.wallagram.Model.Account;
import com.wallagram.Utils.Functions;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity implements LifecycleObserver {
    private static final String TAG = "MAIN_ACTIVITY";

    public static boolean IS_APP_IN_FOREGROUND = false;

    private SharedPreferences sharedPreferences;

    public static ConstraintLayout mLoadingView;

    private DrawerLayout mDrawerLayout;

    // TODO: 13/02/2021 Create a broadcast receiver for static
    public static ImageView mSetProfilePic;
    public static TextView mSetAccountName;
    public androidx.appcompat.widget.SearchView mSearchBar;

    public static RecyclerView mRecyclerView;
    public static AccountListAdapter mAdapter;
    public static List<Account> mDBAccountList = new ArrayList<>();

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

        /*//Run Continuously
        if (sharedPreferences.getInt("state", 1) == 1) {
            Functions.callAlarm(getApplicationContext());
        }*/
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
            builder.setMessage("This application requires battery optimization to be disabled to function correctly.");
            builder.setPositiveButton("Continue", (dialog, which) -> {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                startActivity(intent);

                /*Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);*/
            });
            builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> {
                finish();
            });

            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }

    private void setUpDrawer() {
        mDrawerLayout = findViewById(R.id.drawerLayout);
        RelativeLayout settingsBtn = findViewById(R.id.settingsBtn);

        settingsBtn.setOnClickListener(v -> mDrawerLayout.openDrawer(GravityCompat.END));

        NavigationView mNavigationView = findViewById(R.id.nav_view);
        mNavigationView.getBackground().setAlpha(235);
        mNavigationView.setNavigationItemSelectedListener(menuItem -> {
            mDrawerLayout.closeDrawer(GravityCompat.END);

            if (menuItem.getItemId() == R.id.nav_active) {
                Intent intent = new Intent(MainActivity.this, StateActivity.class);
                startActivity(intent);
            } else if (menuItem.getItemId() == R.id.nav_location) {
                Intent intent = new Intent(MainActivity.this, LocationActivity.class);
                startActivity(intent);
            } else if (menuItem.getItemId() == R.id.nav_duration) {
                Intent intent = new Intent(MainActivity.this, DurationActivity.class);
                startActivity(intent);
            } else if (menuItem.getItemId() == R.id.nav_multi) {
                Intent intent = new Intent(MainActivity.this, MultiImageActivity.class);
                startActivity(intent);
            } else if (menuItem.getItemId() == R.id.nav_recent) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogCustom);
                builder.setCancelable(true);
                builder.setTitle("Clear Recent Accounts");
                builder.setMessage("Are you sure?");
                builder.setPositiveButton("Confirm", (dialog, which) -> {
                    mDBAccountList.clear();
                    Functions.removeDBAccounts(this);
                    updateAccountList();
                });
                builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> {
                    //Do nothing
                });

                AlertDialog dialog = builder.create();
                dialog.show();
            }

            return false;
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.settings_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            if (mDrawerLayout.isDrawerOpen((GravityCompat.END))) {
                mDrawerLayout.closeDrawer(GravityCompat.END);
            } else {
                mDrawerLayout.openDrawer(GravityCompat.END);
            }
        }
        return true;
    }

    private void pageSetup() {
        //Loading View
        mLoadingView = findViewById(R.id.loadingView);
        mLoadingView.setOnClickListener(v -> {
            // TODO: 09/11/2020 Find a better solution
        });

        //Drawer
        setUpDrawer();

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
    }

    private void setupSearchBar() {
        mSearchBar = findViewById(R.id.searchBar);

        //Removing Underline
        View view = mSearchBar.findViewById(R.id.search_plate);
        view.setBackground(null);

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

        setupHideSearch();
    }

    private void setupPreviousAccounts() {
        mAdapter = new AccountListAdapter(getApplicationContext(), mDBAccountList);
        mRecyclerView = findViewById(R.id.accountNameList);
        mDBAccountList = Functions.getDBAccounts(this);
        Collections.reverse(mDBAccountList);
        updateAccountList();
    }

    private void updateAccountList() {
        Log.d(TAG, "Updating RecyclerView (Account List)");

        runOnUiThread(new Thread(() -> {
            mAdapter = new AccountListAdapter(getApplicationContext(), mDBAccountList);
            mRecyclerView.setAdapter(mAdapter);
            mRecyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        }));
    }

    @SuppressLint("ClickableViewAccessibility")
    public void setupHideSearch() {
        RelativeLayout settingsBtn = findViewById(R.id.settingsBtn);
        ConstraintLayout mainContainer = findViewById(R.id.mainContainer);

        mainContainer.setOnTouchListener(new hideKeyboardListener());
        settingsBtn.setOnTouchListener(new hideKeyboardListener());
    }

    class hideKeyboardListener implements View.OnTouchListener {
        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent event) {

            mSearchBar.setIconified(true);

            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);

            getWindow().getDecorView().clearFocus();

            return false;
        }
    }

    private void getScreenSize() {
        DisplayMetrics metrics = new DisplayMetrics();
        Display display = getWindowManager().getDefaultDisplay();
        display.getMetrics(metrics);
        final int screenWidth = metrics.widthPixels;
        final int screenHeight = metrics.heightPixels;

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