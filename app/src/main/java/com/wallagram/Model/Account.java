package com.wallagram.Model;

public class Account {
    private final String accountName;
    private final String profilePicURL;

    public Account(String accountName, String profilePicURL) {
        this.accountName = accountName;
        this.profilePicURL = profilePicURL;
    }

    public String getAccountName() {
        return accountName;
    }

    public String getProfilePicURL() {
        return profilePicURL;
    }
}
