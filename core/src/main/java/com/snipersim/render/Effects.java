package com.snipersim.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.snipersim.world.Shot;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Lightweight world-to-screen effects. Keeping these in 2D lets tracers and
 * impact dust stay crisp and cheap while still tracking 3D world positions.
 */
public final class Effects {
    private static final int MAX_PARTICLES = 700;

    private static final class Particle {
        final Vector3 position = new Vector3();
        final Vector3 velocity = new Vector3();
        float life;
        float maxLife;
        float size;
        float gravity;
        float drag;
        boolean additive;
        final Color color = new Color();
    }

    private final List<Particle> particles = new ArrayList<>();
    private final Texture dot;
    private final Texture pixel;
    private final TextureRegion pixelRegion;
    private final Vector3 projected = new Vector3();
    private final Vector3 toPoint = new Vector3();
    private final Vector3 bulletPos = new Vector3();
    private final Vector3 velocity = new Vector3();

    public Effects() {
        dot = ModelFactory.softDot();
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        pixel = new Texture(pixmap);
        pixel.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pixelRegion = new TextureRegion(pixel);
        pixmap.dispose();
    }

    public void update(float delta) {
        for (Iterator<Particle> it = particles.iterator(); it.hasNext(); ) {
            Particle p = it.next();
            p.life -= delta;
            if (p.life <= 0) {
                it.remove();
                continue;
            }
            p.velocity.y -= p.gravity * delta;
            p.velocity.scl(1f - p.drag * delta);
            p.position.mulAdd(p.velocity, delta);
        }
    }

    public void clear() {
        particles.clear();
    }

    public void spawnImpact(Vector3 point, boolean steel, float energy, float seed) {
        int count = steel ? 34 : 28;
        for (int i = 0; i < count && particles.size() < MAX_PARTICLES; i++) {
            Particle p = new Particle();
            p.position.set(point);
            double angle = MathUtils.random.nextDouble() * Math.PI * 2;
            double speed = steel ? (2.0 + MathUtils.random.nextDouble() * 9.0)
                    : (0.4 + MathUtils.random.nextDouble() * 2.2);
            p.velocity.set((float) (Math.cos(angle) * speed),
                    (float) ((steel ? 2.5 : 0.6) + MathUtils.random.nextDouble() * speed),
                    (float) (Math.sin(angle) * speed));
            p.life = steel ? 0.25f + MathUtils.random.nextFloat() * 0.55f
                    : 0.7f + MathUtils.random.nextFloat() * 1.4f;
            p.maxLife = p.life;
            p.size = steel ? 0.025f + MathUtils.random.nextFloat() * 0.06f
                    : 0.18f + MathUtils.random.nextFloat() * 0.55f;
            p.gravity = steel ? 9.8f : 0.35f;
            p.drag = steel ? 0.5f : 1.9f;
            p.additive = steel;
            if (steel) {
                p.color.set(1f, 0.55f + MathUtils.random.nextFloat() * 0.35f,
                        0.14f + MathUtils.random.nextFloat() * 0.2f, 1f);
            } else {
                p.color.set(0.62f + MathUtils.random.nextFloat() * 0.12f,
                        0.54f + MathUtils.random.nextFloat() * 0.10f,
                        0.39f + MathUtils.random.nextFloat() * 0.08f, 1f);
            }
            particles.add(p);
        }
        // A soft flash puff at the impact point.
        Particle flash = new Particle();
        flash.position.set(point);
        flash.velocity.set(0, 0.2f, 0);
        flash.life = flash.maxLife = steel ? 0.16f : 0.25f;
        flash.size = steel ? 0.55f : 0.8f;
        flash.drag = 2.8f;
        flash.additive = steel;
        flash.color.set(steel ? new Color(1f, 0.8f, 0.45f, 0.9f) : new Color(0.7f, 0.62f, 0.48f, 0.55f));
        particles.add(flash);
    }

    public void spawnMuzzleSmoke(Vector3 muzzle) {
        for (int i = 0; i < 10 && particles.size() < MAX_PARTICLES; i++) {
            Particle p = new Particle();
            p.position.set(muzzle).add(
                    MathUtils.random(-0.04f, 0.04f),
                    MathUtils.random(-0.04f, 0.04f),
                    MathUtils.random(-0.04f, 0.04f));
            p.velocity.set(MathUtils.random(-0.4f, 0.4f), 0.35f + MathUtils.random(0.5f),
                    MathUtils.random(-0.9f, 0.1f));
            p.life = 0.5f + MathUtils.random(0.6f);
            p.maxLife = p.life;
            p.size = 0.12f + MathUtils.random(0.20f);
            p.drag = 2.2f;
            p.additive = false;
            p.color.set(0.75f, 0.72f, 0.66f, 0.45f);
            particles.add(p);
        }
    }

