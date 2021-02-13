package com.wallagram.Utils;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.WallpaperManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.wallagram.MainActivity;
import com.wallagram.Model.Account;
import com.wallagram.R;
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

    public static List<Account> getDBAccounts(Context context){
        SQLiteDatabaseAdapter db = new SQLiteDatabaseAdapter(context);
        return db.getAllAccounts();
    }

    public static void removeDBAccounts(Context context){
        Log.d("DB","Remove all account names from DB");
        SQLiteDatabaseAdapter db = new SQLiteDatabaseAdapter(context);
        db.deleteAll();
    }

    public static void requestPermission(Activity activity){
        ActivityCompat.requestPermissions(activity, new String[]{WRITE_EXTERNAL_STORAGE}, 1);
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    public static boolean checkImageExists(String path, String imageName, Context context){
        String selection = MediaStore.Files.FileColumns.RELATIVE_PATH + " like ? and "+ MediaStore.Files.FileColumns.DISPLAY_NAME + " like ?";
        String[] selectionArgs = {"%" + path + "%", "%" + imageName + "%"};

        Cursor cursor = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null, selection, selectionArgs, null);

        boolean exist = false;
        if(cursor != null && cursor.getCount() > 0)
            exist = true;

        assert cursor != null;
        cursor.close();

        return exist;
    }

    public static void setWallpaper(final Context context, final String postUrl){
        Handler uiHandler = new Handler(Looper.getMainLooper());
        uiHandler.post(() -> Picasso.get()
                .load(postUrl)
                .into(new Target() {
                    @RequiresApi(api = Build.VERSION_CODES.Q)
                    @Override
                    public void onBitmapLoaded(final Bitmap bitmap, Picasso.LoadedFrom from) {

                        WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);
                        try {
                            SharedPreferences sharedPreferences = context.getSharedPreferences("Settings", Context.MODE_PRIVATE);
                            int screenWidth = sharedPreferences.getInt("screenWidth", 0);
                            int screenHeight = sharedPreferences.getInt("screenHeight", 0);

                            Bitmap bm = scaleCrop(bitmap, 0, screenHeight, screenWidth);

                            if(sharedPreferences.getInt("location", 0) == 0){
                                wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM);
                            }
                            else  if(sharedPreferences.getInt("location", 0) == 1){
                                wallpaperManager.setBitmap(bm, null, true, WallpaperManager.FLAG_LOCK);
                            }
                            else {
                                wallpaperManager.setBitmap(bitmap);
                            }
                        } catch (IOException ex) { ex.printStackTrace(); }
                    }

                    @Override
                    public void onBitmapFailed(Exception e, Drawable errorDrawable) {
                    }

                    @Override
                    public void onPrepareLoad(Drawable placeHolderDrawable) {
                    }
                }));


    }

    public static Bitmap scaleCrop(Bitmap source, int pos, int newHeight, int newWidth) {
        int sourceWidth = source.getWidth();
        int sourceHeight = source.getHeight();

        // Compute the scaling factors to fit the new height and width, respectively.
        // To cover the final image, the final scaling will be the bigger
        // of these two.
        float xScale = (float) newWidth / sourceWidth;
        float yScale = (float) newHeight / sourceHeight;
        float scale = Math.max(xScale, yScale);

        // Now get the size of the source bitmap when scaled
        float scaledWidth = scale * sourceWidth;
        float scaledHeight = scale * sourceHeight;

        float left;
        float top;

        if(pos == 0){
            left = 0;
            top = 0;
        } else {
            // Let's find out the upper left coordinates if the scaled bitmap
            // should be centered in the new size give by the parameters
            left = (newWidth - scaledWidth) / 2;
            top = (newHeight - scaledHeight) / 2;
        }

        // The target rectangle for the new, scaled version of the source bitmap will now
        // be
        RectF targetRect = new RectF(left, top, left + scaledWidth, top + scaledHeight);

        // Finally, we create a new bitmap of the specified size and draw our new,
        // scaled bitmap onto it.
        Bitmap dest = Bitmap.createBitmap(newWidth, newHeight, source.getConfig());
        Canvas canvas = new Canvas(dest);
        canvas.drawBitmap(source, null, targetRect, null);

        return dest;
    }

    public static void savePost(final Context context, final String postUrl){
        Handler uiHandler = new Handler(Looper.getMainLooper());
        uiHandler.post(() -> Picasso.get()
                .load(postUrl)
                .into(new Target() {
                    @RequiresApi(api = Build.VERSION_CODES.Q)
                    @Override
                    public void onBitmapLoaded(final Bitmap bitmap, Picasso.LoadedFrom from) {
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
                        Log.v("savePost", Objects.requireNonNull(e.getMessage()));
                    }

                    @Override
                    public void onPrepareLoad(Drawable placeHolderDrawable) {
                    }
                }));
    }

    public static void showNotification(Context context){
        String CHANNEL_ID = "com.wallagram.testnofication";
        String CHANNEL_NAME = "Notification";
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
        channel.enableVibration(true);
        channel.setLightColor(Color.WHITE);
        channel.enableLights(true);

        notificationManager.createNotificationChannel(channel);

        NotificationCompat.Builder notificationBuilder;

        notificationBuilder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setVibrate(new long[]{0, 100})
                .setPriority(NotificationManager.IMPORTANCE_HIGH)
                .setLights(Color.BLUE, 3000, 3000)
                .setAutoCancel(true)
                .setSmallIcon(R.drawable.frown_straight)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(),
                        R.drawable.frown_straight))
                .setContentTitle("title")
                .setContentText("text");

        notificationManager.notify(CHANNEL_ID, 421, notificationBuilder.build());
    }
}
