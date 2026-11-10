package com.snipersim.weapons;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoadoutTest {
    @Test
    void buildKeepsRifleAndAppliesScope() {
        Loadout loadout = Loadout.issued();
        loadout.rifleIndex = 6;
        loadout.adoptOptic(4);
        loadout.zoomDefault = 20;
        loadout.reticle = Reticle.DUPLEX;
        loadout.sightHeight = 0.04;
        Weapon built = loadout.build();
        assertEquals("m107", built.id);
        assertEquals("Nightforce ATACR 7-35×56", built.optic);
        assertEquals(20, built.scopeDefaultZoom, 0.01);
        assertEquals(Reticle.DUPLEX, built.reticle);
        assertEquals(0.04, built.sightHeightMeters, 0.001);
        assertEquals(853, built.bullet.muzzleVelocity, 0.1);
        assertTrue(built.scopeMinZoom <= built.scopeDefaultZoom);
        assertTrue(built.scopeDefaultZoom <= built.scopeMaxZoom);
    }
}