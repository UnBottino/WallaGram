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

public class FindNewRedditPostWorker extends Worker {
    private static final String TAG = "WORKER_FIND_NEW_REDDIT_POST";

    private final SharedPreferences mSharedPreferences;
    private final SharedPreferences.Editor mEditor;

    private String mProfilePicUrl;

    private String mPostUrl;
    private final String mSubreddit;
    private String mID;
    private String mImageName;

    private boolean mSuccess;

    public FindNewRedditPostWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        mSharedPreferences = getApplicationContext().getSharedPreferences("Settings", 0);
        mEditor = mSharedPreferences.edit();
        mEditor.apply();

        mSubreddit = mSharedPreferences.getString("searchName", "");
    }

    @NonNull
    @Override
    public Result doWork() {

        getSubredditIcon();
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

    private void getSubredditIcon() {
        boolean error = false;

        HttpURLConnection connection = null;
        BufferedReader reader = null;
        try {
            String urlString = "https://www.reddit.com/r/" + mSubreddit + "/about.json";

            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setInstanceFollowRedirects(false);
            connection.connect();

            InputStream stream = connection.getInputStream();
            reader = new BufferedReader(new InputStreamReader(stream));

            Log.d(TAG, "Reading Json Response");
            Log.d(TAG, "Parsing Json response");

            JSONObject responseObject = new JSONObject(reader.readLine());
            JSONObject dataObject = responseObject.getJSONObject("data");
            mProfilePicUrl = dataObject.getString("icon_img");

            if(mProfilePicUrl.equalsIgnoreCase("")){
                String community_icon = dataObject.getString("community_icon");

                int indexQ = community_icon.indexOf("?");
                mProfilePicUrl = community_icon.substring(0, indexQ);
                System.out.println(mProfilePicUrl);
            }

            System.out.println(mProfilePicUrl);
        } catch (Exception e) {
            System.out.println("ERROR");
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
            mID = childDataObject.getString("id");

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
        mImageName = mSubreddit + "-" + mID;

        //Show new current account info

        mEditor.putString("setAccountName", mSubreddit);
        mEditor.putString("setProfilePic", mProfilePicUrl);

        mEditor.putString("setPostURL", mPostUrl);
        mEditor.putString("setImageName", mImageName);
        mEditor.putBoolean("repeatingWorker", true);
        mEditor.commit();

        setWallpaper();
        mSuccess = true;
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

    private void setWallpaper() {
        Log.d(TAG, "setWallpaper: Setting Wallpaper");
        String[] options = {"Home Screen", "Lock Screen", "Both"};
        int screenWidth = mSharedPreferences.getInt("screenWidth", 0);
        String setImageAlign = mSharedPreferences.getString("setImageAlign", "Centre");

        try {
            URL url = new URL(mPostUrl);
            Bitmap postImage = BitmapFactory.decodeStream(url.openConnection().getInputStream());
            WallpaperManager wallpaperManager = WallpaperManager.getInstance(getApplicationContext());

            final int desiredH = wallpaperManager.getDesiredMinimumHeight();

            Bitmap bm = scaleCrop(postImage, setImageAlign, desiredH, screenWidth);

            if (mSharedPreferences.getString("setLocation", options[0]).equalsIgnoreCase(options[0])) {
                wallpaperManager.setBitmap(bm, null, false, WallpaperManager.FLAG_SYSTEM);
            } else if (mSharedPreferences.getString("setLocation", options[0]).equalsIgnoreCase(options[1])) {
                wallpaperManager.setBitmap(bm, null, false, WallpaperManager.FLAG_LOCK);
            } else {
                wallpaperManager.setBitmap(bm, null, false, WallpaperManager.FLAG_SYSTEM);
                wallpaperManager.setBitmap(bm, null, false, WallpaperManager.FLAG_LOCK);
            }

            if (mSharedPreferences.getInt("saveWallpaper", 0) == 1) {
                Functions.savePostRequest(getApplicationContext());
            }

            sendUpdateUIBroadcast(false);
        } catch (IOException e) {
            Log.e(TAG, "setWallpaper:  ", e);
            sendUpdateUIBroadcast(true);
        }
    }

    private Bitmap scaleCrop(Bitmap postImage, String imageAlign, int screenHeight,
                             int screenWidth) {
        Log.d(TAG, "scaleCrop: Scaling image to Height: " + screenHeight + ", to Width: " + screenWidth + " and aligned: " + imageAlign);

        int postWidth = postImage.getWidth();
        int postHeight = postImage.getHeight();

        // Compute the scaling factors to fit the new height and width, respectively.
        // To cover the final image, the final scaling will be the bigger
        // of these two.
        float xScale = (float) screenWidth / postWidth;
        float yScale = (float) screenHeight / postHeight;
        float scale = Math.max(xScale, yScale);

        // Now get the size of the source bitmap when scaled
        float scaledWidth = scale * postWidth;
        float scaledHeight = scale * postHeight;

        // Let's find out the upper left coordinates if the scaled bitmap
        // should be centered in the new size give by the parameters
        float left;
        float top;

        switch (imageAlign) {
            case "Left":
                left = 0;
                top = 0;
                break;
            case "Right":
                left = (screenWidth - scaledWidth);
                top = (screenHeight - scaledHeight);
                break;
            default:
                left = (screenWidth - scaledWidth) / 2;
                top = (screenHeight - scaledHeight) / 2;
                break;
        }

        // The target rectangle for the new, scaled version of the source bitmap will now
        // be
        Rect srcRect = new Rect(0, 0, postWidth, postHeight);
        RectF targetRect = new RectF(left, top, left + scaledWidth, top + scaledHeight);

        // Finally, we create a new bitmap of the specified size and draw our new,
        // scaled bitmap onto it.
        Bitmap dest = Bitmap.createBitmap(screenWidth, screenHeight, postImage.getConfig());
        Canvas canvas = new Canvas(dest);
        canvas.drawBitmap(postImage, srcRect, targetRect, null);

        return dest;
    }

    private void sendUpdateUIBroadcast(boolean error) {
        Log.d(TAG, "sendUpdateUIBroadcast: Broadcasting message");

        Intent intent = new Intent("update-set-account");
        intent.putExtra("error", error);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }
}