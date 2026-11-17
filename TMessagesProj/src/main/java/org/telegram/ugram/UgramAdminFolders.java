package org.telegram.ugram;

import android.content.Context;
import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.support.LongSparseIntArray;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.FilterCreateActivity;

import java.util.ArrayList;

/** Folders with the groups and channels the account owns or administers. */
public final class UgramAdminFolders {

    private UgramAdminFolders() {
    }

    private static SharedPreferences prefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences("ugram", Context.MODE_PRIVATE);
    }

    public static void update(BaseFragment fragment) {
        int account = fragment.getCurrentAccount();
        MessagesController controller = MessagesController.getInstance(account);
        boolean ru = UgramConfig.isRussian();
        boolean premium = UserConfig.getInstance(account).isPremium();
        int maxChats = premium ? controller.dialogFiltersChatsLimitPremium : controller.dialogFiltersChatsLimitDefault;
        int maxFolders = premium ? controller.dialogFiltersLimitPremium : controller.dialogFiltersLimitDefault;

        ArrayList<Long> groups = new ArrayList<>();
        ArrayList<Long> channels = new ArrayList<>();
        ArrayList<TLRPC.Dialog> all = new ArrayList<>(controller.getAllDialogs());
        for (TLRPC.Dialog dialog : all) {
            if (dialog.id >= 0) {
                continue;
            }
            TLRPC.Chat chat = controller.getChat(-dialog.id);
            if (chat == null || ChatObject.isNotInChat(chat) || !(chat.creator || ChatObject.hasAdminRights(chat))) {
                continue;
            }
            ArrayList<Long> target = ChatObject.isChannelAndNotMegaGroup(chat) ? channels : groups;
            if (!target.contains(dialog.id) && target.size() < maxChats) {
                target.add(dialog.id);
            }
        }

        String groupsTitle = ru ? "Мои группы" : "My groups";
        String channelsTitle = ru ? "Мои каналы" : "My channels";
        int[] pending = {0};
        boolean[] limitHit = {false};
        Runnable done = () -> {
            if (--pending[0] > 0 || fragment.getParentActivity() == null) {
                return;
            }
            String text = (ru ? "Групп: " : "Groups: ") + groups.size() + (ru ? ", каналов: " : ", channels: ") + channels.size() + ".";
            if (limitHit[0]) {
                text += ru ? "\nНе хватило места для новой папки — достигнут лимит папок." : "\nThe folder limit was reached, so a folder was not created.";
            }
            new AlertDialog.Builder(fragment.getParentActivity())
                    .setTitle(ru ? "Папки обновлены" : "Folders updated")
                    .setMessage(text)
                    .setPositiveButton("OK", null)
                    .show();
        };

        String[][] specs = {{"adminFolderGroups" + account, groupsTitle}, {"adminFolderChannels" + account, channelsTitle}};
        ArrayList<Long>[] lists = new ArrayList[]{groups, channels};
        int nextId = 2;
        for (int i = 0; i < 2; i++) {
            String key = specs[i][0];
            ArrayList<Long> peers = lists[i];
            MessagesController.DialogFilter existing = controller.dialogFiltersById.get(prefs().getInt(key, -1));
            if (peers.isEmpty()) {
                if (existing != null) {
                    TLRPC.TL_messages_updateDialogFilter req = new TLRPC.TL_messages_updateDialogFilter();
                    req.id = existing.id;
                    org.telegram.tgnet.ConnectionsManager.getInstance(account).sendRequest(req, null);
                    controller.removeFilter(existing);
                    org.telegram.messenger.MessagesStorage.getInstance(account).deleteDialogFilter(existing);
                }
                prefs().edit().remove(key).apply();
                continue;
            }
            MessagesController.DialogFilter filter = existing;
            boolean creating = filter == null;
            if (creating) {
                if (controller.dialogFilters.size() - 1 >= maxFolders) {
                    limitHit[0] = true;
                    continue;
                }
                filter = new MessagesController.DialogFilter();
                while (controller.dialogFiltersById.get(nextId) != null) {
                    nextId++;
                }
                filter.id = nextId++;
                filter.name = specs[i][1];
                filter.color = -1;
            }
            prefs().edit().putInt(key, filter.id).apply();
            pending[0]++;
            FilterCreateActivity.saveFilterToServer(filter, 0, filter.name, filter.entities != null ? filter.entities : new ArrayList<>(), filter.title_noanimate, filter.color,
                    new ArrayList<>(peers), new ArrayList<>(), new LongSparseIntArray(), creating, false, true, true, true, fragment, done);
        }
        if (pending[0] == 0) {
            pending[0] = 1;
            done.run();
        }
    }
}
