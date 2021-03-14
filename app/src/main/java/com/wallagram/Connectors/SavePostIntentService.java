package com.wallagram.Connectors;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.wallagram.R;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;

public class SavePostIntentService extends android.app.IntentService {
    private static final String TAG = "SAVE_POST_INTENT_SERVICE";

    public SavePostIntentService() {
        super("name");
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        String postUrl = intent.getStringExtra("postUrl");

        try {
            Log.d(TAG, "onHandleIntent: Fetching bitmap from postUrl");
            URL url = new URL(postUrl);
            Bitmap bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream());

            String imageName = postUrl.substring(postUrl.length() - 50);

            OutputStream fos;

            String path = "Pictures/" + getResources().getString(R.string.app_name);

            ContentResolver resolver = getContentResolver();
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, imageName);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, path);

            if (!checkImageExists(path, imageName, getApplicationContext())) {
                Log.d(TAG, "onHandleIntent: Saving Image to gallery");
                Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
                try {
                    assert imageUri != null;
                    fos = resolver.openOutputStream(imageUri);
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                    assert fos != null;
                    fos.flush();
                    fos.close();
                } catch (IOException e) {
                    Log.e(TAG, "onHandleIntent: " + e.toString());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "onHandleIntent: " + e.getMessage());
        }
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
