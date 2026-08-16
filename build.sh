#!/usr/bin/env bash
# Build script for Parsec Android Mod
# Usage: ./build.sh <path-to-base.apk> <path-to-config.apk> <keystore> <keystore-pass>
#
# Requirements (set via env or edit below):
#   ANDROID_JAR  - android-34 android.jar
#   BUILD_TOOLS  - Android build-tools directory
#   R8_JAR       - R8 8.5.13+ jar
#   BAKSMALI_CP  - baksmali classpath (baksmali.jar:dexlib2.jar:util.jar:guava.jar:jcommander.jar)
#   APKTOOL_JAR  - apktool 2.10.0+ jar

set -euo pipefail

BASE_APK="${1:?Usage: ./build.sh <base.apk> <config.apk> <keystore> <keystore-pass>}"
CONFIG_APK="${2:?}"
KEYSTORE="${3:?}"
KEYSTORE_PASS="${4:?}"

ANDROID_JAR="${ANDROID_JAR:-/opt/android/android.jar}"
BUILD_TOOLS="${BUILD_TOOLS:-/opt/android/build-tools}"
R8_JAR="${R8_JAR:-/opt/android/r8.jar}"
BAKSMALI_CP="${BAKSMALI_CP:-/opt/android/baksmali.jar:/opt/android/dexlib2.jar:/opt/android/util.jar:/opt/android/guava.jar:/opt/android/jcommander.jar}"
APKTOOL_JAR="${APKTOOL_JAR:-/opt/android/apktool.jar}"

WORK=build-work
rm -rf "$WORK"
mkdir -p "$WORK/classes" "$WORK/dex"

echo "[1/8] Decompiling base APK..."
java -jar "$APKTOOL_JAR" d "$BASE_APK" -o "$WORK/apk-orig"

echo "[2/8] Compiling Java sources..."
javac -source 11 -target 11 -classpath "$ANDROID_JAR" -d "$WORK/classes" \
    src/group/matoya/lib/Matoya.java \
    src/tv/parsec/client/MainActivity.java \
    src/tv/parsec/client/R.java

echo "[3/8] Dexing..."
(cd "$WORK/classes" && jar cf ../all.jar .)
java -cp "$R8_JAR" com.android.tools.r8.D8 --min-api 28 \
    --lib "$ANDROID_JAR" --output "$WORK/dex" "$WORK/all.jar"

echo "[4/8] Converting dex to smali..."
java -classpath "$BAKSMALI_CP" org.jf.baksmali.Main d "$WORK/dex/classes.dex" -o "$WORK/smali"

echo "[5/8] Replacing smali..."
rm -rf "$WORK/apk-orig/smali/group" "$WORK/apk-orig/smali/tv"
cp -r "$WORK/smali/"* "$WORK/apk-orig/smali/"

echo "[6/8] Patching manifest and adding native lib..."
sed -i 's/android:requiredSplitTypes="[^"]*"//' "$WORK/apk-orig/AndroidManifest.xml"
sed -i 's/android:splitTypes="[^"]*"//' "$WORK/apk-orig/AndroidManifest.xml"
sed -i '/com.android.vending.splits.required/d' "$WORK/apk-orig/AndroidManifest.xml"
sed -i '/com.android.stamp.source/d' "$WORK/apk-orig/AndroidManifest.xml"
sed -i '/com.android.stamp.type/d' "$WORK/apk-orig/AndroidManifest.xml"
sed -i '/com.android.vending.splits/d' "$WORK/apk-orig/AndroidManifest.xml"
sed -i '/com.android.vending.derived.apk.id/d' "$WORK/apk-orig/AndroidManifest.xml"
sed -i 's/android:extractNativeLibs="false"/android:extractNativeLibs="true"/' "$WORK/apk-orig/AndroidManifest.xml"
mkdir -p "$WORK/apk-orig/lib/arm64-v8a"
unzip -o "$CONFIG_APK" "lib/arm64-v8a/libmain.so" -d "$WORK/native"
cp "$WORK/native/lib/arm64-v8a/libmain.so" "$WORK/apk-orig/lib/arm64-v8a/"

echo "[7/8] Building APK..."
java -jar "$APKTOOL_JAR" b "$WORK/apk-orig" -o "$WORK/mod-unsigned.apk"

echo "[8/8] Signing..."
"$BUILD_TOOLS/zipalign" -f 4 "$WORK/mod-unsigned.apk" "$WORK/mod-aligned.apk"
"$BUILD_TOOLS/apksigner" sign --ks "$KEYSTORE" --ks-pass "pass:$KEYSTORE_PASS" \
    --out parsec-mod.apk "$WORK/mod-aligned.apk"
"$BUILD_TOOLS/apksigner" verify parsec-mod.apk

echo "Done: parsec-mod.apk"
