package onion.fire;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;

import org.mozilla.gecko.PrefsHelper;

import java.util.Locale;

public class Prefs {

    static String TAG = "Prefs";

    static Thread.UncaughtExceptionHandler h;

    static {
        h = Thread.getDefaultUncaughtExceptionHandler();
    }

    public static void init(Activity context) {
        Thread.setDefaultUncaughtExceptionHandler(h);
    }


    public static void setPrefs(Activity context) {

        PrefsHelper.setPref("app.update.autodownload", "never");
        PrefsHelper.setPref("browser.cache.disk.capacity", 0);
        PrefsHelper.setPref("browser.cache.disk.enable", false);
        PrefsHelper.setPref("browser.cache.disk.max_entry_size", 0);
        PrefsHelper.setPref("browser.cache.disk.smart_size.enabled", false);
        PrefsHelper.setPref("browser.cache.disk.smart_size.first_run", false);
        PrefsHelper.setPref("browser.cache.memory.enable", true);
        PrefsHelper.setPref("browser.cache.offline.capacity", 0);
        PrefsHelper.setPref("browser.cache.offline.enable", false);
        PrefsHelper.setPref("browser.chrome.favicons", false);
        PrefsHelper.setPref("browser.download.manager.addToRecentDocs", false);
        PrefsHelper.setPref("browser.download.manager.retention", 1);
        PrefsHelper.setPref("browser.download.manager.scanWhenDone", false);
        PrefsHelper.setPref("browser.gesture.pinch.in", "cmd_fullZoomReduce");
        PrefsHelper.setPref("browser.gesture.pinch.out", "cmd_fullZoomEnlarge");
        PrefsHelper.setPref("browser.privatebrowsing.autostart", true);
        PrefsHelper.setPref("browser.safebrowsing.downloads.enabled", false);
        PrefsHelper.setPref("browser.safebrowsing.enabled", false);
        PrefsHelper.setPref("browser.safebrowsing.malware.enabled", false);
        PrefsHelper.setPref("browser.search.geoip.timeout", 1);
        PrefsHelper.setPref("browser.selfsupport.url", "");
        PrefsHelper.setPref("browser.sessionstore.enabled", false);
        PrefsHelper.setPref("datareporting.healthreport.service.enabled", false);
        PrefsHelper.setPref("datareporting.healthreport.uploadEnabled", false);
        PrefsHelper.setPref("datareporting.policy.dataSubmissionEnabled", false);
        PrefsHelper.setPref("devtools.debugger.remote-enabled", false);
        PrefsHelper.setPref("devtools.webide.autoinstallADBHelper", false);
        PrefsHelper.setPref("devtools.webide.autoinstallFxdtAdapters", false);
        PrefsHelper.setPref("devtools.webide.enabled", false);
        PrefsHelper.setPref("dom.enable_performance", false);
        PrefsHelper.setPref("dom.enable_resource_timing", false);
        PrefsHelper.setPref("dom.enable_user_timing", false);
        PrefsHelper.setPref("geo.enabled", false);
        PrefsHelper.setPref("geo.wifi.uri", "");
        PrefsHelper.setPref("keyword.enabled", false);
        PrefsHelper.setPref("media.peerconnection.enabled", false); // webrtc disabled
        PrefsHelper.setPref("network.dns.disablePrefetch", true);
        PrefsHelper.setPref("network.prefetch-next", false);
        PrefsHelper.setPref("network.protocol-handler.external-default", false);
        PrefsHelper.setPref("network.protocol-handler.external.mailto", false);
        PrefsHelper.setPref("network.protocol-handler.external.news", false);
        PrefsHelper.setPref("network.protocol-handler.external.nntp", false);
        PrefsHelper.setPref("network.protocol-handler.external.snews", false);
        PrefsHelper.setPref("network.proxy.socks", "127.0.0.1"); // manual proxy settings
        PrefsHelper.setPref("network.proxy.socks_port", 4); // manual proxy settings
        PrefsHelper.setPref("network.proxy.socks_remote_dns", true);
        PrefsHelper.setPref("network.proxy.socks_version", 5); // manual proxy settings
        PrefsHelper.setPref("network.proxy.type", 1); // manual proxy settings
        PrefsHelper.setPref("plugin.disable", true);
        PrefsHelper.setPref("privacy.clearOnShutdown.cache", true);
        PrefsHelper.setPref("privacy.clearOnShutdown.cookies", true);
        PrefsHelper.setPref("privacy.clearOnShutdown.downloads", true);
        PrefsHelper.setPref("privacy.clearOnShutdown.formdata", true);
        PrefsHelper.setPref("privacy.clearOnShutdown.history", true);
        PrefsHelper.setPref("privacy.clearOnShutdown.offlineApps", true);
        PrefsHelper.setPref("privacy.clearOnShutdown.passwords", true);
        PrefsHelper.setPref("privacy.clearOnShutdown.sessions", true);
        PrefsHelper.setPref("privacy.clearOnShutdown.siteSettings", true);
        PrefsHelper.setPref("security.checkloaduri", true);
        PrefsHelper.setPref("security.ssl3.ecdh_ecdsa_rc4_128_sha", false);
        PrefsHelper.setPref("security.ssl3.ecdhe_ecdsa_rc4_128_sha", false);
        PrefsHelper.setPref("security.ssl3.ecdhe_rsa_rc4_128_sha", false);
        PrefsHelper.setPref("security.ssl3.ecdh_rsa_rc4_128_sha", false);
        PrefsHelper.setPref("security.ssl3.rsa_rc4_128_md5", false);
        PrefsHelper.setPref("security.ssl3.rsa_rc4_128_sha", false);

        init(context);
        applyPrefs(context);

    }

    public static String getUserAgent(Activity context) {
        SharedPreferences p = Settings.getPrefs(context);
        if (!p.getBoolean("mobileuseragent", false)) {
            return "Mozilla/5.0 (Windows NT 6.1; rv:38.0) Gecko/20100101 Firefox/38.0";
        } else {
            return "Mozilla/5.0 (Android 5.0; Mobile; rv:38.0) Gecko/20100101 Firefox/38.0";
        }
    }

    public static void applyPrefs(Activity context) {

        Log.i(TAG, "applyPrefs");

        SharedPreferences p = Settings.getPrefs(context);

        if (p.getBoolean("allowscreenshots", true) == false) {
            context.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        } else {
            context.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }

        PrefsHelper.setPref("javascript.enabled", p.getBoolean("javascript", true));

        PrefsHelper.setPref("media.peerconnection.enabled", p.getBoolean("webrtc", false)); //webrtc disabled

        PrefsHelper.setPref("general.useragent.locale", "en-us");
        if (p.getBoolean("localization", false)) {
            Locale loc = Locale.getDefault();
            String str = loc.getLanguage().toLowerCase() + "-" + loc.getCountry().toUpperCase() + ", en";
            Log.i("Locale", str);
            PrefsHelper.setPref("intl.accept_languages", str);
        } else {
            PrefsHelper.setPref("intl.accept_languages", "en-US,en;q=0.5");
        }

        PrefsHelper.setPref("general.useragent.override", getUserAgent(context));

    }

}
