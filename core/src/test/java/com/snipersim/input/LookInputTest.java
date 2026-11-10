package com.snipersim.input;

import com.badlogic.gdx.math.Vector2;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LookInputTest {
    @BeforeEach
    @AfterEach
    void reset() {
        LookInput.clear();
    }

    @Test
    void accumulatesAndConsumesRelativeMotion() {
        LookInput.addDelta(12.5f, -4f);
        LookInput.addDelta(-2.5f, 1f);
        Vector2 out = new Vector2();
        LookInput.consume(out);
        assertEquals(10f, out.x, 0.0001f);
        assertEquals(-3f, out.y, 0.0001f);

        LookInput.consume(out);
        assertEquals(0f, out.x, 0.0001f);
        assertEquals(0f, out.y, 0.0001f);
    }

    @Test
    void accumulatesScroll() {
        LookInput.addScroll(1.5f);
        LookInput.addScroll(-0.5f);
        assertEquals(1f, LookInput.consumeScroll(), 0.0001f);
        assertEquals(0f, LookInput.consumeScroll(), 0.0001f);
    }

    @Test
    void scopePressSameFrameDoesNotFire() {
        LookInput.setMouseScope(true);
        LookInput.setMouseFire(true);
        assertTrue(LookInput.isScopeDown());
        assertFalse(LookInput.consumeFireEdge());
    }

    @Test
    void fireThenScopeSameFrameDoesNotFire() {
        LookInput.setMouseFire(true);
        LookInput.setMouseScope(true);
        assertFalse(LookInput.consumeFireEdge());
    }

    @Test
    void fireWhileAlreadyScopedStillFires() {
        LookInput.setMouseScope(true);
        LookInput.endFrame();
        LookInput.setMouseFire(true);
        assertTrue(LookInput.consumeFireEdge());
    }

    @Test
    void phantomPrimaryFromScopeInDoesNotBlockLaterShot() {
        LookInput.setMouseScope(true);
        LookInput.setMouseFire(true);
        LookInput.endFrame();
        LookInput.setMouseFire(true);
        assertFalse(LookInput.consumeFireEdge());
        LookInput.setMouseFire(false);
        LookInput.setMouseFire(true);
        assertTrue(LookInput.isScopeDown());
        assertTrue(LookInput.consumeFireEdge());
    }

    @Test
    void touchScopeDoesNotFire() {
        LookInput.setTouchScope(true);
        LookInput.pulseFire();
        assertTrue(LookInput.isScopeDown());
        assertFalse(LookInput.consumeFireEdge());
    }

    @Test
    void touchFireAfterScopeIsHeldDoesFire() {
        LookInput.setTouchScope(true);
        LookInput.endFrame();
        LookInput.pulseFire();
        assertTrue(LookInput.consumeFireEdge());
    }

    @Test
    void unconfirmedFirePromotesNextFrame() {
        LookInput.armUnconfirmedFire();
        assertFalse(LookInput.consumeFireEdge());
        LookInput.endFrame();
        assertTrue(LookInput.consumeFireEdge());
    }

    @Test
    void scopeCancelsUnconfirmedFire() {
        LookInput.armUnconfirmedFire();
        LookInput.setMouseScope(true);
        LookInput.endFrame();
        assertFalse(LookInput.consumeFireEdge());
    }

    @Test
    void scopeAfterPromotionCancelsBeforeConsume() {
        LookInput.armUnconfirmedFire();
        LookInput.endFrame();
        LookInput.setMouseScope(true);
        assertFalse(LookInput.consumeFireEdge());
    }
}
