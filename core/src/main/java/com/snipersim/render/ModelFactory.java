package com.snipersim.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.GL20;

/** Cached primitive meshes and hand-generated textures; no external art required. */
public final class ModelFactory {
    private static final long USAGE = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal;
    private static ModelBuilder builder;
    private static Texture bullseye;
    private static Texture paperTarget;
    private static Texture softDot;

    private ModelFactory() {
    }

    private static ModelBuilder builder() {
        if (builder == null) {
            builder = new ModelBuilder();
        }
        return builder;
    }

    public static Model box(float width, float height, float depth, Color color) {
        Material material = new Material(ColorAttribute.createDiffuse(color));
        return builder().createBox(width, height, depth, material, USAGE);
    }

    public static Model texturedBox(float width, float height, float depth, Texture texture) {
        Material material = new Material(
                TextureAttribute.createDiffuse(texture),
                ColorAttribute.createDiffuse(Color.WHITE),
                ColorAttribute.createEmissive(new Color(0.08f, 0.08f, 0.07f, 1f)),
                IntAttribute.createCullFace(GL20.GL_NONE));
        return builder().createBox(width, height, depth, material, USAGE | VertexAttributes.Usage.TextureCoordinates);
    }

    public static Model cylinder(float radius, float height, Color color) {
        Material material = new Material(ColorAttribute.createDiffuse(color));
        return builder().createCylinder(radius, height, radius, 16, material, USAGE);
    }

    public static Model cone(float radius, float height, Color color) {
        Material material = new Material(ColorAttribute.createDiffuse(color));
        return builder().createCone(radius, height, radius, 14, material, USAGE);
    }

    public static Model sphere(float radius, Color color) {
        Material material = new Material(ColorAttribute.createDiffuse(color));
        return builder().createSphere(radius * 2f, radius * 2f, radius * 2f, 18, 12, material, USAGE);
    }

    public static Model unlitBox(float width, float height, float depth, Color color) {
        Material material = new Material(
                ColorAttribute.createDiffuse(color),
                IntAttribute.createCullFace(GL20.GL_NONE));
        return builder().createBox(width, height, depth, material, USAGE);
    }

    public static Texture bullseye() {
        if (bullseye == null) {
            bullseye = buildBullseye();
        }
        return bullseye;
    }

    /** White high-power card: black aiming bull, white inner rings. */
    public static Texture paperTarget() {
        if (paperTarget == null) {
            paperTarget = buildPaperTarget();
        }
        return paperTarget;
    }

    public static Texture softDot() {
        if (softDot == null) {
            softDot = buildSoftDot();
        }
        return softDot;
    }

    private static Texture buildBullseye() {
        int size = 256;
        Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pixmap.setColor(new Color(0.78f, 0.34f, 0.12f, 1f));
        pixmap.fill();
        int[] radii = {120, 100, 78, 58, 38, 18};
        Color[] colors = {
                new Color(0.93f, 0.93f, 0.88f, 1f),
                new Color(0.13f, 0.13f, 0.13f, 1f),
                new Color(0.93f, 0.93f, 0.88f, 1f),
                new Color(0.13f, 0.13f, 0.13f, 1f),
                new Color(0.93f, 0.93f, 0.88f, 1f),
                new Color(0.85f, 0.22f, 0.10f, 1f)
        };
        for (int i = 0; i < radii.length; i++) {
            pixmap.setColor(colors[i]);
            pixmap.fillCircle(size / 2, size / 2, radii[i]);
        }
        // Weathering: a few dark scuffs so the plate does not look like a decal.
        pixmap.setColor(0.25f, 0.22f, 0.18f, 0.35f);
        for (int i = 0; i < 24; i++) {
            int x = (i * 73 + 31) % size;
            int y = (i * 47 + 11) % size;
            pixmap.fillCircle(x, y, 2 + (i % 4));
        }
        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pixmap.dispose();
        return texture;
    }

    private static Texture buildPaperTarget() {
        int size = 256;
        Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pixmap.setColor(0.94f, 0.93f, 0.86f, 1f);
        pixmap.fill();
        pixmap.setColor(0.05f, 0.05f, 0.05f, 1f);
        pixmap.fillCircle(size / 2, size / 2, 108);
        pixmap.setColor(0.94f, 0.93f, 0.86f, 1f);
        pixmap.fillCircle(size / 2, size / 2, 86);
        pixmap.setColor(0.05f, 0.05f, 0.05f, 1f);
        pixmap.fillCircle(size / 2, size / 2, 64);
        pixmap.setColor(0.94f, 0.93f, 0.86f, 1f);
        pixmap.fillCircle(size / 2, size / 2, 42);
        pixmap.setColor(0.05f, 0.05f, 0.05f, 1f);
        pixmap.fillCircle(size / 2, size / 2, 22);
        pixmap.setColor(0.82f, 0.16f, 0.10f, 1f);
        pixmap.fillCircle(size / 2, size / 2, 8);
        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pixmap.dispose();
        return texture;
    }

    private static Texture buildSoftDot() {
        int size = 128;
        Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pixmap.setColor(0, 0, 0, 0);
        pixmap.fill();
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float dx = (x - size * 0.5f) / (size * 0.5f);
                float dy = (y - size * 0.5f) / (size * 0.5f);
                float r = (float) Math.sqrt(dx * dx + dy * dy);
                float a = Math.max(0, 1f - r);
                a = a * a * (3 - 2 * a);
                pixmap.setColor(1, 1, 1, a);
                pixmap.drawPixel(x, y);
            }
        }
        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pixmap.dispose();
        return texture;
    }

    public static void dispose() {
        if (bullseye != null) {
            bullseye.dispose();
            bullseye = null;
        }
        if (paperTarget != null) {
            paperTarget.dispose();
            paperTarget = null;
        }
        if (softDot != null) {
            softDot.dispose();
            softDot = null;
        }
        if (Gdx.app != null) {
            Gdx.app.log("ModelFactory", "textures released");
        }
    }
}
