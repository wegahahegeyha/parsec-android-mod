# Parsec Android Mod - Touchpad, Stylus, Zoom & Portrait

A feature mod for the Parsec Android client (`tv.parsec.client`). Adds touchpad mode, S Pen stylus mouse emulation, pinch-to-zoom, and portrait orientation lock.

## Features

### Stylus (S Pen) as Mouse
- **Hover** moves the cursor (real Windows cursor with correct shapes)
- **Touch** = left click, drag = hold & drag
- **Pen button** while hovering = right click
- Works automatically, no toggle needed

### Touchpad Mode
- Toggle: panel button "Тачпад"
- **1 finger** moves the cursor (relative, like a real touchpad - cursor stays in place when you lift and reposition)
- **Tap** = left click at cursor position
- **Double-tap + hold** = drag (tap quickly twice, hold on second tap, move to drag)
- **Long press without moving** = right click
- **2 fingers** = pinch-to-zoom
- **3 fingers** = scroll
- Cursor overlay shows the real Windows cursor bitmap (sent by host)

### Pinch-to-Zoom
- Toggle: panel button "Зум"
- Pinch with 2 fingers to zoom in/out
- 2 fingers move = pan when zoomed
- 3 fingers = scroll
- Zoom anchored at cursor in touchpad mode (cursor stays fixed on screen)
- Keyboard safe: zoom resets when keyboard opens, function keys stay anchored

### Portrait Orientation Lock
- Toggle: panel button "Портрет"
- Forces portrait orientation (like AnyDesk - stream on top, keyboard space below)
- Works with all resolutions
- Native orientation settings cannot override it while enabled

### Panel UI
- Appears when tapping the **Parsec button** (top-left in landscape, top-right in portrait)
- Three toggle buttons: Тачпад (touchpad), Зум (zoom), Портрет (portrait)
- Active state shown with green background
- Tap outside the panel or tap the Parsec button again to close

## Requirements

- Android 9.0+ (API 28)
- Uninstall the official Parsec APK first (signature conflict)

## Installation

1. Download the APK from [Releases](../../releases)
2. Uninstall any existing Parsec installation
3. Enable "Install from unknown sources" on your device
4. Install the APK

## Building from Source

### Prerequisites

- JDK 11+
- Android SDK platform (android-34)
- Build tools (34.0.0+)
- [Apktool](https://apktool.org/) 2.10.0+
- [R8](https://r8.googlesource.com/r8) 8.5.13+ (for dexing)
- [Baksmali](https://github.com/google/smali) 2.5.2+

### Build Steps

```bash
# 1. Download the original Parsec APK (split APK from Google Play)
#    You need tv.parsec.client.apk (base) + config.arm64_v8a.apk (native lib)

# 2. Setup
export ANDROID_JAR=/path/to/android-34/android.jar
export BUILD_TOOLS=/path/to/build-tools/34.0.0
export R8_JAR=/path/to/r8.jar
export BAKSMALI_CP="baksmali.jar:dexlib2.jar:util.jar:guava.jar:jcommander.jar"

# 3. Decompile original APK for resources
java -jar apktool.jar d tv.parsec.client.apk -o apk-orig

# 4. Compile modified Java sources
mkdir -p build/classes build/dex
javac -source 11 -target 11 -classpath $ANDROID_JAR -d build/classes \
    src/group/matoya/lib/Matoya.java \
    src/tv/parsec/client/MainActivity.java \
    src/tv/parsec/client/R.java

# 5. Dex
cd build/classes && jar cf ../all.jar . && cd ../..
java -cp $R8_JAR com.android.tools.r8.D8 --min-api 28 \
    --lib $ANDROID_JAR --output build/dex build/all.jar

# 6. Convert dex to smali
java -classpath "$BAKSMALI_CP" org.jf.baksmali.Main d build/dex/classes.dex -o build/smali

# 7. Replace smali in apk-orig
rm -rf apk-orig/smali/group apk-orig/smali/tv
cp -r build/smali/* apk-orig/smali/

# 8. Patch manifest (remove split requirements)
#    Edit AndroidManifest.xml:
#    - Remove android:requiredSplitTypes, android:splitTypes
#    - Remove com.android.vending.* meta-data
#    - Change extractNativeLibs to "true"
#    - Add lib/arm64-v8a/libmain.so from config.arm64_v8a.apk

# 9. Build APK
java -jar apktool.jar b apk-orig -o build/mod-unsigned.apk

# 10. Sign
$BUILD_TOOLS/zipalign -f 4 build/mod-unsigned.apk build/mod-aligned.apk
$BUILD_TOOLS/apksigner sign --ks your.keystore --out build/parsec-mod.apk build/mod-aligned.apk
```

## How It Works

The Parsec Android client is a thin Java wrapper around a native C++ engine (libmain.so). The Java layer consists of:

- **MainActivity** - simple activity that creates the Matoya view
- **Matoya** - SurfaceView subclass that handles rendering, input, and JNI bridge to native
- **R** - resource identifiers

The mod modifies only the Java layer. The modifications intercept touch/stylus events at the Java level and translate them into mouse events (via the existing JNI methods `app_mouse_motion`, `app_mouse_button`, etc.) instead of touch events (`app_unhandled_touch`). The native library is untouched.

### Key JNI Methods Used

- `app_mouse_motion(boolean relative, float x, float y)` - move cursor (absolute or relative)
- `app_mouse_button(boolean down, int button, float x, float y)` - mouse button (1=left, 2=right)
- `app_scroll(float x, float y, float dx, float dy, int pointerCount)` - scroll
- `app_unhandled_touch(int action, float x, float y, int pointerCount)` - raw touch (used for normal mode)
- `showCursor(boolean)` / `setCursorRGBA(int[], int, int, float, float)` - cursor rendering

## License

This project is a mod for Parsec (proprietary software by Parsec Cloud, Inc.). The modification code is provided under the MIT License. Parsec's original code is not included - only the modified source files are provided.

## Disclaimer

This is an unofficial modification. Parsec Cloud, Inc. is not affiliated with this project. Use at your own risk. The mod does not modify the native library (libmain.so) or the streaming protocol, so it should not affect compatibility with Parsec servers or hosts.