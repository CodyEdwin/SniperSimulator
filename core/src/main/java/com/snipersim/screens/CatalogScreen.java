package com.snipersim.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.snipersim.SniperGame;
import com.snipersim.weapons.Loadout;
import com.snipersim.weapons.Optic;
import com.snipersim.weapons.OpticCatalog;
import com.snipersim.weapons.Reticle;
import com.snipersim.weapons.Weapon;
import com.snipersim.weapons.WeaponCatalog;
import com.snipersim.world.RangeCourse;

import java.util.Locale;

/**
 * Separate armory screen. Pick a rifle, mount a scope, then change magnification,
 * reticle, sight height and zero before deploying.
 */
public final class CatalogScreen extends ScreenAdapter {
    private static final float RIFLE_ROW = 56f;
    private static final float OPTIC_ROW = 36f;
    private static final float TUNE_ROW = 38f;
    private static final int TUNE_ROWS = 6;
    private static final String[] TUNE_LABELS = {
            "ZOOM MIN", "ZOOM MAX", "ZOOM USED", "RETICLE", "SIGHT HEIGHT", "ZERO"
    };

    private final SniperGame game;
    private final GameScreen range;
    private final Loadout loadout;
    private final SpriteBatch batch;
    private final BitmapFont font;
    private final BitmapFont bold;
    private final Texture pixel;
    private final FreeTypeFontGenerator regularGenerator;
    private final FreeTypeFontGenerator boldGenerator;

    private int column;
    private int tuneRow;
    private int width = 1;
    private int height = 1;

    private final InputAdapter input = new InputAdapter() {
        @Override
        public boolean touchDown(int screenX, int screenY, int pointer, int button) {
            hit(screenX, screenY);
            return true;
        }

        @Override
        public boolean keyDown(int keycode) {
            if (keycode == Input.Keys.ESCAPE) {
                back();
                return true;
            }
            if (keycode == Input.Keys.ENTER) {
                deploy();
                return true;
            }
            if (keycode == Input.Keys.TAB) {
                column = (column + 1) % 3;
                return true;
            }
            if (keycode == Input.Keys.UP) {
                move(-1);
                return true;
            }
            if (keycode == Input.Keys.DOWN) {
                move(1);
                return true;
            }
            if (keycode == Input.Keys.LEFT) {
                adjust(-1);
                return true;
            }
            if (keycode == Input.Keys.RIGHT) {
                adjust(1);
                return true;
            }
            if (keycode >= Input.Keys.NUM_1 && keycode <= Input.Keys.NUM_7) {
                loadout.rifleIndex = keycode - Input.Keys.NUM_1;
                column = 0;
                return true;
            }
            return false;
        }
    };

    public CatalogScreen(SniperGame game, GameScreen range, Loadout loadout) {
        this.game = game;
        this.range = range;
        this.loadout = loadout;
        batch = new SpriteBatch();
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        pixel = new Texture(pixmap);
        pixmap.dispose();
        FreeTypeFontGenerator regular = null;
        FreeTypeFontGenerator boldGen = null;
        BitmapFont regularFont = null;
        BitmapFont boldFont = null;
        try {
            regular = new FreeTypeFontGenerator(Gdx.files.internal("fonts/JetBrainsMonoNerdFont-Regular.ttf"));
            boldGen = new FreeTypeFontGenerator(Gdx.files.internal("fonts/JetBrainsMonoNerdFont-Bold.ttf"));
            regularFont = generate(regular);
            boldFont = generate(boldGen);
        } catch (Exception e) {
            Gdx.app.error("Catalog", "Font unavailable", e);
        }
        regularGenerator = regular;
        boldGenerator = boldGen;
        font = regularFont != null ? regularFont : new BitmapFont();
        bold = boldFont != null ? boldFont : font;
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(input);
        Gdx.input.setCursorCatched(false);
    }

