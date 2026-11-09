package com.snipersim.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.model.MeshPart;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.model.NodePart;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.snipersim.ballistics.TerrainFunction;
import com.snipersim.util.BallisticMath;
import com.snipersim.util.Noise;

/**
 * The range is a hand-shaped heightfield: a raised firing ridge, a deep valley,
 * a long far slope for 2 km targets, and rolling side spurs. Heights are
 * analytic, so physics, target placement and the render mesh always agree.
 */
public final class Terrain implements TerrainFunction {
    public static final float MIN_X = -1800f;
    public static final float MAX_X = 1800f;
    public static final float NEAR_Z = 500f;
    public static final float FAR_Z = -3300f;
    public static final int SEGMENTS_X = 180;
    public static final int CHUNK_Z = 40;
    public static final int CHUNKS = 6;

    private final Model terrainModel;
    private final Model mountainModel;
    private final Texture groundTexture;
    private final Texture mountainTexture;

    public Terrain() {
        groundTexture = buildGroundTexture();
        mountainTexture = buildMountainTexture();
        terrainModel = buildTerrainModel();
        mountainModel = buildMountainRing();
    }

    @Override
    public double heightAt(double x, double z) {
        double ridge = 26.0 * Math.exp(-(z - 60.0) * (z - 60.0) / (2.0 * 150.0 * 150.0))
                * Math.exp(-(x * x) / (2.0 * 950.0 * 950.0));
        double valley = -13.0 * Math.exp(-(z + 640.0) * (z + 640.0) / (2.0 * 700.0 * 700.0));
        double farSlope = 34.0 * Math.exp(-(z + 1780.0) * (z + 1780.0) / (2.0 * 820.0 * 820.0))
                * (0.86 + 0.18 * Noise.fbm2D(x * 0.0011, 0.7, 3, 2.0, 0.5));
        double sideSpurs = 18.0
                * smoothstep(430.0, 1500.0, Math.abs(x))
                * (0.35 + 0.65 * Noise.ridge2D(x * 0.0016, z * 0.0021, 4));
        double rolling = 5.5 * Noise.fbm2D(x * 0.0042, z * 0.0042, 4, 2.02, 0.52);
        double detail = 1.25 * Noise.value2D(x * 0.021, z * 0.021);
        double base = 18.0;
        return base + ridge + valley + farSlope + sideSpurs + rolling + detail;
    }

    private static double smoothstep(double edge0, double edge1, double x) {
        double t = BallisticMath.clamp((x - edge0) / (edge1 - edge0), 0, 1);
        return t * t * (3 - 2 * t);
    }

    public float height(float x, float z) {
        return (float) heightAt(x, z);
    }

    /** Shooting ridge position, eye-level reference. */
    public float shooterGround() {
        return height(0, 0);
    }

    public Model terrainModel() {
        return terrainModel;
    }

    public Model mountainModel() {
        return mountainModel;
    }

