package com.wallagram.Connectors;

import android.app.WallpaperManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.wallagram.Utils.Functions;

import java.io.IOException;
import java.net.URL;

public class WorkerSetWallpaper extends Worker {
    private static final String TAG = "WORK_MANAGER";

    private final SharedPreferences sharedPreferences;

    private final String mPostUrl;

    public WorkerSetWallpaper(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);

        sharedPreferences = getApplicationContext().getSharedPreferences("Settings", 0);
        mPostUrl = sharedPreferences.getString("setPostURL", "");
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @NonNull
    @Override
    public Result doWork() {
        try {
            Log.d(TAG, "onHandleIntent: Setting Wallpaper");
            URL url = new URL(mPostUrl);
            Bitmap bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream());
            WallpaperManager wallpaperManager = WallpaperManager.getInstance(getApplicationContext());

            int screenWidth = sharedPreferences.getInt("screenWidth", 0);
            int screenHeight = sharedPreferences.getInt("screenHeight", 0);

            int imageAlign = sharedPreferences.getInt("align", 1);

            Bitmap bm = scaleCrop(bitmap, imageAlign, screenHeight, screenWidth);

            if (sharedPreferences.getInt("location", 0) == 0) {
                wallpaperManager.setBitmap(bm, null, true, WallpaperManager.FLAG_SYSTEM);
            } else if (sharedPreferences.getInt("location", 0) == 1) {
                wallpaperManager.setBitmap(bm, null, true, WallpaperManager.FLAG_LOCK);
            } else {
                wallpaperManager.setBitmap(bm, null, true, WallpaperManager.FLAG_SYSTEM);
                wallpaperManager.setBitmap(bm, null, true, WallpaperManager.FLAG_LOCK);
            }
        } catch (IOException ex) {
            Log.e(TAG, "onHandleIntent: settingWallpaper Failed:  " + ex.getLocalizedMessage());
        }

        if (sharedPreferences.getInt("saveWallpaper", 0) == 1) {
            Functions.savePostRequest(getApplicationContext());
        }

        return Result.success();
    }

    public static Bitmap scaleCrop(Bitmap source, int imageAlign, int newHeight, int newWidth) {
        Log.d(TAG, "scaleCrop: Scaling image to Height: " + newHeight + ", to Width: " + newWidth + " and aligned: " + imageAlign);

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

        // Let's find out the upper left coordinates if the scaled bitmap
        // should be centered in the new size give by the parameters
        float left;
        float top;

        switch (imageAlign) {
            case 0:
                left = 0;
                top = 0;
                break;
            case 2:
                left = (newWidth - scaledWidth);
                top = (newHeight - scaledHeight);
                break;
            default:
                left = (newWidth - scaledWidth) / 2;
                top = (newHeight - scaledHeight) / 2;
                break;
        }

        // The target rectangle for the new, scaled version of the source bitmap will now
        // be
        Rect srcRect = new Rect(0, 0, sourceWidth, sourceHeight);
        RectF targetRect = new RectF(left, top, left + scaledWidth, top + scaledHeight);

        // Finally, we create a new bitmap of the specified size and draw our new,
        // scaled bitmap onto it.
        Bitmap dest = Bitmap.createBitmap(newWidth, newHeight, source.getConfig());
        Canvas canvas = new Canvas(dest);
        canvas.drawBitmap(source, srcRect, targetRect, null);

        return dest;
    }
}