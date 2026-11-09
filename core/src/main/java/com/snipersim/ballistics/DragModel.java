package com.snipersim.ballistics;

/**
 * G7 standard drag curve. A bullet's BC turns its sectional density into a
 * form factor (i = SD / BC); actual drag is Cd = i * CdG7(Mach). This is the
 * same construction modern long-range solvers use for boat-tail match bullets.
 */
public final class DragModel {
    private static final double[] MACH = {
            0.00, 0.40, 0.50, 0.60, 0.70, 0.80, 0.85, 0.90, 0.95, 1.00,
            1.05, 1.10, 1.20, 1.30, 1.40, 1.50, 1.60, 1.80, 2.00, 2.20,
            2.50, 3.00, 4.00
    };
    private static final double[] CD = {
            0.1197, 0.1197, 0.1200, 0.1230, 0.1330, 0.1600, 0.1850, 0.2200, 0.2800, 0.3800,
            0.4200, 0.4250, 0.4100, 0.3900, 0.3700, 0.3500, 0.3350, 0.3150, 0.3000, 0.2900,
            0.2750, 0.2600, 0.2450
    };

    private DragModel() {
    }

    public static double g7(double mach) {
        if (mach <= MACH[0]) {
            return CD[0];
        }
        int last = MACH.length - 1;
        if (mach >= MACH[last]) {
            return CD[last];
        }
        int lo = 0;
        int hi = last;
        while (hi - lo > 1) {
            int mid = (lo + hi) >>> 1;
            if (MACH[mid] <= mach) {
                lo = mid;
            } else {
                hi = mid;
            }
        }
        double span = MACH[hi] - MACH[lo];
        double f = span <= 1e-9 ? 0 : (mach - MACH[lo]) / span;
        return CD[lo] + (CD[hi] - CD[lo]) * f;
    }

    /** Form factor i = SD / BC; Cd = i * G7(Mach). */
    public static double formFactor(double massGrains, double diameterInches, double bcG7) {
        double sd = (massGrains / 7000.0) / (diameterInches * diameterInches);
        return sd / Math.max(0.001, bcG7);
    }
}
