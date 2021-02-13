package com.wallagram.Login;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;

import com.wallagram.R;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AuthenticationDialog extends Dialog {
    private SharedPreferences sharedPreferences;

    private AuthenticationListener listener;
    private String request_url;
    private String redirect_url;

    private String access_token;
    private String resp;

    public AuthenticationDialog(@NonNull Context context, AuthenticationListener listener) {
        super(context);

        sharedPreferences = context.getSharedPreferences("Settings", Context.MODE_PRIVATE);

        this.listener = listener;
        this.redirect_url = context.getResources().getString(R.string.redirect_url);

        request_url = "https://api.instagram.com/oauth/authorize" +
                "?client_id=254177392730519" +
                "&redirect_uri=https://instagram.com/" +
                "&scope=user_profile,user_media" +
                "&response_type=code";

        System.out.println("NEW URL");
        System.out.println("=========================");
        System.out.println("URL = " + request_url);
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

                String code = url;
                code = code.substring(code.lastIndexOf("%3D") + 3, code.lastIndexOf("%23"));
                System.out.println("============================CODE=======================");
                Log.e("code%3D", code);
                //listener.onTokenReceived("code");
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("code", code);
                editor.apply();

                PostTask p = new PostTask();
                p.execute();

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

    public class PostTask extends AsyncTask<String, String, String> {

        public PostTask() {

        }

        @Override
        protected String doInBackground(String... params) {
            // Create a new HttpClient and Post Header
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost("https://api.instagram.com/oauth/access_token");

            try {
                System.out.println("Final Code = " + sharedPreferences.getString("code", ""));

                List<NameValuePair> nameValuePairs = new ArrayList<>(5);
                nameValuePairs.add(new BasicNameValuePair("client_id", "254177392730519"));
                nameValuePairs.add(new BasicNameValuePair("client_secret", "73ba9ce6481ea24504a86a7c5d7ae3ae"));
                nameValuePairs.add(new BasicNameValuePair("code", sharedPreferences.getString("code", "")));
                nameValuePairs.add(new BasicNameValuePair("grant_type", "authorization_code"));
                nameValuePairs.add(new BasicNameValuePair("redirect_uri", "https://instagram.com/"));
                httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                // Execute HTTP Post Request
                HttpResponse response = httpclient.execute(httppost);

                System.out.println("RESPONSE");
                System.out.println("==========================");

                resp = EntityUtils.toString(response.getEntity());
                System.out.println(resp);

                JSONObject obj = new JSONObject(resp);

                access_token = obj.getString("access_token");

                System.out.println(access_token);

                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("access_token", access_token);
                editor.apply();

            } catch (IOException | JSONException e) {
                // TODO Auto-generated catch block
            }

            return "Success";
        }

        protected void onPostExecute(String result) {
            if(result.equalsIgnoreCase("Success")) {
                listener.onTokenReceived(sharedPreferences.getString("access_token", ""));
            }
        }
    }


    public class InfoTask extends AsyncTask<String, String, String> {

        public InfoTask() {

        }

        @Override
        protected String doInBackground(String... params) {
            // Create a new HttpClient and Post Header
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost("https://graph.instagram.com/me/media?fields=id,caption");

            try {
                System.out.println("Access Token = " + sharedPreferences.getString("access_token", ""));

                List<NameValuePair> nameValuePairs = new ArrayList<>(1);
                nameValuePairs.add(new BasicNameValuePair("access_token", sharedPreferences.getString("access_token", "")));
                httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                // Execute HTTP Post Request
                HttpResponse response = httpclient.execute(httppost);

                System.out.println("RESPONSE");
                System.out.println("==========================");

                resp = EntityUtils.toString(response.getEntity());
                System.out.println(resp);

                JSONObject obj = new JSONObject(resp);

                access_token = obj.getString("access_token");

                System.out.println(access_token);

                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("access_token", access_token);
                editor.apply();

            } catch (IOException | JSONException e) {
                // TODO Auto-generated catch block
            }

            return "Success";
        }

        protected void onPostExecute(String result) {
            if(result.equalsIgnoreCase("Success")) {
                listener.onTokenReceived(sharedPreferences.getString("access_token", ""));
            }
        }
    }
}
