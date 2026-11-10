package com.snipersim.android;

import android.os.Build;
import android.os.Bundle;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.badlogic.gdx.backends.android.AndroidGraphics;
import com.snipersim.SniperGame;
import com.snipersim.input.LookInput;

/** Android entry point. All simulation and rendering lives in the shared core. */
public final class AndroidLauncher extends AndroidApplication {
    private View gameView;
    private boolean mouseScope;
    private boolean mouseFire;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
        config.useAccelerometer = false;
        config.useCompass = false;
        config.useGyroscope = false;
        config.useImmersiveMode = true;
        config.numSamples = 0;

        initialize(new SniperGame(false), config);
        installInput();
    }

    private void installInput() {
        gameView = ((AndroidGraphics) Gdx.graphics).getView();
        gameView.setFocusable(true);
        gameView.setFocusableInTouchMode(true);
        gameView.requestFocus();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Relative pointer capture hides the system cursor and keeps
            // delivering motion past the screen edges, like a desktop FPS.
            gameView.setOnCapturedPointerListener(this::onCapturedPointer);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            if (gameView != null) {
                gameView.requestFocus();
                gameView.post(this::requestPointerCapture);
            }
        } else {
            LookInput.setPointerCaptured(false);
            mouseScope = false;
            mouseFire = false;
            LookInput.setMouseScope(false);
            LookInput.setMouseFire(false);
            LookInput.clear();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (gameView != null) {
            gameView.post(this::requestPointerCapture);
        }
    }

    private void requestPointerCapture() {
        if (gameView == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        if (!gameView.isAttachedToWindow() || !gameView.hasWindowFocus()) {
            return;
        }
        gameView.requestPointerCapture();
    }

    private boolean onCapturedPointer(View view, MotionEvent event) {
        // Fingers keep absolute coordinates and hit the on-screen ADS / FIRE
        // buttons through libGDX. Captured mouse buttons are handled here.
        if (isFinger(event)) {
            return false;
        }
        LookInput.setPointerCaptured(true);

        int action = event.getActionMasked();
        int changed = event.getActionButton();
        int state = event.getButtonState();
        if (action == MotionEvent.ACTION_BUTTON_PRESS) {
            state |= changed;
        } else if (action == MotionEvent.ACTION_BUTTON_RELEASE) {
            state &= ~changed;
        }

        boolean secondary = (state & MotionEvent.BUTTON_SECONDARY) != 0;
        boolean primary = (state & MotionEvent.BUTTON_PRIMARY) != 0;
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            primary = false;
            if ((event.getButtonState() & MotionEvent.BUTTON_SECONDARY) == 0) {
                secondary = false;
            }
        }

        // A right-click often arrives as ACTION_DOWN with an empty button
        // mask, and libGDX would treat that mask as the left button. Hold the
        // click for one frame. The secondary press cancels it; a real primary
        // press below commits immediately.
        if (action == MotionEvent.ACTION_DOWN && state == 0 && !secondary) {
            LookInput.armUnconfirmedFire();
        }
        if (secondary) {
            primary = false;
            LookInput.cancelUnconfirmedFire();
        } else if (primary && (action == MotionEvent.ACTION_BUTTON_PRESS
                || action == MotionEvent.ACTION_DOWN)) {
            LookInput.cancelUnconfirmedFire();
        }

        mouseScope = secondary;
        mouseFire = primary && !secondary;
        // Scope first so a same-frame primary edge cannot discharge the rifle.
        LookInput.setMouseScope(mouseScope);
        LookInput.setMouseFire(mouseFire);

        if (action == MotionEvent.ACTION_MOVE || action == MotionEvent.ACTION_HOVER_MOVE) {
            LookInput.addDelta(event.getX(), event.getY());
        } else if (action == MotionEvent.ACTION_SCROLL) {
            float amount = event.getAxisValue(MotionEvent.AXIS_VSCROLL);
            if (amount == 0) {
                amount = event.getAxisValue(MotionEvent.AXIS_HSCROLL);
            }
            LookInput.addScroll(amount);
        }
        return true;
    }

    private static boolean isFinger(MotionEvent event) {
        int source = event.getSource();
        if ((source & InputDevice.SOURCE_MOUSE) != 0
                || (source & InputDevice.SOURCE_STYLUS) != 0) {
            return false;
        }
        int index = event.getActionIndex();
        if (index < 0 || index >= event.getPointerCount()) {
            index = 0;
        }
        return event.getToolType(index) == MotionEvent.TOOL_TYPE_FINGER
                || (source & InputDevice.SOURCE_TOUCHSCREEN) != 0;
    }
}
