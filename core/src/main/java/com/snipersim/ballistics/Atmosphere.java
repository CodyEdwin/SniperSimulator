package com.snipersim.ballistics;

/**
 * Station atmosphere model. The shooter supplies altitude, station pressure,
 * temperature and relative humidity; density and speed of sound are derived
 * from the ideal gas law with a water-vapour correction.
 */
public final class Atmosphere {
    public static final double SEA_LEVEL_DENSITY = 1.225;
    public static final double GRAVITY = 9.80665;
    public static final double EARTH_ROTATION = 7.2921150e-5;
    public static final double R_DRY = 287.058;
    public static final double R_VAPOR = 461.495;
    public static final double GAMMA = 1.4;

    private final double altitudeMeters;
    private final double stationPressureHpa;
    private final double temperatureC;
    private final double humidity;
    private final double latitudeDeg;
    private final double pressurePa;
    private final double temperatureK;
    private final double density;
    private final double speedOfSound;
    private final double vaporPressurePa;
    private final double densityAltitudeMeters;
    private final double pressureAltitudeMeters;

    public Atmosphere(double altitudeMeters,
                      double stationPressureHpa,
                      double temperatureC,
                      double humidity,
                      double latitudeDeg) {
        this.altitudeMeters = altitudeMeters;
        this.stationPressureHpa = stationPressureHpa;
        this.temperatureC = temperatureC;
        this.humidity = clamp(humidity, 0, 1);
        this.latitudeDeg = latitudeDeg;
        this.pressurePa = stationPressureHpa * 100.0;
        this.temperatureK = temperatureC + 273.15;

        double saturation = 610.78 * Math.exp(17.2694 * temperatureC / (temperatureC + 237.29));
        this.vaporPressurePa = saturation * this.humidity;
        double dryPressure = Math.max(1.0, pressurePa - vaporPressurePa);
        this.density = dryPressure / (R_DRY * temperatureK) + vaporPressurePa / (R_VAPOR * temperatureK);
        this.speedOfSound = Math.sqrt(GAMMA * R_DRY * temperatureK);

        this.densityAltitudeMeters = 44330.0 * (1.0 - Math.pow(density / SEA_LEVEL_DENSITY, 1.0 / 4.2559));
        this.pressureAltitudeMeters = 44330.0 * (1.0 - Math.pow(pressurePa / 101325.0, 1.0 / 5.2559));
    }

    public static Atmosphere standard() {
        return new Atmosphere(0, 1013.25, 15.0, 0.0, 34.0);
    }

    public static Atmosphere highDesert() {
        return new Atmosphere(1850, 815.0, 24.0, 0.22, 34.5);
    }

    /** Camp Perry, Ottawa County, on the Lake Erie shore. July match weather. */
    public static Atmosphere campPerry() {
        return new Atmosphere(174, 1016.0, 26.0, 0.68, 41.54);
    }

    /** Bisley Camp, Surrey. */
    public static Atmosphere bisley() {
        return new Atmosphere(80, 1009.0, 16.0, 0.75, 51.31);
    }

    private static double clamp(double v, double a, double b) {
        return v < a ? a : (v > b ? b : v);
    }

    public double altitudeMeters() {
        return altitudeMeters;
    }

    public double stationPressureHpa() {
        return stationPressureHpa;
    }

    public double temperatureC() {
        return temperatureC;
    }

    public double humidity() {
        return humidity;
    }

    public double latitudeDeg() {
        return latitudeDeg;
    }

    public double pressurePa() {
        return pressurePa;
    }

    public double temperatureK() {
        return temperatureK;
    }

    public double density() {
        return density;
    }

    public double densityRatio() {
        return density / SEA_LEVEL_DENSITY;
    }

    public double speedOfSound() {
        return speedOfSound;
    }

    public double vaporPressurePa() {
        return vaporPressurePa;
    }

    public double densityAltitudeMeters() {
        return densityAltitudeMeters;
    }

    public double pressureAltitudeMeters() {
        return pressureAltitudeMeters;
    }

    /** Coriolis vector in the game frame: +X east, +Y up, -Z north. */
    public double coriolisX() {
        return 0;
    }

    public double coriolisY() {
        return EARTH_ROTATION * Math.sin(Math.toRadians(latitudeDeg));
    }

    public double coriolisZ() {
        return -EARTH_ROTATION * Math.cos(Math.toRadians(latitudeDeg));
    }
}
