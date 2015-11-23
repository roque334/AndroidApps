package com.example.roquecontreras.dataapi;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.example.roquecontreras.common.Constants;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.text.DateFormat;
import java.util.Date;

public class MainActivity extends Activity {

    private static final String LOG_TAG = "PhoneActivity";
    private GoogleApiClient mGoogleApiClient;
    private CapabilityApi.CapabilityListener mCapabilityListener = new CapabilityApi.CapabilityListener() {
        @Override
        public void onCapabilityChanged(CapabilityInfo capabilityInfo) {
            Log.d(LOG_TAG, "onCapabilityChanged: " + capabilityInfo.getName());
        }
    };

    /**
     * Builds the GoogleApiClient with the Wearable API and a capabilityListener.
     */
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
        Wearable.CapabilityApi.addCapabilityListener(mGoogleApiClient, mCapabilityListener, Constants.DATA_ANALYSIS_CAPABILITY);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        initializeGoogleApiClient();
        Button arrangeButton = (Button) findViewById(R.id.arrange_wear_over_limbs_button);
        Button startButton = (Button) findViewById(R.id.start_accelerometer_service_button);
        Button stopButton = (Button) findViewById(R.id.stop_accelerometer_service_button);

        arrangeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String[] wearableNodesIDs = getWearablesNodeIDs();
                Intent intent = new Intent(getApplicationContext(), ArrangeSensorsActivity.class);
                intent.putExtra(Constants.WEARABLE_NODES_EXTRA,wearableNodesIDs);
                startActivity(intent);
            }
        });

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        sendNotificationUsingDataItem(Constants.START_ACCLEROMETER_BY_DATAITEM_PATH);
                    }
                });
                thread.start();
            }
        });
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        sendNotificationUsingDataItem(Constants.STOP_ACCLEROMETER_BY_DATAITEM_PATH);
                    }
                });
                thread.start();

            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
    }

    /**
     * Sends a dataitem to the cloud node to stats/stops the accelerometer services of the sensors.
     * @param command the dataitem path.
     * @return True if the dataitem was send correctly to the cloud node; otherwise false.
     */
    private boolean sendNotificationUsingDataItem(String command) {
        Resources res;
        boolean result = false;
        res = getResources();
        if (mGoogleApiClient.isConnected()) {
            PutDataMapRequest dataMapRequest = PutDataMapRequest.create(command);
            dataMapRequest.getDataMap().putDouble(Constants.NOTIFICATION_TIMESTAMP, System.currentTimeMillis());
            dataMapRequest.getDataMap().putLong(Constants.KEY_MEASUREMENTS_SAMPLE_INTERVAL
                    , new Long(getSharedPreferences().getInt(Constants.KEY_MEASUREMENTS_SAMPLE_INTERVAL, res.getInteger(R.integer.measurement_sample_defaultValue))));
            dataMapRequest.getDataMap().putLong(Constants.KEY_HANDHELD_WEAR_SYNC_INTERVAL
                    , new Long(getSharedPreferences().getInt(Constants.KEY_HANDHELD_WEAR_SYNC_INTERVAL, res.getInteger(R.integer.sync_interval_defaultValue))));
            PutDataRequest putDataRequest = dataMapRequest.asPutDataRequest();
            result = Wearable.DataApi.putDataItem(mGoogleApiClient, putDataRequest).await().getStatus().isSuccess();
        }
        else {
            Log.e(LOG_TAG, "No connection to wearable available!");
        }
        return result;
    }

    private SharedPreferences getSharedPreferences(){
        return PreferenceManager.getDefaultSharedPreferences(this);
    }

    /**
     * Returns the sensors id currently connected to the Wireless Body Area Network (WBAN).
     * @return the list of sensors id currently connected to the WBAN
     */
    private String[] getWearablesNodeIDs(){
        String[] result = null;
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            CapabilityApi.GetCapabilityResult capabilityResult = Wearable.CapabilityApi
                    .getCapability(mGoogleApiClient, Constants.DATA_ANALYSIS_CAPABILITY
                            , CapabilityApi.FILTER_REACHABLE).await();
            if (capabilityResult.getCapability().getNodes().size() > 0) {
                result = (String[]) capabilityResult.getCapability().getNodes().toArray();
            }
        }
        return result;
    }

    /**
     * Sends a message to every sensor node connected to the WBAN to stats/stops their
     * accelerometer services.
     * @param command the message path.
     * @param message the message to be sent to stats/stops the accelerometer services.
     * @return True if the message went out of the sender device.
     */
    private boolean sendNotificationUsingMessages(String command, String message) {
        String[] WearableNodes;
        boolean result = true;
        if (mGoogleApiClient.isConnected()) {
            WearableNodes = getWearablesNodeIDs();
            for(String nodeID : WearableNodes) {
                result = result && Wearable.MessageApi.sendMessage(mGoogleApiClient, nodeID,
                        command, message.getBytes()).await().getStatus().isSuccess();
            }
        }
        else {
            result = false;
            Log.e(LOG_TAG, "No connection to wearable available!");
        }
        return result;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
