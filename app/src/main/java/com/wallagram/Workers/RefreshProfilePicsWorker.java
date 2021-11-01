package com.wallagram.Workers;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.wallagram.Model.PreviousAccount;
import com.wallagram.Utils.Functions;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;


public class RefreshProfilePicsWorker extends Worker {
    private static final String TAG = "WORKER_REFRESH_IMAGES";

    private String mAccountType;
    private String mAccountName;
    private String mProfilePicUrl;

    public RefreshProfilePicsWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);

    }

    @NonNull
    @Override
    public Result doWork() {
        boolean error = false;

        Log.d(TAG, "doInBackground: Refreshing profile pics");

        HttpURLConnection connection = null;
        BufferedReader reader = null;

        List<PreviousAccount> previousAccounts = Functions.getDBInstaAccounts(getApplicationContext());

        for (PreviousAccount pa: previousAccounts) {
            mAccountType = pa.getAccountType();
            mAccountName = pa.getAccountName();

            try {
                String urlString = "https://www.instagram.com/" + mAccountName + "/channel/?__a=1";

                URL url = new URL(urlString);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                InputStream stream = connection.getInputStream();
                reader = new BufferedReader(new InputStreamReader(stream));

                JSONObject responseObject = new JSONObject(reader.readLine());

                processResponse(responseObject);
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
        }

        return Result.success();
    }

    private void processResponse(JSONObject jsonObject) {
        try {
            Log.d(TAG, "Parsing Json response");
            JSONObject userObject = jsonObject.getJSONObject("graphql").getJSONObject("user");

            mProfilePicUrl = userObject.getString("profile_pic_url_hd");

            Functions.updateProfilePicURL(getApplicationContext(), mAccountType, mAccountName, mProfilePicUrl);
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing response ("+ mAccountName +"): " + e.getMessage());
        }
    }

    private void sendUpdateSuggestionsUIBroadcast(boolean error) {
        Log.d(TAG, "sendUpdateSuggestionsUIBroadcast: Broadcasting message");

        Intent intent = new Intent("update-suggestions");
        intent.putExtra("error", error);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }
}