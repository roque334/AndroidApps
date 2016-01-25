package com.example.roquecontreras.dataapi;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.example.roquecontreras.common.MobileWearConstants;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.Channel;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

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
        GoogleApiClient googleApiClient;
        Log.d(LOG_TAG, "onChannelOpened: " + channel.getNodeId());
        if (MobileWearConstants.SEND_BY_CHANNEL_PATH.equals(channel.getPath())) {
            googleApiClient = InitializeGoogleApiClient();
            WaitGoogleApiClientConnection(googleApiClient);
            if (ReceiveFile(channel, googleApiClient)) {
                Log.d(LOG_TAG,"onChannelOpened_receiveFile: true");
                googleApiClient.disconnect();
            }
        }
    }

    /**
     * Returns the arrangement of the sensors over the patient body.
     * @return the Map with the arrangement key/values.
     */
    private Map readArrangementFile() {
        InputStream inputStream;
        InputStreamReader inputStreamReader;
        BufferedReader bufferedReader;
        String[] keyValue;
        String receiveString;
        Map result = new HashMap();
        try {
            inputStream = openFileInput(MobileWearConstants.ARRANGEMENT_FILENAME);
            if ( inputStream != null ) {
                inputStreamReader = new InputStreamReader(inputStream);
                bufferedReader = new BufferedReader(inputStreamReader);
                while ( (receiveString = bufferedReader.readLine()) != null ) {
                    keyValue = receiveString.split(";");
                    result.put(keyValue[0], keyValue[1]);
                }
                inputStream.close();
            }
        }
        catch (FileNotFoundException e) {
            Log.e(LOG_TAG, "File not found: " + e.toString());
        } catch (IOException e) {
            Log.e(LOG_TAG, "Can not read file: " + e.toString());
        }
        return result;
    }


    @Override
    public void onInputClosed(Channel channel, int i, int i1) {
        Log.d(LOG_TAG, "onInputClosed: " + channel.getNodeId());
    }

    /**
     * Builds and connects the GoogleApiClient with the Wearable API and a capabilityListener.
     * @return the GoogleApiClient connected.
     */
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
        Wearable.CapabilityApi.addCapabilityListener(googleApiClient, mCapabilityListener, MobileWearConstants.DATA_ANALYSIS_CAPABILITY);
        googleApiClient.connect();
        return googleApiClient;
    }

    /**
     *
     * @param channel
     * @param googleApiClient
     * @return
     */
    private boolean ReceiveFile(Channel channel, GoogleApiClient googleApiClient) {
        String filename;
        boolean result = false;
        fileTimeStamp = System.currentTimeMillis();
        filename = MobileWearConstants.MEASUREMENT_FILENAME_START + channel.getNodeId() + "_" + fileTimeStamp;
        CreateMeasurementFile(filename);
        if (channel.receiveFile(googleApiClient, Uri.fromFile(this.getFileStreamPath(filename)), false).await().isSuccess()) {
            CloseMeasurementFile();
            result = true;
        }
        return result;
    }

    /**
     *  Waits until the GoogleApiClient gets connected.
     * @param googleApiClient the GoogleApiClient requesting the connection.
     */
    private void WaitGoogleApiClientConnection(GoogleApiClient googleApiClient) {
        while (googleApiClient.isConnecting()) {

        }
    }

    /**
     * Creates a file to registry the data received.
     * @param filename the name of the file to create.
     */
    private void CreateMeasurementFile(String filename) {
        try {
            measurementsFile = this.openFileOutput(filename, Context.MODE_PRIVATE);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Closes the current measurement file.
     */
    private void CloseMeasurementFile() {
        try {
            measurementsFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
