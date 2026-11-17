package org.telegram.ugram;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.utils.proxy.ProxySettings;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Getting through Telegram blocks without a VPN:
 * - packet splitting: the first packet of every connection leaves as several small TCP segments
 *   (done in tgnet's ConnectionSocket, like zapret's "split"), so DPI that matches the first packet misses it;
 * - auto proxy: when Telegram can't connect, public MTProxy servers are fetched, checked and the fastest is turned on.
 */
public final class UgramDpi {

    public static final String SPLIT = "dpiSplit";
    public static final String AUTO_PROXY = "dpiAutoProxy";

    private static final String[] SOURCES = {
            "https://raw.githubusercontent.com/SoliSpirit/mtproto/master/all_proxies.txt",
            "https://raw.githubusercontent.com/ALIILAPRO/MTProtoProxy/main/mtproto.txt",
            "https://raw.githubusercontent.com/Grim1313/mtproto-for-telegram/master/all_proxies.txt",
    };
    private static final Pattern LINK = Pattern.compile("server=([A-Za-z0-9.\\-]+)&port=(\\d+)&secret=([A-Za-z0-9_\\-+/=]+)");
    private static final int MAX_CHECKS = 40;
    private static final long CHECK_TIMEOUT = 12000;
    private static final long STUCK_DELAY = 20000;
    private static final long MIN_INTERVAL = 120000;

    public interface Result {
        void run(boolean success, String message);
    }

    private static boolean searching;
    private static long lastSearch;
    private static final Runnable[] stuckChecks = new Runnable[16];

    private UgramDpi() {
    }

