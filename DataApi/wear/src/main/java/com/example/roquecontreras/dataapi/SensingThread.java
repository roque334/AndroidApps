package com.example.roquecontreras.dataapi;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.example.roquecontreras.common.Constants;
import com.example.roquecontreras.common.Measurement;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Channel;
import com.google.android.gms.wearable.Wearable;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by roquecontreras on 26/10/15.
 */
public class SensingThread extends Thread implements SensorEventListener {

    private static final String LOG_TAG = "SensingThread";

    //Thread variables
    private volatile boolean running = false;
    private Context mContext;

    //AccelerometerSensor variables
    private SensorManager mSensorManager;
    private Sensor mSensor;
    private final float alpha = 0.8f;
    private float[] gravity = new float[3];
    private Long lastSensorUpdate;

    //File variables
    private FileOutputStream measurementsFile;
    private final String FILENAME = "measurements_";
    private Long fileTimeStamp;

    //SendByChannetThread variables
    SendByChannelThread mSendByChannelThread;

    public SensingThread(Context context){
        mContext = context;
        mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        CreateMeasurementFile(System.currentTimeMillis());
    }

    public boolean isRunning() {
        return running;
    }

    public void Terminate() {
        mSensorManager.unregisterListener(this);
        CloseMeasurementFile();
        mSendByChannelThread = new SendByChannelThread(mContext, FILENAME + fileTimeStamp);
        mSendByChannelThread.start();
        try {
            mSendByChannelThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        running = false;
    }

    @Override
    public void run() {
        running = true;
        lastSensorUpdate = System.currentTimeMillis();
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_UI);;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Measurement measurement;
        long actualTime = System.currentTimeMillis();

        if (actualTime - lastSensorUpdate > 250) {
            lastSensorUpdate = actualTime;

            SendFileToHandHeld(actualTime);

            // Isolate the force of gravity with the low-pass filter.
            gravity[0] = lowPass(event.values[0], gravity[0]);
            gravity[1] = lowPass(event.values[1], gravity[1]);
            gravity[2] = lowPass(event.values[2], gravity[2]);

            // Remove the gravity contribution with the high-pass filter.
            measurement = new Measurement(highPass(event.values[0], gravity[0]), highPass(event.values[1], gravity[1]), highPass(event.values[2], gravity[2]), actualTime);
            WriteToMeasurementFile(measurement);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private void CreateMeasurementFile(long actualTime) {
        try {
            fileTimeStamp = actualTime;
            measurementsFile = mContext.openFileOutput(FILENAME + fileTimeStamp, Context.MODE_PRIVATE);
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

    private void WriteToMeasurementFile(Measurement measurement) {
        try {
            measurementsFile.write(measurement.toString().getBytes());
            Log.d(LOG_TAG, measurement.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void SendFileToHandHeld(long actualTime) {
        if (actualTime - fileTimeStamp > 10000) {
            CloseMeasurementFile();
            mSendByChannelThread = new SendByChannelThread(mContext, FILENAME + fileTimeStamp);
            mSendByChannelThread.start();
            CreateMeasurementFile(actualTime);
        }
    }

    private float lowPass(float current, float gravity) { return gravity * alpha + current * (1 - alpha); }

    private float highPass(float current, float gravity) {
        return current - gravity;
    }
}
