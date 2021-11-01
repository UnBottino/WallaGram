package com.wallagram.Adapters;

import android.app.Activity;
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

import com.wallagram.Activities.MainActivity;
import com.wallagram.AdapterCallback;
import com.wallagram.Model.PreviousAccount;
import com.wallagram.R;
import com.squareup.picasso.Picasso;
import com.wallagram.Utils.Functions;

import java.util.List;

public class AccountListAdapter extends RecyclerView.Adapter<AccountListAdapter.AccountListItemHolder> {
    private static final String TAG = "ACCOUNT_LIST_ADAPTER";

    private final List<PreviousAccount> mPreviousAccountList;
    private final LayoutInflater mInflater;
    private final Activity mContext;
    final AdapterCallback mAdapterCallback;

    @NonNull
    @Override
    public AccountListItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View mItemView = mInflater.inflate(R.layout.account_list_item, parent, false);
        return new AccountListItemHolder(mItemView, this);
    }

    @Override
    public void onBindViewHolder(final AccountListItemHolder holder, int position) {
        final String mAccountType = mPreviousAccountList.get(position).getAccountType();
        final String mAccountName = mPreviousAccountList.get(position).getAccountName();
        final String mProfilePicURL = mPreviousAccountList.get(position).getProfilePicURL();

        Picasso.get()
                .load(Uri.parse(mProfilePicURL))
                .into(holder.mProfilePicView);

        holder.mAccountNameView.setText(mAccountName);

        holder.itemView.setOnClickListener(v -> {
            Log.d(TAG, "RecyclerView item clicked: (" + mAccountName + ")");

            mAdapterCallback.hideSoftKeyboard();

            if (Functions.isNetworkAvailable(mContext)) {
                MainActivity.mLoadingView.setVisibility(View.VISIBLE);

                SharedPreferences sharedPreferences = mContext.getSharedPreferences("Settings", 0);
                SharedPreferences.Editor mEditor = sharedPreferences.edit();
                mEditor.putString("searchName", mAccountName);
                mEditor.apply();

                String mMode = sharedPreferences.getString("setMode", "Insta");

                if(mMode.equalsIgnoreCase("Insta")){
                    //Activate Alarm
                    if (sharedPreferences.getInt("state", 1) == 1 && !sharedPreferences.getBoolean("repeatingWorker", false)) {
                        Functions.findNewPostPeriodicRequest(mContext);
                    } else {
                        Functions.findNewPostSingleRequest(mContext);
                    }
                } else if (mMode.equalsIgnoreCase("Reddit")){
                    //Activate Alarm
                    if (sharedPreferences.getInt("state", 1) == 1 && !sharedPreferences.getBoolean("repeatingWorker", false)) {
                        Functions.findNewRedditPostPeriodicRequest(mContext);
                    } else {
                        Functions.findNewRedditPostSingleRequest(mContext);
                    }
                }

                mAdapterCallback.showOnline();
            } else {
                Log.d(TAG, "No Network Connection");
                mAdapterCallback.showOffline();
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            Log.d(TAG, "RecyclerView item long clicked: (" + mAccountName + ")");
            mAdapterCallback.hideSoftKeyboard();
            mAdapterCallback.showRemoveConfirmation(mAccountType, mAccountName);

            return true;
        });
    }

    @Override
    public int getItemCount() {
        return mPreviousAccountList.size();
    }

    public AccountListAdapter(Activity context, List<PreviousAccount> previousAccountList, AdapterCallback callback) {
        this.mInflater = LayoutInflater.from(context);
        this.mContext = context;
        this.mPreviousAccountList = previousAccountList;
        this.mAdapterCallback = callback;
    }

    static class AccountListItemHolder extends RecyclerView.ViewHolder {
        final TextView mAccountNameView;
        final ImageView mProfilePicView;
        final AccountListAdapter mAdapter;

        AccountListItemHolder(View itemView, AccountListAdapter adapter) {
            super(itemView);
            this.mAccountNameView = itemView.findViewById(R.id.accountName);
            this.mProfilePicView = itemView.findViewById(R.id.profilePic);
            this.mAdapter = adapter;
        }
    }
}