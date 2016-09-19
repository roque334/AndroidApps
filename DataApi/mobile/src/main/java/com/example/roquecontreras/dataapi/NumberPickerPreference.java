package com.example.roquecontreras.dataapi;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.NumberPicker;

/**
 * Created by roquecontreras on 17/11/15.
 */
public class NumberPickerPreference extends DialogPreference {

    private static String TAG = NumberPickerPreference.class.getSimpleName();

    private static final int DEFAULT_MIN_VALUE = 1;
    private static final int DEFAULT_MAX_VALUE = 1000;
    private static final int DEFAULT_VALUE = 250;

    Integer mMinValue;
    Integer mMaxValue;

    Integer mValue;
    NumberPicker mNumberPicker;

    public NumberPickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        // get attributes specified in XML
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.NumberPickerPreference, 0, 0);
        try {
            mMinValue = (a.getInteger(R.styleable.NumberPickerPreference_min, DEFAULT_MIN_VALUE));
            mMaxValue = (a.getInteger(R.styleable.NumberPickerPreference_android_max, DEFAULT_MAX_VALUE));
        } finally {
            a.recycle();
        }

        setDialogLayoutResource(R.layout.numberpicker_dialog);
        setPositiveButtonText("OK");
        setNegativeButtonText("Cancel");
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        mNumberPicker = (NumberPicker) view.findViewById(R.id.numberPicker);
        mNumberPicker.setMinValue(mMinValue);
        mNumberPicker.setMaxValue(mMaxValue);


        if (mValue != null) mNumberPicker.setValue(mValue);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            mValue = mNumberPicker.getValue();
            persistInt(mValue);
        }
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        if (restorePersistedValue) {
            mValue = getPersistedInt(DEFAULT_VALUE);
        } else {
            mValue = (int) defaultValue;
            persistInt(mValue);
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInteger(index, DEFAULT_VALUE);
    }
}