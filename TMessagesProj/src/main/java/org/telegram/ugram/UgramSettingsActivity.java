package org.telegram.ugram;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;

import java.util.ArrayList;

/**
 * Ugram Tools, laid out like the iOS app: a hub (quick switches, privacy, sections)
 * and one page per section.
 */
public class UgramSettingsActivity extends UniversalFragment {

    public static final int PAGE_HUB = 0;
    public static final int PAGE_BASIC = 1;
    public static final int PAGE_CHATS = 2;
    public static final int PAGE_GHOST = 3;
    public static final int PAGE_MESSAGES = 4;
    public static final int PAGE_MEDIA = 5;
    public static final int PAGE_INTERFACE = 6;

    private static final int ID_GHOST = 1;
    private static final int ID_NO_READ = 2;
    private static final int ID_NO_TYPING = 3;
    private static final int ID_HIDE_ONLINE = 4;
    private static final int ID_STORY_STEALTH = 5;
    private static final int ID_SAVE_DELETED = 6;
    private static final int ID_MARK_DELETED = 7;
    private static final int ID_KEEP_SELF_DESTRUCT = 8;
    private static final int ID_TEXT_STYLE = 9;
    private static final int ID_POSTFIX = 10;
    private static final int ID_HEART = 11;
    private static final int ID_STICKER = 12;
    private static final int ID_AUTO_APPROVE = 13;
    private static final int ID_EDIT_HISTORY = 14;
    private static final int ID_NO_AUTO_DOWNLOAD = 15;
    private static final int ID_UNLIMITED_PINS = 16;
    private static final int ID_ADMIN_FOLDERS = 17;
    private static final int ID_HIDDEN_CHATS = 18;
    private static final int ID_REMINDER = 19;
    private static final int ID_RECOMMENDED = 20;
    private static final int ID_OPEN_BASIC = 30;
    private static final int ID_OPEN_CHATS = 31;
    private static final int ID_OPEN_GHOST = 32;
    private static final int ID_OPEN_MESSAGES = 33;
    private static final int ID_OPEN_MEDIA = 34;
    private static final int ID_OPEN_INTERFACE = 35;
    private static final int ID_PHOTO_QUALITY = 40;
    private static final int ID_EXTRA_BASE = 100;

    /** Plain on/off settings ported from the iOS app, by list id. */
    private static final String[] EXTRA_KEYS = {
            UgramExtras.SHOW_ID, UgramExtras.SHOW_DC, UgramExtras.CONFIRM_CALLS,
            UgramExtras.SEND_LARGE_PHOTOS, UgramExtras.REAR_CAM_VIDEO_NOTES,
            UgramExtras.SECONDS_IN_TIME, UgramExtras.NO_DELETE_EFFECT, UgramExtras.NO_NEXT_CHANNEL, UgramExtras.NO_NEXT_TOPIC,
            UgramExtras.DOUBLE_TAP_EDIT, UgramExtras.HIDE_STORIES,
            UgramExtras.MENU_SAVE_TO_CLOUD, UgramExtras.MENU_REPLY, UgramExtras.MENU_PIN, UgramExtras.MENU_REPORT,
            UgramExtras.MENU_SAVE_MEDIA, UgramExtras.MENU_THREAD,
    };

    private static UItem extra(String key, String text) {
        int index = 0;
        for (int i = 0; i < EXTRA_KEYS.length; i++) {
            if (EXTRA_KEYS[i].equals(key)) {
                index = i;
            }
        }
        return UItem.asCheck(ID_EXTRA_BASE + index, text).setChecked(UgramConfig.get(key));
    }

    private static final String[] RECOMMENDED = {UgramConfig.SAVE_DELETED, UgramConfig.MARK_DELETED, UgramConfig.KEEP_SELF_DESTRUCT,
            UgramEditHistory.ENABLED, UgramConfig.STORY_STEALTH, UgramPins.ENABLED};

    private final int page;

    public UgramSettingsActivity() {
        this(PAGE_HUB);
    }

    public UgramSettingsActivity(int page) {
        super();
        this.page = page;
    }

    private static boolean ru() {
        return UgramConfig.isRussian();
    }

    private static String t(String ru, String en) {
        return ru() ? ru : en;
    }

