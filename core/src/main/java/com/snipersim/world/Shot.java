package com.snipersim.world;

import com.badlogic.gdx.math.Vector3;
import com.snipersim.ballistics.Trajectory;
import com.snipersim.weapons.Weapon;

/** One fired round: its simulated path, arrival data and scoring result. */
public final class Shot {
    public final Weapon weapon;
    public final Trajectory trajectory;
    public final float fireMissionTime;
    public final double muzzleVelocity;
    public final RangeTarget target;
    public final int ring;
    public final float hitTime;
    public final Vector3 impactPoint = new Vector3();
    public final Vector3 impactVelocity = new Vector3();
    public final Vector3 muzzle = new Vector3();
    public final int points;
    public final float distance;
    public final float impactMissionTime;
    public final int ammoAfter;

    public float animTime;
    public boolean impactTriggered;
    public float trailAlpha = 1f;
    public float heat;
    public boolean followed;
    /** Stage index when the shot was fired, or -1 outside a match. */
    public int matchStage = -1;

    public Shot(Weapon weapon, Trajectory trajectory, float fireMissionTime,
                double muzzleVelocity, RangeTarget target, int ring, float hitTime,
                Vector3 impactPoint, Vector3 impactVelocity,
                int points, float distance, int ammoAfter) {
        this.weapon = weapon;
        this.trajectory = trajectory;
        this.fireMissionTime = fireMissionTime;
        this.muzzleVelocity = muzzleVelocity;
        this.target = target;
        this.ring = ring;
        this.hitTime = hitTime;
        this.impactPoint.set(impactPoint);
        this.impactVelocity.set(impactVelocity);
        this.points = points;
        this.distance = distance;
        this.impactMissionTime = fireMissionTime + hitTime;
        this.ammoAfter = ammoAfter;
    }

    public float impactTime() {
        return hitTime;
    }

    public boolean isHit() {
        return target != null;
    }

    public void update(float delta) {
        if (impactTriggered) {
            trailAlpha = Math.max(0, trailAlpha - delta * 0.22f);
        }
        if (heat > 0) {
            heat = Math.max(0, heat - delta * 2.4f);
        }
    }
}
