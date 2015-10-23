package com.example.roquecontreras.shared;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Created by Roque Contreras on 14/10/2015.
 */
public class NodeManager extends WearableListenerService implements
        CapabilityApi.CapabilityListener{


    private GoogleApiClient mGoogleApiClient;
    private String CAPABILITY;
    private String LOG_TAG;
    private boolean result;

    public NodeManager(final MessageApi.MessageListener messageListener, Context context, String capabilityLabel, String tagLabel) {
        LOG_TAG = tagLabel;
        CAPABILITY = capabilityLabel;
        mGoogleApiClient = new GoogleApiClient.Builder(context)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle connectionHint) {
                        Log.d(LOG_TAG, "onConnected: " + connectionHint);
                        Wearable.MessageApi.addListener(mGoogleApiClient, messageListener);
                    }

                    @Override
                    public void onConnectionSuspended(int cause) {
                        Log.d(LOG_TAG, "onConnectionSuspended: " + cause);
                        Wearable.MessageApi.removeListener(mGoogleApiClient, messageListener);
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
        Wearable.CapabilityApi.addCapabilityListener(mGoogleApiClient, this, CAPABILITY);
    }

    public void Connect(){
        mGoogleApiClient.connect();
        result = mGoogleApiClient.isConnected();
    }

    public void Disconnect(){
        Wearable.CapabilityApi.removeCapabilityListener(mGoogleApiClient, this, CAPABILITY);
        mGoogleApiClient.disconnect();
        result = mGoogleApiClient.isConnected();
    }

    public void SendMessageTo(final String capability, final String path, final String message){
        result = true;
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
                        CapabilityApi.GetCapabilityResult capabilityResult = Wearable.CapabilityApi
                                .getCapability(mGoogleApiClient, capability
                                        , CapabilityApi.FILTER_REACHABLE).await();
                        if (capabilityResult.getCapability().getNodes().size() > 0) {
                            for (Node node : capabilityResult.getCapability().getNodes()) {
                                Wearable.MessageApi.sendMessage(mGoogleApiClient, node.getId(),
                                        path, message.getBytes()).await();
                            }
                        } else {
                            Log.d(LOG_TAG, "SendMessageTo: Nodes not found");
                            result = false;
                        }
                    } else {
                        Log.d(LOG_TAG, "SendMessageTo: GoogleClientApi disconnected");
                        result = false;
                    }
                } catch (Exception e) {
                    Log.d(LOG_TAG, e.getMessage());
                }
            }
        });
        thread.start();
    }

    @Override
    public void onCapabilityChanged(CapabilityInfo capabilityInfo) {
        Log.d(LOG_TAG, "onCapabilityChanged: " + capabilityInfo.getName());
    }
}

