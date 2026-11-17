package org.telegram.ugram;

import android.content.Context;
import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;

/**
 * Pinned chats past the server limit, kept on this device only. The server never hears about them,
 * and they are pinned again locally whenever the chat list is re-sorted after a sync.
 */
public final class UgramPins {

    public static final String ENABLED = "unlimitedPins";

    private UgramPins() {
    }

    public static boolean enabled() {
        return UgramConfig.get(ENABLED);
    }

    private static SharedPreferences prefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences("ugram_pins", Context.MODE_PRIVATE);
    }

    private static String key(int account) {
        return "local" + account;
    }

    /** dialogId → pinnedNum, stored as "id:num,id:num". */
    private static synchronized ArrayList<long[]> load(int account) {
        ArrayList<long[]> result = new ArrayList<>();
        String value = prefs().getString(key(account), "");
        if (value.isEmpty()) {
            return result;
        }
        for (String part : value.split(",")) {
            String[] kv = part.split(":");
            if (kv.length != 2) {
                continue;
            }
            try {
                result.add(new long[]{Long.parseLong(kv[0]), Long.parseLong(kv[1])});
            } catch (NumberFormatException ignore) {
            }
        }
        return result;
    }

    private static synchronized void store(int account, ArrayList<long[]> pins) {
        StringBuilder sb = new StringBuilder();
        for (long[] pin : pins) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(pin[0]).append(':').append(pin[1]);
        }
        prefs().edit().putString(key(account), sb.toString()).apply();
    }

    public static synchronized boolean isLocal(int account, long dialogId) {
        for (long[] pin : load(account)) {
            if (pin[0] == dialogId) {
                return true;
            }
        }
        return false;
    }

    public static synchronized void add(int account, long dialogId, int pinnedNum) {
        ArrayList<long[]> pins = load(account);
        for (long[] pin : pins) {
            if (pin[0] == dialogId) {
                pin[1] = pinnedNum;
                store(account, pins);
                return;
            }
        }
        pins.add(new long[]{dialogId, pinnedNum});
        store(account, pins);
    }

    public static synchronized boolean remove(int account, long dialogId) {
        ArrayList<long[]> pins = load(account);
        for (int i = 0; i < pins.size(); i++) {
            if (pins.get(i)[0] == dialogId) {
                pins.remove(i);
                store(account, pins);
                return true;
            }
        }
        return false;
    }

    /** Whether a new pin in this folder no longer fits on the server and must stay local. */
    public static boolean mustPinLocally(MessagesController controller, int account, TLRPC.Dialog newPin) {
        if (!enabled()) {
            return false;
        }
        int limit = UserConfig.getInstance(account).isPremium()
                ? (newPin.folder_id != 0 ? controller.maxFolderPinnedDialogsCountPremium : controller.maxPinnedDialogsCountPremium)
                : (newPin.folder_id != 0 ? controller.maxFolderPinnedDialogsCountDefault : controller.maxPinnedDialogsCountDefault);
        int serverPins = 0;
        for (TLRPC.Dialog d : controller.getDialogs(newPin.folder_id)) {
            if (d == newPin || d instanceof TLRPC.TL_dialogFolder || !d.pinned) {
                continue;
            }
            if (!isLocal(account, d.id) && !org.telegram.messenger.DialogObject.isEncryptedDialog(d.id)) {
                serverPins++;
            }
        }
        return serverPins >= limit;
    }

    /** Called at the start of every chat list sort: pin local pins that a server sync unpinned. */
    public static void apply(MessagesController controller, int account) {
        if (!enabled()) {
            return;
        }
        for (long[] pin : load(account)) {
            TLRPC.Dialog dialog = controller.dialogs_dict.get(pin[0]);
            if (dialog != null && !dialog.pinned) {
                dialog.pinned = true;
                dialog.pinnedNum = (int) pin[1];
            }
        }
    }
}
