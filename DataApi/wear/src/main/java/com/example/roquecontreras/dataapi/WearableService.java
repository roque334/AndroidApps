package com.example.roquecontreras.dataapi;

import android.app.Notification;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.example.roquecontreras.common.MobileWearConstants;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

/**
 * Created by Roque Contreras on 16/10/2015.
 */
public class WearableService extends WearableListenerService {

    private static final String LOG_TAG = "WearableListenerService";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * Execute the proper actions in accordance of the message path.
     * The dataitem received can starts or stops the accelerometer service.
     * @param dataEvents the dataitems received.
     */
    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        NotificationCompat.Builder builder;
        Notification notification;
        NotificationManagerCompat notificationManagerCompat;
        Intent intent;
        for(DataEvent dataEvent: dataEvents) {
            if (dataEvent.getType() == DataEvent.TYPE_CHANGED) {
                switch (dataEvent.getDataItem().getUri().getPath()) {
                    case MobileWearConstants.START_ACCLEROMETER_BY_DATAITEM_PATH:
                        builder = new NotificationCompat.Builder(this)
                                .setSmallIcon(R.drawable.ic_launcher)
                                .setContentTitle("Accelerometer Services")
                                .setContentText("Started.");
                        notification = builder.build();

                        notificationManagerCompat = NotificationManagerCompat.from(this);
                        notificationManagerCompat.notify(1, notification);

                        intent = new Intent(this, SensorService.class);
                        DataMap dataMap = DataMapItem.fromDataItem(dataEvent.getDataItem()).getDataMap();
                        intent.putExtra(MobileWearConstants.KEY_MEASUREMENTS_SAMPLE_INTERVAL, dataMap.getLong(MobileWearConstants.KEY_MEASUREMENTS_SAMPLE_INTERVAL));
                        intent.putExtra(MobileWearConstants.KEY_HANDHELD_WEAR_SYNC_INTERVAL, dataMap.getLong(MobileWearConstants.KEY_HANDHELD_WEAR_SYNC_INTERVAL));
                        startService(intent);
                        break;
                    case MobileWearConstants.STOP_ACCLEROMETER_BY_DATAITEM_PATH:
                        builder = new NotificationCompat.Builder(this)
                                .setSmallIcon(R.drawable.ic_launcher)
                                .setContentTitle("Accelerometer Services")
                                .setContentText("Stopped.");
                        notification = builder.build();

                        notificationManagerCompat = NotificationManagerCompat.from(this);
                        notificationManagerCompat.notify(1, notification);
                        stopService(new Intent(this, SensorService.class));
                    default:
                        break;
                }
            }
        }
    }

    /**
     * Execute the proper actions in accordance of the message path.
     * The message received can arranges the sensors over the patients body and creates a
     * notification with its wearing instructions.
     * @param messageEvent the message received.
     */
    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        super.onMessageReceived(messageEvent);
        NotificationCompat.Builder builder;
        Notification notification;
        NotificationManagerCompat notificationManagerCompat;
        Intent intent;
        String message;
        switch (messageEvent.getPath()) {
            case MobileWearConstants.ARRANGE_SENSORS_BY_MESSAGE_PATH:
                message = new String(messageEvent.getData());
                builder = new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle(WearableService.this.getString(R.string.arrange_title))
                        .setContentText(String.format(WearableService.this.getString(R.string.arrange_text), MobileWearConstants.bodyPartToText(WearableService.this, message).toLowerCase()));

                setNotificationBackgroundImage(builder, message);
                notification = builder.build();

                notificationManagerCompat = NotificationManagerCompat.from(this);
                notificationManagerCompat.notify(1, notification);
                break;
            case MobileWearConstants.START_ACCLEROMETER_BY_MESSAGE_PATH:
                message = new String(messageEvent.getData());
                builder = new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle(WearableService.this.getString(R.string.start_stop_title))
                        .setContentText(WearableService.this.getString(R.string.start_text));
                notification = builder.build();

                notificationManagerCompat = NotificationManagerCompat.from(this);
                notificationManagerCompat.notify(2, notification);

                intent = new Intent(this, SensorService.class);
                startService(intent);
                break;
            case MobileWearConstants.STOP_ACCLEROMETER_BY_MESSAGE_PATH:
                message = new String(messageEvent.getData());
                builder = new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle(WearableService.this.getString(R.string.start_stop_title))
                        .setContentText(WearableService.this.getString(R.string.stop_text));
                notification = builder.build();

                notificationManagerCompat = NotificationManagerCompat.from(this);
                notificationManagerCompat.notify(3, notification);
                stopService(new Intent(this, SensorService.class));
            default:
                break;
        }
    }

    /**
     * Sets the background image of the notification builder in accordance of a message.
     * @param builder the notification builder to set the background image.
     * @param message the message that determines the image to be set.
     */
    private void setNotificationBackgroundImage(NotificationCompat.Builder builder, String message) {
        switch (message) {
            case MobileWearConstants.LARM_MESSAGE:
                builder.extend(new NotificationCompat.WearableExtender().setBackground(BitmapFactory.decodeResource(
                        getResources(), R.drawable.lhand_notif_background)));
                break;
            case MobileWearConstants.RARM_MESSAGE:
                builder.extend(new NotificationCompat.WearableExtender().setBackground(BitmapFactory.decodeResource(
                        getResources(), R.drawable.rhand_notif_background)));
                break;
            case MobileWearConstants.LLEG_MESSAGE:
                builder.extend(new NotificationCompat.WearableExtender().setBackground(BitmapFactory.decodeResource(
                        getResources(), R.drawable.lleg_notif_background)));
                break;
            case MobileWearConstants.RLEG_MESSAGE:
                builder.extend(new NotificationCompat.WearableExtender().setBackground(BitmapFactory.decodeResource(
                        getResources(), R.drawable.rleg_notif_background)));
                break;
            default:
                builder.extend(new NotificationCompat.WearableExtender().setBackground(BitmapFactory.decodeResource(
                        getResources(), R.drawable.notif_background)));
                break;
        }
    }

}
