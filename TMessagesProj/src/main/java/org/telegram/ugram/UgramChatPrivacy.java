package org.telegram.ugram;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.INavigationLayout;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.LaunchActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** Chats locked behind fingerprint / the screen lock, and chats hidden from the chat list. */
public final class UgramChatPrivacy {

    public static final String LOCKED = "lockedChats";
    public static final String HIDDEN = "hiddenChats";

    private static final HashSet<Long> unlocked = new HashSet<>();

    private UgramChatPrivacy() {
    }

    private static SharedPreferences prefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences("ugram", Context.MODE_PRIVATE);
    }

    private static final java.util.HashMap<String, HashSet<Long>> cache = new java.util.HashMap<>();

    public static synchronized Set<Long> peers(String kind, int account) {
        String key = kind + account;
        HashSet<Long> set = cache.get(key);
        if (set == null) {
            set = new HashSet<>();
            for (String s : prefs().getStringSet(key, Collections.emptySet())) {
                try {
                    set.add(Long.parseLong(s));
                } catch (NumberFormatException ignore) {
                }
            }
            cache.put(key, set);
        }
        return set;
    }

    public static synchronized boolean contains(String kind, int account, long dialogId) {
        return peers(kind, account).contains(dialogId);
    }

    public static synchronized void set(String kind, int account, long dialogId, boolean value) {
        Set<Long> set = peers(kind, account);
        if (value) {
            set.add(dialogId);
        } else {
            set.remove(dialogId);
        }
        HashSet<String> stored = new HashSet<>();
        for (Long id : set) {
            stored.add(String.valueOf(id));
        }
        prefs().edit().putStringSet(kind + account, stored).apply();
    }

    public static synchronized boolean needsUnlock(int account, long dialogId) {
        return contains(LOCKED, account, dialogId) && !unlocked.contains(dialogId);
    }

    /** Locks every chat again; called when the app goes to the background. */
    public static synchronized void relockAll() {
        unlocked.clear();
    }

    private static synchronized void markUnlocked(long dialogId) {
        unlocked.add(dialogId);
    }

    /** Removes hidden chats from a chat list shown to the user. */
    public static ArrayList<TLRPC.Dialog> filterHidden(int account, ArrayList<TLRPC.Dialog> dialogs) {
        Set<Long> hidden;
        synchronized (UgramChatPrivacy.class) {
            hidden = peers(HIDDEN, account);
            if (hidden.isEmpty() || dialogs == null) {
                return dialogs;
            }
            hidden = new HashSet<>(hidden);
        }
        boolean any = false;
        for (int i = 0, n = dialogs.size(); i < n; i++) {
            if (hidden.contains(dialogs.get(i).id)) {
                any = true;
                break;
            }
        }
        if (!any) {
            return dialogs;
        }
        ArrayList<TLRPC.Dialog> result = new ArrayList<>(dialogs.size());
        for (int i = 0, n = dialogs.size(); i < n; i++) {
            TLRPC.Dialog dialog = dialogs.get(i);
            if (!hidden.contains(dialog.id)) {
                result.add(dialog);
            }
        }
        return result;
    }

    // region Authentication

    public interface Result {
        void run(boolean success);
    }

    /** Asks for fingerprint / face, falling back to the screen lock. Succeeds at once when the device has no lock. */
    public static void authenticate(String title, Result result) {
        LaunchActivity activity = LaunchActivity.instance;
        if (activity == null) {
            result.run(false);
            return;
        }
        int authenticators = BiometricManager.Authenticators.BIOMETRIC_WEAK | BiometricManager.Authenticators.DEVICE_CREDENTIAL;
        if (android.os.Build.VERSION.SDK_INT < 30) {
            // Biometric + device credential together is only supported from Android 11.
            authenticators = BiometricManager.Authenticators.BIOMETRIC_WEAK;
        }
        if (BiometricManager.from(activity).canAuthenticate(authenticators) != BiometricManager.BIOMETRIC_SUCCESS) {
            android.app.KeyguardManager keyguard = (android.app.KeyguardManager) activity.getSystemService(Context.KEYGUARD_SERVICE);
            if (keyguard == null || !keyguard.isDeviceSecure()) {
                result.run(true);
                return;
            }
            authenticators = BiometricManager.Authenticators.DEVICE_CREDENTIAL;
            if (android.os.Build.VERSION.SDK_INT < 30) {
                // No fingerprint enrolled and the device credential alone needs Android 11: nothing to check against.
                result.run(true);
                return;
            }
        }
        BiometricPrompt.PromptInfo.Builder info = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setAllowedAuthenticators(authenticators);
        if ((authenticators & BiometricManager.Authenticators.DEVICE_CREDENTIAL) == 0) {
            info.setNegativeButtonText(UgramConfig.isRussian() ? "Отмена" : "Cancel");
        }
        try {
            BiometricPrompt prompt = new BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), new BiometricPrompt.AuthenticationCallback() {
                @Override
                public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult r) {
                    result.run(true);
                }

                @Override
                public void onAuthenticationError(int errorCode, CharSequence errString) {
                    result.run(false);
                }
            });
            prompt.authenticate(info.build());
        } catch (Exception e) {
            result.run(false);
        }
    }

    // endregion

    // region Navigation

    private static long dialogIdOf(ChatActivity chat) {
        Bundle args = chat.getArguments();
        if (args == null) {
            return 0;
        }
        long userId = args.getLong("user_id", 0);
        if (userId != 0) {
            return userId;
        }
        long chatId = args.getLong("chat_id", 0);
        if (chatId != 0) {
            return -chatId;
        }
        return 0;
    }

    /** Stops a locked chat from opening until the user authenticates, then opens it. Returns true when intercepted. */
    public static boolean interceptPresent(INavigationLayout layout, INavigationLayout.NavigationParams params) {
        if (!(params.fragment instanceof ChatActivity)) {
            return false;
        }
        ChatActivity chat = (ChatActivity) params.fragment;
        long dialogId = dialogIdOf(chat);
        int account = chat.getCurrentAccount();
        if (dialogId == 0 || !needsUnlock(account, dialogId)) {
            return false;
        }
        if (params.preview) {
            return true;
        }
        authenticate(UgramConfig.isRussian() ? "Открыть чат" : "Open chat", success -> {
            if (success) {
                markUnlocked(dialogId);
                AndroidUtilities.runOnUIThread(() -> layout.presentFragment(params));
            }
        });
        return true;
    }

    /** Long-press on a locked chat: show the Ugram options instead of a preview of its messages. */
    public static boolean handleLockedPreview(BaseFragment fragment, long dialogId) {
        int account = fragment.getCurrentAccount();
        if (!needsUnlock(account, dialogId) || fragment.getParentActivity() == null) {
            return false;
        }
        showOptions(fragment, dialogId);
        return true;
    }

    public static void showOptions(BaseFragment fragment, long dialogId) {
        int account = fragment.getCurrentAccount();
        boolean ru = UgramConfig.isRussian();
        boolean locked = contains(LOCKED, account, dialogId);
        boolean hidden = contains(HIDDEN, account, dialogId);
        CharSequence[] items = {
                locked ? (ru ? "Снять блокировку 🔓" : "Unlock chat 🔓") : (ru ? "Заблокировать чат 🔒" : "Lock chat 🔒"),
                hidden ? (ru ? "Вернуть в список" : "Unhide chat") : (ru ? "Скрыть чат" : "Hide chat")
        };
        new AlertDialog.Builder(fragment.getParentActivity())
                .setTitle(title(account, dialogId))
                .setItems(items, (d, which) -> {
                    if (which == 0) {
                        toggleLock(account, dialogId);
                    } else {
                        toggleHidden(account, dialogId);
                    }
                })
                .show();
    }

    public static void toggleLock(int account, long dialogId) {
        boolean ru = UgramConfig.isRussian();
        if (!contains(LOCKED, account, dialogId)) {
            set(LOCKED, account, dialogId, true);
            return;
        }
        authenticate(ru ? "Снять блокировку" : "Unlock chat", success -> {
            if (success) {
                set(LOCKED, account, dialogId, false);
            }
        });
    }

    public static void toggleHidden(int account, long dialogId) {
        boolean ru = UgramConfig.isRussian();
        Runnable apply = () -> {
            set(HIDDEN, account, dialogId, !contains(HIDDEN, account, dialogId));
            MessagesController.getInstance(account).sortDialogs(null);
            org.telegram.messenger.NotificationCenter.getInstance(account).postNotificationName(org.telegram.messenger.NotificationCenter.dialogsNeedReload);
        };
        if (contains(HIDDEN, account, dialogId)) {
            authenticate(ru ? "Вернуть чат в список" : "Unhide chat", success -> {
                if (success) {
                    apply.run();
                }
            });
        } else {
            apply.run();
        }
    }

    public static String title(int account, long dialogId) {
        MessagesController controller = MessagesController.getInstance(account);
        if (DialogObject.isUserDialog(dialogId)) {
            TLRPC.User user = controller.getUser(dialogId);
            return user != null ? UserObject.getUserName(user) : String.valueOf(dialogId);
        }
        TLRPC.Chat chat = controller.getChat(-dialogId);
        return chat != null ? chat.title : String.valueOf(dialogId);
    }

    /** Settings → Hidden chats: authenticate, then pick a hidden chat to open. */
    public static void openHiddenChats(BaseFragment fragment) {
        boolean ru = UgramConfig.isRussian();
        int account = fragment.getCurrentAccount();
        authenticate(ru ? "Показать скрытые чаты" : "Show hidden chats", success -> {
            if (!success || fragment.getParentActivity() == null) {
                return;
            }
            ArrayList<Long> ids = new ArrayList<>(peers(HIDDEN, account));
            if (ids.isEmpty()) {
                new AlertDialog.Builder(fragment.getParentActivity())
                        .setTitle(ru ? "Скрытые чаты" : "Hidden chats")
                        .setMessage(ru ? "Скрытых чатов нет. Зажмите чат в списке и выберите «Скрыть чат»." : "No hidden chats. Long-press a chat in the list and choose \"Hide chat\".")
                        .setPositiveButton("OK", null)
                        .show();
                return;
            }
            Collections.sort(ids, (a, b) -> title(account, a).compareToIgnoreCase(title(account, b)));
            CharSequence[] names = new CharSequence[ids.size()];
            for (int i = 0; i < ids.size(); i++) {
                names[i] = title(account, ids.get(i));
            }
            new AlertDialog.Builder(fragment.getParentActivity())
                    .setTitle(ru ? "Скрытые чаты 🔒" : "Hidden chats 🔒")
                    .setItems(names, (d, which) -> {
                        long dialogId = ids.get(which);
                        markUnlocked(dialogId);
                        Bundle args = new Bundle();
                        if (DialogObject.isUserDialog(dialogId)) {
                            args.putLong("user_id", dialogId);
                        } else {
                            args.putLong("chat_id", -dialogId);
                        }
                        fragment.presentFragment(new ChatActivity(args));
                    })
                    .setNegativeButton(ru ? "Закрыть" : "Close", null)
                    .show();
        });
    }

    // endregion
}
