package com.wallagram.Sqlite;

public class SQLiteQueries {
    public static String createAccountsTable() {
        return "CREATE TABLE walla_accounts" +
                "(account_type TEXT NOT NULL, " +
                "account_name TEXT NOT NULL, " +
                "profile_pic_url TEXT NOT NULL)";
    }

    public static String getAccountByTypeAndName(String accountType, String accountName) {
        return "SELECT * FROM walla_accounts WHERE account_type = '" + accountType + "' AND account_name = '" + accountName + "' COLLATE NOCASE";
    }

    public static String getAllAccounts() {
        return "SELECT * FROM walla_accounts";
    }

    public static String getAllInstaAccounts() {
        return "SELECT * FROM walla_accounts WHERE account_type = 'Insta'";
    }
}
