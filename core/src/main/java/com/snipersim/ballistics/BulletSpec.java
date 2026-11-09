package com.snipersim.ballistics;

import com.snipersim.util.BallisticMath;

/**
 * Immutable projectile description. Drag, gyroscopic stability, spin rate and
 * the dispersion model are all derived from these physical properties.
 */
public final class BulletSpec {
    public final String name;
    public final double massGrains;
    public final double diameterInches;
    public final double lengthInches;
    public final double bcG7;
    public final double twistInchesPerTurn;
    public final double muzzleVelocity;
    public final double muzzleVelocitySd;
    public final double precisionMoa;

    public BulletSpec(String name,
                      double massGrains,
                      double diameterInches,
                      double lengthInches,
                      double bcG7,
                      double twistInchesPerTurn,
                      double muzzleVelocity,
                      double muzzleVelocitySd,
                      double precisionMoa) {
        this.name = name;
        this.massGrains = massGrains;
        this.diameterInches = diameterInches;
        this.lengthInches = lengthInches;
        this.bcG7 = bcG7;
        this.twistInchesPerTurn = twistInchesPerTurn;
        this.muzzleVelocity = muzzleVelocity;
        this.muzzleVelocitySd = muzzleVelocitySd;
        this.precisionMoa = precisionMoa;
    }

    public double massKg() {
        return BallisticMath.grainsToKg(massGrains);
    }

    public double diameterMeters() {
        return BallisticMath.inchesToMeters(diameterInches);
    }

    public double areaM2() {
        double d = diameterMeters();
        return Math.PI * d * d * 0.25;
    }

    public double twistMeters() {
        return BallisticMath.inchesToMeters(twistInchesPerTurn);
    }

    /**
     * Miller gyroscopic stability factor. {@code t} is the twist expressed in
     * calibers, which is the convention the original rule uses. 1.0 is the
     * stability threshold; long-range loads usually sit between 1.4 and 2.2.
     */
    public double stability(double velocity) {
        double t = twistInchesPerTurn / diameterInches;
        double caliberLength = lengthInches / diameterInches;
        double denom = t * t
                * Math.pow(diameterInches, 3)
                * caliberLength
                * (1.0 + caliberLength * caliberLength);
        if (denom < 1e-9) {
            return 2.0;
        }
        double sg = 30.0 * massGrains / denom;
        double fps = velocity / 0.3048;
        sg *= Math.pow(Math.max(0.1, fps) / 2800.0, 1.0 / 3.0);
        return BallisticMath.clamp(sg, 0.2, 6.0);
    }

    public double spinRateRadPerSec(double velocity) {
        return 2.0 * Math.PI * velocity / Math.max(0.01, twistMeters());
    }

    public double muzzleEnergyJoules() {
        double m = massKg();
        return 0.5 * m * muzzleVelocity * muzzleVelocity;
    }

    public double sectionalDensity() {
        return (massGrains / 7000.0) / (diameterInches * diameterInches);
    }
}
