package com.snipersim.input;

/**
 * On-screen Android controls. Rectangles are in screen space with the origin
 * at the top-left, matching {@code MotionEvent} and libGDX touch coordinates.
 * Aim, fire and zoom are separate hit targets so a scope press cannot land
 * on the trigger.
 */
public final class TouchLayout {
    public final float adsX;
    public final float adsY;
    public final float adsW;
    public final float adsH;
    public final float fireX;
    public final float fireY;
    public final float fireW;
    public final float fireH;
    public final float zoomInX;
    public final float zoomInY;
    public final float zoomOutX;
    public final float zoomOutY;
    public final float zoomW;
    public final float zoomH;

    private TouchLayout(float width, float height) {
        float u = height / 900f;
        float size = 92f * u;
        float gap = 14f * u;
        float margin = 22f * u;
        adsW = fireW = size;
        adsH = fireH = size;
        zoomW = 64f * u;
        zoomH = (size - gap) * 0.5f;
        fireX = width - margin - size;
        fireY = height * 0.56f;
        adsX = fireX;
        adsY = fireY - gap - size;
        zoomInX = fireX - gap - zoomW;
        zoomOutX = zoomInX;
        zoomInY = adsY;
        zoomOutY = adsY + zoomH + gap;
    }

    public static TouchLayout of(float width, float height) {
        return new TouchLayout(Math.max(1f, width), Math.max(1f, height));
    }

    public boolean hitAds(float x, float y) {
        return inside(x, y, adsX, adsY, adsW, adsH);
    }

    public boolean hitFire(float x, float y) {
        return inside(x, y, fireX, fireY, fireW, fireH);
    }

    public boolean hitZoomIn(float x, float y) {
        return inside(x, y, zoomInX, zoomInY, zoomW, zoomH);
    }

    public boolean hitZoomOut(float x, float y) {
        return inside(x, y, zoomOutX, zoomOutY, zoomW, zoomH);
    }

    public boolean hitControl(float x, float y) {
        return hitAds(x, y) || hitFire(x, y) || hitZoomIn(x, y) || hitZoomOut(x, y);
    }

    private static boolean inside(float x, float y, float rx, float ry, float rw, float rh) {
        return x >= rx && x <= rx + rw && y >= ry && y <= ry + rh;
    }
}
