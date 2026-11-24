package ir.internetpro.app;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.Locale;

/**
 * اینترنت پرو — یک دکمه گرد برای روشن/خاموش کردن سرویس اینترنت.
 *
 * کد دستوری: *10*327#  (ستاره، ۱۰، ستاره، ۳۲۷، هشتگ)
 * روشن: پاسخ «1» — خاموش: پاسخ «2»
 */
public class MainActivity extends Activity {

    public static final String USSD_CODE = "*10*327#";
    public static final String REPLY_ON = "1";
    public static final String REPLY_OFF = "2";

    private static final int REQ_PERMS = 1001;
    private static final int TIMEOUT_MS = 60000;

    private final Handler main = new Handler(Looper.getMainLooper());

    private FrameLayout innerButton;
    private View ringView;
    private View circleView;
    private TextView stateText;
    private TextView hintText;
    private TextView detailText;

    private boolean busy = false;
    private boolean targetOn = false;
    private boolean requestedPerms = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View content = buildUi();
        setContentView(content);
        applyInsets(content);
        refreshUi();
        maybeAskNotificationPermission();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshUi();
    }

    /** احترام به نوار وضعیت و نوار ناوبری در اندروید ۱۵/۱۶ (edge-to-edge) */
    private void applyInsets(final View content) {
        try {
            final int baseTop = content.getPaddingTop();
            final int baseBottom = content.getPaddingBottom();
            content.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
                @Override public android.view.WindowInsets onApplyWindowInsets(View v, android.view.WindowInsets insets) {
                    int top, bottom;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        android.graphics.Insets bars = insets.getInsets(
                                android.view.WindowInsets.Type.systemBars() | android.view.WindowInsets.Type.displayCutout());
                        top = bars.top; bottom = bars.bottom;
                    } else {
                        top = insets.getSystemWindowInsetTop();
                        bottom = insets.getSystemWindowInsetBottom();
                    }
                    v.setPadding(v.getPaddingLeft(), baseTop + top, v.getPaddingRight(), baseBottom + bottom);
                    return insets;
                }
            });
            content.requestApplyInsets();
        } catch (Exception ignored) { }
    }

    private void maybeAskNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            try {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1002);
            } catch (Exception ignored) { }
        }
    }

    /* ==================== رابط کاربری ==================== */

    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private View buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundResource(R.drawable.bg_app);
        root.setPadding(dp(20), dp(14), dp(20), dp(18));
        root.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // ---- هدر: عنوان در وسط، چرخ‌دنده در گوشه ----
        FrameLayout header = new FrameLayout(this);
        header.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams tLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tLp.gravity = Gravity.CENTER_HORIZONTAL | Gravity.TOP;
        titles.setLayoutParams(tLp);

        TextView title = new TextView(this);
        title.setText("اینترنت پرو");
        title.setTextColor(getColor(R.color.white));
        title.setTextSize(26);
        title.setGravity(Gravity.CENTER);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        titles.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText(R.string.subtitle);
        subtitle.setTextColor(getColor(R.color.text_dim));
        subtitle.setTextSize(13);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, dp(4), 0, 0);
        titles.addView(subtitle);
        header.addView(titles);

        ImageView gear = new ImageView(this);
        gear.setImageResource(R.drawable.ic_settings);
        gear.setColorFilter(getColor(R.color.text_dim));
        int gs = dp(44);
        FrameLayout.LayoutParams gLp = new FrameLayout.LayoutParams(gs, gs);
        gLp.gravity = Gravity.END | Gravity.TOP;
        gear.setLayoutParams(gLp);
        gear.setPadding(dp(10), dp(10), dp(10), dp(10));
        gear.setBackground(makeCircle(getColor(R.color.card)));
        gear.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (busy) { toast("لطفاً تا پایان عملیات صبر کنید"); return; }
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            }
        });
        header.addView(gear);
        root.addView(header);

        // ---- دکمه گرد ----
        FrameLayout center = new FrameLayout(this);
        center.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        int size = dp(240);
        int ringSize = dp(292);

        ringView = new View(this);
        FrameLayout.LayoutParams ringLp = new FrameLayout.LayoutParams(ringSize, ringSize);
        ringLp.gravity = Gravity.CENTER;
        ringView.setLayoutParams(ringLp);

        circleView = new View(this);
        // ارتفاع کم برای دایره؛ آیکون با ارتفاع بیشتر روی آن کشیده می‌شود
        circleView.setElevation(dp(6));

        // آیکون روشن/خاموش داخل خود drawable دایره (circle_on/circle_off) قرار دارد،
        // بنابراین هیچ‌گاه زیر دایره پنهان نمی‌شود.

        innerButton = new FrameLayout(this);
        innerButton.setLayoutParams(new FrameLayout.LayoutParams(size, size));
        innerButton.setClipChildren(false);
        innerButton.setClipToPadding(false);
        // ترتیب: ابتدا دایره، سپس آیکون (آخرین فرزند = بالاترین لایه)
        innerButton.addView(circleView, new FrameLayout.LayoutParams(size, size, Gravity.CENTER));
        innerButton.setClickable(true);
        innerButton.setFocusable(true);
        innerButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { onCircleClick(); }
        });

        FrameLayout.LayoutParams innerLp = new FrameLayout.LayoutParams(ringSize, ringSize, Gravity.CENTER);
        center.addView(ringView);
        center.addView(innerButton, innerLp);
        root.addView(center);

        // ---- وضعیت ----
        stateText = new TextView(this);
        stateText.setGravity(Gravity.CENTER);
        stateText.setTextSize(34);
        stateText.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        root.addView(stateText);

        hintText = new TextView(this);
        hintText.setGravity(Gravity.CENTER);
        hintText.setTextSize(14);
        hintText.setTextColor(getColor(R.color.text_dim));
        hintText.setPadding(0, dp(8), 0, dp(14));
        root.addView(hintText);

        // ---- پاسخ اپراتور ----
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.card_bg);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));

        TextView cardTitle = new TextView(this);
        cardTitle.setText("پاسخ اپراتور");
        cardTitle.setTextColor(getColor(R.color.text_dimmer));
        cardTitle.setTextSize(12);
        card.addView(cardTitle);

        detailText = new TextView(this);
        detailText.setText("—");
        detailText.setTextColor(getColor(R.color.text_dim));
        detailText.setTextSize(13);
        detailText.setPadding(0, dp(6), 0, 0);
        detailText.setMaxLines(5);
        card.addView(detailText);

        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.bottomMargin = dp(6);
        root.addView(card, cardLp);

        // ---- پانویس: کد دستوری به صورت LTR تا برعکس دیده نشود ----
        TextView footer = new TextView(this);
        footer.setTextDirection(View.TEXT_DIRECTION_LTR);
        footer.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        footer.setGravity(Gravity.CENTER);
        footer.setText("USSD:  *10*327#");
        footer.setTextColor(getColor(R.color.text_dimmer));
        footer.setTextSize(12);
        footer.setPadding(0, dp(12), 0, dp(2));
        root.addView(footer);

        ScrollView sc = new ScrollView(this);
        sc.setFillViewport(true);
        sc.setBackgroundResource(R.drawable.bg_app);
        sc.setOverScrollMode(View.OVER_SCROLL_NEVER);
        sc.addView(root, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return sc;
    }

    private GradientDrawable makeCircle(int color) {
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(color);
        return g;
    }

    /* ==================== نمایش وضعیت ==================== */

    private void refreshUi() {
        boolean on = Prefs.isOn(this);
        int color = on ? getColor(R.color.green) : getColor(R.color.red);
        circleView.setBackgroundResource(on ? R.drawable.circle_on : R.drawable.circle_off);
        ringView.setBackgroundResource(on ? R.drawable.ring_on : R.drawable.ring_off);

        stateText.setTextColor(color);
        if (busy) {
            stateText.setText(R.string.state_working);
            stateText.setTextColor(getColor(R.color.text_dim));
            stateText.setTextSize(21);
            hintText.setText(targetOn ? "ارسال «۱» در پنجره USSD…" : "ارسال «۲» در پنجره USSD…");
        } else {
            stateText.setTextSize(34);
            stateText.setText(on ? R.string.state_on : R.string.state_off);
            hintText.setText(on ? R.string.tap_to_off : R.string.tap_to_on);
        }
        hintText.setTextColor(getColor(R.color.text_dim));
        startPulse(on, busy);

        if (on) KeyNotification.show(this); else KeyNotification.hide(this);
    }

    private ObjectAnimator pulse;

    private void startPulse(boolean on, boolean working) {
        if (pulse != null) { pulse.cancel(); pulse = null; }
        if (working) {
            pulse = ObjectAnimator.ofFloat(ringView, "alpha", 0.15f, 0.6f);
            pulse.setDuration(650);
            pulse.setRepeatMode(ValueAnimator.REVERSE);
            pulse.setRepeatCount(ValueAnimator.INFINITE);
            pulse.setInterpolator(new LinearInterpolator());
            pulse.start();
        } else {
            ringView.setAlpha(on ? 0.75f : 0.55f);
        }
    }

    private void setBusy(boolean b, boolean target) {
        busy = b;
        targetOn = target;
        if (!b) main.removeCallbacks(guardTimeout);
        refreshUi();
    }

    private void showDetail(String s) {
        detailText.setText(TextUtils.isEmpty(s) || s.trim().length() == 0 ? "—" : s.trim());
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();
    }

    /* ==================== منطق دکمه ==================== */

    private void onCircleClick() {
        if (busy) return;
        final boolean turnOn = !Prefs.isOn(this);
        if (!hasPermissions()) {
            targetOn = turnOn;
            requestPermissions(new String[]{
                    Manifest.permission.CALL_PHONE,
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.READ_PHONE_NUMBERS
            }, REQ_PERMS);
            return;
        }
        startOperation(turnOn);
    }

    private boolean hasPermissions() {
        return checkSelfPermission(Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQ_PERMS) return;
        if (hasPermissions()) {
            startOperation(targetOn);
        } else {
            setBusy(false, targetOn);
            showDetail(getString(R.string.error_no_permission));
            toast(getString(R.string.error_no_permission));
        }
    }

    private void startOperation(final boolean turnOn) {
        final List<android.telephony.SubscriptionInfo> sims = SimUtil.activeSims(this);
        if (sims.isEmpty()) {
            toast(getString(R.string.error_no_sim));
            showDetail(getString(R.string.error_no_sim));
            return;
        }
        setBusy(true, turnOn);
        final String reply = turnOn ? REPLY_ON : REPLY_OFF;
        Prefs.log(this, "شروع عملیات: " + (turnOn ? "روشن" : "خاموش") + " (پاسخ " + reply + ")");
        showDetail("ارسال " + reply + " در پنجره USSD …");

        main.removeCallbacks(guardTimeout);
        main.postDelayed(guardTimeout, TIMEOUT_MS);

        if (Prefs.METHOD_API.equals(Prefs.getMethod(this))) {
            runApiFlow(SimUtil.resolveSubId(this), reply, turnOn);
        } else {
            runSessionFlow(reply, turnOn);
        }
    }

    private final Runnable guardTimeout = new Runnable() {
        @Override public void run() {
            if (!busy) return;
            UssdAccessibilityService svc = UssdAccessibilityService.get();
            if (svc != null) svc.cancel();
            if (UssdAccessibilityService.getLastTyped() != null) {
                finishOperation(UssdAccessibilityService.getLastTyped(), targetOn);
            } else {
                setBusy(false, targetOn);
                showDetail("پاسخی از اپراتور دریافت نشد. وضعیت نامشخص است.");
                Prefs.log(MainActivity.this, "پایان بدون پاسخ (تایم‌اوت)");
            }
        }
    };

    /* ---------- مسیر ۱: پنجره USSD + تایپ خودکار در همان نشست (پیش‌فرض) ---------- */

    private void runSessionFlow(final String reply, final boolean turnOn) {
        final UssdAccessibilityService svc = UssdAccessibilityService.get();
        if (svc == null) {
            askEnableAccessibility(reply, turnOn);
            return;
        }
        svc.cancel();
        svc.startSession(reply, new UssdAccessibilityService.Listener() {
            @Override public void onTyped(String dialogText) {
                main.post(new Runnable() {
                    @Override public void run() { finishOperation(dialogText, turnOn); }
                });
            }
            @Override public void onFailed() {
                main.post(new Runnable() {
                    @Override public void run() {
                        setBusy(false, turnOn);
                        showDetail("پنجره USSD پیدا نشد. دوباره تلاش کنید.");
                        Prefs.log(MainActivity.this, "پنجره USSD پیدا نشد");
                    }
                });
            }
        });
        openUssdWindow();
    }

    private void askEnableAccessibility(final String reply, final boolean turnOn) {
        setBusy(false, turnOn);
        new AlertDialog.Builder(this)
                .setTitle(R.string.need_acc_title)
                .setMessage(R.string.need_acc_msg)
                .setPositiveButton(R.string.open_acc, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        try {
                            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                        } catch (Exception ignored) { }
                    }
                })
                .setNegativeButton(R.string.continue_manual, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        setBusy(true, turnOn);
                        final UssdAccessibilityService svc = UssdAccessibilityService.get();
                        if (svc != null) {
                            svc.cancel();
                            svc.startSession(reply, new UssdAccessibilityService.Listener() {
                                @Override public void onTyped(String dialogText) {
                                    main.post(new Runnable() {
                                        @Override public void run() { finishOperation(dialogText, turnOn); }
                                    });
                                }
                                @Override public void onFailed() {
                                    main.post(new Runnable() {
                                        @Override public void run() {
                                            setBusy(false, turnOn);
                                            showDetail("پنجره USSD پیدا نشد.");
                                        }
                                    });
                                }
                            });
                        }
                        openUssdWindow();
                    }
                })
                .show();
    }

    /** باز کردن نشست USSD سیستمی؛ عدد را خودمان در همان پنجره تایپ می‌کنیم */
    private void openUssdWindow() {
        try {
            TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
            if (tm != null) {
                int subId = SimUtil.resolveSubId(this);
                if (subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                    tm = tm.createForSubscriptionId(subId);
                }
            }
            tm.sendUssdRequest(USSD_CODE, new TelephonyManager.UssdResponseCallback() {
                @Override public void onReceiveUssdResponse(TelephonyManager t, String request, CharSequence response) {
                    // تایپ در پنجره توسط سرویس پشتیبان انجام می‌شود
                }
                @Override public void onReceiveUssdResponseFailed(TelephonyManager t, String request, int code) {
                    main.post(new Runnable() {
                        @Override public void run() { callUssdCode(); }
                    });
                }
            }, main);
        } catch (Exception e) {
            callUssdCode();
        }
    }

    private void callUssdCode() {
        try {
            Intent i = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + Uri.encode(USSD_CODE)));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (Exception e) {
            try {
                startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(USSD_CODE))));
            } catch (Exception ignored) { }
        }
    }

    /* ---------- مسیر ۲: فقط API رسمی (اختیاری) ---------- */

    private void runApiFlow(final int subId, final String reply, final boolean turnOn) {
        TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        try {
            if (tm != null && subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                tm = tm.createForSubscriptionId(subId);
            }
        } catch (Exception ignored) { }
        if (tm == null) { runSessionFlow(reply, turnOn); return; }

        final TelephonyManager ftm = tm;
        try {
            ftm.sendUssdRequest(USSD_CODE, new TelephonyManager.UssdResponseCallback() {
                @Override public void onReceiveUssdResponse(TelephonyManager t, String request, final CharSequence response) {
                    main.post(new Runnable() {
                        @Override public void run() {
                            String first = response == null ? "" : response.toString();
                            Prefs.log(MainActivity.this, "پاسخ مرحله ۱: " + first);
                            showDetail("پاسخ: " + first);
                            try {
                                t.sendUssdRequest(reply, new TelephonyManager.UssdResponseCallback() {
                                    @Override public void onReceiveUssdResponse(TelephonyManager t2, String r2, CharSequence res2) {
                                        final String s2 = res2 == null ? "" : res2.toString();
                                        main.post(new Runnable() {
                                            @Override public void run() { finishOperation(s2, turnOn); }
                                        });
                                    }
                                    @Override public void onReceiveUssdResponseFailed(TelephonyManager t2, String r2, int code) {
                                        main.post(new Runnable() {
                                            @Override public void run() { finishOperation("", turnOn); }
                                        });
                                    }
                                }, main);
                            } catch (Exception e) {
                                finishOperation(first, turnOn);
                            }
                        }
                    });
                }
                @Override public void onReceiveUssdResponseFailed(TelephonyManager t, String request, int code) {
                    main.post(new Runnable() {
                        @Override public void run() {
                            Prefs.log(MainActivity.this, "API رسمی پاسخ نداد → پنجره USSD");
                            runSessionFlow(reply, turnOn);
                        }
                    });
                }
            }, main);
        } catch (Exception e) {
            runSessionFlow(reply, turnOn);
        }
    }

    /* ==================== تفسیر پاسخ ==================== */

    private void finishOperation(String response, boolean turnOn) {
        main.removeCallbacks(guardTimeout);
        String text = response == null ? "" : response.trim();
        showDetail(text.length() == 0 ? "پاسخ اپراتور دریافت شد." : text);
        Prefs.log(this, "پاسخ نهایی: " + (text.length() == 0 ? "(خالی)" : text));

        boolean failed = containsAny(text, "خطا", "ناموفق", "نشد", "error", "failed", "invalid",
                "insufficient", "اعتبار", "مجاز نیست", "not allowed", "unable", "نادرست");
        boolean confirmedOn = containsAny(text, "فعال شد", "روشن شد", "انجام شد", "موفق",
                "activated", "enabled", "success", "برقرار شد", "فعال گردید");
        boolean confirmedOff = containsAny(text, "غیرفعال شد", "قطع شد", "خاموش شد", "deactivated",
                "disabled", "لغو شد");

        if (failed) {
            setBusy(false, turnOn);
            toast("عملیات انجام نشد");
            return;
        }
        boolean newState = turnOn;
        if (confirmedOff) newState = false;
        if (confirmedOn) newState = true;
        Prefs.setOn(this, newState);
        setBusy(false, newState);
        toast(newState ? "سرویس روشن شد" : "سرویس خاموش شد");
    }

    private boolean containsAny(String text, String... keys) {
        if (text == null) return false;
        String lower = text.toLowerCase(Locale.US);
        for (String k : keys) {
            if (lower.contains(k.toLowerCase(Locale.US))) return true;
        }
        return false;
    }
}
