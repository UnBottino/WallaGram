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
        SharedPreferences sharedPreferences = context.getSharedPreferences("SET_ACCOUNT", 0);
        String searchName = sharedPreferences.getString("searchName", "NULL");

        NewBgTask testAsyncTask = new NewBgTask(context, searchName);
        testAsyncTask.execute();
    }
}