    @Override
    protected CharSequence getTitle() {
        switch (page) {
            case PAGE_BASIC:
                return t("Основные настройки", "Essentials");
            case PAGE_CHATS:
                return t("Чаты", "Chats");
            case PAGE_GHOST:
                return t("Режим призрака", "Ghost mode");
            case PAGE_MESSAGES:
                return t("Сообщения", "Messages");
            case PAGE_MEDIA:
                return t("Медиа и загрузки", "Media & downloads");
            case PAGE_INTERFACE:
                return t("Интерфейс", "Interface");
            default:
                return "Ugram Tools";
        }
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        switch (page) {
            case PAGE_BASIC:
                fillBasic(items);
                break;
            case PAGE_CHATS:
                fillChats(items);
                break;
            case PAGE_GHOST:
                fillGhost(items);
                break;
            case PAGE_MESSAGES:
                fillMessages(items);
                break;
            case PAGE_MEDIA:
                fillMedia(items);
                break;
            case PAGE_INTERFACE:
                fillInterface(items);
                break;
            default:
                fillHub(items);
                break;
        }
    }

    private void fillHub(ArrayList<UItem> items) {
        items.add(UItem.asHeader(t("Быстро", "Quick")));
        items.add(UItem.asButton(ID_OPEN_BASIC, R.drawable.msg_premium_badge, t("Основные настройки", "Essentials")));
        items.add(UItem.asCheck(ID_GHOST, t("Режим призрака 👻", "Ghost mode 👻")).setChecked(UgramConfig.ghostModeOn()));
        items.add(UItem.asShadow(t("Одним переключателем: не отправлять «прочитано», «печатает…» и «в сети». По отдельности — в разделе «Режим призрака».",
                "One switch for no read receipts, no typing and no online status. One by one in \"Ghost mode\".")));

        items.add(UItem.asHeader(t("Приватность", "Privacy")));
        items.add(UItem.asButton(ID_HIDDEN_CHATS, R.drawable.msg_secret, t("Скрытые чаты", "Hidden chats"),
                String.valueOf(UgramChatPrivacy.peers(UgramChatPrivacy.HIDDEN, currentAccount).size())));
        items.add(UItem.asShadow(t("Зажмите чат в списке, чтобы заблокировать его (вход по отпечатку или коду экрана) или скрыть из списка чатов. Скрытые чаты открываются отсюда.",
                "Long-press a chat in the list to lock it (fingerprint or screen lock) or hide it from the chat list. Hidden chats open from here.")));

        items.add(UItem.asHeader(t("Разделы", "Sections")));
        items.add(UItem.asButton(ID_OPEN_CHATS, R.drawable.msg_discussion, t("Чаты", "Chats")));
        items.add(UItem.asButton(ID_OPEN_GHOST, R.drawable.msg_stories_stealth, t("Режим призрака", "Ghost mode")));
        items.add(UItem.asButton(ID_OPEN_INTERFACE, R.drawable.msg_theme, t("Интерфейс", "Interface")));
        items.add(UItem.asButton(ID_OPEN_MESSAGES, R.drawable.msg_edit, t("Сообщения", "Messages")));
        items.add(UItem.asButton(ID_OPEN_MEDIA, R.drawable.msg_photos, t("Медиа и загрузки", "Media & downloads")));
        items.add(UItem.asShadow(t("Чаты — удалённые сообщения, история правок, список чатов, ваши группы и каналы. Сообщения — стиль, постфикс, эффекты. Интерфейс — истории, время, эффекты. Медиа — автозагрузка, качество фото, кружки.",
                "Chats — deleted messages, edit history, chat list, profiles, message menu. Interface — stories, time, effects. Messages — style, postfix, effects. Media — auto-download, photo quality, video messages.")));
    }

