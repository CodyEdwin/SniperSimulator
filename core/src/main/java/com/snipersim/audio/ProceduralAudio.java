package com.snipersim.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.snipersim.weapons.Weapon;
import com.snipersim.weapons.WeaponCatalog;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Every sound in the game is synthesized at startup into small mono WAV files:
 * muzzle blasts, steel rings, dirt impacts, bolt cycling, reload foley and a
 * loopable wind bed. No audio assets are shipped.
 */
public final class ProceduralAudio {
    private static final int SAMPLE_RATE = 44100;
    private static final String VERSION = "v3";

    private final Map<String, Sound> gunshots = new HashMap<>();
    private Sound steel;
    private Sound dirt;
    private Sound bolt;
    private Sound reload;
    private Sound empty;
    private Sound wind;
    private boolean enabled;

    public ProceduralAudio() {
        try {
            if (Gdx.audio == null) {
                Gdx.app.log("Audio", "No audio backend; running silent");
                return;
            }
            FileHandle dir = Gdx.files.local("generated-audio");
            dir.mkdirs();
            for (Weapon weapon : WeaponCatalog.all()) {
                String name = VERSION + "_shot_" + weapon.id;
                FileHandle file = dir.child(name + ".wav");
                if (!file.exists()) {
                    writeWav(file, synthGunshot(weapon));
                }
                gunshots.put(weapon.id, Gdx.audio.newSound(file));
            }
            steel = loadOrCreate(dir, VERSION + "_steel", synthSteel());
            dirt = loadOrCreate(dir, VERSION + "_dirt", synthDirt());
            bolt = loadOrCreate(dir, VERSION + "_bolt", synthBolt());
            reload = loadOrCreate(dir, VERSION + "_reload", synthReload());
            empty = loadOrCreate(dir, VERSION + "_empty", synthEmpty());
            wind = loadOrCreate(dir, VERSION + "_wind", synthWind());
            wind.loop(0.16f);
            enabled = true;
        } catch (Throwable t) {
            enabled = false;
            if (Gdx.app != null) {
                Gdx.app.error("Audio", "Procedural audio disabled: " + t.getMessage());
            }
        }
    }

    private Sound loadOrCreate(FileHandle dir, String name, short[] samples) {
        FileHandle file = dir.child(name + ".wav");
        if (!file.exists()) {
            writeWav(file, samples);
        }
        return Gdx.audio.newSound(file);
    }

    public void shot(Weapon weapon) {
        if (!enabled || weapon == null) {
            return;
        }
        Sound sound = gunshots.get(weapon.id);
        if (sound != null) {
            sound.play(0.90f, 1f, 0f);
        }
    }

    public void impact(boolean steelHit, float distanceMeters, float energyJoules) {
        if (!enabled) {
            return;
        }
        float volume = (float) Math.max(0.06, Math.min(0.95, 1.0 - distanceMeters / 2600.0));
        if (steelHit) {
            float pitch = (float) (0.9 + Math.min(0.5, energyJoules / 6000.0));
            steel.play(volume, pitch, 0f);
        } else {
            dirt.play(volume * 0.7f, 0.85f + (float) Math.random() * 0.2f, 0f);
        }
    }

    public void boltCycle() {
        if (enabled && bolt != null) {
            bolt.play(0.55f, 0.95f + (float) Math.random() * 0.08f, 0f);
        }
    }

    public void reload() {
        if (enabled && reload != null) {
            reload.play(0.6f, 1f, 0f);
        }
    }

    public void emptyClick() {
        if (enabled && empty != null) {
            empty.play(0.5f, 1f, 0f);
        }
    }

    public void turretClick() {
        if (enabled && empty != null) {
            empty.play(0.22f, 1.7f, 0f);
        }
    }

    public boolean enabled() {
        return enabled;
    }

    // ----------------------------------------------------------- synthesis

