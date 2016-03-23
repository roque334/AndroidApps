package com.example.roquecontreras.dataapi;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Environment;
import android.util.Log;

import com.example.roquecontreras.common.DSPFilter;
import com.example.roquecontreras.common.MeasureType;
import com.example.roquecontreras.common.Measurement;
import com.example.roquecontreras.common.MobileWearConstants;

import java.io.File;
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

    //GyroscopeSensor variables
    private Sensor mSensorGyroscope;
    private boolean mIsFirstGyroscopeSensing;

    //File variables
    private FileOutputStream mMeasurementsFile;
    private String mFilename;
    private Long mFileTimeStamp;

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
    public String Terminate() {
        mSensorManager.unregisterListener(this);
        CloseMeasurementFile();
        mRunning = false;
        return mFilename;
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
                        //DSPFilter.initFilter(event.values[0],event.values[1],event.values[2], true);
                        mIsFirstAccelerometerSensing = false;
                    } else {
                        // Raw acceleration (m/s^2)
                        measurement = new Measurement(MeasureType.RAW_ACCELERATION,event.values[0],event.values[1],event.values[2], actualTime);
                        WriteToMeasurementFile(measurement);

                        /*
                        // Total acceleration (m/s^2)
                        measurement = new Measurement(MeasureType.TOTAL_ACCELERATION, DSPFilter.smoothingAndFiltering(event.values[0],event.values[1],event.values[2], true), actualTime);
                        WriteToMeasurementFile(measurement);

                        // Body acceleration (m/s^2)
                        measurement = new Measurement(MeasureType.BODY_ACCELERATION, DSPFilter.gravitylFiltering(measurement.getX(), measurement.getY(), measurement.getZ()), actualTime);
                        WriteToMeasurementFile(measurement);
                        */
                    }
                    break;
                case Sensor.TYPE_GYROSCOPE:
                    if (mIsFirstGyroscopeSensing) {
                        //DSPFilter.initFilter(event.values[0],event.values[1],event.values[2], false);
                        mIsFirstGyroscopeSensing = false;
                    } else {
                        // Raw Angular speed (rad/s)
                        measurement = new Measurement(MeasureType.RAW_ANGULAR_SPEED, event.values[0], event.values[1], event.values[2], actualTime);
                        WriteToMeasurementFile(measurement);

                        /*
                        // Angular speed (rad/s)
                        measurement = new Measurement(MeasureType.ANGULAR_SPEED, DSPFilter.gyroscopeFiltering(event.values[0], event.values[1], event.values[2]), actualTime);
                        WriteToMeasurementFile(measurement);
                        */
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
        final File file;
        File sdcard, dir;
        try {
            mFileTimeStamp = actualTime;
            mFilename = MobileWearConstants.MEASUREMENT_FILENAME_START + mFileTimeStamp;
            sdcard = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
            dir = new File(sdcard.getAbsolutePath()+ "/Moreno/");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            file = new File(dir, mFilename);
            if (file.exists()) {
                file.delete();
            }
            mMeasurementsFile = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
        }
    }

    /**
     * Closes the current measurement file.
     */
    private void CloseMeasurementFile() {
        try {
            mMeasurementsFile.close();
        } catch (IOException e) {
        }
    }

    /**
     * Writes a measurement to the current measurement file.
     * @param measurement the accelerometer measurement.
     */
    private void WriteToMeasurementFile(Measurement measurement) {
        try {
            mMeasurementsFile.write(measurement.toString().getBytes());
        } catch (IOException e) {
        }
    }

}
