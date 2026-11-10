package com.snipersim.world;

/** A place to shoot. Camp Perry runs the National Match Course. */
public enum RangeCourse {
    DAGGER("DAGGER RIDGE", "High desert, steel from 200 m to 2,000 m.", false),
    CAMP_PERRY("CAMP PERRY",
            "Lake Erie, Ohio. Flat grass, pit banks at 200, 300 and 600 yards. National Match Course.",
            true),
    BISLEY("BISLEY STICKLEDOWN",
            "Surrey heath. Known-distance gongs from 300 to 1,200 yards.",
            false);

    public final String title;
    public final String blurb;
    public final boolean competition;

    RangeCourse(String title, String blurb, boolean competition) {
        this.title = title;
        this.blurb = blurb;
        this.competition = competition;
    }

    public static float yards(double yards) {
        return (float) (yards * 0.9144);
    }

    public RangeCourse next() {
        RangeCourse[] all = values();
        return all[(ordinal() + 1) % all.length];
    }
}
