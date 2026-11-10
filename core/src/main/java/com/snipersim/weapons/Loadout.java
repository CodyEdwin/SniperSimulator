package com.snipersim.weapons;

/** One rifle plus the optic package chosen in the catalog. */
public final class Loadout {
    public int rifleIndex;
    public int opticIndex;
    public String opticName;
    public double zoomMin;
    public double zoomMax;
    public double zoomDefault;
    public Reticle reticle;
    public double sightHeight;
    public int zeroDistance = 100;
    public com.snipersim.world.RangeCourse range = com.snipersim.world.RangeCourse.DAGGER;

    public static Loadout issued() {
        Weapon weapon = WeaponCatalog.defaultWeapon();
        Loadout loadout = new Loadout();
        loadout.rifleIndex = WeaponCatalog.indexOf(weapon);
        loadout.adoptRifle(weapon);
        loadout.opticIndex = 2;
        return loadout;
    }

    public void adoptRifle(Weapon weapon) {
        opticName = weapon.optic;
        zoomMin = weapon.scopeMinZoom;
        zoomMax = weapon.scopeMaxZoom;
        zoomDefault = weapon.scopeDefaultZoom;
        reticle = weapon.reticle;
        sightHeight = weapon.sightHeightMeters;
    }

    public void adoptOptic(int index) {
        Optic optic = OpticCatalog.byIndex(index);
        opticIndex = Math.floorMod(index, OpticCatalog.all().size());
        opticName = optic.name;
        zoomMin = optic.minZoom;
        zoomMax = optic.maxZoom;
        zoomDefault = optic.defaultZoom;
        reticle = optic.reticle;
        sightHeight = optic.sightHeightMeters;
    }

    public Weapon build() {
        return WeaponCatalog.byIndex(rifleIndex).withBuild(
                opticName, zoomMin, zoomMax, zoomDefault, reticle, sightHeight);
    }

    public Loadout copy() {
        Loadout copy = new Loadout();
        copy.rifleIndex = rifleIndex;
        copy.opticIndex = opticIndex;
        copy.opticName = opticName;
        copy.zoomMin = zoomMin;
        copy.zoomMax = zoomMax;
        copy.zoomDefault = zoomDefault;
        copy.reticle = reticle;
        copy.sightHeight = sightHeight;
        copy.zeroDistance = zeroDistance;
        copy.range = range;
        return copy;
    }
}
