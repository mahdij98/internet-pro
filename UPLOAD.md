# آپلود این بسته روی گیت‌هاب

## روش ۱: ساخت مخزن جدید از همین پوشه (سریع‌ترین)

```bash
cd internet-pro-repo
git init -b main
git add .
git commit -m "Internet Pro 1.0 - 1.7 (APK + source)"
```

سپس روی گیت‌هاب یک مخزن خالی بسازید (مثلاً `internet-pro`, بدون README)، و:

```bash
git remote add origin https://github.com/USERNAME/internet-pro.git
git push -u origin main
```

اگر احراز هویت دو مرحله‌ای دارید، به‌جای رمز از **Personal Access Token** استفاده کنید:
GitHub → Settings → Developer settings → Personal access tokens → Tokens (classic) → scope: `repo`.

## روش ۲: افزودن به مخزنی که از قبل دارید

```bash
git clone https://github.com/USERNAME/REPO.git
cp -r internet-pro-repo/* REPO/
cd REPO
git add .
git commit -m "Add Internet Pro APKs (1.0–1.7) and source"
git push
```

## روش ۳: بدون خط فرمان (فقط مرورگر)

1. در گیت‌هاب مخزن جدید بسازید.
2. دکمه **Add file → Upload files** را بزنید.
3. فایل `internet-pro-repo.zip` را از حالت فشرده خارج کنید و محتویات را بکشید و رها کنید.
4. **Commit changes** را بزنید.

## روش ۴: انتشار به‌عنوان Release (توصیه‌شده برای APK)

```bash
gh release create v1.7 \
  releases/InternetPro-1.7.apk \
  releases/InternetPro-latest.apk \
  --title "Internet Pro 1.7" \
  --notes "آیکون پاور اپلیکیشن در مرکز دکمه"
```

(نیازمند نصب [GitHub CLI](https://cli.github.com) و `gh auth login`)

## نکته‌ها

- فایل `keystore.jks` **در این بسته نیست** (کلید امضا). آن را جای امنی نگه دارید؛ برای
  آپدیت کردن اپ باید همان کلید باشد. کلید فعلی در `~/InternetPro/keystore.jks` در محیط ساخت است.
- GitHub هیچ‌وقت منتشرکننده ناشناس نمی‌پذیرد؛ آپلود باید با توکن/ورود خودتان انجام شود.