    private void fillBasic(ArrayList<UItem> items) {
        items.add(UItem.asButton(ID_RECOMMENDED, t("Включить рекомендуемое ✨", "Turn on recommended ✨"), recommendedOn() ? t("Включено", "On") : ""));
        items.add(UItem.asShadow(t("Одним нажатием: удалённые сообщения, исчезающие медиа, история правок, скрытый просмотр историй и безлимитные закрепы.",
                "One tap: deleted messages, view-once media, edit history, story stealth mode and unlimited pins.")));
        items.add(UItem.asCheck(ID_SAVE_DELETED, t("Удалённые сообщения", "Deleted messages")).setChecked(UgramConfig.get(UgramConfig.SAVE_DELETED)));
        items.add(UItem.asCheck(ID_KEEP_SELF_DESTRUCT, t("Сохранять исчезающие медиа", "Keep self-destructing media")).setChecked(UgramConfig.get(UgramConfig.KEEP_SELF_DESTRUCT)));
        items.add(UItem.asCheck(ID_EDIT_HISTORY, t("История правок", "Edit history")).setChecked(UgramConfig.get(UgramEditHistory.ENABLED)));
        items.add(UItem.asCheck(ID_NO_READ, t("Не отправлять «прочитано»", "Don't send read receipts")).setChecked(UgramConfig.get(UgramConfig.NO_READ)));
        items.add(UItem.asCheck(ID_NO_TYPING, t("Не показывать «печатает…»", "Don't send typing status")).setChecked(UgramConfig.get(UgramConfig.NO_TYPING)));
        items.add(UItem.asCheck(ID_HIDE_ONLINE, t("Не показывать «в сети»", "Don't send online status")).setChecked(UgramConfig.get(UgramConfig.HIDE_ONLINE)));
        items.add(UItem.asCheck(ID_STORY_STEALTH, t("Скрытый просмотр историй", "Story stealth mode")).setChecked(UgramConfig.get(UgramConfig.STORY_STEALTH)));
        items.add(UItem.asCheck(ID_UNLIMITED_PINS, t("Безлимитные закрепы", "Unlimited pins")).setChecked(UgramConfig.get(UgramPins.ENABLED)));
        items.add(UItem.asCheck(ID_NO_AUTO_DOWNLOAD, t("Отключить автозагрузку медиа", "Disable media auto-download")).setChecked(UgramConfig.get(UgramConfig.NO_AUTO_DOWNLOAD)));
        items.add(UItem.asShadow(t("Здесь собраны самые нужные функции. Всё остальное и подробные описания — в других разделах Ugram Tools.",
                "The most useful features in one place. Everything else, with details, is in the other Ugram Tools sections.")));
    }

