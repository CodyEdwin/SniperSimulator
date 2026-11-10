package com.snipersim.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.snipersim.SniperGame;
import com.snipersim.weapons.Loadout;

/**
 * Briefing screen shown before the armory and before a range is built, so the
 * window is never a blank frame while meshes are generated.
 */
public final class LoadingScreen extends ScreenAdapter {
    public enum Job { BOOT, RANGE }

    private static final String[] BOOT_LINES = {
            "SPINNING UP FIRE CONTROL",
            "CHECKING OPTICS",
            "LOADING DOPE CARDS",
            "ARMORY IS OPEN"
    };
    private static final String[] RANGE_LINES = {
            "PLOTTING THE RANGE",
            "LAYING TARGETS",
            "READING THE WIND",
            "RANGE IS HOT"
    };

    private final SniperGame game;
    private final Job job;
    private final Loadout loadout;
    private final GameScreen existing;
    private final ShapeRenderer shapes = new ShapeRenderer();
    private final SpriteBatch batch = new SpriteBatch();
    private final BitmapFont font = new BitmapFont();
    private float time;
    private boolean started;

    public LoadingScreen(SniperGame game, Job job, Loadout loadout, GameScreen existing) {
        this.game = game;
        this.job = job;
        this.loadout = loadout;
        this.existing = existing;
        font.getData().setScale(2.1f);
    }

    @Override
    public void render(float delta) {
        time += Math.min(delta, 0.05f);
        int w = Math.max(1, Gdx.graphics.getWidth());
        int h = Math.max(1, Gdx.graphics.getHeight());
        draw(w, h);

        if (!started && time > 0.85f) {
            started = true;
            return;
        }
        if (started && time > 0.95f) {
            finish();
        }
    }

    private void finish() {
        if (job == Job.BOOT) {
            game.setScreen(new CatalogScreen(game, null, Loadout.issued()));
        } else if (existing != null && existing.course() == loadout.range) {
            existing.applyLoadout(loadout);
            game.setScreen(existing);
        } else {
            if (existing != null) {
                existing.dispose();
            }
            game.setScreen(new GameScreen(game, false, loadout));
        }
        dispose();
    }

    private void draw(int w, int h) {
        Gdx.gl.glClearColor(0.012f, 0.018f, 0.014f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        float u = h / 900f;
        shapes.setProjectionMatrix(new com.badlogic.gdx.math.Matrix4().setToOrtho2D(0, 0, w, h));
        shapes.begin(ShapeRenderer.ShapeType.Filled);

        shapes.setColor(0.05f, 0.07f, 0.05f, 0.35f);
        for (int x = 0; x < w; x += (int) (28 * u)) {
            shapes.rect(x, 0, 1f, h);
        }
        for (int y = 0; y < h; y += (int) (28 * u)) {
            shapes.rect(0, y, w, 1f);
        }

        float scan = (time * 0.35f) % 1f;
        shapes.setColor(0.55f, 0.85f, 0.35f, 0.10f);
        shapes.rect(0, h * (1f - scan), w, 18f * u);

        float m = 28f * u;
        shapes.setColor(0.96f, 0.68f, 0.28f, 1f);
        bracket(m, m, 54f * u, 5f * u, false, false);
        bracket(w - m, m, 54f * u, 5f * u, true, false);
        bracket(m, h - m, 54f * u, 5f * u, false, true);
        bracket(w - m, h - m, 54f * u, 5f * u, true, true);

        float cx = w * 0.5f;
        float cy = h * 0.52f;
        float radius = 150f * u;
        shapes.end();
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(0.45f, 0.85f, 0.40f, 0.9f);
        shapes.circle(cx, cy, radius, 64);
        shapes.circle(cx, cy, radius * 0.66f, 48);
        shapes.circle(cx, cy, radius * 0.33f, 32);
        shapes.line(cx - radius - 16f * u, cy, cx + radius + 16f * u, cy);
        shapes.line(cx, cy - radius - 16f * u, cx, cy + radius + 16f * u);
        float sweep = time * 2.4f;
        shapes.setColor(0.96f, 0.68f, 0.28f, 1f);
        shapes.line(cx, cy, cx + MathUtils.cos(sweep) * radius, cy + MathUtils.sin(sweep) * radius);
        for (int i = 0; i < 24; i++) {
            float a = i / 24f * MathUtils.PI2 + time * 0.4f;
            float inner = radius * 1.08f;
            float outer = inner + (i % 3 == 0 ? 16f * u : 8f * u);
            shapes.setColor(i % 3 == 0 ? 0.96f : 0.55f, i % 3 == 0 ? 0.68f : 0.85f, i % 3 == 0 ? 0.28f : 0.4f, 1f);
            shapes.line(cx + MathUtils.cos(a) * inner, cy + MathUtils.sin(a) * inner,
                    cx + MathUtils.cos(a) * outer, cy + MathUtils.sin(a) * outer);
        }
        shapes.end();

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        float barW = w * 0.46f;
        float barX = cx - barW * 0.5f;
        float barY = h * 0.22f;
        shapes.setColor(0.08f, 0.10f, 0.08f, 1f);
        shapes.rect(barX, barY, barW, 14f * u);
        float progress = MathUtils.clamp(time / 0.95f, 0.08f, 1f);
        shapes.setColor(0.96f, 0.55f, 0.18f, 1f);
        shapes.rect(barX, barY, barW * progress, 14f * u);
        shapes.setColor(0.55f, 0.9f, 0.4f, 0.9f);
        shapes.rect(barX, barY + 14f * u, barW * progress, 3f * u);

        float stripe = 16f * u;
        shapes.setColor(0.12f, 0.10f, 0.04f, 1f);
        shapes.rect(0, h - stripe, w, stripe);
        shapes.rect(0, 0, w, stripe);
        shapes.setColor(0.96f, 0.68f, 0.28f, 0.85f);
        for (float x = -h; x < w + h; x += 28f * u) {
            shapes.rect(x + (time * 40f * u) % (28f * u), h - stripe, 10f * u, stripe);
            shapes.rect(x - (time * 40f * u) % (28f * u), 0, 10f * u, stripe);
        }
        shapes.end();

        String[] lines = job == Job.BOOT ? BOOT_LINES : RANGE_LINES;
        String status = lines[Math.min(lines.length - 1, (int) (time / 0.22f))];
        String title = job == Job.RANGE && loadout != null ? loadout.range.title : "DAGGER RIDGE";
        batch.getProjectionMatrix().setToOrtho2D(0, 0, w, h);
        batch.begin();
        font.setColor(0.96f, 0.68f, 0.28f, 1f);
        font.draw(batch, title, 46f * u, h - 46f * u);
        font.setColor(0.75f, 0.95f, 0.62f, 0.7f + 0.3f * (float) Math.sin(time * 6f));
        font.draw(batch, status, 46f * u, h * 0.22f - 18f * u);
        font.setColor(0.62f, 0.67f, 0.58f, 1f);
        font.draw(batch, "LIVE FIRE  ·  DO NOT CROSS THE LINE", 46f * u, 42f * u);
        batch.end();
    }

    private void bracket(float x, float y, float len, float thick, boolean right, boolean up) {
        float sx = right ? -1f : 1f;
        float sy = up ? -1f : 1f;
        shapes.rect(x, y, len * sx, thick * sy);
        shapes.rect(x, y, thick * sx, len * sy);
    }

    @Override
    public void dispose() {
        shapes.dispose();
        batch.dispose();
        font.dispose();
    }
}
