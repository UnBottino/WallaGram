package com.wallagram.Connectors;

import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.Nullable;

import com.wallagram.Model.Account;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Objects;

public class IntentService extends android.app.IntentService {
    private static final String TAG = "INTENT_SERVICE";

    private String mPostUrl;
    private String mProfileUrl;

    private Account account;

    public IntentService() {
        super("name");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        SharedPreferences sharedPreferences = getSharedPreferences("Settings", 0);
        SharedPreferences.Editor editor = getSharedPreferences("Settings", 0).edit();
        String mSearchName = sharedPreferences.getString("searchName", "");

        HttpURLConnection connection = null;
        BufferedReader reader = null;
        boolean error = false;

        try {
            Log.d(TAG, "onHandleIntent: Building account url and connecting");
            String urlString = "https://www.instagram.com/" + mSearchName + "/channel/?__a=1";

            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.connect();

            InputStream stream = connection.getInputStream();

            reader = new BufferedReader(new InputStreamReader(stream));

            Log.d(TAG, "onHandleIntent: Parsing json response");
            JSONObject jsonObject = new JSONObject(reader.readLine());
            JSONObject graphqlObject = jsonObject.getJSONObject("graphql");
            JSONObject userObject = graphqlObject.getJSONObject("user");

            mProfileUrl = userObject.getString("profile_pic_url_hd");
            account = new Account(mSearchName, mProfileUrl);

            JSONObject timelineMediaObject = userObject.getJSONObject("edge_owner_to_timeline_media");
            JSONArray edgesArray = timelineMediaObject.getJSONArray("edges");
            JSONObject edgeObject = edgesArray.getJSONObject(0);
            JSONObject nodeObject = edgeObject.getJSONObject("node");

            JSONObject childrenObject = null;
            try {
                childrenObject = nodeObject.getJSONObject("edge_sidecar_to_children");
            } catch (Exception e) {
                Log.d(TAG, "onHandleIntent: Post has NO children");
            }

            if (childrenObject != null) {
                Log.d(TAG, "onHandleIntent: Children found");

                JSONArray childEdgesArray = childrenObject.getJSONArray("edges");

                int imageNumber = sharedPreferences.getInt("multiImage", 1) - 1;

                JSONObject childEdgeObject = childEdgesArray.getJSONObject(0);

                try {
                    childEdgeObject = childEdgesArray.getJSONObject(imageNumber);
                } catch (Exception e) {
                    Log.d(TAG, "onHandleIntent: Multi-post is not long enough");
                }

                JSONObject childNodeObject = childEdgeObject.getJSONObject("node");
                mPostUrl = childNodeObject.get("display_url").toString();
            } else {
                mPostUrl = nodeObject.get("display_url").toString();
            }
        } catch (JSONException | IOException e) {
            Log.d(TAG, Objects.requireNonNull(e.getMessage()));
            error = true;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (error) {
            Log.d(TAG, "onHandleIntent: Account Search Failed");

            editor.putString("setAccountName", "Account doesn't exist or is set private");
            editor.putString("setProfilePic", "");
            editor.apply();

            Intent i = new Intent(this, ForegroundService.class);
            i.setAction(ForegroundService.ACTION_STOP_FOREGROUND_SERVICE);
            i.putExtra("error", true);
            startForegroundService(i);
        } else {
            editor.putString("setAccountName", account.getAccountName());
            editor.putString("setProfilePic", mProfileUrl);
            editor.putString("setPostURL", mPostUrl);
            editor.commit();

            Intent setWallpaperIntent = new Intent(getApplicationContext(), SetWallpaperIntentService.class);
            setWallpaperIntent.putExtra("postUrl", mPostUrl);
            startService(setWallpaperIntent);

            if (sharedPreferences.getInt("saveWallpaper", 0) == 1) {
                Intent savePostIntent = new Intent(getApplicationContext(), SavePostIntentService.class);
                savePostIntent.putExtra("postUrl", mPostUrl);
                startService(savePostIntent);
            }
        }
    }
}
