package com.wallagram.Connectors;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.wallagram.R;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;

public class WorkerSavePost extends Worker {
    private static final String TAG = "WORK_MANAGER";

    private final String mPostUrl;
    private final String mImageName;

    public WorkerSavePost(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);

        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("Settings", 0);
        mPostUrl = sharedPreferences.getString("setPostURL", "");
        mImageName = sharedPreferences.getString("setImageName", "");
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @NonNull
    @Override
    public Result doWork() {
        try {
            Log.d(TAG, "onHandleIntent: Fetching bitmap from postUrl");
            URL url = new URL(mPostUrl);
            Bitmap bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream());

            OutputStream fos;

            String path = "Pictures/" + getApplicationContext().getResources().getString(R.string.app_name);

            ContentResolver resolver = getApplicationContext().getContentResolver();
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, mImageName);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, path);

            if (!checkImageExists(path, mImageName, getApplicationContext())) {
                Log.d(TAG, "doWork: Saving Image to gallery");
                Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
                try {
                    assert imageUri != null;
                    fos = resolver.openOutputStream(imageUri);
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                    assert fos != null;
                    fos.flush();
                    fos.close();
                } catch (IOException e) {
                    Log.e(TAG, "Adding to gallery: " + e.toString());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "doWork: " + e.getMessage());
        }

        return Result.success();
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

        if (exist) {
            Log.d(TAG, "checkImageExists: Image Found in gallery");
        } else {
            Log.d(TAG, "checkImageExists: Image Not Found in gallery");
        }

        return exist;
    }
}