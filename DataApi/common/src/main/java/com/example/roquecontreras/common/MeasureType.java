package com.example.roquecontreras.common;

/**
 * Created by roquecontreras on 14/03/16.
 */
public enum MeasureType {
    RAW_ACCELERATION(0), TOTAL_ACCELERATION(1), BODY_ACCELERATION(2), BODY_JERK(3), BODY_ACC_MAGNITUDE(4)
    , RAW_ANGULAR_SPEED(5), ANGULAR_SPEED(6), ANGULAR_ACCELERATION(7), ANGULAR_SPEED_MAGNITUDE(8)
    , RAW_GRAVITY(9), GRAVITY(10);

    private int value;

    private MeasureType(int val){
        value = val;
    }

    public int getValue(){
        return value;
    }

    @Override
    public String toString() {
        return String.valueOf(this.getValue());
    }
}
