package org.prologin.marauder;

/**
 * Stores settings needed for the Marauder server to send GCM push notifications to a client.
 */
public class GcmSettings {
  private String token;
  private String appId;

  public GcmSettings(String token, String appId) {
    this.token = token;
    this.appId = appId;
  }

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public String getAppId() {
    return appId;
  }

  public void setAppId(String appId) {
    this.appId = appId;
  }
}
