package com.example.roquecontreras.dataapi;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.example.roquecontreras.common.MeasureType;
import com.example.roquecontreras.common.Measurement;
import com.example.roquecontreras.common.MobileWearConstants;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.Channel;
import com.google.android.gms.wearable.ChannelApi;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.example.roquecontreras.common.MeasureType.TOTAL_ACCELERATION;

/**
 * Created by Roque Contreras on 27/10/2015.
 */
public class WearableService extends WearableListenerService {

    private static final String LOG_TAG = "WearableListenerService";
    private Map<String,String> mArrange;

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
        Future<Boolean> threadResult;
        Callable<Boolean> callable;
        ExecutorService es;
        InitializeGoogleApiClient();
        readArrangementFile();
        if (MobileWearConstants.SEND_BY_CHANNEL_PATH.equals(channel.getPath())) {
            WaitGoogleApiClientConnection();
            if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
                es = Executors.newSingleThreadExecutor();
                callable = new Callable<Boolean>() {
                    @Override
                    public Boolean call() {
                        return ReceiveFile(channel);
                    }
                };
                threadResult = es.submit(callable);
                es.shutdown();
                try {
                    if (threadResult.get().booleanValue()) {
                        runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(getApplicationContext(), String.format(WearableService.this.getString(R.string.data_sent)
                                        , MobileWearConstants.bodyPartToText(WearableService.this, mArrange.get(channel.getNodeId())).toLowerCase())
                                        , Toast.LENGTH_SHORT)
                                        .show();
                            }
                        });
                    } else {
                        runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(getApplicationContext()
                                        , String.format(WearableService.this.getString(R.string.data_not_sent), MobileWearConstants.bodyPartToText(WearableService.this, mArrange.get(channel.getNodeId())).toLowerCase())
                                        , Toast.LENGTH_SHORT)
                                        .show();
                            }
                        });
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
                channel.close(mGoogleApiClient);
            }
        }
    }

    /**
     * Builds and connects the GoogleApiClient with the Wearable API and a capabilityListener.
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
     *  Waits until the GoogleApiClient gets connected.
     */
    private void WaitGoogleApiClientConnection() {
        while (mGoogleApiClient.isConnecting()) {
        }
    }

    /**
     * Receives the data sent by the wear sensor and stores it in a public file.
     * @param channel the channel opened with a wear sensor.
     * @return True if the data was receive correctly; otherwise false.
     */
    private Boolean ReceiveFile(final Channel channel){
        FileOutputStream fos;
        InputStream is;
        byte[] bytes;
        String aux, data;
        int c, size;
        Boolean result = false;
        bytes = new byte[1024];
        aux = "";
        data = "";
        size= 0;
        fos = CreateMeasurementFile(channel);
        Channel.GetInputStreamResult getInputStreamResult = channel.getInputStream(mGoogleApiClient)
                .await();
        if (getInputStreamResult.getStatus().isSuccess()) {
            is = getInputStreamResult.getInputStream();
            try {
                while( (c = is.read(bytes)) != -1){
                    data = data + new String(bytes,0,c);
                    size += data.length();
                    aux = ParseDataAndSave(fos, data);

                }
                Log.d(LOG_TAG, channel.getNodeId() + ": " + new Integer(size).toString());
                is.close();
                CloseMeasurementFile(fos);
                result = true;
            } catch (IOException e) {
                result = false;
                e.printStackTrace();
            }
        }
        return result;
    }

    /**
     * Parses and writes the data by the wear sensor into a public file.
     * @param fos the stream to write the data.
     * @param data the data to write.
     * @return the extra string if it was incomplete; otherwise ""
     */
    private String ParseDataAndSave(FileOutputStream fos, String data) {
        String result, measure;
        boolean flag;
        String[] measures;
        result = "";
        flag = false;
        if (data.charAt(data.length() - 1) != '#') {
            flag = true;
        }
        measures = data.split("#");
        for (int i = 0; i < measures.length; i++) {
            measure = measures[i];
            if ((i == (measures.length - 1)) && flag) {
                result = measure;
            } else {
                WriteToStringFile(fos,measure + System.lineSeparator());
            }
        }
        return result;
    }

    /*private void ParseVaulesAndSave (FileOutputStream fos, String measure) {
        Measurement measurement;
        String[] values;
        values = measure.split(";");
        switch (MeasureType.values()[(Integer.valueOf(values[0]).intValue())]) {
            case TOTAL_ACCELERATION:
                Log.d(LOG_TAG, "ParseAndSave: TOTAL_ACCELERATION");
                measurement = new Measurement(TOTAL_ACCELERATION
                        , (Integer.valueOf(values[1]).intValue())
                        , (Integer.valueOf(values[2]).intValue())
                        , (Integer.valueOf(values[3]).intValue())
                        , (Long.valueOf(values[4]).longValue()));
                Log.d(LOG_TAG, measurement.toString());

                break;
            case BODY_ACCELERATION:
                Log.d(LOG_TAG, "ParseAndSave: BODY_ACCELERATION");
                measurement = new Measurement(MeasureType.BODY_ACCELERATION
                        , (Integer.valueOf(values[1]).intValue())
                        , (Integer.valueOf(values[2]).intValue())
                        , (Integer.valueOf(values[3]).intValue())
                        , (Long.valueOf(values[4]).longValue()));
                Log.d(LOG_TAG, measurement.toString());
                break;
            case ANGULAR_SPEED:
            default:
                Log.d(LOG_TAG, "ParseAndSave: ANGULAR_SPEED");
                measurement = new Measurement(MeasureType.ANGULAR_SPEED
                        , (Integer.valueOf(values[1]).intValue())
                        , (Integer.valueOf(values[2]).intValue())
                        , (Integer.valueOf(values[3]).intValue())
                        , (Long.valueOf(values[4]).longValue()));
                Log.d(LOG_TAG, measurement.toString());
                break;
        }
        if (measurement != null) {
            WriteToMeasurementFile(fos, measurement);
        } else {
            Log.d(LOG_TAG, "ParseVaulesAndSave: measurement == null");
        }
    }*/

    /**
     * Creates the stream to write.
     * @param channel the channel opened with a wear sensor.
     * @return the stream to write.
     */
    private FileOutputStream CreateMeasurementFile(final Channel channel) {
        long fileTimeStamp;
        String filename;
        File sdcard, dir, file;
        FileOutputStream result = null;
        fileTimeStamp = System.currentTimeMillis();
        filename = MobileWearConstants.MEASUREMENT_FILENAME_START + channel.getNodeId() + "_" + fileTimeStamp;
        sdcard = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        dir = new File(sdcard.getAbsolutePath()+ "/Moreno/");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        file = new File(dir, filename);
        if (file.exists()) {
            file.delete();
        }
        try {
            if (file.createNewFile()) {
                result = new FileOutputStream(file);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Closes the stream used to write.
     * @param measurementFile the stream used to write.
     */
    private void CloseMeasurementFile(FileOutputStream measurementFile) {
        try {
            measurementFile.close();
        } catch (IOException e) {
        }
    }

    /**
     * Writes a measurement to the measurement stream.
     * @param measurementFile the measurement stream.
     * @param measurement the accelerometer measurement.
     */
    private void WriteToStringFile(FileOutputStream measurementFile, String measurement) {
        try {
            measurementFile.write(measurement.getBytes());
        } catch (IOException e) {
        }
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

    /**
     * Returns the sensors weared over the patient's body.
     * @return the Map with the arrangement key/values.
     */
    private void readArrangementFile() {
        InputStream inputStream;
        InputStreamReader inputStreamReader;
        BufferedReader bufferedReader;
        String[] keyValue;
        String receiveString;
        mArrange = new HashMap<String,String>();
        try {
            inputStream = openFileInput(MobileWearConstants.ARRANGEMENT_FILENAME);
            if ( inputStream != null ) {
                inputStreamReader = new InputStreamReader(inputStream);
                bufferedReader = new BufferedReader(inputStreamReader);
                while ((receiveString = bufferedReader.readLine()) != null ) {
                    keyValue = receiveString.split(";");
                    mArrange.put(keyValue[0], keyValue[1]);
                }
                inputStream.close();
            }
        }
        catch (FileNotFoundException e) {
            mArrange = null;
        } catch (IOException e) {
            mArrange = null;
        }
    }

    /**
     * Runs an action on the UI thread.
     * @param runnable the action to be run.
     */
    private void runOnUiThread(Runnable runnable) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(runnable);
    }
}
