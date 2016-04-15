package com.example.roquecontreras.common;

import android.hardware.Sensor;
import android.util.Log;

import java.util.Arrays;

/**
 * Created by roquecontreras on 12/02/16.
 */
public class DSPFilter {

    final private static float[] aLow = new float[]{1.76004f,1.18289f,0.27806f};
    final private static float[] bLow = new float[]{0.52762f,1.58287f,1.58287f,0.52762f};

    final private static float[] aHigh = {-2.92461f, 2.85203f, -0.92737f};
    final private static float[] bHigh = {0.96300f, -2.88900f, 2.88900f, -0.96300f};

    private static float[][] accelerometerMedianWindow;
    private static float[][] xAccelerometerButterWindow;
    private static float[][] yAccelerometerButterWindow;

    private static float[][] gravityMedianWindow;
    private static float[][] xGravityButterWindow;
    private static float[][] yGravityButterWindow;

    private static float[][] gyroscopeMedianWindow;
    private static float[][] xLowGyroscopeButterWindow;
    private static float[][] yLowGyroscopeButterWindow;
    private static float[][] xHighGyroscopeButterWindow;
    private static float[][] yHighGyroscopeButterWindow;

    /**
     * Initializes the windows to do the filtering process.
     * @param x the value of the X axis.
     * @param y the value of the Y axis.
     * @param z the value of the Z axis.
     * @param type indicates the sensor.
     */
    public static void initFilter(float x, float y, float z, int type) {
        if (accelerometerMedianWindow == null) {
            accelerometerMedianWindow = new float[3][];
            xAccelerometerButterWindow = new float[3][];
            yAccelerometerButterWindow = new float[3][];
            for (int i = 0; i < accelerometerMedianWindow.length; i++) {
                accelerometerMedianWindow[i] = new float[3];
                Arrays.fill(accelerometerMedianWindow[i], 0f);
            }
            for (int i = 0; i < xAccelerometerButterWindow.length; i++) {
                xAccelerometerButterWindow[i] = new float[4];
                yAccelerometerButterWindow[i] = new float[3];
                Arrays.fill(xAccelerometerButterWindow[i], 0f);
                Arrays.fill(yAccelerometerButterWindow[i], 0f);
            }
        }

        if (gravityMedianWindow == null) {
            gravityMedianWindow = new float[3][];
            xGravityButterWindow = new float[3][];
            yGravityButterWindow = new float[3][];
            for (int i = 0; i < gravityMedianWindow.length; i++) {
                gravityMedianWindow[i] = new float[3];
                Arrays.fill(gravityMedianWindow[i], 0f);
            }
            for (int i = 0; i < xGravityButterWindow.length; i++) {
                xGravityButterWindow[i] = new float[4];
                yGravityButterWindow[i] = new float[3];
                Arrays.fill(xGravityButterWindow[i], 0f);
                Arrays.fill(yGravityButterWindow[i], 0f);
            }
        }

        if (gyroscopeMedianWindow == null) {
            gyroscopeMedianWindow = new float[3][];
            xLowGyroscopeButterWindow = new float[3][];
            yLowGyroscopeButterWindow = new float[3][];
            xHighGyroscopeButterWindow = new float[3][];
            yHighGyroscopeButterWindow = new float[3][];
            for (int i = 0; i < gyroscopeMedianWindow.length; i++) {
                gyroscopeMedianWindow[i] = new float[3];
                Arrays.fill(gyroscopeMedianWindow[i], 0f);
            }
            for (int i = 0; i < xLowGyroscopeButterWindow.length; i++) {
                xLowGyroscopeButterWindow[i] = new float[4];
                yLowGyroscopeButterWindow[i] = new float[3];
                Arrays.fill(xLowGyroscopeButterWindow[i], 0f);
                Arrays.fill(yLowGyroscopeButterWindow[i], 0f);
            }
            for (int i = 0; i < xHighGyroscopeButterWindow.length; i++) {
                xHighGyroscopeButterWindow[i] = new float[4];
                yHighGyroscopeButterWindow[i] = new float[3];
                Arrays.fill(xHighGyroscopeButterWindow[i], 0f);
                Arrays.fill(yHighGyroscopeButterWindow[i], 0f);
            }
        }

        switch (type) {
            case Sensor.TYPE_LINEAR_ACCELERATION:
                accelerometerMedianWindow[0][1] = x;
                accelerometerMedianWindow[1][1] = y;
                accelerometerMedianWindow[2][1] = z;
                break;
            case Sensor.TYPE_GRAVITY:
                gravityMedianWindow[0][1] = x;
                gravityMedianWindow[1][1] = y;
                gravityMedianWindow[2][1] = z;
                break;
            case Sensor.TYPE_GYROSCOPE:
                gyroscopeMedianWindow[0][1] = x;
                gyroscopeMedianWindow[1][1] = y;
                gyroscopeMedianWindow[2][1] = z;
                break;
        }
    }