    private void fillChats(ArrayList<UItem> items) {
        items.add(UItem.asHeader(t("Список чатов", "Chat list")));
        items.add(UItem.asCheck(ID_UNLIMITED_PINS, t("Безлимитные закрепы", "Unlimited pins")).setChecked(UgramConfig.get(UgramPins.ENABLED)));
        items.add(UItem.asShadow(t("Закрепляйте сколько угодно чатов. Закрепы сверх лимита Telegram хранятся только на этом устройстве.",
                "Pin as many chats as you like. Pins over the Telegram limit are kept on this device only.")));
        items.add(UItem.asButton(ID_REMINDER, t("Напоминать о непрочитанном", "Unread reminder"), reminderName(UgramReminder.hours())));
        items.add(UItem.asShadow(t("Если вы закрыли Ugram, а непрочитанные сообщения остались, через выбранное время придёт напоминание.",
                "If you leave Ugram with unread messages, a reminder arrives after the chosen time.")));

        items.add(UItem.asHeader(t("Удалённые сообщения", "Deleted messages")));
        items.add(UItem.asCheck(ID_SAVE_DELETED, t("Удалённые сообщения", "Deleted messages")).setChecked(UgramConfig.get(UgramConfig.SAVE_DELETED)));
        items.add(UItem.asCheck(ID_MARK_DELETED, t("Метка 🗑 у удалённых", "Mark deleted with 🗑")).setChecked(UgramConfig.get(UgramConfig.MARK_DELETED)));
        items.add(UItem.asShadow(t("Сообщения, которые собеседник удалил у всех, остаются в чате на этом устройстве с меткой 🗑. Сохраняется только то, что уже было загружено в приложение. Секретные чаты не сохраняются.",
                "Messages deleted for everyone stay in the chat on this device with a 🗑 mark. Only what was already loaded is kept. Secret chats are not kept.")));
        items.add(UItem.asCheck(ID_EDIT_HISTORY, t("История правок", "Edit history")).setChecked(UgramConfig.get(UgramEditHistory.ENABLED)));
        items.add(UItem.asShadow(t("Зажмите отредактированное сообщение и выберите «История правок», чтобы увидеть прошлые версии. Запоминаются правки, сделанные после установки Ugram, пока приложение получало обновления.",
                "Long-press an edited message → \"Edit history\" to see earlier versions. Edits made after installing Ugram, while it was receiving updates, are remembered.")));
        items.add(UItem.asCheck(ID_KEEP_SELF_DESTRUCT, t("Сохранять исчезающие медиа", "Keep self-destructing media")).setChecked(UgramConfig.get(UgramConfig.KEEP_SELF_DESTRUCT)));
        items.add(UItem.asShadow(t("Одноразовые фото, видео и голосовые приходят как обычные и помечаются 🔥: их можно открывать сколько угодно раз и сохранять. Действует на медиа, полученные после включения. Зажмите сообщение → «Пометить просмотренным», чтобы у собеседника оно исчезло.",
                "View-once photos, videos and voice messages arrive as regular media marked 🔥: open and save them any number of times. Applies to media received after turning this on. Long-press → \"Mark as viewed\" to make it disappear for the sender.")));

        items.add(UItem.asHeader(t("Ваши группы и каналы", "Your groups and channels")));
        items.add(UItem.asCheck(ID_AUTO_APPROVE, t("Авто-принятие заявок", "Auto-approve join requests")).setChecked(UgramConfig.get(UgramFeatures.AUTO_APPROVE)));
        items.add(UItem.asShadow(t("Заявки на вступление в группы и каналы, где вы администратор, одобряются сразу, как только приходят. Работает, пока Ugram запущен.",
                "Join requests to groups and channels you administer are approved as soon as they arrive. Works while Ugram is running.")));
        items.add(UItem.asButton(ID_ADMIN_FOLDERS, t("Папки админа", "Admin folders"), t("Обновить", "Update")));
        items.add(UItem.asShadow(t("Создаёт папки «Мои группы» и «Мои каналы» с чатами, где вы владелец или администратор. Нажмите ещё раз, чтобы обновить их после изменений.",
                "Creates \"My groups\" and \"My channels\" folders with the chats you own or administer. Tap again to refresh them after changes.")));

        items.add(UItem.asHeader(t("Профили", "Profiles")));
        items.add(extra(UgramExtras.SHOW_ID, t("ID профилей", "Profile IDs")));
        items.add(extra(UgramExtras.SHOW_DC, t("Показать дата-центр (DC)", "Show data center (DC)")));
        items.add(extra(UgramExtras.CONFIRM_CALLS, t("Подтверждение вызова", "Call confirmation")));
        items.add(UItem.asShadow(t("ID показывается в профиле, нажмите, чтобы скопировать. Ugram запросит подтверждение перед совершением звонка.",
                "The ID is shown in profiles, tap to copy. Ugram asks before placing a call.")));

        items.add(UItem.asHeader(t("Контекстное меню", "Context menu")));
        items.add(extra(UgramExtras.MENU_SAVE_TO_CLOUD, t("Сохранить в Избранное", "Save to Saved Messages")));
        items.add(extra(UgramExtras.MENU_REPLY, t("Ответить", "Reply")));
        items.add(extra(UgramExtras.MENU_PIN, t("Закрепить", "Pin")));
        items.add(extra(UgramExtras.MENU_REPORT, t("Пожаловаться", "Report")));
        items.add(extra(UgramExtras.MENU_SAVE_MEDIA, t("Сохранить медиа", "Save media")));
        items.add(extra(UgramExtras.MENU_THREAD, t("Ответы / ветка", "Replies / thread")));
        items.add(UItem.asShadow(t("Выключенные пункты пропадают из меню, которое открывается по нажатию на сообщение.", "Switched-off items disappear from the message menu.")));
    }

