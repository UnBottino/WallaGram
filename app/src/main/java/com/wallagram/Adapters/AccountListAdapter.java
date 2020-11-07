package com.wallagram.Adapters;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.wallagram.Model.Account;
import com.wallagram.R;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class AccountListAdapter extends RecyclerView.Adapter<AccountListAdapter.AccountListItemHolder> {

    private List<Account> mAccountList;
    private List<Account> mAccountListFull;
    private LayoutInflater mInflater;

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
    }

    @Override
    public int getItemCount() {
        return mAccountList.size();
    }

    public AccountListAdapter(Context context, List<Account> accountList) {
        mInflater = LayoutInflater.from(context);
        mAccountListFull = new ArrayList<>(accountList);
        this.mAccountList = accountList;
    }

    static class AccountListItemHolder extends RecyclerView.ViewHolder {
        final TextView accountNameView;
        final ImageView profilePicView;

        final com.wallagram.Adapters.AccountListAdapter mAdapter;

        AccountListItemHolder(View itemView, com.wallagram.Adapters.AccountListAdapter adapter) {
            super(itemView);

            accountNameView = itemView.findViewById(R.id.accountName);
            profilePicView = itemView.findViewById(R.id.profilePic);

            this.mAdapter = adapter;
        }
    }
}