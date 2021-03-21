package com.wallagram.Activities;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.lifecycle.LifecycleObserver;
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
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.SpannableString;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.wallagram.Adapters.AccountListAdapter;
import com.wallagram.Adapters.SuggestionListAdapter;
import com.wallagram.Model.Account;
import com.wallagram.Model.SuggestionAccount;
import com.wallagram.R;
import com.wallagram.Sqlite.SQLiteDatabaseAdapter;
import com.wallagram.Utils.Functions;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements LifecycleObserver {
    private static final String TAG = "MAIN_ACTIVITY";

    private SharedPreferences sharedPreferences;

    public static ConstraintLayout mLoadingView;

    public RelativeLayout profilePicGlow;
    public ImageView mSetProfilePic;
    public TextView mSetAccountName;
    public RelativeLayout disabledBtn;

    public androidx.appcompat.widget.SearchView mSearchBar;

    public RecyclerView mRecyclerView;
    public AccountListAdapter mAdapter;
    public List<Account> mDBAccountList = new ArrayList<>();

    private boolean suggestionsOpened = false;
    private LinearLayout suggestionLayout;
    private ImageView suggestionIcon;
    private static RecyclerView mSuggestionsRecyclerView;

    private static final List<SuggestionAccount> mSuggestionAccountList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Register update UI broadcast receiver
        LocalBroadcastManager.getInstance(this).registerReceiver(updateUIReceiver, new IntentFilter("custom-event-name"));

        sharedPreferences = getApplicationContext().getSharedPreferences("Settings", Context.MODE_PRIVATE);

        Functions.getScreenSize(this);

        //General page setup
        pageSetup();

        //Activate Alarm
        Functions.findNewPostRequest(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        PowerManager pm = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
        if (pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName())) {
            Log.d(TAG, "onStart: Asking to deactivate optimization");

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
        Log.d(TAG, "onDestroy: Unregistering updateUIReceiver");
        LocalBroadcastManager.getInstance(this).unregisterReceiver(updateUIReceiver);
    }

    private final BroadcastReceiver updateUIReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive: Broadcast received");

            String setAccountName = sharedPreferences.getString("setAccountName", "");
            String setProfilePic = sharedPreferences.getString("setProfilePic", "");

            boolean error = intent.getBooleanExtra("error", false);

            if (error) {
                Log.d(TAG, "onReceive: Setting Error Profile Pic");
                Picasso.get()
                        .load(R.drawable.frown_straight)
                        .into(mSetProfilePic);

                Log.d(TAG, "onReceive: Setting Error Display Name");
            } else {
                SQLiteDatabaseAdapter db = new SQLiteDatabaseAdapter(getApplicationContext());
                Account account = new Account(setAccountName, setProfilePic);

                if (!db.checkIfAccountExists(account)) {
                    db.addAccount(account);

                    mDBAccountList.add(0, account);
                    mAdapter.notifyItemInserted(0);
                    mAdapter.notifyDataSetChanged();
                } else {
                    Log.d(TAG, "onReceive: Account name already in db (" + setAccountName + ")");

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

                    Objects.requireNonNull(mRecyclerView.getLayoutManager()).scrollToPosition(0);
                }

                Log.d(TAG, "onReceive: Setting Current Profile Pic");
                Picasso.get()
                        .load(Uri.parse(setProfilePic))
                        .into(mSetProfilePic);

                Log.d(TAG, "onReceive: Setting Current Display Name");
            }

            mSetAccountName.setText(setAccountName);
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

    private void stateChanged(int state) {
        if (state == 1) {
            Log.d(TAG, "stateChanged: State enabled: Updating set account visuals");
            profilePicGlow.setBackground(ContextCompat.getDrawable(this, R.drawable.purple_round_glow));
            mSetAccountName.setTextColor(ContextCompat.getColor(this, R.color.purple));
            disabledBtn.setVisibility(View.GONE);
        } else if (state == 0) {
            Log.d(TAG, "stateChanged: State disabled: Updating set account visuals");
            profilePicGlow.setBackground(ContextCompat.getDrawable(this, R.drawable.orange_round_glow));
            mSetAccountName.setTextColor(ContextCompat.getColor(this, R.color.orange));
            disabledBtn.setVisibility(View.VISIBLE);
        }
    }

    private void clearRecentSearches() {
        Log.d(TAG, "clearRecentSearches: Update RecyclerView Received");

        mAdapter.notifyItemRangeRemoved(0, mDBAccountList.size());
        mAdapter.notifyDataSetChanged();
        mDBAccountList.clear();
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

        //Disabled Button
        disabledBtn = findViewById(R.id.disabledBtn);
        disabledBtn.setOnClickListener(view -> Functions.popupMsg(this, new SpannableString("State Disabled"), new SpannableString(getString(R.string.stateDisabledInfoMsg))));

        //Set Profile Information
        profilePicGlow = findViewById(R.id.profilePicGlow);
        mSetProfilePic = findViewById(R.id.setProfilePic);

        String setProfilePic = sharedPreferences.getString("setProfilePic", "");

        if (!setProfilePic.equalsIgnoreCase("")) {
            Picasso.get()
                    .load(Uri.parse(setProfilePic))
                    .into(mSetProfilePic);
        }

        mSetAccountName = findViewById(R.id.SetAccountName);
        mSetAccountName.setText(sharedPreferences.getString("setAccountName", "No Account Set"));

        //State Color
        setupState();

        //Search Box
        setupSearchBar();

        //Previous Accounts
        setupPreviousAccounts();

        //Suggestions
        setupSuggestions();

        //Clear Listeners
        setupClearListeners();
    }

    private void setupState() {
        if (sharedPreferences.getInt("state", 1) == 0) {
            profilePicGlow.setBackground(ContextCompat.getDrawable(this, R.drawable.orange_round_glow));
            mSetAccountName.setTextColor(ContextCompat.getColor(this, R.color.orange));
            disabledBtn.setVisibility(View.VISIBLE);
        }
    }

    private void setupSearchBar() {
        mSearchBar = findViewById(R.id.searchBar);
        EditText searchEditText = mSearchBar.findViewById(androidx.appcompat.R.id.search_src_text);
        searchEditText.setHintTextColor(ContextCompat.getColor(this, R.color.dark_white));

        mSearchBar.setOnClickListener(v -> mSearchBar.setIconified(false));

        mSearchBar.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Log.d(TAG, "onQueryTextSubmit: Searching for: " + query);

                mLoadingView.setVisibility(View.VISIBLE);

                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("searchName", query);
                editor.apply();

                mSearchBar.setQuery("", false);
                mSearchBar.setIconified(true);
                mSearchBar.clearFocus();

                //Activate Alarm
                if (sharedPreferences.getInt("state", 1) == 1 && !sharedPreferences.getBoolean("repeatingWorker", false)) {
                    Functions.findNewPostRequest(getApplicationContext());
                } else {
                    Functions.setWallpaperRequest(getApplicationContext());
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

    private void setupSuggestions() {
        suggestionLayout = findViewById(R.id.suggestionsLayout);
        suggestionIcon = findViewById(R.id.suggestionIcon);
        mSuggestionsRecyclerView = findViewById(R.id.suggestionAccountList);

        new FetchSuggestions(this).execute();

        findViewById(R.id.suggestionBtn).setOnClickListener(v -> {
            if (!suggestionsOpened) {
                showSuggestions();
            } else {
                hideSuggestions();
            }
            suggestionsOpened = !suggestionsOpened;
        });
    }

    private static final class FetchSuggestions extends AsyncTask<Void, Void, String> {
        private final WeakReference<MainActivity> activityReference;

        FetchSuggestions(MainActivity context) {
            activityReference = new WeakReference<>(context);
        }

        @Override
        protected String doInBackground(Void... params) {
            HttpURLConnection connection = null;
            BufferedReader reader = null;

            Log.d(TAG, "doInBackground: Fetching suggestions from cloud db");

            String urlString = "https://utzvkb8roc.execute-api.eu-west-1.amazonaws.com/prod_v2/suggestions";
            try {
                URL url = new URL(urlString);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                InputStream stream = connection.getInputStream();

                reader = new BufferedReader(new InputStreamReader(stream));
            } catch (Exception e) {
                Log.e(TAG, "doInBackground: Error connecting to URL: " + urlString);
            }

            try {
                assert reader != null;
                JSONObject jsonObject = new JSONObject(reader.readLine());
                JSONArray jsonArray = jsonObject.getJSONArray("body");

                mSuggestionAccountList.clear();

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject suggestion = jsonArray.getJSONObject(i);

                    String suggestionName = suggestion.getString("suggestion_name");
                    String suggestionImgUrl = suggestion.getString("suggestion_img_url");

                    SuggestionAccount a = new SuggestionAccount(suggestionName, suggestionImgUrl);
                    mSuggestionAccountList.add(a);
                }
            } catch (Exception e) {
                Log.e(TAG, "doInBackground: Failed to parse Json");
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            return "Executed";
        }

        @Override
        protected void onPostExecute(String result) {
            MainActivity activity = activityReference.get();
            if (activity == null || activity.isFinishing()) return;

            SuggestionListAdapter mSuggestionAdapter = new SuggestionListAdapter(activity, mSuggestionAccountList);
            mSuggestionsRecyclerView.setAdapter(mSuggestionAdapter);
            mSuggestionsRecyclerView.setLayoutManager(new LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false));
        }
    }

    private void showSuggestions() {
        Log.d(TAG, "showSuggestions: Opening suggestions menu");

        Drawable unwrappedDrawable = suggestionIcon.getBackground();
        Drawable wrappedDrawable = DrawableCompat.wrap(unwrappedDrawable);
        DrawableCompat.setTint(wrappedDrawable, ContextCompat.getColor(this, R.color.purple));

        suggestionLayout.setVisibility(View.VISIBLE);
    }

    private void hideSuggestions() {
        Log.d(TAG, "hideSuggestions: Closing suggestions menu");

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
}