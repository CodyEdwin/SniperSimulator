package com.snipersim.ballistics;

import com.badlogic.gdx.math.Vector3;

/**
 * Six-degree-of-freedom-lite point-mass solver. Each RK4 substep evaluates:
 *
 * <ul>
 *   <li>G7 drag with a Mach-dependent coefficient and BC-derived form factor</li>
 *   <li>3D wind with vertical shear and gusts (drag is computed on airspeed)</li>
 *   <li>Coriolis acceleration for the current latitude</li>
 *   <li>gyroscopic spin drift from the Miller stability factor</li>
 *   <li>Magnus force from crosswind acting on the spinning projectile</li>
 *   <li>transonic drag rise and destabilised-projectile drag penalty</li>
 * </ul>
 *
 * Fixed 1 ms RK4 steps keep the integration deterministic and stable at
 * muzzle velocity, while still evaluating wind at intermediate positions.
 */
public final class BallisticsEngine {
    public static final double DEFAULT_MAX_TIME = 8.0;
    public static final double DEFAULT_MAX_RANGE = 4000.0;
    private static final double STEP = 0.001;

    private final Atmosphere atmosphere;
    private final WindModel wind;
    private final TerrainFunction terrain;
    private final Vector3 scratchWind = new Vector3();

    public BallisticsEngine(Atmosphere atmosphere, WindModel wind, TerrainFunction terrain) {
        this.atmosphere = atmosphere;
        this.wind = wind;
        this.terrain = terrain;
    }

    public Atmosphere atmosphere() {
        return atmosphere;
    }

    public WindModel wind() {
        return wind;
    }

    public Trajectory simulate(BulletSpec spec, double muzzleVelocity,
                               Vector3 origin, Vector3 direction) {
        return simulate(spec, muzzleVelocity, origin, direction, DEFAULT_MAX_TIME, DEFAULT_MAX_RANGE);
    }

