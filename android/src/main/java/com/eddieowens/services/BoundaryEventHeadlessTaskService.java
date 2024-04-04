package com.eddieowens.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.eddieowens.R;
import com.facebook.react.HeadlessJsTaskService;
import com.facebook.react.jstasks.HeadlessJsTaskConfig;
import com.facebook.react.bridge.Arguments;

public class BoundaryEventHeadlessTaskService extends HeadlessJsTaskService {
    public static final String NOTIFICATION_CHANNEL_ID = "com.eddieowens.GEOFENCE_SERVICE_CHANNEL";
    private static final String KEY_NOTIFICATION_TITLE = "rnboundary.notification_title";
    private static final String KEY_NOTIFICATION_TEXT = "rnboundary.notification_text";
    private static final String KEY_NOTIFICATION_ICON = "rnboundary.notification_icon";
    private static final String KEY_NOTIFICATION_ICON_COLOR = "rnboundary.notification_icon_color";
    private static final String KEY_NOTIFICATION_CHANNEL_NAME = "rnboundary.notification_channel_name";
    private static final String KEY_NOTIFICATION_CHANNEL_DESCRIPTION = "rnboundary.notification_channel_description";

    @Nullable
    protected HeadlessJsTaskConfig getTaskConfig(Intent intent) {
        Bundle extras = intent.getExtras();
        return new HeadlessJsTaskConfig(
                "OnBoundaryEvent",
                extras != null ? Arguments.fromBundle(extras) : null,
                5000,
                true);
    }

    public NotificationCompat.Builder getNotificationBuilder() {
        Context context = getApplicationContext();
        String title = "Geofencing in progress";
        String text = "You're close to the configured location";
        int iconResource = -1;
        int iconColorResource = R.color.accent_material_light;
        try {
            ApplicationInfo ai = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            Bundle bundle = ai.metaData;
            title = bundle.getString(KEY_NOTIFICATION_TITLE, title);
            text = bundle.getString(KEY_NOTIFICATION_TEXT, text);
            iconResource = bundle.getInt(KEY_NOTIFICATION_ICON, -1);
            iconColorResource = bundle.getInt(KEY_NOTIFICATION_ICON_COLOR, R.color.accent_material_light);
        } catch (Exception e) {
            Log.e(TAG, "Cannot get application Bundle " + e.toString());
        }


        // Notification for the foreground service
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setColor(ContextCompat.getColor(context, iconColorResource));

        if (iconResource > -1) {
            builder.setSmallIcon(iconResource);
        }

        return builder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        startForegroundServiceNotification();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int result = super.onStartCommand(intent, flags, startId);
        startForegroundServiceNotification();
        return result;
    }

    private void startForegroundServiceNotification() {
        Context context = this.getApplicationContext();

        // Channel for the foreground service notification
        createChannel(context);

        // Notification for the foreground service
        NotificationCompat.Builder builder = getNotificationBuilder();
        Notification notification = builder.build();

        Random rand = new Random();
        int notificationId = rand.nextInt(100000);

        startForeground(notificationId, notification);
        HeadlessJsTaskService.acquireWakeLockNow(context);
    }

    private void createChannel(Context context) {
        String channelName = "Geofence Service";
        String channelDescription = "Only used to know when you're close to a configured location.";

        try {
            ApplicationInfo ai = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            Bundle bundle = ai.metaData;
            channelName = bundle.getString(KEY_NOTIFICATION_CHANNEL_NAME, channelName);
            channelDescription = bundle.getString(KEY_NOTIFICATION_CHANNEL_DESCRIPTION, channelDescription);
        } catch (Exception e) {
            Log.e(TAG, "Cannot get application Bundle " + e.toString());
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    channelName,
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription(channelDescription);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}