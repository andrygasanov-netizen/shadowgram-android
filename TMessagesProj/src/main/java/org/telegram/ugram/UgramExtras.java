package org.telegram.ugram;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.BulletinFactory;

import java.util.ArrayList;

/** The Swiftgram settings of the iOS app, ported to Android. */
public final class UgramExtras {

    // Profiles and calls
    public static final String SHOW_ID = "sgShowProfileId";
    public static final String SHOW_DC = "sgShowDC";
    public static final String CONFIRM_CALLS = "sgConfirmCalls";
    // Media
    public static final String PHOTO_QUALITY = "sgPhotoQuality";
    public static final String SEND_LARGE_PHOTOS = "sgSendLargePhotos";
    public static final String REAR_CAM_VIDEO_NOTES = "sgRearCamVideoNotes";
    // Chat
    public static final String SECONDS_IN_TIME = "sgSecondsInMessages";
    public static final String NO_DELETE_EFFECT = "sgNoSnapDeletionEffect";
    public static final String NO_NEXT_CHANNEL = "sgNoPullToNextChannel";
    public static final String NO_NEXT_TOPIC = "sgNoPullToNextTopic";
    public static final String DOUBLE_TAP_EDIT = "sgDoubleTapEdit";
    public static final String HIDE_STORIES = "sgHideStories";
    public static final String WARN_STORIES = "sgWarnBeforeStories";
    public static final String REMEMBER_FOLDER = "sgRememberLastFolder";
    public static final String HIDE_TAB_BAR = "sgHideTabBar";
    // Context menu: shown when true (iOS defaults)
    public static final String MENU_SAVE_TO_CLOUD = "sgMenuSaveToCloud";
    public static final String MENU_REPLY = "sgMenuReply";
    public static final String MENU_PIN = "sgMenuPin";
    public static final String MENU_REPORT = "sgMenuReport";
    public static final String MENU_SAVE_MEDIA = "sgMenuSaveMedia";
    public static final String MENU_THREAD = "sgMenuThread";

    public static final int OPTION_SAVE_TO_CLOUD = 992;
    public static final int[] QUALITIES = {50, 60, 70, 80, 90, 100};

    private UgramExtras() {
    }

    public static boolean on(String key) {
        return UgramConfig.get(key);
    }

    /** Defaults: everything off except the context-menu items, which are shown like in Telegram. */
    static boolean defaultOn(String key) {
        return MENU_REPLY.equals(key) || MENU_PIN.equals(key) || MENU_REPORT.equals(key) || MENU_SAVE_MEDIA.equals(key) || MENU_THREAD.equals(key);
    }

    // region Media

    public static int photoQuality() {
        try {
            int q = Integer.parseInt(UgramConfig.getString(PHOTO_QUALITY));
            return Math.max(10, Math.min(100, q));
        } catch (NumberFormatException e) {
            return 80;
        }
    }

    public static int photoSize(int original) {
        return on(SEND_LARGE_PHOTOS) ? Math.max(original, 2560) : original;
    }

    // endregion

    // region Time

    public static String formatTime(long millis) {
        if (on(SECONDS_IN_TIME)) {
            return LocaleController.getInstance().getFormatterDayWithSeconds().format(millis);
        }
        return LocaleController.getInstance().getFormatterDay().format(millis);
    }

    // endregion

    // region Calls

    /** Asks before a call when "Call confirmation" is on. Returns true when the call must wait for the answer. */
    public static boolean confirmCall(android.app.Activity activity, TLRPC.User user, Runnable start) {
        if (!on(CONFIRM_CALLS) || activity == null) {
            return false;
        }
        boolean ru = UgramConfig.isRussian();
        String name = user != null ? org.telegram.messenger.UserObject.getUserName(user) : "";
        new AlertDialog.Builder(activity)
                .setTitle(ru ? "Позвонить?" : "Call?")
                .setMessage((ru ? "Позвонить " : "Call ") + name + "?")
                .setPositiveButton(ru ? "Позвонить" : "Call", (d, w) -> start.run())
                .setNegativeButton(ru ? "Отмена" : "Cancel", null)
                .show();
        return true;
    }

