package org.telegram.ugram;

import android.graphics.Bitmap;
import android.util.Base64;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.zxing.EncodeHintType;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.TelegramQRCodeWriter;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.LayoutHelper;

import java.util.Arrays;
import java.util.HashMap;

/**
 * Log in by scanning a QR code with an already signed-in Telegram app
 * (Settings → Devices → Link Desktop Device), like Telegram Desktop does.
 */
public final class UgramQrLogin {

    public interface Result {
        /** Exactly one of authorization / error is set. */
        void run(TLRPC.TL_auth_authorization authorization, String error);
    }

    private static UgramQrLogin active;

    private final BaseFragment fragment;
    private final int account;
    private final Result result;
    private final boolean ru = UgramConfig.isRussian();

    private AlertDialog dialog;
    private ImageView qrView;
    private TextView statusView;
    private byte[] shownToken;
    private boolean finished;
    private boolean requesting;
    private final Runnable refresh = this::export;

    private UgramQrLogin(BaseFragment fragment, int account, Result result) {
        this.fragment = fragment;
        this.account = account;
        this.result = result;
    }

    public static void show(BaseFragment fragment, int account, Result result) {
        if (fragment.getParentActivity() == null) {
            return;
        }
        if (active != null) {
            active.close();
        }
        active = new UgramQrLogin(fragment, account, result);
        active.open();
    }

    /** Called for every update: the server sends updateLoginToken once the QR code was scanned. */
    public static void onUpdate(int account, TLRPC.Update update) {
        if (update instanceof org.telegram.tgnet.tl.TL_update.TL_updateLoginToken) {
            AndroidUtilities.runOnUIThread(() -> {
                UgramQrLogin login = active;
                if (login != null && login.account == account && !login.finished) {
                    AndroidUtilities.cancelRunOnUIThread(login.refresh);
                    login.export();
                }
            });
        }
    }

    private void open() {
        LinearLayout layout = new LinearLayout(fragment.getParentActivity());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);

        qrView = new ImageView(fragment.getParentActivity());
        qrView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        layout.addView(qrView, LayoutHelper.createLinear(240, 240, Gravity.CENTER_HORIZONTAL, 0, 8, 0, 8));

        statusView = new TextView(fragment.getParentActivity());
        statusView.setGravity(Gravity.CENTER);
        statusView.setTextSize(14);
        statusView.setTextColor(org.telegram.ui.ActionBar.Theme.getColor(org.telegram.ui.ActionBar.Theme.key_dialogTextBlack));
        statusView.setText(ru ? "Загружаем QR-код…" : "Loading the QR code…");
        layout.addView(statusView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 24, 0, 24, 8));

        dialog = new AlertDialog.Builder(fragment.getParentActivity())
                .setTitle(ru ? "Вход по QR-коду" : "Log in with QR code")
                .setView(layout)
                .setNegativeButton(ru ? "Отмена" : "Cancel", (d, w) -> close())
                .create();
        dialog.setOnDismissListener(d -> close());
        dialog.show();
        export();
    }

    private void close() {
        finished = true;
        AndroidUtilities.cancelRunOnUIThread(refresh);
        if (active == this) {
            active = null;
        }
        if (dialog != null) {
            AlertDialog d = dialog;
            dialog = null;
            try {
                d.dismiss();
            } catch (Exception ignore) {
            }
        }
    }

    private void export() {
        if (finished || requesting) {
            return;
        }
        requesting = true;
        TLRPC.TL_auth_exportLoginToken req = new TLRPC.TL_auth_exportLoginToken();
        req.api_id = BuildVars.APP_ID;
        req.api_hash = BuildVars.APP_HASH;
        ConnectionsManager.getInstance(account).sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
            requesting = false;
            handle(response, error);
        }), ConnectionsManager.RequestFlagWithoutLogin | ConnectionsManager.RequestFlagFailOnServerErrors);
    }

    private void handle(TLObject response, TLRPC.TL_error error) {
        if (finished) {
            return;
        }
        if (error != null) {
            if (error.text != null && error.text.contains("SESSION_PASSWORD_NEEDED")) {
                finish(null, error.text);
            } else {
                statusView.setText((ru ? "Ошибка: " : "Error: ") + error.text);
                AndroidUtilities.runOnUIThread(refresh, 5000);
            }
            return;
        }
        if (response instanceof TLRPC.TL_auth_loginToken) {
            TLRPC.TL_auth_loginToken token = (TLRPC.TL_auth_loginToken) response;
            showToken(token.token);
            int now = ConnectionsManager.getInstance(account).getCurrentTime();
            long delay = Math.max(5, Math.min(30, token.expires - now)) * 1000L;
            AndroidUtilities.runOnUIThread(refresh, delay);
        } else if (response instanceof TLRPC.TL_auth_loginTokenMigrateTo) {
            TLRPC.TL_auth_loginTokenMigrateTo migrate = (TLRPC.TL_auth_loginTokenMigrateTo) response;
            statusView.setText(ru ? "Входим…" : "Logging in…");
            ConnectionsManager.getInstance(account).setDefaultDatacenterId(migrate.dc_id);
            TLRPC.TL_auth_importLoginToken req = new TLRPC.TL_auth_importLoginToken();
            req.token = migrate.token;
            requesting = true;
            ConnectionsManager.getInstance(account).sendRequest(req, (r, e) -> AndroidUtilities.runOnUIThread(() -> {
                requesting = false;
                handle(r, e);
            }), null, null, ConnectionsManager.RequestFlagWithoutLogin | ConnectionsManager.RequestFlagFailOnServerErrors, migrate.dc_id, ConnectionsManager.ConnectionTypeGeneric, true);
        } else if (response instanceof TLRPC.TL_auth_loginTokenSuccess) {
            TLRPC.auth_Authorization authorization = ((TLRPC.TL_auth_loginTokenSuccess) response).authorization;
            if (authorization instanceof TLRPC.TL_auth_authorization) {
                finish((TLRPC.TL_auth_authorization) authorization, null);
            } else {
                finish(null, "UNSUPPORTED_AUTHORIZATION");
            }
        }
    }

    private void showToken(byte[] token) {
        if (shownToken != null && Arrays.equals(shownToken, token)) {
            return;
        }
        shownToken = token;
        String url = "tg://login?token=" + Base64.encodeToString(token, Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
        try {
            HashMap<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.MARGIN, 1);
            Bitmap bitmap = new TelegramQRCodeWriter().encode(url, 768, 768, hints, null, 0.75f, 0xFFFFFFFF, 0xFF000000);
            qrView.setImageBitmap(bitmap);
        } catch (Exception e) {
            statusView.setText(String.valueOf(e));
            return;
        }
        statusView.setText(ru
                ? "На телефоне, где вы уже вошли: Настройки → Устройства → «Подключить устройство» и наведите камеру на этот код."
                : "On a phone where you are logged in: Settings → Devices → \"Link Desktop Device\", then point the camera at this code.");
    }

    private void finish(TLRPC.TL_auth_authorization authorization, String error) {
        Result r = result;
        close();
        r.run(authorization, error);
    }
}
