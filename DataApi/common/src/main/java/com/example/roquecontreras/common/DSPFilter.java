package com.example.roquecontreras.common;

import java.util.Arrays;

/**
 * Created by roquecontreras on 12/02/16.
 */
public class DSPFilter {

    private static float[][] accelerometerMedianWindow;
    private static float[] mGravity;
    private static final float mAlpha = 0.8f;

    private static float[][] gyroscopeMedianWindow;
    private static float[] mDCBias;
    private static float mDCBiasCounter;

    /**
     * Initializes the windows to do the filtering process.
     * @param x the value of the X axis.
     * @param y the value of the Y axis.
     * @param z the value of the Z axis.
     * @param isAccelerometerValue indicates if the values are fom the accelerometer sensor.
     */
    public static void initFilter(float x, float y, float z, boolean isAccelerometerValue) {
        if (accelerometerMedianWindow == null) {
            accelerometerMedianWindow = new float[3][];
            mGravity = new float[3];
            accelerometerMedianWindow[0] = new float[3];
            accelerometerMedianWindow[1] = new float[3];
            accelerometerMedianWindow[2] = new float[3];
            Arrays.fill(accelerometerMedianWindow[0],0f);
            Arrays.fill(accelerometerMedianWindow[1],0f);
            Arrays.fill(accelerometerMedianWindow[2],0f);
            Arrays.fill(mGravity,0f);
        }

        if (gyroscopeMedianWindow == null) {
            mDCBias = new float[3];
            gyroscopeMedianWindow = new float[3][];
            gyroscopeMedianWindow[0] = new float[3];
            gyroscopeMedianWindow[1] = new float[3];
            gyroscopeMedianWindow[2] = new float[3];
            mDCBiasCounter = 0;
            Arrays.fill(gyroscopeMedianWindow[0],0f);
            Arrays.fill(gyroscopeMedianWindow[1],0f);
            Arrays.fill(gyroscopeMedianWindow[2],0f);
        }

        if (isAccelerometerValue) {
            accelerometerMedianWindow[0][1] = x;
            accelerometerMedianWindow[1][1] = y;
            accelerometerMedianWindow[2][1] = z;
        } else {
            gyroscopeMedianWindow[0][1] = x;
            gyroscopeMedianWindow[1][1] = y;
            gyroscopeMedianWindow[2][1] = z;
        }
    }

    /**
     * Filtes the gravity component of the accelerometer values.
     * @param x the value of the X axis.
     * @param y the value of the Y axis.
     * @param z the value of the Z axis.
     * @return the body acceleration.
     */
    public static float[] gravitylFiltering(float x, float y, float z) {
        float[] result;
        result = new float[3];
        mGravity[0] = mAlpha * mGravity[0] + (1 - mAlpha) * x;
        mGravity[1] = mAlpha * mGravity[1] + (1 - mAlpha) * y;
        mGravity[2] = mAlpha * mGravity[2] + (1 - mAlpha) * z;

        result[0] = x - mGravity[0];
        result[1] = y - mGravity[1];
        result[2] = z - mGravity[2];

        return result;
    }

    /**
     * Filters the gyroscope values.
     * @param x the value of the X axis.
     * @param y the value of the Y axis.
     * @param z the value of the Z axis.
     * @return the angular speed.
     */
    public static float[] gyroscopeFiltering(float x, float y, float z) {
        float[] result, aux;
        result = new float[3];
        aux = new float[3];
        aux = smoothingAndFiltering(x, y, z, false);
        mDCBiasCounter += 1;
        mDCBias[0] = ((mDCBias[0] * (mDCBiasCounter-1)) + aux[0])/mDCBiasCounter;
        mDCBias[1] = ((mDCBias[1] * (mDCBiasCounter-1)) + aux[1])/mDCBiasCounter;
        mDCBias[2] = ((mDCBias[2] * (mDCBiasCounter-1)) + aux[2])/mDCBiasCounter;
        result[0] = aux[0] - mDCBias[0];
        result[1] = aux[1] - mDCBias[1];
        result[2] = aux[2] - mDCBias[2];
        return result;
    }

    /**
     * Smooths and Filters the sensor values.
     * @param x the value of the X axis.
     * @param y the value of the Y axis.
     * @param z the value of the Z axis.
     * @param isAccelerometerValue indicates if the values are fom the accelerometer sensor.
     * @return the filtered values of the sensor.
     */
    public static float[] smoothingAndFiltering(float x, float y, float z, boolean isAccelerometerValue) {
        float[] result;
        result = new float[3];
        if (isAccelerometerValue) {
            accelerometerMedianWindow[0][2] = x;
            accelerometerMedianWindow[1][2] = y;
            accelerometerMedianWindow[2][2] = z;
            result[0] = thirdOrderButterWorthFilter(thirdOrderMedianFilter(accelerometerMedianWindow[0]));
            result[1] = thirdOrderButterWorthFilter(thirdOrderMedianFilter(accelerometerMedianWindow[1]));
            result[2] = thirdOrderButterWorthFilter(thirdOrderMedianFilter(accelerometerMedianWindow[2]));
        } else {
            gyroscopeMedianWindow[0][2] = x;
            gyroscopeMedianWindow[1][2] = y;
            gyroscopeMedianWindow[2][2] = z;
            result[0] = thirdOrderButterWorthFilter(thirdOrderMedianFilter(gyroscopeMedianWindow[0]));
            result[1] = thirdOrderButterWorthFilter(thirdOrderMedianFilter(gyroscopeMedianWindow[1]));
            result[2] = thirdOrderButterWorthFilter(thirdOrderMedianFilter(gyroscopeMedianWindow[2]));

        }
        return result;
    }

    /**
     * Applies a third order media filter with a window of size three.
     * @param window the window.
     * @return the value filtered.
     */
    private static float thirdOrderMedianFilter(float[] window){
        float[] auxilarWindow = new float[3];
        float result;
        for(int i=0;i<window.length;i++) {
            auxilarWindow[i] = window[i];
        }
        window[0] = window[1];
        window[1] = window[2];

        Arrays.sort(auxilarWindow);

        result = auxilarWindow[1];

        return result;
    }

    /**
     * Applies a a pre-build butter worth filter over a values.
     * @param x the value to filter.
     * @return the value filtered.
     */
    private static float thirdOrderButterWorthFilter(float x){
        float num,dem,result;
        num = 0.52762f * ((float) Math.pow(x,3)) + 1.58287f * ((float) Math.pow(x,2)) + 1.58287f * x + 0.52762f;
        dem = 1f * ((float) Math.pow(x,3)) + 1.76004f * ((float) Math.pow(x,2)) + 1.18289f * x + 0.27806f;
        result = num / dem;
        return result;
    }

    /**
     * Calculates the DC component from a signal.
     * @param x the signal.
     * @return the DC component.
     */
    private static float DCBiasFilter(float[] x){
        float result = 0;
        for (int i = 0; i < x.length; i++) {
            result += x[i];
        }
        result = result / x.length;;
        return result;
    }
}