    // endregion

    // region Profiles

    public static String idLabel(int account, long dialogId) {
        StringBuilder label = new StringBuilder("ID");
        if (on(SHOW_DC)) {
            int dc = 0;
            MessagesController controller = MessagesController.getInstance(account);
            if (dialogId > 0) {
                TLRPC.User user = controller.getUser(dialogId);
                if (user != null && user.photo != null) {
                    dc = user.photo.dc_id;
                }
            } else {
                TLRPC.Chat chat = controller.getChat(-dialogId);
                if (chat != null && chat.photo != null) {
                    dc = chat.photo.dc_id;
                }
            }
            if (dc != 0) {
                label.append(" · DC").append(dc);
            }
        }
        return label.toString();
    }

    public static String idValue(long dialogId) {
        return String.valueOf(dialogId > 0 ? dialogId : -dialogId);
    }

    public static void copyId(BaseFragment fragment, long dialogId) {
        AndroidUtilities.addToClipboard(idValue(dialogId));
        BulletinFactory.of(fragment).createCopyBulletin(UgramConfig.isRussian() ? "ID скопирован" : "ID copied").show();
    }

    // endregion

    // region Context menu

    /** Hides the message menu items switched off in Ugram Tools and adds "Save to Saved Messages". */
    public static void filterMenu(MessageObject message, ArrayList<CharSequence> items, ArrayList<Integer> options, ArrayList<Integer> icons) {
        for (int i = options.size() - 1; i >= 0; i--) {
            int option = options.get(i);
            boolean hide = option == ChatActivity.OPTION_REPLY && !on(MENU_REPLY)
                    || (option == ChatActivity.OPTION_PIN || option == ChatActivity.OPTION_UNPIN) && !on(MENU_PIN)
                    || option == ChatActivity.OPTION_REPORT_CHAT && !on(MENU_REPORT)
                    || (option == ChatActivity.OPTION_SAVE_TO_GALLERY || option == ChatActivity.OPTION_SAVE_TO_GALLERY2 || option == ChatActivity.OPTION_SAVE_TO_DOWNLOADS_OR_MUSIC) && !on(MENU_SAVE_MEDIA)
                    || option == ChatActivity.OPTION_VIEW_REPLIES_OR_THREAD && !on(MENU_THREAD);
            if (hide && i < items.size() && i < icons.size()) {
                items.remove(i);
                options.remove(i);
                icons.remove(i);
            }
        }
        if (on(MENU_SAVE_TO_CLOUD) && message != null && message.getId() > 0 && !message.isSponsored() && message.messageOwner != null
                && message.messageOwner.action == null && !options.contains(OPTION_SAVE_TO_CLOUD)) {
            int at = Math.min(options.size(), 1);
            items.add(at, UgramConfig.isRussian() ? "Сохранить в Избранное" : "Save to Saved Messages");
            options.add(at, OPTION_SAVE_TO_CLOUD);
            icons.add(at, R.drawable.msg_saved);
        }
    }

    public static void saveToCloud(BaseFragment fragment, MessageObject message, ArrayList<MessageObject> group) {
        if (message == null) {
            return;
        }
        int account = fragment.getCurrentAccount();
        ArrayList<MessageObject> messages = group != null && !group.isEmpty() ? new ArrayList<>(group) : new ArrayList<>();
        if (messages.isEmpty()) {
            messages.add(message);
        }
        long self = UserConfig.getInstance(account).getClientUserId();
        SendMessagesHelper.getInstance(account).sendMessage(messages, self, false, false, true, 0, 0);
        BulletinFactory.of(fragment).createSimpleBulletin(R.raw.saved_messages, UgramConfig.isRussian() ? "Сохранено в Избранное" : "Saved to Saved Messages").show();
    }

    // endregion

    // region Folders

    public static int lastFolder(int account) {
        try {
            return Integer.parseInt(UgramConfig.getString("sgLastFolder" + account));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public static void setLastFolder(int account, int filterId) {
        if (on(REMEMBER_FOLDER)) {
            UgramConfig.setString("sgLastFolder" + account, String.valueOf(filterId));
        }
    }

    // endregion
}