    @Override
    public void render(float delta) {
        width = Math.max(1, Gdx.graphics.getWidth());
        height = Math.max(1, Gdx.graphics.getHeight());
        Gdx.gl.glClearColor(0.012f, 0.016f, 0.014f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
        batch.getProjectionMatrix().setToOrtho2D(0, 0, width, height);
        batch.begin();
        draw();
        batch.end();
    }

    private void draw() {
        float u = height / 900f;
        float m = 22f * u;
        text("ARMORY", m, height - 16f * u, 40f * u, true, new Color(0.96f, 0.68f, 0.28f, 1f));
        text("CLICK A RIFLE, A SCOPE, THEN − OR +    ·    CLICK DEPLOY",
                m, height - 62f * u, 18f * u, false, new Color(0.62f, 0.67f, 0.58f, 1f));
        float chip = 0f;
        for (RangeCourse course : RangeCourse.values()) {
            boolean on = course == loadout.range;
            panel(m + chip, 16f * u, 250f * u, 56f * u,
                    on ? new Color(0.20f, 0.14f, 0.05f, 0.95f) : new Color(0.03f, 0.04f, 0.035f, 0.9f));
            text(course.title, m + chip + 12f * u, 54f * u, 18f * u, on,
                    on ? new Color(0.96f, 0.68f, 0.28f, 1f) : Color.WHITE);
            chip += 258f * u;
        }

        float top = height - 96f * u;
        float gap = 12f * u;
        float colW = (width - m * 2f - gap * 2f) / 3f;
        float listH = top - 84f * u;
        drawRifles(m, top - listH, colW, listH, u);
        drawRifleCard(m + colW + gap, top - listH, colW, listH, u);
        drawBuild(m + (colW + gap) * 2f, top - listH, colW, listH, u);

        float deployW = 240f * u;
        float deployH = 56f * u;
        panel(width - m - deployW, 16f * u, deployW, deployH, new Color(0.20f, 0.14f, 0.05f, 0.95f));
        text("DEPLOY", width - m - deployW + 62f * u, 16f * u + deployH - 12f * u, 22f * u, true,
                new Color(0.96f, 0.68f, 0.28f, 1f));
    }

    private void drawRifles(float x, float y, float w, float h, float u) {
        panel(x, y, w, h, new Color(0.025f, 0.032f, 0.028f, 0.92f));
        text("RIFLES", x + 12f * u, y + h - 10f * u, 20f * u, true, new Color(0.48f, 0.82f, 0.80f, 1f));
        float rowH = RIFLE_ROW * u;
        float rowY = y + h - 48f * u;
        for (int i = 0; i < WeaponCatalog.all().size(); i++) {
            Weapon weapon = WeaponCatalog.byIndex(i);
            boolean on = i == loadout.rifleIndex;
            if (on) {
                batch.setColor(0.10f, 0.12f, 0.09f, 0.95f);
                batch.draw(pixel, x + 8f * u, rowY - rowH + 8f * u, w - 16f * u, rowH - 4f * u);
                batch.setColor(weapon.accent);
                batch.draw(pixel, x + 8f * u, rowY - rowH + 8f * u, 3f * u, rowH - 4f * u);
                batch.setColor(Color.WHITE);
            }
            text((i + 1) + "  " + weapon.name, x + 18f * u, rowY - 2f * u, 20f * u, on,
                    on ? Color.WHITE : new Color(0.62f, 0.67f, 0.58f, 1f));
            text(weapon.caliber + "  ·  " + weapon.action.label, x + 18f * u, rowY - 26f * u, 16f * u, false,
                    new Color(0.62f, 0.67f, 0.58f, 1f));
            rowY -= rowH;
        }
    }

    private void drawRifleCard(float x, float y, float w, float h, float u) {
        panel(x, y, w, h, new Color(0.025f, 0.032f, 0.028f, 0.92f));
        Weapon weapon = WeaponCatalog.byIndex(loadout.rifleIndex);
        text(weapon.name, x + 14f * u, y + h - 12f * u, 26f * u, true, Color.WHITE);
        text(weapon.designation, x + 14f * u, y + h - 46f * u, 18f * u, false,
                new Color(0.62f, 0.67f, 0.58f, 1f));
        String[] lines = {
                weapon.caliber + "  ·  " + weapon.action.label,
                String.format(Locale.ROOT, "%.0f gr   %.0f m/s   G7 %.3f",
                        weapon.bullet.massGrains, weapon.bullet.muzzleVelocity, weapon.bullet.bcG7),
                String.format(Locale.ROOT, "MAG %d   DMG %d   PEN %d",
                        weapon.magazineSize, weapon.damage, weapon.penetration),
                String.format(Locale.ROOT, "EFFECTIVE %d M   CYCLE %.2f S",
                        weapon.maxRangeMeters, weapon.cycleTime),
                "ISSUED  " + weapon.optic,
                "Cartridge stays with the rifle.",
                "Scope, reticle, height and zero",
                "are set on the build."
        };
        float lineY = y + h - 82f * u;
        for (String line : lines) {
            text(line, x + 14f * u, lineY, 18f * u, false, new Color(0.91f, 0.93f, 0.88f, 1f));
            lineY -= 32f * u;
        }
    }

    private void drawBuild(float x, float y, float w, float h, float u) {
        panel(x, y, w, h, new Color(0.025f, 0.032f, 0.028f, 0.92f));
        text("SCOPE", x + 12f * u, y + h - 10f * u, 20f * u, true, new Color(0.48f, 0.82f, 0.80f, 1f));
        float rowH = OPTIC_ROW * u;
        float rowY = y + h - 46f * u;
        for (int i = 0; i < OpticCatalog.all().size(); i++) {
            Optic optic = OpticCatalog.byIndex(i);
            boolean on = i == loadout.opticIndex && column != 0;
            boolean named = optic.name.equals(loadout.opticName);
            text(optic.name, x + 12f * u, rowY, 17f * u, named, named ? new Color(0.96f, 0.68f, 0.28f, 1f)
                    : new Color(0.75f, 0.78f, 0.72f, 1f));
            rowY -= rowH;
        }
        text("BUILD", x + 12f * u, rowY - 6f * u, 20f * u, true, new Color(0.48f, 0.82f, 0.80f, 1f));
        rowY -= 36f * u;
        for (int i = 0; i < TUNE_ROWS; i++) {
            boolean on = column == 2 && tuneRow == i;
            if (on) {
                batch.setColor(0.12f, 0.10f, 0.05f, 0.9f);
                batch.draw(pixel, x + 8f * u, rowY - 22f * u, w - 16f * u, 30f * u);
                batch.setColor(Color.WHITE);
            }
            float btn = 42f * u;
            panel(x + 10f * u, rowY - 28f * u, btn, 34f * u, new Color(0.16f, 0.12f, 0.05f, 0.95f));
            panel(x + w - 14f * u - btn, rowY - 28f * u, btn, 34f * u, new Color(0.16f, 0.12f, 0.05f, 0.95f));
            text("-", x + 22f * u, rowY - 2f * u, 22f * u, true, new Color(0.96f, 0.68f, 0.28f, 1f));
            text("+", x + w - 4f * u - btn, rowY - 2f * u, 22f * u, true, new Color(0.96f, 0.68f, 0.28f, 1f));
            text(TUNE_LABELS[i], x + 62f * u, rowY, 17f * u, false, new Color(0.62f, 0.67f, 0.58f, 1f));
            text(tuneValue(i), x + w * 0.46f, rowY, 18f * u, true, Color.WHITE);
            rowY -= TUNE_ROW * u;
        }
        text("ISSUED SETUP", x + 14f * u, y + 18f * u, 18f * u, true, new Color(0.56f, 0.85f, 0.46f, 1f));
    }

    private String tuneValue(int row) {
        switch (row) {
            case 0:
                return String.format(Locale.ROOT, "%.1fx", loadout.zoomMin);
            case 1:
                return String.format(Locale.ROOT, "%.1fx", loadout.zoomMax);
            case 2:
                return String.format(Locale.ROOT, "%.1fx", loadout.zoomDefault);
            case 3:
                return loadout.reticle.label;
            case 4:
                return String.format(Locale.ROOT, "%.1f mm", loadout.sightHeight * 1000.0);
            default:
                return loadout.zeroDistance + " M";
        }
    }

    private void hit(int x, int y) {
        float u = height / 900f;
        float m = 22f * u;
        float gap = 12f * u;
        float colW = (width - m * 2f - gap * 2f) / 3f;
        float panelTop = height - 96f * u;
        float panelBottom = 84f * u;
        float py = height - y;
        if (py >= 16f * u && py <= 72f * u && x >= width - m - 240f * u) {
            deploy();
            return;
        }
        if (py >= 16f * u && py <= 72f * u && x >= m && x < m + 258f * u * RangeCourse.values().length) {
            int index = (int) ((x - m) / (258f * u));
            if (index >= 0 && index < RangeCourse.values().length) {
                loadout.range = RangeCourse.values()[index];
            }
            return;
        }
        if (x >= m && x < m + colW && py <= panelTop && py >= panelBottom) {
            int row = (int) ((panelTop - 48f * u - py) / (RIFLE_ROW * u));
            if (row >= 0 && row < WeaponCatalog.all().size()) {
                loadout.rifleIndex = row;
                column = 0;
            }
            return;
        }
        float buildX = m + (colW + gap) * 2f;
        if (x < buildX || py < panelBottom || py > panelTop) {
            return;
        }
        float opticTop = panelTop - 46f * u;
        float opticBottom = opticTop - OpticCatalog.all().size() * OPTIC_ROW * u;
        if (py <= opticTop && py > opticBottom) {
            loadout.adoptOptic((int) ((opticTop - py) / (OPTIC_ROW * u)));
            column = 1;
            return;
        }
        float tuneTop = opticBottom - 36f * u;
        int tune = (int) ((tuneTop - py) / (TUNE_ROW * u));
        if (tune >= 0 && tune < TUNE_ROWS) {
            column = 2;
            tuneRow = tune;
            float rowBase = tuneTop - tune * TUNE_ROW * u;
            float btn = 42f * u;
            if (py <= rowBase + 6f * u && py >= rowBase - 28f * u) {
                if (x < buildX + 10f * u + btn + 8f * u) {
                    adjust(-1);
                } else if (x > buildX + colW - 14f * u - btn) {
                    adjust(1);
                }
            }
            return;
        }
        if (py < panelBottom + 40f * u) {
            loadout.adoptRifle(WeaponCatalog.byIndex(loadout.rifleIndex));
            loadout.opticIndex = -1;
        }
    }

    private void move(int direction) {
        if (column == 0) {
            loadout.rifleIndex = Math.floorMod(loadout.rifleIndex + direction, WeaponCatalog.all().size());
        } else if (column == 1) {
            loadout.adoptOptic(loadout.opticIndex + direction);
        } else {
            tuneRow = Math.floorMod(tuneRow + direction, TUNE_ROWS);
        }
    }

    private void adjust(int direction) {
        if (column == 0) {
            column = 1;
            return;
        }
        if (column == 1) {
            column = direction > 0 ? 2 : 0;
            return;
        }
        if (direction < 0 && tuneRow == 0 && loadout.zoomMin <= 1.05) {
            column = 1;
            return;
        }
        switch (tuneRow) {
            case 0:
                loadout.zoomMin = clamp(loadout.zoomMin + direction * 0.5, 1, loadout.zoomMax);
                break;
            case 1:
                loadout.zoomMax = clamp(loadout.zoomMax + direction * 0.5, loadout.zoomMin, 40);
                break;
            case 2:
                loadout.zoomDefault = clamp(loadout.zoomDefault + direction * 0.5, loadout.zoomMin, loadout.zoomMax);
                break;
            case 3:
                Reticle[] reticles = Reticle.values();
                int index = Math.floorMod(loadout.reticle.ordinal() + direction, reticles.length);
                loadout.reticle = reticles[index];
                break;
            case 4:
                loadout.sightHeight = clamp(loadout.sightHeight + direction * 0.005, 0.030, 0.090);
                break;
            default:
                loadout.zeroDistance += direction * 100;
                if (loadout.zeroDistance > 500) {
                    loadout.zeroDistance = 100;
                }
                if (loadout.zeroDistance < 100) {
                    loadout.zeroDistance = 500;
                }
                break;
        }
        loadout.zoomDefault = clamp(loadout.zoomDefault, loadout.zoomMin, loadout.zoomMax);
    }

    private void deploy() {
        game.setScreen(new LoadingScreen(game, LoadingScreen.Job.RANGE, loadout.copy(), range));
        dispose();
    }

    private void back() {
        if (range != null) {
            game.setScreen(range);
            dispose();
        }
    }

    private void panel(float x, float y, float w, float h, Color color) {
        batch.setColor(color);
        batch.draw(pixel, x, y, w, h);
        batch.setColor(Color.WHITE);
    }

    private void text(String value, float x, float top, float size, boolean strong, Color color) {
        BitmapFont face = strong ? bold : font;
        face.getData().setScale(size * 1.35f / 64f);
        face.setColor(color);
        face.draw(batch, value, x, top);
    }

    private static BitmapFont generate(FreeTypeFontGenerator generator) {
        FreeTypeFontGenerator.FreeTypeFontParameter parameter =
                new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = 64;
        parameter.incremental = true;
        BitmapFont result = generator.generateFont(parameter);
        result.setUseIntegerPositions(false);
        return result;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public void dispose() {
        batch.dispose();
        pixel.dispose();
        font.dispose();
        if (bold != font) {
            bold.dispose();
        }
        if (regularGenerator != null) {
            regularGenerator.dispose();
        }
        if (boldGenerator != null) {
            boldGenerator.dispose();
        }
    }
}
