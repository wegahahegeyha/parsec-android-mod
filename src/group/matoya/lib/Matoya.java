package group.matoya.lib;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.hardware.input.InputManager;
import android.net.Uri;
import android.os.SystemClock;
import android.os.Vibrator;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.Toast;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.InputEvent;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.PointerIcon;
import android.view.ScaleGestureDetector;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Scroller;
import java.util.List;

/* loaded from: classes.dex */
public class Matoya extends SurfaceView implements SurfaceHolder.Callback, InputManager.InputDeviceListener, GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener, GestureDetector.OnContextClickListener, ScaleGestureDetector.OnScaleGestureListener, ClipboardManager.OnPrimaryClipChangedListener {

    Activity activity;
    PointerIcon cursor;
    boolean defaultCursor;
    GestureDetector detector;
    float displayDensity;
    boolean hiddenCursor;
    PointerIcon invisCursor;
    boolean kbShowing;
    KeyCharacterMap kbmap;
    int scrollY;
    Scroller scroller;
    ScaleGestureDetector sdetector;
    Vibrator vibrator;

    // --- MOD: state ---
    private boolean touchpadMode = false;
    private boolean zoomEnabled = false;
    private float zoomScale = 1.0f;
    private float zoomOffsetX = 0.0f;
    private float zoomOffsetY = 0.0f;
    private boolean panning = false;
    private float lastCX = 0.0f;
    private float lastCY = 0.0f;
    private float lastX = 0.0f;
    private float lastY = 0.0f;
    private long downTime = 0L;
    private boolean fingerMoved = false;
    private float totalMove = 0.0f;
    private boolean dragMode = false;
    private boolean dragButtonPressed = false;
    private boolean wasMultiTouch = false;
    private boolean touchStartInCorner = false;
    private boolean inStream = false;
    private boolean inSession = true;
    private long lastTapTime = 0L;
    private float lastTapX = 0.0f;
    private float lastTapY = 0.0f;
    private boolean stylusBtnDown = false;
    private boolean stylusTouching = false;
    private int stylusButton = 0;
    // touchpad relative cursor position
    private float cursorX = -1.0f;
    private float cursorY = -1.0f;

    // --- MOD: overlay ---
    private CursorView cursorView;
    private LinearLayout panel;
    private boolean panelVisible = false;
    private Bitmap cursorBitmap = null;
    private float cursorHotX = 0.0f;
    private float cursorHotY = 0.0f;
    private Button touchpadBtn;
    private Button zoomBtn;
    private Button orientBtn;
    private boolean portraitMode = false;

    // --- MOD: resolution menu ---
    private boolean resMenuVisible = false;
    private LinearLayout resPanel;
    private Button resBtn;
    private int customWidth = 0;
    private int customHeight = 0;
    private int selectedResIndex = -1;
    private SharedPreferences prefs;
    private static final String PREFS_NAME = "parsec_mod_res";

    // Resolution presets: {width, height, label}
    // Original ratio 16:9, new 19.5:9 (S24 Ultra), portrait 3:4, custom
    private static final int[][] RES_PRESETS = {
        {3840, 2160}, // 16:9 Ultra HD
        {2560, 1440}, // 16:9 QHD
        {1920, 1080}, // 16:9 Full HD
        {1680, 945},  // 16:9
        {1600, 900},  // 16:9
        {1366, 768},  // 16:9
        {1280, 720},  // 16:9
        {2560, 1182}, // 19.5:9
        {1920, 886},  // 19.5:9
        {1680, 776},  // 19.5:9 (S24 Ultra, ~87% of 1080p)
        {1600, 738},  // 19.5:9
        {1440, 665},  // 19.5:9
        {1280, 591},  // 19.5:9
        // Portrait / tall for landscape phone with keyboard gap
        {1440, 1080}, // 4:3 portrait-style (38% keyboard gap)
        {1280, 960},  // 4:3
        {1080, 810},  // 4:3
        {900, 675},   // 4:3
    };

    private static final float[] RES_ASPECTS = {
        16f/9f, 16f/9f, 16f/9f, 16f/9f, 16f/9f, 16f/9f, 16f/9f,
        19.5f/9f, 19.5f/9f, 19.5f/9f, 19.5f/9f, 19.5f/9f, 19.5f/9f,
        4f/3f, 4f/3f, 4f/3f, 4f/3f,
    };

    private static final String[] RES_LABELS = {
        "3840x2160 (16:9)",
        "2560x1440 (16:9)",
        "1920x1080 (16:9)",
        "1680x945 (16:9)",
        "1600x900 (16:9)",
        "1366x768 (16:9)",
        "1280x720 (16:9)",
        "2560x1182 (19.5:9)",
        "1920x886 (19.5:9)",
        "1680x776 (19.5:9)",
        "1600x738 (19.5:9)",
        "1440x665 (19.5:9)",
        "1280x591 (19.5:9)",
        "1440x1080 (4:3)",
        "1280x960 (4:3)",
        "1080x810 (4:3)",
        "900x675 (4:3)",
    };

    private static final float MAX_ZOOM = 4.0f;
    private static final float TAP_SLOP_DP = 8.0f;
    private static final long TAP_TIMEOUT_MS = 350L;
    private static final long LONG_PRESS_MS = 600L;
    private static final int CORNER_DP = 220;
    private static final float SCALE_EPSILON = 0.01f;
    private static final float PAN_DEADZONE_DP = 2.0f;

    static int fullscreenFlags() {
        return 5382;
    }

    native void app_axis(int i, float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, float f9, float f10);

    native void app_button(int i, boolean z, int i2, boolean z2);

    native void app_check_scroller(boolean z);

    native void app_generic_scroll(float f, float f2);

    native boolean app_key(boolean z, int i, String str, int i2, boolean z2);

    native boolean app_long_press(float f, float f2);

    native void app_mouse_button(boolean z, int i, float f, float f2);

    native void app_mouse_motion(boolean z, float f, float f2);

    native void app_scroll(float f, float f2, float f3, float f4, int i);

    native void app_single_tap_up(float f, float f2);

    native void app_start();

    native void app_stop();

    native void app_unhandled_touch(int i, float f, float f2, int i2);

    native void app_unplug(int i);

    native void gfx_resize(int i, int i2);

    native void gfx_set_surface(Surface surface);

    native void gfx_unset_surface();

    static {
        System.loadLibrary("main");
    }

    public Matoya(Activity activity) {
        super(activity);
        this.activity = activity;
        this.kbmap = KeyCharacterMap.load(-1);
        this.vibrator = (Vibrator) activity.getSystemService("vibrator");
        this.detector = new GestureDetector(activity, this);
        this.sdetector = new ScaleGestureDetector(activity, this);
        this.scroller = new Scroller(activity);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        this.activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        this.displayDensity = displayMetrics.density;
        byte[] decode = Base64.decode("iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAQAAADZc7J/AAAAH0lEQVR42mNkoBAwjhowasCoAaMGjBowasCoAcPNAACOMAAhOO/A7wAAAABJRU5ErkJggg==", 0);
        this.invisCursor = PointerIcon.create(BitmapFactory.decodeByteArray(decode, 0, decode.length, null), 0.0f, 0.0f);
        ((ClipboardManager) this.activity.getSystemService("clipboard")).addPrimaryClipChangedListener(this);
        ((InputManager) activity.getSystemService("input")).registerInputDeviceListener(this, null);
        getHolder().addCallback(this);
        this.detector.setOnDoubleTapListener(this);
        this.detector.setContextClickListener(this);
        setFilterTouchesWhenObscured(true);
        setFocusableInTouchMode(true);
        setFocusable(true);
        requestFocus();
        app_start();
    }