    /**
     * Smooths and Filters the sensor values.
     * @param x the value of the X axis.
     * @param y the value of the Y axis.
     * @param z the value of the Z axis.
     * @param type indicates the sensor.
     * @return the filtered values of the sensor.
     */
    public static float[] smoothingAndFiltering(float x, float y, float z, int type) {
        float[] result;
        result = new float[3];
        switch (type) {
            case Sensor.TYPE_LINEAR_ACCELERATION:
                accelerometerMedianWindow[0][2] = x;
                accelerometerMedianWindow[1][2] = y;
                accelerometerMedianWindow[2][2] = z;

                advanceInLowButterWindow(type, true);
                xAccelerometerButterWindow[0][0] = thirdOrderMedianFilter(accelerometerMedianWindow[0]);
                xAccelerometerButterWindow[1][0] = thirdOrderMedianFilter(accelerometerMedianWindow[1]);
                xAccelerometerButterWindow[2][0] = thirdOrderMedianFilter(accelerometerMedianWindow[2]);
                result = thirdOrderButterWorthLowPassTimeInvariantFilter(type);
                advanceInLowButterWindow(type, false);
                yAccelerometerButterWindow[0][0] = result[0];
                yAccelerometerButterWindow[1][0] = result[1];
                yAccelerometerButterWindow[2][0] = result[2];
                break;
            case Sensor.TYPE_GRAVITY:
                gravityMedianWindow[0][2] = x;
                gravityMedianWindow[1][2] = y;
                gravityMedianWindow[2][2] = z;

                advanceInLowButterWindow(type, true);
                xGravityButterWindow[0][0] = thirdOrderMedianFilter(gyroscopeMedianWindow[0]);
                xGravityButterWindow[1][0] = thirdOrderMedianFilter(gyroscopeMedianWindow[1]);
                xGravityButterWindow[2][0] = thirdOrderMedianFilter(gyroscopeMedianWindow[2]);
                result = thirdOrderButterWorthLowPassTimeInvariantFilter(type);
                advanceInLowButterWindow(type, false);
                yGravityButterWindow[0][0] = result[0];
                yGravityButterWindow[1][0] = result[1];
                yGravityButterWindow[2][0] = result[2];
                break;
            case Sensor.TYPE_GYROSCOPE:
                gyroscopeMedianWindow[0][2] = x;
                gyroscopeMedianWindow[1][2] = y;
                gyroscopeMedianWindow[2][2] = z;

                advanceInLowButterWindow(type, true);
                xLowGyroscopeButterWindow[0][0] = thirdOrderMedianFilter(gyroscopeMedianWindow[0]);
                xLowGyroscopeButterWindow[1][0] = thirdOrderMedianFilter(gyroscopeMedianWindow[1]);
                xLowGyroscopeButterWindow[2][0] = thirdOrderMedianFilter(gyroscopeMedianWindow[2]);
                result = thirdOrderButterWorthLowPassTimeInvariantFilter(type);
                advanceInLowButterWindow(type, false);
                yLowGyroscopeButterWindow[0][0] = result[0];
                yLowGyroscopeButterWindow[1][0] = result[1];
                yLowGyroscopeButterWindow[2][0] = result[2];

                advanceInHighButterWindow(true);
                xHighGyroscopeButterWindow[0][0] = result[0];
                xHighGyroscopeButterWindow[1][0] = result[1];
                xHighGyroscopeButterWindow[2][0] = result[2];
                result = thirdOrderButterWorthHighPassTimeInvariantFilter();
                advanceInHighButterWindow(false);
                yHighGyroscopeButterWindow[0][0] = result[0];
                yHighGyroscopeButterWindow[1][0] = result[1];
                yHighGyroscopeButterWindow[2][0] = result[2];
                break;
        }
        return result;
    }

