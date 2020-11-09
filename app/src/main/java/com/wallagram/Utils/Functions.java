package com.wallagram.Utils;

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import com.wallagram.Model.Account;
import com.wallagram.R;
import com.wallagram.Sqlite.SQLiteDatabaseAdapter;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class Functions {
    public static List<Account> getDBAccounts(Context context){
        Log.d("DB","Getting all account names from DB");
        SQLiteDatabaseAdapter db = new SQLiteDatabaseAdapter(context);
        return db.getAllAccounts();
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
        Picasso.get()
                .load(postUrl)
                .into(new Target() {
                    @RequiresApi(api = Build.VERSION_CODES.Q)
                    @Override
                    public void onBitmapLoaded(final Bitmap bitmap, Picasso.LoadedFrom from) {

                        WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);
                        try {
                            wallpaperManager.setBitmap(bitmap);
                        } catch (IOException ex) { ex.printStackTrace(); }
                    }

                    @Override
                    public void onBitmapFailed(Exception e, Drawable errorDrawable) {
                    }

                    @Override
                    public void onPrepareLoad(Drawable placeHolderDrawable) {
                    }
                });
    }

    public static void savePost(final Context context, final String postUrl){
        Picasso.get()
                .load(postUrl)
                .into(new Target() {
                    @RequiresApi(api = Build.VERSION_CODES.Q)
                    @Override
                    public void onBitmapLoaded(final Bitmap bitmap, Picasso.LoadedFrom from) {
                        String imageName = postUrl.substring(postUrl.length() - 50);

                        OutputStream fos;

                        String path = "DCIM/" + context.getResources().getString(R.string.app_name);

                        ContentResolver resolver = context.getContentResolver();
                        ContentValues contentValues = new ContentValues();
                        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, imageName);
                        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
                        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, path);

                        if (!com.wallagram.Utils.Functions.checkImageExists(path, imageName, context)) {
                            Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
                            try {
                                fos = resolver.openOutputStream(imageUri);
                                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                                fos.flush();
                                fos.close();
                            } catch (IOException e) {
                                Log.w("savePost", e.toString());
                            }
                        }
                    }

                    @Override
                    public void onBitmapFailed(Exception e, Drawable errorDrawable) {
                        Log.v("savePost", e.getMessage());
                    }

                    @Override
                    public void onPrepareLoad(Drawable placeHolderDrawable) {
                    }
                });
    }
}
