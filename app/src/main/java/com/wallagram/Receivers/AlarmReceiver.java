package com.wallagram.Receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.wallagram.Connectors.ForegroundService;

public class AlarmReceiver extends BroadcastReceiver {
    private static final String TAG = "ALARM_RECEIVER";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Alarm Received");

        Intent i = new Intent(context, ForegroundService.class);
        i.setAction(ForegroundService.ACTION_START_FOREGROUND_SERVICE);
        context.startForegroundService(i);
    }
}

