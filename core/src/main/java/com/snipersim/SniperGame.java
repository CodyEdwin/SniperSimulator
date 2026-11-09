package com.snipersim;

import com.badlogic.gdx.Game;
import com.snipersim.screens.GameScreen;

/** libGDX application shell. */
public final class SniperGame extends Game {
    private final boolean smokeTest;

    public SniperGame(boolean smokeTest) {
        this.smokeTest = smokeTest;
    }

    @Override
    public void create() {
        setScreen(new GameScreen(this, smokeTest));
    }

    @Override
    public void dispose() {
        if (getScreen() != null) {
            getScreen().dispose();
        }
        super.dispose();
    }
}
