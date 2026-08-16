# Changelog - Parsec Android Mod

All notable changes to this project will be documented in this file.

## [3.6.0] - 2026-08-15

### Changed
- Removed all keyboardIsShowing() guards from zoom handlers (onScale, pan) - zoom now works in any mode, including portrait
- Removed showKeyboard zoom reset hook - keyboard no longer resets zoom on open
- Removed setOrientation portrait enforcement hook - system rotation button now works, user can exit portrait via native Android button
- Exiting portrait mode now forces LANDSCAPE (6) instead of UNSPECIFIED (-1) - immediate visual feedback on second tap
- All mod features (touchpad, zoom, portrait, corner trigger) now activate only when a host cursor bitmap is received (i.e., during an active stream). Outside streaming, the mod is fully passthrough - no interference with the PC selection screen.

### Fixed
- Zoom blocked in portrait mode due to stuck kbShowing flag
- Portrait mode exit not working (second tap appeared to do nothing)
- System rotation button not appearing (setOrientation hook was overriding system rotation)
- Cursor overlay visible on PC selection screen (now hidden until host cursor received)

## [v3.5] - 2026-08-14

### Added
- Drag mode now delays button press until first movement (2dp threshold) - prevents false double-click detection
- dragButtonPressed flag: second tap enters drag mode, button pressed on first move, UP without movement = double-click

### Changed
- ACTION_CANCEL and pc==2 drag cleanup now resets dragButtonPressed

### Fixed
- Double-tap to drag a file was opening the file instead of dragging (Windows interpreted two rapid clicks as double-click)

## [v3.4] - 2026-08-14

### Changed
- Removed inSession gate from corner trigger - panel always opens, no longer relies on unreliable stayAwake
- Portrait corner zone increased from 60dp to 90dp (easier to hit)
- Added wasMultiTouch flag for re-baseline after multi-finger gestures

### Added
- setOrientation hook: forces portrait when portraitMode is on (prevents native from overriding)
- wasMultiTouch re-baseline: resets lastX/lastY on first single-finger event after multi-touch

### Fixed
- Panel not opening due to stayAwake not being called by native (inSession gate)
- Cursor teleport to remaining finger when lifting one finger after pinch
- Portrait mode being overridden by native setOrientation calls

## [v3.3] - 2026-08-14

### Added
- Zoom anchored at cursor position in touchpad mode (instead of finger focus) - cursor stays fixed during pinch
- Pan compensation: cursor stream position adjusted during 2-finger pan to keep overlay fixed
- Keyboard guards: zoom/pan blocked while keyboard is open (keyboardIsShowing)
- showKeyboard zoom reset: zoom resets to 1:1 when keyboard opens

### Changed
- onScale uses cursor screen position as anchor when in touchpad mode

### Fixed
- Cursor moving during pinch zoom (now anchored at cursor)
- Function keys scaling with zoom (keyboard guarded)
- Overlay sliding during pan (now compensated)

## [v3.2] - 2026-08-14

### Added
- Portrait orientation toggle button ("Портрет") in the panel
- togglePortraitMode() method (setRequestedOrientation PORTRAIT / UNSPECIFIED)
- orientBtn field and UI in buildPanel/refreshButtons
- initCursorIfNeeded() helper - stream-coord center init
- syncTouchpadOverlay() helper - screen pos = stream*scale+offset
- GestureDetector feeding in corner handler (app_single_tap_up for native button)

### Changed
- Corner handler now feeds GestureDetector + app_unhandled_touch (native gets full button sequence)
- stayAwake(false) hides panel + cursor overlay
- CursorView size 64dp -> 96dp (prevents clipping)

### Fixed
- Remote cursor position during zoom (now correct: cursorX/Y in stream coords)
- Parsec button not responding (detector events now forwarded)
- Cursor clipping on state changes (larger CursorView)

## [v3.1] - 2026-08-14

### Changed
- applyZoom re-syncs remote cursor position in touchpad mode (keeps cursor under overlay)

### Fixed
- Remote cursor diverging from overlay during zoom/pan

## [v3.0] - 2026-08-14

