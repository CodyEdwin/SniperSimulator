package com.snipersim.render;

import com.snipersim.ballistics.Atmosphere;
import com.snipersim.ballistics.BallisticTable;
import com.snipersim.weapons.Weapon;

/** Plain data handed from the simulation to the HUD each frame. */
public final class HudState {
    public Weapon weapon;
    public int weaponIndex;
    public int ammo;
    public int magazine;
    public boolean reloading;
    public float reloadFraction;
    public boolean touchControls;

    public int zeroDistance;
    public int elevationClicks;
    public int windageClicks;
    public double holdoverMils;
    public double zoom;
    public double scopeAmount;
    public boolean scoped;

    public Atmosphere atmosphere;
    public double windSpeed;
    public double windFrom;
    public double windClock;
    public double crosswind;
    public double density;
    public double densityAltitude;

    public int score;
    public int shots;
    public int hits;
    public int bestRing;
    public double longestHit;
    public double bestEnergy;
    public double accuracy;

    public double breath = 1;
    public boolean holdingBreath;
    public boolean steady;

    public BallisticTable dope;
    public boolean dopeVisible;
    public boolean briefing;
    public String missionBrief = "";
    public String rangeTitle = "";
    public String matchLine = "";
    public String[] rangeCard;
    public boolean paused;
    public boolean helpVisible;
    public boolean bulletCam;
    public boolean bulletCamArmed;
    public boolean shaderReady;
    public double compassHeading;
    public float pixelsPerMil = 8f;
    public boolean rangeValid;
    public double rangeMeters;
    public int highlightRange;

    public float toastTimer;
    public boolean toastHit;
    public String toastTitle = "";
    public String toastDetail = "";
    public float hitMarker;
    public float missMarker;
    public float flash;
    public String missText = "";

    public double bulletSpeed;
    public double bulletHeight;
    public double bulletTof;
    public double bulletRange;

    public boolean airborne;
    public float airborneProgress;
}
