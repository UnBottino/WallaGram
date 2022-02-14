package com.wallagram.Workers;

import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.wallagram.Utils.Functions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class PreviewRedditPostWorker extends Worker {
    private static final String TAG = "WORKER_FIND_NEW_REDDIT_POST";

    private final SharedPreferences mSharedPreferences;
    private final SharedPreferences.Editor mEditor;

    private String mPostUrl;
    private final String mSubreddit;

    private boolean mSuccess;

    public PreviewRedditPostWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        mSharedPreferences = getApplicationContext().getSharedPreferences("Settings", 0);
        mEditor = mSharedPreferences.edit();
        mEditor.apply();

        mSubreddit = mSharedPreferences.getString("searchName", "");
    }

    @NonNull
    @Override
    public Result doWork() {
        getUrlResponse();

        return (mSuccess ? Result.success() : Result.failure());
    }

    private void getUrlResponse() {
        boolean error = false;

        HttpURLConnection connection = null;
        HttpURLConnection connection2;
        BufferedReader reader = null;
        try {
            Log.d(TAG, "Building account url and connecting");
            String urlString = "https://www.reddit.com/r/" + mSubreddit + "/random.json";

            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setInstanceFollowRedirects(false);
            connection.connect();

            String redirectUrlString = connection.getHeaderField("location");

            URL redirectUrl = new URL(redirectUrlString);
            connection2 = (HttpURLConnection) redirectUrl.openConnection();
            connection2.setInstanceFollowRedirects(false);
            connection2.connect();

            InputStream stream = connection2.getInputStream();
            reader = new BufferedReader(new InputStreamReader(stream));

            Log.d(TAG, "Reading Json Response");

            JSONArray responseArray = new JSONArray(reader.readLine());

            processResponse(responseArray);
        } catch (Exception e) {
            error = true;
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (connection != null) {
                connection.disconnect();
            }
        }

        if (error) {
            Log.d(TAG, "Subreddit Not Found");
            endError("Subreddit Not Found\n(" + mSubreddit + ")");
        }
    }

    private void processResponse(JSONArray responseArray) {
        try {
            Log.d(TAG, "Parsing Json response");
            JSONObject responseObject = responseArray.getJSONObject(0);
            JSONObject dataObject = responseObject.getJSONObject("data");
            JSONArray childrenArray = dataObject.getJSONArray("children");
            JSONObject childDataObject = childrenArray.getJSONObject(0).getJSONObject("data");
            mPostUrl = childDataObject.getString("url");

            System.out.println(mPostUrl);

            String[] extensions = {".jpg", ".png", ".gif", ".jpeg"};
            for (String s : extensions) {
                if (mPostUrl.endsWith(s)) {
                    endSuccess();
                    break;
                }
            }

            if (!mSuccess)
                getUrlResponse();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void endSuccess() {
        mEditor.putString("setPostURL", mPostUrl);
        mEditor.commit();
        mSuccess = true;

        sendUpdatePreviewBroadcast();
    }

    private void endError(String errorMsg) {
        //Show error message
        mEditor.putString("setAccountName", errorMsg);
        mEditor.putString("setProfilePic", "");
        mEditor.putBoolean("repeatingWorker", false);
        mEditor.commit();

        WorkManager.getInstance(getApplicationContext()).cancelAllWorkByTag("findNewPost: Periodic");
        sendUpdateUIBroadcast(true);

        mSuccess = false;
    }

    private void sendUpdateUIBroadcast(boolean error) {
        Log.d(TAG, "sendUpdateUIBroadcast: Broadcasting message");

        Intent intent = new Intent("update-set-account");
        intent.putExtra("error", error);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

    private void sendUpdatePreviewBroadcast() {
        Log.d(TAG, "sendUpdatePreviewBroadcast: Broadcasting message");

        Intent intent = new Intent("update-preview");
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }
}