    private Model buildTerrainModel() {
        int nx = SEGMENTS_X;
        int chunkRows = CHUNK_Z;
        int floatsPerVertex = 3 + 3 + 4 + 2;
        Material material = new Material(
                TextureAttribute.createDiffuse(groundTexture),
                ColorAttribute.createDiffuse(Color.WHITE),
                IntAttribute.createCullFace(GL20.GL_NONE));
        Model model = new Model();
        model.materials.add(material);

        float dx = (MAX_X - MIN_X) / nx;
        float dz = (FAR_Z - NEAR_Z) / (chunkRows * CHUNKS);

        // The heightfield is split into short-index chunks: 181 * 41 vertices
        // and 43,200 indices each keeps us under the 65k index limit.
        for (int chunk = 0; chunk < CHUNKS; chunk++) {
            int verts = (nx + 1) * (chunkRows + 1);
            float[] data = new float[verts * floatsPerVertex];
            short[] indices = new short[nx * chunkRows * 6];
            int p = 0;
            for (int iz = 0; iz <= chunkRows; iz++) {
                int globalZ = chunk * chunkRows + iz;
                float z = NEAR_Z + dz * globalZ;
                for (int ix = 0; ix <= nx; ix++) {
                    float x = MIN_X + dx * ix;
                    float y = height(x, z);
                    double e = 2.0;
                    double hL = heightAt(x - e, z);
                    double hR = heightAt(x + e, z);
                    double hD = heightAt(x, z - e);
                    double hU = heightAt(x, z + e);
                    double normX = -(hR - hL) / (2 * e);
                    double normZ = -(hU - hD) / (2 * e);
                    double normY = 1.0;
                    double len = Math.sqrt(normX * normX + normY * normY + normZ * normZ);
                    normX /= len;
                    normY /= len;
                    normZ /= len;

                    Color color = terrainColor(x, y, z);
                    data[p++] = x;
                    data[p++] = y;
                    data[p++] = z;
                    data[p++] = (float) normX;
                    data[p++] = (float) normY;
                    data[p++] = (float) normZ;
                    data[p++] = color.r;
                    data[p++] = color.g;
                    data[p++] = color.b;
                    data[p++] = color.a;
                    data[p++] = x / 34f;
                    data[p++] = z / 34f;
                }
            }

            int q = 0;
            for (int iz = 0; iz < chunkRows; iz++) {
                for (int ix = 0; ix < nx; ix++) {
                    int a = iz * (nx + 1) + ix;
                    int b = a + 1;
                    int c = a + (nx + 1);
                    int d = c + 1;
                    indices[q++] = (short) a;
                    indices[q++] = (short) c;
                    indices[q++] = (short) b;
                    indices[q++] = (short) b;
                    indices[q++] = (short) c;
                    indices[q++] = (short) d;
                }
            }

            Mesh mesh = new Mesh(false, verts, indices.length,
                    VertexAttribute.Position(),
                    VertexAttribute.Normal(),
                    VertexAttribute.ColorUnpacked(),
                    VertexAttribute.TexCoords(0));
            mesh.setVertices(data);
            mesh.setIndices(indices);

            String id = "range" + chunk;
            MeshPart part = new MeshPart(id, mesh, 0, indices.length, GL20.GL_TRIANGLES);
            part.primitiveType = GL20.GL_TRIANGLES;
            model.meshParts.add(part);
            model.meshes.add(mesh);
            Node node = new Node();
            node.id = id;
            node.parts.add(new NodePart(part, material));
            model.nodes.add(node);
        }
        return model;
    }

    private Color terrainColor(float x, float y, float z) {
        Color lowGrass = new Color(0.29f, 0.34f, 0.20f, 1f);
        Color dryGrass = new Color(0.49f, 0.43f, 0.25f, 1f);
        Color dirt = new Color(0.37f, 0.30f, 0.20f, 1f);
        Color rock = new Color(0.43f, 0.41f, 0.37f, 1f);

        double moisture = Noise.fbm2D(x * 0.009, z * 0.009, 4, 2.03, 0.5) * 0.5 + 0.5;
        double slope = Math.abs(heightAt(x + 3, z) - heightAt(x - 3, z)) * 0.5
                + Math.abs(heightAt(x, z + 3) - heightAt(x, z - 3)) * 0.5;
        float rockMix = (float) BallisticMath.clamp(slope * 0.65 + (y - 48) * 0.010, 0, 0.78);
        float dryMix = (float) BallisticMath.clamp(moisture * 0.9 - 0.15, 0, 0.72);

        Color color = new Color(lowGrass).lerp(dryGrass, dryMix);
        color.lerp(dirt, (float) BallisticMath.clamp((moisture - 0.72) * 1.8, 0, 0.45));
        color.lerp(rock, rockMix);
        color.mul(0.88f + 0.18f * (float) Noise.value2D(x * 0.11, z * 0.11));

        double dist = Math.hypot(x, z);
        float haze = (float) (smoothstep(650, 3000, dist) * 0.62);
        color.lerp(new Color(0.60f, 0.66f, 0.68f, 1f), haze);
        color.a = 1f;
        return color;
    }

