package org.prologin.marauder;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.common.collect.ImmutableList;

import java.io.IOException;
import java.util.List;

/**
 * The reporter service is in charge of sending location updates to the Marauder API.
 */
public class ReporterService extends Service implements LocationListener {
  private static final String TAG = ReporterService.class.getSimpleName();

  public static final String REFRESH_CONFIG_ACTION =
      "org.prologin.marauder.ReporterService.REFRESH_CONFIG_ACTION";
  private static final String REFRESH_GCM_TOKEN_ACTION =
      "org.prologin.marauder.ReporterService.REFRESH_GCM_TOKEN_ACTION";

  // How often to get geofence updates from the API.
  private static final long CONFIG_REFRESH_INTERVAL_MS = 900 * 1000;

  // How often to get location updates within a configured geofence.
  private static final long LOCATION_REFRESH_SHORT_INTERVAL_MS = 15 * 1000;

  // How often to get location updates outside of a configured geofence.
  private static final long LOCATION_REFRESH_LONG_INTERVAL_MS = 120 * 1000;

  private Handler handler;
  private Looper looper;
  private GoogleApiClient gApiClient = null;
  private List<EventLocation> eventLocations = ImmutableList.of();
  private boolean withinGeofence = false;
  private String gcmToken = null;

  public static void sendConfigRefreshIntent(Context context) {
    Intent intent = new Intent(context, ReporterService.class);
    intent.setAction(REFRESH_CONFIG_ACTION);
    context.startService(intent);
  }

  public static void sendGcmTokenRefreshIntent(Context context) {
    Intent intent = new Intent(context, ReporterService.class);
    intent.setAction(REFRESH_GCM_TOKEN_ACTION);
    context.startService(intent);
  }

  @Override
  public void onCreate() {
    super.onCreate();

    HandlerThread handlerThread = new HandlerThread("ReporterService");
    handlerThread.start();
    looper = handlerThread.getLooper();
    handler = new Handler(looper) {
      @Override
      public void handleMessage(Message msg) {
        onHandleIntent((Intent) msg.obj);
      }
    };
  }

  @Override
  public int onStartCommand(Intent intent, int startId, int flags) {
    Message msg = handler.obtainMessage();
    msg.obj = intent;
    handler.sendMessage(msg);

    return START_STICKY;
  }

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  @Override
  public void onDestroy() {
    looper.quit();
    if (gApiClient != null) {
      gApiClient.disconnect();
    }
  }

  private void onHandleIntent(Intent intent) {
    if (intent == null || intent.getAction() == REFRESH_CONFIG_ACTION) {
      Log.w(TAG, "Refreshing Marauder config.");
      eventLocations = getConfiguredGeofences();
      gcmToken = refreshGcmToken();
      setupGeoTracking();
      scheduleAlarm();
      Log.w(TAG, "Marauder config refreshed.");
    } else if (intent.getAction() == REFRESH_GCM_TOKEN_ACTION) {
      Log.w(TAG, "Refreshing GCM token.");
      gcmToken = refreshGcmToken();
      Log.w(TAG, "GCM token refreshed.");
    }
  }

  private GoogleApiClient getGoogleApiClient() {
    if (gApiClient == null) {
      gApiClient = new GoogleApiClient.Builder(this)
          .addApi(LocationServices.API)
          .build();
      gApiClient.blockingConnect();
    }
    return gApiClient;
  }

  private String refreshGcmToken() {
    try {
      InstanceID instanceId = InstanceID.getInstance(this);
      return instanceId.getToken(getString(R.string.gcmSenderId),
          GoogleCloudMessaging.INSTANCE_ID_SCOPE,
          null);
    } catch (IOException e) {
      Log.e(TAG, "Could not get GCM token", e);
      return null;
    }
  }

  private void scheduleAlarm() {
    Intent intent = new Intent(getApplicationContext(), ReporterService.class);
    intent.setAction(REFRESH_CONFIG_ACTION);
    PendingIntent pendingIntent =
        PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    AlarmManager alarmManager = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
    alarmManager.cancel(pendingIntent);
    alarmManager.set(AlarmManager.RTC_WAKEUP,
        System.currentTimeMillis() + CONFIG_REFRESH_INTERVAL_MS,
        pendingIntent);
  }

  private List<EventLocation> getConfiguredGeofences() {
    try {
      ApiClient client = new ApiClient(this);
      return client.getConfiguration();
    } catch (Exception e) {
      Log.e(TAG, "Could not get geofences configuration", e);
      return ImmutableList.of();
    }
  }

  private void setupGeoTracking() {
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
        PackageManager.PERMISSION_GRANTED) {
      Log.w(TAG, "Fine location permission unavailable, no geofencing.");
      return;
    }

    LocationServices.FusedLocationApi.removeLocationUpdates(getGoogleApiClient(), this);
    if (eventLocations.isEmpty()) {
      Log.w(TAG, "No events configured, disabling geofencing.");
      return;
    }

    long intervalMs =
        withinGeofence ? LOCATION_REFRESH_SHORT_INTERVAL_MS : LOCATION_REFRESH_LONG_INTERVAL_MS;
    int accuracy = withinGeofence
        ? LocationRequest.PRIORITY_HIGH_ACCURACY
        : LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY;
    Log.w(TAG, "Setting up geofencing for " + (withinGeofence ? "high" : "low") + " accuracy.");

    LocationRequest request = new LocationRequest();
    request.setInterval(intervalMs);
    request.setFastestInterval(LOCATION_REFRESH_SHORT_INTERVAL_MS);
    request.setPriority(accuracy);
    LocationServices.FusedLocationApi.requestLocationUpdates(getGoogleApiClient(), request, this);
  }

  private GcmSettings getGcmSettings() {
    if (gcmToken == null) {
      return null;
    }
    return new GcmSettings(gcmToken, getString(R.string.gcmAppId));
  }

  @Override
  public void onLocationChanged(Location location) {
    Log.w(TAG, "Location update! " + location.toString());
    boolean oldWithinGeofence = withinGeofence;
    withinGeofence = false;
    for (EventLocation eventLocation : eventLocations) {
      if (location.getAccuracy() > eventLocation.getRadiusInMeters()) {
        continue;
      }
      if (location.distanceTo(eventLocation.toAndroidLocation()) <
          eventLocation.getRadiusInMeters() + location.getAccuracy()) {
        withinGeofence = true;
      }
    }

    try {
      ApiClient apiClient = new ApiClient(this);
      apiClient.reportLocation(location, withinGeofence, getGcmSettings());
    } catch (Exception e) {
      Log.e(TAG, "Could not report updated location", e);
    }

    if (withinGeofence != oldWithinGeofence) {
      Log.w(TAG, "Now " + (withinGeofence ? "inside" : "outside") + " a geofence.");
      setupGeoTracking();
    }
  }
}