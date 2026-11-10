package com.snipersim.screens;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.snipersim.SniperGame;
import com.snipersim.audio.ProceduralAudio;
import com.snipersim.ballistics.BallisticTable;
import com.snipersim.ballistics.BallisticsEngine;
import com.snipersim.ballistics.Trajectory;
import com.snipersim.ballistics.WindModel;
import com.snipersim.ballistics.ZeroSolver;
import com.snipersim.input.LookInput;
import com.snipersim.input.TouchLayout;
import com.snipersim.render.Hud;
import com.snipersim.render.HudState;
import com.snipersim.render.SceneRenderer;
import com.snipersim.render.ScopeRenderer;
import com.snipersim.util.BallisticMath;
import com.snipersim.weapons.Loadout;
import com.snipersim.weapons.Weapon;
import com.snipersim.weapons.WeaponCatalog;
import com.snipersim.world.RangeCourse;
import com.snipersim.world.RangeTarget;
import com.snipersim.world.Shot;
import com.snipersim.world.World;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * The firing point. Owns the camera, shooter state, weapon handling, shot
 * resolution, scope animation and all interaction.
 */
public final class GameScreen extends ScreenAdapter {
    private static final Vector3 WORLD_UP = new Vector3(0, 1, 0);
    private static final float BASE_FOV = 68f;

    private final SniperGame game;
    private final boolean smokeTest;
    private final boolean android;
    private final World world;
    private final SceneRenderer renderer;
    private final ScopeRenderer scopeRenderer;
    private final Hud hud;
    private final ProceduralAudio audio;
    private final PerspectiveCamera camera;
    private final Vector3 shooterEye;
    private final Vector3 currentAim = new Vector3(0, 0, -1);
    private final Vector3 right = new Vector3(1, 0, 0);
    private final Vector3 localUp = new Vector3(0, 1, 0);
    private final RandomXS128 random = new RandomXS128(0xC0FFEEL);

    private final List<Shot> shots = new ArrayList<>();
    private final HudState hudState = new HudState();
    private final BoundingBox scratchBox = new BoundingBox();

    private Loadout loadout;
    private Weapon weapon;
    private int weaponIndex;
    private int zeroDistance = 100;
    private int elevationClicks;
    private int windageClicks;
    private double holdoverMils;
    private double zeroAngle;
    private double zoom;
    private float scopeAmount;
    private boolean scopedInput;
    private boolean boltReady = true;
    private float boltTimer;
    private float boltSoundTimer;

    private float yaw;
    private float pitch = -0.025f;
    private float recoilPitch;
    private float recoilYaw;
    private float recoilPitchVelocity;
    private float recoilYawVelocity;
    private float swayTime;
    private float breath = 1f;
    private boolean holdingBreath;
    private float timeSinceShot = 4f;
    private float muzzleFlash;
    private float screenFlash;
    private float hitMarker;
    private float missMarker;
    private float toastTimer;
    private boolean toastHit;
    private String toastTitle = "";
    private String toastDetail = "";
    private String missText = "";
    private float rangeTimer;
    private boolean rangeValid;
    private double rangeMeters;
    private int highlightRange;

    private boolean briefing = true;
    private boolean paused;
    private boolean helpVisible;
    private boolean cursorCaptured;
    private final Vector2 lookDelta = new Vector2();
    private final boolean[] controlPointer = new boolean[12];
    private int adsPointer = -1;
    private float lastPointerX;
    private float lastPointerY;
    private float lastMouseX;
    private float lastMouseY;
    private boolean pointerDown;
    private boolean mouseSeen;
    private int turretCooldown;
    private int pendingZoom;

    private boolean bulletCamArmed;
    private boolean bulletCamActive;
    private Shot followedShot;
    private float bulletCamHold;
    private final Vector3 bulletCamPos = new Vector3();
    private final Vector3 bulletCamVel = new Vector3();

    private int score;
    private int matchStage;
    private int matchShots;
    private float matchTime;
    private int matchScore;
    private int matchX;
    private boolean matchDone;
    private String[] rangeCard = new String[0];
    private int shotCount;
    private int hitCount;
    private int bestRing;
    private double longestHit;
    private double bestEnergy;
    private BallisticTable dope;
    private boolean dopeDirty = true;

    private int frame;
    private int lastGlError;

    private final InputAdapter scrollAdapter = new InputAdapter() {
        @Override
        public boolean scrolled(float amountX, float amountY) {
            if (!briefing && !paused && !bulletCamActive) {
                pendingZoom += amountY > 0 ? -1 : 1;
            }
            return true;
        }

        @Override
        public boolean touchDown(int screenX, int screenY, int pointer, int button) {
            if (!smokeTest && com.snipersim.render.ArmoryButton.hit(screenX, screenY,
                    Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), paused || briefing)) {
                openArmory();
                return true;
            }
            if (!android) {
                return false;
            }
            if (!briefing && !paused && pressTouchControl(screenX, screenY, pointer, button)) {
                return true;
            }
            if (pointer == 0) {
                lastPointerX = screenX;
                lastPointerY = screenY;
                lastMouseX = screenX;
                lastMouseY = screenY;
                pointerDown = true;
                mouseSeen = true;
            }
            return false;
        }

        @Override
        public boolean touchDragged(int screenX, int screenY, int pointer) {
            if (android && pointer == 0 && !isControlPointer(pointer)) {
                if (pointerDown) {
                    LookInput.addDelta(screenX - lastPointerX, screenY - lastPointerY);
                }
                lastPointerX = screenX;
                lastPointerY = screenY;
                lastMouseX = screenX;
                lastMouseY = screenY;
                pointerDown = true;
                mouseSeen = true;
            }
            return false;
        }

        @Override
        public boolean touchUp(int screenX, int screenY, int pointer, int button) {
            if (!android) {
                return false;
            }
            if (button == Input.Buttons.RIGHT) {
                LookInput.setMouseScope(false);
            }
            if (pointer == adsPointer) {
                adsPointer = -1;
                LookInput.setTouchScope(false);
            }
            clearControlPointer(pointer);
            if (pointer == 0) {
                pointerDown = false;
            }
            return false;
        }

