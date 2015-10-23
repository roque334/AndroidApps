package com.example.roquecontreras.moreno2;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

public class MainActivity extends Activity {

    private final String LOG_TAG = "MobileActivity";
    private Button openButton;
    private GoogleApiClient mGoogleApiClient;
    public static final String DATA_ANALYSIS = "data_analysis";

    private void iniGoogleApiClient() {
        Log.d(LOG_TAG, "iniGoogleApiClient");
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            Log.d(LOG_TAG, "Connected");
        } else {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                        @Override
                        public void onConnected(Bundle bundle) {
                            Log.d(LOG_TAG, "onConnected: " + bundle);
                        }

                        @Override
                        public void onConnectionSuspended(int cause) {
                            Log.d(LOG_TAG, "onConnectionSuspended: " + cause);
                        }
                    })
                    .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                        @Override
                        public void onConnectionFailed(ConnectionResult connectionResult) {
                            Log.d(LOG_TAG, "onConnectionFailed: " + connectionResult.getErrorMessage());
                        }
                    })
                    .addApi(Wearable.API)
                    .build();
            Wearable.CapabilityApi.addCapabilityListener(mGoogleApiClient, new CapabilityApi.CapabilityListener() {
                @Override
                public void onCapabilityChanged(CapabilityInfo capabilityInfo) {
                    Log.d(LOG_TAG, "onCapabilityChanged: " + capabilityInfo);
                }
            }, DATA_ANALYSIS);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(LOG_TAG, "onCreate");
        iniGoogleApiClient();
        openButton = (Button) findViewById(R.id.open_button);
        openButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PutDataMapRequest dataMapRequest = PutDataMapRequest.create(Constant.OPEN_APP);
                PutDataRequest putDataRequest = dataMapRequest.asPutDataRequest();
                Wearable.DataApi.putDataItem(mGoogleApiClient, putDataRequest);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(LOG_TAG, "onStart");
        mGoogleApiClient.connect();

    }

    @Override
    protected void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
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
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
