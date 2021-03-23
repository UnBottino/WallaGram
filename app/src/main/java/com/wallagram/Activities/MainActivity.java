package com.wallagram.Activities;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MAIN_ACTIVITY";

    private SharedPreferences sharedPreferences;

    public static ConstraintLayout mLoadingView;

    private RelativeLayout profilePicGlow;
    private ImageView mSetProfilePic;
    private TextView mSetAccountName;
    private RelativeLayout disabledBtn;

    private LinearLayout suggestionLayout;
    private ImageView suggestionIcon;
    private boolean suggestionsOpened = false;

    private RecyclerView mSuggestionsRecyclerView;
    public static final List<SuggestionAccount> mSuggestionAccountList = new ArrayList<>();

    public androidx.appcompat.widget.SearchView mSearchBar;

    public RecyclerView mPreviousRecyclerView;
    private AccountListAdapter mPreviousAdapter;
    public List<Account> mPreviousAccountList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPreferences = getApplicationContext().getSharedPreferences("Settings", Context.MODE_PRIVATE);

        Functions.getScreenSize(this);

        //General page setup
        pageSetup();

        //Activate Alarm
        // TODO: 23/03/2021 Wrap with IF()
        Functions.findNewPostPeriodicRequest(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: Registering receivers");
        LocalBroadcastManager.getInstance(this).registerReceiver(updateSetAccountUIReceiver, new IntentFilter("update-set-account"));
        LocalBroadcastManager.getInstance(this).registerReceiver(updateSuggestionsUIReceiver, new IntentFilter("update-suggestions"));
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: Unregistering receivers");
        LocalBroadcastManager.getInstance(this).unregisterReceiver(updateSetAccountUIReceiver);
    }

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

    private final BroadcastReceiver updateSetAccountUIReceiver = new BroadcastReceiver() {
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

                    mPreviousAccountList.add(0, account);
                    mPreviousAdapter.notifyItemInserted(0);
                    mPreviousAdapter.notifyDataSetChanged();
                } else {
                    Log.d(TAG, "onReceive: Account name already in db (" + setAccountName + ")");

                    for (Account a : mPreviousAccountList) {
                        if (a.getAccountName().equalsIgnoreCase(account.getAccountName())) {
                            //Not using update because of the ordering in recyclerView
                            db.deleteAccount(a.getAccountName());
                            db.addAccount(account);

                            mPreviousAccountList.remove(a);
                            mPreviousAdapter.notifyItemRemoved(mPreviousAccountList.indexOf(a));
                            mPreviousAccountList.add(0, account);
                            mPreviousAdapter.notifyItemInserted(0);
                            mPreviousAdapter.notifyDataSetChanged();
                            break;
                        }
                    }

                    Objects.requireNonNull(mPreviousRecyclerView.getLayoutManager()).scrollToPosition(0);
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

        mPreviousAdapter.notifyItemRangeRemoved(0, mPreviousAccountList.size());
        mPreviousAdapter.notifyDataSetChanged();
        mPreviousAccountList.clear();
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
        disabledBtn.setOnClickListener(view -> Functions.popupMsg(this, new SpannableString("State Disabled"), new SpannableString(getString(R.string.state_disabled_info_msg))));

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
                    Functions.findNewPostPeriodicRequest(getApplicationContext());
                } else {
                    Functions.findNewPostSingleRequest(getApplicationContext());
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
        mPreviousRecyclerView = findViewById(R.id.accountNameList);
        mPreviousAccountList = Functions.getDBAccounts(this);
        Collections.reverse(mPreviousAccountList);

        mPreviousAdapter = new AccountListAdapter(getApplicationContext(), mPreviousAccountList);

        mPreviousAdapter.setOnDataChangeListener(accountName -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this, R.style.AlertDialogCustom);
            builder.setCancelable(true);
            builder.setTitle("Remove '" + accountName + "' from list");
            builder.setMessage("Are you sure?");
            builder.setPositiveButton("Confirm", (dialog, which) -> {
                Functions.removeDBAccountByName(MainActivity.this, accountName);

                int pos = 0;
                for (Account a : mPreviousAccountList) {
                    if (a.getAccountName().equalsIgnoreCase(accountName)) {
                        mPreviousAccountList.remove(a);
                        mPreviousAdapter.notifyItemRemoved(pos);
                        mPreviousAdapter.notifyDataSetChanged();
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

        mPreviousRecyclerView.setAdapter(mPreviousAdapter);
        mPreviousRecyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
    }

    private void setupSuggestions() {
        suggestionLayout = findViewById(R.id.suggestionsLayout);
        suggestionIcon = findViewById(R.id.suggestionIcon);
        mSuggestionsRecyclerView = findViewById(R.id.suggestionAccountList);

        Functions.fetchSuggestionsRequest(this);

        findViewById(R.id.suggestionBtn).setOnClickListener(v -> {
            Drawable unwrappedDrawable = suggestionIcon.getBackground();
            Drawable wrappedDrawable = DrawableCompat.wrap(unwrappedDrawable);

            if (!suggestionsOpened) {
                if (!mSuggestionAccountList.isEmpty()) {
                    DrawableCompat.setTint(wrappedDrawable, ContextCompat.getColor(this, R.color.purple));
                    suggestionLayout.setVisibility(View.VISIBLE);

                    suggestionsOpened = !suggestionsOpened;
                } else {
                    Functions.popupMsg(this, new SpannableString("Network Error!"), new SpannableString(getString(R.string.no_network_msg)));
                }
            } else {
                DrawableCompat.setTint(wrappedDrawable, ContextCompat.getColor(this, R.color.white));
                suggestionLayout.setVisibility(View.GONE);
                suggestionsOpened = !suggestionsOpened;
            }
        });
    }

    private final BroadcastReceiver updateSuggestionsUIReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "updateSuggestions: Broadcast received");

            boolean error = intent.getBooleanExtra("error", false);

            if (!error) {
                SuggestionListAdapter mSuggestionAdapter = new SuggestionListAdapter(getApplicationContext(), mSuggestionAccountList);
                mSuggestionsRecyclerView.setAdapter(mSuggestionAdapter);
                mSuggestionsRecyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext(), LinearLayoutManager.HORIZONTAL, false));
            } else {
                Drawable unwrappedDrawable = suggestionIcon.getBackground();
                Drawable wrappedDrawable = DrawableCompat.wrap(unwrappedDrawable);
                DrawableCompat.setTint(wrappedDrawable, ContextCompat.getColor(getApplicationContext(), R.color.orange));
            }

            LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(updateSuggestionsUIReceiver);
        }
    };

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