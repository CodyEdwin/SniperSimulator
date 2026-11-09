package com.snipersim.ballistics;

/** Terrain height lookup used by the ballistic integrator for ground impacts. */
public interface TerrainFunction {
    double heightAt(double x, double z);
}
