#!/bin/bash
set -e
export JAVA_HOME=~/.local/jdk/jdk-17.0.13+11
export PATH=$JAVA_HOME/bin:$PATH
SDK=~/android-sdk
BT=$SDK/build-tools/35.0.0
PLATFORM=$SDK/platforms/android-35/android.jar
SRC=~/InternetPro/app/src/main
OUT=~/InternetPro/build
rm -rf $OUT && mkdir -p $OUT/compiled $OUT/gen $OUT/classes $OUT/dex

echo "== aapt2 compile =="
$BT/aapt2 compile --dir $SRC/res -o $OUT/res.zip

echo "== aapt2 link =="
$BT/aapt2 link -o $OUT/base.apk -I $PLATFORM --manifest $SRC/AndroidManifest.xml \
  -R $OUT/res.zip --java $OUT/gen --auto-add-overlay

echo "== javac =="
find $SRC/java $OUT/gen -name "*.java" > $OUT/sources.txt
javac -source 8 -target 8 -encoding UTF-8 -bootclasspath $PLATFORM \
  -d $OUT/classes @$OUT/sources.txt

echo "== d8 =="
find $OUT/classes -name "*.class" > $OUT/classlist.txt
$BT/d8 --release --min-api 26 --lib $PLATFORM --output $OUT/dex $(cat $OUT/classlist.txt)

echo "== package =="
cd $OUT && cp base.apk unsigned.apk && cd $OUT/dex && zip -q ../unsigned.apk classes.dex && cd $OUT
$BT/zipalign -f 4 unsigned.apk aligned.apk

echo "== sign =="
if [ ! -f ~/InternetPro/keystore.jks ]; then
  keytool -genkeypair -v -keystore ~/InternetPro/keystore.jks -alias internetpro \
    -keyalg RSA -keysize 2048 -validity 10950 \
    -storepass internetpro -keypass internetpro \
    -dname "CN=Internet Pro, OU=Mobile, O=InternetPro, L=Tehran, C=IR" > /dev/null 2>&1
fi
$BT/apksigner sign --ks ~/InternetPro/keystore.jks --ks-pass pass:internetpro \
  --key-pass pass:internetpro --v1-signing-enabled true --v2-signing-enabled true \
  --out ~/InternetPro/InternetPro.apk aligned.apk
$BT/apksigner verify --print-certs ~/InternetPro/InternetPro.apk | head -5
ls -la ~/InternetPro/InternetPro.apk
