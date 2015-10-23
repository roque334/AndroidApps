package com.example.roquecontreras.moreno;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.example.roquecontreras.shared.Capability;
import com.example.roquecontreras.shared.Path;
import com.example.roquecontreras.shared.NodeManager;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;

public class MainActivity extends AppCompatActivity {

    private NodeManager mNodeManager;
    private static final String LOG_TAG = "MobileActivity";
    private Button mSendMessageButton;
    private EditText mSendMessageInput;

    private MessageApi.MessageListener mMessageListener = new MessageApi.MessageListener() {
        @Override
        public void onMessageReceived(final MessageEvent messageEvent) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (messageEvent.getPath().equalsIgnoreCase(Path.TEXT_MESSAGE)) {
                        mSendMessageInput.setText(new String(messageEvent.getData()));
                    }
                }
            });
        }
    };

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mNodeManager = new NodeManager(mMessageListener, this, Capability.DATA_ANALYSIS, LOG_TAG);
        mSendMessageButton = (Button) findViewById(R.id.send_message_button);
        mSendMessageInput = (EditText) findViewById(R.id.send_message_input);

        mSendMessageInput.setHint(R.string.send_message_text);

        mSendMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mNodeManager.SendMessageTo(Capability.TREMOR_QUANTIFICATION, Path.TEXT_MESSAGE, mSendMessageInput.getText().toString());
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        mNodeManager.Connect();
    }

    @Override
    protected void onResume() {
        super.onResume();
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
