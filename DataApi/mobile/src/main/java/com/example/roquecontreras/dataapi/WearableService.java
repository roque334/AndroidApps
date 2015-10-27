package com.example.roquecontreras.dataapi;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.example.roquecontreras.common.Constants;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.Channel;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static com.google.android.gms.wearable.PutDataRequest.WEAR_URI_SCHEME;

/**
 * Created by Roque Contreras on 27/10/2015.
 */
public class WearableService extends WearableListenerService {

    private static final String LOG_TAG = "WearableListenerService";

    private FileOutputStream measurementsFile;
    Long fileTimeStamp;

    private CapabilityApi.CapabilityListener mCapabilityListener = new CapabilityApi.CapabilityListener() {
        @Override
        public void onCapabilityChanged(CapabilityInfo capabilityInfo) {
            Log.d(LOG_TAG, "onCapabilityChanged: " + capabilityInfo.getName());
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(LOG_TAG, "onStartCommand");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onChannelOpened(final Channel channel) {
        Log.d(LOG_TAG, "onChannelOpened: " + channel.getNodeId());
        if (Constants.SEND_BY_CHANNEL_PATH.equals(channel.getPath())) {
            GoogleApiClient googleApiClient = InitializeGoogleApiClient();
            WaitGoogleApiClientConnection(googleApiClient);
            if (ReceiveFile(channel, googleApiClient)) {
                Log.d(LOG_TAG,"onChannelOpened_receiveFile: true");
                googleApiClient.disconnect();
            }
        }
    }

    @Override
    public void onInputClosed(Channel channel, int i, int i1) {
        Log.d(LOG_TAG, "onInputClosed: " + channel.getNodeId());
    }

    private GoogleApiClient InitializeGoogleApiClient() {
        GoogleApiClient googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle connectionHint) {
                        Log.d(LOG_TAG, "onConnected: " + connectionHint);
                    }
                    @Override
                    public void onConnectionSuspended(int cause) {
                        Log.d(LOG_TAG, "onConnectionSuspended: " + cause);
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult result) {
                        Log.d(LOG_TAG, "onConnectionFailed: " + result);
                    }
                })
                .addApi(Wearable.API)
                .build();
        Wearable.CapabilityApi.addCapabilityListener(googleApiClient, mCapabilityListener, Constants.DATA_ANALYSIS_CAPABILITY);
        googleApiClient.connect();
        return googleApiClient;
    }

    private boolean ReceiveFile(Channel channel, GoogleApiClient googleApiClient) {
        fileTimeStamp = System.currentTimeMillis();
        String filename = "measurements_" + channel.getNodeId() + "_" + fileTimeStamp;
        CreateMeasurementFile(filename);
        return channel.receiveFile(googleApiClient, Uri.fromFile(this.getFileStreamPath(filename)), false).await().isSuccess();
    }

    private void WaitGoogleApiClientConnection(GoogleApiClient googleApiClient) {
        while (googleApiClient.isConnecting()) {

        }
    }

    private void CreateMeasurementFile(String filename) {
        try {
            measurementsFile = this.openFileOutput(filename, Context.MODE_PRIVATE);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void CloseMeasurementFile() {
        try {
            measurementsFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
