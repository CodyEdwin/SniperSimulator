package com.snipersim.android;

import android.os.Build;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.badlogic.gdx.backends.android.AndroidGraphics;
import com.snipersim.SniperGame;
import com.snipersim.input.LookInput;

/** Android entry point. All simulation and rendering lives in the shared core. */
public final class AndroidLauncher extends AndroidApplication {
    private View gameView;

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
        LookInput.setPointerCaptured(true);

        int action = event.getActionMasked();
        int buttonState = event.getButtonState();
        int changedButton = event.getActionButton();
        if (action == MotionEvent.ACTION_BUTTON_PRESS) {
            buttonState |= changedButton;
        } else if (action == MotionEvent.ACTION_BUTTON_RELEASE) {
            buttonState &= ~changedButton;
        } else if (action == MotionEvent.ACTION_DOWN) {
            buttonState |= MotionEvent.BUTTON_PRIMARY;
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            buttonState &= ~MotionEvent.BUTTON_PRIMARY;
        }
        LookInput.setButtons(toLookButtons(buttonState));

        if (action == MotionEvent.ACTION_MOVE || action == MotionEvent.ACTION_HOVER_MOVE) {
            // In pointer-capture mode getX()/getY() are relative deltas.
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

    private static int toLookButtons(int androidButtons) {
        int buttons = 0;
        if ((androidButtons & MotionEvent.BUTTON_PRIMARY) != 0) {
            buttons |= 1 << Input.Buttons.LEFT;
        }
        if ((androidButtons & MotionEvent.BUTTON_SECONDARY) != 0) {
            buttons |= 1 << Input.Buttons.RIGHT;
        }
        if ((androidButtons & MotionEvent.BUTTON_TERTIARY) != 0) {
            buttons |= 1 << Input.Buttons.MIDDLE;
        }
        if ((androidButtons & MotionEvent.BUTTON_BACK) != 0) {
            buttons |= 1 << Input.Buttons.BACK;
        }
        if ((androidButtons & MotionEvent.BUTTON_FORWARD) != 0) {
            buttons |= 1 << Input.Buttons.FORWARD;
        }
        return buttons;
    }
}
