package ma.nobtk.app;

import android.app.Activity;
import android.content.ContentValues;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.webkit.JavascriptInterface;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setStatusBarColor(Color.parseColor("#3A31D2"));

        web = new WebView(this);
        web.setBackgroundColor(Color.parseColor("#F2F1FA"));
        setContentView(web);

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

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        web.saveState(outState);
    }

    @Override
    public void onBackPressed() {
        if (web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }
}
