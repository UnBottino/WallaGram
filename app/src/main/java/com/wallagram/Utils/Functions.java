package com.wallagram.Utils;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.SpannableString;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;

import com.wallagram.Workers.FetchSuggestionsWorker;
import com.wallagram.Workers.FindNewPostWorker;
import com.wallagram.Workers.FindNewRedditPostWorker;
import com.wallagram.Workers.RefreshProfilePicsWorker;
import com.wallagram.Workers.SavePostWorker;
import com.wallagram.Model.PreviousAccount;
import com.wallagram.R;
import com.wallagram.Sqlite.SQLiteDatabaseAdapter;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class Functions {
    private static final String TAG = "FUNCTIONS";

    public static void findNewRedditPostSingleRequest(Context context) {
        String WORK_TAG = "findNewRedditPost: Single";

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        final OneTimeWorkRequest owr = new OneTimeWorkRequest.Builder(FindNewRedditPostWorker.class)
                .addTag(WORK_TAG)
                .setConstraints(constraints)
                .build();

        androidx.work.WorkManager workManager = androidx.work.WorkManager.getInstance(context);
        workManager.enqueueUniqueWork(WORK_TAG, ExistingWorkPolicy.REPLACE, owr);
    }

    public static void findNewRedditPostPeriodicRequest(Context context) {
        String WORK_TAG = "findNewRedditPost: Periodic";

        SharedPreferences sharedPreferences = context.getSharedPreferences("Settings", Context.MODE_PRIVATE);
        String metric = sharedPreferences.getString("metric", "hours");

        int convertedDuration;

        if (metric.equalsIgnoreCase("hours")) {
            convertedDuration = sharedPreferences.getInt("duration", 1) * 60;
        } else {
            convertedDuration = sharedPreferences.getInt("duration", 1) * 60 * 24;
        }

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        final PeriodicWorkRequest pwr = new PeriodicWorkRequest.Builder(FindNewRedditPostWorker.class, convertedDuration, TimeUnit.MINUTES)
                .addTag(WORK_TAG)
                .setConstraints(constraints)
                .build();

        androidx.work.WorkManager workManager = androidx.work.WorkManager.getInstance(context);
        workManager.enqueueUniquePeriodicWork(WORK_TAG, ExistingPeriodicWorkPolicy.REPLACE, pwr);
    }

    public static void refreshImagesSingleRequest(Context context) {
        String WORK_TAG = "refreshImages: Single";

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        final OneTimeWorkRequest owr = new OneTimeWorkRequest.Builder(RefreshProfilePicsWorker.class)
                .addTag(WORK_TAG)
                .setConstraints(constraints)
                .build();

        androidx.work.WorkManager workManager = androidx.work.WorkManager.getInstance(context);
        workManager.enqueueUniqueWork(WORK_TAG, ExistingWorkPolicy.REPLACE, owr);
    }

    public static void findNewPostPeriodicRequest(Context context) {
        String WORK_TAG = "findNewPost: Periodic";

        SharedPreferences sharedPreferences = context.getSharedPreferences("Settings", Context.MODE_PRIVATE);
        String metric = sharedPreferences.getString("metric", "hours");

        int convertedDuration;

        if (metric.equalsIgnoreCase("hours")) {
            convertedDuration = sharedPreferences.getInt("duration", 1) * 60;
        } else {
            convertedDuration = sharedPreferences.getInt("duration", 1) * 60 * 24;
        }

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        final PeriodicWorkRequest pwr = new PeriodicWorkRequest.Builder(FindNewPostWorker.class, convertedDuration, TimeUnit.MINUTES)
                .addTag(WORK_TAG)
                .setConstraints(constraints)
                .build();

        androidx.work.WorkManager workManager = androidx.work.WorkManager.getInstance(context);
        workManager.enqueueUniquePeriodicWork(WORK_TAG, ExistingPeriodicWorkPolicy.REPLACE, pwr);
    }

    public static void findNewPostSingleRequest(Context context) {
        String WORK_TAG = "findNewPost: Single";

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        final OneTimeWorkRequest owr = new OneTimeWorkRequest.Builder(FindNewPostWorker.class)
                .addTag(WORK_TAG)
                .setConstraints(constraints)
                .build();

        androidx.work.WorkManager workManager = androidx.work.WorkManager.getInstance(context);
        workManager.enqueueUniqueWork(WORK_TAG, ExistingWorkPolicy.REPLACE, owr);
    }

    public static void fetchSuggestionsRequest(Context context) {
        String WORK_TAG = "fetchSuggestions:";

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        final OneTimeWorkRequest owr = new OneTimeWorkRequest.Builder(FetchSuggestionsWorker.class)
                .addTag(WORK_TAG)
                .setConstraints(constraints)
                .build();

        androidx.work.WorkManager workManager = androidx.work.WorkManager.getInstance(context);
        workManager.enqueueUniqueWork(WORK_TAG, ExistingWorkPolicy.REPLACE, owr);
    }

    public static void savePostRequest(Context context) {
        String WORK_TAG = "savePost";

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        final OneTimeWorkRequest owr = new OneTimeWorkRequest.Builder(SavePostWorker.class)
                .addTag(WORK_TAG)
                .setConstraints(constraints)
                .build();

        androidx.work.WorkManager workManager = androidx.work.WorkManager.getInstance(context);
        workManager.enqueueUniqueWork(WORK_TAG, ExistingWorkPolicy.REPLACE, owr);
    }

    public static boolean isNetworkAvailable(Context context) {
        Log.d(TAG, "isNetworkAvailable: Checking device network status");

        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = Objects.requireNonNull(connectivityManager).getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public static List<PreviousAccount> getDBAccounts(Context context) {
        SQLiteDatabaseAdapter db = new SQLiteDatabaseAdapter(context);
        return db.getAllAccounts();
    }

    public static List<PreviousAccount> getDBInstaAccounts(Context context) {
        SQLiteDatabaseAdapter db = new SQLiteDatabaseAdapter(context);
        return db.getAllInstaAccounts();
    }

    public static void updateProfilePicURL(Context context, String accountType, String accountName, String newProfilePicURL) {
        SQLiteDatabaseAdapter db = new SQLiteDatabaseAdapter(context);
        db.updateProfilePicURL(accountType, accountName, newProfilePicURL);
    }

    public static void removeDBAccounts(Context context) {
        Log.d(TAG, "removeDBAccounts: Removing all accounts from DB");

        SQLiteDatabaseAdapter db = new SQLiteDatabaseAdapter(context);
        db.deleteAll();
    }

    public static void removeDBAccount(Context context, String accountType, String accountName) {
        Log.d(TAG, "removeDBAccountByName: Removing " + accountName + "'s information from DB");

        SQLiteDatabaseAdapter db = new SQLiteDatabaseAdapter(context);
        db.deleteAccount(accountType, accountName);
    }

    public static void requestPermission(Activity activity) {
        ActivityCompat.requestPermissions(activity, new String[]{WRITE_EXTERNAL_STORAGE}, 1);
    }

    public static void enableApply(View v) {
        v.setAlpha(1);
        v.setEnabled(true);
    }

    public static void disableApply(View v) {
        v.setAlpha((float) 0.24);
        v.setEnabled(false);
    }

    public static void popupMsg(Activity activity, SpannableString title, SpannableString msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setCancelable(true);

        LayoutInflater inflater = activity.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_info, null);
        builder.setView(dialogView);
        TextView infoTitle = dialogView.findViewById(R.id.infoTitle);
        TextView infoMsg = dialogView.findViewById(R.id.infoMsg);

        AlertDialog dialog = builder.create();
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.show();

        infoTitle.setText(title);
        infoMsg.setText(msg);
        dialogView.setOnClickListener(view -> dialog.cancel());
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
                .setSmallIcon(R.drawable.notification)
                .setContentTitle(title)
                .setContentText(text);

        notificationManager.notify(CHANNEL_ID, 421, notificationBuilder.build());
    }
}