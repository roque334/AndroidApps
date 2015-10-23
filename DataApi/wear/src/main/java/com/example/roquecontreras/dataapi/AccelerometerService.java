package com.example.roquecontreras.dataapi;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.util.Log;

public class AccelerometerService extends Service implements SensorEventListener{

    private static final String LOG_TAG = "AccelerometerService";
    private SensorManager mSensorManager;
    private Sensor mSensor;
    private final float alpha = 0.8f;
    private float[] gravity = new float[3];
    private float[] linear_acceleration = new float[3];
    private Long lastSensorUpdate;


    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(LOG_TAG, "onCreate");
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(LOG_TAG, "onStartCommand");
        lastSensorUpdate = System.currentTimeMillis();
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_UI);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        mSensorManager.unregisterListener(this);
        Log.d(LOG_TAG, "onDestroy");
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        long actualTime = System.currentTimeMillis();

        if (actualTime - lastSensorUpdate > 250) {

            lastSensorUpdate = actualTime;

            // Isolate the force of gravity with the low-pass filter.
            gravity[0] = lowPass(event.values[0], gravity[0]);
            gravity[1] = lowPass(event.values[1], gravity[1]);
            gravity[2] = lowPass(event.values[2], gravity[2]);

            // Remove the gravity contribution with the high-pass filter.
            linear_acceleration[0] = highPass(event.values[0], gravity[0]);
            linear_acceleration[1] = highPass(event.values[1], gravity[1]);
            linear_acceleration[2] = highPass(event.values[2], gravity[2]);

            Log.d(LOG_TAG, "X: " + linear_acceleration[0] + "  Y: " + linear_acceleration[1] + "  Z: " + linear_acceleration[2]);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private float lowPass(float current, float gravity) {
        return gravity * alpha + current * (1 - alpha);
    }

    private float highPass(float current, float gravity) {
        return current - gravity;
    }
}
