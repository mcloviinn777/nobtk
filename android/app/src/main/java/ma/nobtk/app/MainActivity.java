package ma.nobtk.app;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.pm.PackageManager;
import android.view.View;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.window.OnBackInvokedDispatcher;
import android.content.ContentValues;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * NOBTK barber app: a full-screen window on the NOBTK barber dashboard.
 * The screen stays on while the app is open, so the barber always sees the line.
 */
public class MainActivity extends Activity {

    private WebView web;
    // When the barber turns on "Bouton Terminé à distance", volume + / a Bluetooth selfie button taps Terminé and volume - undoes.
    private volatile boolean remote = false;
    // Photo picker for <input type="file">: salon photo and client photo.
    private static final int PICK_FILE = 7;
    private ValueCallback<Uri[]> pickCallback;
    private static final int ASK_NOTIF = 8;
    private static final String CHANNEL = "nobtk_alerts";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Android 15+ draws apps edge to edge: keep the page out from under the status bar,
        // the navigation bar and the keyboard by padding the frame around the WebView.
        FrameLayout frame = new FrameLayout(this);
        frame.setBackgroundColor(Color.parseColor("#3A31D2"));
        web = new WebView(this);
        web.setBackgroundColor(Color.parseColor("#F2F1FA"));
        frame.addView(web, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        setContentView(frame);
        if (Build.VERSION.SDK_INT < 35) getWindow().setStatusBarColor(Color.parseColor("#3A31D2"));
        frame.setOnApplyWindowInsetsListener((v, insets) -> {
            int l, t, r, b;
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                android.graphics.Insets ime = insets.getInsets(WindowInsets.Type.ime());
                l = bars.left; t = bars.top; r = bars.right; b = Math.max(bars.bottom, ime.bottom);
            } else {
                l = insets.getSystemWindowInsetLeft(); t = insets.getSystemWindowInsetTop();
                r = insets.getSystemWindowInsetRight(); b = insets.getSystemWindowInsetBottom();
            }
            v.setPadding(l, t, r, b);
            return insets;
        });

