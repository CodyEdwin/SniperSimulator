package com.snipersim.weapons;

import com.badlogic.gdx.graphics.Color;
import com.snipersim.ballistics.BulletSpec;
import com.snipersim.weapons.Weapon.Action;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Seven issued military sniper rifles. One row per rifle. Ballistics use the
 * published cartridge (mass, muzzle velocity, twist); G7 BCs are the published
 * figure where one exists and are marked when estimated. Damage is muzzle
 * energy relative to the M24's M118LR (3,539 J = 100). Penetration is a 1–10
 * index from sectional density × velocity. Magazine capacity is the real
 * feed; the simulation does not consume ammunition.
 *
 * <p>Sources (abbreviated; see the release notes for the full list):
 * <ul>
 *   <li>M110 — US Army TM / Knight's operator data: 20 in barrel, M118LR
 *       2,571 fps (784 m/s), effective 800 m point / 1,000 m with M118LR,
 *       Leupold Mark 4 3.5–10×40, 10- or 20-round SR-25 magazine.</li>
 *   <li>M24 — TM 9-1005-306-10 and Wikipedia M24 SWS: 24 in, 1:11.2 twist,
 *       M118LR 2,580–2,600 fps, effective 800 m, 5-round internal magazine,
 *       Leupold Ultra M3A 10×42 / Mk 4 10×40 fixed.</li>
 *   <li>L96A1 — Wikipedia Arctic Warfare / militaryfactory: PM adopted 1984–86
 *       as L96A1, 7.62×51, 10-round magazine, 850 m/s, Schmidt &amp; Bender
 *       6×42 (L13A1); later 3–12×50. Accuracy trials used 168 gr.</li>
 *   <li>L115A3 — British Army equipment page: 8.59 mm, 936 m/s, 5-round box,
 *       effective 1,100+ m. SSIP optic is the Schmidt &amp; Bender PM II 5–25×56.
 *       The 250 gr LockBase B408 is the issued projectile (Harrison, 2009).</li>
 *   <li>M200 — CheyTac 2026 military catalog: .408, 29.5 in, 1:10, 419 gr
 *       Balanced Flight at 2,750 fps, G1 BC 0.949, 7-round magazine, effective
 *       2,500+ yd. Military kit optic Nightforce ATACR 7–35×56.</li>
 *   <li>TAC-50 — Wikipedia / McMillan manual: .50 BMG, bolt, 29 in 1:15,
 *       5-round magazine, 750 gr A-MAX at 823 m/s, effective 1,800 m.
 *       Canadian C15 optic Schmidt &amp; Bender 5–25×56 PM II.</li>
 *   <li>M107 — Barrett M107A1 operator manual: semi-auto, 29 in, 1:15,
 *       10-round magazine, 660–661 gr M33 at 2,750 fps (853 m/s). Wikipedia:
 *       Leupold Mark 4 4.5–14×50, effective about 1,800–1,830 m.</li>
 * </ul>
 */
public final class WeaponCatalog {
    private static final List<Weapon> WEAPONS;

