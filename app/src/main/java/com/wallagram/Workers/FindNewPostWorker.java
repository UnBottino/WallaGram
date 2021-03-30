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

public class FindNewPostWorker extends Worker {
    private static final String TAG = "WORKER_FIND_NEW_POST";

    private final SharedPreferences mSharedPreferences;
    private final SharedPreferences.Editor mEditor;

    private final String mSearchName;
    private final boolean mAllowVideos;
    private final int mPreferredChild;

    private boolean mIsPrivate;
    private String mProfilePicUrl;
    private int mPostCount;

    private JSONObject mTimelineMedia;
    private JSONObject mPostNode;
    private JSONArray mChildEdgesArray;
    private JSONObject mChildNode;

    private String mPostUrl;
    private String mImageName;

    private boolean mSuccess;

    public FindNewPostWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        mSharedPreferences = getApplicationContext().getSharedPreferences("Settings", 0);
        mEditor = mSharedPreferences.edit();
        mEditor.apply();

        mSearchName = mSharedPreferences.getString("searchName", "");
        mAllowVideos = mSharedPreferences.getBoolean("allowVideos", false);
        mPreferredChild = mSharedPreferences.getInt("multiImage", 1) - 1;
    }

    @NonNull
    @Override
    public Result doWork() {
        HttpURLConnection connection = null;
        BufferedReader reader = null;
        try {
            Log.d(TAG, "Building account url and connecting");
            String urlString = "https://www.instagram.com/" + mSearchName + "/channel/?__a=1";

            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.connect();

            InputStream stream = connection.getInputStream();
            reader = new BufferedReader(new InputStreamReader(stream));

            Log.d(TAG, "Reading Json Response");
            JSONObject responseObject = new JSONObject(reader.readLine());

            processResponse(responseObject);
        } catch (Exception e) {
            Log.d(TAG, "Account Not Found");
            endError("Account Not Found\n(" + mSearchName + ")");
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

        return (mSuccess ? Result.success() : Result.failure());
    }

    private void processResponse(JSONObject responseObject) {
        try {
            getData(responseObject);

            if (mIsPrivate) {
                endError("Private account\n(" + mSearchName + ")");
            } else if (mPostCount == 0) {
                endError("No Posts Yet\n(" + mSearchName + ")");
            } else {
                //Loop through posts
                for (int postNumber = 0; postNumber < 12; postNumber++) {
                    Log.d(TAG, "Looking at post: " + postNumber);
                    mPostNode = mTimelineMedia.getJSONArray("edges").getJSONObject(postNumber).getJSONObject("node");

                    if (getPostChildren()) {
                        Log.d(TAG, "Children found");
                        if (checkPreferableChild()) {
                            endSuccess();
                            return;
                        } else if (loopPostChildren()) {
                            endSuccess();
                            return;
                        }
                    } else {
                        if (mPostNode.get("is_video").toString().equalsIgnoreCase("false") || mAllowVideos) {
                            Log.d(TAG, "Image found");
                            mPostUrl = mPostNode.get("display_url").toString();
                            endSuccess();
                            return;
                        }
                    }
                }

                //Looped 12 posts and found no image
                endError("No Recent Image Posts\n(" + mSearchName + ")");
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing response: " + e.getMessage());
            endError("Unexpected Error");
        }
    }

    private void getData(JSONObject jsonObject) throws JSONException {
        Log.d(TAG, "Parsing Json response");
        JSONObject userObject = jsonObject.getJSONObject("graphql").getJSONObject("user");

        mIsPrivate = userObject.getBoolean("is_private");
        mProfilePicUrl = userObject.getString("profile_pic_url_hd");
        mTimelineMedia = userObject.getJSONObject("edge_owner_to_timeline_media");

        mPostCount = mTimelineMedia.getInt("count");
    }

    private boolean getPostChildren() {
        try {
            mChildEdgesArray = mPostNode.getJSONObject("edge_sidecar_to_children").getJSONArray("edges");
            return true;
        } catch (Exception e) {
            Log.d(TAG, "Post has NO children");
            return false;
        }
    }

    private boolean checkPreferableChild() {
        try {
            mChildNode = mChildEdgesArray.getJSONObject(mPreferredChild).getJSONObject("node");

            if (mChildNode.get("is_video").toString().equalsIgnoreCase("false") || mAllowVideos) {
                Log.d(TAG, "Preferred child found");
                mPostUrl = mChildNode.get("display_url").toString();
                return true;
            }
        } catch (Exception e) {
            Log.d(TAG, "Preferred child not found");
        }

        return false;
    }

    private boolean loopPostChildren() {
        Log.d(TAG, "loopChildren: Looping Children");
        for (int childNumber = 0; childNumber <= mChildEdgesArray.length(); childNumber++) {
            try {
                mChildNode = mChildEdgesArray.getJSONObject(childNumber).getJSONObject("node");

                if (mChildNode.get("is_video").toString().equalsIgnoreCase("false") || mAllowVideos) {
                    Log.d(TAG, "loopChildren: Image found in child : " + childNumber);
                    mPostUrl = mChildNode.get("display_url").toString();
                    return true;
                }
            } catch (Exception e) {
                Log.e(TAG, "loopChildren: " + e.getMessage());
            }
        }

        return false;
    }

    private void endSuccess() {
        try {
            mImageName = mPostNode.get("id").toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        //Show new current account info
        mEditor.putString("setAccountName", mSearchName);
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

        WorkManager.getInstance(getApplicationContext()).cancelAllWorkByTag("findNewPost");
        sendUpdateUIBroadcast(true);

        mSuccess = false;
    }

    private void setWallpaper() {
        Log.d(TAG, "setWallpaper: Setting Wallpaper");

        try {
            URL url = new URL(mPostUrl);
            Bitmap bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream());
            WallpaperManager wallpaperManager = WallpaperManager.getInstance(getApplicationContext());

            int screenWidth = mSharedPreferences.getInt("screenWidth", 0);
            int screenHeight = mSharedPreferences.getInt("screenHeight", 0);
            int imageAlign = mSharedPreferences.getInt("align", 1);

            Bitmap bm = scaleCrop(bitmap, imageAlign, screenHeight, screenWidth);

            if (mSharedPreferences.getInt("location", 0) == 0) {
                wallpaperManager.setBitmap(bm, null, true, WallpaperManager.FLAG_SYSTEM);
            } else if (mSharedPreferences.getInt("location", 0) == 1) {
                wallpaperManager.setBitmap(bm, null, true, WallpaperManager.FLAG_LOCK);
            } else {
                wallpaperManager.setBitmap(bm, null, true, WallpaperManager.FLAG_SYSTEM);
                wallpaperManager.setBitmap(bm, null, true, WallpaperManager.FLAG_LOCK);
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

    private Bitmap scaleCrop(Bitmap source, int imageAlign, int newHeight, int newWidth) {
        Log.d(TAG, "scaleCrop: Scaling image to Height: " + newHeight + ", to Width: " + newWidth + " and aligned: " + imageAlign);

        int sourceWidth = source.getWidth();
        int sourceHeight = source.getHeight();

        // Compute the scaling factors to fit the new height and width, respectively.
        // To cover the final image, the final scaling will be the bigger
        // of these two.
        float xScale = (float) newWidth / sourceWidth;
        float yScale = (float) newHeight / sourceHeight;
        float scale = Math.max(xScale, yScale);

        // Now get the size of the source bitmap when scaled
        float scaledWidth = scale * sourceWidth;
        float scaledHeight = scale * sourceHeight;

        // Let's find out the upper left coordinates if the scaled bitmap
        // should be centered in the new size give by the parameters
        float left;
        float top;

        switch (imageAlign) {
            case 0:
                left = 0;
                top = 0;
                break;
            case 2:
                left = (newWidth - scaledWidth);
                top = (newHeight - scaledHeight);
                break;
            default:
                left = (newWidth - scaledWidth) / 2;
                top = (newHeight - scaledHeight) / 2;
                break;
        }

        // The target rectangle for the new, scaled version of the source bitmap will now
        // be
        Rect srcRect = new Rect(0, 0, sourceWidth, sourceHeight);
        RectF targetRect = new RectF(left, top, left + scaledWidth, top + scaledHeight);

        // Finally, we create a new bitmap of the specified size and draw our new,
        // scaled bitmap onto it.
        Bitmap dest = Bitmap.createBitmap(newWidth, newHeight, source.getConfig());
        Canvas canvas = new Canvas(dest);
        canvas.drawBitmap(source, srcRect, targetRect, null);

        return dest;
    }

    private void sendUpdateUIBroadcast(boolean error) {
        Log.d(TAG, "sendUpdateUIBroadcast: Broadcasting message");

        Intent intent = new Intent("update-set-account");
        intent.putExtra("error", error);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }
}