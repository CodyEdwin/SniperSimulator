package com.snipersim.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.graphics.Camera;

import java.util.ArrayList;
import java.util.List;

/**
 * A compact precision rifle built from primitives and rigidly attached to the
 * camera. It is cosmetic: the ballistic shot originates at the scope line.
 */
public final class RifleViewModel {
    private static final Color GUNMETAL = new Color(0.115f, 0.125f, 0.13f, 1f);
    private static final Color BLACK = new Color(0.055f, 0.055f, 0.06f, 1f);
    private static final Color TAN = new Color(0.36f, 0.31f, 0.21f, 1f);
    private static final Color GLASS = new Color(0.10f, 0.18f, 0.20f, 1f);

    private static final class Part {
        final ModelInstance instance;
        final Matrix4 local = new Matrix4();

        Part(ModelInstance instance) {
            this.instance = instance;
        }
    }

    private final List<Part> parts = new ArrayList<>();
    private final List<Model> models = new ArrayList<>();
    private final Matrix4 cameraTransform = new Matrix4();
    private final Matrix4 combined = new Matrix4();
    private final Vector3 muzzleLocal = new Vector3(0.16f, -0.16f, -1.20f);
    private final Vector3 muzzleWorld = new Vector3();
    private boolean visible = true;

    public RifleViewModel(Color accent) {
        Color furniture = new Color(TAN).lerp(accent, 0.18f);

        addBox(0.060f, 0.085f, 0.30f, TAN, 0.16f, -0.205f, -0.31f, 0, 0, 0);
        addBox(0.065f, 0.060f, 0.31f, GUNMETAL, 0.16f, -0.150f, -0.45f, 0, 0, 0);
        addBox(0.050f, 0.045f, 0.10f, GUNMETAL, 0.16f, -0.255f, -0.42f, 18, 0, 0);
        addBox(0.048f, 0.100f, 0.11f, BLACK, 0.16f, -0.275f, -0.46f, 9, 0, 0);
        addBox(0.075f, 0.030f, 0.16f, furniture, 0.16f, -0.128f, -0.25f, 0, 0, 0);
        addBox(0.070f, 0.045f, 0.15f, furniture, 0.16f, -0.235f, -0.18f, -4, 0, 0);

        addCylinder(0.011f, 0.56f, GUNMETAL, 0.16f, -0.160f, -0.88f, -90, 0, 0);
        addCylinder(0.019f, 0.10f, BLACK, 0.16f, -0.160f, -1.18f, -90, 0, 0);
        addCylinder(0.023f, 0.26f, BLACK, 0.16f, -0.108f, -0.48f, -90, 0, 0);
        addCylinder(0.033f, 0.08f, BLACK, 0.16f, -0.108f, -0.63f, -90, 0, 0);
        addCylinder(0.029f, 0.07f, BLACK, 0.16f, -0.108f, -0.315f, -90, 0, 0);
        addCylinder(0.025f, 0.012f, GLASS, 0.16f, -0.108f, -0.667f, -90, 0, 0);
        addBox(0.050f, 0.020f, 0.045f, GUNMETAL, 0.16f, -0.132f, -0.55f, 0, 0, 0);
        addBox(0.050f, 0.020f, 0.045f, GUNMETAL, 0.16f, -0.132f, -0.40f, 0, 0, 0);

        addCylinder(0.007f, 0.10f, BLACK, 0.225f, -0.195f, -0.30f, 0, 0, 90);
        addBox(0.055f, 0.014f, 0.014f, GUNMETAL, 0.215f, -0.190f, -0.345f, 0, 0, 0);

        addCylinder(0.006f, 0.30f, BLACK, 0.065f, -0.285f, -0.79f, 12, 0, 22);
        addCylinder(0.006f, 0.30f, BLACK, 0.255f, -0.285f, -0.79f, 12, 0, -22);
        addBox(0.30f, 0.020f, 0.05f, BLACK, 0.16f, -0.245f, -0.70f, 0, 0, 0);

        addCylinder(0.009f, 0.055f, GUNMETAL, 0.16f, -0.078f, -0.42f, 0, 0, 0);
        addCylinder(0.009f, 0.055f, GUNMETAL, 0.16f, -0.078f, -0.52f, 0, 0, 0);
    }

    private void addBox(float w, float h, float d, Color color,
                        float x, float y, float z, float rx, float ry, float rz) {
        Model model = ModelFactory.box(w, h, d, color);
        models.add(model);
        Part part = new Part(new ModelInstance(model));
        part.local.idt().translate(x, y, z).rotate(1, 0, 0, rx).rotate(0, 1, 0, ry).rotate(0, 0, 1, rz);
        parts.add(part);
    }

    private void addCylinder(float radius, float height, Color color,
                             float x, float y, float z, float rx, float ry, float rz) {
        Model model = ModelFactory.cylinder(radius, height, color);
        models.add(model);
        Part part = new Part(new ModelInstance(model));
        part.local.idt().translate(x, y, z).rotate(1, 0, 0, rx).rotate(0, 1, 0, ry).rotate(0, 0, 1, rz);
        parts.add(part);
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean visible() {
        return visible;
    }

    public void update(Camera camera) {
        cameraTransform.set(camera.view).inv();
        for (Part part : parts) {
            combined.set(cameraTransform).mul(part.local);
            part.instance.transform.set(combined);
        }
        muzzleWorld.set(muzzleLocal).mul(cameraTransform);
    }

    public void render(ModelBatch batch, Environment environment) {
        if (!visible) {
            return;
        }
        for (Part part : parts) {
            batch.render(part.instance, environment);
        }
    }

    public Vector3 muzzleWorld() {
        return muzzleWorld;
    }

    public void dispose() {
        for (Model model : models) {
            model.dispose();
        }
        models.clear();
        parts.clear();
    }
}
