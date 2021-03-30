package com.wallagram.Workers;

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
import java.util.Objects;

public class SavePostWorker extends Worker {
    private static final String TAG = "WORKER_SAVE_POST";

    private final String mPostUrl;
    private final String mImageName;

    public SavePostWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);

        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("Settings", 0);
        mPostUrl = sharedPreferences.getString("setPostURL", "");
        mImageName = sharedPreferences.getString("setImageName", "");
    }

    // TODO: 22/03/2021 Make Backwards compatible
    @RequiresApi(api = Build.VERSION_CODES.Q)
    @NonNull
    @Override
    public Result doWork() {
        try {
            Log.d(TAG, "Fetching bitmap from postUrl");
            URL url = new URL(mPostUrl);
            Bitmap bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream());

            OutputStream fos = null;

            String path = "Pictures/" + getApplicationContext().getResources().getString(R.string.app_name);

            ContentResolver resolver = getApplicationContext().getContentResolver();
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, mImageName);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, path);

            if (!checkImageExists(path, mImageName, getApplicationContext())) {
                Log.d(TAG, "Saving Image to gallery");
                Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
                try {
                    fos = resolver.openOutputStream(Objects.requireNonNull(imageUri));
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                } catch (IOException e) {
                    Log.e(TAG, "Adding to gallery: " + e.toString());
                } finally {
                    if (fos != null) {
                        fos.flush();
                        fos.close();
                    }
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

        Objects.requireNonNull(cursor).close();

        if (exist) {
            Log.d(TAG, "checkImageExists: Image Found in gallery");
        } else {
            Log.d(TAG, "checkImageExists: Image Not Found in gallery");
        }

        return exist;
    }
}