    private static SharedPreferences prefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences("ugram_dpi", Context.MODE_PRIVATE);
    }

    private static boolean ru() {
        return UgramConfig.isRussian();
    }

    // region Packet splitting

    /** Passes the "split packets" setting to the network core. Called at start-up and when toggled. */
    public static void applySplit() {
        try {
            ConnectionsManager.native_setDpiSplitMode(UgramConfig.get(SPLIT) ? 1 : 0);
        } catch (Throwable ignore) {
        }
    }

    // endregion

    // region Auto proxy

    public static boolean isAutoProxy(SharedConfig.ProxyInfo info) {
        return info != null && prefs().getStringSet("auto", Collections.emptySet()).contains(key(info.settings));
    }

    private static String key(ProxySettings s) {
        return s.getAddress() + ":" + s.getPort() + ":" + s.getSecret();
    }

    /** Called on every connection state change (UI thread). */
    public static void onConnectionState(int account, int state) {
        if (account < 0 || account >= stuckChecks.length) {
            return;
        }
        if (stuckChecks[account] != null) {
            AndroidUtilities.cancelRunOnUIThread(stuckChecks[account]);
            stuckChecks[account] = null;
        }
        if (!UgramConfig.get(AUTO_PROXY)) {
            return;
        }
        if (state != ConnectionsManager.ConnectionStateConnecting && state != ConnectionsManager.ConnectionStateConnectingToProxy) {
            return;
        }
        Runnable check = () -> {
            stuckChecks[account] = null;
            int now = ConnectionsManager.getInstance(account).getConnectionState();
            if (now != ConnectionsManager.ConnectionStateConnecting && now != ConnectionsManager.ConnectionStateConnectingToProxy) {
                return;
            }
            if (!ApplicationLoader.isNetworkOnline() || searching || SystemClock.elapsedRealtime() - lastSearch < MIN_INTERVAL) {
                return;
            }
            // Never replace a proxy the user added by hand.
            if (SharedConfig.isProxyEnabled() && !isAutoProxy(SharedConfig.currentProxy)) {
                return;
            }
            findAndEnable(account, null);
        };
        stuckChecks[account] = check;
        AndroidUtilities.runOnUIThread(check, STUCK_DELAY);
    }

    /** Downloads public MTProxy lists, checks them and turns on the fastest. Result runs on the UI thread. */
    public static void findAndEnable(int account, Result result) {
        if (searching) {
            if (result != null) {
                result.run(false, ru() ? "Поиск уже идёт…" : "Already searching…");
            }
            return;
        }
        searching = true;
        lastSearch = SystemClock.elapsedRealtime();
        Utilities.globalQueue.postRunnable(() -> {
            ArrayList<ProxySettings> candidates = loadCandidates();
            AndroidUtilities.runOnUIThread(() -> check(account, candidates, result));
        });
    }

    private static ArrayList<ProxySettings> loadCandidates() {
        StringBuilder all = new StringBuilder();
        for (String source : SOURCES) {
            String text = download(source);
            if (text != null) {
                all.append(text).append('\n');
            }
        }
        if (all.length() > 0) {
            prefs().edit().putString("cache", all.length() > 200000 ? all.substring(0, 200000) : all.toString()).apply();
        } else {
            all.append(prefs().getString("cache", ""));
        }
        LinkedHashMap<String, ProxySettings> unique = new LinkedHashMap<>();
        Matcher m = LINK.matcher(all);
        while (m.find()) {
            String host = m.group(1);
            while (host.endsWith(".")) {
                host = host.substring(0, host.length() - 1);
            }
            int port;
            try {
                port = Integer.parseInt(m.group(2));
            } catch (NumberFormatException e) {
                continue;
            }
            ProxySettings s = ProxySettings.builder()
                    .setType(ProxySettings.Type.MTPROTO)
                    .setAddress(host)
                    .setPort(port)
                    .setUser("")
                    .setPassword("")
                    .setSecret(m.group(3))
                    .build();
            if (s.isValid()) {
                unique.put(key(s), s);
            }
        }
        ArrayList<ProxySettings> fakeTls = new ArrayList<>();
        ArrayList<ProxySettings> other = new ArrayList<>();
        for (ProxySettings s : unique.values()) {
            String secret = s.getSecret();
            // FakeTLS secrets ("ee…" in hex, "7…" in base64) look like normal HTTPS to DPI: try them first.
            if (secret.startsWith("ee") || secret.startsWith("7")) {
                fakeTls.add(s);
            } else {
                other.add(s);
            }
        }
        Collections.shuffle(fakeTls);
        Collections.shuffle(other);
        ArrayList<ProxySettings> result = new ArrayList<>(fakeTls);
        result.addAll(other);
        while (result.size() > MAX_CHECKS) {
            result.remove(result.size() - 1);
        }
        return result;
    }

    private static String download(String url) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(8000);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            if (connection.getResponseCode() != 200) {
                return null;
            }
            try (InputStream in = connection.getInputStream()) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) > 0 && out.size() < 500000) {
                    out.write(buffer, 0, read);
                }
                return out.toString("UTF-8");
            }
        } catch (Throwable e) {
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static void check(int account, ArrayList<ProxySettings> candidates, Result result) {
        if (candidates.isEmpty()) {
            searching = false;
            if (result != null) {
                result.run(false, ru() ? "Не удалось загрузить список прокси. Проверьте интернет." : "Couldn't load the proxy list. Check your internet connection.");
            }
            return;
        }
        final ProxySettings[] best = {null};
        final long[] bestPing = {Long.MAX_VALUE};
        final int[] pending = {candidates.size()};
        final boolean[] done = {false};
        Runnable finish = () -> {
            if (done[0]) {
                return;
            }
            done[0] = true;
            searching = false;
            if (best[0] == null) {
                if (result != null) {
                    result.run(false, ru() ? "Рабочих прокси не нашлось. Попробуйте позже." : "No working proxy found. Try again later.");
                }
                return;
            }
            enable(best[0], bestPing[0]);
            if (result != null) {
                result.run(true, (ru() ? "Подключено через " : "Connected via ") + best[0].getAddress() + " (" + bestPing[0] + " ms)");
            }
        };
        AndroidUtilities.runOnUIThread(finish, CHECK_TIMEOUT);
        for (ProxySettings s : candidates) {
            ConnectionsManager.getInstance(account).checkProxy(s, time -> AndroidUtilities.runOnUIThread(() -> {
                if (done[0]) {
                    return;
                }
                if (time > 0 && time < bestPing[0]) {
                    bestPing[0] = time;
                    best[0] = s;
                }
                pending[0]--;
                // Stop early once every check answered, or a fast proxy turned up.
                if (pending[0] <= 0 || bestPing[0] < 400) {
                    AndroidUtilities.cancelRunOnUIThread(finish);
                    finish.run();
                }
            }));
        }
    }

    private static void enable(ProxySettings settings, long ping) {
        SharedConfig.ProxyInfo previous = SharedConfig.currentProxy;
        SharedConfig.ProxyInfo info = SharedConfig.addProxy(new SharedConfig.ProxyInfo(settings));
        info.ping = ping;
        info.available = true;
        info.availableCheckTime = SystemClock.elapsedRealtime();

        HashSet<String> auto = new HashSet<>(prefs().getStringSet("auto", Collections.emptySet()));
        auto.add(key(settings));
        prefs().edit().putStringSet("auto", auto).apply();

        SharedPreferences.Editor editor = MessagesController.getGlobalMainSettings().edit();
        settings.toSharedPreferences(editor);
        editor.putBoolean("proxy_enabled", true);
        editor.commit();
        SharedConfig.currentProxy = info;
        ConnectionsManager.setProxySettings(true, settings);
        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.proxySettingsChanged);

        // Keep the proxy list tidy: drop the auto proxy this one replaces.
        if (previous != null && previous != info && isAutoProxy(previous)) {
            SharedConfig.deleteProxy(previous);
            auto.remove(key(previous.settings));
            prefs().edit().putStringSet("auto", auto).apply();
        }
    }

    public static boolean proxyOn() {
        return SharedConfig.isProxyEnabled();
    }

    public static String currentProxyName() {
        SharedConfig.ProxyInfo info = SharedConfig.currentProxy;
        if (!proxyOn() || info == null) {
            return ru() ? "Выключен" : "Off";
        }
        return info.settings.getAddress();
    }

    public static void disableProxy() {
        SharedPreferences.Editor editor = MessagesController.getGlobalMainSettings().edit();
        editor.putBoolean("proxy_enabled", false);
        editor.commit();
        ConnectionsManager.setProxySettings(false, null);
        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.proxySettingsChanged);
    }

    // endregion
}