        @Override
        public boolean mouseMoved(int screenX, int screenY) {
            // With pointer capture the system sends relative deltas to the
            // capture listener instead, and the cursor cannot reach an edge.
            if (android && !LookInput.isPointerCaptured()) {
                if (mouseSeen) {
                    float dx = screenX - lastMouseX;
                    float dy = screenY - lastMouseY;
                    // Ignore teleport-sized jumps when the pointer re-enters.
                    if (Math.abs(dx) < 300 && Math.abs(dy) < 300) {
                        LookInput.addDelta(dx, dy);
                    }
                }
                lastMouseX = screenX;
                lastMouseY = screenY;
                mouseSeen = true;
            }
            return false;
        }
    };

    public GameScreen(SniperGame game, boolean smokeTest) {
        this(game, smokeTest, Loadout.issued());
    }

    public GameScreen(SniperGame game, boolean smokeTest, Loadout loadout) {
        this.game = game;
        this.smokeTest = smokeTest;
        this.loadout = loadout.copy();
        this.android = Gdx.app.getType() == Application.ApplicationType.Android;
        world = new World(this.loadout.range);
        if (world.course.competition) {
            matchTime = MATCH[0].seconds;
        }
        buildRangeCard();
        shooterEye = new Vector3(world.shooterEye);
        camera = new PerspectiveCamera(BASE_FOV, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(shooterEye);
        camera.near = 0.04f;
        camera.far = 6500f;
        camera.up.set(WORLD_UP);
        camera.direction.set(0, 0, -1);
        camera.update();

        weapon = this.loadout.build();
        weaponIndex = this.loadout.rifleIndex;
        zeroDistance = this.loadout.zeroDistance;
        zoom = weapon.scopeDefaultZoom;

        renderer = new SceneRenderer(weapon.accent);
        scopeRenderer = new ScopeRenderer();
        scopeRenderer.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        hud = new Hud();
        audio = new ProceduralAudio();
        recomputeZero();
        updateAim();
        camera.direction.set(currentAim);
        camera.update();
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(scrollAdapter);
        if (!smokeTest) {
            Gdx.input.setCursorCatched(true);
            cursorCaptured = true;
        }
        audio.boltCycle();
    }

    @Override
    public void hide() {
        Gdx.input.setCursorCatched(false);
        cursorCaptured = false;
    }

    @Override
    public void resize(int width, int height) {
        camera.viewportWidth = Math.max(1, width);
        camera.viewportHeight = Math.max(1, height);
        camera.update();
        scopeRenderer.resize(Math.max(1, width), Math.max(1, height));
    }

    @Override
    public void render(float delta) {
        frame++;
        delta = Math.min(delta, 0.05f);
        width = Math.max(1, Gdx.graphics.getWidth());
        height = Math.max(1, Gdx.graphics.getHeight());

        handleGlobalInput();
        if (!android) {
            // Scope is sampled first. A right-button edge in this frame clears
            // any fire edge, including a backend that also reports primary.
            LookInput.setMouseScope(Gdx.input.isButtonPressed(Input.Buttons.RIGHT));
            LookInput.setMouseFire(Gdx.input.isButtonPressed(Input.Buttons.LEFT));
        }

        boolean interactive = !briefing && !paused && !bulletCamActive;
        if (interactive) {
            updateMouseLook();
        } else {
            Gdx.input.getDeltaX();
            Gdx.input.getDeltaY();
            LookInput.consume(lookDelta);
            LookInput.consumeScroll();
            LookInput.consumeFireEdge();
        }

        updateBreathing(delta);
        updateRecoil(delta);
        updateScope(delta, interactive);
        updateAim();
        updateCameraFov();
        updateCameraFromAim();

        if (interactive) {
            handleGameplayInput(delta);
        } else {
            LookInput.consumeFireEdge();
        }
        LookInput.endFrame();

        if (!paused && !briefing && world.course.competition && !matchDone) {
            matchTime -= delta;
            if (matchTime <= 0) {
                advanceMatch();
            }
        }
        if (!paused) {
            updateWeapon(delta);
            updateShots(delta);
            world.update(delta);
            renderer.update(delta);
        }
        updateFeedback(delta);
        updateHudData();

        if (bulletCamActive) {
            updateBulletCamera(delta);
        }

        if (smokeTest) {
            updateSmokeTest();
        }

        renderFrame(delta);
    }

    private int width;
    private int height;

    // ------------------------------------------------------------- input

    private void handleGlobalInput() {
        boolean start = Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
                || (android && Gdx.input.justTouched());
        if (start && briefing) {
            briefing = false;
            LookInput.consumeFireEdge();
            LookInput.cancelUnconfirmedFire();
            if (!smokeTest) {
                captureMouse();
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (!briefing) {
                if (paused) {
                    paused = false;
                    captureMouse();
                } else {
                    paused = true;
                    endBulletCam();
                    releaseMouse();
                }
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.H) && !briefing) {
            helpVisible = !helpVisible;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.C) && !smokeTest) {
            openArmory();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.V)) {
            if (bulletCamActive) {
                endBulletCam();
                showToast(false, "BULLET CAM OFF", "", "");
                toastTimer = 1.2f;
            } else if (!briefing && !paused) {
                handleBulletCamKey();
            }
        }
        if (paused && !briefing) {
            boolean clicked = Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)
                    || (android && Gdx.input.justTouched());
            if (clicked) {
                paused = false;
                captureMouse();
                LookInput.consumeFireEdge();
                LookInput.cancelUnconfirmedFire();
            }
        }
    }

    private void updateMouseLook() {
        if (!cursorCaptured) {
            return;
        }
        float sensitivity = 0.0019f * (camera.fieldOfView / BASE_FOV);
        if (android) {
            LookInput.consume(lookDelta);
            yaw += lookDelta.x * sensitivity;
            pitch -= lookDelta.y * sensitivity;
        } else {
            yaw += Gdx.input.getDeltaX() * sensitivity;
            pitch -= Gdx.input.getDeltaY() * sensitivity;
        }
        pitch = MathUtils.clamp(pitch, -0.38f, 0.38f);
        yaw = wrapAngle(yaw);
    }

    private boolean pressTouchControl(int screenX, int screenY, int pointer, int button) {
        if (button == Input.Buttons.RIGHT) {
            LookInput.setMouseScope(true);
            return false;
        }
        TouchLayout layout = TouchLayout.of(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        if (layout.hitAds(screenX, screenY)) {
            adsPointer = pointer;
            LookInput.setTouchScope(true);
            markControlPointer(pointer);
            return true;
        }
        if (layout.hitFire(screenX, screenY)) {
            LookInput.pulseFire();
            markControlPointer(pointer);
            return true;
        }
        if (layout.hitZoomIn(screenX, screenY)) {
            pendingZoom += 1;
            markControlPointer(pointer);
            return true;
        }
        if (layout.hitZoomOut(screenX, screenY)) {
            pendingZoom -= 1;
            markControlPointer(pointer);
            return true;
        }
        if (LookInput.isScopeDown()) {
            LookInput.pulseFire();
        }
        return false;
    }

    private void markControlPointer(int pointer) {
        if (pointer >= 0 && pointer < controlPointer.length) {
            controlPointer[pointer] = true;
        }
    }

    private void clearControlPointer(int pointer) {
        if (pointer >= 0 && pointer < controlPointer.length) {
            controlPointer[pointer] = false;
        }
    }

    private boolean isControlPointer(int pointer) {
        return pointer >= 0 && pointer < controlPointer.length && controlPointer[pointer];
    }

    private void handleGameplayInput(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.Z)) {
            cycleZero();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F5)) {
            world.rerollWind();
            recomputeZero();
            dopeDirty = true;
            showToast(false, "CONDITIONS UPDATED",
                    String.format(Locale.ROOT, "WIND %.1f M/S FROM %03.0f°", world.wind.speedAt(world.missionTime, 10),
                            world.wind.bearingAt(world.missionTime)), "");
            toastTimer = 2.0f;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            showToast(false, "UNLIMITED", "NO RELOAD  ·  MAGAZINE IS A REFERENCE", "");
            toastTimer = 1.4f;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.L)) {
            laserRange();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT_BRACKET)) {
            adjustHoldover(-1.0);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT_BRACKET)) {
            adjustHoldover(1.0);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.PAGE_DOWN)) {
            adjustHoldover(-5.0);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.PAGE_UP)) {
            adjustHoldover(5.0);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.BACKSLASH)) {
            setHoldover(0, true);
        }

        for (int i = 0; i < WeaponCatalog.all().size(); i++) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1 + i)
                    || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_1 + i)) {
                switchWeapon(i);
            }
        }

        float wheel = LookInput.consumeScroll();
        if (wheel != 0) {
            pendingZoom += wheel > 0 ? -1 : 1;
        }

        if (pendingZoom != 0) {
            zoom = BallisticMath.clamp(zoom + pendingZoom, weapon.scopeMinZoom, weapon.scopeMaxZoom);
            pendingZoom = 0;
        }

        if (turretCooldown > 0) {
            turretCooldown--;
        }
        if (turretCooldown <= 0) {
            int adjust = 0;
            if (Gdx.input.isKeyPressed(Input.Keys.UP)) {
                adjust += 1;
            }
            if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
                adjust -= 1;
            }
            if (adjust != 0) {
                elevationClicks = (int) BallisticMath.clamp(elevationClicks + adjust, -300, 1200);
                turretCooldown = 3;
                audio.turretClick();
            }
            int windAdjust = 0;
            if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
                windAdjust += 1;
            }
            if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
                windAdjust -= 1;
            }
            if (windAdjust != 0) {
                windageClicks = (int) BallisticMath.clamp(windageClicks + windAdjust, -300, 300);
                turretCooldown = 3;
                audio.turretClick();
            }
        }

        if (LookInput.consumeFireEdge()) {
            fire();
        }
    }

    private void updateBreathing(float delta) {
        boolean wantsHold = (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)
                || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT)) && !paused && !briefing;
        holdingBreath = wantsHold && breath > 0.02f;
        if (holdingBreath) {
            breath = Math.max(0, breath - delta * 0.19f);
            if (breath <= 0.01f) {
                holdingBreath = false;
            }
        } else {
            breath = Math.min(1, breath + delta * 0.11f);
        }
    }

    private void updateRecoil(float delta) {
        recoilPitchVelocity += (-recoilPitch * 90f - recoilPitchVelocity * 13f) * delta;
        recoilYawVelocity += (-recoilYaw * 90f - recoilYawVelocity * 13f) * delta;
        recoilPitch += recoilPitchVelocity * delta;
        recoilYaw += recoilYawVelocity * delta;
        timeSinceShot += delta;
    }

    private void updateScope(float delta, boolean interactive) {
        scopedInput = interactive && LookInput.isScopeDown();
        if (smokeTest && frame > 5) {
            scopedInput = true;
        }
        float target = scopedInput ? 1f : 0f;
        scopeAmount += (target - scopeAmount) * Math.min(1f, delta * 9f);
        if (scopeAmount < 0.001f) {
            scopeAmount = 0;
        }
        zoom = BallisticMath.clamp(zoom, weapon.scopeMinZoom, weapon.scopeMaxZoom);
    }

    private void updateAim() {
        float stability = 1f;
        if (timeSinceShot < 1.3f) {
            stability = 1.25f;
        }
        float swayScale = world.course.competition && !briefing && !matchDone
                ? MATCH[matchStage].sway : 1f;
        float amp = 0.00022f * stability * swayScale
                * (holdingBreath ? 0.16f : 1f)
                * (1f + (1f - breath) * 1.5f);
        float swayYaw = amp * ((float) Math.sin(swayTime * 1.13 + 0.4)
                + 0.45f * (float) Math.sin(swayTime * 2.77 + 1.1)
                + 0.14f * (float) Math.sin(swayTime * 4.9));
        float swayPitch = amp * (0.8f * (float) Math.cos(swayTime * 0.93 + 0.2)
                + 0.38f * (float) Math.sin(swayTime * 3.17 + 0.7));
        if (breath < 0.55f) {
            float heartbeat = (0.55f - breath) * 0.00035f
                    * (float) Math.sin(swayTime * Math.PI * 2.0 * 1.25);
            swayYaw += heartbeat * 0.4f;
            swayPitch += heartbeat;
        }
        swayTime += Gdx.graphics.getDeltaTime();

        double effectiveYaw = yaw + recoilYaw + swayYaw;
        double effectivePitch = pitch + recoilPitch + swayPitch;
        float cp = (float) Math.cos(effectivePitch);
        currentAim.set((float) (Math.sin(effectiveYaw) * cp),
                (float) Math.sin(effectivePitch),
                (float) (-Math.cos(effectiveYaw) * cp)).nor();
        right.set(currentAim).crs(WORLD_UP).nor();
        localUp.set(right).crs(currentAim).nor();
    }

    private void updateCameraFov() {
        double scopedFov = Math.toDegrees(2.0 * Math.atan(
                Math.tan(Math.toRadians(BASE_FOV * 0.5)) / Math.max(1.0, zoom)));
        float fov = (float) (BASE_FOV + (scopedFov - BASE_FOV) * scopeAmount);
        camera.fieldOfView = fov;
        camera.viewportWidth = width;
        camera.viewportHeight = height;
    }

    private void updateCameraFromAim() {
        if (bulletCamActive) {
            return;
        }
        camera.position.set(shooterEye);
        camera.direction.set(currentAim);
        camera.up.set(WORLD_UP);
        camera.update();
    }

    // -------------------------------------------------------------- shooting

    private void fire() {
        if (briefing || paused || !boltReady || bulletCamActive) {
            return;
        }
        if (world.course.competition && (matchDone || matchTime <= 0
                || matchShots >= MATCH[matchStage].shots)) {
            return;
        }

        Vector3 forward = new Vector3(currentAim);
        Vector3 side = new Vector3(forward).crs(WORLD_UP).nor();
        Vector3 up = new Vector3(side).crs(forward).nor();
        // A positive holdover means the shooter is placing a mark below the
        // target on the reticle, which raises the barrel relative to the target.
        float elevation = (float) (zeroAngle + elevationClicks * 0.0001
                + holdoverMils * BallisticMath.MIL_RAD);
        Vector3 direction = new Vector3(forward).rotateRad(side, elevation);
        direction.rotateRad(up, (float) (-windageClicks * 0.0001));

        float sigma = (float) (weapon.bullet.precisionMoa * BallisticMath.MOA_RAD * 0.35);
        direction.rotateRad(side, gaussian() * sigma);
        direction.rotateRad(up, gaussian() * sigma);
        direction.nor();

        double muzzleVelocity = weapon.bullet.muzzleVelocity
                + gaussian() * weapon.bullet.muzzleVelocitySd;
        muzzleVelocity = Math.max(80, muzzleVelocity);

        Vector3 origin = new Vector3(shooterEye).mulAdd(forward, 0.35f);
        origin.y -= weapon.sightHeightMeters;

        BallisticsEngine engine = new BallisticsEngine(world.atmosphere, world.wind, world.terrain);
        Trajectory trajectory = engine.simulate(weapon.bullet, muzzleVelocity, origin, direction);
        float fireMissionTime = world.missionTime;

        HitResult hit = findTargetHit(trajectory, fireMissionTime);
        Vector3 impactPoint;
        Vector3 impactVelocity = new Vector3();
        RangeTarget hitTarget = null;
        int ring = 0;
        int points = 0;
        float hitTime;
        if (hit != null) {
            hitTarget = hit.target;
            ring = hit.ring;
            points = hitTarget.pointsFor(ring);
            hitTime = hit.localTime;
            impactPoint = hit.point;
            impactVelocity.set(hit.velocity);
        } else {
            impactPoint = new Vector3(trajectory.impactPoint);
            impactVelocity.set(trajectory.impactVelocity);
            hitTime = trajectory.impactTime;
        }
        float distance = (float) Math.hypot(impactPoint.x - shooterEye.x, impactPoint.z - shooterEye.z);

        shotCount++;
        Shot shot = new Shot(weapon, trajectory, fireMissionTime, muzzleVelocity,
                hitTarget, ring, hitTime, impactPoint, impactVelocity, points, distance,
                weapon.magazineSize);
        renderer.syncRifle(camera);
        shot.muzzle.set(renderer.muzzleWorld());
        if (world.course.competition) {
            shot.matchStage = matchStage;
            matchShots++;
            if (matchShots >= MATCH[matchStage].shots) {
                advanceMatch();
            }
        }
        shots.add(shot);
        if (shots.size() > 14) {
            Shot removed = shots.remove(0);
            if (removed == followedShot) {
                endBulletCam();
            }
        }

        boltReady = false;
        boltTimer = (float) weapon.cycleTime;
        boltSoundTimer = boltTimer;

        recoilPitchVelocity += weapon.recoilPitchRad * 30.0f;
        recoilYawVelocity += weapon.recoilYawRad * (gaussian() < 0 ? -30.0f : 30.0f)
                * (0.65f + random.nextFloat() * 0.7f);
        timeSinceShot = 0;
        muzzleFlash = 1f;
        screenFlash = 1f;
        audio.shot(weapon);
        if (scopeAmount < 0.2f) {
            renderer.spawnMuzzleSmoke();
        }

        if (bulletCamArmed) {
            startBulletCam(shot);
        }
    }

    private HitResult findTargetHit(Trajectory trajectory, float fireMissionTime) {
        HitResult best = null;
        for (int i = 1; i < trajectory.size(); i++) {
            float t0 = trajectory.time(i - 1);
            float t1 = trajectory.time(i);
            float x0 = trajectory.x(i - 1);
            float y0 = trajectory.y(i - 1);
            float z0 = trajectory.z(i - 1);
            float x1 = trajectory.x(i);
            float y1 = trajectory.y(i);
            float z1 = trajectory.z(i);
            if (best != null && t0 > best.localTime) {
                break;
            }
            for (RangeTarget target : world.targets) {
                target.boundsAt(fireMissionTime + t1, scratchBox);
                scratchBox.min.add(-0.015f, -0.015f, -0.015f);
                scratchBox.max.add(0.015f, 0.015f, 0.015f);
                float frac = segmentAabb(x0, y0, z0, x1, y1, z1, scratchBox);
                if (frac < 0) {
                    continue;
                }
                float localTime = t0 + (t1 - t0) * frac;
                float hx = x0 + (x1 - x0) * frac;
                float hy = y0 + (y1 - y0) * frac;
                float hz = z0 + (z1 - z0) * frac;
                double globalTime = fireMissionTime + localTime;
                target.boundsAt(globalTime, scratchBox);
                if (hx < scratchBox.min.x - 0.02f || hx > scratchBox.max.x + 0.02f
                        || hy < scratchBox.min.y - 0.02f || hy > scratchBox.max.y + 0.02f
                        || hz < scratchBox.min.z - 0.02f || hz > scratchBox.max.z + 0.02f) {
                    continue;
                }
                int ring = target.ringAt(globalTime, hx, hy);
                if (ring < 0) {
                    continue;
                }
                if (best == null || localTime < best.localTime) {
                    HitResult result = new HitResult();
                    result.target = target;
                    result.ring = ring;
                    result.localTime = localTime;
                    result.point = new Vector3(hx, hy, hz);
                    result.velocity = new Vector3();
                    trajectory.getVelocity(localTime, result.velocity);
                    best = result;
                }
            }
        }
        return best;
    }

    private static float segmentAabb(float x0, float y0, float z0,
                                     float x1, float y1, float z1,
                                     BoundingBox box) {
        float dx = x1 - x0;
        float dy = y1 - y0;
        float dz = z1 - z0;
        float tmin = 0f;
        float tmax = 1f;
        for (int axis = 0; axis < 3; axis++) {
            float start = axis == 0 ? x0 : (axis == 1 ? y0 : z0);
            float delta = axis == 0 ? dx : (axis == 1 ? dy : dz);
            float min = axis == 0 ? box.min.x : (axis == 1 ? box.min.y : box.min.z);
            float max = axis == 0 ? box.max.x : (axis == 1 ? box.max.y : box.max.z);
            if (Math.abs(delta) < 1e-8f) {
                if (start < min || start > max) {
                    return -1;
                }
            } else {
                float t1 = (min - start) / delta;
                float t2 = (max - start) / delta;
                if (t1 > t2) {
                    float tmp = t1;
                    t1 = t2;
                    t2 = tmp;
                }
                if (t1 > tmin) {
                    tmin = t1;
                }
                if (t2 < tmax) {
                    tmax = t2;
                }
                if (tmin > tmax) {
                    return -1;
                }
            }
        }
        return tmin;
    }

    private void triggerImpact(Shot shot) {
        boolean steel = shot.target != null;
        float energy = 0;
        if (shot.impactVelocity.len2() > 0) {
            energy = 0.5f * (float) shot.weapon.bullet.massKg()
                    * shot.impactVelocity.len2();
        }
        float distance = shot.distance;
        audio.impact(steel, distance, energy);
        if (shot.trajectory.impactType == Trajectory.IMPACT_TERRAIN || steel) {
            renderer.spawnImpact(shot.impactPoint, steel, energy);
        }

        if (steel) {
            shot.target.registerHit();
            if (shot.matchStage >= 0) {
                if (shot.target == liveMatchTarget(shot.matchStage) && shot.ring >= 5) {
                    matchScore += shot.ring;
                    if (isMatchX(shot)) {
                        matchX++;
                    }
                }
            } else {
                score += shot.points;
            }
            hitCount++;
            bestRing = Math.max(bestRing, shot.ring);
            longestHit = Math.max(longestHit, distance);
            bestEnergy = Math.max(bestEnergy, BallisticMath.joulesToFootPounds(energy));
            hitMarker = 1f;
            missMarker = 0f;
            double impactTime = shot.fireMissionTime + shot.hitTime;
            double centerX = shot.target.xAt(impactTime);
            double offset = Math.hypot(shot.impactPoint.x - centerX,
                    shot.impactPoint.y - shot.target.baseY);
            double moa = Math.toDegrees(Math.atan2(offset, Math.max(1, distance))) * 60.0;
            int shown = shot.matchStage >= 0 ? shot.ring : shot.points;
            showToast(true,
                    String.format(Locale.ROOT, "%s  ·  RING %d  ·  +%d",
                            shot.target.label(), shot.ring, shown),
                    String.format(Locale.ROOT, "%.0f M  ·  TOF %.2f S  ·  %.0f FT-LB  ·  %.2f MOA",
                            distance, shot.hitTime, BallisticMath.joulesToFootPounds(energy), moa),
                    "");
            toastTimer = 3.6f;
        } else {
            hitMarker = 0f;
            missMarker = 1f;
            String title = "SPLASH";
            String detail = String.format(Locale.ROOT, "IMPACT %.0f M  ·  TOF %.2f S",
                    distance, shot.hitTime);
            String miss = nearestMissText(shot);
            showToast(false, title, detail, miss);
            toastTimer = 3.0f;
        }
    }

    private String nearestMissText(Shot shot) {
        RangeTarget nearest = null;
        double best = Double.MAX_VALUE;
        double impactTime = shot.fireMissionTime + shot.hitTime;
        for (RangeTarget target : world.targets) {
            Vector3 center = target.positionAt(impactTime);
            double d = center.dst(shot.impactPoint);
            if (d < best) {
                best = d;
                nearest = target;
            }
        }
        if (nearest == null || best > 45) {
            return "";
        }
        Vector3 center = nearest.positionAt(impactTime);
        double dx = shot.impactPoint.x - center.x;
        double dy = shot.impactPoint.y - center.y;
        double range = Math.max(50, nearest.distance);
        double horizontal = Math.toDegrees(Math.atan2(dx, range)) * 60.0;
        double vertical = Math.toDegrees(Math.atan2(dy, range)) * 60.0;
        String h = Math.abs(horizontal) < 0.05 ? "CENTER"
                : String.format(Locale.ROOT, "%.1f MOA %s", Math.abs(horizontal),
                horizontal > 0 ? "RIGHT" : "LEFT");
        String v = Math.abs(vertical) < 0.05 ? "LEVEL"
                : String.format(Locale.ROOT, "%.1f MOA %s", Math.abs(vertical),
                vertical > 0 ? "HIGH" : "LOW");
        return "IMPACT " + h + "  ·  " + v;
    }

    private void updateWeapon(float delta) {
        if (!boltReady) {
            boltTimer -= delta;
            if (boltTimer <= 0) {
                boltReady = true;
                audio.boltCycle();
            }
        }
        if (boltSoundTimer > 0) {
            boltSoundTimer -= delta;
        }
    }

    private void updateShots(float delta) {
        for (Iterator<Shot> it = shots.iterator(); it.hasNext(); ) {
            Shot shot = it.next();
            boolean followed = shot == followedShot;
            float speed = followed ? 0.24f : 1f;
            if (!shot.impactTriggered) {
                shot.animTime += delta * speed;
            }
            if (!shot.impactTriggered && shot.animTime >= shot.impactTime()) {
                shot.animTime = shot.impactTime();
                shot.impactTriggered = true;
                triggerImpact(shot);
            }
            shot.update(delta);
            if (shot.impactTriggered && shot.trailAlpha <= 0 && !followed) {
                it.remove();
            }
        }
        if (followedShot != null && followedShot.impactTriggered) {
            bulletCamHold += delta;
            if (bulletCamHold > 1.05f) {
                endBulletCam();
            }
        }
    }

    // ------------------------------------------------------------- bullet cam

    private void handleBulletCamKey() {
        if (bulletCamActive) {
            endBulletCam();
            showToast(false, "BULLET CAM OFF", "", "");
            toastTimer = 1.2f;
            return;
        }
        for (int i = shots.size() - 1; i >= 0; i--) {
            Shot shot = shots.get(i);
            if (!shot.impactTriggered) {
                startBulletCam(shot);
                return;
            }
        }
        bulletCamArmed = !bulletCamArmed;
        showToast(false, bulletCamArmed ? "BULLET CAM ARMED" : "BULLET CAM OFF",
                bulletCamArmed ? "NEXT ROUND WILL FOLLOW THE TRACE" : "", "");
        toastTimer = 1.5f;
    }

    private void startBulletCam(Shot shot) {
        if (shot.impactTriggered) {
            return;
        }
        followedShot = shot;
        bulletCamActive = true;
        bulletCamArmed = false;
        bulletCamHold = 0;
        shot.followed = true;
        shot.animTime = Math.max(shot.animTime, shot.impactTime() - 0.65f);
        showToast(false, "BULLET CAM", "SLOW MOTION · V TO EXIT", "");
        toastTimer = 1.3f;
    }

    private void endBulletCam() {
        if (followedShot != null) {
            followedShot.followed = false;
        }
        followedShot = null;
        bulletCamActive = false;
        bulletCamHold = 0;
    }

    private void updateBulletCamera(float delta) {
        if (followedShot == null) {
            bulletCamActive = false;
            return;
        }
        float t = Math.min(followedShot.animTime, followedShot.impactTime());
        followedShot.trajectory.getPosition(t, bulletCamPos);
        followedShot.trajectory.getVelocity(t, bulletCamVel);
        if (bulletCamVel.len2() < 1e-4f) {
            bulletCamVel.set(0, 0, -1);
        }
        Vector3 forward = new Vector3(bulletCamVel).nor();
        Vector3 side = new Vector3(forward).crs(WORLD_UP).nor();
        camera.position.set(bulletCamPos).mulAdd(forward, -2.6f).add(0, 0.42f, 0)
                .mulAdd(side, 0.34f);
        camera.direction.set(bulletCamPos).sub(camera.position).nor();
        camera.up.set(WORLD_UP);
        camera.fieldOfView = 47f;
        camera.viewportWidth = width;
        camera.viewportHeight = height;
        camera.update();
    }

    // --------------------------------------------------------------- zero

    private void recomputeZero() {
        Vector3 level = new Vector3((float) Math.sin(yaw), 0, (float) -Math.cos(yaw));
        zeroAngle = ZeroSolver.solveZeroAngle(weapon.bullet, world.atmosphere, world.wind,
                world.terrain, shooterEye, level, zeroDistance, weapon.sightHeightMeters);
        dopeDirty = true;
    }

    private void cycleZero() {
        zeroDistance += 100;
        if (zeroDistance > 500) {
            zeroDistance = 100;
        }
        elevationClicks = 0;
        windageClicks = 0;
        holdoverMils = 0;
        recomputeZero();
        showToast(false, "ZERO CHANGED", "ZERO DISTANCE " + zeroDistance + " M", "");
        toastTimer = 1.6f;
    }

    public void applyLoadout(Loadout next) {
        loadout = next.copy();
        weapon = loadout.build();
        weaponIndex = loadout.rifleIndex;
        zeroDistance = loadout.zeroDistance;
        zoom = weapon.scopeDefaultZoom;
        elevationClicks = 0;
        windageClicks = 0;
        holdoverMils = 0;
        boltReady = true;
        boltTimer = 0;
        briefing = false;
        paused = false;
        recomputeZero();
        if (audio != null) {
            audio.boltCycle();
        }
        showToast(false, weapon.name, weapon.optic, "");
        toastTimer = 2.2f;
    }

    private void switchWeapon(int index) {
        if (index == weaponIndex) {
            return;
        }
        loadout.rifleIndex = index;
        weapon = loadout.build();
        weaponIndex = index;
        boltReady = true;
        boltTimer = 0;
        zoom = weapon.scopeDefaultZoom;
        elevationClicks = 0;
        windageClicks = 0;
        holdoverMils = 0;
        recomputeZero();
        audio.boltCycle();
        showToast(false, weapon.name, weapon.caliber + "  ·  " + weapon.specLine(), "");
        toastTimer = 2.2f;
    }

    private void adjustHoldover(double delta) {
        setHoldover(holdoverMils + delta, false);
    }

    private void setHoldover(double mils, boolean quiet) {
        double next = Math.round(BallisticMath.clamp(mils, 0, 30) * 10.0) / 10.0;
        if (next == holdoverMils) {
            return;
        }
        holdoverMils = next;
        audio.turretClick();
        if (!quiet) {
            showToast(false, "HOLDOVER",
                    holdoverMils <= 0.001
                            ? "CENTER HOLD  ·  \\ TO RESET"
                            : String.format(Locale.ROOT, "HOLD +%.1f MIL  ·  \\ TO RESET", holdoverMils),
                    "");
            toastTimer = 1.1f;
        }
    }

    private void ensureDope() {
        if (!dopeDirty && dope != null) {
            return;
        }
        dope = new BallisticTable(weapon.bullet, world.atmosphere, world.wind, world.terrain,
                shooterEye, zeroDistance, weapon.sightHeightMeters, weapon.maxRangeMeters);
        dopeDirty = false;
    }

    // ------------------------------------------------------------- rangefind

    private void laserRange() {
        double max = 4000;
        double bestT = max;
        RangeTarget bestTarget = null;
        for (RangeTarget target : world.targets) {
            target.boundsAt(world.missionTime, scratchBox);
            float frac = segmentAabb(shooterEye.x, shooterEye.y, shooterEye.z,
                    shooterEye.x + currentAim.x * (float) max,
                    shooterEye.y + currentAim.y * (float) max,
                    shooterEye.z + currentAim.z * (float) max,
                    scratchBox);
            if (frac >= 0) {
                double t = frac * max;
                if (t < bestT) {
                    bestT = t;
                    bestTarget = target;
                }
            }
        }
        if (bestTarget != null) {
            rangeMeters = Math.hypot(bestTarget.xAt(world.missionTime) - shooterEye.x,
                    bestTarget.baseZ - shooterEye.z);
            highlightRange = (int) (Math.round(rangeMeters / 100.0) * 100);
            rangeValid = true;
            rangeTimer = 6f;
            audio.turretClick();
            return;
        }

        // March the ray against the analytic terrain.
        double step = 3.0;
        double previous = 0;
        for (double t = step; t <= max; t += step) {
            double x = shooterEye.x + currentAim.x * t;
            double y = shooterEye.y + currentAim.y * t;
            double z = shooterEye.z + currentAim.z * t;
            double ground = world.terrain.heightAt(x, z);
            if (y <= ground) {
                rangeMeters = previous;
                highlightRange = (int) (Math.round(rangeMeters / 100.0) * 100);
                rangeValid = true;
                rangeTimer = 6f;
                audio.turretClick();
                return;
            }
            previous = t;
        }
        rangeValid = false;
    }

    // ------------------------------------------------------------- feedback

    private void updateFeedback(float delta) {
        muzzleFlash = Math.max(0, muzzleFlash - delta * 11f);
        screenFlash = Math.max(0, screenFlash - delta * 8f);
        hitMarker = Math.max(0, hitMarker - delta * 3.4f);
        missMarker = Math.max(0, missMarker - delta * 2.2f);
        if (toastTimer > 0) {
            toastTimer -= delta;
            if (toastTimer <= 0) {
                toastTitle = "";
                toastDetail = "";
                missText = "";
            }
        }
        if (rangeValid) {
            rangeTimer -= delta;
            if (rangeTimer <= 0) {
                rangeValid = false;
            }
        }
    }

    private void showToast(boolean hit, String title, String detail, String miss) {
        toastHit = hit;
        toastTitle = title;
        toastDetail = detail;
        missText = miss;
    }

    // ---------------------------------------------------------------- HUD

    private void updateHudData() {
        HudState s = hudState;
        s.weapon = weapon;
        s.weaponIndex = weaponIndex;
        s.ammo = weapon.magazineSize;
        s.magazine = weapon.magazineSize;
        s.reloading = false;
        s.reloadFraction = 0;
        s.touchControls = android && !briefing && !paused && !bulletCamActive;
        s.zeroDistance = zeroDistance;
        s.elevationClicks = elevationClicks;
        s.windageClicks = windageClicks;
        s.holdoverMils = holdoverMils;
        s.zoom = zoom;
        s.scopeAmount = bulletCamActive ? 0f : scopeAmount;
        s.scoped = scopedInput && !bulletCamActive;
        s.atmosphere = world.atmosphere;
        s.windSpeed = world.wind.speedAt(world.missionTime, 10);
        s.windFrom = world.wind.bearingAt(world.missionTime);
        s.windClock = world.wind.clockPosition(currentAim);
        s.crosswind = world.wind.crosswindComponent(currentAim, world.missionTime);
        s.density = world.atmosphere.density();
        s.densityAltitude = world.atmosphere.densityAltitudeMeters();
        s.score = score;
        s.shots = shotCount;
        s.hits = hitCount;
        s.accuracy = shotCount == 0 ? 0 : hitCount / (double) shotCount;
        s.bestRing = bestRing;
        s.longestHit = longestHit;
        s.bestEnergy = bestEnergy;
        s.breath = breath;
        s.holdingBreath = holdingBreath;
        s.steady = timeSinceShot > 1.3f && !holdingBreath;
        s.dope = dope;
        s.dopeVisible = !briefing && !paused && Gdx.input.isKeyPressed(Input.Keys.TAB);
        if (s.dopeVisible) {
            ensureDope();
            s.dope = dope;
        }
        s.briefing = briefing;
        s.rangeCard = rangeCard;
        s.missionBrief = world.course.blurb;
        s.rangeTitle = world.course.title;
        if (world.course.competition) {
            if (matchDone) {
                s.matchLine = String.format(Locale.ROOT, "MATCH COMPLETE  %d-%dX", matchScore, matchX);
            } else {
                int sec = Math.max(0, (int) Math.ceil(matchTime));
                Stage stage = MATCH[matchStage];
                s.matchLine = String.format(Locale.ROOT, "%s   %d/%d   %d:%02d   %d-%dX",
                        stage.name, matchShots, stage.shots, sec / 60, sec % 60, matchScore, matchX);
            }
        } else {
            s.matchLine = "";
        }
        s.paused = paused;
        s.helpVisible = helpVisible;
        s.bulletCam = bulletCamActive;
        s.bulletCamArmed = bulletCamArmed;
        s.shaderReady = scopeRenderer.shaderReady();
        s.compassHeading = WindModel.normalizeDegrees(Math.toDegrees(yaw));
        s.rangeValid = rangeValid;
        s.rangeMeters = rangeMeters;
        s.highlightRange = highlightRange;
        s.toastTimer = toastTimer;
        s.toastHit = toastHit;
        s.toastTitle = toastTitle;
        s.toastDetail = toastDetail;
        s.missText = missText;
        s.hitMarker = hitMarker;
        s.missMarker = missMarker;
        s.flash = screenFlash;
        s.pixelsPerMil = (float) ((height * 0.5) / Math.tan(Math.toRadians(camera.fieldOfView * 0.5)) * 0.001);
        s.airborne = followedShot != null && !followedShot.impactTriggered;
        if (followedShot != null) {
            float t = Math.min(followedShot.animTime, followedShot.impactTime());
            followedShot.trajectory.getPosition(t, bulletCamPos);
            followedShot.trajectory.getVelocity(t, bulletCamVel);
            s.bulletSpeed = bulletCamVel.len();
            s.bulletHeight = bulletCamPos.y;
            s.bulletTof = t;
            s.bulletRange = Math.hypot(bulletCamPos.x - shooterEye.x, bulletCamPos.z - shooterEye.z);
        }
    }

    // -------------------------------------------------------------- render

    private void renderFrame(float delta) {
        boolean useScope = scopeAmount > 0.01f && !bulletCamActive;
        boolean showRifle = !bulletCamActive && scopeAmount < 0.18f;

        if (useScope) {
            scopeRenderer.begin();
            clearScene();
            renderWorld(showRifle);
            scopeRenderer.end(scopeAmount, 0.65f, delta);
        } else {
            Gdx.gl.glViewport(0, 0, width, height);
            clearScene();
            renderWorld(showRifle);
        }

        Gdx.gl.glViewport(0, 0, width, height);
        hud.render(hudState, delta);
    }

    private void clearScene() {
        Gdx.gl.glClearColor(0.045f, 0.06f, 0.075f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDepthMask(true);
    }

    private void renderWorld(boolean showRifle) {
        renderer.renderSky(camera, width, height);
        Gdx.gl.glClear(GL20.GL_DEPTH_BUFFER_BIT);
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        renderer.renderWorld(camera, world, showRifle);
        renderer.renderEffects(camera, width, height, shots,
                showRifle ? muzzleFlash : 0f);
    }

    // ------------------------------------------------------------- utility

    private float gaussian() {
        double u1 = Math.max(1e-7, random.nextDouble());
        double u2 = random.nextDouble();
        return (float) (Math.sqrt(-2.0 * Math.log(u1)) * Math.cos(Math.PI * 2.0 * u2));
    }

    private static float wrapAngle(float angle) {
        while (angle > Math.PI) {
            angle -= Math.PI * 2;
        }
        while (angle < -Math.PI) {
            angle += Math.PI * 2;
        }
        return angle;
    }

    private void captureMouse() {
        if (smokeTest) {
            return;
        }
        Gdx.input.setCursorCatched(true);
        cursorCaptured = true;
    }

    private void releaseMouse() {
        Gdx.input.setCursorCatched(false);
        cursorCaptured = false;
    }

    private void updateSmokeTest() {
        if (frame == 3) {
            briefing = false;
            scopeAmount = 1f;
            // Put the crosshair on the 200 m silhouette so the smoke test
            // exercises hit detection and scoring, not just rendering.
            RangeTarget target = world.targets.get(0);
            Vector3 center = target.positionAt(world.missionTime);
            double dx = center.x - shooterEye.x;
            double dy = center.y - shooterEye.y;
            double dz = center.z - shooterEye.z;
            yaw = (float) Math.atan2(dx, -dz);
            pitch = (float) Math.atan2(dy, Math.hypot(dx, dz));
        }
        if (frame == 15) {
            fire();
        }
        if (frame == 90) {
            lastGlError = Gdx.gl.glGetError();
            Gdx.app.log("SmokeTest", "SMOKE_TEST_OK frames=" + frame
                    + " glError=" + lastGlError + " shots=" + shots.size()
                    + " hits=" + hitCount + " score=" + score);
            Gdx.app.exit();
        }
    }

    @Override
    public void dispose() {
        renderer.dispose();
        scopeRenderer.dispose();
        hud.dispose();
        audio.dispose();
        world.dispose();
        com.snipersim.render.ModelFactory.dispose();
    }

    @Override
    public void pause() {
        if (!briefing) {
            paused = true;
            endBulletCam();
            releaseMouse();
        }
    }

    @Override
    public void resume() {
    }

    private void openArmory() {
        if (smokeTest) {
            return;
        }
        paused = true;
        endBulletCam();
        releaseMouse();
        game.setScreen(new CatalogScreen(game, this, loadout.copy()));
    }

    public RangeCourse course() {
        return world.course;
    }

    private void buildRangeCard() {
        java.util.List<String> lines = new java.util.ArrayList<>();
        for (com.snipersim.world.RangeTarget target : world.targets) {
            if (Math.abs(target.baseX) > 0.2f) {
                continue;
            }
            lines.add(String.format(Locale.ROOT, "%.0f m  %s", target.distance, target.label()));
        }
        rangeCard = lines.toArray(new String[0]);
    }

    private void advanceMatch() {
        if (matchDone) {
            return;
        }
        matchStage++;
        matchShots = 0;
        if (matchStage >= MATCH.length) {
            matchDone = true;
            showToast(true, "MATCH COMPLETE",
                    String.format(Locale.ROOT, "%d-%dX", matchScore, matchX), "");
            toastTimer = 6f;
            return;
        }
        matchTime = MATCH[matchStage].seconds;
        showToast(false, MATCH[matchStage].name,
                MATCH[matchStage].shots + " SHOTS", "");
        toastTimer = 2.5f;
    }

    private com.snipersim.world.RangeTarget liveMatchTarget(int stage) {
        float meters = MATCH[stage].meters;
        com.snipersim.world.RangeTarget best = null;
        for (com.snipersim.world.RangeTarget target : world.targets) {
            if (Math.abs(target.baseX) > 0.2f) {
                continue;
            }
            if (best == null || Math.abs(target.distance - meters) < Math.abs(best.distance - meters)) {
                best = target;
            }
        }
        return best;
    }

    private static boolean isMatchX(Shot shot) {
        double dx = shot.impactPoint.x - shot.target.xAt(shot.fireMissionTime + shot.hitTime);
        double dy = shot.impactPoint.y - shot.target.baseY;
        return Math.hypot(dx, dy) / (shot.target.width * 0.5) <= 0.081;
    }

    private static final class Stage {
        final String name;
        final float meters;
        final int shots;
        final float seconds;
        final float sway;

        Stage(String name, float meters, int shots, float seconds, float sway) {
            this.name = name;
            this.meters = meters;
            this.shots = shots;
            this.seconds = seconds;
            this.sway = sway;
        }
    }

    private static final Stage[] MATCH = {
            new Stage("200 YD STANDING SLOW", RangeCourse.yards(200), 10, 600f, 4.2f),
            new Stage("200 YD RAPID SITTING", RangeCourse.yards(200), 10, 60f, 2.0f),
            new Stage("300 YD RAPID PRONE", RangeCourse.yards(300), 10, 70f, 1.15f),
            new Stage("600 YD PRONE SLOW", RangeCourse.yards(600), 20, 1200f, 0.85f)
    };

    private static final class HitResult {
        RangeTarget target;
        int ring;
        float localTime;
        Vector3 point;
        Vector3 velocity;
    }
}
