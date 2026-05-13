package com.poop.log;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.webkit.*;
import android.widget.Toast;
import java.io.*;

public class MainActivity extends Activity {
    private WebView mWeb;
    private ValueCallback<Uri[]> mFileCb;
    private static final int REQ_FILE = 1;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        mWeb = new WebView(this);
        setContentView(mWeb);

        WebSettings ws = mWeb.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setAllowFileAccessFromFileURLs(true);
        ws.setAllowUniversalAccessFromFileURLs(true);
        ws.setCacheMode(WebSettings.LOAD_DEFAULT);

        mWeb.addJavascriptInterface(new Bridge(), "Android");
        mWeb.setWebViewClient(new WebViewClient());
        mWeb.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb,
                                             FileChooserParams p) {
                mFileCb = cb;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.setType("application/json");
                i.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(Intent.createChooser(i, "選擇備份檔"), REQ_FILE);
                return true;
            }
        });
        mWeb.loadUrl("file:///android_asset/index.html");
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        if (req == REQ_FILE && mFileCb != null) {
            mFileCb.onReceiveValue(res == RESULT_OK && data != null
                    ? new Uri[]{data.getData()} : null);
            mFileCb = null;
        }
    }

    @Override
    public void onBackPressed() {
        if (mWeb.canGoBack()) mWeb.goBack();
        else super.onBackPressed();
    }

    private class Bridge {
        @JavascriptInterface
        public void saveFile(String name, String content) {
            try {
                ContentValues cv = new ContentValues();
                cv.put(MediaStore.Downloads.DISPLAY_NAME, name);
                cv.put(MediaStore.Downloads.MIME_TYPE, "application/json");
                Uri uri = getContentResolver().insert(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
                if (uri != null)
                    try (OutputStream os = getContentResolver().openOutputStream(uri)) {
                        os.write(content.getBytes("UTF-8"));
                    }
                runOnUiThread(() -> Toast.makeText(MainActivity.this,
                        "已儲存至 Downloads", Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
