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

import com.android.volley.NetworkResponse;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.example.roquecontreras.common.MobileWearConstants;
import com.example.roquecontreras.common.WebServerConstants;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {

    private static final String LOG_TAG = "PhoneActivity";
    Switch mArrangeSwitch, mSensingSwitch;
    String[] mDevicesID;

    NetworkChangeReceiver mNetworkChangeReceiver;

    private GoogleApiClient mGoogleApiClient;
    private String mIdToken;

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
        ImageButton arrangeButton, startButton, uploadDataButton, preferencesButton;
        setContentView(R.layout.activity_main);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if(extras == null) {
                this.mIdToken = null;
            } else {
                this.mIdToken = extras.getString(WebServerConstants.ID_TOKEN_LABEL);
            }
        } else {
            this.mIdToken = (String) savedInstanceState.getSerializable(WebServerConstants.ID_TOKEN_LABEL);
        }

        initializeGoogleApiClient();
        arrangeButton = (ImageButton) findViewById(R.id.arrangement_button);
        mArrangeSwitch = (Switch) findViewById(R.id.arrangement_status);
        startButton = (ImageButton) findViewById(R.id.sesing_imageview);
        mSensingSwitch = (Switch) findViewById(R.id.sensing_status);
        uploadDataButton = (ImageButton) findViewById(R.id.upload_data_imageview);
        preferencesButton = (ImageButton) findViewById(R.id.preferences_imageview);

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
                if (!mSensingSwitch.isChecked()) {
                    mSensingSwitch.setChecked(startSendNotificationUsingDataItemThread(MobileWearConstants.START_ACCLEROMETER_BY_DATAITEM_PATH));
                } else {
                    mSensingSwitch.setChecked(!startSendNotificationUsingDataItemThread(MobileWearConstants.STOP_ACCLEROMETER_BY_DATAITEM_PATH));
                }
            }
        });

        uploadDataButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String stringPattern;
                Pattern pattern;
                Matcher matcher;
                File filesDir;
                File[] directoryListing;
                stringPattern = "^" + MobileWearConstants.ARRANGE_SENSORS_BY_MESSAGE_PATH + "(.*)";
                pattern = Pattern.compile(stringPattern);
                filesDir = getApplicationContext().getFilesDir();
                directoryListing = filesDir.listFiles();
                for (File child : directoryListing) {
                    if (!child.isDirectory()) {
                        matcher =  pattern.matcher(child.getName());
                        if (matcher.matches()){
                            sendTextFileToServer(child);
                        }
                    }
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
        IntentFilter filter;
        mNetworkChangeReceiver = new NetworkChangeReceiver();
        filter = new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");
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
        PutDataMapRequest dataMapRequest;
        PutDataRequest putDataRequest;
        Resources res;
        boolean result = false;
        res = getResources();
        if (mGoogleApiClient.isConnected()) {
            dataMapRequest = PutDataMapRequest.create(command);
            dataMapRequest.getDataMap().putDouble(MobileWearConstants.NOTIFICATION_TIMESTAMP, System.currentTimeMillis());
            dataMapRequest.getDataMap().putLong(MobileWearConstants.KEY_MEASUREMENTS_SAMPLE_INTERVAL
                    , new Long(getSharedPreferences().getInt(MobileWearConstants.KEY_MEASUREMENTS_SAMPLE_INTERVAL, res.getInteger(R.integer.measurement_sample_defaultValue))));
            dataMapRequest.getDataMap().putLong(MobileWearConstants.KEY_HANDHELD_WEAR_SYNC_INTERVAL
                    , new Long(getSharedPreferences().getInt(MobileWearConstants.KEY_HANDHELD_WEAR_SYNC_INTERVAL, res.getInteger(R.integer.sync_interval_defaultValue))));
            putDataRequest = dataMapRequest.asPutDataRequest();
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
        CapabilityApi.GetCapabilityResult capabilityResult;
        String[] result = null;
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            Log.d(LOG_TAG, "getWearablesNodeIDs_mGoogleApiClientConnected");

            if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
                capabilityResult = Wearable.CapabilityApi
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

    /**
     * Parses a set of currently connected nodes into a string array of node's id.
     * @param wearableNodes the set of currrently connected nodes.
     * @return the array of sensors id currently connected to the WBAN
     */
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

    /**
     * Deals with the result of an activity started for result.
     * @param requestCode the code for a particular request
     * @param resultCode the result code of the particular request
     * @param data the data returned from the activity started for result
     */
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

    /**
     * Sends a file to the app server with a multipart request.
     * @param file the file to send
     */
    private void sendTextFileToServer(File file) {
        final String idToken = this.mIdToken;
        MultipartFileRequest multipartFileRequest = new MultipartFileRequest(WebServerConstants.LOCAL_WEB_SERVER_UPLOAD_MEASURE_FILE_URL, file, getApplicationContext()
                , new Response.Listener<NetworkResponse>() {
            @Override
            public void onResponse(NetworkResponse response) {
                try {
                    JSONObject jsonObject = new JSONObject(new String(response.data));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                Log.d("Request", "Response: " + new String(response.data));
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("Request", "Error: " + error.toString());
            }
        }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = super.getHeaders();
                headers.put(WebServerConstants.ID_TOKEN_LABEL, idToken);
                return headers;
            }
        };

        SingletonRequestQueue.getInstance(this).addToRequestQueue(multipartFileRequest);
    }

}
