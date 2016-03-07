package com.example.roquecontreras.dataapi;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import com.example.roquecontreras.common.DSPFilter;
import com.example.roquecontreras.common.Measurement;
import com.example.roquecontreras.common.MobileWearConstants;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by roquecontreras on 26/10/15.
 */
public class SensingThread extends Thread implements SensorEventListener {

    private static final String LOG_TAG = "SensingThread";

    //Thread variables
    private volatile boolean mRunning = false;
    private Context mContext;

    //AccelerometerSensor variables
    private SensorManager mSensorManager;
    private Sensor mSensorAcceletomer;
    private boolean mIsFirstAccelerometerSensing;
    private final float mAlpha = 0.8f;
    private float[] mGravity = new float[3];

    //GyroscopeSensor variables
    private Sensor mSensorGyroscope;
    private boolean mIsFirstGyroscopeSensing;

    //File variables
    private FileOutputStream mMeasurementsFile;
    private String mFilename;
    private Long mFileTimeStamp;

    private Long mFileSendingTime;

    //SendByChannetThread variables
    SendByChannelThread mSendByChannelThread;

    /**
     * Class constructor specifying the application context.
     */
    public SensingThread(Context context){
        mContext = context;
        mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        mSensorAcceletomer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mIsFirstAccelerometerSensing = mIsFirstGyroscopeSensing = true;
        CreateMeasurementFile(System.currentTimeMillis());
    }

    /**
     * Return weather the thread is running or not.
     * @return True if the thread is currently running; otherwise False.
     */
    public boolean isRunning() {
        return mRunning;
    }

    /**
     * Stops the sensor accelerometer measurement. Closes the current measurement file and
     * send it to the handheld devices. Finally, sets false to the the current thread
     * running status.
     */
    public void Terminate() {
        Log.d(LOG_TAG, "Terminate");
        mSensorManager.unregisterListener(this);
        CloseMeasurementFile();
        mSendByChannelThread = new SendByChannelThread(mContext, mFilename + mFileTimeStamp);
        mSendByChannelThread.start();
        try {
            mSendByChannelThread.join();
            Log.d(LOG_TAG, "Status_SendByChannelThread: " + mSendByChannelThread.isSucces());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mRunning = false;
    }

    /**
     * Starts the sensor accelerometer measurement.
     */
    @Override
    public void run() {
        mRunning = true;
        //SensorManager.SENSOR_DELAY_GAME senses every 0.02 seconds (Frecuency Sampling = 50Hz).
        mSensorManager.registerListener(this, mSensorAcceletomer, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, mSensorGyroscope, SensorManager.SENSOR_DELAY_GAME);
    }

    /**
     * Gets the accelerometer variations, removes the gravity force and write it to the
     * current measurement file.
     * @param event the accelerometer variation.
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        synchronized (this) {
            long actualTime;
            Measurement measurement;
            actualTime = event.timestamp;

            //SendFileToHandHeld(actualTime);

            switch (event.sensor.getType()) {

                case Sensor.TYPE_ACCELEROMETER:

                    if (mIsFirstAccelerometerSensing) {
                        DSPFilter.initFilter(event.values[0],event.values[1],event.values[2], true);
                        mIsFirstAccelerometerSensing = false;
                    } else {
                        // Noise reduced from acceleration of the device's local X, Y and Z axis (m/s^2)
                        measurement = new Measurement(DSPFilter.smoothingAndFiltering(event.values[0],event.values[1],event.values[2], true), actualTime);
                        Log.d(LOG_TAG, "Acceleration: " + measurement.toString());
                        WriteToMeasurementFile(measurement);

                        // Isolate the force of gravity with the low-pass filter.
                        mGravity[0] = lowPass(measurement.getX(), mGravity[0]);
                        mGravity[1] = lowPass(measurement.getY(), mGravity[1]);
                        mGravity[2] = lowPass(measurement.getZ(), mGravity[2]);

                        // Magnitude of gravity (m/s^2)
                        measurement = new Measurement(mGravity[0], mGravity[1], mGravity[2], actualTime);
                        WriteToMeasurementFile(measurement);
                    }
                    break;
                case Sensor.TYPE_GYROSCOPE:
                    if (mIsFirstGyroscopeSensing) {
                        DSPFilter.initFilter(event.values[0],event.values[1],event.values[2], false);
                        mIsFirstGyroscopeSensing = false;
                    } else {
                        // Noise reduced rate of rotation around the device's local X, Y and Z axis (rad/s)
                        measurement = new Measurement(DSPFilter.smoothingAndFiltering(event.values[0],event.values[1],event.values[2], false), actualTime);
                        Log.d(LOG_TAG, "Rotation: " + measurement.toString());
                        WriteToMeasurementFile(measurement);
                    }
                    break;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    /**
     * Creates a file to registry the accelerometer variations.
     * @param actualTime the time (milliseconds) when the file is created.
     */
    private void CreateMeasurementFile(long actualTime) {
        try {
            mFileTimeStamp = actualTime;
            mFilename = MobileWearConstants.MEASUREMENT_FILENAME_START + mFileTimeStamp;
            mMeasurementsFile = mContext.openFileOutput(mFilename, Context.MODE_PRIVATE);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Closes the current measurement file.
     */
    private void CloseMeasurementFile() {
        try {
            mMeasurementsFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Writes a measurement to the current measurement file.
     * @param measurement the accelerometer measurement.
     */
    private void WriteToMeasurementFile(Measurement measurement) {
        try {
            mMeasurementsFile.write(measurement.toString().getBytes());
            //Log.d(LOG_TAG, measurement.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends the current measurement file to the handheld node.
     * @param actualTime the current time (millisecond).
     */
    private void SendFileToHandHeld(long actualTime) {
        if (actualTime - mFileTimeStamp > mFileSendingTime) {
            CloseMeasurementFile();
            mSendByChannelThread = new SendByChannelThread(mContext, mFilename);
            mSendByChannelThread.start();
            CreateMeasurementFile(actualTime);
        }
    }

    public void setFileSendingTime(Long mFileSendingTime) {
        this.mFileSendingTime = mFileSendingTime;
    }

    private float lowPass(float current, float gravity) { return gravity * mAlpha + current * (1 - mAlpha); }

    private float highPass(float current, float gravity) {
        return current - gravity;
    }
}
