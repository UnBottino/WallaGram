package com.wallagram.Receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import com.wallagram.AsyncTasks.NewBgTask;

public class AlarmReceiver extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent) {
        NewBgTask testAsyncTask = new NewBgTask(context);
        testAsyncTask.execute();
    }
}

