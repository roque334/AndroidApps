package com.example.roquecontreras.common;

/**
 * Created by roquecontreras on 27/10/15.
 */
public class Measurement {
    private MeasureType type;
    private float x;
    private float y;
    private float z;
    long timestamp;

    public Measurement(MeasureType type, float x, float y, float z, long timestamp) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.z = z;
        this.timestamp = timestamp;
    }

    public Measurement(MeasureType type, float[] measures, long timestamp) {
        this.type = type;
        this.x = measures[0];
        this.y = measures[1];
        this.z = measures[2];
        this.timestamp = timestamp;
    }

    public MeasureType getType() {
        return type;
    }

    public void setType(MeasureType type) {
        this.type = type;
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
        return type.toString() + ";" + x + ";" + y + ";" + z + ";" + timestamp + "#" + System.lineSeparator();
    }
}
