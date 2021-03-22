package com.wallagram.Connectors;

import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
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

public class WorkerFindNewPost extends Worker {
    private static final String TAG = "WORK_MANAGER";

    private final SharedPreferences sharedPreferences;
    private final SharedPreferences.Editor editor;

    private final String mSearchName;
    private String mProfileUrl;

    private JSONObject nodeObject;
    private final boolean videoCheckDisabled;
    private String mPostUrl;
    private String mImageName;

    private String errorMsg = null;

    public WorkerFindNewPost(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);

        sharedPreferences = getApplicationContext().getSharedPreferences("Settings", 0);
        editor = sharedPreferences.edit();
        editor.apply();

        mSearchName = sharedPreferences.getString("searchName", "");
        videoCheckDisabled = sharedPreferences.getBoolean("allowVideos", false);
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @NonNull
    @Override
    public Result doWork() {
        Functions.debugNotification(getApplicationContext(), "Timestamp");

        HttpURLConnection connection = null;
        BufferedReader reader = null;

        //Connect and get response
        try {
            Log.d(TAG, "onHandleIntent: Building account url and connecting");
            String urlString = "https://www.instagram.com/" + mSearchName + "/channel/?__a=1";

            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.connect();

            InputStream stream = connection.getInputStream();
            reader = new BufferedReader(new InputStreamReader(stream));
        } catch (Exception e) {
            Log.d(TAG, "onHandleIntent: Account Not Found");
            errorMsg = "Account Not Found\n(" + mSearchName + ")";
            assert connection != null;
            connection.disconnect();
        }

        if (errorMsg == null) {
            try {
                Log.d(TAG, "onHandleIntent: Parsing json response");
                assert reader != null;
                JSONObject jsonObject = new JSONObject(reader.readLine());

                JSONObject graphqlObject = jsonObject.getJSONObject("graphql");
                JSONObject userObject = graphqlObject.getJSONObject("user");

                //Get profile pic
                mProfileUrl = userObject.getString("profile_pic_url_hd");

                JSONObject timelineMediaObject = userObject.getJSONObject("edge_owner_to_timeline_media");

                //Check for 'no post' account
                int postCount = timelineMediaObject.getInt("count");
                Log.d(TAG, "onHandleIntent: Post Count: " + postCount);
                if (postCount == 0)
                    errorMsg = "No Posts Yet\n(" + mSearchName + ")";

                //Get usable wallpaper
                if (errorMsg == null) {
                    JSONArray edgesArray = timelineMediaObject.getJSONArray("edges");

                    //Loop through posts
                    for (int postNumber = 0; postNumber < 12; postNumber++) {
                        Log.d(TAG, "onHandleIntent: Looking at post: " + postNumber);

                        JSONObject edgeObject;

                        //Check for private account
                        try {
                            edgeObject = edgesArray.getJSONObject(postNumber);
                        } catch (Exception e) {
                            Log.d(TAG, "onHandleIntent: Private Account");
                            errorMsg = "Private Account\n(" + mSearchName + ")";
                            break;
                        }

                        nodeObject = edgeObject.getJSONObject("node");

                        //Check for post children
                        JSONObject childrenObject = null;
                        try {
                            childrenObject = nodeObject.getJSONObject("edge_sidecar_to_children");
                        } catch (Exception e) {
                            Log.d(TAG, "onHandleIntent: Post has NO children");
                        }

                        if (childrenObject != null) {
                            Log.d(TAG, "onHandleIntent: Children found");
                            JSONArray childEdgesArray = childrenObject.getJSONArray("edges");

                            //Check if preferred child is usable
                            try {
                                int preferredChildNumber = sharedPreferences.getInt("multiImage", 1) - 1;
                                JSONObject childEdgeObject = childEdgesArray.getJSONObject(preferredChildNumber);
                                JSONObject childNodeObject = childEdgeObject.getJSONObject("node");

                                if (childNodeObject.get("is_video").toString().equalsIgnoreCase("false") || videoCheckDisabled) {
                                    Log.d(TAG, "onHandleIntent: Preferred child found");
                                    mPostUrl = childNodeObject.get("display_url").toString();
                                    break;
                                }
                            } catch (Exception e) {
                                Log.d(TAG, "onHandleIntent: Preferred child not found");
                            }

                            //Loop through all children if preferred has problem
                            if (loopChildren(childEdgesArray)) {
                                break;
                            }
                        } else {
                            //Check if post is usable if no children
                            if (nodeObject.get("is_video").toString().equalsIgnoreCase("false") || videoCheckDisabled) {
                                Log.d(TAG, "onHandleIntent: Image found");
                                mPostUrl = nodeObject.get("display_url").toString();
                                break;
                            }
                        }

                        //Check if 12 most recent posts has been checked
                        if (postNumber == 11)
                            errorMsg = "No Recent Image Posts\n(" + mSearchName + ")";
                    }

                    if (errorMsg == null)
                        mImageName = nodeObject.get("id").toString();
                }
            } catch (JSONException | IOException e) {
                Log.e(TAG, "Error parsing response: " + e.getMessage());
                errorMsg = "Unexpected Error";
            } finally {
                connection.disconnect();

                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (IOException e) {
                    Log.e(TAG, "onHandleIntent: " + e.getMessage());
                }
            }
        }

        if (errorMsg != null) {
            //Show error message
            editor.putString("setAccountName", errorMsg);
            editor.putString("setProfilePic", "");
            editor.putBoolean("repeatingWorker", false);
            editor.commit();

            WorkManager.getInstance(getApplicationContext()).cancelAllWorkByTag("findNewPost");
            sendUpdateUIBroadcast(true);
        } else {
            //Show new current account info
            editor.putString("setAccountName", mSearchName);
            editor.putString("setProfilePic", mProfileUrl);
            editor.putString("setPostURL", mPostUrl);
            editor.putString("setImageName", mImageName);
            editor.putBoolean("repeatingWorker", true);
            editor.commit();

            setWallpaper();
        }

        return Result.success();
    }

