package org.telegram.ugram;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.collection.LongSparseArray;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_account;
import org.telegram.tgnet.tl.TL_stories;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Ugram feature switches and the small local stores behind them
 * (messages deleted by others, kept view-once media).
 */
public final class UgramConfig {

    public static final String SAVE_DELETED = "saveDeleted";
    public static final String MARK_DELETED = "markDeleted";
    public static final String KEEP_SELF_DESTRUCT = "keepSelfDestruct";
    public static final String NO_READ = "noReadReceipts";
    public static final String NO_TYPING = "noTyping";
    public static final String HIDE_ONLINE = "hideOnline";
    public static final String STORY_STEALTH = "storyStealth";
    public static final String NO_AUTO_DOWNLOAD = "disableAutoDownload";

    private static SharedPreferences prefs;
    private static final Object lock = new Object();
    private static final HashSet<String>[] deleted = new HashSet[16];
    private static final HashSet<String>[] selfDestruct = new HashSet[16];
    private static final HashSet<String>[] selfDestructViewed = new HashSet[16];

    /** Requests Ugram itself sends that ghost mode must let through. */
    private static final Set<TLObject> bypass = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

    private UgramConfig() {
    }

    private static SharedPreferences prefs() {
        if (prefs == null) {
            prefs = ApplicationLoader.applicationContext.getSharedPreferences("ugram", Context.MODE_PRIVATE);
        }
        return prefs;
    }

    public static boolean get(String key) {
        boolean def = SAVE_DELETED.equals(key) || MARK_DELETED.equals(key) || KEEP_SELF_DESTRUCT.equals(key) || UgramEditHistory.ENABLED.equals(key) || UgramExtras.defaultOn(key);
        return prefs().getBoolean(key, def);
    }

    public static void set(String key, boolean value) {
        prefs().edit().putBoolean(key, value).apply();
    }

    public static String getString(String key) {
        return prefs().getString(key, "");
    }

    public static void setString(String key, String value) {
        prefs().edit().putString(key, value == null ? "" : value).apply();
    }

    public static boolean isRussian() {
        org.telegram.messenger.LocaleController.LocaleInfo info = org.telegram.messenger.LocaleController.getInstance().getCurrentLocaleInfo();
        String lang = info != null ? info.getLangCode() : null;
        return lang != null && lang.startsWith("ru");
    }

    /** "Disable auto-download": media waits for a tap; stickers and emoji still load. */
    public static boolean noAutoDownload(TLRPC.Message message) {
        if (!get(NO_AUTO_DOWNLOAD) || message == null) {
            return false;
        }
        return !MessageObject.isStickerMessage(message) && !MessageObject.isAnimatedStickerMessage(message);
    }

    public static boolean ghostModeOn() {
        return get(NO_READ) && get(NO_TYPING) && get(HIDE_ONLINE);
    }

    public static void setGhostMode(boolean value) {
        prefs().edit().putBoolean(NO_READ, value).putBoolean(NO_TYPING, value).putBoolean(HIDE_ONLINE, value).apply();
    }

    // region Local id sets

    private static String key(long dialogId, int messageId) {
        return dialogId + "_" + messageId;
    }

    private static HashSet<String> load(HashSet<String>[] cache, String name, int account) {
        synchronized (lock) {
            if (cache[account] == null) {
                Set<String> stored = prefs().getStringSet(name + account, null);
                cache[account] = stored != null ? new HashSet<>(stored) : new HashSet<>();
            }
            return cache[account];
        }
    }

    private static void store(HashSet<String> set, String name, int account) {
        synchronized (lock) {
            prefs().edit().putStringSet(name + account, new HashSet<>(set)).apply();
        }
    }

    /** Messages of private chats and basic groups are keyed by dialog 0, as the server reports their deletion. */
    private static long deletionDialog(MessageObject messageObject) {
        TLRPC.Peer peer = messageObject.messageOwner.peer_id;
        return peer instanceof TLRPC.TL_peerChannel ? -peer.channel_id : 0;
    }

    // endregion

    // region Deleted messages

    /** Records server-side deletions instead of applying them. Keys: 0 for users/basic groups, -channelId for channels. */
    public static void markDeleted(int account, LongSparseArray<ArrayList<Integer>> deletedMessages) {
        HashSet<String> set = load(deleted, "deleted", account);
        synchronized (lock) {
            for (int a = 0; a < deletedMessages.size(); a++) {
                long dialogId = deletedMessages.keyAt(a);
                for (Integer id : deletedMessages.valueAt(a)) {
                    set.add(key(dialogId, id));
                }
            }
        }
        store(set, "deleted", account);
    }

    public static boolean isDeleted(MessageObject messageObject) {
        if (messageObject == null || messageObject.messageOwner == null || messageObject.getId() <= 0) {
            return false;
        }
        HashSet<String> set = load(deleted, "deleted", messageObject.currentAccount);
        synchronized (lock) {
            return set.contains(key(deletionDialog(messageObject), messageObject.getId()));
        }
    }

