package com.example.roquecontreras.dataapi;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

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
    private Sensor mSensor;
    private final float mAlpha = 0.8f;
    private float[] mGravity = new float[3];
    private Long mLastSensorUpdate;

    public Long mMeasuringTime;

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
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
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
        mSensorManager.unregisterListener(this);
        CloseMeasurementFile();
        mSendByChannelThread = new SendByChannelThread(mContext, mFilename + mFileTimeStamp);
        mSendByChannelThread.start();
        try {
            mSendByChannelThread.join();
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
        mLastSensorUpdate = System.currentTimeMillis();
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_UI);;
    }

    /**
     * Gets the accelerometer variations, removes the gravity force and write it to the
     * current measurement file.
     * @param event the accelerometer variation.
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        Measurement measurement;
        long actualTime = System.currentTimeMillis();

        if (actualTime - mLastSensorUpdate > mMeasuringTime) {
            mLastSensorUpdate = actualTime;

            SendFileToHandHeld(actualTime);

            // Isolate the force of gravity with the low-pass filter.
            mGravity[0] = lowPass(event.values[0], mGravity[0]);
            mGravity[1] = lowPass(event.values[1], mGravity[1]);
            mGravity[2] = lowPass(event.values[2], mGravity[2]);

            // Remove the gravity contribution with the high-pass filter.
            measurement = new Measurement(highPass(event.values[0], mGravity[0]), highPass(event.values[1], mGravity[1]), highPass(event.values[2], mGravity[2]), actualTime);
            WriteToMeasurementFile(measurement);
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
            Log.d(LOG_TAG, measurement.toString());
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

    public void setMeasuringTime(Long mMeasuringTime) {
        this.mMeasuringTime = mMeasuringTime;
    }

    public void setFileSendingTime(Long mFileSendingTime) {
        this.mFileSendingTime = mFileSendingTime;
    }

    private float lowPass(float current, float gravity) { return gravity * mAlpha + current * (1 - mAlpha); }

    private float highPass(float current, float gravity) {
        return current - gravity;
    }
}
