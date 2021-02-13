package com.wallagram.Connectors;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.squareup.picasso.Picasso;
import com.wallagram.MainActivity;
import com.wallagram.Model.Account;
import com.wallagram.R;
import com.wallagram.Sqlite.SQLiteDatabaseAdapter;
import com.wallagram.Utils.Functions;

public class ForegroundService extends Service {

    public static final String ACTION_START_FOREGROUND_SERVICE = "ACTION_START_FOREGROUND_SERVICE";
    public static final String ACTION_STOP_FOREGROUND_SERVICE = "ACTION_STOP_FOREGROUND_SERVICE";

    private static final String TAG = "FOREGROUND_SERVICE";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();

            switch (action) {
                case ACTION_START_FOREGROUND_SERVICE:
                    startForeground();

                    Intent serviceIntent = new Intent(this, IntentService.class);
                    startService(serviceIntent);

                    break;
                case ACTION_STOP_FOREGROUND_SERVICE:
                    stopForegroundService();
                    break;
            }
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void startForeground() {
        Log.d(TAG, "Starting Foreground");

        String NOTIFICATION_CHANNEL_ID = "com.wallagram.foregroundservice";
        String channelName = "WallaGram Foreground Service";
        NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_LOW);
        chan.setLightColor(Color.WHITE);
        NotificationManager manager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(chan);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        Notification notification = notificationBuilder.setOngoing(true)
                .setSmallIcon(R.drawable.frown_big)
                .setContentTitle("You can disable app notifications")
                .setPriority(NotificationManager.IMPORTANCE_LOW)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
        startForeground(987, notification);
    }

    private void stopForegroundService() {
        SharedPreferences sharedPreferences = this.getSharedPreferences("Settings", Context.MODE_PRIVATE);

        String setAccountName = sharedPreferences.getString("setAccountName", "");
        String setProfilePic = sharedPreferences.getString("setProfilePic", "");
        String setPostURL = sharedPreferences.getString("setPostURL", "");

        Account account = new Account(setAccountName, setProfilePic);

        Log.d(TAG, "Setting Wallpaper and Saving Post");
        Functions.setWallpaper(this, setPostURL);
        Functions.savePost(this, setPostURL);


        if (MainActivity.IS_APP_IN_FOREGROUND) {
            Log.d(TAG, "Setting Profile Pic");
            Picasso.get()
                    .load(Uri.parse(setProfilePic))
                    .into(MainActivity.mSetProfilePic);

            Log.d(TAG, "Setting Display Name");
            MainActivity.mSetAccountName.setText(setAccountName);
            
            SQLiteDatabaseAdapter db = new SQLiteDatabaseAdapter(this);

            if (!db.checkIfAccountExists(account)) {
                db.addAccount(account);

                MainActivity.mDBAccountList.add(0, account);
                MainActivity.mAdapter.notifyItemInserted(0);
                MainActivity.mAdapter.notifyDataSetChanged();
            } else {
                Log.d(TAG, "Account name already in db (" + setAccountName + ")");

                for (Account a : MainActivity.mDBAccountList) {
                    if (a.getAccountName().equalsIgnoreCase(account.getAccountName())) {
                        db.deleteAccount(a.getAccountName());
                        db.addAccount(account);

                        // TODO: 12/02/2021 Make shorted with better code
                        MainActivity.mDBAccountList.remove(a);
                        MainActivity.mAdapter.notifyItemRemoved(MainActivity.mDBAccountList.indexOf(a));
                        MainActivity.mDBAccountList.add(0, account);
                        MainActivity.mAdapter.notifyItemInserted(0);
                        MainActivity.mAdapter.notifyDataSetChanged();

                        break;
                    }
                }
            }

            //Removing loading screen
            MainActivity.mLoadingView.setVisibility(View.INVISIBLE);
        }

        Log.d(TAG, "Stopping foreground service.");
        stopForeground(true);
        stopSelf();
    }
}