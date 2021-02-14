package com.wallagram.Receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.wallagram.Connectors.ForegroundService;
import com.wallagram.Utils.Functions;

public class AlarmReceiver extends BroadcastReceiver {
    private static final String TAG = "ALARM_RECEIVER";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Alarm Received");

        if (Functions.isNetworkAvailable(context)) {
            Intent i = new Intent(context, ForegroundService.class);
            i.setAction(ForegroundService.ACTION_START_FOREGROUND_SERVICE);
            context.startForegroundService(i);
        } else {
            Log.d(TAG, "No Network Connection");
            Functions.showNotification(context, "Search Failure", "No network connection found");
        }
    }
}

