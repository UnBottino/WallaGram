package com.wallagram.Workers;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.wallagram.Activities.MainActivity;
import com.wallagram.Model.SuggestionAccount;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Objects;


public class FetchSuggestionsWorker extends Worker {
    private static final String TAG = "WORKER_FETCH_SUGGESTIONS";

    public FetchSuggestionsWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);

    }

    @NonNull
    @Override
    public Result doWork() {
        boolean error = false;

        HttpURLConnection connection = null;
        BufferedReader reader;

        Log.d(TAG, "doInBackground: Fetching suggestions from cloud db");

        String urlString = "https://utzvkb8roc.execute-api.eu-west-1.amazonaws.com/prod_v2/suggestions";
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.connect();

            InputStream stream = connection.getInputStream();

            reader = new BufferedReader(new InputStreamReader(stream));

            try {
                JSONObject jsonObject = new JSONObject(Objects.requireNonNull(reader).readLine());

                JSONArray jsonArray = jsonObject.getJSONArray("body");

                MainActivity.mSuggestionAccountList.clear();

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject suggestion = jsonArray.getJSONObject(i);

                    String suggestionName = suggestion.getString("suggestion_name");
                    String suggestionImgUrl = suggestion.getString("suggestion_img_url");

                    SuggestionAccount a = new SuggestionAccount(suggestionName, suggestionImgUrl);
                    MainActivity.mSuggestionAccountList.add(a);
                }
            } catch (Exception e) {
                Log.e(TAG, "doInBackground: Failed to parse Json");
            } finally {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    error = true;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "doInBackground: Error connecting to URL: " + urlString);
            error = true;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }

        sendUpdateSuggestionsUIBroadcast(error);

        return Result.success();
    }

    private void sendUpdateSuggestionsUIBroadcast(boolean error) {
        Log.d(TAG, "sendUpdateSuggestionsUIBroadcast: Broadcasting message");

        Intent intent = new Intent("update-suggestions");
        intent.putExtra("error", error);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }
}