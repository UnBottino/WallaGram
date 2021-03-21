package com.wallagram.Connectors;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.WorkManager;

import com.wallagram.R;
import com.wallagram.Utils.Functions;

public class ForegroundService extends Service {
    private static final String TAG = "FOREGROUND_SERVICE";

    public static final String ACTION_START_FOREGROUND_SERVICE = "ACTION_START_FOREGROUND_SERVICE";
    public static final String ACTION_STOP_FOREGROUND_SERVICE = "ACTION_STOP_FOREGROUND_SERVICE";

    private static boolean error;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        startForeground();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        error = intent.getBooleanExtra("error", false);

        String action = intent.getAction();

        if (action != null) {
            switch (action) {
                case ACTION_START_FOREGROUND_SERVICE:
                    Log.d(TAG, "onStartCommand: Starting Foreground");
                    startForeground();

                    Intent serviceIntent = new Intent(this, IntentService.class);
                    startService(serviceIntent);

                    break;
                case ACTION_STOP_FOREGROUND_SERVICE:
                    stopForegroundService();
                    break;
            }
        } else {
            Log.e(TAG, "onStartCommand: Foreground intent has no action");
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void startForeground() {
        String NOTIFICATION_CHANNEL_ID = "com.wallagram.foregroundservice";
        String channelName = "WallaGram Foreground Service";
        NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_LOW);
        NotificationManager manager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(chan);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        Notification notification = notificationBuilder.setOngoing(true)
                .setSmallIcon(R.drawable.notification)
                .setContentTitle("Updating wallpaper")
                .setPriority(NotificationManager.IMPORTANCE_LOW)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
        startForeground(987, notification);
    }

    private void stopForegroundService() {
        if (!error) {
            sendUpdateUIBroadcast(false);
        } else {
            sendUpdateUIBroadcast(true);

            WorkManager.getInstance(getApplicationContext()).cancelAllWorkByTag("findNewPost");
        }

        Log.d(TAG, "stopForegroundService: Stopping foreground service.");
        waitALittle();
        stopForeground(true);
        stopSelf();
    }

    private void waitALittle() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void sendUpdateUIBroadcast(boolean error) {
        Log.d(TAG, "sendUpdateUIBroadcast: Broadcasting message");

        Intent intent = new Intent("custom-event-name");
        intent.putExtra("error", error);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}