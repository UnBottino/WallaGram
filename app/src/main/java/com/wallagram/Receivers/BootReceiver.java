package com.wallagram.Receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.wallagram.Utils.Functions;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BOOT_RECEIVER";

    @Override
    public void onReceive(Context context, Intent i) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(i.getAction())) {
            SharedPreferences sharedPreferences = context.getSharedPreferences("Settings", Context.MODE_PRIVATE);

            String mMode = sharedPreferences.getString("setMode", "Insta");

            if (mMode.equalsIgnoreCase("Insta")) {
                //Activate Alarm
                if (sharedPreferences.getInt("state", 1) == 1 && sharedPreferences.getBoolean("repeatingWorker", false)) {
                    Log.d(TAG, "onBindViewHolder: periodic");
                    Functions.findNewPostPeriodicRequest(context);
                } else {
                    Functions.findNewPostSingleRequest(context);
                    Log.d(TAG, "onBindViewHolder: Single");
                }
            } else if (mMode.equalsIgnoreCase("Reddit")) {
                //Activate Alarm
                if (sharedPreferences.getInt("state", 1) == 1 && sharedPreferences.getBoolean("repeatingWorker", false)) {
                    Log.d(TAG, "onBindViewHolder: periodic");
                    Functions.findNewRedditPostPeriodicRequest(context);
                } else {
                    Log.d(TAG, "onBindViewHolder: Single");
                    Functions.findNewRedditPostSingleRequest(context);
                }
            }
        }
    }
}