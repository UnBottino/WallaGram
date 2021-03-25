package com.wallagram;

public interface AdapterCallback {
    void showOffline();
    void showOnline();

    void showRemoveConfirmation(String accountName);
    void hideSoftKeyboard();
}
