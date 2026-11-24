# اینترنت پرو — Internet Pro

اپلیکیشن اندروید برای **روشن/خاموش کردن سرویس اینترنت** با کد دستوری `*10*327#`.

- روشن کردن: ارسال `*10*327#` و پاسخ خودکار **`1`**
- خاموش کردن: ارسال `*10*327#` و پاسخ خودکار **`2`**
- یک دکمه گرد (سبز = روشن، قرمز = خاموش) + انتخاب سیم‌کارت در تنظیمات
- پشتیبانی از اندروید ۸ تا ۱۶ (targetSdk 35، minSdk 26)
- رابط فارسی و راست‌به‌چپ

---

## دانلود و نصب

| نسخه | فایل | توضیح |
|---|---|---|
| **1.7** (آخرین) | [`releases/InternetPro-1.7.apk`](releases/InternetPro-1.7.apk) | آیکون پاور اپ در مرکز دکمه |
| 1.6 | [`releases/InternetPro-1.6.apk`](releases/InternetPro-1.6.apk) | رفع قطعی نمایش آیکون دکمه (layer-list) |
| 1.4 | [`releases/InternetPro-1.4.apk`](releases/InternetPro-1.4.apk) | حذف بج کلید، عنوان وسط‌چین، آیکون «پرو» در نوتیفیکیشن |
| 1.3 | [`releases/InternetPro-1.3.apk`](releases/InternetPro-1.3.apk) | آیکون پاور توپُر در دکمه |
| 1.2 | [`releases/InternetPro-1.2.apk`](releases/InternetPro-1.2.apk) | آیکون پاور ریموتی + بج «پرو» با فونت B Titr |
| 1.1 | [`releases/InternetPro-1.1.apk`](releases/InternetPro-1.1.apk) | رفع خطای `invalid code` (ارسال پاسخ در همان نشست USSD) |
| 1.0 | [`releases/InternetPro-1.0.apk`](releases/InternetPro-1.0.apk) | نسخه اول |

همچنین [`releases/InternetPro-latest.apk`](releases/InternetPro-latest.apk) = آخرین نسخه،
و [`releases/SHA256SUMS.txt`](releases/SHA256SUMS.txt) برای بررسی صحت فایل‌ها.

> نسخه ۱.۵ فقط یک بیلد میانی بود و بلافاصله با ۱.۶ جایگزین شد؛ فایل جداگانه‌ای برای آن منتشر نشد.

### نصب روی گوشی
1. فایل APK را دانلود و باز کنید.
2. اگر «نصب از منبع ناشناس» پرسید، اجازه دهید (Play Protect → «Install anyway»).
3. اجازه **تماس (Phone)** را بدهید.
4. برای ارسال خودکار پاسخ: تنظیمات اپ → «باز کردن تنظیمات دسترس‌پذیری» →
   «اینترنت پرو – ارسال خودکار پاسخ USSD» را روشن کنید.

جزئیات کامل: [`docs/README-fa.md`](docs/README-fa.md) — پیش‌نمایش طراحی: [`docs/preview.html`](docs/preview.html)

---

## ساختار مخزن

```
releases/            فایل‌های APK امضاشده + SHA256SUMS
source/app/          سورس کامل (Java + منابع XML)
source/build.sh      اسکریپت ساخت APK بدون Gradle (aapt2 + javac + d8 + apksigner)
docs/                راهنمای فارسی و پیش‌نمایش HTML
```

## ساخت مجدد از سورس

پیش‌نیاز: JDK 17، Android SDK Build-Tools 35، Platform android-35.

```bash
export ANDROID_HOME=$HOME/android-sdk
bash source/build.sh      # خروجی: InternetPro.apk
```

## مشخصات فنی

| مورد | مقدار |
|---|---|
| نام بسته | `ir.internetpro.app` |
| حداقل اندروید | 8 (API 26) |
| هدف | Android 15 (API 35) |
| مجوزها | `CALL_PHONE`، `READ_PHONE_STATE`، `READ_PHONE_NUMBERS`، `POST_NOTIFICATIONS`، سرویس دسترس‌پذیری |
| امضا | v2 + v3 (کلید محلی پروژه) |
| کد دستوری | `*10*327#` → پاسخ `1` (روشن) / `2` (خاموش) |

## نکات فنی

- پاسخ مرحله دوم **در همان نشست USSD** ارسال می‌شود (نه با درخواست جدید)، چون اپراتور درخواست
  جداگانه را `invalid code` می‌بیند.
- ارسال خودکار پاسخ از طریق سرویس دسترس‌پذیری انجام می‌شود؛ مسیر جایگزین API رسمی
  `sendUssdRequest` در تنظیمات قابل انتخاب است.
- آیکون دکمه به‌صورت `layer-list` داخل خود دکمه قرار دارد تا مستقل از elevation و ترتیب ویوها
  همیشه دیده شود.

## مجوز

این پروژه برای استفاده شخصی ساخته شده است. فونت‌های استفاده‌شده در تصاویر (Titr Bold، Vazirmatn Bold)
توسط صاحبانشان منتشر شده‌اند و فقط برای رندر تصویر استفاده شده‌اند، نه توزیع فایل فونت.
