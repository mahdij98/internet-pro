package ir.internetpro.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/** نوتیفیکیشن با آیکون آبی کلید وقتی سرویس روشن است */
public final class KeyNotification {

    private static final String CHANNEL = "internetpro_status";
    private static final int ID = 1101;

    private KeyNotification() {}

    private static NotificationManager nm(Context c) {
        return (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    private static void ensureChannel(Context c) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager m = nm(c);
            if (m == null) return;
            if (m.getNotificationChannel(CHANNEL) == null) {
                NotificationChannel ch = new NotificationChannel(CHANNEL,
                        "وضعیت سرویس اینترنت", NotificationManager.IMPORTANCE_LOW);
                ch.setShowBadge(false);
                ch.enableVibration(false);
                m.createNotificationChannel(ch);
            }
        }
    }

    public static void show(Context c) {
        ensureChannel(c);
        NotificationManager m = nm(c);
        if (m == null) return;

        Intent i = new Intent(c, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pi = PendingIntent.getActivity(c, 0, i, flags);

        Notification.Builder b;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            b = new Notification.Builder(c, CHANNEL);
        } else {
            b = new Notification.Builder(c);
        }
        b.setSmallIcon(R.drawable.ic_stat_pro)
                .setContentTitle(c.getString(R.string.notif_title))
                .setContentText(c.getString(R.string.notif_text))
                .setColor(0)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setContentIntent(pi);

        try {
            m.notify(ID, b.build());
        } catch (Exception ignored) { }
    }

    public static void hide(Context c) {
        NotificationManager m = nm(c);
        if (m != null) {
            try { m.cancel(ID); } catch (Exception ignored) { }
        }
    }
}
