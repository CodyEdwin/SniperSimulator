package com.snipersim.ballistics;

import com.badlogic.gdx.math.Vector3;
import com.snipersim.util.BallisticMath;

/**
 * Time-varying wind field. The "from" bearing is meteorological (270 means
 * wind out of the west, blowing east). Gusts are a deterministic sum of
 * sinusoids so the same seed always produces the same range conditions.
 */
public final class WindModel {
    public static final double[] WIND_LEVELS_M = {0, 1, 2, 5, 10, 20, 30, 50};
    private static final double[] SHEAR = {0.18, 0.28, 0.40, 0.62, 1.00, 1.22, 1.32, 1.42};

    public double baseSpeed;
    public double fromBearingDeg;
    public double gustAmplitude;
    public double directionVariationDeg;
    public long seed;

    public WindModel(double baseSpeed, double fromBearingDeg, double gustAmplitude,
                     double directionVariationDeg, long seed) {
        this.baseSpeed = baseSpeed;
        this.fromBearingDeg = fromBearingDeg;
        this.gustAmplitude = gustAmplitude;
        this.directionVariationDeg = directionVariationDeg;
        this.seed = seed;
    }

    public static WindModel calm() {
        return new WindModel(0, 0, 0, 0, 1L);
    }

    public double speedAt(double time, double heightMeters) {
        double height = Math.max(0.15, heightMeters);
        double shear = windShear(height);
        double t = time + (seed & 1023) * 0.013;
        double gust = 0.55 * Math.sin(t * 1.71 + seed * 0.37)
                + 0.28 * Math.sin(t * 3.13 + seed * 0.91)
                + 0.17 * Math.sin(t * 0.73 + seed * 1.73);
        double speed = baseSpeed * shear + gustAmplitude * gust;
        return Math.max(0.0, speed);
    }

    public double bearingAt(double time) {
        double t = time + (seed & 255) * 0.017;
        double wobble = directionVariationDeg
                * (0.7 * Math.sin(t * 0.61 + seed * 0.21) + 0.3 * Math.sin(t * 1.47 + seed));
        return fromBearingDeg + wobble;
    }

    private static double windShear(double height) {
        if (height <= WIND_LEVELS_M[0]) {
            return SHEAR[0];
        }
        int last = WIND_LEVELS_M.length - 1;
        if (height >= WIND_LEVELS_M[last]) {
            return SHEAR[last];
        }
        int lo = 0;
        int hi = last;
        while (hi - lo > 1) {
            int mid = (lo + hi) >>> 1;
            if (WIND_LEVELS_M[mid] <= height) {
                lo = mid;
            } else {
                hi = mid;
            }
        }
        double span = WIND_LEVELS_M[hi] - WIND_LEVELS_M[lo];
        double f = (height - WIND_LEVELS_M[lo]) / span;
        return BallisticMath.lerp(SHEAR[lo], SHEAR[hi], f);
    }

    /**
     * Wind velocity in world space: +X east, +Y up, -Z north. The height is
     * metres above ground, not altitude, because the shear profile is defined
     * relative to the surface the bullet is flying over.
     */
    public void windAt(double heightAboveGround, double time, Vector3 out) {
        double speed = speedAt(time, heightAboveGround);
        double bearing = bearingAt(time);
        double toward = Math.toRadians(bearing + 180.0);
        out.set((float) (Math.sin(toward) * speed), 0f, (float) (-Math.cos(toward) * speed));
    }

    /** Bearing relative to a line of fire, 0-360 degrees clockwise. */
    public double relativeBearing(Vector3 aimDirection) {
        double aimBearing = Math.toDegrees(Math.atan2(aimDirection.x, -aimDirection.z));
        double rel = normalizeDegrees(fromBearingDeg - aimBearing);
        return rel;
    }

    /** "Clock" call: 12 = headwind, 6 = tailwind, 3 = from the right. */
    public double clockPosition(Vector3 aimDirection) {
        double rel = Math.toRadians(relativeBearing(aimDirection));
        double clock = (rel / (Math.PI * 2.0)) * 12.0;
        return (clock + 12.0) % 12.0;
    }

    /**
     * Full-value crosswind component (m/s). Positive is left-to-right across
     * the line of fire, matching the direction the bullet gets pushed.
     */
    public double crosswindComponent(Vector3 aimDirection, double time) {
        double speed = speedAt(time, 10);
        double bearing = bearingAt(time);
        double toward = Math.toRadians(bearing + 180.0);
        double windX = Math.sin(toward) * speed;
        double windZ = -Math.cos(toward) * speed;
        double dirX = aimDirection.x;
        double dirZ = aimDirection.z;
        double len = Math.hypot(dirX, dirZ);
        if (len < 1e-6) {
            return 0;
        }
        dirX /= len;
        dirZ /= len;
        double rightX = -dirZ;
        double rightZ = dirX;
        return windX * rightX + windZ * rightZ;
    }

    public static double normalizeDegrees(double deg) {
        double d = deg % 360.0;
        return d < 0 ? d + 360.0 : d;
    }

    public static String compassPoint(double bearing) {
        String[] points = {"N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
                "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW"};
        int idx = (int) Math.round(normalizeDegrees(bearing) / 22.5) & 15;
        return points[idx];
    }
}
