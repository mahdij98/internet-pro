package ir.internetpro.app;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** کمکی‌ها برای انتخاب سیم‌کارت */
public final class SimUtil {

    private SimUtil() {}

    public static boolean hasPhonePermission(Context c) {
        return c.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED;
    }

    public static List<SubscriptionInfo> activeSims(Context c) {
        List<SubscriptionInfo> out = new ArrayList<>();
        try {
            SubscriptionManager sm = (SubscriptionManager) c.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
            if (sm == null) return out;
            if (c.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) return out;
            List<SubscriptionInfo> list = sm.getActiveSubscriptionInfoList();
            if (list != null) out.addAll(list);
            Collections.sort(out, new Comparator<SubscriptionInfo>() {
                @Override public int compare(SubscriptionInfo a, SubscriptionInfo b) {
                    return Integer.compare(a.getSimSlotIndex(), b.getSimSlotIndex());
                }
            });
        } catch (Exception ignored) { }
        return out;
    }

    /**
     * شناسه سیم‌کارتی که باید استفاده شود.
     * پیش‌فرض: اولین سیم‌کارت فعال (سیم‌کارت ۱). اگر انتخابی ذخیره شده باشد همان استفاده می‌شود.
     */
    public static int resolveSubId(Context c) {
        List<SubscriptionInfo> sims = activeSims(c);
        int saved = Prefs.getSubId(c);
        for (SubscriptionInfo s : sims) {
            if (s.getSubscriptionId() == saved) return saved;
        }
        if (!sims.isEmpty()) return sims.get(0).getSubscriptionId();
        return SubscriptionManager.INVALID_SUBSCRIPTION_ID;
    }

    public static String simLabel(Context c, SubscriptionInfo info) {
        int slot = info.getSimSlotIndex() + 1;
        CharSequence name = info.getDisplayName();
        String carrier = "";
        try { carrier = String.valueOf(info.getCarrierName()); } catch (Exception ignored) { }
        String label = "سیم‌کارت " + Prefs.fa(String.valueOf(slot));
        if (name != null && name.length() > 0) label += " — " + name;
        else if (carrier != null && carrier.length() > 0 && !"null".equals(carrier)) label += " — " + carrier;
        return label;
    }
}
