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

        // Edges only. A held right button used to clear primary for the whole
        // aim, so the rifle could not fire while scoped.
        if (action == MotionEvent.ACTION_BUTTON_PRESS) {
            if (changed == MotionEvent.BUTTON_SECONDARY) {
                mouseScope = true;
                LookInput.cancelUnconfirmedFire();
            } else if (changed == MotionEvent.BUTTON_PRIMARY) {
                mouseFire = true;
                LookInput.cancelUnconfirmedFire();
            }
        } else if (action == MotionEvent.ACTION_BUTTON_RELEASE) {
            if (changed == MotionEvent.BUTTON_SECONDARY) {
                mouseScope = false;
            } else if (changed == MotionEvent.BUTTON_PRIMARY) {
                mouseFire = false;
            }
        } else if (action == MotionEvent.ACTION_DOWN) {
            if ((state & MotionEvent.BUTTON_SECONDARY) != 0) {
                mouseScope = true;
                LookInput.cancelUnconfirmedFire();
            } else if ((state & MotionEvent.BUTTON_PRIMARY) != 0) {
                mouseFire = true;
                LookInput.cancelUnconfirmedFire();
            } else {
                LookInput.armUnconfirmedFire();
            }
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            mouseFire = false;
            if ((state & MotionEvent.BUTTON_SECONDARY) == 0) {
                mouseScope = false;
            }
        }

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
