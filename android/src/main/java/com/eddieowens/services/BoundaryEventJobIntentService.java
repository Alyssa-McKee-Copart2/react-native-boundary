package com.eddieowens.services;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.JobIntentService;
import android.util.Log;
import android.os.Build;
import android.app.ActivityManager;

import com.eddieowens.RNBoundaryModule;
import com.eddieowens.errors.GeofenceErrorMessages;
import com.facebook.react.HeadlessJsTaskService;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import java.util.ArrayList;
import java.util.List;
import java.lang.Exception;
import com.crashlytics.android.Crashlytics;

import static com.eddieowens.RNBoundaryModule.TAG;
import com.reactnativenavigation.NavigationApplication;

public class BoundaryEventJobIntentService extends JobIntentService {

    public static final String ON_ENTER = "onEnter";
    public static final String ON_EXIT = "onExit";

    public BoundaryEventJobIntentService() {
        super();
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        if (!canHandleJob()) {
            // safety net, don't start the service if the OS >= Oreo and app is in background
            // TODO: actually fix this for Oreo+ devices, using foreground service? a different headlessJSService that implements Jobs?
            Log.i(TAG, "Can not handle geofence event. Job cannot be safely handled");
            return;
        }
        if (!NavigationApplication.instance.isReactContextInitialized()) {
            // dont do anything if the app isn't initialized, or job can not be safely handled
            Log.i(TAG, "Can not handle geofence event. React not initialized");
            return;
        };

        Log.i(TAG, "Handling geofencing event");
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);

        Log.i(TAG, "Geofence transition: " + geofencingEvent.getGeofenceTransition());
        if (geofencingEvent.hasError()) {
            Log.e(TAG, "Error in handling geofence " + GeofenceErrorMessages.getErrorString(geofencingEvent.getErrorCode()));
            return;
        }
        switch (geofencingEvent.getGeofenceTransition()) {
            case Geofence.GEOFENCE_TRANSITION_ENTER:
                Log.i(TAG, "Enter geofence event detected. Sending event.");
                final ArrayList<String> enteredGeofences = new ArrayList<>();
                for (Geofence geofence : geofencingEvent.getTriggeringGeofences()) {
                    enteredGeofences.add(geofence.getRequestId());
                }
                sendEvent(this.getApplicationContext(), ON_ENTER, enteredGeofences);
                break;
            case Geofence.GEOFENCE_TRANSITION_EXIT:
                Log.i(TAG, "Exit geofence event detected. Sending event.");
                final ArrayList<String> exitingGeofences = new ArrayList<>();
                for (Geofence geofence : geofencingEvent.getTriggeringGeofences()) {
                    exitingGeofences.add(geofence.getRequestId());
                }
                sendEvent(this.getApplicationContext(), ON_EXIT, exitingGeofences);
                break;
        }
    }

    private void sendEvent(Context context, String event, ArrayList<String> params) {
        final Intent intent = new Intent(RNBoundaryModule.GEOFENCE_DATA_TO_EMIT);
        intent.putExtra("event", event);
        intent.putExtra("params", params);

        Bundle bundle = new Bundle();
        bundle.putString("event", event);
        bundle.putStringArrayList("ids", intent.getStringArrayListExtra("params"));

        Intent headlessBoundaryIntent = new Intent(context, BoundaryEventHeadlessTaskService.class);
        headlessBoundaryIntent.putExtras(bundle);
        try {
            context.startService(headlessBoundaryIntent);
            HeadlessJsTaskService.acquireWakeLockNow(context);
        } catch (Exception e) {
            Crashlytics.logException(e);
            // one final safety net to prevent the app from crashing if starting the service still throws an exception
            // TODO: Crashlytics Non-Fatal Exception?
            Log.i(TAG, "EXCEPTION Caught starting the HeadlessJS Task: " + e.getMessage());
        }
    }
    private boolean canHandleJob() {
        boolean osIsBeforeOreo = Build.VERSION.SDK_INT < Build.VERSION_CODES.O;
        return osIsBeforeOreo || isAppOnForeground(this.getApplicationContext());
    }
    private boolean isAppOnForeground(Context context) {
        /**
         http://stackoverflow.com/questions/8489993/check-android-application-is-in-foreground-or-not
        **/
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> appProcesses =
        activityManager.getRunningAppProcesses();
        if (appProcesses == null) {
            return false;
        }
        final String packageName = context.getPackageName();
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            if (appProcess.importance ==
            ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND &&
             appProcess.processName.equals(packageName)) {
                return true;
            }
        }
        return false;
    }
}