    private Model buildMountainRing() {
        int segments = 220;
        int verts = (segments + 1) * 2;
        float[] data = new float[verts * 12];
        short[] indices = new short[segments * 6];
        double centerZ = -700.0;
        double radius = 4300.0;

        int p = 0;
        for (int i = 0; i <= segments; i++) {
            double a = (Math.PI * 2.0 * i) / segments;
            double x = Math.cos(a) * radius;
            double z = centerZ + Math.sin(a) * radius;
            double ridge = Noise.ridge2D(Math.cos(a) * 3.1 + 10, Math.sin(a) * 3.1 + 10, 5);
            double peaks = Noise.fbm2D(Math.cos(a) * 1.7 + 4, Math.sin(a) * 1.7 + 4, 4, 2, 0.55);
            float top = (float) (90 + ridge * 260 + peaks * 120);
            float bottom = -90;

            float inset = (float) (0.44 + 0.10 * Noise.value2D(i * 0.22, 1.7));
            Color topColor = new Color(0.43f + inset * 0.16f, 0.50f + inset * 0.15f, 0.58f + inset * 0.13f, 1f);
            Color bottomColor = new Color(0.62f, 0.67f, 0.69f, 1f);

            data[p++] = (float) x;
            data[p++] = bottom;
            data[p++] = (float) z;
            data[p++] = (float) -Math.cos(a);
            data[p++] = 0;
            data[p++] = (float) -Math.sin(a);
            data[p++] = bottomColor.r;
            data[p++] = bottomColor.g;
            data[p++] = bottomColor.b;
            data[p++] = 1;
            data[p++] = 0;
            data[p++] = 0;

            data[p++] = (float) x;
            data[p++] = top;
            data[p++] = (float) z;
            data[p++] = (float) -Math.cos(a);
            data[p++] = 0.2f;
            data[p++] = (float) -Math.sin(a);
            data[p++] = topColor.r;
            data[p++] = topColor.g;
            data[p++] = topColor.b;
            data[p++] = 1;
            data[p++] = 0;
            data[p++] = 1;
        }

        int q = 0;
        for (int i = 0; i < segments; i++) {
            int a = i * 2;
            int b = a + 1;
            int c = a + 2;
            int d = a + 3;
            indices[q++] = (short) a;
            indices[q++] = (short) c;
            indices[q++] = (short) b;
            indices[q++] = (short) b;
            indices[q++] = (short) c;
            indices[q++] = (short) d;
        }

        Mesh mesh = new Mesh(false, verts, indices.length,
                VertexAttribute.Position(),
                VertexAttribute.Normal(),
                VertexAttribute.ColorUnpacked(),
                VertexAttribute.TexCoords(0));
        mesh.setVertices(data);
        mesh.setIndices(indices);

        Model model = new Model();
        MeshPart part = new MeshPart("mountains", mesh, 0, indices.length, GL20.GL_TRIANGLES);
        part.primitiveType = GL20.GL_TRIANGLES;
        model.meshParts.add(part);
        model.meshes.add(mesh);
        Material material = new Material(
                TextureAttribute.createDiffuse(mountainTexture),
                ColorAttribute.createDiffuse(Color.WHITE),
                IntAttribute.createCullFace(GL20.GL_NONE));
        model.materials.add(material);
        Node node = new Node();
        node.id = "mountains";
        node.parts.add(new NodePart(part, material));
        model.nodes.add(node);
        return model;
    }

    private Texture buildGroundTexture() {
        int size = 256;
        Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                double n = Noise.fbm2D(x * 0.055, y * 0.055, 4, 2.02, 0.52) * 0.5 + 0.5;
                double fine = Noise.value2D(x * 0.33, y * 0.33) * 0.5 + 0.5;
                double patch = Noise.fbm2D(x * 0.012 + 40, y * 0.012 + 40, 3, 2, 0.5) * 0.5 + 0.5;
                float r = (float) (0.36 + n * 0.18 + fine * 0.05);
                float g = (float) (0.39 + n * 0.20 + fine * 0.05);
                float b = (float) (0.27 + n * 0.12 + fine * 0.04);
                if (patch > 0.62) {
                    r = (float) (0.44 + n * 0.12);
                    g = (float) (0.38 + n * 0.10);
                    b = (float) (0.27 + n * 0.08);
                }
                pixmap.setColor(r, g, b, 1f);
                pixmap.drawPixel(x, y);
            }
        }
        Texture texture = new Texture(pixmap, true);
        texture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
        texture.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
        pixmap.dispose();
        return texture;
    }

    private Texture buildMountainTexture() {
        int size = 64;
        Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float n = (float) (Noise.value2D(x * 0.22, y * 0.22) * 0.5 + 0.5);
                pixmap.setColor(0.76f + n * 0.10f, 0.80f + n * 0.08f, 0.83f + n * 0.06f, 1f);
                pixmap.drawPixel(x, y);
            }
        }
        Texture texture = new Texture(pixmap, true);
        texture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
        texture.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
        pixmap.dispose();
        return texture;
    }

    public void dispose() {
        terrainModel.dispose();
        mountainModel.dispose();
        groundTexture.dispose();
        mountainTexture.dispose();
    }

    public static void log(String message) {
        if (Gdx.app != null) {
            Gdx.app.log("Terrain", message);
        }
    }
}
