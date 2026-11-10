package com.snipersim;

import com.badlogic.gdx.Game;
import com.snipersim.screens.CatalogScreen;
import com.snipersim.screens.GameScreen;
import com.snipersim.weapons.Loadout;

/** libGDX application shell. */
public final class SniperGame extends Game {
    private final boolean smokeTest;

    public SniperGame(boolean smokeTest) {
        this.smokeTest = smokeTest;
    }

    @Override
    public void create() {
        if (smokeTest) {
            setScreen(new GameScreen(this, true, Loadout.issued()));
        } else {
            setScreen(new CatalogScreen(this, null, Loadout.issued()));
        }
    }

    @Override
    public void dispose() {
        if (getScreen() != null) {
            getScreen().dispose();
        }
        super.dispose();
    }
}
