package com.snipersim.ballistics;

import com.badlogic.gdx.math.Vector3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BallisticsEngineTest {
    private static final TerrainFunction FLAT = (x, z) -> -1000;

    private final BulletSpec spec = new BulletSpec("175 SMK", 175, 0.308, 1.24, 0.243,
            11.25, 792, 5, 0.5);
    private final Atmosphere atmosphere = Atmosphere.highDesert();

    private Trajectory fire(WindModel wind) {
        BallisticsEngine engine = new BallisticsEngine(atmosphere, wind, FLAT);
        return engine.simulate(spec, spec.muzzleVelocity,
                new Vector3(0, 0, 0), new Vector3(0, 0, -1));
    }

    @Test
    void bulletDeceleratesAndDrops() {
        Trajectory trajectory = fire(WindModel.calm());
        Vector3 atRange = new Vector3();
        Vector3 velocity = new Vector3();
        float[] tof = new float[1];
        assertTrue(trajectory.sampleAtRange(500, atRange, velocity, tof));
        assertTrue(velocity.len() < spec.muzzleVelocity);
        assertTrue(atRange.y < -1.0, "drop=" + atRange.y);
        assertTrue(atRange.y > -8.0, "drop=" + atRange.y);
        assertTrue(tof[0] > 0.5 && tof[0] < 1.2);
    }

    @Test
    void windFromWestPushesBulletEast() {
        WindModel noWind = WindModel.calm();
        WindModel westWind = new WindModel(10, 270, 0, 0, 42);
        Vector3 calmPos = new Vector3();
        Vector3 windyPos = new Vector3();
        Vector3 vel = new Vector3();
        fire(noWind).sampleAtRange(600, calmPos, vel, null);
        fire(westWind).sampleAtRange(600, windyPos, vel, null);
        assertTrue(windyPos.x > calmPos.x + 0.2,
                "calm x=" + calmPos.x + " windy x=" + windyPos.x);
    }

    @Test
    void northernHemisphereCoriolisAndSpinDriftGoRight() {
        Trajectory trajectory = fire(WindModel.calm());
        Vector3 pos = new Vector3();
        Vector3 vel = new Vector3();
        trajectory.sampleAtRange(900, pos, vel, null);
        assertTrue(pos.x > 0.02, "rightward drift=" + pos.x);
    }

    @Test
    void terrainStopsBullet() {
        BallisticsEngine engine = new BallisticsEngine(atmosphere, WindModel.calm(), (x, z) -> 0);
        Trajectory trajectory = engine.simulate(spec, spec.muzzleVelocity,
                new Vector3(0, 10, 0), new Vector3(0, 0, -1));
        assertTrue(trajectory.impactType == Trajectory.IMPACT_TERRAIN);
        assertTrue(trajectory.impactPoint.y <= 0.3);
    }

    @Test
    void fasterLowerDragBulletRetainsMoreEnergy() {
        BulletSpec lapua = new BulletSpec("300 Scenar", 300, 0.338, 1.65, 0.381,
                9.375, 869, 5, 0.4);
        BallisticsEngine engine = new BallisticsEngine(atmosphere, WindModel.calm(), FLAT);
        Trajectory slow = fire(WindModel.calm());
        Trajectory fast = engine.simulate(lapua, lapua.muzzleVelocity,
                new Vector3(0, 0, 0), new Vector3(0, 0, -1));
        Vector3 p1 = new Vector3();
        Vector3 p2 = new Vector3();
        Vector3 v1 = new Vector3();
        Vector3 v2 = new Vector3();
        slow.sampleAtRange(1000, p1, v1, null);
        fast.sampleAtRange(1000, p2, v2, null);
        assertTrue(v2.len() > v1.len());
    }
}
