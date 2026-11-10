package com.snipersim.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RangeCourseTest {
    @Test
    void campPerryUsesYardsAndAMatch() {
        assertEquals(182.88f, RangeCourse.yards(200), 0.02f);
        assertEquals(274.32f, RangeCourse.yards(300), 0.02f);
        assertEquals(548.64f, RangeCourse.yards(600), 0.02f);
        assertTrue(RangeCourse.CAMP_PERRY.competition);
        assertFalse(RangeCourse.DAGGER.competition);
        assertFalse(RangeCourse.BISLEY.competition);
    }
}