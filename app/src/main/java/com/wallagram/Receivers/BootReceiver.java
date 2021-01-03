package com.wallagram.Receivers;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import com.wallagram.Connectors.ForegroundService;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            /*Intent serviceIntent = new Intent(context, ForegroundService.class);
            context.startService(serviceIntent);*/

            Intent alarmIntent = new Intent(context, AlarmReceiver.class);
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            PendingIntent pendingIntent = PendingIntent
                    .getBroadcast(context, 0, alarmIntent, PendingIntent.FLAG_CANCEL_CURRENT);
            if(alarmManager != null) {
                alarmManager.cancel(pendingIntent);
                alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 900000, pendingIntent);
            }
        }
    }
}