    public Trajectory simulate(BulletSpec spec, double muzzleVelocity,
                               Vector3 origin, Vector3 direction,
                               double maxTime, double maxRange) {
        double dirLen = direction.len();
        double dx = direction.x / dirLen;
        double dy = direction.y / dirLen;
        double dz = direction.z / dirLen;

        double x = origin.x;
        double y = origin.y;
        double z = origin.z;
        double vx = dx * muzzleVelocity;
        double vy = dy * muzzleVelocity;
        double vz = dz * muzzleVelocity;

        Trajectory trajectory = new Trajectory(origin, direction);
        trajectory.add(0f, x, y, z, vx, vy, vz);

        double hx = dx;
        double hz = dz;
        double hLen = Math.hypot(hx, hz);
        if (hLen < 1e-9) {
            hx = 1;
            hz = 0;
        } else {
            hx /= hLen;
            hz /= hLen;
        }
        // Right-hand twist drifts to the shooter's right. World frame is +X east, -Z north.
        final double rightX = -hz;
        final double rightZ = hx;

        final double stability = spec.stability(muzzleVelocity);
        final double formFactor = DragModel.formFactor(spec.massGrains, spec.diameterInches, spec.bcG7);
        final double area = spec.areaM2();
        final double mass = spec.massKg();
        final double spinRate = spec.spinRateRadPerSec(muzzleVelocity);
        final double dragMultiplier = stability < 1.0 ? 1.0 + 0.65 * (1.0 - stability) : 1.0;

        double t = 0;
        int steps = (int) Math.ceil(maxTime / STEP);
        for (int step = 0; step < steps; step++) {
            // Ground height is fixed for the four RK4 stages. Over one
            // millisecond the bullet moves under a millimetre horizontally, so
            // recomputing the terrain four times would only add cost.
            double groundBelow = terrain != null ? terrain.heightAt(x, z) : 0;
            double[] a = acceleration(x, y, z, vx, vy, vz, t,
                    formFactor, area, mass, stability, dragMultiplier, spinRate, rightX, rightZ,
                    groundBelow);
            double[] b = acceleration(
                    x + vx * STEP * 0.5, y + vy * STEP * 0.5, z + vz * STEP * 0.5,
                    vx + a[0] * STEP * 0.5, vy + a[1] * STEP * 0.5, vz + a[2] * STEP * 0.5,
                    t + STEP * 0.5,
                    formFactor, area, mass, stability, dragMultiplier, spinRate, rightX, rightZ,
                    groundBelow);
            double[] c = acceleration(
                    x + (vx + a[0] * STEP * 0.5) * STEP * 0.5,
                    y + (vy + a[1] * STEP * 0.5) * STEP * 0.5,
                    z + (vz + a[2] * STEP * 0.5) * STEP * 0.5,
                    vx + b[0] * STEP * 0.5, vy + b[1] * STEP * 0.5, vz + b[2] * STEP * 0.5,
                    t + STEP * 0.5,
                    formFactor, area, mass, stability, dragMultiplier, spinRate, rightX, rightZ,
                    groundBelow);
            double[] d = acceleration(
                    x + (vx + b[0] * STEP * 0.5) * STEP,
                    y + (vy + b[1] * STEP * 0.5) * STEP,
                    z + (vz + b[2] * STEP * 0.5) * STEP,
                    vx + c[0] * STEP, vy + c[1] * STEP, vz + c[2] * STEP,
                    t + STEP,
                    formFactor, area, mass, stability, dragMultiplier, spinRate, rightX, rightZ,
                    groundBelow);

            double prevX = x;
            double prevY = y;
            double prevZ = z;
            double prevVx = vx;
            double prevVy = vy;
            double prevVz = vz;

            // Classical RK4 position update. k2/k3 use the midpoint
            // accelerations from the stages above; k4 uses the end acceleration.
            x += STEP / 6.0 * (vx + 2.0 * (vx + a[0] * STEP * 0.5)
                    + 2.0 * (vx + b[0] * STEP * 0.5) + (vx + c[0] * STEP));
            y += STEP / 6.0 * (vy + 2.0 * (vy + a[1] * STEP * 0.5)
                    + 2.0 * (vy + b[1] * STEP * 0.5) + (vy + c[1] * STEP));
            z += STEP / 6.0 * (vz + 2.0 * (vz + a[2] * STEP * 0.5)
                    + 2.0 * (vz + b[2] * STEP * 0.5) + (vz + c[2] * STEP));
            vx += STEP * (a[0] + 2 * b[0] + 2 * c[0] + d[0]) / 6.0;
            vy += STEP * (a[1] + 2 * b[1] + 2 * c[1] + d[1]) / 6.0;
            vz += STEP * (a[2] + 2 * b[2] + 2 * c[2] + d[2]) / 6.0;
            t += STEP;

            float aboveLos = (float) (y - origin.y);
            if (aboveLos > trajectory.maxOrdinate) {
                trajectory.maxOrdinate = aboveLos;
            }
            if (y > trajectory.maxHeight) {
                trajectory.maxHeight = (float) y;
            }

            if (terrain != null) {
                double ground = terrain.heightAt(x, z);
                if (y <= ground) {
                    double fraction = findGroundFraction(prevX, prevY, prevZ, x, y, z, ground);
                    double hitX = prevX + (x - prevX) * fraction;
                    double hitY = prevY + (y - prevY) * fraction;
                    double hitZ = prevZ + (z - prevZ) * fraction;
                    double hitT = t - STEP + STEP * fraction;
                    trajectory.add((float) hitT, hitX, hitY, hitZ,
                            prevVx + (vx - prevVx) * fraction,
                            prevVy + (vy - prevVy) * fraction,
                            prevVz + (vz - prevVz) * fraction);
                    trajectory.impactType = Trajectory.IMPACT_TERRAIN;
                    trajectory.impactTime = (float) hitT;
                    trajectory.impactPoint.set((float) hitX, (float) hitY, (float) hitZ);
                    trajectory.impactVelocity.set(
                            (float) (prevVx + (vx - prevVx) * fraction),
                            (float) (prevVy + (vy - prevVy) * fraction),
                            (float) (prevVz + (vz - prevVz) * fraction));
                    return trajectory;
                }
            }

            trajectory.add((float) t, x, y, z, vx, vy, vz);

            double horizontal = Math.hypot(x - origin.x, z - origin.z);
            if (horizontal >= maxRange) {
                trajectory.impactType = Trajectory.IMPACT_MAX_RANGE;
                trajectory.impactTime = (float) t;
                trajectory.impactPoint.set((float) x, (float) y, (float) z);
                trajectory.impactVelocity.set((float) vx, (float) vy, (float) vz);
                return trajectory;
            }
            if (y < -500) {
                trajectory.impactType = Trajectory.IMPACT_MAX_RANGE;
                trajectory.impactTime = (float) t;
                trajectory.impactPoint.set((float) x, (float) y, (float) z);
                trajectory.impactVelocity.set((float) vx, (float) vy, (float) vz);
                return trajectory;
            }
        }

        trajectory.impactType = Trajectory.IMPACT_MAX_TIME;
        trajectory.impactTime = (float) t;
        trajectory.impactPoint.set((float) x, (float) y, (float) z);
        trajectory.impactVelocity.set((float) vx, (float) vy, (float) vz);
        return trajectory;
    }

