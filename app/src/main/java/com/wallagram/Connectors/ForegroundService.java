package com.wallagram.Connectors;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.squareup.picasso.Picasso;
import com.wallagram.Activities.MainActivity;
import com.wallagram.Model.Account;
import com.wallagram.R;
import com.wallagram.Sqlite.SQLiteDatabaseAdapter;
import com.wallagram.Utils.Functions;

import java.util.Objects;

public class ForegroundService extends Service {
    public static final String ACTION_START_FOREGROUND_SERVICE = "ACTION_START_FOREGROUND_SERVICE";
    public static final String ACTION_STOP_FOREGROUND_SERVICE = "ACTION_STOP_FOREGROUND_SERVICE";

    private static final String TAG = "FOREGROUND_SERVICE";

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
                    Log.d(TAG, "Starting Foreground");
                    startForeground();

                    Intent serviceIntent = new Intent(this, IntentService.class);
                    startService(serviceIntent);

                    break;
                case ACTION_STOP_FOREGROUND_SERVICE:
                    stopForegroundService();
                    break;
            }
        } else {
            Log.e(TAG, "Foreground intent has no action");
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
        SharedPreferences sharedPreferences = this.getSharedPreferences("Settings", Context.MODE_PRIVATE);

        if (!error) {
            String setAccountName = sharedPreferences.getString("setAccountName", "");
            String setProfilePic = sharedPreferences.getString("setProfilePic", "");
            //String previousPostURL = sharedPreferences.getString("previousPostURL", "");
            String setPostURL = sharedPreferences.getString("setPostURL", "");

            //boolean settingsUpdated = sharedPreferences.getBoolean("settingsUpdated", false);

            //if (!previousPostURL.equalsIgnoreCase(setPostURL) || settingsUpdated) {
            Log.d(TAG, "Setting Wallpaper");
            Functions.setWallpaper(this, setPostURL);

            //Resetting settings update check
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("settingsUpdated", false);
            editor.apply();

            if (sharedPreferences.getInt("saveWallpaper", 0) == 1) {
                Log.d(TAG, "Saving Post");
                Functions.savePost(this, setPostURL);
            }

            if (MainActivity.IS_APP_IN_FOREGROUND) {
                Log.d(TAG, "Setting Profile Pic");
                Picasso.get()
                        .load(Uri.parse(setProfilePic))
                        .into(MainActivity.mSetProfilePic);

                Log.d(TAG, "Setting Display Name");
                MainActivity.mSetAccountName.setText(setAccountName);

                SQLiteDatabaseAdapter db = new SQLiteDatabaseAdapter(this);

                Account account = new Account(setAccountName, setProfilePic);

                if (!db.checkIfAccountExists(account)) {
                    db.addAccount(account);

                    MainActivity.mDBAccountList.add(0, account);
                    MainActivity.mAdapter.notifyItemInserted(0);
                    MainActivity.mAdapter.notifyDataSetChanged();
                } else {
                    Log.d(TAG, "Account name already in db (" + setAccountName + ")");

                    //int position =  0;
                    for (Account a : MainActivity.mDBAccountList) {
                        if (a.getAccountName().equalsIgnoreCase(account.getAccountName())) {
                            //Not using update because of the ordering in recyclerView
                            db.deleteAccount(a.getAccountName());
                            db.addAccount(account);

                            //Cleaner code but items loading in is visible to user as process is slower
                                /*MainActivity.mDBAccountList.remove(a);
                                MainActivity.mDBAccountList.add(0, account);
                                MainActivity.mAdapter.notifyItemMoved(position, 0);*/

                            MainActivity.mDBAccountList.remove(a);
                            MainActivity.mAdapter.notifyItemRemoved(MainActivity.mDBAccountList.indexOf(a));
                            MainActivity.mDBAccountList.add(0, account);
                            MainActivity.mAdapter.notifyItemInserted(0);
                            MainActivity.mAdapter.notifyDataSetChanged();
                            break;
                        }
                        //position++;
                    }
                }

                Objects.requireNonNull(MainActivity.mRecyclerView.getLayoutManager()).scrollToPosition(0);
            }

            if (MainActivity.IS_APP_IN_FOREGROUND) {
                MainActivity.mLoadingView.setVisibility(View.INVISIBLE);
            }
        } else {
            if (MainActivity.IS_APP_IN_FOREGROUND) {
                Log.d(TAG, "Setting Error Profile Pic");
                Picasso.get()
                        .load(R.drawable.frown_straight)
                        .into(MainActivity.mSetProfilePic);

                Log.d(TAG, "Setting Error Display Name");
                String setAccountName = sharedPreferences.getString("setAccountName", "");
                MainActivity.mSetAccountName.setText(setAccountName);

                MainActivity.mLoadingView.setVisibility(View.INVISIBLE);
            }

            Functions.cancelAlarm(this);
        }

        Log.d(TAG, "Stopping foreground service.");
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
}