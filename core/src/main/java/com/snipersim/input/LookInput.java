package com.snipersim.input;

import com.badlogic.gdx.math.Vector2;

/**
 * Pointer motion plus separate fire and scope edges.
 *
 * <p>Scope and fire are different actions. A scope press always wins over a
 * fire edge in the same frame, including the case where a backend reports the
 * secondary button as a primary press. An unconfirmed primary (a pointer-down
 * with no button bits) is held for one frame so a following scope event can
 * cancel it before a shot is committed.
 */
public final class LookInput {
    private static float deltaX;
    private static float deltaY;
    private static float scroll;

    private static boolean mouseScope;
    private static boolean touchScope;
    private static boolean mouseFire;
    private static boolean scopeThisFrame;
    private static boolean fireEdge;
    private static boolean fireFromUnconfirmed;
    private static boolean unconfirmedFire;
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

    /**
     * Mouse / captured-pointer scope button. Call scope before fire when both
     * edges are sampled in the same frame.
     */
    public static synchronized void setMouseScope(boolean down) {
        if (down && !mouseScope) {
            beginScope();
        }
        mouseScope = down;
    }

    /** On-screen aim control. Holding it must not produce a shot. */
    public static synchronized void setTouchScope(boolean down) {
        if (down && !touchScope) {
            beginScope();
        }
        touchScope = down;
    }

    /**
     * Confirmed primary button. Ignored on the same frame a scope press began,
     * so aim-down-sights cannot discharge the rifle.
     */
    public static synchronized void setMouseFire(boolean down) {
        if (down && !mouseFire && !scopeThisFrame) {
            fireEdge = true;
            fireFromUnconfirmed = false;
        }
        mouseFire = down;
    }

    /** Explicit fire control (on-screen button). Same-frame scope still wins. */
    public static synchronized void pulseFire() {
        if (!scopeThisFrame) {
            fireEdge = true;
            fireFromUnconfirmed = false;
        }
    }

    /**
     * Pointer went down without a button bit. Promoted next frame unless a
     * scope press arrives first.
     */
    public static synchronized void armUnconfirmedFire() {
        if (scopeThisFrame || mouseScope || touchScope) {
            return;
        }
        unconfirmedFire = true;
    }

    public static synchronized void cancelUnconfirmedFire() {
        unconfirmedFire = false;
        if (fireFromUnconfirmed) {
            fireEdge = false;
            fireFromUnconfirmed = false;
            mouseFire = false;
        }
    }

    public static synchronized boolean isScopeDown() {
        return mouseScope || touchScope;
    }

    public static synchronized boolean isFireDown() {
        return mouseFire;
    }

    public static synchronized boolean consumeFireEdge() {
        boolean fire = fireEdge && !scopeThisFrame;
        fireEdge = false;
        fireFromUnconfirmed = false;
        return fire;
    }

    /** Closes the frame: promotes or drops an unconfirmed click, then clears the scope edge. */
    public static synchronized void endFrame() {
        if (unconfirmedFire && !mouseScope && !touchScope && !scopeThisFrame) {
            fireEdge = true;
            fireFromUnconfirmed = true;
        }
        unconfirmedFire = false;
        scopeThisFrame = false;
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
        mouseScope = false;
        touchScope = false;
        mouseFire = false;
        scopeThisFrame = false;
        fireEdge = false;
        fireFromUnconfirmed = false;
        unconfirmedFire = false;
        pointerCaptured = false;
    }

    private static void beginScope() {
        scopeThisFrame = true;
        fireEdge = false;
        fireFromUnconfirmed = false;
        unconfirmedFire = false;
    }
}
