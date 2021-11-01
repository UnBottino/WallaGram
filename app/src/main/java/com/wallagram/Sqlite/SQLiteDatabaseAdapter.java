package com.wallagram.Sqlite;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.wallagram.Model.PreviousAccount;

import java.util.ArrayList;
import java.util.List;

public class SQLiteDatabaseAdapter {
    private static final String TAG = "SQLITE_DATABASE_ADAPTER";

    SQLiteDatabaseHelper dbHelper;

    public SQLiteDatabaseAdapter(Context context) {
        dbHelper = new SQLiteDatabaseHelper(context);
    }

    public void addAccount(PreviousAccount previousAccount) {
        Log.d(TAG, "addAccount: Adding previousAccount into DB (" + previousAccount.getAccountName() + ")");

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        ContentValues values = new ContentValues();
        values.put("account_type", previousAccount.getAccountType());
        values.put("account_name", previousAccount.getAccountName());
        values.put("profile_pic_url", previousAccount.getProfilePicURL());

        db.insert(SQLiteDatabaseHelper.TABLE_ACCOUNT_NAMES, null, values);
        db.close();
    }

    public void updateProfilePicURL(String accountType, String accountName, String newProfilePicURL) {
        Log.d(TAG, "updateAccount: Updating previousAccount profilePicURL (" + accountName + ")");

        SQLiteDatabase db = dbHelper.getReadableDatabase();

        ContentValues cv = new ContentValues();
        cv.put("profile_pic_url", newProfilePicURL);

        db.update(SQLiteDatabaseHelper.TABLE_ACCOUNT_NAMES, cv, "account_type = ? AND account_name = ?", new String[]{accountType, accountName});
        db.close();
    }

    public List<PreviousAccount> getAllAccounts() {
        Log.d(TAG, "getAllAccounts");

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = SQLiteQueries.getAllAccounts();
        Cursor cursor = db.rawQuery(query, null);
        List<PreviousAccount> previousAccountList = new ArrayList<>();

        if (cursor != null) {
            cursor.moveToNext();
            for (int i = 0; i < cursor.getCount(); i++) {
                PreviousAccount previousAccount = new PreviousAccount(cursor.getString(0),
                        cursor.getString(1), cursor.getString(2));
                previousAccountList.add(previousAccount);
                cursor.moveToNext();
            }
            cursor.close();
            db.close();
            return previousAccountList;
        }

        return null;
    }

    public List<PreviousAccount> getAllInstaAccounts() {
        Log.d(TAG, "getAllInstaAccounts");

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = SQLiteQueries.getAllInstaAccounts();
        Cursor cursor = db.rawQuery(query, null);
        List<PreviousAccount> previousAccountList = new ArrayList<>();

        if (cursor != null) {
            cursor.moveToNext();
            for (int i = 0; i < cursor.getCount(); i++) {
                PreviousAccount previousAccount = new PreviousAccount(cursor.getString(0),
                        cursor.getString(1), cursor.getString(2));
                previousAccountList.add(previousAccount);
                cursor.moveToNext();
            }
            cursor.close();
            db.close();
            return previousAccountList;
        }

        return null;
    }

    public boolean checkIfAccountExists(PreviousAccount previousAccount) {
        Log.d(TAG, "checkIfAccountExists");

        boolean exists = false;

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = SQLiteQueries.getAccountByTypeAndName(previousAccount.getAccountType(), previousAccount.getAccountName());
        Cursor cursor = db.rawQuery(query, null);

        if (cursor.getCount() > 0)
            exists = true;

        cursor.close();
        db.close();
        return exists;
    }

    public void deleteAccount(String accountType, String accountName) {
        Log.d(TAG, "deleteAccount: Deleting account from DB (" + accountName + "(" + accountType + "))");

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        db.delete(SQLiteDatabaseHelper.TABLE_ACCOUNT_NAMES, "account_type = '" + accountType + "' AND account_name" + "='" + accountName + "'", null);
        db.close();
    }

    public void deleteAll() {
        Log.d(TAG, "deleteAll: Removing all accounts from table '" + SQLiteDatabaseHelper.TABLE_ACCOUNT_NAMES + "'");

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(SQLiteDatabaseHelper.TABLE_ACCOUNT_NAMES, null, null);
        db.close();
    }

    static class SQLiteDatabaseHelper extends SQLiteOpenHelper {
        private static final int DATABASE_VERSION = 25;
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
