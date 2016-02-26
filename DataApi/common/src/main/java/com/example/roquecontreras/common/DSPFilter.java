package com.example.roquecontreras.common;

import java.util.Arrays;

/**
 * Created by roquecontreras on 12/02/16.
 */
public class DSPFilter {

    private static final float mDCBias = 0;
    private static float[][] accelerometerMedianWindow;
    private static float[][] gyroscopeMedianWindow;

    public static void initFilter(float x, float y, float z, boolean isAccelerometerValue) {
        if (accelerometerMedianWindow == null) {
            accelerometerMedianWindow = new float[3][];
            accelerometerMedianWindow[0] = new float[3];
            accelerometerMedianWindow[1] = new float[3];
            accelerometerMedianWindow[2] = new float[3];
            Arrays.fill(accelerometerMedianWindow[0],0f);
            Arrays.fill(accelerometerMedianWindow[1],0f);
            Arrays.fill(accelerometerMedianWindow[2],0f);
        }

        if (gyroscopeMedianWindow == null) {
            gyroscopeMedianWindow = new float[3][];
            gyroscopeMedianWindow[0] = new float[3];
            gyroscopeMedianWindow[1] = new float[3];
            gyroscopeMedianWindow[2] = new float[3];
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

    private static float thirdOrderMedianFilter(float[] window){
        float result;
        Arrays.sort(window);
        result = window[1];
        window[0] = window[1];
        window[1] = window[2];
        return result;
    }

    private static float thirdOrderButterWorthFilter(float x){
        float num,dem,result;
        num = 0.52762f * ((float) Math.pow(x,3)) + 1.58287f * ((float) Math.pow(x,2)) + 1.58287f * x + 0.52762f;
        dem = 1f * ((float) Math.pow(x,3)) + 1.76004f * ((float) Math.pow(x,2)) + 1.18289f * x + 0.27806f;
        result = num / dem;
        return result;
    }

    private static float DCBiasFilter(float x) {
        return x - mDCBias;
    }

    private static float DCBiasFilter(float[] x){
        float result = 0;
        for (int i = 0; i < x.length; i++) {
            result += x[i];
        }
        result = result / x.length;;
        return result;
    }
}
