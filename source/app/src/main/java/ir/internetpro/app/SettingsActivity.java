package ir.internetpro.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class SettingsActivity extends Activity {

    private static final int REQ = 2002;
    private LinearLayout simContainer;
    private TextView accStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View content = build();
        setContentView(content);
        final int baseTop = content.getPaddingTop();
        final int baseBottom = content.getPaddingBottom();
        content.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override public android.view.WindowInsets onApplyWindowInsets(View v, android.view.WindowInsets insets) {
                int top, bottom;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
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
    }

    @Override protected void onResume() {
        super.onResume();
        rebuildSims();
        updateAccStatus();
    }

    private int dp(float v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    private View build() {
        ScrollView sc = new ScrollView(this);
        sc.setBackgroundResource(R.drawable.bg_app);
        sc.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(28));
        sc.setClipToPadding(false);
        sc.addView(root);

        // هدر
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        View back = new ImageView(this);
        int bs = dp(44);
        back.setLayoutParams(new LinearLayout.LayoutParams(bs, bs));
        back.setBackground(makeCircle(getColor(R.color.card)));
        ((ImageView) back).setImageResource(R.drawable.ic_power);
        ((ImageView) back).setColorFilter(getColor(R.color.text_dim));
        ((ImageView) back).setRotation(180f);
        ((ImageView) back).setPadding(dp(11), dp(11), dp(11), dp(11));
        back.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { finish(); }
        });
        header.addView(back);

        TextView title = new TextView(this);
        title.setText("تنظیمات");
        title.setTextColor(getColor(R.color.white));
        title.setTextSize(22);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        title.setPadding(dp(14), 0, 0, 0);
        header.addView(title);
        root.addView(header);

        // ---- سیم‌کارت ----
        LinearLayout simCard = card(root, "سیم‌کارت مورد استفاده", getString(R.string.sim_hint));
        simContainer = new LinearLayout(this);
        simContainer.setOrientation(LinearLayout.VERTICAL);
        simCard.addView(simContainer);

        // ---- روش ارسال ----
        LinearLayout methodCard = card(root, "روش ارسال کد", null);
        addMethodRow(methodCard, Prefs.METHOD_SESSION, getString(R.string.method_session),
                getString(R.string.method_session_desc));
        addMethodRow(methodCard, Prefs.METHOD_API, getString(R.string.method_api),
                getString(R.string.method_api_desc));

        // ---- دسترس‌پذیری ----
        LinearLayout accCard = card(root, "حالت پشتیبان", getString(R.string.accessibility_desc));
        accStatus = new TextView(this);
        accStatus.setTextSize(13);
        accStatus.setPadding(0, dp(6), 0, dp(10));
        accCard.addView(accStatus);

        TextView openAcc = new TextView(this);
        openAcc.setText(getString(R.string.accessibility_open));
        openAcc.setTextColor(getColor(R.color.white));
        openAcc.setTextSize(14);
        openAcc.setGravity(Gravity.CENTER);
        openAcc.setPadding(dp(14), dp(13), dp(14), dp(13));
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(14));
        g.setColor(getColor(R.color.green_dark));
        openAcc.setBackground(g);
        openAcc.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                try {
                    startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                } catch (Exception e) {
                    Toast.makeText(SettingsActivity.this, "تنظیمات دسترس‌پذیری باز نشد",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
        accCard.addView(openAcc);

        // ---- گزارش عملیات ----
        LinearLayout logCard = card(root, "گزارش عملیات",
                "اگر پاسخی دریافت نشد، متن این بخش را بفرستید تا دقیق تنظیم شود.");
        final TextView logView = new TextView(this);
        logView.setText(Prefs.getLog(this));
        logView.setTextColor(getColor(R.color.text_dim));
        logView.setTextSize(11.5f);
        logView.setLineSpacing(dp(3), 1f);
        logView.setPadding(0, dp(6), 0, dp(10));
        logView.setTextDirection(View.TEXT_DIRECTION_LTR);
        logCard.addView(logView);

        LinearLayout logButtons = new LinearLayout(this);
        logButtons.setOrientation(LinearLayout.HORIZONTAL);

        TextView refresh = smallButton("به‌روزرسانی");
        refresh.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { logView.setText(Prefs.getLog(SettingsActivity.this)); }
        });
        LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        bLp.setMarginEnd(dp(8));
        logButtons.addView(refresh, bLp);

        TextView clear = smallButton("پاک کردن");
        clear.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Prefs.clearLog(SettingsActivity.this);
                logView.setText(Prefs.getLog(SettingsActivity.this));
            }
        });
        logButtons.addView(clear, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        logCard.addView(logButtons);

        // ---- تست کد دستوری ----
        LinearLayout testCard = card(root, "تست کد دستوری",
                "اگر کد دستوری گوشی هنگام ارسال کامل دیده نمی‌شود (مثلاً ۳۲۷#*۱۰*)، این دکمه را بزنید تا فقط پنجره کد باز شود و ترتیب را ببینید.");
        TextView testBtn = smallButton(getString(R.string.test_code));
        testBtn.setBackground(roundBg(getColor(R.color.blue_dark)));
        testBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                try {
                    Intent i = new Intent(Intent.ACTION_CALL, android.net.Uri.parse("tel:"
                            + android.net.Uri.encode(MainActivity.USSD_CODE)));
                    startActivity(i);
                } catch (Exception e) {
                    Toast.makeText(SettingsActivity.this, "اجازه تماس داده نشده است", Toast.LENGTH_SHORT).show();
                }
            }
        });
        testCard.addView(testBtn);

        // ---- درباره ----
        LinearLayout aboutCard = card(root, "درباره", getString(R.string.about_text));

        return sc;
    }

    private TextView smallButton(String text) {
        TextView b = new TextView(this);
        b.setText(text);
        b.setTextColor(getColor(R.color.white));
        b.setTextSize(13.5f);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(12), dp(12), dp(12), dp(12));
        b.setBackground(roundBg(getColor(R.color.stroke)));
        return b;
    }

    private GradientDrawable roundBg(int color) {
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(14));
        g.setColor(color);
        return g;
    }

    private GradientDrawable makeCircle(int color) {
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(color);
        return g;
    }

    private LinearLayout card(LinearLayout parent, String titleText, String desc) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.card_bg);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(16);
        parent.addView(card, lp);

        TextView t = new TextView(this);
        t.setText(titleText);
        t.setTextColor(getColor(R.color.white));
        t.setTextSize(16);
        t.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        card.addView(t);

        if (desc != null) {
            TextView d = new TextView(this);
            d.setText(desc);
            d.setTextColor(getColor(R.color.text_dim));
            d.setTextSize(12.5f);
            d.setPadding(0, dp(6), 0, dp(8));
            d.setLineSpacing(dp(3), 1f);
            card.addView(d);
        }
        return card;
    }

    private void addMethodRow(LinearLayout parent, final String key, String title, String desc) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(10), 0, dp(10));
        row.setGravity(Gravity.TOP);

        final RadioButton rb = new RadioButton(this);
        rb.setChecked(key.equals(Prefs.getMethod(this)));
        rb.setButtonTintList(android.content.res.ColorStateList.valueOf(getColor(R.color.green)));
        rb.setLayoutParams(new LinearLayout.LayoutParams(dp(36), ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView t = new TextView(this);
        t.setText(title);
        t.setTextColor(getColor(R.color.white));
        t.setTextSize(14.5f);
        texts.addView(t);

        TextView d = new TextView(this);
        d.setText(desc);
        d.setTextColor(getColor(R.color.text_dim));
        d.setTextSize(12);
        d.setPadding(0, dp(3), 0, 0);
        texts.addView(d);

        row.addView(rb);
        row.addView(texts);

        final CompoundButton.OnCheckedChangeListener none = null;
        row.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Prefs.setMethod(SettingsActivity.this, key);
                addMethodRowRefresh(parent);
            }
        });
        rb.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Prefs.setMethod(SettingsActivity.this, key);
                addMethodRowRefresh(parent);
            }
        });

        parent.addView(row);
    }

    private void addMethodRowRefresh(LinearLayout parent) {
        // بازسازی ساده: اکتیویتی را دوباره می‌سازیم
        recreate();
    }

    private void rebuildSims() {
        if (simContainer == null) return;
        simContainer.removeAllViews();

        if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_PHONE_STATE}, REQ);
        }

        List<SubscriptionInfo> sims = SimUtil.activeSims(this);
        int current = SimUtil.resolveSubId(this);

        if (sims.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(getString(R.string.sim_none));
            empty.setTextColor(getColor(R.color.text_dim));
            empty.setTextSize(13);
            empty.setPadding(0, dp(8), 0, dp(8));
            simContainer.addView(empty);
            return;
        }

        for (final SubscriptionInfo info : sims) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(9), 0, dp(9));

            final RadioButton rb = new RadioButton(this);
            rb.setChecked(info.getSubscriptionId() == current);
            rb.setButtonTintList(android.content.res.ColorStateList.valueOf(getColor(R.color.green)));
            rb.setLayoutParams(new LinearLayout.LayoutParams(dp(36), ViewGroup.LayoutParams.WRAP_CONTENT));
            rb.setClickable(false);

            TextView label = new TextView(this);
            label.setText(SimUtil.simLabel(this, info));
            label.setTextColor(getColor(R.color.white));
            label.setTextSize(14.5f);
            label.setLayoutParams(new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            row.addView(rb);
            row.addView(label);

            View.OnClickListener click = new View.OnClickListener() {
                @Override public void onClick(View v) {
                    Prefs.setSubId(SettingsActivity.this, info.getSubscriptionId());
                    Toast.makeText(SettingsActivity.this,
                            "سیم‌کارت انتخاب شد: " + SimUtil.simLabel(SettingsActivity.this, info),
                            Toast.LENGTH_SHORT).show();
                    rebuildSims();
                }
            };
            row.setOnClickListener(click);
            label.setOnClickListener(click);
            simContainer.addView(row);
        }
    }

    private void updateAccStatus() {
        if (accStatus == null) return;
        boolean on = UssdAccessibilityService.isConnected() || isAccessibilityEnabledInSettings();
        accStatus.setText(on ? "وضعیت: فعال ✅" : "وضعیت: غیرفعال");
        accStatus.setTextColor(on ? getColor(R.color.green) : getColor(R.color.text_dim));
    }

    private boolean isAccessibilityEnabledInSettings() {
        try {
            String enabled = Settings.Secure.getString(getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            return enabled != null && enabled.contains(getPackageName() + "/"
                    + UssdAccessibilityService.class.getName());
        } catch (Exception e) {
            return false;
        }
    }
}
