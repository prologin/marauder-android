package org.prologin.marauder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Broadcast intent receiver that starts up the Marauder background reporting service.
 */
public class ServiceLauncherReceiver extends BroadcastReceiver {
  @Override
  public void onReceive(Context context, Intent intent) {
    ReporterService.sendConfigRefreshIntent(context);
  }
}
