package com.snipersim.input;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Vector2;

/**
 * Accumulates relative pointer movement and mouse button state from platform
 * input backends. Desktop reads libGDX's captured-cursor deltas directly;
 * Android feeds this class from pointer-capture events, where the system
 * cursor is hidden and motion is reported as relative deltas.
 */
public final class LookInput {
    private static float deltaX;
    private static float deltaY;
    private static float scroll;
    private static int buttons;
    private static int justPressed;
    private static boolean pointerCaptured;

    private LookInput() {
    }

    public static synchronized void addDelta(float x, float y) {
        deltaX += x;
        deltaY += y;
    }

    public static synchronized void consume(Vector2 out) {
        out.set(deltaX, deltaY);
        deltaX = 0;
        deltaY = 0;
    }

    public static synchronized void addScroll(float amount) {
        scroll += amount;
    }

    public static synchronized float consumeScroll() {
        float value = scroll;
        scroll = 0;
        return value;
    }

    /** Replaces the current button state and records newly pressed buttons. */
    public static synchronized void setButtons(int buttonState) {
        int changed = buttonState ^ buttons;
        justPressed |= changed & buttonState;
        buttons = buttonState;
    }

    public static synchronized boolean isButtonPressed(int button) {
        return (buttons & (1 << button)) != 0;
    }

    public static synchronized boolean consumeJustPressed(int button) {
        int mask = 1 << button;
        boolean pressed = (justPressed & mask) != 0;
        justPressed &= ~mask;
        return pressed;
    }

    public static synchronized void clearJustPressed() {
        justPressed = 0;
    }

    public static synchronized void setPointerCaptured(boolean captured) {
        pointerCaptured = captured;
    }

    public static synchronized boolean isPointerCaptured() {
        return pointerCaptured;
    }

    public static synchronized void clear() {
        deltaX = 0;
        deltaY = 0;
        scroll = 0;
        buttons = 0;
        justPressed = 0;
    }
}
