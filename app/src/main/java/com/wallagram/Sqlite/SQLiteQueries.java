package com.wallagram.Sqlite;

public class SQLiteQueries {

    public static String createAccountsTable(){
        return "CREATE TABLE walla_accounts" +
                "(account_name TEXT NOT NULL UNIQUE, " +
                "profile_pic_url TEXT NOT NULL)";
    }

    public static String getAccountNameByName(String accountName){
        return "SELECT * FROM walla_accounts WHERE account_name LIKE '%" + accountName + "%'";
    }

    public static String getAllAccounts(){
        return "SELECT * FROM walla_accounts";
    }
}
