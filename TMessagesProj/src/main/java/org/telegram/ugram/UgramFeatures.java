package org.telegram.ugram;

import android.text.TextUtils;

import org.telegram.messenger.DialogObject;
import org.telegram.messenger.MediaDataController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_update;

import java.util.ArrayList;

/** Ugram behaviour hooked into sending and updates. */
public final class UgramFeatures {

    public static final String AUTO_POSTFIX = "autoPostfix";
    public static final String TEXT_STYLE = "textStyle";
    public static final String HEART_EFFECT = "heartEffect";
    public static final String AUTO_STICKER = "autoSticker";
    public static final String AUTO_APPROVE = "autoApproveJoinRequests";

    public static final String[] STYLES = {"", "bold", "italic", "underline", "strikethrough", "monospace", "spoiler"};
    public static final String[] STYLE_NAMES_RU = {"Без стиля", "Жирный", "Курсив", "Подчёркнутый", "Зачёркнутый", "Моноширинный", "Скрытый (спойлер)"};
    public static final String[] STYLE_NAMES_EN = {"No style", "Bold", "Italic", "Underline", "Strikethrough", "Monospace", "Spoiler"};

    private static final long HEART_EFFECT_ID = 5159385139981059251L;

    private UgramFeatures() {
    }

    /** Postfix, text style and heart effect for an outgoing text message; queues the auto-sticker. */
    public static void transformOutgoing(int account, SendMessagesHelper.SendMessageParams params) {
        if (params == null || params.retryMessageObject != null || TextUtils.isEmpty(params.message)) {
            return;
        }
        if (params.photo != null || params.document != null || params.location != null || params.poll != null
                || params.game != null || params.invoice != null || params.user != null || params.todo != null) {
            return;
        }
        if (DialogObject.isEncryptedDialog(params.peer) || params.message.startsWith("/")) {
            return;
        }
        String postfix = UgramConfig.getString(AUTO_POSTFIX);
        if (!TextUtils.isEmpty(postfix)) {
            params.message = params.message + "\n" + postfix;
        }
        TLRPC.MessageEntity entity = styleEntity(UgramConfig.getString(TEXT_STYLE));
        if (entity != null) {
            entity.offset = 0;
            entity.length = params.message.length();
            ArrayList<TLRPC.MessageEntity> entities = params.entities != null ? new ArrayList<>(params.entities) : new ArrayList<>();
            entities.add(entity);
            params.entities = entities;
        }
        if (UgramConfig.get(HEART_EFFECT) && params.effect_id == 0 && DialogObject.isUserDialog(params.peer)
                && params.peer != UserConfig.getInstance(account).getClientUserId()) {
            params.effect_id = HEART_EFFECT_ID;
        }
        if (UgramConfig.get(AUTO_STICKER) && params.scheduleDate == 0) {
            final long peer = params.peer;
            final org.telegram.messenger.MessageObject replyToTopMsg = params.replyToTopMsg;
            org.telegram.messenger.AndroidUtilities.runOnUIThread(() -> {
                ArrayList<TLRPC.Document> recent = MediaDataController.getInstance(account).getRecentStickers(MediaDataController.TYPE_IMAGE);
                if (recent == null || recent.isEmpty() || !(recent.get(0) instanceof TLRPC.TL_document)) {
                    return;
                }
                SendMessagesHelper.getInstance(account).sendMessage(SendMessagesHelper.SendMessageParams.of((TLRPC.TL_document) recent.get(0), null, null, peer, null, replyToTopMsg, null, null, null, null, true, 0, 0, 0, "sticker", null, false));
            }, 300);
        }
    }

    private static TLRPC.MessageEntity styleEntity(String style) {
        if (style == null) {
            return null;
        }
        switch (style) {
            case "bold":
                return new TLRPC.TL_messageEntityBold();
            case "italic":
                return new TLRPC.TL_messageEntityItalic();
            case "underline":
                return new TLRPC.TL_messageEntityUnderline();
            case "strikethrough":
                return new TLRPC.TL_messageEntityStrike();
            case "monospace":
                return new TLRPC.TL_messageEntityCode();
            case "spoiler":
                return new TLRPC.TL_messageEntitySpoiler();
            default:
                return null;
        }
    }

    /** Approves every pending join request of a chat the account administers. */
    public static void onPendingJoinRequests(int account, TL_update.TL_updatePendingJoinRequests update) {
        if (!UgramConfig.get(AUTO_APPROVE) || update.requests_pending <= 0 || update.peer == null) {
            return;
        }
        TLRPC.TL_messages_hideAllChatJoinRequests req = new TLRPC.TL_messages_hideAllChatJoinRequests();
        req.approved = true;
        req.peer = MessagesController.getInstance(account).getInputPeer(update.peer);
        ConnectionsManager.getInstance(account).sendRequest(req, (response, error) -> {
            if (response instanceof TLRPC.Updates) {
                MessagesController.getInstance(account).processUpdates((TLRPC.Updates) response, false);
            }
        });
    }
}
