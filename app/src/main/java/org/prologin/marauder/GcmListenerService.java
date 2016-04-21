package org.prologin.marauder;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class GcmListenerService extends com.google.android.gms.gcm.GcmListenerService {
  private static final String TAG = "GcmListenerService";

  @Override
  public void onMessageReceived(String from, Bundle data) {
    String title = data.getString("title");
    String message = data.getString("message");

    if (title == null || message == null) {
      Log.e(TAG, "Invalid notification message received: " + data.toString());
      return;
    }

    Notification notification = new NotificationCompat.Builder(this)
        .setSmallIcon(R.drawable.logo)
        .setContentTitle(title)
        .setContentText(message)
        .setPriority(NotificationCompat.PRIORITY_MAX)
        .setVibrate(new long[] { 1000, 250, 250, 250 })
        .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
        .setLights(Color.YELLOW, 250, 250)
        .build();
    NotificationManager manager =
        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    manager.notify(0, notification);

  }
}
