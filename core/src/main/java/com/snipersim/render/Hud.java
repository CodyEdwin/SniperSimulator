package com.snipersim.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.snipersim.ballistics.BallisticTable;
import com.snipersim.util.BallisticMath;
import com.snipersim.weapons.Weapon;
import com.snipersim.weapons.WeaponCatalog;

import java.util.List;
import java.util.Locale;

/**
 * Heads-up display: weapon/ammo panel, environmental data, wind call, compass,
 * scope reticle, hit feedback, DOPE card and briefing/help screens.
 */
public final class Hud {
    private static final Color BG = new Color(0.025f, 0.032f, 0.028f, 0.74f);
    private static final Color BG_SOFT = new Color(0.03f, 0.04f, 0.035f, 0.55f);
    private static final Color WHITE = new Color(0.91f, 0.93f, 0.88f, 1f);
    private static final Color DIM = new Color(0.62f, 0.67f, 0.58f, 1f);
    private static final Color AMBER = new Color(0.96f, 0.68f, 0.28f, 1f);
    private static final Color GREEN = new Color(0.56f, 0.85f, 0.46f, 1f);
    private static final Color CYAN = new Color(0.48f, 0.82f, 0.80f, 1f);
    private static final Color RED = new Color(0.94f, 0.34f, 0.25f, 1f);
    private static final Color INK = new Color(0.02f, 0.025f, 0.022f, 0.92f);

    private final SpriteBatch batch;
    private final ShapeRenderer shapes;
    private final BitmapFont font;
    private final BitmapFont bold;
    private final FreeTypeFontGenerator regularGenerator;
    private final FreeTypeFontGenerator boldGenerator;
    private final Texture pixel;
    private float time;
    private int width;
    private int height;

    public Hud() {
        batch = new SpriteBatch();
        shapes = new ShapeRenderer();
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        pixel = new Texture(pixmap);
        pixmap.dispose();

        FreeTypeFontGenerator rg = null;
        FreeTypeFontGenerator bg = null;
        BitmapFont regular = null;
        BitmapFont boldFont = null;
        try {
            rg = new FreeTypeFontGenerator(Gdx.files.internal("fonts/JetBrainsMonoNerdFont-Regular.ttf"));
            bg = new FreeTypeFontGenerator(Gdx.files.internal("fonts/JetBrainsMonoNerdFont-Bold.ttf"));
            regular = generate(rg);
            boldFont = generate(bg);
        } catch (Exception e) {
            Gdx.app.error("HUD", "JetBrains Mono Nerd Font unavailable, using fallback font", e);
        }
        regularGenerator = rg;
        boldGenerator = bg;
        font = regular != null ? regular : new BitmapFont();
        bold = boldFont != null ? boldFont : font;
        font.setUseIntegerPositions(false);
        bold.setUseIntegerPositions(false);
    }

    private static BitmapFont generate(FreeTypeFontGenerator generator) {
        FreeTypeFontGenerator.FreeTypeFontParameter parameter =
                new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = 64;
        parameter.incremental = true;
        parameter.kerning = true;
        parameter.minFilter = Texture.TextureFilter.Linear;
        parameter.magFilter = Texture.TextureFilter.Linear;
        parameter.characters = FreeTypeFontGenerator.DEFAULT_CHARS
                + "°±·—–→←↑↓•×✓";
        BitmapFont result = generator.generateFont(parameter);
        result.setUseIntegerPositions(false);
        return result;
    }

