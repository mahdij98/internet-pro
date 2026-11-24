package ir.internetpro.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.telephony.SubscriptionManager;

/** تنظیمات ذخیره‌شده‌ی برنامه */
public final class Prefs {
    private static final String FILE = "internetpro";

    public static final String KEY_SUB_ID = "sub_id";
    public static final String KEY_METHOD = "method";      // session | api
    public static final String KEY_IS_ON = "is_on";
    public static final String KEY_LOG = "log";

    public static final String METHOD_SESSION = "session";
    public static final String METHOD_API = "api";

    private Prefs() {}

    private static SharedPreferences sp(Context c) {
        return c.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public static int getSubId(Context c) {
        return sp(c).getInt(KEY_SUB_ID, SubscriptionManager.INVALID_SUBSCRIPTION_ID);
    }

    public static void setSubId(Context c, int subId) {
        sp(c).edit().putInt(KEY_SUB_ID, subId).apply();
    }

    public static String getMethod(Context c) {
        return sp(c).getString(KEY_METHOD, METHOD_SESSION);
    }

    public static void setMethod(Context c, String m) {
        sp(c).edit().putString(KEY_METHOD, m).apply();
    }

    public static boolean isOn(Context c) {
        return sp(c).getBoolean(KEY_IS_ON, false);
    }

    public static void setOn(Context c, boolean on) {
        sp(c).edit().putBoolean(KEY_IS_ON, on).apply();
    }

    public static void log(Context c, String line) {
        String old = sp(c).getString(KEY_LOG, "");
        String stamp = new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US)
                .format(new java.util.Date());
        String entry = "[" + stamp + "] " + line + "\n";
        String all = entry + old;
        if (all.length() > 6000) all = all.substring(0, 6000);
        sp(c).edit().putString(KEY_LOG, all).apply();
    }

    public static String getLog(Context c) {
        String s = sp(c).getString(KEY_LOG, "");
        return s.length() == 0 ? "هنوز عملیاتی ثبت نشده است." : s;
    }

    public static void clearLog(Context c) {
        sp(c).edit().remove(KEY_LOG).apply();
    }

    /** ارقام فارسی برای نمایش */
    public static String fa(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch >= '0' && ch <= '9') sb.append((char) ('\u06F0' + (ch - '0')));
            else sb.append(ch);
        }
        return sb.toString();
    }
}
