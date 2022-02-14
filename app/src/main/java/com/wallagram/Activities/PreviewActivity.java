package com.wallagram.Activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;

import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.squareup.picasso.Picasso;
import com.wallagram.R;
import com.wallagram.Utils.Functions;
import com.wallagram.Workers.PreviewRedditPostWorker;

import java.io.IOException;
import java.net.URL;

public class PreviewActivity extends AppCompatActivity {
    private static final String TAG = "PREVIEW_ACTIVITY";

    private RelativeLayout mSetWallpaperBtn;
    private RelativeLayout mRefreshBtn;

    private SharedPreferences mSharedPreferences;
    private ImageView mPostPreview;

    private String mPostUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);

        LocalBroadcastManager.getInstance(this).registerReceiver(updatePreviewReceiver, new IntentFilter("update-preview"));

        mSharedPreferences = getApplicationContext().getSharedPreferences("Settings", Context.MODE_PRIVATE);

        mPostPreview = findViewById(R.id.postPreview);

        //Refresh Button
        mRefreshBtn = findViewById(R.id.refreshBtn);
        mRefreshBtn.setOnClickListener(view -> {
            PreviewRedditPostSingleRequest(this);
        });

        //Set wallpaper Button
        mSetWallpaperBtn = findViewById(R.id.setWallpaperBtn);
        mSetWallpaperBtn.setOnClickListener(view -> {
            setWallpaper();
        });


        PreviewRedditPostSingleRequest(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(updatePreviewReceiver);
    }

    public static void PreviewRedditPostSingleRequest(Context context) {
        String WORK_TAG = "PreviewRedditPost: Single";

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        final OneTimeWorkRequest owr = new OneTimeWorkRequest.Builder(PreviewRedditPostWorker.class)
                .addTag(WORK_TAG)
                .setConstraints(constraints)
                .build();

        androidx.work.WorkManager workManager = androidx.work.WorkManager.getInstance(context);
        workManager.enqueueUniqueWork(WORK_TAG, ExistingWorkPolicy.REPLACE, owr);
    }

    private final BroadcastReceiver updatePreviewReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mPostUrl = mSharedPreferences.getString("setPostURL", "");

            Picasso.get()
                    .load(Uri.parse(mPostUrl))
                    .into(mPostPreview);
            if(!mSetWallpaperBtn.isShown())
                mSetWallpaperBtn.setVisibility(View.VISIBLE);
        }
    };

    private void setWallpaper() {
        Log.d(TAG, "setWallpaper: Setting Wallpaper");
        String[] options = {"Home Screen", "Lock Screen", "Both"};
        int screenWidth = mSharedPreferences.getInt("screenWidth", 0);
        String setImageAlign = mSharedPreferences.getString("setImageAlign", "Centre");

        try {
            URL url = new URL(mPostUrl);
            Bitmap postImage = BitmapFactory.decodeStream(url.openConnection().getInputStream());
            WallpaperManager wallpaperManager = WallpaperManager.getInstance(getApplicationContext());

            final int desiredH = wallpaperManager.getDesiredMinimumHeight();

            Bitmap bm = scaleCrop(postImage, setImageAlign, desiredH, screenWidth);

            if (mSharedPreferences.getString("setLocation", options[0]).equalsIgnoreCase(options[0])) {
                wallpaperManager.setBitmap(bm, null, false, WallpaperManager.FLAG_SYSTEM);
            } else if (mSharedPreferences.getString("setLocation", options[0]).equalsIgnoreCase(options[1])) {
                wallpaperManager.setBitmap(bm, null, false, WallpaperManager.FLAG_LOCK);
            } else {
                wallpaperManager.setBitmap(bm, null, false, WallpaperManager.FLAG_SYSTEM);
                wallpaperManager.setBitmap(bm, null, false, WallpaperManager.FLAG_LOCK);
            }

            if (mSharedPreferences.getInt("saveWallpaper", 0) == 1) {
                Functions.savePostRequest(getApplicationContext());
            }
        } catch (IOException e) {
            Log.e(TAG, "setWallpaper:  ", e);
        }
    }

    private Bitmap scaleCrop(Bitmap postImage, String imageAlign, int screenHeight, int screenWidth) {
        Log.d(TAG, "scaleCrop: Scaling image to Height: " + screenHeight + ", to Width: " + screenWidth + " and aligned: " + imageAlign);

        int postWidth = postImage.getWidth();
        int postHeight = postImage.getHeight();

        // Compute the scaling factors to fit the new height and width, respectively.
        // To cover the final image, the final scaling will be the bigger
        // of these two.
        float xScale = (float) screenWidth / postWidth;
        float yScale = (float) screenHeight / postHeight;
        float scale = Math.max(xScale, yScale);

        // Now get the size of the source bitmap when scaled
        float scaledWidth = scale * postWidth;
        float scaledHeight = scale * postHeight;

        // Let's find out the upper left coordinates if the scaled bitmap
        // should be centered in the new size give by the parameters
        float left;
        float top;

        switch (imageAlign) {
            case "Left":
                left = 0;
                top = 0;
                break;
            case "Right":
                left = (screenWidth - scaledWidth);
                top = (screenHeight - scaledHeight);
                break;
            default:
                left = (screenWidth - scaledWidth) / 2;
                top = (screenHeight - scaledHeight) / 2;
                break;
        }

        // The target rectangle for the new, scaled version of the source bitmap will now
        // be
        Rect srcRect = new Rect(0, 0, postWidth, postHeight);
        RectF targetRect = new RectF(left, top, left + scaledWidth, top + scaledHeight);

        // Finally, we create a new bitmap of the specified size and draw our new,
        // scaled bitmap onto it.
        Bitmap dest = Bitmap.createBitmap(screenWidth, screenHeight, postImage.getConfig());
        Canvas canvas = new Canvas(dest);
        canvas.drawBitmap(postImage, srcRect, targetRect, null);

        return dest;
    }
}