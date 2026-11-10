package com.snipersim.world;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.math.Vector3;
import com.snipersim.ballistics.Atmosphere;
import com.snipersim.ballistics.WindModel;
import com.snipersim.render.TargetVisual;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** The complete range: firing ridge, target line, flags, foliage and weather. */
public final class World {
    public final Terrain terrain;
    public final Atmosphere atmosphere;
    public WindModel wind;
    public final List<RangeTarget> targets = new ArrayList<>();
    public final List<TargetVisual> targetVisuals = new ArrayList<>();
    public final List<WindFlag> flags = new ArrayList<>();
    public final Vector3 shooterEye = new Vector3();
    public final ModelInstance terrainInstance;
    public final ModelInstance mountainInstance;

    private final List<ModelInstance> scenery = new ArrayList<>();
    private final List<Model> ownedModels = new ArrayList<>();
    private final RandomXS128 random = new RandomXS128(0xD46E4B01L);
    public final RangeCourse course;
    public float missionTime;

    public World() {
        this(RangeCourse.DAGGER);
    }

    public World(RangeCourse course) {
        this.course = course == null ? RangeCourse.DAGGER : course;
        terrain = new Terrain(this.course);
        if (this.course == RangeCourse.CAMP_PERRY) {
            atmosphere = Atmosphere.campPerry();
            wind = new WindModel(5.2, 75, 2.4, 11, 0x0C0A7L);
        } else if (this.course == RangeCourse.BISLEY) {
            atmosphere = Atmosphere.bisley();
            wind = new WindModel(4.1, 230, 1.5, 14, 0xB151EEL);
        } else {
            atmosphere = Atmosphere.highDesert();
            wind = new WindModel(6.4, 292, 2.3, 13, 0x51A7L);
        }

        shooterEye.set(0, terrain.shooterGround() + 1.12f, 0);
        terrainInstance = new ModelInstance(terrain.terrainModel());
        mountainInstance = new ModelInstance(terrain.mountainModel());

        buildTargets();
        buildFlags();
        buildScenery();
        buildSandbags();
    }

    private void buildTargets() {
        if (course == RangeCourse.CAMP_PERRY) {
            buildPerryTargets();
            return;
        }
        if (course == RangeCourse.BISLEY) {
            buildYardGongs(new double[] {300, 500, 600, 900, 1000, 1200},
                    new float[] {0.55f, 0.62f, 0.70f, 0.85f, 0.95f, 1.10f});
            return;
        }
        // Centered on the firing line (x = 0) so every plate sits in the lane
        // the shooter is looking down. Distances stay staggered. Movers swing
        // a few metres either side of that centerline and still read as the
        // middle of the range.
        float[][] layout = {
                // range, x offset, kind (0=gong,1=sil,2=mover), width, height, move amp, period
                {200, 0, 1, 0.55f, 1.72f, 0, 0},
                {320, 0, 0, 0.58f, 0.58f, 0, 0},
                {450, 0, 0, 0.42f, 0.42f, 0, 0},
                {600, 0, 2, 0.55f, 1.72f, 6f, 17},
                {760, 0, 0, 0.48f, 0.48f, 0, 0},
                {930, 0, 1, 0.58f, 1.78f, 0, 0},
                {1100, 0, 0, 0.46f, 0.46f, 0, 0},
                {1280, 0, 2, 0.58f, 1.78f, 8f, 22},
                {1540, 0, 0, 0.58f, 0.58f, 0, 0},
                {1780, 0, 1, 0.62f, 1.84f, 0, 0},
                {2000, 0, 0, 0.74f, 0.74f, 0, 0}
        };

        int id = 0;
        for (float[] row : layout) {
            float range = row[0];
            float offset = row[1];
            RangeTarget.Kind kind = row[2] == 0 ? RangeTarget.Kind.GONG
                    : (row[2] == 1 ? RangeTarget.Kind.SILHOUETTE : RangeTarget.Kind.MOVER);
            float width = row[3];
            float height = row[4];
            float move = row[5];
            float period = row[6];
            float ground = terrain.height(offset, -range);
            float centerY = ground + (kind == RangeTarget.Kind.GONG ? 1.45f : 1.12f) + height * 0.5f;
            Color paint = kind == RangeTarget.Kind.GONG
                    ? new Color(0.82f, 0.36f, 0.13f, 1f)
                    : new Color(0.60f, 0.30f, 0.14f, 1f);
            RangeTarget target = new RangeTarget(id++, kind, width, height, 0.075f,
                    offset, centerY, -range, move, period,
                    range * 0.037f, shooterEye.x, shooterEye.z, paint);
            targets.add(target);
            targetVisuals.add(new TargetVisual(target, ground));
        }
        targets.sort(Comparator.comparingDouble(t -> t.distance));
        targetVisuals.sort(Comparator.comparingDouble(v -> v.target().distance));
    }

