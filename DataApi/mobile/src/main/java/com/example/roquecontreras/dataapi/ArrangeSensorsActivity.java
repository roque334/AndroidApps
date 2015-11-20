package com.example.roquecontreras.dataapi;

import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.roquecontreras.common.Constants;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ArrangeSensorsActivity extends Activity {

    private static final String LOG_TAG = "ArrangeSensorsActivity";

    private String[] mWearableNodesIDs;
    private int mNumberWearablesAvailables;

    private ImageView mBodyLeftArm;
    private ImageView mBodyRightArm;
    private ImageView mBodyLeftLeg;
    private ImageView mBodyRightLeg;
    private TextView mAvailableSensors;

    private GoogleApiClient mGoogleApiClient;

    private void initializeGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle connectionHint) {
                        Log.d(LOG_TAG, "onConnected: " + connectionHint);
                    }
                    @Override
                    public void onConnectionSuspended(int cause) {
                        Log.d(LOG_TAG, "onConnectionSuspended: " + cause);
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult result) {
                        Log.d(LOG_TAG, "onConnectionFailed: " + result);
                    }
                })
                .addApi(Wearable.API)
                .build();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_arrange_sensors);

        initializeGoogleApiClient();
        loadExtras(getIntent());

        Button arrangeSensorsButton = (Button) findViewById(R.id.arrange_sensors_button);
        mBodyLeftArm = (ImageView) findViewById(R.id.body_left_arm_imageview);
        mBodyRightArm = (ImageView) findViewById(R.id.body_right_arm_imageview);
        mBodyLeftLeg = (ImageView) findViewById(R.id.body_left_leg_imageview);
        mBodyRightLeg = (ImageView) findViewById(R.id.body_right_leg_imageview);
        mAvailableSensors = (TextView) findViewById(R.id.available_sensors_textview);

        mAvailableSensors.setText(new Integer(mNumberWearablesAvailables).toString());

        arrangeSensorsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                arrangeSensorsUsingMessages(Constants.ARRANGE_SENSORS_BY_DATAITEM_PATH);
            }
        });
    }

    public void onCheckboxClicked(View view) {
        int viewID;
        boolean isChecked;
        CheckBox checkBox = (CheckBox) view;
        isChecked =  checkBox.isChecked();
        viewID = view.getId();
        setImageToImageView(viewID, isChecked);
        if (mNumberWearablesAvailables == 0) {
            setEnabledStatusToCheckBoxes(getCheckBoxesByStatus(false),false);
        }else {
            if (mNumberWearablesAvailables == 1) {
                setEnabledStatusToCheckBoxes(getCheckBoxesByStatus(false),true);
            }
        }
        mAvailableSensors.setText(new Integer(mNumberWearablesAvailables).toString());
    }

    private void setImageToImageView(int viewID, boolean isChecked){
        setImage(viewID, isChecked);
        if (isChecked) {
            mNumberWearablesAvailables -= 1;
        } else {
            mNumberWearablesAvailables += 1;
        }

    }

    private void setImage(int viewID, boolean isChecked) {
        switch(viewID) {
            case R.id.left_arm_checkbox:
                if (isChecked) {
                    mBodyLeftArm.setImageResource(R.drawable.body_left_arm);
                }else {
                    mBodyLeftArm.setImageResource(R.drawable.body_left_arm_none);
                }
                break;
            case R.id.right_arm_checkbox:
                if (isChecked) {
                    mBodyRightArm.setImageResource(R.drawable.body_right_arm);
                }else {
                    mBodyRightArm.setImageResource(R.drawable.body_right_arm_none);
                }
                break;
            case R.id.left_leg_checkbox:
                if (isChecked) {
                    mBodyLeftLeg.setImageResource(R.drawable.body_left_leg);
                }else {
                    mBodyLeftLeg.setImageResource(R.drawable.body_left_leg_none);
                }
                break;
            case R.id.right_leg_checkbox:
                if (isChecked) {
                    mBodyRightLeg.setImageResource(R.drawable.body_right_leg);
                }else {
                    mBodyRightLeg.setImageResource(R.drawable.body_right_leg_none);
                }
            default:
                break;
        }
    }

    private void loadExtras(Intent currentIntent) {
        Bundle extras = currentIntent.getExtras();
        mWearableNodesIDs = extras.getStringArray(Constants.WEARABLE_NODES_EXTRA);
        mNumberWearablesAvailables = mWearableNodesIDs.length;
    }

    private ArrayList<CheckBox> getCheckBoxes(){
        ArrayList<CheckBox> checkBoxes = new ArrayList<CheckBox>();
        checkBoxes.add((CheckBox) this.findViewById(R.id.left_arm_checkbox));
        checkBoxes.add((CheckBox) this.findViewById(R.id.right_arm_checkbox));
        checkBoxes.add((CheckBox) this.findViewById(R.id.left_leg_checkbox));
        checkBoxes.add((CheckBox) this.findViewById(R.id.right_leg_checkbox));
        return checkBoxes;
    }


    private ArrayList<CheckBox> getCheckBoxesByStatus(boolean checkStatus) {
        ArrayList<CheckBox> checkBoxes, result;
        result = new ArrayList<>();
        checkBoxes = getCheckBoxes();
        for(CheckBox item : checkBoxes) {
            if (item.isChecked() == checkStatus){
                result.add(item);
            }
        }
        return result;

    }

    private void setEnabledStatusToCheckBoxes(ArrayList<CheckBox> checkBoxes, boolean enabledStatus) {
        for(CheckBox item : checkBoxes) {
            item.setEnabled(enabledStatus);
        }
    }


    private boolean arrangeSensorsUsingMessages(String command) {
        String[] WearableNodes;
        String message;
        boolean result = true;
        Map arrange = new HashMap<>();
        if (mGoogleApiClient.isConnected()) {
            for(int i = 0; i < mNumberWearablesAvailables; i++) {
                message = getBodyPart(getCheckBoxesByStatus(true),i);
                result = result && Wearable.MessageApi.sendMessage(mGoogleApiClient, mWearableNodesIDs[i],
                        command, message.getBytes()).await().getStatus().isSuccess();
                arrange.put(mWearableNodesIDs[i], message);
            }
        }
        else {
            result = false;
            Log.e(LOG_TAG, "No connection to wearable available!");
        }
        return result;
    }

    private String getBodyPart(ArrayList<CheckBox> checkBoxes, int pos) {
        String result = "";
        switch (checkBoxes.get(pos).getId()) {
            case R.id.left_arm_checkbox:
                result = "a_left";
                break;
            case R.id.right_arm_checkbox:
                result = "a_right";
                break;
            case R.id.left_leg_checkbox:
                result = "l_left";
                break;
            case R.id.right_leg_checkbox:
                result = "l_right";
            default:
                break;
        }
        return result;
    }
}
