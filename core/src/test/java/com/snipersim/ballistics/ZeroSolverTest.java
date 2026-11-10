package com.snipersim.ballistics;

import com.badlogic.gdx.math.Vector3;
import com.snipersim.weapons.Weapon;
import com.snipersim.weapons.WeaponCatalog;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZeroSolverTest {
    private static final TerrainFunction FLAT = (x, z) -> -1000;

    @Test
    void zeroSolutionCrossesLineOfSightAtZeroDistance() {
        BulletSpec spec = new BulletSpec("175 SMK", 175, 0.308, 1.24, 0.243,
                11.25, 792, 5, 0.5);
        Atmosphere atmosphere = Atmosphere.highDesert();
        double sightHeight = 0.062;
        Vector3 scope = new Vector3(0, 0, 0);
        Vector3 level = new Vector3(0, 0, -1);
        double angle = ZeroSolver.solveZeroAngle(spec, atmosphere, WindModel.calm(), FLAT,
                scope, level, 100, sightHeight);

        Vector3 origin = new Vector3(0, (float) -sightHeight, 0);
        Vector3 dir = new Vector3(0, (float) Math.sin(angle), (float) -Math.cos(angle));
        BallisticsEngine engine = new BallisticsEngine(atmosphere, WindModel.calm(), FLAT);
        Trajectory trajectory = engine.simulate(spec, spec.muzzleVelocity, origin, dir);
        Vector3 pos = new Vector3();
        Vector3 vel = new Vector3();
        assertTrue(trajectory.sampleAtRange(100, pos, vel, null));
        assertEquals(-sightHeight, pos.y, 0.005);
    }

    @Test
    void dopeTableIsMonotonicAndComplete() {
        Weapon weapon = WeaponCatalog.defaultWeapon();
        Atmosphere atmosphere = Atmosphere.highDesert();
        BallisticTable table = new BallisticTable(weapon.bullet, atmosphere, WindModel.calm(),
                FLAT, new Vector3(0, 0, 0), 100, weapon.sightHeightMeters, weapon.maxRangeMeters);
        assertFalse(table.rows.isEmpty());
        assertTrue(table.rows.size() >= 10);
        int previousClicks = Integer.MIN_VALUE;
        for (BallisticTable.Row row : table.rows) {
            assertTrue(row.dropMeters <= 0.02);
            assertTrue(row.tof > 0);
            assertTrue(row.velocity < weapon.bullet.muzzleVelocity);
            assertTrue(row.elevationClicks >= previousClicks);
            previousClicks = row.elevationClicks;
        }
        assertTrue(table.rows.get(0).elevationClicks == 0);
        assertTrue(table.rows.get(table.rows.size() - 1).elevationClicks > 20);
    }

    @Test
    void everyChamberingProducesADopeTable() {
        Atmosphere atmosphere = Atmosphere.highDesert();
        for (Weapon weapon : WeaponCatalog.all()) {
            BallisticTable table = new BallisticTable(weapon.bullet, atmosphere, WindModel.calm(),
                    FLAT, new Vector3(0, 30, 0), 100, weapon.sightHeightMeters,
                    weapon.maxRangeMeters);
            assertFalse(table.rows.isEmpty(), weapon.caliber);
            assertTrue(table.rows.get(0).velocity < weapon.bullet.muzzleVelocity, weapon.caliber);
        }
    }

    @Test
    void serviceRifleDropsMoreThanCheyTac() {
        Weapon service = WeaponCatalog.all().get(0);
        Weapon cheytac = WeaponCatalog.all().get(3);
        BallisticsEngine serviceEngine = new BallisticsEngine(Atmosphere.highDesert(), WindModel.calm(), FLAT);
        BallisticsEngine cheyEngine = new BallisticsEngine(Atmosphere.highDesert(), WindModel.calm(), FLAT);
        Trajectory servicePath = serviceEngine.simulate(service.bullet, service.bullet.muzzleVelocity,
                new Vector3(0, 0, 0), new Vector3(0, 0, -1));
        Trajectory cheyPath = cheyEngine.simulate(cheytac.bullet, cheytac.bullet.muzzleVelocity,
                new Vector3(0, 0, 0), new Vector3(0, 0, -1));
        Vector3 p1 = new Vector3();
        Vector3 p2 = new Vector3();
        Vector3 v = new Vector3();
        assertTrue(servicePath.sampleAtRange(800, p1, v, null));
        assertTrue(cheyPath.sampleAtRange(800, p2, v, null));
        assertTrue(p1.y < p2.y - 0.5);
    }
}