### Added
- Corner trigger zone: orientation-dependent (landscape=top-left 220dp, portrait=top-right 220dp)
- inSession flag + stayAwake hook for session detection
- touchStartInCorner flag - only touches starting in corner get corner handling
- ACTION_BUTTON_PRESS/RELEASE (11/12) handling for stylus in onGenericMotionEvent
- stylusTouching flag (prevents right-click while touching)
- Drag mode with double-tap detection + long-press right-click
- overlay update in setCursorBitmap/setCursorRGBA

### Changed
- Corner taps no longer consumed - native menu also opens
- Touchpad cursor: relative mode (cursorX/Y tracked, deltas applied)
- Gesture width/height -> mapX/mapY mapping for zoom
- Pan deadzone (2dp), scale epsilon (0.01) for jitter reduction
- Stylus: always left button on touch (right-click via hover+button)
- Touchpad: long-press = right-click (button 2, was 3)

### Fixed
- Right-click button constant (was 3, now 2 = MotionEvent.BUTTON_SECONDARY)
- All stylus button bits checked (SECONDARY | STYLUS_PRIMARY | STYLUS_SECONDARY)
- Zoom jitter from finger centroid noise
- Cursor clamping at screen edge when zoomed

## [v2.0] - 2026-08-14

### Added
- Stylus: hover = mouse motion, touch = click, button = right-click
- Touchpad mode: 1 finger = cursor, 2 fingers = zoom, 3 fingers = scroll
- Zoom mode: pinch = zoom, 2 fingers = pan, 3 fingers = scroll
- Cursor overlay (CursorView) with host cursor bitmap support
- Panel UI (LinearLayout) with Тачпад/Зум toggle buttons
- Panel trigger: tap top-right corner (finger only)
- Orientation-aware panel position (landscape=top-left, portrait=top-right)
- updatePanelPosition() for rotation handling

### Changed
- MainActivity wraps Matoya in FrameLayout, calls attachOverlays
- onTouchEvent routes to mode-specific handlers
- onHoverEvent intercepts stylus hover for mouse motion
- setCursorBitmap/setCursorRGBA update overlay cursor
- enableFullscreen hook for panel sync (unused - native doesn't call it)

### Fixed
- Pipeline: javac -> R8 D8 -> baksmali -> apktool -> sign (workaround for R8 8.2.2 bug)
- Split APK merged into single APK (manifest patched)

## [v1.0] - 2026-08-14

### Added
- Initial Parsec APK decompilation (apktool, jadx)
- Matoya.java analysis - JNI methods indentified
- Stylus hover -> mouse motion (initial implementation)
- Corner trigger (top-right, consumed tap)
- Cursor overlay (initial, 64dp)

### Known Issues
- Right-click button constant wrong (3 instead of 2) - ПКМ not working
- Corner trigger consumed tap - Parsec button blocked
- Cursor moves during zoom/pan
- Action button press/release not handled
- Zoom not gated for keyboard
## [3.6.3] - 2026-08-15

### Added
- Stream detection via host cursor bitmap: `inStream` flag set to true when setCursorBitmap receives a non-null bitmap. All mod features activate only when inStream is true.

### Changed
- Outside a stream (PC selection screen): touchpad, zoom, corner trigger, and all mod features are fully disabled. Touches pass through to the native library as if the mod doesn't exist.
- No interference with the PC selection screen UI.

### Fixed
- Cursor overlay no longer visible on PC selection screen
- Corner trigger no longer blocks PC selection screen buttons
- Touchpad no longer intercepts touches on PC selection screen

## [3.6.4] - 2026-08-15

### Changed
- Version numbering: subsequent minor fixes are now 3.6.x (3.7 → 3.6.1, 3.8 → 3.6.2, 3.9 → 3.6.3, this → 3.6.4)
- Cleaned up: removed inStream and panelVisible gating from all mod features
- All features (touchpad, zoom, portrait, stylus, drag) remain fully functional
- stayAwake hook remains as the only cursor-hiding mechanism (hides cursor overlay when the session ends)
- No feature gates on PC selection screen - mod works wherever the user enables it

### Fixed
- Portrait mode button and all other features present in this build
- No inStream or panelVisible interference
