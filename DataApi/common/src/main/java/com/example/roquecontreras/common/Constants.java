package com.example.roquecontreras.common;

/**
 * Created by Roque Contreras on 16/10/2015.
 */
public class Constants {

        //PATHS
        public static final String ARRANGE_SENSORS_BY_DATAITEM_PATH = "/arrange-sensors-dataitem";
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

        public static final String ACTION_DISMISS = "com.example.roquecontreras.dataapi.DISMISS";
}
