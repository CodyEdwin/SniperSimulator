package com.snipersim.util;

/**
 * Small ballistic/angular unit helpers shared by the simulation and the HUD.
 * Everything inside the simulation is SI (metres, seconds, radians); the HUD
 * converts to the units shooters actually speak.
 */
public final class BallisticMath {
    public static final double MOA_RAD = Math.toRadians(1.0 / 60.0);
    public static final double MIL_RAD = 0.001;
    public static final double MOA_PER_MIL = MIL_RAD / MOA_RAD;
    public static final double METERS_PER_YARD = 0.9144;
    public static final double JOULES_PER_FOOT_POUND = 1.3558179483314;
    public static final double GRAINS_TO_KG = 0.00006479891;
    public static final double INCHES_TO_METERS = 0.0254;

    private BallisticMath() {
    }

    public static double clamp(double value, double min, double max) {
        return value < min ? min : (value > max ? max : value);
    }

    public static float clamp(float value, float min, float max) {
        return value < min ? min : (value > max ? max : value);
    }

    public static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    public static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    public static double metersToYards(double meters) {
        return meters / METERS_PER_YARD;
    }

    public static double joulesToFootPounds(double joules) {
        return joules / JOULES_PER_FOOT_POUND;
    }

    public static double grainsToKg(double grains) {
        return grains * GRAINS_TO_KG;
    }

    public static double inchesToMeters(double inches) {
        return inches * INCHES_TO_METERS;
    }

    public static double milsToRadians(double mils) {
        return mils * MIL_RAD;
    }

    public static double radiansToMils(double radians) {
        return radians / MIL_RAD;
    }

    public static double radiansToMoa(double radians) {
        return radians / MOA_RAD;
    }

    public static double moaToRadians(double moa) {
        return moa * MOA_RAD;
    }

    /** Angular size in mils of an object of {@code size} metres at {@code range} metres. */
    public static double sizeToMils(double sizeMeters, double rangeMeters) {
        if (rangeMeters <= 1e-6) {
            return 0;
        }
        return Math.toDegrees(2.0 * Math.atan(sizeMeters / (2.0 * rangeMeters))) * 1000.0 / 17.4532925;
    }

    /** Linear interpolation between scalar arrays, clamped at both ends. */
    public static double interpolate(double[] xs, double[] ys, double x) {
        if (x <= xs[0]) {
            return ys[0];
        }
        int last = xs.length - 1;
        if (x >= xs[last]) {
            return ys[last];
        }
        int lo = 0;
        int hi = last;
        while (hi - lo > 1) {
            int mid = (lo + hi) >>> 1;
            if (xs[mid] <= x) {
                lo = mid;
            } else {
                hi = mid;
            }
        }
        double span = xs[hi] - xs[lo];
        double t = span <= 1e-12 ? 0 : (x - xs[lo]) / span;
        return ys[lo] + (ys[hi] - ys[lo]) * t;
    }

    public static String formatSigned(double value, int decimals) {
        String body = String.format(java.util.Locale.ROOT, "%." + decimals + "f", Math.abs(value));
        return (value >= 0 ? "+" : "-") + body;
    }
}
