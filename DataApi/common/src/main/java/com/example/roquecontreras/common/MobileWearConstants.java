package com.example.roquecontreras.common;

import android.content.Context;
import android.util.Log;

/**
 * Created by Roque Contreras on 16/10/2015.
 */
public class MobileWearConstants {

        //PATHS
        public static final String ARRANGE_SENSORS_BY_MESSAGE_PATH = "/arrange-sensors-message";
        public static final String START_ACCELEROMETER_BY_MESSAGE_PATH = "/start-accelerometer-message-service";
        public static final String STOP_ACCELEROMETER_BY_MESSAGE_PATH = "/stop-accelerometer-message-service";
        public static final String START_ACCELEROMETER_BY_DATAITEM_PATH = "/start-accelerometer-dataitem-service";
        public static final String STOP_ACCELEROMETER_BY_DATAITEM_PATH = "/stop-accelerometer-dataitem-service";
        public static final String START_SYNC_WEAR_BY_MESSAGE_PATH = "/start-sync-wear-message-service";
        public static final String START_DEL_WEAR_BY_MESSAGE_PATH = "/start-del-wear-message-service";
        public static final String SEND_BY_CHANNEL_PATH = "/send-by-channel";

        //CAPABILITIES
        public static final String DATA_ANALYSIS_CAPABILITY = "data_analysis";
        public static final String TREMOR_QUANTIFICATION_CAPABILITY = "tremor_quantification";

        //TO MAKE EVERY NOTIFICACION UNIQUE
        public static final String NOTIFICATION_TIMESTAMP = "timestamp";

        //EXTRAS
        public static final String WEARABLE_NODES_EXTRA = "werable_nodes_extra";

        //MESSAGE
        public static final String LARM_MESSAGE = "left hand";
        public static final String RARM_MESSAGE = "right hand";
        public static final String LLEG_MESSAGE = "left leg";
        public static final String RLEG_MESSAGE = "right leg";

        //PREFERENCES KEYS
        public static final String KEY_MEASUREMENTS_SAMPLE_INTERVAL = "pref_key_measurements_sample_interval";
        public static final String KEY_HANDHELD_WEAR_SYNC_NOTIFICATION = "pref_key_handheld_wear_sync_notification";
        public static final String KEY_HANDHELD_WEAR_SYNC_WIFI = "pref_key_handheld_wear_sync_wifi";
        public static final String KEY_HANDHELD_WEAR_SYNC_INTERVAL = "pref_key_handheld_wear_sync_interval";
        public static final String KEY_HANDHELP_SERVER_SYNC_NOTIFICATION = "pref_key_handhelp_server_sync_notification";
        public static final String KEY_HANDHELP_SERVER_SYNC_WIFI = "pref_key_handhelp_server_sync_wifi";
        public static final String KEY_HANDHELD_SERVER_SYNC_INTERVAL = "pref_key_handheld_server_sync_interval";

        //ARRANGEMENT FILE
        public static final String ARRANGEMENT_FILENAME = "arrangement_file";

        //RESULT CODES
        public static final int RESULT_CODE_ARRANGEMENT = 0;
        public static final int RESULT_OK = 1;
        public static final int RESULT_ERROR = 0;

        //MEASUREMENT FILE
        public static final String MEASUREMENT_FILENAME_START = "measurements_";

        /**
         * @param context  the context of the application.
         * @param bodyPart the message with body part.
         * @return the body part readable by a human.
         */
        public static String bodyPartToText(Context context, String bodyPart) {
                String result = "";
                Log.d("BodyPartToText", bodyPart);
                switch (bodyPart) {
                        case MobileWearConstants.LARM_MESSAGE:
                                result = context.getString(R.string.left_arm);
                                break;
                        case MobileWearConstants.RARM_MESSAGE:
                                result = context.getString(R.string.right_arm);
                                break;
                        case MobileWearConstants.LLEG_MESSAGE:
                                result = context.getString(R.string.left_leg);
                                break;
                        case MobileWearConstants.RLEG_MESSAGE:
                                result = context.getString(R.string.right_leg);
                        default:
                                break;
                }
                return result;
        }
}