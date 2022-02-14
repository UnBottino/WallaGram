package com.wallagram.Adapters;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;
import com.wallagram.Activities.MainActivity;
import com.wallagram.AdapterCallback;
import com.wallagram.Model.SuggestionAccount;
import com.wallagram.R;
import com.wallagram.Utils.Functions;

import java.util.List;

public class SuggestionListAdapter extends RecyclerView.Adapter<SuggestionListAdapter.SuggestionListItemHolder> {
    private static final String TAG = "SUGGESTION_LIST_ADAPTER";

    private final List<SuggestionAccount> mSuggestionAccountList;
    private final Application mApplication;
    private final LayoutInflater mInflater;
    private final Context mContext;
    private SharedPreferences mSharedPreferences;
    private final AdapterCallback mAdapterCallback;

    @NonNull
    @Override
    public SuggestionListItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View mItemView = mInflater.inflate(R.layout.suggestion_account_list_item, parent, false);
        return new SuggestionListItemHolder(mItemView, this);
    }

    @Override
    public void onBindViewHolder(final SuggestionListItemHolder holder, int position) {
        final String mSuggestionName = mSuggestionAccountList.get(position).getSuggestionName();
        final String mSuggestionImgUrl = mSuggestionAccountList.get(position).getSuggestionImgUrl();

        int deviceWidth = mSharedPreferences.getInt("screenWidth", 0);
        holder.suggestionItemContainer.getLayoutParams().width = (int) ((deviceWidth - 125) / 2.8);

        Picasso.get()
                .load(Uri.parse(mSuggestionImgUrl))
                .into(holder.profilePicView);

        holder.accountNameView.setText(mSuggestionName);

        holder.itemView.setOnClickListener(v -> {
            Log.d(TAG, "Suggestion RecyclerView item clicked: (" + mSuggestionName + ")");

            mAdapterCallback.hideSoftKeyboard();

            if (Functions.isNetworkAvailable(mApplication)) {
                MainActivity.mLoadingView.setVisibility(View.VISIBLE);

                mSharedPreferences = mContext.getSharedPreferences("Settings", 0);
                SharedPreferences.Editor mEditor = mSharedPreferences.edit();
                mEditor.putString("searchName", mSuggestionName);
                mEditor.apply();

                //Activate Alarm
                if (mSharedPreferences.getInt("state", 1) == 1 && !mSharedPreferences.getBoolean("repeatingWorker", false)) {
                    Functions.findNewPostPeriodicRequest(mContext);
                } else {
                    Functions.findNewPostSingleRequest(mContext);
                }
                mAdapterCallback.showOnline();
            } else {
                Log.d(TAG, "No Network Connection");
                mAdapterCallback.showOffline();
            }
        });
    }

    @Override
    public int getItemCount() {
        return mSuggestionAccountList.size();
    }

    public SuggestionListAdapter(Application application, List<SuggestionAccount> accountList, AdapterCallback callback) {
        this.mApplication = application;
        this.mInflater = LayoutInflater.from(application.getBaseContext());
        this.mContext = application.getBaseContext();
        this.mSharedPreferences = mContext.getSharedPreferences("Settings", Context.MODE_PRIVATE);
        this.mSuggestionAccountList = accountList;
        this.mAdapterCallback = callback;
    }

    static class SuggestionListItemHolder extends RecyclerView.ViewHolder {
        final ConstraintLayout suggestionItemContainer;
        final TextView accountNameView;
        final ImageView profilePicView;
        final SuggestionListAdapter mAdapter;

        SuggestionListItemHolder(View itemView, SuggestionListAdapter adapter) {
            super(itemView);
            this.suggestionItemContainer = itemView.findViewById(R.id.suggestionItemContainer);
            this.accountNameView = itemView.findViewById(R.id.accountName);
            this.profilePicView = itemView.findViewById(R.id.profilePic);
            this.mAdapter = adapter;
        }
    }
}