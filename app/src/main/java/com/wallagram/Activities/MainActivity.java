package com.wallagram.Activities;

import androidx.annotation.ColorInt;
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
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.wallagram.AdapterCallback;
import com.wallagram.Adapters.AccountListAdapter;
import com.wallagram.Adapters.SuggestionListAdapter;
import com.wallagram.Model.PreviousAccount;
import com.wallagram.Model.SuggestionAccount;
import com.wallagram.R;
import com.wallagram.Sqlite.SQLiteDatabaseAdapter;
import com.wallagram.Utils.Functions;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements AdapterCallback {
    private static final String TAG = "MAIN_ACTIVITY";

    private SharedPreferences sharedPreferences;

    public static ConstraintLayout mLoadingView;

    private RelativeLayout profilePicGlow;
    private ImageView mSetProfilePic;
    private TextView mSetAccountName;
    private RelativeLayout disabledBtn;

    private ImageView suggestionIcon;
    private boolean suggestionsOpened = false;

    private RecyclerView mSuggestionsRecyclerView;
    public static final List<SuggestionAccount> mSuggestionAccountList = new ArrayList<>();

    private final AdapterCallback mAdapterCallback = this;
    private Animation shakeAnimation;
    public RelativeLayout networkMsg;
    private boolean offline;

    public androidx.appcompat.widget.SearchView mSearchBar;

    public RecyclerView mPreviousRecyclerView;
    public LinearLayout emptyPrevious;
    private AccountListAdapter mPreviousAdapter;
    public List<PreviousAccount> mPreviousPreviousAccountList = new ArrayList<>();

    private @ColorInt
    int colorError;
    private @ColorInt
    int colorPrimary;
    private @ColorInt
    int colorPrimaryVariant;
    private @ColorInt
    int colorOnPrimary;
    private @ColorInt
    int colorSurface;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupHideKeyboard(findViewById(R.id.mainContainer));

        Log.d(TAG, "onCreate: Registering receivers");
        LocalBroadcastManager.getInstance(this).registerReceiver(updateSetAccountUIReceiver, new IntentFilter("update-set-account"));
        LocalBroadcastManager.getInstance(this).registerReceiver(updateSuggestionsUIReceiver, new IntentFilter("update-suggestions"));

        sharedPreferences = getApplicationContext().getSharedPreferences("Settings", Context.MODE_PRIVATE);

        Functions.getScreenSize(this);

        //General page setup
        pageSetup();

        //Activate Alarm
        if (sharedPreferences.getInt("state", 1) == 1 && !sharedPreferences.getString("searchName", "").equalsIgnoreCase("")) {
            Functions.findNewPostPeriodicRequest(this);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: Unregistering receivers");
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
                PreviousAccount previousAccount = new PreviousAccount(setAccountName, setProfilePic);

                if (!db.checkIfAccountExists(previousAccount)) {
                    db.addAccount(previousAccount);

                    mPreviousPreviousAccountList.add(0, previousAccount);
                    mPreviousAdapter.notifyItemInserted(0);
                    mPreviousAdapter.notifyDataSetChanged();
                } else {
                    Log.d(TAG, "onReceive: PreviousAccount name already in db (" + setAccountName + ")");

                    for (PreviousAccount a : mPreviousPreviousAccountList) {
                        if (a.getAccountName().equalsIgnoreCase(previousAccount.getAccountName())) {
                            //Not using update because of the ordering in recyclerView
                            db.deleteAccount(a.getAccountName());
                            db.addAccount(previousAccount);

                            mPreviousPreviousAccountList.remove(a);
                            mPreviousAdapter.notifyItemRemoved(mPreviousPreviousAccountList.indexOf(a));
                            mPreviousPreviousAccountList.add(0, previousAccount);
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

                if (emptyPrevious.getVisibility() == View.VISIBLE) {
                    emptyPrevious.setVisibility(View.GONE);
                }
            }

            mSetAccountName.setText(setAccountName);
            mLoadingView.setVisibility(View.INVISIBLE);
        }
    };

    private void stateChanged(int state) {
        if (state == 1) {
            Log.d(TAG, "stateChanged: State enabled: Updating set account visuals");
            profilePicGlow.setBackground(ContextCompat.getDrawable(this, R.drawable.purple_round_glow));
            mSetAccountName.setTextColor(colorPrimary);
            disabledBtn.setVisibility(View.INVISIBLE);
        } else if (state == 0) {
            Log.d(TAG, "stateChanged: State disabled: Updating set account visuals");
            profilePicGlow.setBackground(ContextCompat.getDrawable(this, R.drawable.orange_round_glow));
            mSetAccountName.setTextColor(colorError);
            disabledBtn.setVisibility(View.VISIBLE);
        }
    }

    private void clearRecentSearches() {
        Log.d(TAG, "clearRecentSearches: Update RecyclerView Received");

        mPreviousAdapter.notifyItemRangeRemoved(0, mPreviousPreviousAccountList.size());
        mPreviousAdapter.notifyDataSetChanged();
        mPreviousPreviousAccountList.clear();

        emptyPrevious.setVisibility(View.VISIBLE);
    }

    private void pageSetup() {
        getColors();

        //Network Message
        networkMsg = findViewById(R.id.networkMsg);
        shakeAnimation = AnimationUtils.loadAnimation(this, R.anim.bounce);

        //Loading View
        mLoadingView = findViewById(R.id.loadingView);
        mLoadingView.setOnClickListener(v -> {
            //Do nothing
        });

        //Settings Button
        RelativeLayout mSettingsBtn = findViewById(R.id.settingsBtn);
        mSettingsBtn.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivityForResult(intent, 59);
        });

        //Disabled Button
        disabledBtn = findViewById(R.id.disabledBtn);
        disabledBtn.setOnClickListener(view -> Functions.popupMsg(this, new SpannableString("State Disabled"), new SpannableString(getString(R.string.state_disabled_info_msg))));

        //Set Profile Information
        profilePicGlow = findViewById(R.id.profilePicGlow);

        //Auto-size set profile pic
        int screenHeight = sharedPreferences.getInt("screenHeight", 0);
        int screenWidth = sharedPreferences.getInt("screenWidth", 0);
        float ratio = ((float) screenHeight / (float) screenWidth);

        ConstraintLayout.LayoutParams lp = (ConstraintLayout.LayoutParams) profilePicGlow.getLayoutParams();
        lp.matchConstraintPercentHeight = (float) (0.15);
        lp.matchConstraintPercentWidth = (float) (ratio * 0.15);
        profilePicGlow.setLayoutParams(lp);

        mSetProfilePic = findViewById(R.id.setProfilePic);
        String setProfilePic = sharedPreferences.getString("setProfilePic", "");

        if (!setProfilePic.equalsIgnoreCase("")) {
            Picasso.get()
                    .load(Uri.parse(setProfilePic))
                    .into(mSetProfilePic);
        }

        mSetAccountName = findViewById(R.id.setAccountName);
        mSetAccountName.setText(sharedPreferences.getString("setAccountName", "No Account Set"));

        //State Color
        setupState();

        //Search Box
        setupSearchBar();

        //Previous Accounts
        setupPreviousAccounts();

        //Suggestions
        setupSuggestions();
    }

    private void setupState() {
        if (sharedPreferences.getInt("state", 1) == 0) {
            profilePicGlow.setBackground(ContextCompat.getDrawable(this, R.drawable.orange_round_glow));
            mSetAccountName.setTextColor(ContextCompat.getColor(this, R.color.orange));
            disabledBtn.setVisibility(View.VISIBLE);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupSearchBar() {
        mSearchBar = findViewById(R.id.searchBar);
        ImageView searchClose = mSearchBar.findViewById(androidx.appcompat.R.id.search_close_btn);
        searchClose.setColorFilter(colorOnPrimary, PorterDuff.Mode.SRC_ATOP);

        mSearchBar.setOnTouchListener((view, motionEvent) -> {
            mSearchBar.setIconified(false);
            mSearchBar.setBackgroundResource(R.drawable.search_bar_light);
            return true;
        });

        mSearchBar.setOnCloseListener(() -> {
            mSearchBar.setBackgroundResource(R.drawable.search_bar);
            return false;
        });

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
                mSearchBar.setBackgroundResource(R.drawable.search_bar);
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
        emptyPrevious = findViewById(R.id.emptyPrevious);

        mPreviousPreviousAccountList = Functions.getDBAccounts(this);
        Collections.reverse(mPreviousPreviousAccountList);

        /*mPreviousPreviousAccountList != null*/

        if (!mPreviousPreviousAccountList.isEmpty()) {
            emptyPrevious.setVisibility(View.GONE);
        }

        mPreviousAdapter = new AccountListAdapter(this, mPreviousPreviousAccountList, mAdapterCallback);
        mPreviousRecyclerView.setAdapter(mPreviousAdapter);
        mPreviousRecyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
    }

    private void getColors() {
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = getTheme();
        theme.resolveAttribute(R.attr.colorError, typedValue, true);
        colorError = typedValue.data;
        theme.resolveAttribute(R.attr.colorPrimary, typedValue, true);
        colorPrimary = typedValue.data;
        theme.resolveAttribute(R.attr.colorPrimaryVariant, typedValue, true);
        colorPrimaryVariant = typedValue.data;
        theme.resolveAttribute(R.attr.colorOnPrimary, typedValue, true);
        colorOnPrimary = typedValue.data;
        theme.resolveAttribute(R.attr.colorSurface, typedValue, true);
        colorSurface = typedValue.data;
    }

    private void setupSuggestions() {
        RelativeLayout mSuggestionsBtn = findViewById(R.id.suggestionBtn);
        suggestionIcon = findViewById(R.id.suggestionIcon);
        mSuggestionsRecyclerView = findViewById(R.id.suggestionAccountList);

        Functions.fetchSuggestionsRequest(this);

        Drawable unwrappedDrawable = suggestionIcon.getBackground();
        Drawable wrappedDrawable = DrawableCompat.wrap(unwrappedDrawable);

        if (!Functions.isNetworkAvailable(this)) {
            DrawableCompat.setTint(wrappedDrawable, colorError);
            showOffline();
        }

        mSuggestionsBtn.setOnClickListener(v -> {
            if (!suggestionsOpened && !mSuggestionAccountList.isEmpty()) {
                mSuggestionsBtn.setBackgroundTintList(ColorStateList.valueOf(colorPrimaryVariant));
                DrawableCompat.setTint(wrappedDrawable, colorSurface);
                mSuggestionsRecyclerView.setVisibility(View.VISIBLE);
                suggestionsOpened = !suggestionsOpened;
            } else if (suggestionsOpened) {
                mSuggestionsBtn.setBackgroundTintList(ColorStateList.valueOf(colorSurface));
                DrawableCompat.setTint(wrappedDrawable, colorPrimary);
                mSuggestionsRecyclerView.setVisibility(View.GONE);
                suggestionsOpened = !suggestionsOpened;
            } else if (offline) {
                showOffline();
            }
        });
    }

    private final BroadcastReceiver updateSuggestionsUIReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "updateSuggestions: Broadcast received");

            boolean error = intent.getBooleanExtra("error", false);

            Drawable unwrappedDrawable = suggestionIcon.getBackground();
            Drawable wrappedDrawable = DrawableCompat.wrap(unwrappedDrawable);
            if (!error) {
                showOnline();
                SuggestionListAdapter mSuggestionAdapter = new SuggestionListAdapter(getBaseContext(), mSuggestionAccountList, mAdapterCallback);
                mSuggestionsRecyclerView.setAdapter(mSuggestionAdapter);
                mSuggestionsRecyclerView.setLayoutManager(new LinearLayoutManager(getBaseContext(), LinearLayoutManager.HORIZONTAL, false));
            } else {
                DrawableCompat.setTint(wrappedDrawable, colorError);
            }

            LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(updateSuggestionsUIReceiver);
        }
    };

    @Override
    public void showOffline() {
        if (!offline) {
            networkMsg.setVisibility(View.VISIBLE);
            mSearchBar.setEnabled(false);
            offline = true;
        } else {
            networkMsg.startAnimation(shakeAnimation);
        }
    }

    @Override
    public void showOnline() {
        networkMsg.setVisibility(View.GONE);
        mSearchBar.setEnabled(true);
        offline = false;
    }

    @Override
    public void showRemoveConfirmation(String accountName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setCancelable(true);

        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_warning, null);
        builder.setView(dialogView);
        TextView infoTitle = dialogView.findViewById(R.id.infoTitle);
        TextView infoMsg = dialogView.findViewById(R.id.infoMsg);

        SpannableStringBuilder removePreviousString = new SpannableStringBuilder();
        removePreviousString.append("Remove");
        SpannableString coloredAccountName = new SpannableString(" '" + accountName + "' ");
        coloredAccountName.setSpan(new ForegroundColorSpan(colorPrimary), 0, accountName.length() + 3, 0);
        removePreviousString.append(coloredAccountName);
        removePreviousString.append("from previous searches");

        infoTitle.setText(removePreviousString, TextView.BufferType.SPANNABLE);
        infoMsg.setText(R.string.warning_msg);

        AlertDialog dialog = builder.create();
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.show();

        TextView cancelBtn = dialogView.findViewById(R.id.cancelBtn);
        TextView confirmBtn = dialogView.findViewById(R.id.confirmBtn);

        confirmBtn.setOnClickListener(view -> {
            Functions.removeDBAccountByName(MainActivity.this, accountName);

            int pos = 0;
            for (PreviousAccount a : mPreviousPreviousAccountList) {
                if (a.getAccountName().equalsIgnoreCase(accountName)) {
                    mPreviousPreviousAccountList.remove(a);
                    mPreviousAdapter.notifyItemRemoved(pos);
                    mPreviousAdapter.notifyDataSetChanged();
                    break;
                }
                pos++;
            }

            dialog.cancel();
        });

        cancelBtn.setOnClickListener(view -> dialog.cancel());
    }

    @Override
    public void hideSoftKeyboard() {
        mSearchBar.setIconified(true);
        mSearchBar.setBackgroundResource(R.drawable.search_bar);
        getWindow().getDecorView().clearFocus();
    }

    @SuppressLint("ClickableViewAccessibility")
    public void setupHideKeyboard(View view) {
        setOnTouchHideKeyboard(view);

        for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
            View innerView = ((ViewGroup) view).getChildAt(i);
            setOnTouchHideKeyboard(innerView);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    public void setOnTouchHideKeyboard(View view) {
        if (!(view instanceof SearchView)) {
            view.setOnTouchListener((v, event) -> {
                hideSoftKeyboard();
                return false;
            });
        }
    }
}