    private void fillGhost(ArrayList<UItem> items) {
        items.add(UItem.asHeader(t("Режим призрака", "Ghost mode")));
        items.add(UItem.asCheck(ID_NO_READ, t("Не отправлять «прочитано»", "Don't send read receipts")).setChecked(UgramConfig.get(UgramConfig.NO_READ)));
        items.add(UItem.asCheck(ID_NO_TYPING, t("Не показывать «печатает…»", "Don't send typing status")).setChecked(UgramConfig.get(UgramConfig.NO_TYPING)));
        items.add(UItem.asCheck(ID_HIDE_ONLINE, t("Не показывать «в сети»", "Don't send online status")).setChecked(UgramConfig.get(UgramConfig.HIDE_ONLINE)));
        items.add(UItem.asCheck(ID_STORY_STEALTH, t("Скрытый просмотр историй", "Story stealth mode")).setChecked(UgramConfig.get(UgramConfig.STORY_STEALTH)));
        items.add(UItem.asShadow(t("Пока «прочитано» выключено, чаты могут снова становиться непрочитанными после синхронизации. Отправка сообщения всё равно показывает вас в сети.",
                "While read receipts are off, chats may become unread again after a sync. Sending a message still makes you appear online.")));
    }

    private void fillMessages(ArrayList<UItem> items) {
        items.add(UItem.asHeader(t("Исходящие сообщения", "Outgoing messages")));
        items.add(UItem.asButton(ID_TEXT_STYLE, t("Стиль текста", "Text style"), styleName(UgramConfig.getString(UgramFeatures.TEXT_STYLE))));
        String postfix = UgramConfig.getString(UgramFeatures.AUTO_POSTFIX);
        items.add(UItem.asButton(ID_POSTFIX, t("Авто-постфикс", "Auto-postfix"), postfix.isEmpty() ? t("Отключено", "Off") : postfix));
        items.add(UItem.asCheck(ID_HEART, t("Эффект сердечка ❤️", "Heart effect ❤️")).setChecked(UgramConfig.get(UgramFeatures.HEART_EFFECT)));
        items.add(UItem.asCheck(ID_STICKER, t("Авто-стикер", "Auto-sticker")).setChecked(UgramConfig.get(UgramFeatures.AUTO_STICKER)));
        items.add(extra(UgramExtras.DOUBLE_TAP_EDIT, t("Редактирование двойным тапом", "Double tap to edit")));
        items.add(UItem.asShadow(t("Стиль применяется ко всему тексту сообщения. Постфикс добавляется новой строкой в конец. Сердечко — анимированный эффект ❤️, работает только в личных чатах. Авто-стикер отправляет после текстового сообщения последний использованный стикер. Команды ботам (/…) не меняются.",
                "The style applies to the whole message. The postfix is added on a new line at the end. The heart is an animated ❤️ effect, private chats only. Auto-sticker sends your last used sticker after a text message. Bot commands (/…) are left alone.")));
    }

    private void fillMedia(ArrayList<UItem> items) {
        items.add(UItem.asHeader(t("Автозагрузка", "Auto-download")));
        items.add(UItem.asCheck(ID_NO_AUTO_DOWNLOAD, t("Отключить автозагрузку", "Disable auto-download")).setChecked(UgramConfig.get(UgramConfig.NO_AUTO_DOWNLOAD)));
        items.add(UItem.asShadow(t("Фото, видео и файлы не скачиваются сами ни по мобильной сети, ни по Wi-Fi: нажмите на медиа, чтобы загрузить. Стикеры загружаются как обычно.",
                "Photos, videos and files are not downloaded by themselves on cellular or Wi-Fi: tap media to load it. Stickers load as usual.")));

        items.add(UItem.asHeader(t("Фото", "Photos")));
        items.add(UItem.asButton(ID_PHOTO_QUALITY, t("Качество", "Quality"), UgramExtras.photoQuality() + "%"));
        items.add(UItem.asShadow(t("Качество исходящих фото.", "Quality of outgoing photos.")));
        items.add(extra(UgramExtras.SEND_LARGE_PHOTOS, t("Отправлять большие фото", "Send large photos")));
        items.add(UItem.asShadow(t("Увеличить лимит сторон для сжатых фото до 2560 пкс.", "Raise the side limit of compressed photos to 2560 px.")));

        items.add(UItem.asHeader(t("Круглые видео", "Video messages")));
        items.add(extra(UgramExtras.REAR_CAM_VIDEO_NOTES, t("На заднюю камеру", "Start with rear camera")));
    }

