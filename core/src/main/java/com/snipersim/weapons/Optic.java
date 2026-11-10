package com.snipersim.weapons;

/** A scope that can be mounted on any of the catalog rifles. */
public final class Optic {
    public final String name;
    public final double minZoom;
    public final double maxZoom;
    public final double defaultZoom;
    public final Reticle reticle;
    public final double sightHeightMeters;

    public Optic(String name, double minZoom, double maxZoom, double defaultZoom,
                 Reticle reticle, double sightHeightMeters) {
        this.name = name;
        this.minZoom = minZoom;
        this.maxZoom = maxZoom;
        this.defaultZoom = defaultZoom;
        this.reticle = reticle;
        this.sightHeightMeters = sightHeightMeters;
    }
}
