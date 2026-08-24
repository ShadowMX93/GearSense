package dev.shadowmx.gearsense;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PreferenceTest {
    @Test
    void parsesFriendlySilkAlias() {
        assertEquals(Preference.SILK_TOUCH, Preference.parse("silk"));
        assertEquals(Preference.SILK_TOUCH, Preference.parse("silk-touch"));
    }

    @Test
    void unknownPreferenceIsSafe() {
        assertEquals(Preference.NONE, Preference.parse("unknown"));
        assertEquals(Preference.NONE, Preference.parse(null));
    }
}
