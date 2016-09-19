package com.example.roquecontreras.dataapi;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.support.v4.content.IntentCompat;

import com.example.roquecontreras.common.MobileWearConstants;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by roquecontreras on 24/03/16.
 */
public class NetworkAndInternetConnection {

    private static ProgressDialog mProgressDialog;

    /**
     * Checks whether the device is connected to Internet or not.
     *
     * @param context the application context.
     */
    public static void checkNetworkAndInternet(Context context) {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(context);
            mProgressDialog.setTitle(context.getString(R.string.no_internet_connection_title));
            mProgressDialog.setMessage(context.getString(R.string.no_internet_connection_message));
        }
        if (isMobileWiFiNetworkAvailable(context) && isInternetConnectionAvailable(1000)) {
            mProgressDialog.setCancelable(true);
            mProgressDialog.dismiss();
        } else {
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.setCancelable(false);
            mProgressDialog.show();
            resetAppAfterSomeTime(context);
        }
    }

    /**
     * Resets the application if the device is disconnected of Internet for a period of time.
     *
     * @param context the application context.
     */
    private static void resetAppAfterSomeTime(final Context context) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Intent intentSignInActivity;
                try {
                    Thread.sleep(10000);
                    if ((!isMobileWiFiNetworkAvailable(context)) && (!isInternetConnectionAvailable(1000))) {
                        mProgressDialog.dismiss();
                        intentSignInActivity = new Intent(context, SignInActivity.class);
                        intentSignInActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | IntentCompat.FLAG_ACTIVITY_CLEAR_TASK);
                        context.startActivity(intentSignInActivity);
                    }
                } catch (Exception e) {

                }
            }
        }).start();
    }

    /**
     * Checks whether the device is connected to internet through a Mobile or WiFi network.
     *
     * @param context the application context.
     * @return true if the device is connected to internet through a Mobile or WiFi network.
     * false if the device is not connected to internet through a Mobile or WiFi network.
     */
    private static boolean isMobileWiFiNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager;
        NetworkInfo activeNetworkInfo;
        SharedPreferences sp;
        boolean result = true;
        connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        if (activeNetworkInfo != null && activeNetworkInfo.isConnected()) {
            // connected to the internet
            sp = PreferenceManager.getDefaultSharedPreferences(context);
            if (sp.getBoolean(MobileWearConstants.KEY_HANDHELD_WEAR_SYNC_WIFI, true) && (activeNetworkInfo.getType() != ConnectivityManager.TYPE_WIFI)) {
                //not connected to wifi
                result = false;
            }
        } else {
            // not connected to the internet
            result = false;
        }
        return result;
    }

    /**
     * Checks whether the device is connected to Internet or not through a ping to google.
     *
     * @param timeOut amount of milliseconds to obtain a respond from the ping to google.
     * @return true if the ping was performed successfully;
     * false if the ping was a fail.
     */
    private static boolean isInternetConnectionAvailable(int timeOut) {
        InetAddress inetAddress = null;
        try {
            Future<InetAddress> future = Executors.newSingleThreadExecutor().submit(new Callable<InetAddress>() {
                @Override
                public InetAddress call() {
                    try {
                        return InetAddress.getByName("google.com");
                    } catch (UnknownHostException e) {
                        return null;
                    }
                }
            });
            inetAddress = future.get(timeOut, TimeUnit.MILLISECONDS);
            future.cancel(true);
        } catch (InterruptedException e) {
        } catch (ExecutionException e) {
        } catch (TimeoutException e) {
        }
        return inetAddress != null && !inetAddress.equals("");
    }
}