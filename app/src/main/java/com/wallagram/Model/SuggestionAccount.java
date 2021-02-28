package com.wallagram.Model;

public class SuggestionAccount {
    private final String accountName;
    private final int profilePicID;

    public SuggestionAccount(String accountName, int profilePicID) {
        this.accountName = accountName;
        this.profilePicID = profilePicID;
    }

    public String getAccountName() {
        return accountName;
    }

    public int getProfilePicID() {
        return profilePicID;
    }
}
