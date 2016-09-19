package com.example.roquecontreras.dataapi;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.widget.Toast;

import com.example.roquecontreras.common.MobileWearConstants;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.Channel;
import com.google.android.gms.wearable.ChannelApi;
import com.google.android.gms.wearable.Wearable;

import java.io.File;

/**
 * Created by roquecontreras on 26/10/15.
 */
public class SendByChannelThread extends Thread implements ChannelApi.ChannelListener {

    private final String LOG_TAG = "SendByChannelThread";

    //Thread variables
    private Context mContext;
    private volatile boolean mIsSuccess = false;
    private volatile boolean mRunning = false;
    private File mfile;
    private String mMeasurementsFilename;

    private GoogleApiClient mGoogleApiClient;
    private String mNodeID;
    private Channel mChannel;

    public SendByChannelThread(Context context, String measurements, GoogleApiClient googleApiClient) {
        mContext = context;
        mMeasurementsFilename = measurements;
        mGoogleApiClient = googleApiClient;
        mGoogleApiClient.connect();
    }

    public boolean isSuccess() {
        return mIsSuccess;
    }

    /**
     * Return weather the thread is running or not.
     *
     * @return True if the thread is currently running; otherwise False.
     */
    public boolean isRunning() {
        return mRunning;
    }

    @Override
    public void run() {
        mIsSuccess = false;
        mRunning = true;

        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mNodeID = GetHandheldNodeID();
            if (!mNodeID.isEmpty()) {
                mIsSuccess = SendFile();
            }
        }
        if (mIsSuccess) {
            Thread.currentThread().interrupt();
            return;
        } else {
            Thread.currentThread().interrupt();
            return;
        }
    }

    /**
     * Returns the id of handheld node.
     *
     * @return the id of the handheld node.
     */
    private String GetHandheldNodeID() {
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
     *
     * @return True if the file was send correctly; otherwise False.
     */
    private boolean SendFile() {
        File sdcard, dir;
        ChannelApi.OpenChannelResult channelResult;
        Status statusPendingResult;
        boolean isChannelOpenned;
        final boolean[] result = {false};
        isChannelOpenned = false;
        channelResult = null;
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            while (!(isChannelOpenned)) {
                channelResult = Wearable.ChannelApi.openChannel(mGoogleApiClient, mNodeID, MobileWearConstants.SEND_BY_CHANNEL_PATH).await();
                isChannelOpenned = channelResult.getStatus().isSuccess();
            }
            if ((channelResult != null) && (isChannelOpenned)) {
                mChannel = channelResult.getChannel();
                sdcard = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
                dir = new File(sdcard.getAbsolutePath() + "/Moreno/");
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                mfile = new File(dir, mMeasurementsFilename);
                statusPendingResult = mChannel.sendFile(mGoogleApiClient, Uri.fromFile(mfile)).await();
                mIsSuccess = statusPendingResult.isSuccess();
                mRunning = false;
            } else {
                Toast.makeText(mContext, mContext.getString(R.string.send_error), Toast.LENGTH_SHORT).show();
            }
        }
        return result[0];
    }

    @Override
    public void onChannelOpened(Channel channel) {
        // Log.d(LOG_TAG, "onChannelOpened");
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
        /*Log.d(LOG_TAG, "onOutputClosed");
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
        }*/
    }
}
