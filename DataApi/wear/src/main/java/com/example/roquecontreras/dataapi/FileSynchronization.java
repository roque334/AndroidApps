package com.example.roquecontreras.dataapi;

import android.content.Context;
import android.os.Bundle;
import android.os.Environment;

import com.example.roquecontreras.common.MobileWearConstants;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.Wearable;

import java.io.File;

/**
 * Created by roque on 20/04/16.
 */
public class FileSynchronization extends Thread {

    private static final String LOG_TAG = "FileSynchronization";
    private static Context mContext;
    private static GoogleApiClient mGoogleApiClient;
    private static File mFile;
    private volatile boolean mRunning = false;
    private volatile boolean mIsSuccess = false;

    private CapabilityApi.CapabilityListener mCapabilityListener = new CapabilityApi.CapabilityListener() {
        @Override
        public void onCapabilityChanged(CapabilityInfo capabilityInfo) {
        }
    };

    public FileSynchronization(Context context) {
        mContext = context;
        mFile = null;
        initializeGoogleApiClient();
        mGoogleApiClient.connect();
    }

    public FileSynchronization(Context context, GoogleApiClient googleApiClient) {
        mContext = context;
        mFile = null;
        mGoogleApiClient = googleApiClient;
    }

    /**
     * Sets the file into the private mFile variable.
     *
     * @param file the file.
     */
    public void setFile(File file) {
        mFile = file;
    }

    /**
     * Sends all the files of the Moreno's directory.
     */
    public void sendAllFiles() {
        File sdcard, dir, files[];
        SendByChannelThread sendByChannelThread;
        sdcard = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        dir = new File(sdcard.getAbsolutePath() + "/Moreno/");
        WaitGoogleApiClientConnection();
        files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    sendByChannelThread = new SendByChannelThread(mContext, file.getName(), mGoogleApiClient);
                    sendByChannelThread.start();
                    try {
                        sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    try {
                        sendByChannelThread.join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * Sends the previously file set in the mFile variable.
     */
    public void sendFile() {
        SendByChannelThread sendByChannelThread;
        sendByChannelThread = new SendByChannelThread(mContext, mFile.getName(), mGoogleApiClient);
        sendByChannelThread.start();
        while (sendByChannelThread.isRunning()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        try {
            sendByChannelThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Return weather the thread is running or not.
     *
     * @return True if the thread is currently running; otherwise False.
     */
    public boolean isRunning() {
        return mRunning;
    }

    public boolean isSucces() {
        return mIsSuccess;
    }

    @Override
    public void run() {
        mIsSuccess = true;

        if (mFile != null) {
            sendFile();
        } else {
            sendAllFiles();
        }

        if (mIsSuccess) {
            mGoogleApiClient.disconnect();
            Thread.currentThread().interrupt();
            mRunning = false;
            return;
        }
    }

    private void initializeGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(mContext)
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

    /**
     * Waits until the GoogleApiClient gets connected.
     */
    private void WaitGoogleApiClientConnection() {
        while (mGoogleApiClient.isConnecting()) {
        }
    }
}