    /**
     * Advances int the windows of the Low-Pass Butter Filter.
     * @param type the type of sensor.
     * @param isInput the windows type.
     */
    private static void advanceInLowButterWindow(int type, boolean isInput) {
        switch (type) {
            case Sensor.TYPE_LINEAR_ACCELERATION:
                if (isInput) {
                    for (int i = 0; i < xAccelerometerButterWindow.length; i++) {
                        xAccelerometerButterWindow[i][3] = xAccelerometerButterWindow[i][2];
                        xAccelerometerButterWindow[i][2] = xAccelerometerButterWindow[i][1];
                        xAccelerometerButterWindow[i][1] = xAccelerometerButterWindow[i][0];
                    }
                } else {
                    for (int i = 0; i < yAccelerometerButterWindow.length; i++) {
                        yAccelerometerButterWindow[i][2] = yAccelerometerButterWindow[i][1];
                        yAccelerometerButterWindow[i][1] = yAccelerometerButterWindow[i][0];
                    }
                }
                break;
            case Sensor.TYPE_GRAVITY:
                if (isInput) {
                    for (int i = 0; i < xGravityButterWindow.length; i++) {
                        xGravityButterWindow[i][3] = xGravityButterWindow[i][2];
                        xGravityButterWindow[i][2] = xGravityButterWindow[i][1];
                        xGravityButterWindow[i][1] = xGravityButterWindow[i][0];
                    }
                } else {
                    for (int i = 0; i < yGravityButterWindow.length; i++) {
                        yGravityButterWindow[i][2] = yGravityButterWindow[i][1];
                        yGravityButterWindow[i][1] = yGravityButterWindow[i][0];
                    }
                }
                break;
            case Sensor.TYPE_GYROSCOPE:
                if (isInput) {
                    for (int i = 0; i < xLowGyroscopeButterWindow.length; i++) {
                        xLowGyroscopeButterWindow[i][3] = xLowGyroscopeButterWindow[i][2];
                        xLowGyroscopeButterWindow[i][2] = xLowGyroscopeButterWindow[i][1];
                        xLowGyroscopeButterWindow[i][1] = xLowGyroscopeButterWindow[i][0];
                    }
                } else {
                    for (int i = 0; i < yLowGyroscopeButterWindow.length; i++) {
                        yLowGyroscopeButterWindow[i][2] = yLowGyroscopeButterWindow[i][1];
                        yLowGyroscopeButterWindow[i][1] = yLowGyroscopeButterWindow[i][0];
                    }
                }
                break;
        }
    }

