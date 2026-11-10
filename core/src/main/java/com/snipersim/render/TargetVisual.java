package com.snipersim.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.snipersim.world.RangeTarget;

import java.util.ArrayList;
import java.util.List;

/** Visual rig for one target: steel face, bullseye decal and an A-frame stand. */
public final class TargetVisual {
    private static final Color STEEL = new Color(0.25f, 0.24f, 0.22f, 1f);
    private static final Color STAND = new Color(0.16f, 0.15f, 0.14f, 1f);

    private final RangeTarget target;
    private final List<ModelInstance> parts = new ArrayList<>();
    private final List<Vector3> offsets = new ArrayList<>();
    private final List<Model> ownedModels = new ArrayList<>();
    private final Matrix4 group = new Matrix4();
    private final Matrix4 local = new Matrix4();
    private final Vector3 tmp = new Vector3();

    public TargetVisual(RangeTarget target, float groundY) {
        this.target = target;
        Color paint = target.paint;

        if (target.kind == RangeTarget.Kind.GONG) {
            Texture faceTexture = target.paper ? ModelFactory.paperTarget() : ModelFactory.bullseye();
            Model face = ModelFactory.texturedBox(target.width, target.height, 0.055f, faceTexture);
            ownedModels.add(face);
            add(face, 0, 0, 0);

            float standTop = target.baseY - target.height * 0.5f;
            float standHeight = Math.max(0.2f, standTop - groundY);
            Model post = ModelFactory.box(0.09f, standHeight, 0.09f, STAND);
            ownedModels.add(post);
            add(post, 0, -target.height * 0.5f - standHeight * 0.5f, -0.03f);

            Model footA = ModelFactory.box(0.06f, standHeight * 0.75f, 0.06f, STAND);
            ownedModels.add(footA);
            add(footA, -0.28f, -target.height * 0.5f - standHeight * 0.30f, -0.10f);
            Model footB = ModelFactory.box(0.06f, standHeight * 0.75f, 0.06f, STAND);
            ownedModels.add(footB);
            add(footB, 0.28f, -target.height * 0.5f - standHeight * 0.30f, -0.10f);

            Model cross = ModelFactory.box(Math.min(target.width * 1.15f, 1.4f), 0.05f, 0.05f, STAND);
            ownedModels.add(cross);
            add(cross, 0, -target.height * 0.5f - standHeight + 0.02f, -0.04f);
        } else {
            // Keep every visual part inside the scoring AABB: the plate is
            // height tall from baseY - h/2 to baseY + h/2, so the torso and
            // head are laid out to finish just inside that envelope.
            float bodyHeight = target.height * 0.72f;
            float bodyWidth = target.width * 0.62f;
            Model body = ModelFactory.box(bodyWidth, bodyHeight, 0.07f, paint);
            ownedModels.add(body);
            add(body, 0, -target.height * 0.10f, 0);

            Model shoulders = ModelFactory.box(bodyWidth * 1.55f, bodyHeight * 0.14f, 0.07f, paint);
            ownedModels.add(shoulders);
            add(shoulders, 0, target.height * 0.29f, 0);

            Model head = ModelFactory.sphere(target.height * 0.07f, paint);
            ownedModels.add(head);
            add(head, 0, target.height * 0.40f, 0);

            Model chest = ModelFactory.texturedBox(bodyWidth * 0.46f, bodyHeight * 0.46f, 0.012f,
                    ModelFactory.bullseye());
            ownedModels.add(chest);
            add(chest, 0, -target.height * 0.10f, 0.046f);

            float bodyBottom = target.height * 0.46f;
            float standTop = target.baseY - bodyBottom;
            float standHeight = Math.max(0.15f, standTop - groundY);
            Model post = ModelFactory.box(0.10f, standHeight, 0.10f, STAND);
            ownedModels.add(post);
            add(post, 0, -bodyBottom - standHeight * 0.5f, -0.04f);

            Model foot = ModelFactory.box(0.9f, 0.08f, 0.5f, STAND);
            ownedModels.add(foot);
            add(foot, 0, -target.height * 0.5f - standHeight + 0.02f, -0.06f);
        }
    }

    private void add(Model model, float x, float y, float z) {
        ModelInstance instance = new ModelInstance(model);
        parts.add(instance);
        offsets.add(new Vector3(x, y, z));
    }

    public void update(double time) {
        float x = target.xAt(time);
        group.idt()
                .translate(x, target.baseY, target.baseZ)
                .rotate(0, 0, 1, target.wobble * 57.29578f);
        for (int i = 0; i < parts.size(); i++) {
            Vector3 offset = offsets.get(i);
            local.idt().translate(offset.x, offset.y, offset.z);
            parts.get(i).transform.set(group).mul(local);
        }
    }

    public void render(com.badlogic.gdx.graphics.g3d.ModelBatch batch,
                       com.badlogic.gdx.graphics.g3d.Environment environment) {
        if (!target.active) {
            return;
        }
        for (ModelInstance part : parts) {
            batch.render(part, environment);
        }
    }

    public Vector3 centerAt(double time, Vector3 out) {
        target.centerAt(time, out);
        return out;
    }

    public RangeTarget target() {
        return target;
    }

    public void dispose() {
        for (Model model : ownedModels) {
            model.dispose();
        }
        ownedModels.clear();
        parts.clear();
        offsets.clear();
    }
}
