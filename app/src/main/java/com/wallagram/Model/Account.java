package com.wallagram.Model;

public class Account {
    private String accountName;
    private String profilePicURL;

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

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }
    public void setProfilePicURL(String profilePicURL) {
        this.profilePicURL = profilePicURL;
    }
}
