package org.prologin.marauder;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Window;
import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

// The main activity is a simple wrapper around a WebView that shows
// the online Marauder's map UI.
public class MainActivity extends AppCompatActivity {

  private static final String TAG = MainActivity.class.getSimpleName();
  private final Object lock = new Object();
  protected WebView webView;
  private Intent callPhoneIntent;
  private JsInterface jsInterface;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    getWindow().requestFeature(Window.FEATURE_PROGRESS);
    super.onCreate(savedInstanceState);

    webView = new WebView(this);
    setContentView(webView);

    final String defaultUserAgent = webView.getSettings().getUserAgentString();
    webView.getSettings().setUserAgentString(String.format("Marauder/%s (%s)",
        BuildConfig.VERSION_NAME, defaultUserAgent));

    webView.getSettings().setJavaScriptEnabled(true);
    webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
    webView.getSettings().setGeolocationEnabled(true);

    webView.setWebChromeClient(new WebChromeClient() {
      public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
        callback.invoke(origin, true, false);
      }
    });
    jsInterface = new JsInterface(this, webView);
    webView.addJavascriptInterface(jsInterface, "Native");

    if ("DEBUG".equals(getString(R.string.buildName)) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      Log.i(TAG, "Enabling webview debugging");
      WebView.setWebContentsDebuggingEnabled(true);
    }

    Log.d(TAG, "Loading UI at " + getUiUrl());
    webView.loadUrl(getUiUrl());

    final Activity activity = this;
    webView.setWebViewClient(new WebViewClient() {
      @Override
      public void onReceivedError(WebView view, WebResourceRequest request,
                                  WebResourceError error) {
        Toast.makeText(activity, "Error: " + error.getDescription(), Toast.LENGTH_SHORT).show();
      }

      @Override
      public boolean shouldOverrideUrlLoading(WebView view, String url) {
        if (urlMapsToUi(url)) {
          view.loadUrl(url);
        } else {
          startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        }
        return true;
      }

      @Override
      public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
        CookieManager manager = CookieManager.getInstance();
        new Preferences(MainActivity.this).setCookie(manager.getCookie(url));
      }
    });
  }

  public void triggerConfigRefreshOrPermissionUpdate() {
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
        PackageManager.PERMISSION_GRANTED) {
      requestFineLocation();
    } else {
      ReporterService.sendConfigRefreshIntent(this);
    }
  }

  private void requestFineLocation() {
    ActivityCompat.requestPermissions(this,
        new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);
  }

  private void requestCallPhone() {
    ActivityCompat.requestPermissions(this,
        new String[]{Manifest.permission.CALL_PHONE}, 0);
  }

  @Override
  public void onRequestPermissionsResult(int requestCode,
                                         @NonNull String[] permissions,
                                         @NonNull int[] grantResults) {
    for (int i = 0; i < permissions.length; i++) {
      String permission = permissions[i];
      int result = grantResults[i];
      if (permission.equals(Manifest.permission.ACCESS_FINE_LOCATION)) {
        if (result == PackageManager.PERMISSION_GRANTED) {
          ReporterService.sendConfigRefreshIntent(this);
        } else {
          requestFineLocation();
        }
      }
      if (permission.equals(Manifest.permission.CALL_PHONE)) {
        if (result == PackageManager.PERMISSION_GRANTED) {
          executePendingCall();
        } else {
          requestCallPhone();
        }
      }
    }
  }

  private String getLoginUrl() {
    return getString(R.string.baseApiUrl) + "/user/login/";
  }

  private String getUiUrl() {
    return getString(R.string.baseApiUrl) + "/marauder/";
  }

  private boolean urlMapsToUi(String url) {
    return url.startsWith(getUiUrl()) || url.startsWith(getLoginUrl());
  }

  private void executePendingCall() {
    synchronized (lock) {
      if (callPhoneIntent != null) {
        // Eventually start pending phone call intent
        jsInterface.actionSuccess("call-phone");
        startActivity(callPhoneIntent);
        callPhoneIntent = null;
      }
    }
  }

  public void callPhoneNumber(String phoneNumber) {
    synchronized (lock) {
      try {
        callPhoneIntent = new Intent(Intent.ACTION_CALL);
        callPhoneIntent.setData(Uri.parse("tel:" + phoneNumber));
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) !=
            PackageManager.PERMISSION_GRANTED) {
          requestCallPhone();
        } else {
          executePendingCall();
        }
      } catch (android.content.ActivityNotFoundException ex) {
        runOnUiThread(new Runnable() {
          @Override
          public void run() {
            Toast.makeText(MainActivity.this, "Unable to make the phone call.",
                Toast.LENGTH_SHORT).show();
          }
        });
      }
    }
  }
}
