package com.example.roquecontreras.shared;

/**
 * Created by Roque Contreras on 15/10/2015.
 */
public class Measurement {

    private float x;
    private float y;
    private float z;

    public Measurement(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
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

}
