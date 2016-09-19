package com.example.roquecontreras.dataapi;

import android.content.Context;
import android.os.Environment;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by roquecontreras on 13/09/16.
 */
public class FileManager extends Thread {

    private static final String LOG_TAG = "FileManager";
    private static FileOutputStream mMeasurementsFile;
    private static Context mContext;
    private static StringBuilder mText;
    private volatile boolean mIsSuccess = false;

    public FileManager(Context context) {
        mContext = context;
        mText = null;
        mMeasurementsFile = null;
    }

    public FileManager(Context context, FileOutputStream measurementsFile) {
        mContext = context;
        mMeasurementsFile = measurementsFile;
        mText = new StringBuilder();
    }

    public boolean isSuccess() {
        return mIsSuccess;
    }

    public void setText(StringBuilder text) {
        mText = text;
    }

    /**
     * Writes the measures to the file.
     *
     * @return true if the file was written correctly;
     * false otherwise.
     */
    public boolean WriteMeasurement() {
        boolean result = true;
        try {
            if (mMeasurementsFile != null) {
                mMeasurementsFile.write(mText.toString().getBytes());
            }
        } catch (IOException e) {
            result = false;
        }
        return result;
    }

    /**
     * Deletes the files in the Moreno directory.
     *
     * @return true if all files was deleted correctly;
     * false otherwise.
     */
    public boolean DeleteMeasurementFiles() {
        boolean result = true;
        File sdcard, dir, files[];
        sdcard = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        dir = new File(sdcard.getAbsolutePath() + "/Moreno/");
        if (dir.exists()) {
            files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        result = result && file.delete();
                    }
                }
            }
        } else {
        }
        return result;
    }

    @Override
    public void run() {
        if ((mMeasurementsFile != null) && (mText != null)) {
            mIsSuccess = WriteMeasurement();
        } else {
            mIsSuccess = DeleteMeasurementFiles();
        }
        Thread.currentThread().interrupt();
        return;
    }
}