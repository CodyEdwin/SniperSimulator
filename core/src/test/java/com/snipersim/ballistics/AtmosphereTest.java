package com.snipersim.ballistics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtmosphereTest {

    @Test
    void standardSeaLevelDensity() {
        Atmosphere atmosphere = new Atmosphere(0, 1013.25, 15, 0, 0);
        assertEquals(1.225, atmosphere.density(), 0.002);
        assertEquals(340.3, atmosphere.speedOfSound(), 1.0);
        assertEquals(0, atmosphere.densityAltitudeMeters(), 1.0);
    }

    @Test
    void humidityLowersDensity() {
        Atmosphere dry = new Atmosphere(1000, 900, 30, 0, 30);
        Atmosphere humid = new Atmosphere(1000, 900, 30, 0.95, 30);
        assertTrue(humid.density() < dry.density());
    }

    @Test
    void highAltitudeIsThinner() {
        Atmosphere sea = new Atmosphere(0, 1013.25, 15, 0.2, 34);
        Atmosphere mountain = new Atmosphere(2500, 750, 10, 0.2, 34);
        assertTrue(mountain.density() < sea.density());
        assertTrue(mountain.densityAltitudeMeters() > 1500);
    }

    @Test
    void g7CurveHasTransonicRise() {
        double subsonic = DragModel.g7(0.8);
        double transonic = DragModel.g7(1.05);
        double supersonic = DragModel.g7(2.5);
        assertTrue(transonic > subsonic);
        assertTrue(transonic > supersonic);
        assertEquals(0.245, DragModel.g7(4.0), 0.01);
    }

    @Test
    void knownRifleHasStableBullet() {
        BulletSpec spec = new BulletSpec("175 SMK", 175, 0.308, 1.24, 0.243,
                11.25, 792, 5, 0.5);
        double stability = spec.stability(spec.muzzleVelocity);
        assertTrue(stability > 1.2, "stability=" + stability);
        assertTrue(stability < 2.6, "stability=" + stability);
    }
}
