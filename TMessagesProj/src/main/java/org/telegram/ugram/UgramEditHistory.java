package org.telegram.ugram;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesStorage;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;

/** Earlier versions of messages edited after they arrived, kept on this device. */
public final class UgramEditHistory {

    public static final String ENABLED = "editHistory";
    private static final int MAX_VERSIONS = 20;

    public static final class Version {
        public final int date;
        public final String text;

        Version(int date, String text) {
            this.date = date;
            this.text = text;
        }
    }

    private static SharedPreferences prefs;

    private UgramEditHistory() {
    }

    private static SharedPreferences prefs() {
        if (prefs == null) {
            prefs = ApplicationLoader.applicationContext.getSharedPreferences("ugram_edits", Context.MODE_PRIVATE);
        }
        return prefs;
    }

    private static String key(int account, long dialogId, int messageId) {
        return account + "_" + dialogId + "_" + messageId;
    }

    /** Called with an edited message from an update, before it replaces the stored copy. */
    public static void record(int account, TLRPC.Message edited) {
        if (edited == null || edited.id <= 0 || !UgramConfig.get(ENABLED)) {
            return;
        }
        long dialogId = MessageObject.getDialogId(edited);
        TLRPC.Message previous;
        try {
            previous = MessagesStorage.getInstance(account).getMessage(dialogId, edited.id);
        } catch (Exception e) {
            return;
        }
        if (previous == null) {
            return;
        }
        String oldText = previous.message != null ? previous.message : "";
        String newText = edited.message != null ? edited.message : "";
        if (TextUtils.isEmpty(oldText) || oldText.equals(newText)) {
            return;
        }
        int date = previous.edit_date != 0 ? previous.edit_date : previous.date;
        String k = key(account, dialogId, edited.id);
        synchronized (UgramEditHistory.class) {
            try {
                JSONArray array = new JSONArray(prefs().getString(k, "[]"));
                if (array.length() > 0) {
                    JSONObject last = array.getJSONObject(array.length() - 1);
                    if (oldText.equals(last.optString("t"))) {
                        return;
                    }
                }
                JSONObject item = new JSONObject();
                item.put("d", date);
                item.put("t", oldText);
                array.put(item);
                JSONArray trimmed = new JSONArray();
                for (int i = Math.max(0, array.length() - MAX_VERSIONS); i < array.length(); i++) {
                    trimmed.put(array.get(i));
                }
                prefs().edit().putString(k, trimmed.toString()).apply();
            } catch (Exception ignore) {
            }
        }
    }

    public static ArrayList<Version> get(MessageObject messageObject) {
        ArrayList<Version> result = new ArrayList<>();
        if (messageObject == null || messageObject.messageOwner == null || messageObject.getId() <= 0) {
            return result;
        }
        String stored = prefs().getString(key(messageObject.currentAccount, messageObject.getDialogId(), messageObject.getId()), null);
        if (stored == null) {
            return result;
        }
        try {
            JSONArray array = new JSONArray(stored);
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.getJSONObject(i);
                result.add(new Version(item.optInt("d"), item.optString("t")));
            }
        } catch (Exception ignore) {
        }
        return result;
    }

    /** Long-press → Edit history: earlier versions, newest first. */
    public static void show(org.telegram.ui.ActionBar.BaseFragment fragment, MessageObject messageObject) {
        if (fragment.getParentActivity() == null) {
            return;
        }
        ArrayList<Version> versions = get(messageObject);
        boolean ru = UgramConfig.isRussian();
        StringBuilder text = new StringBuilder();
        for (int i = versions.size() - 1, shown = 0; i >= 0 && shown < 10; i--, shown++) {
            Version v = versions.get(i);
            if (text.length() > 0) {
                text.append("\n\n");
            }
            text.append(org.telegram.messenger.LocaleController.formatDateChat(v.date)).append(", ")
                    .append(org.telegram.messenger.LocaleController.getInstance().getFormatterDay().format(v.date * 1000L)).append('\n');
            String t = v.text;
            if (t.length() > 400) {
                t = t.substring(0, 400) + "…";
            }
            text.append(t);
        }
        new org.telegram.ui.ActionBar.AlertDialog.Builder(fragment.getParentActivity())
                .setTitle((ru ? "История правок (" : "Edit history (") + versions.size() + ")")
                .setMessage(text.toString())
                .setPositiveButton("OK", null)
                .show();
    }

    public static boolean has(MessageObject messageObject) {
        return messageObject != null && messageObject.messageOwner != null && messageObject.getId() > 0
                && prefs().contains(key(messageObject.currentAccount, messageObject.getDialogId(), messageObject.getId()));
    }
}