    private void buildPerryTargets() {
        float[] yards = {200f, 300f, 600f};
        float[] fiveRing = {0.940f, 1.410f, 2.819f};
        float[] banks = {-7f, -3.5f, 0f, 3.5f, 7f};
        int id = 0;
        for (int i = 0; i < yards.length; i++) {
            float z = -RangeCourse.yards(yards[i]);
            for (float x : banks) {
                float ground = terrain.height(x, z);
                float size = fiveRing[i];
                float centerY = ground + 0.35f + size * 0.5f;
                RangeTarget target = new RangeTarget(id++, RangeTarget.Kind.GONG, size, size, 0.04f,
                        x, centerY, z, 0, 0, 0, shooterEye.x, shooterEye.z,
                        new Color(0.96f, 0.96f, 0.94f, 1f));
                if (Math.abs(x) < 0.1f) {
                    target.name = String.format(java.util.Locale.ROOT, "%.0f YD", yards[i]);
                } else {
                    target.name = "FRAME";
                }
                targets.add(target);
                targetVisuals.add(new TargetVisual(target, ground));
            }
        }
    }

    private void buildYardGongs(double[] yards, float[] diameters) {
        int id = 0;
        for (int i = 0; i < yards.length; i++) {
            float z = -RangeCourse.yards(yards[i]);
            float size = diameters[i];
            float ground = terrain.height(0, z);
            float centerY = ground + 1.2f + size * 0.5f;
            RangeTarget target = new RangeTarget(id++, RangeTarget.Kind.GONG, size, size, 0.06f,
                    0, centerY, z, 0, 0, 0, shooterEye.x, shooterEye.z,
                    new Color(0.82f, 0.36f, 0.13f, 1f));
            target.name = String.format(java.util.Locale.ROOT, "%.0f YD", yards[i]);
            targets.add(target);
            targetVisuals.add(new TargetVisual(target, ground));
        }
    }

    private void buildFlags() {
        if (course == RangeCourse.CAMP_PERRY) {
            for (int yards = 100; yards <= 600; yards += 100) {
                float z = -RangeCourse.yards(yards);
                float side = yards % 200 == 0 ? 12f : -12f;
                float ground = terrain.height(side, z);
                flags.add(new WindFlag(yards, side, ground, z, 3.2f,
                        new Color(0.96f, 0.82f, 0.20f, 1f)));
            }
            return;
        }
        int far = course == RangeCourse.BISLEY ? 1100 : 1900;
        for (int range = 100; range <= far; range += 200) {
            float side = ((range / 200) % 2 == 0) ? 19f : -19f;
            float ground = terrain.height(side, -range);
            Color color = (range / 200) % 2 == 0
                    ? new Color(0.96f, 0.55f, 0.12f, 1f)
                    : new Color(0.85f, 0.20f, 0.13f, 1f);
            flags.add(new WindFlag(range, side, ground, -range, 4.4f, color));
        }
    }

