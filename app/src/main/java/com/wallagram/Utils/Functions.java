package com.wallagram.Utils;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.Layout;
import android.text.SpannableString;
import android.text.style.AlignmentSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.wallagram.Connectors.TestJobService;
import com.wallagram.Model.Account;
import com.wallagram.R;
import com.wallagram.Receivers.AlarmReceiver;
import com.wallagram.Sqlite.SQLiteDatabaseAdapter;

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

        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
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

    public static void enableApply(View v) {
        v.setAlpha(1);
        v.setEnabled(true);
    }

    public static void disableApply(View v) {
        v.setAlpha((float) 0.5);
        v.setEnabled(false);
    }

    public static void popupMsg(Activity activity, SpannableString title, SpannableString msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.AlertDialogCustom);
        builder.setCancelable(true);

        LayoutInflater inflater = activity.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.info_dialog, null);
        builder.setView(dialogView);

        TextView alertInfoBtn = dialogView.findViewById(R.id.alertInfoBtn);

        // alert dialog title align center
        title.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), 0, title.length(), 0);
        // alert dialog msg align center
        msg.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), 0, msg.length(), 0);

        builder.setTitle(title);
        builder.setMessage(msg);

        AlertDialog dialog = builder.create();
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.show();

        alertInfoBtn.setOnClickListener(v -> dialog.cancel());
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










    public static void scheduleJob(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("Settings", Context.MODE_PRIVATE);
        String metric = sharedPreferences.getString("metric", "hours");

        int convertedDuration;

        if (metric.equalsIgnoreCase("hours")) {
            convertedDuration = sharedPreferences.getInt("duration", 1) * 60;
        } else {
            convertedDuration = sharedPreferences.getInt("duration", 1) * 60 * 24;
        }

        ComponentName componentName = new ComponentName(context, TestJobService.class);
        JobInfo info = new JobInfo.Builder(123, componentName)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPersisted(true)
                .setPeriodic(1000 * 60 * convertedDuration)
                .build();

        JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

        int resultCode = scheduler.schedule(info);
        if (resultCode == JobScheduler.RESULT_SUCCESS) {
            Log.d(TAG, "Job scheduled");
            alarmActive = true;
        } else {
            Log.d(TAG, "Job scheduling failed");
            Functions.showNotification(context, "Search Failure", "No network connection found");
        }
    }

    public static void cancelJob(Activity activity) {
        JobScheduler scheduler = (JobScheduler) activity.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        scheduler.cancel(123);
        Log.d(TAG, "Job cancelled");
    }
}