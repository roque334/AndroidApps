package com.example.roquecontreras.moreno;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.RemoteInput;
import android.util.Log;

import com.example.roquecontreras.moreno.MainActivity;

/**
 * Created by Roque Contreras on 30/09/2015.
 */
public class DemandIntentReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

//        if (intent.getAction().equals(MainActivity.ACTION_DEMAND)) {
//            String message = intent.getStringExtra(MainActivity.EXTRA_MESSAGE);
//            Log.v("MyTag", "Extra message from intent = " + message);
//            Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
//            CharSequence reply = remoteInput.getCharSequence(MainActivity.EXTRA_VOICE_REPLY);
//            Log.v("MyTag", "User reply from wearable: " + reply);
//        }
    }
}
