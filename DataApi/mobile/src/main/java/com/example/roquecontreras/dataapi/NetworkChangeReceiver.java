package com.example.roquecontreras.dataapi;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class NetworkChangeReceiver extends BroadcastReceiver {

    ProgressDialog mProgressDialog;

    public NetworkChangeReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        NetworkAndInternetConnection.checkNetworkAndInternet(context);
    }
}