        // Back gesture / back button: go back inside NOBTK first, then leave the app.
        if (Build.VERSION.SDK_INT >= 33) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT, () -> {
                if (web.canGoBack()) web.goBack(); else finish();
            });
        }

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String host = uri.getHost();
                // Stay inside the app for NOBTK pages; open WhatsApp, phone and other sites outside.
                if (host != null && host.endsWith("github.io") && "https".equals(uri.getScheme())) return false;
                try { startActivity(new Intent(Intent.ACTION_VIEW, uri)); } catch (Exception ignored) { }
                return true;
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) {
                    view.loadDataWithBaseURL(null,
                        "<html><body style='font-family:sans-serif;background:#F2F1FA;color:#15143A;display:flex;flex-direction:column;align-items:center;justify-content:center;height:90vh;text-align:center;padding:24px'>"
                        + "<h2>Pas de connexion internet</h2><p>NOBTK a besoin d'internet pour suivre la file en direct.</p>"
                        + "<button onclick=\"location.href='" + BuildConfig.START_URL + "'\" style='background:#3A31D2;color:#fff;border:0;border-radius:12px;padding:14px 22px;font-size:16px'>Réessayer</button>"
                        + "</body></html>", "text/html", "UTF-8", null);
                }
            }
        });

        // Without this, tapping "Choisir une photo" does nothing inside an Android WebView.
        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (pickCallback != null) pickCallback.onReceiveValue(null);
                pickCallback = callback;
                Intent pick = new Intent(Intent.ACTION_GET_CONTENT);
                pick.addCategory(Intent.CATEGORY_OPENABLE);
                pick.setType("image/*");
                try {
                    startActivityForResult(Intent.createChooser(pick, "Choisir une photo"), PICK_FILE);
                } catch (Exception e) {
                    pickCallback = null;
                    callback.onReceiveValue(null);
                    Toast.makeText(MainActivity.this, "Impossible d'ouvrir les photos.", Toast.LENGTH_LONG).show();
                    return false;
                }
                return true;
            }
        });

        // Lets the page save PDFs it builds (sign-up forms) into the phone's Downloads folder.
        web.addJavascriptInterface(new FileBridge(), "NobtkAndroid");
        // Plain file links (the blank PDF form) open in the phone's browser, which downloads them.
        web.setDownloadListener((url, ua, cd, mime, len) -> {
            try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); } catch (Exception ignored) { }
        });

        if (savedInstanceState != null) web.restoreState(savedInstanceState);
        else web.loadUrl(BuildConfig.START_URL);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_FILE) {
            if (pickCallback != null) {
                Uri[] result = null;
                if (resultCode == RESULT_OK && data != null && data.getData() != null) result = new Uri[]{ data.getData() };
                pickCallback.onReceiveValue(result);
                pickCallback = null;
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        int k = e.getKeyCode();
        boolean up = k == KeyEvent.KEYCODE_VOLUME_UP || k == KeyEvent.KEYCODE_ENTER || k == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE || k == KeyEvent.KEYCODE_CAMERA;
        boolean down = k == KeyEvent.KEYCODE_VOLUME_DOWN;
        if (remote && web != null && (up || down)) {
            if (e.getAction() == KeyEvent.ACTION_DOWN && e.getRepeatCount() == 0) {
                web.evaluateJavascript("window.NOBTK_KEY&&window.NOBTK_KEY('" + (up ? "up" : "down") + "')", null);
            }
            return true;
        }
        return super.dispatchKeyEvent(e);
    }

    private class FileBridge {
        @JavascriptInterface
        public void setRemote(boolean on) { remote = on; }

        // Phone notifications for clients ("ton tour arrive"), since a WebView has no Notification API.
        @JavascriptInterface
        public String notifyPermission() {
            if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return "default";
            NotificationManager nm = getSystemService(NotificationManager.class);
            return (nm != null && !nm.areNotificationsEnabled()) ? "denied" : "granted";
        }

        @JavascriptInterface
        public void requestNotify() {
            runOnUiThread(() -> {
                if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{ Manifest.permission.POST_NOTIFICATIONS }, ASK_NOTIF);
                } else {
                    tellPage(notifyPermission());
                }
            });
        }

        @JavascriptInterface
        public void notify(String title, String body) {
            try {
                NotificationManager nm = getSystemService(NotificationManager.class);
                if (nm == null) return;
                Notification.Builder b;
                if (Build.VERSION.SDK_INT >= 26) {
                    NotificationChannel ch = new NotificationChannel(CHANNEL, "Alertes de la file", NotificationManager.IMPORTANCE_HIGH);
                    ch.setDescription("Préviens quand ton tour approche");
                    ch.enableVibration(true);
                    nm.createNotificationChannel(ch);
                    b = new Notification.Builder(MainActivity.this, CHANNEL);
                } else {
                    b = new Notification.Builder(MainActivity.this).setPriority(Notification.PRIORITY_HIGH).setDefaults(Notification.DEFAULT_ALL);
                }
                Intent open = new Intent(MainActivity.this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                PendingIntent pi = PendingIntent.getActivity(MainActivity.this, 0, open, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
                b.setSmallIcon(R.drawable.ic_stat_nobtk).setContentTitle(title).setContentText(body)
                    .setStyle(new Notification.BigTextStyle().bigText(body))
                    .setColor(Color.parseColor("#3A31D2")).setAutoCancel(true).setContentIntent(pi);
                nm.notify(1, b.build());
            } catch (Exception ignored) { }
        }

        @JavascriptInterface
        public void saveFile(String name, String base64, String mime) {
            String safe = name.replaceAll("[^A-Za-z0-9._-]", "_");
            try {
                byte[] data = Base64.decode(base64, Base64.DEFAULT);
                Uri saved;
                if (Build.VERSION.SDK_INT >= 29) {
                    ContentValues v = new ContentValues();
                    v.put(MediaStore.Downloads.DISPLAY_NAME, safe);
                    v.put(MediaStore.Downloads.MIME_TYPE, mime);
                    v.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                    saved = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
                    try (OutputStream out = getContentResolver().openOutputStream(saved)) { out.write(data); }
                } else {
                    File dir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
                    File f = new File(dir, safe);
                    try (FileOutputStream out = new FileOutputStream(f)) { out.write(data); }
                    saved = null;
                }
                final Uri open = saved;
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "PDF enregistré dans Téléchargements : " + safe, Toast.LENGTH_LONG).show();
                    if (open != null) {
                        try {
                            Intent i = new Intent(Intent.ACTION_VIEW);
                            i.setDataAndType(open, mime);
                            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            startActivity(i);
                        } catch (Exception ignored) { }
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Le PDF n'a pas pu être enregistré.", Toast.LENGTH_LONG).show());
            }
        }
    }

    private void tellPage(String state) {
        if (web != null) web.evaluateJavascript("window.NOBTK_NOTIF&&window.NOBTK_NOTIF('" + state + "')", null);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        if (requestCode == ASK_NOTIF) {
            tellPage(results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED ? "granted" : "denied");
            return;
        }
        super.onRequestPermissionsResult(requestCode, permissions, results);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        web.saveState(outState);
    }

    // Back button on Android 12 and older.
    @Override
    public void onBackPressed() {
        if (web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }
}
