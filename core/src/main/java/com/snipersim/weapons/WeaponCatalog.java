package com.snipersim.weapons;

import com.badlogic.gdx.graphics.Color;
import com.snipersim.ballistics.BulletSpec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Seven chamberings from a rimfire trainer to an anti-materiel rifle. G7 BCs,
 * twist rates and muzzle velocities are representative factory/match data.
 */
public final class WeaponCatalog {
    private static final List<Weapon> WEAPONS;

    static {
        List<Weapon> list = new ArrayList<>();

        list.add(new Weapon(
                "rimfire", "RIMFIRE TRAINER", "CZ 457 VARMINT", ".22 LR",
                new BulletSpec("40 gr Lead Round Nose", 40, 0.224, 0.500, 0.065,
                        16.0, 326, 8.0, 1.20),
                5, 0.45, 2.6, 0.0022, 0.0007,
                4, 16, 8, 0.050, 500, Reticle.DUPLEX,
                new Color(0.55f, 0.72f, 0.85f, 1f), 0.55, 1.32));

        list.add(new Weapon(
                "carbine", "CARBINE", "MK 12 SPECIAL PURPOSE", "5.56 NATO",
                new BulletSpec("62 gr M855", 62, 0.224, 0.906, 0.151,
                        7.0, 948, 6.0, 0.90),
                10, 0.38, 2.2, 0.0050, 0.0015,
                3, 18, 10, 0.058, 900, Reticle.MIL_DOT,
                new Color(0.72f, 0.82f, 0.55f, 1f), 0.75, 1.08));

        list.add(new Weapon(
                "creedmoor", "CREEDMOOR", "MPA BA 6.5", "6.5 CREEDMOOR",
                new BulletSpec("140 gr ELD Match", 140, 0.264, 1.350, 0.322,
                        8.0, 823, 4.0, 0.40),
                5, 0.80, 3.1, 0.0075, 0.0021,
                5, 25, 14, 0.060, 1600, Reticle.CHRISTMAS_TREE,
                new Color(0.42f, 0.85f, 0.72f, 1f), 0.85, 0.96));

        list.add(new Weapon(
                "workhorse", "THE WORKHORSE", "M40A6", ".308 WIN",
                new BulletSpec("175 gr SMK", 175, 0.308, 1.240, 0.243,
                        11.25, 792, 5.0, 0.45),
                5, 0.85, 3.2, 0.0095, 0.0028,
                5, 25, 12, 0.062, 1400, Reticle.MIL_DOT,
                new Color(0.95f, 0.72f, 0.35f, 1f), 0.95, 0.88));

        list.add(new Weapon(
                "magnum", "MAGNUM", "AWM .300", ".300 WIN MAG",
                new BulletSpec("190 gr Berger LR", 190, 0.308, 1.400, 0.285,
                        10.0, 900, 6.0, 0.55),
                5, 0.95, 3.4, 0.0140, 0.0038,
                5, 25, 16, 0.064, 1800, Reticle.CHRISTMAS_TREE,
                new Color(0.96f, 0.56f, 0.30f, 1f), 1.10, 0.78));

        list.add(new Weapon(
                "lapua", "LAPUA", "AI AXMC", ".338 LAPUA MAG",
                new BulletSpec("300 gr Scenar", 300, 0.338, 1.650, 0.381,
                        9.375, 869, 5.0, 0.40),
                5, 1.05, 3.8, 0.0190, 0.0048,
                5, 27, 18, 0.067, 2200, Reticle.CHRISTMAS_TREE,
                new Color(0.82f, 0.55f, 0.98f, 1f), 1.25, 0.68));

        list.add(new Weapon(
                "antimateriel", "ANTI-MATERIEL", "BARRETT M107A1", ".50 BMG",
                new BulletSpec("750 gr A-MAX", 750, 0.510, 2.300, 0.530,
                        15.0, 860, 6.0, 0.80),
                10, 1.35, 5.0, 0.0340, 0.0085,
                4, 20, 10, 0.075, 2500, Reticle.MIL_DOT,
                new Color(0.95f, 0.32f, 0.28f, 1f), 1.60, 0.52));

        WEAPONS = Collections.unmodifiableList(list);
    }

    private WeaponCatalog() {
    }

    public static List<Weapon> all() {
        return WEAPONS;
    }

    public static Weapon byIndex(int index) {
        int i = Math.floorMod(index, WEAPONS.size());
        return WEAPONS.get(i);
    }

    public static int indexOf(Weapon weapon) {
        return WEAPONS.indexOf(weapon);
    }

    public static Weapon defaultWeapon() {
        return WEAPONS.get(3);
    }
}
