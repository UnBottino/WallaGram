package com.wallagram;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.TextView;

import com.wallagram.Adapters.AccountListAdapter;
import com.wallagram.AsyncTasks.NewBgTask;
import com.wallagram.Connectors.ForegroundService;
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

    public static ImageView mSetProfilePic;
    public static TextView mSetAccountName;
    public SearchView mSearchBar;

    public static RecyclerView mRecyclerView;
    public static AccountListAdapter mAdapter;
    public static List<Account> mDBAccountList = new ArrayList<>();

    public static ProgressBar mProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Request Storage Access
        Functions.requestPermission(this);

        //General page setup
        pageSetup();

        //Run Continuously
        Intent intent = new Intent(getApplicationContext(), ForegroundService.class);
        intent.setAction(ForegroundService.ACTION_START_FOREGROUND_SERVICE);
        startService(intent);
    }

    private void setupToolbar() {
        Toolbar mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        if(getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.settings_menu, menu);
        return true;
    }

    private void pageSetup(){
        mSharedPreferences = getSharedPreferences("SET_ACCOUNT", 0);
        mEditor = getSharedPreferences("SET_ACCOUNT", 0).edit();
        mEditor.apply();

        //Toolbar
        setupToolbar();

        //Set Profile Information
        mSetProfilePic = findViewById(R.id.setProfilePic);

        if(!mSharedPreferences.getString("setProfilePic", "").equalsIgnoreCase("No Account Set")) {
            Picasso.get()
                    .load(Uri.parse(mSharedPreferences.getString("setProfilePic", "")))
                    .into(mSetProfilePic);
        }

        mSetAccountName = findViewById(R.id.SetAccountName);
        mSetAccountName.setText(mSharedPreferences.getString("setAccountName", "No Account Set"));

        //Progress Bar
        mProgressBar = findViewById(R.id.progressBar);

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
        searchPlateView.setBackgroundResource(R.color.purple);

        mSearchBar.setOnClickListener(v -> mSearchBar.setIconified(false));

        mSearchBar.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                mEditor.putString("searchName", query);
                mEditor.commit();

                mSearchBar.setQuery("", false);

                mProgressBar.setVisibility(View.VISIBLE);

                Intent intent = new Intent(getApplicationContext(), AlarmReceiver.class);
                PendingIntent pendingIntent = PendingIntent
                        .getBroadcast(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

                NewBgTask testAsyncTask = new NewBgTask(getApplicationContext(), mSharedPreferences.getString("searchName", "NULL"));
                testAsyncTask.execute();

                AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
                alarmManager.setRepeating(AlarmManager.RTC_WAKEUP,System.currentTimeMillis(), 900000, pendingIntent);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String query) {
                return false;
            }
        });
    }

    private void setupPreviousAccounts(){
        mAdapter = new AccountListAdapter(getApplicationContext(), mDBAccountList);
        mRecyclerView = findViewById(R.id.accountNameList);
        mDBAccountList = Functions.getDBAccounts(this);
        Collections.reverse(mDBAccountList);
        updateTrackList();
    }

    private void updateTrackList(){
        runOnUiThread(new Thread(() -> {
            mAdapter = new AccountListAdapter(getApplicationContext(), mDBAccountList);
            mRecyclerView.setAdapter(mAdapter);
            mRecyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        }));
    }
}