package dev.shadowmx.gearsense;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VersionNumberTest {
    @Test
    void patchReleasesIncreaseInOrder() {
        assertTrue(VersionNumber.parse("1.0.2").compareTo(VersionNumber.parse("1.0.1")) > 0);
    }

    @Test
    void acceptsGitHubVPrefix() {
        assertEquals(0, VersionNumber.parse("v1.0.0").compareTo(VersionNumber.parse("1.0.0")));
    }

    @Test
    void releaseIsNewerThanPrerelease() {
        assertTrue(VersionNumber.parse("1.1.0").compareTo(VersionNumber.parse("1.1.0-beta.1")) > 0);
    }

    @Test
    void missingComponentsAreZero() {
        assertEquals(0, VersionNumber.parse("1.0").compareTo(VersionNumber.parse("1.0.0")));
    }
}