    private void fillInterface(ArrayList<UItem> items) {
        items.add(UItem.asHeader(t("Истории", "Stories")));
        items.add(extra(UgramExtras.HIDE_STORIES, t("Скрыть истории", "Hide stories")));
        items.add(UItem.asCheck(ID_STORY_STEALTH, t("Скрытый просмотр историй", "Story stealth mode")).setChecked(UgramConfig.get(UgramConfig.STORY_STEALTH)));
        items.add(UItem.asShadow(t("Скрывает ленту историй над списком чатов.", "Hides the stories row above the chat list.")));

        items.add(UItem.asHeader(t("Другое", "Other")));
        items.add(extra(UgramExtras.SECONDS_IN_TIME, t("Секунды во времени сообщений", "Seconds in message time")));
        items.add(extra(UgramExtras.NO_DELETE_EFFECT, t("Без эффекта удаления", "No deletion effect")));
        items.add(extra(UgramExtras.NO_NEXT_CHANNEL, t("Без перехода к следующему каналу", "No pull to next channel")));
        items.add(extra(UgramExtras.NO_NEXT_TOPIC, t("Без перехода к следующей теме", "No pull to next topic")));
        items.add(UItem.asShadow(t("Эффект удаления — рассыпание сообщения в пыль. Переход к следующему каналу или теме — потянуть вверх в конце ленты.",
                "The deletion effect turns a message into dust. Pull to next channel or topic is pulling up at the end of the feed.")));
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        switch (item.id) {
            case ID_OPEN_BASIC:
                presentFragment(new UgramSettingsActivity(PAGE_BASIC));
                return;
            case ID_OPEN_CHATS:
                presentFragment(new UgramSettingsActivity(PAGE_CHATS));
                return;
            case ID_OPEN_GHOST:
                presentFragment(new UgramSettingsActivity(PAGE_GHOST));
                return;
            case ID_OPEN_MESSAGES:
                presentFragment(new UgramSettingsActivity(PAGE_MESSAGES));
                return;
            case ID_OPEN_MEDIA:
                presentFragment(new UgramSettingsActivity(PAGE_MEDIA));
                return;
            case ID_OPEN_INTERFACE:
                presentFragment(new UgramSettingsActivity(PAGE_INTERFACE));
                return;
            case ID_PHOTO_QUALITY: {
                CharSequence[] names = new CharSequence[UgramExtras.QUALITIES.length];
                for (int i = 0; i < names.length; i++) {
                    names[i] = UgramExtras.QUALITIES[i] + "%" + (UgramExtras.QUALITIES[i] == 80 ? t(" (как в Telegram)", " (Telegram default)") : "");
                }
                new AlertDialog.Builder(getContext())
                        .setTitle(t("Качество фото", "Photo quality"))
                        .setItems(names, (dialog, which) -> {
                            UgramConfig.setString(UgramExtras.PHOTO_QUALITY, String.valueOf(UgramExtras.QUALITIES[which]));
                            refresh();
                        })
                        .show();
                return;
            }
            case ID_GHOST:
                UgramConfig.setGhostMode(!UgramConfig.ghostModeOn());
                break;
            case ID_NO_READ:
                toggle(UgramConfig.NO_READ);
                break;
            case ID_NO_TYPING:
                toggle(UgramConfig.NO_TYPING);
                break;
            case ID_HIDE_ONLINE:
                toggle(UgramConfig.HIDE_ONLINE);
                break;
            case ID_STORY_STEALTH:
                toggle(UgramConfig.STORY_STEALTH);
                break;
            case ID_SAVE_DELETED:
                toggle(UgramConfig.SAVE_DELETED);
                break;
            case ID_MARK_DELETED:
                toggle(UgramConfig.MARK_DELETED);
                break;
            case ID_KEEP_SELF_DESTRUCT:
                toggle(UgramConfig.KEEP_SELF_DESTRUCT);
                break;
            case ID_HEART:
                toggle(UgramFeatures.HEART_EFFECT);
                break;
            case ID_STICKER:
                toggle(UgramFeatures.AUTO_STICKER);
                break;
            case ID_AUTO_APPROVE:
                toggle(UgramFeatures.AUTO_APPROVE);
                break;
            case ID_EDIT_HISTORY:
                toggle(UgramEditHistory.ENABLED);
                break;
            case ID_NO_AUTO_DOWNLOAD:
                toggle(UgramConfig.NO_AUTO_DOWNLOAD);
                break;
            case ID_UNLIMITED_PINS:
                toggle(UgramPins.ENABLED);
                break;
            case ID_RECOMMENDED:
                for (String key : RECOMMENDED) {
                    UgramConfig.set(key, true);
                }
                BulletinFactory.of(this).createSimpleBulletin(R.raw.contact_check, t("Рекомендуемые функции включены.", "Recommended features are on.")).show();
                break;
            case ID_ADMIN_FOLDERS:
                UgramAdminFolders.update(this);
                return;
            case ID_HIDDEN_CHATS:
                UgramChatPrivacy.openHiddenChats(this);
                return;
            case ID_REMINDER: {
                CharSequence[] names = new CharSequence[UgramReminder.OPTIONS.length];
                for (int i = 0; i < names.length; i++) {
                    names[i] = reminderName(UgramReminder.OPTIONS[i]);
                }
                new AlertDialog.Builder(getContext())
                        .setTitle(t("Напоминать о непрочитанном", "Unread reminder"))
                        .setItems(names, (dialog, which) -> {
                            UgramConfig.setString(UgramReminder.HOURS, String.valueOf(UgramReminder.OPTIONS[which]));
                            refresh();
                        })
                        .show();
                return;
            }
            case ID_TEXT_STYLE: {
                String[] names = ru() ? UgramFeatures.STYLE_NAMES_RU : UgramFeatures.STYLE_NAMES_EN;
                new AlertDialog.Builder(getContext())
                        .setTitle(t("Стиль текста", "Text style"))
                        .setItems(names, (dialog, which) -> {
                            UgramConfig.setString(UgramFeatures.TEXT_STYLE, UgramFeatures.STYLES[which]);
                            refresh();
                        })
                        .show();
                return;
            }
            case ID_POSTFIX: {
                EditText input = new EditText(getContext());
                input.setText(UgramConfig.getString(UgramFeatures.AUTO_POSTFIX));
                input.setHint(t("Например: — отправлено из Ugram", "For example: sent from Ugram"));
                new AlertDialog.Builder(getContext())
                        .setTitle(t("Авто-постфикс", "Auto-postfix"))
                        .setMessage(t("Текст добавится в конец каждого вашего сообщения. Оставьте пустым, чтобы отключить.", "This text is added to the end of each of your messages. Leave empty to turn off."))
                        .setView(input)
                        .setPositiveButton("OK", (dialog, which) -> {
                            UgramConfig.setString(UgramFeatures.AUTO_POSTFIX, input.getText().toString().trim());
                            refresh();
                        })
                        .setNegativeButton(t("Отмена", "Cancel"), null)
                        .show();
                return;
            }
            default:
                if (item.id >= ID_EXTRA_BASE && item.id < ID_EXTRA_BASE + EXTRA_KEYS.length) {
                    toggle(EXTRA_KEYS[item.id - ID_EXTRA_BASE]);
                    break;
                }
                return;
        }
        refresh();
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        if (listView != null && listView.adapter != null) {
            listView.adapter.update(true);
        }
    }

    private static boolean recommendedOn() {
        for (String key : RECOMMENDED) {
            if (!UgramConfig.get(key)) {
                return false;
            }
        }
        return true;
    }

    private static String reminderName(int hours) {
        if (hours <= 0) {
            return t("Выключено", "Off");
        }
        return t("Через " + hours + " ч", "After " + hours + " h");
    }

    private static String styleName(String style) {
        for (int i = 0; i < UgramFeatures.STYLES.length; i++) {
            if (UgramFeatures.STYLES[i].equals(style)) {
                return ru() ? UgramFeatures.STYLE_NAMES_RU[i] : UgramFeatures.STYLE_NAMES_EN[i];
            }
        }
        return ru() ? UgramFeatures.STYLE_NAMES_RU[0] : UgramFeatures.STYLE_NAMES_EN[0];
    }

    private static void toggle(String key) {
        UgramConfig.set(key, !UgramConfig.get(key));
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        return false;
    }
}
