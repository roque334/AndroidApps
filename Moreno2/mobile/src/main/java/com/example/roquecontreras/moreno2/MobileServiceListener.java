package com.example.roquecontreras.moreno2;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

/**
 * Created by Roque Contreras on 15/10/2015.
 */
public class MobileServiceListener extends WearableListenerService implements
    GoogleApiClient.ConnectionCallbacks,
    GoogleApiClient.OnConnectionFailedListener,
    CapabilityApi.CapabilityListener{

        private final String LOG_TAG = "WearListenerService";
        private GoogleApiClient mGoogleApiClient;
        public static final String DATA_ANALYSIS = "data_analysis";

        @Override
        public void onConnected(Bundle bundle) {
            Log.d(LOG_TAG, "onConnected: " + bundle);
        }

        @Override
        public void onConnectionSuspended(int cause) {
            Log.d(LOG_TAG, "onConnectionSuspended: " + cause);
        }

        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {
            Log.d(LOG_TAG, "onConnectionFailed: " + connectionResult.getErrorMessage());
        }

        @Override
        public void onCreate() {
            super.onCreate();
            iniGoogleApiClient();
        }

    private void iniGoogleApiClient() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            Log.d(LOG_TAG, "Connected");
        }else{
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(Wearable.API)
                    .build();
            Wearable.CapabilityApi.addCapabilityListener(mGoogleApiClient, this, DATA_ANALYSIS);
        }
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        for(DataEvent dataEvent: dataEvents) {
            if (dataEvent.getType() == DataEvent.TYPE_CHANGED) {
                if (DATA_ANALYSIS.equals(dataEvent.getDataItem().getUri().getPath())) {
                    Intent intent = new Intent(this, MainActivity.class);
                    startActivity(intent);
                }
            }
        }
    }

}
