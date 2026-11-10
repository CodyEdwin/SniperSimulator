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

import java.util.Locale;

/**
 * Separate armory screen. Pick a rifle, mount a scope, then change magnification,
 * reticle, sight height and zero before deploying.
 */
public final class CatalogScreen extends ScreenAdapter {
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
        text("ARMORY", m, height - 18f * u, 28f * u, true, new Color(0.96f, 0.68f, 0.28f, 1f));
        text("RIFLE, SCOPE AND ZERO  ·  ENTER DEPLOY  ·  ARROWS ADJUST  ·  ESC BACK",
                m, height - 52f * u, 12f * u, false, new Color(0.62f, 0.67f, 0.58f, 1f));

        float top = height - 78f * u;
        float gap = 12f * u;
        float colW = (width - m * 2f - gap * 2f) / 3f;
        float listH = top - 70f * u;
        drawRifles(m, top - listH, colW, listH, u);
        drawRifleCard(m + colW + gap, top - listH, colW, listH, u);
        drawBuild(m + (colW + gap) * 2f, top - listH, colW, listH, u);

        float deployW = 220f * u;
        float deployH = 42f * u;
        panel(width - m - deployW, 16f * u, deployW, deployH, new Color(0.20f, 0.14f, 0.05f, 0.95f));
        text("DEPLOY", width - m - deployW + 58f * u, 16f * u + deployH - 8f * u, 16f * u, true,
                new Color(0.96f, 0.68f, 0.28f, 1f));
    }

    private void drawRifles(float x, float y, float w, float h, float u) {
        panel(x, y, w, h, new Color(0.025f, 0.032f, 0.028f, 0.92f));
        text("RIFLES", x + 12f * u, y + h - 8f * u, 13f * u, true, new Color(0.48f, 0.82f, 0.80f, 1f));
        float rowH = 36f * u;
        float rowY = y + h - 36f * u;
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
            text((i + 1) + "  " + weapon.name, x + 18f * u, rowY - 4f * u, 13f * u, on,
                    on ? Color.WHITE : new Color(0.62f, 0.67f, 0.58f, 1f));
            text(weapon.caliber + "  ·  " + weapon.action.label, x + 18f * u, rowY - 20f * u, 10f * u, false,
                    new Color(0.62f, 0.67f, 0.58f, 1f));
            rowY -= rowH;
        }
    }

    private void drawRifleCard(float x, float y, float w, float h, float u) {
        panel(x, y, w, h, new Color(0.025f, 0.032f, 0.028f, 0.92f));
        Weapon weapon = WeaponCatalog.byIndex(loadout.rifleIndex);
        text(weapon.name, x + 14f * u, y + h - 10f * u, 18f * u, true, Color.WHITE);
        text(weapon.designation, x + 14f * u, y + h - 36f * u, 12f * u, false,
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
        float lineY = y + h - 64f * u;
        for (String line : lines) {
            text(line, x + 14f * u, lineY, 12f * u, false, new Color(0.91f, 0.93f, 0.88f, 1f));
            lineY -= 22f * u;
        }
    }

    private void drawBuild(float x, float y, float w, float h, float u) {
        panel(x, y, w, h, new Color(0.025f, 0.032f, 0.028f, 0.92f));
        text("SCOPE", x + 12f * u, y + h - 8f * u, 13f * u, true, new Color(0.48f, 0.82f, 0.80f, 1f));
        float rowH = 22f * u;
        float rowY = y + h - 32f * u;
        for (int i = 0; i < OpticCatalog.all().size(); i++) {
            Optic optic = OpticCatalog.byIndex(i);
            boolean on = i == loadout.opticIndex && column != 0;
            boolean named = optic.name.equals(loadout.opticName);
            text(optic.name, x + 12f * u, rowY, 10.5f * u, named, named ? new Color(0.96f, 0.68f, 0.28f, 1f)
                    : new Color(0.75f, 0.78f, 0.72f, 1f));
            rowY -= rowH;
        }
        text("BUILD", x + 12f * u, rowY - 4f * u, 13f * u, true, new Color(0.48f, 0.82f, 0.80f, 1f));
        rowY -= 26f * u;
        for (int i = 0; i < TUNE_ROWS; i++) {
            boolean on = column == 2 && tuneRow == i;
            if (on) {
                batch.setColor(0.12f, 0.10f, 0.05f, 0.9f);
                batch.draw(pixel, x + 8f * u, rowY - 16f * u, w - 16f * u, 20f * u);
                batch.setColor(Color.WHITE);
            }
            text(TUNE_LABELS[i], x + 14f * u, rowY, 11f * u, false, new Color(0.62f, 0.67f, 0.58f, 1f));
            text(tuneValue(i), x + w - 150f * u, rowY, 12f * u, true, Color.WHITE);
            rowY -= 24f * u;
        }
        text("ISSUED SETUP", x + 14f * u, y + 16f * u, 12f * u, true, new Color(0.56f, 0.85f, 0.46f, 1f));
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
        float panelTop = height - 78f * u;
        float panelBottom = 70f * u;
        float py = height - y;
        if (py >= 16f * u && py <= 58f * u && x >= width - m - 220f * u) {
            deploy();
            return;
        }
        if (x >= m && x < m + colW && py <= panelTop && py >= panelBottom) {
            int row = (int) ((panelTop - 36f * u - py) / (36f * u));
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
        float opticTop = panelTop - 32f * u;
        float opticBottom = opticTop - OpticCatalog.all().size() * 22f * u;
        if (py <= opticTop && py > opticBottom) {
            loadout.adoptOptic((int) ((opticTop - py) / (22f * u)));
            column = 1;
            return;
        }
        float tuneTop = opticBottom - 26f * u;
        int tune = (int) ((tuneTop - py) / (24f * u));
        if (tune >= 0 && tune < TUNE_ROWS) {
            column = 2;
            tuneRow = tune;
            adjust(x > buildX + colW * 0.55f ? 1 : -1);
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
        if (range == null) {
            game.setScreen(new GameScreen(game, false, loadout));
        } else {
            range.applyLoadout(loadout);
            game.setScreen(range);
        }
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
        face.getData().setScale(size / 64f);
        face.setColor(color);
        face.draw(batch, value, x, top);
    }

    private static BitmapFont generate(FreeTypeFontGenerator generator) {
        FreeTypeFontGenerator.FreeTypeFontParameter parameter =
                new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = 48;
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