    private short[] synthGunshot(Weapon weapon) {
        int length = (int) (SAMPLE_RATE * 1.35);
        float[] buffer = new float[length];
        Random random = new Random(0xB00L + weapon.id.hashCode());
        double pitch = weapon.soundPitch;
        double f0 = 82.0 / Math.max(0.5, pitch);
        double bodyTau = 0.22 + weapon.muzzleFlashScale * 0.10;
        double crackTau = 0.018 + weapon.muzzleFlashScale * 0.009;
        double lp = 0;
        double lp2 = 0;
        double lpAlpha = 0.16 + 0.05 * pitch;
        for (int i = 0; i < length; i++) {
            double t = i / (double) SAMPLE_RATE;
            double noise = random.nextGaussian() * 0.42;
            lp += (noise - lp) * lpAlpha;
            lp2 += (lp - lp2) * 0.035;
            double crack = (noise - lp) * Math.exp(-t / crackTau) * 1.8;
            double body = Math.sin(2 * Math.PI * f0 * t * (1.0 - Math.min(0.24, t * 0.36)))
                    * Math.exp(-t / bodyTau) * (0.55 + 0.10 * pitch);
            body += Math.sin(2 * Math.PI * f0 * 1.58 * t) * Math.exp(-t / (bodyTau * 0.55)) * 0.22;
            double mid = lp * Math.exp(-t / 0.10) * 0.9;
            double tail = lp2 * Math.exp(-t / 0.45) * 0.5;
            double sample = crack + body + mid + tail;
            // first reflection off the firing berm
            if (i > SAMPLE_RATE * 0.13) {
                int j = i - (int) (SAMPLE_RATE * 0.13);
                sample += buffer[j] * 0.16;
            }
            buffer[i] = (float) Math.tanh(sample * 1.3);
        }
        return normalize(buffer, 0.88);
    }

    private short[] synthSteel() {
        int length = (int) (SAMPLE_RATE * 1.8);
        float[] buffer = new float[length];
        Random random = new Random(991);
        double[] freqs = {1180, 1740, 2510, 3660, 4880};
        double[] amps = {0.55, 0.38, 0.25, 0.14, 0.08};
        double[] taus = {1.15, 0.82, 0.62, 0.44, 0.28};
        for (int i = 0; i < length; i++) {
            double t = i / (double) SAMPLE_RATE;
            double sample = 0;
            for (int h = 0; h < freqs.length; h++) {
                sample += Math.sin(2 * Math.PI * freqs[h] * t) * amps[h] * Math.exp(-t / taus[h]);
            }
            if (t < 0.03) {
                sample += random.nextGaussian() * 0.6 * Math.exp(-t / 0.012);
            }
            buffer[i] = (float) Math.tanh(sample * 1.2);
        }
        return normalize(buffer, 0.65);
    }

    private short[] synthDirt() {
        int length = (int) (SAMPLE_RATE * 0.65);
        float[] buffer = new float[length];
        Random random = new Random(223);
        double lp = 0;
        for (int i = 0; i < length; i++) {
            double t = i / (double) SAMPLE_RATE;
            double n = random.nextGaussian() * 0.5;
            lp += (n - lp) * 0.055;
            double thud = Math.sin(2 * Math.PI * 62 * t) * Math.exp(-t / 0.10) * 0.4;
            buffer[i] = (float) Math.tanh((lp * 2.2 + thud) * Math.exp(-t / 0.28) * 1.4);
        }
        return normalize(buffer, 0.6);
    }

    private short[] synthBolt() {
        int length = (int) (SAMPLE_RATE * 0.68);
        float[] buffer = new float[length];
        Random random = new Random(771);
        double[] clicks = {0.02, 0.18, 0.38, 0.51};
        double[] gains = {0.8, 0.55, 0.7, 0.5};
        for (int i = 0; i < length; i++) {
            double t = i / (double) SAMPLE_RATE;
            double sample = 0;
            for (int c = 0; c < clicks.length; c++) {
                double dt = t - clicks[c];
                if (dt >= 0 && dt < 0.14) {
                    double burst = random.nextGaussian() * 0.35 * Math.exp(-dt / 0.012);
                    double metal = Math.sin(2 * Math.PI * (2100 + c * 330) * dt)
                            * Math.exp(-dt / 0.035) * 0.22;
                    sample += (burst + metal) * gains[c];
                }
            }
            if (t > 0.13 && t < 0.36) {
                sample += Math.sin(2 * Math.PI * 870 * t) * 0.035;
            }
            buffer[i] = (float) Math.tanh(sample * 1.1);
        }
        return normalize(buffer, 0.55);
    }

