package com.wallagram.Sqlite;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.wallagram.Model.Account;

import java.util.ArrayList;
import java.util.List;

public class SQLiteDatabaseAdapter {

    SQLiteDatabaseHelper dbHelper;

    public SQLiteDatabaseAdapter(Context context) {
        dbHelper = new SQLiteDatabaseHelper(context);
    }

    public void addAccount(Account account) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        ContentValues values = new ContentValues();
        values.put("account_name", account.getAccountName());
        values.put("profile_pic_url", account.getProfilePicURL());

        db.insert(SQLiteDatabaseHelper.TABLE_ACCOUNT_NAMES, null, values);
        db.close();
    }

    public List<Account> getAllAccounts() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = SQLiteQueries.getAllAccounts();
        Cursor cursor = db.rawQuery(query, null);
        List<Account> accountList = new ArrayList<>();

        if(cursor != null){
            cursor.moveToNext();
            for(int i = 0; i < cursor.getCount(); i++){
                Account account = new Account(cursor.getString(0),
                cursor.getString(1));
                accountList.add(account);
                cursor.moveToNext();
            }
            cursor.close();
            db.close();
            return accountList;
        }

        return null;
    }

    public boolean checkIfAccountExists(Account account){
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = SQLiteQueries.getAccountNameByName(account.getAccountName());
        Cursor cursor = db.rawQuery(query, null);

        if(cursor.getCount() <= 0){
            cursor.close();
            return false;
        }

        cursor.close();
        db.close();
        return true;
    }

    public void deleteAccount(String name) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        db.delete(SQLiteDatabaseHelper.TABLE_ACCOUNT_NAMES, "account_name" + "='" + name + "'", null);
        db.close();
    }

    static class SQLiteDatabaseHelper  extends SQLiteOpenHelper {
        private static final int DATABASE_VERSION = 23;
        private static final String DATABASE_NAME = "WallaGram";
        private static final String TABLE_ACCOUNT_NAMES = "walla_accounts";

        public SQLiteDatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(SQLiteQueries.createAccountsTable());
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_ACCOUNT_NAMES);
            onCreate(db);
        }
    }
}
