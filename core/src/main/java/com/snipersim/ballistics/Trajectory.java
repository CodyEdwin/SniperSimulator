package com.snipersim.ballistics;

import com.badlogic.gdx.math.Vector3;

/**
 * Densely sampled bullet path. Positions and velocities are stored as floats;
 * the integrator itself runs in double precision. Samples are recorded at
 * 1 kHz so the visual bullet, tracer and hit tests all use the same data.
 */
public final class Trajectory {
    public static final int IMPACT_NONE = 0;
    public static final int IMPACT_TERRAIN = 1;
    public static final int IMPACT_MAX_TIME = 2;
    public static final int IMPACT_MAX_RANGE = 3;

    private static final int INITIAL_CAPACITY = 8192;

    public final float originX;
    public final float originY;
    public final float originZ;
    public final float aimX;
    public final float aimY;
    public final float aimZ;

    private float[] time = new float[INITIAL_CAPACITY];
    private float[] px = new float[INITIAL_CAPACITY];
    private float[] py = new float[INITIAL_CAPACITY];
    private float[] pz = new float[INITIAL_CAPACITY];
    private float[] vx = new float[INITIAL_CAPACITY];
    private float[] vy = new float[INITIAL_CAPACITY];
    private float[] vz = new float[INITIAL_CAPACITY];
    private int size;

    public int impactType = IMPACT_NONE;
    public float impactTime;
    public float maxHeight;
    public float maxOrdinate;
    public final Vector3 impactPoint = new Vector3();
    public final Vector3 impactVelocity = new Vector3();

    public Trajectory(Vector3 origin, Vector3 aim) {
        this.originX = origin.x;
        this.originY = origin.y;
        this.originZ = origin.z;
        this.aimX = aim.x;
        this.aimY = aim.y;
        this.aimZ = aim.z;
    }

    public void add(float t, double x, double y, double z,
                    double dx, double dy, double dz) {
        if (size == time.length) {
            grow();
        }
        time[size] = t;
        px[size] = (float) x;
        py[size] = (float) y;
        pz[size] = (float) z;
        vx[size] = (float) dx;
        vy[size] = (float) dy;
        vz[size] = (float) dz;
        size++;
    }

    private void grow() {
        int next = time.length << 1;
        time = java.util.Arrays.copyOf(time, next);
        px = java.util.Arrays.copyOf(px, next);
        py = java.util.Arrays.copyOf(py, next);
        pz = java.util.Arrays.copyOf(pz, next);
        vx = java.util.Arrays.copyOf(vx, next);
        vy = java.util.Arrays.copyOf(vy, next);
        vz = java.util.Arrays.copyOf(vz, next);
    }

    public int size() {
        return size;
    }

    public float time(int index) {
        return time[index];
    }

    public float x(int index) {
        return px[index];
    }

    public float y(int index) {
        return py[index];
    }

    public float z(int index) {
        return pz[index];
    }

    public float vx(int index) {
        return vx[index];
    }

    public float vy(int index) {
        return vy[index];
    }

    public float vz(int index) {
        return vz[index];
    }

    public float duration() {
        return size == 0 ? 0 : time[size - 1];
    }

    public void getPosition(float atTime, Vector3 out) {
        if (size == 0) {
            out.set(originX, originY, originZ);
            return;
        }
        if (atTime <= time[0]) {
            out.set(px[0], py[0], pz[0]);
            return;
        }
        if (atTime >= time[size - 1]) {
            out.set(px[size - 1], py[size - 1], pz[size - 1]);
            return;
        }
        int idx = binarySearch(atTime);
        float t0 = time[idx];
        float t1 = time[idx + 1];
        float f = t1 - t0 <= 1e-9f ? 0 : (atTime - t0) / (t1 - t0);
        out.set(px[idx] + (px[idx + 1] - px[idx]) * f,
                py[idx] + (py[idx + 1] - py[idx]) * f,
                pz[idx] + (pz[idx + 1] - pz[idx]) * f);
    }

    public void getVelocity(float atTime, Vector3 out) {
        if (size == 0) {
            out.set(0, 0, 0);
            return;
        }
        if (atTime <= time[0]) {
            out.set(vx[0], vy[0], vz[0]);
            return;
        }
        if (atTime >= time[size - 1]) {
            out.set(vx[size - 1], vy[size - 1], vz[size - 1]);
            return;
        }
        int idx = binarySearch(atTime);
        float t0 = time[idx];
        float t1 = time[idx + 1];
        float f = t1 - t0 <= 1e-9f ? 0 : (atTime - t0) / (t1 - t0);
        out.set(vx[idx] + (vx[idx + 1] - vx[idx]) * f,
                vy[idx] + (vy[idx + 1] - vy[idx]) * f,
                vz[idx] + (vz[idx + 1] - vz[idx]) * f);
    }

    private int binarySearch(float atTime) {
        int lo = 0;
        int hi = size - 1;
        while (hi - lo > 1) {
            int mid = (lo + hi) >>> 1;
            if (time[mid] <= atTime) {
                lo = mid;
            } else {
                hi = mid;
            }
        }
        return lo;
    }

    /**
     * Interpolates the path where horizontal distance from the muzzle equals
     * {@code range}. Used for DOPE tables and the zero solver. Returns false
     * if the bullet never reaches that distance.
     */
    public boolean sampleAtRange(double range, Vector3 outPosition, Vector3 outVelocity, float[] outTime) {
        for (int i = 1; i < size; i++) {
            double d0 = horizontalDistance(i - 1);
            double d1 = horizontalDistance(i);
            if (d1 >= range && d0 <= range) {
                double span = d1 - d0;
                double f = span <= 1e-9 ? 0 : (range - d0) / span;
                outPosition.set(
                        (float) (px[i - 1] + (px[i] - px[i - 1]) * f),
                        (float) (py[i - 1] + (py[i] - py[i - 1]) * f),
                        (float) (pz[i - 1] + (pz[i] - pz[i - 1]) * f));
                outVelocity.set(
                        (float) (vx[i - 1] + (vx[i] - vx[i - 1]) * f),
                        (float) (vy[i - 1] + (vy[i] - vy[i - 1]) * f),
                        (float) (vz[i - 1] + (vz[i] - vz[i - 1]) * f));
                if (outTime != null) {
                    outTime[0] = (float) (time[i - 1] + (time[i] - time[i - 1]) * f);
                }
                return true;
            }
        }
        return false;
    }

    private double horizontalDistance(int index) {
        double dx = px[index] - originX;
        double dz = pz[index] - originZ;
        return Math.hypot(dx, dz);
    }

    /** Charge strength proxy used to colour the tracer by speed. */
    public float speedAt(int index) {
        return (float) Math.sqrt(vx[index] * vx[index] + vy[index] * vy[index] + vz[index] * vz[index]);
    }
}
