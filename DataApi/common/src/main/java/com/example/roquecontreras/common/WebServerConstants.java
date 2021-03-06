package com.example.roquecontreras.common;

/**
 * Created by roquecontreras on 19/01/16.
 */
public class WebServerConstants {

    public static final String ID_TOKEN_LABEL = "id-token";

    public static final String WEB_SERVER_CLIENT_ID = "503008496668-e83akin5nrof707ijj8j7vs8edo4j5vb.apps.googleusercontent.com";

    public static final String LOCAL_WEB_SERVER_UPLOAD_MEASURE_FILE_URL = "http://10.0.2.2:8000/measureUpload";

    //Set the ip of the proprietary server in the following URL
    public static final String WEB_SERVER_UPLOAD_MEASURE_FILE_URL = "http://ip:8000/measureUpload";

    public static final String HEROKU_WEB_SERVER_UPLOAD_MEASURE_FILE_URL = "https://moreno-app.herokuapp.com/measureUpload";

    public static final String LOCAL_WEB_SERVER_VALIDATE_TOKEN_URL = "http://10.0.2.2:8000/patientVerify";

    //Set the ip of the proprietary server in the following URL
    public static final String WEB_SERVER_VALIDATE_TOKEN_URL = "http://ip:8000/patientVerify";

    public static final String HEROKU_WEB_SERVER_VALIDATE_TOKEN_URL = "https://moreno-app.herokuapp.com/patientVerify";
}