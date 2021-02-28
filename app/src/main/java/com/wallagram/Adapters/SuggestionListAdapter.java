package com.wallagram.Adapters;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;
import com.wallagram.Activities.MainActivity;
import com.wallagram.Connectors.ForegroundService;
import com.wallagram.Model.Account;
import com.wallagram.Model.SuggestionAccount;
import com.wallagram.R;

import java.util.List;

public class SuggestionListAdapter extends RecyclerView.Adapter<SuggestionListAdapter.SuggestionListItemHolder> {
    private static final String TAG = "SUGGESTION_LIST_ADAPTER";

    private final List<SuggestionAccount> mSuggestionAccountList;
    private final LayoutInflater mInflater;

    private final Context mContext;
    private SharedPreferences.Editor mEditor;

    @NonNull
    @Override
    public SuggestionListItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View mItemView = mInflater.inflate(R.layout.account_list_item, parent, false);
        return new SuggestionListItemHolder(mItemView, this);
    }

    @Override
    public void onBindViewHolder(final SuggestionListItemHolder holder, int position) {
        final String mAccountName = mSuggestionAccountList.get(position).getAccountName();
        final int mProfilePicID = mSuggestionAccountList.get(position).getProfilePicID();

        holder.profilePicView.setImageResource(mProfilePicID);
        holder.accountNameView.setText(mAccountName);

        holder.itemView.setOnClickListener(v -> {
            Log.d(TAG, "Suggestion RecyclerView item clicked: (" + mAccountName + ")");

            MainActivity.mLoadingView.setVisibility(View.VISIBLE);

            mEditor = mContext.getSharedPreferences("Settings", 0).edit();
            mEditor.putString("searchName", mAccountName);
            mEditor.apply();

            Intent i = new Intent(mContext, ForegroundService.class);
            i.setAction(ForegroundService.ACTION_START_FOREGROUND_SERVICE);
            mContext.startForegroundService(i);
        });
    }

    @Override
    public int getItemCount() {
        return mSuggestionAccountList.size();
    }

    public SuggestionListAdapter(Context context, List<SuggestionAccount> accountList) {
        mInflater = LayoutInflater.from(context);
        this.mSuggestionAccountList = accountList;

        mContext = context;
    }

    static class SuggestionListItemHolder extends RecyclerView.ViewHolder {
        final TextView accountNameView;
        final ImageView profilePicView;

        final SuggestionListAdapter mAdapter;

        SuggestionListItemHolder(View itemView, SuggestionListAdapter adapter) {
            super(itemView);

            accountNameView = itemView.findViewById(R.id.accountName);
            profilePicView = itemView.findViewById(R.id.profilePic);

            this.mAdapter = adapter;
        }
    }
}