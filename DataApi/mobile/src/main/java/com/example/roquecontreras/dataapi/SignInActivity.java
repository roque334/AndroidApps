package com.example.roquecontreras.dataapi;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.IntentCompat;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.example.roquecontreras.common.WebServerConstants;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;

import org.json.JSONObject;

import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;

public class SignInActivity extends FragmentActivity implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        View.OnClickListener {

    private GoogleApiClient mGoogleApiClient;
    private static final int RC_SIGN_IN = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SignInButton signInButton;
        GoogleSignInOptions gso;
        setContentView(R.layout.activity_sign_in);

        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestIdToken(WebServerConstants.WEB_SERVER_CLIENT_ID)
                .build();

        // Build a GoogleApiClient with access to the Google Sign-In API and the
        // options specified by gso.
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        signInButton = (SignInButton) findViewById(R.id.sign_in_button);
        signInButton.setSize(SignInButton.SIZE_STANDARD);
        setGooglePlusButtonText(signInButton, "Google");
        signInButton.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sign_in_button:
                signIn();
                break;
        }
    }

    private void signIn() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        GoogleSignInResult result;
        if (requestCode == RC_SIGN_IN) {
            result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(result);
        }
    }

    private void handleSignInResult(GoogleSignInResult result) {
        GoogleSignInAccount acct;
        String idToken;
        FileOutputStream fos = null;
        if (result.isSuccess()) {
            // Signed in successfully, show authenticated UI.
            acct = result.getSignInAccount();
            idToken = acct.getIdToken();
            sendTokenToServerToValidate(idToken);
        } else {
            // Signed out, show unauthenticated UI.
            if (isInternetConnectionOn()) {
                Toast.makeText(SignInActivity.this, SignInActivity.this.getString(R.string.impossible_sing_in_1), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(SignInActivity.this, SignInActivity.this.getString(R.string.impossible_sing_in_2), Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Sends the idtoken, given by the google authenticator, to the app server.
     *
     * @param idToken the token given by the google authenticator
     */
    private void sendTokenToServerToValidate(final String idToken) {
        JsonObjectRequest jsonObjectRequest;
        HashMap<String, String> params = new HashMap<String, String>();
        params.put(WebServerConstants.ID_TOKEN_LABEL, idToken);

        //Uncomment the following line if you want to validate the user in the proprietary server
        //jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, WebServerConstants.LOCAL_WEB_SERVER_VALIDATE_TOKEN_URL, new JSONObject(params)
        //Uncomment the following line if you want to validate the user in the heroku proprietary server
        jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, WebServerConstants.HEROKU_WEB_SERVER_VALIDATE_TOKEN_URL, new JSONObject(params)
                , new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | IntentCompat.FLAG_ACTIVITY_CLEAR_TASK);
                intent.putExtra(WebServerConstants.ID_TOKEN_LABEL, idToken);
                SignInActivity.this.getApplicationContext().startActivity(intent);
            }
        }, new Response.ErrorListener() {
            //Create an error listener to handle errors appropriately.
            @Override
            public void onErrorResponse(VolleyError error) {
                //This code is executed if there is an error.
            }
        }) {

            @Override
            public Map<String, String> getHeaders() {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put(WebServerConstants.ID_TOKEN_LABEL, idToken);
                return headers;
            }
        };
        SingletonRequestQueue.getInstance(this).addToRequestQueue(jsonObjectRequest);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    public void onConnected(Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    /**
     * Sets the text in the google plus button.
     *
     * @param signInButton the google plus button.
     * @param buttonText   the text to be set.
     */
    protected void setGooglePlusButtonText(SignInButton signInButton,
                                           String buttonText) {
        View v;
        TextView tv;
        for (int i = 0; i < signInButton.getChildCount(); i++) {
            v = signInButton.getChildAt(i);

            if (v instanceof TextView) {
                tv = (TextView) v;
                tv.setTextSize(15);
                tv.setTypeface(null, Typeface.NORMAL);
                tv.setText(buttonText);
                return;
            }
        }
    }

    /**
     * Checks whether the device is connected to internet or not.
     *
     * @return true if the device is connected to internet;
     * false if the device is not connected to internet.
     */
    public boolean isInternetConnectionOn() {
        ConnectivityManager connectivityManager;
        NetworkInfo activeNetworkInfo;
        boolean result = true;
        connectivityManager = (ConnectivityManager) SignInActivity.this.getSystemService(Context.CONNECTIVITY_SERVICE);
        activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        if (activeNetworkInfo != null && activeNetworkInfo.isConnected()) {
            // connected to the internet
            if (activeNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                // connected to wifi
            } else if (activeNetworkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                // connected to the mobile provider's data plan
            }
        } else { // not connected to the internet
            result = false;
        }
        return result;
    }
}