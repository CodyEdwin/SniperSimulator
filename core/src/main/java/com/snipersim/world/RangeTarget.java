package com.snipersim.world;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.math.Vector3;
import com.snipersim.util.BallisticMath;

/**
 * A range target. Gongs are round plates; silhouettes are humanoid steel. All
 * targets face the shooter, can slide along the range, and score in rings from
 * 5 to 10. Movement is analytic in mission time so a precomputed bullet path
 * can be tested against the target's position at the moment the round arrives.
 */
public final class RangeTarget {
    public enum Kind { GONG, SILHOUETTE, MOVER }

    public final int id;
    public final Kind kind;
    public final float width;
    public final float height;
    public final float depth;
    public final float baseX;
    public final float baseY;
    public final float baseZ;
    public final float moveAmplitude;
    public final float movePeriod;
    public final float movePhase;
    public final float distance;
    public final int basePoints;
    public final Color paint;

    public int hits;
    public float hitFlash;
    public float wobble;
    public float wobbleVelocity;

    private final Vector3 scratch = new Vector3();

    public RangeTarget(int id, Kind kind, float width, float height, float depth,
                       float baseX, float baseY, float baseZ,
                       float moveAmplitude, float movePeriod, float movePhase,
                       float shooterX, float shooterZ, Color paint) {
        this.id = id;
        this.kind = kind;
        this.width = width;
        this.height = height;
        this.depth = depth;
        this.baseX = baseX;
        this.baseY = baseY;
        this.baseZ = baseZ;
        this.moveAmplitude = moveAmplitude;
        this.movePeriod = movePeriod;
        this.movePhase = movePhase;
        this.paint = paint;
        this.distance = (float) Math.hypot(baseX - shooterX, baseZ - shooterZ);
        this.basePoints = 60 + Math.round(distance * 0.6f) + (kind == Kind.MOVER ? 120 : 0);
    }

    public float xAt(double time) {
        if (moveAmplitude <= 0.001f || movePeriod <= 0.001f) {
            return baseX;
        }
        return baseX + moveAmplitude
                * (float) Math.sin(Math.PI * 2.0 * time / movePeriod + movePhase);
    }

    public void centerAt(double time, Vector3 out) {
        out.set(xAt(time), baseY, baseZ);
    }

    public void boundsAt(double time, BoundingBox box) {
        float x = xAt(time);
        box.min.set(x - width * 0.5f, baseY - height * 0.5f, baseZ - depth * 0.5f);
        box.max.set(x + width * 0.5f, baseY + height * 0.5f, baseZ + depth * 0.5f);
    }

    public double distanceFrom(Vector3 origin) {
        return Math.hypot(baseX - origin.x, baseZ - origin.z);
    }

    /** -1 if outside the scoring face, otherwise the ring value 5..10. */
    public int ringAt(double time, float hitX, float hitY) {
        float dx = hitX - xAt(time);
        float dy = hitY - baseY;
        if (kind == Kind.GONG) {
            double r = Math.hypot(dx, dy) / (width * 0.5);
            if (r > 1.0) {
                return -1;
            }
            return ringForNormalized(r);
        }
        if (Math.abs(dx) > width * 0.5 || Math.abs(dy) > height * 0.5) {
            return -1;
        }
        double r = Math.sqrt(
                (dx * dx) / (width * width * 0.25) + (dy * dy) / (height * height * 0.25));
        return ringForNormalized(r);
    }

    private static int ringForNormalized(double r) {
        if (r <= 0.12) {
            return 10;
        }
        if (r <= 0.26) {
            return 9;
        }
        if (r <= 0.42) {
            return 8;
        }
        if (r <= 0.60) {
            return 7;
        }
        if (r <= 0.78) {
            return 6;
        }
        return 5;
    }

    public int pointsFor(int ring) {
        double multiplier = 0.55 + ring * 0.09;
        int points = (int) Math.round(distance * multiplier) + basePoints;
        if (kind == Kind.MOVER) {
            points = (int) Math.round(points * 1.35);
        }
        return points;
    }

    public void registerHit() {
        hits++;
        hitFlash = 1f;
        wobbleVelocity += kind == Kind.GONG ? 7.0f : 3.2f;
    }

    public void update(float delta) {
        hitFlash = Math.max(0, hitFlash - delta * 2.6f);
        wobbleVelocity += (-wobble * 42.0f - wobbleVelocity * 7.5f) * delta;
        wobble += wobbleVelocity * delta;
        wobble = BallisticMath.clamp(wobble, -0.35f, 0.35f);
    }

    public boolean isMoving() {
        return moveAmplitude > 0.001f;
    }

    public String label() {
        switch (kind) {
            case GONG:
                return "STEEL GONG";
            case SILHOUETTE:
                return "SILHOUETTE";
            case MOVER:
                return "MOVING SILHOUETTE";
            default:
                return "TARGET";
        }
    }

    public Vector3 positionAt(double time) {
        centerAt(time, scratch);
        return scratch;
    }
}
