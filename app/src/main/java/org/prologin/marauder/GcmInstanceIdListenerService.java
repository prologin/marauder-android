package org.prologin.marauder;

import com.google.android.gms.iid.InstanceIDListenerService;

public class GcmInstanceIdListenerService extends InstanceIDListenerService {
  @Override
  public void onTokenRefresh() {
    ReporterService.sendGcmTokenRefreshIntent(this);
  }
}
