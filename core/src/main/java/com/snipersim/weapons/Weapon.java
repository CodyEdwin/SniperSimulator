package com.snipersim.weapons;

import com.badlogic.gdx.graphics.Color;
import com.snipersim.ballistics.BulletSpec;

/** A service sniper rifle: cartridge, optic, handling and terminal-effect ratings. */
public final class Weapon {
    public enum Action {
        BOLT("BOLT"),
        SEMI("SEMI");

        public final String label;

        Action(String label) {
            this.label = label;
        }
    }

    public final String id;
    public final String name;
    public final String shortName;
    public final String designation;
    public final String caliber;
    public final BulletSpec bullet;
    /** Real detachable or internal magazine. Ammo is not consumed. */
    public final int magazineSize;
    public final Action action;
    /** Seconds before the next shot. Bolt throw for bolt guns, action cycle for self-loaders. */
    public final double cycleTime;
    public final double recoilPitchRad;
    public final double recoilYawRad;
    public final double scopeMinZoom;
    public final double scopeMaxZoom;
    public final double scopeDefaultZoom;
    public final String optic;
    public final double sightHeightMeters;
    /** Published effective range used as the DOPE card limit, in metres. */
    public final int maxRangeMeters;
    /**
     * Muzzle-energy index, M24 / M118LR = 100. Terminal effect on a hit is still
     * the retained kinetic energy of the simulated bullet.
     */
    public final int damage;
    /** Relative penetration from sectional density and velocity. 7.62 match ≈ 4, .50 ≈ 9. */
    public final int penetration;
    public final Reticle reticle;
    public final Color accent;
    public final double muzzleFlashScale;
    public final double soundPitch;

    public Weapon(String id, String name, String shortName, String designation, String caliber,
                  BulletSpec bullet, int magazineSize, Action action, double cycleTime,
                  double recoilPitchRad, double recoilYawRad,
                  double scopeMinZoom, double scopeMaxZoom, double scopeDefaultZoom, String optic,
                  double sightHeightMeters, int maxRangeMeters, int damage, int penetration,
                  Reticle reticle, Color accent, double muzzleFlashScale, double soundPitch) {
        this.id = id;
        this.name = name;
        this.shortName = shortName;
        this.designation = designation;
        this.caliber = caliber;
        this.bullet = bullet;
        this.magazineSize = magazineSize;
        this.action = action;
        this.cycleTime = cycleTime;
        this.recoilPitchRad = recoilPitchRad;
        this.recoilYawRad = recoilYawRad;
        this.scopeMinZoom = scopeMinZoom;
        this.scopeMaxZoom = scopeMaxZoom;
        this.scopeDefaultZoom = scopeDefaultZoom;
        this.optic = optic;
        this.sightHeightMeters = sightHeightMeters;
        this.maxRangeMeters = maxRangeMeters;
        this.damage = damage;
        this.penetration = penetration;
        this.reticle = reticle;
        this.accent = accent;
        this.muzzleFlashScale = muzzleFlashScale;
        this.soundPitch = soundPitch;
    }

    public String specLine() {
        return String.format(java.util.Locale.ROOT,
                "%s | %.0f gr | %.0f m/s | G7 %.3f | %s",
                action.label, bullet.massGrains, bullet.muzzleVelocity, bullet.bcG7, optic);
    }
}
