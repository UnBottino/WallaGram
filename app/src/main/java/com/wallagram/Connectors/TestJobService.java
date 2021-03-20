package com.wallagram.Connectors;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.util.Log;

public class TestJobService extends JobService {
    private static final String TAG = "ExampleJobService";

    @Override
    public boolean onStartJob(JobParameters params) {
        Log.d(TAG, "Job started");
        doBackgroundWork(params);

        return true;
    }

    private void doBackgroundWork(final JobParameters params) {
        new Thread(() -> {
            Intent i = new Intent(this, ForegroundService.class);
            i.setAction(ForegroundService.ACTION_START_FOREGROUND_SERVICE);
            this.startForegroundService(i);

            Log.d(TAG, "Job finished");
            jobFinished(params, false);
        }).start();
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Log.d(TAG, "Job cancelled before completion");
        return true;
    }
}