    /**
     * Renders into the currently bound framebuffer. Call after the 3D scene and
     * before the scope composite.
     */
    public void render(SpriteBatch batch, PerspectiveCamera camera, int width, int height,
                       List<Shot> shots, Vector3 muzzle, float muzzleFlash) {
        batch.getProjectionMatrix().setToOrtho2D(0, 0, width, height);
        batch.begin();
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        for (Shot shot : shots) {
            if (shot.trailAlpha <= 0.01f) {
                continue;
            }
            drawTrail(batch, camera, shot, width, height);
        }

        for (Particle p : particles) {
            if (!project(camera, p.position, width, height)) {
                continue;
            }
            float alpha = MathUtils.clamp(p.life / p.maxLife, 0, 1);
            float size = Math.max(1.5f, pixelSize(camera, p.position, p.size, height));
            if (size < 2.5f) {
                continue;
            }
            batch.setColor(p.color.r, p.color.g, p.color.b, p.color.a * alpha);
            batch.setBlendFunction(p.additive ? GL20.GL_SRC_ALPHA : GL20.GL_SRC_ALPHA,
                    p.additive ? GL20.GL_ONE : GL20.GL_ONE_MINUS_SRC_ALPHA);
            batch.draw(dot, projected.x - size * 0.5f, projected.y - size * 0.5f, size, size);
        }

        if (muzzleFlash > 0.01f) {
            if (project(camera, muzzle, width, height)) {
                float size = 60f + 150f * muzzleFlash;
                batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
                batch.setColor(1f, 0.62f, 0.22f, 0.55f * muzzleFlash);
                batch.draw(dot, projected.x - size * 0.5f, projected.y - size * 0.5f, size, size);
                batch.setColor(1f, 0.92f, 0.7f, 0.9f * muzzleFlash);
                size *= 0.32f;
                batch.draw(dot, projected.x - size * 0.5f, projected.y - size * 0.5f, size, size);
            }
        }

        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.setColor(Color.WHITE);
        batch.end();
    }

    private void drawTrail(SpriteBatch batch, PerspectiveCamera camera, Shot shot, int width, int height) {
        float maxTime = Math.min(shot.animTime, shot.impactTime());
        int stride = Math.max(1, shot.trajectory.size() / 520);
        float alpha = shot.trailAlpha * (shot.impactTriggered ? 0.30f : 0.42f);
        if (alpha <= 0.01f) {
            return;
        }
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.setColor(0.82f, 0.86f, 0.88f, alpha * 0.55f);
        float prevX = 0;
        float prevY = 0;
        boolean hasPrev = false;
        for (int i = 0; i < shot.trajectory.size(); i += stride) {
            if (shot.trajectory.time(i) > maxTime) {
                break;
            }
            toPoint.set(shot.trajectory.x(i), shot.trajectory.y(i), shot.trajectory.z(i));
            if (!project(camera, toPoint, width, height)) {
                hasPrev = false;
                continue;
            }
            if (hasPrev) {
                drawSegment(batch, prevX, prevY, projected.x, projected.y, 1.35f);
            }
            prevX = projected.x;
            prevY = projected.y;
            hasPrev = true;
        }

        // Bright tracer head.
        shot.trajectory.getPosition(maxTime, bulletPos);
        if (project(camera, bulletPos, width, height)) {
            float size = Math.max(2f, pixelSize(camera, bulletPos, 0.16f, height));
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
            batch.setColor(1f, 0.68f, 0.26f, 0.85f * shot.trailAlpha);
            batch.draw(dot, projected.x - size, projected.y - size, size * 2, size * 2);
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        }
    }

    private void drawSegment(SpriteBatch batch, float x1, float y1, float x2, float y2, float thickness) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 0.01f || len > 500f) {
            return;
        }
        float angle = (float) (Math.atan2(dy, dx) * MathUtils.radiansToDegrees);
        batch.draw(pixelRegion, x1, y1 - thickness * 0.5f, 0, thickness * 0.5f,
                len, thickness, 1f, 1f, angle);
    }

    private boolean project(PerspectiveCamera camera, Vector3 world, int width, int height) {
        toPoint.set(world).sub(camera.position);
        if (toPoint.dot(camera.direction) <= 0.02f) {
            return false;
        }
        projected.set(world);
        camera.project(projected, 0, 0, width, height);
        return projected.x >= -80 && projected.x <= width + 80
                && projected.y >= -80 && projected.y <= height + 80;
    }

    private float pixelSize(PerspectiveCamera camera, Vector3 world, float sizeMeters, int height) {
        float distance = Math.max(0.1f, camera.position.dst(world));
        float focal = height / (2f * (float) Math.tan(Math.toRadians(camera.fieldOfView) * 0.5f));
        return sizeMeters * focal / distance;
    }

    public void dispose() {
        if (pixel != null) {
            pixel.dispose();
        }
    }

    public static void log(String message) {
        if (Gdx.app != null) {
            Gdx.app.log("Effects", message);
        }
    }
}
