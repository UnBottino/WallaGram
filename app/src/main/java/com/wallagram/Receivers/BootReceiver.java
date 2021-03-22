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

            if (sharedPreferences.getInt("state", 1) == 1 && !sharedPreferences.getString("searchName", "").equalsIgnoreCase("")) {
                Log.d(TAG, "onReceive: Activating Alarm");
                Functions.findNewPostPeriodicRequest(context);
            }
        }
    }
}