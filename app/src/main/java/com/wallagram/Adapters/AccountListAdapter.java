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

import com.wallagram.Activities.MainActivity;
import com.wallagram.Model.Account;
import com.wallagram.R;
import com.squareup.picasso.Picasso;
import com.wallagram.Utils.Functions;

import java.util.List;

public class AccountListAdapter extends RecyclerView.Adapter<AccountListAdapter.AccountListItemHolder> {
    private static final String TAG = "ACCOUNT_LIST_ADAPTER";

    public boolean isClickable = true;

    private final List<Account> mAccountList;
    private final LayoutInflater mInflater;

    private final Context mContext;

    @NonNull
    @Override
    public AccountListItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View mItemView = mInflater.inflate(R.layout.account_list_item, parent, false);
        return new AccountListItemHolder(mItemView, this);
    }

    @Override
    public void onBindViewHolder(final AccountListItemHolder holder, int position) {
        final String mAccountName = mAccountList.get(position).getAccountName();
        final String mProfilePicURL = mAccountList.get(position).getProfilePicURL();

        Picasso.get()
                .load(Uri.parse(mProfilePicURL))
                .into(holder.profilePicView);

        holder.accountNameView.setText(mAccountName);

        holder.itemView.setOnClickListener(v -> {
            if (isClickable) {
                Log.d(TAG, "onBindViewHolder: RecyclerView item clicked: (" + mAccountName + ")");

                if (Functions.isNetworkAvailable(mContext)) {
                    MainActivity.mLoadingView.setVisibility(View.VISIBLE);

                    SharedPreferences sharedPreferences = mContext.getSharedPreferences("Settings", 0);
                    SharedPreferences.Editor mEditor = sharedPreferences.edit();
                    mEditor.putString("searchName", mAccountName);
                    mEditor.apply();

                    //Activate Alarm
                    if (sharedPreferences.getInt("state", 1) == 1 && !sharedPreferences.getBoolean("repeatingWorker", false)) {
                        Functions.findNewPostPeriodicRequest(mContext);
                    } else {
                        Functions.findNewPostSingleRequest(mContext);
                    }
                } else {
                    Log.d(TAG, "onBindViewHolder: No Network Connection");
                    Functions.showNotification(mContext, "Search Failure", "No network connection found");
                }
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            Log.d(TAG, "onBindViewHolder: RecyclerView item long clicked: (" + mAccountName + ")");

            if (isClickable) {
                if (mOnDataChangeListener != null) {
                    mOnDataChangeListener.updateAccountList(mAccountName);
                }
            }

            return true;
        });
    }

    @Override
    public int getItemCount() {
        return mAccountList.size();
    }

    public AccountListAdapter(Context context, List<Account> accountList) {
        mInflater = LayoutInflater.from(context);
        this.mAccountList = accountList;

        mContext = context;
    }

    static class AccountListItemHolder extends RecyclerView.ViewHolder {
        final TextView accountNameView;
        final ImageView profilePicView;

        final AccountListAdapter mAdapter;

        AccountListItemHolder(View itemView, AccountListAdapter adapter) {
            super(itemView);

            accountNameView = itemView.findViewById(R.id.accountName);
            profilePicView = itemView.findViewById(R.id.profilePic);

            this.mAdapter = adapter;
        }
    }

    public interface OnDataChangeListener {
        void updateAccountList(String accountName);
    }

    OnDataChangeListener mOnDataChangeListener;

    public void setOnDataChangeListener(OnDataChangeListener onDataChangeListener) {
        mOnDataChangeListener = onDataChangeListener;
    }
}