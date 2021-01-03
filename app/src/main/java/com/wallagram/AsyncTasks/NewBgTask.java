package com.wallagram.AsyncTasks;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;

import com.squareup.picasso.Picasso;
import com.wallagram.AdapterCallback;
import com.wallagram.MainActivity;
import com.wallagram.Model.Account;
import com.wallagram.Receivers.AlarmReceiver;
import com.wallagram.Sqlite.SQLiteDatabaseAdapter;
import com.wallagram.Utils.Functions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class NewBgTask extends AsyncTask<String, String, String> {
    private Context mContext;
    private static String mSearchName;

    private static SQLiteDatabaseAdapter db;
    private static SharedPreferences sharedPreferences;
    private static SharedPreferences.Editor editor;

    private String mPostUrl;
    private String mProfileUrl;

    private Account account;

    public NewBgTask(Context context) {
        this.mContext = context;

        db = new SQLiteDatabaseAdapter(context);
        sharedPreferences = context.getSharedPreferences("SET_ACCOUNT", 0);
        editor = context.getSharedPreferences("SET_ACCOUNT", 0).edit();
        editor.apply();

        mSearchName = sharedPreferences.getString("searchName", "NULL");
    }

    @Override
    protected String doInBackground(String... params) {
        HttpURLConnection connection = null;
        BufferedReader reader = null;
        boolean error = false;

        try {
            String urlString = "https://www.instagram.com/" + mSearchName + "/?__a=1";

            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.connect();

            InputStream stream = connection.getInputStream();

            reader = new BufferedReader(new InputStreamReader(stream));

            JSONObject jsonObject = new JSONObject(reader.readLine());
            JSONObject graphqlObject = jsonObject.getJSONObject("graphql");
            JSONObject userObject = graphqlObject.getJSONObject("user");

            mProfileUrl = userObject.getString("profile_pic_url_hd");
            account = new Account(mSearchName, mProfileUrl);

            JSONObject timelineMediaObject = userObject.getJSONObject("edge_owner_to_timeline_media");
            JSONArray edgesArray = timelineMediaObject.getJSONArray("edges");
            JSONObject edgeObject = edgesArray.getJSONObject(0);
            JSONObject nodeObject = edgeObject.getJSONObject("node");

            mPostUrl = nodeObject.get("display_url").toString();
        }
        catch (JSONException | IOException e) {
            error = true;
        }
        finally {
            if (connection != null) {
                connection.disconnect();
            }
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if(error) {
            Log.v("NewBgTask", "Account Search Failed");
            return "Error";
        }

        return "Success";
    }

    protected void onPostExecute(String result) {
        if (result.equalsIgnoreCase("Error")) {
            MainActivity.mSetAccountName.setText("Account is private or doesn't exist!");
            editor.putString("setAccountName", "Not Set");
            editor.commit();

            Intent intent = new Intent(mContext, AlarmReceiver.class);
            PendingIntent pendingIntent = PendingIntent
                    .getBroadcast(mContext, 0, intent, PendingIntent.FLAG_NO_CREATE);

            AlarmManager alarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager!= null) {
                alarmManager.cancel(pendingIntent);
            }
        }
        else {
            MainActivity.mSetAccountName.setText(mSearchName);

            if(!db.checkIfAccountExists(account)) {
                Log.d("DB", "Adding new account name into db (" + mSearchName + ")");
                db.addAccount(account);

                MainActivity.mDBAccountList.add(0, account);
                MainActivity.mAdapter.notifyItemInserted(0);
                MainActivity.mAdapter.notifyDataSetChanged();
            }
            else{
                Log.d("DB", "Account name already in db (" + mSearchName + ")");

                for(Account a : MainActivity.mDBAccountList){
                    if(a.getAccountName().equalsIgnoreCase(account.getAccountName())){
                        MainActivity.mDBAccountList.remove(a);
                        MainActivity.mAdapter.notifyItemRemoved(MainActivity.mDBAccountList.indexOf(a));

                        MainActivity.mDBAccountList.add(0, account);
                        MainActivity.mAdapter.notifyItemInserted(0);

                        MainActivity.mAdapter.notifyDataSetChanged();

                        db.deleteAccount(a.getAccountName());
                        db.addAccount(account);

                        break;
                    }
                }
            }

            //Set displayed profile pic
            Picasso.get()
                    .load(Uri.parse(account.getProfilePicURL()))
                    .into(MainActivity.mSetProfilePic);

            if(!sharedPreferences.getString("setPostURL", "null").equalsIgnoreCase(mPostUrl)){
                //Set background and save to storage
                Functions.setWallpaper(mContext, mPostUrl);
                Functions.savePost(mContext, mPostUrl);
            }

            editor.putString("setProfilePic", mProfileUrl);
            editor.putString("setAccountName", account.getAccountName());
            editor.putString("setPostURL", mPostUrl);
            editor.commit();
        }

        MainActivity.mLoadingView.setVisibility(View.INVISIBLE);
    }
}