    private double findGroundFraction(double x0, double y0, double z0,
                                      double x1, double y1, double z1,
                                      double groundAtEnd) {
        double lo = 0;
        double hi = 1;
        for (int i = 0; i < 14; i++) {
            double mid = (lo + hi) * 0.5;
            double mx = x0 + (x1 - x0) * mid;
            double my = y0 + (y1 - y0) * mid;
            double mz = z0 + (z1 - z0) * mid;
            double ground = terrain.heightAt(mx, mz);
            if (my > ground) {
                lo = mid;
            } else {
                hi = mid;
            }
        }
        return (lo + hi) * 0.5;
    }

    private double[] acceleration(double x, double y, double z,
                                  double vx, double vy, double vz, double time,
                                  double formFactor, double area, double mass,
                                  double stability, double dragMultiplier,
                                  double spinRate, double rightX, double rightZ,
                                  double groundBelow) {
        double heightAgl = Math.max(0.15, y - groundBelow);
        wind.windAt(heightAgl, time, scratchWind);
        double wx = scratchWind.x;
        double wy = scratchWind.y;
        double wz = scratchWind.z;

        double rvx = vx - wx;
        double rvy = vy - wy;
        double rvz = vz - wz;
        double relSpeed = Math.sqrt(rvx * rvx + rvy * rvy + rvz * rvz);
        if (relSpeed < 1e-6) {
            relSpeed = 1e-6;
        }
        double mach = relSpeed / atmosphere.speedOfSound();
        double cd = formFactor * DragModel.g7(mach) * dragMultiplier;
        double dragK = 0.5 * atmosphere.density() * cd * area / mass;
        double dragMag = dragK * relSpeed;

        double ax = -dragMag * rvx;
        double ay = -dragMag * rvy;
        double az = -dragMag * rvz;

        ay -= Atmosphere.GRAVITY;

        // Coriolis: a = -2 * omega x v.
        double ox = atmosphere.coriolisX();
        double oy = atmosphere.coriolisY();
        double oz = atmosphere.coriolisZ();
        ax -= 2.0 * (oy * vz - oz * vy);
        ay -= 2.0 * (oz * vx - ox * vz);
        az -= 2.0 * (ox * vy - oy * vx);

        // Gyroscopic spin drift (Litz-style approximation). Acceleration is
        // lateral and integrated into a rightward drift for right-hand twist.
        double tof = Math.max(time, 0.001);
        double driftMeters = 1.25 * (stability + 1.2) * Math.pow(tof, 1.83)
                * com.snipersim.util.BallisticMath.INCHES_TO_METERS;
        double driftAccel = 2.0 * driftMeters / Math.max(tof * tof, 0.04);
        ax += rightX * driftAccel;
        az += rightZ * driftAccel;

        // Magnus: crosswind acting on the spinning bullet. Kept deliberately
        // small; it is a real effect but secondary to drag and wind drift.
        double crosswind = wx * rightX + wz * rightZ;
        double magnus = 1.15e-9 * spinRate * crosswind * relSpeed;
        ax += rightX * magnus;
        az += rightZ * magnus;

        // Very small aerodynamic jump from the vertical wind component.
        ay += 2.5e-6 * spinRate * (wy) * relSpeed;

        return new double[]{ax, ay, az};
    }
}