    // endregion

    // region View-once media

    /** Turns incoming view-once media into regular media, remembering which messages they were. */
    public static void processIncoming(int account, TLRPC.Message message) {
        if (message == null || message.out || !(message instanceof TLRPC.TL_message) || message.media == null || message.media.ttl_seconds == 0) {
            return;
        }
        if (!get(KEEP_SELF_DESTRUCT)) {
            return;
        }
        long dialogId = MessageObject.getDialogId(message);
        HashSet<String> set = load(selfDestruct, "selfDestruct", account);
        boolean added;
        synchronized (lock) {
            added = set.add(key(dialogId, message.id));
        }
        if (added) {
            store(set, "selfDestruct", account);
        }
        message.media.ttl_seconds = 0;
    }

    public static boolean isSelfDestruct(MessageObject messageObject) {
        if (messageObject == null || messageObject.messageOwner == null) {
            return false;
        }
        HashSet<String> set = load(selfDestruct, "selfDestruct", messageObject.currentAccount);
        synchronized (lock) {
            return set.contains(key(messageObject.getDialogId(), messageObject.getId()));
        }
    }

    public static boolean isSelfDestructViewed(MessageObject messageObject) {
        HashSet<String> set = load(selfDestructViewed, "selfDestructViewed", messageObject.currentAccount);
        synchronized (lock) {
            return set.contains(key(messageObject.getDialogId(), messageObject.getId()));
        }
    }

    /** Tells the sender that kept view-once media was opened; the local copy stays. */
    public static void markViewed(MessageObject messageObject) {
        int account = messageObject.currentAccount;
        TLObject request;
        TLRPC.Peer peer = messageObject.messageOwner.peer_id;
        if (peer instanceof TLRPC.TL_peerChannel) {
            TLRPC.TL_channels_readMessageContents req = new TLRPC.TL_channels_readMessageContents();
            req.channel = MessagesController.getInstance(account).getInputChannel(peer.channel_id);
            req.id.add(messageObject.getId());
            request = req;
        } else {
            TLRPC.TL_messages_readMessageContents req = new TLRPC.TL_messages_readMessageContents();
            req.id.add(messageObject.getId());
            request = req;
        }
        bypass.add(request);
        ConnectionsManager.getInstance(account).sendRequest(request, (response, error) -> {
            if (response instanceof TLRPC.TL_messages_affectedMessages) {
                TLRPC.TL_messages_affectedMessages res = (TLRPC.TL_messages_affectedMessages) response;
                MessagesController.getInstance(account).processNewDifferenceParams(-1, res.pts, -1, res.pts_count);
            }
        });
        setSelfDestructViewed(messageObject);
    }

    public static void setSelfDestructViewed(MessageObject messageObject) {
        HashSet<String> set = load(selfDestructViewed, "selfDestructViewed", messageObject.currentAccount);
        synchronized (lock) {
            set.add(key(messageObject.getDialogId(), messageObject.getId()));
        }
        store(set, "selfDestructViewed", messageObject.currentAccount);
    }

    // endregion

    /** The time label prefix for a message: 🗑 deleted, 🔥 kept view-once, 👁 kept and marked viewed. */
    public static CharSequence timePrefix(MessageObject messageObject) {
        StringBuilder prefix = new StringBuilder();
        if (get(MARK_DELETED) && isDeleted(messageObject)) {
            prefix.append("🗑 ");
        }
        if (isSelfDestruct(messageObject)) {
            prefix.append(isSelfDestructViewed(messageObject) ? "👁 " : "🔥 ");
        }
        return prefix;
    }

    // region Ghost mode

    /**
     * Applies ghost mode to an outgoing request. Returns false when the request must not be sent.
     */
    public static boolean filterRequest(TLObject object) {
        if (object == null) {
            return true;
        }
        if (get(NO_READ) && !bypass.contains(object)) {
            if (object instanceof TLRPC.TL_messages_readHistory
                    || object instanceof TLRPC.TL_channels_readHistory
                    || object instanceof TLRPC.TL_messages_readDiscussion
                    || object instanceof TLRPC.TL_messages_readMessageContents
                    || object instanceof TLRPC.TL_channels_readMessageContents) {
                return false;
            }
        }
        if (get(NO_TYPING) && object instanceof TLRPC.TL_messages_setTyping) {
            return false;
        }
        if (get(STORY_STEALTH) && object instanceof TL_stories.TL_stories_readStories) {
            return false;
        }
        if (get(HIDE_ONLINE) && object instanceof TL_account.updateStatus) {
            ((TL_account.updateStatus) object).offline = true;
        }
        return true;
    }

    // endregion
}
