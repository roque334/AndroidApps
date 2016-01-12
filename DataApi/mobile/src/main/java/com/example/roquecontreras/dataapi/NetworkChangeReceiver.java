package com.example.roquecontreras.dataapi;

import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.view.View;

import java.util.List;

public class NetworkChangeReceiver extends BroadcastReceiver {

    ProgressDialog mProgressDialog;

    public NetworkChangeReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if(mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(context);
            mProgressDialog.setTitle("Please wait");
            mProgressDialog.setMessage("Connecting...");
        }
        if (isInternetConnectionOn(context)) {
            mProgressDialog.setCancelable(true);
            mProgressDialog.dismiss();
        } else {
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.setCancelable(false);
            mProgressDialog.show();
            resetAppAfterSomeTime(context);
        }
    }

    public void resetAppAfterSomeTime(final Context context) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(10000);
                    if (!isInternetConnectionOn(context)) {
                        mProgressDialog.dismiss();
                        Intent intentSignInActivity = new Intent(context, SignInActivity.class);
                        intentSignInActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(intentSignInActivity);
                    }
                } catch (Exception e) {

                }
            }
        }).start();
    }

    public boolean isInternetConnectionOn(Context context){
        boolean result = true;
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        if (activeNetworkInfo != null && activeNetworkInfo.isConnected()) { // connected to the internet
            if (activeNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                // connected to wifi
            } else if (activeNetworkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                // connected to the mobile provider's data plan
            }
        } else { // not connected to the internet
            result = false;
        }
        return result;
    }
}
