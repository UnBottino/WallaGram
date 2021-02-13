package com.wallagram.Adapters;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.wallagram.Connectors.ForegroundService;
import com.wallagram.MainActivity;
import com.wallagram.Model.Account;
import com.wallagram.R;
import com.squareup.picasso.Picasso;

import java.util.List;

public class AccountListAdapter extends RecyclerView.Adapter<AccountListAdapter.AccountListItemHolder> {

    private final List<Account> mAccountList;
    private final LayoutInflater mInflater;

    private final Context mContext;
    private SharedPreferences mSharedPreferences;
    private SharedPreferences.Editor mEditor;

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

        //Picasso
        Picasso.get()
                .load(Uri.parse(mProfilePicURL))
                .into(holder.profilePicView);

        holder.accountNameView.setText(mAccountName);

        holder.itemView.setOnClickListener(v -> {
            MainActivity.mLoadingView.setVisibility(View.VISIBLE);

            mSharedPreferences = mContext.getSharedPreferences("Settings", 0);
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
}