    // --- MOD: orientation check ---
    private boolean isLandscape() {
        return getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    // --- MOD: attach overlay UI (called by MainActivity after adding to layout) ---
    public void attachOverlays(ViewGroup root) {
        this.cursorView = new CursorView(getContext());
        this.cursorView.setVisibility(View.GONE);
        root.addView(this.cursorView, new FrameLayout.LayoutParams(dp(96), dp(96)));
        buildPanel(root);
    }

    public void updatePanelPosition() {
        if (this.panel == null) {
            return;
        }
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) this.panel.getLayoutParams();
        if (isLandscape()) {
            lp.gravity = Gravity.TOP | Gravity.LEFT;
            lp.topMargin = dp(100);
            lp.leftMargin = dp(16);
            lp.rightMargin = -1;
        } else {
            lp.gravity = Gravity.TOP | Gravity.RIGHT;
            lp.topMargin = dp(120);
            lp.rightMargin = dp(12);
            lp.leftMargin = -1;
        }
        this.panel.setLayoutParams(lp);
    }

    private void buildPanel(ViewGroup root) {
        this.panel = new LinearLayout(getContext());
        this.panel.setOrientation(LinearLayout.VERTICAL);
        this.panel.setBackgroundColor(0xCC1E1E1E);
        int pad = dp(8);
        this.panel.setPadding(pad, pad, pad, pad);
        this.panel.setVisibility(View.GONE);
        this.panel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Matoya.this.hidePanel();
            }
        });

        this.touchpadBtn = new Button(getContext());
        this.touchpadBtn.setText("Touchpad");
        this.touchpadBtn.setTextSize(14.0f);
        this.touchpadBtn.setAllCaps(false);
        this.touchpadBtn.setPadding(dp(10), dp(4), dp(10), dp(4));
        this.touchpadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Matoya.this.setTouchpadMode(!Matoya.this.touchpadMode);
                Matoya.this.refreshButtons();
            }
        });

        this.zoomBtn = new Button(getContext());
        this.zoomBtn.setText("Zoom");
        this.zoomBtn.setTextSize(14.0f);
        this.zoomBtn.setAllCaps(false);
        this.zoomBtn.setPadding(dp(10), dp(4), dp(10), dp(4));
        this.zoomBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Matoya.this.setZoomEnabled(!Matoya.this.zoomEnabled);
                Matoya.this.refreshButtons();
            }
        });

        this.orientBtn = new Button(getContext());
        this.orientBtn.setText("Portrait");
        this.orientBtn.setTextSize(14.0f);
        this.orientBtn.setAllCaps(false);
        this.orientBtn.setPadding(dp(10), dp(4), dp(10), dp(4));
        this.orientBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Matoya.this.togglePortraitMode();
            }
        });

        // --- MOD: resolution sub-panel ---
        this.prefs = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.customWidth = this.prefs.getInt("custom_w", 0);
        this.customHeight = this.prefs.getInt("custom_h", 0);
        this.selectedResIndex = this.prefs.getInt("res_index", -1);

        this.resBtn = new Button(getContext());
        this.resBtn.setText("Resolution");
        this.resBtn.setTextSize(14.0f);
        this.resBtn.setAllCaps(false);
        this.resBtn.setPadding(dp(10), dp(4), dp(10), dp(4));
        this.resBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Matoya.this.toggleResMenu();
            }
        });

        this.panel.addView(this.touchpadBtn);
        this.panel.addView(this.zoomBtn);
        this.panel.addView(this.orientBtn);
        this.panel.addView(this.resBtn);

        // Build hidden resolution sub-menu
        this.resPanel = new LinearLayout(getContext());
        this.resPanel.setOrientation(LinearLayout.VERTICAL);
        this.resPanel.setBackgroundColor(0xCC1E1E1E);
        this.resPanel.setPadding(dp(8), dp(8), dp(8), dp(8));
        this.resPanel.setVisibility(View.GONE);
        buildResPresetButtons();
        // Custom row
        Button customBtn = new Button(getContext());
        customBtn.setText("Custom...");
        customBtn.setTextSize(13.0f);
        customBtn.setAllCaps(false);
        customBtn.setPadding(dp(8), dp(3), dp(8), dp(3));
        customBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Matoya.this.showCustomDialog();
            }
        });
        this.resPanel.addView(customBtn);
        this.panel.addView(this.resPanel);

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        if (isLandscape()) {
            lp.gravity = Gravity.TOP | Gravity.LEFT;
            lp.topMargin = dp(100);
            lp.leftMargin = dp(16);
        } else {
            lp.gravity = Gravity.TOP | Gravity.RIGHT;
            lp.topMargin = dp(120);
            lp.rightMargin = dp(12);
        }
        root.addView(this.panel, lp);
        refreshButtons();
    }

    private void refreshButtons() {
        if (this.touchpadBtn == null) {
            return;
        }
        this.touchpadBtn.setBackgroundColor(this.touchpadMode ? Color.parseColor("#2E7D32") : Color.parseColor("#555555"));
        this.touchpadBtn.setTextColor(this.touchpadMode ? Color.WHITE : Color.LTGRAY);
        this.zoomBtn.setBackgroundColor(this.zoomEnabled ? Color.parseColor("#2E7D32") : Color.parseColor("#555555"));
        this.zoomBtn.setTextColor(this.zoomEnabled ? Color.WHITE : Color.LTGRAY);
        this.orientBtn.setBackgroundColor(this.portraitMode ? Color.parseColor("#2E7D32") : Color.parseColor("#555555"));
        this.orientBtn.setTextColor(this.portraitMode ? Color.WHITE : Color.LTGRAY);
        this.resBtn.setBackgroundColor(this.resMenuVisible ? Color.parseColor("#2E7D32") : Color.parseColor("#555555"));
        this.resBtn.setTextColor(Color.WHITE);
    }

    // --- MOD: resolution menu ---
    private void buildResPresetButtons() {
        this.resPanel.removeAllViews();
        for (int i = 0; i < RES_PRESETS.length; i++) {
            final int idx = i;
            Button b = new Button(getContext());
            b.setText(RES_LABELS[i]);
            b.setTextSize(12.0f);
            b.setAllCaps(false);
            b.setPadding(dp(6), dp(2), dp(6), dp(2));
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Matoya.this.selectResPreset(idx);
                }
            });
            if (this.selectedResIndex == i) {
                b.setBackgroundColor(Color.parseColor("#2E7D32"));
                b.setTextColor(Color.WHITE);
            } else {
                b.setBackgroundColor(Color.parseColor("#3A3A3A"));
                b.setTextColor(Color.LTGRAY);
            }
            this.resPanel.addView(b);
        }
        // Re-add custom button at the end
        Button customBtn = new Button(getContext());
        customBtn.setText("Custom...");
        customBtn.setTextSize(13.0f);
        customBtn.setAllCaps(false);
        customBtn.setPadding(dp(8), dp(3), dp(8), dp(3));
        customBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Matoya.this.showCustomDialog();
            }
        });
        this.resPanel.addView(customBtn);
    }

    private void selectResPreset(int idx) {
        if (idx >= 0 && idx < RES_PRESETS.length) {
            this.selectedResIndex = idx;
            saveRes(idx, RES_PRESETS[idx][0], RES_PRESETS[idx][1]);
            applyResolution(RES_PRESETS[idx][0], RES_PRESETS[idx][1]);
            // Highlight selection
            buildResPresetButtons();
            refreshButtons();
        }
    }

    private void applyResolution(int w, int h) {
        if (w <= 0 || h <= 0) return;
        Toast.makeText(getContext(), "Apply " + w + "x" + h, Toast.LENGTH_SHORT).show();
        gfx_resize(w, h);
    }

    private void showCustomDialog() {
        // Ask width, compute height by ratio, save
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Custom Resolution");
        builder.setMessage("Enter width (e.g. 1680). Height computed by aspect (default 19.5:9).");
        final EditText input = new EditText(getContext());
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setHint("Width");
        builder.setView(input);
        builder.setPositiveButton("OK", null);
        builder.setNegativeButton("Cancel", null);
        final AlertDialog dialog = builder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(final android.content.DialogInterface d) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String str = input.getText().toString().trim();
                        if (str.isEmpty()) { return; }
                        try {
                            int w = Integer.parseInt(str);
                            if (w < 400 || w > 4000) {
                                Toast.makeText(getContext(), "Width must be 400-4000", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            // Compute height by 19.5:9 ratio, ensure even
                            int h = (int) Math.round((float) w * 9.0f / 19.5f);
                            if (h % 2 != 0) h--;
                            Matoya.this.customWidth = w;
                            Matoya.this.customHeight = h;
                            Matoya.this.selectedResIndex = -1;
                            Matoya.this.saveRes(-1, w, h);
                            Matoya.this.applyResolution(w, h);
                            Matoya.this.buildResPresetButtons();
                            Matoya.this.refreshButtons();
                            d.dismiss();
                        } catch (NumberFormatException e) {
                            Toast.makeText(getContext(), "Invalid number", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
        dialog.show();
    }

    private void saveRes(int idx, int w, int h) {
        this.prefs.edit().putInt("res_index", idx).putInt("custom_w", w).putInt("custom_h", h).apply();
    }

    private void toggleResMenu() {
        if (this.resPanel == null) return;
        if (this.resPanel.getVisibility() == View.VISIBLE) {
            this.resPanel.setVisibility(View.GONE);
            this.resMenuVisible = false;
        } else {
            // If custom was selected, show it in menu
            if (this.selectedResIndex >= 0) {
                buildResPresetButtons();
            }
            this.resPanel.setVisibility(View.VISIBLE);
            this.resMenuVisible = true;
        }
        refreshButtons();
    }

    private void togglePortraitMode() {
        this.portraitMode = !this.portraitMode;
        if (this.portraitMode) {
            this.activity.setRequestedOrientation(1); // SCREEN_ORIENTATION_PORTRAIT
        } else {
            this.activity.setRequestedOrientation(6); // SCREEN_ORIENTATION_LANDSCAPE
        }
        refreshButtons();
    }

    void togglePanel() {
        if (this.panelVisible) {
            hidePanel();
        } else {
            showPanel();
        }
    }

    void showPanel() {
        if (this.panel == null) {
            return;
        }
        updatePanelPosition();
        refreshButtons();
        this.panel.setVisibility(View.VISIBLE);
        this.panelVisible = true;
    }

    void hidePanel() {
        if (this.panel == null) {
            return;
        }
        this.panel.setVisibility(View.GONE);
        this.panelVisible = false;
    }

    // --- MOD: public toggles ---
    public void setTouchpadMode(boolean on) {
        this.touchpadMode = on;
        if (on) {
            // init cursor at screen center (stream coords)
            this.cursorX = (getWidth() / 2.0f - this.zoomOffsetX) / this.zoomScale;
            this.cursorY = (getHeight() / 2.0f - this.zoomOffsetY) / this.zoomScale;
            syncTouchpadOverlay();
        } else if (this.cursorView != null) {
            this.cursorView.setVisibility(View.GONE);
        }
    }

    public boolean getTouchpadMode() {
        return this.touchpadMode;
    }

    public void setZoomEnabled(boolean on) {
        this.zoomEnabled = on;
        if (!on) {
            this.zoomScale = 1.0f;
            this.zoomOffsetX = 0.0f;
            this.zoomOffsetY = 0.0f;
            applyZoom();
        }
    }

    public boolean getZoomEnabled() {
        return this.zoomEnabled;
    }

    // --- MOD: helpers ---
    private int dp(int v) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, (float) v, getResources().getDisplayMetrics()));
    }

    private int dpf(float v) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics()));
    }

    private float clamp(float v, float lo, float hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    private float mapX(float x) {
        return (x - this.zoomOffsetX) / this.zoomScale;
    }

    private float mapY(float y) {
        return (y - this.zoomOffsetY) / this.zoomScale;
    }

    private float minOX() {
        return getWidth() * (1.0f - this.zoomScale);
    }

    private float minOY() {
        return getHeight() * (1.0f - this.zoomScale);
    }

    private float centroidX(MotionEvent e) {
        float s = 0.0f;
        int n = e.getPointerCount();
        for (int i = 0; i < n; i++) {
            s += e.getX(i);
        }
        return s / n;
    }

    private float centroidY(MotionEvent e) {
        float s = 0.0f;
        int n = e.getPointerCount();
        for (int i = 0; i < n; i++) {
            s += e.getY(i);
        }
        return s / n;
    }

    private void applyZoom() {
        if (this.zoomScale <= 1.0f) {
            this.zoomScale = 1.0f;
            this.zoomOffsetX = 0.0f;
            this.zoomOffsetY = 0.0f;
        }
        setPivotX(0.0f);
        setPivotY(0.0f);
        setScaleX(this.zoomScale);
        setScaleY(this.zoomScale);
        setTranslationX(this.zoomOffsetX);
        setTranslationY(this.zoomOffsetY);
        // move the overlay to follow the stream point (cursor stays at same stream pos)
        if (this.touchpadMode) {
            syncTouchpadOverlay();
        }
    }

    private boolean inCorner(float x, float y) {
        int c = isLandscape() ? dp(220) : dp(90);
        if (isLandscape()) {
            // landscape: Parsec button is top-left
            return x <= c && y <= c;
        } else {
            // portrait: Parsec button is top-right
            return x >= getWidth() - c && y <= c;
        }
    }

    private boolean isStylusEvent(MotionEvent e) {
        return e.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS;
    }

    // --- MOD: stylus touch = left mouse only (right-click via hover+button) ---
    private boolean handleStylusTouch(MotionEvent e) {
        if (this.cursorView != null) {
            this.cursorView.setVisibility(View.GONE);
        }
        int action = e.getActionMasked();
        float x = mapX(e.getX(0));
        float y = mapY(e.getY(0));
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                this.stylusTouching = true;
                this.stylusButton = 1;
                app_mouse_button(true, 1, x, y);
                return true;
            case MotionEvent.ACTION_MOVE:
                app_mouse_motion(false, x, y);
                return true;
            case MotionEvent.ACTION_UP:
                this.stylusTouching = false;
                if (this.stylusButton != 0) {
                    app_mouse_button(false, this.stylusButton, x, y);
                    this.stylusButton = 0;
                }
                return true;
            case MotionEvent.ACTION_CANCEL:
                this.stylusTouching = false;
                if (this.stylusButton != 0) {
                    app_mouse_button(false, this.stylusButton, x, y);
                    this.stylusButton = 0;
                }
                return true;
            default:
                return true;
        }
    }

    // --- MOD: touchpad relative mode ---
    private boolean handleTouchpad(MotionEvent e) {
        int action = e.getActionMasked();
        int pc = e.getPointerCount();
        if (pc >= 2) {
            this.wasMultiTouch = true;
        }
        if (pc >= 3) {
            // 3-finger scroll - use cursor position so it doesn't jump
            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
                this.lastCX = centroidX(e);
                this.lastCY = centroidY(e);
            } else if (action == MotionEvent.ACTION_MOVE) {
                float cx = centroidX(e);
                float cy = centroidY(e);
                float sx = this.cursorX >= 0.0f ? this.cursorX * this.zoomScale + this.zoomOffsetX : -1.0f;
                float sy = this.cursorY >= 0.0f ? this.cursorY * this.zoomScale + this.zoomOffsetY : -1.0f;
                app_scroll(sx, sy, this.lastCX - cx, this.lastCY - cy, pc);
                this.lastCX = cx;
                this.lastCY = cy;
            }
            return true;
        }
        if (pc == 2) {
            // 2-finger pinch-zoom + pan
            // if a drag was in progress, release the button
            if (this.dragMode) {
                this.dragMode = false;
                this.dragButtonPressed = false;
                if (this.cursorX >= 0.0f) {
                    app_mouse_button(false, 1, this.cursorX, this.cursorY);
                }
            }
            this.sdetector.onTouchEvent(e);
            if (action == MotionEvent.ACTION_MOVE && this.zoomScale > 1.0f) {
                float cx = centroidX(e);
                float cy = centroidY(e);
                if (this.panning) {
                    float pdx = cx - this.lastCX;
                    float pdy = cy - this.lastCY;
                    if (Math.abs(pdx) + Math.abs(pdy) >= dpf(PAN_DEADZONE_DP)) {
                        this.zoomOffsetX = clamp(this.zoomOffsetX + pdx, minOX(), 0.0f);
                        this.zoomOffsetY = clamp(this.zoomOffsetY + pdy, minOY(), 0.0f);
                        // keep the phone cursor fixed: compensate its stream position
                        if (this.cursorX >= 0.0f) {
                            this.cursorX = clamp(this.cursorX - pdx / this.zoomScale, 0.0f, (float) getWidth());
                            this.cursorY = clamp(this.cursorY - pdy / this.zoomScale, 0.0f, (float) getHeight());
                            app_mouse_motion(false, this.cursorX, this.cursorY);
                        }
                        applyZoom();
                    }
                }
                this.lastCX = cx;
                this.lastCY = cy;
                this.panning = true;
            }
            if (action == MotionEvent.ACTION_POINTER_UP || action == MotionEvent.ACTION_UP) {
                this.panning = false;
            }
            return true;
        }
        // 1 finger = relative mouse cursor (cursorX/Y in STREAM coordinates)
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                this.wasMultiTouch = false;
                this.lastX = e.getX(0);
                this.lastY = e.getY(0);
                this.downTime = SystemClock.uptimeMillis();
                this.fingerMoved = false;
                this.totalMove = 0.0f;
                // check for double-tap to start drag
                if (this.lastTapTime != 0L) {
                    long sinceLast = SystemClock.uptimeMillis() - this.lastTapTime;
                    float dist = Math.abs(e.getX(0) - this.lastTapX) + Math.abs(e.getY(0) - this.lastTapY);
                    if (sinceLast < 400L && dist < dpf(150.0f)) {
                        // second tap: enter drag mode, press button on first MOVE
                        this.dragMode = true;
                        this.dragButtonPressed = false;
                        this.lastTapTime = 0L;
                        this.lastX = e.getX(0);
                        this.lastY = e.getY(0);
                        initCursorIfNeeded();
                        syncTouchpadOverlay();
                        return true;
                    }
                }
                return true;
            case MotionEvent.ACTION_MOVE:
                if (this.wasMultiTouch) {
                    // re-baseline after multi-finger gesture (prevents cursor teleport)
                    this.lastX = e.getX(0);
                    this.lastY = e.getY(0);
                    this.wasMultiTouch = false;
                    this.fingerMoved = true;
                    return true;
                }
                if (this.dragMode && !this.dragButtonPressed) {
                    // first move after drag start - press button on movement
                    float mx = e.getX(0);
                    float my = e.getY(0);
                    float dx = mx - this.lastX;
                    float dy = my - this.lastY;
                    this.lastX = mx;
                    this.lastY = my;
                    if (dx != 0.0f || dy != 0.0f) {
                        this.dragButtonPressed = true;
                        this.fingerMoved = true;
                        initCursorIfNeeded();
                        app_mouse_button(true, 1, this.cursorX, this.cursorY);
                        this.cursorX = clamp(this.cursorX + dx / this.zoomScale, 0.0f, (float) getWidth());
                        this.cursorY = clamp(this.cursorY + dy / this.zoomScale, 0.0f, (float) getHeight());
                        syncTouchpadOverlay();
                        app_mouse_motion(false, this.cursorX, this.cursorY);
                    }
                    return true;
                }
                if (this.dragMode && this.dragButtonPressed) {
                    float mx = e.getX(0);
                    float my = e.getY(0);
                    float dx = mx - this.lastX;
                    float dy = my - this.lastY;
                    this.lastX = mx;
                    this.lastY = my;
                    if (dx != 0.0f || dy != 0.0f) {
                        this.fingerMoved = true;
                        initCursorIfNeeded();
                        this.cursorX = clamp(this.cursorX + dx / this.zoomScale, 0.0f, (float) getWidth());
                        this.cursorY = clamp(this.cursorY + dy / this.zoomScale, 0.0f, (float) getHeight());
                        syncTouchpadOverlay();
                        app_mouse_motion(false, this.cursorX, this.cursorY);
                    }
                    return true;
                }
                float mx = e.getX(0);
                float my = e.getY(0);
                float dx = mx - this.lastX;
                float dy = my - this.lastY;
                this.lastX = mx;
                this.lastY = my;
                if (dx != 0.0f || dy != 0.0f) {
                    this.totalMove += Math.abs(dx) + Math.abs(dy);
                    if (this.totalMove > dpf(TAP_SLOP_DP)) {
                        this.fingerMoved = true;
                    }
                    initCursorIfNeeded();
                    // convert screen delta to stream delta
                    this.cursorX = clamp(this.cursorX + dx / this.zoomScale, 0.0f, (float) getWidth());
                    this.cursorY = clamp(this.cursorY + dy / this.zoomScale, 0.0f, (float) getHeight());
                    syncTouchpadOverlay();
                    app_mouse_motion(false, this.cursorX, this.cursorY);
                }
                return true;
            case MotionEvent.ACTION_UP: {
                if (this.wasMultiTouch) {
                    this.wasMultiTouch = false;
                    this.fingerMoved = true;
                }
                if (this.dragMode) {
                    this.dragMode = false;
                    if (this.dragButtonPressed) {
                        // drag ended: release button
                        this.dragButtonPressed = false;
                        initCursorIfNeeded();
                        app_mouse_button(false, 1, this.cursorX, this.cursorY);
                    } else {
                        // double-tap without movement = double-click
                        this.dragButtonPressed = false;
                        initCursorIfNeeded();
                        app_mouse_button(true, 1, this.cursorX, this.cursorY);
                        app_mouse_button(false, 1, this.cursorX, this.cursorY);
                        this.lastTapTime = SystemClock.uptimeMillis();
                        this.lastTapX = e.getX(0);
                        this.lastTapY = e.getY(0);
                    }
                    return true;
                }
                long dur = SystemClock.uptimeMillis() - this.downTime;
                if (!this.fingerMoved) {
                    initCursorIfNeeded();
                    if (dur < TAP_TIMEOUT_MS) {
                        // single tap = left click, record for double-tap detection
                        app_mouse_button(true, 1, this.cursorX, this.cursorY);
                        app_mouse_button(false, 1, this.cursorX, this.cursorY);
                        this.lastTapTime = SystemClock.uptimeMillis();
                        this.lastTapX = e.getX(0);
                        this.lastTapY = e.getY(0);
                    } else if (dur >= LONG_PRESS_MS) {
                        // long press = right click
                        app_mouse_button(true, 2, this.cursorX, this.cursorY);
                        app_mouse_button(false, 2, this.cursorX, this.cursorY);
                        this.lastTapTime = 0L;
                    }
                } else {
                    this.lastTapTime = 0L;
                }
                return true;
            }
            case MotionEvent.ACTION_CANCEL:
                if (this.dragMode) {
                    this.dragMode = false;
                    this.dragButtonPressed = false;
                    if (this.cursorX >= 0.0f) {
                        app_mouse_button(false, 1, this.cursorX, this.cursorY);
                    }
                }
                this.lastTapTime = 0L;
                return true;
            default:
                return true;
        }
    }

    // --- MOD: normal+zoom mode ---
    private boolean handleZoomTouch(MotionEvent e) {
        int action = e.getActionMasked();
        int pc = e.getPointerCount();
        if (pc >= 3) {
            // 3-finger scroll
            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
                this.lastCX = centroidX(e);
                this.lastCY = centroidY(e);
            } else if (action == MotionEvent.ACTION_MOVE) {
                float cx = centroidX(e);
                float cy = centroidY(e);
                app_scroll(cx, cy, this.lastCX - cx, this.lastCY - cy, pc);
                this.lastCX = cx;
                this.lastCY = cy;
            }
            return true;
        }
        if (pc == 2) {
            // 2-finger pinch-zoom + pan
            // if a drag was in progress, release the button
            if (this.dragMode) {
                this.dragMode = false;
                this.dragButtonPressed = false;
                if (this.cursorX >= 0.0f) {
                    app_mouse_button(false, 1, this.cursorX, this.cursorY);
                }
            }
            this.sdetector.onTouchEvent(e);
            if (action == MotionEvent.ACTION_POINTER_DOWN || action == MotionEvent.ACTION_POINTER_UP || action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                app_unhandled_touch(action, mapX(e.getX(0)), mapY(e.getY(0)), pc);
            }
            if (action == MotionEvent.ACTION_MOVE && this.zoomScale > 1.0f) {
                float cx = centroidX(e);
                float cy = centroidY(e);
                if (this.panning) {
                    float pdx = cx - this.lastCX;
                    float pdy = cy - this.lastCY;
                    if (Math.abs(pdx) + Math.abs(pdy) >= dpf(PAN_DEADZONE_DP)) {
                        this.zoomOffsetX = clamp(this.zoomOffsetX + pdx, minOX(), 0.0f);
                        this.zoomOffsetY = clamp(this.zoomOffsetY + pdy, minOY(), 0.0f);
                        applyZoom();
                    }
                }
                this.lastCX = cx;
                this.lastCY = cy;
                this.panning = true;
            }
            if (action == MotionEvent.ACTION_POINTER_UP || action == MotionEvent.ACTION_UP) {
                this.panning = false;
            }
            return true;
        }
        // 1 finger = original click-through behavior, zoom-mapped coords
        this.detector.onTouchEvent(e);
        this.sdetector.onTouchEvent(e);
        app_unhandled_touch(action, mapX(e.getX(0)), mapY(e.getY(0)), 1);
        return true;
    }

    private void initCursorIfNeeded() {
        if (this.cursorX < 0.0f) {
            this.cursorX = (getWidth() / 2.0f - this.zoomOffsetX) / this.zoomScale;
            this.cursorY = (getHeight() / 2.0f - this.zoomOffsetY) / this.zoomScale;
        }
    }

    private void syncTouchpadOverlay() {
        if (this.cursorView == null || this.cursorX < 0.0f) {
            return;
        }
        float sx = this.cursorX * this.zoomScale + this.zoomOffsetX;
        float sy = this.cursorY * this.zoomScale + this.zoomOffsetY;
        showCursorAt(sx, sy);
    }

    private void showCursorAt(float x, float y) {
        if (this.cursorView == null) {
            return;
        }
        this.cursorView.setVisibility(View.VISIBLE);
        this.cursorView.setX(x - this.cursorHotX);
        this.cursorView.setY(y - this.cursorHotY);
    }

    @Override // android.view.View
    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (isMouseEvent(motionEvent)) {
            if (motionEvent.getActionMasked() != 2) {
                return true;
            }
            app_mouse_motion(false, mapX(motionEvent.getX(0)), mapY(motionEvent.getY(0)));
            return true;
        }
        if (isStylusEvent(motionEvent)) {
            return handleStylusTouch(motionEvent);
        }
        int action = motionEvent.getActionMasked();
        int pc = motionEvent.getPointerCount();
        // Not in a stream (PC selection screen): pass through to native, no mod features
        if (!this.inStream) {
            this.detector.onTouchEvent(motionEvent);
            this.sdetector.onTouchEvent(motionEvent);
            app_unhandled_touch(motionEvent.getActionMasked(), mapX(motionEvent.getX(0)), mapY(motionEvent.getY(0)), pc);
            return true;
        }
        // Parsec button corner: forward to native (button must work), toggle panel on UP
        if (pc == 1) {
            float x = motionEvent.getX(0);
            float y = motionEvent.getY(0);
            if (action == MotionEvent.ACTION_DOWN) {
                this.touchStartInCorner = inCorner(x, y);
            }
            if (this.touchStartInCorner) {
                // feed detector so the native button gesture works (app_single_tap_up)
                this.detector.onTouchEvent(motionEvent);
                this.sdetector.onTouchEvent(motionEvent);
                // always forward to native for Parsec button
                if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_UP) {
                    app_unhandled_touch(action, mapX(x), mapY(y), 1);
                    if (action == MotionEvent.ACTION_UP) {
                        togglePanel();
                        this.touchStartInCorner = false;
                    }
                    return true;
                }
                // MOVE/CANCEL in corner: consume
                return true;
            }
        } else {
            this.touchStartInCorner = false;
        }
        if (this.touchpadMode) {
            return handleTouchpad(motionEvent);
        }
        if (this.zoomEnabled) {
            return handleZoomTouch(motionEvent);
        }
        this.detector.onTouchEvent(motionEvent);
        this.sdetector.onTouchEvent(motionEvent);
        app_unhandled_touch(motionEvent.getActionMasked(), mapX(motionEvent.getX(0)), mapY(motionEvent.getY(0)), pc);
        return true;
    }

    @Override // android.view.View
    public boolean onHoverEvent(MotionEvent e) {
        if (isStylusEvent(e)) {
            if (this.cursorView != null) {
                this.cursorView.setVisibility(View.GONE);
            }
            int action = e.getActionMasked();
            float x = mapX(e.getX(0));
            float y = mapY(e.getY(0));
            if (action == MotionEvent.ACTION_HOVER_MOVE) {
                app_mouse_motion(false, x, y);
                // check all stylus button bits
                boolean btn = (e.getButtonState() &
                    (MotionEvent.BUTTON_SECONDARY | MotionEvent.BUTTON_STYLUS_PRIMARY | MotionEvent.BUTTON_STYLUS_SECONDARY)) != 0;
                if (btn && !this.stylusBtnDown) {
                    app_mouse_button(true, 2, x, y);
                    this.stylusBtnDown = true;
                } else if (!btn && this.stylusBtnDown) {
                    app_mouse_button(false, 2, x, y);
                    this.stylusBtnDown = false;
                }
            } else if (action == MotionEvent.ACTION_HOVER_EXIT) {
                if (this.stylusBtnDown) {
                    app_mouse_button(false, 2, x, y);
                    this.stylusBtnDown = false;
                }
            }
            return true;
        }
        return super.onHoverEvent(e);
    }

    public void destroy() {
        app_stop();
    }

    @Override // android.view.SurfaceHolder.Callback
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i2, int i3) {
        gfx_resize(i2, i3);
    }

    @Override // android.view.SurfaceHolder.Callback
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        gfx_set_surface(surfaceHolder.getSurface());
    }

    @Override // android.view.SurfaceHolder.Callback
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        gfx_unset_surface();
    }

    PointerIcon getCursor() {
        if (this.defaultCursor) {
            return null;
        }
        return this.hiddenCursor ? this.invisCursor : this.cursor;
    }

    @Override // android.view.View
    public PointerIcon onResolvePointerIcon(MotionEvent motionEvent, int i) {
        return getCursor();
    }

    @Override // android.hardware.input.InputManager.InputDeviceListener
    public void onInputDeviceAdded(int i) {
    }

    @Override // android.hardware.input.InputManager.InputDeviceListener
    public void onInputDeviceChanged(int i) {
    }

    @Override // android.hardware.input.InputManager.InputDeviceListener
    public void onInputDeviceRemoved(int i) {
        app_unplug(i);
    }

    @Override // android.view.View
    public InputConnection onCreateInputConnection(EditorInfo editorInfo) {
        editorInfo.inputType = 0;
        editorInfo.imeOptions = 33554432;
        return new BaseInputConnection(this, false);
    }

    static boolean isKeyboardEvent(InputEvent inputEvent) {
        InputDevice device = inputEvent.getDevice();
        return device != null ? device.getKeyboardType() == 2 : (inputEvent.getSource() & 257) == 257;
    }

    static boolean isMouseEvent(InputEvent inputEvent) {
        return (inputEvent.getSource() & 8194) == 8194 || (inputEvent.getSource() & 1048584) == 1048584;
    }

    static boolean isGamepadEvent(InputEvent inputEvent) {
        InputDevice device = inputEvent.getDevice();
        return device != null ? device.getControllerNumber() != 0 : (inputEvent.getSource() & 1025) == 1025 || (inputEvent.getSource() & 16777232) == 16777232 || (inputEvent.getSource() & 513) == 513;
    }

    static boolean hasAxisTriggers(InputEvent inputEvent) {
        List<InputDevice.MotionRange> motionRanges;
        InputDevice device = inputEvent.getDevice();
        if (device == null || (motionRanges = device.getMotionRanges()) == null) {
            return false;
        }
        for (InputDevice.MotionRange motionRange : motionRanges) {
            if (motionRange.getAxis() == 17 || motionRange.getAxis() == 18) {
                return true;
            }
        }
        return false;
    }

    boolean keyEvent(int i, KeyEvent keyEvent, boolean z) {
        if (isGamepadEvent(keyEvent)) {
            app_button(keyEvent.getDeviceId(), z, i, hasAxisTriggers(keyEvent));
        }
        if (!isKeyboardEvent(keyEvent) || isMouseEvent(keyEvent)) {
            return true;
        }
        int unicodeChar = keyEvent.getUnicodeChar();
        return app_key(z, i, (unicodeChar == 0 || !Character.isDefined(unicodeChar)) ? null : String.format("%c", Integer.valueOf(unicodeChar)), keyEvent.getMetaState(), keyEvent.getDeviceId() <= 0) || isGamepadEvent(keyEvent);
    }

    @Override // android.view.View, android.view.KeyEvent.Callback
    public boolean onKeyDown(int i, KeyEvent keyEvent) {
        return keyEvent(i, keyEvent, true);
    }

    @Override // android.view.View, android.view.KeyEvent.Callback
    public boolean onKeyUp(int i, KeyEvent keyEvent) {
        return keyEvent(i, keyEvent, false);
    }

    @Override // android.view.GestureDetector.OnGestureListener
    public boolean onDown(MotionEvent motionEvent) {
        if (isMouseEvent(motionEvent)) {
            return false;
        }
        this.scroller.forceFinished(true);
        return true;
    }

    @Override // android.view.GestureDetector.OnGestureListener
    public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent2, float f, float f2) {
        if (isMouseEvent(motionEvent) || isMouseEvent(motionEvent2)) {
            return false;
        }
        if (this.touchpadMode) {
            return true;
        }
        if (this.zoomEnabled && motionEvent2.getPointerCount() >= 2) {
            return true;
        }
        this.scroller.forceFinished(true);
        this.scroller.fling(0, 0, Math.round(f), Math.round(f2), Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE);
        app_check_scroller(true);
        return true;
    }

    @Override // android.view.GestureDetector.OnGestureListener
    public void onShowPress(MotionEvent motionEvent) {
    }

    @Override // android.view.GestureDetector.OnGestureListener
    public void onLongPress(MotionEvent motionEvent) {
        if (!isMouseEvent(motionEvent) && app_long_press(motionEvent.getX(0), motionEvent.getY(0))) {
            this.vibrator.vibrate(10L);
        }
    }

    @Override // android.view.GestureDetector.OnGestureListener
    public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent2, float f, float f2) {
        if (isMouseEvent(motionEvent) || isMouseEvent(motionEvent2)) {
            return false;
        }
        this.scroller.forceFinished(true);
        if (!this.touchpadMode) {
            if (this.zoomEnabled && motionEvent2.getPointerCount() >= 2) {
                return true;
            }
            app_scroll(motionEvent2.getX(0), motionEvent2.getY(0), f, f2, motionEvent2.getPointerCount());
        }
        return true;
    }

    @Override // android.view.ScaleGestureDetector.OnScaleGestureListener
    public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector) {
        return true;
    }

    @Override // android.view.ScaleGestureDetector.OnScaleGestureListener
    public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
        if (!this.zoomEnabled) {
            return true;
        }
        float factor = scaleGestureDetector.getScaleFactor();
        if (Float.isNaN(factor) || Float.isInfinite(factor)) {
            return true;
        }
        // ignore jitter
        if (Math.abs(factor - 1.0f) < SCALE_EPSILON) {
            return true;
        }
        float newScale = clamp(this.zoomScale * factor, 1.0f, MAX_ZOOM);
        if (newScale == this.zoomScale) {
            return true;
        }
        if (this.touchpadMode && this.cursorX >= 0.0f) {
            // anchor zoom at the cursor so it stays fixed during pinch
            float ax = this.cursorX * this.zoomScale + this.zoomOffsetX;
            float ay = this.cursorY * this.zoomScale + this.zoomOffsetY;
            this.zoomOffsetX = ax - ((ax - this.zoomOffsetX) * (newScale / this.zoomScale));
            this.zoomOffsetY = ay - ((ay - this.zoomOffsetY) * (newScale / this.zoomScale));
        } else {
            float focusX = scaleGestureDetector.getFocusX();
            float focusY = scaleGestureDetector.getFocusY();
            this.zoomOffsetX = focusX - ((focusX - this.zoomOffsetX) * (newScale / this.zoomScale));
            this.zoomOffsetY = focusY - ((focusY - this.zoomOffsetY) * (newScale / this.zoomScale));
        }
        this.zoomScale = newScale;
        applyZoom();
        return true;
    }

    @Override // android.view.ScaleGestureDetector.OnScaleGestureListener
    public void onScaleEnd(ScaleGestureDetector scaleGestureDetector) {
    }

    @Override // android.view.GestureDetector.OnGestureListener
    public boolean onSingleTapUp(MotionEvent motionEvent) {
        if (isMouseEvent(motionEvent)) {
            return false;
        }
        app_single_tap_up(motionEvent.getX(0), motionEvent.getY(0));
        return true;
    }

    @Override // android.view.GestureDetector.OnDoubleTapListener
    public boolean onDoubleTap(MotionEvent motionEvent) {
        if (isMouseEvent(motionEvent)) {
            return false;
        }
        app_single_tap_up(motionEvent.getX(0), motionEvent.getY(0));
        return true;
    }

    @Override // android.view.GestureDetector.OnDoubleTapListener
    public boolean onDoubleTapEvent(MotionEvent motionEvent) {
        return true;
    }

    @Override // android.view.GestureDetector.OnDoubleTapListener
    public boolean onSingleTapConfirmed(MotionEvent motionEvent) {
        return true;
    }

    @Override // android.view.GestureDetector.OnContextClickListener
    public boolean onContextClick(MotionEvent motionEvent) {
        return true;
    }

    @Override // android.content.ClipboardManager.OnPrimaryClipChangedListener
    public void onPrimaryClipChanged() {
    }

    @Override // android.view.View
    public boolean onGenericMotionEvent(MotionEvent motionEvent) {
        if (isMouseEvent(motionEvent)) {
            int actionMasked = motionEvent.getActionMasked();
            if (actionMasked == 7) {
                app_mouse_motion(false, mapX(motionEvent.getX(0)), mapY(motionEvent.getY(0)));
                return true;
            }
            if (actionMasked == 8) {
                app_generic_scroll(motionEvent.getAxisValue(10), motionEvent.getAxisValue(9));
                return true;
            }
            if (actionMasked == 11) {
                app_mouse_button(true, motionEvent.getActionButton(), mapX(motionEvent.getX(0)), mapY(motionEvent.getY(0)));
                return true;
            }
            if (actionMasked == 12) {
                app_mouse_button(false, motionEvent.getActionButton(), mapX(motionEvent.getX(0)), mapY(motionEvent.getY(0)));
                return true;
            }
        } else if (isStylusEvent(motionEvent)) {
            // handle stylus button press/release while hovering
            int actionMasked = motionEvent.getActionMasked();
            if (actionMasked == 11 && !this.stylusTouching) {
                // ACTION_BUTTON_PRESS
                if (!this.stylusBtnDown) {
                    app_mouse_button(true, 2, mapX(motionEvent.getX(0)), mapY(motionEvent.getY(0)));
                    this.stylusBtnDown = true;
                }
                return true;
            }
            if (actionMasked == 12) {
                // ACTION_BUTTON_RELEASE
                if (this.stylusBtnDown) {
                    app_mouse_button(false, 2, mapX(motionEvent.getX(0)), mapY(motionEvent.getY(0)));
                    this.stylusBtnDown = false;
                }
                return true;
            }
        } else if (isGamepadEvent(motionEvent)) {
            app_axis(motionEvent.getDeviceId(), motionEvent.getAxisValue(15), motionEvent.getAxisValue(16), motionEvent.getAxisValue(0), motionEvent.getAxisValue(1), motionEvent.getAxisValue(11), motionEvent.getAxisValue(14), motionEvent.getAxisValue(17), motionEvent.getAxisValue(18), motionEvent.getAxisValue(23), motionEvent.getAxisValue(22));
        }
        return true;
    }

    @Override // android.view.View
    public boolean onCapturedPointerEvent(MotionEvent motionEvent) {
        app_mouse_motion(true, motionEvent.getX(0), motionEvent.getY(0));
        return onGenericMotionEvent(motionEvent);
    }

    public boolean isFullscreen() {
        return (this.activity.getWindow().getDecorView().getSystemUiVisibility() & fullscreenFlags()) == fullscreenFlags();
    }

    public void enableFullscreen(final boolean z) {
        this.activity.runOnUiThread(new Runnable() {
            @Override // java.lang.Runnable
            public void run() {
                if (z) {
                    Matoya.this.activity.getWindow().getDecorView().setSystemUiVisibility(Matoya.fullscreenFlags());
                } else {
                    Matoya.this.activity.getWindow().getDecorView().setSystemUiVisibility(0);
                }
            }
        });
    }

    public void setClipboard(String str) {
        ((ClipboardManager) this.activity.getSystemService("clipboard")).setPrimaryClip(ClipData.newPlainText("MTY", str));
    }

    public String getClipboard() {
        CharSequence text;
        ClipData primaryClip = ((ClipboardManager) this.activity.getSystemService("clipboard")).getPrimaryClip();
        if (primaryClip == null || (text = primaryClip.getItemAt(0).getText()) == null) {
            return null;
        }
        return text.toString();
    }

    void setCursor() {
        setPointerIcon(getCursor());
    }

    void setCursorBitmap(final Bitmap bitmap, final float f, final float f2) {
        if (bitmap != null) {
            this.inStream = true;
        }
        this.activity.runOnUiThread(new Runnable() {
            @Override // java.lang.Runnable
            public void run() {
                Bitmap bitmap2 = bitmap;
                if (bitmap2 != null) {
                    Matoya.this.cursor = PointerIcon.create(bitmap2, Math.max(0.0f, Math.min((float) (bitmap.getWidth() - 1), f)), Math.max(0.0f, Math.min((float) (bitmap.getHeight() - 1), f2)));
                } else {
                    Matoya.this.cursor = null;
                }
                Matoya.this.setPointerIcon(Matoya.this.cursor);
                // MOD: update overlay cursor
                Matoya.this.cursorBitmap = bitmap;
                Matoya.this.cursorHotX = f;
                Matoya.this.cursorHotY = f2;
                if (Matoya.this.cursorView != null) {
                    Matoya.this.cursorView.update();
                }
            }
        });
    }

    public void setCursorRGBA(int[] iArr, int i, int i2, float f, float f2) {
        setCursorBitmap((iArr == null || iArr.length <= 0 || i <= 0 || i2 <= 0) ? null : Bitmap.createBitmap(iArr, i, i2, Bitmap.Config.ARGB_8888), f, f2);
    }

    public void setCursor(byte[] bArr, float f, float f2) {
        Bitmap bitmap = null;
        if (bArr != null && bArr.length > 0) {
            bitmap = BitmapFactory.decodeByteArray(bArr, 0, bArr.length, null);
        }
        setCursorBitmap(bitmap, f, f2);
    }

    public void showCursor(final boolean z) {
        this.activity.runOnUiThread(new Runnable() {
            @Override // java.lang.Runnable
            public void run() {
                Matoya.this.hiddenCursor = !z;
                Matoya.this.setCursor();
            }
        });
    }

    public void useDefaultCursor(final boolean z) {
        this.activity.runOnUiThread(new Runnable() {
            @Override // java.lang.Runnable
            public void run() {
                Matoya.this.defaultCursor = z;
                Matoya.this.setCursor();
                if (z && Matoya.this.cursorView != null) {
                    Matoya.this.cursorBitmap = null;
                    Matoya.this.cursorView.update();
                }
            }
        });
    }

    public void setRelativeMouse(final boolean z) {
        this.activity.runOnUiThread(new Runnable() {
            @Override // java.lang.Runnable
            public void run() {
                if (z) {
                    Matoya.this.requestPointerCapture();
                } else {
                    Matoya.this.releasePointerCapture();
                }
            }
        });
    }

    public boolean getRelativeMouse() {
        return hasPointerCapture();
    }

    public void openURI(String str) {
        this.activity.startActivity(new Intent("android.intent.action.VIEW", Uri.parse(str)));
    }

    public float getDisplayDensity() {
        return this.displayDensity;
    }

    public String getKey(int i) {
        char displayLabel = this.kbmap.getDisplayLabel(i);
        if (displayLabel != 0) {
            return String.format("%c", Character.valueOf(displayLabel));
        }
        return KeyEvent.keyCodeToString(i).replaceAll("KEYCODE_", "").replaceAll("MOVE_", "");
    }

    public void checkScroller() {
        this.scroller.computeScrollOffset();
        if (!this.scroller.isFinished()) {
            int currY = this.scroller.getCurrY();
            int i = this.scrollY - currY;
            if (i != 0) {
                app_scroll(-1.0f, -1.0f, 0.0f, i, 1);
            }
            this.scrollY = currY;
            return;
        }
        app_check_scroller(false);
        this.scrollY = 0;
    }

    public int keyboardHeight() {
        InputMethodManager inputMethodManager = (InputMethodManager) this.activity.getSystemService("input_method");
        try {
            return ((Integer) inputMethodManager.getClass().getMethod("getInputMethodWindowVisibleHeight", new Class[0]).invoke(inputMethodManager, new Object[0])).intValue();
        } catch (Exception unused) {
            return -1;
        }
    }

    public boolean keyboardIsShowing() {
        int keyboardHeight = keyboardHeight();
        if (keyboardHeight == -1) {
            return this.kbShowing;
        }
        return keyboardHeight > 0;
    }

    public void showKeyboard(boolean z) {
        InputMethodManager inputMethodManager = (InputMethodManager) this.activity.getSystemService("input_method");
        if (z) {
            inputMethodManager.showSoftInput(this, 0, null);
        } else {
            inputMethodManager.hideSoftInputFromWindow(getWindowToken(), 0, null);
        }
        this.kbShowing = z;
    }

    public int getOrientation() {
        int i = this.activity.getResources().getConfiguration().orientation;
        if (i != 1) {
            return i != 2 ? 0 : 1;
        }
        return 2;
    }

    public void setOrientation(final int i) {
        this.activity.runOnUiThread(new Runnable() {
            @Override // java.lang.Runnable
            public void run() {
                int i2 = i;
                if (i2 == 1) {
                    Matoya.this.activity.setRequestedOrientation(6);
                } else if (i2 == 2) {
                    Matoya.this.activity.setRequestedOrientation(7);
                } else {
                    Matoya.this.activity.setRequestedOrientation(-1);
                }
            }
        });
    }

    public void stayAwake(final boolean z) {
        this.activity.runOnUiThread(new Runnable() {
            @Override // java.lang.Runnable
            public void run() {
                if (z) {
                    Matoya.this.activity.getWindow().addFlags(128);
                } else {
                    Matoya.this.activity.getWindow().clearFlags(128);
                }
                Matoya.this.inSession = z;
                if (!z) {
                    Matoya.this.hidePanel();
                    if (Matoya.this.cursorView != null) {
                        Matoya.this.cursorView.setVisibility(View.GONE);
                    }
                }
            }
        });
    }

    public void finish() {
        this.activity.finishAndRemoveTask();
    }

    public String getExternalFilesDir() {
        return this.activity.getExternalFilesDir(null).getAbsolutePath();
    }

    public String getInternalFilesDir() {
        return this.activity.getFilesDir().getAbsolutePath();
    }

    public int getHardwareIds(int i) {
        int i2;
        int i3;
        InputDevice device = InputDevice.getDevice(i);
        if (device != null) {
            i2 = device.getProductId();
            i3 = device.getVendorId();
        } else {
            i2 = 0;
            i3 = 0;
        }
        return (i3 << 16) | (i2 & 65535);
    }

    // --- MOD: cursor overlay view ---
    private class CursorView extends View {
        CursorView(android.content.Context c) {
            super(c);
            setClickable(false);
            setFocusable(false);
            setWillNotDraw(false);
        }

        void update() {
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            Bitmap bmp = Matoya.this.cursorBitmap;
            if (bmp != null) {
                canvas.drawBitmap(bmp, -Matoya.this.cursorHotX, -Matoya.this.cursorHotY, null);
            } else {
                drawDefaultArrow(canvas);
            }
        }

        private void drawDefaultArrow(Canvas canvas) {
            Paint p = new Paint();
            p.setAntiAlias(true);
            p.setStyle(Paint.Style.FILL);
            p.setColor(Color.BLACK);
            Path path = new Path();
            path.moveTo(0.0f, 0.0f);
            path.lineTo(12.0f, 28.0f);
            path.lineTo(16.0f, 23.0f);
            path.lineTo(21.0f, 33.0f);
            path.lineTo(27.0f, 30.0f);
            path.lineTo(22.0f, 20.0f);
            path.lineTo(30.0f, 19.0f);
            path.close();
            canvas.drawPath(path, p);
            p.setColor(Color.WHITE);
            Path path2 = new Path();
            path2.moveTo(3.0f, 3.0f);
            path2.lineTo(11.0f, 23.0f);
            path2.lineTo(14.0f, 19.0f);
            path2.lineTo(19.0f, 28.0f);
            path2.lineTo(23.0f, 26.0f);
            path2.lineTo(18.0f, 17.0f);
            path2.lineTo(26.0f, 16.0f);
            path2.close();
            canvas.drawPath(path2, p);
        }
    }
}