    static {
        List<Weapon> list = new ArrayList<>();

        // M110 SASS. 175 gr SMK G7 0.243 (Sierra). MV 784 m/s (2,571 fps, TM).
        // Energy ≈ 3,485 J → damage 98. SD 0.263.
        list.add(rifle(
                "m110", "M110 SASS", "M110", "KNIGHT'S ARMAMENT", "7.62×51mm",
                new BulletSpec("175 gr M118LR SMK", 175, 0.308, 1.240, 0.243,
                        11.0, 784, 4.0, 0.75),
                20, Action.SEMI, 0.18, 0.0065, 0.0016,
                3.5, 10, 6, "Leupold Mark 4 3.5-10×40",
                0.055, 1000, 98, 4, Reticle.MIL_DOT,
                new Color(0.55f, 0.72f, 0.85f, 1f), 0.85, 0.95));

        // M24 SWS. Same 175 gr SMK. MV 790 m/s (Wikipedia 2,580 fps; TM ~2,600).
        // Energy ≈ 3,539 J → damage 100. Fixed 10× issued optic. Twist 1:11.25.
        list.add(rifle(
                "m24", "M24 SWS", "M24", "REMINGTON", "7.62×51mm",
                new BulletSpec("175 gr M118LR SMK", 175, 0.308, 1.240, 0.243,
                        11.25, 790, 4.0, 0.50),
                5, Action.BOLT, 1.05, 0.0090, 0.0024,
                10, 10, 10, "Leupold Mk 4 10×40 fixed",
                0.045, 800, 100, 4, Reticle.MIL_DOT,
                new Color(0.95f, 0.72f, 0.35f, 1f), 0.95, 0.90));

        // L96A1. 168 gr SMK, G7 0.223 (JBM / Sierra G1 0.462). Published MV
        // 850 m/s from the 26 in barrel. Energy ≈ 3,933 J → damage 111.
        // Issued 6×42, with the 3–12×50 the rifle was later fitted with.
        list.add(rifle(
                "l96a1", "L96A1", "L96A1", "ACCURACY INTERNATIONAL", "7.62×51mm",
                new BulletSpec("168 gr SMK", 168, 0.308, 1.215, 0.223,
                        12.0, 850, 4.0, 0.45),
                10, Action.BOLT, 1.10, 0.0085, 0.0022,
                3, 12, 6, "Schmidt & Bender 6×42 / 3-12×50",
                0.048, 1000, 111, 4, Reticle.MIL_DOT,
                new Color(0.42f, 0.78f, 0.62f, 1f), 0.90, 0.88));

        // L115A3 AWM. 250 gr LockBase B408 at 936 m/s (British Army).
        // G7 0.322 from the Lapua 250 gr Scenar, the published neighbour.
        // Energy ≈ 7,096 J → damage 201. S&B PM II 5–25×56.
        list.add(rifle(
                "l115a3", "L115A3", "L115", "AI AWM", ".338 LAPUA",
                new BulletSpec("250 gr LockBase B408", 250, 0.338, 1.575, 0.322,
                        11.0, 936, 3.0, 0.40),
                5, Action.BOLT, 1.25, 0.0150, 0.0036,
                5, 25, 12, "Schmidt & Bender PM II 5-25×56",
                0.055, 1500, 201, 6, Reticle.CHRISTMAS_TREE,
                new Color(0.82f, 0.55f, 0.98f, 1f), 1.20, 0.72));

        // CheyTac M200. 419 gr at 838 m/s (2,750 fps, CheyTac catalog).
        // Published BC is G1 0.949; G7 0.480 is the boat-tail estimate.
        // Energy ≈ 9,534 J → damage 269. Nightforce ATACR 7–35×56.
        list.add(rifle(
                "m200", "M200", "M200", "CHEYTAC INTERVENTION", ".408 CHEYTAC",
                new BulletSpec("419 gr Balanced Flight", 419, 0.408, 2.050, 0.480,
                        10.0, 838, 4.0, 0.35),
                7, Action.BOLT, 1.45, 0.0200, 0.0046,
                7, 35, 15, "Nightforce ATACR 7-35×56",
                0.065, 2300, 269, 7, Reticle.CHRISTMAS_TREE,
                new Color(0.96f, 0.56f, 0.30f, 1f), 1.40, 0.62));

        // McMillan TAC-50. 750 gr A-MAX, Hornady G7 0.530, 823 m/s (Wikipedia).
        // Energy ≈ 16,460 J → damage 465. Canadian 5–25×56.
        list.add(rifle(
                "tac50", "TAC-50", "TAC50", "MCMILLAN", ".50 BMG",
                new BulletSpec("750 gr A-MAX", 750, 0.510, 2.300, 0.530,
                        15.0, 823, 5.0, 0.50),
                5, Action.BOLT, 1.55, 0.0300, 0.0065,
                5, 25, 12, "Schmidt & Bender PM II 5-25×56",
                0.070, 1800, 465, 9, Reticle.CHRISTMAS_TREE,
                new Color(0.95f, 0.82f, 0.45f, 1f), 1.65, 0.50));

        // Barrett M82 / M107. M33 ball, Barrett manual 853 m/s, catalog G1 0.62
        // → G7 ≈ 0.310. Energy ≈ 15,582 J → damage 440. Leupold 4.5–14×, ~10×.
        list.add(rifle(
                "m107", "M107", "M107", "BARRETT M82A1", ".50 BMG",
                new BulletSpec("661 gr M33 Ball", 661, 0.510, 2.220, 0.310,
                        15.0, 853, 6.0, 1.50),
                10, Action.SEMI, 0.28, 0.0260, 0.0060,
                4.5, 14, 10, "Leupold Mark 4 4.5-14×50",
                0.075, 1830, 440, 9, Reticle.MIL_DOT,
                new Color(0.95f, 0.32f, 0.28f, 1f), 1.70, 0.48));

        WEAPONS = Collections.unmodifiableList(list);
    }

    private WeaponCatalog() {
    }

    private static Weapon rifle(String id, String name, String shortName, String designation, String caliber,
                                BulletSpec bullet, int magazine, Action action, double cycle,
                                double recoilPitch, double recoilYaw,
                                double zoomMin, double zoomMax, double zoomDefault, String optic,
                                double sightHeight, int maxRange, int damage, int penetration,
                                Reticle reticle, Color accent, double flash, double pitch) {
        return new Weapon(id, name, shortName, designation, caliber, bullet, magazine, action, cycle,
                recoilPitch, recoilYaw, zoomMin, zoomMax, zoomDefault, optic, sightHeight, maxRange,
                damage, penetration, reticle, accent, flash, pitch);
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

    /** The L96A1: the rifle the range is zeroed around by default. */
    public static Weapon defaultWeapon() {
        return WEAPONS.get(2);
    }
}
