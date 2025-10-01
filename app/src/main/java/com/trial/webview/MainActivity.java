package com.trial.webview;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.net.Uri;
import android.content.Intent;
import android.provider.MediaStore;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import androidx.core.content.FileProvider;

public class MainActivity extends AppCompatActivity {

    private WebView webViewOnline, webViewOffline;
    private final String LIVE_URL = "https://idjforlife.com/referral/";
    private final String OFFLINE_URL = "file:///android_asset/idj_offline/index.html";
    private boolean alreadyLoaded = false;

    private ValueCallback<Uri[]> filePathCallback;
    private static final int FILE_CHOOSER_REQUEST_CODE = 1001;
    private Uri cameraImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(new String[]{
                        Manifest.permission.CAMERA,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                }, 1);
            }
        }


        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
            window.setNavigationBarColor(Color.TRANSPARENT);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false);
        } else {
            View decorView = window.getDecorView();
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            );
        }

        setContentView(R.layout.activity_main);

        webViewOnline = findViewById(R.id.webview_online);
        webViewOffline = findViewById(R.id.webview_offline);

        setupWebView(webViewOnline);
        setupWebView(webViewOffline);

        webViewOnline.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                if (MainActivity.this.filePathCallback != null) {
                    MainActivity.this.filePathCallback.onReceiveValue(null);
                }
                MainActivity.this.filePathCallback = filePathCallback;

                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                    File photoFile = null;
                    try {
                        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                        String imageFileName = "JPEG_" + timeStamp + "_";
                        File storageDir = getExternalFilesDir(null);
                        photoFile = File.createTempFile(imageFileName, ".jpg", storageDir);
                        cameraImageUri = FileProvider.getUriForFile(MainActivity.this, getPackageName() + ".provider", photoFile);
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
                        takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // ADD THIS
                    } catch (IOException ex) {
                        ex.printStackTrace();
                        takePictureIntent = null;
                    }
                }

                Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
                contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
                contentSelectionIntent.setType("image/*"); // CHANGE to image/*

                Intent[] intentArray = takePictureIntent != null ? new Intent[]{takePictureIntent} : new Intent[0];

                Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
                chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
                chooserIntent.putExtra(Intent.EXTRA_TITLE, "Pilih atau Ambil Foto");
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);
                chooserIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // ADD THIS

                startActivityForResult(chooserIntent, FILE_CHOOSER_REQUEST_CODE);
                return true;
            }
        });

        if (isNetworkAvailable()) {
            webViewOffline.setVisibility(View.VISIBLE);
            webViewOnline.setVisibility(View.GONE);
            webViewOffline.loadUrl(OFFLINE_URL);

            webViewOnline.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);

                    webViewOnline.setAlpha(0f);
                    webViewOnline.setVisibility(View.VISIBLE);

                    webViewOnline.animate()
                            .alpha(1f)
                            .setDuration(500)
                            .withEndAction(() -> {
                                webViewOffline.animate()
                                        .alpha(0f)
                                        .setDuration(500)
                                        .withEndAction(() -> {
                                            webViewOffline.setVisibility(View.GONE);
                                            webViewOffline.loadUrl("about:blank");
                                        })
                                        .start();
                            })
                            .start();
                }
            });

            webViewOnline.loadUrl(LIVE_URL);
            alreadyLoaded = true;

        } else {
            webViewOffline.setVisibility(View.VISIBLE);
            webViewOnline.setVisibility(View.GONE);
            webViewOffline.loadUrl(OFFLINE_URL);
            startNetworkMonitor();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER_REQUEST_CODE) {
            if (filePathCallback == null) return;
            Uri[] results = null;
            if (resultCode == RESULT_OK) {
                if (data == null) {
                    if (cameraImageUri != null) {
                        sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, cameraImageUri)); // Tambahkan ini
                        results = new Uri[]{cameraImageUri};
                    }
                } else {
                    Uri dataUri = data.getData();
                    if (dataUri != null) {
                        results = new Uri[]{dataUri};
                    }
                }
            }
            filePathCallback.onReceiveValue(results);
            filePathCallback = null;
        }
    }

    private void setupWebView(WebView webView) {
        webView.setBackgroundColor(Color.parseColor("#120516"));
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        WebSettings webSettings = webView.getSettings();
        webView.setVerticalScrollBarEnabled(false);
        webView.setHorizontalScrollBarEnabled(false);
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setSupportZoom(false);
        webSettings.setBuiltInZoomControls(false);
        webSettings.setDisplayZoomControls(false);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnected();
    }

    private void startNetworkMonitor() {
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException ignored) {}
                if (isNetworkAvailable()) {
                    runOnUiThread(() -> {
                        if (!alreadyLoaded) {
                            webViewOnline.setWebViewClient(new WebViewClient() {
                                @Override
                                public void onPageFinished(WebView view, String url) {
                                    super.onPageFinished(view, url);
                                    webViewOnline.setAlpha(0f);
                                    webViewOnline.setVisibility(View.VISIBLE);

                                    webViewOnline.animate()
                                            .alpha(1f)
                                            .setDuration(500)
                                            .withEndAction(() -> {
                                                webViewOffline.animate()
                                                        .alpha(0f)
                                                        .setDuration(500)
                                                        .withEndAction(() -> {
                                                            webViewOffline.setVisibility(View.GONE);
                                                            webViewOffline.loadUrl("about:blank");
                                                        })
                                                        .start();
                                            })
                                            .start();
                                }
                            });

                            webViewOnline.loadUrl(LIVE_URL);
                            alreadyLoaded = true;
                        }
                    });
                    break;
                }
            }
        }).start();
    }

    @Override
    public void onBackPressed() {
        if (webViewOnline.getVisibility() == View.VISIBLE && webViewOnline.canGoBack()) {
            webViewOnline.goBack();
        } else if (webViewOffline.getVisibility() == View.VISIBLE && webViewOffline.canGoBack()) {
            webViewOffline.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    // Jika user menolak, bisa tampilkan toast agar user paham
                    Toast.makeText(this, "Izin diperlukan agar upload kamera & file dapat berjalan.", Toast.LENGTH_LONG).show();
                    return;
                }
            }
            // Jika semua izin diberikan, tidak perlu melakukan apa-apa
        }
    }

}