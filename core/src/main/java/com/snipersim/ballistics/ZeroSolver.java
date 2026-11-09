package com.snipersim.ballistics;

import com.badlogic.gdx.math.Vector3;

/**
 * Finds the barrel elevation (relative to the scope line of sight) that makes
 * the bullet cross the line of sight at the zero distance. The bullet leaves
 * the muzzle below the scope by {@code sightHeight}; the solver accounts for
 * both that offset and aerodynamic drop.
 */
public final class ZeroSolver {
    private ZeroSolver() {
    }

    public static double solveZeroAngle(BulletSpec spec,
                                        Atmosphere atmosphere,
                                        WindModel wind,
                                        TerrainFunction terrain,
                                        Vector3 scopeOrigin,
                                        Vector3 levelDirection,
                                        double zeroDistanceMeters,
                                        double sightHeightMeters) {
        if (zeroDistanceMeters <= 5.0) {
            return 0;
        }
        BallisticsEngine engine = new BallisticsEngine(atmosphere, wind, terrain);
        Vector3 origin = new Vector3(scopeOrigin.x, scopeOrigin.y - (float) sightHeightMeters, scopeOrigin.z);
        Vector3 fwd = new Vector3(levelDirection.x, 0, levelDirection.z).nor();
        if (fwd.len2() < 1e-6) {
            fwd.set(0, 0, -1);
        }

        double a0 = 0.0005;
        double a1 = 0.0025;
        double e0 = error(spec, engine, origin, fwd, zeroDistanceMeters, a0);
        double e1 = error(spec, engine, origin, fwd, zeroDistanceMeters, a1);
        for (int i = 0; i < 14; i++) {
            double denom = e1 - e0;
            if (Math.abs(denom) < 1e-10) {
                break;
            }
            double a2 = a1 - e1 * (a1 - a0) / denom;
            if (!Double.isFinite(a2)) {
                break;
            }
            a2 = Math.max(-0.2, Math.min(0.2, a2));
            double e2 = error(spec, engine, origin, fwd, zeroDistanceMeters, a2);
            a0 = a1;
            e0 = e1;
            a1 = a2;
            e1 = e2;
            if (Math.abs(e2) < 0.0004) {
                break;
            }
        }
        return a1;
    }

    private static double error(BulletSpec spec, BallisticsEngine engine, Vector3 origin,
                                Vector3 levelDirection, double zeroDistance, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        Vector3 dir = new Vector3(
                (float) (levelDirection.x * cos),
                (float) sin,
                (float) (levelDirection.z * cos)).nor();
        double maxTime = Math.min(6.0, zeroDistance / 240.0 + 0.8);
        Trajectory trajectory = engine.simulate(spec, spec.muzzleVelocity, origin, dir, maxTime, 4000);
        Vector3 pos = new Vector3();
        Vector3 vel = new Vector3();
        float[] t = new float[1];
        if (!trajectory.sampleAtRange(zeroDistance, pos, vel, t)) {
            return 100;
        }
        return pos.y - origin.y;
    }
}
