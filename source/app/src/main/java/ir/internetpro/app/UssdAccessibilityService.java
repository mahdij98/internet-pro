package ir.internetpro.app;

import android.accessibilityservice.AccessibilityService;
import android.app.Instrumentation;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * حالت پشتیبان: پنجره USSD سیستمی را پیدا می‌کند و عدد مورد نظر (۱ یا ۲) را
 * در همان نشست تایپ و ارسال می‌کند. همین کار باعث می‌شود اپراتور پاسخ را
 * «کد نامعتبر» تشخیص ندهد؛ چون ارسال در ادامه‌ی همان نشست انجام می‌شود.
 */
public class UssdAccessibilityService extends AccessibilityService {

    public interface Listener {
        void onTyped(String dialogText);
        void onFailed();
    }

    private static UssdAccessibilityService instance;
    private static String lastTyped;

    private static final Object LOCK = new Object();
    private String pendingReply;
    private long deadline;
    private Listener listener;
    private int setTextFailures = 0;

    private final Handler handler = new Handler(Looper.getMainLooper());

    public static UssdAccessibilityService get() { return instance; }
    public static boolean isConnected() { return instance != null; }
    public static String getLastTyped() { return lastTyped; }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
    }

    @Override
    public void onDestroy() {
        if (instance == this) instance = null;
        super.onDestroy();
    }

    @Override
    public void onInterrupt() { }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        synchronized (LOCK) {
            if (pendingReply == null) return;
        }
        handler.removeCallbacks(attempt);
        handler.postDelayed(attempt, 100);
    }

    /** شروع یک نشست: به‌محض ظاهر شدن پنجره USSD، پاسخ تایپ و ارسال می‌شود */
    public void startSession(final String reply, Listener l) {
        synchronized (LOCK) {
            pendingReply = reply;
            listener = l;
            deadline = System.currentTimeMillis() + 45000;
            setTextFailures = 0;
        }
        lastTyped = null;
        handler.removeCallbacks(attempt);
        handler.post(attempt);
    }

    public void cancel() {
        synchronized (LOCK) {
            pendingReply = null;
            listener = null;
        }
        handler.removeCallbacks(attempt);
    }

    private final Runnable attempt = new Runnable() {
        @Override public void run() {
            final String reply;
            final Listener l;
            synchronized (LOCK) {
                reply = pendingReply;
                l = listener;
            }
            if (reply == null) return;

            if (System.currentTimeMillis() > deadline) {
                synchronized (LOCK) { pendingReply = null; listener = null; }
                Prefs.log(UssdAccessibilityService.this, "پنجره USSD پیدا نشد (تایم‌اوت)");
                if (l != null) l.onFailed();
                return;
            }

            if (tryType(reply)) {
                synchronized (LOCK) { pendingReply = null; listener = null; }
                if (l != null) {
                    handler.postDelayed(new Runnable() {
                        @Override public void run() {
                            lastTyped = readUssdText();
                            l.onTyped(lastTyped);
                        }
                    }, 1600);
                }
            } else {
                handler.postDelayed(this, 450);
            }
        }
    };

    /* ------------------ تایپ و ارسال ------------------ */

    private boolean tryType(String reply) {
        List<AccessibilityNodeInfo> roots = findUssdRoots();
        for (AccessibilityNodeInfo root : roots) {
            AccessibilityNodeInfo edit = findEditText(root);
            if (edit != null) {
                if (setText(edit, reply)) {
                    Prefs.log(this, "عدد «" + reply + "» در پنجره USSD نوشته شد");
                    clickSend(root);
                    return true;
                }
                setTextFailures++;
                if (setTextFailures >= 3) {
                    // روش دوم: تزریق رویداد کلید در همان فیلد
                    edit.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
                    if (injectKeys(reply)) {
                        Prefs.log(this, "عدد «" + reply + "» با تزریق کلید ارسال شد");
                        clickSend(root);
                        return true;
                    }
                }
                return false;
            }
            // بعضی دیالرها گزینه‌ها را به شکل لیست نشان می‌دهند
            AccessibilityNodeInfo option = findClickableWithText(root, reply);
            if (option != null) {
                option.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                Prefs.log(this, "گزینه «" + reply + "» لمس شد");
                return true;
            }
        }
        return false;
    }

    private boolean setText(AccessibilityNodeInfo node, String text) {
        node.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
        Bundle args = new Bundle();
        args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
        if (node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)) return true;
        try {
            android.content.ClipboardManager cm =
                    (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(android.content.ClipData.newPlainText("ussd", text));
                if (node.performAction(AccessibilityNodeInfo.ACTION_PASTE)) return true;
            }
        } catch (Exception ignored) { }
        return false;
    }

    private boolean injectKeys(String digits) {
        try {
            Instrumentation inst = new Instrumentation();
            for (int i = 0; i < digits.length(); i++) {
                char c = digits.charAt(i);
                if (c < '0' || c > '9') continue;
                int keyCode = KeyEvent.KEYCODE_0 + (c - '0');
                long t = SystemClock.uptimeMillis();
                inst.sendKeyDownUpSync(keyCode);
                SystemClock.sleep(60);
            }
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private void clickSend(AccessibilityNodeInfo root) {
        AccessibilityNodeInfo send = findSendButton(root);
        if (send == null) {
            // آخرین راه: دکمه تأیید گفتگو (AlertDialog) غیر از «لغو»
            AccessibilityNodeInfo fallback = findNonCancelButton(root);
            if (fallback != null) {
                fallback.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                Prefs.log(this, "دکمه ارسال (پشتیبان) لمس شد");
                return;
            }
            // اگر دکمه‌ای نبود، معمولاً فشردن Enter کار را انجام می‌دهد
            try {
                Instrumentation inst = new Instrumentation();
                inst.sendKeyDownUpSync(KeyEvent.KEYCODE_ENTER);
                Prefs.log(this, "کلید Enter برای ارسال زده شد");
            } catch (Throwable ignored) { }
            return;
        }
        send.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        Prefs.log(this, "دکمه ارسال لمس شد");
    }

    /* ------------------ پیدا کردن عناصر ------------------ */

    private List<AccessibilityNodeInfo> findUssdRoots() {
        List<AccessibilityNodeInfo> roots = new ArrayList<>();
        List<AccessibilityWindowInfo> windows;
        try { windows = getWindows(); } catch (Exception e) { windows = null; }
        if (windows != null) {
            for (AccessibilityWindowInfo w : windows) {
                AccessibilityNodeInfo root = w.getRoot();
                if (root == null) continue;
                if (looksLikeUssd(root)) roots.add(root);
            }
        }
        if (roots.isEmpty()) {
            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (root != null && looksLikeUssd(root)) roots.add(root);
        }
        return roots;
    }

    private boolean looksLikeUssd(AccessibilityNodeInfo root) {
        if (root == null) return false;
        // پنجره برنامه خودمان را هرگز به‌عنوان پنجره USSD در نظر نگیر
        try {
            CharSequence pkg = root.getPackageName();
            if (pkg != null && pkg.toString().equals(getPackageName())) return false;
        } catch (Exception ignored) { }
        if (hasUssdId(root, 0)) return true;
        return findEditText(root) != null;
    }

    private boolean hasUssdId(AccessibilityNodeInfo node, int depth) {
        if (node == null || depth > 25) return false;
        try {
            String id = node.getViewIdResourceName();
            if (id != null && id.toLowerCase().contains("ussd")) return true;
            CharSequence txt = node.getText();
            if (txt != null && txt.toString().toLowerCase().contains("ussd")) return true;
        } catch (Exception ignored) { }
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo ch = node.getChild(i);
            if (ch != null && hasUssdId(ch, depth + 1)) return true;
        }
        return false;
    }

    private AccessibilityNodeInfo findEditText(AccessibilityNodeInfo node) {
        return findEditText(node, 0);
    }

    private AccessibilityNodeInfo findEditText(AccessibilityNodeInfo node, int depth) {
        if (node == null || depth > 30) return null;
        CharSequence cls = node.getClassName();
        if (cls != null && cls.toString().contains("EditText") && node.isVisibleToUser()) return node;
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo res = findEditText(node.getChild(i), depth + 1);
            if (res != null) return res;
        }
        return null;
    }

    private AccessibilityNodeInfo findSendButton(AccessibilityNodeInfo node) {
        List<AccessibilityNodeInfo> clickables = new ArrayList<>();
        collectClickable(node, clickables, 0);
        for (AccessibilityNodeInfo n : clickables) {
            CharSequence t = n.getText();
            if (t == null) t = n.getContentDescription();
            if (t == null) continue;
            String s = t.toString().trim().toLowerCase();
            if (s.equals("send") || s.equals("ارسال") || s.equals("ok") || s.equals("تایید")
                    || s.equals("تأیید") || s.equals("submit") || s.equals("call") || s.equals("تماس")
                    || s.equals("continue") || s.equals("ادامه") || s.equals("yes") || s.equals("بله")) {
                return n;
            }
        }
        return null;
    }

    private AccessibilityNodeInfo findNonCancelButton(AccessibilityNodeInfo node) {
        List<AccessibilityNodeInfo> clickables = new ArrayList<>();
        collectClickable(node, clickables, 0);
        for (AccessibilityNodeInfo n : clickables) {
            CharSequence cls = n.getClassName();
            if (cls == null || !cls.toString().contains("Button")) continue;
            CharSequence t = n.getText();
            String s = t == null ? "" : t.toString().trim().toLowerCase();
            if (s.equals("cancel") || s.equals("لغو") || s.equals("بستن") || s.equals("dismiss")) continue;
            return n;
        }
        return null;
    }

    private AccessibilityNodeInfo findClickableWithText(AccessibilityNodeInfo node, String text) {
        List<AccessibilityNodeInfo> clickables = new ArrayList<>();
        collectClickable(node, clickables, 0);
        String want2 = Prefs.fa(text);
        for (AccessibilityNodeInfo n : clickables) {
            CharSequence t = n.getText();
            if (t == null) t = n.getContentDescription();
            if (t == null) continue;
            String s = t.toString().trim();
            if (s.equals(text) || s.equals(want2)) return n;
        }
        return null;
    }

    private void collectClickable(AccessibilityNodeInfo node, List<AccessibilityNodeInfo> out, int depth) {
        if (node == null || depth > 30) return;
        if (node.isClickable() && node.isVisibleToUser()) out.add(node);
        for (int i = 0; i < node.getChildCount(); i++) {
            collectClickable(node.getChild(i), out, depth + 1);
        }
    }

    /** متن پنجره USSD فعلی (پاسخ اپراتور) */
    public String readUssdText() {
        StringBuilder sb = new StringBuilder();
        List<AccessibilityNodeInfo> roots = findUssdRoots();
        for (AccessibilityNodeInfo r : roots) collectText(r, sb, 0);
        String s = sb.toString().trim();
        if (s.length() > 400) s = s.substring(0, 400);
        return s;
    }

    private void collectText(AccessibilityNodeInfo node, StringBuilder sb, int depth) {
        if (node == null || depth > 30) return;
        CharSequence t = node.getText();
        if (t != null && t.length() > 0) {
            if (sb.length() > 0) sb.append('\n');
            sb.append(t);
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            collectText(node.getChild(i), sb, depth + 1);
        }
    }
}
