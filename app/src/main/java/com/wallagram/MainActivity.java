package com.wallagram;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.SearchView;
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
    private NavigationView mNavigationView;
    private Toolbar mToolbar;

    public static ImageView mSetProfilePic;
    public static TextView mSetAccountName;
    public SearchView mSearchBar;

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

    private void setupToolbar() {
        Toolbar mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        if(getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
    }

    public void setUpDrawer(){
        mDrawerLayout = findViewById(R.id.drawerLayout);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, mDrawerLayout, mToolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        mDrawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        mNavigationView = findViewById(R.id.nav_view);
        mNavigationView.setNavigationItemSelectedListener(menuItem -> {
            if (menuItem.getItemId() == R.id.nav_active) {

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
        switch (item.getItemId()) {
            case R.id.action_settings:
                if (mDrawerLayout.isDrawerOpen((GravityCompat.END))) {
                    mDrawerLayout.closeDrawer(GravityCompat.END);
                } else {
                    mDrawerLayout.openDrawer(GravityCompat.END);
                }
                break;
        }
        return true;
    }

    private void pageSetup(){
        //Loading View
        mLoadingView = findViewById(R.id.loadingView);
        mLoadingView.setOnClickListener(v -> {
            // TODO: 09/11/2020 Find a better solution
        });

        mSharedPreferences = getSharedPreferences("SET_ACCOUNT", 0);
        mEditor = getSharedPreferences("SET_ACCOUNT", 0).edit();
        mEditor.apply();

        //Toolbar
        setupToolbar();

        //Drawer
        setUpDrawer();

        //Set Profile Information
        mSetProfilePic = findViewById(R.id.setProfilePic);

        if(!mSharedPreferences.getString("setProfilePic", "").equalsIgnoreCase("No Account Set")) {
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
        int searchPlateId = mSearchBar.getContext().getResources()
                .getIdentifier("android:id/search_plate", null, null);
        View searchPlateView = mSearchBar.findViewById(searchPlateId);
        searchPlateView.setBackgroundResource(R.color.dark_black);

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

                if(!mSharedPreferences.getBoolean("alarmActive", false)){
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
    }

    private void setupPreviousAccounts(){
        mAdapter = new AccountListAdapter(getApplicationContext(), mDBAccountList/*, this*/);
        mRecyclerView = findViewById(R.id.accountNameList);
        mDBAccountList = Functions.getDBAccounts(this);
        Collections.reverse(mDBAccountList);
        updateTrackList();
    }

    private void updateTrackList(){
        runOnUiThread(new Thread(() -> {
            mAdapter = new AccountListAdapter(getApplicationContext(), mDBAccountList/*, this*/);
            mRecyclerView.setAdapter(mAdapter);
            mRecyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        }));
    }

    public static void callAlarm(Context context){
        Intent intent = new Intent(context, AlarmReceiver.class);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = PendingIntent
                .getBroadcast(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        if(alarmManager != null) {
            alarmManager.cancel(pendingIntent);
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 900000, pendingIntent);
        }
    }
}