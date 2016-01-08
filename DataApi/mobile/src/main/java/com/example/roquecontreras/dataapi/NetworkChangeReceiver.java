package com.example.roquecontreras.dataapi;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import java.util.List;

public class NetworkChangeReceiver extends BroadcastReceiver {
    public NetworkChangeReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String currentClassName = "";
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            List<ActivityManager.AppTask> tasks = activityManager.getAppTasks();
            currentClassName = tasks.get(0).getTaskInfo().baseIntent.getComponent().getShortClassName();
            Log.d("NetworkChangeReceiver", "Lollipop: " + currentClassName);
        } else {
            currentClassName = activityManager.getRunningTasks(1).get(0).topActivity.getShortClassName();
            Log.d("NetworkChangeReceiver", "Others: " + currentClassName);
        }

        if (!currentClassName.equals(".SignInActivity")) {

            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            if (activeNetworkInfo != null && activeNetworkInfo.isConnected()) { // connected to the internet
                if (activeNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                    // connected to wifi
                } else if (activeNetworkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                    // connected to the mobile provider's data plan
                }
            } else { // not connected to the internet
                Intent intentSignInActivity = new Intent(context, SignInActivity.class);
                intentSignInActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intentSignInActivity);
            }
        }
    }
}