    /**
     * Applies a a pre-build low-pass butter worth filter over a values with fc = 20Hz.
     * @param type indicates the sensor.
     * @return the value filtered.
     */
    private static float[] thirdOrderButterWorthLowPassTimeInvariantFilter(int type){
        float [] aux1,aux2, result;
        aux1 = new float[3];
        aux2 = new float[3];
        result = new float[3];
        Arrays.fill(aux1, 0f);
        Arrays.fill(aux2, 0f);
        switch (type) {
            case Sensor.TYPE_LINEAR_ACCELERATION:
                for (int i = 0; i < bLow.length; i++) {
                    aux1[0] += bLow[i] * xAccelerometerButterWindow[0][i];
                    aux1[1] += bLow[i] * xAccelerometerButterWindow[1][i];
                    aux1[2] += bLow[i] * xAccelerometerButterWindow[2][i];
                }
                for (int i = 0; i < aLow.length; i++) {
                    aux2[0] += aLow[i] * yAccelerometerButterWindow[0][i];
                    aux2[1] += aLow[i] * yAccelerometerButterWindow[1][i];
                    aux2[2] += aLow[i] * yAccelerometerButterWindow[2][i];
                }
                break;
            case Sensor.TYPE_GRAVITY:
                for (int i = 0; i < bLow.length; i++) {
                    aux1[0] += bLow[i] * xGravityButterWindow[0][i];
                    aux1[1] += bLow[i] * xGravityButterWindow[1][i];
                    aux1[2] += bLow[i] * xGravityButterWindow[2][i];
                }
                for (int i = 0; i < aLow.length; i++) {
                    aux2[0] += aLow[i] * yGravityButterWindow[0][i];
                    aux2[1] += aLow[i] * yGravityButterWindow[1][i];
                    aux2[2] += aLow[i] * yGravityButterWindow[2][i];
                }
                break;
            case Sensor.TYPE_GYROSCOPE:
                for (int i = 0; i < bLow.length; i++) {
                    aux1[0] += bLow[i] * xLowGyroscopeButterWindow[0][i];
                    aux1[1] += bLow[i] * xLowGyroscopeButterWindow[1][i];
                    aux1[2] += bLow[i] * xLowGyroscopeButterWindow[2][i];
                }
                for (int i = 0; i < aLow.length; i++) {
                    aux2[0] += aLow[i] * yLowGyroscopeButterWindow[0][i];
                    aux2[1] += aLow[i] * yLowGyroscopeButterWindow[1][i];
                    aux2[2] += aLow[i] * yLowGyroscopeButterWindow[2][i];
                }
                break;
        }
        result[0] = aux1[0] - aux2[0];
        result[1] = aux1[1] - aux2[1];
        result[2] = aux1[2] - aux2[2];
        return result;
    }

    /**
     * Advances in the windows of the High-Pass Butter Filter.
     * @param isInput the windows type.
     */
    private static void advanceInHighButterWindow(boolean isInput) {

        if (isInput) {
            for (int i = 0; i < xHighGyroscopeButterWindow.length; i++) {
                xHighGyroscopeButterWindow[i][3] = xHighGyroscopeButterWindow[i][2];
                xHighGyroscopeButterWindow[i][2] = xHighGyroscopeButterWindow[i][1];
                xHighGyroscopeButterWindow[i][1] = xHighGyroscopeButterWindow[i][0];
            }
        } else {
            for (int i = 0; i < yHighGyroscopeButterWindow.length; i++) {
                yHighGyroscopeButterWindow[i][2] = yHighGyroscopeButterWindow[i][1];
                yHighGyroscopeButterWindow[i][1] = yHighGyroscopeButterWindow[i][0];
            }
        }
    }

    /**
     * Applies a a pre-build high-pass butter worth filter over a values with fc = 0.3Hz.
     * @return the value filtered.
     */
    private static float[] thirdOrderButterWorthHighPassTimeInvariantFilter() {
        float[] aux1, aux2, result;
        aux1 = new float[3];
        aux2 = new float[3];
        result = new float[3];
        Arrays.fill(aux1, 0f);
        Arrays.fill(aux2, 0f);
        for (int i = 0; i < bHigh.length; i++) {
            aux1[0] += bHigh[i] * xHighGyroscopeButterWindow[0][i];
            aux1[1] += bHigh[i] * xHighGyroscopeButterWindow[1][i];
            aux1[2] += bHigh[i] * xHighGyroscopeButterWindow[2][i];
        }
        for (int i = 0; i < aHigh.length; i++) {
            aux2[0] += aHigh[i] * yHighGyroscopeButterWindow[0][i];
            aux2[1] += aHigh[i] * yHighGyroscopeButterWindow[1][i];
            aux2[2] += aHigh[i] * yHighGyroscopeButterWindow[2][i];
        }

        result[0] = aux1[0] - aux2[0];
        result[1] = aux1[1] - aux2[1];
        result[2] = aux1[2] - aux2[2];
        return result;
    }

    /**
     * Applies a third order media filter with a window of size three.
     * @param window the window.
     * @return the value filtered.
     */
    private static float thirdOrderMedianFilter(float[] window){
        float[] auxWindow = new float[window.length];
        float result;
        for(int i=0;i<auxWindow.length;i++) {
            auxWindow[i] = window[i];
        }
        Arrays.sort(auxWindow);
        result = auxWindow[1];

        window[0] = window[1];
        window[1] = window[2];

        return result;
    }
}
