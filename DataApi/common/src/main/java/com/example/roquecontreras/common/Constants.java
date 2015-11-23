package com.example.roquecontreras.common;

/**
 * Created by Roque Contreras on 16/10/2015.
 */
public class Constants {

        //PATHS
        public static final String ARRANGE_SENSORS_BY_MESSAGE_PATH = "/arrange-sensors-message";
        public static final String START_ACCLEROMETER_BY_DATAITEM_PATH = "/start-accelerometer-dataitem-service";
        public static final String STOP_ACCLEROMETER_BY_DATAITEM_PATH = "/stop-accelerometer-dataitem-service";
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

        //PREFERENCES
        public static final String KEY_MEASUREMENTS_SAMPLE_INTERVAL = "pref_key_measurements_sample_interval";
        public static final String KEY_HANDHELD_WEAR_SYNC_NOTIFICATION = "pref_key_handheld_wear_sync_notification";
        public static final String KEY_HANDHELD_WEAR_SYNC_WIFI = "pref_key_handheld_wear_sync_wifi";
        public static final String KEY_HANDHELD_WEAR_SYNC_INTERVAL = "pref_key_handheld_wear_sync_interval";
        public static final String KEY_HANDHELP_SERVER_SYNC_NOTIFICATION = "pref_key_handhelp_server_sync_notification";
        public static final String KEY_HANDHELP_SERVER_SYNC_WIFI = "pref_key_handhelp_server_sync_wifi";
        public static final String KEY_HANDHELD_SERVER_SYNC_INTERVAL = "pref_key_handheld_server_sync_interval";
}
