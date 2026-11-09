package com.snipersim.ballistics;

import com.badlogic.gdx.math.Vector3;

import java.util.ArrayList;
import java.util.List;

/**
 * Data of prior engagement (DOPE) table. One nominal trajectory (no shooter
 * dispersion) is sampled every 100 m, giving drop, drift, time of flight,
 * retained velocity/energy and the elevation/windage corrections a shooter
 * would dial onto the scope.
 */
public final class BallisticTable {
    public static final int STEP_METERS = 100;

    public static final class Row {
        public final int range;
        public final double dropMeters;
        public final double driftMeters;
        public final double tof;
        public final double velocity;
        public final double energyJoules;
        public final double mach;
        public final int elevationClicks;
        public final int windClicks;

        Row(int range, double dropMeters, double driftMeters, double tof, double velocity,
            double energyJoules, double mach, int elevationClicks, int windClicks) {
            this.range = range;
            this.dropMeters = dropMeters;
            this.driftMeters = driftMeters;
            this.tof = tof;
            this.velocity = velocity;
            this.energyJoules = energyJoules;
            this.mach = mach;
            this.elevationClicks = elevationClicks;
            this.windClicks = windClicks;
        }
    }

    public final Atmosphere atmosphere;
    public final WindModel wind;
    public final BulletSpec bullet;
    public final double zeroAngle;
    public final int zeroDistance;
    public final List<Row> rows = new ArrayList<>();

    public BallisticTable(BulletSpec bullet,
                          Atmosphere atmosphere,
                          WindModel wind,
                          TerrainFunction terrain,
                          Vector3 scopeOrigin,
                          double zeroDistance,
                          double sightHeightMeters,
                          int maxRange) {
        this.bullet = bullet;
        this.atmosphere = atmosphere;
        this.wind = wind;
        this.zeroDistance = (int) zeroDistance;
        Vector3 aim = new Vector3(0, 0, -1);
        this.zeroAngle = ZeroSolver.solveZeroAngle(bullet, atmosphere, wind, terrain,
                scopeOrigin, aim, zeroDistance, sightHeightMeters);

        BallisticsEngine engine = new BallisticsEngine(atmosphere, wind, terrain);
        Vector3 origin = new Vector3(scopeOrigin.x, scopeOrigin.y - (float) sightHeightMeters, scopeOrigin.z);
        Vector3 direction = new Vector3(0, (float) Math.sin(zeroAngle), (float) -Math.cos(zeroAngle));
        Trajectory trajectory = engine.simulate(bullet, bullet.muzzleVelocity, origin, direction, 10.0, 4000);
        Vector3 pos = new Vector3();
        Vector3 vel = new Vector3();
        float[] tof = new float[1];
        int step = Math.max(100, STEP_METERS);
        for (int range = step; range <= maxRange; range += step) {
            if (!trajectory.sampleAtRange(range, pos, vel, tof)) {
                break;
            }
            double drop = pos.y - origin.y;
            double drift = pos.x - origin.x;
            double speed = vel.len();
            double energy = 0.5 * bullet.massKg() * speed * speed;
            double elevationMils = Math.atan2(-drop, range) * 1000.0;
            double windMils = -Math.atan2(drift, range) * 1000.0;
            rows.add(new Row(range, drop, drift, tof[0], speed, energy,
                    speed / atmosphere.speedOfSound(),
                    (int) Math.round(elevationMils * 10),
                    (int) Math.round(windMils * 10)));
        }
    }

    public Row rowFor(int range) {
        Row best = null;
        for (Row row : rows) {
            if (row.range <= range) {
                best = row;
            } else {
                break;
            }
        }
        return best;
    }

    public boolean isEmpty() {
        return rows.isEmpty();
    }
}
