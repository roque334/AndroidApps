package com.example.roquecontreras.dataapi;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Switch;

import com.example.roquecontreras.common.MobileWearConstants;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class MainActivity extends Activity {

    private static final String LOG_TAG = "PhoneActivity";
    Switch mArrangeSwitch, mSesingSwitch;
    String[] mDevicesID;

    NetworkChangeReceiver mNetworkChangeReceiver;

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
        Wearable.CapabilityApi.addCapabilityListener(mGoogleApiClient, mCapabilityListener, MobileWearConstants.DATA_ANALYSIS_CAPABILITY);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        initializeGoogleApiClient();
        ImageButton arrangeButton = (ImageButton) findViewById(R.id.arrangement_button);
        mArrangeSwitch = (Switch) findViewById(R.id.arrangement_status);
        ImageButton startButton = (ImageButton) findViewById(R.id.sesing_imageview);
        mSesingSwitch = (Switch) findViewById(R.id.sensing_status);
        ImageButton preferencesButton = (ImageButton) findViewById(R.id.preferences_imageview);

        arrangeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(LOG_TAG, "ArrangeButton_onClick");
                mDevicesID = getWearablesNodeIDsThread();
                if (mDevicesID != null) {
                    Intent intent = new Intent(getApplicationContext(), ArrangeSensorsActivity.class);
                    intent.putExtra(MobileWearConstants.WEARABLE_NODES_EXTRA, mDevicesID);
                    startActivityForResult(intent, MobileWearConstants.RESULT_CODE_ARRANGEMENT);
                } else {
                    Log.d(LOG_TAG, "ArrangeButton_onClick: mDevicesID == null");
                }
            }
        });

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mSesingSwitch.isChecked()) {
                    mSesingSwitch.setChecked(startSendNotificationUsingDataItemThread(MobileWearConstants.START_ACCLEROMETER_BY_DATAITEM_PATH));
                } else {
                    mSesingSwitch.setChecked(!startSendNotificationUsingDataItemThread(MobileWearConstants.STOP_ACCLEROMETER_BY_DATAITEM_PATH));
                }
            }
        });

        preferencesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
                startActivity(intent);

            }
        });
    }

    /**
     * Starts the thread that sends the notification to the cloud node to start/stop
     * the accelerometer service.
     *
     * @param command the dataitem path.
     * @return True if the dataitem was send correctly to the cloud node; otherwise false.
     */
    private boolean startSendNotificationUsingDataItemThread(final String command) {
        boolean result = false;
        Future<Boolean> threadResult;
        Callable<Boolean> callable;
        ExecutorService es = Executors.newSingleThreadExecutor();
        callable = new Callable<Boolean>() {
            @Override
            public Boolean call() {
                return sendNotificationUsingDataItem(command);
            }
        };
        threadResult = es.submit(callable);
        es.shutdown();
        try {
            result = threadResult.get().booleanValue();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return result;
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
     * Sends a dataitem to the cloud node to stats/stops the accelerometer services of the sensors.
     *
     * @param command the dataitem path.
     * @return True if the dataitem was send correctly to the cloud node; otherwise false.
     */
    private boolean sendNotificationUsingDataItem(String command) {
        Resources res;
        boolean result = false;
        res = getResources();
        if (mGoogleApiClient.isConnected()) {
            PutDataMapRequest dataMapRequest = PutDataMapRequest.create(command);
            dataMapRequest.getDataMap().putDouble(MobileWearConstants.NOTIFICATION_TIMESTAMP, System.currentTimeMillis());
            dataMapRequest.getDataMap().putLong(MobileWearConstants.KEY_MEASUREMENTS_SAMPLE_INTERVAL
                    , new Long(getSharedPreferences().getInt(MobileWearConstants.KEY_MEASUREMENTS_SAMPLE_INTERVAL, res.getInteger(R.integer.measurement_sample_defaultValue))));
            dataMapRequest.getDataMap().putLong(MobileWearConstants.KEY_HANDHELD_WEAR_SYNC_INTERVAL
                    , new Long(getSharedPreferences().getInt(MobileWearConstants.KEY_HANDHELD_WEAR_SYNC_INTERVAL, res.getInteger(R.integer.sync_interval_defaultValue))));
            PutDataRequest putDataRequest = dataMapRequest.asPutDataRequest();
            result = Wearable.DataApi.putDataItem(mGoogleApiClient, putDataRequest).await().getStatus().isSuccess();
        } else {
            Log.e(LOG_TAG, "No connection to wearable available!");
        }
        return result;
    }

    private SharedPreferences getSharedPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(this);
    }

    /**
     * Starts the thread that Returns the sensors id currently connected to
     * the Wireless Body Area Network (WBAN).
     *
     * @return the array of sensors id currently connected to the WBAN; otherwise null.
     */
    private String[] getWearablesNodeIDsThread() {
        String[] result = null;
        Future<String[]> threadResult;
        Callable<String[]> callable;
        ExecutorService es = Executors.newSingleThreadExecutor();
        callable = new Callable<String[]>() {
            @Override
            public String[] call() {
                return getWearablesNodeIDs();
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
     * Returns the sensors id currently connected to the Wireless Body Area Network (WBAN).
     *
     * @return the array of sensors id currently connected to the WBAN
     */
    private String[] getWearablesNodeIDs() {
        Log.d(LOG_TAG, "getWearablesNodeIDs");
        String[] result = null;
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            Log.d(LOG_TAG, "getWearablesNodeIDs_mGoogleApiClientConnected");

            if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
                CapabilityApi.GetCapabilityResult capabilityResult = Wearable.CapabilityApi
                        .getCapability(mGoogleApiClient, MobileWearConstants.TREMOR_QUANTIFICATION_CAPABILITY
                                , CapabilityApi.FILTER_REACHABLE).await();
                if (capabilityResult.getStatus().isSuccess()) {
                    if (capabilityResult.getCapability().getNodes().size() > 0) {
                        result = setNodeToArrayString(capabilityResult.getCapability().getNodes());
                    }
                } else {
                    Log.d(LOG_TAG, "getWearablesNodeIDs_capabilityResult: " + capabilityResult.getStatus().getStatusMessage());
                }
            } else {
                Log.d(LOG_TAG, "getWearablesNodeIDs: GoogleClientApi disconnected");
            }
        }
        return result;
    }

    /**
     * Starts the thread that sends a message to every sensor node connected to the WBAN
     * to stats/stops their accelerometer services.
     *
     * @param command the message path.
     * @param message the message to be sent to stats/stops the accelerometer services.
     * @return True if the message went out of the sender device.
     */
    private boolean startSendNotificationUsingMessagesThread(final String command, final String message) {
        boolean result = false;
        Future<Boolean> threadResult;
        Callable<Boolean> callable;
        ExecutorService es = Executors.newSingleThreadExecutor();
        callable = new Callable<Boolean>() {
            @Override
            public Boolean call() {
                return sendNotificationUsingMessages(command, message);
            }
        };
        threadResult = es.submit(callable);
        es.shutdown();
        try {
            result = threadResult.get().booleanValue();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Sends a message to every sensor node connected to the WBAN to stats/stops their
     * accelerometer services.
     *
     * @param command the message path.
     * @param message the message to be sent to stats/stops the accelerometer services.
     * @return True if the message went out of the sender device.
     */
    private boolean sendNotificationUsingMessages(String command, String message) {
        String[] wearableNodes;
        boolean result = true;
        if (mGoogleApiClient.isConnected()) {
            wearableNodes = getWearablesNodeIDs();
            if (wearableNodes != null) {
                for (String nodeID : wearableNodes) {
                    result = result && Wearable.MessageApi.sendMessage(mGoogleApiClient, nodeID,
                            command, message.getBytes()).await().getStatus().isSuccess();
                }
            } else {
                Log.d(LOG_TAG, "sendNotificationUsingMessages: WearableNodes == null");
            }
        } else {
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

    private String[] setNodeToArrayString(Set<Node> wearableNodes) {
        int i;
        String[] result = new String[wearableNodes.size()];
        i = 0;
        for(Node node:  wearableNodes){
            result[i] = node.getId();
            i++;
        }
        return result;
    }

    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        if (requestCode == MobileWearConstants.RESULT_CODE_ARRANGEMENT) {
            if (resultCode == RESULT_OK) {
                mArrangeSwitch.setChecked(true);
            } else {
                mArrangeSwitch.setChecked(false);
            }
        }
    }

}
