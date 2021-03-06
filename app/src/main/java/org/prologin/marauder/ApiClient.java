package org.prologin.marauder;

import android.accounts.NetworkErrorException;
import android.content.Context;
import android.location.Location;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Client for the Marauder reporting API. Used by the ReporterService to get event locations
 * as well as report the current location.
 */
public class ApiClient {
  private static final String TAG = ApiClient.class.getSimpleName();

  private static final String API_BASE = "/marauder/api";
  private static final int READ_TIMEOUT_MS = 5000;
  private static final int CONNECT_TIMEOUT_MS = 5000;

  private final String apiUrl;
  private String authCookie;

  public ApiClient(Context context) throws UnconfiguredException {
    this.authCookie = ApiClient.loadCookieFromPreferences(context);
    this.apiUrl = context.getString(R.string.baseApiUrl) + API_BASE;
  }

  public List<EventLocation> getConfiguration() throws NetworkErrorException {
    try {
      JSONObject result = performApiCall("/geofences/", null);
      JSONArray zones = result.getJSONArray("zones");
      ArrayList<EventLocation> eventLocations = new ArrayList<>();
      for (int i = 0; i < zones.length(); ++i) {
        JSONObject zone = zones.getJSONObject(i);
        eventLocations.add(new EventLocation(zone.getDouble("lat"), zone.getDouble("lon"),
                                             zone.getDouble("radius_meters")));
      }
      return eventLocations;
    } catch (Exception e) {
      throw new NetworkErrorException("Geofences API call failed", e);
    }
  }

  public void reportLocation(Location l, boolean withinGeofence, GcmSettings gcmSettings) throws NetworkErrorException {
    try {
      JSONObject data = new JSONObject();
      data.put("in_area", withinGeofence);
      if (withinGeofence) {
        data.put("lat", l.getLatitude());
        data.put("lon", l.getLongitude());
        data.put("accuracy_meters", l.getAccuracy());
      }
      if (gcmSettings != null) {
        JSONObject gcmData = new JSONObject();
        gcmData.put("app_id", gcmSettings.getAppId());
        gcmData.put("token", gcmSettings.getToken());
        data.put("gcm", gcmData);
      }
      performApiCall("/report/", data);
    } catch (Exception e) {
      throw new NetworkErrorException("Report API call failed", e);
    }
  }

  private JSONObject performApiCall(String handler, JSONObject data)
      throws NetworkErrorException {
    try {
      URL url = new URL(apiUrl + handler);
      Log.d(TAG, "Performing API call at " + url);
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setReadTimeout(READ_TIMEOUT_MS);
      connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
      connection.setDoOutput(data != null);
      connection.setRequestProperty("Cookie", authCookie);
      connection.setRequestMethod(data != null ? "POST" : "GET");

      if (data != null) {
        OutputStreamWriter outputStreamWriter =
            new OutputStreamWriter(connection.getOutputStream());
        outputStreamWriter.write(data.toString());
        outputStreamWriter.close();
      }

      int responseCode = connection.getResponseCode();
      if (responseCode != HttpURLConnection.HTTP_OK) {
        throw new NetworkErrorException("Geofences API returned HTTP " + responseCode);
      }
      StringBuilder sb = new StringBuilder();
      BufferedReader bufferedReader =
          new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"));
      String line;
      while ((line = bufferedReader.readLine()) != null) {
        sb.append(line).append("\n");
      }
      bufferedReader.close();
      connection.disconnect();

      return new JSONObject(sb.toString());
    } catch (Exception e) {
      throw new NetworkErrorException("Could not open API connection.", e);
    }
  }

  private static String loadCookieFromPreferences(Context context)
      throws UnconfiguredException {
    Preferences prefs = new Preferences(context);
    String cookie = prefs.getCookie();
    if (cookie == null) {
      throw new UnconfiguredException("Missing authentication cookie.");
    }
    return cookie;
  }
}
