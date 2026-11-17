package org.telegram.ugram;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;

import androidx.core.app.NotificationCompat;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.NotificationsController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.LaunchActivity;

/** A local notification when unread messages are left after leaving the app. */
public class UgramReminder extends BroadcastReceiver {

    public static final String HOURS = "unreadReminderHours";
    public static final int[] OPTIONS = {0, 1, 3, 6, 12, 24};
    private static final String CHANNEL = "ugram_reminder";
    private static final int NOTIFICATION_ID = 0x75677231;

    public static int hours() {
        try {
            return Integer.parseInt(UgramConfig.getString(HOURS));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static PendingIntent intent(Context context, int count) {
        Intent intent = new Intent(context, UgramReminder.class);
        intent.putExtra("count", count);
        return PendingIntent.getBroadcast(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /** Called when the app goes to the background. */
    public static void schedule() {
        Context context = ApplicationLoader.applicationContext;
        AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarms == null) {
            return;
        }
        int hours = hours();
        int count = 0;
        if (hours > 0) {
            for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
                if (UserConfig.getInstance(a).isClientActivated()) {
                    count += NotificationsController.getInstance(a).getTotalUnreadCount();
                }
            }
        }
        alarms.cancel(intent(context, 0));
        if (hours <= 0 || count <= 0) {
            return;
        }
        alarms.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + hours * 3600_000L, intent(context, count));
    }

    /** Called when the app comes back. */
    public static void cancel() {
        Context context = ApplicationLoader.applicationContext;
        AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarms != null) {
            alarms.cancel(intent(context, 0));
        }
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.cancel(NOTIFICATION_ID);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        int count = intent.getIntExtra("count", 0);
        if (count <= 0) {
            return;
        }
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) {
            return;
        }
        boolean ru = java.util.Locale.getDefault().getLanguage().startsWith("ru");
        if (Build.VERSION.SDK_INT >= 26 && manager.getNotificationChannel(CHANNEL) == null) {
            manager.createNotificationChannel(new NotificationChannel(CHANNEL, ru ? "Напоминания Ugram" : "Ugram reminders", NotificationManager.IMPORTANCE_DEFAULT));
        }
        Intent open = new Intent(context, LaunchActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent content = PendingIntent.getActivity(context, 2, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.notification)
                .setContentTitle("Ugram")
                .setContentText(ru ? "Непрочитанных сообщений: " + count + ". Загляните, пока не забыли." : "You have " + count + " unread messages.")
                .setContentIntent(content)
                .setAutoCancel(true);
        try {
            manager.notify(NOTIFICATION_ID, builder.build());
        } catch (SecurityException ignore) {
            // No notification permission.
        }
    }
}
