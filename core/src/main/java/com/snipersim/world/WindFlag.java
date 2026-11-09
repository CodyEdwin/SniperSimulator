package com.snipersim.world;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector3;
import com.snipersim.ballistics.WindModel;

/** A 5 m range flag used to read the wind between the firing point and targets. */
public final class WindFlag {
    public final int range;
    public final float x;
    public final float y;
    public final float z;
    public final float poleHeight;
    public final Color clothColor;

    public WindFlag(int range, float x, float groundY, float z, float poleHeight, Color clothColor) {
        this.range = range;
        this.x = x;
        this.y = groundY;
        this.z = z;
        this.poleHeight = poleHeight;
        this.clothColor = clothColor;
    }

    public float speedAt(WindModel wind, double time) {
        return (float) wind.speedAt(time, poleHeight);
    }

    public void clothTip(WindModel wind, double time, Vector3 out) {
        double speed = wind.speedAt(time, poleHeight);
        double toward = Math.toRadians(wind.bearingAt(time) + 180.0);
        double flutter = Math.sin(time * 6.2 + range * 0.37) * (0.10 + speed * 0.014);
        double length = 0.9 + Math.min(2.1, speed * 0.10);
        float dirX = (float) Math.sin(toward);
        float dirZ = (float) -Math.cos(toward);
        out.set(x + dirX * (float) length,
                y + poleHeight - 0.15f + (float) flutter,
                z + dirZ * (float) length);
    }

    public Vector3 top(Vector3 out) {
        return out.set(x, y + poleHeight, z);
    }
}
