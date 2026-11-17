<p align="center">
  <img src=".github/shadowgram.png" width="128" height="128" alt="Shadowgram">
</p>

<h1 align="center">Shadowgram для Android</h1>

<p align="center">
  Неофициальный клиент Telegram с режимом призрака, сохранением удалённых сообщений, автопрокси и десятками настроек.<br>
  <a href="https://github.com/andrygasanov-netizen/shadowgram-android/releases/latest"><b>Скачать последнюю версию</b></a> ·
  <a href="https://github.com/andrygasanov-netizen/shadowgram">iPhone</a> ·
  <a href="https://github.com/andrygasanov-netizen/shadowgram-desktop">Windows</a> ·
  <a href="#english">English</a>
</p>

> Shadowgram не связан с Telegram FZ-LLC. Это независимый форк с открытым исходным кодом (GPL v2),
> основанный на официальном [Telegram for Android](https://github.com/DrKLO/Telegram).

## Содержание

- [Установка](#установка)
- [Возможности](#возможности)
- [Подключение и прокси](#подключение-и-прокси)
- [Частые вопросы](#частые-вопросы)
- [Сборка](#сборка)
- [Лицензия и благодарности](#лицензия-и-благодарности)

## Установка

1. Скачайте `Shadowgram.apk` со страницы [последнего релиза](https://github.com/andrygasanov-netizen/shadowgram-android/releases/latest).
2. Откройте файл на телефоне и разрешите установку из этого источника, если Android спросит.
3. Войдите в аккаунт Telegram: по номеру телефона или **по QR-коду** с другого устройства (Настройки → Устройства → «Подключить устройство»).

APK собран для 64-битных ARM-процессоров, то есть почти для всех телефонов последних лет.

> Начиная с версии **13.2.2.1b** APK подписан собственным ключом Shadowgram. Если у вас стоит более ранняя версия, удалите её перед установкой: поверх старой подписи Android новую версию не поставит.

## Возможности

Все настройки собраны в одном месте: **Настройки → Shadowgram Tools**.
Для быстрого старта откройте **Основные настройки** и нажмите **«Включить рекомендуемое»**: включатся удалённые сообщения, исчезающие медиа, история правок, скрытый просмотр историй и безлимитные закрепы.

### Режим призрака 👻

| Функция | Что делает |
|---|---|
| Не отправлять «прочитано» | Собеседник не видит, что вы прочитали сообщение |
| Не показывать «печатает…» | Набор текста не виден |
| Не показывать «в сети» | Вы остаётесь «был(а) недавно», пока не отправите сообщение |
| Скрытый просмотр историй | Истории смотрятся без отметки о просмотре |
| Быстрый переключатель | Включает и выключает всё сразу с главной страницы Tools |

### Чаты

- **Удалённые сообщения.** Сообщения, которые собеседник удалил у всех, остаются в чате с меткой 🗑.
- **Исчезающие медиа.** Одноразовые фото, видео и голосовые приходят как обычные с меткой 🔥: их можно пересматривать и сохранять. Зажмите сообщение → «Пометить просмотренным», чтобы у собеседника оно исчезло.
- **История правок.** Зажмите отредактированное сообщение → «История правок».
- **Блокировка чатов.** Зажмите чат → «Заблокировать чат»: он откроется только по отпечатку пальца или коду экрана.
- **Скрытые чаты.** Зажмите чат → «Скрыть чат». Скрытые чаты открываются из Shadowgram Tools → «Скрытые чаты».
- **Безлимитные закрепы.** Закрепы сверх лимита Telegram хранятся на устройстве.
- **Папки админа** «Мои группы» и «Мои каналы».
- **Авто-принятие заявок** на вступление в ваши группы и каналы.
- **Напоминания о непрочитанном** через 1–24 часа после выхода из приложения.
- **ID и дата-центр** в профиле (нажмите, чтобы скопировать), подтверждение перед звонком.
- **Меню сообщения:** скрыть ненужные пункты, добавить «Сохранить в Избранное».

### Сообщения

- **Стиль текста** для всех исходящих сообщений: жирный, курсив, подчёркнутый, зачёркнутый, моноширинный или спойлер.
- **Авто-постфикс:** свой текст в конце каждого сообщения.
- **Эффект сердечка ❤️** в личных чатах.
- **Авто-стикер** после текстового сообщения.
- **Редактирование двойным тапом.**

### Интерфейс

- Скрыть ленту историй над списком чатов.
- Секунды во времени сообщений.
- Без эффекта рассыпания при удалении.
- Без перехода к следующему каналу или теме жестом в конце ленты.

### Медиа и загрузки

- **Отключить автозагрузку** фото, видео и файлов по мобильной сети и Wi-Fi.
- Качество отправляемых фото (50–100 %) и отправка больших фото до 2560 пикселей.
- Круглые видео сразу на заднюю камеру.

## Подключение и прокси

Раздел **Shadowgram Tools → «Обход блокировок»** помогает, когда Telegram долго висит на «Соединение…».

- **Автопрокси** (включён сразу). Если Telegram не подключается 20 секунд, Shadowgram скачивает открытые списки MTProxy, проверяет их скорость и включает самый быстрый. Ваши собственные прокси не заменяются.
- **«Найти рабочий прокси»** запускает такой поиск вручную, **«Отключить прокси»** выключает прокси.
- **Дробление пакетов** (эксперимент). Первый пакет каждого соединения уходит несколькими частями. Помогает только там, где Telegram распознают по содержимому трафика.

Сообщения шифруются до серверов Telegram, поэтому владелец прокси не может их прочитать, но видит ваш IP-адрес.

## Частые вопросы

**Это безопасно? Мои сообщения куда-то уходят?**
Нет. Shadowgram подключается к серверам Telegram так же, как официальное приложение. Удалённые сообщения, история правок, скрытые и заблокированные чаты хранятся только на телефоне. Исходный код открыт.

**VirusTotal показывает 1 детект. Это вирус?**
Нет. Обычно это эвристика одного антивируса (например, AhnLab «PUP/Malct»). Она срабатывает на новые APK без репутации, а раньше ещё и на публичный тестовый ключ подписи Telegram, которым подписаны тысячи сторонних сборок. С версии 13.2.2.1b Shadowgram подписан своим ключом. Весь код можно проверить в этом репозитории.

**Сохраняются ли сообщения, удалённые до установки?**
Нет. Сохраняется только то, что приложение успело получить, пока было установлено.

**Работает ли на эмуляторе?**
Да, но для эмулятора на компьютере (x86) нужна отдельная сборка `-PugramAbis=x86_64` (см. «Сборка»). ARM-сборка там работает очень медленно.

## Сборка

Нужны JDK 21, Android SDK и NDK `27.2.12479018`.

1. В `local.properties` укажите свои ключи с [my.telegram.org](https://my.telegram.org/apps):
   ```properties
   UGRAM_API_ID=123456
   UGRAM_API_HASH=0123456789abcdef0123456789abcdef
   ```
2. Для подписи своим ключом добавьте туда же (без этого используется публичный тестовый ключ Telegram):
   ```properties
   SHADOWGRAM_KEYSTORE=/путь/к/release.jks
   SHADOWGRAM_KEY_ALIAS=shadowgram
   SHADOWGRAM_KEY_PASSWORD=пароль
   ```
3. Соберите:
   ```sh
   ./gradlew :TMessagesProj_App:assembleAfatRelease
   # для эмулятора:
   ./gradlew -PugramAbis=x86_64 :TMessagesProj_App:assembleAfatRelease
   ```
   APK появится в `TMessagesProj_App/build/outputs/apk/afat/release/`.

Код Shadowgram лежит в `TMessagesProj/src/main/java/org/telegram/ugram/`. Прокси и дробление пакетов — в `UgramDpi.java` и `TMessagesProj/jni/tgnet/ConnectionSocket.cpp`.

## Лицензия и благодарности

Код распространяется под лицензией **GNU GPL v2**, как и Telegram for Android. Спасибо [Telegram](https://github.com/DrKLO/Telegram); часть функций перенесена из [Swiftgram](https://github.com/Swiftgram/Telegram-iOS).

---

<a id="english"></a>

# Shadowgram for Android (English)

An unofficial Telegram client with ghost mode, saved deleted messages, auto proxy and dozens of extra settings. Based on the official [Telegram for Android](https://github.com/DrKLO/Telegram). Not affiliated with Telegram FZ-LLC.

## Install

Download `Shadowgram.apk` from the [latest release](https://github.com/andrygasanov-netizen/shadowgram-android/releases/latest) and install it (64-bit ARM phones). Log in with your phone number or **with a QR code** from another device. Since **13.2.2.1b** the APK is signed with Shadowgram's own key, so uninstall older versions first.

## Features

Everything is in **Settings → Shadowgram Tools**. Open **Essentials** and tap **"Turn on recommended"** for a quick start.

- **Ghost mode:** no read receipts, no "typing…", no online status, story stealth mode, one switch for all.
- **Chats:** deleted messages kept with a 🗑 mark, view-once media kept (🔥), edit history, chats locked with fingerprint, hidden chats, unlimited pins, admin folders, auto-approve join requests, unread reminders, profile ID and data center, call confirmation, custom message menu with "Save to Saved Messages".
- **Messages:** text style for every message, auto-postfix, heart effect, auto-sticker, double tap to edit.
- **Interface:** hide stories, seconds in message time, no deletion effect, no pull to next channel or topic.
- **Media:** turn off auto-download, outgoing photo quality, large photos, rear camera for video messages.
- **Connection:** auto proxy (on by default) finds and turns on the fastest public MTProxy when Telegram can't connect; experimental packet splitting.

## Build

JDK 21, Android SDK and NDK `27.2.12479018`. Put `UGRAM_API_ID` / `UGRAM_API_HASH` (and optionally `SHADOWGRAM_KEYSTORE`, `SHADOWGRAM_KEY_ALIAS`, `SHADOWGRAM_KEY_PASSWORD`) into `local.properties`, then run `./gradlew :TMessagesProj_App:assembleAfatRelease`. Add `-PugramAbis=x86_64` for an emulator build.

## License

GNU GPL v2, like Telegram for Android. Thanks to [Telegram](https://github.com/DrKLO/Telegram) and [Swiftgram](https://github.com/Swiftgram/Telegram-iOS).
