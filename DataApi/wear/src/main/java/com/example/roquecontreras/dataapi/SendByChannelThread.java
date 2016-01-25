package com.example.roquecontreras.dataapi;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.example.roquecontreras.common.MobileWearConstants;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.Channel;
import com.google.android.gms.wearable.ChannelApi;
import com.google.android.gms.wearable.Wearable;

/**
 * Created by roquecontreras on 26/10/15.
 */
public class SendByChannelThread extends Thread implements ChannelApi.ChannelListener{

    private final String LOG_TAG="SendByChannelThread";
    private Context mContext;
    private String mMeasurementsFilename;

    private GoogleApiClient mGoogleApiClient;
    private String mNodeID;
    private Channel mChannel;
    private CapabilityApi.CapabilityListener mCapabilityListener = new CapabilityApi.CapabilityListener() {
        @Override
        public void onCapabilityChanged(CapabilityInfo capabilityInfo) {
            Log.d(LOG_TAG, "onCapabilityChanged: " + capabilityInfo.getName());
        }
    };

    public SendByChannelThread(Context context, String measurements) {
        mContext = context;
        mMeasurementsFilename = measurements;
        InitializeGoogleApiClient();
    }

    @Override
    public void run() {
        boolean isSuccess = false;
        WaitGoogleApiClientConnection();

        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            Log.d(LOG_TAG, "run: isConnected");
            mNodeID = GetHandheldNodeID();
            Log.d(LOG_TAG, mNodeID);
            if (!mNodeID.isEmpty()) {
                isSuccess = SendFile();
                Log.d(LOG_TAG, "SendFileSucced: " + isSuccess);
            }
        }

        if (isSuccess) {
            Wearable.CapabilityApi.removeCapabilityListener(mGoogleApiClient, mCapabilityListener, MobileWearConstants.TREMOR_QUANTIFICATION_CAPABILITY);
            mGoogleApiClient.disconnect();
            Thread.currentThread().interrupt();
            return;
        }
    }

    /**
     * Builds the GoogleApiClient with the Wearable API and a capabilityListener.
     */
    private void InitializeGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(mContext)
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
        Wearable.CapabilityApi.addCapabilityListener(mGoogleApiClient, mCapabilityListener, MobileWearConstants.TREMOR_QUANTIFICATION_CAPABILITY);
        mGoogleApiClient.connect();
    }

    /**
     * Waits until the GoogleApiClient gets connected.
     */
    private void WaitGoogleApiClientConnection() {
        while (mGoogleApiClient.isConnecting()) {

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
            Log.d(LOG_TAG, "GetHandheldNodeID: isConnected");
            capabilityResult = Wearable.CapabilityApi
                    .getCapability(mGoogleApiClient, MobileWearConstants.DATA_ANALYSIS_CAPABILITY
                            , CapabilityApi.FILTER_REACHABLE).await();
            Log.d(LOG_TAG, "GetHandheldNodeID: " + capabilityResult.getStatus().isSuccess());
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
        ChannelApi.OpenChannelResult channelResult;
        boolean result = false;
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            Log.d(LOG_TAG, "SendFile: isConnected");
            channelResult = Wearable.ChannelApi.openChannel(mGoogleApiClient, mNodeID, MobileWearConstants.SEND_BY_CHANNEL_PATH).await();
            Log.d(LOG_TAG, "SendFile_OpenChannelResult: " +channelResult.getStatus().isSuccess());
            mChannel = channelResult.getChannel();
            Log.d(LOG_TAG, "channel: " + mChannel.getNodeId());
            result = mChannel.sendFile(mGoogleApiClient, Uri.fromFile(mContext.getFileStreamPath(mMeasurementsFilename))).await().isSuccess();
            Log.d(LOG_TAG, "SendFile: " + result);
        }
        return result;
    }

    @Override
    public void onChannelOpened(Channel channel) {
        Log.d(LOG_TAG, "onChannelOpened: " + channel.getNodeId());
    }

    @Override
    public void onChannelClosed(Channel channel, int i, int i1) {
        Log.d(LOG_TAG, "onChannelClosed: " + channel.getNodeId());
    }

    @Override
    public void onInputClosed(Channel channel, int i, int i1) {
        Log.d(LOG_TAG, "onInputClosed: " + channel.getNodeId());
    }

    @Override
    public void onOutputClosed(Channel channel, int i, int i1) {
        Log.d(LOG_TAG, "onOutputClosed: " + channel.getNodeId());
        mChannel.close(mGoogleApiClient).await();
    }
}
