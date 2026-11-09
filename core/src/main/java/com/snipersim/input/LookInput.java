package com.snipersim.input;

import com.badlogic.gdx.math.Vector2;

/**
 * Accumulates relative pointer movement from platform input backends.
 * Desktop reads libGDX's captured-cursor deltas directly; Android feeds this
 * class from mouse hover/drag events, where getDeltaX() is always zero.
 */
public final class LookInput {
    private static float deltaX;
    private static float deltaY;

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

    public static synchronized void clear() {
        deltaX = 0;
        deltaY = 0;
    }
}
