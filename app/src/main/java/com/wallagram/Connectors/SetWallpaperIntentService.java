package com.wallagram.Connectors;

import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.IOException;
import java.net.URL;

public class SetWallpaperIntentService extends android.app.IntentService {
    private static final String TAG = "SET_WALLPAPER_INTENT_SERVICE";

    public SetWallpaperIntentService() {
        super("name");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        String postUrl = intent.getStringExtra("postUrl");

        try {
            Log.d(TAG, "onHandleIntent: Setting Wallpaper");
            URL url = new URL(postUrl);
            Bitmap bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream());
            WallpaperManager wallpaperManager = WallpaperManager.getInstance(getApplicationContext());

            SharedPreferences sharedPreferences = getSharedPreferences("Settings", Context.MODE_PRIVATE);
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

        Intent i = new Intent(this, ForegroundService.class);
        i.setAction(ForegroundService.ACTION_STOP_FOREGROUND_SERVICE);
        startForegroundService(i);
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
