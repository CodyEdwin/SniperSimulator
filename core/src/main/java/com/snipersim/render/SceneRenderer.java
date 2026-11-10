package com.snipersim.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.snipersim.util.BallisticMath;
import com.snipersim.world.Shot;
import com.snipersim.world.WindFlag;
import com.snipersim.world.World;

import java.util.List;

/** Draws the entire 3D range and the 2D traces/particles layered over it. */
public final class SceneRenderer {
    private final ModelBatch modelBatch;
    private final Environment environment;
    private final SpriteBatch overlay;
    private final ShapeRenderer shapes;
    private final RifleViewModel rifle;
    private final Effects effects;
    private final Color topSky = new Color(0.10f, 0.22f, 0.38f, 1f);
    private final Color midSky = new Color(0.31f, 0.48f, 0.62f, 1f);
    private final Color horizonSky = new Color(0.83f, 0.72f, 0.53f, 1f);
    private final Color skyBottom = new Color(0.55f, 0.56f, 0.49f, 1f);
    private final Vector3 sunDirection = new Vector3(-0.48f, 0.50f, -0.42f).nor();
    private final Vector3 sunProjected = new Vector3();
    private final Vector3 sunWorld = new Vector3();
    private final Vector3 flagTip = new Vector3();
    private final Vector3 flagTop = new Vector3();
    private final Vector3 flagBase = new Vector3();
    private final Vector3 flagTopScreen = new Vector3();
    private final Vector3 flagMidScreen = new Vector3();
    private final Vector3 flagTipScreen = new Vector3();
    private final Vector3 flagRel = new Vector3();

    public SceneRenderer(Color weaponAccent) {
        modelBatch = new ModelBatch();
        environment = new Environment();
        environment.set(ColorAttribute.createAmbientLight(new Color(0.50f, 0.52f, 0.47f, 1f)));
        environment.add(new DirectionalLight().set(
                new Color(1.0f, 0.86f, 0.64f, 1f),
                -sunDirection.x, -sunDirection.y, -sunDirection.z));
        overlay = new SpriteBatch();
        shapes = new ShapeRenderer();
        rifle = new RifleViewModel(weaponAccent);
        effects = new Effects();
    }

