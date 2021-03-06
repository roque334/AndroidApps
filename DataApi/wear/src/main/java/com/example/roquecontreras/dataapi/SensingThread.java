package com.example.roquecontreras.dataapi;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Environment;

//In case you want to filter the signal uncomment the following line.
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

    private SensorManager mSensorManager;

    //AccelerometerSensor variables
    private Sensor mSensorAccelerometer;
//    private boolean mIsFirstAccelerometerSensing;

    //AccelerometerSensor variables
    private Sensor mSensorGravity;
//    private boolean mIsFirstGravitySensing;

    //GyroscopeSensor variables
    private Sensor mSensorGyroscope;
//    private boolean mIsFirstGyroscopeSensing;


    //File variables
    private FileManager mFileManagerThread;
    private FileOutputStream mMeasurementsFile;
    private String mFilename;
    private StringBuilder mText;
    private int mSamplesNumber;
    private Long mFileTimeStamp;

    /**
     * Class constructor specifying the application context.
     */
    public SensingThread(Context context) {
        mContext = context;
        mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        mSensorAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        mSensorGravity = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        mSensorGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        //In case you want to filter the signal uncomment the following line.
        //mIsFirstAccelerometerSensing = mIsFirstGravitySensing = mIsFirstGyroscopeSensing = true;
        CreateMeasurementFile(System.currentTimeMillis());
        mSamplesNumber = 0;
        mText = new StringBuilder();
    }

    /**
     * Return weather the thread is running or not.
     *
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
        WriteMeasurements();
        CloseMeasurementFile();
        mSensorManager.unregisterListener(this);
        mRunning = false;
        return mFilename;
    }

    /**
     * Starts the sensor accelerometer measurement.
     */
    @Override
    public void run() {
        mRunning = true;
        //SensorManager.SENSOR_DELAY_GAME senses with frequency of sampling = 50Hz).
        mSensorManager.registerListener(this, mSensorAccelerometer, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, mSensorGravity, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, mSensorGyroscope, SensorManager.SENSOR_DELAY_GAME);
    }

    /**
     * Gets the accelerometer variations, removes the gravity force and write it to the
     * current measurement file.
     *
     * @param event the accelerometer variation.
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        synchronized (this) {
            long actualTime;
            Measurement measurement;

            actualTime = System.currentTimeMillis();

            switch (event.sensor.getType()) {
                case Sensor.TYPE_LINEAR_ACCELERATION:
                    //In case you want to filter the signal uncomment the following lines.
                    /*if (mIsFirstAccelerometerSensing) {
                        DSPFilter.initFilter(event.values[0],event.values[1],event.values[2], event.sensor.getType());
                        mIsFirstAccelerometerSensing = false;
                    } else {*/
                    measurement = new Measurement(MeasureType.RAW_ACCELERATION, event.values[0], event.values[1], event.values[2], actualTime);
                    mText.append(measurement.toString());
                    //In case you want to filter the signal uncomment the following lines.
                    //measurement = new Measurement(MeasureType.BODY_ACCELERATION, DSPFilter.smoothingAndFiltering(event.values[0], event.values[1], event.values[2], event.sensor.getType()), actualTime);
                    //mText.append(measurement.toString());
                    //}
                    break;
                case Sensor.TYPE_GRAVITY:
                    //In case you want to filter the signal uncomment the following lines.
                    /*if (mIsFirstGravitySensing) {
                        DSPFilter.initFilter(event.values[0],event.values[1],event.values[2], event.sensor.getType());
                        mIsFirstGravitySensing = false;
                    } else {*/
                    measurement = new Measurement(MeasureType.RAW_GRAVITY, event.values[0], event.values[1], event.values[2], actualTime);
                    mText.append(measurement.toString());
                    //In case you want to filter the signal uncomment the following lines.
                    //measurement = new Measurement(MeasureType.GRAVITY, DSPFilter.smoothingAndFiltering(event.values[0], event.values[1], event.values[2], event.sensor.getType()), actualTime);
                    //mText.append(measurement.toString());
                    //}
                    break;
                case Sensor.TYPE_GYROSCOPE:
                    //In case you want to filter the signal uncomment the following lines.
                    /*if (mIsFirstGyroscopeSensing) {
                        DSPFilter.initFilter(event.values[0],event.values[1],event.values[2], event.sensor.getType());
                        mIsFirstGyroscopeSensing = false;
                    } else {*/
                    measurement = new Measurement(MeasureType.RAW_ANGULAR_SPEED, event.values[0], event.values[1], event.values[2], actualTime);
                    mText.append(measurement.toString());
                    //In case you want to filter the signal uncomment the following lines.
                    //measurement = new Measurement(MeasureType.ANGULAR_SPEED, DSPFilter.smoothingAndFiltering(event.values[0], event.values[1], event.values[2], event.sensor.getType()), actualTime);
                    //mText.append(measurement.toString());
                    //}
                    break;
            }
            mSamplesNumber += 1;
            if (mSamplesNumber > 500) {
                WriteMeasurements();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    /**
     * Creates a file to registry the accelerometer variations.
     *
     * @param actualTime the time (milliseconds) when the file is created.
     */
    private void CreateMeasurementFile(long actualTime) {
        final File file;
        File sdcard, dir;
        try {
            mFileTimeStamp = actualTime;
            mFilename = MobileWearConstants.MEASUREMENT_FILENAME_START + mFileTimeStamp;
            sdcard = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
            dir = new File(sdcard.getAbsolutePath() + "/Moreno/");
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
     * Writes the measures into a file in a separate thread.
     */
    private void WriteMeasurements() {
        mFileManagerThread = new FileManager(mContext, mMeasurementsFile);
        mFileManagerThread.setText(mText);
        mFileManagerThread.start();
        try {
            mFileManagerThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mText = new StringBuilder();
        mSamplesNumber = 0;
    }
}