    private void buildScenery() {
        Model trunk = ModelFactoryHolder.box(0.18f, 2.6f, 0.18f, new Color(0.25f, 0.18f, 0.11f, 1f));
        Model coneA = ModelFactoryHolder.cone(1.35f, 3.4f, new Color(0.16f, 0.27f, 0.14f, 1f));
        Model coneB = ModelFactoryHolder.cone(0.95f, 2.8f, new Color(0.20f, 0.31f, 0.16f, 1f));
        Model rock = ModelFactoryHolder.sphere(0.65f, new Color(0.38f, 0.37f, 0.34f, 1f));
        ownedModels.add(trunk);
        ownedModels.add(coneA);
        ownedModels.add(coneB);
        ownedModels.add(rock);

        int trees = course == RangeCourse.CAMP_PERRY ? 18 : 88;
        for (int i = 0; i < trees; i++) {
            float side = random.nextBoolean() ? 1 : -1;
            float x = side * (course == RangeCourse.CAMP_PERRY ? 30 + random.nextFloat() * 80
                    : 220 + random.nextFloat() * 1250);
            float z = course == RangeCourse.CAMP_PERRY
                    ? 25 + random.nextFloat() * 140
                    : -80 - random.nextFloat() * 2780;
            float ground = terrain.height(x, z);
            float scale = 0.65f + random.nextFloat() * 1.15f;
            ModelInstance trunkInstance = new ModelInstance(trunk);
            trunkInstance.transform.idt().translate(x, ground + 1.3f * scale, z).scale(scale, scale, scale);
            scenery.add(trunkInstance);
            ModelInstance coneInstance = new ModelInstance(coneA);
            coneInstance.transform.idt().translate(x, ground + (2.55f + 1.7f) * scale, z)
                    .rotate(0, 1, 0, random.nextFloat() * 360f).scale(scale, scale, scale);
            scenery.add(coneInstance);
            ModelInstance topInstance = new ModelInstance(coneB);
            topInstance.transform.idt().translate(x, ground + (4.5f + 1.4f) * scale, z)
                    .rotate(0, 1, 0, random.nextFloat() * 360f).scale(scale, scale, scale);
            scenery.add(topInstance);
        }

        if (course == RangeCourse.CAMP_PERRY) {
            return;
        }
        for (int i = 0; i < 46; i++) {
            float side = random.nextBoolean() ? 1 : -1;
            float x = side * (120 + random.nextFloat() * 1350);
            float z = -30 - random.nextFloat() * 2900;
            float ground = terrain.height(x, z);
            float scale = 0.4f + random.nextFloat() * 1.8f;
            ModelInstance instance = new ModelInstance(rock);
            instance.transform.idt().translate(x, ground + 0.15f * scale, z)
                    .rotate(0, 1, 0, random.nextFloat() * 360f)
                    .scale(scale, scale * (0.55f + random.nextFloat() * 0.35f), scale);
            scenery.add(instance);
        }
    }

    private void buildSandbags() {
        Model bag = ModelFactoryHolder.box(0.72f, 0.26f, 0.34f, new Color(0.42f, 0.37f, 0.27f, 1f));
        ownedModels.add(bag);
        float base = terrain.shooterGround();
        float[] xs = {-1.15f, -0.38f, 0.38f, 1.15f};
        for (int i = 0; i < xs.length; i++) {
            ModelInstance instance = new ModelInstance(bag);
            instance.transform.idt().translate(xs[i], base + 0.14f, -2.15f)
                    .rotate(0, 1, 0, -8 + i * 5);
            scenery.add(instance);
        }
        for (int i = 0; i < 3; i++) {
            ModelInstance instance = new ModelInstance(bag);
            instance.transform.idt().translate(-0.75f + i * 0.75f, base + 0.40f, -2.15f)
                    .rotate(0, 1, 0, 3 - i * 4);
            scenery.add(instance);
        }
    }

    public void update(float delta) {
        missionTime += delta;
        for (RangeTarget target : targets) {
            target.update(delta);
        }
        for (TargetVisual visual : targetVisuals) {
            visual.update(missionTime);
        }
    }

    public void rerollWind() {
        long seed = MathUtils.random.nextLong();
        double speed = 3.0 + MathUtils.random.nextDouble() * 6.0;
        double from = 240.0 + MathUtils.random.nextDouble() * 90.0;
        double gust = 1.0 + MathUtils.random.nextDouble() * 2.6;
        wind = new WindModel(speed, from, gust, 8.0 + MathUtils.random.nextDouble() * 16.0, seed);
    }

    public void renderScenery(com.badlogic.gdx.graphics.g3d.ModelBatch batch,
                              com.badlogic.gdx.graphics.g3d.Environment environment) {
        for (ModelInstance instance : scenery) {
            batch.render(instance, environment);
        }
    }

    public void dispose() {
        for (TargetVisual visual : targetVisuals) {
            visual.dispose();
        }
        for (Model model : ownedModels) {
            model.dispose();
        }
        ownedModels.clear();
        scenery.clear();
        terrain.dispose();
    }

    /** Small indirection so the world does not need to import render helpers directly at field init. */
    private static final class ModelFactoryHolder {
        static Model box(float w, float h, float d, Color color) {
            return com.snipersim.render.ModelFactory.box(w, h, d, color);
        }

        static Model cone(float r, float h, Color color) {
            return com.snipersim.render.ModelFactory.cone(r, h, color);
        }

        static Model sphere(float r, Color color) {
            return com.snipersim.render.ModelFactory.sphere(r, color);
        }
    }

    public List<WindFlag> flags() {
        return Collections.unmodifiableList(flags);
    }
}
