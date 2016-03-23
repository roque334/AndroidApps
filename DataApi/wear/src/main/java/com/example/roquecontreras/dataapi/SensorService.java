package com.example.roquecontreras.dataapi;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.example.roquecontreras.common.MobileWearConstants;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.Wearable;

import java.util.concurrent.TimeUnit;

public class SensorService extends Service {

    private static final String LOG_TAG = "SensorService";
    private GoogleApiClient mGoogleApiClient;
    private SensingThread mSensingThread;

    private CapabilityApi.CapabilityListener mCapabilityListener = new CapabilityApi.CapabilityListener() {
        @Override
        public void onCapabilityChanged(CapabilityInfo capabilityInfo) {
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        initializeGoogleApiClient();
        mSensingThread = new SensingThread(getApplicationContext());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mGoogleApiClient.connect();
        if (!mSensingThread.isRunning()) {
            mSensingThread.start();
        }
        return START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        String filename;
        SendByChannelThread sendByChannelThread;
        if (mSensingThread != null && mSensingThread.isRunning()) {

            filename = mSensingThread.Terminate();
            try {
                mSensingThread.join();
            } catch (InterruptedException e) {
            }
            sendByChannelThread = new SendByChannelThread(getApplicationContext(), filename, mGoogleApiClient);
            sendByChannelThread.start();
            try {
                sendByChannelThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void initializeGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle connectionHint) {
                    }

                    @Override
                    public void onConnectionSuspended(int cause) {
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult result) {
                    }
                })
                .addApi(Wearable.API)
                .build();
        Wearable.CapabilityApi.addCapabilityListener(mGoogleApiClient, mCapabilityListener, MobileWearConstants.TREMOR_QUANTIFICATION_CAPABILITY);
    }

}
