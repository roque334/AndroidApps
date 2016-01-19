package com.example.roquecontreras.dataapi;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.example.roquecontreras.common.MobileWearConstants;

public class AccelerometerService extends Service {

    private static final String LOG_TAG = "AccelerometerService";
    private SensingThread mSensingThread;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(LOG_TAG, "onCreate");

        mSensingThread = new SensingThread(getApplicationContext());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(LOG_TAG, "onStartCommand");
        if (!mSensingThread.isRunning()) {
            mSensingThread.setMeasuringTime(intent.getExtras().getLong(MobileWearConstants.KEY_MEASUREMENTS_SAMPLE_INTERVAL));
            mSensingThread.setFileSendingTime(intent.getExtras().getLong(MobileWearConstants.KEY_HANDHELD_WEAR_SYNC_INTERVAL));
            mSensingThread.start();
        }
        return START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        Log.d(LOG_TAG, "onDestroy");
        if (mSensingThread != null && mSensingThread.isRunning()) {
            mSensingThread.Terminate();
            try {
                mSensingThread.join();
            } catch (InterruptedException e) {
                Log.d(LOG_TAG, "onDestroy: \n" + e.getMessage());
            }
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
