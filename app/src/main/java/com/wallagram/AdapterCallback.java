package com.wallagram;

public interface AdapterCallback {
    void showOffline();
    void showOnline();

    void showRemoveConfirmation(String accountType, String accountName);
    void hideSoftKeyboard();
}
