package org.prologin.marauder;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Window;
import android.webkit.CookieManager;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import org.jetbrains.annotations.Contract;

// The main activity is a simple wrapper around a WebView that shows
// the online Marauder's map UI.
public class MainActivity extends AppCompatActivity {

  protected WebView webView;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    getWindow().requestFeature(Window.FEATURE_PROGRESS);
    super.onCreate(savedInstanceState);

    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
        PackageManager.PERMISSION_GRANTED) {
      requestFineLocation();
    } else {
      ReporterService.sendConfigRefreshIntent(this);
    }

    webView = new WebView(this);
    setContentView(webView);

    webView.getSettings().setJavaScriptEnabled(true);

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

  private void requestFineLocation() {
    ActivityCompat.requestPermissions(this,
                                      new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);
  }

  @Override
  public void onRequestPermissionsResult(int requestCode,
                                         @NonNull String[] permissions,
                                         @NonNull int[] grantResults) {
    if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
      ReporterService.sendConfigRefreshIntent(this);
    } else {
      requestFineLocation();
    }
  }

  private String getLoginUrl() {
    return "https://prologin.org/user/login/";
  }

  private String getUiUrl() {
    return "https://prologin.org/marauder/";
  }

  private boolean urlMapsToUi(String url) {
    return url.startsWith(getUiUrl()) || url.startsWith(getLoginUrl());
  }
}
