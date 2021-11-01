package com.wallagram.Model;

public class PreviousAccount {
    private final String accountType;
    private final String accountName;
    private final String profilePicURL;

    public PreviousAccount(String accountType, String accountName, String profilePicURL) {
        this.accountType = accountType;
        this.accountName = accountName;
        this.profilePicURL = profilePicURL;
    }

    public String getAccountType() {
        return accountType;
    }

    public String getAccountName() { return accountName; }

    public String getProfilePicURL() {
        return profilePicURL;
    }
}
