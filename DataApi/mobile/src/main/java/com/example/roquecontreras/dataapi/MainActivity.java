package com.example.roquecontreras.dataapi;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.Toast;

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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class MainActivity extends Activity {

    private static final String LOG_TAG = "PhoneActivity";
    private Map<String, String> mArrange;

    private ImageButton mArrangeButton, mStartButton;
    private Switch mArrangeSwitch, mSensingSwitch;

    NetworkChangeReceiver mNetworkChangeReceiver;

    private GoogleApiClient mGoogleApiClient;
    private String mIdToken;

    private CapabilityApi.CapabilityListener mCapabilityListener = new CapabilityApi.CapabilityListener() {
        @Override
        public void onCapabilityChanged(CapabilityInfo capabilityInfo) {
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
        Wearable.CapabilityApi.addCapabilityListener(mGoogleApiClient, mCapabilityListener, MobileWearConstants.DATA_ANALYSIS_CAPABILITY);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final ImageButton arrangeButton, uploadDataButton, preferencesButton;
        setContentView(R.layout.activity_main);
        loadSavedInstanceState(savedInstanceState);

        initializeGoogleApiClient();
        mArrangeButton = (ImageButton) findViewById(R.id.arrangement_button);
        mArrangeSwitch = (Switch) findViewById(R.id.arrangement_status);
        mStartButton = (ImageButton) findViewById(R.id.sesing_imageview);
        mStartButton.setEnabled(false);
        mSensingSwitch = (Switch) findViewById(R.id.sensing_status);
        uploadDataButton = (ImageButton) findViewById(R.id.upload_data_imageview);
        uploadDataButton.setEnabled(false);
        preferencesButton = (ImageButton) findViewById(R.id.preferences_imageview);

        mArrangeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                arrangeButtonOnClick();
            }
        });

        mStartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startButtonOnClick();
            }
        });

        uploadDataButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadButtonOnClick();
            }
        });

        preferencesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                preferencesButtonOnClick();
            }
        });
    }

    /**
     * Loads the values contained on the Bundle instance.
     *
     * @param savedInstanceState the Bundle instance.
     */
    private void loadSavedInstanceState(Bundle savedInstanceState) {
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if (extras == null) {
                this.mIdToken = null;
            } else {
                this.mIdToken = extras.getString(WebServerConstants.ID_TOKEN_LABEL);
            }
        } else {
            this.mIdToken = (String) savedInstanceState.getSerializable(WebServerConstants.ID_TOKEN_LABEL);
        }
    }

    /**
     * Executes the actions of the arrangeButton onClick.
     */
    private void arrangeButtonOnClick() {
        final String[] devicesID;
        devicesID = getWearablesNodeIDsThread();
        if (devicesID != null) {
            if (mArrange != null) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setMessage(R.string.arrange_dialog_message)
                        .setTitle(R.string.arrange_dialog_title);
                builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        deleteFile(MobileWearConstants.ARRANGEMENT_FILENAME);
                        startArrangeSensorActivity(devicesID);
                    }
                });
                builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
            } else {
                startArrangeSensorActivity(devicesID);
            }
        } else {
            Toast.makeText(getApplicationContext(), MainActivity.this.getString(R.string.devices_not_connected), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Starts the activity of the arrangement of the wear nodes over the patient's body.
     *
     * @param devicesID the wear IDs currently connected.
     */
    private void startArrangeSensorActivity(String[] devicesID) {
        Intent intent = new Intent(getApplicationContext(), ArrangeSensorsActivity.class);
        intent.putExtra(MobileWearConstants.WEARABLE_NODES_EXTRA, devicesID);
        startActivityForResult(intent, MobileWearConstants.RESULT_CODE_ARRANGEMENT);
    }

    /**
     * Executes the actions of the startButton onClick.
     */
    private void startButtonOnClick() {
        if (mArrangeSwitch.isChecked()) {
            startDialog();
        } else {
            Toast.makeText(getApplicationContext(), MainActivity.this.getString(R.string.devices_not_set), Toast.LENGTH_SHORT).show();
            ;
        }
    }

    /**
     * Shows the dialog that corresponds to the current sensing state.
     */
    private void startDialog() {
        AlertDialog.Builder builder;
        final SharedPreferences.Editor editor;
        builder = new AlertDialog.Builder(MainActivity.this);
        editor = getSharedPreferences().edit();
        if (!getSharedPreferences().getBoolean("isSensing", false)) {
            builder.setMessage(R.string.start_sensing_dialog_message)
                    .setTitle(R.string.start_sensing_dialog_title);
            builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    mSensingSwitch.setChecked(startSendNotificationUsingMessagesThread(MobileWearConstants.START_ACCELEROMETER_BY_MESSAGE_PATH, "start"));
                    editor.putBoolean("isSensing", true);
                    editor.commit();
                    mArrangeButton.setEnabled(false);
                }
            });
            builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {

                }
            });
        } else {
            builder.setMessage(R.string.stop_sensing_dialog_message)
                    .setTitle(R.string.stop_sensing_dialog_title);
            builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    mSensingSwitch.setChecked(!startSendNotificationUsingMessagesThread(MobileWearConstants.STOP_ACCELEROMETER_BY_MESSAGE_PATH, "stop"));
                    editor.putBoolean("isSensing", false);
                    editor.commit();
                    mArrangeButton.setEnabled(true);
                }
            });
            builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {

                }
            });
        }
        builder.create().show();
    }

    /**
     * Executes the actions of the uploadButton onClick.
     */
    private void uploadButtonOnClick() {
        //In case you want to upload the data to the server, uncomment the
        //following lines and install the server application on a proprietary server.
        /*
        String stringPattern;
        Pattern pattern;
        Matcher matcher;
        File filesDir;
        File[] directoryListing;
        stringPattern = "^" + MobileWearConstants.MEASUREMENT_FILENAME_START + "(.*)";
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
        */
    }

    /**
     * Executes the actions of the preferencesButton onClick.
     */
    private void preferencesButtonOnClick() {
        Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
        startActivity(intent);
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
        readArrangementFile();
        if (mArrange != null) {
            mArrangeSwitch.setChecked(true);
            mStartButton.setEnabled(true);
        } else {
            mArrangeSwitch.setChecked(false);
            mStartButton.setEnabled(false);
        }
        if (!getSharedPreferences().getBoolean("isSensing", false)) {
            mArrangeButton.setEnabled(true);
            mSensingSwitch.setChecked(false);
        } else {
            mArrangeButton.setEnabled(false);
            mSensingSwitch.setChecked(true);
        }
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
        }
        return result;
    }

    /**
     * Gets the SharedPreferences of the application.
     *
     * @return the SharedPreferences of the application.
     */
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
        CapabilityApi.GetCapabilityResult capabilityResult;
        String[] result = null;
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            capabilityResult = Wearable.CapabilityApi
                    .getCapability(mGoogleApiClient, MobileWearConstants.TREMOR_QUANTIFICATION_CAPABILITY
                            , CapabilityApi.FILTER_REACHABLE).await();
            if (capabilityResult.getStatus().isSuccess()) {
                if (capabilityResult.getCapability().getNodes().size() > 0) {
                    result = setNodeToArrayString(capabilityResult.getCapability().getNodes());
                }
            }
        }
        if (result == null) {

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
     * Sends a message to every sensor node weared on the patient's body
     * to stats/stops their sensing services.
     *
     * @param command the message path.
     * @param message the message to be sent to stats/stops the accelerometer services.
     * @return True if the message went out of the sender device.
     */
    private boolean sendNotificationUsingMessages(String command, String message) {
        Iterator<String> wearableNodes;
        String nodeID;
        boolean result = true;
        if (mGoogleApiClient.isConnected()) {
            wearableNodes = mArrange.keySet().iterator();
            if (wearableNodes != null) {
                while (wearableNodes.hasNext()) {
                    nodeID = wearableNodes.next();
                    result = result && Wearable.MessageApi.sendMessage(mGoogleApiClient, nodeID,
                            command, message.getBytes()).await().getStatus().isSuccess();
                }
            }
        } else {
            result = false;
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
        switch (id) {
            case R.id.action_sync_wear:
                startSendNotificationUsingMessagesThread(MobileWearConstants.START_SYNC_WEAR_BY_MESSAGE_PATH, "start");
                return true;
            case R.id.action_del_wear:
                startSendNotificationUsingMessagesThread(MobileWearConstants.START_DEL_WEAR_BY_MESSAGE_PATH, "start");
                return true;
        }


        return super.onOptionsItemSelected(item);
    }

    /**
     * Parses a set of currently connected nodes into a string array of node's id.
     *
     * @param wearableNodes the set of currrently connected nodes.
     * @return the array of sensors id currently connected to the WBAN
     */
    private String[] setNodeToArrayString(Set<Node> wearableNodes) {
        int i;
        String[] result = new String[wearableNodes.size()];
        i = 0;
        for (Node node : wearableNodes) {
            result[i] = node.getId();
            i++;
        }
        return result;
    }

    /**
     * Deals with the result of an activity started for result.
     *
     * @param requestCode the code for a particular request
     * @param resultCode  the result code of the particular request
     * @param data        the data returned from the activity started for result
     */
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        if (requestCode == MobileWearConstants.RESULT_CODE_ARRANGEMENT) {
            if (resultCode != RESULT_OK) {
            }
        }
    }

    /**
     * Sends a file to the app server with a multipart request.
     *
     * @param file the file to send
     */
    private void sendTextFileToServer(File file) {
        final String idToken = this.mIdToken;
        //Uncomment the following line if you want to send the data to the proprietary server
        //MultipartFileRequest multipartFileRequest = new MultipartFileRequest(WebServerConstants.WEB_SERVER_UPLOAD_MEASURE_FILE_URL, file, getApplicationContext()
        //Uncomment the following line if you want to send the data to the heroku server
        MultipartFileRequest multipartFileRequest = new MultipartFileRequest(WebServerConstants.HEROKU_WEB_SERVER_UPLOAD_MEASURE_FILE_URL, file, getApplicationContext()
                , new Response.Listener<NetworkResponse>() {
            @Override
            public void onResponse(NetworkResponse response) {
                try {
                    JSONObject jsonObject = new JSONObject(new String(response.data));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
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

    /**
     * Returns the sensors weared over the patient's body.
     *
     * @return the Map with the arrangement key/values.
     */
    private void readArrangementFile() {
        InputStream inputStream;
        InputStreamReader inputStreamReader;
        BufferedReader bufferedReader;
        String[] keyValue;
        String receiveString;
        mArrange = new HashMap<String, String>();
        try {
            inputStream = openFileInput(MobileWearConstants.ARRANGEMENT_FILENAME);
            if (inputStream != null) {
                inputStreamReader = new InputStreamReader(inputStream);
                bufferedReader = new BufferedReader(inputStreamReader);
                while ((receiveString = bufferedReader.readLine()) != null) {
                    keyValue = receiveString.split(";");
                    mArrange.put(keyValue[0], keyValue[1]);
                }
                inputStream.close();
            }
        } catch (FileNotFoundException e) {
            mArrange = null;
        } catch (IOException e) {
            mArrange = null;
        }
    }
}