package com.example.roquecontreras.dataapi;

import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.example.roquecontreras.common.MobileWearConstants;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.Channel;
import com.google.android.gms.wearable.ChannelApi;
import com.google.android.gms.wearable.Wearable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

/**
 * Created by roquecontreras on 26/10/15.
 */
public class SendByChannelThread extends Thread implements ChannelApi.ChannelListener, ResultCallback<Channel.GetOutputStreamResult>{

    private final String LOG_TAG="SendByChannelThread";

    //Thread variables
    private Context mContext;
    private volatile boolean mIsSucces = false;
    private File mfile;
    private String mMeasurementsFilename;

    private GoogleApiClient mGoogleApiClient;
    private String mNodeID;
    private Channel mChannel;

    public SendByChannelThread(Context context, String measurements, GoogleApiClient googleApiClient) {
        mContext = context;
        mMeasurementsFilename = measurements;
        mGoogleApiClient = googleApiClient;
    }

    public boolean isSucces() {
        return mIsSucces;
    }

    @Override
    public void run() {
        mIsSucces = false;

        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mNodeID = GetHandheldNodeID();
            if (!mNodeID.isEmpty()) {
                mIsSucces = SendFile();
            }
        }
        if (mIsSucces) {
            Thread.currentThread().interrupt();
            return;
        }
    }

    /**
     * Returns the id of handheld node.
     * @return the id of the handheld node.
     */
    private String GetHandheldNodeID(){
        CapabilityApi.GetCapabilityResult capabilityResult;
        String result = "";
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            capabilityResult = Wearable.CapabilityApi
                    .getCapability(mGoogleApiClient, MobileWearConstants.DATA_ANALYSIS_CAPABILITY
                            , CapabilityApi.FILTER_REACHABLE).await();
            if (capabilityResult.getCapability().getNodes().size() > 0) {
                result = capabilityResult.getCapability().getNodes().iterator().next().getId();
            }
        }
        return result;
    }

    /**
     * Sends the measurement file to the handheld node.
     * @return True if the file was send correctly; otherwise False.
     */
    private boolean SendFile(){
        File sdcard, dir;
        ChannelApi.OpenChannelResult channelResult;
        Status statusPendingResult;
        final boolean[] result = {false};
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            channelResult = Wearable.ChannelApi.openChannel(mGoogleApiClient, mNodeID, MobileWearConstants.SEND_BY_CHANNEL_PATH).await();
            if (channelResult.getStatus().isSuccess()) {
                mChannel = channelResult.getChannel();
                sdcard = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
                dir = new File(sdcard.getAbsolutePath() + "/Moreno/");
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                mfile = new File(dir, mMeasurementsFilename);
                mChannel.getOutputStream(mGoogleApiClient).setResultCallback(this);
            } else {
                Toast.makeText(mContext, mContext.getString(R.string.send_error), Toast.LENGTH_SHORT).show();
            }
        }
        return result[0];
    }

    @Override
    public void onChannelOpened(Channel channel) {
    }

    @Override
    public void onChannelClosed(Channel channel, int closeReason, int appSpecificErrorCode) {
        switch (closeReason) {
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
        }

    }

    @Override
    public void onInputClosed(Channel channel, int closeReason, int appSpecificErrorCode) {
        switch (closeReason) {
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
        }
    }

    @Override
    public void onOutputClosed(Channel channel, int closeReason, int appSpecificErrorCode) {
        switch (closeReason) {
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
        }
    }

    @Override
    public void onResult(Channel.GetOutputStreamResult getOutputStreamResult) {
        FileInputStream fis;
        InputStreamReader inputStreamReader;
        BufferedReader bufferedReader;
        String receiveString;
        int total;
        boolean result = false;
        OutputStream os;
        result = getOutputStreamResult.getStatus().isSuccess();
        total = 0;
        if (result) {
            os = getOutputStreamResult.getOutputStream();
            try {

                fis = new FileInputStream(mfile);
                inputStreamReader = new InputStreamReader(fis);
                bufferedReader = new BufferedReader(inputStreamReader);
                while ((receiveString = bufferedReader.readLine()) != null ) {
                    total += receiveString.length();
                    os.write(receiveString.getBytes());
                    os.flush();
                }
                os.close();
            } catch (IOException e) {
            }
        } else {
        }
    }
}
