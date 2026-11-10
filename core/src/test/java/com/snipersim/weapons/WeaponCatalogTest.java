package com.snipersim.weapons;

import com.snipersim.weapons.Weapon.Action;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeaponCatalogTest {
    @Test
    void sevenServiceRiflesMatchPublishedFeedsAndOptics() {
        List<Weapon> weapons = WeaponCatalog.all();
        assertEquals(7, weapons.size());
        assertEquals("L96A1", WeaponCatalog.defaultWeapon().name);

        Set<String> names = new HashSet<>();
        for (Weapon weapon : weapons) {
            names.add(weapon.name);
            assertTrue(weapon.magazineSize >= 5, weapon.name);
            assertTrue(weapon.scopeDefaultZoom >= weapon.scopeMinZoom, weapon.name);
            assertTrue(weapon.scopeDefaultZoom <= weapon.scopeMaxZoom, weapon.name);
            assertTrue(weapon.bullet.muzzleVelocity > 700, weapon.name);
            assertTrue(weapon.damage >= 90, weapon.name);
            assertTrue(weapon.penetration >= 4 && weapon.penetration <= 9, weapon.name);
            assertTrue(weapon.maxRangeMeters >= 800, weapon.name);
        }
        assertTrue(names.contains("L96A1"));
        assertTrue(names.contains("M107"));

        Weapon l96 = weapons.get(2);
        assertEquals("7.62×51mm", l96.caliber);
        assertEquals(10, l96.magazineSize);
        assertEquals(Action.BOLT, l96.action);
        assertEquals(3, l96.scopeMinZoom, 0.01);
        assertEquals(12, l96.scopeMaxZoom, 0.01);
        assertEquals(6, l96.scopeDefaultZoom, 0.01);
        assertEquals(850, l96.bullet.muzzleVelocity, 0.1);

        Weapon m107 = weapons.get(6);
        assertEquals(".50 BMG", m107.caliber);
        assertEquals(10, m107.magazineSize);
        assertEquals(Action.SEMI, m107.action);
        assertEquals(4.5, m107.scopeMinZoom, 0.01);
        assertEquals(14, m107.scopeMaxZoom, 0.01);
        assertEquals(10, m107.scopeDefaultZoom, 0.01);
        assertEquals(853, m107.bullet.muzzleVelocity, 0.1);
        assertTrue(m107.damage > l96.damage);
        assertTrue(m107.cycleTime < l96.cycleTime);

        Weapon m24 = weapons.get(1);
        assertEquals(10, m24.scopeMinZoom, 0.01);
        assertEquals(10, m24.scopeMaxZoom, 0.01);

        Weapon m200 = weapons.get(4);
        assertEquals(35, m200.scopeMaxZoom, 0.01);
        assertTrue(m200.bullet.bcG7 > m24.bullet.bcG7);
    }
}