    public void render(HudState s, float delta) {
        time += delta;
        width = Gdx.graphics.getWidth();
        height = Gdx.graphics.getHeight();
        batch.getProjectionMatrix().setToOrtho2D(0, 0, width, height);
        float u = height / 900f;
        float margin = 22f * u;
        float top = height - margin;

        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        if (!s.briefing && !s.paused && !s.bulletCam) {
            drawReticle(s, u);
        }

        batch.begin();
        batch.setColor(Color.WHITE);
        if (s.briefing) {
            drawBriefing(s, u, margin);
        } else {
            if (s.paused) {
                drawPause(s, u);
            } else {
                drawWeaponPanel(s, u, margin, top);
                drawEnvironmentPanel(s, u, margin, top);
                drawStats(s, u, margin);
                drawWeaponStrip(s, u, margin);
                drawBreathBar(s, u);
                drawCompass(s, u, top);
                drawWindCall(s, u, top);
                if (s.scopeAmount > 0.25) {
                    drawScopeData(s, u);
                }
                if (s.dopeVisible) {
                    drawDope(s, u);
                }
                if (s.helpVisible) {
                    drawHelp(s, u, margin);
                }
                drawToast(s, u);
                if (s.bulletCam) {
                    drawBulletCam(s, u, margin, top);
                }
                if (s.flash > 0.01f) {
                    batch.setColor(1f, 0.75f, 0.40f, s.flash * 0.16f);
                    batch.draw(pixel, 0, 0, width, height);
                    batch.setColor(Color.WHITE);
                }
            }
        }
        batch.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    // ---------------------------------------------------------------- panels

    private void drawWeaponPanel(HudState s, float u, float m, float top) {
        float w = 350 * u;
        float h = 194 * u;
        float x = m;
        float y = top - h;
        panel(x, y, w, h, BG, 1f);
        Weapon weapon = s.weapon;
        if (weapon == null) {
            return;
        }
        batch.setColor(weapon.accent.r, weapon.accent.g, weapon.accent.b, 0.95f);
        batch.draw(pixel, x, y, 4 * u, h);
        batch.setColor(Color.WHITE);

        textTop(weapon.name, x + 16 * u, top - 12 * u, 25 * u, WHITE, true);
        textTop(weapon.designation, x + 16 * u, top - 44 * u, 13 * u, DIM, false);
        textTop(weapon.caliber, x + 16 * u, top - 62 * u, 15 * u, weapon.accent, true);

        ammoPips(s, x + 16 * u, y + 106 * u, u, weapon);

        textTop("AMMO", x + 16 * u, y + 92 * u, 11 * u, DIM, false);
        textTop(String.format(Locale.ROOT, "%02d", s.ammo), x + 16 * u, y + 86 * u, 32 * u,
                s.ammo == 0 ? RED : WHITE, true);
        textTop("/ " + s.magazine, x + 56 * u, y + 86 * u, 15 * u, DIM, false);

        textTop("ZERO  " + s.zeroDistance + " M", x + 132 * u, y + 86 * u, 13 * u, WHITE, false);
        textTop("ELEV  " + String.format(Locale.ROOT, "%+.1f MIL", s.elevationClicks * 0.1), x + 132 * u,
                y + 67 * u, 13 * u, AMBER, false);
        textTop("WIND  " + String.format(Locale.ROOT, "%+.1f MIL", s.windageClicks * 0.1), x + 132 * u,
                y + 48 * u, 13 * u, CYAN, false);
        textTop(String.format(Locale.ROOT, "HOLD %+.1f MIL  ·  %.1fx", s.holdoverMils, s.zoom),
                x + 132 * u, y + 28 * u, 10.5f * u, s.holdoverMils > 0.01 ? GREEN : DIM, false);

        if (s.reloading) {
            textTop("RELOADING", x + 16 * u, y + 18 * u, 11 * u, AMBER, true);
            batch.setColor(0.2f, 0.2f, 0.16f, 0.9f);
            batch.draw(pixel, x + 90 * u, y + 12 * u, 120 * u, 4 * u);
            batch.setColor(AMBER);
            batch.draw(pixel, x + 90 * u, y + 12 * u, 120 * u * s.reloadFraction, 4 * u);
            batch.setColor(Color.WHITE);
        }
    }

    private void ammoPips(HudState s, float x, float y, float u, Weapon weapon) {
        int max = Math.min(10, weapon.magazineSize);
        for (int i = 0; i < max; i++) {
            float px = x + i * 9 * u;
            batch.setColor(i < s.ammo ? weapon.accent : new Color(0.22f, 0.24f, 0.20f, 1f));
            batch.draw(pixel, px, y, 5 * u, 12 * u);
        }
        batch.setColor(Color.WHITE);
    }

    private void drawEnvironmentPanel(HudState s, float u, float m, float top) {
        float w = 340 * u;
        float h = 166 * u;
        float x = width - m - w;
        float y = top - h;
        panel(x, y, w, h, BG, 1f);
        textTop("ENVIRONMENT", x + 14 * u, top - 10 * u, 12 * u, CYAN, true);
        if (s.atmosphere != null) {
            textTop(String.format(Locale.ROOT, "WIND   %.1f M/S  FROM %03.0f°", s.windSpeed, s.windFrom),
                    x + 14 * u, top - 30 * u, 13 * u, AMBER, false);
            textTop(String.format(Locale.ROOT, "CALL   %.0f O'CLOCK   %s", s.windClock,
                    Math.abs(s.crosswind) < 0.35 ? "NO VALUE" : "FULL VALUE"),
                    x + 14 * u, top - 49 * u, 12 * u, DIM, false);
            textTop(String.format(Locale.ROOT, "TEMP   %.1f°C   PRESS %.0f HPA",
                    s.atmosphere.temperatureC(), s.atmosphere.stationPressureHpa()),
                    x + 14 * u, top - 69 * u, 12 * u, DIM, false);
            textTop(String.format(Locale.ROOT, "HUMID  %.0f%%   DENS ALT %.0f M",
                    s.atmosphere.humidity() * 100, s.densityAltitude),
                    x + 14 * u, top - 87 * u, 12 * u, DIM, false);
            textTop(String.format(Locale.ROOT, "CORIOLIS  %.1f°N  DRIFT RIGHT", s.atmosphere.latitudeDeg()),
                    x + 14 * u, top - 105 * u, 12 * u, DIM, false);
            textTop(String.format(Locale.ROOT, "DENSITY  %.3f KG/M³", s.density),
                    x + 14 * u, top - 123 * u, 11 * u, DIM, false);
        }
        textTop("RNG " + (s.rangeValid ? Math.round(s.rangeMeters) + " M" : "---"),
                x + 14 * u, top - 145 * u, 13 * u, GREEN, true);
    }

    private void drawStats(HudState s, float u, float m) {
        float w = 350 * u;
        float h = 100 * u;
        float y = m;
        panel(m, y, w, h, BG_SOFT, 1f);
        textTop("SCORE", m + 14 * u, y + h - 8 * u, 11 * u, DIM, false);
        textTop(String.format(Locale.ROOT, "%06d", s.score), m + 14 * u, y + h - 20 * u, 30 * u, AMBER, true);
        textTop(String.format(Locale.ROOT, "SHOTS %d   HITS %d   ACC %.0f%%", s.shots, s.hits, s.accuracy * 100),
                m + 172 * u, y + h - 28 * u, 12 * u, WHITE, false);
        textTop(String.format(Locale.ROOT, "LONGEST %.0f M", s.longestHit), m + 172 * u, y + h - 47 * u,
                12 * u, DIM, false);
        textTop(String.format(Locale.ROOT, "BEST RING %d   %.0f FT-LB", s.bestRing, s.bestEnergy),
                m + 172 * u, y + h - 65 * u, 11 * u, DIM, false);
        textTop("DAGGER RIDGE  /  LIVE FIRE", m + 14 * u, y + 16 * u, 10 * u, DIM, false);
    }

    private void drawWeaponStrip(HudState s, float u, float m) {
        if (s.scopeAmount > 0.45) {
            return;
        }
        List<Weapon> catalog = WeaponCatalog.all();
        String[] labels = {"RIMFIRE", "CARBINE", "6.5", ".308", ".300WM", ".338LM", ".50BMG"};
        float cell = 84 * u;
        float gap = 4 * u;
        float x = width - m - (cell + gap) * catalog.size() + gap;
        float y = m;
        for (int i = 0; i < catalog.size(); i++) {
            boolean active = i == s.weaponIndex;
            float cx = x + i * (cell + gap);
            panel(cx, y, cell, 32 * u, active ? new Color(0.10f, 0.12f, 0.09f, 0.92f) : BG_SOFT, 1f);
            if (active) {
                batch.setColor(catalog.get(i).accent);
                batch.draw(pixel, cx, y, cell, 3 * u);
                batch.setColor(Color.WHITE);
            }
            textCentered((i + 1) + "", cx + 10 * u, y + 21 * u, 10 * u, active ? AMBER : DIM, true);
            textTop(labels[i], cx + 21 * u, y + 26 * u, 10.5f * u,
                    active ? WHITE : DIM, active);
        }
    }

    private void drawBreathBar(HudState s, float u) {
        float w = 300 * u;
        float x = width * 0.5f - w * 0.5f;
        float y = 42 * u;
        batch.setColor(0.03f, 0.045f, 0.038f, 0.75f);
        batch.draw(pixel, x - 6 * u, y - 6 * u, w + 12 * u, 20 * u);
        batch.setColor(0.18f, 0.21f, 0.17f, 1f);
        batch.draw(pixel, x, y, w, 7 * u);
        Color c = s.breath > 0.55f ? GREEN : (s.breath > 0.25f ? AMBER : RED);
        batch.setColor(c);
        batch.draw(pixel, x, y, w * MathUtils.clamp((float) s.breath, 0, 1), 7 * u);
        batch.setColor(Color.WHITE);
        String label = s.holdingBreath ? "HOLDING BREATH" : (s.steady ? "STEADY" : "BREATH");
        textCentered(label, x + w * 0.5f, y + 22 * u, 10 * u, s.holdingBreath ? AMBER : DIM, true);
        if (!s.holdingBreath && s.breath < 0.35) {
            textCentered("RECOVERING", x + w * 0.5f, y - 10 * u, 9 * u, RED, false);
        }
    }

    private void drawCompass(HudState s, float u, float top) {
        if (s.scopeAmount > 0.45) {
            return;
        }
        float cx = width * 0.5f;
        float y = top - 18 * u;
        float halfWidth = 220 * u;
        batch.setColor(0.03f, 0.04f, 0.035f, 0.45f);
        batch.draw(pixel, cx - halfWidth, y - 8 * u, halfWidth * 2, 24 * u);
        batch.setColor(Color.WHITE);
        String[] names = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
        for (int deg = 0; deg < 360; deg += 15) {
            float diff = angleDiff(deg, (float) s.compassHeading);
            if (Math.abs(diff) > 72) {
                continue;
            }
            float x = cx + (diff / 72f) * halfWidth;
            boolean major = deg % 45 == 0;
            batch.setColor(major ? WHITE : DIM);
            batch.draw(pixel, x - 0.5f * u, y - (major ? 7 : 4) * u, 1.2f * u, (major ? 9 : 6) * u);
            if (major) {
                textCentered(names[deg / 45], x, y + 12 * u, 10 * u, deg == 0 ? AMBER : WHITE, true);
            }
        }
        batch.setColor(AMBER);
        batch.draw(pixel, cx - 1f * u, y + 2 * u, 2f * u, 9 * u);
        batch.setColor(Color.WHITE);
    }

    private void drawWindCall(HudState s, float u, float top) {
        if (s.scopeAmount > 0.45) {
            return;
        }
        String arrow = s.crosswind >= 0 ? "→" : "←";
        String text = String.format(Locale.ROOT, "%s  WIND %.1f M/S  FROM %03.0f°  /  %.0f O'CLOCK",
                arrow, s.windSpeed, s.windFrom, s.windClock);
        float w = 510 * u;
        float x = width * 0.5f - w * 0.5f;
        panel(x, top - 70 * u, w, 30 * u, BG_SOFT, 0.85f);
        textCentered(text, width * 0.5f, top - 50 * u, 12 * u, AMBER, false);
    }

    private void drawScopeData(HudState s, float u) {
        float cx = width * 0.5f;
        float cy = height * 0.5f;
        float r = 0.464f * height * MathUtils.clamp((float) s.scopeAmount, 0, 1);
        float left = cx - r * 0.73f;
        float right = cx + r * 0.73f;
        float bottom = cy - r * 0.72f;
        float top = cy + r * 0.66f;
        Color c = new Color(0.84f, 0.92f, 0.82f, 0.82f);
        textTop(String.format(Locale.ROOT, "RNG %04.0f", s.rangeValid ? s.rangeMeters : 0), left - 26 * u,
                top + 12 * u, 13 * u, s.rangeValid ? GREEN : c, true);
        textTop(String.format(Locale.ROOT, "WIND %+.1f MIL", -s.crosswind * 0.25), left - 26 * u,
                top - 8 * u, 11 * u, c, false);
        textTop(String.format(Locale.ROOT, "ELEV %+.1f MIL", s.elevationClicks * 0.1), left - 26 * u,
                top - 26 * u, 11 * u, AMBER, false);
        textTop(String.format(Locale.ROOT, "HOLD %+.1f MIL", s.holdoverMils), left - 26 * u,
                top - 44 * u, 11 * u, s.holdoverMils > 0.01 ? GREEN : c, false);
        textTop(String.format(Locale.ROOT, "%.1fx", s.zoom), right - 34 * u, top + 12 * u,
                13 * u, c, true);
        textTop(s.weapon != null ? s.weapon.caliber : "", right - 74 * u, top - 8 * u,
                10 * u, c, false);
        textTop(s.weapon != null ? s.weapon.reticle.label : "", right - 74 * u, top - 24 * u,
                9 * u, c, false);
        textTop(String.format(Locale.ROOT, "TOF %.2f S", s.airborne ? s.bulletTof : 0), right - 74 * u,
                bottom + 2 * u, 10 * u, c, false);
        if (!s.rangeValid) {
            textTop("LRF ----", right - 74 * u, bottom + 20 * u, 10 * u, DIM, false);
        } else {
            textTop(String.format(Locale.ROOT, "LRF %04.0f", s.rangeMeters), right - 74 * u,
                    bottom + 20 * u, 10 * u, GREEN, false);
        }
        textCentered("HOLD SHIFT TO STEADY", cx, bottom - 2 * u, 9 * u,
                s.breath < 0.3f ? RED : DIM, false);
    }

    private void drawDope(HudState s, float u) {
        float w = Math.min(1020 * u, width - 60 * u);
        float x = (width - w) * 0.5f;
        float h = 600 * u;
        float y = height * 0.5f - h * 0.5f;
        panel(x, y, w, h, new Color(0.02f, 0.028f, 0.024f, 0.94f), 1f);
        batch.setColor(CYAN);
        batch.draw(pixel, x, y + h - 3 * u, w, 3 * u);
        batch.setColor(Color.WHITE);
        textTop("DOPE CARD", x + 20 * u, y + h - 16 * u, 20 * u, WHITE, true);
        if (s.dope != null && !s.dope.rows.isEmpty()) {
            textTop(String.format(Locale.ROOT, "%s  /  ZERO %d M  /  %.0f HPA  %.1f°C  %.0f%% RH",
                    s.dope.bullet.name, s.dope.zeroDistance,
                    s.dope.atmosphere.stationPressureHpa(), s.dope.atmosphere.temperatureC(),
                    s.dope.atmosphere.humidity() * 100),
                    x + 20 * u, y + h - 42 * u, 10 * u, DIM, false);

            float colRng = x + 20 * u;
            float colDrop = x + 120 * u;
            float colDrift = x + 230 * u;
            float colTof = x + 340 * u;
            float colVel = x + 450 * u;
            float colEnergy = x + 570 * u;
            float colElev = x + 710 * u;
            float colWind = x + 840 * u;
            float headerY = y + h - 72 * u;
            textTop("RNG", colRng, headerY, 11 * u, AMBER, true);
            textTop("DROP", colDrop, headerY, 11 * u, AMBER, true);
            textTop("DRIFT", colDrift, headerY, 11 * u, AMBER, true);
            textTop("TOF", colTof, headerY, 11 * u, AMBER, true);
            textTop("VEL", colVel, headerY, 11 * u, AMBER, true);
            textTop("ENERGY", colEnergy, headerY, 11 * u, AMBER, true);
            textTop("ELEV", colElev, headerY, 11 * u, AMBER, true);
            textTop("WIND", colWind, headerY, 11 * u, AMBER, true);
            batch.setColor(0.35f, 0.40f, 0.33f, 0.9f);
            batch.draw(pixel, x + 20 * u, headerY - 8 * u, w - 40 * u, 1 * u);
            batch.setColor(Color.WHITE);

            float rowY = headerY - 26 * u;
            for (BallisticTable.Row row : s.dope.rows) {
                boolean highlight = Math.abs(row.range - s.highlightRange) < 50;
                if (highlight) {
                    batch.setColor(0.16f, 0.24f, 0.16f, 0.75f);
                    batch.draw(pixel, x + 14 * u, rowY - 13 * u, w - 28 * u, 15.5f * u);
                    batch.setColor(Color.WHITE);
                }
                Color c = highlight ? GREEN : WHITE;
                textTop(String.format(Locale.ROOT, "%4d", row.range), colRng, rowY, 9.5f * u, c, false);
                textTop(String.format(Locale.ROOT, "%+.0f cm", row.dropMeters * 100), colDrop, rowY, 9.5f * u, c, false);
                textTop(String.format(Locale.ROOT, "%+.0f cm", row.driftMeters * 100), colDrift, rowY, 9.5f * u, c, false);
                textTop(String.format(Locale.ROOT, "%.2f", row.tof), colTof, rowY, 9.5f * u, c, false);
                textTop(String.format(Locale.ROOT, "%.0f", row.velocity), colVel, rowY, 9.5f * u, c, false);
                textTop(String.format(Locale.ROOT, "%.0f J", row.energyJoules), colEnergy, rowY, 9.5f * u, c, false);
                textTop(String.format(Locale.ROOT, "%+d", row.elevationClicks), colElev, rowY, 9.5f * u,
                        row.elevationClicks == 0 ? DIM : AMBER, false);
                textTop(String.format(Locale.ROOT, "%+d", row.windClicks), colWind, rowY, 9.5f * u,
                        row.windClicks == 0 ? DIM : CYAN, false);
                rowY -= 17f * u;
            }
            textTop("ELEV/WIND CLICKS: 0.1 MIL  ·  DRIFT + = RIGHT  ·  WIND + = HOLD RIGHT",
                    x + 20 * u, y + 24 * u, 9 * u, DIM, false);
            textTop("HOLD TAB TO CLOSE", x + w - 180 * u, y + 24 * u, 9 * u, DIM, false);
        } else {
            textTop("NO BALLISTIC SOLUTION — LASER RANGE A TARGET", x + 20 * u,
                    y + h * 0.5f, 13 * u, DIM, false);
        }
    }

    private void drawToast(HudState s, float u) {
        if (s.toastTimer <= 0 || s.toastTitle.isEmpty()) {
            return;
        }
        float alpha = MathUtils.clamp(s.toastTimer, 0, 1);
        float w = 620 * u;
        float h = 68 * u;
        float x = width * 0.5f - w * 0.5f;
        float y = height * 0.68f;
        panel(x, y, w, h, new Color(0.02f, 0.03f, 0.025f, 0.90f * alpha), alpha);
        batch.setColor(s.toastHit ? GREEN : RED);
        batch.draw(pixel, x, y, 4 * u, h);
        batch.setColor(Color.WHITE);
        textTop(s.toastTitle, x + 18 * u, y + h - 10 * u, 18 * u,
                s.toastHit ? GREEN : RED, true);
        textTop(s.toastDetail, x + 18 * u, y + 20 * u, 11 * u, WHITE, false);
        if (!s.missText.isEmpty()) {
            textTop(s.missText, x + 18 * u, y + 5 * u, 10 * u, AMBER, false);
        }
    }

    private void drawBulletCam(HudState s, float u, float m, float top) {
        panel(m, top - 98 * u, 320 * u, 88 * u, new Color(0.02f, 0.03f, 0.025f, 0.85f), 1f);
        textTop("BULLET CAM", m + 14 * u, top - 8 * u, 14 * u, AMBER, true);
        textTop(String.format(Locale.ROOT, "SPEED  %.0f M/S", s.bulletSpeed), m + 14 * u,
                top - 32 * u, 11 * u, WHITE, false);
        textTop(String.format(Locale.ROOT, "HEIGHT %.1f M   TOF %.2f S", s.bulletHeight, s.bulletTof),
                m + 14 * u, top - 50 * u, 11 * u, WHITE, false);
        textTop(String.format(Locale.ROOT, "RANGE  %.0f M", s.bulletRange), m + 14 * u,
                top - 68 * u, 11 * u, WHITE, false);
    }

    private void drawBriefing(HudState s, float u, float m) {
        batch.setColor(0.012f, 0.018f, 0.016f, 0.82f);
        batch.draw(pixel, 0, 0, width, height);
        batch.setColor(Color.WHITE);
        textTop("DAGGER RIDGE", m + 42 * u, height - 56 * u, 62 * u, WHITE, true);
        textTop("LONG-RANGE PRECISION SIMULATOR", m + 46 * u, height - 118 * u, 15 * u, AMBER, true);
        batch.setColor(0.96f, 0.68f, 0.28f, 0.9f);
        batch.draw(pixel, m + 44 * u, height - 140 * u, 560 * u, 2 * u);
        batch.setColor(Color.WHITE);

        float col1 = m + 46 * u;
        float col2 = width * 0.49f;
        float y = height - 180 * u;
        textTop("MISSION", col1, y, 15 * u, CYAN, true);
        textTop("Engage the steel line from 200 to 2,000 m.", col1, y - 26 * u, 13 * u, WHITE, false);
        textTop("Read the wind, dial your DOPE and make the", col1, y - 45 * u, 13 * u, WHITE, false);
        textTop("first round count. Eleven targets. No markers.", col1, y - 64 * u, 13 * u, WHITE, false);
        textTop("[ ] / PGUP PGDN  holdover without dialing  ·  \\ clears",
                col1, y - 83 * u, 10 * u, GREEN, false);

        textTop("RANGE CARD", col1, y - 100 * u, 12 * u, AMBER, true);
        String[] ranges = {"200 m  SILHOUETTE", "320 m  GONG", "450 m  GONG", "600 m  MOVER",
                "760 m  GONG", "930 m  SILHOUETTE", "1,100 m  GONG", "1,280 m  MOVER",
                "1,540 m  GONG", "1,780 m  SILHOUETTE", "2,000 m  GONG"};
        for (int i = 0; i < ranges.length; i++) {
            textTop(ranges[i], col1 + (i / 6) * 210 * u, y - 124 * u - (i % 6) * 19 * u,
                    10.5f * u, DIM, false);
        }

        textTop("CONDITIONS", col1, y - 258 * u, 12 * u, AMBER, true);
        if (s.atmosphere != null) {
            textTop(String.format(Locale.ROOT, "WIND %.1f M/S FROM %03.0f°  (%.0f O'CLOCK)",
                    s.windSpeed, s.windFrom, s.windClock), col1, y - 280 * u, 11 * u, WHITE, false);
            textTop(String.format(Locale.ROOT, "TEMP %.1f°C  PRESS %.0f HPA  RH %.0f%%",
                    s.atmosphere.temperatureC(), s.atmosphere.stationPressureHpa(),
                    s.atmosphere.humidity() * 100), col1, y - 298 * u, 11 * u, WHITE, false);
            textTop(String.format(Locale.ROOT, "DENSITY ALT %.0f M  LAT %.1f°N",
                    s.densityAltitude, s.atmosphere.latitudeDeg()), col1, y - 316 * u, 11 * u, WHITE, false);
        }

        textTop("CONTROLS", col2, y, 15 * u, CYAN, true);
        String[][] controls = {
                {"MOUSE", "Aim / look"},
                {"RIGHT MOUSE", "Scope (hold) / wheel zoom"},
                {"LEFT MOUSE", "Fire"},
                {"SHIFT", "Hold breath"},
                {"ARROWS", "Elevation / windage turrets"},
                {"R", "Reload"},
                {"1-7", "Select weapon"},
                {"L", "Laser rangefinder"},
                {"TAB", "Hold for DOPE card"},
                {"V", "Bullet camera"},
                {"Z", "Cycle zero distance"},
                {"F5", "New wind conditions"},
                {"H", "Help overlay"},
                {"ESC", "Pause"}
        };
        for (int i = 0; i < controls.length; i++) {
            textTop(controls[i][0], col2, y - 30 * u - i * 22 * u, 11 * u, AMBER, true);
            textTop(controls[i][1], col2 + 130 * u, y - 30 * u - i * 22 * u, 11 * u, WHITE, false);
        }

        Weapon weapon = s.weapon;
        if (weapon != null) {
            textTop("PRIMARY", col2, y - 360 * u, 12 * u, AMBER, true);
            textTop(weapon.name + "  /  " + weapon.caliber, col2, y - 382 * u, 13 * u, WHITE, true);
            textTop(weapon.specLine(), col2, y - 401 * u, 10.5f * u, DIM, false);
            textTop(String.format(Locale.ROOT, "MAG %d  ·  %d M MAX  ·  ZERO %d M",
                    weapon.magazineSize, weapon.maxRangeMeters, s.zeroDistance),
                    col2, y - 419 * u, 10.5f * u, DIM, false);
        }

        float blink = 0.65f + 0.35f * (float) Math.sin(time * 3.2);
        textCentered("PRESS ENTER TO DEPLOY", width * 0.5f, m + 34 * u, 19 * u,
                new Color(AMBER.r, AMBER.g, AMBER.b, blink), true);
        textCentered("JetBrains Mono Nerd Font  ·  procedural ballistics  ·  no external art",
                width * 0.5f, m + 14 * u, 8.5f * u, DIM, false);
    }

    private void drawPause(HudState s, float u) {
        batch.setColor(0.01f, 0.016f, 0.014f, 0.75f);
        batch.draw(pixel, 0, 0, width, height);
        batch.setColor(Color.WHITE);
        textCentered("PAUSED", width * 0.5f, height * 0.62f, 44 * u, WHITE, true);
        textCentered("CLICK TO CAPTURE MOUSE  ·  ESC TO RESUME", width * 0.5f,
                height * 0.62f - 40 * u, 12 * u, AMBER, false);
        String[] lines = {
                "LMB fire    RMB scope    SHIFT breathe    R reload",
                "ARROWS turrets    1-7 weapon    L rangefinder    TAB DOPE",
                "V bullet cam    Z zero distance    F5 new wind    H help"
        };
        for (int i = 0; i < lines.length; i++) {
            textCentered(lines[i], width * 0.5f, height * 0.50f - i * 22 * u, 11 * u, DIM, false);
        }
    }

    private void drawHelp(HudState s, float u, float m) {
        float w = 680 * u;
        float h = 470 * u;
        float x = width - m - w;
        float y = height * 0.5f - h * 0.5f;
        panel(x, y, w, h, new Color(0.02f, 0.03f, 0.025f, 0.93f), 1f);
        textTop("FIELD MANUAL", x + 20 * u, y + h - 16 * u, 18 * u, AMBER, true);
        String[][] controls = {
                {"MOUSE", "Aim / look"},
                {"RIGHT MOUSE", "Scope (hold), wheel zoom"},
                {"LEFT MOUSE", "Fire"},
                {"SHIFT", "Hold breath"},
                {"ARROW KEYS", "Elevation / windage (0.1 mil)"},
                {"R", "Reload"},
                {"1-7", "Select weapon"},
                {"L", "Laser rangefinder"},
                {"TAB", "DOPE card (hold)"},
                {"V", "Bullet camera"},
                {"Z", "Cycle zero 100/200/300/400/500"},
                {"F5", "Reroll wind conditions"},
                {"[ ] / PGUP PGDN", "Holdover 1 / 5 mil without dialing"},
                {"\\", "Clear holdover"},
                {"H", "Close field manual"},
                {"ESC", "Pause / release mouse"}
        };
        for (int i = 0; i < controls.length; i++) {
            textTop(controls[i][0], x + 20 * u, y + h - 48 * u - i * 24 * u, 11 * u, CYAN, true);
            textTop(controls[i][1], x + 190 * u, y + h - 48 * u - i * 24 * u, 11 * u, WHITE, false);
        }
    }

    // --------------------------------------------------------------- reticle

    private void drawReticle(HudState s, float u) {
        float cx = width * 0.5f;
        float cy = height * 0.5f;
        float scope = MathUtils.clamp((float) s.scopeAmount, 0, 1);
        shapes.setProjectionMatrix(new com.badlogic.gdx.math.Matrix4().setToOrtho2D(0, 0, width, height));
        shapes.begin(ShapeRenderer.ShapeType.Filled);

        if (scope > 0.02) {
            float alpha = MathUtils.clamp(scope * 1.4f, 0, 1);
            Color line = new Color(0.035f, 0.035f, 0.03f, 0.90f * alpha);
            Color halo = new Color(0.85f, 0.87f, 0.82f, 0.12f * alpha);
            float mil = Math.max(2.2f, s.pixelsPerMil);
            float thick = Math.max(1f, 1.05f * u);
            float radius = 0.464f * height * scope;

            float armX = Math.min(radius * 0.92f, 13 * mil);
            float armYUp = Math.min(radius * 0.55f, 5.5f * mil);
            float armYDown = Math.min(radius * 0.72f, 9 * mil);
            shapes.setColor(halo);
            rect(cx - armX, cy - thick * 0.5f - 0.5f, armX * 2, thick + 1f);
            rect(cx - thick * 0.5f - 0.5f, cy - armYDown, thick + 1f, armYUp + armYDown);
            shapes.setColor(line);
            // center gap
            float gap = Math.max(2.5f * u, 1.8f);
            rect(cx - armX, cy - thick * 0.5f, armX - gap, thick);
            rect(cx + gap, cy - thick * 0.5f, armX - gap, thick);
            rect(cx - thick * 0.5f, cy + gap, thick, armYUp - gap);
            rect(cx - thick * 0.5f, cy - armYDown, thick, armYDown - gap);

            int maxMil = 8;
            for (int i = 1; i <= maxMil; i++) {
                float y = cy - i * mil;
                if (y < cy - radius * 0.78f) {
                    break;
                }
                float tick = (i % 2 == 0) ? 4.0f * u : 2.4f * u;
                rect(cx - tick, y - thick * 0.5f, tick * 2, thick);
                if (s.weapon != null && s.weapon.reticle == com.snipersim.weapons.Reticle.CHRISTMAS_TREE) {
                    float span = i * mil * 0.42f;
                    rect(cx - span, y - thick * 0.5f, span * 2, thick * 0.75f);
                    for (int j = 1; j <= 3; j++) {
                        float hx = cx + j * mil * 0.5f;
                        rect(hx, y - 2.2f * u, thick * 0.75f, 4.4f * u);
                        rect(cx - hx + cx, y - 2.2f * u, thick * 0.75f, 4.4f * u);
                    }
                }
            }
            for (int i = 1; i <= 6; i++) {
                float x = cx + i * mil;
                if (x > cx + radius * 0.82f) {
                    break;
                }
                rect(x, cy - 2.2f * u, thick, 4.4f * u);
                rect(cx - (x - cx), cy - 2.2f * u, thick, 4.4f * u);
            }

            if (s.holdoverMils > 0.01) {
                float holdPx = (float) (s.holdoverMils * mil);
                if (holdPx < radius * 0.86f) {
                    shapes.setColor(new Color(0.96f, 0.68f, 0.28f, 0.95f * alpha));
                    float half = 6.5f * u;
                    rect(cx - half, cy - holdPx - thick * 0.75f, half * 2, thick * 1.5f);
                    rect(cx - 0.7f * u, cy - holdPx - thick * 0.75f, 1.4f * u, 5.5f * u);
                }
            }

            shapes.setColor(new Color(0.95f, 0.28f, 0.16f, 0.95f * alpha));
            shapes.circle(cx, cy, Math.max(1.4f * u, 1.2f), 12);
            shapes.setColor(new Color(0.03f, 0.03f, 0.025f, 0.85f * alpha));
            shapes.circle(cx, cy, Math.max(0.6f * u, 0.5f), 8);
        } else {
            Color c = new Color(0.92f, 0.94f, 0.88f, 0.88f);
            shapes.setColor(new Color(0.02f, 0.03f, 0.02f, 0.5f));
            float gap = 5 * u;
            float len = 9 * u;
            rect(cx - gap - len, cy - 1.5f * u, len, 3 * u);
            rect(cx + gap, cy - 1.5f * u, len, 3 * u);
            rect(cx - 1.5f * u, cy - gap - len, 3 * u, len);
            rect(cx - 1.5f * u, cy + gap, 3 * u, len);
            shapes.setColor(c);
            rect(cx - gap - len, cy - 0.65f * u, len, 1.3f * u);
            rect(cx + gap, cy - 0.65f * u, len, 1.3f * u);
            rect(cx - 0.65f * u, cy - gap - len, 1.3f * u, len);
            rect(cx - 0.65f * u, cy + gap, 1.3f * u, len);
            shapes.setColor(new Color(0.95f, 0.32f, 0.20f, 0.9f));
            shapes.circle(cx, cy, 1.4f * u, 10);
        }

        if (s.hitMarker > 0.01f) {
            float a = MathUtils.clamp(s.hitMarker, 0, 1);
            float len = 13 * u;
            float th = 2 * u;
            shapes.setColor(new Color(0.96f, 0.92f, 0.75f, a));
            for (int i = 0; i < 4; i++) {
                float dx = (i == 0 || i == 3) ? 1 : -1;
                float dy = (i < 2) ? 1 : -1;
                float x = cx + dx * (5 * u);
                float y = cy + dy * (5 * u);
                shapes.rect(x, y, dx > 0 ? len : -len, th);
                shapes.rect(x, y, th, dy > 0 ? len : -len);
            }
        }
        if (s.missMarker > 0.01f) {
            float a = MathUtils.clamp(s.missMarker, 0, 1);
            shapes.setColor(new Color(0.9f, 0.35f, 0.25f, a * 0.7f));
            shapes.circle(cx, cy, 3 * u, 12);
        }
        shapes.end();
    }

    private void rect(float x, float y, float w, float h) {
        if (w < 0) {
            x += w;
            w = -w;
        }
        if (h < 0) {
            y += h;
            h = -h;
        }
        shapes.rect(x, y, w, h);
    }

    // ---------------------------------------------------------------- helpers

    private void panel(float x, float y, float w, float h, Color color, float alpha) {
        batch.setColor(color.r, color.g, color.b, color.a * alpha);
        batch.draw(pixel, x, y, w, h);
        batch.setColor(0.55f, 0.60f, 0.50f, 0.22f * alpha);
        batch.draw(pixel, x, y, w, 1);
        batch.setColor(Color.WHITE);
    }

    private void textTop(String text, float x, float topY, float size, Color color, boolean isBold) {
        BitmapFont f = isBold ? bold : font;
        f.getData().setScale(size / 64f);
        f.setColor(0, 0, 0, color.a * 0.55f);
        f.draw(batch, text, x + 1.2f, topY - size * 0.78f - 1.2f);
        f.setColor(color);
        f.draw(batch, text, x, topY - size * 0.78f);
    }

    private void textCentered(String text, float cx, float topY, float size, Color color, boolean isBold) {
        float widthEstimate = text.length() * size * 0.602f;
        textTop(text, cx - widthEstimate * 0.5f, topY, size, color, isBold);
    }

    private static float angleDiff(float a, float b) {
        float d = (a - b + 540f) % 360f - 180f;
        return d;
    }

    public void dispose() {
        batch.dispose();
        shapes.dispose();
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
