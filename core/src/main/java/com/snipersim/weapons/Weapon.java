package com.snipersim.weapons;

import com.badlogic.gdx.graphics.Color;
import com.snipersim.ballistics.BulletSpec;

/** A complete rifle: chambering, projectile, optics and handling characteristics. */
public final class Weapon {
    public final String id;
    public final String name;
    public final String designation;
    public final String caliber;
    public final BulletSpec bullet;
    public final int magazineSize;
    public final double cycleTime;
    public final double reloadTime;
    public final double recoilPitchRad;
    public final double recoilYawRad;
    public final double scopeMinZoom;
    public final double scopeMaxZoom;
    public final double scopeDefaultZoom;
    public final double sightHeightMeters;
    public final int maxRangeMeters;
    public final Reticle reticle;
    public final Color accent;
    public final double muzzleFlashScale;
    public final double soundPitch;

    public Weapon(String id, String name, String designation, String caliber,
                  BulletSpec bullet, int magazineSize, double cycleTime, double reloadTime,
                  double recoilPitchRad, double recoilYawRad,
                  double scopeMinZoom, double scopeMaxZoom, double scopeDefaultZoom,
                  double sightHeightMeters, int maxRangeMeters, Reticle reticle,
                  Color accent, double muzzleFlashScale, double soundPitch) {
        this.id = id;
        this.name = name;
        this.designation = designation;
        this.caliber = caliber;
        this.bullet = bullet;
        this.magazineSize = magazineSize;
        this.cycleTime = cycleTime;
        this.reloadTime = reloadTime;
        this.recoilPitchRad = recoilPitchRad;
        this.recoilYawRad = recoilYawRad;
        this.scopeMinZoom = scopeMinZoom;
        this.scopeMaxZoom = scopeMaxZoom;
        this.scopeDefaultZoom = scopeDefaultZoom;
        this.sightHeightMeters = sightHeightMeters;
        this.maxRangeMeters = maxRangeMeters;
        this.reticle = reticle;
        this.accent = accent;
        this.muzzleFlashScale = muzzleFlashScale;
        this.soundPitch = soundPitch;
    }

    public String specLine() {
        return String.format(java.util.Locale.ROOT,
                "%.1f gr | %.0f m/s | G7 BC %.3f | 1:%.2f\" twist",
                bullet.massGrains, bullet.muzzleVelocity, bullet.bcG7, bullet.twistInchesPerTurn);
    }
}
