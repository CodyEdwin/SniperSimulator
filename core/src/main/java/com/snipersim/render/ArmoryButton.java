package com.snipersim.render;

/** Click target for opening the armory. Coordinates match libGDX touch (origin at the top-left). */
public final class ArmoryButton {
    private ArmoryButton() {
    }

    public static float boxW(float height) {
        return 220f * height / 900f;
    }

    public static float boxH(float height) {
        return 58f * height / 900f;
    }

    public static float cornerX(float width, float height) {
        return width - 18f * height / 900f - boxW(height);
    }

    public static float cornerTop(float height) {
        return height * 0.42f;
    }

    public static boolean hit(float x, float y, float width, float height, boolean big) {
        if (inside(x, y, cornerX(width, height), cornerTop(height), boxW(height), boxH(height))) {
            return true;
        }
        if (!big) {
            return false;
        }
        float w = 360f * height / 900f;
        float h = 78f * height / 900f;
        return inside(x, y, width * 0.5f - w * 0.5f, height * 0.48f, w, h);
    }

    private static boolean inside(float x, float y, float rx, float ry, float rw, float rh) {
        return x >= rx && x <= rx + rw && y >= ry && y <= ry + rh;
    }
}