    public void renderSky(PerspectiveCamera camera, int width, int height) {
        float halfFovTan = (float) Math.tan(Math.toRadians(camera.fieldOfView) * 0.5);
        float pitch = (float) Math.asin(BallisticMath.clamp(camera.direction.y, -1, 1));
        float horizon = height * (0.48f - (pitch / Math.max(0.01f, halfFovTan)) * 0.22f);
        horizon = BallisticMath.clamp(horizon, height * 0.10f, height * 0.92f);

        shapes.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0, width, height));
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        int bands = 40;
        for (int i = 0; i < bands; i++) {
            float f0 = i / (float) bands;
            float f1 = (i + 1) / (float) bands;
            Color c0 = skyColor(f0);
            Color c1 = skyColor(f1);
            float y0 = horizon + (height - horizon) * f0;
            float y1 = horizon + (height - horizon) * f1;
            shapes.rect(0, y0, width, y1 - y0 + 1, c0, c0, c1, c1);
        }
        // Below the horizon: warm dust haze that terrain will mostly cover.
        shapes.rect(0, 0, width, horizon + 1,
                skyBottom, skyBottom, horizonSky, horizonSky);
        shapes.end();

        sunWorld.set(camera.position).mulAdd(sunDirection, 4000f);
        Vector3 rel = new Vector3(sunWorld).sub(camera.position);
        if (rel.dot(camera.direction) > 0) {
            sunProjected.set(sunWorld);
            camera.project(sunProjected, 0, 0, width, height);
            float focal = height / (2f * halfFovTan);
            float core = Math.min(180f, 0.022f * focal);
            if (core > 1.5f) {
                overlay.getProjectionMatrix().setToOrtho2D(0, 0, width, height);
                overlay.begin();
                overlay.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
                overlay.setColor(1f, 0.82f, 0.45f, 0.10f);
                overlay.draw(ModelFactory.softDot(),
                        sunProjected.x - core * 5f, sunProjected.y - core * 5f, core * 10f, core * 10f);
                overlay.setColor(1f, 0.88f, 0.60f, 0.35f);
                overlay.draw(ModelFactory.softDot(),
                        sunProjected.x - core * 2.2f, sunProjected.y - core * 2.2f, core * 4.4f, core * 4.4f);
                overlay.setColor(1f, 0.96f, 0.82f, 0.95f);
                overlay.draw(ModelFactory.softDot(),
                        sunProjected.x - core, sunProjected.y - core, core * 2f, core * 2f);
                overlay.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
                overlay.setColor(Color.WHITE);
                overlay.end();
            }
        }
    }

    private Color skyColor(float f) {
        // f=0 at horizon, 1 at zenith.
        Color color = new Color(horizonSky).lerp(midSky, MathUtils.clamp(f * 1.8f, 0, 1));
        color.lerp(topSky, MathUtils.clamp((f - 0.42f) / 0.58f, 0, 1));
        return color;
    }

    public void renderWorld(Camera camera, World world, boolean showRifle) {
        modelBatch.begin(camera);
        modelBatch.render(world.terrainInstance, environment);
        modelBatch.render(world.mountainInstance, environment);
        world.renderScenery(modelBatch, environment);
        for (TargetVisual visual : world.targetVisuals) {
            visual.render(modelBatch, environment);
        }
        rifle.setVisible(showRifle);
        rifle.update(camera);
        rifle.render(modelBatch, environment);
        modelBatch.end();

        renderFlags(camera, world);
    }

    public void renderEffects(PerspectiveCamera camera, int width, int height,
                              List<Shot> shots, float muzzleFlash) {
        effects.render(overlay, camera, width, height, shots, rifle.muzzleWorld(), muzzleFlash);
    }

    public void update(float delta) {
        effects.update(delta);
    }

    public void spawnImpact(Vector3 point, boolean steel, float energy) {
        effects.spawnImpact(point, steel, energy, 0f);
    }

    public void spawnMuzzleSmoke() {
        effects.spawnMuzzleSmoke(rifle.muzzleWorld());
    }

    public void syncRifle(com.badlogic.gdx.graphics.Camera camera) {
        rifle.update(camera);
    }

    public Vector3 muzzleWorld() {
        return rifle.muzzleWorld();
    }

    public void clearEffects() {
        effects.clear();
    }

    private void renderFlags(Camera camera, World world) {
        int width = Gdx.graphics.getWidth();
        int height = Gdx.graphics.getHeight();
        shapes.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0, width, height));
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (WindFlag flag : world.flags) {
            flag.top(flagTop);
            flag.clothTip(world.wind, world.missionTime, flagTip);
            flagBase.set(flagTop.x, flagTop.y - flag.poleHeight, flagTop.z);
            Vector3 topMid = new Vector3(flagTop.x, flagTop.y - 0.16f, flagTop.z);
            Vector3 lowerMid = new Vector3(flagTop.x, flagTop.y - 0.58f, flagTop.z);
            if (!projectScreen(camera, flagTop, flagTopScreen, width, height)
                    || !projectScreen(camera, flagBase, flagBase, width, height)
                    || !projectScreen(camera, topMid, flagMidScreen, width, height)
                    || !projectScreen(camera, lowerMid, lowerMid, width, height)
                    || !projectScreen(camera, flagTip, flagTipScreen, width, height)) {
                continue;
            }
            float distance = camera.position.dst(flagTop);
            float poleWidth = MathUtils.clamp(900f / Math.max(20f, distance), 1.2f, 4f);
            shapes.setColor(0.24f, 0.22f, 0.18f, 0.95f);
            shapes.rectLine(flagTopScreen.x, flagTopScreen.y, flagBase.x, flagBase.y, poleWidth);
            Color cloth = flag.clothColor;
            shapes.setColor(cloth.r, cloth.g, cloth.b, 0.95f);
            shapes.triangle(flagMidScreen.x, flagMidScreen.y,
                    lowerMid.x, lowerMid.y, flagTipScreen.x, flagTipScreen.y);
            shapes.setColor(cloth.r * 0.72f, cloth.g * 0.72f, cloth.b * 0.72f, 0.95f);
            shapes.triangle(flagMidScreen.x, flagMidScreen.y,
                    flagTipScreen.x, flagTipScreen.y, flagTipScreen.x, flagTipScreen.y - 8f);
        }
        shapes.end();
    }

    private boolean projectScreen(Camera camera, Vector3 world, Vector3 out, int width, int height) {
        flagRel.set(world).sub(camera.position);
        if (flagRel.dot(camera.direction) <= 0.01f) {
            return false;
        }
        out.set(world);
        camera.project(out, 0, 0, width, height);
        return out.x > -100 && out.x < width + 100 && out.y > -100 && out.y < height + 100;
    }

    public Vector3 muzzlePosition() {
        return rifle.muzzleWorld();
    }

    public void dispose() {
        modelBatch.dispose();
        overlay.dispose();
        shapes.dispose();
        rifle.dispose();
        effects.dispose();
    }

    public static void log(String message) {
        if (Gdx.app != null) {
            Gdx.app.log("Scene", message);
        }
    }
}
