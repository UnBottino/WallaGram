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

    private String mProfileUrl;
    private Account account = null;

    private boolean videoCheckDisabled = false;
    private String mPostUrl;

    private boolean error = false;
    private boolean empty = true;

    public IntentService() {
        super("name");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        SharedPreferences sharedPreferences = getSharedPreferences("Settings", 0);
        SharedPreferences.Editor editor = getSharedPreferences("Settings", 0).edit();
        String mSearchName = sharedPreferences.getString("searchName", "");
        videoCheckDisabled = sharedPreferences.getBoolean("allowVideos", false);

        HttpURLConnection connection = null;
        BufferedReader reader = null;

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

            for (int postNumber = 0; postNumber < 12; postNumber++) {
                Log.d(TAG, "onHandleIntent: Looking at post: " + postNumber);

                JSONObject edgeObject = edgesArray.getJSONObject(postNumber);
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

                    try {
                        int preferredChildNumber = sharedPreferences.getInt("multiImage", 1) - 1;
                        JSONObject childEdgeObject = childEdgesArray.getJSONObject(preferredChildNumber);
                        JSONObject childNodeObject = childEdgeObject.getJSONObject("node");

                        if (childNodeObject.get("is_video").toString().equalsIgnoreCase("false") || videoCheckDisabled) {
                            Log.d(TAG, "onHandleIntent: Preferred child found");
                            mPostUrl = childNodeObject.get("display_url").toString();
                            empty = false;
                            break;
                        }
                    } catch (Exception e) {
                        Log.d(TAG, "onHandleIntent: Preferred child not found");
                    }

                    if (loopChildren(childEdgesArray)) {
                        break;
                    }
                } else {
                    if (nodeObject.get("is_video").toString().equalsIgnoreCase("false") || videoCheckDisabled) {
                        Log.d(TAG, "onHandleIntent: Image found");
                        mPostUrl = nodeObject.get("display_url").toString();
                        empty = false;
                        break;
                    }
                }
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

        if (error || empty) {
            String errorMsg = "Account Not Found\n(" + mSearchName + ")";

            if (!error) errorMsg = "Empty Account\n(" + mSearchName + ")";
            else if (account != null) errorMsg = "Private Account\n(" + mSearchName + ")";

            Log.d(TAG, "onHandleIntent: Account Search Failed");
            editor.putString("setAccountName", errorMsg);
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

    public boolean loopChildren(JSONArray childEdgesArray) {
        int childNumber = 0;

        do {
            try {
                JSONObject childEdgeObject = childEdgesArray.getJSONObject(childNumber);

                JSONObject childNodeObject = childEdgeObject.getJSONObject("node");
                if (childNodeObject.get("is_video").toString().equalsIgnoreCase("false") || videoCheckDisabled) {
                    Log.d(TAG, "onHandleIntent: Image found in child : " + childNumber);
                    mPostUrl = childNodeObject.get("display_url").toString();
                    empty = false;
                    return true;
                }
            } catch (Exception e) {
                Log.e(TAG, "loopChildren: " + e.getMessage());
            }

            childNumber++;
        } while (childNumber <= childEdgesArray.length());

        return false;
    }
}