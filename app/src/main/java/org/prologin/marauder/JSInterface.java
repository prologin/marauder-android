package org.prologin.marauder;

import android.os.Build;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import org.json.JSONArray;

import java.util.Arrays;

public class JSInterface {
  private static final String TAG = JSInterface.class.getSimpleName();
  private final MainActivity activity;
  private final WebView webView;

  JSInterface(MainActivity activity, WebView webView) {
    this.activity = activity;
    this.webView = webView;
  }

  void invoke(String method, Object... args) {
    final String js = String.format("Client.%s(%s);", method, new JSONArray(Arrays.asList(args)).toString());
    Log.d(TAG, "Invoking: " + js);
    webView.post(new Runnable() {
      @Override
      public void run() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
          webView.evaluateJavascript(js, null);
        } else {
          webView.loadUrl("javascript:do {" + js + "} while(0)");
        }
      }
    });
  }

  void actionSuccess(String action) {
    invoke("actionSuccess", action);
  }

  @JavascriptInterface
  public void callPhoneNumber(String phoneNumber) {
    activity.callPhoneNumber(phoneNumber);
  }

  @JavascriptInterface
  public void sendPing(String userId) {
    activity.sendPing(userId);
  }
}
