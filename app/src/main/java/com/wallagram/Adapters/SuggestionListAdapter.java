package com.wallagram.Adapters;

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
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;
import com.wallagram.Activities.MainActivity;
import com.wallagram.Model.SuggestionAccount;
import com.wallagram.R;
import com.wallagram.Utils.Functions;

import java.util.List;

public class SuggestionListAdapter extends RecyclerView.Adapter<SuggestionListAdapter.SuggestionListItemHolder> {
    private static final String TAG = "SUGGESTION_LIST_ADAPTER";

    private final List<SuggestionAccount> mSuggestionAccountList;
    private final LayoutInflater mInflater;

    private final Context mContext;

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

        Picasso.get()
                .load(Uri.parse(mSuggestionImgUrl))
                .into(holder.profilePicView);

        holder.accountNameView.setText(mSuggestionName);

        holder.itemView.setOnClickListener(v -> {
            Log.d(TAG, "Suggestion RecyclerView item clicked: (" + mSuggestionName + ")");

            MainActivity.mLoadingView.setVisibility(View.VISIBLE);

            SharedPreferences sharedPreferences = mContext.getSharedPreferences("Settings", 0);
            SharedPreferences.Editor mEditor = sharedPreferences.edit();
            mEditor.putString("searchName", mSuggestionName);
            mEditor.apply();

            //Activate Alarm
            if (sharedPreferences.getInt("state", 1) == 1 && !sharedPreferences.getBoolean("repeatingWorker", false)) {
                Functions.findNewPostPeriodicRequest(mContext);
            } else {
                Functions.findNewPostSingleRequest(mContext);
            }
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