    private short[] synthReload() {
        int length = (int) (SAMPLE_RATE * 1.5);
        float[] buffer = new float[length];
        Random random = new Random(553);
        double[] clicks = {0.05, 0.22, 0.52, 0.68, 0.98, 1.18, 1.32};
        double[] gains = {0.4, 0.55, 0.35, 0.8, 0.45, 0.75, 0.55};
        for (int i = 0; i < length; i++) {
            double t = i / (double) SAMPLE_RATE;
            double sample = 0;
            for (int c = 0; c < clicks.length; c++) {
                double dt = t - clicks[c];
                if (dt >= 0 && dt < 0.11) {
                    sample += (random.nextGaussian() * 0.28
                            + Math.sin(2 * Math.PI * (1600 + c * 140) * dt) * 0.20)
                            * Math.exp(-dt / 0.022) * gains[c];
                }
            }
            if (t > 0.24 && t < 0.50) {
                sample += random.nextGaussian() * 0.06 * Math.sin(Math.PI * (t - 0.24) / 0.26);
            }
            buffer[i] = (float) Math.tanh(sample);
        }
        return normalize(buffer, 0.55);
    }

    private short[] synthEmpty() {
        int length = (int) (SAMPLE_RATE * 0.10);
        float[] buffer = new float[length];
        Random random = new Random(881);
        for (int i = 0; i < length; i++) {
            double t = i / (double) SAMPLE_RATE;
            buffer[i] = (float) ((random.nextGaussian() * 0.5 * Math.exp(-t / 0.006))
                    + Math.sin(2 * Math.PI * 2800 * t) * 0.5 * Math.exp(-t / 0.012));
        }
        return normalize(buffer, 0.45);
    }

    private short[] synthWind() {
        int length = SAMPLE_RATE * 6;
        float[] buffer = new float[length];
        Random random = new Random(9182);
        double lp = 0;
        double lp2 = 0;
        for (int i = 0; i < length; i++) {
            double t = i / (double) SAMPLE_RATE;
            double n = random.nextGaussian() * 0.35;
            lp += (n - lp) * 0.045;
            lp2 += (lp - lp2) * 0.008;
            double gust = 0.55 + 0.32 * Math.sin(t * 0.72) + 0.18 * Math.sin(t * 1.73 + 1.2);
            double whistle = Math.sin(2 * Math.PI * (320 + 40 * Math.sin(t * 0.23)) * t) * 0.008;
            buffer[i] = (float) ((lp * 0.9 + lp2 * 2.4 + whistle) * gust);
        }
        // Crossfade the loop seam so it cannot click.
        int fade = SAMPLE_RATE / 2;
        for (int i = 0; i < fade; i++) {
            float f = i / (float) fade;
            buffer[i] = buffer[i] * f + buffer[length - fade + i] * (1 - f);
        }
        return normalize(buffer, 0.30);
    }

    private static short[] normalize(float[] buffer, double peak) {
        double max = 0.00001;
        for (float v : buffer) {
            max = Math.max(max, Math.abs(v));
        }
        double scale = peak / max;
        short[] samples = new short[buffer.length];
        for (int i = 0; i < buffer.length; i++) {
            samples[i] = (short) (Math.max(-1, Math.min(1, buffer[i] * scale)) * 32767);
        }
        return samples;
    }

    private static void writeWav(FileHandle file, short[] samples) {
        int dataSize = samples.length * 2;
        ByteBuffer buffer = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(new byte[]{'R', 'I', 'F', 'F'});
        buffer.putInt(36 + dataSize);
        buffer.put(new byte[]{'W', 'A', 'V', 'E'});
        buffer.put(new byte[]{'f', 'm', 't', ' '});
        buffer.putInt(16);
        buffer.putShort((short) 1);
        buffer.putShort((short) 1);
        buffer.putInt(SAMPLE_RATE);
        buffer.putInt(SAMPLE_RATE * 2);
        buffer.putShort((short) 2);
        buffer.putShort((short) 16);
        buffer.put(new byte[]{'d', 'a', 't', 'a'});
        buffer.putInt(dataSize);
        for (short sample : samples) {
            buffer.putShort(sample);
        }
        file.writeBytes(buffer.array(), false);
    }

    public void dispose() {
        if (steel != null) {
            steel.dispose();
        }
        if (dirt != null) {
            dirt.dispose();
        }
        if (bolt != null) {
            bolt.dispose();
        }
        if (reload != null) {
            reload.dispose();
        }
        if (empty != null) {
            empty.dispose();
        }
        if (wind != null) {
            wind.stop();
            wind.dispose();
        }
        for (Sound sound : gunshots.values()) {
            sound.dispose();
        }
        gunshots.clear();
        enabled = false;
    }
}
