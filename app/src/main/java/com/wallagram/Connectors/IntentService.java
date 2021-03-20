package com.wallagram.Connectors;

import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class IntentService extends android.app.IntentService {
    private static final String TAG = "INTENT_SERVICE";

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    private String mSearchName;
    private String mProfileUrl;

    private JSONObject nodeObject;
    private boolean videoCheckDisabled = false;
    private String mPostUrl;
    private String mImageName;

    private String errorMsg = null;

    public IntentService() {
        super("name");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        sharedPreferences = this.getSharedPreferences("Settings", 0);
        editor = sharedPreferences.edit();
        editor.apply();

        mSearchName = sharedPreferences.getString("searchName", "");
        videoCheckDisabled = sharedPreferences.getBoolean("allowVideos", false);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
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
            connection.disconnect();
        }

        if (errorMsg == null) {
            try {
                Log.d(TAG, "onHandleIntent: Parsing json response");
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
                if (connection != null) {
                    connection.disconnect();
                }
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
            editor.commit();

            Intent i = new Intent(this, ForegroundService.class);
            i.setAction(ForegroundService.ACTION_STOP_FOREGROUND_SERVICE);
            i.putExtra("error", true);
            startForegroundService(i);
        } else {
            //Show new current account info
            editor.putString("setAccountName", mSearchName);
            editor.putString("setProfilePic", mProfileUrl);
            editor.putString("setPostURL", mPostUrl);
            editor.putString("setImageName", mImageName);
            editor.commit();

            Intent setWallpaperIntent = new Intent(getApplicationContext(), SetWallpaperIntentService.class);
            setWallpaperIntent.putExtra("postUrl", mPostUrl);
            startService(setWallpaperIntent);

            if (sharedPreferences.getInt("saveWallpaper", 0) == 1) {
                Intent savePostIntent = new Intent(getApplicationContext(), SavePostIntentService.class);
                startService(savePostIntent);
            }
        }
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
}