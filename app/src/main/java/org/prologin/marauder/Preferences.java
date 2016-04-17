package org.prologin.marauder;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Provides access to the app's preferences.
 */
public class Preferences {
  private Context context;

  private final String PREFERENCES_FILE_NAME = "MarauderPreferences";

  private final String COOKIE_KEY = "cookie";

  public Preferences(Context context) {
    this.context = context;
  }

  public String getCookie() {
    return loadPreferences().getString(COOKIE_KEY, null);
  }

  public void setCookie(String cookie) {
    SharedPreferences.Editor prefs = loadPreferences().edit();
    prefs.putString(COOKIE_KEY, cookie);
    prefs.commit();
  }

  private SharedPreferences loadPreferences() {
    return context.getSharedPreferences(PREFERENCES_FILE_NAME, Context.MODE_PRIVATE);
  }
}