    public boolean loopChildren(JSONArray childEdgesArray) {
        for (int childNumber = 0; childNumber <= childEdgesArray.length(); childNumber++) {
            try {
                JSONObject childEdgeObject = childEdgesArray.getJSONObject(childNumber);

                JSONObject childNodeObject = childEdgeObject.getJSONObject("node");
                if (childNodeObject.get("is_video").toString().equalsIgnoreCase("false") || videoCheckDisabled) {
                    Log.d(TAG, "onHandleIntent: Image found in child : " + childNumber);
                    mPostUrl = childNodeObject.get("display_url").toString();
                    return true;
                }
            } catch (Exception e) {
                Log.e(TAG, "loopChildren: " + e.getMessage());
            }
        }

        return false;
    }

    private void setWallpaper() {
        try {
            Log.d(TAG, "onHandleIntent: Setting Wallpaper");
            URL url = new URL(mPostUrl);
            Bitmap bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream());
            WallpaperManager wallpaperManager = WallpaperManager.getInstance(getApplicationContext());

            int screenWidth = sharedPreferences.getInt("screenWidth", 0);
            int screenHeight = sharedPreferences.getInt("screenHeight", 0);

            int imageAlign = sharedPreferences.getInt("align", 1);

            Bitmap bm = scaleCrop(bitmap, imageAlign, screenHeight, screenWidth);

            if (sharedPreferences.getInt("location", 0) == 0) {
                wallpaperManager.setBitmap(bm, null, true, WallpaperManager.FLAG_SYSTEM);
            } else if (sharedPreferences.getInt("location", 0) == 1) {
                wallpaperManager.setBitmap(bm, null, true, WallpaperManager.FLAG_LOCK);
            } else {
                wallpaperManager.setBitmap(bm, null, true, WallpaperManager.FLAG_SYSTEM);
                wallpaperManager.setBitmap(bm, null, true, WallpaperManager.FLAG_LOCK);
            }
        } catch (IOException ex) {
            Log.e(TAG, "onHandleIntent: settingWallpaper Failed:  " + ex.getLocalizedMessage());
        }

        if (sharedPreferences.getInt("saveWallpaper", 0) == 1) {
            Functions.savePostRequest(getApplicationContext());
        }

        sendUpdateUIBroadcast(false);
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

        Intent intent = new Intent("custom-event-name");
        intent.putExtra("error", error);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }
}