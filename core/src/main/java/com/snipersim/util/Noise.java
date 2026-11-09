package com.snipersim.util;

import java.util.Random;

/** Deterministic value noise used for terrain, foliage and the sky. */
public final class Noise {
    private static final int[] PERM = new int[512];

    static {
        Random random = new Random(0x5EEDBEEFL);
        int[] p = new int[256];
        for (int i = 0; i < 256; i++) {
            p[i] = i;
        }
        for (int i = 255; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int tmp = p[i];
            p[i] = p[j];
            p[j] = tmp;
        }
        for (int i = 0; i < 512; i++) {
            PERM[i] = p[i & 255];
        }
    }

    private Noise() {
    }

    private static double fade(double t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    private static double grad(int hash, double x, double y) {
        switch (hash & 7) {
            case 0:
                return x + y;
            case 1:
                return x - y;
            case 2:
                return -x + y;
            case 3:
                return -x - y;
            case 4:
                return x;
            case 5:
                return -x;
            case 6:
                return y;
            default:
                return -y;
        }
    }

    public static double value2D(double x, double y) {
        int xi = (int) Math.floor(x);
        int yi = (int) Math.floor(y);
        double xf = x - xi;
        double yf = y - yi;
        int aa = PERM[(PERM[xi & 255] + yi) & 255];
        int ab = PERM[(PERM[xi & 255] + yi + 1) & 255];
        int ba = PERM[(PERM[(xi + 1) & 255] + yi) & 255];
        int bb = PERM[(PERM[(xi + 1) & 255] + yi + 1) & 255];
        double u = fade(xf);
        double v = fade(yf);
        double x1 = BallisticMath.lerp(grad(aa, xf, yf), grad(ba, xf - 1, yf), u);
        double x2 = BallisticMath.lerp(grad(ab, xf, yf - 1), grad(bb, xf - 1, yf - 1), u);
        return BallisticMath.lerp(x1, x2, v);
    }

    public static double fbm2D(double x, double y, int octaves, double lacunarity, double gain) {
        double sum = 0;
        double amp = 1;
        double freq = 1;
        double norm = 0;
        for (int i = 0; i < octaves; i++) {
            sum += value2D(x * freq, y * freq) * amp;
            norm += amp;
            amp *= gain;
            freq *= lacunarity;
        }
        return norm > 0 ? sum / norm : 0;
    }

    /** Ridge noise; returns 0..1 with sharp crests, good for distant mountains. */
    public static double ridge2D(double x, double y, int octaves) {
        double sum = 0;
        double amp = 1;
        double freq = 1;
        double norm = 0;
        for (int i = 0; i < octaves; i++) {
            double n = 1.0 - Math.abs(value2D(x * freq, y * freq));
            sum += n * n * amp;
            norm += amp;
            amp *= 0.5;
            freq *= 2.03;
        }
        return norm > 0 ? sum / norm : 0;
    }

    public static double value1D(double x, long seed) {
        int xi = (int) Math.floor(x);
        double f = x - xi;
        double a = hashToUnit(xi, seed);
        double b = hashToUnit(xi + 1, seed);
        double u = fade(f);
        return BallisticMath.lerp(a, b, u);
    }

    public static double hashToUnit(int value, long seed) {
        long z = value * 0x9E3779B97F4A7C15L + seed;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        z = z ^ (z >>> 31);
        return (z & 0xFFFFFF) / (double) 0xFFFFFF;
    }
}
