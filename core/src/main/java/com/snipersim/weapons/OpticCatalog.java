package com.snipersim.weapons;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Issued optics. Mounting one copies its magnification, reticle and sight height into the build. */
public final class OpticCatalog {
    private static final List<Optic> OPTICS = Collections.unmodifiableList(Arrays.asList(
            new Optic("Leupold Mark 4 3.5-10×40", 3.5, 10, 6, Reticle.MIL_DOT, 0.055),
            new Optic("Leupold Mk 4 10×40 fixed", 10, 10, 10, Reticle.MIL_DOT, 0.045),
            new Optic("Schmidt & Bender 3-12×50", 3, 12, 6, Reticle.MIL_DOT, 0.048),
            new Optic("Schmidt & Bender PM II 5-25×56", 5, 25, 12, Reticle.CHRISTMAS_TREE, 0.055),
            new Optic("Nightforce ATACR 7-35×56", 7, 35, 15, Reticle.CHRISTMAS_TREE, 0.065),
            new Optic("Leupold Mark 4 4.5-14×50", 4.5, 14, 10, Reticle.MIL_DOT, 0.075)
    ));

    private OpticCatalog() {
    }

    public static List<Optic> all() {
        return OPTICS;
    }

    public static Optic byIndex(int index) {
        return OPTICS.get(Math.floorMod(index, OPTICS.size()));
    }
}
