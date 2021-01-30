package com.wallagram.Login;

public interface AuthenticationListener {
    void onTokenReceived(String auth_token);
}
