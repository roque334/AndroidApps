package com.example.roquecontreras.dataapi;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.roquecontreras.common.MobileWearConstants;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Wearable;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ArrangeSensorsActivity extends Activity {

    private static final String LOG_TAG = "ArrangeSensorsActivity";

    private String[] mWearableNodesIDs;
    private int mNumberWearablesAvailables;

    private ImageView mBodyLeftArm;
    private ImageView mBodyRightArm;
    private ImageView mBodyLeftLeg;
    private ImageView mBodyRightLeg;
    private TextView mAvailableSensors;

    NetworkChangeReceiver mNetworkChangeReceiver;

    private GoogleApiClient mGoogleApiClient;

    /**
     * Builds the GoogleApiClient with the Wearable API.
     */
    private void initializeGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle connectionHint) {
                    }

                    @Override
                    public void onConnectionSuspended(int cause) {
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult result) {
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
                Map<String, String> arrange = startArrangeSensorsUsingMessagesThread(MobileWearConstants.ARRANGE_SENSORS_BY_MESSAGE_PATH);
                if (arrange != null) {
                    savesSensorArrangement(arrange);
                    setResult(RESULT_OK);
                    finish();
                } else {
                    setResult(RESULT_CANCELED);
                    finish();
                }
            }
        });
    }

    /**
     * Saves the sensor arrangement to long persistent file
     *
     * @param arrange the sensor arrangement
     */
    private void savesSensorArrangement(Map<String, String> arrange) {
        String line;
        FileOutputStream fos = null;
        try {
            fos = openFileOutput(MobileWearConstants.ARRANGEMENT_FILENAME, Context.MODE_PRIVATE);
            for (int i = 0; i < mWearableNodesIDs.length; i++) {
                if (arrange.containsKey(mWearableNodesIDs[i])) {
                    line = mWearableNodesIDs[i] + ";" + arrange.get(mWearableNodesIDs[i]) + System.lineSeparator();
                    fos.write(line.getBytes());
                }
            }
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");
        mNetworkChangeReceiver = new NetworkChangeReceiver();
        registerReceiver(mNetworkChangeReceiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mNetworkChangeReceiver);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
    }

    /**
     * Sets the proper image to a layout ImageView in accordance of the id of a view
     * and its checked status. Then updates the number of available sensors. Finally,
     * updates the layout checkboxes with false checked status if that number is equal
     * to zero; otherwise, enables the checkboxes with false checked status.
     *
     * @param view
     */
    public void onCheckboxClicked(View view) {
        int viewID;
        boolean isChecked;
        CheckBox checkBox = (CheckBox) view;
        isChecked = checkBox.isChecked();
        viewID = view.getId();
        setImageToImageView(viewID, isChecked);
        if (mNumberWearablesAvailables == 0) {
            setEnabledStatusToCheckBoxes(getCheckBoxesByStatus(false), false);
        } else {
            if (mNumberWearablesAvailables == 1) {
                setEnabledStatusToCheckBoxes(getCheckBoxesByStatus(false), true);
            }
        }
        mAvailableSensors.setText(new Integer(mNumberWearablesAvailables).toString());
    }

    /**
     * Sets the proper image to a layout ImageView in accordance of the id of a view
     * and its checked status. Then updates the number of available sensors.
     *
     * @param viewID    the id of a view.
     * @param isChecked the checked status of a view with the previous id,
     */
    private void setImageToImageView(int viewID, boolean isChecked) {
        setImage(viewID, isChecked);
        if (isChecked) {
            mNumberWearablesAvailables -= 1;
        } else {
            mNumberWearablesAvailables += 1;
        }

    }

    /**
     * Sets the proper image to a layout ImageView in accordance of the id of a view
     * and its checked status.
     *
     * @param viewID    the id of a view.
     * @param isChecked the checked status of a view with the previous id,
     */
    private void setImage(int viewID, boolean isChecked) {
        switch (viewID) {
            case R.id.left_arm_checkbox:
                if (isChecked) {
                    mBodyLeftArm.setImageResource(R.drawable.body_left_arm);
                } else {
                    mBodyLeftArm.setImageResource(R.drawable.body_left_arm_none);
                }
                break;
            case R.id.right_arm_checkbox:
                if (isChecked) {
                    mBodyRightArm.setImageResource(R.drawable.body_right_arm);
                } else {
                    mBodyRightArm.setImageResource(R.drawable.body_right_arm_none);
                }
                break;
            case R.id.left_leg_checkbox:
                if (isChecked) {
                    mBodyLeftLeg.setImageResource(R.drawable.body_left_leg);
                } else {
                    mBodyLeftLeg.setImageResource(R.drawable.body_left_leg_none);
                }
                break;
            case R.id.right_leg_checkbox:
                if (isChecked) {
                    mBodyRightLeg.setImageResource(R.drawable.body_right_leg);
                } else {
                    mBodyRightLeg.setImageResource(R.drawable.body_right_leg_none);
                }
            default:
                break;
        }
    }

    /**
     * Gets and load bundles data of the current intent.
     *
     * @param currentIntent the current intend.
     */
    private void loadExtras(Intent currentIntent) {
        Bundle extras = currentIntent.getExtras();
        mWearableNodesIDs = extras.getStringArray(MobileWearConstants.WEARABLE_NODES_EXTRA);
        mNumberWearablesAvailables = mWearableNodesIDs.length;
    }

    /**
     * Returns a list with all the layout checkboxes.
     *
     * @return the list with all the layout checkboxes.
     */
    private ArrayList<CheckBox> getCheckBoxes() {
        ArrayList<CheckBox> checkBoxes = new ArrayList<CheckBox>();
        checkBoxes.add((CheckBox) this.findViewById(R.id.left_arm_checkbox));
        checkBoxes.add((CheckBox) this.findViewById(R.id.right_arm_checkbox));
        checkBoxes.add((CheckBox) this.findViewById(R.id.left_leg_checkbox));
        checkBoxes.add((CheckBox) this.findViewById(R.id.right_leg_checkbox));
        return checkBoxes;
    }

    /**
     * Returns a checkbox list that have the checked attribute asked.
     *
     * @param checkStatus the checked status asked for.
     * @return the list of checkboxes that have the corresponding checked attribute.
     */
    private ArrayList<CheckBox> getCheckBoxesByStatus(boolean checkStatus) {
        ArrayList<CheckBox> checkBoxes, result;
        result = new ArrayList<>();
        checkBoxes = getCheckBoxes();
        for (CheckBox item : checkBoxes) {
            if (item.isChecked() == checkStatus) {
                result.add(item);
            }
        }
        return result;

    }

    /**
     * Sets the enabled attribute of the checkbox.
     *
     * @param checkBoxes    the checkbox to update the enabled attribute.
     * @param enabledStatus the status to be set to the checkbox.
     */
    private void setEnabledStatusToCheckBoxes(ArrayList<CheckBox> checkBoxes, boolean enabledStatus) {
        for (CheckBox item : checkBoxes) {
            item.setEnabled(enabledStatus);
        }
    }

    /**
     * Starts the thread that returns the map with every sensorID and its body part.
     *
     * @param command the path that identifies the message.
     * @return the map with every sensorID and its body part; otherwise null.
     */
    private Map<String, String> startArrangeSensorsUsingMessagesThread(final String command) {
        Map<String, String> result = new HashMap<String, String>();
        Future<Map<String, String>> threadResult;
        Callable<Map<String, String>> callable;
        ExecutorService es = Executors.newSingleThreadExecutor();
        callable = new Callable<Map<String, String>>() {
            @Override
            public Map<String, String> call() {
                return arrangeSensorsUsingMessages(command);
            }
        };
        threadResult = es.submit(callable);
        es.shutdown();
        try {
            result = threadResult.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Returns the map with every sensorID and its body part.
     *
     * @param command the path that identifies the message.
     * @return the map with every sensorID and its body part; otherwise null.
     */
    private Map<String, String> arrangeSensorsUsingMessages(String command) {
        String message;
        Map<String, String> result;
        ArrayList<CheckBox> checkBoxesWithTrueStatus = getCheckBoxesByStatus(true);
        result = new HashMap<String, String>();
        if (mGoogleApiClient.isConnected()) {
            for (int i = 0; i < checkBoxesWithTrueStatus.size(); i++) {
                message = getBodyPart(checkBoxesWithTrueStatus.get(i));
                if (Wearable.MessageApi.sendMessage(mGoogleApiClient, mWearableNodesIDs[i],
                        command, message.getBytes()).await().getStatus().isSuccess()) {
                    result.put(mWearableNodesIDs[i], message);
                } else {
                    result = null;
                    break;
                }
            }
        } else {
            result = null;
        }
        return result;
    }

    /**
     * Returns the body part represented by the check box.
     *
     * @param checkBox the checkbox passed to get its body part.
     * @return the body part string of the check box.
     */
    private String getBodyPart(CheckBox checkBox) {
        String result = "";
        switch (checkBox.getId()) {
            case R.id.left_arm_checkbox:
                result = MobileWearConstants.LARM_MESSAGE;
                break;
            case R.id.right_arm_checkbox:
                result = MobileWearConstants.RARM_MESSAGE;
                break;
            case R.id.left_leg_checkbox:
                result = MobileWearConstants.LLEG_MESSAGE;
                break;
            case R.id.right_leg_checkbox:
                result = MobileWearConstants.RLEG_MESSAGE;
            default:
                break;
        }
        return result;
    }
}