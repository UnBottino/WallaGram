package com.wallagram;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
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
import com.wallagram.AsyncTasks.NewBgTask;
import com.wallagram.Model.Account;
import com.wallagram.Receivers.AlarmReceiver;
import com.wallagram.Utils.Functions;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private SharedPreferences mSharedPreferences;
    private SharedPreferences.Editor mEditor;

    public static ConstraintLayout mLoadingView;

    private DrawerLayout mDrawerLayout;

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

        //Request Storage Access
        Functions.requestPermission(this);

        //General page setup
        pageSetup();

        /*//Run Continuously
        Intent intent = new Intent(getApplicationContext(), ForegroundService.class);
        intent.setAction(ForegroundService.ACTION_START_FOREGROUND_SERVICE);
        startService(intent);*/
    }

    public void setUpDrawer() {
        mDrawerLayout = findViewById(R.id.drawerLayout);
        RelativeLayout settingsBtn = findViewById(R.id.settingsBtn);

        settingsBtn.setOnClickListener(v -> mDrawerLayout.openDrawer(GravityCompat.END));

        NavigationView mNavigationView = findViewById(R.id.nav_view);
        mNavigationView.getBackground().setAlpha(235);
        mNavigationView.setNavigationItemSelectedListener(menuItem -> {
            mDrawerLayout.closeDrawer(GravityCompat.END);

            if (menuItem.getItemId() == R.id.nav_active) {
                Intent intent = new Intent(MainActivity.this, ActiveActivity.class);
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
                    // TODO: 27/01/2021 Remove from DB
                    updateTrackList();
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

        mSharedPreferences = getSharedPreferences("SET_ACCOUNT", 0);
        mEditor = getSharedPreferences("SET_ACCOUNT", 0).edit();
        mEditor.apply();

        //Drawer
        setUpDrawer();

        //Set Profile Information
        mSetProfilePic = findViewById(R.id.setProfilePic);

        if (!mSharedPreferences.getString("setProfilePic", "").equalsIgnoreCase("No Account Set")) {
            Picasso.get()
                    .load(Uri.parse(mSharedPreferences.getString("setProfilePic", "")))
                    .into(mSetProfilePic);
        }

        mSetAccountName = findViewById(R.id.SetAccountName);
        mSetAccountName.setText(mSharedPreferences.getString("setAccountName", "No Account Set"));

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
                mLoadingView.setVisibility(View.VISIBLE);

                mEditor.putString("searchName", query);
                mEditor.commit();

                mSearchBar.setQuery("", false);

                NewBgTask testAsyncTask = new NewBgTask(getApplicationContext());
                testAsyncTask.execute();

                if (!mSharedPreferences.getBoolean("alarmActive", false)) {
                    mEditor.putBoolean("alarmActive", true);
                    mEditor.commit();
                    callAlarm(getApplicationContext());
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String query) {
                return false;
            }
        });

        setupHideSearch();
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

    private void setupPreviousAccounts() {
        mAdapter = new AccountListAdapter(getApplicationContext(), mDBAccountList/*, this*/);
        mRecyclerView = findViewById(R.id.accountNameList);
        mDBAccountList = Functions.getDBAccounts(this);
        Collections.reverse(mDBAccountList);
        updateTrackList();
    }

    private void updateTrackList() {
        runOnUiThread(new Thread(() -> {
            mAdapter = new AccountListAdapter(getApplicationContext(), mDBAccountList/*, this*/);
            mRecyclerView.setAdapter(mAdapter);
            mRecyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        }));
    }

    public static void callAlarm(Context context) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = PendingIntent
                .getBroadcast(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 900000, pendingIntent);
        }
    }
}