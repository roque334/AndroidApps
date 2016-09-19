package com.example.roquecontreras.dataapi;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v7.app.NotificationCompat;

import com.example.roquecontreras.common.MobileWearConstants;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.Channel;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
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
    private Map<String, String> mArrange;
    private int mSync = 0;

    private GoogleApiClient mGoogleApiClient;

    private CapabilityApi.CapabilityListener mCapabilityListener = new CapabilityApi.CapabilityListener() {
        @Override
        public void onCapabilityChanged(CapabilityInfo capabilityInfo) {
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onChannelOpened(final Channel channel) {
        long fileTimeStamp;
        String filename;
        File sdcard, dir, file;
        readArrangementFile();
        InitializeGoogleApiClient();
        if (mSync >= 5) {
            mSync = 0;
        }
        if (MobileWearConstants.SEND_BY_CHANNEL_PATH.equals(channel.getPath())) {
            WaitGoogleApiClientConnection();
            fileTimeStamp = System.currentTimeMillis();
            filename = MobileWearConstants.MEASUREMENT_FILENAME_START + mArrange.get(channel.getNodeId()).trim().replaceAll(" ", "_") + "_" + fileTimeStamp;
            sdcard = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
            dir = new File(sdcard.getAbsolutePath() + "/Moreno/");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            file = new File(dir, filename);
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            channel.receiveFile(mGoogleApiClient, Uri.fromFile(file), false);
        }
    }

    /**
     * Builds and connects the GoogleApiClient with the Wearable API and a capabilityListener.
     *
     * @return the GoogleApiClient connected.
     */
    private void InitializeGoogleApiClient() {
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
        Wearable.CapabilityApi.addCapabilityListener(mGoogleApiClient, mCapabilityListener, MobileWearConstants.DATA_ANALYSIS_CAPABILITY);
        mGoogleApiClient.connect();
    }

    /**
     * Waits until the GoogleApiClient gets connected.
     */
    private void WaitGoogleApiClientConnection() {
        while (mGoogleApiClient.isConnecting()) {
        }
    }

    @Override
    public void onChannelClosed(Channel channel, int closeReason, int appSpecificErrorCode) {
        /*switch (closeReason) {
            case CLOSE_REASON_NORMAL:
                Log.d(LOG_TAG, "onChannelClosed: Channel closed. Reason: normal close (" + closeReason + ") Error code: " + appSpecificErrorCode + "\n" +
                        "From Node ID" + channel.getNodeId() + "\n" +
                        "Path: " + channel.getPath());
                break;
            case CLOSE_REASON_DISCONNECTED:
                Log.d(LOG_TAG, "onChannelClosed: Channel closed. Reason: disconnected (" + closeReason + ") Error code: " + appSpecificErrorCode + "\n" +
                        "From Node ID" + channel.getNodeId() + "\n" +
                        "Path: " + channel.getPath());
                break;
            case CLOSE_REASON_REMOTE_CLOSE:
                Log.d(LOG_TAG, "onChannelClosed: Channel closed. Reason: closed by remote (" + closeReason + ") Error code: " + appSpecificErrorCode + "\n" +
                        "From Node ID" + channel.getNodeId() + "\n" +
                        "Path: " + channel.getPath());
                break;
            case CLOSE_REASON_LOCAL_CLOSE:
                Log.d(LOG_TAG, "onChannelClosed: Channel closed. Reason: closed locally (" + closeReason + ") Error code: " + appSpecificErrorCode + "\n" +
                        "From Node ID" + channel.getNodeId() + "\n" +
                        "Path: " + channel.getPath());
                break;
        }*/
    }

    @Override
    public void onInputClosed(Channel channel, int closeReason, int appSpecificErrorCode) {
        mSync++;
        if (closeReason == CLOSE_REASON_NORMAL) {
            runOnUiThread(new Runnable() {
                public void run() {
                    SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(WearableService.this);
                    if (sp.getBoolean(MobileWearConstants.KEY_HANDHELD_WEAR_SYNC_NOTIFICATION, true)) {
                        NotificationCompat.Builder mBuilder =
                                (NotificationCompat.Builder) new NotificationCompat.Builder(WearableService.this)
                                        .setSmallIcon(R.drawable.sensing)
                                        .setContentTitle(WearableService.this.getString(R.string.handheld_wear_sync))
                                        .setContentText(WearableService.this.getString(R.string.data_sent))
                                        .setPriority(NotificationCompat.PRIORITY_LOW);
                        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

                        mNotificationManager.notify(mSync, mBuilder.build());
                    }
                }
            });
        } else {
            runOnUiThread(new Runnable() {
                public void run() {
                    SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(WearableService.this);
                    if (sp.getBoolean(MobileWearConstants.KEY_HANDHELD_WEAR_SYNC_NOTIFICATION, true)) {
                        NotificationCompat.Builder mBuilder =
                                (NotificationCompat.Builder) new NotificationCompat.Builder(WearableService.this)
                                        .setSmallIcon(R.drawable.sensing)
                                        .setContentTitle(WearableService.this.getString(R.string.handheld_wear_sync))
                                        .setContentText(WearableService.this.getString(R.string.data_not_sent))
                                        .setPriority(NotificationCompat.PRIORITY_LOW);
                        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                        mNotificationManager.notify(mSync, mBuilder.build());
                    }
                }
            });
        }
        /*switch (closeReason) {
            case CLOSE_REASON_NORMAL:
                Log.d(LOG_TAG, "onInputClosed: Channel input side closed. Reason: normal close (" + closeReason + ") Error code: " + appSpecificErrorCode + "\n" +
                        "From Node ID" + channel.getNodeId() + "\n" +
                        "Path: " + channel.getPath());
                break;
            case CLOSE_REASON_DISCONNECTED:
                Log.d(LOG_TAG, "onInputClosed: Channel input side closed. Reason: disconnected (" + closeReason + ") Error code: " + appSpecificErrorCode + "\n" +
                        "From Node ID" + channel.getNodeId() + "\n" +
                        "Path: " + channel.getPath());
                break;
            case CLOSE_REASON_REMOTE_CLOSE:
                Log.d(LOG_TAG, "onInputClosed: Channel input side closed. Reason: closed by remote (" + closeReason + ") Error code: " + appSpecificErrorCode + "\n" +
                        "From Node ID" + channel.getNodeId() + "\n" +
                        "Path: " + channel.getPath());
                break;
            case CLOSE_REASON_LOCAL_CLOSE:
                Log.d(LOG_TAG, "onInputClosed: Channel input side closed. Reason: closed locally (" + closeReason + ") Error code: " + appSpecificErrorCode + "\n" +
                        "From Node ID" + channel.getNodeId() + "\n" +
                        "Path: " + channel.getPath());
        }*/
    }

    @Override
    public void onOutputClosed(Channel channel, int closeReason, int appSpecificErrorCode) {
        /*switch (closeReason) {
            case CLOSE_REASON_NORMAL:
                Log.d(LOG_TAG, "onOutputClosed: Channel output side closed. Reason: normal close (" + closeReason + ") Error code: " + appSpecificErrorCode + "\n" +
                        "From Node ID" + channel.getNodeId() + "\n" +
                        "Path: " + channel.getPath());
                break;
            case CLOSE_REASON_DISCONNECTED:
                Log.d(LOG_TAG, "onOutputClosed: Channel output side closed. Reason: disconnected (" + closeReason + ") Error code: " + appSpecificErrorCode + "\n" +
                        "From Node ID" + channel.getNodeId() + "\n" +
                        "Path: " + channel.getPath());
                break;
            case CLOSE_REASON_REMOTE_CLOSE:
                Log.d(LOG_TAG, "onOutputClosed: Channel output side closed. Reason: closed by remote (" + closeReason + ") Error code: " + appSpecificErrorCode + "\n" +
                        "From Node ID" + channel.getNodeId() + "\n" +
                        "Path: " + channel.getPath());
                break;
            case CLOSE_REASON_LOCAL_CLOSE:
                Log.d(LOG_TAG, "onOutputClosed: Channel output side closed. Reason: closed locally (" + closeReason + ") Error code: " + appSpecificErrorCode + "\n" +
                        "From Node ID" + channel.getNodeId() + "\n" +
                        "Path: " + channel.getPath());
                break;
        }*/
    }

    /**
     * Returns the sensors weared over the patient's body.
     *
     * @return the Map with the arrangement key/values.
     */
    private void readArrangementFile() {
        InputStream inputStream;
        InputStreamReader inputStreamReader;
        BufferedReader bufferedReader;
        String[] keyValue;
        String receiveString;
        mArrange = new HashMap<String, String>();
        try {
            inputStream = openFileInput(MobileWearConstants.ARRANGEMENT_FILENAME);
            if (inputStream != null) {
                inputStreamReader = new InputStreamReader(inputStream);
                bufferedReader = new BufferedReader(inputStreamReader);
                while ((receiveString = bufferedReader.readLine()) != null) {
                    keyValue = receiveString.split(";");
                    mArrange.put(keyValue[0], keyValue[1]);
                }
                inputStream.close();
            }
        } catch (FileNotFoundException e) {
            mArrange = null;
        } catch (IOException e) {
            mArrange = null;
        }
    }

    /**
     * Runs an action on the UI thread.
     *
     * @param runnable the action to be run.
     */
    private void runOnUiThread(Runnable runnable) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(runnable);
    }
}