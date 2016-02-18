package com.example.roquecontreras.common;

/**
 * Created by roquecontreras on 27/10/15.
 */
public class Measurement {
    float x;
    float y;
    float z;
    long timestamp;

    public Measurement(float x, float y, float z, long timestamp) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.timestamp = timestamp;
    }

    public Measurement(float[] measures, long timestamp) {
        this.x = measures[0];
        this.y = measures[1];
        this.z = measures[2];
        this.timestamp = timestamp;
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public float getZ() {
        return z;
    }

    public void setZ(float z) {
        this.z = z;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return x + ";" + y + ";" + z + ";" + timestamp;
    }
}
