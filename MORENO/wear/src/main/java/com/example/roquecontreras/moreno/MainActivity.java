package com.example.roquecontreras.moreno;

import android.app.Activity;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.widget.TextView;

import com.example.roquecontreras.shared.Capability;
import com.example.roquecontreras.shared.Path;
import com.example.roquecontreras.shared.NodeManager;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;

public class MainActivity extends Activity {

    private TextView mTextView;
    private NodeManager mNodeManager;
    private final String LOG_TAG = "WearActivity";

    public static final String TREMOR_QUANTIFICATION = "tremor_quantification";

    MessageApi.MessageListener mMessageListener = new MessageApi.MessageListener() {
        @Override
        public void onMessageReceived(final MessageEvent messageEvent) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        mNodeManager.SendMessageTo(Capability.DATA_ANALYSIS, Path.TEXT_MESSAGE, "Recibido");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (messageEvent.getPath().equalsIgnoreCase(Path.TEXT_MESSAGE)) {
                                    mTextView.setText(new String(messageEvent.getData()));
                                } else if (messageEvent.getPath().equalsIgnoreCase(Path.START_ACCELEROMETER)) {

                                } else if (messageEvent.getPath().equalsIgnoreCase(Path.STOP_ACCELEROMETER)) {

                                }
                            }
                        });
                    }catch (Exception e) {
                        Log.d(LOG_TAG, e.getMessage());
                    }
                }
            });
            thread.start();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mTextView = (TextView) stub.findViewById(R.id.received_message_input);
            }
        });
        mNodeManager = new NodeManager(mMessageListener, this, Capability.TREMOR_QUANTIFICATION, LOG_TAG);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mNodeManager.Connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mNodeManager.Disconnect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
