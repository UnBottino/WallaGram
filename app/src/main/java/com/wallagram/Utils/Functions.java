package com.wallagram.Utils;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.wallagram.Model.Account;
import com.wallagram.R;
import com.wallagram.Receivers.AlarmReceiver;
import com.wallagram.Sqlite.SQLiteDatabaseAdapter;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Objects;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class Functions {
    private static final String TAG = "FUNCTIONS";

    public static boolean alarmActive = false;

    public static void callAlarm(Context context) {
        Log.d(TAG, "callAlarm: Activating Alarm");

        SharedPreferences sharedPreferences = context.getSharedPreferences("Settings", Context.MODE_PRIVATE);
        String metric = sharedPreferences.getString("metric", "hours");

        int convertedDuration;

        if (metric.equalsIgnoreCase("hours")) {
            convertedDuration = sharedPreferences.getInt("duration", 1) * 60;
        } else {
            convertedDuration = sharedPreferences.getInt("duration", 1) * 60 * 24;
        }

        Intent intent = new Intent(context, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 123, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 1000 * 60 * convertedDuration, pendingIntent); // Millisec * Second * Minute
        }

        alarmActive = true;
    }

    public static void cancelAlarm(Context context) {
        Log.d(TAG, "cancelAlarm: Deactivating Alarm");

        Intent intent = new Intent(context, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 123, intent, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }

        alarmActive = false;
    }

    public static boolean isNetworkAvailable(Context context) {
        Log.d(TAG, "isNetworkAvailable: Checking device network status");

        ConnectivityManager conn = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = null;
        if (conn != null) {
            networkInfo = conn.getActiveNetworkInfo();
        }
        return null != networkInfo && networkInfo.isConnected();
    }

    public static List<Account> getDBAccounts(Context context) {
        SQLiteDatabaseAdapter db = new SQLiteDatabaseAdapter(context);
        return db.getAllAccounts();
    }

    public static void removeDBAccounts(Context context) {
        Log.d(TAG, "removeDBAccounts: Removing all accounts from DB");

        SQLiteDatabaseAdapter db = new SQLiteDatabaseAdapter(context);
        db.deleteAll();
    }

    public static void removeDBAccountByName(Context context, String accountName) {
        Log.d(TAG, "removeDBAccountByName: Removing " + accountName + "'s information from DB");

        SQLiteDatabaseAdapter db = new SQLiteDatabaseAdapter(context);
        db.deleteAccount(accountName);
    }

    public static void requestPermission(Activity activity) {
        ActivityCompat.requestPermissions(activity, new String[]{WRITE_EXTERNAL_STORAGE}, 1);
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    public static boolean checkImageExists(String path, String imageName, Context context) {
        Log.d(TAG, "checkImageExists: Looking for " + imageName + " in users gallery");

        String selection = MediaStore.Files.FileColumns.RELATIVE_PATH + " like ? and " + MediaStore.Files.FileColumns.DISPLAY_NAME + " like ?";
        String[] selectionArgs = {"%" + path + "%", "%" + imageName + "%"};

        Cursor cursor = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null, selection, selectionArgs, null);

        boolean exist = false;
        if (cursor != null && cursor.getCount() > 0)
            exist = true;

        assert cursor != null;
        cursor.close();

        return exist;
    }

    public static void savePost(final Context context, final String postUrl) {
        Handler uiHandler = new Handler(Looper.getMainLooper());
        uiHandler.post(() -> Picasso.get()
                .load(postUrl)
                .into(new Target() {
                    @RequiresApi(api = Build.VERSION_CODES.Q)
                    @Override
                    public void onBitmapLoaded(final Bitmap bitmap, Picasso.LoadedFrom from) {
                        Log.d(TAG, "savePost: onBitmapLoaded: Saving image to gallery");

                        String imageName = postUrl.substring(postUrl.length() - 50);

                        OutputStream fos;

                        String path = "Pictures/" + context.getResources().getString(R.string.app_name);

                        ContentResolver resolver = context.getContentResolver();
                        ContentValues contentValues = new ContentValues();
                        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, imageName);
                        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
                        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, path);

                        if (!Functions.checkImageExists(path, imageName, context)) {
                            Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
                            try {
                                assert imageUri != null;
                                fos = resolver.openOutputStream(imageUri);
                                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                                assert fos != null;
                                fos.flush();
                                fos.close();
                            } catch (IOException e) {
                                Log.w("savePost", e.toString());
                            }
                        }
                    }

                    @Override
                    public void onBitmapFailed(Exception e, Drawable errorDrawable) {
                        Log.e(TAG, "onBitmapFailed: " + Objects.requireNonNull(e.getMessage()));
                    }

                    @Override
                    public void onPrepareLoad(Drawable placeHolderDrawable) {
                    }
                }));
    }

    public static void enableApply(View v) {
        v.setAlpha(1);
        v.setEnabled(true);
    }

    public static void disableApply(View v) {
        v.setAlpha((float) 0.5);
        v.setEnabled(false);
    }

    public static void showNotification(Context context, String title, String text) {
        Log.d(TAG, "showNotification: Title: " + title + ", Text: " + text);

        String CHANNEL_ID = "com.wallagram.nofication";
        String CHANNEL_NAME = "WallaGram Notification";
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
        channel.enableVibration(true);
        channel.enableLights(true);

        Objects.requireNonNull(notificationManager).createNotificationChannel(channel);

        NotificationCompat.Builder notificationBuilder;

        notificationBuilder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setPriority(NotificationManager.IMPORTANCE_DEFAULT)
                .setAutoCancel(true)
                .setColor(ContextCompat.getColor(context, R.color.purple))
                .setSmallIcon(R.drawable.notification)
                .setContentTitle(title)
                .setContentText(text);

        notificationManager.notify(CHANNEL_ID, 421, notificationBuilder.build());
    }

    public static void getScreenSize(Activity activity) {
        DisplayMetrics metrics = new DisplayMetrics();
        Display display = activity.getWindowManager().getDefaultDisplay();
        display.getMetrics(metrics);
        final int screenWidth = metrics.widthPixels;
        final int screenHeight = metrics.heightPixels;

        Log.d(TAG, "getScreenSize: Screen Width: " + screenWidth);
        Log.d(TAG, "getScreenSize: Screen Height: " + screenHeight);

        SharedPreferences sharedPreferences = activity.getSharedPreferences("Settings", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("screenWidth", screenWidth);
        editor.putInt("screenHeight", screenHeight);
        editor.apply();

        Log.d(TAG, "getScreenSize: Global screen dimensions set");
    }
}