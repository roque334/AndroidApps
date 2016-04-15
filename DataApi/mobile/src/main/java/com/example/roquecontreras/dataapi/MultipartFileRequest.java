package com.example.roquecontreras.dataapi;

import android.content.Context;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by roquecontreras on 20/01/16.
 */
class MultipartFileRequest extends Request<NetworkResponse> {
    private final Response.Listener<NetworkResponse> mListener;
    private final Response.ErrorListener mErrorListener;
    private final Map<String, String> mHeaders;
    private final String mMimeType;
    private final byte[] mMultipartBody;
    private final Context mContext;
    private File mfile;

    public MultipartFileRequest(String url, File file, Context context, Response.Listener<NetworkResponse> listener, Response.ErrorListener errorListener) {
        super(Method.POST, url, errorListener);
        ByteArrayOutputStream bos;
        DataOutputStream dos;
        String mimeType;
        byte[] multipartBody;
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "text/plain");
        headers.put("Content-Disposition", "attachment;filename=" + file.getName());
        headers.put("Accept", "application/json");
        mimeType = "attachment;filename=" + file.getName();
        bos = new ByteArrayOutputStream();
        dos = new DataOutputStream(bos);

        Log.d("Request", "FileSize = " + file.length());
        buildTextPart(dos, readFromFile(file));
        multipartBody = bos.toByteArray();

        this.mfile = file;
        this.mContext = context;
        this.mListener = listener;
        this.mErrorListener = errorListener;
        this.mHeaders = headers;
        this.mMimeType = mimeType;
        this.mMultipartBody = multipartBody;
    }

    @Override
    public Map<String, String> getHeaders() {
        if (mHeaders != null) {
            return mHeaders;
        } else {
            try {
                return super.getHeaders();
            } catch (AuthFailureError authFailureError) {
                authFailureError.printStackTrace();
                return mHeaders;
            }
        }
    }

    @Override
    public String getBodyContentType() {
        return mMimeType;
    }

    @Override
    public byte[] getBody() {
        return mMultipartBody;
    }

    @Override
    protected Response<NetworkResponse> parseNetworkResponse(NetworkResponse response) {
        JSONObject jsonObject;
        try {
            deliverResponse(response);
            try {
                jsonObject = new JSONObject(new String(response.data));
                if (mfile.length() == jsonObject.getLong("file-size")) {
                    mContext.deleteFile(mfile.getName());
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return Response.success(
                    response,
                    HttpHeaderParser.parseCacheHeaders(response));
        } catch (Exception e) {
            deliverError(new ParseError(e));
            return Response.error(new ParseError(e));
        }
    }

    @Override
    protected void deliverResponse(NetworkResponse response) {
        mListener.onResponse(response);
    }

    @Override
    public void deliverError(VolleyError error) {
        mErrorListener.onErrorResponse(error);
    }

    private void buildTextPart(DataOutputStream dataOutputStream, StringBuilder parameterValue) {
        try {
            dataOutputStream.writeBytes(parameterValue.toString());
        } catch (IOException e) {
            Log.d("Request", "buildTextPart: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private StringBuilder readFromFile(File file) {
        StringBuilder contents = new StringBuilder();
        FileInputStream fis;
        byte[] bytes;
        int length;

        fis = null;
        length = (int) file.length();
        bytes = new byte[length];

        try {
            fis = new FileInputStream(file);
            fis.read(bytes);
            fis.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        contents.append(new String(bytes));
        return contents;
    }
}