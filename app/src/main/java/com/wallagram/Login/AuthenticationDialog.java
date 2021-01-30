package com.wallagram.Login;

import android.app.Dialog;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;

import com.wallagram.R;

public class AuthenticationDialog extends Dialog {
    private AuthenticationListener listener;
    private final String request_url;
    private final String redirect_url;
    public AuthenticationDialog(@NonNull Context context, AuthenticationListener listener) {
        super(context);
        this.listener = listener;
        this.redirect_url = context.getResources().getString(R.string.redirect_url);
        this.request_url = "https://api.instagram.com/oauth/authorize" +
                "?client_id=254177392730519" +
                "&redirect_uri=https://instagram.com/" +
                "&scope=user_profile,user_media" +
                "&response_type=code";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.auth_dialog);
        initializeWebView();
    }

    private void initializeWebView() {
        WebView webView = findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.loadUrl(request_url);
        webView.setWebViewClient(webViewClient);
    }

    WebViewClient webViewClient = new WebViewClient() {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (url.startsWith(redirect_url)) {
                AuthenticationDialog.this.dismiss();
                return true;
            }
            return false;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);

            if (url.contains("code%3D")) {
                System.out.println("============================URL=======================");
                System.out.println(url);

                String access_token = url;
                access_token = access_token.substring(access_token.lastIndexOf("%3D") + 3, access_token.lastIndexOf("%23"));
                System.out.println("============================DIALOG_TOKEN=======================");
                Log.e("code%3D", access_token);
                listener.onTokenReceived("IGQVJYS3lwUU1QdVVoWEtheEd2aHVldXAyWG5yZAHBCelM4QU9tSHRJYi00R2Jqa0FzUzE3S2JCWVd2aGJNUmczbGp0NWRxVXpGbm8tZAkZAiaTN1UDdwWG9LeFEtYmdlODN1ekNNcGZAqbXdYc1ZAtd3JKVXhEbmVuZADRCMENN");
                dismiss();
            } else if (url.contains("?error")) {
                Log.e("code%3D", "getting error fetching access token");
                dismiss();
            }
            else{
                Log.e("code%3D", "Error");
            }
        }


    };
}
