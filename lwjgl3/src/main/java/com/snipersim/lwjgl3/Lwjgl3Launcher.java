package com.snipersim.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.snipersim.SniperGame;

public final class Lwjgl3Launcher {
    private Lwjgl3Launcher() {
    }

    public static void main(String[] args) {
        boolean smokeTest = false;
        int width = 1440;
        int height = 810;
        for (String arg : args) {
            if ("--smoke-test".equals(arg)) {
                smokeTest = true;
            } else if (arg.startsWith("--width=")) {
                width = Integer.parseInt(arg.substring("--width=".length()));
            } else if (arg.startsWith("--height=")) {
                height = Integer.parseInt(arg.substring("--height=".length()));
            }
        }

        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("DAGGER RIDGE — 3D Long-Range Sniper Simulation");
        config.setWindowedMode(width, height);
        config.setResizable(true);
        config.useVsync(!smokeTest);
        config.setForegroundFPS(smokeTest ? 60 : 144);
        config.setBackBufferConfig(8, 8, 8, 8, 16, 0, smokeTest ? 0 : 2);
        config.disableAudio(smokeTest);
        config.setWindowSizeLimits(960, 540, -1, -1);

        new Lwjgl3Application(new SniperGame(smokeTest), config);
    }
}
