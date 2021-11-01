package com.wallagram.Activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.squareup.picasso.Picasso;
import com.wallagram.R;
import com.wallagram.Workers.PreviewRedditPostWorker;

public class PreviewActivity extends AppCompatActivity {
    private static final String TAG = "PREVIEW_ACTIVITY";

    private SharedPreferences sharedPreferences;
    private ImageView mPostPreview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);

        LocalBroadcastManager.getInstance(this).registerReceiver(updatePreviewReceiver, new IntentFilter("update-preview"));

        sharedPreferences = getApplicationContext().getSharedPreferences("Settings", Context.MODE_PRIVATE);

        mPostPreview = findViewById(R.id.postPreview);

        //Refresh Button
        RelativeLayout mRefreshBtn = findViewById(R.id.refreshBtn);
        mRefreshBtn.setOnClickListener(view -> {
            PreviewRedditPostSingleRequest(this);
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
            String setPostURL = sharedPreferences.getString("setPostURL", "");

            Picasso.get()
                    .load(Uri.parse(setPostURL))
                    .into(mPostPreview);
        }
    };
}