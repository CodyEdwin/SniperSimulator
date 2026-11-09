package com.snipersim.input;

import com.badlogic.gdx.Input;
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
        LookInput.clearJustPressed();
        LookInput.setPointerCaptured(false);
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
    void tracksIndependentButtonStatesAndEdges() {
        LookInput.setButtons((1 << Input.Buttons.LEFT) | (1 << Input.Buttons.RIGHT));
        assertTrue(LookInput.isButtonPressed(Input.Buttons.LEFT));
        assertTrue(LookInput.isButtonPressed(Input.Buttons.RIGHT));
        assertTrue(LookInput.consumeJustPressed(Input.Buttons.LEFT));
        assertTrue(LookInput.consumeJustPressed(Input.Buttons.RIGHT));
        assertFalse(LookInput.consumeJustPressed(Input.Buttons.LEFT));

        LookInput.setButtons(1 << Input.Buttons.LEFT);
        assertTrue(LookInput.isButtonPressed(Input.Buttons.LEFT));
        assertFalse(LookInput.isButtonPressed(Input.Buttons.RIGHT));
        assertFalse(LookInput.consumeJustPressed(Input.Buttons.LEFT));
        assertFalse(LookInput.consumeJustPressed(Input.Buttons.RIGHT));
    }

    @Test
    void accumulatesScroll() {
        LookInput.addScroll(1.5f);
        LookInput.addScroll(-0.5f);
        assertEquals(1f, LookInput.consumeScroll(), 0.0001f);
        assertEquals(0f, LookInput.consumeScroll(), 0.0001f);
    }
}
