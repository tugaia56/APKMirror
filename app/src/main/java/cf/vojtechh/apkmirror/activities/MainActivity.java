package cf.vojtechh.apkmirror.activities;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.DownloadManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.Window;
import android.view.animation.DecelerateInterpolator;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.URLUtil;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import cf.vojtechh.apkmirror.R;
import cf.vojtechh.apkmirror.classes.SearchDialog;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private ProgressBar progressBar;
    private BottomNavigationView navigation;
    private FloatingActionButton fabSearch;

    private SwipeRefreshLayout refreshLayout;
    private View settingsLayoutFragment;
    private View webContainer;
    private View progressBarContainer;
    private View firstLoadingView;

    private static final String APKMIRROR_URL = "https://www.apkmirror.com/";
    private static final String APKMIRROR_UPLOAD_URL = "https://www.apkmirror.com/apk-upload/";
    private static final int APKMIRROR_ORANGE = Color.parseColor("#FF8B14");

    private int shortAnimDuration;
    private int previsionThemeColor = APKMIRROR_ORANGE; // set to defaultColor() in onCreate

    private SharedPreferences sharedPreferences;

    private boolean settingsShortcut = false;
    private boolean triggerAction = true;

    private ValueCallback<Uri[]> filePathCallback;
    private String pendingDownloadUrl, pendingDownloadName, pendingDownloadAgent;

    private final ActivityResultLauncher<Intent> fileChooser = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (filePathCallback == null) return;
                filePathCallback.onReceiveValue(
                        WebChromeClient.FileChooserParams.parseResult(result.getResultCode(), result.getData()));
                filePathCallback = null;
            });

    private final ActivityResultLauncher<String> storagePermission = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted && pendingDownloadUrl != null) {
                    startDownload(pendingDownloadUrl, pendingDownloadName, pendingDownloadAgent);
                } else if (!granted) {
                    Toast.makeText(this, getString(R.string.storage_denied), Toast.LENGTH_LONG).show();
                }
                pendingDownloadUrl = null;
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setTheme(R.style.AppTheme);
            setContentView(R.layout.activity_main);

            //Views
            refreshLayout = findViewById(R.id.refresh_layout);
            progressBar = findViewById(R.id.main_progress_bar);
            navigation = findViewById(R.id.navigation);
            settingsLayoutFragment = findViewById(R.id.settings_layout_fragment);
            webContainer = findViewById(R.id.web_container);
            firstLoadingView = findViewById(R.id.first_loading_view);
            webView = findViewById(R.id.main_webview);
            fabSearch = findViewById(R.id.fab_search);
            progressBarContainer = findViewById(R.id.main_progress_bar_container);
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

            initSearchFab();
            shortAnimDuration = getResources().getInteger(android.R.integer.config_shortAnimTime);

            initNavigation();
            previsionThemeColor = defaultColor();
            applyNavigationColor(previsionThemeColor);

            boolean saveUrl = sharedPreferences.getBoolean("save_url", false);

            String url;

            Intent link = getIntent();
            Uri data = link.getData();

            String savedUrl = savedInstanceState == null ? null : savedInstanceState.getString("url");
            boolean savedSettings = savedInstanceState != null && savedInstanceState.getBoolean("settings", false);

            if (savedUrl != null) {
                //the activity was recreated (theme or language changed): come back to the same page
                url = savedUrl;
                if (savedSettings) {
                    triggerAction = false;
                    navigation.setSelectedItemId(R.id.navigation_settings);
                    crossFade(webContainer, settingsLayoutFragment);
                    settingsShortcut = true;
                }
            } else if (data != null) {
                //App was opened from browser
                url = data.toString();
            } else {
                //data is null which means it was either launched from shortcuts or normally
                Bundle bundle = link.getExtras();
                String bundleUrl = bundle == null ? null : bundle.getString("url");
                if (bundleUrl != null) {
                    if (bundleUrl.equals("apkmirror://settings")) {
                        //It was the settings shortcut
                        url = APKMIRROR_URL;
                        triggerAction = false;
                        navigation.setSelectedItemId(R.id.navigation_settings);
                        crossFade(webContainer, settingsLayoutFragment);
                        settingsShortcut = true;
                    } else {
                        url = bundleUrl;
                    }
                } else if (saveUrl) {
                    url = sharedPreferences.getString("last_url", APKMIRROR_URL);
                } else {
                    url = APKMIRROR_URL;
                }
            }

            initWebView(url);
            initBackButton();

            if (!settingsShortcut) {
                firstLoadingView.setVisibility(View.VISIBLE);
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (firstLoadingView.getVisibility() == View.VISIBLE) {
                        crossFade(firstLoadingView, webContainer);
                    }
                }, 2000);
            }

        } catch (final RuntimeException e) {

            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.error)
                    .setMessage(R.string.runtime_error_dialog_content)
                    .setPositiveButton(android.R.string.ok, (d, w) -> finish())
                    .setNeutralButton(R.string.copy_log, (d, w) -> {
                        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                        if (clipboard != null) {
                            clipboard.setPrimaryClip(ClipData.newPlainText("log", e.toString()));
                        } else {
                            Toast.makeText(MainActivity.this, getString(R.string.clip_error), Toast.LENGTH_LONG).show();
                        }
                    })
                    .show();
        }
    }

    private void initNavigation() {

        // the exit button is an extra item: swap the menu BEFORE the listeners exist
        if (sharedPreferences.getBoolean("show_exit", false)) {
            navigation.getMenu().clear();
            navigation.inflateMenu(R.menu.navigation_exit);
        }

        navigation.setOnItemSelectedListener(item -> {
            onTabSelected(item.getItemId());
            return true;
        });
        navigation.setOnItemReselectedListener(item -> {
            if (!triggerAction) {
                // selected from the code (not by a tap): nothing to do
                triggerAction = true;
                return;
            }
            onTabReselected(item.getItemId());
        });
    }

    private void initSearchFab() {
        boolean fab = sharedPreferences.getBoolean("fab", true);
        if (fab) {
            fabSearch.show();
            fabSearch.setOnClickListener(v -> search());
        }
    }

    private void initWebView(String url) {

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(true);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        applyWebViewDarkMode(settings);

        webView.setWebViewClient(webClient);
        webView.setWebChromeClient(chromeClient);
        webView.setDownloadListener((downloadUrl, userAgent, contentDisposition, mimeType, contentLength) ->
                onDownloadRequested(downloadUrl, URLUtil.guessFileName(downloadUrl, contentDisposition, mimeType), userAgent));
        webView.loadUrl(url);

        refreshLayout.setOnRefreshListener(() -> webView.reload());
    }

    /**
     * Dark theme: when the app is dark, the site shows its own dark theme (apkmirror.com follows
     * prefers-color-scheme); a page that has no dark theme is darkened by the WebView itself.
     */
    private void applyWebViewDarkMode(WebSettings settings) {
        boolean night = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
        if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
            WebSettingsCompat.setAlgorithmicDarkeningAllowed(settings, night);
        } else if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
            WebSettingsCompat.setForceDark(settings,
                    night ? WebSettingsCompat.FORCE_DARK_ON : WebSettingsCompat.FORCE_DARK_OFF);
            if (night && WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK_STRATEGY)) {
                WebSettingsCompat.setForceDarkStrategy(settings,
                        WebSettingsCompat.DARK_STRATEGY_PREFER_WEB_THEME_OVER_USER_AGENT_DARKENING);
            }
        }
    }

    private void initBackButton() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (settingsLayoutFragment.getVisibility() == View.VISIBLE) {
                    crossFade(settingsLayoutFragment, webContainer);
                    triggerAction = false;
                    String current = webView.getUrl();
                    navigation.setSelectedItemId(APKMIRROR_UPLOAD_URL.equals(current)
                            ? R.id.navigation_upload : R.id.navigation_home);
                } else if (webView.canGoBack()) {
                    webView.goBack();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        String current = webView == null ? null : webView.getUrl();
        if (current != null) outState.putString("url", current);
        outState.putBoolean("settings", settingsLayoutFragment != null
                && settingsLayoutFragment.getVisibility() == View.VISIBLE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
    }

    @Override
    protected void onPause() {
        webView.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        webView.stopLoading();
        webView.destroy();
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        String current = webView.getUrl();
        if (sharedPreferences.getBoolean("save_url", false) && current != null
                && !current.equals("apkmirror://settings")) {
            sharedPreferences.edit().putString("last_url", current).apply();
        }
        super.onStop();
    }

    private void search() {
        SearchDialog.show(this, url -> webView.loadUrl(url), null);
    }

    private void onTabReselected(int tabId) {
        int webScrollY = webView.getScrollY();

        if (tabId == R.id.navigation_home) {
            if (webScrollY != 0) {
                webView.setScrollY(0);
            } else {
                webView.loadUrl(APKMIRROR_URL);
            }
        } else if (tabId == R.id.navigation_upload) {
            if (webScrollY != 0) {
                webView.setScrollY(0);
            } else {
                webView.loadUrl(APKMIRROR_UPLOAD_URL);
            }
        }
    }

    private void onTabSelected(int tabId) {

        if (triggerAction) {

            if (tabId == R.id.navigation_home) {
                if (settingsLayoutFragment.getVisibility() != View.VISIBLE) {
                    webView.loadUrl(APKMIRROR_URL);
                } else {
                    //settings is visible, gonna hide it
                    if (APKMIRROR_UPLOAD_URL.equals(webView.getUrl())) {
                        webView.loadUrl(APKMIRROR_URL);
                    }
                    crossFade(settingsLayoutFragment, webContainer);
                    changeUIColor(previsionThemeColor);
                }
            } else if (tabId == R.id.navigation_upload) {
                if (settingsLayoutFragment.getVisibility() != View.VISIBLE) {
                    webView.loadUrl(APKMIRROR_UPLOAD_URL);
                } else {
                    if (!APKMIRROR_UPLOAD_URL.equals(webView.getUrl())) {
                        webView.loadUrl(APKMIRROR_UPLOAD_URL);
                    }
                    crossFade(settingsLayoutFragment, webContainer);
                    changeUIColor(previsionThemeColor);
                }
            } else if (tabId == R.id.navigation_settings) {
                if (firstLoadingView.getVisibility() == View.VISIBLE) {
                    firstLoadingView.setVisibility(View.GONE);
                }
                crossFade(webContainer, settingsLayoutFragment);
                changeUIColor(ContextCompat.getColor(MainActivity.this, R.color.colorPrimary));
            } else if (tabId == R.id.navigation_exit) {
                finish();
            }
        }

        triggerAction = true;
    }

    private void crossFade(final View toHide, View toShow) {

        toShow.setAlpha(0f);
        toShow.setVisibility(View.VISIBLE);

        toShow.animate()
                .alpha(1f)
                .setDuration(shortAnimDuration)
                .setListener(null);

        toHide.animate()
                .alpha(0f)
                .setDuration(shortAnimDuration)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        toHide.setVisibility(View.GONE);
                    }
                });
    }

    // ── Downloads ────────────────────────────────────────────────────────────

    private void onDownloadRequested(String url, String name, String userAgent) {
        if (sharedPreferences.getBoolean("external_download", false)) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            return;
        }
        if (isWritePermissionGranted()) {
            startDownload(url, name, userAgent);
        } else {
            pendingDownloadUrl = url;
            pendingDownloadName = name;
            pendingDownloadAgent = userAgent;
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.write_permission)
                    .setMessage(R.string.storage_access)
                    .setPositiveButton(R.string.request_permission,
                            (d, w) -> storagePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE))
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        }
    }

    /** Android 10+ needs no storage permission: the DownloadManager saves into Downloads by itself. */
    private boolean isWritePermissionGranted() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void startDownload(String url, String name, String userAgent) {
        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setTitle(name);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name);
            String cookies = CookieManager.getInstance().getCookie(url);
            if (cookies != null) request.addRequestHeader("Cookie", cookies);
            if (userAgent != null) request.addRequestHeader("User-Agent", userAgent);
            DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            if (dm == null) throw new IllegalStateException("no download manager");
            dm.enqueue(request);
            Toast.makeText(this, getString(R.string.download_started), Toast.LENGTH_SHORT).show();
        } catch (RuntimeException e) {
            Toast.makeText(this, getString(R.string.cant_download), Toast.LENGTH_SHORT).show();
        }
    }

    // ── Colours: the page colour (meta theme-color) tints the app bars ──────

    private void onThemeColor(int themeColor) {
        changeUIColor(themeColor);
        previsionThemeColor = themeColor;
    }

    private void applyNavigationColor(int color) {
        int[][] states = {{android.R.attr.state_checked}, {}};
        ColorStateList tint = new ColorStateList(states,
                new int[]{color, ContextCompat.getColor(this, R.color.inactive_icon)});
        navigation.setItemIconTintList(tint);
        navigation.setItemTextColor(tint);
    }

    private void changeUIColor(int color) {

        ValueAnimator anim = ValueAnimator.ofArgb(previsionThemeColor, color);
        anim.setEvaluator(new ArgbEvaluator());

        anim.addUpdateListener(valueAnimator -> {
            int value = (Integer) valueAnimator.getAnimatedValue();
            progressBar.setProgressTintList(ColorStateList.valueOf(value));
            setSystemBarColor(value);
            applyNavigationColor(value);
            fabSearch.setBackgroundTintList(ColorStateList.valueOf(value));
        });

        anim.setDuration(getResources().getInteger(android.R.integer.config_shortAnimTime));
        anim.start();
        refreshLayout.setColorSchemeColors(color, color, color);
    }

    private void setSystemBarColor(int color) {

        int clr;

        if (isNight()) {
            // dark theme: dark status bar (same colour as the header of the site)
            clr = ContextCompat.getColor(this, R.color.bars_bg);
        } else if (usePageColor() && color != APKMIRROR_ORANGE) {
            float[] hsv = new float[3];
            Color.colorToHSV(color, hsv);
            hsv[2] *= 0.8f;
            clr = Color.HSVToColor(hsv);
        } else {
            clr = Color.parseColor("#F47D20");
        }

        Window window = getWindow();
        window.setStatusBarColor(clr);
    }

    // ── WebView ──────────────────────────────────────────────────────────────

    private boolean isNight() {
        return (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
    }

    /** CSS colour with alpha (the ripple colour of the Theme is translucent). */
    private String rgba(int colorRes) {
        int c = ContextCompat.getColor(this, colorRes);
        return String.format(java.util.Locale.US, "rgba(%d,%d,%d,%.3f)", Color.red(c), Color.green(c), Color.blue(c), Color.alpha(c) / 255f);
    }

    private String hex(int colorRes) {
        return String.format("#%06X", ContextCompat.getColor(this, colorRes) & 0xFFFFFF);
    }

    /** Colour of the bars when the page gives none (or when the Theme overlay asks to ignore the page colour). */
    private int defaultColor() {
        return ContextCompat.getColor(this, R.color.colorPrimary);
    }

    private boolean usePageColor() {
        return getResources().getBoolean(R.bool.use_page_color);
    }

    private String darkScript;

    /** Dark theme: assets/dark.js sets the page background (#121212), the lighter cards and the dark site header. */
    private void injectDarkHeader(WebView view) {
        if (!isNight()) return;
        if (darkScript == null) {
            try (java.io.InputStream in = getAssets().open("dark.js");
                 java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
                byte[] buf = new byte[4096];
                int n;
                while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                darkScript = out.toString("UTF-8");
            } catch (java.io.IOException e) {
                darkScript = "";
            }
        }
        if (!darkScript.isEmpty()) {
            String script = darkScript
                    .replace("%PAGE_BG%", hex(R.color.page_bg))
                    .replace("%CARD_BG%", hex(R.color.card_bg))
                    .replace("%CARD_BG_2%", hex(R.color.card_bg_2))
                    .replace("%CARD_BG_3%", hex(R.color.card_bg_3))
                    .replace("%HEADER_BG%", hex(R.color.header_bg))
                    .replace("%ACCENT%", hex(R.color.colorPrimary))
                    .replace("%INFO%", rgba(R.color.info_icon));
            view.evaluateJavascript(script, null);
        }
    }

    private String readAsset(String name) {
        try (java.io.InputStream in = getAssets().open(name);
             java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            return out.toString("UTF-8");
        } catch (java.io.IOException e) {
            return "";
        }
    }

    private String translateScript;

    /** Texts of the site (side menu) shown in the language of the app. */
    private void injectTranslations(WebView view) {
        if (translateScript == null) translateScript = readAsset("translate.js");
        if (translateScript.isEmpty()) return;
        try {
            org.json.JSONObject map = new org.json.JSONObject();
            map.put("All Developers", getString(R.string.menu_all_developers));
            map.put("All Categories", getString(R.string.menu_all_categories));
            map.put("FAQ", getString(R.string.menu_faq));
            map.put("Contact", getString(R.string.menu_contact));
            map.put("Premium", getString(R.string.menu_premium));
            view.evaluateJavascript(translateScript + "(" + map + ")", null);
        } catch (org.json.JSONException ignored) {
        }
    }

    private boolean isApkMirrorHost(Uri uri) {
        String host = uri.getHost();
        return host != null && (host.equals("apkmirror.com") || host.endsWith(".apkmirror.com"));
    }

    private final WebViewClient webClient = new WebViewClient() {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            Uri uri = request.getUrl();
            if (isApkMirrorHost(uri)) return false;
            // anything else (other sites, mailto:, market:...) opens outside of the app
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, uri));
            } catch (RuntimeException e) {
                Toast.makeText(MainActivity.this, uri.toString(), Toast.LENGTH_SHORT).show();
            }
            return true;
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            if (url.contains("apkmirror.com/wp-content/")) return;

            //Updating bottom navigation
            if (navigation.getSelectedItemId() == R.id.navigation_home) {
                if (url.equals(APKMIRROR_UPLOAD_URL)) {
                    triggerAction = false;
                    navigation.setSelectedItemId(R.id.navigation_upload);
                }
            } else if (navigation.getSelectedItemId() == R.id.navigation_upload) {
                if (!url.equals(APKMIRROR_UPLOAD_URL)) {
                    triggerAction = false;
                    navigation.setSelectedItemId(R.id.navigation_home);
                }
            }

            //Showing progress bar
            progressBarContainer.animate()
                    .alpha(1f)
                    .setDuration(getResources().getInteger(android.R.integer.config_shortAnimTime))
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationStart(Animator animation) {
                            super.onAnimationStart(animation);
                            progressBarContainer.setVisibility(View.VISIBLE);
                        }
                    });
        }

        @Override
        public void onPageCommitVisible(WebView view, String url) {
            injectDarkHeader(view);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            injectDarkHeader(view);
            injectTranslations(view);

            progressBarContainer.animate()
                    .alpha(0f)
                    .setDuration(getResources().getInteger(android.R.integer.config_longAnimTime))
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            progressBarContainer.setVisibility(View.GONE);
                        }
                    });

            if (refreshLayout.isRefreshing()) {
                refreshLayout.setRefreshing(false);
            }

            // the colour of the page (meta theme-color) tints the status bar, the bottom bar and the buttons
            view.evaluateJavascript("(function(){var m=document.querySelector('meta[name=theme-color]');"
                    + "return m?m.getAttribute('content'):'';})()", value -> {
                int color = defaultColor();
                if (value != null && usePageColor()) {
                    String c = value.replace("\"", "").trim();
                    if (!c.isEmpty() && !c.equals("null")) {
                        try {
                            color = Color.parseColor(c);
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                }
                if (settingsLayoutFragment.getVisibility() != View.VISIBLE) {
                    onThemeColor(color);
                } else {
                    previsionThemeColor = color;
                }
            });
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            if (!request.isForMainFrame()) return;
            int code = error.getErrorCode();
            if (code == ERROR_HOST_LOOKUP || code == ERROR_CONNECT || code == ERROR_TIMEOUT) {
                showLoadError(code, String.valueOf(error.getDescription()), request.getUrl().toString());
            }
        }
    };

    private void showLoadError(int errorCode, String description, String failingUrl) {
        if (isFinishing()) return;
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.error)
                .setMessage(getString(R.string.error_while_loading_page) + " " + failingUrl
                        + " (" + errorCode + " " + description + ")")
                .setPositiveButton(R.string.refresh, (d, w) -> webView.reload())
                .setNegativeButton(android.R.string.cancel, (d, w) -> finish())
                .setNeutralButton(R.string.dismiss, null)
                .show();
    }

    private final WebChromeClient chromeClient = new WebChromeClient() {

        @Override
        public void onProgressChanged(WebView view, int progress) {
            //update the progressbar value
            ObjectAnimator animation = ObjectAnimator.ofInt(progressBar, "progress", progress);
            animation.setDuration(100);
            animation.setInterpolator(new DecelerateInterpolator());
            animation.start();
        }

        @Override
        public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
            if (filePathCallback != null) filePathCallback.onReceiveValue(null);
            filePathCallback = callback;
            try {
                fileChooser.launch(params.createIntent());
            } catch (RuntimeException e) {
                filePathCallback = null;
                return false;
            }
            return true;
        }
    };

}
