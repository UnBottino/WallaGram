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
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import com.wallagram.Sqlite.SQLiteDatabaseAdapter;
import com.wallagram.Utils.Functions;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements LifecycleObserver {
    private final String TAG = "MAIN_ACTIVITY";

    public static boolean IS_APP_IN_FOREGROUND = false;

    private SharedPreferences sharedPreferences;

    public static ConstraintLayout mLoadingView;

    // TODO: 13/02/2021 Create a broadcast receiver for static
    public ImageView mSetProfilePic;
    public TextView mSetAccountName;

    public androidx.appcompat.widget.SearchView mSearchBar;

    public RecyclerView mRecyclerView;
    public AccountListAdapter mAdapter;
    public List<Account> mDBAccountList = new ArrayList<>();

    private boolean suggestionsOpened = false;
    private LinearLayout suggestionLayout;
    private ImageView suggestionIcon;

    private final List<SuggestionAccount> mSuggestionAccountList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Check is app in foreground
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);

        //Register update UI broadcast receiver
        LocalBroadcastManager.getInstance(this).registerReceiver(updateUIReceiver, new IntentFilter("custom-event-name"));

        sharedPreferences = getApplicationContext().getSharedPreferences("Settings", Context.MODE_PRIVATE);

        getScreenSize();

        //General page setup
        pageSetup();

        //Activate Alarm
        if (sharedPreferences.getInt("state", 1) == 1 && !sharedPreferences.getString("searchName", "").equalsIgnoreCase("")) {
            Functions.callAlarm(getApplicationContext());
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        PowerManager pm = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
        if (pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName())) {
            Log.e(TAG, "Asking for optimization");

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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unregister update UI broadcast receiver
        LocalBroadcastManager.getInstance(this).unregisterReceiver(updateUIReceiver);
    }

    private final BroadcastReceiver updateUIReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            boolean error = intent.getBooleanExtra("error", false);
            Log.d(TAG, "Received Broadcast: error = " + error);

            if (error) {
                Log.d(TAG, "Setting Error Profile Pic");
                Picasso.get()
                        .load(R.drawable.frown_straight)
                        .into(mSetProfilePic);

                Log.d(TAG, "Setting Error Display Name");
                String setAccountName = sharedPreferences.getString("setAccountName", "");
                mSetAccountName.setText(setAccountName);
            } else {
                String setAccountName = sharedPreferences.getString("setAccountName", "");
                String setProfilePic = sharedPreferences.getString("setProfilePic", "");

                Log.d(TAG, "Setting Profile Pic");
                Picasso.get()
                        .load(Uri.parse(setProfilePic))
                        .into(mSetProfilePic);

                Log.d(TAG, "Setting Display Name");
                mSetAccountName.setText(setAccountName);

                SQLiteDatabaseAdapter db = new SQLiteDatabaseAdapter(getApplicationContext());

                Account account = new Account(setAccountName, setProfilePic);

                if (!db.checkIfAccountExists(account)) {
                    db.addAccount(account);

                    mDBAccountList.add(0, account);
                    mAdapter.notifyItemInserted(0);
                    mAdapter.notifyDataSetChanged();
                } else {
                    Log.d(TAG, "Account name already in db (" + setAccountName + ")");

                    for (Account a : mDBAccountList) {
                        if (a.getAccountName().equalsIgnoreCase(account.getAccountName())) {
                            //Not using update because of the ordering in recyclerView
                            db.deleteAccount(a.getAccountName());
                            db.addAccount(account);

                            mDBAccountList.remove(a);
                            mAdapter.notifyItemRemoved(mDBAccountList.indexOf(a));
                            mDBAccountList.add(0, account);
                            mAdapter.notifyItemInserted(0);
                            mAdapter.notifyDataSetChanged();
                            break;
                        }
                    }
                }

                Objects.requireNonNull(mRecyclerView.getLayoutManager()).scrollToPosition(0);
            }

            mLoadingView.setVisibility(View.INVISIBLE);
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 59) {
            if (resultCode == 10) {
                stateChanged(1);
            } else if (resultCode == 11) {
                stateChanged(0);
            } else if (resultCode == 100) {
                clearRecentSearches();
            } else if (resultCode == 110) {
                stateChanged(1);
                clearRecentSearches();
            } else if (resultCode == 111) {
                stateChanged(0);
                clearRecentSearches();
            }
        }
    }

    private void clearRecentSearches() {
        Log.d(TAG, "Update RecyclerView Received (Clear Recent Searches)");
        mAdapter.notifyItemRangeRemoved(0, mDBAccountList.size());
        mAdapter.notifyDataSetChanged();
        mDBAccountList.clear();
    }

    private void stateChanged(int state) {
        if (state == 1) {
            Log.d(TAG, "State enabled: Updating set account visuals");
            String setAccountName = sharedPreferences.getString("setAccountName", "No Account Set");
            String setProfilePic = sharedPreferences.getString("setProfilePic", "");

            Log.d(TAG, "Setting Profile Pic");
            Picasso.get()
                    .load(Uri.parse(setProfilePic))
                    .into(mSetProfilePic);

            mSetAccountName.setText(setAccountName);
        } else if (state == 0) {
            Log.d(TAG, "State disabled: Updating set account visuals");
            Picasso.get()
                    .load(R.drawable.frown_straight)
                    .into(mSetProfilePic);

            mSetAccountName.setText(R.string.state_disabled);
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

                //Activate Alarm
                if (sharedPreferences.getInt("state", 1) == 1 && !Functions.alarmActive) {
                    Functions.callAlarm(getApplicationContext());
                }

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

    // TODO: 28/02/2021 Populate suggestions list via http
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
        mSuggestionRecyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext(), LinearLayoutManager.HORIZONTAL, false));

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
    }

    private void hideSuggestions() {
        Log.d(TAG, "Closing suggestions menu");

        mAdapter.isClickable = true;

        Drawable unwrappedDrawable = suggestionIcon.getBackground();
        Drawable wrappedDrawable = DrawableCompat.wrap(unwrappedDrawable);
        DrawableCompat.setTint(wrappedDrawable, ContextCompat.getColor(this, R.color.white));

        suggestionLayout.setVisibility(View.GONE);
    }

    @SuppressLint("ClickableViewAccessibility")
    public void setupClearListeners() {
        RelativeLayout settingsBtn = findViewById(R.id.settingsBtn);
        ConstraintLayout mainContainer = findViewById(R.id.mainContainer);

        mainContainer.setOnTouchListener(new clearViewListener());
        settingsBtn.setOnTouchListener(new clearViewListener());
    }

    class clearViewListener implements View.OnTouchListener {
        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            mSearchBar.setIconified(true);

            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            }

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